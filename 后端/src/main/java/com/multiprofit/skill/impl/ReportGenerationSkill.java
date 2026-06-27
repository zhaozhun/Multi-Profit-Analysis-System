package com.multiprofit.skill.impl;

import com.multiprofit.ai.FunctionRegistry;
import com.multiprofit.skill.SkillContext;
import com.multiprofit.skill.SkillResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 报告生成技能
 * 生成各类经营分析报告
 */
@Slf4j
@Component
public class ReportGenerationSkill extends SimpleSkill {

    @Autowired
    private FunctionRegistry functionRegistry;

    @Override
    public String getName() {
        return "report-generation";
    }

    @Override
    public String getDescription() {
        return "生成各类经营分析报告";
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("报告", "简报", "经营报告", "月报", "季报", "生成报告");
    }

    @Override
    public SkillResult execute(SkillContext context) {
        log.info("执行报告生成技能");

        String period = context.getPeriod();
        String reportType = context.getStringParam("report_type");

        // 默认生成月度经营简报
        if (reportType == null) {
            reportType = "monthly";
        }

        try {
            // 生成报告
            Map<String, Object> params = new HashMap<>();
            params.put("period", period);
            params.put("scope", "全行");
            params.put("format", "text");

            Map<String, Object> reportResult = (Map<String, Object>) functionRegistry.execute(
                "generate_business_brief", params);

            // 生成报告内容
            String report = generateReportContent(reportResult, period, reportType);

            return SkillResult.success(report, reportResult);

        } catch (Exception e) {
            log.error("报告生成失败", e);
            return SkillResult.failed("报告生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成报告内容
     */
    private String generateReportContent(Map<String, Object> reportResult, String period, String reportType) {
        StringBuilder report = new StringBuilder();

        report.append("📋 ").append(period).append(" 经营简报\n\n");

        // 从报告结果中提取数据
        Map<String, Object> brief = extractMap(reportResult, "brief");
        if (brief != null) {
            // 一、整体经营情况
            report.append("一、整体经营情况\n");
            double totalProfit = extractDouble(brief, "totalProfit");
            double profitGrowth = extractDouble(brief, "profitGrowth");
            report.append("• 本").append("期实现净利润：").append(formatAmount(totalProfit)).append("\n");
            report.append("• 同比增长：").append(String.format("%.2f", profitGrowth)).append("%\n\n");

            // 二、利润构成
            report.append("二、利润构成\n");
            double revenue = extractDouble(brief, "revenue");
            double cost = extractDouble(brief, "cost");
            report.append("• 业务总收入：").append(formatAmount(revenue)).append("\n");
            report.append("• 业务总成本：").append(formatAmount(cost)).append("\n");
            double costIncomeRatio = revenue != 0 ? (cost / revenue) * 100 : 0;
            report.append("• 成本收入比：").append(String.format("%.2f", costIncomeRatio)).append("%\n\n");

            // 三、重点关注
            report.append("三、重点关注\n");
            List<String> highlights = extractStringList(brief, "highlights");
            if (highlights != null && !highlights.isEmpty()) {
                for (int i = 0; i < highlights.size(); i++) {
                    report.append(i + 1).append(". ").append(highlights.get(i)).append("\n");
                }
            } else {
                report.append("暂无重点关注事项\n");
            }

            // 四、经营建议
            report.append("\n四、经营建议\n");
            List<String> suggestions = extractStringList(brief, "suggestions");
            if (suggestions != null && !suggestions.isEmpty()) {
                for (int i = 0; i < suggestions.size(); i++) {
                    report.append(i + 1).append(". ").append(suggestions.get(i)).append("\n");
                }
            } else {
                report.append("暂无经营建议\n");
            }
        } else {
            report.append("报告数据生成中，请稍后查看...\n");
        }

        return report.toString();
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

    /**
     * 提取String List值
     */
    @SuppressWarnings("unchecked")
    private List<String> extractStringList(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return Collections.emptyList();
        Object value = map.get(key);
        if (value instanceof List) return (List<String>) value;
        return Collections.emptyList();
    }
}
