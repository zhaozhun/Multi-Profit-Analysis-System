package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import com.multiprofit.service.DataWarehouseETLService;
import com.multiprofit.service.DataWarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dw")
public class DataWarehouseController {

    @Autowired
    private DataWarehouseService dataWarehouseService;

    @Autowired
    private DataWarehouseETLService dataWarehouseETLService;

    /**
     * 获取指标列表
     */
    @GetMapping("/indicator/list")
    public ApiResponse<List<Map<String, Object>>> getIndicatorList() {
        try {
            List<Map<String, Object>> result = dataWarehouseService.getIndicatorList();
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取指标列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标汇总
     */
    @GetMapping("/indicator/summary")
    public ApiResponse<Map<String, Object>> getIndicatorSummary(
            @RequestParam String indicatorCode,
            @RequestParam String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            Map<String, Object> result = dataWarehouseService.getIndicatorSummary(indicatorCode, period, caliberType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取指标汇总失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标维度数据
     */
    @GetMapping("/indicator/dimension")
    public ApiResponse<List<Map<String, Object>>> getIndicatorDimension(
            @RequestParam String indicatorCode,
            @RequestParam String period,
            @RequestParam String dimType,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            List<Map<String, Object>> result = dataWarehouseService.getIndicatorDimension(indicatorCode, period, dimType, caliberType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取指标维度数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标明细数据
     */
    @GetMapping("/indicator/detail")
    public ApiResponse<Map<String, Object>> getIndicatorDetail(
            @RequestParam String indicatorCode,
            @RequestParam String period,
            @RequestParam String dimType,
            @RequestParam Long dimId,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            Map<String, Object> result = dataWarehouseService.getIndicatorDetail(indicatorCode, period, dimType, dimId, caliberType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取指标明细数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标趋势
     */
    @GetMapping("/indicator/trend")
    public ApiResponse<List<Map<String, Object>>> getIndicatorTrend(
            @RequestParam String indicatorCode,
            @RequestParam(required = false, defaultValue = "6") int months,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            List<Map<String, Object>> result = dataWarehouseService.getIndicatorTrend(indicatorCode, months, caliberType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取指标趋势失败: " + e.getMessage());
        }
    }

    /**
     * 执行ETL
     */
    @PostMapping("/etl/execute")
    public ApiResponse<Map<String, Object>> executeETL(@RequestParam String period) {
        try {
            Map<String, Object> result = dataWarehouseETLService.executeETL(period);
            if ((Boolean) result.get("success")) {
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.error((String) result.get("message"));
            }
        } catch (Exception e) {
            return ApiResponse.error("执行ETL失败: " + e.getMessage());
        }
    }
}
