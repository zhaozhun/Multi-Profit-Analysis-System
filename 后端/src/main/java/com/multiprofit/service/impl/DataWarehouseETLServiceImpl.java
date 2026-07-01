package com.multiprofit.service.impl;

import com.multiprofit.service.DataWarehouseETLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据仓库ETL服务实现(重写版)
 * ODS日级数据 → DWD月度聚合 → DWS全维度层级上卷(MONTH/YEAR)
 */
@Service
public class DataWarehouseETLServiceImpl implements DataWarehouseETLService {

    private static final Logger log = LoggerFactory.getLogger(DataWarehouseETLServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> executeETL(String period) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            ensureDwdTableStructure();

            // 1. 执行费用分摊(使用真实ODS日级数据)
            log.info("开始执行费用分摊: {}", period);
            generateExpenseAllocationData(period);

            // 2. ETL: ODS日级 → DWD月度聚合 → DWS全维度
            log.info("开始执行ETL: {}", period);
            executeETLSQL(period);

            // 3. 年度累计
            String year = period.substring(0, 4);
            calculateYearlyAggregation(year);

            // 4. 统计结果
            Integer dwdLoanCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_loan_detail WHERE account_period = ?",
                Integer.class, period);
            Integer dwdDepositCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_deposit_detail WHERE account_period = ?",
                Integer.class, period);
            Integer dwFactMonthCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dw_indicator_fact WHERE period = ? AND period_type = 'MONTH'",
                Integer.class, period);
            Integer dwFactYearCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dw_indicator_fact WHERE period = ? AND period_type = 'YEAR'",
                Integer.class, year);

            long timeCost = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("period", period);
            result.put("dwdLoanCount", dwdLoanCount);
            result.put("dwdDepositCount", dwdDepositCount);
            result.put("dwFactMonthCount", dwFactMonthCount);
            result.put("dwFactYearCount", dwFactYearCount);
            result.put("duration", timeCost);
            result.put("message", "ETL执行成功");

            log.info("ETL执行完成: {}, 耗时: {}ms, DWD贷款:{}存款:{}, DWS月:{}年:{}",
                period, timeCost, dwdLoanCount, dwdDepositCount, dwFactMonthCount, dwFactYearCount);

        } catch (Exception e) {
            log.error("ETL执行失败: {}", period, e);
            result.put("success", false);
            result.put("message", "ETL执行失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 生成费用分摊数据(基于ODS日级真实数据,月末那一天)
     */
    private void generateExpenseAllocationData(String period) {
        jdbcTemplate.update("DELETE FROM expense_allocation_result WHERE period = ?", period);

        // 贷款运营成本 = 月末余额 × 0.5% / 12 (模拟,实际由分摊引擎计算)
        String loanSql = "INSERT INTO expense_allocation_result " +
            "(period, biz_id, biz_type, expense_type, expense_name, allocated_amount, factor_value, factor_ratio, rule_code, batch_no) " +
            "SELECT ?, l.biz_id, 'LOAN', 'OP_COST', '运营成本', " +
            "l.loan_balance * 0.005 / 12, l.loan_balance, 1, 'LOAN_BALANCE_RATIO', ? " +
            "FROM loan_indicator_detail l " +
            "WHERE l.account_period = ? AND l.stat_date = LAST_DAY(l.stat_date)";
        jdbcTemplate.update(loanSql, period, "BATCH_" + period, period);

        // 存款运营成本
        String depositSql = "INSERT INTO expense_allocation_result " +
            "(period, biz_id, biz_type, expense_type, expense_name, allocated_amount, factor_value, factor_ratio, rule_code, batch_no) " +
            "SELECT ?, d.biz_id, 'DEPOSIT', 'OP_COST', '运营成本', " +
            "d.deposit_balance * 0.003 / 12, d.deposit_balance, 1, 'DEPOSIT_BALANCE_RATIO', ? " +
            "FROM deposit_indicator_detail d " +
            "WHERE d.account_period = ? AND d.stat_date = LAST_DAY(d.stat_date)";
        jdbcTemplate.update(depositSql, period, "BATCH_" + period, period);

        log.info("费用分摊数据生成完成: {}", period);
    }

    /**
     * 确保DWD层表结构
     */
    private void ensureDwdTableStructure() {
        addColumnIfNotExists("dwd_loan_detail", "loan_profit", "DECIMAL(18,4)", "贷款利润");
        addColumnIfNotExists("dwd_loan_detail", "net_interest_margin", "DECIMAL(18,4)", "净利差");
        addColumnIfNotExists("dwd_loan_detail", "dept_id", "BIGINT", "部门ID");
        addColumnIfNotExists("dwd_loan_detail", "dept_name", "VARCHAR(200)", "部门名称");
        addColumnIfNotExists("dwd_deposit_detail", "deposit_profit", "DECIMAL(18,4)", "存款利润");
        addColumnIfNotExists("dwd_deposit_detail", "dept_id", "BIGINT", "部门ID");
        addColumnIfNotExists("dwd_deposit_detail", "dept_name", "VARCHAR(200)", "部门名称");
        log.info("DWD层表结构检查完成");
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnType, String comment) {
        try {
            String checkSql = "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName, columnName);
            if (count != null && count == 0) {
                String alterSql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType + " COMMENT '" + comment + "'";
                jdbcTemplate.execute(alterSql);
                log.info("添加列: {}.{}", tableName, columnName);
            }
        } catch (Exception e) {
            log.warn("检查/添加列失败: {}.{} - {}", tableName, columnName, e.getMessage());
        }
    }

    /**
     * 执行ETL核心SQL: ODS日级 → DWD月度 → DWS全维度层级上卷
     */
    private void executeETLSQL(String period) {
        // === 1. 清除DWD该期间数据 ===
        jdbcTemplate.update("DELETE FROM dwd_loan_detail WHERE account_period = ?", period);
        jdbcTemplate.update("DELETE FROM dwd_deposit_detail WHERE account_period = ?", period);

        // === 2. ODS日级 → DWD月度聚合(贷款) ===
        // 时点余额=月末那天,月度利息/FTP/风险=月内每日SUM,运营成本=分摊结果
        String loanDwdSql =
            "INSERT INTO dwd_loan_detail (" +
            "biz_id, account_period, caliber_type, " +
            "org_id, org_name, biz_line_id, biz_line_name, " +
            "product_id, product_name, channel_id, channel_name, " +
            "manager_id, manager_name, dept_id, dept_name, " +
            "customer_id, customer_name, " +
            "loan_balance, loan_monthly_interest, ftp_cost, risk_cost, op_cost, " +
            "loan_profit, net_interest_margin" +
            ") SELECT " +
            "l.biz_id, l.account_period, l.caliber_type, " +
            "l.org_id, l.org_name, l.biz_line_id, l.biz_line_name, " +
            "l.product_id, l.product_name, l.channel_id, l.channel_name, " +
            "l.manager_id, l.manager_name, l.dept_id, l.dept_name, " +
            "l.customer_id, l.customer_name, " +
            "MAX(CASE WHEN l.stat_date = LAST_DAY(l.stat_date) THEN l.loan_balance END), " +
            "SUM(l.loan_daily_interest), " +
            "SUM(l.ftp_cost), " +
            "SUM(l.risk_cost), " +
            "COALESCE(ea.op_cost, 0), " +
            "SUM(l.loan_daily_interest) - SUM(l.ftp_cost) - SUM(l.risk_cost) - COALESCE(ea.op_cost, 0), " +
            "SUM(l.loan_daily_interest) - SUM(l.ftp_cost) " +
            "FROM loan_indicator_detail l " +
            "LEFT JOIN (" +
            "  SELECT biz_id, SUM(allocated_amount) as op_cost " +
            "  FROM expense_allocation_result WHERE period = ? GROUP BY biz_id" +
            ") ea ON l.biz_id = ea.biz_id " +
            "WHERE l.account_period = ? " +
            "GROUP BY l.biz_id, l.account_period, l.caliber_type, l.org_id, l.org_name, " +
            "l.biz_line_id, l.biz_line_name, l.product_id, l.product_name, " +
            "l.channel_id, l.channel_name, l.manager_id, l.manager_name, " +
            "l.dept_id, l.dept_name, l.customer_id, l.customer_name";
        jdbcTemplate.update(loanDwdSql, period, period);

        // === 3. ODS日级 → DWD月度聚合(存款) ===
        String depositDwdSql =
            "INSERT INTO dwd_deposit_detail (" +
            "biz_id, account_period, caliber_type, " +
            "org_id, org_name, biz_line_id, biz_line_name, " +
            "product_id, product_name, channel_id, channel_name, " +
            "manager_id, manager_name, dept_id, dept_name, " +
            "customer_id, customer_name, " +
            "deposit_balance, deposit_monthly_interest, ftp_income, op_cost, deposit_profit" +
            ") SELECT " +
            "d.biz_id, d.account_period, d.caliber_type, " +
            "d.org_id, d.org_name, d.biz_line_id, d.biz_line_name, " +
            "d.product_id, d.product_name, d.channel_id, d.channel_name, " +
            "d.manager_id, d.manager_name, d.dept_id, d.dept_name, " +
            "d.customer_id, d.customer_name, " +
            "MAX(CASE WHEN d.stat_date = LAST_DAY(d.stat_date) THEN d.deposit_balance END), " +
            "SUM(d.deposit_daily_interest), " +
            "SUM(d.ftp_income), " +
            "COALESCE(ea.op_cost, 0), " +
            "SUM(d.ftp_income) - SUM(d.deposit_daily_interest) - COALESCE(ea.op_cost, 0) " +
            "FROM deposit_indicator_detail d " +
            "LEFT JOIN (" +
            "  SELECT biz_id, SUM(allocated_amount) as op_cost " +
            "  FROM expense_allocation_result WHERE period = ? GROUP BY biz_id" +
            ") ea ON d.biz_id = ea.biz_id " +
            "WHERE d.account_period = ? " +
            "GROUP BY d.biz_id, d.account_period, d.caliber_type, d.org_id, d.org_name, " +
            "d.biz_line_id, d.biz_line_name, d.product_id, d.product_name, " +
            "d.channel_id, d.channel_name, d.manager_id, d.manager_name, " +
            "d.dept_id, d.dept_name, d.customer_id, d.customer_name";
        jdbcTemplate.update(depositDwdSql, period, period);

        // === 4. 清除DWS MONTH数据 ===
        jdbcTemplate.update("DELETE FROM dw_indicator_fact WHERE period = ? AND period_type = 'MONTH'", period);

        // === 5. DWS MONTH: 全维度层级上卷 ===
        String[] dimTypes = {"ORG", "BIZ_LINE", "DEPT", "PRODUCT", "CHANNEL", "MANAGER", "CUSTOMER"};
        String[] dimTables = {"dim_organization", "dim_biz_line", "dim_dept",
                              "dim_product", "dim_channel", "dim_manager", "dim_customer_type"};

        for (int i = 0; i < dimTypes.length; i++) {
            String dimType = dimTypes[i];
            String dimTable = dimTables[i];
            String loanDimCol = getDimIdColumn(dimType);

            // 层级上卷: 沿parent_id链正确上卷
            for (int level = 3; level >= 1; level--) {
                // 贷款用 dl 别名, 存款用 dd 别名
                String loanJoin, depositJoin;
                if (level == 3) {
                    loanJoin = "JOIN " + dimTable + " d ON dl." + loanDimCol + " = d.id";
                    depositJoin = "JOIN " + dimTable + " d ON dd." + loanDimCol + " = d.id";
                } else if (level == 2) {
                    loanJoin = "JOIN " + dimTable + " leaf ON dl." + loanDimCol + " = leaf.id " +
                               "JOIN " + dimTable + " d ON leaf.parent_id = d.id";
                    depositJoin = "JOIN " + dimTable + " leaf ON dd." + loanDimCol + " = leaf.id " +
                                  "JOIN " + dimTable + " d ON leaf.parent_id = d.id";
                } else {
                    loanJoin = "JOIN " + dimTable + " leaf ON dl." + loanDimCol + " = leaf.id " +
                               "JOIN " + dimTable + " mid ON leaf.parent_id = mid.id " +
                               "JOIN " + dimTable + " d ON mid.parent_id = d.id";
                    depositJoin = "JOIN " + dimTable + " leaf ON dd." + loanDimCol + " = leaf.id " +
                                  "JOIN " + dimTable + " mid ON leaf.parent_id = mid.id " +
                                  "JOIN " + dimTable + " d ON mid.parent_id = d.id";
                }

                // 贷款指标
                String loanSql =
                    "INSERT IGNORE INTO dw_indicator_fact " +
                    "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
                    "SELECT 'LOAN_MONTHLY_INTEREST', ?, 'MONTH', ?, d.id, d.name, SUM(dl.loan_monthly_interest)/10000, 'ASSESS', NOW() " +
                    "FROM dwd_loan_detail dl " + loanJoin + " " +
                    "WHERE dl.account_period = ? AND d.level = ? GROUP BY d.id, d.name " +
                    "UNION ALL " +
                    "SELECT 'LOAN_FTP_COST', ?, 'MONTH', ?, d.id, d.name, SUM(dl.ftp_cost)/10000, 'ASSESS', NOW() " +
                    "FROM dwd_loan_detail dl " + loanJoin + " " +
                    "WHERE dl.account_period = ? AND d.level = ? GROUP BY d.id, d.name " +
                    "UNION ALL " +
                    "SELECT 'LOAN_RISK_COST', ?, 'MONTH', ?, d.id, d.name, SUM(dl.risk_cost)/10000, 'ASSESS', NOW() " +
                    "FROM dwd_loan_detail dl " + loanJoin + " " +
                    "WHERE dl.account_period = ? AND d.level = ? GROUP BY d.id, d.name " +
                    "UNION ALL " +
                    "SELECT 'LOAN_OP_COST', ?, 'MONTH', ?, d.id, d.name, SUM(dl.op_cost)/10000, 'ASSESS', NOW() " +
                    "FROM dwd_loan_detail dl " + loanJoin + " " +
                    "WHERE dl.account_period = ? AND d.level = ? GROUP BY d.id, d.name " +
                    "UNION ALL " +
                    "SELECT 'LOAN_MONTHLY_PROFIT', ?, 'MONTH', ?, d.id, d.name, SUM(dl.loan_profit)/10000, 'ASSESS', NOW() " +
                    "FROM dwd_loan_detail dl " + loanJoin + " " +
                    "WHERE dl.account_period = ? AND d.level = ? GROUP BY d.id, d.name";
                jdbcTemplate.update(loanSql,
                    period, dimType, period, level,
                    period, dimType, period, level,
                    period, dimType, period, level,
                    period, dimType, period, level,
                    period, dimType, period, level);

                // 存款指标
                String depositSql =
                    "INSERT IGNORE INTO dw_indicator_fact " +
                    "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
                    "SELECT 'FTP_MONTHLY_INCOME', ?, 'MONTH', ?, d.id, d.name, SUM(dd.ftp_income)/10000, 'ASSESS', NOW() " +
                    "FROM dwd_deposit_detail dd " + depositJoin + " " +
                    "WHERE dd.account_period = ? AND d.level = ? GROUP BY d.id, d.name " +
                    "UNION ALL " +
                    "SELECT 'INTEREST_MONTHLY_EXPENSE', ?, 'MONTH', ?, d.id, d.name, SUM(dd.deposit_monthly_interest)/10000, 'ASSESS', NOW() " +
                    "FROM dwd_deposit_detail dd " + depositJoin + " " +
                    "WHERE dd.account_period = ? AND d.level = ? GROUP BY d.id, d.name " +
                    "UNION ALL " +
                    "SELECT 'DEPOSIT_OP_COST', ?, 'MONTH', ?, d.id, d.name, SUM(dd.op_cost)/10000, 'ASSESS', NOW() " +
                    "FROM dwd_deposit_detail dd " + depositJoin + " " +
                    "WHERE dd.account_period = ? AND d.level = ? GROUP BY d.id, d.name " +
                    "UNION ALL " +
                    "SELECT 'DEPOSIT_MONTHLY_PROFIT', ?, 'MONTH', ?, d.id, d.name, SUM(dd.deposit_profit)/10000, 'ASSESS', NOW() " +
                    "FROM dwd_deposit_detail dd " + depositJoin + " " +
                    "WHERE dd.account_period = ? AND d.level = ? GROUP BY d.id, d.name";
                jdbcTemplate.update(depositSql,
                    period, dimType, period, level,
                    period, dimType, period, level,
                    period, dimType, period, level,
                    period, dimType, period, level);
            }
        }

        // === 5.5. 后计算 TOTAL_MONTHLY_PROFIT = LOAN_MONTHLY_PROFIT + DEPOSIT_MONTHLY_PROFIT ===
        String totalProfitSql =
            "INSERT IGNORE INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'TOTAL_MONTHLY_PROFIT', ?, 'MONTH', l.dim_type, l.dim_id, l.dim_name, " +
            "(l.calc_value + COALESCE(d.calc_value, 0)), 'ASSESS', NOW() " +
            "FROM dw_indicator_fact l " +
            "LEFT JOIN dw_indicator_fact d ON l.dim_type = d.dim_type AND l.dim_id = d.dim_id " +
            "AND l.period = d.period AND l.period_type = d.period_type " +
            "AND d.indicator_code = 'DEPOSIT_MONTHLY_PROFIT' " +
            "WHERE l.period = ? AND l.period_type = 'MONTH' " +
            "AND l.indicator_code = 'LOAN_MONTHLY_PROFIT' " +
            "AND l.dim_type != 'TOTAL'";
        jdbcTemplate.update(totalProfitSql, period, period);

        // === 6. DWS MONTH: TOTAL汇总 ===
        insertTotalMonthIndicators(period);

        log.info("ETL MONTH执行完成: {}", period);
    }

    /**
     * 插入MONTH TOTAL汇总行
     */
    private void insertTotalMonthIndicators(String period) {
        String totalSql =
            "INSERT IGNORE INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'LOAN_MONTHLY_INTEREST', ?, 'MONTH', 'TOTAL', 0, '全部', COALESCE(SUM(loan_monthly_interest),0)/10000, 'ASSESS', NOW() FROM dwd_loan_detail WHERE account_period = ? " +
            "UNION ALL SELECT 'LOAN_FTP_COST', ?, 'MONTH', 'TOTAL', 0, '全部', COALESCE(SUM(ftp_cost),0)/10000, 'ASSESS', NOW() FROM dwd_loan_detail WHERE account_period = ? " +
            "UNION ALL SELECT 'LOAN_RISK_COST', ?, 'MONTH', 'TOTAL', 0, '全部', COALESCE(SUM(risk_cost),0)/10000, 'ASSESS', NOW() FROM dwd_loan_detail WHERE account_period = ? " +
            "UNION ALL SELECT 'LOAN_OP_COST', ?, 'MONTH', 'TOTAL', 0, '全部', COALESCE(SUM(op_cost),0)/10000, 'ASSESS', NOW() FROM dwd_loan_detail WHERE account_period = ? " +
            "UNION ALL SELECT 'LOAN_MONTHLY_PROFIT', ?, 'MONTH', 'TOTAL', 0, '全部', COALESCE(SUM(loan_profit),0)/10000, 'ASSESS', NOW() FROM dwd_loan_detail WHERE account_period = ? " +
            "UNION ALL SELECT 'FTP_MONTHLY_INCOME', ?, 'MONTH', 'TOTAL', 0, '全部', COALESCE(SUM(ftp_income),0)/10000, 'ASSESS', NOW() FROM dwd_deposit_detail WHERE account_period = ? " +
            "UNION ALL SELECT 'INTEREST_MONTHLY_EXPENSE', ?, 'MONTH', 'TOTAL', 0, '全部', COALESCE(SUM(deposit_monthly_interest),0)/10000, 'ASSESS', NOW() FROM dwd_deposit_detail WHERE account_period = ? " +
            "UNION ALL SELECT 'DEPOSIT_OP_COST', ?, 'MONTH', 'TOTAL', 0, '全部', COALESCE(SUM(op_cost),0)/10000, 'ASSESS', NOW() FROM dwd_deposit_detail WHERE account_period = ? " +
            "UNION ALL SELECT 'DEPOSIT_MONTHLY_PROFIT', ?, 'MONTH', 'TOTAL', 0, '全部', COALESCE(SUM(deposit_profit),0)/10000, 'ASSESS', NOW() FROM dwd_deposit_detail WHERE account_period = ? " +
            "UNION ALL SELECT 'TOTAL_MONTHLY_PROFIT', ?, 'MONTH', 'TOTAL', 0, '全部', " +
            "((SELECT COALESCE(SUM(loan_profit),0) FROM dwd_loan_detail WHERE account_period = ?) + " +
            " (SELECT COALESCE(SUM(deposit_profit),0) FROM dwd_deposit_detail WHERE account_period = ?)) / 10000, 'ASSESS', NOW()";
        // 9 segments × 2 params + 1 segment × 3 params = 21 params
        jdbcTemplate.update(totalSql,
            period, period, period, period, period, period, period, period, period, period,
            period, period, period, period, period, period, period, period, period, period,
            period);
    }

    /**
     * 年度累计: 汇总MONTH数据
     */
    private void calculateYearlyAggregation(String year) {
        jdbcTemplate.update("DELETE FROM dw_indicator_fact WHERE period = ? AND period_type = 'YEAR'", year);

        String yearlySql =
            "INSERT IGNORE INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT indicator_code, ?, 'YEAR', dim_type, dim_id, dim_name, SUM(calc_value), caliber_type, NOW() " +
            "FROM dw_indicator_fact " +
            "WHERE period LIKE CONCAT(?, '-%') AND period_type = 'MONTH' " +
            "GROUP BY indicator_code, dim_type, dim_id, dim_name, caliber_type";
        jdbcTemplate.update(yearlySql, year, year);

        log.info("年度累计计算完成: {}", year);
    }

    /**
     * 维度类型 → DWD表ID列名映射
     */
    private String getDimIdColumn(String dimType) {
        switch (dimType) {
            case "ORG": return "org_id";
            case "BIZ_LINE": return "biz_line_id";
            case "DEPT": return "dept_id";
            case "PRODUCT": return "product_id";
            case "CHANNEL": return "channel_id";
            case "MANAGER": return "manager_id";
            case "CUSTOMER": return "customer_id";
            default: throw new IllegalArgumentException("Unknown dimType: " + dimType);
        }
    }
}
