package com.multiprofit.service.impl;

import com.multiprofit.service.DataWarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataWarehouseServiceImpl implements DataWarehouseService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> getIndicatorSummary(String indicatorCode, String period, String caliberType) {
        String sql = "SELECT indicator_code, dim_name, calc_value FROM dw_indicator_fact " +
            "WHERE indicator_code = ? AND period = ? AND dim_type = 'TOTAL' AND caliber_type = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, indicatorCode, period, caliberType);

        Map<String, Object> result = new HashMap<>();
        result.put("indicatorCode", indicatorCode);
        result.put("period", period);
        result.put("caliberType", caliberType);

        if (!rows.isEmpty()) {
            result.put("totalValue", rows.get(0).get("calc_value"));
        } else {
            result.put("totalValue", 0);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getIndicatorDimension(String indicatorCode, String period, String dimType, String caliberType) {
        String sql = "SELECT dim_id, dim_name, calc_value FROM dw_indicator_fact " +
            "WHERE indicator_code = ? AND period = ? AND dim_type = ? AND caliber_type = ? " +
            "ORDER BY calc_value DESC";

        return jdbcTemplate.queryForList(sql, indicatorCode, period, dimType, caliberType);
    }

    @Override
    public Map<String, Object> getIndicatorDetail(String indicatorCode, String period, String dimType, Long dimId, String caliberType) {
        String sql = "SELECT * FROM dw_indicator_fact " +
            "WHERE indicator_code = ? AND period = ? AND dim_type = ? AND dim_id = ? AND caliber_type = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, indicatorCode, period, dimType, dimId, caliberType);

        if (!rows.isEmpty()) {
            return rows.get(0);
        }

        return new HashMap<>();
    }

    @Override
    public List<Map<String, Object>> getIndicatorTrend(String indicatorCode, int months, String caliberType) {
        String sql = "SELECT period, calc_value FROM dw_indicator_fact " +
            "WHERE indicator_code = ? AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "ORDER BY period DESC LIMIT ?";

        return jdbcTemplate.queryForList(sql, indicatorCode, caliberType, months);
    }

    @Override
    public List<Map<String, Object>> getIndicatorList() {
        String sql = "SELECT DISTINCT indicator_code FROM dw_indicator_fact ORDER BY indicator_code";
        return jdbcTemplate.queryForList(sql);
    }
}
