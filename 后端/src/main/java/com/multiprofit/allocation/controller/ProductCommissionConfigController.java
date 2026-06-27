package com.multiprofit.allocation.controller;

import com.multiprofit.allocation.model.ProductCommissionConfig;
import com.multiprofit.allocation.service.ProductCommissionConfigService;
import com.multiprofit.allocation.service.ProductCommissionConfigService.CommissionCalcResult;
import com.multiprofit.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 产品分润配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/allocation/commission-config")
public class ProductCommissionConfigController {

    @Autowired
    private ProductCommissionConfigService configService;

    /**
     * 获取所有产品分润配置
     */
    @GetMapping("/list")
    public Result<List<ProductCommissionConfig>> getAllConfigs() {
        try {
            List<ProductCommissionConfig> configs = configService.getAllConfigs();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取产品分润配置失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取需要分润的产品配置
     */
    @GetMapping("/commission")
    public Result<List<ProductCommissionConfig>> getCommissionConfigs() {
        try {
            List<ProductCommissionConfig> configs = configService.getCommissionConfigs();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取分润产品配置失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取不需要分润的产品配置
     */
    @GetMapping("/non-commission")
    public Result<List<ProductCommissionConfig>> getNonCommissionConfigs() {
        try {
            List<ProductCommissionConfig> configs = configService.getNonCommissionConfigs();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取非分润产品配置失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 更新产品分润配置
     */
    @PutMapping("/{productCode}")
    public Result<ProductCommissionConfig> updateConfig(
            @PathVariable String productCode,
            @RequestBody ProductCommissionConfig config) {
        try {
            ProductCommissionConfig updated = configService.updateConfig(productCode, config);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新产品分润配置失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 计算产品分润
     */
    @PostMapping("/calculate")
    public Result<CommissionCalcResult> calculateCommission(@RequestBody Map<String, String> request) {
        try {
            String period = request.get("period");
            if (period == null) {
                return Result.error("期间不能为空");
            }

            CommissionCalcResult result = configService.calculateCommission(period);
            return Result.success(result);
        } catch (Exception e) {
            log.error("计算产品分润失败", e);
            return Result.error("计算失败: " + e.getMessage());
        }
    }

    /**
     * 预览产品分润
     */
    @GetMapping("/preview")
    public Result<List<Map<String, Object>>> previewCommission(@RequestParam String period) {
        try {
            List<Map<String, Object>> preview = configService.previewCommission(period);
            return Result.success(preview);
        } catch (Exception e) {
            log.error("预览产品分润失败", e);
            return Result.error("预览失败: " + e.getMessage());
        }
    }
}
