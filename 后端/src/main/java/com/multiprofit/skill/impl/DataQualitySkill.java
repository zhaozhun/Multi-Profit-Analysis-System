package com.multiprofit.skill.impl;

import com.multiprofit.ai.FunctionRegistry;
import com.multiprofit.skill.SkillContext;
import com.multiprofit.skill.SkillResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 数据质量扫描技能
 * 扫描数据质量，识别问题
 */
@Slf4j
@Component
public class DataQualitySkill extends SimpleSkill {

    @Autowired
    private FunctionRegistry functionRegistry;

    @Override
    public String getName() {
        return "data-quality";
    }

    @Override
    public String getDescription() {
        return "扫描数据质量，识别问题";
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("数据质量", "质量扫描", "数据校验", "数据检查");
    }

    @Override
    public SkillResult execute(SkillContext context) {
        log.info("执行数据质量扫描技能");

        String period = context.getPeriod();
        String dimType = context.getStringParam("dim_type");

        try {
            // 扫描数据质量
            Map<String, Object> params = new HashMap<>();
            params.put("period", period);
            if (dimType != null) {
                params.put("dim_type", dimType);
            }

            Map<String, Object> result = (Map<String, Object>) functionRegistry.execute(
                "scan_data_quality", params);

            // 生成报告
            String report = generateReport(result, period, dimType);

            return SkillResult.success(report, result);

        } catch (Exception e) {
            log.error("数据质量扫描失败", e);
            return SkillResult.failed("扫描失败: " + e.getMessage());
        }
    }

    /**
     * 生成报告
     */
    private String generateReport(Map<String, Object> result, String period, String dimType) {
        StringBuilder report = new StringBuilder();

        report.append("📊 数据质量扫描报告\n\n");
        report.append("【扫描期间】").append(period).append("\n");
        if (dimType != null) {
            report.append("【扫描维度】").append(getDimensionName(dimType)).append("\n");
        }
        report.append("\n");

        Map<String, Object> reportData = extractMap(result, "report");
        if (reportData != null) {
            // 整体评分
            int overallScore = extractInt(reportData, "overallScore");
            report.append("【整体评分】").append(overallScore).append("分\n\n");

            // 完整性
            int completeness = extractInt(reportData, "completeness");
            report.append("【完整性】").append(completeness).append("分\n");
            report.append("• ").append(getScoreDescription(completeness)).append("\n\n");

            // 一致性
            int consistency = extractInt(reportData, "consistency");
            report.append("【一致性】").append(consistency).append("分\n");
            report.append("• ").append(getScoreDescription(consistency)).append("\n\n");

            // 准确性
            int accuracy = extractInt(reportData, "accuracy");
            report.append("【准确性】").append(accuracy).append("分\n");
            report.append("• ").append(getScoreDescription(accuracy)).append("\n\n");

            // 问题清单
            List<Map<String, Object>> issues = extractList(reportData, "issues");
            if (issues != null && !issues.isEmpty()) {
                report.append("【问题清单】\n");
                for (int i = 0; i < Math.min(5, issues.size()); i++) {
                    Map<String, Object> issue = issues.get(i);
                    String level = extractString(issue, "level");
                    String description = extractString(issue, "description");

                    report.append(i + 1).append(". ").append(getLevelIcon(level)).append(" ");
                    report.append(description).append("\n");
                }
            }
        }

        return report.toString();
    }

    /**
     * 获取维度名称
     */
    private String getDimensionName(String dimType) {
        switch (dimType) {
            case "ORG": return "机构";
            case "PRODUCT": return "产品";
            case "DEPT": return "部门";
            case "CHANNEL": return "渠道";
            default: return dimType;
        }
    }

    /**
     * 获取评分描述
     */
    private String getScoreDescription(int score) {
        if (score >= 90) {
            return "优秀，数据质量良好";
        } else if (score >= 80) {
            return "良好，存在少量问题";
        } else if (score >= 60) {
            return "一般，需要关注改进";
        } else {
            return "较差，需要立即处理";
        }
    }

    /**
     * 获取级别图标
     */
    private String getLevelIcon(String level) {
        switch (level.toLowerCase()) {
            case "critical": return "🔴";
            case "warning": return "🟡";
            case "info": return "🟢";
            default: return "⚪";
        }
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

    /**
     * 提取Map值
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return null;
        Object value = map.get(key);
        if (value instanceof Map) return (Map<String, Object>) value;
        return null;
    }
}
