package com.multiprofit.allocation.service.engine.algorithms;

import com.multiprofit.allocation.service.engine.AllocationAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 公式分摊算法
 * 自定义公式分摊，支持复杂业务逻辑
 * 使用SpEL表达式引擎
 */
@Slf4j
@Component
public class FormulaAlgorithm implements AllocationAlgorithm {

    private static final String CODE = "FORMULA";
    private static final String NAME = "公式分摊";
    private static final String DESCRIPTION = "自定义公式分摊，支持复杂业务逻辑，使用SpEL表达式引擎";

    private final ExpressionParser parser = new SpelExpressionParser();

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
            new AlgorithmParam("expression", "string", true, "分摊公式表达式(SpEL语法)", null),
            new AlgorithmParam("precision", "integer", false, "计算精度(小数位数)", 2)
        );
    }

    @Override
    public List<AllocationResult> execute(AllocationContext context) {
        BigDecimal totalCost = context.getTotalCost();
        Map<String, BigDecimal> factorValues = context.getFactorValues();
        Map<String, Object> variables = context.getVariables();
        Integer precision = context.getParam("precision", Integer.class);
        if (precision == null) precision = 2;

        String expression = context.getParam("expression", String.class);
        if (expression == null || expression.isBlank()) {
            log.warn("未指定分摊公式，无法执行公式分摊");
            return Collections.emptyList();
        }

        if (factorValues == null || factorValues.isEmpty()) {
            log.warn("因子值为空，无法执行公式分摊");
            return Collections.emptyList();
        }

        List<AllocationResult> results = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : factorValues.entrySet()) {
            String targetDimCode = entry.getKey();
            BigDecimal factorValue = entry.getValue();

            try {
                // 构建表达式上下文
                StandardEvaluationContext evalContext = new StandardEvaluationContext();

                // 注入基础变量
                evalContext.setVariable("total_cost", totalCost);
                evalContext.setVariable("factor_value", factorValue);
                evalContext.setVariable("target_code", targetDimCode);
                evalContext.setVariable("source_code", context.getSourceDimCode());

                // 注入所有因子值
                if (factorValues != null) {
                    for (Map.Entry<String, BigDecimal> factorEntry : factorValues.entrySet()) {
                        evalContext.setVariable("factor_" + factorEntry.getKey(), factorEntry.getValue());
                    }
                }

                // 注入额外变量
                if (variables != null) {
                    for (Map.Entry<String, Object> varEntry : variables.entrySet()) {
                        evalContext.setVariable(varEntry.getKey(), varEntry.getValue());
                    }
                }

                // 解析并执行表达式
                Expression expr = parser.parseExpression(expression);
                Object resultValue = expr.getValue(evalContext);

                // 转换结果为BigDecimal
                BigDecimal allocatedAmount;
                if (resultValue instanceof BigDecimal) {
                    allocatedAmount = (BigDecimal) resultValue;
                } else if (resultValue instanceof Number) {
                    allocatedAmount = new BigDecimal(resultValue.toString());
                } else {
                    log.warn("公式返回非数值类型: {} -> {}", targetDimCode, resultValue);
                    continue;
                }

                // 设置精度
                allocatedAmount = allocatedAmount.setScale(precision, RoundingMode.HALF_UP);

                // 计算比例（相对于总成本）
                BigDecimal ratio = BigDecimal.ZERO;
                if (totalCost.compareTo(BigDecimal.ZERO) != 0) {
                    ratio = allocatedAmount.divide(totalCost, 10, RoundingMode.HALF_UP);
                }

                // 构建计算详情
                Map<String, Object> calcDetails = new HashMap<>();
                calcDetails.put("expression", expression);
                calcDetails.put("factorValue", factorValue);
                calcDetails.put("resultValue", resultValue);
                calcDetails.put("formula", "自定义公式");

                AllocationResult result = AllocationResult.builder()
                    .targetDimCode(targetDimCode)
                    .targetDimType(context.getTargetDimType())
                    .allocatedAmount(allocatedAmount)
                    .ratio(ratio)
                    .factorValue(factorValue)
                    .calcDetails(calcDetails)
                    .build();

                results.add(result);

            } catch (Exception e) {
                log.error("公式计算失败: target={}, expression={}, error={}",
                    targetDimCode, expression, e.getMessage());
                // 继续处理其他目标，不中断整个分摊
            }
        }

        log.info("公式分摊完成: 成本类型={}, 期间={}, 目标数量={}, 总金额={}",
            context.getCostType(), context.getPeriod(), results.size(), totalCost);

        return results;
    }

    @Override
    public ValidationResult validateParams(Map<String, Object> params) {
        List<String> errors = new ArrayList<>();

        // 校验公式表达式
        if (params == null || !params.containsKey("expression")) {
            errors.add("缺少必填参数: expression");
        } else {
            String expression = (String) params.get("expression");
            if (expression == null || expression.isBlank()) {
                errors.add("参数值为空: expression");
            } else {
                // 尝试解析表达式
                try {
                    parser.parseExpression(expression);
                } catch (Exception e) {
                    errors.add("表达式语法错误: " + e.getMessage());
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
