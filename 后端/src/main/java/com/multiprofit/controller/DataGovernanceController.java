package com.multiprofit.controller;

import com.multiprofit.ai.ModelApiClient;
import com.multiprofit.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/governance")
public class DataGovernanceController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ModelApiClient claudeClient;

    /**
     * 数据质量扫描
     */
    @PostMapping("/scan")
    public ApiResponse<Map<String, Object>> scanQuality(
            @RequestParam(required = false, defaultValue = "2026-05") String period) {

        Map<String, Object> report = new HashMap<>();

        // 1. 完整性检测
        Map<String, Object> completeness = checkCompleteness(period);
        report.put("completeness", completeness);

        // 2. 一致性检测（利润公式）
        Map<String, Object> consistency = checkConsistency(period);
        report.put("consistency", consistency);

        // 3. 准确性检测（异常值）
        Map<String, Object> accuracy = checkAccuracy(period);
        report.put("accuracy", accuracy);

        // 4. 时效性检测
        Map<String, Object> timeliness = checkTimeliness(period);
        report.put("timeliness", timeliness);

        // 5. 计算总分
        int completenessScore = (int) completeness.get("score");
        int consistencyScore = (int) consistency.get("score");
        int accuracyScore = (int) accuracy.get("score");
        int timelinessScore = (int) timeliness.get("score");
        int overallScore = (completenessScore + consistencyScore + accuracyScore + timelinessScore) / 4;

        report.put("overallScore", overallScore);
        report.put("period", period);

        return ApiResponse.ok(report);
    }

    /**
     * 获取问题列表
     */
    @GetMapping("/issues")
    public ApiResponse<List<Map<String, Object>>> getIssues(
            @RequestParam(required = false, defaultValue = "2026-05") String period) {

        List<Map<String, Object>> issues = new ArrayList<>();

        // 检查利润公式不平衡
        List<Map<String, Object>> formulaIssues = checkFormulaBalance(period);
        issues.addAll(formulaIssues);

        // 检查异常波动
        List<Map<String, Object>> anomalyIssues = checkAnomaly(period);
        issues.addAll(anomalyIssues);

        // 检查缺失数据
        List<Map<String, Object>> missingIssues = checkMissingData(period);
        issues.addAll(missingIssues);

        return ApiResponse.ok(issues);
    }

    /**
     * 完整性检测
     */
    private Map<String, Object> checkCompleteness(String period) {
        Map<String, Object> result = new HashMap<>();

        // 检查必填字段空值（ID字段）
        Integer nullCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM dw_indicator_fact WHERE account_period = '" + period + "' " +
            "AND (org_id IS NULL OR product_id IS NULL OR manager_id IS NULL)",
            Integer.class
        );

        Integer totalCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM dw_indicator_fact WHERE account_period = '" + period + "'",
            Integer.class
        );

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

    /**
     * 一致性检测
     */
    private Map<String, Object> checkConsistency(String period) {
        Map<String, Object> result = new HashMap<>();

        // 检查利润公式：净利润 = 收入 - FTP - 风险 - 运营
        Integer mismatchCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM dw_indicator_fact WHERE account_period = '" + period + "' " +
            "AND ABS(net_profit - (revenue - ftp_cost - risk_cost - op_cost)) > 0.01",
            Integer.class
        );

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

    /**
     * 准确性检测
     */
    private Map<String, Object> checkAccuracy(String period) {
        Map<String, Object> result = new HashMap<>();

        // 检查异常波动：环比变化超过50%
        Integer anomalyCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM (" +
            "  SELECT org_id, account_period, sum(net_profit) as profit," +
            "    LAG(sum(net_profit)) OVER (PARTITION BY org_id ORDER BY account_period) as prev_profit" +
            "  FROM dw_indicator_fact GROUP BY org_id, account_period" +
            ") t WHERE account_period = '" + period + "' AND prev_profit IS NOT NULL " +
            "AND ABS((profit - prev_profit) / NULLIF(prev_profit, 0)) > 0.5",
            Integer.class
        );

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

    /**
     * 时效性检测
     */
    private Map<String, Object> checkTimeliness(String period) {
        Map<String, Object> result = new HashMap<>();

        // 检查数据是否更新到最新期间
        String latestPeriod = jdbcTemplate.queryForObject(
            "SELECT MAX(account_period) FROM dw_indicator_fact", String.class
        );

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

    /**
     * 检查利润公式不平衡
     */
    private List<Map<String, Object>> checkFormulaBalance(String period) {
        List<Map<String, Object>> issues = new ArrayList<>();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT bl.biz_id, org.name as org_name, prod.name as product_name, bl.net_profit, " +
            "(bl.revenue - bl.ftp_cost - bl.risk_cost - bl.op_cost) as expected_profit " +
            "FROM dw_indicator_fact bl " +
            "LEFT JOIN dim_organization org ON bl.org_id = org.id " +
            "LEFT JOIN dim_organization prod ON bl.product_id = prod.id " +
            "WHERE bl.account_period = '" + period + "' " +
            "AND ABS(bl.net_profit - (bl.revenue - bl.ftp_cost - bl.risk_cost - bl.op_cost)) > 0.01 LIMIT 5"
        );

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

    /**
     * 检查异常波动
     */
    private List<Map<String, Object>> checkAnomaly(String period) {
        List<Map<String, Object>> issues = new ArrayList<>();

        // 检查各机构环比变化
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT org.name as org_name, sum(bl.net_profit) as profit FROM dw_indicator_fact bl " +
            "LEFT JOIN dim_organization org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '" + period + "' GROUP BY org.name"
        );

        String prevPeriod = getPreviousMonth(period);
        List<Map<String, Object>> prevRows = jdbcTemplate.queryForList(
            "SELECT org.name as org_name, sum(bl.net_profit) as profit FROM dw_indicator_fact bl " +
            "LEFT JOIN dim_organization org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '" + prevPeriod + "' GROUP BY org.name"
        );

        Map<String, Double> prevMap = new HashMap<>();
        for (Map<String, Object> row : prevRows) {
            prevMap.put((String) row.get("org_name"), toDouble(row.get("profit")));
        }

        for (Map<String, Object> row : rows) {
            String orgName = (String) row.get("org_name");
            double currentProfit = toDouble(row.get("profit"));
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

    /**
     * 检查缺失数据
     */
    private List<Map<String, Object>> checkMissingData(String period) {
        List<Map<String, Object>> issues = new ArrayList<>();

        // 检查缺少客户经理的记录
        Integer missingManager = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM dw_indicator_fact WHERE account_period = '" + period + "' " +
            "AND (manager_id IS NULL OR manager_id = 0)",
            Integer.class
        );

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
     * 每日指标监控（定时调用或手动触发）
     * 监控各指标是否异常，返回需要关注的问题
     */
    @PostMapping("/monitor")
    public ApiResponse<List<Map<String, Object>>> monitorIndicators(
            @RequestParam(required = false, defaultValue = "2026-05") String period) {

        List<Map<String, Object>> alerts = new ArrayList<>();

        // 1. 监控利润异常波动
        alerts.addAll(monitorProfitChange(period));

        // 2. 监控成本异常
        alerts.addAll(monitorCostAnomaly(period));

        // 3. 监控存款利差异常
        alerts.addAll(monitorDepositSpread(period));

        // 4. 监控贷款资产质量
        alerts.addAll(monitorLoanQuality(period));

        return ApiResponse.ok(alerts);
    }

    /**
     * 监控利润异常波动
     */
    private List<Map<String, Object>> monitorProfitChange(String period) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        String prevPeriod = getPreviousMonth(period);

        List<Map<String, Object>> current = jdbcTemplate.queryForList(
            "SELECT org.name as org_name, sum(bl.net_profit) as profit FROM dw_indicator_fact bl " +
            "LEFT JOIN dim_organization org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '" + period + "' GROUP BY org.name"
        );

        Map<String, Double> prevMap = new HashMap<>();
        List<Map<String, Object>> prev = jdbcTemplate.queryForList(
            "SELECT org.name as org_name, sum(bl.net_profit) as profit FROM dw_indicator_fact bl " +
            "LEFT JOIN dim_organization org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '" + prevPeriod + "' GROUP BY org.name"
        );
        for (Map<String, Object> row : prev) {
            prevMap.put((String) row.get("org_name"), toDouble(row.get("profit")));
        }

        for (Map<String, Object> row : current) {
            String orgName = (String) row.get("org_name");
            double currentProfit = toDouble(row.get("profit"));
            Double prevProfit = prevMap.get(orgName);

            if (prevProfit != null && prevProfit != 0) {
                double changeRate = (currentProfit - prevProfit) / Math.abs(prevProfit) * 100;
                if (Math.abs(changeRate) > 20) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("metric", "净利润");
                    alert.put("dimName", orgName);
                    alert.put("currentValue", currentProfit);
                    alert.put("previousValue", prevProfit);
                    alert.put("changeRate", Math.round(changeRate * 10) / 10.0);
                    alert.put("level", Math.abs(changeRate) > 40 ? "CRITICAL" : "WARNING");
                    alert.put("message", String.format("%s 净利润环比 %s%.1f%%",
                        orgName, changeRate > 0 ? "+" : "", changeRate));
                    alert.put("aiAnalysis", changeRate < 0 ?
                        "利润大幅下降，建议检查资产质量和成本控制情况" :
                        "利润大幅增长，业务表现优异，建议总结成功经验");
                    alerts.add(alert);
                }
            }
        }
        return alerts;
    }

    /**
     * 监控成本异常
     */
    private List<Map<String, Object>> monitorCostAnomaly(String period) {
        List<Map<String, Object>> alerts = new ArrayList<>();

        // 检查各机构成本收入比
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT org.name as org_name, " +
            "sum(bl.revenue) as revenue, " +
            "sum(bl.ftp_cost + bl.risk_cost + bl.op_cost) as total_cost " +
            "FROM dw_indicator_fact bl " +
            "LEFT JOIN dim_organization org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '" + period + "' " +
            "GROUP BY org.name"
        );

        for (Map<String, Object> row : rows) {
            double revenue = toDouble(row.get("revenue"));
            double totalCost = toDouble(row.get("total_cost"));
            if (revenue > 0) {
                double costRatio = totalCost / revenue * 100;
                if (costRatio > 70) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("metric", "成本收入比");
                    alert.put("dimName", row.get("org_name"));
                    alert.put("currentValue", Math.round(costRatio * 10) / 10.0);
                    alert.put("level", costRatio > 80 ? "CRITICAL" : "WARNING");
                    alert.put("message", String.format("%s 成本收入比 %.1f%%，偏高",
                        row.get("org_name"), costRatio));
                    alert.put("aiAnalysis", "成本收入比偏高，建议优化运营成本结构");
                    alerts.add(alert);
                }
            }
        }
        return alerts;
    }

    /**
     * 监控存款利差
     */
    private List<Map<String, Object>> monitorDepositSpread(String period) {
        List<Map<String, Object>> alerts = new ArrayList<>();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT org.name as org_name, " +
            "sum(bl.interest_income) as ftp_income, " +
            "sum(bl.interest_expense) as cust_interest, " +
            "sum(bl.biz_amount) as deposit_balance " +
            "FROM dw_indicator_fact bl " +
            "LEFT JOIN dim_organization org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '" + period + "' AND bl.product_type = 'DEPOSIT' " +
            "GROUP BY org.name"
        );

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
                    alert.put("message", String.format("%s 存款利差 %.2f%%，偏低",
                        row.get("org_name"), spread));
                    alert.put("aiAnalysis", "存款利差偏低，可能是对客利率偏高或FTP定价偏低");
                    alerts.add(alert);
                }
            }
        }
        return alerts;
    }

    /**
     * 监控贷款资产质量
     */
    private List<Map<String, Object>> monitorLoanQuality(String period) {
        List<Map<String, Object>> alerts = new ArrayList<>();

        // 检查风险成本率
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT org.name as org_name, " +
            "sum(bl.interest_income) as loan_income, " +
            "sum(bl.risk_cost) as risk_cost " +
            "FROM dw_indicator_fact bl " +
            "LEFT JOIN dim_organization org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '" + period + "' AND bl.product_type = 'LOAN' " +
            "GROUP BY org.name"
        );

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
                    alert.put("message", String.format("%s 风险成本率 %.1f%%，资产质量需关注",
                        row.get("org_name"), riskRate));
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
