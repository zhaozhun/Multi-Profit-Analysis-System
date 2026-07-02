package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import com.multiprofit.util.IndicatorFactSql;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 报表中心
 * 数据源：dw_indicator_fact(EAV透视,单维度) + dwd_loan/deposit_detail(明细台账/跨维度)
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String DEFAULT_PERIOD = "2026-06";

    /**
     * DWD贷款+存款明细UNION视图（跨维度查询用）
     * 统一列：biz_id, account_period, product_type, 各维度name, biz_amount, interest_income, ftp_cost, risk_cost, op_cost, net_profit
     */
    private String dwdUnion(String period) {
        String p = IndicatorFactSql.esc(period);
        return "(SELECT biz_id, account_period, 'LOAN' as product_type, org_name, biz_line_name, product_name, " +
            "channel_name, manager_name, customer_name, dept_name, " +
            "loan_balance as biz_amount, loan_monthly_interest as interest_income, ftp_cost, risk_cost, op_cost, " +
            "0 as ftp_income, loan_profit as net_profit FROM dwd_loan_detail WHERE account_period='" + p + "' " +
            "UNION ALL " +
            "SELECT biz_id, account_period, 'DEPOSIT', org_name, biz_line_name, product_name, " +
            "channel_name, manager_name, customer_name, dept_name, " +
            "deposit_balance, deposit_monthly_interest, 0, 0, op_cost, ftp_income, deposit_profit " +
            "FROM dwd_deposit_detail WHERE account_period='" + p + "')";
    }

    /**
     * 明细台账查询 - 查DWD宽表
     */
    @GetMapping("/ledger")
    public ApiResponse<Map<String, Object>> getLedger(
            @RequestParam(required = false, defaultValue = DEFAULT_PERIOD) String period,
            @RequestParam(required = false) String orgName,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "50") int pageSize) {

        String u = dwdUnion(period);
        StringBuilder where = new StringBuilder("WHERE 1=1");
        if (orgName != null && !orgName.isEmpty()) where.append(" AND d.org_name='").append(IndicatorFactSql.esc(orgName)).append("'");
        if (productName != null && !productName.isEmpty()) where.append(" AND d.product_name='").append(IndicatorFactSql.esc(productName)).append("'");
        if (customerName != null && !customerName.isEmpty()) where.append(" AND d.customer_name LIKE '%").append(IndicatorFactSql.esc(customerName)).append("%'");

        int offset = (page - 1) * pageSize;
        String sql = "SELECT d.* FROM " + u + " d " + where +
            " ORDER BY d.net_profit DESC LIMIT " + pageSize + " OFFSET " + offset;

        // 总数
        Integer total = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM " + u + " d " + where, Integer.class);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total : 0);
        result.put("list", rows);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ApiResponse.ok(result);
    }

    /**
     * 机构利润报表 - EAV透视
     */
    @GetMapping("/profit/org")
    public ApiResponse<Map<String, Object>> getOrgProfitReport(
            @RequestParam(required = false, defaultValue = DEFAULT_PERIOD) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {

        String sql = "SELECT t.dim_name as name, t.loan_interest as interest_income, " +
            "t.ftp_cost, t.risk_cost, t.op_cost, t.revenue, t.net_profit, " +
            "CASE WHEN t.revenue>0 THEN round(t.op_cost*100.0/t.revenue,2) ELSE 0 end as cost_income_ratio, " +
            "CASE WHEN t.revenue>0 THEN round(t.net_profit*100.0/t.revenue,2) ELSE 0 end as profit_margin " +
            "FROM (" + IndicatorFactSql.pivot("ORG", period) + ") t ORDER BY t.net_profit DESC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Map<String, Object> total = new HashMap<>();
        total.put("name", "合计");
        total.put("interest_income", rows.stream().mapToDouble(r -> toDouble(r.get("interest_income"))).sum());
        total.put("ftp_cost", rows.stream().mapToDouble(r -> toDouble(r.get("ftp_cost"))).sum());
        total.put("risk_cost", rows.stream().mapToDouble(r -> toDouble(r.get("risk_cost"))).sum());
        total.put("op_cost", rows.stream().mapToDouble(r -> toDouble(r.get("op_cost"))).sum());
        total.put("revenue", rows.stream().mapToDouble(r -> toDouble(r.get("revenue"))).sum());
        total.put("net_profit", rows.stream().mapToDouble(r -> toDouble(r.get("net_profit"))).sum());

        double totalRevenue = toDouble(total.get("revenue"));
        double totalCost = toDouble(total.get("op_cost"));
        total.put("cost_income_ratio", totalRevenue > 0 ? Math.round(totalCost * 10000 / totalRevenue) / 100.0 : 0);
        total.put("profit_margin", totalRevenue > 0 ? Math.round(toDouble(total.get("net_profit")) * 10000 / totalRevenue) / 100.0 : 0);

        Map<String, Object> result = new HashMap<>();
        result.put("list", rows);
        result.put("total", total);
        return ApiResponse.ok(result);
    }

    /**
     * 产品损益报表 - EAV透视
     */
    @GetMapping("/profit/product")
    public ApiResponse<Map<String, Object>> getProductProfitReport(
            @RequestParam(required = false, defaultValue = DEFAULT_PERIOD) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {

        String sql = "SELECT t.dim_name as name, t.revenue, t.ftp_cost, t.risk_cost, t.op_cost, t.net_profit " +
            "FROM (" + IndicatorFactSql.pivot("PRODUCT", period) + ") t ORDER BY t.net_profit DESC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Map<String, Object> result = new HashMap<>();
        result.put("list", rows);
        return ApiResponse.ok(result);
    }

    /**
     * 客户经理绩效报表 - DWD明细(需跨机构维度)
     */
    @GetMapping("/profit/manager")
    public ApiResponse<Map<String, Object>> getManagerProfitReport(
            @RequestParam(required = false, defaultValue = DEFAULT_PERIOD) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {

        String sql = "SELECT d.manager_name as name, d.org_name as org_name, " +
            "count(distinct d.customer_name) as customer_cnt, " +
            "sum(d.interest_income) as revenue, sum(d.net_profit) as net_profit " +
            "FROM " + dwdUnion(period) + " d " +
            "WHERE d.manager_name IS NOT NULL " +
            "GROUP BY d.manager_name, d.org_name ORDER BY net_profit DESC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Map<String, Object> result = new HashMap<>();
        result.put("list", rows);
        return ApiResponse.ok(result);
    }

    /**
     * 自定义查询 - DWD明细(支持多行维度)
     */
    @PostMapping("/custom/query")
    public ApiResponse<Map<String, Object>> customQuery(@RequestBody Map<String, Object> body) {
        List<String> rowDims = (List<String>) body.get("rowDims");
        List<String> colMetrics = (List<String>) body.get("colMetrics");
        String period = (String) body.getOrDefault("period", DEFAULT_PERIOD);

        if (rowDims == null || rowDims.isEmpty() || colMetrics == null || colMetrics.isEmpty()) {
            return ApiResponse.error("请选择行维度和列指标");
        }

        List<String> selectCols = new ArrayList<>();
        List<String> groupByCols = new ArrayList<>();
        for (String dim : rowDims) {
            String nameCol = getDimNameColumn(dim);
            selectCols.add(nameCol + " as " + dim.toLowerCase());
            groupByCols.add(nameCol);
        }
        for (String metric : colMetrics) {
            selectCols.add(getMetricExpression(metric) + " as " + metric.toLowerCase());
        }

        String sql = "SELECT " + String.join(", ", selectCols) +
            " FROM " + dwdUnion(period) + " d WHERE " + String.join(" IS NOT NULL AND ", groupByCols) + " IS NOT NULL" +
            " GROUP BY " + String.join(", ", groupByCols) + " ORDER BY " + groupByCols.get(0);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Map<String, Object> result = new HashMap<>();
        result.put("list", rows);
        result.put("sql", sql);
        return ApiResponse.ok(result);
    }

    /**
     * 获取自定义报表模板
     */
    @GetMapping("/custom/templates")
    public ApiResponse<List<Map<String, Object>>> getCustomTemplates() {
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(
                "SELECT * FROM custom_report_template ORDER BY is_system DESC, create_time DESC");
        } catch (Exception e) {
            rows = Collections.emptyList();
        }
        return ApiResponse.ok(rows);
    }

    /**
     * 生成综合分析报告
     */
    @GetMapping("/analysis")
    public ApiResponse<Map<String, Object>> getAnalysisReport(
            @RequestParam(required = false, defaultValue = DEFAULT_PERIOD) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {

        Map<String, Object> report = new HashMap<>();

        // 1. 整体经营概况 - EAV TOTAL透视
        Map<String, Object> overview = jdbcTemplate.queryForMap(
            "SELECT t.revenue as total_revenue, t.ftp_cost as total_ftp_cost, " +
            "t.risk_cost as total_risk_cost, t.op_cost as total_op_cost, t.net_profit as total_profit, " +
            "t.loan_interest as loan_revenue, t.loan_profit as loan_profit, " +
            "t.ftp_income as deposit_revenue, t.deposit_profit as deposit_profit " +
            "FROM (" + IndicatorFactSql.pivotTotal(period) + ") t");
        report.put("overview", overview);

        // 2. 机构排名
        List<Map<String, Object>> orgRanking = jdbcTemplate.queryForList(
            "SELECT t.dim_name as name, t.net_profit as profit, " +
            "CASE WHEN t.revenue>0 THEN round(t.net_profit*100.0/t.revenue,2) ELSE 0 end as margin " +
            "FROM (" + IndicatorFactSql.pivot("ORG", period) + ") t ORDER BY profit DESC");
        report.put("orgRanking", orgRanking);

        // 3. 产品排名
        List<Map<String, Object>> prodRanking = jdbcTemplate.queryForList(
            "SELECT t.dim_name as name, t.net_profit as profit " +
            "FROM (" + IndicatorFactSql.pivot("PRODUCT", period) + ") t ORDER BY profit DESC");
        report.put("prodRanking", prodRanking);

        // 4. 渠道分析
        List<Map<String, Object>> channelAnalysis = jdbcTemplate.queryForList(
            "SELECT t.dim_name as name, t.revenue, t.net_profit as profit " +
            "FROM (" + IndicatorFactSql.pivot("CHANNEL", period) + ") t ORDER BY profit DESC");
        report.put("channelAnalysis", channelAnalysis);

        // 5. 客户经理绩效 - DWD
        List<Map<String, Object>> managerPerformance = jdbcTemplate.queryForList(
            "SELECT d.manager_name as name, d.org_name as org_name, sum(d.net_profit) as profit " +
            "FROM " + dwdUnion(period) + " d WHERE d.manager_name IS NOT NULL " +
            "GROUP BY d.manager_name, d.org_name ORDER BY profit DESC LIMIT 10");
        report.put("managerPerformance", managerPerformance);

        // 6. 问题与风险识别
        List<Map<String, Object>> issues = new ArrayList<>();
        for (Map<String, Object> org : orgRanking) {
            double profit = toDouble(org.get("profit"));
            if (profit < 0) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "LOSS_ORG");
                issue.put("level", "HIGH");
                issue.put("name", org.get("name"));
                issue.put("description", String.format("机构【%s】当期亏损%.0f万元，需重点关注", org.get("name"), Math.abs(profit)));
                issues.add(issue);
            }
        }
        for (Map<String, Object> org : orgRanking) {
            double margin = toDouble(org.get("margin"));
            if (margin > 0 && margin < 5) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "LOW_MARGIN");
                issue.put("level", "MEDIUM");
                issue.put("name", org.get("name"));
                issue.put("description", String.format("机构【%s】利润率仅%.1f%%，低于行业标准", org.get("name"), margin));
                issues.add(issue);
            }
        }
        for (Map<String, Object> prod : prodRanking) {
            double profit = toDouble(prod.get("profit"));
            if (profit < 0) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "LOSS_PRODUCT");
                issue.put("level", "HIGH");
                issue.put("name", prod.get("name"));
                issue.put("description", String.format("产品【%s】当期亏损%.0f万元，需优化定价或成本", prod.get("name"), Math.abs(profit)));
                issues.add(issue);
            }
        }
        report.put("issues", issues);

        // 7. 建议
        List<String> suggestions = new ArrayList<>();
        suggestions.add("建议关注亏损机构的成本结构，优化运营效率");
        suggestions.add("建议加强高利润产品的推广，提升市场份额");
        suggestions.add("建议定期监控成本收入比，确保盈利水平稳定");
        suggestions.add("建议加强客户经理培训，提升人均产能");
        report.put("suggestions", suggestions);

        return ApiResponse.ok(report);
    }

    /** DWD明细中的维度名称列 */
    private String getDimNameColumn(String dimType) {
        return switch (dimType) {
            case "ORG" -> "d.org_name";
            case "BIZ_LINE" -> "d.biz_line_name";
            case "DEPT" -> "d.dept_name";
            case "PRODUCT" -> "d.product_name";
            case "CHANNEL" -> "d.channel_name";
            case "MANAGER" -> "d.manager_name";
            case "CUSTOMER" -> "d.customer_name";
            default -> "d.org_name";
        };
    }

    /** 自定义查询指标表达式(DWD列) */
    private String getMetricExpression(String metric) {
        return switch (metric) {
            case "REV_TOTAL", "REV_INTEREST" -> "sum(d.interest_income)";
            case "REV_FEE", "REV_NON_INTEREST" -> "0";
            case "COST_FTP" -> "sum(d.ftp_cost)";
            case "COST_RISK" -> "sum(d.risk_cost)";
            case "COST_OP" -> "sum(d.op_cost)";
            case "PROFIT_NET" -> "sum(d.net_profit)";
            case "SCALE_BIZ_AMT" -> "sum(d.biz_amount)";
            case "COST_INCOME_RATIO" -> "CASE WHEN sum(d.interest_income)>0 THEN round(sum(d.op_cost)*100.0/sum(d.interest_income),2) ELSE 0 END";
            case "PROFIT_MARGIN" -> "CASE WHEN sum(d.interest_income)>0 THEN round(sum(d.net_profit)*100.0/sum(d.interest_income),2) ELSE 0 END";
            default -> "sum(d.net_profit)";
        };
    }

    private double toDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0; }
    }
}
