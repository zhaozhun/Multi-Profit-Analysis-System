-- ============================================
-- 从 biz_ledger 填充 loan_indicator_detail 和 deposit_indicator_detail
-- 时间范围：2025-01 到 2026-06（18个月）
-- 每月：贷款350条 + 存款150条 = 500条
-- ============================================

USE multi_profit;

-- 清空现有数据
TRUNCATE TABLE loan_indicator_detail;
TRUNCATE TABLE deposit_indicator_detail;

-- 统一字符集
ALTER TABLE loan_indicator_detail CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE deposit_indicator_detail CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================
-- 1. 填充贷款数据（每月350条）
-- ============================================

DROP PROCEDURE IF EXISTS populate_loan_data;

DELIMITER //

CREATE PROCEDURE populate_loan_data()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE month_val VARCHAR(7);
    DECLARE months_arr VARCHAR(300) DEFAULT '2025-01,2025-02,2025-03,2025-04,2025-05,2025-06,2025-07,2025-08,2025-09,2025-10,2025-11,2025-12,2026-01,2026-02,2026-03,2026-04,2026-05,2026-06';

    WHILE i < 18 DO
        SET month_val = SUBSTRING_INDEX(SUBSTRING_INDEX(months_arr, ',', i+1), ',', -1);

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
            CONCAT('客户', LPAD(ROW_NUMBER() OVER (ORDER BY bl.biz_id), 4, '0')) as customer_name,
            bl.org_id,
            org.name as org_name,
            bl.biz_line_id,
            biz.name as biz_line_name,
            bl.dept_id,
            dept.name as dept_name,
            bl.product_id,
            prod.name as product_name,
            bl.channel_id,
            ch.name as channel_name,
            bl.manager_id,
            mgr.name as manager_name,
            -- 贷款余额
            bl.biz_amount as loan_balance,
            -- 贷款利率：根据利息收入反推（interest_income / biz_amount）
            ROUND(bl.interest_income / bl.biz_amount, 6) as loan_rate,
            'DAILY_ACCUMULATED' as loan_interest_calc_type,
            -- 当日利息 = 利息收入 / 30（假设月利息）
            ROUND(bl.interest_income / 30, 4) as loan_daily_interest,
            -- 当月利息 = 利息收入
            bl.interest_income as loan_monthly_interest,
            -- 累计利息 = 利息收入 * 3（假设3个月累计）
            ROUND(bl.interest_income * 3, 4) as loan_cumulative_interest,
            -- FTP利率 = FTP成本 / 余额
            ROUND(bl.ftp_cost / bl.biz_amount, 6) as ftp_rate,
            bl.ftp_cost,
            bl.risk_cost,
            bl.op_cost,
            CASE WHEN RAND() > 0.5 THEN '人工费用' ELSE '系统费用' END as expense_type,
            'ASSESS' as caliber_type
        FROM biz_ledger bl
        LEFT JOIN dimension_master org ON bl.org_id = org.id AND org.dim_type = 'ORG'
        LEFT JOIN dimension_master biz ON bl.biz_line_id = biz.id AND biz.dim_type = 'BIZ_LINE'
        LEFT JOIN dimension_master dept ON bl.dept_id = dept.id AND dept.dim_type = 'DEPT'
        LEFT JOIN dimension_master prod ON bl.product_id = prod.id AND prod.dim_type = 'PRODUCT'
        LEFT JOIN dimension_master ch ON bl.channel_id = ch.id AND ch.dim_type = 'CHANNEL'
        LEFT JOIN dimension_master mgr ON bl.manager_id = mgr.id AND mgr.dim_type = 'MANAGER'
        WHERE bl.account_period = month_val
          AND bl.product_type = 'LOAN'
        ORDER BY RAND()
        LIMIT 350;

        SET i = i + 1;
    END WHILE;
END //

DELIMITER ;

-- 执行贷款数据填充
CALL populate_loan_data();
DROP PROCEDURE IF EXISTS populate_loan_data;

-- ============================================
-- 2. 填充存款数据（每月150条）
-- ============================================

DROP PROCEDURE IF EXISTS populate_deposit_data;

DELIMITER //

CREATE PROCEDURE populate_deposit_data()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE month_val VARCHAR(7);
    DECLARE months_arr VARCHAR(300) DEFAULT '2025-01,2025-02,2025-03,2025-04,2025-05,2025-06,2025-07,2025-08,2025-09,2025-10,2025-11,2025-12,2026-01,2026-02,2026-03,2026-04,2026-05,2026-06';

    WHILE i < 18 DO
        SET month_val = SUBSTRING_INDEX(SUBSTRING_INDEX(months_arr, ',', i+1), ',', -1);

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
            CONCAT('客户', LPAD(ROW_NUMBER() OVER (ORDER BY bl.biz_id), 4, '0')) as customer_name,
            bl.org_id,
            org.name as org_name,
            bl.biz_line_id,
            biz.name as biz_line_name,
            bl.dept_id,
            dept.name as dept_name,
            bl.product_id,
            prod.name as product_name,
            bl.channel_id,
            ch.name as channel_name,
            bl.manager_id,
            mgr.name as manager_name,
            -- 存款余额
            bl.biz_amount as deposit_balance,
            -- 存款利率 = 利息支出 / 余额
            ROUND(bl.interest_expense / bl.biz_amount, 6) as deposit_rate,
            'DAILY_ACCUMULATED' as deposit_interest_calc_type,
            -- 当日利息 = 利息支出 / 30
            ROUND(bl.interest_expense / 30, 4) as deposit_daily_interest,
            -- 当月利息 = 利息支出
            bl.interest_expense as deposit_monthly_interest,
            -- 累计利息 = 利息支出 * 3
            ROUND(bl.interest_expense * 3, 4) as deposit_cumulative_interest,
            -- FTP利率 = FTP收入 / 余额（存款没有ftp_cost，用interest_income代替）
            ROUND(bl.interest_income / bl.biz_amount, 6) as ftp_rate,
            bl.interest_income as ftp_income,
            bl.op_cost,
            CASE WHEN RAND() > 0.5 THEN '人工费用' ELSE '系统费用' END as expense_type,
            'ASSESS' as caliber_type
        FROM biz_ledger bl
        LEFT JOIN dimension_master org ON bl.org_id = org.id AND org.dim_type = 'ORG'
        LEFT JOIN dimension_master biz ON bl.biz_line_id = biz.id AND biz.dim_type = 'BIZ_LINE'
        LEFT JOIN dimension_master dept ON bl.dept_id = dept.id AND dept.dim_type = 'DEPT'
        LEFT JOIN dimension_master prod ON bl.product_id = prod.id AND prod.dim_type = 'PRODUCT'
        LEFT JOIN dimension_master ch ON bl.channel_id = ch.id AND ch.dim_type = 'CHANNEL'
        LEFT JOIN dimension_master mgr ON bl.manager_id = mgr.id AND mgr.dim_type = 'MANAGER'
        WHERE bl.account_period = month_val
          AND bl.product_type = 'DEPOSIT'
        ORDER BY RAND()
        LIMIT 150;

        SET i = i + 1;
    END WHILE;
END //

DELIMITER ;

-- 执行存款数据填充
CALL populate_deposit_data();
DROP PROCEDURE IF EXISTS populate_deposit_data;

-- ============================================
-- 3. 验证数据
-- ============================================

SELECT '贷款数据统计' as info;
SELECT account_period, COUNT(*) as count
FROM loan_indicator_detail
GROUP BY account_period
ORDER BY account_period;

SELECT '存款数据统计' as info;
SELECT account_period, COUNT(*) as count
FROM deposit_indicator_detail
GROUP BY account_period
ORDER BY account_period;

SELECT '总数据量' as info;
SELECT 'loan_indicator_detail' as table_name, COUNT(*) as count FROM loan_indicator_detail
UNION ALL
SELECT 'deposit_indicator_detail', COUNT(*) FROM deposit_indicator_detail;

SELECT '数据填充完成' as result;
