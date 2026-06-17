package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 明细台账查询 - 使用 JOIN 获取维度名称
     */
    @GetMapping("/ledger")
    public ApiResponse<Map<String, Object>> getLedger(
            @RequestParam(required = false, defaultValue = "2026-05") String period,
            @RequestParam(required = false) String orgName,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "50") int pageSize) {

        StringBuilder sql = new StringBuilder(
            "SELECT bl.biz_id, bl.stat_date, bl.account_period, " +
            "org.name as org_name, prod.name as product_name, biz_line.name as biz_line_name, " +
            "ch.name as channel_name, mgr.name as manager_name, " +
            "bl.biz_amount, bl.revenue, bl.interest_income, " +
            "bl.fee_income, bl.ftp_cost, bl.risk_cost, bl.op_cost, bl.net_profit " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master org ON bl.org_id = org.id " +
            "LEFT JOIN dimension_master prod ON bl.product_id = prod.id " +
            "LEFT JOIN dimension_master biz_line ON bl.biz_line_id = biz_line.id " +
            "LEFT JOIN dimension_master ch ON bl.channel_id = ch.id " +
            "LEFT JOIN dimension_master mgr ON bl.manager_id = mgr.id " +
            "WHERE bl.account_period = '" + period + "'"
        );

        if (orgName != null && !orgName.isEmpty()) {
            sql.append(" AND org.name = '").append(orgName).append("'");
        }
        if (productName != null && !productName.isEmpty()) {
            sql.append(" AND prod.name = '").append(productName).append("'");
        }
        if (customerName != null && !customerName.isEmpty()) {
            sql.append(" AND mgr.name LIKE '%").append(customerName).append("%'");
        }

        sql.append(" ORDER BY bl.net_profit DESC");

        // 总数
        String countSql = "SELECT count(*) FROM biz_ledger WHERE account_period = '" + period + "'";
        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString());

        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total : 0);
        result.put("list", rows);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ApiResponse.ok(result);
    }

    /**
     * 机构利润报表 - 使用 JOIN 获取维度名称
     */
    @GetMapping("/profit/org")
    public ApiResponse<Map<String, Object>> getOrgProfitReport(
            @RequestParam(required = false, defaultValue = "2026-05") String period,
            @RequestParam(required = false, defaultValue = "BOOK") String caliberType) {

        String sql = String.format(
            "SELECT org.name as name, " +
            "sum(bl.revenue) as revenue, sum(bl.interest_income) as interest_income, " +
            "sum(bl.fee_income) as fee_income, sum(bl.non_interest_income) as non_interest_income, " +
            "sum(bl.ftp_cost) as ftp_cost, sum(bl.risk_cost) as risk_cost, sum(bl.op_cost) as op_cost, " +
            "sum(bl.net_profit) as net_profit, " +
            "CASE WHEN sum(bl.revenue) > 0 THEN round(sum(bl.ftp_cost+bl.risk_cost+bl.op_cost)*100.0/sum(bl.revenue),2) ELSE 0 end as cost_income_ratio, " +
            "CASE WHEN sum(bl.revenue) > 0 THEN round(sum(bl.net_profit)*100.0/sum(bl.revenue),2) ELSE 0 end as profit_margin " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY org.name ORDER BY net_profit DESC",
            period, caliberType
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        // 计算合计
        Map<String, Object> total = new HashMap<>();
        total.put("name", "合计");
        total.put("revenue", rows.stream().mapToDouble(r -> toDouble(r.get("revenue"))).sum());
        total.put("interest_income", rows.stream().mapToDouble(r -> toDouble(r.get("interest_income"))).sum());
        total.put("fee_income", rows.stream().mapToDouble(r -> toDouble(r.get("fee_income"))).sum());
        total.put("ftp_cost", rows.stream().mapToDouble(r -> toDouble(r.get("ftp_cost"))).sum());
        total.put("risk_cost", rows.stream().mapToDouble(r -> toDouble(r.get("risk_cost"))).sum());
        total.put("op_cost", rows.stream().mapToDouble(r -> toDouble(r.get("op_cost"))).sum());
        total.put("net_profit", rows.stream().mapToDouble(r -> toDouble(r.get("net_profit"))).sum());

        double totalRevenue = toDouble(total.get("revenue"));
        double totalCost = toDouble(total.get("ftp_cost")) + toDouble(total.get("risk_cost")) + toDouble(total.get("op_cost"));
        total.put("cost_income_ratio", totalRevenue > 0 ? Math.round(totalCost * 10000 / totalRevenue) / 100.0 : 0);
        total.put("profit_margin", totalRevenue > 0 ? Math.round(toDouble(total.get("net_profit")) * 10000 / totalRevenue) / 100.0 : 0);

        Map<String, Object> result = new HashMap<>();
        result.put("list", rows);
        result.put("total", total);

        return ApiResponse.ok(result);
    }

    /**
     * 产品损益报表 - 使用 JOIN 获取维度名称
     */
    @GetMapping("/profit/product")
    public ApiResponse<Map<String, Object>> getProductProfitReport(
            @RequestParam(required = false, defaultValue = "2026-05") String period,
            @RequestParam(required = false, defaultValue = "BOOK") String caliberType) {

        String sql = String.format(
            "SELECT prod.name as name, " +
            "sum(bl.biz_amount) as biz_amount, sum(bl.revenue) as revenue, " +
            "sum(bl.ftp_cost) as ftp_cost, sum(bl.risk_cost) as risk_cost, sum(bl.op_cost) as op_cost, " +
            "sum(bl.net_profit) as net_profit " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master prod ON bl.product_id = prod.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY prod.name ORDER BY net_profit DESC",
            period, caliberType
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Map<String, Object> result = new HashMap<>();
        result.put("list", rows);
        return ApiResponse.ok(result);
    }

    /**
     * 客户经理绩效报表 - 使用 JOIN 获取维度名称
     */
    @GetMapping("/profit/manager")
    public ApiResponse<Map<String, Object>> getManagerProfitReport(
            @RequestParam(required = false, defaultValue = "2026-05") String period,
            @RequestParam(required = false, defaultValue = "BOOK") String caliberType) {

        String sql = String.format(
            "SELECT mgr.name as name, org.name as org_name, " +
            "count(distinct bl.customer_id) as customer_cnt, " +
            "sum(bl.revenue) as revenue, sum(bl.net_profit) as net_profit " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master mgr ON bl.manager_id = mgr.id " +
            "LEFT JOIN dimension_master org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY mgr.name, org.name ORDER BY net_profit DESC",
            period, caliberType
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Map<String, Object> result = new HashMap<>();
        result.put("list", rows);
        return ApiResponse.ok(result);
    }

    /**
     * 自定义查询 - 使用 JOIN 获取维度名称
     */
    @PostMapping("/custom/query")
    public ApiResponse<Map<String, Object>> customQuery(@RequestBody Map<String, Object> body) {
        List<String> rowDims = (List<String>) body.get("rowDims");
        List<String> colMetrics = (List<String>) body.get("colMetrics");
        String period = (String) body.getOrDefault("period", "2026-05");
        String caliberType = (String) body.getOrDefault("caliberType", "BOOK");

        if (rowDims == null || rowDims.isEmpty() || colMetrics == null || colMetrics.isEmpty()) {
            return ApiResponse.error("请选择行维度和列指标");
        }

        // 构建JOIN子句
        StringBuilder joinClause = new StringBuilder();
        List<String> selectCols = new ArrayList<>();
        List<String> groupByCols = new ArrayList<>();

        for (String dim : rowDims) {
            String joinAlias = dim.toLowerCase() + "_dm";
            String idCol = getDimIdColumn(dim);
            String joinSql = String.format("LEFT JOIN dimension_master %s ON bl.%s = %s.id", joinAlias, idCol, joinAlias);
            joinClause.append(" ").append(joinSql);
            selectCols.add(joinAlias + ".name as " + dim.toLowerCase());
            groupByCols.add(joinAlias + ".name");
        }

        // 构建指标列
        for (String metric : colMetrics) {
            String aggExpr = getMetricExpression(metric);
            selectCols.add(aggExpr + " as " + metric.toLowerCase());
        }

        String sql = String.format(
            "SELECT %s FROM biz_ledger bl %s WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' GROUP BY %s ORDER BY %s",
            String.join(", ", selectCols), joinClause.toString(), period, caliberType,
            String.join(", ", groupByCols), groupByCols.get(0)
        );

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
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM custom_report_template ORDER BY is_system DESC, create_time DESC"
        );
        return ApiResponse.ok(rows);
    }

    /**
     * 生成综合分析报告
     */
    @GetMapping("/analysis")
    public ApiResponse<Map<String, Object>> getAnalysisReport(
            @RequestParam(required = false, defaultValue = "2026-05") String period,
            @RequestParam(required = false, defaultValue = "BOOK") String caliberType) {

        Map<String, Object> report = new HashMap<>();

        // 1. 整体经营概况
        String overviewSql = String.format(
            "SELECT " +
            "sum(revenue) as total_revenue, sum(ftp_cost) as total_ftp_cost, " +
            "sum(risk_cost) as total_risk_cost, sum(op_cost) as total_op_cost, " +
            "sum(net_profit) as total_profit, " +
            "sum(loan_revenue) as loan_revenue, sum(loan_profit) as loan_profit, " +
            "sum(deposit_revenue) as deposit_revenue, sum(deposit_profit) as deposit_profit " +
            "FROM biz_ledger WHERE account_period = '%s' AND caliber_type = '%s'",
            period, caliberType
        );
        Map<String, Object> overview = jdbcTemplate.queryForMap(overviewSql);
        report.put("overview", overview);

        // 2. 机构排名
        String orgRankSql = String.format(
            "SELECT org.name, sum(bl.net_profit) as profit, " +
            "CASE WHEN sum(bl.revenue) > 0 THEN round(sum(bl.net_profit)*100.0/sum(bl.revenue),2) ELSE 0 end as margin " +
            "FROM biz_ledger bl LEFT JOIN dimension_master org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY org.name ORDER BY profit DESC",
            period, caliberType
        );
        List<Map<String, Object>> orgRanking = jdbcTemplate.queryForList(orgRankSql);
        report.put("orgRanking", orgRanking);

        // 3. 产品排名
        String prodRankSql = String.format(
            "SELECT prod.name, sum(bl.biz_amount) as amount, sum(bl.net_profit) as profit " +
            "FROM biz_ledger bl LEFT JOIN dimension_master prod ON bl.product_id = prod.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY prod.name ORDER BY profit DESC",
            period, caliberType
        );
        List<Map<String, Object>> prodRanking = jdbcTemplate.queryForList(prodRankSql);
        report.put("prodRanking", prodRanking);

        // 4. 渠道分析
        String channelSql = String.format(
            "SELECT ch.name, sum(bl.revenue) as revenue, sum(bl.net_profit) as profit " +
            "FROM biz_ledger bl LEFT JOIN dimension_master ch ON bl.channel_id = ch.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY ch.name ORDER BY profit DESC",
            period, caliberType
        );
        List<Map<String, Object>> channelAnalysis = jdbcTemplate.queryForList(channelSql);
        report.put("channelAnalysis", channelAnalysis);

        // 5. 客户经理绩效
        String managerSql = String.format(
            "SELECT mgr.name, org.name as org_name, sum(bl.net_profit) as profit " +
            "FROM biz_ledger bl " +
            "LEFT JOIN dimension_master mgr ON bl.manager_id = mgr.id " +
            "LEFT JOIN dimension_master org ON bl.org_id = org.id " +
            "WHERE bl.account_period = '%s' AND bl.caliber_type = '%s' " +
            "GROUP BY mgr.name, org.name ORDER BY profit DESC LIMIT 10",
            period, caliberType
        );
        List<Map<String, Object>> managerPerformance = jdbcTemplate.queryForList(managerSql);
        report.put("managerPerformance", managerPerformance);

        // 6. 问题与风险识别
        List<Map<String, Object>> issues = new ArrayList<>();

        // 识别亏损机构
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

        // 识别利润率异常低的机构
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

        // 识别亏损产品
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

        // 7. 建议与注意事项
        List<String> suggestions = new ArrayList<>();
        suggestions.add("建议关注亏损机构的成本结构，优化运营效率");
        suggestions.add("建议加强高利润产品的推广，提升市场份额");
        suggestions.add("建议定期监控成本收入比，确保盈利水平稳定");
        suggestions.add("建议加强客户经理培训，提升人均产能");
        report.put("suggestions", suggestions);

        return ApiResponse.ok(report);
    }

    private String getDimIdColumn(String dimType) {
        return switch (dimType) {
            case "ORG" -> "org_id";
            case "BIZ_LINE" -> "biz_line_id";
            case "DEPT" -> "dept_id";
            case "PRODUCT" -> "product_id";
            case "CHANNEL" -> "channel_id";
            case "MANAGER" -> "manager_id";
            case "CUSTOMER" -> "customer_id";
            default -> "org_id";
        };
    }

    private String getMetricExpression(String metric) {
        return switch (metric) {
            case "REV_TOTAL" -> "sum(bl.revenue)";
            case "REV_INTEREST" -> "sum(bl.interest_income)";
            case "REV_FEE" -> "sum(bl.fee_income)";
            case "REV_NON_INTEREST" -> "sum(bl.non_interest_income)";
            case "COST_FTP" -> "sum(bl.ftp_cost)";
            case "COST_RISK" -> "sum(bl.risk_cost)";
            case "COST_OP" -> "sum(bl.op_cost)";
            case "PROFIT_NET" -> "sum(bl.net_profit)";
            case "SCALE_BIZ_AMT" -> "sum(bl.biz_amount)";
            case "COST_INCOME_RATIO" -> "CASE WHEN sum(bl.revenue)>0 THEN round(sum(bl.ftp_cost+bl.risk_cost+bl.op_cost)*100.0/sum(bl.revenue),2) ELSE 0 END";
            case "PROFIT_MARGIN" -> "CASE WHEN sum(bl.revenue)>0 THEN round(sum(bl.net_profit)*100.0/sum(bl.revenue),2) ELSE 0 END";
            default -> "sum(bl.net_profit)";
        };
    }

    private double toDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0; }
    }
}
