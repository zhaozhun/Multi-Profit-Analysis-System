package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import com.multiprofit.service.ExpenseAllocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 运营成本分摊Controller
 */
@RestController
@RequestMapping("/api/expense")
public class ExpenseAllocationController {

    @Autowired
    private ExpenseAllocationService expenseAllocationService;

    /**
     * 获取运营成本汇总
     */
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getExpenseSummary(
            @RequestParam String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(required = false, defaultValue = "ORG") String dimension) {
        try {
            Map<String, Object> result = expenseAllocationService.getExpenseSummary(period, caliberType, dimension);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取运营成本汇总失败: " + e.getMessage());
        }
    }

    /**
     * 获取业务费用组成
     */
    @GetMapping("/biz-composition")
    public ApiResponse<List<Map<String, Object>>> getBizExpenseComposition(
            @RequestParam String period,
            @RequestParam String bizId) {
        try {
            List<Map<String, Object>> result = expenseAllocationService.getBizExpenseComposition(period, bizId);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取业务费用组成失败: " + e.getMessage());
        }
    }

    /**
     * 获取费用原始数据
     */
    @GetMapping("/original")
    public ApiResponse<List<Map<String, Object>>> getExpenseOriginalData(
            @RequestParam String period,
            @RequestParam String expenseType) {
        try {
            List<Map<String, Object>> result = expenseAllocationService.getExpenseOriginalData(period, expenseType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取费用原始数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取分摊因子列表
     */
    @GetMapping("/factors")
    public ApiResponse<List<Map<String, Object>>> getAllocationFactors() {
        try {
            List<Map<String, Object>> result = expenseAllocationService.getAllocationFactors();
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取分摊因子失败: " + e.getMessage());
        }
    }

    /**
     * 获取分摊规则列表
     */
    @GetMapping("/rules")
    public ApiResponse<List<Map<String, Object>>> getAllocationRules(
            @RequestParam(required = false) String expenseType) {
        try {
            List<Map<String, Object>> result = expenseAllocationService.getAllocationRules(expenseType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取分摊规则失败: " + e.getMessage());
        }
    }

    /**
     * 保存分摊规则
     */
    @PostMapping("/rules")
    public ApiResponse<Map<String, Object>> saveAllocationRule(@RequestBody Map<String, Object> rule) {
        try {
            Map<String, Object> result = expenseAllocationService.saveAllocationRule(rule);
            if ((Boolean) result.get("success")) {
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.error((String) result.get("message"));
            }
        } catch (Exception e) {
            return ApiResponse.error("保存分摊规则失败: " + e.getMessage());
        }
    }

    /**
     * 执行分摊
     */
    @PostMapping("/execute")
    public ApiResponse<Map<String, Object>> executeAllocation(
            @RequestParam String period,
            @RequestParam(required = false) String expenseType) {
        try {
            Map<String, Object> result = expenseAllocationService.executeAllocation(period, expenseType);
            if ((Boolean) result.get("success")) {
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.error((String) result.get("message"));
            }
        } catch (Exception e) {
            return ApiResponse.error("执行分摊失败: " + e.getMessage());
        }
    }
}
