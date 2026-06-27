package com.multiprofit.allocation.controller;

import com.multiprofit.allocation.ai.AllocationAiService;
import com.multiprofit.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 分摊AI控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/allocation/ai")
public class AllocationAiController {

    @Autowired
    private AllocationAiService aiService;

    /**
     * AI对话
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            String context = request.get("context");

            if (message == null || message.isBlank()) {
                return Result.error("消息不能为空");
            }

            String response = aiService.chat(message, context);
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return Result.error("AI对话失败: " + e.getMessage());
        }
    }

    /**
     * 分析分摊结果
     */
    @PostMapping("/analyze")
    public Result<String> analyzeAllocation(@RequestBody Map<String, String> request) {
        try {
            String period = request.get("period");
            String dimType = request.get("dimType");
            String costType = request.get("costType");

            if (period == null || period.isBlank()) {
                return Result.error("期间不能为空");
            }

            String analysis = aiService.analyzeAllocation(period, dimType, costType);
            return Result.success(analysis);
        } catch (Exception e) {
            log.error("分析分摊结果失败", e);
            return Result.error("分析失败: " + e.getMessage());
        }
    }

    /**
     * 推荐分摊规则
     */
    @PostMapping("/suggest")
    public Result<String> suggestRule(@RequestBody Map<String, Object> request) {
        try {
            String costType = (String) request.get("costType");
            Double costAmount = request.get("costAmount") != null ?
                Double.parseDouble(request.get("costAmount").toString()) : null;
            String businessContext = (String) request.get("businessContext");

            if (costType == null || costType.isBlank()) {
                return Result.error("成本类型不能为空");
            }

            String suggestion = aiService.suggestRule(costType, costAmount, businessContext);
            return Result.success(suggestion);
        } catch (Exception e) {
            log.error("推荐分摊规则失败", e);
            return Result.error("推荐失败: " + e.getMessage());
        }
    }

    /**
     * 诊断分摊规则
     */
    @PostMapping("/diagnose")
    public Result<String> diagnoseRules(@RequestBody Map<String, String> request) {
        try {
            String period = request.get("period");

            if (period == null || period.isBlank()) {
                return Result.error("期间不能为空");
            }

            String diagnosis = aiService.diagnoseRules(period);
            return Result.success(diagnosis);
        } catch (Exception e) {
            log.error("诊断分摊规则失败", e);
            return Result.error("诊断失败: " + e.getMessage());
        }
    }

    /**
     * 生成经营简报
     */
    @PostMapping("/brief")
    public Result<String> generateBrief(@RequestBody Map<String, String> request) {
        try {
            String period = request.get("period");

            if (period == null || period.isBlank()) {
                return Result.error("期间不能为空");
            }

            String brief = aiService.generateBrief(period);
            return Result.success(brief);
        } catch (Exception e) {
            log.error("生成经营简报失败", e);
            return Result.error("生成简报失败: " + e.getMessage());
        }
    }
}
