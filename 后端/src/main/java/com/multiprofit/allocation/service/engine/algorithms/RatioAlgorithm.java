package com.multiprofit.allocation.service.engine.algorithms;

import com.multiprofit.allocation.service.engine.AllocationAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 比例分摊算法
 * 按单一因子占比分摊
 * 公式：分摊额 = 总额 × (因子值 / 因子总和)
 */
@Slf4j
@Component
public class RatioAlgorithm implements AllocationAlgorithm {

    private static final String CODE = "RATIO";
    private static final String NAME = "比例分摊";
    private static final String DESCRIPTION = "按单一因子占比分摊，公式：分摊额 = 总额 × (因子值 / 因子总和)";

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
            new AlgorithmParam("factor_code", "string", true, "分摊因子编码", null),
            new AlgorithmParam("precision", "integer", false, "计算精度(小数位数)", 2)
        );
    }

    @Override
    public List<AllocationResult> execute(AllocationContext context) {
        BigDecimal totalCost = context.getTotalCost();
        Map<String, BigDecimal> factorValues = context.getFactorValues();
        Integer precision = context.getParam("precision", Integer.class);
        if (precision == null) precision = 2;

        if (factorValues == null || factorValues.isEmpty()) {
            log.warn("因子值为空，无法执行比例分摊");
            return Collections.emptyList();
        }

        // 计算因子总和
        BigDecimal factorTotal = factorValues.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (factorTotal.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("因子总和为零，无法执行比例分摊");
            return Collections.emptyList();
        }

        // 计算每个目标的分摊金额
        List<AllocationResult> results = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : factorValues.entrySet()) {
            String targetDimCode = entry.getKey();
            BigDecimal factorValue = entry.getValue();

            // 计算比例
            BigDecimal ratio = factorValue.divide(factorTotal, 10, RoundingMode.HALF_UP);

            // 计算分摊金额
            BigDecimal allocatedAmount = totalCost.multiply(ratio)
                .setScale(precision, RoundingMode.HALF_UP);

            // 构建计算详情
            Map<String, Object> calcDetails = new HashMap<>();
            calcDetails.put("factorCode", context.getParam("factor_code", String.class));
            calcDetails.put("factorValue", factorValue);
            calcDetails.put("factorTotal", factorTotal);
            calcDetails.put("ratio", ratio);
            calcDetails.put("formula", "totalCost * (factorValue / factorTotal)");
            calcDetails.put("calculation", String.format("%s * (%s / %s)", totalCost, factorValue, factorTotal));

            AllocationResult result = AllocationResult.builder()
                .targetDimCode(targetDimCode)
                .targetDimType(context.getTargetDimType())
                .allocatedAmount(allocatedAmount)
                .ratio(ratio)
                .factorValue(factorValue)
                .calcDetails(calcDetails)
                .build();

            results.add(result);
        }

        log.info("比例分摊完成: 成本类型={}, 期间={}, 目标数量={}, 总金额={}",
            context.getCostType(), context.getPeriod(), results.size(), totalCost);

        return results;
    }

    @Override
    public ValidationResult validateParams(Map<String, Object> params) {
        List<String> errors = new ArrayList<>();

        // 校验因子编码
        if (params == null || !params.containsKey("factor_code")) {
            errors.add("缺少必填参数: factor_code");
        } else if (!(params.get("factor_code") instanceof String)) {
            errors.add("参数类型错误: factor_code 应为字符串");
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
