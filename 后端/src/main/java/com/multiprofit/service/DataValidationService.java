package com.multiprofit.service;

import java.util.List;
import java.util.Map;

/**
 * 数据校验服务接口
 */
public interface DataValidationService {

    /**
     * 校验单条业务数据
     * @return 校验结果列表，空列表表示通过
     */
    List<ValidationResult> validate(Map<String, Object> record);

    /**
     * 批量校验
     */
    Map<String, List<ValidationResult>> batchValidate(List<Map<String, Object>> records);

    /**
     * 利润公式平衡校验
     */
    ValidationResult validateProfitFormula(Map<String, Object> record);

    /**
     * 同比环比异常检测
     */
    List<ValidationResult> detectAnomaly(String period, String dimType);

    /**
     * 异常结果写入预警记录
     */
    void saveAlertRecords(List<ValidationResult> anomalies);

    /**
     * 校验结果
     */
    class ValidationResult {
        public enum Level { ERROR, WARNING, INFO }
        private Level level;
        private String code;
        private String message;
        private String field;
        private Object expected;
        private Object actual;
        private String aiAnalysis;  // AI根因分析
        private java.math.BigDecimal anomalyValue;  // 异常幅度

        public ValidationResult(Level level, String code, String message) {
            this.level = level;
            this.code = code;
            this.message = message;
        }

        // Getters & Setters
        public Level getLevel() { return level; }
        public void setLevel(Level level) { this.level = level; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public Object getExpected() { return expected; }
        public void setExpected(Object expected) { this.expected = expected; }
        public Object getActual() { return actual; }
        public void setActual(Object actual) { this.actual = actual; }
        public String getAiAnalysis() { return aiAnalysis; }
        public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }
        public java.math.BigDecimal getAnomalyValue() { return anomalyValue; }
        public void setAnomalyValue(java.math.BigDecimal anomalyValue) { this.anomalyValue = anomalyValue; }
    }
}
