package com.multiprofit.service.impl;

import com.multiprofit.service.IndicatorLibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 指标库服务实现
 * 数据源: indicator_library表
 */
@Service
public class IndicatorLibraryServiceImpl implements IndicatorLibraryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<String> getCodesByCategory(String category) {
        return jdbcTemplate.queryForList(
            "SELECT code FROM indicator_library WHERE category = ? AND status = 1 ORDER BY sort_order",
            String.class, category);
    }

    @Override
    public List<String> getAllActiveCodes() {
        return jdbcTemplate.queryForList(
            "SELECT code FROM indicator_library WHERE status = 1 ORDER BY sort_order",
            String.class);
    }

    @Override
    public Map<String, Object> getIndicatorByCode(String code) {
        return jdbcTemplate.queryForMap(
            "SELECT * FROM indicator_library WHERE code = ? AND status = 1", code);
    }
}
