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
            // 1. 执行费用分摊（当前用模拟数据）
            log.info("开始执行费用分摊: {}", period);
            generateExpenseAllocationData(period);

            // 2. 调用ETL存储过程
            log.info("开始执行ETL存储过程: {}", period);
            jdbcTemplate.execute("CALL sp_etl_recalculate('" + period + "')");

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
}
