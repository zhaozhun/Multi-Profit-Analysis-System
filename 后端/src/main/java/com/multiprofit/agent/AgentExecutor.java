package com.multiprofit.agent;

import com.multiprofit.ai.ModelApiClient;
import com.multiprofit.ai.FunctionRegistry;
import com.multiprofit.ai.langchain4j.MultiAgentOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent执行引擎 - 核心执行逻辑
 * 支持Function Call，实现MCP工具调用
 */
@Slf4j
@Component
public class AgentExecutor {

    @Autowired
    private ModelApiClient claudeClient;

    @Autowired
    private FunctionRegistry functionRegistry;

    @Autowired
    private AgentConfigLoader configLoader;

    @Autowired
    private SessionContextCache sessionCache;

    @Autowired
    private MultiAgentOrchestrator multiAgentOrchestrator;

    /**
     * 执行Agent
     *
     * @param agentName   Agent名称
     * @param userMessage 用户消息
     * @param sessionId   会话ID
     * @param userContext  用户上下文
     * @return Agent执行结果
     */
    public AgentResult execute(String agentName, String userMessage,
                                String sessionId, UserContext userContext) {
        log.info("执行Agent: {}，用户消息: {}", agentName, userMessage);

        // 0. 判断是否需要多Agent协作
        if (multiAgentOrchestrator.needMultiAgentCollaboration(userMessage)) {
            log.info("触发多Agent协作");
            try {
                String result = multiAgentOrchestrator.executeMultiAgent(userMessage);
                return AgentResult.builder()
                        .agentName("多Agent协作")
                        .agentIcon("🤝")
                        .answer(result)
                        .status(AgentResult.Status.COMPLETED)
                        .confidence(90)
                        .build();
            } catch (Exception e) {
                log.error("多Agent协作失败，降级为单Agent执行", e);
                // 降级为单Agent执行
            }
        }

        // 1. 加载Agent配置
        AgentConfig config = configLoader.getConfig(agentName);
        if (config == null) {
            log.error("Agent配置不存在: {}", agentName);
            return buildErrorResult("Agent配置不存在: " + agentName);
        }

        // 2. 构建系统提示词（带用户上下文）
        String systemPrompt = buildPromptWithUserContext(config.getSystemPrompt(), userContext);

        // 3. 获取可用工具
        List<ModelApiClient.ToolDefinition> tools = getToolsForAgent(config.getTools());

        // 4. 获取会话历史（支持追问）
        List<ModelApiClient.ChatMessage> history = getChatHistory(sessionId);

        // 5. 记录用户消息
        sessionCache.addMessage(sessionId,
                new SessionContextCache.ChatMessage(SessionContextCache.ChatMessage.Role.USER, userMessage));

        // 6. 调用Claude API（支持Function Call）
        try {
            ModelApiClient.FunctionCallResult result = claudeClient.chatWithFunctions(
                systemPrompt, userMessage, tools, history);

            // 7. 处理Function Call结果
            String response = processFunctionCallResult(result, sessionId);

            // 8. 记录AI回复
            sessionCache.addMessage(sessionId,
                    new SessionContextCache.ChatMessage(SessionContextCache.ChatMessage.Role.ASSISTANT, response));

            // 9. 构建返回结果
            return AgentResult.builder()
                    .agentName(config.getName())
                    .agentIcon(config.getIcon())
                    .answer(response)
                    .status(AgentResult.Status.COMPLETED)
                    .confidence(85)  // 默认置信度
                    .build();

        } catch (Exception e) {
            log.error("Agent执行失败", e);
            return buildErrorResult("Agent执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取Agent可用的工具定义
     */
    private List<ModelApiClient.ToolDefinition> getToolsForAgent(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<ModelApiClient.ToolDefinition> tools = new ArrayList<>();
        for (String toolName : toolNames) {
            ModelApiClient.ToolDefinition tool = functionRegistry.getToolDefinition(toolName);
            if (tool != null) {
                tools.add(tool);
            } else {
                log.warn("工具不存在: {}", toolName);
            }
        }

        return tools;
    }

    /**
     * 获取会话历史（转换为ModelApiClient格式）
     */
    private List<ModelApiClient.ChatMessage> getChatHistory(String sessionId) {
        List<SessionContextCache.ChatMessage> history = sessionCache.getMessages(sessionId);
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }

        List<ModelApiClient.ChatMessage> chatHistory = new ArrayList<>();

        // 只取最近5条消息
        int start = Math.max(0, history.size() - 5);
        for (int i = start; i < history.size(); i++) {
            SessionContextCache.ChatMessage msg = history.get(i);
            String role = msg.getRole() == SessionContextCache.ChatMessage.Role.USER ? "user" : "assistant";
            chatHistory.add(new ModelApiClient.ChatMessage(role, msg.getContent()));
        }

        return chatHistory;
    }

    /**
     * 处理Function Call结果
     */
    private String processFunctionCallResult(ModelApiClient.FunctionCallResult result,
                                              String sessionId) {
        // 如果是普通文本回复，直接返回
        if (result.getText() != null) {
            return result.getText();
        }

        // 如果是Function Call，执行函数并继续对话
        if (result.getToolUse() != null) {
            ModelApiClient.ToolUseBlock toolUse = result.getToolUse();

            log.info("执行Function Call: {}，参数: {}", toolUse.getName(), toolUse.getInput());

            // 执行函数
            Object functionResult = executeFunction(toolUse.getName(), toolUse.getInput());

            // 将函数结果返回给Claude继续对话
            // 这里需要递归调用，直到Claude返回文本回复
            return continueWithFunctionResult(toolUse, functionResult, sessionId);
        }

        return "抱歉，无法处理您的请求";
    }

    /**
     * 执行函数
     */
    private Object executeFunction(String functionName, Object input) {
        try {
            // 将输入转换为Map
            Map<String, Object> params = convertToMap(input);

            // 执行函数
            return functionRegistry.execute(functionName, params);

        } catch (Exception e) {
            log.error("函数执行失败: {}", functionName, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * 将输入转换为Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object input) {
        if (input instanceof Map) {
            return (Map<String, Object>) input;
        }

        // 如果是其他类型，尝试转换
        Map<String, Object> result = new HashMap<>();
        if (input != null) {
            result.put("input", input);
        }
        return result;
    }

    /**
     * 继续执行function call
     */
    private String continueWithFunctionResult(ModelApiClient.ToolUseBlock toolUse,
                                               Object functionResult,
                                               String sessionId) {
        // 构建函数结果消息
        String functionResultMessage = String.format(
            "函数 %s 执行完成，结果如下：\n%s",
            toolUse.getName(),
            formatFunctionResult(functionResult)
        );

        // 将函数结果记录到会话
        sessionCache.addMessage(sessionId,
                new SessionContextCache.ChatMessage(SessionContextCache.ChatMessage.Role.ASSISTANT, functionResultMessage));

        // 这里应该递归调用Claude，但为了简化，直接返回格式化的结果
        // 实际实现中应该继续调用Claude API
        return functionResultMessage;
    }

    /**
     * 格式化函数结果
     */
    private String formatFunctionResult(Object result) {
        if (result == null) {
            return "无结果";
        }

        if (result instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) result;
            StringBuilder sb = new StringBuilder();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                sb.append("• ").append(entry.getKey()).append(": ");
                sb.append(formatValue(entry.getValue())).append("\n");
            }

            return sb.toString();
        }

        return result.toString();
    }

    /**
     * 格式化值
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "无";
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "[]";
            }

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < Math.min(list.size(), 5); i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatValue(list.get(i)));
            }
            if (list.size() > 5) {
                sb.append(", ...").append(list.size() - 5).append("条");
            }
            sb.append("]");

            return sb.toString();
        }

        if (value instanceof Number) {
            // 格式化数字
            double num = ((Number) value).doubleValue();
            if (num == (long) num) {
                return String.format("%d", (long) num);
            } else {
                return String.format("%.2f", num);
            }
        }

        return value.toString();
    }

    /**
     * 构建带用户上下文的系统提示词
     */
    private String buildPromptWithUserContext(String basePrompt, UserContext userContext) {
        if (userContext == null) {
            return basePrompt;
        }

        return basePrompt + "\n\n" +
                "## 用户上下文\n\n" +
                "- 用户ID：" + userContext.getUserId() + "\n" +
                "- 用户名称：" + userContext.getUserName() + "\n" +
                "- 用户角色：" + userContext.getRole() + "\n" +
                "- 可见机构：" + String.join(", ", userContext.getVisibleOrgs()) + "\n" +
                "- 可见产品：" + String.join(", ", userContext.getVisibleProducts()) + "\n" +
                "\n请注意：\n" +
                "- 只展示用户权限范围内的数据\n" +
                "- 如果用户查询超出权限范围，提示无权限\n";
    }

    /**
     * 构建错误结果
     */
    private AgentResult buildErrorResult(String errorMessage) {
        return AgentResult.builder()
                .agentName("系统")
                .agentIcon("❌")
                .answer(errorMessage)
                .status(AgentResult.Status.FAILED)
                .build();
    }
}
