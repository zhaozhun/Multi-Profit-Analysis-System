package com.multiprofit.allocation.service.engine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 分摊算法接口
 * 所有分摊算法必须实现此接口
 */
public interface AllocationAlgorithm {

    /**
     * 获取算法编码
     */
    String getCode();

    /**
     * 获取算法名称
     */
    String getName();

    /**
     * 获取算法描述
     */
    String getDescription();

    /**
     * 获取算法参数定义
     */
    List<AlgorithmParam> getParamDefinitions();

    /**
     * 执行分摊计算
     *
     * @param context 分摊上下文
     * @return 分摊结果列表
     */
    List<AllocationResult> execute(AllocationContext context);

    /**
     * 校验参数
     */
    ValidationResult validateParams(Map<String, Object> params);

    /**
     * 算法参数定义
     */
    record AlgorithmParam(
        String name,
        String type,
        boolean required,
        String description,
        Object defaultValue
    ) {}

    /**
     * 分摊结果
     */
    class AllocationResult {
        private String targetDimCode;
        private String targetDimType;
        private BigDecimal allocatedAmount;
        private BigDecimal ratio;
        private BigDecimal factorValue;
        private Map<String, BigDecimal> factorValues; // 多因子时使用
        private Map<String, Object> calcDetails; // 计算详情

        // Builder模式
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final AllocationResult result = new AllocationResult();

            public Builder targetDimCode(String code) {
                result.targetDimCode = code;
                return this;
            }

            public Builder targetDimType(String type) {
                result.targetDimType = type;
                return this;
            }

            public Builder allocatedAmount(BigDecimal amount) {
                result.allocatedAmount = amount;
                return this;
            }

            public Builder ratio(BigDecimal ratio) {
                result.ratio = ratio;
                return this;
            }

            public Builder factorValue(BigDecimal value) {
                result.factorValue = value;
                return this;
            }

            public Builder factorValues(Map<String, BigDecimal> values) {
                result.factorValues = values;
                return this;
            }

            public Builder calcDetails(Map<String, Object> details) {
                result.calcDetails = details;
                return this;
            }

            public AllocationResult build() {
                return result;
            }
        }

        // Getters
        public String getTargetDimCode() { return targetDimCode; }
        public String getTargetDimType() { return targetDimType; }
        public BigDecimal getAllocatedAmount() { return allocatedAmount; }
        public BigDecimal getRatio() { return ratio; }
        public BigDecimal getFactorValue() { return factorValue; }
        public Map<String, BigDecimal> getFactorValues() { return factorValues; }
        public Map<String, Object> getCalcDetails() { return calcDetails; }
    }

    /**
     * 分摊上下文
     */
    class AllocationContext {
        private BigDecimal totalCost;
        private String costType;
        private String period;
        private String sourceDimType;
        private String sourceDimCode;
        private String targetDimType;
        private String targetDimFilter;
        private Map<String, BigDecimal> factorValues; // 单因子: dimCode -> factorValue
        private Map<String, Map<String, BigDecimal>> multiFactorValues; // 多因子: dimCode -> (factorCode -> factorValue)
        private Map<String, BigDecimal> factorWeights; // 因子权重: factorCode -> weight
        private Map<String, Object> algorithmParams; // 算法参数
        private Map<String, Object> variables; // 额外变量

        // Builder模式
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final AllocationContext context = new AllocationContext();

            public Builder totalCost(BigDecimal cost) {
                context.totalCost = cost;
                return this;
            }

            public Builder costType(String type) {
                context.costType = type;
                return this;
            }

            public Builder period(String period) {
                context.period = period;
                return this;
            }

            public Builder sourceDimType(String type) {
                context.sourceDimType = type;
                return this;
            }

            public Builder sourceDimCode(String code) {
                context.sourceDimCode = code;
                return this;
            }

            public Builder targetDimType(String type) {
                context.targetDimType = type;
                return this;
            }

            public Builder targetDimFilter(String filter) {
                context.targetDimFilter = filter;
                return this;
            }

            public Builder factorValues(Map<String, BigDecimal> values) {
                context.factorValues = values;
                return this;
            }

            public Builder multiFactorValues(Map<String, Map<String, BigDecimal>> values) {
                context.multiFactorValues = values;
                return this;
            }

            public Builder factorWeights(Map<String, BigDecimal> weights) {
                context.factorWeights = weights;
                return this;
            }

            public Builder algorithmParams(Map<String, Object> params) {
                context.algorithmParams = params;
                return this;
            }

            public Builder variables(Map<String, Object> vars) {
                context.variables = vars;
                return this;
            }

            public AllocationContext build() {
                return context;
            }
        }

        // Getters
        public BigDecimal getTotalCost() { return totalCost; }
        public String getCostType() { return costType; }
        public String getPeriod() { return period; }
        public String getSourceDimType() { return sourceDimType; }
        public String getSourceDimCode() { return sourceDimCode; }
        public String getTargetDimType() { return targetDimType; }
        public String getTargetDimFilter() { return targetDimFilter; }
        public Map<String, BigDecimal> getFactorValues() { return factorValues; }
        public Map<String, Map<String, BigDecimal>> getMultiFactorValues() { return multiFactorValues; }
        public Map<String, BigDecimal> getFactorWeights() { return factorWeights; }
        public Map<String, Object> getAlgorithmParams() { return algorithmParams; }
        public Map<String, Object> getVariables() { return variables; }

        /**
         * 获取参数值
         */
        @SuppressWarnings("unchecked")
        public <T> T getParam(String name, Class<T> type) {
            if (algorithmParams == null) return null;
            Object value = algorithmParams.get(name);
            if (value == null) return null;
            return (T) value;
        }
    }

    /**
     * 参数校验结果
     */
    class ValidationResult {
        private boolean valid;
        private List<String> errors;

        public static ValidationResult success() {
            ValidationResult result = new ValidationResult();
            result.valid = true;
            return result;
        }

        public static ValidationResult failure(List<String> errors) {
            ValidationResult result = new ValidationResult();
            result.valid = false;
            result.errors = errors;
            return result;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }
}
