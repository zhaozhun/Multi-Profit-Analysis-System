package com.multiprofit.allocation.controller;

import com.multiprofit.allocation.model.ProductCommissionDetail;
import com.multiprofit.allocation.service.ProductCommissionService;
import com.multiprofit.allocation.service.ProductCommissionService.CommissionResult;
import com.multiprofit.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 产品分润控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/allocation/commission")
public class ProductCommissionController {

    @Autowired
    private ProductCommissionService commissionService;

    /**
     * 执行产品分润计算
     */
    @PostMapping("/execute")
    public Result<CommissionResult> executeCommission(@RequestBody Map<String, String> request) {
        try {
            String period = request.get("period");
            String productType = request.get("productType");

            if (period == null) {
                return Result.error("期间不能为空");
            }

            CommissionResult result = commissionService.executeCommission(period, productType);
            return Result.success(result);
        } catch (Exception e) {
            log.error("执行产品分润计算失败", e);
            return Result.error("执行失败: " + e.getMessage());
        }
    }

    /**
     * 查询分润明细
     */
    @GetMapping("/detail")
    public Result<List<ProductCommissionDetail>> getCommissionDetails(
            @RequestParam String period,
            @RequestParam(required = false) String productCode) {
        try {
            List<ProductCommissionDetail> details = commissionService.getCommissionDetails(period, productCode);
            return Result.success(details);
        } catch (Exception e) {
            log.error("查询分润明细失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 按产品汇总分润
     */
    @GetMapping("/summary/product")
    public Result<List<Map<String, Object>>> getProductSummary(@RequestParam String period) {
        try {
            List<Map<String, Object>> summary = commissionService.getProductCommissionSummary(period);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("查询产品分润汇总失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 按产品类型汇总分润
     */
    @GetMapping("/summary/product-type")
    public Result<List<Map<String, Object>>> getProductTypeSummary(@RequestParam String period) {
        try {
            List<Map<String, Object>> summary = commissionService.getProductTypeCommissionSummary(period);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("查询产品类型分润汇总失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
