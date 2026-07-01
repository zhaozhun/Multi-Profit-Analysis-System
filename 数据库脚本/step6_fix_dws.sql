-- ============================================
-- Step 6: 修复DWS层数据
-- ============================================

USE multi_profit;

-- 清空
TRUNCATE TABLE dw_indicator_fact;

-- 按机构汇总 - 各指标
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

-- 验证
SELECT 'DWS数据统计' AS info;
SELECT dim_type, COUNT(*) as cnt FROM dw_indicator_fact GROUP BY dim_type;

SELECT '机构维度示例' AS info;
SELECT indicator_code, dim_name, calc_value FROM dw_indicator_fact
WHERE dim_type = 'ORG' AND period = '2026-06' AND indicator_code = 'LOAN_PROFIT'
LIMIT 5;

SELECT 'Step 6 完成' AS result;
