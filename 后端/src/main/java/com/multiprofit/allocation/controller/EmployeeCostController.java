package com.multiprofit.allocation.controller;

import com.multiprofit.allocation.model.EmployeeCostAllocation;
import com.multiprofit.allocation.service.EmployeeCostAllocationService;
import com.multiprofit.allocation.service.EmployeeCostAllocationService.EmployeeAllocationResult;
import com.multiprofit.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 员工费用分摊控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/allocation/employee")
public class EmployeeCostController {

    @Autowired
    private EmployeeCostAllocationService employeeCostService;

    /**
     * 执行员工费用分摊
     */
    @PostMapping("/execute")
    public Result<EmployeeAllocationResult> executeAllocation(@RequestBody Map<String, String> request) {
        try {
            String period = request.get("period");
            String costType = request.get("costType");
            String factorType = request.get("factorType");

            if (period == null || costType == null || factorType == null) {
                return Result.error("期间、成本类型、分摊因子不能为空");
            }

            EmployeeAllocationResult result = employeeCostService.executeAllocation(period, costType, factorType);
            return Result.success(result);
        } catch (Exception e) {
            log.error("执行员工费用分摊失败", e);
            return Result.error("执行失败: " + e.getMessage());
        }
    }

    /**
     * 查询员工费用分摊结果
     */
    @GetMapping("/result")
    public Result<List<EmployeeCostAllocation>> getAllocations(
            @RequestParam String period,
            @RequestParam(required = false) String costType,
            @RequestParam(required = false) String employeeCode) {
        try {
            List<EmployeeCostAllocation> allocations = employeeCostService.getAllocations(period, costType, employeeCode);
            return Result.success(allocations);
        } catch (Exception e) {
            log.error("查询员工费用分摊结果失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询员工费用汇总
     */
    @GetMapping("/summary/employee")
    public Result<List<Map<String, Object>>> getEmployeeSummary(@RequestParam String period) {
        try {
            List<Map<String, Object>> summary = employeeCostService.getEmployeeSummary(period);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("查询员工费用汇总失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询部门费用汇总
     */
    @GetMapping("/summary/dept")
    public Result<List<Map<String, Object>>> getDeptSummary(@RequestParam String period) {
        try {
            List<Map<String, Object>> summary = employeeCostService.getDeptSummary(period);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("查询部门费用汇总失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
