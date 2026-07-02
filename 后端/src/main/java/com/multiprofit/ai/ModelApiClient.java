package com.multiprofit.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * 统一的模型API客户端
 * 支持普通对话和Function Call
 * 兼容小米、豆包等国产模型（OpenAI格式）
 */
@Component
public class ModelApiClient {

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.base-url:}")
    private String baseUrl;

    @Value("${ai.model:}")
    private String model;

    @Value("${ai.max-tokens:4096}")
    private int maxTokens;

    private boolean available = false;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        // 识别占位符:your_api_key / your-api-key-here / 空 均视为未配置
        if (apiKey != null && !apiKey.isEmpty()
                && !apiKey.equals("your_api_key")
                && !apiKey.equals("your-api-key-here")) {
            available = true;
            System.out.println("✅ 模型API已配置，AI功能可用");
            System.out.println("   Base URL: " + baseUrl);
            System.out.println("   Model: " + model);
        } else {
            available = false;
            System.out.println("ℹ️ 未配置AI_API_KEY，AI功能使用Mock模式");
        }
    }

    /**
     * 普通对话（无Function Call）
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
                System.err.println("模型API错误: " + response.statusCode() + " - " + response.body());
                return "AI服务暂时不可用，请稍后重试。错误码: " + response.statusCode();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("模型API调用失败: " + e.getMessage());
            return "AI服务调用失败: " + e.getMessage();
        }
    }

    public String chat(String message) {
        return chat(getSystemPrompt(), message);
    }

    /**
     * 支持Function Call的对话
     */
    public FunctionCallResult chatWithFunctions(String systemPrompt, String userMessage,
                                                 List<ToolDefinition> tools,
                                                 List<ChatMessage> history) {
        try {
            // 1. 构建带tools的请求
            String requestBody = buildRequestWithTools(systemPrompt, userMessage, tools, history);

            // 2. 调用模型API
            HttpResponse<String> response = callApi(requestBody);

            // 3. 解析响应，判断是否需要调用函数
            ModelResponse modelResponse = parseModelResponse(response.body());

            // 4. 如果是function_call，执行函数并递归调用
            if ("tool_use".equals(modelResponse.getStopReason())) {
                ToolUseBlock toolUse = modelResponse.getToolUse();

                // 5. 将函数结果返回给模型继续对话
                return continueWithFunctionResult(systemPrompt, userMessage,
                    toolUse, null, tools, history);
            }

            return new FunctionCallResult(modelResponse.getText(), null, null);

        } catch (Exception e) {
            throw new RuntimeException("模型API调用失败: " + e.getMessage(), e);
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * 将工具执行结果回灌给模型，让模型基于结果生成最终自然语言回答。
     * Anthropic Messages 格式: messages 末尾追加
     *   - assistant 消息(含 tool_use 块)
     *   - user 消息(含 tool_result 块, tool_use_id 关联)
     * 模型据此生成精炼文本(而非原始工具字段)。
     *
     * @param systemPrompt 系统提示词(同 Agent 配置)
     * @param userMessage  原始用户问题
     * @param toolUse      模型上一轮返回的 tool_use 块(含 id/name/input)
     * @param toolResultJson 工具执行结果(JSON 字符串)
     * @param tools        可用工具定义(允许模型继续调用,支持多轮)
     * @param history      会话历史
     * @return 模型生成的最终文本回答;失败时返回 null
     */
    public String continueWithToolResult(String systemPrompt,
                                          String userMessage,
                                          ToolUseBlock toolUse,
                                          String toolResultJson,
                                          List<ToolDefinition> tools,
                                          List<ChatMessage> history) {
        try {
            String requestBody = buildRequestWithToolResult(systemPrompt, userMessage,
                    toolUse, toolResultJson, tools, history);
            HttpResponse<String> response = callApi(requestBody);
            ModelResponse modelResponse = parseModelResponse(response.body());

            // 模型可能再次返回 tool_use(多轮工具调用),此处取文本;若仍为 tool_use 则返回已有文本(避免无限递归)
            if (modelResponse.getText() != null) {
                return modelResponse.getText();
            }
            // 无文本(可能再次要求工具),降级返回工具结果摘要
            return null;
        } catch (Exception e) {
            throw new RuntimeException("工具结果回灌模型失败: " + e.getMessage(), e);
        }
    }

    private String getSystemPrompt() {
        return "你是多维盈利分析系统的AI助手，专注于银行经营数据分析。" +
            "你需要根据用户的问题，提供专业的经营分析、数据解读和建议。" +
            "回答要简洁、专业、有洞察力。";
    }

    /**
     * 构建带tools的请求
     */
    private String buildRequestWithTools(String systemPrompt, String userMessage,
                                          List<ToolDefinition> tools,
                                          List<ChatMessage> history) throws JsonProcessingException {
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("max_tokens", maxTokens);
        request.put("system", systemPrompt);

        // 添加tools
        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> toolList = new ArrayList<>();
            for (ToolDefinition tool : tools) {
                Map<String, Object> toolMap = new HashMap<>();
                toolMap.put("name", tool.getName());
                toolMap.put("description", tool.getDescription());
                toolMap.put("input_schema", tool.getInputSchema());
                toolList.add(toolMap);
            }
            request.put("tools", toolList);
        }

        // 构建messages
        List<Map<String, Object>> messages = new ArrayList<>();

        // 添加历史消息
        if (history != null) {
            for (ChatMessage msg : history) {
                Map<String, Object> message = new HashMap<>();
                message.put("role", msg.getRole());
                message.put("content", msg.getContent());
                messages.add(message);
            }
        }

        // 添加当前用户消息
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        request.put("messages", messages);

        return objectMapper.writeValueAsString(request);
    }

    /**
     * 构建带工具结果回灌的请求(Anthropic Messages 格式)。
     * messages = history + user原问题 + assistant(tool_use块) + user(tool_result块)
     */
    @SuppressWarnings("unchecked")
    private String buildRequestWithToolResult(String systemPrompt, String userMessage,
                                               ToolUseBlock toolUse, String toolResultJson,
                                               List<ToolDefinition> tools,
                                               List<ChatMessage> history) throws JsonProcessingException {
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("max_tokens", maxTokens);
        request.put("system", systemPrompt);

        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> toolList = new ArrayList<>();
            for (ToolDefinition tool : tools) {
                Map<String, Object> toolMap = new HashMap<>();
                toolMap.put("name", tool.getName());
                toolMap.put("description", tool.getDescription());
                toolMap.put("input_schema", tool.getInputSchema());
                toolList.add(toolMap);
            }
            request.put("tools", toolList);
        }

        List<Map<String, Object>> messages = new ArrayList<>();

        // 历史消息
        if (history != null) {
            for (ChatMessage msg : history) {
                Map<String, Object> message = new HashMap<>();
                message.put("role", msg.getRole());
                message.put("content", msg.getContent());
                messages.add(message);
            }
        }

        // 当前用户原始问题
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        // assistant 回复:含 tool_use 块(回显模型上一轮的工具调用)
        Map<String, Object> toolUseBlock = new HashMap<>();
        toolUseBlock.put("type", "tool_use");
        toolUseBlock.put("id", toolUse.getId());
        toolUseBlock.put("name", toolUse.getName());
        toolUseBlock.put("input", toolUse.getInput() instanceof Map
                ? toolUse.getInput() : objectMapper.readValue(toolResultJson, Map.class));
        Map<String, Object> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", new Object[]{ toolUseBlock });
        messages.add(assistantMsg);

        // user 回复:tool_result 块(工具执行结果)
        Map<String, Object> toolResultBlock = new HashMap<>();
        toolResultBlock.put("type", "tool_result");
        toolResultBlock.put("tool_use_id", toolUse.getId());
        toolResultBlock.put("content", toolResultJson);
        Map<String, Object> toolResultMsg = new HashMap<>();
        toolResultMsg.put("role", "user");
        toolResultMsg.put("content", new Object[]{ toolResultBlock });
        messages.add(toolResultMsg);

        request.put("messages", messages);

        return objectMapper.writeValueAsString(request);
    }

    /**
     * 调用模型API
     */
    private HttpResponse<String> callApi(String requestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/v1/messages"))
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 解析模型响应（支持Function Call）
     */
    private ModelResponse parseModelResponse(String responseBody) throws JsonProcessingException {
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

        ModelResponse result = new ModelResponse();
        result.setStopReason((String) response.get("stop_reason"));

        // 解析content
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content != null && !content.isEmpty()) {
            for (Map<String, Object> block : content) {
                String type = (String) block.get("type");
                if ("text".equals(type)) {
                    result.setText((String) block.get("text"));
                } else if ("tool_use".equals(type)) {
                    ToolUseBlock toolUse = new ToolUseBlock();
                    toolUse.setId((String) block.get("id"));
                    toolUse.setName((String) block.get("name"));
                    toolUse.setInput(block.get("input"));
                    result.setToolUse(toolUse);
                }
            }
        }

        return result;
    }

    /**
     * 继续执行function call
     */
    private FunctionCallResult continueWithFunctionResult(String systemPrompt, String userMessage,
                                                           ToolUseBlock toolUse, Object functionResult,
                                                           List<ToolDefinition> tools,
                                                           List<ChatMessage> history) {
        // 这里应该执行function call，然后将结果返回给模型
        // 由于实际执行需要FunctionRegistry，这里返回一个占位结果
        // 实际执行会在AgentExecutor中完成

        Map<String, Object> toolResult = new HashMap<>();
        toolResult.put("tool_use_id", toolUse.getId());
        toolResult.put("content", "Function call result placeholder");

        return new FunctionCallResult(null, toolUse, toolResult);
    }

    /**
     * 解析API响应（普通对话）— Anthropic Messages格式
     * 响应体: {"content":[{"type":"text","text":"..."},...], ...}
     * 用ObjectMapper解析,提取首个text块的文本(避免与thinking等块混淆)
     */
    private String parseResponse(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (content != null) {
                for (Map<String, Object> block : content) {
                    if ("text".equals(block.get("type"))) {
                        return (String) block.get("text");
                    }
                }
            }
            // 无text块,返回原始响应
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
            "当前为Mock模式，配置AI_API_KEY后可启用真实AI分析能力。\n" +
            "支持的功能：数据查询、趋势分析、异常诊断、经营简报生成。",
            question
        );
    }

    /**
     * 内部类：模型响应
     */
    private static class ModelResponse {
        private String text;
        private String stopReason;
        private ToolUseBlock toolUse;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getStopReason() { return stopReason; }
        public void setStopReason(String stopReason) { this.stopReason = stopReason; }
        public ToolUseBlock getToolUse() { return toolUse; }
        public void setToolUse(ToolUseBlock toolUse) { this.toolUse = toolUse; }
    }

    /**
     * 内部类：Tool Use Block
     */
    public static class ToolUseBlock {
        private String id;
        private String name;
        private Object input;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Object getInput() { return input; }
        public void setInput(Object input) { this.input = input; }
    }

    /**
     * 内部类：Function Call结果
     */
    public static class FunctionCallResult {
        private String text;
        private ToolUseBlock toolUse;
        private Map<String, Object> toolResult;

        public FunctionCallResult(String text, ToolUseBlock toolUse, Map<String, Object> toolResult) {
            this.text = text;
            this.toolUse = toolUse;
            this.toolResult = toolResult;
        }

        public String getText() { return text; }
        public ToolUseBlock getToolUse() { return toolUse; }
        public Map<String, Object> getToolResult() { return toolResult; }
    }

    /**
     * 内部类：Tool Definition
     */
    public static class ToolDefinition {
        private String name;
        private String description;
        private Map<String, Object> inputSchema;

        public ToolDefinition(String name, String description, Map<String, Object> inputSchema) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, Object> getInputSchema() { return inputSchema; }
    }

    /**
     * 内部类：Chat Message
     */
    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }
}
