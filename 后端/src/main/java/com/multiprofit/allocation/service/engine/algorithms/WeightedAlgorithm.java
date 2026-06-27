package com.multiprofit.allocation.service.engine.algorithms;

import com.multiprofit.allocation.service.engine.AllocationAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 加权分摊算法
 * 按多因子加权分摊
 * 公式：分摊额 = 总额 × Σ(因子值 × 权重)
 */
@Slf4j
@Component
public class WeightedAlgorithm implements AllocationAlgorithm {

    private static final String CODE = "WEIGHTED";
    private static final String NAME = "加权分摊";
    private static final String DESCRIPTION = "按多因子加权分摊，公式：分摊额 = 总额 × Σ(因子值 × 权重)";

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public List<AlgorithmParam> getParamDefinitions() {
        return List.of(
            new AlgorithmParam("factors", "array", true, "因子列表及权重", null),
            new AlgorithmParam("precision", "integer", false, "计算精度(小数位数)", 2)
        );
    }

    @Override
    public List<AllocationResult> execute(AllocationContext context) {
        BigDecimal totalCost = context.getTotalCost();
        Map<String, Map<String, BigDecimal>> multiFactorValues = context.getMultiFactorValues();
        Map<String, BigDecimal> weights = context.getFactorWeights();
        Integer precision = context.getParam("precision", Integer.class);
        if (precision == null) precision = 2;

        if (multiFactorValues == null || multiFactorValues.isEmpty()) {
            log.warn("多因子值为空，无法执行加权分摊");
            return Collections.emptyList();
        }

        if (weights == null || weights.isEmpty()) {
            log.warn("因子权重为空，无法执行加权分摊");
            return Collections.emptyList();
        }

        // 计算每个目标的加权因子值
        Map<String, BigDecimal> weightedFactors = new LinkedHashMap<>();
        Map<String, Map<String, BigDecimal>> factorDetails = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, BigDecimal>> entry : multiFactorValues.entrySet()) {
            String targetDimCode = entry.getKey();
            Map<String, BigDecimal> factors = entry.getValue();

            BigDecimal weightedValue = BigDecimal.ZERO;
            Map<String, BigDecimal> detail = new LinkedHashMap<>();

            for (Map.Entry<String, BigDecimal> factorEntry : factors.entrySet()) {
                String factorCode = factorEntry.getKey();
                BigDecimal factorValue = factorEntry.getValue();
                BigDecimal weight = weights.getOrDefault(factorCode, BigDecimal.ZERO);

                // 计算加权值
                BigDecimal weighted = factorValue.multiply(weight);
                weightedValue = weightedValue.add(weighted);

                detail.put(factorCode + "_value", factorValue);
                detail.put(factorCode + "_weight", weight);
                detail.put(factorCode + "_weighted", weighted);
            }

            weightedFactors.put(targetDimCode, weightedValue);
            factorDetails.put(targetDimCode, detail);
        }

        // 计算加权因子总和
        BigDecimal factorTotal = weightedFactors.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (factorTotal.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("加权因子总和为零，无法执行加权分摊");
            return Collections.emptyList();
        }

        // 计算每个目标的分摊金额
        List<AllocationResult> results = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : weightedFactors.entrySet()) {
            String targetDimCode = entry.getKey();
            BigDecimal weightedValue = entry.getValue();

            // 计算比例
            BigDecimal ratio = weightedValue.divide(factorTotal, 10, RoundingMode.HALF_UP);

            // 计算分摊金额
            BigDecimal allocatedAmount = totalCost.multiply(ratio)
                .setScale(precision, RoundingMode.HALF_UP);

            // 构建计算详情
            Map<String, Object> calcDetails = new HashMap<>();
            calcDetails.put("weightedValue", weightedValue);
            calcDetails.put("factorTotal", factorTotal);
            calcDetails.put("ratio", ratio);
            calcDetails.put("factorDetails", factorDetails.get(targetDimCode));
            calcDetails.put("formula", "totalCost * (weightedValue / factorTotal)");
            calcDetails.put("calculation", String.format("%s * (%s / %s)", totalCost, weightedValue, factorTotal));

            AllocationResult result = AllocationResult.builder()
                .targetDimCode(targetDimCode)
                .targetDimType(context.getTargetDimType())
                .allocatedAmount(allocatedAmount)
                .ratio(ratio)
                .factorValue(weightedValue)
                .factorValues(factorDetails.get(targetDimCode))
                .calcDetails(calcDetails)
                .build();

            results.add(result);
        }

        log.info("加权分摊完成: 成本类型={}, 期间={}, 目标数量={}, 总金额={}",
            context.getCostType(), context.getPeriod(), results.size(), totalCost);

        return results;
    }

    @Override
    public ValidationResult validateParams(Map<String, Object> params) {
        List<String> errors = new ArrayList<>();

        // 校验因子列表
        if (params == null || !params.containsKey("factors")) {
            errors.add("缺少必填参数: factors");
        } else {
            Object factors = params.get("factors");
            if (!(factors instanceof List)) {
                errors.add("参数类型错误: factors 应为数组");
            } else {
                List<?> factorList = (List<?>) factors;
                for (int i = 0; i < factorList.size(); i++) {
                    Object factor = factorList.get(i);
                    if (factor instanceof Map) {
                        Map<?, ?> factorMap = (Map<?, ?>) factor;
                        if (!factorMap.containsKey("code") || !factorMap.containsKey("weight")) {
                            errors.add("因子配置错误: 第 " + (i + 1) + " 个因子缺少 code 或 weight");
                        }
                    } else {
                        errors.add("因子配置错误: 第 " + (i + 1) + " 个因子格式不正确");
                    }
                }
            }
        }

        // 校验精度
        if (params != null && params.containsKey("precision")) {
            Object precision = params.get("precision");
            if (precision instanceof Integer) {
                int p = (Integer) precision;
                if (p < 0 || p > 10) {
                    errors.add("参数范围错误: precision 应在 0-10 之间");
                }
            } else {
                errors.add("参数类型错误: precision 应为整数");
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        } else {
            return ValidationResult.failure(errors);
        }
    }
}
