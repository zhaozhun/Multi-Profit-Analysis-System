package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import com.multiprofit.service.IndicatorDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 指标数据Controller
 */
@RestController
@RequestMapping("/api/indicator")
public class IndicatorController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IndicatorDetailService indicatorDetailService;

    // ============================================
    // 贷款指标API
    // ============================================

    /**
     * 获取贷款指标汇总数据
     */
    @GetMapping("/loan/summary")
    public ApiResponse<Map<String, Object>> getLoanIndicatorSummary(
            @RequestParam String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            Map<String, Object> result = indicatorDetailService.getLoanIndicatorSummary(period, caliberType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取贷款指标汇总数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取贷款指标明细列表
     */
    @GetMapping("/loan/detail")
    public ApiResponse<Map<String, Object>> getLoanIndicatorDetailList(
            @RequestParam String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(required = false) String dimension,
            @RequestParam(required = false) String dimensionValue,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize) {
        try {
            Map<String, Object> result = indicatorDetailService.getLoanIndicatorDetailList(
                period, caliberType, dimension, dimensionValue, page, pageSize);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取贷款指标明细列表失败: " + e.getMessage());
        }
    }

    /**
     * 按维度汇总贷款指标
     */
    @GetMapping("/loan/dimension")
    public ApiResponse<List<Map<String, Object>>> getLoanIndicatorByDimension(
            @RequestParam String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(defaultValue = "ORG") String dimension) {
        try {
            List<Map<String, Object>> result = indicatorDetailService.getLoanIndicatorByDimension(period, caliberType, dimension);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("按维度汇总贷款指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取贷款指标趋势
     */
    @GetMapping("/loan/trend")
    public ApiResponse<List<Map<String, Object>>> getLoanIndicatorTrend(
            @RequestParam(required = false, defaultValue = "6") int months,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            List<Map<String, Object>> result = indicatorDetailService.getLoanIndicatorTrend(months, caliberType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取贷款指标趋势失败: " + e.getMessage());
        }
    }

    // ============================================
    // 存款指标API
    // ============================================

    /**
     * 获取存款指标汇总数据
     */
    @GetMapping("/deposit/summary")
    public ApiResponse<Map<String, Object>> getDepositIndicatorSummary(
            @RequestParam String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            Map<String, Object> result = indicatorDetailService.getDepositIndicatorSummary(period, caliberType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取存款指标汇总数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取存款指标明细列表
     */
    @GetMapping("/deposit/detail")
    public ApiResponse<Map<String, Object>> getDepositIndicatorDetailList(
            @RequestParam String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(required = false) String dimension,
            @RequestParam(required = false) String dimensionValue,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize) {
        try {
            Map<String, Object> result = indicatorDetailService.getDepositIndicatorDetailList(
                period, caliberType, dimension, dimensionValue, page, pageSize);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取存款指标明细列表失败: " + e.getMessage());
        }
    }

    /**
     * 按维度汇总存款指标
     */
    @GetMapping("/deposit/dimension")
    public ApiResponse<List<Map<String, Object>>> getDepositIndicatorByDimension(
            @RequestParam String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType,
            @RequestParam(defaultValue = "ORG") String dimension) {
        try {
            List<Map<String, Object>> result = indicatorDetailService.getDepositIndicatorByDimension(period, caliberType, dimension);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("按维度汇总存款指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取存款指标趋势
     */
    @GetMapping("/deposit/trend")
    public ApiResponse<List<Map<String, Object>>> getDepositIndicatorTrend(
            @RequestParam(required = false, defaultValue = "6") int months,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            List<Map<String, Object>> result = indicatorDetailService.getDepositIndicatorTrend(months, caliberType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取存款指标趋势失败: " + e.getMessage());
        }
    }

    // ============================================
    // 指标库API
    // ============================================

    /**
     * 获取指标定义列表
     */
    @GetMapping("/definitions")
    public ApiResponse<List<Map<String, Object>>> getIndicatorDefinitions(
            @RequestParam(required = false) String indicatorType,
            @RequestParam(required = false) String businessLine) {
        try {
            StringBuilder sql = new StringBuilder(
                "SELECT * FROM indicator_definition WHERE status = 'ACTIVE'");
            List<Object> params = new ArrayList<>();

            if (indicatorType != null && !indicatorType.isEmpty()) {
                sql.append(" AND indicator_type = ?");
                params.add(indicatorType);
            }
            if (businessLine != null && !businessLine.isEmpty()) {
                sql.append(" AND business_line = ?");
                params.add(businessLine);
            }

            sql.append(" ORDER BY sort_order");
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取指标定义失败: " + e.getMessage());
        }
    }

}
