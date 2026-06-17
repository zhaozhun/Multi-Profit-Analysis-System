package com.multiprofit.service.impl;

import com.multiprofit.dto.DashboardDTO;
import com.multiprofit.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public DashboardDTO getDashboardData(String startDate, String endDate, String caliberType, Long orgScope) {
        DashboardDTO dto = new DashboardDTO();
        dto.setKpiCards(getKpiCards(startDate, endDate, caliberType));
        dto.setWaterfall(getWaterfallData(startDate, endDate, caliberType));
        dto.setTrend(getTrendData(endDate, caliberType));
        dto.setDimOverviews(getAllDimOverviews(startDate, endDate, caliberType));
        dto.setAlerts(new ArrayList<>());
        return dto;
    }

    private List<DashboardDTO.KpiCard> getKpiCards(String startDate, String endDate, String caliberType) {
        String sql = """
            SELECT
              sum(b.revenue) as revenue,
              sum(b.ftp_cost) as ftp_cost,
              sum(b.risk_cost) as risk_cost,
              sum(b.op_cost) as op_cost,
              sum(b.net_profit) as net_profit,
              sum(b.loan_revenue) as loan_revenue,
              sum(b.loan_ftp_cost) as loan_ftp_cost,
              sum(b.loan_risk_cost) as loan_risk_cost,
              sum(b.loan_op_cost) as loan_op_cost,
              sum(b.loan_profit) as loan_profit,
              sum(b.deposit_revenue) as deposit_revenue,
              sum(b.deposit_interest) as deposit_interest,
              sum(b.deposit_op_cost) as deposit_op_cost,
              sum(b.deposit_profit) as deposit_profit
            FROM biz_ledger b
            WHERE b.stat_date >= ? AND b.stat_date <= ? AND b.caliber_type = ?
            """;
        Map<String, Object> data = jdbcTemplate.queryForMap(sql, startDate, endDate, caliberType);

        List<DashboardDTO.KpiCard> cards = new ArrayList<>();
        cards.add(buildCard("总利润", toBD(data.get("net_profit")), "#52c41a"));
        cards.add(buildCard("贷款利润", toBD(data.get("loan_profit")), "#1890ff"));
        cards.add(buildCard("存款利润", toBD(data.get("deposit_profit")), "#722ed1"));
        cards.add(buildCard("贷款收入", toBD(data.get("loan_revenue")), "#36cfc9"));
        cards.add(buildCard("存款收入", toBD(data.get("deposit_revenue")), "#b37feb"));
        cards.add(buildCard("FTP成本", toBD(data.get("ftp_cost")), "#fa8c16"));
        cards.add(buildCard("风险成本", toBD(data.get("risk_cost")), "#f5222d"));
        cards.add(buildCard("运营成本", toBD(data.get("op_cost")), "#8c8c8c"));

        return cards;
    }

    @Override
    public DashboardDTO.WaterfallData getWaterfallData(String startDate, String endDate, String caliberType) {
        String sql = """
            SELECT
              sum(b.revenue) as revenue,
              sum(b.ftp_cost) as ftp_cost,
              sum(b.risk_cost) as risk_cost,
              sum(b.op_cost) as op_cost,
              sum(b.net_profit) as net_profit
            FROM biz_ledger b
            WHERE b.stat_date >= ? AND b.stat_date <= ? AND b.caliber_type = ?
            """;
        Map<String, Object> data = jdbcTemplate.queryForMap(sql, startDate, endDate, caliberType);

        DashboardDTO.WaterfallData waterfall = new DashboardDTO.WaterfallData();
        BigDecimal revenue = toBD(data.get("revenue"));
        waterfall.setRevenue(revenue);
        waterfall.setFtpCost(toBD(data.get("ftp_cost")));
        waterfall.setRiskCost(toBD(data.get("risk_cost")));
        waterfall.setOpCost(toBD(data.get("op_cost")));
        waterfall.setNetProfit(toBD(data.get("net_profit")));

        if (revenue.compareTo(BigDecimal.ZERO) != 0) {
            waterfall.setFtpCostRatio(waterfall.getFtpCost().multiply(new BigDecimal("100")).divide(revenue, 2, BigDecimal.ROUND_HALF_UP));
            waterfall.setRiskCostRatio(waterfall.getRiskCost().multiply(new BigDecimal("100")).divide(revenue, 2, BigDecimal.ROUND_HALF_UP));
            waterfall.setOpCostRatio(waterfall.getOpCost().multiply(new BigDecimal("100")).divide(revenue, 2, BigDecimal.ROUND_HALF_UP));
        }
        return waterfall;
    }

    @Override
    public DashboardDTO.TrendData getTrendData(String endDate, String caliberType) {
        String sql = """
            SELECT b.account_period,
              sum(b.revenue) as revenue,
              sum(b.net_profit) as net_profit,
              sum(b.ftp_cost) as ftp_cost,
              sum(b.risk_cost) as risk_cost,
              sum(b.op_cost) as op_cost
            FROM biz_ledger b
            WHERE b.account_period >= DATE_FORMAT(DATE_SUB(?, INTERVAL 11 MONTH), '%Y-%m')
            AND b.account_period <= DATE_FORMAT(?, '%Y-%m')
            AND b.caliber_type = ?
            GROUP BY b.account_period
            ORDER BY b.account_period
            """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, endDate, endDate, caliberType);
        DashboardDTO.TrendData trend = new DashboardDTO.TrendData();
        trend.setMonths(new ArrayList<>());
        trend.setRevenueTrend(new ArrayList<>());
        trend.setProfitTrend(new ArrayList<>());
        trend.setFtpCostTrend(new ArrayList<>());
        trend.setRiskCostTrend(new ArrayList<>());
        trend.setOpCostTrend(new ArrayList<>());

        for (Map<String, Object> row : rows) {
            trend.getMonths().add(String.valueOf(row.get("account_period")));
            trend.getRevenueTrend().add(toBD(row.get("revenue")));
            trend.getProfitTrend().add(toBD(row.get("net_profit")));
            trend.getFtpCostTrend().add(toBD(row.get("ftp_cost")));
            trend.getRiskCostTrend().add(toBD(row.get("risk_cost")));
            trend.getOpCostTrend().add(toBD(row.get("op_cost")));
        }
        return trend;
    }

    @Override
    public DashboardDTO.DimOverview getDimOverview(String dimType, String startDate, String endDate, String caliberType) {
        DashboardDTO.DimOverview overview = new DashboardDTO.DimOverview();
        overview.setDimType(dimType);

        String idCol = getDimIdColumn(dimType);
        overview.setDimName(getDimLabel(dimType));

        // TOP5 - 使用 JOIN 查询
        String topSql = String.format("""
            SELECT dm.name, sum(b.net_profit) as net_profit, sum(b.revenue) as revenue
            FROM biz_ledger b
            JOIN dimension_master dm ON b.%s = dm.id
            WHERE b.stat_date >= ? AND b.stat_date <= ? AND b.caliber_type = ?
            GROUP BY dm.name
            ORDER BY net_profit DESC
            LIMIT 5
            """, idCol);
        List<Map<String, Object>> topRows = jdbcTemplate.queryForList(topSql, startDate, endDate, caliberType);
        List<DashboardDTO.DimTopItem> topItems = new ArrayList<>();
        for (Map<String, Object> row : topRows) {
            DashboardDTO.DimTopItem item = new DashboardDTO.DimTopItem();
            item.setName(String.valueOf(row.get("name")));
            item.setNetProfit(toBD(row.get("net_profit")));
            topItems.add(item);
        }
        overview.setTopItems(topItems);

        // 占比 - 使用 JOIN 查询
        String pieSql = String.format("""
            SELECT dm.name, sum(b.net_profit) as val
            FROM biz_ledger b
            JOIN dimension_master dm ON b.%s = dm.id
            WHERE b.stat_date >= ? AND b.stat_date <= ? AND b.caliber_type = ?
            GROUP BY dm.name
            ORDER BY val DESC
            """, idCol);
        List<Map<String, Object>> pieRows = jdbcTemplate.queryForList(pieSql, startDate, endDate, caliberType);
        BigDecimal total = pieRows.stream().map(r -> toBD(r.get("val"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<DashboardDTO.DimPieItem> pieItems = new ArrayList<>();
        for (Map<String, Object> row : pieRows) {
            DashboardDTO.DimPieItem item = new DashboardDTO.DimPieItem();
            item.setName(String.valueOf(row.get("name")));
            item.setValue(toBD(row.get("val")));
            if (total.compareTo(BigDecimal.ZERO) != 0) {
                item.setRatio(item.getValue().multiply(new BigDecimal("100")).divide(total, 2, BigDecimal.ROUND_HALF_UP));
            }
            pieItems.add(item);
        }
        overview.setPieItems(pieItems);

        return overview;
    }

    private List<DashboardDTO.DimOverview> getAllDimOverviews(String startDate, String endDate, String caliberType) {
        List<DashboardDTO.DimOverview> overviews = new ArrayList<>();
        overviews.add(getDimOverview("ORG", startDate, endDate, caliberType));
        overviews.add(getDimOverview("BIZ_LINE", startDate, endDate, caliberType));
        overviews.add(getDimOverview("DEPT", startDate, endDate, caliberType));
        overviews.add(getDimOverview("PRODUCT", startDate, endDate, caliberType));
        overviews.add(getDimOverview("CHANNEL", startDate, endDate, caliberType));
        overviews.add(getDimOverview("MANAGER", startDate, endDate, caliberType));
        return overviews;
    }

    // === Helper Methods ===

    /**
     * 维度ID列名映射（外键字段）
     */
    private String getDimIdColumn(String dimType) {
        return switch (dimType) {
            case "ORG" -> "org_id";
            case "BIZ_LINE" -> "biz_line_id";
            case "DEPT" -> "dept_id";
            case "PRODUCT" -> "product_id";
            case "CHANNEL" -> "channel_id";
            case "MANAGER" -> "manager_id";
            case "CUSTOMER" -> "customer_id";
            default -> "org_id";
        };
    }

    private String getDimLabel(String dimType) {
        return switch (dimType) {
            case "ORG" -> "机构维度";
            case "BIZ_LINE" -> "条线维度";
            case "DEPT" -> "部门维度";
            case "PRODUCT" -> "产品维度";
            case "CHANNEL" -> "渠道维度";
            case "MANAGER" -> "客户经理维度";
            case "CUSTOMER" -> "客户维度";
            default -> "维度";
        };
    }

    private BigDecimal toBD(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }

    private DashboardDTO.KpiCard buildCard(String name, BigDecimal value, String color) {
        DashboardDTO.KpiCard card = new DashboardDTO.KpiCard();
        card.setMetricName(name);
        card.setValue(value);
        card.setColor(color);
        card.setUnit("万元");
        return card;
    }
}
