package com.multiprofit.skill.impl;

import com.multiprofit.ai.FunctionRegistry;
import com.multiprofit.skill.SkillContext;
import com.multiprofit.skill.SkillResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 敏感性分析技能
 * 分析各因素对盈利的影响程度
 */
@Slf4j
@Component
public class SensitivityAnalysisSkill extends SimpleSkill {

    @Autowired
    private FunctionRegistry functionRegistry;

    @Override
    public String getName() {
        return "sensitivity-analysis";
    }

    @Override
    public String getDescription() {
        return "分析各因素对盈利的影响程度";
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("敏感性", "影响因素", "如果", "假设", "情景分析");
    }

    @Override
    public SkillResult execute(SkillContext context) {
        log.info("执行敏感性分析技能");

        String period = context.getPeriod();
        double changeRange = context.getDoubleParam("change_range");
        if (changeRange == 0) {
            changeRange = 0.1; // 默认10%变化范围
        }

        try {
            // 定义分析因素
            List<Map<String, Object>> factors = Arrays.asList(
                Map.of("name", "收入", "baseValue", 100000000.0, "weight", 0.4),
                Map.of("name", "成本", "baseValue", 80000000.0, "weight", 0.3),
                Map.of("name", "风险成本", "baseValue", 10000000.0, "weight", 0.2),
                Map.of("name", "运营成本", "baseValue", 5000000.0, "weight", 0.1)
            );

            // 执行敏感性分析
            Map<String, Object> params = new HashMap<>();
            params.put("factors", factors);
            params.put("change_range", changeRange);

            Map<String, Object> result = (Map<String, Object>) functionRegistry.execute(
                "analyze_sensitivity", params);

            // 生成报告
            String report = generateReport(result, period, changeRange);

            return SkillResult.success(report, result);

        } catch (Exception e) {
            log.error("敏感性分析失败", e);
            return SkillResult.failed("分析失败: " + e.getMessage());
        }
    }

    /**
     * 生成报告
     */
    private String generateReport(Map<String, Object> result, String period, double changeRange) {
        StringBuilder report = new StringBuilder();

        report.append("📊 敏感性分析报告\n\n");
        report.append("【分析期间】").append(period).append("\n");
        report.append("【变化范围】").append(String.format("%.1f", changeRange * 100)).append("%\n\n");

        List<Map<String, Object>> analysis = extractList(result, "sensitivityAnalysis");
        if (analysis != null && !analysis.isEmpty()) {
            report.append("【分析结果】\n");

            // 按敏感性系数排序
            analysis.sort((a, b) -> Double.compare(
                extractDouble(b, "sensitivityCoefficient"),
                extractDouble(a, "sensitivityCoefficient")
            ));

            for (int i = 0; i < analysis.size(); i++) {
                Map<String, Object> item = analysis.get(i);
                String factorName = extractString(item, "factorName");
                double coefficient = extractDouble(item, "sensitivityCoefficient");

                report.append(i + 1).append(". ").append(factorName).append("\n");
                report.append("   • 敏感性系数：").append(String.format("%.4f", coefficient)).append("\n");
                report.append("   • 影响程度：").append(getSensitivityLevel(coefficient)).append("\n\n");
            }

            report.append("【结论】\n");
            if (!analysis.isEmpty()) {
                Map<String, Object> mostSensitive = analysis.get(0);
                report.append("• 最敏感因素：").append(extractString(mostSensitive, "factorName")).append("\n");
                report.append("• 建议重点关注该因素的变化\n");
            }
        }

        return report.toString();
    }

    /**
     * 获取敏感性等级
     */
    private String getSensitivityLevel(double coefficient) {
        if (coefficient > 0.5) {
            return "高敏感";
        } else if (coefficient > 0.2) {
            return "中敏感";
        } else {
            return "低敏感";
        }
    }
}
