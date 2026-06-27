package com.multiprofit.skill.impl;

import com.multiprofit.ai.FunctionRegistry;
import com.multiprofit.skill.SkillContext;
import com.multiprofit.skill.SkillResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 智能查询技能
 * 处理简单的数据查询请求
 */
@Slf4j
@Component
public class SmartQuerySkill extends SimpleSkill {

    @Autowired
    private FunctionRegistry functionRegistry;

    @Override
    public String getName() {
        return "smart-query";
    }

    @Override
    public String getDescription() {
        return "处理简单的数据查询请求";
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("查询", "查看", "多少", "排名", "对比", "数据");
    }

    @Override
    public SkillResult execute(SkillContext context) {
        log.info("执行智能查询技能");

        String userMessage = context.getUserMessage();
        String period = context.getPeriod();

        try {
            // 分析查询意图
            QueryIntent intent = analyzeQueryIntent(userMessage);

            // 执行查询
            Map<String, Object> result = executeQuery(intent, period);

            // 生成回答
            String answer = generateAnswer(intent, result, period);

            return SkillResult.success(answer, result);

        } catch (Exception e) {
            log.error("查询执行失败", e);
            return SkillResult.failed("查询失败: " + e.getMessage());
        }
    }

    /**
     * 分析查询意图
     */
    private QueryIntent analyzeQueryIntent(String userMessage) {
        QueryIntent intent = new QueryIntent();

        // 分析维度
        if (userMessage.contains("机构") || userMessage.contains("分行")) {
            intent.setDimension("ORG");
        } else if (userMessage.contains("产品")) {
            intent.setDimension("PRODUCT");
        } else if (userMessage.contains("部门")) {
            intent.setDimension("DEPT");
        } else if (userMessage.contains("渠道")) {
            intent.setDimension("CHANNEL");
        } else {
            intent.setDimension("ORG"); // 默认机构维度
        }

        // 分析指标
        if (userMessage.contains("收入")) {
            intent.setMetric("REVENUE");
        } else if (userMessage.contains("成本")) {
            intent.setMetric("COST");
        } else if (userMessage.contains("利润")) {
            intent.setMetric("PROFIT");
        } else {
            intent.setMetric("PROFIT"); // 默认利润
        }

        // 分析查询类型
        if (userMessage.contains("排名")) {
            intent.setQueryType("ranking");
        } else if (userMessage.contains("对比") || userMessage.contains("比较")) {
            intent.setQueryType("compare");
        } else if (userMessage.contains("趋势")) {
            intent.setQueryType("trend");
        } else {
            intent.setQueryType("summary");
        }

        return intent;
    }

    /**
     * 执行查询
     */
    private Map<String, Object> executeQuery(QueryIntent intent, String period) {
        Map<String, Object> params = new HashMap<>();
        params.put("dimensions", Arrays.asList(intent.getDimension()));
        params.put("metrics", Arrays.asList(intent.getMetric()));
        params.put("period", period);

        return (Map<String, Object>) functionRegistry.execute("query_profit_metrics", params);
    }

    /**
     * 生成回答
     */
    private String generateAnswer(QueryIntent intent, Map<String, Object> result, String period) {
        StringBuilder answer = new StringBuilder();

        answer.append("📊 查询结果\n\n");
        answer.append("【查询维度】").append(getDimensionName(intent.getDimension())).append("\n");
        answer.append("【查询指标】").append(getMetricName(intent.getMetric())).append("\n");
        answer.append("【查询期间】").append(period).append("\n\n");

        List<Map<String, Object>> data = extractList(result, "data");
        if (data != null && !data.isEmpty()) {
            answer.append("【数据结果】\n");

            if ("ranking".equals(intent.getQueryType())) {
                // 排名查询
                data.sort((a, b) -> Double.compare(
                    extractDouble(b, intent.getMetric().toLowerCase()),
                    extractDouble(a, intent.getMetric().toLowerCase())
                ));

                for (int i = 0; i < Math.min(5, data.size()); i++) {
                    Map<String, Object> item = data.get(i);
                    String dimValue = extractString(item, intent.getDimension().toLowerCase());
                    double value = extractDouble(item, intent.getMetric().toLowerCase());
                    answer.append(i + 1).append(". ").append(dimValue).append("：").append(formatAmount(value)).append("\n");
                }
            } else {
                // 汇总查询
                double total = 0;
                for (Map<String, Object> item : data) {
                    total += extractDouble(item, intent.getMetric().toLowerCase());
                }
                answer.append("• 总计：").append(formatAmount(total)).append("\n");
                answer.append("• 平均：").append(formatAmount(total / data.size())).append("\n");
                answer.append("• 数据条数：").append(data.size()).append("\n");
            }
        } else {
            answer.append("【数据结果】\n暂无数据\n");
        }

        return answer.toString();
    }

    /**
     * 获取维度名称
     */
    private String getDimensionName(String dimension) {
        switch (dimension) {
            case "ORG": return "机构";
            case "PRODUCT": return "产品";
            case "DEPT": return "部门";
            case "CHANNEL": return "渠道";
            default: return dimension;
        }
    }

    /**
     * 获取指标名称
     */
    private String getMetricName(String metric) {
        switch (metric) {
            case "REVENUE": return "收入";
            case "COST": return "成本";
            case "PROFIT": return "利润";
            default: return metric;
        }
    }

    /**
     * 查询意图内部类
     */
    private static class QueryIntent {
        private String dimension;
        private String metric;
        private String queryType;

        public String getDimension() { return dimension; }
        public void setDimension(String dimension) { this.dimension = dimension; }
        public String getMetric() { return metric; }
        public void setMetric(String metric) { this.metric = metric; }
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
    }
}
