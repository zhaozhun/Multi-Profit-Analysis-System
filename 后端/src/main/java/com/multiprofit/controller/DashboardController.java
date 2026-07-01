package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import com.multiprofit.dto.DashboardDTO;
import com.multiprofit.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 解析period参数，转换为startDate和endDate
     * 支持格式：2026-06 → 2026-06-01 ~ 2026-06-30
     */
    private String[] parsePeriod(String period) {
        if (period == null || period.isEmpty()) {
            return new String[]{getDefaultStartDate(), getDefaultEndDate()};
        }
        // 格式：YYYY-MM
        YearMonth ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));
        return new String[]{
            ym.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
            ym.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE)
        };
    }

    /**
     * 获取当月第一天
     */
    private String getDefaultStartDate() {
        return YearMonth.now().atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * 获取今天日期
     */
    private String getDefaultEndDate() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * 获取驾驶舱全量数据（支持日期范围或period）
     */
    @GetMapping("/overview")
    public ApiResponse<DashboardDTO> getDashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(required = false) Long orgScope) {
        if (startDate == null && endDate == null && period != null) {
            String[] dates = parsePeriod(period);
            startDate = dates[0];
            endDate = dates[1];
        }
        if (startDate == null) startDate = getDefaultStartDate();
        if (endDate == null) endDate = getDefaultEndDate();
        return ApiResponse.ok(dashboardService.getDashboardData(startDate, endDate, caliberType, orgScope));
    }

    /**
     * 获取瀑布图数据
     */
    @GetMapping("/waterfall")
    public ApiResponse<DashboardDTO.WaterfallData> getWaterfall(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        if (startDate == null && endDate == null && period != null) {
            String[] dates = parsePeriod(period);
            startDate = dates[0];
            endDate = dates[1];
        }
        if (startDate == null) startDate = getDefaultStartDate();
        if (endDate == null) endDate = getDefaultEndDate();
        return ApiResponse.ok(dashboardService.getWaterfallData(startDate, endDate, caliberType));
    }

    /**
     * 获取趋势数据（近12个月）
     */
    @GetMapping("/trend")
    public ApiResponse<DashboardDTO.TrendData> getTrend(
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        if (endDate == null && period != null) {
            String[] dates = parsePeriod(period);
            endDate = dates[1];
        }
        if (endDate == null) endDate = getDefaultEndDate();
        return ApiResponse.ok(dashboardService.getTrendData(endDate, caliberType));
    }

    /**
     * 获取维度概览
     */
    @GetMapping("/dim-overview/{dimType}")
    public ApiResponse<DashboardDTO.DimOverview> getDimOverview(
            @PathVariable String dimType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        if (startDate == null && endDate == null && period != null) {
            String[] dates = parsePeriod(period);
            startDate = dates[0];
            endDate = dates[1];
        }
        if (startDate == null) startDate = getDefaultStartDate();
        if (endDate == null) endDate = getDefaultEndDate();
        return ApiResponse.ok(dashboardService.getDimOverview(dimType, startDate, endDate, caliberType));
    }
}
