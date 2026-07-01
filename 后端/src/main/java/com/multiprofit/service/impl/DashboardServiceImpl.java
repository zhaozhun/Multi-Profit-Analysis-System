package com.multiprofit.service.impl;

import com.multiprofit.dto.DashboardDTO;
import com.multiprofit.service.DashboardService;
import com.multiprofit.service.IndicatorLibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 驾驶舱服务实现类(改造版)
 * 数据源：dw_indicator_fact(period_type+period), 指标库驱动KPI卡
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IndicatorLibraryService indicatorLibraryService;

    @Override
    public DashboardDTO getDashboardData(String startDate, String endDate, String caliberType, Long orgScope) {
        // 1. 确定期间类型和期间值
        String periodType = determinePeriodType(startDate, endDate);
        String period = determinePeriod(startDate, endDate, periodType);

        // 2. 从指标库取驾驶舱需要的指标code列表
        List<String> profitCodes = indicatorLibraryService.getCodesByCategory("PROFIT");
        List<String> revenueCodes = indicatorLibraryService.getCodesByCategory("REVENUE");
        List<String> costCodes = indicatorLibraryService.getCodesByCategory("COST");

        // 过滤出MONTH/YEAR级别的指标(排除DAILY)
        List<String> allCodes = new ArrayList<>();
        for (String code : profitCodes) {
            Map<String, Object> ind = indicatorLibraryService.getIndicatorByCode(code);
            if (ind != null) {
                String periods = (String) ind.get("pre_calc_periods");
                if (periods != null && periods.contains(periodType)) {
                    allCodes.add(code);
                }
            }
        }
        for (String code : revenueCodes) {
            Map<String, Object> ind = indicatorLibraryService.getIndicatorByCode(code);
            if (ind != null) {
                String periods = (String) ind.get("pre_calc_periods");
                if (periods != null && periods.contains(periodType)) {
                    allCodes.add(code);
                }
            }
        }
        for (String code : costCodes) {
            Map<String, Object> ind = indicatorLibraryService.getIndicatorByCode(code);
            if (ind != null) {
                String periods = (String) ind.get("pre_calc_periods");
                if (periods != null && periods.contains(periodType)) {
                    allCodes.add(code);
                }
            }
        }

        if (allCodes.isEmpty()) {
            // fallback: use MONTH codes
            periodType = "MONTH";
            period = startDate.substring(0, 7);
            allCodes = indicatorLibraryService.getAllActiveCodes();
        }

        // 3. 从DWS直读
        String placeholders = String.join(",", Collections.nCopies(allCodes.size(), "?"));
        String sql = "SELECT indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = ? AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "AND indicator_code IN (" + placeholders + ")";

        List<Object> params = new ArrayList<>();
        params.add(period);
        params.add(periodType);
        params.add(caliberType);
        params.addAll(allCodes);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

        // 4. 构建指标值映射
        Map<String, BigDecimal> indicatorMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String code = (String) row.get("indicator_code");
            Object val = row.get("calc_value");
            BigDecimal value = val != null ? new BigDecimal(val.toString()) : BigDecimal.ZERO;
            indicatorMap.put(code, value);
        }

        // 5. 构建KPI卡片(由指标库驱动)
        List<DashboardDTO.KpiCard> kpiCards = new ArrayList<>();
        for (String code : allCodes) {
            Map<String, Object> indicator = indicatorLibraryService.getIndicatorByCode(code);
            if (indicator != null) {
                kpiCards.add(createKpiCard(code, (String) indicator.get("name"),
                    indicatorMap.getOrDefault(code, BigDecimal.ZERO), (String) indicator.get("unit")));
            }
        }

        // 6. 瀑布图(使用新指标码)
        DashboardDTO.WaterfallData waterfall = new DashboardDTO.WaterfallData();
        BigDecimal loanRevenue = indicatorMap.getOrDefault("LOAN_MONTHLY_INTEREST", BigDecimal.ZERO);
        BigDecimal ftpIncome = indicatorMap.getOrDefault("FTP_MONTHLY_INCOME", BigDecimal.ZERO);
        BigDecimal totalRevenue = loanRevenue.add(ftpIncome);
        BigDecimal loanFtpCost = indicatorMap.getOrDefault("LOAN_FTP_COST", BigDecimal.ZERO);
        BigDecimal loanRiskCost = indicatorMap.getOrDefault("LOAN_RISK_COST", BigDecimal.ZERO);
        BigDecimal loanOpCost = indicatorMap.getOrDefault("LOAN_OP_COST", BigDecimal.ZERO);
        BigDecimal depositOpCost = indicatorMap.getOrDefault("DEPOSIT_OP_COST", BigDecimal.ZERO);
        BigDecimal interestExpense = indicatorMap.getOrDefault("INTEREST_MONTHLY_EXPENSE", BigDecimal.ZERO);
        BigDecimal totalCost = loanFtpCost.add(loanRiskCost).add(loanOpCost).add(depositOpCost).add(interestExpense);
        BigDecimal netProfit = indicatorMap.getOrDefault("TOTAL_MONTHLY_PROFIT", BigDecimal.ZERO);

        waterfall.setRevenue(totalRevenue);
        waterfall.setFtpCost(loanFtpCost);
        waterfall.setRiskCost(loanRiskCost);
        waterfall.setOpCost(loanOpCost.add(depositOpCost));
        waterfall.setNetProfit(netProfit);
        waterfall.setFtpCostRatio(calculateRatio(loanFtpCost, totalRevenue));
        waterfall.setRiskCostRatio(calculateRatio(loanRiskCost, totalRevenue));
        waterfall.setOpCostRatio(calculateRatio(loanOpCost.add(depositOpCost), totalRevenue));

        // 7. 趋势数据
        DashboardDTO.TrendData trend = getTrendData(endDate, caliberType);

        // 8. 维度概览
        List<DashboardDTO.DimOverview> dimOverviews = new ArrayList<>();
        dimOverviews.add(getDimOverview("ORG", startDate, endDate, caliberType));

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
        String periodType = determinePeriodType(startDate, endDate);
        String period = determinePeriod(startDate, endDate, periodType);

        String sql = "SELECT indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE period = ? AND period_type = ? AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "AND indicator_code IN ('LOAN_MONTHLY_INTEREST','FTP_MONTHLY_INCOME','LOAN_FTP_COST','LOAN_RISK_COST','LOAN_OP_COST','DEPOSIT_OP_COST','INTEREST_MONTHLY_EXPENSE','TOTAL_MONTHLY_PROFIT')";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, period, periodType, caliberType);

        Map<String, BigDecimal> indicatorMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object val = row.get("calc_value");
            indicatorMap.put((String) row.get("indicator_code"),
                val != null ? new BigDecimal(val.toString()) : BigDecimal.ZERO);
        }

        DashboardDTO.WaterfallData waterfall = new DashboardDTO.WaterfallData();
        BigDecimal loanRevenue = indicatorMap.getOrDefault("LOAN_MONTHLY_INTEREST", BigDecimal.ZERO);
        BigDecimal ftpIncome = indicatorMap.getOrDefault("FTP_MONTHLY_INCOME", BigDecimal.ZERO);
        BigDecimal totalRevenue = loanRevenue.add(ftpIncome);
        BigDecimal loanFtpCost = indicatorMap.getOrDefault("LOAN_FTP_COST", BigDecimal.ZERO);
        BigDecimal loanRiskCost = indicatorMap.getOrDefault("LOAN_RISK_COST", BigDecimal.ZERO);
        BigDecimal loanOpCost = indicatorMap.getOrDefault("LOAN_OP_COST", BigDecimal.ZERO);
        BigDecimal depositOpCost = indicatorMap.getOrDefault("DEPOSIT_OP_COST", BigDecimal.ZERO);
        BigDecimal interestExpense = indicatorMap.getOrDefault("INTEREST_MONTHLY_EXPENSE", BigDecimal.ZERO);

        waterfall.setRevenue(totalRevenue);
        waterfall.setFtpCost(loanFtpCost);
        waterfall.setRiskCost(loanRiskCost);
        waterfall.setOpCost(loanOpCost.add(depositOpCost));
        waterfall.setNetProfit(indicatorMap.getOrDefault("TOTAL_MONTHLY_PROFIT", BigDecimal.ZERO));
        waterfall.setFtpCostRatio(calculateRatio(loanFtpCost, totalRevenue));
        waterfall.setRiskCostRatio(calculateRatio(loanRiskCost, totalRevenue));
        waterfall.setOpCostRatio(calculateRatio(loanOpCost.add(depositOpCost), totalRevenue));

        return waterfall;
    }

    @Override
    public DashboardDTO.TrendData getTrendData(String endDate, String caliberType) {
        // 最近6个月
        String endPeriod = endDate.substring(0, 7);
        java.time.YearMonth endYM = java.time.YearMonth.parse(endPeriod);
        java.time.YearMonth startYM = endYM.minusMonths(5);
        String startPeriod = startYM.toString();

        String sql = "SELECT period, indicator_code, calc_value FROM dw_indicator_fact " +
            "WHERE indicator_code IN ('TOTAL_MONTHLY_PROFIT','LOAN_MONTHLY_INTEREST','FTP_MONTHLY_INCOME','LOAN_FTP_COST','LOAN_RISK_COST','LOAN_OP_COST','DEPOSIT_OP_COST','INTEREST_MONTHLY_EXPENSE') " +
            "AND period_type = 'MONTH' AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "AND period >= ? AND period <= ? ORDER BY period ASC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, caliberType, startPeriod, endPeriod);

        Map<String, Map<String, BigDecimal>> periodMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String period = (String) row.get("period");
            String code = (String) row.get("indicator_code");
            Object val = row.get("calc_value");
            BigDecimal value = val != null ? new BigDecimal(val.toString()) : BigDecimal.ZERO;
            periodMap.computeIfAbsent(period, k -> new HashMap<>()).put(code, value);
        }

        DashboardDTO.TrendData trend = new DashboardDTO.TrendData();
        List<String> months = new ArrayList<>();
        List<BigDecimal> profitTrend = new ArrayList<>();
        List<BigDecimal> revenueTrend = new ArrayList<>();
        List<BigDecimal> costTrend = new ArrayList<>();

        for (Map.Entry<String, Map<String, BigDecimal>> entry : periodMap.entrySet()) {
            months.add(entry.getKey());
            Map<String, BigDecimal> values = entry.getValue();
            profitTrend.add(values.getOrDefault("TOTAL_MONTHLY_PROFIT", BigDecimal.ZERO));
            BigDecimal loanInt = values.getOrDefault("LOAN_MONTHLY_INTEREST", BigDecimal.ZERO);
            BigDecimal ftpInc = values.getOrDefault("FTP_MONTHLY_INCOME", BigDecimal.ZERO);
            revenueTrend.add(loanInt.add(ftpInc));
            BigDecimal totalCost = values.getOrDefault("LOAN_FTP_COST", BigDecimal.ZERO)
                .add(values.getOrDefault("LOAN_RISK_COST", BigDecimal.ZERO))
                .add(values.getOrDefault("LOAN_OP_COST", BigDecimal.ZERO))
                .add(values.getOrDefault("DEPOSIT_OP_COST", BigDecimal.ZERO))
                .add(values.getOrDefault("INTEREST_MONTHLY_EXPENSE", BigDecimal.ZERO));
            costTrend.add(totalCost);
        }

        trend.setMonths(months);
        trend.setProfitTrend(profitTrend);
        trend.setRevenueTrend(revenueTrend);
        trend.setFtpCostTrend(new ArrayList<>());
        trend.setRiskCostTrend(new ArrayList<>());
        trend.setOpCostTrend(costTrend);

        return trend;
    }

    @Override
    public DashboardDTO.DimOverview getDimOverview(String dimType, String startDate, String endDate, String caliberType) {
        String periodType = determinePeriodType(startDate, endDate);
        String period = determinePeriod(startDate, endDate, periodType);
        String dimTable = getDimTable(dimType);

        String sql = "SELECT f.dim_id, dm.name as dim_name, f.calc_value FROM dw_indicator_fact f " +
            "JOIN " + dimTable + " dm ON f.dim_id = dm.id " +
            "WHERE f.indicator_code = 'TOTAL_MONTHLY_PROFIT' AND f.period = ? AND f.period_type = ? " +
            "AND f.dim_type = ? AND f.caliber_type = ? ORDER BY f.calc_value DESC LIMIT 5";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, period, periodType, dimType, caliberType);

        DashboardDTO.DimOverview overview = new DashboardDTO.DimOverview();
        overview.setDimType(dimType);
        overview.setDimName(getDimName(dimType));

        List<DashboardDTO.DimTopItem> topItems = new ArrayList<>();
        List<DashboardDTO.DimPieItem> pieItems = new ArrayList<>();

        BigDecimal totalProfit = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            Object val = row.get("calc_value");
            BigDecimal value = val != null ? new BigDecimal(val.toString()) : BigDecimal.ZERO;
            totalProfit = totalProfit.add(value);
        }

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String name = (String) row.get("dim_name");
            Object val = row.get("calc_value");
            BigDecimal value = val != null ? new BigDecimal(val.toString()) : BigDecimal.ZERO;

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
                value.multiply(new BigDecimal("100")).divide(totalProfit, 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);
            pieItems.add(pieItem);
        }

        overview.setTopItems(topItems);
        overview.setPieItems(pieItems);

        return overview;
    }

    // ========== 辅助方法 ==========

    private String determinePeriodType(String startDate, String endDate) {
        if (startDate == null) return "MONTH";
        if (startDate.endsWith("-01-01") && endDate != null && endDate.endsWith("-12-31")) return "YEAR";
        if (startDate.equals(endDate)) return "DAY";
        return "MONTH";
    }

    private String determinePeriod(String startDate, String endDate, String periodType) {
        if (startDate == null) {
            java.time.LocalDate now = java.time.LocalDate.now();
            return now.minusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        switch (periodType) {
            case "YEAR": return startDate.substring(0, 4);
            case "MONTH": return startDate.substring(0, 7);
            case "DAY": return startDate;
            default: return startDate.substring(0, 7);
        }
    }

    private String getDimTable(String dimType) {
        switch (dimType) {
            case "ORG": return "dim_organization";
            case "BIZ_LINE": return "dim_biz_line";
            case "DEPT": return "dim_dept";
            case "PRODUCT": return "dim_product";
            case "CHANNEL": return "dim_channel";
            case "MANAGER": return "dim_manager";
            case "CUSTOMER": return "dim_customer_type";
            default: return "dim_organization";
        }
    }

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

    private BigDecimal calculateRatio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return numerator.multiply(new BigDecimal("100")).divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
