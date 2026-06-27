package com.multiprofit.skill;

import lombok.Data;

import java.util.List;

/**
 * 技能配置
 * 从MD文件解析的技能配置信息
 */
@Data
public class SkillConfig {

    /**
     * 技能名称
     */
    private String name;

    /**
     * 技能图标
     */
    private String icon;

    /**
     * 技能描述
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
     * 优先级（数字越小优先级越高）
     */
    private int priority;

    /**
     * 技能说明（从MD文件提取）
     */
    private String documentation;
}
