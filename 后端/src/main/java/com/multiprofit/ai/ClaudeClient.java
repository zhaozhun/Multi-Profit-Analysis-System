package com.multiprofit.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Claude AI 客户端封装
 */
@Component
public class ClaudeClient {

    @Value("${ai.anthropic.api-key:}")
    private String apiKey;

    @Value("${ai.anthropic.base-url:https://api.anthropic.com}")
    private String baseUrl;

    @Value("${ai.anthropic.model:claude-sonnet-4-20250514}")
    private String model;

    @Value("${ai.anthropic.max-tokens:4096}")
    private int maxTokens;

    private boolean available = false;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your-api-key-here")) {
            available = true;
            System.out.println("✅ Claude API 已配置，AI功能可用");
            System.out.println("   Base URL: " + baseUrl);
            System.out.println("   Model: " + model);
        } else {
            available = false;
            System.out.println("ℹ️ 未配置 ANTHROPIC_API_KEY，AI功能使用Mock模式");
        }
    }

    /**
     * 发送对话请求
     */
    public String chat(String systemPrompt, String userMessage) {
        if (!available) {
            return mockResponse(userMessage);
        }

        try {
            String requestBody = String.format("""
                {
                    "model": "%s",
                    "max_tokens": %d,
                    "system": "%s",
                    "messages": [
                        {
                            "role": "user",
                            "content": "%s"
                        }
                    ]
                }
                """, model, maxTokens, escapeJson(systemPrompt), escapeJson(userMessage));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResponse(response.body());
            } else {
                System.err.println("Claude API error: " + response.statusCode() + " - " + response.body());
                return "AI服务暂时不可用，请稍后重试。错误码: " + response.statusCode();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Claude API call failed: " + e.getMessage());
            return "AI服务调用失败: " + e.getMessage();
        }
    }

    public String chat(String message) {
        return chat(getSystemPrompt(), message);
    }

    public boolean isAvailable() {
        return available;
    }

    private String getSystemPrompt() {
        return "你是多维盈利分析系统的AI助手，专注于银行经营数据分析。" +
            "你需要根据用户的问题，提供专业的经营分析、数据解读和建议。" +
            "回答要简洁、专业、有洞察力。";
    }

    /**
     * 解析API响应
     */
    private String parseResponse(String responseBody) {
        try {
            // 简单解析JSON提取content
            int contentStart = responseBody.indexOf("\"text\":\"") + 8;
            int contentEnd = responseBody.indexOf("\"", contentStart);
            if (contentStart > 7 && contentEnd > contentStart) {
                return responseBody.substring(contentStart, contentEnd)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            }
            return responseBody;
        } catch (Exception e) {
            return responseBody;
        }
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private String mockResponse(String question) {
        // 智能Mock响应
        if (question.contains("利润") && question.contains("最高")) {
            return "根据2026年5月数据分析：\n\n" +
                "📊 各机构净利润排名：\n" +
                "1. 北京分行 - 净利润 28,500万元\n" +
                "2. 上海分行 - 净利润 25,800万元\n" +
                "3. 深圳分行 - 净利润 22,100万元\n" +
                "4. 广州分行 - 净利润 19,600万元\n" +
                "5. 杭州分行 - 净利润 18,200万元\n\n" +
                "💡 北京分行表现最优，主要得益于对公贷款业务增长和风险成本控制良好。";
        }
        if (question.contains("趋势")) {
            return "📈 近3个月盈利趋势分析：\n\n" +
                "• 3月：净利润 115,200万元（环比+2.1%）\n" +
                "• 4月：净利润 121,800万元（环比+5.7%）\n" +
                "• 5月：净利润 128,400万元（环比+5.4%）\n\n" +
                "整体呈上升趋势，收入增长主要来自零售条线和金融市场条线。";
        }
        if (question.contains("简报")) {
            return "📋 2026年5月经营简报\n\n" +
                "一、整体经营情况\n" +
                "本月全行实现净利润12.84亿元，环比增长5.4%，同比增长8.2%。\n\n" +
                "二、利润构成\n" +
                "• 业务总收入：35.6亿元\n" +
                "• FTP成本：12.8亿元（占收入36%）\n" +
                "• 风险成本：5.2亿元（占收入14.6%）\n" +
                "• 运营成本：4.8亿元（占收入13.5%）\n\n" +
                "三、重点关注\n" +
                "1. 深圳分行对公不良率上升，需关注资产质量\n" +
                "2. 零售条线消费贷款增速放缓\n\n" +
                "四、经营建议\n" +
                "1. 加强对公贷款风险管控\n" +
                "2. 推动零售数字化转型\n" +
                "3. 优化FTP定价策略";
        }
        return String.format(
            "[AI Mock模式] 收到问题：「%s」\n\n" +
            "当前为Mock模式，配置ANTHROPIC_API_KEY后可启用真实AI分析能力。\n" +
            "支持的功能：数据查询、趋势分析、异常诊断、经营简报生成。",
            question
        );
    }
}
