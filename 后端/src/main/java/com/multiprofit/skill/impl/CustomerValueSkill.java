package com.multiprofit.skill.impl;

import com.multiprofit.ai.FunctionRegistry;
import com.multiprofit.skill.SkillContext;
import com.multiprofit.skill.SkillResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 客户价值分析技能
 * 分析客户价值分布，找出高价值客户
 */
@Slf4j
@Component
public class CustomerValueSkill extends SimpleSkill {

    @Autowired
    private FunctionRegistry functionRegistry;

    @Override
    public String getName() {
        return "customer-value";
    }

    @Override
    public String getDescription() {
        return "分析客户价值分布，找出高价值客户";
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("客户价值", "客户分层", "客户分析", "高价值客户", "客户贡献");
    }

    @Override
    public SkillResult execute(SkillContext context) {
        log.info("执行客户价值分析技能");

        String period = context.getPeriod();

        try {
            // 查询客户维度数据
            Map<String, Object> params = new HashMap<>();
            params.put("dimensions", Arrays.asList("CUSTOMER"));
            params.put("metrics", Arrays.asList("REVENUE", "COST", "PROFIT"));
            params.put("period", period);

            Map<String, Object> result = (Map<String, Object>) functionRegistry.execute(
                "query_profit_metrics", params);

            // 分析客户价值
            List<Map<String, Object>> customerData = extractList(result, "data");
            Map<String, Object> analysis = analyzeCustomerValue(customerData);

            // 生成报告
            String report = generateReport(analysis, period);

            return SkillResult.success(report, analysis);

        } catch (Exception e) {
            log.error("客户价值分析失败", e);
            return SkillResult.failed("分析失败: " + e.getMessage());
        }
    }

    /**
     * 分析客户价值
     */
    private Map<String, Object> analyzeCustomerValue(List<Map<String, Object>> customerData) {
        Map<String, Object> analysis = new HashMap<>();

        if (customerData == null || customerData.isEmpty()) {
            return analysis;
        }

        // 按利润排序
        customerData.sort((a, b) -> Double.compare(
            extractDouble(b, "profit"),
            extractDouble(a, "profit")
        ));

        // 计算总利润
        double totalProfit = customerData.stream()
            .mapToDouble(d -> extractDouble(d, "profit"))
            .sum();

        // 客户分层
        List<Map<String, Object>> highValue = new ArrayList<>();
        List<Map<String, Object>> mediumValue = new ArrayList<>();
        List<Map<String, Object>> lowValue = new ArrayList<>();

        double cumulativeProfit = 0;
        for (Map<String, Object> customer : customerData) {
            double profit = extractDouble(customer, "profit");
            cumulativeProfit += profit;
            double cumulativeRatio = totalProfit != 0 ? cumulativeProfit / totalProfit : 0;

            Map<String, Object> customerWithRatio = new HashMap<>(customer);
            customerWithRatio.put("cumulativeRatio", cumulativeRatio);

            if (cumulativeRatio <= 0.8) {
                highValue.add(customerWithRatio);
            } else if (cumulativeRatio <= 0.95) {
                mediumValue.add(customerWithRatio);
            } else {
                lowValue.add(customerWithRatio);
            }
        }

        analysis.put("totalCustomers", customerData.size());
        analysis.put("totalProfit", totalProfit);
        analysis.put("highValue", highValue);
        analysis.put("mediumValue", mediumValue);
        analysis.put("lowValue", lowValue);
        analysis.put("highValueCount", highValue.size());
        analysis.put("mediumValueCount", mediumValue.size());
        analysis.put("lowValueCount", lowValue.size());

        return analysis;
    }

    /**
     * 生成报告
     */
    @SuppressWarnings("unchecked")
    private String generateReport(Map<String, Object> analysis, String period) {
        StringBuilder report = new StringBuilder();

        report.append("👥 客户价值分析报告\n\n");
        report.append("【分析期间】").append(period).append("\n\n");

        int totalCustomers = extractInt(analysis, "totalCustomers");
        double totalProfit = extractDouble(analysis, "totalProfit");

        report.append("【整体情况】\n");
        report.append("• 客户总数：").append(totalCustomers).append("\n");
        report.append("• 总利润：").append(formatAmount(totalProfit)).append("\n\n");

        // 高价值客户
        List<Map<String, Object>> highValue = (List<Map<String, Object>>) analysis.get("highValue");
        if (highValue != null && !highValue.isEmpty()) {
            double highValueProfit = highValue.stream()
                .mapToDouble(d -> extractDouble(d, "profit"))
                .sum();

            report.append("【高价值客户】TOP ").append(highValue.size()).append("（贡献80%利润）\n");
            report.append("• 利润贡献：").append(formatAmount(highValueProfit)).append("\n");
            report.append("• 占比：").append(String.format("%.1f", totalProfit != 0 ? highValueProfit / totalProfit * 100 : 0)).append("%\n");

            for (int i = 0; i < Math.min(3, highValue.size()); i++) {
                Map<String, Object> customer = highValue.get(i);
                String customerName = extractString(customer, "customer");
                double profit = extractDouble(customer, "profit");
                report.append("• ").append(customerName).append("：").append(formatAmount(profit)).append("\n");
            }
            report.append("\n");
        }

        // 中价值客户
        List<Map<String, Object>> mediumValue = (List<Map<String, Object>>) analysis.get("mediumValue");
        if (mediumValue != null && !mediumValue.isEmpty()) {
            double mediumValueProfit = mediumValue.stream()
                .mapToDouble(d -> extractDouble(d, "profit"))
                .sum();

            report.append("【中价值客户】").append(mediumValue.size()).append("个（贡献15%利润）\n");
            report.append("• 利润贡献：").append(formatAmount(mediumValueProfit)).append("\n");
            report.append("• 占比：").append(String.format("%.1f", totalProfit != 0 ? mediumValueProfit / totalProfit * 100 : 0)).append("%\n\n");
        }

        // 低价值客户
        List<Map<String, Object>> lowValue = (List<Map<String, Object>>) analysis.get("lowValue");
        if (lowValue != null && !lowValue.isEmpty()) {
            double lowValueProfit = lowValue.stream()
                .mapToDouble(d -> extractDouble(d, "profit"))
                .sum();

            report.append("【低价值客户】").append(lowValue.size()).append("个（贡献5%利润）\n");
            report.append("• 利润贡献：").append(formatAmount(lowValueProfit)).append("\n");
            report.append("• 占比：").append(String.format("%.1f", totalProfit != 0 ? lowValueProfit / totalProfit * 100 : 0)).append("%\n\n");
        }

        // 建议
        report.append("【建议】\n");
        report.append("• 聚焦高价值客户维护，提升客户满意度\n");
        report.append("• 挖掘中价值客户潜力，提升客户等级\n");
        report.append("• 评估低价值客户，优化资源配置\n");

        return report.toString();
    }

    /**
     * 提取int值
     */
    private int extractInt(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0;
        Object value = map.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
