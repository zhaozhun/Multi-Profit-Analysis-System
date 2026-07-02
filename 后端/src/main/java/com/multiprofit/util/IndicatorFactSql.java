package com.multiprofit.util;

/**
 * dw_indicator_fact 透视SQL生成器
 * 将EAV长表(indicator_code+calc_value)透视为宽列，供报表/治理/AI探查/异常检测统一复用。
 * 维度名称来自 dim_name 列(ETL已回填)；如需精确名称可自行JOIN dim_*表。
 */
public final class IndicatorFactSql {

    /** 支持的指标编码 —— 决定透视列 */
    public static final String
        TOTAL_PROFIT   = "TOTAL_MONTHLY_PROFIT",
        LOAN_PROFIT    = "LOAN_MONTHLY_PROFIT",
        DEPOSIT_PROFIT = "DEPOSIT_MONTHLY_PROFIT",
        LOAN_INTEREST  = "LOAN_MONTHLY_INTEREST",
        FTP_INCOME     = "FTP_MONTHLY_INCOME",
        INTEREST_EXP   = "INTEREST_MONTHLY_EXPENSE",
        LOAN_FTP_COST  = "LOAN_FTP_COST",
        LOAN_RISK_COST = "LOAN_RISK_COST",
        LOAN_OP_COST   = "LOAN_OP_COST",
        DEPOSIT_OP_COST= "DEPOSIT_OP_COST";

    private IndicatorFactSql() {}

    /**
     * 维度类型 → 维度主数据表名
     */
    public static String dimTable(String dimType) {
        return switch (dimType) {
            case "ORG" -> "dim_organization";
            case "BIZ_LINE" -> "dim_biz_line";
            case "DEPT" -> "dim_dept";
            case "PRODUCT" -> "dim_product";
            case "CHANNEL" -> "dim_channel";
            case "MANAGER" -> "dim_manager";
            case "CUSTOMER" -> "dim_customer_type";
            default -> null;
        };
    }

    /**
     * 生成按维度透视的子查询 pivot(仅叶子节点 level=3,避免层级重复计数)
     * 输出列: dim_id, dim_name,
     *   loan_interest, ftp_income, interest_expense,
     *   ftp_cost, risk_cost, loan_op_cost, deposit_op_cost,
     *   loan_profit, deposit_profit,
     *   revenue(=loan_interest+ftp_income), op_cost(=loan_op_cost+deposit_op_cost),
     *   net_profit(=loan_profit+deposit_profit)
     * @param dimType 维度类型 ORG/BIZ_LINE/DEPT/PRODUCT/CHANNEL/CUSTOMER/MANAGER
     * @param period  YYYY-MM
     */
    public static String pivot(String dimType, String period) {
        String tbl = dimTable(dimType);
        String joinAndFilter = tbl != null
            ? " JOIN " + tbl + " dm ON dw_indicator_fact.dim_id=dm.id AND dm.level=3 "
            : " ";
        return "SELECT dim_id, dim_name, " +
            pv(LOAN_INTEREST, "loan_interest") + ", " +
            pv(FTP_INCOME, "ftp_income") + ", " +
            pv(INTEREST_EXP, "interest_expense") + ", " +
            pv(LOAN_FTP_COST, "ftp_cost") + ", " +
            pv(LOAN_RISK_COST, "risk_cost") + ", " +
            pv(LOAN_OP_COST, "loan_op_cost") + ", " +
            pv(DEPOSIT_OP_COST, "deposit_op_cost") + ", " +
            pv(LOAN_PROFIT, "loan_profit") + ", " +
            pv(DEPOSIT_PROFIT, "deposit_profit") + ", " +
            "(COALESCE(" + pv0(LOAN_INTEREST) + ",0)+COALESCE(" + pv0(FTP_INCOME) + ",0)) as revenue, " +
            "(COALESCE(" + pv0(LOAN_OP_COST) + ",0)+COALESCE(" + pv0(DEPOSIT_OP_COST) + ",0)) as op_cost, " +
            "(COALESCE(" + pv0(LOAN_PROFIT) + ",0)+COALESCE(" + pv0(DEPOSIT_PROFIT) + ",0)) as net_profit " +
            "FROM dw_indicator_fact" + joinAndFilter +
            "WHERE period_type='MONTH' AND period='" + esc(period) + "' " +
            "AND dim_type='" + esc(dimType) + "' AND caliber_type='ASSESS' " +
            "GROUP BY dim_id, dim_name";
    }

    /**
     * 全行汇总(TOTAL维度)的单行透视
     */
    public static String pivotTotal(String period) {
        return "SELECT " +
            pv(LOAN_INTEREST, "loan_interest") + ", " +
            pv(FTP_INCOME, "ftp_income") + ", " +
            pv(INTEREST_EXP, "interest_expense") + ", " +
            pv(LOAN_FTP_COST, "ftp_cost") + ", " +
            pv(LOAN_RISK_COST, "risk_cost") + ", " +
            pv(LOAN_OP_COST, "loan_op_cost") + ", " +
            pv(DEPOSIT_OP_COST, "deposit_op_cost") + ", " +
            pv(LOAN_PROFIT, "loan_profit") + ", " +
            pv(DEPOSIT_PROFIT, "deposit_profit") + ", " +
            pv(TOTAL_PROFIT, "net_profit") + ", " +
            "(COALESCE(" + pv0(LOAN_INTEREST) + ",0)+COALESCE(" + pv0(FTP_INCOME) + ",0)) as revenue, " +
            "(COALESCE(" + pv0(LOAN_OP_COST) + ",0)+COALESCE(" + pv0(DEPOSIT_OP_COST) + ",0)) as op_cost " +
            "FROM dw_indicator_fact " +
            "WHERE period_type='MONTH' AND period='" + esc(period) + "' " +
            "AND dim_type='TOTAL' AND caliber_type='ASSESS'";
    }

    /** 单指标按维度取值的SQL片段(用于排名等) */
    public static String metricByDim(String dimType, String indicatorCode, String period) {
        return "SELECT dim_id, dim_name, calc_value FROM dw_indicator_fact " +
            "WHERE period_type='MONTH' AND period='" + esc(period) + "' " +
            "AND dim_type='" + esc(dimType) + "' AND caliber_type='ASSESS' " +
            "AND indicator_code='" + esc(indicatorCode) + "' ORDER BY calc_value DESC";
    }

    /** 单指标按期间的汇总值(用于趋势) */
    public static String metricTrend(String indicatorCode, String dimType) {
        String dt = dimType != null ? " AND dim_type='" + esc(dimType) + "'" : " AND dim_type='TOTAL'";
        return "SELECT period, SUM(calc_value) as value FROM dw_indicator_fact " +
            "WHERE period_type='MONTH' AND caliber_type='ASSESS' AND indicator_code='" + esc(indicatorCode) + "'" + dt +
            " GROUP BY period ORDER BY period";
    }

    private static String pv(String code, String alias) {
        return "MAX(CASE WHEN indicator_code='" + code + "' THEN calc_value END) as " + alias;
    }
    private static String pv0(String code) {
        return "MAX(CASE WHEN indicator_code='" + code + "' THEN calc_value END)";
    }
    public static String esc(String s) {
        return s == null ? "" : s.replace("'", "''");
    }
}
