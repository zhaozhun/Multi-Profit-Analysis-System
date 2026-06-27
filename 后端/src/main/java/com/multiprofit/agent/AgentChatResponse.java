package com.multiprofit.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Agent对话响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatResponse {

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * Agent图标
     */
    private String agentIcon;

    /**
     * 回答内容
     */
    private String answer;

    /**
     * 附加数据（图表、表格等）
     */
    private Map<String, Object> data;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 状态
     */
    private String status;

    /**
     * 置信度
     */
    private Integer confidence;

    /**
     * 建议的后续问题
     */
    private List<String> suggestions;
}
