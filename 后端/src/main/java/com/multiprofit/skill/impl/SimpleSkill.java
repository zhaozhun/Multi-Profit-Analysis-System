package com.multiprofit.skill.impl;

import com.multiprofit.skill.Skill;
import com.multiprofit.skill.SkillContext;
import com.multiprofit.skill.SkillResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 简单技能基类
 * 提供通用的技能实现框架
 */
@Slf4j
public abstract class SimpleSkill implements Skill {

    @Override
    public boolean canHandle(String userMessage) {
        if (userMessage == null) return false;
        String message = userMessage.toLowerCase();
        return getTriggers().stream()
            .anyMatch(trigger -> message.contains(trigger));
    }

    /**
     * 提取double值
     */
    protected double extractDouble(java.util.Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0;
        Object value = map.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 提取String值
     */
    protected String extractString(java.util.Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return "";
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * 提取List值
     */
    @SuppressWarnings("unchecked")
    protected List<java.util.Map<String, Object>> extractList(java.util.Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return java.util.Collections.emptyList();
        Object value = map.get(key);
        if (value instanceof List) return (List<java.util.Map<String, Object>>) value;
        return java.util.Collections.emptyList();
    }

    /**
     * 格式化金额
     */
    protected String formatAmount(double amount) {
        if (Math.abs(amount) >= 100000000) {
            return String.format("%.2f亿", amount / 100000000);
        } else if (Math.abs(amount) >= 10000) {
            return String.format("%.2f万", amount / 10000);
        } else {
            return String.format("%.2f", amount);
        }
    }

    /**
     * 计算基期
     */
    protected String calculateBasePeriod(String currentPeriod, String compareType) {
        String[] parts = currentPeriod.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        if ("YOY".equalsIgnoreCase(compareType)) {
            return (year - 1) + "-" + String.format("%02d", month);
        } else {
            if (month == 1) {
                return (year - 1) + "-12";
            } else {
                return year + "-" + String.format("%02d", month - 1);
            }
        }
    }
}
