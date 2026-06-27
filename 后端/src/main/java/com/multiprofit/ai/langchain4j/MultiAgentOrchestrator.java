package com.multiprofit.ai.langchain4j;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 多Agent编排器
 * 支持串行和并行的多Agent协作
 */
@Slf4j
@Component
public class MultiAgentOrchestrator {

    @Autowired
    private DynamicAgentFactory agentFactory;

    /**
     * 场景A：分析+报告（串行）
     * 专项分析Agent分析 → 智能助手Agent生成报告
     */
    public String analysisAndReport(String query) {
        log.info("执行分析+报告流程，查询: {}", query);

        // 1. 创建专项分析Agent接口
        AnalysisAgent analysisAgent = agentFactory.createAgent("专项分析Agent", AnalysisAgent.class);

        // 2. 执行分析
        String analysisResult = analysisAgent.analyze(query);
        log.info("分析完成，结果长度: {}", analysisResult.length());

        // 3. 创建报告生成Agent接口
        ReportAgent reportAgent = agentFactory.createAgent("智能助手Agent", ReportAgent.class);

        // 4. 生成报告
        String report = reportAgent.generateReport(analysisResult);
        log.info("报告生成完成");

        return report;
    }

    /**
     * 场景B：数据+分析（串行）
     * 数据接入Agent处理 → 专项分析Agent分析
     */
    public String dataAndAnalysis(String query) {
        log.info("执行数据+分析流程，查询: {}", query);

        // 1. 创建数据接入Agent接口
        DataIngestionAgent dataAgent = agentFactory.createAgent("数据接入Agent", DataIngestionAgent.class);

        // 2. 处理数据
        String dataResult = dataAgent.process(query);
        log.info("数据处理完成，结果长度: {}", dataResult.length());

        // 3. 创建专项分析Agent接口
        AnalysisAgent analysisAgent = agentFactory.createAgent("专项分析Agent", AnalysisAgent.class);

        // 4. 执行分析
        String analysisResult = analysisAgent.analyze(dataResult);
        log.info("分析完成");

        return analysisResult;
    }

    /**
     * 场景C：分析+预警（串行）
     * 专项分析Agent分析 → 风险预警Agent检测
     */
    public String analysisAndAlert(String query) {
        log.info("执行分析+预警流程，查询: {}", query);

        // 1. 创建专项分析Agent接口
        AnalysisAgent analysisAgent = agentFactory.createAgent("专项分析Agent", AnalysisAgent.class);

        // 2. 执行分析
        String analysisResult = analysisAgent.analyze(query);
        log.info("分析完成，结果长度: {}", analysisResult.length());

        // 3. 创建风险预警Agent接口
        RiskAlertAgent riskAgent = agentFactory.createAgent("风险预警Agent", RiskAlertAgent.class);

        // 4. 检测风险
        String riskResult = riskAgent.detect(analysisResult);
        log.info("风险检测完成");

        return riskResult;
    }

    /**
     * 场景D：全维度分析（并行）
     * 多个Agent同时执行，最后汇总
     */
    public String fullDimensionAnalysis(String query) {
        log.info("执行全维度分析，查询: {}", query);

        // 并行执行多个Agent
        CompletableFuture<String> profitFuture = CompletableFuture.supplyAsync(() -> {
            AnalysisAgent agent = agentFactory.createAgent("专项分析Agent", AnalysisAgent.class);
            return agent.analyze("利润维度：" + query);
        });

        CompletableFuture<String> riskFuture = CompletableFuture.supplyAsync(() -> {
            RiskAlertAgent agent = agentFactory.createAgent("风险预警Agent", RiskAlertAgent.class);
            return agent.detect("风险维度：" + query);
        });

        CompletableFuture<String> costFuture = CompletableFuture.supplyAsync(() -> {
            AllocationAgent agent = agentFactory.createAgent("费用分摊Agent", AllocationAgent.class);
            return agent.analyze("成本维度：" + query);
        });

        // 等待所有任务完成
        CompletableFuture.allOf(profitFuture, riskFuture, costFuture).join();

        // 汇总结果
        String profitResult = profitFuture.join();
        String riskResult = riskFuture.join();
        String costResult = costFuture.join();

        // 生成综合报告
        ReportAgent reportAgent = agentFactory.createAgent("智能助手Agent", ReportAgent.class);
        return reportAgent.generateReport(
            "利润分析：" + profitResult + "\n\n" +
            "风险分析：" + riskResult + "\n\n" +
            "成本分析：" + costResult
        );
    }

    /**
     * 判断是否需要多Agent协作
     */
    public boolean needMultiAgentCollaboration(String userMessage) {
        // 关键词匹配
        return userMessage.contains("并生成报告") ||
               userMessage.contains("全维度分析") ||
               userMessage.contains("月度经营分析") ||
               userMessage.contains("导入数据后分析") ||
               userMessage.contains("分析风险");
    }

    /**
     * 执行多Agent协作
     */
    public String executeMultiAgent(String userMessage) {
        if (userMessage.contains("并生成报告")) {
            // 场景A：分析+报告
            return analysisAndReport(userMessage);
        } else if (userMessage.contains("导入数据后分析")) {
            // 场景B：数据+分析
            return dataAndAnalysis(userMessage);
        } else if (userMessage.contains("分析风险")) {
            // 场景C：分析+预警
            return analysisAndAlert(userMessage);
        } else if (userMessage.contains("全维度分析") || userMessage.contains("月度经营分析")) {
            // 场景D：全维度分析
            return fullDimensionAnalysis(userMessage);
        } else {
            // 默认走分析+报告
            return analysisAndReport(userMessage);
        }
    }

    // ========== Agent接口定义 ==========

    /**
     * 专项分析Agent接口
     */
    public interface AnalysisAgent {
        String analyze(String query);
    }

    /**
     * 报告生成Agent接口
     */
    public interface ReportAgent {
        String generateReport(String analysisResult);
    }

    /**
     * 数据接入Agent接口
     */
    public interface DataIngestionAgent {
        String process(String query);
    }

    /**
     * 风险预警Agent接口
     */
    public interface RiskAlertAgent {
        String detect(String query);
    }

    /**
     * 费用分摊Agent接口
     */
    public interface AllocationAgent {
        String analyze(String query);
    }
}
