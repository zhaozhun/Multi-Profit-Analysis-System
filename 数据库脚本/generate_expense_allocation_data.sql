-- ============================================
-- 生成模拟的费用分摊结果数据
-- 创建时间：2026-06-30
-- 说明：按业务金额的1%-3%生成运营成本
-- ============================================

USE multi_profit;

-- 清除旧数据
DELETE FROM expense_allocation_result WHERE period = '2026-06';

-- 贷款业务的运营成本
INSERT INTO expense_allocation_result (period, biz_id, biz_type, expense_type, expense_name, allocated_amount, rule_code, batch_no)
SELECT
    '2026-06' as period,
    l.biz_id,
    'LOAN' as biz_type,
    'OP_COST' as expense_type,
    '运营成本' as expense_name,
    ROUND(l.loan_balance * (0.01 + RAND() * 0.02), 4) as allocated_amount,
    'SIMULATED' as rule_code,
    'BATCH_202606' as batch_no
FROM loan_indicator_detail l
WHERE l.account_period = '2026-06';

-- 存款业务的运营成本
INSERT INTO expense_allocation_result (period, biz_id, biz_type, expense_type, expense_name, allocated_amount, rule_code, batch_no)
SELECT
    '2026-06' as period,
    d.biz_id,
    'DEPOSIT' as biz_type,
    'OP_COST' as expense_type,
    '运营成本' as expense_name,
    ROUND(d.deposit_balance * (0.01 + RAND() * 0.02), 4) as allocated_amount,
    'SIMULATED' as rule_code,
    'BATCH_202606' as batch_no
FROM deposit_indicator_detail d
WHERE d.account_period = '2026-06';

-- 验证数据
SELECT '贷款业务费用分摊条数：' as info, COUNT(*) as cnt FROM expense_allocation_result WHERE period = '2026-06' AND biz_type = 'LOAN';
SELECT '存款业务费用分摊条数：' as info, COUNT(*) as cnt FROM expense_allocation_result WHERE period = '2026-06' AND biz_type = 'DEPOSIT';
SELECT '费用分摊总金额：' as info, ROUND(SUM(allocated_amount), 2) as total FROM expense_allocation_result WHERE period = '2026-06';

SELECT '模拟费用分摊数据生成完成' AS result;
