package com.multiprofit.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Function Call结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionCallResult {

    /**
     * AI生成的文本回复
     */
    private String text;

    /**
     * 函数调用结果（如果有）
     */
    private Map<String, Object> functionResult;

    /**
     * 是否包含函数调用
     */
    private boolean hasFunctionCall;

    /**
     * 调用的函数名
     */
    private String functionName;

    /**
     * 函数调用ID
     */
    private String toolUseId;
}
