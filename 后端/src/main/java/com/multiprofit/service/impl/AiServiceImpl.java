package com.multiprofit.service.impl;

import com.multiprofit.ai.ModelApiClient;
import com.multiprofit.dto.AiChatRequest;
import com.multiprofit.dto.AiChatResponse;
import com.multiprofit.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private ModelApiClient claudeClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        // 构建上下文
        String context = buildContext(request.getContext());

        // 让AI分析并生成SQL（如果是数据查询类问题）
        String sqlPrompt = buildSqlPrompt(request.getMessage(), context);
        String sqlResult = claudeClient.chat(sqlPrompt);

        AiChatResponse response = new AiChatResponse();

        // 如果AI生成了SQL，尝试执行
        if (sqlResult.contains("SELECT")) {
            try {
                String sql = extractSql(sqlResult);
                List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
                response.setData(data);

                // 让AI解读查询结果
                String interpretPrompt = String.format(
                    "用户问题：%s\n查询结果数据：%s\n请用简洁的中文解读这些数据，给出经营分析结论。",
                    request.getMessage(), data.toString()
                );
                response.setAnswer(claudeClient.chat(interpretPrompt));
            } catch (Exception e) {
                // SQL执行失败，直接让AI回答
                response.setAnswer(claudeClient.chat(
                    buildContextPrompt(context) + "\n用户问题：" + request.getMessage()
                ));
            }
        } else {
            // 非数据查询类问题，直接回答
            response.setAnswer(sqlResult);
        }

        return response;
    }

    @Override
    public String analyzeAnomaly(String metricName, String currentValue,
                                  String previousValue, double changeRate, String context) {
        String prompt = String.format(
            "经营数据异常分析：\n" +
            "指标名称：%s\n" +
            "当期值：%s\n" +
            "上期值：%s\n" +
            "环比变化：%.2f%%\n" +
            "背景信息：%s\n\n" +
            "请从银行经营角度分析该异常的可能原因（3-5条），并给出处理建议。" +
            "分析要专业、具体、可操作。",
            metricName, currentValue, previousValue, changeRate, context
        );
        return claudeClient.chat(prompt);
    }

    @Override
    public String generateBusinessBrief(String period, String scope) {
        // 从ClickHouse获取当期核心数据
        String dataSql = String.format(
            "SELECT " +
            "  sum(revenue) as total_revenue, " +
            "  sum(ftp_cost) as total_ftp, " +
            "  sum(risk_cost) as total_risk, " +
            "  sum(op_cost) as total_op, " +
            "  sum(net_profit) as total_profit " +
            "FROM biz_ledger WHERE account_period = '%s'",
            period
        );

        try {
            Map<String, Object> data = jdbcTemplate.queryForMap(dataSql);

            String prompt = String.format(
                "请基于以下数据生成一份简洁的月度经营分析简报（500字左右）：\n\n" +
                "统计期间：%s\n" +
                "业务总收入：%s\n" +
                "FTP资金成本：%s\n" +
                "风险成本：%s\n" +
                "运营成本：%s\n" +
                "净利润：%s\n\n" +
                "要求：\n" +
                "1. 概述整体经营情况\n" +
                "2. 分析利润构成和变化\n" +
                "3. 指出需要关注的问题\n" +
                "4. 给出2-3条经营建议\n" +
                "用专业但易懂的中文撰写。",
                period,
                data.get("total_revenue"), data.get("total_ftp"),
                data.get("total_risk"), data.get("total_op"),
                data.get("total_profit")
            );

            return claudeClient.chat(prompt);
        } catch (Exception e) {
            return "生成经营简报失败：" + e.getMessage();
        }
    }

    @Override
    public String naturalLanguageToSql(String question) {
        String schemaInfo = getClickHouseSchema();
        String prompt = String.format(
            "你是一个SQL专家。请根据以下数据库表结构，将用户的自然语言问题转换为ClickHouse SQL查询。\n\n" +
            "数据库表结构：\n%s\n\n" +
            "规则：\n" +
            "1. 只返回SQL语句，不要其他内容\n" +
            "2. 使用ClickHouse语法\n" +
            "3. 表名：biz_ledger\n" +
            "4. 时间字段用account_period（格式yyyy-MM）\n\n" +
            "用户问题：%s",
            schemaInfo, question
        );
        return claudeClient.chat(prompt);
    }

    @Override
    public String interpretData(String metricName, Map<String, Object> data) {
        String prompt = String.format(
            "请解读以下银行经营数据：\n指标：%s\n数据：%s\n\n" +
            "用简洁的中文说明数据含义、是否正常、需要注意什么。",
            metricName, data.toString()
        );
        return claudeClient.chat(prompt);
    }

    private String buildContext(String context) {
        if (context == null || context.isEmpty()) {
            return "当前查看全行数据，最新账期数据。";
        }
        return context;
    }

    private String buildSqlPrompt(String question, String context) {
        String schema = getClickHouseSchema();
        return String.format(
            "你是ClickHouse SQL专家。根据表结构将问题转为SQL。\n" +
            "表结构：\n%s\n上下文：%s\n\n" +
            "规则：只返回SQL，表名biz_ledger，时间字段account_period(yyyy-MM)。\n" +
            "问题：%s",
            schema, context, question
        );
    }

    private String buildContextPrompt(String context) {
        return "你是银行经营分析专家。" + context + "请回答以下问题：";
    }

    private String extractSql(String aiResponse) {
        // 提取SQL语句（去除markdown代码块标记）
        String sql = aiResponse.trim();
        if (sql.contains("```sql")) {
            sql = sql.substring(sql.indexOf("```sql") + 6);
            sql = sql.substring(0, sql.indexOf("```"));
        } else if (sql.contains("```")) {
            sql = sql.substring(sql.indexOf("```") + 3);
            sql = sql.substring(0, sql.indexOf("```"));
        }
        return sql.trim();
    }

    private String getClickHouseSchema() {
        return "biz_ledger表字段：\n" +
               "- biz_id: 业务编号\n" +
               "- account_period: 账期(yyyy-MM)\n" +
               "- org_code/org_name: 机构编码/名称\n" +
               "- product_code/product_name: 产品编码/名称\n" +
               "- biz_line_code/biz_line_name: 条线编码/名称\n" +
               "- dept_code/dept_name: 部门编码/名称\n" +
               "- channel_code/channel_name: 渠道编码/名称\n" +
               "- manager_code/manager_name: 客户经理编码/名称\n" +
               "- revenue: 业务收入\n" +
               "- ftp_cost: FTP资金成本\n" +
               "- risk_cost: 风险成本\n" +
               "- op_cost: 运营成本\n" +
               "- net_profit: 净利润\n" +
               "- biz_amount: 业务金额\n" +
               "- caliber_type: 口径(BOOK/ASSESS)";
    }
}
