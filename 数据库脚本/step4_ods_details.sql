-- ============================================
-- Step 4: ODS明细表（从biz_ledger提取）
-- loan_indicator_detail + deposit_indicator_detail
-- ============================================

USE multi_profit;

-- 清空旧数据
TRUNCATE TABLE loan_indicator_detail;
TRUNCATE TABLE deposit_indicator_detail;

-- ============================================
-- 1. 贷款指标明细表
-- ============================================
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
    bl.biz_id,
    bl.stat_date,
    bl.account_period,
    bl.customer_id,
    cm.customer_name,
    bl.org_id,
    org.name,
    bl.biz_line_id,
    biz.name,
    bl.dept_id,
    dept.name,
    bl.product_id,
    prod.name,
    bl.channel_id,
    ch.name,
    bl.manager_id,
    mgr.name,
    -- 贷款余额
    bl.biz_amount,
    -- 贷款利率 = 利息收入 / 余额 * 12（年化）
    ROUND(bl.interest_income / bl.biz_amount * 12, 6),
    'DAILY_ACCUMULATED',
    -- 当日利息 = 利息收入 / 30
    ROUND(bl.interest_income / 30, 4),
    -- 当月利息 = 利息收入
    bl.interest_income,
    -- 累计利息 = 利息收入 * 6（假设半年累计）
    ROUND(bl.interest_income * 6, 4),
    -- FTP利率 = FTP成本 / 余额 * 12
    ROUND(bl.ftp_cost / bl.biz_amount * 12, 6),
    bl.ftp_cost,
    bl.risk_cost,
    bl.op_cost,
    CASE WHEN RAND() > 0.5 THEN '人工费用' ELSE '系统费用' END,
    bl.caliber_type
FROM biz_ledger bl
LEFT JOIN customer_master cm ON bl.customer_id = cm.id
LEFT JOIN dimension_master org ON bl.org_id = org.id AND org.dim_type = 'ORG'
LEFT JOIN dimension_master biz ON bl.biz_line_id = biz.id AND biz.dim_type = 'BIZ_LINE'
LEFT JOIN dimension_master dept ON bl.dept_id = dept.id AND dept.dim_type = 'DEPT'
LEFT JOIN dimension_master prod ON bl.product_id = prod.id AND prod.dim_type = 'PRODUCT'
LEFT JOIN dimension_master ch ON bl.channel_id = ch.id AND ch.dim_type = 'CHANNEL'
LEFT JOIN dimension_master mgr ON bl.manager_id = mgr.id AND mgr.dim_type = 'MANAGER'
WHERE bl.product_type = 'LOAN';

-- ============================================
-- 2. 存款指标明细表
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
    bl.biz_id,
    bl.stat_date,
    bl.account_period,
    bl.customer_id,
    cm.customer_name,
    bl.org_id,
    org.name,
    bl.biz_line_id,
    biz.name,
    bl.dept_id,
    dept.name,
    bl.product_id,
    prod.name,
    bl.channel_id,
    ch.name,
    bl.manager_id,
    mgr.name,
    -- 存款余额
    bl.biz_amount,
    -- 存款利率 = 利息支出 / 余额 * 12
    ROUND(bl.interest_expense / bl.biz_amount * 12, 6),
    'DAILY_ACCUMULATED',
    -- 当日利息 = 利息支出 / 30
    ROUND(bl.interest_expense / 30, 4),
    -- 当月利息 = 利息支出
    bl.interest_expense,
    -- 累计利息 = 利息支出 * 6
    ROUND(bl.interest_expense * 6, 4),
    -- FTP利率 = FTP收入 / 余额 * 12
    ROUND(bl.interest_income / bl.biz_amount * 12, 6),
    bl.interest_income,
    bl.op_cost,
    CASE WHEN RAND() > 0.5 THEN '人工费用' ELSE '系统费用' END,
    bl.caliber_type
FROM biz_ledger bl
LEFT JOIN customer_master cm ON bl.customer_id = cm.id
LEFT JOIN dimension_master org ON bl.org_id = org.id AND org.dim_type = 'ORG'
LEFT JOIN dimension_master biz ON bl.biz_line_id = biz.id AND biz.dim_type = 'BIZ_LINE'
LEFT JOIN dimension_master dept ON bl.dept_id = dept.id AND dept.dim_type = 'DEPT'
LEFT JOIN dimension_master prod ON bl.product_id = prod.id AND prod.dim_type = 'PRODUCT'
LEFT JOIN dimension_master ch ON bl.channel_id = ch.id AND ch.dim_type = 'CHANNEL'
LEFT JOIN dimension_master mgr ON bl.manager_id = mgr.id AND mgr.dim_type = 'MANAGER'
WHERE bl.product_type = 'DEPOSIT';

-- ============================================
-- 3. 验证
-- ============================================
SELECT 'ODS贷款明细' AS info, COUNT(*) AS cnt FROM loan_indicator_detail;
SELECT 'ODS存款明细' AS info, COUNT(*) AS cnt FROM deposit_indicator_detail;
SELECT '贷款月度分布' AS info, account_period, COUNT(*) AS cnt FROM loan_indicator_detail GROUP BY account_period ORDER BY account_period LIMIT 5;
SELECT '存款月度分布' AS info, account_period, COUNT(*) AS cnt FROM deposit_indicator_detail GROUP BY account_period ORDER BY account_period LIMIT 5;

SELECT 'Step 4 完成：ODS明细表数据生成成功' AS result;
