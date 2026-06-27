package com.multiprofit.allocation.controller;

import com.multiprofit.allocation.model.*;
import com.multiprofit.allocation.service.CostTypeService;
import com.multiprofit.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 费用类型管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cost-type")
public class CostTypeController {

    @Autowired
    private CostTypeService costTypeService;

    // ========== 费用类型管理 ==========

    /**
     * 获取所有费用类型
     */
    @GetMapping("/list")
    public Result<List<CostTypeMaster>> getAllCostTypes() {
        try {
            List<CostTypeMaster> list = costTypeService.getAllCostTypes();
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取费用类型列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取费用类型层级结构
     */
    @GetMapping("/hierarchy")
    public Result<List<Map<String, Object>>> getCostTypeHierarchy() {
        try {
            List<Map<String, Object>> hierarchy = costTypeService.getCostTypeHierarchy();
            return Result.success(hierarchy);
        } catch (Exception e) {
            log.error("获取费用类型层级结构失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定层级的费用类型
     */
    @GetMapping("/level/{level}")
    public Result<List<CostTypeMaster>> getCostTypesByLevel(@PathVariable Integer level) {
        try {
            List<CostTypeMaster> list = costTypeService.getCostTypesByLevel(level);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取费用类型失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定大类下的费用类型
     */
    @GetMapping("/parent/{parentCode}")
    public Result<List<CostTypeMaster>> getCostTypesByParent(@PathVariable String parentCode) {
        try {
            List<CostTypeMaster> list = costTypeService.getCostTypesByParent(parentCode);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取费用类型失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取需要分摊的费用类型
     */
    @GetMapping("/allocation-required")
    public Result<List<CostTypeMaster>> getAllocationRequiredTypes() {
        try {
            List<CostTypeMaster> list = costTypeService.getAllocationRequiredTypes();
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取费用类型失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据费用性质获取费用类型
     */
    @GetMapping("/nature/{costNature}")
    public Result<List<CostTypeMaster>> getCostTypesByNature(@PathVariable String costNature) {
        try {
            List<CostTypeMaster> list = costTypeService.getCostTypesByNature(costNature);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取费用类型失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据费用性质获取费用类型
     */
    @GetMapping("/category/{costCategory}")
    public Result<List<CostTypeMaster>> getCostTypesByCategory(@PathVariable String costCategory) {
        try {
            List<CostTypeMaster> list = costTypeService.getCostTypesByCategory(costCategory);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取费用类型失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取费用类型详情
     */
    @GetMapping("/{costCode}")
    public Result<CostTypeMaster> getCostType(@PathVariable String costCode) {
        try {
            CostTypeMaster costType = costTypeService.getCostType(costCode);
            if (costType == null) {
                return Result.error("费用类型不存在");
            }
            return Result.success(costType);
        } catch (Exception e) {
            log.error("获取费用类型详情失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 创建费用类型
     */
    @PostMapping
    public Result<CostTypeMaster> createCostType(@RequestBody CostTypeMaster costType) {
        try {
            CostTypeMaster created = costTypeService.createCostType(costType);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建费用类型失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新费用类型
     */
    @PutMapping("/{costCode}")
    public Result<CostTypeMaster> updateCostType(
            @PathVariable String costCode,
            @RequestBody CostTypeMaster costType) {
        try {
            CostTypeMaster updated = costTypeService.updateCostType(costCode, costType);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新费用类型失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除费用类型
     */
    @DeleteMapping("/{costCode}")
    public Result<Void> deleteCostType(@PathVariable String costCode) {
        try {
            costTypeService.deleteCostType(costCode);
            return Result.success();
        } catch (Exception e) {
            log.error("删除费用类型失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    // ========== 费用分摊规则管理 ==========

    /**
     * 获取费用分摊规则列表
     */
    @GetMapping("/allocation-rule/list")
    public Result<List<CostAllocationRuleConfig>> getAllocationRules() {
        try {
            List<CostAllocationRuleConfig> rules = costTypeService.getAllocationRules();
            return Result.success(rules);
        } catch (Exception e) {
            log.error("获取分摊规则失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取费用分摊规则详情
     */
    @GetMapping("/allocation-rule/{costCode}")
    public Result<CostAllocationRuleConfig> getAllocationRule(@PathVariable String costCode) {
        try {
            CostAllocationRuleConfig rule = costTypeService.getAllocationRule(costCode);
            if (rule == null) {
                return Result.error("分摊规则不存在");
            }
            return Result.success(rule);
        } catch (Exception e) {
            log.error("获取分摊规则失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 更新费用分摊规则
     */
    @PutMapping("/allocation-rule/{costCode}")
    public Result<CostAllocationRuleConfig> updateAllocationRule(
            @PathVariable String costCode,
            @RequestBody CostAllocationRuleConfig rule) {
        try {
            CostAllocationRuleConfig updated = costTypeService.updateAllocationRule(costCode, rule);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新分摊规则失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    // ========== 费用实际记录管理 ==========

    /**
     * 创建费用实际记录
     */
    @PostMapping("/actual-record")
    public Result<CostActualRecord> createActualRecord(@RequestBody CostActualRecord record) {
        try {
            CostActualRecord created = costTypeService.createActualRecord(record);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建费用记录失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 获取费用实际记录
     */
    @GetMapping("/actual-record")
    public Result<List<CostActualRecord>> getActualRecords(
            @RequestParam String period,
            @RequestParam(required = false) String costCode,
            @RequestParam(required = false) String costType) {
        try {
            List<CostActualRecord> records = costTypeService.getActualRecords(period, costCode, costType);
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取费用记录失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    // ========== 费用统计 ==========

    /**
     * 获取费用汇总统计
     */
    @GetMapping("/summary")
    public Result<List<Map<String, Object>>> getCostSummary(@RequestParam String period) {
        try {
            List<Map<String, Object>> summary = costTypeService.getCostSummary(period);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("获取费用汇总失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 按费用性质汇总
     */
    @GetMapping("/summary/nature")
    public Result<Map<String, BigDecimal>> getSummaryByNature(@RequestParam String period) {
        try {
            Map<String, BigDecimal> summary = costTypeService.getSummaryByNature(period);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("获取费用汇总失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 按费用性质（固定/变动/直接）汇总
     */
    @GetMapping("/summary/category")
    public Result<Map<String, BigDecimal>> getSummaryByCategory(@RequestParam String period) {
        try {
            Map<String, BigDecimal> summary = costTypeService.getSummaryByCategory(period);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("获取费用汇总失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
