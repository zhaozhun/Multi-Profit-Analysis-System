package com.multiprofit.controller;

import com.multiprofit.ai.ModelApiClient;
import com.multiprofit.dto.ApiResponse;
import com.multiprofit.util.IndicatorFactSql;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 数据治理 - 质量扫描/问题识别/指标监控
 * 数据源：dw_indicator_fact(EAV,机构级汇总) + dwd_loan/deposit_detail(明细,公式校验/利差)
 */
@RestController
@RequestMapping("/api/governance")
public class DataGovernanceController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ModelApiClient claudeClient;

    private static final String DEFAULT_PERIOD = "2026-06";

    /** DWD明细UNION视图(同ReportController) */
    private String dwdUnion(String period) {
        String p = IndicatorFactSql.esc(period);
        return "(SELECT biz_id, 'LOAN' as product_type, org_name, product_name, manager_name, customer_name, " +
            "loan_balance as biz_amount, loan_monthly_interest as interest_income, ftp_cost, risk_cost, op_cost, " +
            "0 as ftp_income, loan_monthly_interest as cust_interest, loan_profit as net_profit, " +
            "(loan_monthly_interest - ftp_cost - risk_cost - op_cost) as expected_profit " +
            "FROM dwd_loan_detail WHERE account_period='" + p + "' " +
            "UNION ALL " +
            "SELECT biz_id, 'DEPOSIT', org_name, product_name, manager_name, customer_name, " +
            "deposit_balance, deposit_monthly_interest, 0, 0, op_cost, " +
            "ftp_income, deposit_monthly_interest, deposit_profit, " +
            "(ftp_income - deposit_monthly_interest - op_cost) " +
            "FROM dwd_deposit_detail WHERE account_period='" + p + "')";
    }

    /**
     * 数据质量扫描
     */
    @PostMapping("/scan")
    public ApiResponse<Map<String, Object>> scanQuality(
            @RequestParam(required = false, defaultValue = DEFAULT_PERIOD) String period) {

        Map<String, Object> report = new HashMap<>();
        Map<String, Object> completeness = checkCompleteness(period);
        Map<String, Object> consistency = checkConsistency(period);
        Map<String, Object> accuracy = checkAccuracy(period);
        Map<String, Object> timeliness = checkTimeliness(period);
        report.put("completeness", completeness);
        report.put("consistency", consistency);
        report.put("accuracy", accuracy);
        report.put("timeliness", timeliness);

        int overall = ((int) completeness.get("score") + (int) consistency.get("score")
            + (int) accuracy.get("score") + (int) timeliness.get("score")) / 4;
        report.put("overallScore", overall);
        report.put("period", period);
        return ApiResponse.ok(report);
    }

    /**
     * 获取问题列表
     */
    @GetMapping("/issues")
    public ApiResponse<List<Map<String, Object>>> getIssues(
            @RequestParam(required = false, defaultValue = DEFAULT_PERIOD) String period) {
        List<Map<String, Object>> issues = new ArrayList<>();
        issues.addAll(checkFormulaBalance(period));
        issues.addAll(checkAnomaly(period));
        issues.addAll(checkMissingData(period));
        return ApiResponse.ok(issues);
    }

    /** 完整性检测 - DWD明细必填字段空值 */
    private Map<String, Object> checkCompleteness(String period) {
        Map<String, Object> result = new HashMap<>();
        Integer nullCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM " + dwdUnion(period) + " d " +
            "WHERE d.org_name IS NULL OR d.product_name IS NULL OR d.manager_name IS NULL", Integer.class);
        Integer totalCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM " + dwdUnion(period) + " d", Integer.class);

        int score = 100;
        String desc = "必填字段完整，无缺失";
        List<String> issues = new ArrayList<>();
        if (nullCount != null && nullCount > 0) {
            score = Math.max(0, 100 - (nullCount * 100 / (totalCount != null ? totalCount : 1)));
            desc = String.format("发现 %d 条记录存在空值", nullCount);
            issues.add(String.format("%d 条记录缺少必填字段", nullCount));
        }
        result.put("score", score);
        result.put("desc", desc);
        result.put("issues", issues);
        result.put("nullCount", nullCount != null ? nullCount : 0);
        return result;
    }

    /** 一致性检测 - DWD利润公式平衡 */
    private Map<String, Object> checkConsistency(String period) {
        Map<String, Object> result = new HashMap<>();
        Integer mismatchCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM " + dwdUnion(period) + " d " +
            "WHERE ABS(d.net_profit - d.expected_profit) > 0.01", Integer.class);

        int score = 100;
        String desc = "利润公式平衡，数据一致";
        List<String> issues = new ArrayList<>();
        if (mismatchCount != null && mismatchCount > 0) {
            score = Math.max(0, 100 - (mismatchCount * 5));
            desc = String.format("发现 %d 条记录利润公式不平衡", mismatchCount);
            issues.add(String.format("%d 条记录存在公式不一致", mismatchCount));
        }
        result.put("score", score);
        result.put("desc", desc);
        result.put("issues", issues);
        result.put("mismatchCount", mismatchCount != null ? mismatchCount : 0);
        return result;
    }

    /** 准确性检测 - 机构利润环比异常波动(>50%) */
    private Map<String, Object> checkAccuracy(String period) {
        Map<String, Object> result = new HashMap<>();
        Integer anomalyCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM (" +
            "  SELECT dim_id, period, calc_value as profit, " +
            "    LAG(calc_value) OVER (PARTITION BY dim_id ORDER BY period) as prev_profit " +
            "  FROM dw_indicator_fact " +
            "  WHERE period_type='MONTH' AND dim_type='ORG' AND caliber_type='ASSESS' " +
            "    AND indicator_code='" + IndicatorFactSql.TOTAL_PROFIT + "'" +
            ") t WHERE t.period='" + IndicatorFactSql.esc(period) + "' AND t.prev_profit IS NOT NULL " +
            "AND ABS((t.profit - t.prev_profit) / NULLIF(t.prev_profit, 0)) > 0.5", Integer.class);

        int score = anomalyCount != null && anomalyCount > 0 ? Math.max(60, 100 - anomalyCount * 10) : 100;
        String desc = anomalyCount != null && anomalyCount > 0 ?
            String.format("发现 %d 个主体存在异常波动", anomalyCount) : "数据准确，无明显异常";
        List<String> issues = new ArrayList<>();
        if (anomalyCount != null && anomalyCount > 0) {
            issues.add(String.format("%d 个机构利润环比波动超过50%%", anomalyCount));
        }
        result.put("score", score);
        result.put("desc", desc);
        result.put("issues", issues);
        result.put("anomalyCount", anomalyCount != null ? anomalyCount : 0);
        return result;
    }

    /** 时效性检测 - 最新期间 */
    private Map<String, Object> checkTimeliness(String period) {
        Map<String, Object> result = new HashMap<>();
        String latestPeriod = jdbcTemplate.queryForObject(
            "SELECT MAX(period) FROM dw_indicator_fact WHERE period_type='MONTH'", String.class);

        int score = 100;
        String desc = "数据更新及时";
        if (latestPeriod != null && latestPeriod.compareTo(period) < 0) {
            score = 70;
            desc = String.format("最新数据为 %s，落后于当前期间 %s", latestPeriod, period);
        }
        result.put("score", score);
        result.put("desc", desc);
        result.put("latestPeriod", latestPeriod);
        return result;
    }

    /** 检查利润公式不平衡 - DWD明细TOP5 */
    private List<Map<String, Object>> checkFormulaBalance(String period) {
        List<Map<String, Object>> issues = new ArrayList<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT d.biz_id, d.org_name, d.product_name, d.net_profit, d.expected_profit " +
            "FROM " + dwdUnion(period) + " d " +
            "WHERE ABS(d.net_profit - d.expected_profit) > 0.01 LIMIT 5");

        for (Map<String, Object> row : rows) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("type", "一致性");
            issue.put("level", "warning");
            issue.put("title", String.format("业务 %s 利润公式不平衡", row.get("biz_id")));
            issue.put("aiAnalysis", String.format("实际利润 %.2f，计算利润 %.2f，差异 %.2f",
                toDouble(row.get("net_profit")), toDouble(row.get("expected_profit")),
                Math.abs(toDouble(row.get("net_profit")) - toDouble(row.get("expected_profit")))));
            issue.put("suggestion", "检查各成本项数据是否正确录入");
            issue.put("status", "pending");
            issues.add(issue);
        }
        return issues;
    }

    /** 检查异常波动 - 机构利润环比>30% */
    private List<Map<String, Object>> checkAnomaly(String period) {
        List<Map<String, Object>> issues = new ArrayList<>();
        String prevPeriod = getPreviousMonth(period);

        Map<String, Double> curMap = new HashMap<>();
        List<Map<String, Object>> cur = jdbcTemplate.queryForList(
            "SELECT dim_name, calc_value as profit FROM dw_indicator_fact " +
            "WHERE period_type='MONTH' AND period='" + IndicatorFactSql.esc(period) + "' " +
            "AND dim_type='ORG' AND caliber_type='ASSESS' AND indicator_code='" + IndicatorFactSql.TOTAL_PROFIT + "'");
        for (Map<String, Object> row : cur) {
            curMap.put((String) row.get("dim_name"), toDouble(row.get("profit")));
        }

        List<Map<String, Object>> prev = jdbcTemplate.queryForList(
            "SELECT dim_name, calc_value as profit FROM dw_indicator_fact " +
            "WHERE period_type='MONTH' AND period='" + IndicatorFactSql.esc(prevPeriod) + "' " +
            "AND dim_type='ORG' AND caliber_type='ASSESS' AND indicator_code='" + IndicatorFactSql.TOTAL_PROFIT + "'");
        Map<String, Double> prevMap = new HashMap<>();
        for (Map<String, Object> row : prev) {
            prevMap.put((String) row.get("dim_name"), toDouble(row.get("profit")));
        }

        for (Map.Entry<String, Double> e : curMap.entrySet()) {
            String orgName = e.getKey();
            double currentProfit = e.getValue();
            Double prevProfit = prevMap.get(orgName);
            if (prevProfit != null && prevProfit != 0) {
                double changeRate = (currentProfit - prevProfit) / Math.abs(prevProfit) * 100;
                if (Math.abs(changeRate) > 30) {
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("type", "准确性");
                    issue.put("level", Math.abs(changeRate) > 50 ? "critical" : "warning");
                    issue.put("title", String.format("%s 利润环比变化 %.1f%%", orgName, changeRate));
                    issue.put("aiAnalysis", String.format("当期利润 %.0f 万，上期 %.0f 万。%s",
                        currentProfit, prevProfit,
                        changeRate < 0 ? "利润大幅下降，需关注资产质量" : "利润大幅增长，业务表现优异"));
                    issue.put("suggestion", changeRate < 0 ? "核实该机构资产质量，关注不良贷款" : "总结成功经验，推广最佳实践");
                    issue.put("status", "pending");
                    issues.add(issue);
                }
            }
        }
        return issues;
    }

    /** 检查缺失数据 - DWD缺客户经理 */
    private List<Map<String, Object>> checkMissingData(String period) {
        List<Map<String, Object>> issues = new ArrayList<>();
        Integer missingManager = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM " + dwdUnion(period) + " d WHERE d.manager_name IS NULL OR d.manager_name=''", Integer.class);

        if (missingManager != null && missingManager > 0) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("type", "完整性");
            issue.put("level", "info");
            issue.put("title", String.format("%d 条记录缺少客户经理信息", missingManager));
            issue.put("aiAnalysis", "可能是批量导入时字段映射遗漏");
            issue.put("suggestion", "补录客户经理关联");
            issue.put("status", "pending");
            issues.add(issue);
        }
        return issues;
    }

    /**
     * 每日指标监控
     */
    @PostMapping("/monitor")
    public ApiResponse<List<Map<String, Object>>> monitorIndicators(
            @RequestParam(required = false, defaultValue = DEFAULT_PERIOD) String period) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        alerts.addAll(monitorProfitChange(period));
        alerts.addAll(monitorCostAnomaly(period));
        alerts.addAll(monitorDepositSpread(period));
        alerts.addAll(monitorLoanQuality(period));
        return ApiResponse.ok(alerts);
    }

    /** 监控利润异常波动 - 机构环比>20% */
    private List<Map<String, Object>> monitorProfitChange(String period) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        String prevPeriod = getPreviousMonth(period);

        Map<String, Double> curMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
            "SELECT dim_name, calc_value as profit FROM dw_indicator_fact " +
            "WHERE period_type='MONTH' AND period='" + IndicatorFactSql.esc(period) + "' " +
            "AND dim_type='ORG' AND caliber_type='ASSESS' AND indicator_code='" + IndicatorFactSql.TOTAL_PROFIT + "'")) {
            curMap.put((String) row.get("dim_name"), toDouble(row.get("profit")));
        }
        Map<String, Double> prevMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
            "SELECT dim_name, calc_value as profit FROM dw_indicator_fact " +
            "WHERE period_type='MONTH' AND period='" + IndicatorFactSql.esc(prevPeriod) + "' " +
            "AND dim_type='ORG' AND caliber_type='ASSESS' AND indicator_code='" + IndicatorFactSql.TOTAL_PROFIT + "'")) {
            prevMap.put((String) row.get("dim_name"), toDouble(row.get("profit")));
        }

        for (Map.Entry<String, Double> e : curMap.entrySet()) {
            Double prevProfit = prevMap.get(e.getKey());
            if (prevProfit != null && prevProfit != 0) {
                double changeRate = (e.getValue() - prevProfit) / Math.abs(prevProfit) * 100;
                if (Math.abs(changeRate) > 20) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("metric", "净利润");
                    alert.put("dimName", e.getKey());
                    alert.put("currentValue", e.getValue());
                    alert.put("previousValue", prevProfit);
                    alert.put("changeRate", Math.round(changeRate * 10) / 10.0);
                    alert.put("level", Math.abs(changeRate) > 40 ? "CRITICAL" : "WARNING");
                    alert.put("message", String.format("%s 净利润环比 %s%.1f%%",
                        e.getKey(), changeRate > 0 ? "+" : "", changeRate));
                    alert.put("aiAnalysis", changeRate < 0 ?
                        "利润大幅下降，建议检查资产质量和成本控制情况" :
                        "利润大幅增长，业务表现优异，建议总结成功经验");
                    alerts.add(alert);
                }
            }
        }
        return alerts;
    }

    /** 监控成本异常 - 机构成本收入比>70% (EAV透视) */
    private List<Map<String, Object>> monitorCostAnomaly(String period) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT t.dim_name as org_name, t.revenue, t.op_cost " +
            "FROM (" + IndicatorFactSql.pivot("ORG", period) + ") t");

        for (Map<String, Object> row : rows) {
            double revenue = toDouble(row.get("revenue"));
            double opCost = toDouble(row.get("op_cost"));
            if (revenue > 0) {
                double costRatio = opCost / revenue * 100;
                if (costRatio > 70) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("metric", "成本收入比");
                    alert.put("dimName", row.get("org_name"));
                    alert.put("currentValue", Math.round(costRatio * 10) / 10.0);
                    alert.put("level", costRatio > 80 ? "CRITICAL" : "WARNING");
                    alert.put("message", String.format("%s 成本收入比 %.1f%%，偏高", row.get("org_name"), costRatio));
                    alert.put("aiAnalysis", "成本收入比偏高，建议优化运营成本结构");
                    alerts.add(alert);
                }
            }
        }
        return alerts;
    }

    /** 监控存款利差 - DWD存款明细 */
    private List<Map<String, Object>> monitorDepositSpread(String period) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        String p = IndicatorFactSql.esc(period);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT org_name, sum(ftp_income) as ftp_income, sum(deposit_monthly_interest) as cust_interest, " +
            "sum(deposit_balance) as deposit_balance " +
            "FROM dwd_deposit_detail WHERE account_period='" + p + "' GROUP BY org_name");

        for (Map<String, Object> row : rows) {
            double ftpIncome = toDouble(row.get("ftp_income"));
            double custInterest = toDouble(row.get("cust_interest"));
            double depositBalance = toDouble(row.get("deposit_balance"));
            if (depositBalance > 0) {
                double spread = (ftpIncome - custInterest) / depositBalance * 100;
                if (spread < 0.3) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("metric", "存款利差");
                    alert.put("dimName", row.get("org_name"));
                    alert.put("currentValue", Math.round(spread * 100) / 100.0);
                    alert.put("level", "WARNING");
                    alert.put("message", String.format("%s 存款利差 %.2f%%，偏低", row.get("org_name"), spread));
                    alert.put("aiAnalysis", "存款利差偏低，可能是对客利率偏高或FTP定价偏低");
                    alerts.add(alert);
                }
            }
        }
        return alerts;
    }

    /** 监控贷款资产质量 - DWD贷款明细 */
    private List<Map<String, Object>> monitorLoanQuality(String period) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        String p = IndicatorFactSql.esc(period);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT org_name, sum(loan_monthly_interest) as loan_income, sum(risk_cost) as risk_cost " +
            "FROM dwd_loan_detail WHERE account_period='" + p + "' GROUP BY org_name");

        for (Map<String, Object> row : rows) {
            double loanIncome = toDouble(row.get("loan_income"));
            double riskCost = toDouble(row.get("risk_cost"));
            if (loanIncome > 0) {
                double riskRate = riskCost / loanIncome * 100;
                if (riskRate > 12) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("metric", "风险成本率");
                    alert.put("dimName", row.get("org_name"));
                    alert.put("currentValue", Math.round(riskRate * 10) / 10.0);
                    alert.put("level", riskRate > 15 ? "CRITICAL" : "WARNING");
                    alert.put("message", String.format("%s 风险成本率 %.1f%%，资产质量需关注", row.get("org_name"), riskRate));
                    alert.put("aiAnalysis", "风险成本率偏高，建议关注不良贷款和逾期情况");
                    alerts.add(alert);
                }
            }
        }
        return alerts;
    }

    private String getPreviousMonth(String period) {
        String[] parts = period.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1;
        if (month < 1) { month = 12; year--; }
        return String.format("%d-%02d", year, month);
    }

    private double toDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0; }
    }
}
