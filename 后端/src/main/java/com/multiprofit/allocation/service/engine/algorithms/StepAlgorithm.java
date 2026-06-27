package com.multiprofit.allocation.service.engine.algorithms;

import com.multiprofit.allocation.service.engine.AllocationAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 阶梯分摊算法
 * 按层级逐级分摊，适用于层级组织结构
 * 支持从上级到下级的逐级分摊
 */
@Slf4j
@Component
public class StepAlgorithm implements AllocationAlgorithm {

    private static final String CODE = "STEP";
    private static final String NAME = "阶梯分摊";
    private static final String DESCRIPTION = "按层级逐级分摊，适用于层级组织结构，支持从上级到下级的逐级分摊";

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
            new AlgorithmParam("max_level", "integer", false, "最大分摊层级", 3),
            new AlgorithmParam("precision", "integer", false, "计算精度(小数位数)", 2)
        );
    }

    @Override
    public List<AllocationResult> execute(AllocationContext context) {
        BigDecimal totalCost = context.getTotalCost();
        Map<String, BigDecimal> factorValues = context.getFactorValues();
        Integer maxLevel = context.getParam("max_level", Integer.class);
        Integer precision = context.getParam("precision", Integer.class);

        if (maxLevel == null) maxLevel = 3;
        if (precision == null) precision = 2;

        if (factorValues == null || factorValues.isEmpty()) {
            log.warn("因子值为空，无法执行阶梯分摊");
            return Collections.emptyList();
        }

        // 解析层级结构
        Map<String, List<String>> hierarchy = parseHierarchy(factorValues.keySet());

        // 按层级分摊
        List<AllocationResult> results = new ArrayList<>();
        BigDecimal remainingCost = totalCost;

        for (int level = 1; level <= maxLevel && remainingCost.compareTo(BigDecimal.ZERO) > 0; level++) {
            List<String> levelNodes = hierarchy.get("level_" + level);
            if (levelNodes == null || levelNodes.isEmpty()) {
                continue;
            }

            // 计算当前层级的因子总和
            BigDecimal levelFactorTotal = BigDecimal.ZERO;
            for (String node : levelNodes) {
                BigDecimal factor = factorValues.getOrDefault(node, BigDecimal.ZERO);
                levelFactorTotal = levelFactorTotal.add(factor);
            }

            if (levelFactorTotal.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // 分摊到当前层级
            for (String node : levelNodes) {
                BigDecimal factorValue = factorValues.getOrDefault(node, BigDecimal.ZERO);
                BigDecimal ratio = factorValue.divide(levelFactorTotal, 10, RoundingMode.HALF_UP);
                BigDecimal allocatedAmount = remainingCost.multiply(ratio)
                    .setScale(precision, RoundingMode.HALF_UP);

                // 构建计算详情
                Map<String, Object> calcDetails = new HashMap<>();
                calcDetails.put("level", level);
                calcDetails.put("factorCode", context.getParam("factor_code", String.class));
                calcDetails.put("factorValue", factorValue);
                calcDetails.put("levelFactorTotal", levelFactorTotal);
                calcDetails.put("ratio", ratio);
                calcDetails.put("formula", "remainingCost * (factorValue / levelFactorTotal)");

                AllocationResult result = AllocationResult.builder()
                    .targetDimCode(node)
                    .targetDimType(context.getTargetDimType())
                    .allocatedAmount(allocatedAmount)
                    .ratio(ratio)
                    .factorValue(factorValue)
                    .calcDetails(calcDetails)
                    .build();

                results.add(result);
            }

            // 更新剩余成本（如果需要逐级递减）
            // 这里简化处理，全额分摊到每一层
            // 实际场景可能需要根据业务逻辑调整
        }

        log.info("阶梯分摊完成: 成本类型={}, 期间={}, 目标数量={}, 总金额={}",
            context.getCostType(), context.getPeriod(), results.size(), totalCost);

        return results;
    }

    /**
     * 解析层级结构
     * 假设维度编码格式为: LEVEL1_CODE.LEVEL2_CODE.LEVEL3_CODE
     */
    private Map<String, List<String>> parseHierarchy(Set<String> dimCodes) {
        Map<String, List<String>> hierarchy = new HashMap<>();

        for (String code : dimCodes) {
            String[] parts = code.split("\\.");
            int level = parts.length;

            String levelKey = "level_" + level;
            hierarchy.computeIfAbsent(levelKey, k -> new ArrayList<>()).add(code);
        }

        return hierarchy;
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

        // 校验最大层级
        if (params != null && params.containsKey("max_level")) {
            Object maxLevel = params.get("max_level");
            if (maxLevel instanceof Integer) {
                int p = (Integer) maxLevel;
                if (p < 1 || p > 10) {
                    errors.add("参数范围错误: max_level 应在 1-10 之间");
                }
            } else {
                errors.add("参数类型错误: max_level 应为整数");
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
