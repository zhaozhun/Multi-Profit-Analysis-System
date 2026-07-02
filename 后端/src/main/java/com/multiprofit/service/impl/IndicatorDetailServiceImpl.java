package com.multiprofit.service.impl;

import com.multiprofit.service.IndicatorDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 指标明细服务实现类
 * 数据源：dwd_loan_detail / dwd_deposit_detail（DWD月度明细，含余额/利息/各成本/维度名）
 * 注：原实现查 dw_indicator_fact 用了不存在的 indicator_code(LOAN_BALANCE/INTEREST_INCOME 等)，
 * 且 EAV 表不存余额/笔数，故改为直查 DWD 明细表聚合。
 */
@Service
public class IndicatorDetailServiceImpl implements IndicatorDetailService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 维度类型 → DWD表维度列名(snake_case) */
    private static String dimColumn(String dimension) {
        if (dimension == null) return null;
        return switch (dimension) {
            case "ORG" -> "org_name";
            case "BIZ_LINE" -> "biz_line_name";
            case "PRODUCT" -> "product_name";
            case "CHANNEL" -> "channel_name";
            case "MANAGER" -> "manager_name";
            case "CUSTOMER" -> "customer_name";
            case "DEPT" -> "dept_name";
            default -> null;
        };
    }

    /** 取 period 的年份前缀(YYYY)用于 YTD 累计 */
    private static String yearOf(String period) {
        return period != null && period.length() >= 4 ? period.substring(0, 4) : "2026";
    }

    @Override
    public Map<String, Object> getLoanIndicatorSummary(String period, String caliberType) {
        // 当月汇总
        String sql = "SELECT " +
            "COUNT(*) AS total_count, " +
            "COALESCE(SUM(loan_balance),0) AS total_balance, " +
            "COALESCE(SUM(loan_monthly_interest),0) AS total_monthly_interest, " +
            "COALESCE(SUM(ftp_cost),0) AS total_ftp_cost, " +
            "COALESCE(SUM(risk_cost),0) AS total_risk_cost, " +
            "COALESCE(SUM(op_cost),0) AS total_op_cost, " +
            "COALESCE(SUM(loan_profit),0) AS total_profit, " +
            "CASE WHEN SUM(loan_balance)>0 THEN SUM(loan_monthly_interest)/SUM(loan_balance) ELSE 0 END AS avg_rate, " +
            "COALESCE(SUM(loan_monthly_interest),0)/30 AS total_daily_interest " +
            "FROM dwd_loan_detail WHERE account_period = ? AND caliber_type = ?";

        Map<String, Object> summary = new HashMap<>(jdbcTemplate.queryForMap(sql, period, caliberType));

        // 当年累计利息(YTD: 同年且 <= 当期)
        String ytdSql = "SELECT COALESCE(SUM(loan_monthly_interest),0) AS total_cumulative_interest " +
            "FROM dwd_loan_detail WHERE account_period LIKE ? AND account_period <= ? AND caliber_type = ?";
        Map<String, Object> ytd = jdbcTemplate.queryForMap(ytdSql, yearOf(period) + "-%", period, caliberType);
        summary.put("total_cumulative_interest", ytd.get("total_cumulative_interest"));

        return summary;
    }

    @Override
    public Map<String, Object> getDepositIndicatorSummary(String period, String caliberType) {
        String sql = "SELECT " +
            "COUNT(*) AS total_count, " +
            "COALESCE(SUM(deposit_balance),0) AS total_balance, " +
            "COALESCE(SUM(deposit_monthly_interest),0) AS total_monthly_interest, " +
            "COALESCE(SUM(ftp_income),0) AS total_ftp_income, " +
            "COALESCE(SUM(op_cost),0) AS total_op_cost, " +
            "COALESCE(SUM(deposit_profit),0) AS total_profit, " +
            "CASE WHEN SUM(deposit_balance)>0 THEN SUM(deposit_monthly_interest)/SUM(deposit_balance) ELSE 0 END AS avg_rate, " +
            "COALESCE(SUM(deposit_monthly_interest),0)/30 AS total_daily_interest " +
            "FROM dwd_deposit_detail WHERE account_period = ? AND caliber_type = ?";

        Map<String, Object> summary = new HashMap<>(jdbcTemplate.queryForMap(sql, period, caliberType));

        String ytdSql = "SELECT COALESCE(SUM(deposit_monthly_interest),0) AS total_cumulative_interest " +
            "FROM dwd_deposit_detail WHERE account_period LIKE ? AND account_period <= ? AND caliber_type = ?";
        Map<String, Object> ytd = jdbcTemplate.queryForMap(ytdSql, yearOf(period) + "-%", period, caliberType);
        summary.put("total_cumulative_interest", ytd.get("total_cumulative_interest"));

        return summary;
    }

    @Override
    public Map<String, Object> getLoanIndicatorDetailList(String period, String caliberType,
                                                           String dimension, String dimensionValue,
                                                           int page, int pageSize) {
        String dimCol = dimColumn(dimension);
        List<Object> params = new ArrayList<>();
        // 按维度值筛选(可选)
        String dimFilter = "";
        if (dimensionValue != null && !dimensionValue.isEmpty() && dimCol != null) {
            dimFilter = " AND " + dimCol + " = ?";
            params.add(dimensionValue);
        }

        // 总数
        String countSql = "SELECT COUNT(*) AS cnt FROM dwd_loan_detail WHERE account_period = ? AND caliber_type = ?" + dimFilter;
        List<Object> countParams = new ArrayList<>(Arrays.asList(period, caliberType));
        countParams.addAll(params);
        int total = ((Number) jdbcTemplate.queryForMap(countSql, countParams.toArray()).get("cnt")).intValue();

        // 分页列表(别名转驼峰,匹配前端 dataIndex)
        int offset = Math.max(page - 1, 0) * pageSize;
        String listSql = "SELECT biz_id AS bizId, customer_name AS customerName, org_name AS orgName, " +
            "biz_line_name AS bizLineName, product_name AS productName, channel_name AS channelName, " +
            "manager_name AS managerName, loan_balance AS loanBalance, " +
            "loan_monthly_interest AS loanMonthlyInterest, loan_monthly_interest/30 AS loanDailyInterest, " +
            "ftp_cost AS ftpCost, risk_cost AS riskCost, op_cost AS opCost, loan_profit AS loanProfit, " +
            "CASE WHEN loan_balance>0 THEN loan_monthly_interest/loan_balance ELSE 0 END AS loanRate, " +
            "CASE WHEN loan_balance>0 THEN ftp_cost/loan_balance ELSE 0 END AS ftpRate " +
            "FROM dwd_loan_detail WHERE account_period = ? AND caliber_type = ?" + dimFilter +
            " ORDER BY loan_balance DESC LIMIT ? OFFSET ?";

        List<Object> listParams = new ArrayList<>(Arrays.asList(period, caliberType));
        listParams.addAll(params);
        listParams.add(pageSize);
        listParams.add(offset);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(listSql, listParams.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", list);
        return result;
    }

    @Override
    public Map<String, Object> getDepositIndicatorDetailList(String period, String caliberType,
                                                              String dimension, String dimensionValue,
                                                              int page, int pageSize) {
        String dimCol = dimColumn(dimension);
        List<Object> params = new ArrayList<>();
        String dimFilter = "";
        if (dimensionValue != null && !dimensionValue.isEmpty() && dimCol != null) {
            dimFilter = " AND " + dimCol + " = ?";
            params.add(dimensionValue);
        }

        String countSql = "SELECT COUNT(*) AS cnt FROM dwd_deposit_detail WHERE account_period = ? AND caliber_type = ?" + dimFilter;
        List<Object> countParams = new ArrayList<>(Arrays.asList(period, caliberType));
        countParams.addAll(params);
        int total = ((Number) jdbcTemplate.queryForMap(countSql, countParams.toArray()).get("cnt")).intValue();

        int offset = Math.max(page - 1, 0) * pageSize;
        String listSql = "SELECT biz_id AS bizId, customer_name AS customerName, org_name AS orgName, " +
            "biz_line_name AS bizLineName, product_name AS productName, channel_name AS channelName, " +
            "manager_name AS managerName, deposit_balance AS depositBalance, " +
            "deposit_monthly_interest AS depositMonthlyInterest, deposit_monthly_interest/30 AS depositDailyInterest, " +
            "ftp_income AS ftpIncome, op_cost AS opCost, deposit_profit AS depositProfit, " +
            "CASE WHEN deposit_balance>0 THEN deposit_monthly_interest/deposit_balance ELSE 0 END AS depositRate, " +
            "CASE WHEN deposit_balance>0 THEN ftp_income/deposit_balance ELSE 0 END AS ftpRate " +
            "FROM dwd_deposit_detail WHERE account_period = ? AND caliber_type = ?" + dimFilter +
            " ORDER BY deposit_balance DESC LIMIT ? OFFSET ?";

        List<Object> listParams = new ArrayList<>(Arrays.asList(period, caliberType));
        listParams.addAll(params);
        listParams.add(pageSize);
        listParams.add(offset);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(listSql, listParams.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", list);
        return result;
    }

    @Override
    public List<Map<String, Object>> getLoanIndicatorByDimension(String period, String caliberType, String dimension) {
        String dimCol = dimColumn(dimension);
        if (dimCol == null) return Collections.emptyList();

        String sql = "SELECT " + dimCol + " AS dim_name, " +
            "COUNT(*) AS count, " +
            "COALESCE(SUM(loan_balance),0) AS total_balance, " +
            "COALESCE(SUM(loan_monthly_interest),0) AS total_interest, " +
            "COALESCE(SUM(ftp_cost),0) AS total_ftp_cost, " +
            "COALESCE(SUM(risk_cost),0) AS total_risk_cost, " +
            "COALESCE(SUM(op_cost),0) AS total_op_cost, " +
            "COALESCE(SUM(loan_profit),0) AS total_profit, " +
            "CASE WHEN SUM(loan_balance)>0 THEN SUM(loan_monthly_interest)/SUM(loan_balance) ELSE 0 END AS avg_rate " +
            "FROM dwd_loan_detail WHERE account_period = ? AND caliber_type = ? AND " + dimCol + " IS NOT NULL " +
            "GROUP BY " + dimCol + " ORDER BY total_balance DESC";

        return jdbcTemplate.queryForList(sql, period, caliberType);
    }

    @Override
    public List<Map<String, Object>> getDepositIndicatorByDimension(String period, String caliberType, String dimension) {
        String dimCol = dimColumn(dimension);
        if (dimCol == null) return Collections.emptyList();

        String sql = "SELECT " + dimCol + " AS dim_name, " +
            "COUNT(*) AS count, " +
            "COALESCE(SUM(deposit_balance),0) AS total_balance, " +
            "COALESCE(SUM(deposit_monthly_interest),0) AS total_interest, " +
            "COALESCE(SUM(ftp_income),0) AS total_ftp_income, " +
            "COALESCE(SUM(op_cost),0) AS total_op_cost, " +
            "COALESCE(SUM(deposit_profit),0) AS total_profit, " +
            "CASE WHEN SUM(deposit_balance)>0 THEN SUM(deposit_monthly_interest)/SUM(deposit_balance) ELSE 0 END AS avg_rate " +
            "FROM dwd_deposit_detail WHERE account_period = ? AND caliber_type = ? AND " + dimCol + " IS NOT NULL " +
            "GROUP BY " + dimCol + " ORDER BY total_balance DESC";

        return jdbcTemplate.queryForList(sql, period, caliberType);
    }

    @Override
    public List<Map<String, Object>> getLoanIndicatorTrend(int months, String caliberType) {
        String sql = "SELECT account_period AS period, " +
            "COUNT(*) AS total_count, " +
            "COALESCE(SUM(loan_balance),0) AS total_balance, " +
            "COALESCE(SUM(loan_monthly_interest),0) AS total_interest, " +
            "COALESCE(SUM(ftp_cost),0) AS total_ftp_cost, " +
            "COALESCE(SUM(risk_cost),0) AS total_risk_cost, " +
            "COALESCE(SUM(op_cost),0) AS total_op_cost, " +
            "COALESCE(SUM(loan_profit),0) AS total_profit " +
            "FROM dwd_loan_detail WHERE caliber_type = ? " +
            "GROUP BY account_period ORDER BY account_period DESC LIMIT ?";
        return jdbcTemplate.queryForList(sql, caliberType, months);
    }

    @Override
    public List<Map<String, Object>> getDepositIndicatorTrend(int months, String caliberType) {
        String sql = "SELECT account_period AS period, " +
            "COUNT(*) AS total_count, " +
            "COALESCE(SUM(deposit_balance),0) AS total_balance, " +
            "COALESCE(SUM(deposit_monthly_interest),0) AS total_interest, " +
            "COALESCE(SUM(ftp_income),0) AS total_ftp_income, " +
            "COALESCE(SUM(op_cost),0) AS total_op_cost, " +
            "COALESCE(SUM(deposit_profit),0) AS total_profit " +
            "FROM dwd_deposit_detail WHERE caliber_type = ? " +
            "GROUP BY account_period ORDER BY account_period DESC LIMIT ?";
        return jdbcTemplate.queryForList(sql, caliberType, months);
    }
}
