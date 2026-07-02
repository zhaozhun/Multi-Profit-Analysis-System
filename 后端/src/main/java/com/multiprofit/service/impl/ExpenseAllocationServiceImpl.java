package com.multiprofit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.multiprofit.entity.*;
import com.multiprofit.mapper.*;
import com.multiprofit.service.ExpenseAllocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 运营成本分摊服务实现类
 */
@Service
public class ExpenseAllocationServiceImpl implements ExpenseAllocationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AllocationFactorMapper allocationFactorMapper;

    @Autowired
    private ExpenseAllocationRuleMapper expenseAllocationRuleMapper;

    @Autowired
    private ExpenseAllocationResultMapper expenseAllocationResultMapper;

    @Autowired
    private BizProfitSummaryMapper bizProfitSummaryMapper;

    @Override
    public Map<String, Object> getExpenseSummary(String period, String caliberType, String dimension) {
        Map<String, Object> result = new HashMap<>();

        // 维度类型 → 列名(白名单校验,防注入)
        String dimField;
        switch (dimension) {
            case "ORG": dimField = "org_name"; break;
            case "BIZ_LINE": dimField = "biz_line_name"; break;
            case "PRODUCT": dimField = "product_name"; break;
            case "CHANNEL": dimField = "channel_name"; break;
            case "MANAGER": dimField = "manager_name"; break;
            case "CUSTOMER": dimField = "customer_name"; break;
            case "DEPT": dimField = "dept_name"; break;
            default: dimField = "org_name";
        }

        // 贷款+存款明细UNION(原 biz_profit_summary 表不存在,改用 DWD 明细表聚合运营成本)
        String union = "SELECT biz_id, org_name, biz_line_name, product_name, channel_name, " +
            "manager_name, customer_name, dept_name, " +
            "loan_balance AS balance, loan_monthly_interest AS interest, " +
            "ftp_cost, risk_cost, op_cost, loan_profit AS profit " +
            "FROM dwd_loan_detail WHERE account_period = ? AND caliber_type = ? " +
            "UNION ALL " +
            "SELECT biz_id, org_name, biz_line_name, product_name, channel_name, " +
            "manager_name, customer_name, dept_name, " +
            "deposit_balance AS balance, deposit_monthly_interest AS interest, " +
            "ftp_income AS ftp_cost, 0 AS risk_cost, op_cost, deposit_profit AS profit " +
            "FROM dwd_deposit_detail WHERE account_period = ? AND caliber_type = ?";

        // 汇总
        try {
            String summarySql = "SELECT COUNT(*) AS total_count, " +
                "COALESCE(SUM(balance),0) AS total_balance, " +
                "COALESCE(SUM(op_cost),0) AS total_op_cost, " +
                "COALESCE(SUM(interest),0) AS total_interest, " +
                "COALESCE(SUM(profit),0) AS total_profit " +
                "FROM (" + union + ") t";
            Map<String, Object> summary = jdbcTemplate.queryForMap(summarySql, period, caliberType, period, caliberType);
            result.put("summary", summary);
        } catch (Exception e) {
            result.put("summary", new HashMap<>());
        }

        // 按维度汇总
        try {
            String dimSql = "SELECT " + dimField + " AS dim_name, COUNT(*) AS count, " +
                "COALESCE(SUM(balance),0) AS total_balance, " +
                "COALESCE(SUM(op_cost),0) AS total_op_cost, " +
                "CASE WHEN SUM(balance)>0 THEN SUM(op_cost)/SUM(balance)*100 ELSE 0 END AS cost_ratio " +
                "FROM (" + union + ") t WHERE " + dimField + " IS NOT NULL " +
                "GROUP BY " + dimField + " ORDER BY total_op_cost DESC";
            List<Map<String, Object>> dimData = jdbcTemplate.queryForList(dimSql, period, caliberType, period, caliberType);
            result.put("dimension", dimData);
        } catch (Exception e) {
            result.put("dimension", new ArrayList<>());
        }

        // 业务明细
        try {
            String detailSql = "SELECT * FROM (" + union + ") t ORDER BY op_cost DESC LIMIT 100";
            List<Map<String, Object>> detail = jdbcTemplate.queryForList(detailSql, period, caliberType, period, caliberType);
            result.put("detail", detail);
        } catch (Exception e) {
            result.put("detail", new ArrayList<>());
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getBizExpenseComposition(String period, String bizId) {
        String sql = "SELECT " +
            "expense_type, expense_name, allocated_amount, factor_value, factor_ratio, rule_code " +
            "FROM expense_allocation_result " +
            "WHERE period = ? AND biz_id = ? " +
            "ORDER BY allocated_amount DESC";

        return jdbcTemplate.queryForList(sql, period, bizId);
    }

    @Override
    public List<Map<String, Object>> getExpenseOriginalData(String period, String expenseType) {
        String tableName;
        switch (expenseType) {
            case "RENT": tableName = "expense_rent"; break;
            case "SALARY": tableName = "expense_salary"; break;
            case "IT": tableName = "expense_it"; break;
            case "MARKETING": tableName = "expense_marketing"; break;
            default: tableName = "expense_other";
        }

        String sql = "SELECT * FROM " + tableName + " WHERE period = ? AND status = 'ACTIVE'";
        return jdbcTemplate.queryForList(sql, period);
    }

    @Override
    public List<Map<String, Object>> getAllocationFactors() {
        List<AllocationFactor> factors = allocationFactorMapper.selectList(
            new LambdaQueryWrapper<AllocationFactor>()
                .eq(AllocationFactor::getStatus, "ACTIVE")
                .orderByAsc(AllocationFactor::getFactorCode)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (AllocationFactor factor : factors) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", factor.getId());
            map.put("factorCode", factor.getFactorCode());
            map.put("factorName", factor.getFactorName());
            map.put("factorType", factor.getFactorType());
            map.put("sourceTable", factor.getSourceTable());
            map.put("sourceField", factor.getSourceField());
            map.put("description", factor.getDescription());
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getAllocationRules(String expenseType) {
        LambdaQueryWrapper<ExpenseAllocationRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseAllocationRule::getStatus, "ACTIVE");
        if (expenseType != null && !expenseType.isEmpty()) {
            wrapper.eq(ExpenseAllocationRule::getExpenseType, expenseType);
        }
        wrapper.orderByAsc(ExpenseAllocationRule::getRuleCode);

        List<ExpenseAllocationRule> rules = expenseAllocationRuleMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExpenseAllocationRule rule : rules) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", rule.getId());
            map.put("ruleCode", rule.getRuleCode());
            map.put("ruleName", rule.getRuleName());
            map.put("expenseTable", rule.getExpenseTable());
            map.put("expenseType", rule.getExpenseType());
            map.put("sourceDimType", rule.getSourceDimType());
            map.put("factorCode", rule.getFactorCode());
            map.put("description", rule.getDescription());
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> saveAllocationRule(Map<String, Object> rule) {
        Map<String, Object> result = new HashMap<>();

        try {
            String ruleCode = (String) rule.get("ruleCode");
            String ruleName = (String) rule.get("ruleName");
            String expenseTable = (String) rule.get("expenseTable");
            String expenseType = (String) rule.get("expenseType");
            String sourceDimType = (String) rule.get("sourceDimType");
            String factorCode = (String) rule.get("factorCode");
            String description = (String) rule.get("description");

            // 检查规则是否存在
            ExpenseAllocationRule existingRule = expenseAllocationRuleMapper.selectOne(
                new LambdaQueryWrapper<ExpenseAllocationRule>()
                    .eq(ExpenseAllocationRule::getRuleCode, ruleCode)
            );

            if (existingRule != null) {
                // 更新
                existingRule.setRuleName(ruleName);
                existingRule.setExpenseTable(expenseTable);
                existingRule.setExpenseType(expenseType);
                existingRule.setSourceDimType(sourceDimType);
                existingRule.setFactorCode(factorCode);
                existingRule.setDescription(description);
                expenseAllocationRuleMapper.updateById(existingRule);
            } else {
                // 新增
                ExpenseAllocationRule newRule = new ExpenseAllocationRule();
                newRule.setRuleCode(ruleCode);
                newRule.setRuleName(ruleName);
                newRule.setExpenseTable(expenseTable);
                newRule.setExpenseType(expenseType);
                newRule.setSourceDimType(sourceDimType);
                newRule.setTargetType("BIZ");
                newRule.setFactorCode(factorCode);
                newRule.setDescription(description);
                newRule.setStatus("ACTIVE");
                expenseAllocationRuleMapper.insert(newRule);
            }

            result.put("success", true);
            result.put("message", "保存成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "保存失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> executeAllocation(String period, String expenseType) {
        Map<String, Object> result = new HashMap<>();
        String batchNo = "BATCH-" + period + "-" + System.currentTimeMillis();

        try {
            // 删除该期间的旧分摊结果
            LambdaQueryWrapper<ExpenseAllocationResult> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(ExpenseAllocationResult::getPeriod, period);
            if (expenseType != null && !expenseType.isEmpty()) {
                deleteWrapper.eq(ExpenseAllocationResult::getExpenseType, expenseType);
            }
            expenseAllocationResultMapper.delete(deleteWrapper);

            // 获取分摊规则
            LambdaQueryWrapper<ExpenseAllocationRule> ruleWrapper = new LambdaQueryWrapper<>();
            ruleWrapper.eq(ExpenseAllocationRule::getStatus, "ACTIVE");
            if (expenseType != null && !expenseType.isEmpty()) {
                ruleWrapper.eq(ExpenseAllocationRule::getExpenseType, expenseType);
            }
            List<ExpenseAllocationRule> rules = expenseAllocationRuleMapper.selectList(ruleWrapper);

            int totalRecords = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (ExpenseAllocationRule rule : rules) {
                // 执行分摊
                Map<String, Object> allocResult = executeSingleRule(period, rule, batchNo);
                int records = (int) allocResult.get("records");
                BigDecimal amount = (BigDecimal) allocResult.get("amount");
                totalRecords += records;
                totalAmount = totalAmount.add(amount);
            }

            // 重新计算业务利润
            recalculateBizProfit(period);

            result.put("success", true);
            result.put("batchNo", batchNo);
            result.put("totalRecords", totalRecords);
            result.put("totalAmount", totalAmount);
            result.put("message", "分摊执行成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "分摊执行失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 执行单条分摊规则
     */
    private Map<String, Object> executeSingleRule(String period, ExpenseAllocationRule rule, String batchNo) {
        Map<String, Object> result = new HashMap<>();
        int records = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 获取费用原始数据
        List<Map<String, Object>> expenseData = getExpenseOriginalData(period, rule.getExpenseType());

        // 获取业务数据
        String bizSql = "SELECT biz_id, 'LOAN' as biz_type, " +
            "org_id, org_name, product_id, product_name, manager_id, manager_name, " +
            "loan_balance as balance " +
            "FROM loan_indicator_detail " +
            "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
            "UNION ALL " +
            "SELECT biz_id, 'DEPOSIT' as biz_type, " +
            "org_id, org_name, product_id, product_name, manager_id, manager_name, " +
            "deposit_balance as balance " +
            "FROM deposit_indicator_detail " +
            "WHERE account_period = ? AND caliber_type = 'ASSESS'";

        List<Map<String, Object>> bizData = jdbcTemplate.queryForList(bizSql, period, period);

        if (bizData.isEmpty()) {
            result.put("records", 0);
            result.put("amount", BigDecimal.ZERO);
            return result;
        }

        // 按规则分摊
        for (Map<String, Object> expense : expenseData) {
            BigDecimal amount = (BigDecimal) expense.get("amount");
            if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) continue;

            // 计算分摊比例
            BigDecimal totalFactor = BigDecimal.ZERO;
            for (Map<String, Object> biz : bizData) {
                BigDecimal factorValue = getFactorValue(biz, rule.getFactorCode(), rule.getSourceDimType(), expense);
                totalFactor = totalFactor.add(factorValue);
            }

            if (totalFactor.compareTo(BigDecimal.ZERO) == 0) continue;

            // 分摊到每笔业务
            for (Map<String, Object> biz : bizData) {
                BigDecimal factorValue = getFactorValue(biz, rule.getFactorCode(), rule.getSourceDimType(), expense);
                BigDecimal ratio = factorValue.divide(totalFactor, 8, RoundingMode.HALF_UP);
                BigDecimal allocatedAmount = amount.multiply(ratio).setScale(4, RoundingMode.HALF_UP);

                if (allocatedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    ExpenseAllocationResult allocResult = new ExpenseAllocationResult();
                    allocResult.setPeriod(period);
                    allocResult.setBizId((String) biz.get("biz_id"));
                    allocResult.setBizType((String) biz.get("biz_type"));
                    allocResult.setExpenseType(rule.getExpenseType());
                    allocResult.setExpenseName(rule.getRuleName());
                    allocResult.setAllocatedAmount(allocatedAmount);
                    allocResult.setFactorValue(factorValue);
                    allocResult.setFactorRatio(ratio);
                    allocResult.setRuleCode(rule.getRuleCode());
                    allocResult.setBatchNo(batchNo);
                    expenseAllocationResultMapper.insert(allocResult);

                    records++;
                    totalAmount = totalAmount.add(allocatedAmount);
                }
            }
        }

        result.put("records", records);
        result.put("amount", totalAmount);
        return result;
    }

    /**
     * 获取因子值
     */
    private BigDecimal getFactorValue(Map<String, Object> biz, String factorCode, String sourceDimType, Map<String, Object> expense) {
        switch (factorCode) {
            case "MANAGER_COUNT":
                // 按客户经理人数分摊，每个客户经理权重相同
                return BigDecimal.ONE;
            case "BIZ_AMOUNT":
            case "LOAN_BALANCE":
            case "DEPOSIT_BALANCE":
                // 按余额分摊
                BigDecimal balance = (BigDecimal) biz.get("balance");
                return balance != null ? balance : BigDecimal.ZERO;
            case "BIZ_COUNT":
                // 按笔数分摊
                return BigDecimal.ONE;
            case "REVENUE":
                // 按收入分摊
                BigDecimal interest = (BigDecimal) biz.get("interest_income");
                return interest != null ? interest : BigDecimal.ZERO;
            default:
                return BigDecimal.ONE;
        }
    }

    /**
     * 重新计算业务利润
     */
    private void recalculateBizProfit(String period) {
        // 删除旧数据
        jdbcTemplate.update("DELETE FROM biz_profit_summary WHERE period = ?", period);

        // 插入贷款业务利润
        String loanSql = "INSERT INTO biz_profit_summary " +
            "(period, biz_id, biz_type, customer_id, customer_name, " +
            "org_id, org_name, biz_line_id, biz_line_name, product_id, product_name, " +
            "channel_id, channel_name, manager_id, manager_name, " +
            "balance, interest_income, ftp_cost, risk_cost, op_cost, profit, caliber_type) " +
            "SELECT " +
            "l.account_period, l.biz_id, 'LOAN', l.customer_id, l.customer_name, " +
            "l.org_id, l.org_name, l.biz_line_id, l.biz_line_name, l.product_id, l.product_name, " +
            "l.channel_id, l.channel_name, l.manager_id, l.manager_name, " +
            "l.loan_balance, l.loan_monthly_interest, l.ftp_cost, l.risk_cost, " +
            "COALESCE(e.op_cost, 0), " +
            "l.loan_monthly_interest - l.ftp_cost - l.risk_cost - COALESCE(e.op_cost, 0), " +
            "'ASSESS' " +
            "FROM loan_indicator_detail l " +
            "LEFT JOIN ( " +
            "    SELECT biz_id, SUM(allocated_amount) as op_cost " +
            "    FROM expense_allocation_result " +
            "    WHERE period = ? " +
            "    GROUP BY biz_id " +
            ") e ON l.biz_id = e.biz_id " +
            "WHERE l.account_period = ? AND l.caliber_type = 'ASSESS'";

        jdbcTemplate.update(loanSql, period, period);

        // 插入存款业务利润
        String depositSql = "INSERT INTO biz_profit_summary " +
            "(period, biz_id, biz_type, customer_id, customer_name, " +
            "org_id, org_name, biz_line_id, biz_line_name, product_id, product_name, " +
            "channel_id, channel_name, manager_id, manager_name, " +
            "balance, interest_income, ftp_cost, risk_cost, op_cost, profit, caliber_type) " +
            "SELECT " +
            "d.account_period, d.biz_id, 'DEPOSIT', d.customer_id, d.customer_name, " +
            "d.org_id, d.org_name, d.biz_line_id, d.biz_line_name, d.product_id, d.product_name, " +
            "d.channel_id, d.channel_name, d.manager_id, d.manager_name, " +
            "d.deposit_balance, d.deposit_monthly_interest, 0, 0, " +
            "COALESCE(e.op_cost, 0), " +
            "d.deposit_monthly_interest - COALESCE(e.op_cost, 0), " +
            "'ASSESS' " +
            "FROM deposit_indicator_detail d " +
            "LEFT JOIN ( " +
            "    SELECT biz_id, SUM(allocated_amount) as op_cost " +
            "    FROM expense_allocation_result " +
            "    WHERE period = ? " +
            "    GROUP BY biz_id " +
            ") e ON d.biz_id = e.biz_id " +
            "WHERE d.account_period = ? AND d.caliber_type = 'ASSESS'";

        jdbcTemplate.update(depositSql, period, period);
    }
}
