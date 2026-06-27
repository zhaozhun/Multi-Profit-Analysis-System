package com.multiprofit.skill;

import java.util.List;

/**
 * 技能接口
 * 定义业务技能的标准接口
 */
public interface Skill {

    /**
     * 获取技能名称
     */
    String getName();

    /**
     * 获取技能描述
     */
    String getDescription();

    /**
     * 获取触发词列表
     */
    List<String> getTriggers();

    /**
     * 判断是否能处理该用户消息
     */
    boolean canHandle(String userMessage);

    /**
     * 执行技能
     */
    SkillResult execute(SkillContext context);
}
