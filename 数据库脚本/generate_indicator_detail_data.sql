-- ============================================
-- 指标明细表模拟数据生成脚本
-- 创建时间：2026-06-29
-- ============================================

USE multi_profit;

-- ============================================
-- 1. 生成贷款指标明细数据（50条业务，2026年1-6月）
-- ============================================

-- 先插入基础贷款记录（取最新的50条业务）
INSERT INTO loan_indicator_detail (
    biz_id, stat_date, account_period,
    customer_id, customer_name, org_id, org_name,
    biz_line_id, biz_line_name, dept_id, dept_name,
    product_id, product_name, channel_id, channel_name,
    manager_id, manager_name,
    loan_balance, loan_rate, loan_interest_calc_type,
    loan_daily_interest, loan_monthly_interest, loan_cumulative_interest,
    ftp_rate, ftp_cost, risk_cost, op_cost, expense_type, caliber_type
)
SELECT
    biz_id,
    stat_date,
    account_period,
    customer_id,
    CONCAT('客户', LPAD(ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY stat_date), 3, '0')) as customer_name,
    org_id,
    (SELECT name FROM dimension_master WHERE id = bl.org_id) as org_name,
    biz_line_id,
    (SELECT name FROM dimension_master WHERE id = bl.biz_line_id) as biz_line_name,
    dept_id,
    (SELECT name FROM dimension_master WHERE id = bl.dept_id) as dept_name,
    product_id,
    (SELECT name FROM dimension_master WHERE id = bl.product_id) as product_name,
    channel_id,
    (SELECT name FROM dimension_master WHERE id = bl.channel_id) as channel_name,
    manager_id,
    (SELECT name FROM dimension_master WHERE id = bl.manager_id) as manager_name,
    -- 贷款余额：100万-1000万
    ROUND(1000000 + RAND() * 9000000, 2) as loan_balance,
    -- 贷款利率：3%-15%
    ROUND(0.03 + RAND() * 0.12, 6) as loan_rate,
    'DAILY_ACCUMULATED' as loan_interest_calc_type,
    -- 当日利息 = 余额 × 利率 / 365
    ROUND((1000000 + RAND() * 9000000) * (0.03 + RAND() * 0.12) / 365, 4) as loan_daily_interest,
    -- 当月利息 = 当日利息 × 当月天数
    ROUND((1000000 + RAND() * 9000000) * (0.03 + RAND() * 0.12) / 365 * DAY(LAST_DAY(stat_date)), 4) as loan_monthly_interest,
    -- 累计利息 = 当日利息 × 已过天数
    ROUND((1000000 + RAND() * 9000000) * (0.03 + RAND() * 0.12) / 365 * DATEDIFF(stat_date, DATE_SUB(stat_date, INTERVAL 3 MONTH)), 4) as loan_cumulative_interest,
    -- FTP利率：2%-4%
    ROUND(0.02 + RAND() * 0.02, 6) as ftp_rate,
    -- FTP成本 = 余额 × FTP利率 / 365
    ROUND((1000000 + RAND() * 9000000) * (0.02 + RAND() * 0.02) / 365, 2) as ftp_cost,
    -- 风险成本 = 余额 × 0.5%-2%
    ROUND((1000000 + RAND() * 9000000) * (0.005 + RAND() * 0.015), 2) as risk_cost,
    -- 运营成本 = 余额 × 0.3%-1%
    ROUND((1000000 + RAND() * 9000000) * (0.003 + RAND() * 0.007), 2) as op_cost,
    CASE WHEN RAND() > 0.5 THEN '人工费用' ELSE '系统费用' END as expense_type,
    'BOOK' as caliber_type
FROM biz_ledger bl
WHERE product_type = 'LOAN'
ORDER BY stat_date DESC
LIMIT 50;

-- 生成历史数据（2026年1-5月）
INSERT INTO loan_indicator_detail (
    biz_id, stat_date, account_period,
    customer_id, customer_name, org_id, org_name,
    biz_line_id, biz_line_name, dept_id, dept_name,
    product_id, product_name, channel_id, channel_name,
    manager_id, manager_name,
    loan_balance, loan_rate, loan_interest_calc_type,
    loan_daily_interest, loan_monthly_interest, loan_cumulative_interest,
    ftp_rate, ftp_cost, risk_cost, op_cost, expense_type, caliber_type
)
SELECT
    biz_id,
    DATE_SUB(stat_date, INTERVAL n MONTH) as stat_date,
    DATE_FORMAT(DATE_SUB(stat_date, INTERVAL n MONTH), '%Y-%m') as account_period,
    customer_id, customer_name, org_id, org_name,
    biz_line_id, biz_line_name, dept_id, dept_name,
    product_id, product_name, channel_id, channel_name,
    manager_id, manager_name,
    -- 余额波动±10%
    loan_balance * (0.9 + RAND() * 0.2),
    -- 利率不变
    loan_rate,
    'DAILY_ACCUMULATED',
    -- 利息随余额变化
    loan_daily_interest * (0.9 + RAND() * 0.2),
    loan_monthly_interest * (0.9 + RAND() * 0.2),
    loan_cumulative_interest * (0.9 + RAND() * 0.2),
    ftp_rate,
    ftp_cost * (0.9 + RAND() * 0.2),
    risk_cost * (0.9 + RAND() * 0.2),
    op_cost * (0.9 + RAND() * 0.2),
    expense_type,
    'BOOK'
FROM loan_indicator_detail,
     (SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) months
WHERE stat_date = (SELECT MAX(stat_date) FROM loan_indicator_detail);

-- ============================================
-- 2. 生成存款指标明细数据（50条业务，2026年1-6月）
-- ============================================

INSERT INTO deposit_indicator_detail (
    biz_id, stat_date, account_period,
    customer_id, customer_name, org_id, org_name,
    biz_line_id, biz_line_name, dept_id, dept_name,
    product_id, product_name, channel_id, channel_name,
    manager_id, manager_name,
    deposit_balance, deposit_rate, deposit_interest_calc_type,
    deposit_daily_interest, deposit_monthly_interest, deposit_cumulative_interest,
    ftp_rate, ftp_income, op_cost, expense_type, caliber_type
)
SELECT
    biz_id,
    stat_date,
    account_period,
    customer_id,
    CONCAT('客户', LPAD(ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY stat_date), 3, '0')) as customer_name,
    org_id,
    (SELECT name FROM dimension_master WHERE id = bl.org_id) as org_name,
    biz_line_id,
    (SELECT name FROM dimension_master WHERE id = bl.biz_line_id) as biz_line_name,
    dept_id,
    (SELECT name FROM dimension_master WHERE id = bl.dept_id) as dept_name,
    product_id,
    (SELECT name FROM dimension_master WHERE id = bl.product_id) as product_name,
    channel_id,
    (SELECT name FROM dimension_master WHERE id = bl.channel_id) as channel_name,
    manager_id,
    (SELECT name FROM dimension_master WHERE id = bl.manager_id) as manager_name,
    -- 存款余额：50万-500万
    ROUND(500000 + RAND() * 4500000, 2) as deposit_balance,
    -- 存款利率：1%-3%
    ROUND(0.01 + RAND() * 0.02, 6) as deposit_rate,
    'DAILY_ACCUMULATED' as deposit_interest_calc_type,
    -- 当日利息 = 余额 × 利率 / 365
    ROUND((500000 + RAND() * 4500000) * (0.01 + RAND() * 0.02) / 365, 4) as deposit_daily_interest,
    -- 当月利息 = 当日利息 × 当月天数
    ROUND((500000 + RAND() * 4500000) * (0.01 + RAND() * 0.02) / 365 * DAY(LAST_DAY(stat_date)), 4) as deposit_monthly_interest,
    -- 累计利息 = 当日利息 × 已过天数
    ROUND((500000 + RAND() * 4500000) * (0.01 + RAND() * 0.02) / 365 * DATEDIFF(stat_date, DATE_SUB(stat_date, INTERVAL 3 MONTH)), 4) as deposit_cumulative_interest,
    -- FTP利率：2.5%-4%
    ROUND(0.025 + RAND() * 0.015, 6) as ftp_rate,
    -- FTP收入 = 余额 × FTP利率 / 365
    ROUND((500000 + RAND() * 4500000) * (0.025 + RAND() * 0.015) / 365, 2) as ftp_income,
    -- 运营成本 = 余额 × 0.2%-0.8%
    ROUND((500000 + RAND() * 4500000) * (0.002 + RAND() * 0.006), 2) as op_cost,
    CASE WHEN RAND() > 0.5 THEN '人工费用' ELSE '系统费用' END as expense_type,
    'BOOK' as caliber_type
FROM biz_ledger bl
WHERE product_type = 'DEPOSIT'
ORDER BY stat_date DESC
LIMIT 50;

-- 生成存款历史数据（2026年1-5月）
INSERT INTO deposit_indicator_detail (
    biz_id, stat_date, account_period,
    customer_id, customer_name, org_id, org_name,
    biz_line_id, biz_line_name, dept_id, dept_name,
    product_id, product_name, channel_id, channel_name,
    manager_id, manager_name,
    deposit_balance, deposit_rate, deposit_interest_calc_type,
    deposit_daily_interest, deposit_monthly_interest, deposit_cumulative_interest,
    ftp_rate, ftp_income, op_cost, expense_type, caliber_type
)
SELECT
    biz_id,
    DATE_SUB(stat_date, INTERVAL n MONTH) as stat_date,
    DATE_FORMAT(DATE_SUB(stat_date, INTERVAL n MONTH), '%Y-%m') as account_period,
    customer_id, customer_name, org_id, org_name,
    biz_line_id, biz_line_name, dept_id, dept_name,
    product_id, product_name, channel_id, channel_name,
    manager_id, manager_name,
    -- 余额波动±10%
    deposit_balance * (0.9 + RAND() * 0.2),
    -- 利率不变
    deposit_rate,
    'DAILY_ACCUMULATED',
    -- 利息随余额变化
    deposit_daily_interest * (0.9 + RAND() * 0.2),
    deposit_monthly_interest * (0.9 + RAND() * 0.2),
    deposit_cumulative_interest * (0.9 + RAND() * 0.2),
    ftp_rate,
    ftp_income * (0.9 + RAND() * 0.2),
    op_cost * (0.9 + RAND() * 0.2),
    expense_type,
    'BOOK'
FROM deposit_indicator_detail,
     (SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) months
WHERE stat_date = (SELECT MAX(stat_date) FROM deposit_indicator_detail);

-- ============================================
-- 3. 验证数据
-- ============================================

SELECT '贷款指标明细数据条数：' as info, COUNT(*) as cnt FROM loan_indicator_detail;
SELECT '存款指标明细数据条数：' as info, COUNT(*) as cnt FROM deposit_indicator_detail;
SELECT '贷款指标月度分布：' as info, account_period, COUNT(*) as cnt FROM loan_indicator_detail GROUP BY account_period ORDER BY account_period;
SELECT '存款指标月度分布：' as info, account_period, COUNT(*) as cnt FROM deposit_indicator_detail GROUP BY account_period ORDER BY account_period;

SELECT '模拟数据生成完成' as result;
