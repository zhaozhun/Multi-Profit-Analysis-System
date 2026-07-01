package com.multiprofit.service.impl;

import com.multiprofit.dto.DashboardDTO;
import com.multiprofit.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 驾驶舱服务实现类
 * 数据源：dw_indicator_fact（预计算数据，dim_type='TOTAL'）
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取数据库中最新的月份
     */
    private String getLatestPeriod() {
        try {
            String latest = jdbcTemplate.queryForObject(
                "SELECT MAX(period) FROM dw_indicator_fact WHERE period_type = 'MONTH'", String.class);
            return latest != null ? latest : "2026-06";
        } catch (Exception e) {
            return "2026-06";
        }
    }

    /**
     * 智能获取期间：如果指定期间没有数据，自动使用最新期间
     */
    private String getEffectivePeriod(String startDate) {
        String requestedPeriod = startDate.substring(0, 7);
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM dw_indicator_fact WHERE period = ? AND period_type = 'MONTH'",
            Integer.class, requestedPeriod);
        if (count != null && count > 0) {
            return requestedPeriod;
        }
        return getLatestPeriod();
    }

    @Override
    public DashboardDTO getDashboardData(String startDate, String endDate, String caliberType, Long orgScope) {
        String period = getEffectivePeriod(startDate);

        // 从 dw_indicator_fact 获取汇总数据（dim_type = 'TOTAL'）
        String sql = "SELECT indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = 'MONTH' AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "AND indicator_code IN ('TOTAL_PROFIT', 'LOAN_PROFIT', 'DEPOSIT_PROFIT', 'INTEREST_INCOME', 'FTP_INCOME', 'FTP_COST', 'RISK_COST', 'OP_COST')";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, period, caliberType);

        // 构建指标值映射
        Map<String, BigDecimal> indicatorMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String code = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");
            indicatorMap.put(code, value);
        }

        // 构建 KPI 卡片
        List<DashboardDTO.KpiCard> kpiCards = new ArrayList<>();
        kpiCards.add(createKpiCard("TOTAL_PROFIT", "总利润", indicatorMap.getOrDefault("TOTAL_PROFIT", BigDecimal.ZERO), "万元"));
        kpiCards.add(createKpiCard("LOAN_PROFIT", "贷款利润", indicatorMap.getOrDefault("LOAN_PROFIT", BigDecimal.ZERO), "万元"));
        kpiCards.add(createKpiCard("DEPOSIT_PROFIT", "存款利润", indicatorMap.getOrDefault("DEPOSIT_PROFIT", BigDecimal.ZERO), "万元"));
        kpiCards.add(createKpiCard("INTEREST_INCOME", "利息收入", indicatorMap.getOrDefault("INTEREST_INCOME", BigDecimal.ZERO), "万元"));

        // 构建瀑布图数据
        DashboardDTO.WaterfallData waterfall = new DashboardDTO.WaterfallData();
        BigDecimal revenue = indicatorMap.getOrDefault("INTEREST_INCOME", BigDecimal.ZERO);
        BigDecimal ftpCost = indicatorMap.getOrDefault("FTP_COST", BigDecimal.ZERO);
        BigDecimal riskCost = indicatorMap.getOrDefault("RISK_COST", BigDecimal.ZERO);
        BigDecimal opCost = indicatorMap.getOrDefault("OP_COST", BigDecimal.ZERO);
        BigDecimal netProfit = indicatorMap.getOrDefault("TOTAL_PROFIT", BigDecimal.ZERO);

        waterfall.setRevenue(revenue);
        waterfall.setFtpCost(ftpCost);
        waterfall.setRiskCost(riskCost);
        waterfall.setOpCost(opCost);
        waterfall.setNetProfit(netProfit);
        waterfall.setFtpCostRatio(calculateRatio(ftpCost, revenue));
        waterfall.setRiskCostRatio(calculateRatio(riskCost, revenue));
        waterfall.setOpCostRatio(calculateRatio(opCost, revenue));

        // 构建趋势数据
        DashboardDTO.TrendData trend = getTrendData(endDate, caliberType);

        // 构建维度概览
        List<DashboardDTO.DimOverview> dimOverviews = new ArrayList<>();
        dimOverviews.add(getDimOverview("ORG", startDate, endDate, caliberType));

        // 组装结果
        DashboardDTO dashboard = new DashboardDTO();
        dashboard.setKpiCards(kpiCards);
        dashboard.setWaterfall(waterfall);
        dashboard.setTrend(trend);
        dashboard.setDimOverviews(dimOverviews);
        dashboard.setAlerts(new ArrayList<>());

        return dashboard;
    }

    @Override
    public DashboardDTO.WaterfallData getWaterfallData(String startDate, String endDate, String caliberType) {
        String period = startDate.substring(0, 7);

        String sql = "SELECT indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = 'MONTH' AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "AND indicator_code IN ('INTEREST_INCOME', 'FTP_COST', 'RISK_COST', 'OP_COST', 'TOTAL_PROFIT')";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, period, caliberType);

        Map<String, BigDecimal> indicatorMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String code = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");
            indicatorMap.put(code, value);
        }

        DashboardDTO.WaterfallData waterfall = new DashboardDTO.WaterfallData();
        BigDecimal revenue = indicatorMap.getOrDefault("INTEREST_INCOME", BigDecimal.ZERO);
        BigDecimal ftpCost = indicatorMap.getOrDefault("FTP_COST", BigDecimal.ZERO);
        BigDecimal riskCost = indicatorMap.getOrDefault("RISK_COST", BigDecimal.ZERO);
        BigDecimal opCost = indicatorMap.getOrDefault("OP_COST", BigDecimal.ZERO);
        BigDecimal netProfit = indicatorMap.getOrDefault("TOTAL_PROFIT", BigDecimal.ZERO);

        waterfall.setRevenue(revenue);
        waterfall.setFtpCost(ftpCost);
        waterfall.setRiskCost(riskCost);
        waterfall.setOpCost(opCost);
        waterfall.setNetProfit(netProfit);
        waterfall.setFtpCostRatio(calculateRatio(ftpCost, revenue));
        waterfall.setRiskCostRatio(calculateRatio(riskCost, revenue));
        waterfall.setOpCostRatio(calculateRatio(opCost, revenue));

        return waterfall;
    }

    @Override
    public DashboardDTO.TrendData getTrendData(String endDate, String caliberType) {
        // 根据endDate计算最近6个月的期间范围
        String endPeriod = endDate.substring(0, 7); // YYYY-MM
        // 计算6个月前的期间
        java.time.YearMonth endYM = java.time.YearMonth.parse(endPeriod);
        java.time.YearMonth startYM = endYM.minusMonths(5);
        String startPeriod = startYM.toString();

        // 获取指定范围内的趋势数据
        String sql = "SELECT period, indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE indicator_code IN ('TOTAL_PROFIT', 'INTEREST_INCOME', 'FTP_COST', 'RISK_COST', 'OP_COST') " +
            "AND period_type = 'MONTH' AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "AND period >= ? AND period <= ? " +
            "ORDER BY period ASC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, caliberType, startPeriod, endPeriod);

        // 按期间分组
        Map<String, Map<String, BigDecimal>> periodMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String period = (String) row.get("period");
            String indicatorCode = (String) row.get("indicator_code");
            BigDecimal value = (BigDecimal) row.get("calc_value");

            periodMap.computeIfAbsent(period, k -> new HashMap<>());
            periodMap.get(period).put(indicatorCode, value);
        }

        DashboardDTO.TrendData trend = new DashboardDTO.TrendData();
        List<String> months = new ArrayList<>();
        List<BigDecimal> profitTrend = new ArrayList<>();
        List<BigDecimal> revenueTrend = new ArrayList<>();
        List<BigDecimal> ftpCostTrend = new ArrayList<>();
        List<BigDecimal> riskCostTrend = new ArrayList<>();
        List<BigDecimal> opCostTrend = new ArrayList<>();

        for (Map.Entry<String, Map<String, BigDecimal>> entry : periodMap.entrySet()) {
            months.add(entry.getKey());
            Map<String, BigDecimal> values = entry.getValue();
            profitTrend.add(values.getOrDefault("TOTAL_PROFIT", BigDecimal.ZERO));
            revenueTrend.add(values.getOrDefault("INTEREST_INCOME", BigDecimal.ZERO));
            ftpCostTrend.add(values.getOrDefault("FTP_COST", BigDecimal.ZERO));
            riskCostTrend.add(values.getOrDefault("RISK_COST", BigDecimal.ZERO));
            opCostTrend.add(values.getOrDefault("OP_COST", BigDecimal.ZERO));
        }

        trend.setMonths(months);
        trend.setProfitTrend(profitTrend);
        trend.setRevenueTrend(revenueTrend);
        trend.setFtpCostTrend(ftpCostTrend);
        trend.setRiskCostTrend(riskCostTrend);
        trend.setOpCostTrend(opCostTrend);

        return trend;
    }

    @Override
    public DashboardDTO.DimOverview getDimOverview(String dimType, String startDate, String endDate, String caliberType) {
        String period = startDate.substring(0, 7);

        // 获取该维度的总利润前5名
        String sql = "SELECT dim_name, calc_value FROM dw_indicator_fact " +
            "WHERE indicator_code = 'TOTAL_PROFIT' AND period = ? AND period_type = 'MONTH' " +
            "AND dim_type = ? AND caliber_type = ? " +
            "ORDER BY calc_value DESC LIMIT 5";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, period, dimType, caliberType);

        DashboardDTO.DimOverview overview = new DashboardDTO.DimOverview();
        overview.setDimType(dimType);
        overview.setDimName(getDimName(dimType));

        List<DashboardDTO.DimTopItem> topItems = new ArrayList<>();
        List<DashboardDTO.DimPieItem> pieItems = new ArrayList<>();

        BigDecimal totalProfit = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            BigDecimal value = (BigDecimal) row.get("calc_value");
            totalProfit = totalProfit.add(value);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String name = (String) row.get("dim_name");
            BigDecimal value = (BigDecimal) row.get("calc_value");

            DashboardDTO.DimTopItem topItem = new DashboardDTO.DimTopItem();
            topItem.setId((long) (i + 1));
            topItem.setName(name);
            topItem.setNetProfit(value);
            topItem.setGrowth(BigDecimal.ZERO);
            topItems.add(topItem);

            DashboardDTO.DimPieItem pieItem = new DashboardDTO.DimPieItem();
            pieItem.setName(name);
            pieItem.setValue(value);
            pieItem.setRatio(totalProfit.compareTo(BigDecimal.ZERO) > 0 ?
                value.multiply(new BigDecimal("100")).divide(totalProfit, 2, BigDecimal.ROUND_HALF_UP) :
                BigDecimal.ZERO);
            pieItems.add(pieItem);
        }

        overview.setTopItems(topItems);
        overview.setPieItems(pieItems);

        return overview;
    }

    /**
     * 创建 KPI 卡片
     */
    private DashboardDTO.KpiCard createKpiCard(String code, String name, BigDecimal value, String unit) {
        DashboardDTO.KpiCard card = new DashboardDTO.KpiCard();
        card.setMetricCode(code);
        card.setMetricName(name);
        card.setValue(value);
        card.setUnit(unit);
        card.setYoyGrowth(BigDecimal.ZERO);
        card.setMomGrowth(BigDecimal.ZERO);
        card.setBudgetRate(BigDecimal.ZERO);
        card.setColor("#1890ff");
        return card;
    }

    /**
     * 计算比率
     */
    private BigDecimal calculateRatio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(new BigDecimal("100")).divide(denominator, 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 获取维度中文名称
     */
    private String getDimName(String dimType) {
        switch (dimType) {
            case "ORG": return "机构";
            case "BIZ_LINE": return "业务线";
            case "DEPT": return "部门";
            case "PRODUCT": return "产品";
            case "CHANNEL": return "渠道";
            case "MANAGER": return "客户经理";
            case "CUSTOMER": return "客户";
            default: return dimType;
        }
    }
}
