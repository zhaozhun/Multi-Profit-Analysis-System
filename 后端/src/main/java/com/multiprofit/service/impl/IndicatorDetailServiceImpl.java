package com.multiprofit.service.impl;

import com.multiprofit.service.IndicatorDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 指标明细服务实现类
 * 数据源：dw_indicator_fact（预计算数据）
 */
@Service
public class IndicatorDetailServiceImpl implements IndicatorDetailService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> getLoanIndicatorSummary(String period, String caliberType) {
        // 从 dw_indicator_fact 读取贷款指标汇总
        String sql = "SELECT indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = 'MONTH' AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "AND indicator_code IN ('LOAN_BALANCE', 'LOAN_COUNT', 'INTEREST_INCOME', 'FTP_COST', 'RISK_COST', 'OP_COST', 'LOAN_PROFIT')";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, period, caliberType);

        Map<String, BigDecimal> indicatorMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String code = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");
            indicatorMap.put(code, value);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("total_count", indicatorMap.getOrDefault("LOAN_COUNT", BigDecimal.ZERO).intValue());
        summary.put("total_balance", indicatorMap.getOrDefault("LOAN_BALANCE", BigDecimal.ZERO));
        summary.put("total_monthly_interest", indicatorMap.getOrDefault("INTEREST_INCOME", BigDecimal.ZERO));
        summary.put("total_ftp_cost", indicatorMap.getOrDefault("FTP_COST", BigDecimal.ZERO));
        summary.put("total_risk_cost", indicatorMap.getOrDefault("RISK_COST", BigDecimal.ZERO));
        summary.put("total_op_cost", indicatorMap.getOrDefault("OP_COST", BigDecimal.ZERO));
        summary.put("total_profit", indicatorMap.getOrDefault("LOAN_PROFIT", BigDecimal.ZERO));

        return summary;
    }

    @Override
    public Map<String, Object> getDepositIndicatorSummary(String period, String caliberType) {
        // 从 dw_indicator_fact 读取存款指标汇总
        String sql = "SELECT indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = 'MONTH' AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "AND indicator_code IN ('DEPOSIT_BALANCE', 'DEPOSIT_COUNT', 'FTP_INCOME', 'INTEREST_EXPENSE', 'LIABILITY_OP_COST', 'DEPOSIT_PROFIT')";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, period, caliberType);

        Map<String, BigDecimal> indicatorMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String code = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");
            indicatorMap.put(code, value);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("total_count", indicatorMap.getOrDefault("DEPOSIT_COUNT", BigDecimal.ZERO).intValue());
        summary.put("total_balance", indicatorMap.getOrDefault("DEPOSIT_BALANCE", BigDecimal.ZERO));
        summary.put("total_ftp_income", indicatorMap.getOrDefault("FTP_INCOME", BigDecimal.ZERO));
        summary.put("total_monthly_interest", indicatorMap.getOrDefault("INTEREST_EXPENSE", BigDecimal.ZERO));
        summary.put("total_op_cost", indicatorMap.getOrDefault("LIABILITY_OP_COST", BigDecimal.ZERO));
        summary.put("total_profit", indicatorMap.getOrDefault("DEPOSIT_PROFIT", BigDecimal.ZERO));

        return summary;
    }

    @Override
    public Map<String, Object> getLoanIndicatorDetailList(String period, String caliberType,
                                                           String dimension, String dimensionValue,
                                                           int page, int pageSize) {
        // 从 dw_indicator_fact 读取贷款明细（按维度）
        String dimType = dimension != null ? dimension : "TOTAL";
        String sql = "SELECT dim_name, indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = 'MONTH' AND dim_type = ? AND caliber_type = ? " +
            "AND indicator_code IN ('LOAN_BALANCE', 'INTEREST_INCOME', 'FTP_COST', 'RISK_COST', 'OP_COST', 'LOAN_PROFIT')";

        List<Object> params = new ArrayList<>(Arrays.asList(period, dimType, caliberType));

        if (dimensionValue != null && !dimensionValue.isEmpty()) {
            sql += " AND dim_name = ?";
            params.add(dimensionValue);
        }

        sql += " ORDER BY dim_name";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

        // 按维度名称分组
        Map<String, Map<String, BigDecimal>> dimMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String dimName = (String) row.get("dim_name");
            String indicatorCode = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");

            dimMap.computeIfAbsent(dimName, k -> new HashMap<>());
            dimMap.get(dimName).put(indicatorCode, value);
        }

        // 构建明细列表
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : dimMap.entrySet()) {
            Map<String, BigDecimal> values = entry.getValue();
            Map<String, Object> item = new HashMap<>();
            item.put("dim_name", entry.getKey());
            item.put("loan_balance", values.getOrDefault("LOAN_BALANCE", BigDecimal.ZERO));
            item.put("interest_income", values.getOrDefault("INTEREST_INCOME", BigDecimal.ZERO));
            item.put("ftp_cost", values.getOrDefault("FTP_COST", BigDecimal.ZERO));
            item.put("risk_cost", values.getOrDefault("RISK_COST", BigDecimal.ZERO));
            item.put("op_cost", values.getOrDefault("OP_COST", BigDecimal.ZERO));
            item.put("profit", values.getOrDefault("LOAN_PROFIT", BigDecimal.ZERO));
            list.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", list.size());
        result.put("list", list);
        return result;
    }

    @Override
    public Map<String, Object> getDepositIndicatorDetailList(String period, String caliberType,
                                                              String dimension, String dimensionValue,
                                                              int page, int pageSize) {
        // 从 dw_indicator_fact 读取存款明细（按维度）
        String dimType = dimension != null ? dimension : "TOTAL";
        String sql = "SELECT dim_name, indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = 'MONTH' AND dim_type = ? AND caliber_type = ? " +
            "AND indicator_code IN ('DEPOSIT_BALANCE', 'FTP_INCOME', 'OP_COST', 'DEPOSIT_PROFIT')";

        List<Object> params = new ArrayList<>(Arrays.asList(period, dimType, caliberType));

        if (dimensionValue != null && !dimensionValue.isEmpty()) {
            sql += " AND dim_name = ?";
            params.add(dimensionValue);
        }

        sql += " ORDER BY dim_name";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

        // 按维度名称分组
        Map<String, Map<String, BigDecimal>> dimMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String dimName = (String) row.get("dim_name");
            String indicatorCode = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");

            dimMap.computeIfAbsent(dimName, k -> new HashMap<>());
            dimMap.get(dimName).put(indicatorCode, value);
        }

        // 构建明细列表
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : dimMap.entrySet()) {
            Map<String, BigDecimal> values = entry.getValue();
            Map<String, Object> item = new HashMap<>();
            item.put("dim_name", entry.getKey());
            item.put("deposit_balance", values.getOrDefault("DEPOSIT_BALANCE", BigDecimal.ZERO));
            item.put("ftp_income", values.getOrDefault("FTP_INCOME", BigDecimal.ZERO));
            item.put("op_cost", values.getOrDefault("OP_COST", BigDecimal.ZERO));
            item.put("profit", values.getOrDefault("DEPOSIT_PROFIT", BigDecimal.ZERO));
            list.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", list.size());
        result.put("list", list);
        return result;
    }

    @Override
    public List<Map<String, Object>> getLoanIndicatorByDimension(String period, String caliberType, String dimension) {
        // 从 dw_indicator_fact 读取贷款维度汇总
        String sql = "SELECT dim_name, indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = 'MONTH' AND dim_type = ? AND caliber_type = ? " +
            "AND indicator_code IN ('LOAN_BALANCE', 'INTEREST_INCOME', 'FTP_COST', 'RISK_COST', 'OP_COST', 'LOAN_PROFIT') " +
            "ORDER BY dim_name";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, period, dimension, caliberType);

        // 按维度名称分组
        Map<String, Map<String, BigDecimal>> dimMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String dimName = (String) row.get("dim_name");
            String indicatorCode = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");

            dimMap.computeIfAbsent(dimName, k -> new HashMap<>());
            dimMap.get(dimName).put(indicatorCode, value);
        }

        // 构建结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : dimMap.entrySet()) {
            Map<String, BigDecimal> values = entry.getValue();
            Map<String, Object> item = new HashMap<>();
            item.put("dim_name", entry.getKey());
            item.put("total_balance", values.getOrDefault("LOAN_BALANCE", BigDecimal.ZERO));
            item.put("total_interest", values.getOrDefault("INTEREST_INCOME", BigDecimal.ZERO));
            item.put("total_ftp_cost", values.getOrDefault("FTP_COST", BigDecimal.ZERO));
            item.put("total_risk_cost", values.getOrDefault("RISK_COST", BigDecimal.ZERO));
            item.put("total_op_cost", values.getOrDefault("OP_COST", BigDecimal.ZERO));
            item.put("total_profit", values.getOrDefault("LOAN_PROFIT", BigDecimal.ZERO));
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getDepositIndicatorByDimension(String period, String caliberType, String dimension) {
        // 从 dw_indicator_fact 读取存款维度汇总
        String sql = "SELECT dim_name, indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = 'MONTH' AND dim_type = ? AND caliber_type = ? " +
            "AND indicator_code IN ('DEPOSIT_BALANCE', 'FTP_INCOME', 'OP_COST', 'DEPOSIT_PROFIT') " +
            "ORDER BY dim_name";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, period, dimension, caliberType);

        // 按维度名称分组
        Map<String, Map<String, BigDecimal>> dimMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String dimName = (String) row.get("dim_name");
            String indicatorCode = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");

            dimMap.computeIfAbsent(dimName, k -> new HashMap<>());
            dimMap.get(dimName).put(indicatorCode, value);
        }

        // 构建结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : dimMap.entrySet()) {
            Map<String, BigDecimal> values = entry.getValue();
            Map<String, Object> item = new HashMap<>();
            item.put("dim_name", entry.getKey());
            item.put("total_balance", values.getOrDefault("DEPOSIT_BALANCE", BigDecimal.ZERO));
            item.put("total_ftp_income", values.getOrDefault("FTP_INCOME", BigDecimal.ZERO));
            item.put("total_op_cost", values.getOrDefault("OP_COST", BigDecimal.ZERO));
            item.put("total_profit", values.getOrDefault("DEPOSIT_PROFIT", BigDecimal.ZERO));
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getLoanIndicatorTrend(int months, String caliberType) {
        // 从 dw_indicator_fact 读取贷款趋势
        String sql = "SELECT period, indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE indicator_code IN ('LOAN_BALANCE', 'INTEREST_INCOME', 'FTP_COST', 'RISK_COST', 'OP_COST', 'LOAN_PROFIT') " +
            "AND period_type = 'MONTH' AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "ORDER BY period DESC LIMIT ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, caliberType, months * 6);

        // 按期间分组
        Map<String, Map<String, BigDecimal>> periodMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String period = (String) row.get("period");
            String indicatorCode = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");

            periodMap.computeIfAbsent(period, k -> new HashMap<>());
            periodMap.get(period).put(indicatorCode, value);
        }

        // 构建趋势数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : periodMap.entrySet()) {
            Map<String, BigDecimal> values = entry.getValue();
            Map<String, Object> item = new HashMap<>();
            item.put("period", entry.getKey());
            item.put("total_balance", values.getOrDefault("LOAN_BALANCE", BigDecimal.ZERO));
            item.put("total_interest", values.getOrDefault("INTEREST_INCOME", BigDecimal.ZERO));
            item.put("total_ftp_cost", values.getOrDefault("FTP_COST", BigDecimal.ZERO));
            item.put("total_risk_cost", values.getOrDefault("RISK_COST", BigDecimal.ZERO));
            item.put("total_op_cost", values.getOrDefault("OP_COST", BigDecimal.ZERO));
            item.put("total_profit", values.getOrDefault("LOAN_PROFIT", BigDecimal.ZERO));
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getDepositIndicatorTrend(int months, String caliberType) {
        // 从 dw_indicator_fact 读取存款趋势
        String sql = "SELECT period, indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE indicator_code IN ('DEPOSIT_BALANCE', 'FTP_INCOME', 'OP_COST', 'DEPOSIT_PROFIT') " +
            "AND period_type = 'MONTH' AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "ORDER BY period DESC LIMIT ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, caliberType, months * 4);

        // 按期间分组
        Map<String, Map<String, BigDecimal>> periodMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String period = (String) row.get("period");
            String indicatorCode = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");

            periodMap.computeIfAbsent(period, k -> new HashMap<>());
            periodMap.get(period).put(indicatorCode, value);
        }

        // 构建趋势数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : periodMap.entrySet()) {
            Map<String, BigDecimal> values = entry.getValue();
            Map<String, Object> item = new HashMap<>();
            item.put("period", entry.getKey());
            item.put("total_balance", values.getOrDefault("DEPOSIT_BALANCE", BigDecimal.ZERO));
            item.put("total_ftp_income", values.getOrDefault("FTP_INCOME", BigDecimal.ZERO));
            item.put("total_op_cost", values.getOrDefault("OP_COST", BigDecimal.ZERO));
            item.put("total_profit", values.getOrDefault("DEPOSIT_PROFIT", BigDecimal.ZERO));
            result.add(item);
        }

        return result;
    }
}
