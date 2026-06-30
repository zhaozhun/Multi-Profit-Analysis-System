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
 * 数据仓库ETL服务实现
 * 调用数据库存储过程sp_etl_recalculate完成数据清洗和指标计算
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
            // 0. 确保表结构正确
            ensureDwdTableStructure();

            // 1. 执行费用分摊（当前用模拟数据）
            log.info("开始执行费用分摊: {}", period);
            generateExpenseAllocationData(period);

            // 2. 执行ETL（直接执行SQL，不依赖存储过程）
            log.info("开始执行ETL: {}", period);
            executeETLSQL(period);

            // 3. 统计结果
            Integer dwdLoanCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_loan_detail WHERE account_period = ?",
                Integer.class, period);
            Integer dwdDepositCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_deposit_detail WHERE account_period = ?",
                Integer.class, period);
            Integer dwFactCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dw_indicator_fact WHERE period = ?",
                Integer.class, period);

            long timeCost = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("period", period);
            result.put("dwdLoanCount", dwdLoanCount);
            result.put("dwdDepositCount", dwdDepositCount);
            result.put("dwFactCount", dwFactCount);
            result.put("duration", timeCost);
            result.put("message", "ETL执行成功");

            log.info("ETL执行完成: {}, 耗时: {}ms", period, timeCost);

        } catch (Exception e) {
            log.error("ETL执行失败: {}", period, e);
            result.put("success", false);
            result.put("message", "ETL执行失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 生成模拟费用分摊数据
     * 在ETL执行前，为每笔业务生成运营成本分摊结果
     */
    private void generateExpenseAllocationData(String period) {
        // 清除旧数据
        jdbcTemplate.update("DELETE FROM expense_allocation_result WHERE period = ?", period);

        // 生成贷款业务的运营成本
        String loanSql = "INSERT INTO expense_allocation_result " +
            "(period, biz_id, biz_type, expense_type, expense_name, allocated_amount, factor_value, factor_ratio, rule_code, batch_no) " +
            "SELECT ?, l.biz_id, 'LOAN', 'OP_COST', '运营成本', " +
            "l.loan_balance * (0.01 + RAND() * 0.02), l.loan_balance, 1, 'SIMULATED', ? " +
            "FROM loan_indicator_detail l WHERE l.account_period = ?";
        jdbcTemplate.update(loanSql, period, "BATCH_" + period, period);

        // 生成存款业务的运营成本
        String depositSql = "INSERT INTO expense_allocation_result " +
            "(period, biz_id, biz_type, expense_type, expense_name, allocated_amount, factor_value, factor_ratio, rule_code, batch_no) " +
            "SELECT ?, d.biz_id, 'DEPOSIT', 'OP_COST', '运营成本', " +
            "d.deposit_balance * (0.01 + RAND() * 0.02), d.deposit_balance, 1, 'SIMULATED', ? " +
            "FROM deposit_indicator_detail d WHERE d.account_period = ?";
        jdbcTemplate.update(depositSql, period, "BATCH_" + period, period);

        log.info("模拟费用分摊数据生成完成: {}", period);
    }

    /**
     * 确保DWD层表有正确的结构
     */
    private void ensureDwdTableStructure() {
        // 检查并添加loan_profit列
        addColumnIfNotExists("dwd_loan_detail", "loan_profit", "DECIMAL(18,4)", "贷款利润");
        addColumnIfNotExists("dwd_loan_detail", "net_interest_margin", "DECIMAL(18,4)", "净利差");
        addColumnIfNotExists("dwd_deposit_detail", "deposit_profit", "DECIMAL(18,4)", "存款利润");

        // 修复表的字符集和排序规则
        fixTableCollation("dwd_loan_detail");
        fixTableCollation("dwd_deposit_detail");
        fixTableCollation("loan_indicator_detail");
        fixTableCollation("deposit_indicator_detail");
        fixTableCollation("expense_allocation_result");
        fixTableCollation("dw_dim_organization");
        fixTableCollation("dw_dim_biz_line");
        fixTableCollation("dw_dim_product");
        fixTableCollation("dw_dim_channel");
        fixTableCollation("dw_dim_manager");

        log.info("DWD层表结构检查完成");
    }

    /**
     * 修复表的字符集和排序规则
     */
    private void fixTableCollation(String tableName) {
        try {
            String sql = "ALTER TABLE " + tableName + " CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci";
            jdbcTemplate.execute(sql);
            log.info("修复表排序规则: {}", tableName);
        } catch (Exception e) {
            log.warn("修复表排序规则失败: {} - {}", tableName, e.getMessage());
        }
    }

    /**
     * 如果列不存在则添加
     */
    private void addColumnIfNotExists(String tableName, String columnName, String columnType, String comment) {
        try {
            // 检查列是否存在
            String checkSql = "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = '" + tableName + "' AND column_name = '" + columnName + "'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            if (count != null && count == 0) {
                // 列不存在，添加它
                String alterSql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType + " COMMENT '" + comment + "'";
                jdbcTemplate.execute(alterSql);
                log.info("添加列: {}.{}", tableName, columnName);
            }
        } catch (Exception e) {
            log.warn("检查/添加列失败: {}.{} - {}", tableName, columnName, e.getMessage());
        }
    }

    /**
     * 执行ETL SQL（直接执行，不依赖存储过程）
     */
    private void executeETLSQL(String period) {
        // 1. 清除DWD层数据
        jdbcTemplate.update("DELETE FROM dwd_loan_detail WHERE account_period = '" + period + "'");
        jdbcTemplate.update("DELETE FROM dwd_deposit_detail WHERE account_period = '" + period + "'");

        // 2. 从ODS层清洗数据到DWD层（贷款）
        String loanDwdSql = "INSERT INTO dwd_loan_detail (" +
            "biz_id, account_period, caliber_type, " +
            "org_id, org_name, biz_line_id, biz_line_name, " +
            "product_id, product_name, channel_id, channel_name, " +
            "manager_id, manager_name, customer_id, customer_name, " +
            "loan_balance, loan_monthly_interest, ftp_cost, risk_cost, op_cost, " +
            "loan_profit, net_interest_margin" +
            ") SELECT " +
            "l.biz_id, l.account_period, l.caliber_type, " +
            "o.id, l.org_name, bl.id, l.biz_line_name, " +
            "p.id, l.product_name, c.id, l.channel_name, " +
            "m.id, l.manager_name, l.customer_id, l.customer_name, " +
            "l.loan_balance, l.loan_monthly_interest, l.ftp_cost, l.risk_cost, " +
            "COALESCE(ear.op_cost, 0), " +
            "(l.loan_monthly_interest - l.ftp_cost - l.risk_cost - COALESCE(ear.op_cost, 0)), " +
            "(l.loan_monthly_interest - l.ftp_cost) " +
            "FROM loan_indicator_detail l " +
            "LEFT JOIN dw_dim_organization o ON l.org_name = o.org_name " +
            "LEFT JOIN dw_dim_biz_line bl ON l.biz_line_name = bl.line_name " +
            "LEFT JOIN dw_dim_product p ON l.product_name = p.product_name " +
            "LEFT JOIN dw_dim_channel c ON l.channel_name = c.channel_name " +
            "LEFT JOIN dw_dim_manager m ON l.manager_name = m.manager_name " +
            "LEFT JOIN (" +
            "  SELECT biz_id, SUM(allocated_amount) as op_cost " +
            "  FROM expense_allocation_result WHERE period = '" + period + "' GROUP BY biz_id" +
            ") ear ON l.biz_id = ear.biz_id " +
            "WHERE l.account_period = '" + period + "'";
        jdbcTemplate.execute(loanDwdSql);

        // 3. 从ODS层清洗数据到DWD层（存款）
        String depositDwdSql = "INSERT INTO dwd_deposit_detail (" +
            "biz_id, account_period, caliber_type, " +
            "org_id, org_name, biz_line_id, biz_line_name, " +
            "product_id, product_name, channel_id, channel_name, " +
            "manager_id, manager_name, customer_id, customer_name, " +
            "deposit_balance, deposit_monthly_interest, ftp_income, op_cost, " +
            "deposit_profit" +
            ") SELECT " +
            "d.biz_id, d.account_period, d.caliber_type, " +
            "o.id, d.org_name, bl.id, d.biz_line_name, " +
            "p.id, d.product_name, c.id, d.channel_name, " +
            "m.id, d.manager_name, d.customer_id, d.customer_name, " +
            "d.deposit_balance, d.deposit_monthly_interest, d.ftp_income, " +
            "COALESCE(ear.op_cost, 0), " +
            "(d.ftp_income - d.deposit_monthly_interest - COALESCE(ear.op_cost, 0)) " +
            "FROM deposit_indicator_detail d " +
            "LEFT JOIN dw_dim_organization o ON d.org_name = o.org_name " +
            "LEFT JOIN dw_dim_biz_line bl ON d.biz_line_name = bl.line_name " +
            "LEFT JOIN dw_dim_product p ON d.product_name = p.product_name " +
            "LEFT JOIN dw_dim_channel c ON d.channel_name = c.channel_name " +
            "LEFT JOIN dw_dim_manager m ON d.manager_name = m.manager_name " +
            "LEFT JOIN (" +
            "  SELECT biz_id, SUM(allocated_amount) as op_cost " +
            "  FROM expense_allocation_result WHERE period = '" + period + "' GROUP BY biz_id" +
            ") ear ON d.biz_id = ear.biz_id " +
            "WHERE d.account_period = '" + period + "'";
        jdbcTemplate.execute(depositDwdSql);

        // 4. 清除DWS层数据
        jdbcTemplate.update("DELETE FROM dw_indicator_fact WHERE period = '" + period + "'");

        // 5. 按各维度汇总指标到DWS层
        // 5.1 按机构维度汇总贷款指标
        String orgLoanSql = "INSERT IGNORE INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'INTEREST_INCOME', '" + period + "', 'MONTH', 'ORG', org_id, org_name, SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW() " +
            "FROM dwd_loan_detail WHERE account_period = '" + period + "' GROUP BY org_id, org_name";
        jdbcTemplate.execute(orgLoanSql);

        // 5.2 按机构维度汇总存款指标
        String orgDepositSql = "INSERT IGNORE INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'FTP_INCOME', '" + period + "', 'MONTH', 'ORG', org_id, org_name, SUM(ftp_income) / 10000, 'ASSESS', NOW() " +
            "FROM dwd_deposit_detail WHERE account_period = '" + period + "' GROUP BY org_id, org_name";
        jdbcTemplate.execute(orgDepositSql);

        // 5.3 按各维度汇总运营成本
        String opCostOrgSql = "INSERT IGNORE INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'OP_COST', '" + period + "', 'MONTH', 'ORG', org_id, org_name, SUM(op_cost) / 10000, 'ASSESS', NOW() " +
            "FROM (SELECT org_id, org_name, op_cost FROM dwd_loan_detail WHERE account_period = '" + period + "' " +
            "UNION ALL SELECT org_id, org_name, op_cost FROM dwd_deposit_detail WHERE account_period = '" + period + "') t " +
            "GROUP BY org_id, org_name";
        jdbcTemplate.execute(opCostOrgSql);

        String opCostBizLineSql = "INSERT IGNORE INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'OP_COST', '" + period + "', 'MONTH', 'BIZ_LINE', biz_line_id, biz_line_name, SUM(op_cost) / 10000, 'ASSESS', NOW() " +
            "FROM (SELECT biz_line_id, biz_line_name, op_cost FROM dwd_loan_detail WHERE account_period = '" + period + "' " +
            "UNION ALL SELECT biz_line_id, biz_line_name, op_cost FROM dwd_deposit_detail WHERE account_period = '" + period + "') t " +
            "GROUP BY biz_line_id, biz_line_name";
        jdbcTemplate.execute(opCostBizLineSql);

        String opCostProductSql = "INSERT IGNORE INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'OP_COST', '" + period + "', 'MONTH', 'PRODUCT', product_id, product_name, SUM(op_cost) / 10000, 'ASSESS', NOW() " +
            "FROM (SELECT product_id, product_name, op_cost FROM dwd_loan_detail WHERE account_period = '" + period + "' " +
            "UNION ALL SELECT product_id, product_name, op_cost FROM dwd_deposit_detail WHERE account_period = '" + period + "') t " +
            "GROUP BY product_id, product_name";
        jdbcTemplate.execute(opCostProductSql);

        String opCostChannelSql = "INSERT IGNORE INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'OP_COST', '" + period + "', 'MONTH', 'CHANNEL', channel_id, channel_name, SUM(op_cost) / 10000, 'ASSESS', NOW() " +
            "FROM (SELECT channel_id, channel_name, op_cost FROM dwd_loan_detail WHERE account_period = '" + period + "' " +
            "UNION ALL SELECT channel_id, channel_name, op_cost FROM dwd_deposit_detail WHERE account_period = '" + period + "') t " +
            "GROUP BY channel_id, channel_name";
        jdbcTemplate.execute(opCostChannelSql);

        String opCostManagerSql = "INSERT IGNORE INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'OP_COST', '" + period + "', 'MONTH', 'MANAGER', manager_id, manager_name, SUM(op_cost) / 10000, 'ASSESS', NOW() " +
            "FROM (SELECT manager_id, manager_name, op_cost FROM dwd_loan_detail WHERE account_period = '" + period + "' " +
            "UNION ALL SELECT manager_id, manager_name, op_cost FROM dwd_deposit_detail WHERE account_period = '" + period + "') t " +
            "GROUP BY manager_id, manager_name";
        jdbcTemplate.execute(opCostManagerSql);

        String opCostCustomerSql = "INSERT IGNORE INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'OP_COST', '" + period + "', 'MONTH', 'CUSTOMER', customer_id, customer_name, SUM(op_cost) / 10000, 'ASSESS', NOW() " +
            "FROM (SELECT customer_id, customer_name, op_cost FROM dwd_loan_detail WHERE account_period = '" + period + "' " +
            "UNION ALL SELECT customer_id, customer_name, op_cost FROM dwd_deposit_detail WHERE account_period = '" + period + "') t " +
            "GROUP BY customer_id, customer_name";
        jdbcTemplate.execute(opCostCustomerSql);

        // 5.4 计算TOTAL汇总
        String totalOpCostSql = "INSERT IGNORE INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'OP_COST', '" + period + "', 'MONTH', 'TOTAL', 0, '全部', SUM(op_cost) / 10000, 'ASSESS', NOW() " +
            "FROM (SELECT op_cost FROM dwd_loan_detail WHERE account_period = '" + period + "' " +
            "UNION ALL SELECT op_cost FROM dwd_deposit_detail WHERE account_period = '" + period + "') t";
        jdbcTemplate.execute(totalOpCostSql);

        log.info("ETL SQL执行完成: {}", period);
    }
}
