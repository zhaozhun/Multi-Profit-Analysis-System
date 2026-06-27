package com.multiprofit.mcp;

import java.lang.annotation.*;

/**
 * MCP工具注解
 * 用于标记MCP Server中的工具方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpTool {
    /**
     * 工具名称
     */
    String name();

    /**
     * 工具描述
     */
    String description();
}
