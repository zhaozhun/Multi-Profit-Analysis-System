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
 * 全维度亏损扫描技能
 * 扫描各维度下的亏损情况，找出问题点
 */
@Slf4j
@Component
public class LossScanSkill implements Skill {

    @Autowired
    private FunctionRegistry functionRegistry;

    @Override
    public String getName() {
        return "loss-scan";
    }

    @Override
    public String getDescription() {
        return "扫描各维度下的亏损情况，找出问题点";
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("亏损", "低利", "低利润", "亏损分析", "哪些亏损", "亏损排名");
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
        log.info("执行全维度亏损扫描技能");

        String period = context.getPeriod();

        try {
            // 扫描各维度的亏损情况
            List<Map<String, Object>> orgLosses = scanDimensionLosses("ORG", period);
            List<Map<String, Object>> productLosses = scanDimensionLosses("PRODUCT", period);
            List<Map<String, Object>> deptLosses = scanDimensionLosses("DEPT", period);

            // 生成报告
            String report = generateReport(orgLosses, productLosses, deptLosses, period);

            return SkillResult.success(report, Map.of(
                "orgLosses", orgLosses,
                "productLosses", productLosses,
                "deptLosses", deptLosses
            ));

        } catch (Exception e) {
            log.error("亏损扫描执行失败", e);
            return SkillResult.failed("扫描失败: " + e.getMessage());
        }
    }

    /**
     * 扫描维度亏损
     */
    private List<Map<String, Object>> scanDimensionLosses(String dimType, String period) {
        Map<String, Object> params = new HashMap<>();
        params.put("dimensions", Arrays.asList(dimType));
        params.put("metrics", Arrays.asList("REVENUE", "COST", "PROFIT"));
        params.put("period", period);

        Map<String, Object> result = (Map<String, Object>) functionRegistry.execute("query_profit_metrics", params);
        List<Map<String, Object>> data = extractList(result, "data");

        // 筛选亏损记录
        List<Map<String, Object>> losses = new ArrayList<>();
        if (data != null) {
            for (Map<String, Object> item : data) {
                double profit = extractDouble(item, "profit");
                if (profit < 0) {
                    losses.add(item);
                }
            }
        }

        // 按利润排序（亏损最多的在前）
        losses.sort((a, b) -> Double.compare(
            extractDouble(a, "profit"),
            extractDouble(b, "profit")
        ));

        return losses;
    }

    /**
     * 生成报告
     */
    private String generateReport(List<Map<String, Object>> orgLosses,
                                   List<Map<String, Object>> productLosses,
                                   List<Map<String, Object>> deptLosses,
                                   String period) {
        StringBuilder report = new StringBuilder();

        report.append("📊 全维度亏损扫描报告\n\n");
        report.append("【扫描期间】").append(period).append("\n\n");

        // 机构维度
        report.append("【机构维度亏损】\n");
        if (orgLosses.isEmpty()) {
            report.append("• 无亏损机构\n");
        } else {
            report.append("• 亏损机构数：").append(orgLosses.size()).append("\n");
            for (int i = 0; i < Math.min(3, orgLosses.size()); i++) {
                Map<String, Object> item = orgLosses.get(i);
                String org = extractString(item, "org");
                double profit = extractDouble(item, "profit");
                report.append("• ").append(org).append("：").append(formatAmount(profit)).append("\n");
            }
        }

        // 产品维度
        report.append("\n【产品维度亏损】\n");
        if (productLosses.isEmpty()) {
            report.append("• 无亏损产品\n");
        } else {
            report.append("• 亏损产品数：").append(productLosses.size()).append("\n");
            for (int i = 0; i < Math.min(3, productLosses.size()); i++) {
                Map<String, Object> item = productLosses.get(i);
                String product = extractString(item, "product");
                double profit = extractDouble(item, "profit");
                report.append("• ").append(product).append("：").append(formatAmount(profit)).append("\n");
            }
        }

        // 部门维度
        report.append("\n【部门维度亏损】\n");
        if (deptLosses.isEmpty()) {
            report.append("• 无亏损部门\n");
        } else {
            report.append("• 亏损部门数：").append(deptLosses.size()).append("\n");
            for (int i = 0; i < Math.min(3, deptLosses.size()); i++) {
                Map<String, Object> item = deptLosses.get(i);
                String dept = extractString(item, "dept");
                double profit = extractDouble(item, "profit");
                report.append("• ").append(dept).append("：").append(formatAmount(profit)).append("\n");
            }
        }

        // 总结
        int totalLosses = orgLosses.size() + productLosses.size() + deptLosses.size();
        report.append("\n【总结】\n");
        report.append("• 共发现 ").append(totalLosses).append(" 个亏损点\n");
        report.append("• 建议重点关注亏损最严重的维度，分析原因并制定改善措施\n");

        return report.toString();
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
