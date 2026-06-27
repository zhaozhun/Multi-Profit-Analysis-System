package com.multiprofit.allocation.controller;

import com.multiprofit.allocation.model.*;
import com.multiprofit.allocation.service.AllocationConfigService;
import com.multiprofit.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分摊配置管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/allocation")
public class AllocationConfigController {

    @Autowired
    private AllocationConfigService configService;

    // ========== 成本类型管理 ==========

    /**
     * 查询成本类型列表
     */
    @GetMapping("/cost-type")
    public Result<List<CostTypeConfig>> listCostTypes(
            @RequestParam(required = false) String status) {
        try {
            List<CostTypeConfig> list = configService.listCostTypes(status);
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询成本类型列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取成本类型详情
     */
    @GetMapping("/cost-type/{code}")
    public Result<CostTypeConfig> getCostType(@PathVariable String code) {
        try {
            CostTypeConfig costType = configService.getCostType(code);
            if (costType == null) {
                return Result.error("成本类型不存在");
            }
            return Result.success(costType);
        } catch (Exception e) {
            log.error("获取成本类型详情失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 创建成本类型
     */
    @PostMapping("/cost-type")
    public Result<CostTypeConfig> createCostType(@RequestBody @Valid CostTypeConfig costType) {
        try {
            CostTypeConfig created = configService.createCostType(costType);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建成本类型失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新成本类型
     */
    @PutMapping("/cost-type/{code}")
    public Result<CostTypeConfig> updateCostType(
            @PathVariable String code,
            @RequestBody @Valid CostTypeConfig costType) {
        try {
            CostTypeConfig updated = configService.updateCostType(code, costType);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新成本类型失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除成本类型
     */
    @DeleteMapping("/cost-type/{code}")
    public Result<Void> deleteCostType(@PathVariable String code) {
        try {
            configService.deleteCostType(code);
            return Result.success();
        } catch (Exception e) {
            log.error("删除成本类型失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    // ========== 因子配置管理 ==========

    /**
     * 查询因子列表
     */
    @GetMapping("/factor")
    public Result<List<AllocationFactorConfig>> listFactors(
            @RequestParam(required = false) String factorType,
            @RequestParam(required = false) String costType) {
        try {
            List<AllocationFactorConfig> list = configService.listFactors(factorType, costType);
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询因子列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取因子详情
     */
    @GetMapping("/factor/{code}")
    public Result<AllocationFactorConfig> getFactor(@PathVariable String code) {
        try {
            AllocationFactorConfig factor = configService.getFactor(code);
            if (factor == null) {
                return Result.error("因子不存在");
            }
            return Result.success(factor);
        } catch (Exception e) {
            log.error("获取因子详情失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 创建因子
     */
    @PostMapping("/factor")
    public Result<AllocationFactorConfig> createFactor(@RequestBody @Valid AllocationFactorConfig factor) {
        try {
            AllocationFactorConfig created = configService.createFactor(factor);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建因子失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新因子
     */
    @PutMapping("/factor/{code}")
    public Result<AllocationFactorConfig> updateFactor(
            @PathVariable String code,
            @RequestBody @Valid AllocationFactorConfig factor) {
        try {
            AllocationFactorConfig updated = configService.updateFactor(code, factor);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新因子失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除因子
     */
    @DeleteMapping("/factor/{code}")
    public Result<Void> deleteFactor(@PathVariable String code) {
        try {
            configService.deleteFactor(code);
            return Result.success();
        } catch (Exception e) {
            log.error("删除因子失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 计算因子值
     */
    @PostMapping("/factor/{code}/calc")
    public Result<Map<String, Object>> calculateFactor(
            @PathVariable String code,
            @RequestParam String period,
            @RequestParam String dimType) {
        try {
            // TODO: 实现因子值计算逻辑
            Map<String, Object> result = new HashMap<>();
            result.put("factorCode", code);
            result.put("period", period);
            result.put("dimType", dimType);
            result.put("message", "因子值计算功能待实现");
            return Result.success(result);
        } catch (Exception e) {
            log.error("计算因子值失败", e);
            return Result.error("计算失败: " + e.getMessage());
        }
    }

    // ========== 算法配置管理 ==========

    /**
     * 查询算法列表
     */
    @GetMapping("/algorithm")
    public Result<List<AllocationAlgorithmConfig>> listAlgorithms(
            @RequestParam(required = false) String algorithmType) {
        try {
            List<AllocationAlgorithmConfig> list = configService.listAlgorithms(algorithmType);
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询算法列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取算法详情
     */
    @GetMapping("/algorithm/{code}")
    public Result<AllocationAlgorithmConfig> getAlgorithm(@PathVariable String code) {
        try {
            AllocationAlgorithmConfig algorithm = configService.getAlgorithm(code);
            if (algorithm == null) {
                return Result.error("算法不存在");
            }
            return Result.success(algorithm);
        } catch (Exception e) {
            log.error("获取算法详情失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 创建算法
     */
    @PostMapping("/algorithm")
    public Result<AllocationAlgorithmConfig> createAlgorithm(@RequestBody @Valid AllocationAlgorithmConfig algorithm) {
        try {
            AllocationAlgorithmConfig created = configService.createAlgorithm(algorithm);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建算法失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新算法
     */
    @PutMapping("/algorithm/{code}")
    public Result<AllocationAlgorithmConfig> updateAlgorithm(
            @PathVariable String code,
            @RequestBody @Valid AllocationAlgorithmConfig algorithm) {
        try {
            AllocationAlgorithmConfig updated = configService.updateAlgorithm(code, algorithm);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新算法失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除算法
     */
    @DeleteMapping("/algorithm/{code}")
    public Result<Void> deleteAlgorithm(@PathVariable String code) {
        try {
            configService.deleteAlgorithm(code);
            return Result.success();
        } catch (Exception e) {
            log.error("删除算法失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 测试算法
     */
    @PostMapping("/algorithm/test")
    public Result<Map<String, Object>> testAlgorithm(@RequestBody Map<String, Object> request) {
        try {
            // TODO: 实现算法测试逻辑
            Map<String, Object> result = new HashMap<>();
            result.put("algorithmCode", request.get("algorithmCode"));
            result.put("message", "算法测试功能待实现");
            return Result.success(result);
        } catch (Exception e) {
            log.error("测试算法失败", e);
            return Result.error("测试失败: " + e.getMessage());
        }
    }

    // ========== 规则配置管理 ==========

    /**
     * 查询规则列表
     */
    @GetMapping("/rule")
    public Result<List<AllocationRuleConfig>> listRules(
            @RequestParam(required = false) String costType,
            @RequestParam(required = false) String status) {
        try {
            List<AllocationRuleConfig> list = configService.listRules(costType, status);
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询规则列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询有效规则
     */
    @GetMapping("/rule/effective")
    public Result<List<AllocationRuleConfig>> listEffectiveRules(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            LocalDate queryDate = date != null ? date : LocalDate.now();
            List<AllocationRuleConfig> list = configService.listEffectiveRules(queryDate);
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询有效规则失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询自动执行的规则
     */
    @GetMapping("/rule/auto-execute")
    public Result<List<AllocationRuleConfig>> listAutoExecuteRules() {
        try {
            List<AllocationRuleConfig> list = configService.listAutoExecuteRules();
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询自动执行规则失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取规则详情
     */
    @GetMapping("/rule/{id}")
    public Result<Map<String, Object>> getRule(@PathVariable Long id) {
        try {
            AllocationRuleConfig rule = configService.getRule(id);
            if (rule == null) {
                return Result.error("规则不存在");
            }

            // 获取权重配置
            List<AllocationFactorWeight> weights = configService.getRuleWeights(id);

            Map<String, Object> result = new HashMap<>();
            result.put("rule", rule);
            result.put("weights", weights);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取规则详情失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 创建规则
     */
    @PostMapping("/rule")
    public Result<AllocationRuleConfig> createRule(@RequestBody Map<String, Object> request) {
        try {
            // 解析请求
            AllocationRuleConfig rule = parseRuleFromRequest(request);
            List<AllocationFactorWeight> weights = parseWeightsFromRequest(request, null);

            AllocationRuleConfig created = configService.createRule(rule, weights);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建规则失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新规则
     */
    @PutMapping("/rule/{id}")
    public Result<AllocationRuleConfig> updateRule(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            // 解析请求
            AllocationRuleConfig rule = parseRuleFromRequest(request);
            List<AllocationFactorWeight> weights = parseWeightsFromRequest(request, id);

            AllocationRuleConfig updated = configService.updateRule(id, rule, weights);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新规则失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/rule/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        try {
            configService.deleteRule(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除规则失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 启用规则
     */
    @PostMapping("/rule/{id}/enable")
    public Result<Void> enableRule(@PathVariable Long id) {
        try {
            configService.enableRule(id);
            return Result.success();
        } catch (Exception e) {
            log.error("启用规则失败", e);
            return Result.error("启用失败: " + e.getMessage());
        }
    }

    /**
     * 禁用规则
     */
    @PostMapping("/rule/{id}/disable")
    public Result<Void> disableRule(@PathVariable Long id) {
        try {
            configService.disableRule(id);
            return Result.success();
        } catch (Exception e) {
            log.error("禁用规则失败", e);
            return Result.error("禁用失败: " + e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 从请求中解析规则配置
     */
    private AllocationRuleConfig parseRuleFromRequest(Map<String, Object> request) {
        AllocationRuleConfig rule = new AllocationRuleConfig();
        rule.setRuleCode((String) request.get("ruleCode"));
        rule.setRuleName((String) request.get("ruleName"));
        rule.setCostType((String) request.get("costType"));
        rule.setDescription((String) request.get("description"));
        rule.setPriority(request.get("priority") != null ? (Integer) request.get("priority") : 100);
        rule.setSourceDimType((String) request.get("sourceDimType"));
        rule.setSourceDimCode((String) request.get("sourceDimCode"));
        rule.setTargetDimType((String) request.get("targetDimType"));
        rule.setTargetDimFilter((String) request.get("targetDimFilter"));
        rule.setAlgorithmCode((String) request.get("algorithmCode"));
        rule.setAlgorithmParams((String) request.get("algorithmParams"));
        rule.setPeriodType((String) request.get("periodType"));
        rule.setAutoExecute(request.get("autoExecute") != null ? (Boolean) request.get("autoExecute") : true);
        rule.setStatus((String) request.get("status"));
        return rule;
    }

    /**
     * 从请求中解析权重配置
     */
    @SuppressWarnings("unchecked")
    private List<AllocationFactorWeight> parseWeightsFromRequest(Map<String, Object> request, Long ruleId) {
        List<Map<String, Object>> weightsData = (List<Map<String, Object>>) request.get("weights");
        if (weightsData == null || weightsData.isEmpty()) {
            return null;
        }

        return weightsData.stream().map(w -> {
            AllocationFactorWeight weight = new AllocationFactorWeight();
            weight.setRuleId(ruleId);
            weight.setFactorCode((String) w.get("factorCode"));
            weight.setWeight(new java.math.BigDecimal(w.get("weight").toString()));
            return weight;
        }).toList();
    }
}
