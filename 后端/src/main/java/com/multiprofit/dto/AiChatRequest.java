package com.multiprofit.dto;

import lombok.Data;

/**
 * AI对话请求
 */
@Data
public class AiChatRequest {
    /** 用户问题 */
    private String message;
    /** 上下文（可选，如当前筛选条件） */
    private String context;
}
