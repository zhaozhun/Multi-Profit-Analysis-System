package com.multiprofit.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 驾驶舱数据DTO
 */
@Data
public class DashboardDTO {

    /** 核心KPI指标卡 */
    private List<KpiCard> kpiCards;

    /** 利润瀑布图数据 */
    private WaterfallData waterfall;

    /** 趋势数据 */
    private TrendData trend;

    /** 维度盈利概览 */
    private List<DimOverview> dimOverviews;

    /** 异常预警列表 */
    private List<AlertDTO> alerts;

    @Data
    public static class KpiCard {
        private String metricCode;
        private String metricName;
        private BigDecimal value;
        private String unit;
        private BigDecimal yoyGrowth;     // 同比增速
        private BigDecimal momGrowth;     // 环比增速
        private BigDecimal budgetRate;    // 预算完成率
        private String color;             // 卡片颜色
    }

    @Data
    public static class WaterfallData {
        private BigDecimal revenue;
        private BigDecimal ftpCost;
        private BigDecimal riskCost;
        private BigDecimal opCost;
        private BigDecimal netProfit;
        private BigDecimal ftpCostRatio;   // FTP成本占收入比
        private BigDecimal riskCostRatio;
        private BigDecimal opCostRatio;
    }

    @Data
    public static class TrendData {
        private List<String> months;
        private List<BigDecimal> revenueTrend;
        private List<BigDecimal> profitTrend;
        private List<BigDecimal> ftpCostTrend;
        private List<BigDecimal> riskCostTrend;
        private List<BigDecimal> opCostTrend;
    }

    @Data
    public static class DimOverview {
        private String dimType;
        private String dimName;
        private List<DimTopItem> topItems;
        private List<DimPieItem> pieItems;
    }

    @Data
    public static class DimTopItem {
        private Long id;
        private String name;
        private BigDecimal netProfit;
        private BigDecimal growth;
    }

    @Data
    public static class DimPieItem {
        private String name;
        private BigDecimal value;
        private BigDecimal ratio;
    }

    @Data
    public static class AlertDTO {
        private Long id;
        private String level;
        private String alertType;
        private String content;
        private BigDecimal anomalyValue;
        private String dimType;
        private String dimName;
        private String status;
        private String createTime;
    }
}
