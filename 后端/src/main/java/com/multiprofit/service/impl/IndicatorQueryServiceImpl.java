package com.multiprofit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.multiprofit.entity.AtomicIndicator;
import com.multiprofit.entity.DerivedIndicator;
import com.multiprofit.entity.IndicatorPreCalc;
import com.multiprofit.mapper.AtomicIndicatorMapper;
import com.multiprofit.mapper.DerivedIndicatorMapper;
import com.multiprofit.mapper.IndicatorPreCalcMapper;
import com.multiprofit.service.IndicatorQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 指标查询服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorQueryServiceImpl implements IndicatorQueryService {

    private final AtomicIndicatorMapper atomicIndicatorMapper;
    private final DerivedIndicatorMapper derivedIndicatorMapper;
    private final IndicatorPreCalcMapper indicatorPreCalcMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> getIndicatorValue(String indicatorCode, String calcPeriod, String periodValue) {
        // 从预计算结果表获取指标值
        IndicatorPreCalc preCalc = indicatorPreCalcMapper.selectOne(
            new QueryWrapper<IndicatorPreCalc>()
                .eq("indicator_code", indicatorCode)
                .eq("calc_period", calcPeriod)
                .eq("period_value", periodValue)
                .isNull("stat_type")
        );

        Map<String, Object> result = new HashMap<>();
        result.put("code", indicatorCode);
        result.put("calcPeriod", calcPeriod);
        result.put("periodValue", periodValue);

        if (preCalc != null) {
            result.put("value", preCalc.getCalcValue());
            result.put("calcTime", preCalc.getCalcTime());
        } else {
            result.put("value", null);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getIndicatorTrend(String indicatorCode, int months) {
        List<Map<String, Object>> trend = new ArrayList<>();

        // 获取最近N个月的趋势数据
        LocalDate now = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String periodValue = monthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            IndicatorPreCalc preCalc = indicatorPreCalcMapper.selectOne(
                new QueryWrapper<IndicatorPreCalc>()
                    .eq("indicator_code", indicatorCode)
                    .eq("calc_period", "MONTH")
                    .eq("period_value", periodValue)
                    .isNull("stat_type")
            );

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("periodValue", periodValue);
            monthData.put("value", preCalc != null ? preCalc.getCalcValue() : null);
            trend.add(monthData);
        }

        return trend;
    }

    @Override
    public Map<String, Object> getIndicatorDetail(String indicatorCode, String calcPeriod, String periodValue, int page, int size) {
        // 获取原子指标配置
        AtomicIndicator indicator = atomicIndicatorMapper.selectById(indicatorCode);
        if (indicator == null) {
            throw new RuntimeException("原子指标不存在: " + indicatorCode);
        }

        // 构建明细查询SQL
        String sql = buildDetailSql(indicator, calcPeriod, periodValue, page, size);

        // 执行查询
        List<Map<String, Object>> details = executeDetailSql(sql);

        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("code", indicatorCode);
        result.put("name", indicator.getName());
        result.put("calcPeriod", calcPeriod);
        result.put("periodValue", periodValue);
        result.put("page", page);
        result.put("size", size);
        result.put("details", details);

        return result;
    }

    @Override
    public Map<String, Object> getIndicatorDetailByGroup(String indicatorCode, String calcPeriod, String periodValue, String groupValue) {
        // 获取原子指标配置
        AtomicIndicator indicator = atomicIndicatorMapper.selectById(indicatorCode);
        if (indicator == null) {
            throw new RuntimeException("原子指标不存在: " + indicatorCode);
        }

        // 构建分组明细查询SQL
        String sql = buildGroupDetailSql(indicator, calcPeriod, periodValue, groupValue);

        // 执行查询
        List<Map<String, Object>> details = executeDetailSql(sql);

        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("code", indicatorCode);
        result.put("name", indicator.getName());
        result.put("calcPeriod", calcPeriod);
        result.put("periodValue", periodValue);
        result.put("groupValue", groupValue);
        result.put("details", details);

        return result;
    }

    @Override
    public Map<String, Object> compareIndicators(List<String> indicatorCodes, String calcPeriod, String periodValue) {
        Map<String, Object> result = new HashMap<>();
        result.put("calcPeriod", calcPeriod);
        result.put("periodValue", periodValue);

        List<Map<String, Object>> comparisons = new ArrayList<>();
        for (String code : indicatorCodes) {
            Map<String, Object> comparison = getIndicatorValue(code, calcPeriod, periodValue);
            comparisons.add(comparison);
        }

        result.put("comparisons", comparisons);
        return result;
    }

    // 私有辅助方法
    private String buildDetailSql(AtomicIndicator indicator, String calcPeriod, String periodValue, int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(indicator.getDetailDisplayFields().replace("[", "").replace("]", "").replace("\"", ""));
        sql.append(" FROM ").append(indicator.getDetailTable()).append(" ");
        sql.append("WHERE 1=1 ");

        if (indicator.getFilterCondition() != null && !indicator.getFilterCondition().isEmpty()) {
            sql.append("AND ").append(indicator.getFilterCondition()).append(" ");
        }

        // 添加时间筛选
        if ("MONTH".equals(calcPeriod)) {
            sql.append("AND DATE_FORMAT(stat_date, '%Y-%m') = '").append(periodValue).append("' ");
        } else if ("YEAR".equals(calcPeriod)) {
            sql.append("AND YEAR(stat_date) = ").append(periodValue).append(" ");
        }

        // 添加分页
        sql.append("LIMIT ").append(size).append(" OFFSET ").append((page - 1) * size);

        return sql.toString();
    }

    private String buildGroupDetailSql(AtomicIndicator indicator, String calcPeriod, String periodValue, String groupValue) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(indicator.getDetailDisplayFields().replace("[", "").replace("]", "").replace("\"", ""));
        sql.append(" FROM ").append(indicator.getDetailTable()).append(" ");
        sql.append("WHERE 1=1 ");

        if (indicator.getFilterCondition() != null && !indicator.getFilterCondition().isEmpty()) {
            sql.append("AND ").append(indicator.getFilterCondition()).append(" ");
        }

        // 添加时间筛选
        if ("MONTH".equals(calcPeriod)) {
            sql.append("AND DATE_FORMAT(stat_date, '%Y-%m') = '").append(periodValue).append("' ");
        } else if ("YEAR".equals(calcPeriod)) {
            sql.append("AND YEAR(stat_date) = ").append(periodValue).append(" ");
        }

        // 添加分组筛选
        if (indicator.getDetailGroupBy() != null && !indicator.getDetailGroupBy().isEmpty()) {
            sql.append("AND ").append(indicator.getDetailGroupBy()).append(" = '").append(groupValue).append("' ");
        }

        return sql.toString();
    }

    private List<Map<String, Object>> executeDetailSql(String sql) {
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("执行明细SQL失败: {}", sql, e);
            return new ArrayList<>();
        }
    }
}
