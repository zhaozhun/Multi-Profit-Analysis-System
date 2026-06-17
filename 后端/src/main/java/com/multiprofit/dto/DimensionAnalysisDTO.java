package com.multiprofit.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 维度分析页数据DTO（支持贷款/存款利润拆分）
 */
@Data
public class DimensionAnalysisDTO {

    private String dimType;
    private String dimLabel;
    private Integer currentLevel;
    private List<Map<String, Object>> drillPath;
    private List<DashboardDTO.KpiCard> kpiCards;
    private DashboardDTO.WaterfallData waterfall;
    private List<RankItem> ranking;
    private List<DashboardDTO.DimPieItem> costStructure;
    private List<TreeNode> treeData;
    private TableData tableData;
    private CrossDimData crossDimData;
    private String aiInsight;
    private Map<String, Object> specialCharts;

    @Data
    public static class RankItem {
        private Long id;
        private String name;
        private String parentName;
        private BigDecimal netProfit;
        private BigDecimal loanProfit;
        private BigDecimal depositProfit;
        private BigDecimal revenue;
        private BigDecimal yoyGrowth;
        private Integer rankIndex;
    }

    @Data
    public static class TableData {
        private List<TableRow> rows;
        private long total;
        private int page;
        private int pageSize;
    }

    @Data
    public static class TableRow {
        private Long id;
        private String name;
        private String parentName;
        private Integer level;
        private boolean hasChildren;

        // 贷款数据
        private BigDecimal loanRevenue;
        private BigDecimal loanFtpCost;
        private BigDecimal loanRiskCost;
        private BigDecimal loanOpCost;
        private BigDecimal loanProfit;

        // 存款数据
        private BigDecimal depositRevenue;
        private BigDecimal depositInterest;
        private BigDecimal depositOpCost;
        private BigDecimal depositProfit;

        // 汇总数据
        private BigDecimal revenue;
        private BigDecimal ftpCost;
        private BigDecimal riskCost;
        private BigDecimal opCost;
        private BigDecimal netProfit;
        private BigDecimal profitYoy;
        private BigDecimal costIncomeRatio;
        private String profitStatus;
    }

    @Data
    public static class TreeNode {
        private Long id;
        private String key;
        private String name;
        private String code;
        private Integer level;
        private Integer childCount;
        private boolean leaf;

        // 贷款数据
        private BigDecimal loanRevenue;
        private BigDecimal loanFtpCost;
        private BigDecimal loanRiskCost;
        private BigDecimal loanOpCost;
        private BigDecimal loanProfit;

        // 存款数据
        private BigDecimal depositRevenue;
        private BigDecimal depositInterest;
        private BigDecimal depositOpCost;
        private BigDecimal depositProfit;

        // 汇总数据
        private BigDecimal revenue;
        private BigDecimal ftpCost;
        private BigDecimal riskCost;
        private BigDecimal opCost;
        private BigDecimal netProfit;
        private BigDecimal profitYoy;
        private BigDecimal costIncomeRatio;
        private String profitStatus;

        private List<TreeNode> children;
    }

    @Data
    public static class CrossDimData {
        private String crossDimType;
        private String crossDimLabel;
        private String fromDimName;
        private List<CrossDimRow> rows;
    }

    @Data
    public static class CrossDimRow {
        private String name;
        private BigDecimal loanRevenue;
        private BigDecimal loanFtpCost;
        private BigDecimal loanRiskCost;
        private BigDecimal loanOpCost;
        private BigDecimal loanProfit;
        private BigDecimal depositRevenue;
        private BigDecimal depositInterest;
        private BigDecimal depositOpCost;
        private BigDecimal depositProfit;
        private BigDecimal revenue;
        private BigDecimal netProfit;
        private BigDecimal costIncomeRatio;
        private String profitStatus;
    }

    @Data
    public static class BreakdownData {
        private String dimName;
        private BigDecimal totalProfit;
        private BigDecimal loanProfit;
        private BigDecimal depositProfit;
        private List<BreakdownItem> bySubDimension;
        private List<BreakdownItem> byProduct;
    }

    @Data
    public static class BreakdownItem {
        private String name;
        private BigDecimal loanProfit;
        private BigDecimal depositProfit;
        private BigDecimal total;
    }
}
