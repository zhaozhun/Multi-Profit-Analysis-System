package com.multiprofit.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 技能注册中心
 * 管理所有业务技能，提供技能匹配和执行功能
 */
@Slf4j
@Component
public class SkillRegistry {

    @Autowired
    private List<Skill> skills;

    @Autowired
    private SkillConfigLoader configLoader;

    /**
     * 根据用户消息匹配技能
     */
    public Skill matchSkill(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return null;
        }

        // 优先使用配置文件匹配
        SkillConfig config = configLoader.matchByTrigger(userMessage);
        if (config != null) {
            return skills.stream()
                .filter(s -> s.getName().equals(config.getName()))
                .findFirst()
                .orElse(null);
        }

        // 降级到Java实现匹配
        return skills.stream()
            .filter(skill -> skill.canHandle(userMessage))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取所有可用技能
     */
    public List<Skill> getAllSkills() {
        return skills;
    }

    /**
     * 获取技能摘要信息
     */
    public List<SkillSummary> getSkillSummaries() {
        return skills.stream()
            .map(skill -> {
                SkillConfig config = configLoader.getConfig(skill.getName());
                return new SkillSummary(
                    skill.getName(),
                    config != null ? config.getIcon() : "📊",
                    config != null ? config.getDescription() : skill.getDescription(),
                    config != null ? config.getTriggers() : skill.getTriggers()
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * 执行技能
     */
    public SkillResult executeSkill(String skillName, SkillContext context) {
        Skill skill = skills.stream()
            .filter(s -> s.getName().equals(skillName))
            .findFirst()
            .orElse(null);

        if (skill == null) {
            log.error("技能不存在: {}", skillName);
            return SkillResult.failed("技能不存在: " + skillName);
        }

        try {
            log.info("执行技能: {}", skillName);
            return skill.execute(context);
        } catch (Exception e) {
            log.error("技能执行失败: {}", skillName, e);
            return SkillResult.failed("技能执行失败: " + e.getMessage());
        }
    }

    /**
     * 根据名称获取技能
     */
    public Skill getSkill(String skillName) {
        return skills.stream()
            .filter(s -> s.getName().equals(skillName))
            .findFirst()
            .orElse(null);
    }

    /**
     * 检查技能是否存在
     */
    public boolean hasSkill(String skillName) {
        return skills.stream()
            .anyMatch(s -> s.getName().equals(skillName));
    }

    /**
     * 获取技能配置
     */
    public SkillConfig getSkillConfig(String skillName) {
        return configLoader.getConfig(skillName);
    }

    /**
     * 获取所有技能配置
     */
    public List<SkillConfig> getAllSkillConfigs() {
        return configLoader.getAllConfigs();
    }

    /**
     * 技能摘要信息
     */
    public static class SkillSummary {
        private final String name;
        private final String icon;
        private final String description;
        private final List<String> triggers;

        public SkillSummary(String name, String icon, String description, List<String> triggers) {
            this.name = name;
            this.icon = icon;
            this.description = description;
            this.triggers = triggers;
        }

        public String getName() { return name; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
        public List<String> getTriggers() { return triggers; }
    }
}
