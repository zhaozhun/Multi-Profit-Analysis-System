package com.multiprofit.skill.impl;

import com.multiprofit.ai.FunctionRegistry;
import com.multiprofit.skill.SkillContext;
import com.multiprofit.skill.SkillResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 费用分摊技能
 * 处理费用分摊相关的查询和操作
 */
@Slf4j
@Component
public class AllocationSkill extends SimpleSkill {

    @Autowired
    private FunctionRegistry functionRegistry;

    @Override
    public String getName() {
        return "allocation";
    }

    @Override
    public String getDescription() {
        return "处理费用分摊相关的查询和操作";
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("分摊", "费用分摊", "成本分摊", "分摊规则", "分摊因子");
    }

    @Override
    public SkillResult execute(SkillContext context) {
        log.info("执行费用分摊技能");

        String period = context.getPeriod();
        String action = context.getStringParam("action");

        try {
            if ("diagnose".equals(action)) {
                return diagnoseRules(period);
            } else if ("execute".equals(action)) {
                String ruleCode = context.getStringParam("rule_code");
                return executeAllocation(ruleCode, period);
            } else {
                return queryAllocationInfo(period);
            }

        } catch (Exception e) {
            log.error("费用分摊技能执行失败", e);
            return SkillResult.failed("执行失败: " + e.getMessage());
        }
    }

    /**
     * 查询分摊信息
     */
    private SkillResult queryAllocationInfo(String period) {
        Map<String, Object> params = new HashMap<>();
        params.put("period", period);

        Map<String, Object> result = (Map<String, Object>) functionRegistry.execute(
            "diagnose_allocation_rules", params);

        String report = generateQueryReport(result, period);
        return SkillResult.success(report, result);
    }

    /**
     * 诊断规则
     */
    private SkillResult diagnoseRules(String period) {
        Map<String, Object> params = new HashMap<>();
        params.put("period", period);

        Map<String, Object> result = (Map<String, Object>) functionRegistry.execute(
            "diagnose_allocation_rules", params);

        String report = generateDiagnosisReport(result, period);
        return SkillResult.success(report, result);
    }

    /**
     * 执行分摊
     */
    private SkillResult executeAllocation(String ruleCode, String period) {
        Map<String, Object> params = new HashMap<>();
        params.put("rule_code", ruleCode);
        params.put("period", period);
        params.put("dry_run", false);

        Map<String, Object> result = (Map<String, Object>) functionRegistry.execute(
            "execute_allocation", params);

        String report = generateExecutionReport(result, ruleCode, period);
        return SkillResult.success(report, result);
    }

    /**
     * 生成查询报告
     */
    private String generateQueryReport(Map<String, Object> result, String period) {
        StringBuilder report = new StringBuilder();

        report.append("💰 费用分摊信息\n\n");
        report.append("【查询期间】").append(period).append("\n\n");

        int score = extractInt(result, "score");
        int totalRules = extractInt(result, "totalRules");

        report.append("【整体情况】\n");
        report.append("• 规则总数：").append(totalRules).append("\n");
        report.append("• 健康评分：").append(score).append("分\n\n");

        List<String> uncovered = extractStringList(result, "uncoveredCostTypes");
        if (!uncovered.isEmpty()) {
            report.append("【未覆盖费用类型】\n");
            report.append("• 数量：").append(uncovered.size()).append("\n");
            for (int i = 0; i < Math.min(5, uncovered.size()); i++) {
                report.append("• ").append(uncovered.get(i)).append("\n");
            }
        }

        return report.toString();
    }

    /**
     * 生成诊断报告
     */
    private String generateDiagnosisReport(Map<String, Object> result, String period) {
        StringBuilder report = new StringBuilder();

        report.append("📋 分摊规则诊断报告\n\n");
        report.append("【诊断期间】").append(period).append("\n\n");

        int score = extractInt(result, "score");
        report.append("【整体评分】").append(score).append("分\n\n");

        // 问题1：规则覆盖度
        List<String> uncovered = extractStringList(result, "uncoveredCostTypes");
        if (!uncovered.isEmpty()) {
            report.append("【问题1】规则覆盖度不足 (严重)\n");
            report.append("• ").append(uncovered.size()).append("种费用类型未配置分摊规则\n");
            report.append("• 包括：").append(String.join("、", uncovered.subList(0, Math.min(3, uncovered.size())))).append("\n\n");
        }

        // 问题2：规则冲突
        List<Map<String, Object>> conflicts = extractList(result, "conflicts");
        if (!conflicts.isEmpty()) {
            report.append("【问题2】规则冲突 (警告)\n");
            report.append("• ").append(conflicts.size()).append("组规则存在冲突\n\n");
        }

        // 问题3：因子数据缺失
        List<Map<String, Object>> missingFactors = extractList(result, "missingFactors");
        if (!missingFactors.isEmpty()) {
            report.append("【问题3】因子数据缺失 (警告)\n");
            report.append("• ").append(missingFactors.size()).append("个因子缺少数据\n\n");
        }

        // 优化建议
        report.append("【优化建议】\n");
        if (!uncovered.isEmpty()) {
            report.append("1. 优先补充未配置规则的费用类型\n");
        }
        if (!conflicts.isEmpty()) {
            report.append("2. 解决规则冲突问题\n");
        }
        if (!missingFactors.isEmpty()) {
            report.append("3. 补充缺失的因子数据\n");
        }

        return report.toString();
    }

    /**
     * 生成执行报告
     */
    private String generateExecutionReport(Map<String, Object> result, String ruleCode, String period) {
        StringBuilder report = new StringBuilder();

        report.append("✅ 费用分摊执行报告\n\n");
        report.append("【执行信息】\n");
        report.append("• 规则编码：").append(ruleCode).append("\n");
        report.append("• 执行期间：").append(period).append("\n");
        report.append("• 执行模式：").append(extractString(result, "mode")).append("\n\n");

        Map<String, Object> allocResult = extractMap(result, "result");
        if (allocResult != null) {
            report.append("【执行结果】\n");
            report.append("• 分摊总额：").append(formatAmount(extractDouble(allocResult, "totalAmount"))).append("\n");
            report.append("• 分摊明细数：").append(extractInt(allocResult, "detailCount")).append("\n");
        }

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
