package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import com.multiprofit.entity.AtomicIndicator;
import com.multiprofit.entity.DerivedIndicator;
import com.multiprofit.mapper.AtomicIndicatorMapper;
import com.multiprofit.mapper.DerivedIndicatorMapper;
import com.multiprofit.service.IndicatorCalcService;
import com.multiprofit.service.IndicatorQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/indicator")
public class IndicatorController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AtomicIndicatorMapper atomicIndicatorMapper;

    @Autowired
    private DerivedIndicatorMapper derivedIndicatorMapper;

    @Autowired
    private IndicatorCalcService indicatorCalcService;

    @Autowired
    private IndicatorQueryService indicatorQueryService;

    /**
     * 查询指标列表
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "50") int pageSize) {

        StringBuilder sql = new StringBuilder("SELECT * FROM indicator_library WHERE 1=1");

        if (category != null && !category.isEmpty() && !category.equals("all")) {
            sql.append(" AND category = '").append(category).append("'");
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (name LIKE '%").append(keyword).append("%' OR code LIKE '%").append(keyword).append("%')");
        }

        sql.append(" ORDER BY category, sort_order");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString());

        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("list", rows);

        return ApiResponse.ok(result);
    }

    /**
     * 获取指标详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getDetail(@PathVariable Long id) {
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM indicator_library WHERE id = ?", id);
        return ApiResponse.ok(row);
    }

    /**
     * 新增指标
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        String name = (String) body.get("name");
        String category = (String) body.get("category");
        String unit = (String) body.get("unit");
        Integer precisionVal = body.get("precisionVal") != null ? Integer.parseInt(body.get("precisionVal").toString()) : 2;
        String calcFormula = (String) body.get("calcFormula");
        String dataSource = (String) body.get("dataSource");
        String sourceField = (String) body.get("sourceField");
        String preCalcPeriods = (String) body.get("preCalcPeriods");
        String supportedDims = (String) body.get("supportedDims");
        String description = (String) body.get("description");
        Integer sortOrder = body.get("sortOrder") != null ? Integer.parseInt(body.get("sortOrder").toString()) : 0;

        // 检查编码唯一性
        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM indicator_library WHERE code = ?", Integer.class, code
        );
        if (count != null && count > 0) {
            return ApiResponse.error("指标编码已存在");
        }

        jdbcTemplate.update(
            "INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, " +
            "calc_formula, data_source, source_field, pre_calc_periods, supported_dims, description, sort_order) " +
            "VALUES (?, ?, ?, 'DECIMAL', ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            code, name, category, unit, precisionVal, calcFormula, dataSource, sourceField,
            preCalcPeriods, supportedDims, description, sortOrder
        );

        Long id = jdbcTemplate.queryForObject("SELECT SCOPE_IDENTITY()", Long.class);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        return ApiResponse.ok(result);
    }

    /**
     * 修改指标
     */
    @PutMapping("/{id}")
    public ApiResponse<String> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String unit = (String) body.get("unit");
        String calcFormula = (String) body.get("calcFormula");
        String preCalcPeriods = (String) body.get("preCalcPeriods");
        String description = (String) body.get("description");
        Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : null;

        StringBuilder sql = new StringBuilder("UPDATE indicator_library SET ");
        List<Object> params = new ArrayList<>();
        List<String> sets = new ArrayList<>();

        if (name != null) { sets.add("name = ?"); params.add(name); }
        if (unit != null) { sets.add("unit = ?"); params.add(unit); }
        if (calcFormula != null) { sets.add("calc_formula = ?"); params.add(calcFormula); }
        if (preCalcPeriods != null) { sets.add("pre_calc_periods = ?"); params.add(preCalcPeriods); }
        if (description != null) { sets.add("description = ?"); params.add(description); }
        if (status != null) { sets.add("status = ?"); params.add(status); }

        if (sets.isEmpty()) return ApiResponse.error("没有要更新的字段");

        sql.append(String.join(", ", sets));
        sql.append(" WHERE id = ?");
        params.add(id);

        jdbcTemplate.update(sql.toString(), params.toArray());
        return ApiResponse.ok("更新成功");
    }

    /**
     * 删除指标
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        // 删除关联的预计算数据
        jdbcTemplate.update("DELETE FROM indicator_pre_calc WHERE indicator_code = " +
            "(SELECT code FROM indicator_library WHERE id = ?)", id);
        jdbcTemplate.update("DELETE FROM indicator_library WHERE id = ?", id);
        return ApiResponse.ok("删除成功");
    }

    /**
     * 手动触发预计算
     */
    @PostMapping("/{id}/calc")
    public ApiResponse<String> triggerCalc(
            @PathVariable Long id,
            @RequestParam String period,
            @RequestParam(required = false, defaultValue = "MONTH") String calcPeriod) {

        // 获取指标信息
        Map<String, Object> indicator;
        try {
            indicator = jdbcTemplate.queryForMap("SELECT * FROM indicator_library WHERE id = ?", id);
        } catch (Exception e) {
            return ApiResponse.error("指标不存在");
        }

        String code = (String) indicator.get("code");
        String sourceField = (String) indicator.get("source_field");
        String category = (String) indicator.get("category");

        // 根据指标类型执行不同的计算逻辑
        try {
            executeCalculation(code, sourceField, category, period, calcPeriod);
            return ApiResponse.ok("预计算完成");
        } catch (Exception e) {
            return ApiResponse.error("计算失败: " + e.getMessage());
        }
    }

    /**
     * 查询预计算数据
     */
    @GetMapping("/pre-calc/data")
    public ApiResponse<List<Map<String, Object>>> getPreCalcData(
            @RequestParam String indicatorCode,
            @RequestParam String calcPeriod,
            @RequestParam(required = false) String periodFrom,
            @RequestParam(required = false) String periodTo,
            @RequestParam(required = false) String dimType) {

        StringBuilder sql = new StringBuilder(
            "SELECT * FROM indicator_pre_calc WHERE indicator_code = '" + indicatorCode + "'" +
            " AND calc_period = '" + calcPeriod + "'"
        );

        if (periodFrom != null) sql.append(" AND period_value >= '").append(periodFrom).append("'");
        if (periodTo != null) sql.append(" AND period_value <= '").append(periodTo).append("'");
        if (dimType != null) sql.append(" AND dim_type = '").append(dimType).append("'");

        sql.append(" ORDER BY period_value DESC, dim_type, dim_name");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString());
        return ApiResponse.ok(rows);
    }

    /**
     * 获取指标统计
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 按分类统计
        List<Map<String, Object>> categoryStats = jdbcTemplate.queryForList(
            "SELECT category, count(*) as count FROM indicator_library GROUP BY category"
        );
        stats.put("categoryStats", categoryStats);

        // 总数
        Integer total = jdbcTemplate.queryForObject("SELECT count(*) FROM indicator_library", Integer.class);
        stats.put("total", total);

        // 已预计算数量
        Integer preCalcCount = jdbcTemplate.queryForObject(
            "SELECT count(DISTINCT indicator_code) FROM indicator_pre_calc", Integer.class
        );
        stats.put("preCalcCount", preCalcCount != null ? preCalcCount : 0);

        return ApiResponse.ok(stats);
    }

    /**
     * 执行预计算
     */
    private void executeCalculation(String code, String sourceField, String category,
                                     String period, String calcPeriod) {
        // 删除该指标该期间的旧数据
        jdbcTemplate.update(
            "DELETE FROM indicator_pre_calc WHERE indicator_code = ? AND calc_period = ? AND period_value = ?",
            code, calcPeriod, period
        );

        // 按机构维度计算
        String sql = String.format(
            "SELECT org_name as dim_name, sum(%s) as calc_value " +
            "FROM biz_ledger WHERE account_period = '%s' GROUP BY org_name",
            sourceField != null ? sourceField : "net_profit", period
        );

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> row : results) {
            String dimName = (String) row.get("dim_name");
            Object calcValue = row.get("calc_value");

            jdbcTemplate.update(
                "INSERT INTO indicator_pre_calc (indicator_code, calc_period, period_value, " +
                "dim_type, dim_name, calc_value, calc_time) VALUES (?, ?, ?, 'ORG', ?, ?, CURRENT_TIMESTAMP)",
                code, calcPeriod, period, dimName, calcValue
            );
        }
    }

    // ============================================
    // 新增：指标体系API
    // ============================================

    /**
     * 获取所有原子指标列表
     */
    @GetMapping("/atomic")
    public ResponseEntity<List<AtomicIndicator>> getAtomicIndicators() {
        List<AtomicIndicator> indicators = atomicIndicatorMapper.selectList(null);
        return ResponseEntity.ok(indicators);
    }

    /**
     * 获取所有派生指标列表
     */
    @GetMapping("/derived")
    public ResponseEntity<List<DerivedIndicator>> getDerivedIndicators() {
        List<DerivedIndicator> indicators = derivedIndicatorMapper.selectList(null);
        return ResponseEntity.ok(indicators);
    }

    /**
     * 获取指标值
     */
    @GetMapping("/value/{code}")
    public ResponseEntity<Map<String, Object>> getIndicatorValue(
            @PathVariable String code,
            @RequestParam String period,
            @RequestParam String periodValue) {
        Map<String, Object> result = indicatorQueryService.getIndicatorValue(code, period, periodValue);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取指标趋势
     */
    @GetMapping("/trend/{code}")
    public ResponseEntity<List<Map<String, Object>>> getIndicatorTrend(
            @PathVariable String code,
            @RequestParam(defaultValue = "12") int months) {
        List<Map<String, Object>> trend = indicatorQueryService.getIndicatorTrend(code, months);
        return ResponseEntity.ok(trend);
    }

    /**
     * 获取指标明细（分页）
     */
    @GetMapping("/detail/{code}")
    public ResponseEntity<Map<String, Object>> getIndicatorDetail(
            @PathVariable String code,
            @RequestParam String period,
            @RequestParam String periodValue,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        Map<String, Object> result = indicatorQueryService.getIndicatorDetail(code, period, periodValue, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * 按分组获取明细
     */
    @GetMapping("/detail/{code}/group/{groupValue}")
    public ResponseEntity<Map<String, Object>> getIndicatorDetailByGroup(
            @PathVariable String code,
            @PathVariable String groupValue,
            @RequestParam String period,
            @RequestParam String periodValue) {
        Map<String, Object> result = indicatorQueryService.getIndicatorDetailByGroup(code, period, periodValue, groupValue);
        return ResponseEntity.ok(result);
    }

    /**
     * 手动触发计算（新接口）
     */
    @PostMapping("/calc-new")
    public ResponseEntity<List<Map<String, Object>>> calcIndicators(
            @RequestParam String period,
            @RequestParam String periodValue) {
        List<Map<String, Object>> results = indicatorCalcService.calcAllIndicators(period, periodValue);
        return ResponseEntity.ok(results);
    }
}
