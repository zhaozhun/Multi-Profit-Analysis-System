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
     * 获取原子指标列表(有source_field,直接取数)
     */
    @GetMapping("/atomic")
    public ApiResponse<List<Map<String, Object>>> getAtomicIndicators() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT code, name, category, data_source, source_field, filter_condition, " +
                "unit, precision_val, sort_order, status, description " +
                "FROM indicator_library WHERE status = 1 " +
                "AND source_field IS NOT NULL AND source_field <> '' ORDER BY sort_order");
            return ApiResponse.ok(toIndicatorRows(rows));
        } catch (Exception e) {
            return ApiResponse.error("获取原子指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取派生指标列表(无source_field,靠calc_formula组合)
     */
    @GetMapping("/derived")
    public ApiResponse<List<Map<String, Object>>> getDerivedIndicators() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT code, name, category, calc_formula, unit, precision_val, " +
                "sort_order, status, description " +
                "FROM indicator_library WHERE status = 1 " +
                "AND (source_field IS NULL OR source_field = '') " +
                "AND calc_formula IS NOT NULL AND calc_formula <> '' ORDER BY sort_order");
            return ApiResponse.ok(toIndicatorRows(rows));
        } catch (Exception e) {
            return ApiResponse.error("获取派生指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标定义列表(支持按category过滤)
     * 返回DB原始下划线字段 + 推断 business_line/indicator_type,兼容前端 IndicatorLibrary/IndicatorDetail 页面
     */
    @GetMapping("/definitions")
    public ApiResponse<List<Map<String, Object>>> getIndicatorDefinitions(
            @RequestParam(required = false) String indicatorType,
            @RequestParam(required = false) String businessLine) {
        try {
            StringBuilder sql = new StringBuilder(
                "SELECT * FROM indicator_library WHERE status = 1");
            List<Object> params = new ArrayList<>();

            // 兼容前端 category 维度(indicatorType/businessLine均映射到category)
            String category = (indicatorType != null && !indicatorType.isEmpty()) ? indicatorType : businessLine;
            if (category != null && !category.isEmpty() && !"ALL".equals(category)
                && !"ASSET".equals(category) && !"LIABILITY".equals(category)) {
                sql.append(" AND category = ?");
                params.add(category);
            }

            sql.append(" ORDER BY sort_order");
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            // 补前端期望的别名字段(DB无此列)
            for (Map<String, Object> row : rows) {
                String code = String.valueOf(row.get("code"));
                row.put("indicator_code", code);
                row.put("indicator_name", row.get("name"));
                row.put("business_line", inferBusinessLine(code));
                row.put("indicator_type", row.get("category"));
                row.put("status", "ACTIVE"); // DB为1,前端期望字符串ACTIVE
            }
            return ApiResponse.ok(rows);
        } catch (Exception e) {
            return ApiResponse.error("获取指标定义失败: " + e.getMessage());
        }
    }

    /**
     * DB行转前端驼峰字段 + 推断业务条线
     * 业务条线:LOAN开头=ASSET, DEPOSIT或INTEREST或FTP收入=LIABILITY, 其余=BOTH
     */
    private List<Map<String, Object>> toIndicatorRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", row.get("code"));
            m.put("name", row.get("name"));
            m.put("category", row.get("category"));
            m.put("businessLine", inferBusinessLine(String.valueOf(row.get("code"))));
            m.put("sourceTable", row.get("data_source"));
            m.put("sourceField", row.get("source_field"));
            m.put("filterCondition", row.get("filter_condition"));
            m.put("calcFormula", row.get("calc_formula"));
            m.put("formulaVars", null);
            m.put("unit", row.get("unit"));
            m.put("precisionVal", row.get("precision_val"));
            m.put("sortOrder", row.get("sort_order"));
            m.put("status", row.get("status"));
            m.put("description", row.get("description"));
            result.add(m);
        }
        return result;
    }

    /**
     * 按code前缀推断业务条线
     * LOAN开头=ASSET, DEPOSIT/INTEREST/FTP收入=LIABILITY, TOTAL/比率=ALL
     */
    private String inferBusinessLine(String code) {
        if (code == null) return "ALL";
        if (code.startsWith("LOAN_")) return "ASSET";
        if (code.startsWith("DEPOSIT_") || code.startsWith("INTEREST_")
            || code.startsWith("FTP_DAILY_INCOME") || code.startsWith("FTP_MONTHLY_INCOME")
            || code.startsWith("FTP_YEARLY_INCOME")) return "LIABILITY";
        return "ALL";
    }

}
