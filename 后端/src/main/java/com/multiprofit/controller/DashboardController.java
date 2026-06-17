package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import com.multiprofit.dto.DashboardDTO;
import com.multiprofit.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 获取驾驶舱全量数据（支持日期范围）
     */
    @GetMapping("/overview")
    public ApiResponse<DashboardDTO> getDashboard(
            @RequestParam(required = false, defaultValue = "2026-06-01") String startDate,
            @RequestParam(required = false, defaultValue = "2026-06-16") String endDate,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(required = false) Long orgScope) {
        return ApiResponse.ok(dashboardService.getDashboardData(startDate, endDate, caliberType, orgScope));
    }

    /**
     * 获取瀑布图数据
     */
    @GetMapping("/waterfall")
    public ApiResponse<DashboardDTO.WaterfallData> getWaterfall(
            @RequestParam(required = false, defaultValue = "2026-06-01") String startDate,
            @RequestParam(required = false, defaultValue = "2026-06-16") String endDate,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        return ApiResponse.ok(dashboardService.getWaterfallData(startDate, endDate, caliberType));
    }

    /**
     * 获取趋势数据（近12个月）
     */
    @GetMapping("/trend")
    public ApiResponse<DashboardDTO.TrendData> getTrend(
            @RequestParam(required = false, defaultValue = "2026-06-16") String endDate,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        return ApiResponse.ok(dashboardService.getTrendData(endDate, caliberType));
    }

    /**
     * 获取维度概览
     */
    @GetMapping("/dim-overview/{dimType}")
    public ApiResponse<DashboardDTO.DimOverview> getDimOverview(
            @PathVariable String dimType,
            @RequestParam(required = false, defaultValue = "2026-06-01") String startDate,
            @RequestParam(required = false, defaultValue = "2026-06-16") String endDate,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        return ApiResponse.ok(dashboardService.getDimOverview(dimType, startDate, endDate, caliberType));
    }
}
