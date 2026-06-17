package com.multiprofit.controller;

import com.multiprofit.ai.ClaudeClient;
import com.multiprofit.dto.AiChatRequest;
import com.multiprofit.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai-explore")
public class AiExploreController {

    @Autowired
    private ClaudeClient claudeClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * AI数据探查 - 自然语言查询
     */
    @PostMapping("/query")
    public ApiResponse<Map<String, Object>> explore(@RequestBody AiChatRequest request) {
        String question = request.getMessage();

        // 1. 根据问题生成SQL
        String sql = generateSqlFromQuestion(question);

        // 2. 执行查询
        List<Map<String, Object>> data = new ArrayList<>();
        String errorMsg = null;
        try {
            data = jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            errorMsg = "SQL执行失败: " + e.getMessage();
        }

        // 3. 生成分析结论
        String analysis = generateAnalysis(question, data, errorMsg);

        // 4. 推荐图表类型
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
     * 根据问题生成SQL
     */
    private String generateSqlFromQuestion(String question) {
        // 简化实现：根据关键词匹配生成SQL
        String q = question.toLowerCase();

        if (q.contains("排名") || q.contains("最高") || q.contains("最低")) {
            if (q.contains("分行") || q.contains("机构")) {
                return "SELECT org_name as name, sum(net_profit) as net_profit, sum(revenue) as revenue " +
                    "FROM biz_ledger WHERE account_period = '2026-05' GROUP BY org_name ORDER BY net_profit DESC LIMIT 10";
            }
            if (q.contains("产品")) {
                return "SELECT product_name as name, sum(net_profit) as net_profit, sum(revenue) as revenue " +
                    "FROM biz_ledger WHERE account_period = '2026-05' GROUP BY product_name ORDER BY net_profit DESC LIMIT 10";
            }
            if (q.contains("条线")) {
                return "SELECT biz_line_name as name, sum(net_profit) as net_profit, sum(revenue) as revenue " +
                    "FROM biz_ledger WHERE account_period = '2026-05' GROUP BY biz_line_name ORDER BY net_profit DESC";
            }
            if (q.contains("客户经理")) {
                return "SELECT manager_name as name, sum(net_profit) as net_profit, count(distinct customer_name) as customer_cnt " +
                    "FROM biz_ledger WHERE account_period = '2026-05' GROUP BY manager_name ORDER BY net_profit DESC LIMIT 10";
            }
            return "SELECT org_name as name, sum(net_profit) as net_profit " +
                "FROM biz_ledger WHERE account_period = '2026-05' GROUP BY org_name ORDER BY net_profit DESC LIMIT 10";
        }

        if (q.contains("趋势") || q.contains("变化")) {
            return "SELECT account_period as period, sum(revenue) as revenue, sum(net_profit) as net_profit " +
                "FROM biz_ledger GROUP BY account_period ORDER BY account_period";
        }

        if (q.contains("成本")) {
            if (q.contains("结构") || q.contains("占比")) {
                return "SELECT 'FTP成本' as name, sum(ftp_cost) as value FROM biz_ledger WHERE account_period = '2026-05' " +
                    "UNION ALL SELECT '风险成本', sum(risk_cost) FROM biz_ledger WHERE account_period = '2026-05' " +
                    "UNION ALL SELECT '运营成本', sum(op_cost) FROM biz_ledger WHERE account_period = '2026-05'";
            }
            return "SELECT org_name as name, sum(ftp_cost) as ftp_cost, sum(risk_cost) as risk_cost, sum(op_cost) as op_cost " +
                "FROM biz_ledger WHERE account_period = '2026-05' GROUP BY org_name ORDER BY sum(ftp_cost+risk_cost+op_cost) DESC";
        }

        if (q.contains("客户")) {
            return "SELECT customer_name as name, sum(net_profit) as net_profit, sum(revenue) as revenue " +
                "FROM biz_ledger WHERE account_period = '2026-05' GROUP BY customer_name ORDER BY net_profit DESC LIMIT 10";
        }

        // 默认：返回各机构汇总
        return "SELECT org_name as name, sum(revenue) as revenue, sum(net_profit) as net_profit, " +
            "sum(ftp_cost) as ftp_cost, sum(risk_cost) as risk_cost, sum(op_cost) as op_cost " +
            "FROM biz_ledger WHERE account_period = '2026-05' GROUP BY org_name ORDER BY net_profit DESC";
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

        // 根据数据生成分析
        if (data.get(0).containsKey("net_profit")) {
            Map<String, Object> top = data.get(0);
            sb.append(String.format("根据数据分析：\n\n"));
            sb.append(String.format("📊 **排名第一：%s**，净利润 %.0f 万元\n",
                top.get("name"), toDouble(top.get("net_profit"))));

            if (data.size() > 1) {
                Map<String, Object> second = data.get(1);
                sb.append(String.format("📊 **排名第二：%s**，净利润 %.0f 万元\n",
                    second.get("name"), toDouble(second.get("net_profit"))));
            }

            // 计算合计
            double totalProfit = data.stream().mapToDouble(d -> toDouble(d.get("net_profit"))).sum();
            sb.append(String.format("\n💰 **合计净利润：%.0f 万元**\n", totalProfit));

            // 计算TOP3占比
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
