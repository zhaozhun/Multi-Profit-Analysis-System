package com.multiprofit.allocation.controller;

import com.multiprofit.allocation.model.OperationCostAllocationResultEntity;
import com.multiprofit.allocation.service.OperationCostAllocationService;
import com.multiprofit.allocation.service.OperationCostAllocationService.OperationCostAllocationResult;
import com.multiprofit.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 运营费用分摊控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/allocation/operation-cost")
public class OperationCostController {

    @Autowired
    private OperationCostAllocationService operationCostService;

    /**
     * 执行运营费用分摊
     */
    @PostMapping("/execute")
    public Result<OperationCostAllocationResult> executeAllocation(@RequestBody Map<String, String> request) {
        try {
            String period = request.get("period");
            String costType = request.get("costType");

            if (period == null) {
                return Result.error("期间不能为空");
            }

            OperationCostAllocationResult result = operationCostService.executeAllocation(period, costType);
            return Result.success(result);
        } catch (Exception e) {
            log.error("执行运营费用分摊失败", e);
            return Result.error("执行失败: " + e.getMessage());
        }
    }

    /**
     * 查询运营费用分摊结果
     */
    @GetMapping("/result")
    public Result<List<OperationCostAllocationResultEntity>> getAllocationResults(
            @RequestParam String period,
            @RequestParam(required = false) String costType) {
        try {
            List<OperationCostAllocationResultEntity> results = operationCostService.getAllocationResults(period, costType);
            return Result.success(results);
        } catch (Exception e) {
            log.error("查询运营费用分摊结果失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 按费用类型汇总
     */
    @GetMapping("/summary/cost-type")
    public Result<List<Map<String, Object>>> getSummaryByCostType(@RequestParam String period) {
        try {
            List<Map<String, Object>> summary = operationCostService.getSummaryByCostType(period);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("查询费用类型汇总失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 按员工汇总
     */
    @GetMapping("/summary/employee")
    public Result<List<Map<String, Object>>> getSummaryByEmployee(@RequestParam String period) {
        try {
            List<Map<String, Object>> summary = operationCostService.getSummaryByEmployee(period);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("查询员工费用汇总失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
