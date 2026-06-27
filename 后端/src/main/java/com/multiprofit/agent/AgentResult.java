package com.multiprofit.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Agent执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * 图标
     */
    private String agentIcon;

    /**
     * 回答文本
     */
    private String answer;

    /**
     * 附加数据
     */
    private Map<String, Object> data;

    /**
     * 执行步骤
     */
    private List<AgentStep> steps;

    /**
     * 执行状态
     */
    private Status status;

    /**
     * 置信度（0-100）
     */
    private Integer confidence;

    /**
     * 状态枚举
     */
    public enum Status {
        RUNNING, COMPLETED, FAILED
    }

    /**
     * Agent步骤
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentStep {
        private String id;
        private String name;
        private String tool;
        private StepStatus status;
        private Long duration;
        private String output;

        public enum StepStatus {
            PENDING, RUNNING, COMPLETED, FAILED
        }
    }
}
