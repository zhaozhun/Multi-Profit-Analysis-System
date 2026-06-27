package com.multiprofit.allocation.controller;

import com.multiprofit.allocation.model.AllocationBatch;
import com.multiprofit.allocation.service.AllocationService;
import com.multiprofit.allocation.service.AllocationService.*;
import com.multiprofit.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分摊执行控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/allocation")
public class AllocationController {

    @Autowired
    private AllocationService allocationService;

    /**
     * 执行分摊计算
     */
    @PostMapping("/execute")
    public Result<AllocationBatch> executeAllocation(@RequestBody AllocationRequest request) {
        try {
            AllocationBatch batch = allocationService.executeAllocation(request);
            return Result.success(batch);
        } catch (Exception e) {
            log.error("执行分摊失败", e);
            return Result.error("执行分摊失败: " + e.getMessage());
        }
    }

    /**
     * 预览分摊结果
     */
    @PostMapping("/preview")
    public Result<AllocationPreview> previewAllocation(@RequestBody AllocationRequest request) {
        try {
            AllocationPreview preview = allocationService.previewAllocation(request);
            return Result.success(preview);
        } catch (Exception e) {
            log.error("预览分摊失败", e);
            return Result.error("预览分摊失败: " + e.getMessage());
        }
    }

    /**
     * 查询分摊结果列表
     */
    @GetMapping("/result")
    public Result<List<AllocationBatch>> listResults(
            @RequestParam String period,
            @RequestParam(required = false) String costType) {
        try {
            List<AllocationBatch> results = allocationService.listResults(period, costType);
            return Result.success(results);
        } catch (Exception e) {
            log.error("查询分摊结果失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取分摊结果详情
     */
    @GetMapping("/result/{batchId}")
    public Result<AllocationBatchDetail> getResultDetail(@PathVariable Long batchId) {
        try {
            AllocationBatchDetail detail = allocationService.getResultDetail(batchId);
            return Result.success(detail);
        } catch (Exception e) {
            log.error("获取分摊详情失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询分摊汇总
     */
    @GetMapping("/result/{batchId}/summary")
    public Result<List<Map<String, Object>>> getAllocationSummary(
            @PathVariable Long batchId,
            @RequestParam(required = false) String targetDimType) {
        try {
            List<Map<String, Object>> summary = allocationService.getAllocationSummary(batchId, targetDimType);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("查询分摊汇总失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
