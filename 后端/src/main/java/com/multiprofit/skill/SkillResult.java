package com.multiprofit.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 技能执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillResult {

    /**
     * 回答文本
     */
    private String answer;

    /**
     * 数据结果
     */
    private Map<String, Object> data;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 图表配置
     */
    private Map<String, Object> chartConfig;

    /**
     * 建议列表
     */
    private List<String> suggestions;

    /**
     * 置信度（0-100）
     */
    private int confidence;

    /**
     * 状态
     */
    private Status status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 状态枚举
     */
    public enum Status {
        SUCCESS,
        FAILED,
        PARTIAL
    }

    /**
     * 创建成功结果
     */
    public static SkillResult success(String answer, Map<String, Object> data) {
        return SkillResult.builder()
                .answer(answer)
                .data(data)
                .status(Status.SUCCESS)
                .confidence(85)
                .build();
    }

    /**
     * 创建成功结果（带图表）
     */
    public static SkillResult successWithChart(String answer, Map<String, Object> data,
                                                String chartType, Map<String, Object> chartConfig) {
        return SkillResult.builder()
                .answer(answer)
                .data(data)
                .chartType(chartType)
                .chartConfig(chartConfig)
                .status(Status.SUCCESS)
                .confidence(85)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static SkillResult failed(String errorMessage) {
        return SkillResult.builder()
                .status(Status.FAILED)
                .errorMessage(errorMessage)
                .confidence(0)
                .build();
    }

    /**
     * 创建部分成功结果
     */
    public static SkillResult partial(String answer, Map<String, Object> data, String errorMessage) {
        return SkillResult.builder()
                .answer(answer)
                .data(data)
                .status(Status.PARTIAL)
                .errorMessage(errorMessage)
                .confidence(60)
                .build();
    }
}
