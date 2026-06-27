package com.multiprofit.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * Agent对话请求
 */
@Data
public class AgentChatRequest {

    /**
     * 用户消息
     */
    @NotBlank(message = "消息不能为空")
    private String message;

    /**
     * 会话ID（可选，用于追问场景）
     */
    private String sessionId;

    /**
     * 上下文参数（可选）
     */
    private Map<String, Object> context;
}
