package com.multiprofit.service.impl;

import com.multiprofit.ai.ModelApiClient;
import com.multiprofit.model.AlertRecord;
import com.multiprofit.service.DataValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DataValidationServiceImpl implements DataValidationService {

    @Autowired
    private ModelApiClient claudeClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${validation.anomaly.profit-change-threshold:20}")
    private double profitChangeThreshold;

    @Value("${validation.anomaly.cost-change-threshold:15}")
    private double costChangeThreshold;

    @Value("${validation.anomaly.consecutive-decline-months:3}")
    private int consecutiveDeclineMonths;

    @Override
    public List<ValidationResult> validate(Map<String, Object> record) {
        List<ValidationResult> results = new ArrayList<>();

        // 1. 利润公式校验
        ValidationResult formulaResult = validateProfitFormula(record);
        if (formulaResult != null) {
            results.add(formulaResult);
        }

        // 2. 必填字段校验（星型模型使用ID外键）
        if (record.get("org_id") == null) {
            results.add(new ValidationResult(ValidationResult.Level.ERROR, "MISSING_ORG", "机构ID不能为空"));
        }
        if (record.get("product_id") == null) {
            results.add(new ValidationResult(ValidationResult.Level.ERROR, "MISSING_PRODUCT", "产品ID不能为空"));
        }

        // 3. 逻辑校验
        Object revenueObj = record.get("revenue");
        BigDecimal revenue = revenueObj != null ? new BigDecimal(revenueObj.toString()) : BigDecimal.ZERO;
        if (revenue.compareTo(BigDecimal.ZERO) < 0) {
            results.add(new ValidationResult(ValidationResult.Level.WARNING, "NEGATIVE_REVENUE", "收入为负值，请确认"));
        }

        return results;
    }

    @Override
    public Map<String, List<ValidationResult>> batchValidate(List<Map<String, Object>> records) {
        Map<String, List<ValidationResult>> resultMap = new LinkedHashMap<>();
        for (Map<String, Object> record : records) {
            List<ValidationResult> results = validate(record);
            if (!results.isEmpty()) {
                resultMap.put(String.valueOf(record.get("biz_id")), results);
            }
        }
        return resultMap;
    }

    @Override
    public ValidationResult validateProfitFormula(Map<String, Object> record) {
        BigDecimal expected;
        String formulaDesc;

        if ("DEPOSIT".equals((String)record.get("product_type"))) {
            // 存款：净利润 = FTP收入 - 对客利息支出 - 运营成本
            BigDecimal ftpIncome = toBD(record.get("interest_income"));
            BigDecimal custInterest = toBD(record.get("interest_expense"));
            BigDecimal op = toBD(record.get("op_cost"));
            expected = ftpIncome.subtract(custInterest).subtract(op);
            formulaDesc = "存款公式：FTP收入 - 对客利息支出 - 运营成本";
        } else if ("LOAN".equals((String)record.get("product_type"))) {
            // 贷款：净利润 = 对客利息收入 - FTP成本 - 风险成本 - 运营成本
            BigDecimal income = toBD(record.get("interest_income"));
            BigDecimal ftp = toBD(record.get("ftp_cost"));
            BigDecimal risk = toBD(record.get("risk_cost"));
            BigDecimal op = toBD(record.get("op_cost"));
            expected = income.subtract(ftp).subtract(risk).subtract(op);
            formulaDesc = "贷款公式：对客利息收入 - FTP成本 - 风险成本 - 运营成本";
        } else {
            // 其他：净利润 = 收入 - FTP成本 - 风险成本 - 运营成本
            BigDecimal rev = toBD(record.get("revenue"));
            BigDecimal ftp = toBD(record.get("ftp_cost"));
            BigDecimal risk = toBD(record.get("risk_cost"));
            BigDecimal op = toBD(record.get("op_cost"));
            expected = rev.subtract(ftp).subtract(risk).subtract(op);
            formulaDesc = "通用公式：收入 - FTP成本 - 风险成本 - 运营成本";
        }

        BigDecimal actual = toBD(record.get("net_profit"));
        BigDecimal diff = expected.subtract(actual).abs();

        // 允许0.01的精度误差
        if (diff.compareTo(new BigDecimal("0.01")) > 0) {
            ValidationResult result = new ValidationResult(
                ValidationResult.Level.ERROR,
                "FORMULA_MISMATCH",
                String.format("利润公式不平衡：期望%.2f，实际%.2f，差异%.2f",
                    expected, actual, diff)
            );
            result.setExpected(expected);
            result.setActual(actual);
            return result;
        }
        return null;
    }

    @Override
    public List<ValidationResult> detectAnomaly(String period, String dimType) {
        List<ValidationResult> anomalies = new ArrayList<>();
        String nameCol = getDimNameColumn(dimType);

        // 获取当期数据
        String currentSql = String.format(
            "SELECT %s as name, sum(revenue) as revenue, sum(net_profit) as net_profit, " +
            "sum(ftp_cost) as ftp_cost, sum(risk_cost) as risk_cost, sum(op_cost) as op_cost " +
            "FROM dw_indicator_fact WHERE period = '%s' GROUP BY %s",
            nameCol, period, nameCol
        );
        List<Map<String, Object>> currentData = jdbcTemplate.queryForList(currentSql);

        // 获取上期数据
        String prevMonth = getPreviousMonth(period);
        String prevSql = String.format(
            "SELECT %s as name, sum(revenue) as revenue, sum(net_profit) as net_profit " +
            "FROM dw_indicator_fact WHERE period = '%s' GROUP BY %s",
            nameCol, prevMonth, nameCol
        );
        List<Map<String, Object>> prevData = jdbcTemplate.queryForList(prevSql);
        Map<String, Map<String, Object>> prevMap = new HashMap<>();
        for (Map<String, Object> row : prevData) {
            prevMap.put(String.valueOf(row.get("name")), row);
        }

        // 对比分析
        for (Map<String, Object> current : currentData) {
            String name = String.valueOf(current.get("name"));
            BigDecimal currentProfit = toBD(current.get("net_profit"));
            BigDecimal currentRevenue = toBD(current.get("revenue"));

            Map<String, Object> prev = prevMap.get(name);
            if (prev == null) continue;

            BigDecimal prevProfit = toBD(prev.get("net_profit"));

            // 利润环比异常检测
            if (prevProfit.compareTo(BigDecimal.ZERO) != 0) {
                double changeRate = currentProfit.subtract(prevProfit)
                        .multiply(new BigDecimal("100"))
                        .divide(prevProfit.abs(), 2, BigDecimal.ROUND_HALF_UP)
                        .doubleValue();

                if (Math.abs(changeRate) > profitChangeThreshold) {
                    ValidationResult result = new ValidationResult(
                        changeRate < 0 ? ValidationResult.Level.WARNING : ValidationResult.Level.INFO,
                        "PROFIT_ANOMALY",
                        String.format("%s 利润环比变化 %.2f%%（当期: %.2f, 上期: %.2f）",
                            name, changeRate, currentProfit, prevProfit)
                    );
                    result.setAnomalyValue(new BigDecimal(changeRate));

                    // AI根因分析
                    if (claudeClient.isAvailable()) {
                        String analysis = claudeClient.chat(String.format(
                            "分析%s的利润变化：%s环比变化%.2f%%，当期利润%.2f万，上期%.2f万，收入%.2f万。请给出可能原因。",
                            name, name, changeRate, currentProfit, prevProfit, currentRevenue
                        ));
                        result.setAiAnalysis(analysis);
                    }

                    anomalies.add(result);
                }
            }

            // 亏损主体检测
            if (currentProfit.compareTo(BigDecimal.ZERO) < 0) {
                ValidationResult result = new ValidationResult(
                    ValidationResult.Level.WARNING,
                    "LOSS_ENTITY",
                    String.format("%s 当期亏损 %.2f 万元", name, currentProfit.abs())
                );
                anomalies.add(result);
            }

            // 成本异常检测
            BigDecimal ftpCost = toBD(current.get("ftp_cost"));
            BigDecimal riskCost = toBD(current.get("risk_cost"));
            BigDecimal opCost = toBD(current.get("op_cost"));
            BigDecimal totalCost = ftpCost.add(riskCost).add(opCost);

            if (currentRevenue.compareTo(BigDecimal.ZERO) != 0) {
                double costRatio = totalCost.multiply(new BigDecimal("100"))
                        .divide(currentRevenue, 2, BigDecimal.ROUND_HALF_UP)
                        .doubleValue();

                if (costRatio > 90) {
                    ValidationResult result = new ValidationResult(
                        ValidationResult.Level.WARNING,
                        "HIGH_COST_RATIO",
                        String.format("%s 成本收入比 %.2f%%，成本过高", name, costRatio)
                    );
                    anomalies.add(result);
                }
            }
        }

        return anomalies;
    }

    @Override
    public void saveAlertRecords(List<ValidationResult> anomalies) {
        // TODO: 保存到MySQL alert_record表
    }

    private String getDimNameColumn(String dimType) {
        return switch (dimType) {
            case "ORG" -> "org_name";
            case "BIZ_LINE" -> "biz_line_name";
            case "DEPT" -> "dept_name";
            case "PRODUCT" -> "product_name";
            case "CHANNEL" -> "channel_name";
            case "MANAGER" -> "manager_name";
            default -> "org_name";
        };
    }

    private BigDecimal toBD(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }

    private String getPreviousMonth(String period) {
        String[] parts = period.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1;
        if (month < 1) { month = 12; year--; }
        return String.format("%d-%02d", year, month);
    }
}
