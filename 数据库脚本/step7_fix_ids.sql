-- ============================================
-- Step 7: 修复ID映射
-- biz_ledger中的ID是硬编码的，需要映射到dimension_master的实际ID
-- ============================================

USE multi_profit;

-- ============================================
-- 1. 创建映射表
-- ============================================
DROP TEMPORARY TABLE IF EXISTS tmp_org_mapping;
CREATE TEMPORARY TABLE tmp_org_mapping (old_id INT, new_id INT);
INSERT INTO tmp_org_mapping VALUES
(23, 99), (24, 100), (25, 101),  -- 北京: 朝阳/海淀/西城
(26, 102), (27, 103),            -- 上海: 浦东/浦西
(28, 104), (29, 105),            -- 深圳: 南山/福田
(30, 106),                       -- 广州: 天河
(31, 107);                       -- 杭州: 西湖

-- 产品映射
DROP TEMPORARY TABLE IF EXISTS tmp_prod_mapping;
CREATE TEMPORARY TABLE tmp_prod_mapping (old_id INT, new_id INT);
INSERT INTO tmp_prod_mapping VALUES
(58, 114), (59, 115),  -- 短期贷款/中长期贷款
(60, 116), (61, 117),  -- 住房贷款/消费贷款
(62, 118), (63, 119),  -- 活期存款/定期存款
(64, 120), (65, 121),  -- 活期储蓄/定期储蓄
(66, 122), (67, 123),  -- 净值型理财/结构性存款
(68, 124), (69, 125);  -- 国际结算/贸易融资

-- 条线映射
DROP TEMPORARY TABLE IF EXISTS tmp_biz_mapping;
CREATE TEMPORARY TABLE tmp_biz_mapping (old_id INT, new_id INT);
INSERT INTO tmp_biz_mapping VALUES
(78, 145), (79, 146),  -- 大客户部/中小企业部
(80, 147), (81, 148),  -- 个人信贷部/个人负债部
(82, 149), (83, 150);  -- 同业部/投资部

-- 部门映射
DROP TEMPORARY TABLE IF EXISTS tmp_dept_mapping;
CREATE TEMPORARY TABLE tmp_dept_mapping (old_id INT, new_id INT);
INSERT INTO tmp_dept_mapping VALUES
(1, 57), (2, 58), (3, 59), (4, 60), (5, 61), (6, 62), (7, 63);

-- 渠道映射
DROP TEMPORARY TABLE IF EXISTS tmp_ch_mapping;
CREATE TEMPORARY TABLE tmp_ch_mapping (old_id INT, new_id INT);
INSERT INTO tmp_ch_mapping VALUES
(88, 165), (89, 166),  -- 网点渠道/外拓渠道
(90, 167), (91, 168);  -- 手机银行/网上银行

-- 客户经理映射
DROP TEMPORARY TABLE IF EXISTS tmp_mgr_mapping;
CREATE TEMPORARY TABLE tmp_mgr_mapping (old_id INT, new_id INT);
INSERT INTO tmp_mgr_mapping VALUES
(110, 191), (111, 192), (112, 193), (113, 194),  -- 北京
(114, 195), (115, 196), (116, 197),              -- 上海
(117, 198), (118, 199),                           -- 深圳
(119, 200),                                       -- 广州
(120, 201), (121, 202);                           -- 杭州

-- ============================================
-- 2. 修复biz_ledger
-- ============================================
UPDATE biz_ledger bl
JOIN tmp_org_mapping m ON bl.org_id = m.old_id
SET bl.org_id = m.new_id;

UPDATE biz_ledger bl
JOIN tmp_prod_mapping m ON bl.product_id = m.old_id
SET bl.product_id = m.new_id;

UPDATE biz_ledger bl
JOIN tmp_biz_mapping m ON bl.biz_line_id = m.old_id
SET bl.biz_line_id = m.new_id;

UPDATE biz_ledger bl
JOIN tmp_dept_mapping m ON bl.dept_id = m.old_id
SET bl.dept_id = m.new_id;

UPDATE biz_ledger bl
JOIN tmp_ch_mapping m ON bl.channel_id = m.old_id
SET bl.channel_id = m.new_id;

UPDATE biz_ledger bl
JOIN tmp_mgr_mapping m ON bl.manager_id = m.old_id
SET bl.manager_id = m.new_id;

-- ============================================
-- 3. 重新生成ODS明细表
-- ============================================
TRUNCATE TABLE loan_indicator_detail;
TRUNCATE TABLE deposit_indicator_detail;

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
    bl.biz_id, bl.stat_date, bl.account_period,
    bl.customer_id, cm.customer_name,
    bl.org_id, org.name,
    bl.biz_line_id, biz.name,
    bl.dept_id, dept.name,
    bl.product_id, prod.name,
    bl.channel_id, ch.name,
    bl.manager_id, mgr.name,
    bl.biz_amount,
    ROUND(bl.interest_income / bl.biz_amount * 12, 6),
    'DAILY_ACCUMULATED',
    ROUND(bl.interest_income / 30, 4),
    bl.interest_income,
    ROUND(bl.interest_income * 6, 4),
    ROUND(bl.ftp_cost / bl.biz_amount * 12, 6),
    bl.ftp_cost, bl.risk_cost, bl.op_cost,
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
    bl.biz_id, bl.stat_date, bl.account_period,
    bl.customer_id, cm.customer_name,
    bl.org_id, org.name,
    bl.biz_line_id, biz.name,
    bl.dept_id, dept.name,
    bl.product_id, prod.name,
    bl.channel_id, ch.name,
    bl.manager_id, mgr.name,
    bl.biz_amount,
    ROUND(bl.interest_expense / bl.biz_amount * 12, 6),
    'DAILY_ACCUMULATED',
    ROUND(bl.interest_expense / 30, 4),
    bl.interest_expense,
    ROUND(bl.interest_expense * 6, 4),
    ROUND(bl.interest_income / bl.biz_amount * 12, 6),
    bl.interest_income, bl.op_cost,
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
-- 4. 重新生成DWD层
-- ============================================
TRUNCATE TABLE dwd_loan_detail;
TRUNCATE TABLE dwd_deposit_detail;

INSERT INTO dwd_loan_detail (
    biz_id, account_period, caliber_type,
    org_id, org_name, biz_line_id, biz_line_name,
    product_id, product_name, channel_id, channel_name,
    manager_id, manager_name, customer_id, customer_name,
    loan_balance, loan_monthly_interest, ftp_cost, risk_cost, op_cost,
    loan_profit, net_interest_margin
)
SELECT
    l.biz_id, l.account_period, l.caliber_type,
    l.org_id, l.org_name, l.biz_line_id, l.biz_line_name,
    l.product_id, l.product_name, l.channel_id, l.channel_name,
    l.manager_id, l.manager_name, l.customer_id, l.customer_name,
    l.loan_balance, l.loan_monthly_interest, l.ftp_cost, l.risk_cost, l.op_cost,
    (l.loan_monthly_interest - l.ftp_cost - l.risk_cost - l.op_cost),
    (l.loan_monthly_interest - l.ftp_cost)
FROM loan_indicator_detail l;

INSERT INTO dwd_deposit_detail (
    biz_id, account_period, caliber_type,
    org_id, org_name, biz_line_id, biz_line_name,
    product_id, product_name, channel_id, channel_name,
    manager_id, manager_name, customer_id, customer_name,
    deposit_balance, deposit_monthly_interest, ftp_income, op_cost,
    deposit_profit
)
SELECT
    d.biz_id, d.account_period, d.caliber_type,
    d.org_id, d.org_name, d.biz_line_id, d.biz_line_name,
    d.product_id, d.product_name, d.channel_id, d.channel_name,
    d.manager_id, d.manager_name, d.customer_id, d.customer_name,
    d.deposit_balance, d.deposit_monthly_interest, d.ftp_income, d.op_cost,
    (d.ftp_income - d.deposit_monthly_interest - d.op_cost)
FROM deposit_indicator_detail d;

-- ============================================
-- 5. 重新生成DWS层
-- ============================================
TRUNCATE TABLE dw_indicator_fact;

-- 按机构汇总
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_INCOME', account_period, 'MONTH', 'ORG', org_id, org_name, SUM(loan_monthly_interest), 'ASSESS', NOW()
FROM dwd_loan_detail GROUP BY account_period, org_id, org_name;

INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'LOAN_PROFIT', account_period, 'MONTH', 'ORG', org_id, org_name, SUM(loan_profit), 'ASSESS', NOW()
FROM dwd_loan_detail GROUP BY account_period, org_id, org_name;

INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'FTP_COST', account_period, 'MONTH', 'ORG', org_id, org_name, SUM(ftp_cost), 'ASSESS', NOW()
FROM dwd_loan_detail GROUP BY account_period, org_id, org_name;

INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'RISK_COST', account_period, 'MONTH', 'ORG', org_id, org_name, SUM(risk_cost), 'ASSESS', NOW()
FROM dwd_loan_detail GROUP BY account_period, org_id, org_name;

INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'OP_COST', account_period, 'MONTH', 'ORG', org_id, org_name, SUM(op_cost), 'ASSESS', NOW()
FROM dwd_loan_detail GROUP BY account_period, org_id, org_name;

INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'DEPOSIT_PROFIT', account_period, 'MONTH', 'ORG', org_id, org_name, SUM(deposit_profit), 'ASSESS', NOW()
FROM dwd_deposit_detail GROUP BY account_period, org_id, org_name;

INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'FTP_INCOME', account_period, 'MONTH', 'ORG', org_id, org_name, SUM(ftp_income), 'ASSESS', NOW()
FROM dwd_deposit_detail GROUP BY account_period, org_id, org_name;

-- 按产品汇总
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_INCOME', account_period, 'MONTH', 'PRODUCT', product_id, product_name, SUM(loan_monthly_interest), 'ASSESS', NOW()
FROM dwd_loan_detail GROUP BY account_period, product_id, product_name;

INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'LOAN_PROFIT', account_period, 'MONTH', 'PRODUCT', product_id, product_name, SUM(loan_profit), 'ASSESS', NOW()
FROM dwd_loan_detail GROUP BY account_period, product_id, product_name;

-- 按条线汇总
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_INCOME', account_period, 'MONTH', 'BIZ_LINE', biz_line_id, biz_line_name, SUM(loan_monthly_interest), 'ASSESS', NOW()
FROM dwd_loan_detail GROUP BY account_period, biz_line_id, biz_line_name;

INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'LOAN_PROFIT', account_period, 'MONTH', 'BIZ_LINE', biz_line_id, biz_line_name, SUM(loan_profit), 'ASSESS', NOW()
FROM dwd_loan_detail GROUP BY account_period, biz_line_id, biz_line_name;

-- 按渠道汇总
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_INCOME', account_period, 'MONTH', 'CHANNEL', channel_id, channel_name, SUM(loan_monthly_interest), 'ASSESS', NOW()
FROM dwd_loan_detail GROUP BY account_period, channel_id, channel_name;

-- 按客户经理汇总
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_INCOME', account_period, 'MONTH', 'MANAGER', manager_id, manager_name, SUM(loan_monthly_interest), 'ASSESS', NOW()
FROM dwd_loan_detail GROUP BY account_period, manager_id, manager_name;

INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'LOAN_PROFIT', account_period, 'MONTH', 'MANAGER', manager_id, manager_name, SUM(loan_profit), 'ASSESS', NOW()
FROM dwd_loan_detail GROUP BY account_period, manager_id, manager_name;

-- TOTAL维度汇总
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT indicator_code, period, period_type, 'TOTAL', 0, '全行', SUM(calc_value), 'ASSESS', NOW()
FROM dw_indicator_fact WHERE dim_type = 'ORG'
GROUP BY indicator_code, period, period_type;

-- TOTAL_PROFIT
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'TOTAL_PROFIT', period, period_type, 'TOTAL', 0, '全行',
       SUM(CASE WHEN indicator_code IN ('LOAN_PROFIT', 'DEPOSIT_PROFIT') THEN calc_value ELSE 0 END),
       'ASSESS', NOW()
FROM dw_indicator_fact WHERE dim_type = 'TOTAL' AND indicator_code IN ('LOAN_PROFIT', 'DEPOSIT_PROFIT')
GROUP BY period, period_type;

-- ============================================
-- 6. 验证
-- ============================================
SELECT '验证机构维度名称' AS info;
SELECT DISTINCT dim_name FROM dw_indicator_fact WHERE dim_type = 'ORG' LIMIT 10;

SELECT '验证机构利润数据' AS info;
SELECT dim_name, calc_value FROM dw_indicator_fact
WHERE dim_type = 'ORG' AND period = '2026-06' AND indicator_code = 'LOAN_PROFIT'
LIMIT 5;

SELECT 'Step 7 完成：ID映射修复成功' AS result;
