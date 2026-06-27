package com.multiprofit.skill;

import com.multiprofit.agent.UserContext;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 技能上下文
 * 包含技能执行所需的所有上下文信息
 */
@Data
@Builder
public class SkillContext {

    /**
     * 用户消息
     */
    private String userMessage;

    /**
     * 期间（YYYY-MM格式）
     */
    private String period;

    /**
     * 维度类型
     */
    private String dimType;

    /**
     * 用户上下文（权限信息）
     */
    private UserContext userContext;

    /**
     * 额外参数
     */
    private Map<String, Object> params;

    /**
     * 获取参数值
     */
    public Object getParam(String key) {
        return params != null ? params.get(key) : null;
    }

    /**
     * 获取字符串参数
     */
    public String getStringParam(String key) {
        Object value = getParam(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取整数参数
     */
    public Integer getIntegerParam(String key) {
        Object value = getParam(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取双精度参数
     */
    public Double getDoubleParam(String key) {
        Object value = getParam(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取布尔参数
     */
    public Boolean getBooleanParam(String key) {
        Object value = getParam(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
}
