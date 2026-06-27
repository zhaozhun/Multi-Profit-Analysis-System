package com.multiprofit.skill.impl;

import com.multiprofit.ai.FunctionRegistry;
import com.multiprofit.skill.Skill;
import com.multiprofit.skill.SkillContext;
import com.multiprofit.skill.SkillResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 利润波动归因技能
 * 分析利润变化的驱动因素，找出关键影响维度
 */
@Slf4j
@Component
public class ProfitVarianceSkill implements Skill {

    @Autowired
    private FunctionRegistry functionRegistry;

    @Override
    public String getName() {
        return "profit-variance";
    }

    @Override
    public String getDescription() {
        return "分析利润变化的驱动因素，找出关键影响维度";
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("利润变化", "波动分析", "利润归因", "为什么利润", "利润下降", "利润上升");
    }

    @Override
    public boolean canHandle(String userMessage) {
        if (userMessage == null) return false;
        String message = userMessage.toLowerCase();
        return getTriggers().stream()
            .anyMatch(trigger -> message.contains(trigger));
    }

    @Override
    public SkillResult execute(SkillContext context) {
        log.info("执行利润波动归因技能");

        String period = context.getPeriod();
        String basePeriod = context.getStringParam("base_period");

        // 如果没有指定基期，默认使用上月
        if (basePeriod == null) {
            basePeriod = calculateBasePeriod(period, "MOM");
        }

        try {
            // Step 1: 获取当期和基期整体数据
            Map<String, Object> currentData = queryProfitMetrics(
                Arrays.asList("ALL"),
                Arrays.asList("REVENUE", "COST", "PROFIT"),
                period
            );

            Map<String, Object> baseData = queryProfitMetrics(
                Arrays.asList("ALL"),
                Arrays.asList("REVENUE", "COST", "PROFIT"),
                basePeriod
            );

            // Step 2: 按机构维度计算贡献度
            Map<String, Object> orgContribution = calculateContribution(
                "PROFIT", "ORG", basePeriod, period
            );

            // Step 3: 按产品维度计算贡献度
            Map<String, Object> productContribution = calculateContribution(
                "PROFIT", "PRODUCT", basePeriod, period
            );

            // Step 4: 生成分析报告
            String report = generateReport(currentData, baseData, orgContribution, productContribution, period, basePeriod);

            // Step 5: 生成图表配置
            Map<String, Object> chartConfig = generateChartConfig(orgContribution);

            return SkillResult.successWithChart(report, Map.of(
                "current", currentData,
                "base", baseData,
                "orgContribution", orgContribution,
                "productContribution", productContribution
            ), "waterfall", chartConfig);

        } catch (Exception e) {
            log.error("利润波动归因执行失败", e);
            return SkillResult.failed("分析失败: " + e.getMessage());
        }
    }

    /**
     * 查询盈利指标
     */
    private Map<String, Object> queryProfitMetrics(List<String> dimensions, List<String> metrics, String period) {
        Map<String, Object> params = new HashMap<>();
        params.put("dimensions", dimensions);
        params.put("metrics", metrics);
        params.put("period", period);

        return (Map<String, Object>) functionRegistry.execute("query_profit_metrics", params);
    }

    /**
     * 计算贡献度
     */
    private Map<String, Object> calculateContribution(String metric, String dimension, String basePeriod, String currentPeriod) {
        Map<String, Object> params = new HashMap<>();
        params.put("target_metric", metric);
        params.put("dimension", dimension);
        params.put("base_period", basePeriod);
        params.put("current_period", currentPeriod);

        return (Map<String, Object>) functionRegistry.execute("calculate_contribution", params);
    }

    /**
     * 生成分析报告
     */
    private String generateReport(Map<String, Object> currentData, Map<String, Object> baseData,
                                   Map<String, Object> orgContribution, Map<String, Object> productContribution,
                                   String period, String basePeriod) {
        StringBuilder report = new StringBuilder();

        report.append("📊 利润波动归因分析\n\n");

        // 整体情况
        report.append("【整体情况】\n");
        report.append("• 分析期间：").append(basePeriod).append(" → ").append(period).append("\n");

        // 提取数据
        double currentProfit = extractDouble(currentData, "profit");
        double baseProfit = extractDouble(baseData, "profit");
        double profitChange = currentProfit - baseProfit;
        double profitChangeRate = baseProfit != 0 ? (profitChange / baseProfit) * 100 : 0;

        report.append("• 当期利润：").append(formatAmount(currentProfit)).append("\n");
        report.append("• 基期利润：").append(formatAmount(baseProfit)).append("\n");
        report.append("• 利润变化：").append(formatAmount(profitChange));
        report.append("（").append(String.format("%.2f", profitChangeRate)).append("%）\n\n");

        // 机构维度分析
        report.append("【机构维度分析】\n");
        List<Map<String, Object>> orgContributions = extractList(orgContribution, "contributions");
        if (orgContributions != null && !orgContributions.isEmpty()) {
            for (int i = 0; i < Math.min(3, orgContributions.size()); i++) {
                Map<String, Object> item = orgContributions.get(i);
                String org = extractString(item, "dimension");
                double contribution = extractDouble(item, "contribution");
                double change = extractDouble(item, "change");

                report.append("• ").append(org).append("：贡献度 ").append(String.format("%.1f", contribution));
                report.append("%，变化 ").append(formatAmount(change)).append("\n");
            }
        }

        // 产品维度分析
        report.append("\n【产品维度分析】\n");
        List<Map<String, Object>> productContributions = extractList(productContribution, "contributions");
        if (productContributions != null && !productContributions.isEmpty()) {
            for (int i = 0; i < Math.min(3, productContributions.size()); i++) {
                Map<String, Object> item = productContributions.get(i);
                String product = extractString(item, "dimension");
                double contribution = extractDouble(item, "contribution");
                double change = extractDouble(item, "change");

                report.append("• ").append(product).append("：贡献度 ").append(String.format("%.1f", contribution));
                report.append("%，变化 ").append(formatAmount(change)).append("\n");
            }
        }

        // 结论
        report.append("\n【结论】\n");
        if (profitChange > 0) {
            report.append("• 利润增长主要来自");
        } else {
            report.append("• 利润下降主要受");
        }

        if (orgContributions != null && !orgContributions.isEmpty()) {
            report.append(extractString(orgContributions.get(0), "dimension"));
        }
        report.append("影响\n");

        return report.toString();
    }

    /**
     * 生成图表配置
     */
    private Map<String, Object> generateChartConfig(Map<String, Object> contribution) {
        Map<String, Object> config = new HashMap<>();

        List<Map<String, Object>> contributions = extractList(contribution, "contributions");
        if (contributions != null) {
            List<String> categories = new ArrayList<>();
            List<Double> values = new ArrayList<>();

            for (Map<String, Object> item : contributions) {
                categories.add(extractString(item, "dimension"));
                values.add(extractDouble(item, "change"));
            }

            config.put("categories", categories);
            config.put("values", values);
        }

        return config;
    }

    /**
     * 计算基期
     */
    private String calculateBasePeriod(String currentPeriod, String compareType) {
        String[] parts = currentPeriod.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        if ("YOY".equalsIgnoreCase(compareType)) {
            return (year - 1) + "-" + String.format("%02d", month);
        } else {
            if (month == 1) {
                return (year - 1) + "-12";
            } else {
                return year + "-" + String.format("%02d", month - 1);
            }
        }
    }

    /**
     * 提取double值
     */
    private double extractDouble(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0;
        Object value = map.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 提取String值
     */
    private String extractString(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return "";
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * 提取List值
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractList(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return Collections.emptyList();
        Object value = map.get(key);
        if (value instanceof List) return (List<Map<String, Object>>) value;
        return Collections.emptyList();
    }

    /**
     * 格式化金额
     */
    private String formatAmount(double amount) {
        if (Math.abs(amount) >= 100000000) {
            return String.format("%.2f亿", amount / 100000000);
        } else if (Math.abs(amount) >= 10000) {
            return String.format("%.2f万", amount / 10000);
        } else {
            return String.format("%.2f", amount);
        }
    }
}
