package com.multiprofit.controller;

import com.multiprofit.ai.ModelApiClient;
import com.multiprofit.dto.AiChatRequest;
import com.multiprofit.dto.ApiResponse;
import com.multiprofit.util.IndicatorFactSql;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI数据探查 - 自然语言转SQL，数据源dw_indicator_fact(EAV透视)
 */
@RestController
@RequestMapping("/api/ai-explore")
public class AiExploreController {

    @Autowired
    private ModelApiClient claudeClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String PERIOD = "2026-06";

    /**
     * AI数据探查 - 自然语言查询
     */
    @PostMapping("/query")
    public ApiResponse<Map<String, Object>> explore(@RequestBody AiChatRequest request) {
        String question = request.getMessage();
        String sql = generateSqlFromQuestion(question);

        List<Map<String, Object>> data = new ArrayList<>();
        String errorMsg = null;
        try {
            data = jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            errorMsg = "SQL执行失败: " + e.getMessage();
        }

        String analysis = generateAnalysis(question, data, errorMsg);
        String chartType = recommendChartType(question);

        Map<String, Object> result = new HashMap<>();
        result.put("answer", analysis);
        result.put("sql", sql);
        result.put("data", data);
        result.put("chartType", chartType);
        result.put("error", errorMsg);

        return ApiResponse.ok(result);
    }

    /**
     * 根据问题生成SQL - 基于EAV透视
     */
    private String generateSqlFromQuestion(String question) {
        String q = question.toLowerCase();

        if (q.contains("排名") || q.contains("最高") || q.contains("最低")) {
            String dim = "ORG", name = "机构";
            if (q.contains("产品")) { dim = "PRODUCT"; name = "产品"; }
            else if (q.contains("条线")) { dim = "BIZ_LINE"; name = "条线"; }
            else if (q.contains("客户经理")) { dim = "MANAGER"; name = "客户经理"; }
            else if (q.contains("部门")) { dim = "DEPT"; name = "部门"; }
            return "SELECT t.dim_name as name, t.net_profit, t.revenue " +
                "FROM (" + IndicatorFactSql.pivot(dim, PERIOD) + ") t " +
                "ORDER BY t.net_profit DESC LIMIT 10";
        }

        if (q.contains("趋势") || q.contains("变化")) {
            // 全行利润趋势
            return IndicatorFactSql.metricTrend(IndicatorFactSql.TOTAL_PROFIT, "TOTAL")
                .replace("as value", "as net_profit");
        }

        if (q.contains("成本")) {
            if (q.contains("结构") || q.contains("占比")) {
                return "SELECT 'FTP成本' as name, t.ftp_cost as value FROM (" + IndicatorFactSql.pivotTotal(PERIOD) + ") t " +
                    "UNION ALL SELECT '风险成本', t.risk_cost FROM (" + IndicatorFactSql.pivotTotal(PERIOD) + ") t " +
                    "UNION ALL SELECT '运营成本', t.op_cost FROM (" + IndicatorFactSql.pivotTotal(PERIOD) + ") t";
            }
            return "SELECT t.dim_name as name, t.ftp_cost, t.risk_cost, t.op_cost " +
                "FROM (" + IndicatorFactSql.pivot("ORG", PERIOD) + ") t " +
                "ORDER BY (t.ftp_cost+t.risk_cost+t.op_cost) DESC";
        }

        if (q.contains("客户")) {
            return "SELECT t.dim_name as name, t.net_profit, t.revenue " +
                "FROM (" + IndicatorFactSql.pivot("CUSTOMER", PERIOD) + ") t " +
                "ORDER BY t.net_profit DESC LIMIT 10";
        }

        // 默认：各机构汇总
        return "SELECT t.dim_name as name, t.revenue, t.net_profit, t.ftp_cost, t.risk_cost, t.op_cost " +
            "FROM (" + IndicatorFactSql.pivot("ORG", PERIOD) + ") t ORDER BY t.net_profit DESC";
    }

    /**
     * 生成分析结论
     */
    private String generateAnalysis(String question, List<Map<String, Object>> data, String errorMsg) {
        if (errorMsg != null) {
            return "查询出错：" + errorMsg;
        }
        if (data.isEmpty()) {
            return "未查询到相关数据。";
        }

        StringBuilder sb = new StringBuilder();
        if (data.get(0).containsKey("net_profit")) {
            Map<String, Object> top = data.get(0);
            sb.append("根据数据分析：\n\n");
            sb.append(String.format("📊 **排名第一：%s**，净利润 %.0f 万元\n",
                top.get("name"), toDouble(top.get("net_profit"))));

            if (data.size() > 1) {
                Map<String, Object> second = data.get(1);
                sb.append(String.format("📊 **排名第二：%s**，净利润 %.0f 万元\n",
                    second.get("name"), toDouble(second.get("net_profit"))));
            }

            double totalProfit = data.stream().mapToDouble(d -> toDouble(d.get("net_profit"))).sum();
            sb.append(String.format("\n💰 **合计净利润：%.0f 万元**\n", totalProfit));

            if (data.size() >= 3) {
                double top3Profit = data.subList(0, 3).stream().mapToDouble(d -> toDouble(d.get("net_profit"))).sum();
                double top3Ratio = totalProfit != 0 ? top3Profit / totalProfit * 100 : 0;
                sb.append(String.format("📈 TOP3占比：%.1f%%\n", top3Ratio));
            }
        }

        sb.append("\n💡 可以继续追问：");
        return sb.toString();
    }

    /**
     * 推荐图表类型
     */
    private String recommendChartType(String question) {
        String q = question.toLowerCase();
        if (q.contains("趋势") || q.contains("变化")) return "line";
        if (q.contains("占比") || q.contains("结构")) return "pie";
        return "bar";
    }

    private double toDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0; }
    }
}
