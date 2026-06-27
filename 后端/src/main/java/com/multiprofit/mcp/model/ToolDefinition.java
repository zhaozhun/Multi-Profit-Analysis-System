package com.multiprofit.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具定义 - Function Call使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 输入参数JSON Schema
     */
    private Map<String, Object> inputSchema;

    /**
     * 构建工具定义
     */
    public static ToolDefinition of(String name, String description, Map<String, Object> inputSchema) {
        return ToolDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .build();
    }
}
