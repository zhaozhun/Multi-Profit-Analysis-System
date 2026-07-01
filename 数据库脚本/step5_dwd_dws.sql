-- ============================================
-- Step 5: DWD层 + DWS层 + 维度表
-- ============================================

USE multi_profit;

-- ============================================
-- 一、维度表（从dimension_master同步）
-- ============================================

-- 机构维度表
TRUNCATE TABLE dw_dim_organization;
INSERT INTO dw_dim_organization (org_code, org_name, parent_id, level, status)
SELECT code, name, parent_id, level, status
FROM dimension_master WHERE dim_type = 'ORG';

-- 条线维度表
TRUNCATE TABLE dw_dim_biz_line;
INSERT INTO dw_dim_biz_line (line_code, line_name, status)
SELECT code, name, status FROM dimension_master WHERE dim_type = 'BIZ_LINE';

-- 产品维度表
TRUNCATE TABLE dw_dim_product;
INSERT INTO dw_dim_product (product_code, product_name, product_type, status)
SELECT code, name, product_type, status FROM dimension_master WHERE dim_type = 'PRODUCT';

-- 渠道维度表
TRUNCATE TABLE dw_dim_channel;
INSERT INTO dw_dim_channel (channel_code, channel_name, status)
SELECT code, name, status FROM dimension_master WHERE dim_type = 'CHANNEL';

-- 客户经理维度表
TRUNCATE TABLE dw_dim_manager;
INSERT INTO dw_dim_manager (manager_code, manager_name, org_id, status)
SELECT code, name, parent_id, status FROM dimension_master WHERE dim_type = 'MANAGER';

-- 客户维度表
TRUNCATE TABLE dw_dim_customer;
INSERT INTO dw_dim_customer (customer_code, customer_name, customer_type, status)
SELECT customer_code, customer_name, customer_type, status FROM customer_master;

-- ============================================
-- 二、DWD层（从ODS层清洗）
-- ============================================

-- 贷款DWD
TRUNCATE TABLE dwd_loan_detail;
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
    -- 贷款利润 = 利息收入 - FTP成本 - 风险成本 - 运营成本
    (l.loan_monthly_interest - l.ftp_cost - l.risk_cost - l.op_cost),
    -- 净利差 = 利息收入 - FTP成本
    (l.loan_monthly_interest - l.ftp_cost)
FROM loan_indicator_detail l;

-- 存款DWD
TRUNCATE TABLE dwd_deposit_detail;
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
    -- 存款利润 = FTP收入 - 利息支出 - 运营成本
    (d.ftp_income - d.deposit_monthly_interest - d.op_cost)
FROM deposit_indicator_detail d;

-- ============================================
-- 三、DWS层（按维度汇总指标）
-- ============================================

TRUNCATE TABLE dw_indicator_fact;

-- 1. 按机构汇总 - 贷款利息收入
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_INCOME', account_period, 'MONTH', 'ORG', org_id, org_name,
       SUM(loan_monthly_interest), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, org_id, org_name;

-- 2. 按机构汇总 - 贷款FTP成本
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'FTP_COST', account_period, 'MONTH', 'ORG', org_id, org_name,
       SUM(ftp_cost), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, org_id, org_name;

-- 3. 按机构汇总 - 贷款风险成本
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'RISK_COST', account_period, 'MONTH', 'ORG', org_id, org_name,
       SUM(risk_cost), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, org_id, org_name;

-- 4. 按机构汇总 - 贷款运营成本
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'OP_COST', account_period, 'MONTH', 'ORG', org_id, org_name,
       SUM(op_cost), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, org_id, org_name;

-- 5. 按机构汇总 - 贷款利润
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'LOAN_PROFIT', account_period, 'MONTH', 'ORG', org_id, org_name,
       SUM(loan_profit), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, org_id, org_name;

-- 6. 按机构汇总 - 贷款余额
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'LOAN_BALANCE', account_period, 'MONTH', 'ORG', org_id, org_name,
       SUM(loan_balance), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, org_id, org_name;

-- 7. 按机构汇总 - 存款FTP收入
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'FTP_INCOME', account_period, 'MONTH', 'ORG', org_id, org_name,
       SUM(ftp_income), 'ASSESS', NOW()
FROM dwd_deposit_detail
GROUP BY account_period, org_id, org_name;

-- 8. 按机构汇总 - 存款利息支出
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_EXPENSE', account_period, 'MONTH', 'ORG', org_id, org_name,
       SUM(deposit_monthly_interest), 'ASSESS', NOW()
FROM dwd_deposit_detail
GROUP BY account_period, org_id, org_name;

-- 9. 按机构汇总 - 存款运营成本
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'LIABILITY_OP_COST', account_period, 'MONTH', 'ORG', org_id, org_name,
       SUM(op_cost), 'ASSESS', NOW()
FROM dwd_deposit_detail
GROUP BY account_period, org_id, org_name;

-- 10. 按机构汇总 - 存款利润
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'DEPOSIT_PROFIT', account_period, 'MONTH', 'ORG', org_id, org_name,
       SUM(deposit_profit), 'ASSESS', NOW()
FROM dwd_deposit_detail
GROUP BY account_period, org_id, org_name;

-- 11. 按机构汇总 - 存款余额
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'DEPOSIT_BALANCE', account_period, 'MONTH', 'ORG', org_id, org_name,
       SUM(deposit_balance), 'ASSESS', NOW()
FROM dwd_deposit_detail
GROUP BY account_period, org_id, org_name;

-- 12. 按产品汇总 - 贷款利息收入
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_INCOME', account_period, 'MONTH', 'PRODUCT', product_id, product_name,
       SUM(loan_monthly_interest), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, product_id, product_name;

-- 13. 按产品汇总 - 贷款FTP成本
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'FTP_COST', account_period, 'MONTH', 'PRODUCT', product_id, product_name,
       SUM(ftp_cost), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, product_id, product_name;

-- 14. 按产品汇总 - 贷款风险成本
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'RISK_COST', account_period, 'MONTH', 'PRODUCT', product_id, product_name,
       SUM(risk_cost), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, product_id, product_name;

-- 15. 按产品汇总 - 贷款运营成本
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'OP_COST', account_period, 'MONTH', 'PRODUCT', product_id, product_name,
       SUM(op_cost), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, product_id, product_name;

-- 16. 按产品汇总 - 贷款利润
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'LOAN_PROFIT', account_period, 'MONTH', 'PRODUCT', product_id, product_name,
       SUM(loan_profit), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, product_id, product_name;

-- 17. 按产品汇总 - 存款FTP收入
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'FTP_INCOME', account_period, 'MONTH', 'PRODUCT', product_id, product_name,
       SUM(ftp_income), 'ASSESS', NOW()
FROM dwd_deposit_detail
GROUP BY account_period, product_id, product_name;

-- 18. 按产品汇总 - 存款利息支出
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_EXPENSE', account_period, 'MONTH', 'PRODUCT', product_id, product_name,
       SUM(deposit_monthly_interest), 'ASSESS', NOW()
FROM dwd_deposit_detail
GROUP BY account_period, product_id, product_name;

-- 19. 按产品汇总 - 存款利润
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'DEPOSIT_PROFIT', account_period, 'MONTH', 'PRODUCT', product_id, product_name,
       SUM(deposit_profit), 'ASSESS', NOW()
FROM dwd_deposit_detail
GROUP BY account_period, product_id, product_name;

-- 20. 按条线汇总 - 贷款利息收入
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_INCOME', account_period, 'MONTH', 'BIZ_LINE', biz_line_id, biz_line_name,
       SUM(loan_monthly_interest), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, biz_line_id, biz_line_name;

-- 21. 按条线汇总 - 贷款利润
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'LOAN_PROFIT', account_period, 'MONTH', 'BIZ_LINE', biz_line_id, biz_line_name,
       SUM(loan_profit), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, biz_line_id, biz_line_name;

-- 22. 按条线汇总 - 存款FTP收入
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'FTP_INCOME', account_period, 'MONTH', 'BIZ_LINE', biz_line_id, biz_line_name,
       SUM(ftp_income), 'ASSESS', NOW()
FROM dwd_deposit_detail
GROUP BY account_period, biz_line_id, biz_line_name;

-- 23. 按条线汇总 - 存款利润
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'DEPOSIT_PROFIT', account_period, 'MONTH', 'BIZ_LINE', biz_line_id, biz_line_name,
       SUM(deposit_profit), 'ASSESS', NOW()
FROM dwd_deposit_detail
GROUP BY account_period, biz_line_id, biz_line_name;

-- 24. 按渠道汇总 - 贷款利息收入
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_INCOME', account_period, 'MONTH', 'CHANNEL', channel_id, channel_name,
       SUM(loan_monthly_interest), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, channel_id, channel_name;

-- 25. 按渠道汇总 - 贷款利润
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'LOAN_PROFIT', account_period, 'MONTH', 'CHANNEL', channel_id, channel_name,
       SUM(loan_profit), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, channel_id, channel_name;

-- 26. 按客户经理汇总 - 贷款利息收入
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_INCOME', account_period, 'MONTH', 'MANAGER', manager_id, manager_name,
       SUM(loan_monthly_interest), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, manager_id, manager_name;

-- 27. 按客户经理汇总 - 贷款利润
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'LOAN_PROFIT', account_period, 'MONTH', 'MANAGER', manager_id, manager_name,
       SUM(loan_profit), 'ASSESS', NOW()
FROM dwd_loan_detail
GROUP BY account_period, manager_id, manager_name;

-- 28. 按客户经理汇总 - 存款利润
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'DEPOSIT_PROFIT', account_period, 'MONTH', 'MANAGER', manager_id, manager_name,
       SUM(deposit_profit), 'ASSESS', NOW()
FROM dwd_deposit_detail
GROUP BY account_period, manager_id, manager_name;

-- ============================================
-- 验证
-- ============================================
SELECT '维度表统计' AS info;
SELECT 'dw_dim_organization' AS tbl, COUNT(*) AS cnt FROM dw_dim_organization
UNION ALL SELECT 'dw_dim_biz_line', COUNT(*) FROM dw_dim_biz_line
UNION ALL SELECT 'dw_dim_product', COUNT(*) FROM dw_dim_product
UNION ALL SELECT 'dw_dim_channel', COUNT(*) FROM dw_dim_channel
UNION ALL SELECT 'dw_dim_manager', COUNT(*) FROM dw_dim_manager
UNION ALL SELECT 'dw_dim_customer', COUNT(*) FROM dw_dim_customer;

SELECT 'DWD层统计' AS info;
SELECT 'dwd_loan_detail' AS tbl, COUNT(*) AS cnt FROM dwd_loan_detail
UNION ALL SELECT 'dwd_deposit_detail', COUNT(*) FROM dwd_deposit_detail;

SELECT 'DWS层统计' AS info;
SELECT dim_type, COUNT(*) AS cnt FROM dw_indicator_fact GROUP BY dim_type;

SELECT 'DWS层指标统计' AS info;
SELECT indicator_code, COUNT(*) AS cnt FROM dw_indicator_fact GROUP BY indicator_code ORDER BY cnt DESC;

SELECT 'Step 5 完成：DWD+DWS+维度表 数据生成成功' AS result;
