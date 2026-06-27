package com.multiprofit.allocation.service.engine.algorithms;

import com.multiprofit.allocation.service.engine.AllocationAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 直接归属算法
 * 直接计入目标维度，无需计算分摊比例
 * 适用于可直接归因的成本
 */
@Slf4j
@Component
public class DirectAlgorithm implements AllocationAlgorithm {

    private static final String CODE = "DIRECT";
    private static final String NAME = "直接归属";
    private static final String DESCRIPTION = "直接计入目标维度，无需计算分摊比例，适用于可直接归因的成本";

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
            new AlgorithmParam("target_code", "string", true, "目标维度编码", null),
            new AlgorithmParam("precision", "integer", false, "计算精度(小数位数)", 2)
        );
    }

    @Override
    public List<AllocationResult> execute(AllocationContext context) {
        BigDecimal totalCost = context.getTotalCost();
        Integer precision = context.getParam("precision", Integer.class);
        if (precision == null) precision = 2;

        // 获取目标维度编码
        String targetCode = context.getParam("target_code", String.class);
        if (targetCode == null) {
            // 如果未指定目标编码，使用来源编码
            targetCode = context.getSourceDimCode();
        }

        if (targetCode == null) {
            log.warn("未指定目标维度编码，无法执行直接归属");
            return Collections.emptyList();
        }

        // 直接归属，比例为1
        BigDecimal ratio = BigDecimal.ONE;
        BigDecimal allocatedAmount = totalCost.setScale(precision, RoundingMode.HALF_UP);

        // 构建计算详情
        Map<String, Object> calcDetails = new HashMap<>();
        calcDetails.put("algorithm", "DIRECT");
        calcDetails.put("targetCode", targetCode);
        calcDetails.put("totalCost", totalCost);
        calcDetails.put("ratio", ratio);
        calcDetails.put("formula", "直接归属，全额计入目标维度");

        AllocationResult result = AllocationResult.builder()
            .targetDimCode(targetCode)
            .targetDimType(context.getTargetDimType())
            .allocatedAmount(allocatedAmount)
            .ratio(ratio)
            .factorValue(totalCost)
            .calcDetails(calcDetails)
            .build();

        List<AllocationResult> results = List.of(result);

        log.info("直接归属完成: 成本类型={}, 期间={}, 目标={}, 金额={}",
            context.getCostType(), context.getPeriod(), targetCode, allocatedAmount);

        return results;
    }

    @Override
    public ValidationResult validateParams(Map<String, Object> params) {
        List<String> errors = new ArrayList<>();

        // 校验目标编码（可选，如果未提供则使用来源编码）
        if (params != null && params.containsKey("target_code")) {
            if (!(params.get("target_code") instanceof String)) {
                errors.add("参数类型错误: target_code 应为字符串");
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
