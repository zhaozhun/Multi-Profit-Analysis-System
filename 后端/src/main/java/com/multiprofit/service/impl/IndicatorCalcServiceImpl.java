package com.multiprofit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.multiprofit.entity.AtomicIndicator;
import com.multiprofit.entity.DerivedIndicator;
import com.multiprofit.entity.IndicatorPreCalc;
import com.multiprofit.entity.IndicatorStatConfig;
import com.multiprofit.mapper.AtomicIndicatorMapper;
import com.multiprofit.mapper.DerivedIndicatorMapper;
import com.multiprofit.mapper.IndicatorPreCalcMapper;
import com.multiprofit.mapper.IndicatorStatConfigMapper;
import com.multiprofit.service.IndicatorCalcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 指标计算服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorCalcServiceImpl implements IndicatorCalcService {

    private final AtomicIndicatorMapper atomicIndicatorMapper;
    private final DerivedIndicatorMapper derivedIndicatorMapper;
    private final IndicatorStatConfigMapper indicatorStatConfigMapper;
    private final IndicatorPreCalcMapper indicatorPreCalcMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> calcAtomicIndicator(String indicatorCode, String calcPeriod, String periodValue) {
        // 1. 获取原子指标配置
        AtomicIndicator indicator = atomicIndicatorMapper.selectById(indicatorCode);
        if (indicator == null) {
            throw new RuntimeException("原子指标不存在: " + indicatorCode);
        }

        // 2. 构建SQL查询
        String sql = buildAtomicIndicatorSql(indicator, calcPeriod, periodValue);

        // 3. 执行查询获取结果
        BigDecimal value = executeIndicatorSql(sql);

        // 4. 保存预计算结果
        savePreCalcResult(indicatorCode, "ATOMIC", null, calcPeriod, periodValue, value);

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("code", indicatorCode);
        result.put("name", indicator.getName());
        result.put("calcPeriod", calcPeriod);
        result.put("periodValue", periodValue);
        result.put("value", value);
        result.put("unit", indicator.getUnit());
        return result;
    }

    @Override
    public Map<String, Object> calcDerivedIndicator(String indicatorCode, String calcPeriod, String periodValue) {
        // 1. 获取派生指标配置
        DerivedIndicator indicator = derivedIndicatorMapper.selectById(indicatorCode);
        if (indicator == null) {
            throw new RuntimeException("派生指标不存在: " + indicatorCode);
        }

        // 2. 获取公式变量
        String formulaVars = indicator.getFormulaVars();
        List<String> vars = parseFormulaVars(formulaVars);

        // 3. 获取各变量的值
        Map<String, BigDecimal> varValues = new HashMap<>();
        for (String var : vars) {
            BigDecimal value = getIndicatorValue(var, calcPeriod, periodValue);
            varValues.put(var, value);
        }

        // 4. 计算公式
        BigDecimal value = calculateFormula(indicator.getCalcFormula(), varValues);

        // 5. 保存预计算结果
        savePreCalcResult(indicatorCode, "DERIVED", null, calcPeriod, periodValue, value);

        // 6. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("code", indicatorCode);
        result.put("name", indicator.getName());
        result.put("calcPeriod", calcPeriod);
        result.put("periodValue", periodValue);
        result.put("value", value);
        result.put("unit", indicator.getUnit());
        return result;
    }

    @Override
    public Map<String, Object> calcStatConfig(String indicatorCode, String statType, String calcPeriod, String periodValue) {
        // 1. 获取统计口径配置
        IndicatorStatConfig config = indicatorStatConfigMapper.selectOne(
            new QueryWrapper<IndicatorStatConfig>()
                .eq("indicator_code", indicatorCode)
                .eq("stat_type", statType)
        );
        if (config == null) {
            throw new RuntimeException("统计口径不存在: " + indicatorCode + " - " + statType);
        }

        // 2. 获取原子指标值
        BigDecimal atomicValue = getIndicatorValue(indicatorCode, calcPeriod, periodValue);

        // 3. 计算统计口径值
        BigDecimal value = calculateStatValue(atomicValue, statType, calcPeriod, periodValue);

        // 4. 保存预计算结果
        savePreCalcResult(indicatorCode, "ATOMIC", statType, calcPeriod, periodValue, value);

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("code", indicatorCode);
        result.put("statType", statType);
        result.put("calcPeriod", calcPeriod);
        result.put("periodValue", periodValue);
        result.put("value", value);
        result.put("unit", "万元");
        return result;
    }

    @Override
    @Transactional
    public List<Map<String, Object>> calcAllIndicators(String calcPeriod, String periodValue) {
        List<Map<String, Object>> results = new ArrayList<>();

        // 1. 计算所有原子指标
        List<AtomicIndicator> atomicIndicators = atomicIndicatorMapper.selectList(null);
        for (AtomicIndicator indicator : atomicIndicators) {
            try {
                Map<String, Object> result = calcAtomicIndicator(indicator.getCode(), calcPeriod, periodValue);
                results.add(result);
            } catch (Exception e) {
                log.error("计算原子指标失败: {}", indicator.getCode(), e);
            }
        }

        // 2. 计算所有派生指标
        List<DerivedIndicator> derivedIndicators = derivedIndicatorMapper.selectList(null);
        for (DerivedIndicator indicator : derivedIndicators) {
            try {
                Map<String, Object> result = calcDerivedIndicator(indicator.getCode(), calcPeriod, periodValue);
                results.add(result);
            } catch (Exception e) {
                log.error("计算派生指标失败: {}", indicator.getCode(), e);
            }
        }

        // 3. 计算所有统计口径
        List<IndicatorStatConfig> statConfigs = indicatorStatConfigMapper.selectList(null);
        for (IndicatorStatConfig config : statConfigs) {
            try {
                Map<String, Object> result = calcStatConfig(
                    config.getIndicatorCode(),
                    config.getStatType(),
                    calcPeriod,
                    periodValue
                );
                results.add(result);
            } catch (Exception e) {
                log.error("计算统计口径失败: {} - {}", config.getIndicatorCode(), config.getStatType(), e);
            }
        }

        return results;
    }

    @Override
    public Map<String, Object> getIndicatorDetail(String indicatorCode, String calcPeriod, String periodValue, int page, int size) {
        // 1. 获取原子指标配置
        AtomicIndicator indicator = atomicIndicatorMapper.selectById(indicatorCode);
        if (indicator == null) {
            throw new RuntimeException("原子指标不存在: " + indicatorCode);
        }

        // 2. 构建明细查询SQL
        String sql = buildDetailSql(indicator, calcPeriod, periodValue, page, size);

        // 3. 执行查询
        List<Map<String, Object>> details = executeDetailSql(sql);

        // 4. 构建分组数据
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

    // 私有辅助方法
    private String buildAtomicIndicatorSql(AtomicIndicator indicator, String calcPeriod, String periodValue) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT SUM(").append(indicator.getSourceField()).append(") AS value ");
        sql.append("FROM ").append(indicator.getSourceTable()).append(" ");
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

        return sql.toString();
    }

    private BigDecimal executeIndicatorSql(String sql) {
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            if (results.isEmpty() || results.get(0).get("value") == null) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(results.get(0).get("value").toString());
        } catch (Exception e) {
            log.error("执行指标SQL失败: {}", sql, e);
            return BigDecimal.ZERO;
        }
    }

    private List<Map<String, Object>> executeDetailSql(String sql) {
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("执行明细SQL失败: {}", sql, e);
            return new ArrayList<>();
        }
    }

    private List<String> parseFormulaVars(String formulaVars) {
        List<String> vars = new ArrayList<>();
        if (formulaVars == null || formulaVars.isEmpty()) {
            return vars;
        }
        // 解析JSON格式的公式变量
        // 格式：["LOAN_INTEREST","LOAN_FTP","LOAN_RISK","LOAN_OP"]
        String cleaned = formulaVars.replace("[", "").replace("]", "").replace("\"", "");
        String[] parts = cleaned.split(",");
        for (String part : parts) {
            vars.add(part.trim());
        }
        return vars;
    }

    private BigDecimal getIndicatorValue(String indicatorCode, String calcPeriod, String periodValue) {
        // 从预计算结果表获取指标值
        IndicatorPreCalc preCalc = indicatorPreCalcMapper.selectOne(
            new QueryWrapper<IndicatorPreCalc>()
                .eq("indicator_code", indicatorCode)
                .eq("calc_period", calcPeriod)
                .eq("period_value", periodValue)
                .isNull("stat_type")
        );
        return preCalc != null ? preCalc.getCalcValue() : BigDecimal.ZERO;
    }

    private BigDecimal calculateFormula(String formula, Map<String, BigDecimal> varValues) {
        // 替换公式中的变量为实际值
        String expression = formula;
        for (Map.Entry<String, BigDecimal> entry : varValues.entrySet()) {
            expression = expression.replace(entry.getKey(), entry.getValue().toString());
        }

        // 简单的公式计算（支持加减乘除）
        try {
            return evaluateExpression(expression);
        } catch (Exception e) {
            log.error("计算公式失败: {}", formula, e);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal evaluateExpression(String expression) {
        // 简单的表达式计算实现
        // 支持：+、-、*、/、()
        expression = expression.replaceAll("\\s+", "");

        // 处理括号
        while (expression.contains("(")) {
            int start = expression.lastIndexOf("(");
            int end = expression.indexOf(")", start);
            if (end == -1) {
                throw new RuntimeException("括号不匹配");
            }
            String subExpr = expression.substring(start + 1, end);
            BigDecimal subResult = evaluateExpression(subExpr);
            expression = expression.substring(0, start) + subResult + expression.substring(end + 1);
        }

        // 处理乘除
        while (expression.contains("*") || expression.contains("/")) {
            int mulIndex = expression.indexOf("*");
            int divIndex = expression.indexOf("/");

            int opIndex;
            if (mulIndex == -1) {
                opIndex = divIndex;
            } else if (divIndex == -1) {
                opIndex = mulIndex;
            } else {
                opIndex = Math.min(mulIndex, divIndex);
            }

            // 找到操作数
            int leftStart = opIndex - 1;
            while (leftStart > 0 && (Character.isDigit(expression.charAt(leftStart - 1)) || expression.charAt(leftStart - 1) == '.')) {
                leftStart--;
            }
            int rightEnd = opIndex + 1;
            while (rightEnd < expression.length() && (Character.isDigit(expression.charAt(rightEnd)) || expression.charAt(rightEnd) == '.')) {
                rightEnd++;
            }

            BigDecimal left = new BigDecimal(expression.substring(leftStart, opIndex));
            BigDecimal right = new BigDecimal(expression.substring(opIndex + 1, rightEnd));
            BigDecimal result;

            if (expression.charAt(opIndex) == '*') {
                result = left.multiply(right);
            } else {
                result = left.divide(right, 4, RoundingMode.HALF_UP);
            }

            expression = expression.substring(0, leftStart) + result + expression.substring(rightEnd);
        }

        // 处理加减
        while (expression.contains("+") || (expression.contains("-") && expression.indexOf("-") > 0)) {
            int addIndex = expression.indexOf("+");
            int subIndex = expression.indexOf("-", 1);

            int opIndex;
            if (addIndex == -1) {
                opIndex = subIndex;
            } else if (subIndex == -1) {
                opIndex = addIndex;
            } else {
                opIndex = Math.min(addIndex, subIndex);
            }

            // 找到操作数
            int leftStart = opIndex - 1;
            while (leftStart > 0 && (Character.isDigit(expression.charAt(leftStart - 1)) || expression.charAt(leftStart - 1) == '.')) {
                leftStart--;
            }
            int rightEnd = opIndex + 1;
            while (rightEnd < expression.length() && (Character.isDigit(expression.charAt(rightEnd)) || expression.charAt(rightEnd) == '.')) {
                rightEnd++;
            }

            BigDecimal left = new BigDecimal(expression.substring(leftStart, opIndex));
            BigDecimal right = new BigDecimal(expression.substring(opIndex + 1, rightEnd));
            BigDecimal result;

            if (expression.charAt(opIndex) == '+') {
                result = left.add(right);
            } else {
                result = left.subtract(right);
            }

            expression = expression.substring(0, leftStart) + result + expression.substring(rightEnd);
        }

        return new BigDecimal(expression);
    }

    private BigDecimal calculateStatValue(BigDecimal atomicValue, String statType, String calcPeriod, String periodValue) {
        if ("MONTHLY_DAILY_AVG".equals(statType)) {
            // 月日均 = 当月累计 / 当月天数
            int daysInMonth = getDaysInMonth(periodValue);
            return atomicValue.divide(BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP);
        } else if ("YEARLY_DAILY_AVG".equals(statType)) {
            // 年日均 = 当年累计 / 当年天数
            int daysInYear = getDaysInYear(periodValue);
            return atomicValue.divide(BigDecimal.valueOf(daysInYear), 4, RoundingMode.HALF_UP);
        }
        return atomicValue;
    }

    private int getDaysInMonth(String periodValue) {
        try {
            YearMonth yearMonth = YearMonth.parse(periodValue, DateTimeFormatter.ofPattern("yyyy-MM"));
            return yearMonth.lengthOfMonth();
        } catch (Exception e) {
            log.error("解析月份失败: {}", periodValue, e);
            return 30;
        }
    }

    private int getDaysInYear(String periodValue) {
        try {
            int year = Integer.parseInt(periodValue);
            return LocalDate.ofYearDay(year, 1).isLeapYear() ? 366 : 365;
        } catch (Exception e) {
            log.error("解析年份失败: {}", periodValue, e);
            return 365;
        }
    }

    private void savePreCalcResult(String indicatorCode, String indicatorType, String statType,
                                   String calcPeriod, String periodValue, BigDecimal value) {
        IndicatorPreCalc preCalc = new IndicatorPreCalc();
        preCalc.setIndicatorCode(indicatorCode);
        preCalc.setIndicatorType(indicatorType);
        preCalc.setStatType(statType);
        preCalc.setCalcPeriod(calcPeriod);
        preCalc.setPeriodValue(periodValue);
        preCalc.setCalcValue(value);
        preCalc.setCalcTime(LocalDateTime.now());
        preCalc.setStatus(1);

        indicatorPreCalcMapper.insert(preCalc);
    }

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
}
