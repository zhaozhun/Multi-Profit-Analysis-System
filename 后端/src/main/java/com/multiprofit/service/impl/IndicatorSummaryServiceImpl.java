package com.multiprofit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.multiprofit.entity.AtomicIndicator;
import com.multiprofit.entity.IndicatorSummary;
import com.multiprofit.entity.CostAllocationResult;
import com.multiprofit.mapper.AtomicIndicatorMapper;
import com.multiprofit.mapper.IndicatorSummaryMapper;
import com.multiprofit.mapper.CostAllocationResultMapper;
import com.multiprofit.service.IndicatorSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorSummaryServiceImpl implements IndicatorSummaryService {

    private final IndicatorSummaryMapper indicatorSummaryMapper;
    private final CostAllocationResultMapper costAllocationResultMapper;
    private final AtomicIndicatorMapper atomicIndicatorMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> getIndicatorSummary(String businessLine, String period, String statType) {
        QueryWrapper<IndicatorSummary> wrapper = new QueryWrapper<>();
        wrapper.eq("business_line", businessLine)
               .eq("period", period)
               .eq("status", 1);

        List<IndicatorSummary> summaries = indicatorSummaryMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();

        for (IndicatorSummary summary : summaries) {
            Map<String, Object> item = new HashMap<>();
            AtomicIndicator indicator = atomicIndicatorMapper.selectById(summary.getIndicatorCode());
            item.put("code", summary.getIndicatorCode());
            item.put("name", indicator != null ? indicator.getName() : summary.getIndicatorCode());
            item.put("value", summary.getCalcValue());
            item.put("unit", indicator != null ? indicator.getUnit() : "万元");
            item.put("period", summary.getPeriod());
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getCostTypes(String businessLine) {
        // 从atomic_indicator表获取费用类型
        QueryWrapper<AtomicIndicator> wrapper = new QueryWrapper<>();
        wrapper.eq("business_line", businessLine)
               .like("name", "成本")
               .eq("status", 1);

        List<AtomicIndicator> indicators = atomicIndicatorMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();

        for (AtomicIndicator indicator : indicators) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", indicator.getCode());
            item.put("name", indicator.getName());
            item.put("businessLine", indicator.getBusinessLine());
            result.add(item);
        }

        return result;
    }

    @Override
    public Map<String, Object> getCostAllocationResult(String costType, String period, int page, int size) {
        QueryWrapper<CostAllocationResult> wrapper = new QueryWrapper<>();
        wrapper.eq("cost_type", costType)
               .eq("period", period)
               .orderByDesc("allocated_amount");

        List<CostAllocationResult> results = costAllocationResultMapper.selectList(wrapper);

        // 分页处理
        int start = (page - 1) * size;
        int end = Math.min(start + size, results.size());
        List<CostAllocationResult> pageResults = results.subList(start, end);

        // 计算总金额
        BigDecimal totalAmount = results.stream()
                .map(CostAllocationResult::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("costType", costType);
        result.put("totalAmount", totalAmount);
        result.put("page", page);
        result.put("size", size);
        result.put("total", results.size());
        result.put("details", pageResults);

        return result;
    }

    @Override
    public Map<String, Object> getCostOriginalData(String costType, String period, String dimType) {
        // 查询原始费用数据
        String sql = String.format("""
            SELECT
                source_dim_code as dimCode,
                source_dim_name as dimName,
                SUM(amount) as amount
            FROM cost_actual_record
            WHERE cost_type = '%s'
              AND period = '%s'
              AND source_dim_type = '%s'
            GROUP BY source_dim_code, source_dim_name
            ORDER BY amount DESC
            """, costType, period, dimType);

        List<Map<String, Object>> details = jdbcTemplate.queryForList(sql);

        Map<String, Object> result = new HashMap<>();
        result.put("costType", costType);
        result.put("dimType", dimType);
        result.put("details", details);

        return result;
    }

    @Override
    public Map<String, Object> calculateIndicators(String period, String indicatorCode) {
        try {
            List<AtomicIndicator> indicators;
            if (indicatorCode != null && !indicatorCode.isEmpty()) {
                AtomicIndicator indicator = atomicIndicatorMapper.selectById(indicatorCode);
                indicators = indicator != null ? List.of(indicator) : Collections.emptyList();
            } else {
                indicators = atomicIndicatorMapper.selectList(null);
            }

            int calculatedCount = 0;
            for (AtomicIndicator indicator : indicators) {
                // 计算指标汇总值
                BigDecimal value = calculateIndicatorValue(indicator, period);

                // 保存到indicator_summary表
                IndicatorSummary summary = new IndicatorSummary();
                summary.setPeriod(period);
                summary.setIndicatorCode(indicator.getCode());
                summary.setIndicatorType("ATOMIC");
                summary.setBusinessLine(indicator.getBusinessLine());
                summary.setCalcValue(value);
                summary.setCalcTime(LocalDateTime.now());
                summary.setStatus(1);

                indicatorSummaryMapper.insert(summary);
                calculatedCount++;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "计算完成");
            result.put("calculatedCount", calculatedCount);
            return result;

        } catch (Exception e) {
            log.error("指标预计算失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return result;
        }
    }

    private BigDecimal calculateIndicatorValue(AtomicIndicator indicator, String period) {
        // 根据指标配置计算汇总值
        String sql = String.format("""
            SELECT SUM(%s) as value
            FROM %s
            WHERE DATE_FORMAT(stat_date, '%%Y-%%m') = '%s'
            """, indicator.getSourceField(), indicator.getSourceTable(), period);

        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(sql);
            Object value = row.get("value");
            return value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("计算指标{}失败: {}", indicator.getCode(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
