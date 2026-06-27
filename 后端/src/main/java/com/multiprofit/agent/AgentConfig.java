package com.multiprofit.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent配置 - 从MD文件加载
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfig {

    /**
     * Agent名称
     */
    private String name;

    /**
     * 图标
     */
    private String icon;

    /**
     * 描述
     */
    private String description;

    /**
     * 触发词列表
     */
    private List<String> triggers;

    /**
     * 可用工具列表
     */
    private List<String> tools;

    /**
     * 最大迭代次数
     */
    private int maxIterations;

    /**
     * 系统提示词（从MD文件提取）
     */
    private String systemPrompt;
}
