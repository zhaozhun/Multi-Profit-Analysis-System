package com.multiprofit.allocation.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multiprofit.ai.ModelApiClient;
import com.multiprofit.allocation.model.*;
import com.multiprofit.allocation.service.AllocationConfigService;
import com.multiprofit.allocation.service.AllocationService;
import com.multiprofit.allocation.service.AllocationService.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分摊AI服务
 * 集成Claude AI，提供智能化的分摊分析和建议
 */
@Slf4j
@Service
public class AllocationAiService {

    @Autowired
    private ModelApiClient claudeClient;

    @Autowired
    private AllocationConfigService configService;

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * AI对话接口
     */
    public String chat(String userMessage, String context) {
        try {
            // 构建系统提示词
            String systemPrompt = buildSystemPrompt();

            // 构建完整的消息
            String fullMessage = buildFullMessage(userMessage, context);

            // 调用Claude API
            return claudeClient.chat(systemPrompt, fullMessage);

        } catch (Exception e) {
            log.error("AI对话失败", e);
            return "抱歉，AI服务暂时不可用，请稍后再试。";
        }
    }

    /**
     * 分析分摊结果
     */
    public String analyzeAllocation(String period, String dimType, String costType) {
        try {
            // 获取分摊结果
            List<AllocationBatch> results = allocationService.listResults(period, costType);
            if (results.isEmpty()) {
                return "未找到 " + period + " 期间的分摊结果。";
            }

            // 获取最新的批次详情
            AllocationBatch latestBatch = results.get(0);
            AllocationBatchDetail detail = allocationService.getResultDetail(latestBatch.getId());

            // 构建分析提示词
            String analysisPrompt = buildAnalysisPrompt(detail, dimType);

            // 调用AI分析
            return claudeClient.chat(
                "你是一个银行盈利分析专家，擅长分析成本分摊结果。",
                analysisPrompt
            );

        } catch (Exception e) {
            log.error("分析分摊结果失败", e);
            return "分析失败: " + e.getMessage();
        }
    }

    /**
     * 推荐分摊规则
     */
    public String suggestRule(String costType, Double costAmount, String businessContext) {
        try {
            // 获取现有规则
            List<AllocationRuleConfig> existingRules = configService.listRules(costType, null);

            // 获取可用因子
            List<AllocationFactorConfig> factors = configService.listFactors(null, costType);

            // 获取可用算法
            List<AllocationAlgorithmConfig> algorithms = configService.listAlgorithms(null);

            // 构建推荐提示词
            String suggestPrompt = buildSuggestPrompt(costType, costAmount, businessContext,
                existingRules, factors, algorithms);

            // 调用AI推荐
            return claudeClient.chat(
                "你是一个银行成本分摊专家，擅长设计分摊规则。",
                suggestPrompt
            );

        } catch (Exception e) {
            log.error("推荐分摊规则失败", e);
            return "推荐失败: " + e.getMessage();
        }
    }

    /**
     * 诊断分摊规则
     */
    public String diagnoseRules(String period) {
        try {
            // 获取所有规则
            List<AllocationRuleConfig> rules = configService.listRules(null, null);

            // 获取所有因子
            List<AllocationFactorConfig> factors = configService.listFactors(null, null);

            // 获取所有算法
            List<AllocationAlgorithmConfig> algorithms = configService.listAlgorithms(null);

            // 构建诊断提示词
            String diagnosePrompt = buildDiagnosePrompt(period, rules, factors, algorithms);

            // 调用AI诊断
            return claudeClient.chat(
                "你是一个银行成本分摊专家，擅长诊断分摊规则配置问题。",
                diagnosePrompt
            );

        } catch (Exception e) {
            log.error("诊断分摊规则失败", e);
            return "诊断失败: " + e.getMessage();
        }
    }

    /**
     * 生成经营简报（包含分摊分析）
     */
    public String generateBrief(String period) {
        try {
            // 获取分摊结果
            List<AllocationBatch> results = allocationService.listResults(period, null);

            // 获取分摊规则
            List<AllocationRuleConfig> rules = configService.listRules(null, "ACTIVE");

            // 构建简报提示词
            String briefPrompt = buildBriefPrompt(period, results, rules);

            // 调用AI生成简报
            return claudeClient.chat(
                "你是一个银行经营分析专家，擅长生成经营简报。",
                briefPrompt
            );

        } catch (Exception e) {
            log.error("生成经营简报失败", e);
            return "生成简报失败: " + e.getMessage();
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return """
            你是多维盈利分析系统的AI助手，专门负责成本分摊相关的问题。

            你的能力包括：
            1. 查询和解释分摊规则
            2. 预览和分析分摊结果
            3. 推荐分摊规则配置
            4. 诊断分摊规则问题
            5. 生成分摊相关的经营简报

            请用专业、清晰的语言回答用户问题，并提供有价值的分析和建议。
            """;
    }

    /**
     * 构建完整消息
     */
    private String buildFullMessage(String userMessage, String context) {
        StringBuilder sb = new StringBuilder();

        if (context != null && !context.isBlank()) {
            sb.append("上下文信息：\n").append(context).append("\n\n");
        }

        sb.append("用户问题：\n").append(userMessage);

        return sb.toString();
    }

    /**
     * 构建分析提示词
     */
    private String buildAnalysisPrompt(AllocationBatchDetail detail, String dimType) {
        StringBuilder sb = new StringBuilder();

        sb.append("请分析以下分摊结果：\n\n");

        // 批次信息
        AllocationBatch batch = detail.getBatch();
        sb.append("## 分摊批次信息\n");
        sb.append("- 批次号: ").append(batch.getBatchNo()).append("\n");
        sb.append("- 期间: ").append(batch.getPeriod()).append("\n");
        sb.append("- 成本类型: ").append(batch.getCostType()).append("\n");
        sb.append("- 总金额: ").append(batch.getTotalAmount()).append("\n");
        sb.append("- 已分摊金额: ").append(batch.getAllocatedAmount()).append("\n");
        sb.append("- 记录数: ").append(batch.getRecordCount()).append("\n\n");

        // 分摊明细
        sb.append("## 分摊明细\n");
        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        for (AllocationDetail d : detail.getDetails()) {
            summary.merge(d.getTargetDimCode(), d.getAllocatedAmount(), BigDecimal::add);
        }

        for (Map.Entry<String, BigDecimal> entry : summary.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        sb.append("\n请从以下角度进行分析：\n");
        sb.append("1. 分摊结果的合理性\n");
        sb.append("2. 各维度的差异分析\n");
        sb.append("3. 可能存在的问题\n");
        sb.append("4. 优化建议\n");

        return sb.toString();
    }

    /**
     * 构建推荐提示词
     */
    private String buildSuggestPrompt(String costType, Double costAmount, String businessContext,
                                       List<AllocationRuleConfig> existingRules,
                                       List<AllocationFactorConfig> factors,
                                       List<AllocationAlgorithmConfig> algorithms) {
        StringBuilder sb = new StringBuilder();

        sb.append("请为以下成本类型推荐分摊规则配置：\n\n");

        sb.append("## 成本信息\n");
        sb.append("- 成本类型: ").append(costType).append("\n");
        if (costAmount != null) {
            sb.append("- 成本金额: ").append(costAmount).append("\n");
        }
        if (businessContext != null && !businessContext.isBlank()) {
            sb.append("- 业务背景: ").append(businessContext).append("\n");
        }

        sb.append("\n## 现有规则\n");
        if (existingRules.isEmpty()) {
            sb.append("暂无现有规则\n");
        } else {
            for (AllocationRuleConfig rule : existingRules) {
                sb.append("- ").append(rule.getRuleCode()).append(": ").append(rule.getRuleName())
                  .append(" (算法: ").append(rule.getAlgorithmCode()).append(")\n");
            }
        }

        sb.append("\n## 可用因子\n");
        for (AllocationFactorConfig factor : factors) {
            sb.append("- ").append(factor.getFactorCode()).append(": ").append(factor.getFactorName())
              .append(" (类型: ").append(factor.getFactorType()).append(")\n");
        }

        sb.append("\n## 可用算法\n");
        for (AllocationAlgorithmConfig algorithm : algorithms) {
            sb.append("- ").append(algorithm.getAlgorithmCode()).append(": ").append(algorithm.getAlgorithmName())
              .append(" (").append(algorithm.getDescription()).append(")\n");
        }

        sb.append("\n请推荐：\n");
        sb.append("1. 适合的分摊算法\n");
        sb.append("2. 适合的分摊因子\n");
        sb.append("3. 具体的规则配置建议\n");
        sb.append("4. 配置理由说明\n");

        return sb.toString();
    }

    /**
     * 构建诊断提示词
     */
    private String buildDiagnosePrompt(String period, List<AllocationRuleConfig> rules,
                                        List<AllocationFactorConfig> factors,
                                        List<AllocationAlgorithmConfig> algorithms) {
        StringBuilder sb = new StringBuilder();

        sb.append("请诊断以下分摊规则配置：\n\n");

        sb.append("## 诊断期间: ").append(period).append("\n\n");

        sb.append("## 现有规则配置\n");
        for (AllocationRuleConfig rule : rules) {
            sb.append("### ").append(rule.getRuleCode()).append(" - ").append(rule.getRuleName()).append("\n");
            sb.append("- 成本类型: ").append(rule.getCostType()).append("\n");
            sb.append("- 来源维度: ").append(rule.getSourceDimType()).append("\n");
            sb.append("- 目标维度: ").append(rule.getTargetDimType()).append("\n");
            sb.append("- 算法: ").append(rule.getAlgorithmCode()).append("\n");
            sb.append("- 状态: ").append(rule.getStatus()).append("\n");
            sb.append("- 优先级: ").append(rule.getPriority()).append("\n\n");
        }

        sb.append("## 可用因子\n");
        for (AllocationFactorConfig factor : factors) {
            sb.append("- ").append(factor.getFactorCode()).append(": ").append(factor.getFactorName())
              .append(" (适用: ").append(factor.getApplicableCostTypes()).append(")\n");
        }

        sb.append("\n## 可用算法\n");
        for (AllocationAlgorithmConfig algorithm : algorithms) {
            sb.append("- ").append(algorithm.getAlgorithmCode()).append(": ").append(algorithm.getAlgorithmName())
              .append("\n");
        }

        sb.append("\n请从以下角度进行诊断：\n");
        sb.append("1. 规则覆盖度（是否覆盖所有成本类型）\n");
        sb.append("2. 规则冲突检查（是否存在重叠或矛盾）\n");
        sb.append("3. 因子选择合理性\n");
        sb.append("4. 算法适用性\n");
        sb.append("5. 配置问题和优化建议\n");

        return sb.toString();
    }

    /**
     * 构建简报提示词
     */
    private String buildBriefPrompt(String period, List<AllocationBatch> results,
                                     List<AllocationRuleConfig> rules) {
        StringBuilder sb = new StringBuilder();

        sb.append("请生成 ").append(period).append(" 期间的成本分摊经营简报：\n\n");

        sb.append("## 分摊执行情况\n");
        if (results.isEmpty()) {
            sb.append("暂无分摊执行记录\n");
        } else {
            for (AllocationBatch batch : results) {
                sb.append("- 批次: ").append(batch.getBatchNo())
                  .append(", 状态: ").append(batch.getStatus())
                  .append(", 总金额: ").append(batch.getTotalAmount())
                  .append(", 已分摊: ").append(batch.getAllocatedAmount())
                  .append("\n");
            }
        }

        sb.append("\n## 活跃规则\n");
        for (AllocationRuleConfig rule : rules) {
            sb.append("- ").append(rule.getRuleCode()).append(": ").append(rule.getRuleName())
              .append(" (").append(rule.getCostType()).append(" -> ").append(rule.getTargetDimType()).append(")\n");
        }

        sb.append("\n请生成简报，包括：\n");
        sb.append("1. 分摊执行概况\n");
        sb.append("2. 成本分布分析\n");
        sb.append("3. 关键发现和问题\n");
        sb.append("4. 管理建议\n");

        return sb.toString();
    }
}
