-- ============================================
-- Step 3: biz_ledger 业务台账数据（修正版）
-- 时间范围：2025-01 到 2026-06（18个月）
-- 每月：贷款350条 + 存款150条 = 500条
-- 合计：9000条
-- ============================================

USE multi_profit;

-- 清空旧数据
TRUNCATE TABLE biz_ledger;

-- 临时账期表
DROP TEMPORARY TABLE IF EXISTS tmp_months;
CREATE TEMPORARY TABLE tmp_months (idx INT, period VARCHAR(7), period_date DATE);
INSERT INTO tmp_months VALUES
(1, '2025-01', '2025-01-15'), (2, '2025-02', '2025-02-15'), (3, '2025-03', '2025-03-15'),
(4, '2025-04', '2025-04-15'), (5, '2025-05', '2025-05-15'), (6, '2025-06', '2025-06-15'),
(7, '2025-07', '2025-07-15'), (8, '2025-08', '2025-08-15'), (9, '2025-09', '2025-09-15'),
(10, '2025-10', '2025-10-15'), (11, '2025-11', '2025-11-15'), (12, '2025-12', '2025-12-15'),
(13, '2026-01', '2026-01-15'), (14, '2026-02', '2026-02-15'), (15, '2026-03', '2026-03-15'),
(16, '2026-04', '2026-04-15'), (17, '2026-05', '2026-05-15'), (18, '2026-06', '2026-06-15');

-- ============================================
-- 1. 生成贷款数据（每月350条，共6300条）
-- ============================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_gen_loan//

CREATE PROCEDURE sp_gen_loan()
BEGIN
    DECLARE m INT DEFAULT 0;
    DECLARE p VARCHAR(7);
    DECLARE pd VARCHAR(10);
    DECLARE base_id INT;
    DECLARE i INT;

    WHILE m < 18 DO
        SELECT period, DATE_FORMAT(period_date, '%Y-%m-%d') INTO p, pd FROM tmp_months WHERE idx = m + 1;
        SET base_id = m * 350;
        SET i = 1;

        WHILE i <= 350 DO
            INSERT INTO biz_ledger (
                biz_id, stat_date, account_period,
                org_id, product_id, biz_line_id, dept_id, channel_id, manager_id, customer_id,
                product_type, biz_amount, revenue, interest_income, interest_expense,
                ftp_cost, risk_cost, op_cost, net_profit,
                loan_revenue, loan_ftp_cost, loan_risk_cost, loan_op_cost, loan_profit,
                caliber_type
            )
            SELECT
                CONCAT('L', LPAD(base_id + i, 6, '0')),
                STR_TO_DATE(pd, '%Y-%m-%d'),
                p,
                -- 机构
                CASE
                    WHEN i <= 88 THEN ELT(1 + FLOOR(RAND()*3), 23, 24, 25)
                    WHEN i <= 175 THEN ELT(1 + FLOOR(RAND()*2), 26, 27)
                    WHEN i <= 245 THEN ELT(1 + FLOOR(RAND()*2), 28, 29)
                    WHEN i <= 315 THEN 30
                    ELSE 31
                END,
                -- 产品
                CASE
                    WHEN i <= 140 THEN ELT(1 + FLOOR(RAND()*2), 56, 57)
                    WHEN i <= 263 THEN ELT(1 + FLOOR(RAND()*2), 58, 59)
                    ELSE ELT(1 + FLOOR(RAND()*2), 62, 63)
                END,
                -- 条线
                CASE
                    WHEN i <= 140 THEN 69
                    WHEN i <= 263 THEN 71
                    ELSE 73
                END,
                -- 部门
                CASE
                    WHEN i <= 140 THEN ELT(1 + FLOOR(RAND()*2), 44, 45)
                    WHEN i <= 263 THEN ELT(1 + FLOOR(RAND()*2), 46, 47)
                    ELSE 50
                END,
                -- 渠道
                CASE
                    WHEN i <= 210 THEN ELT(1 + FLOOR(RAND()*2), 84, 85)
                    ELSE ELT(1 + FLOOR(RAND()*2), 86, 87)
                END,
                -- 客户经理
                ELT(1 + FLOOR(RAND()*12), 115,116,117,118,119,120,121,122,123,124,125,126),
                -- 客户
                (SELECT id FROM customer_master WHERE customer_type='CORP' ORDER BY RAND() LIMIT 1),
                'LOAN',
                -- 余额
                CASE
                    WHEN i <= 140 THEN ROUND(5000000 + RAND() * 45000000, 2)
                    WHEN i <= 263 THEN ROUND(100000 + RAND() * 4900000, 2)
                    ELSE ROUND(2000000 + RAND() * 28000000, 2)
                END,
                0,
                -- 利息收入
                CASE
                    WHEN i <= 140 THEN ROUND((5000000 + RAND() * 45000000) * (0.03 + RAND() * 0.03) / 12, 2)
                    WHEN i <= 263 THEN ROUND((100000 + RAND() * 4900000) * (0.04 + RAND() * 0.06) / 12, 2)
                    ELSE ROUND((2000000 + RAND() * 28000000) * (0.05 + RAND() * 0.10) / 12, 2)
                END,
                0,
                -- FTP成本
                CASE
                    WHEN i <= 140 THEN ROUND((5000000 + RAND() * 45000000) * (0.02 + RAND() * 0.02) / 12, 2)
                    WHEN i <= 263 THEN ROUND((100000 + RAND() * 4900000) * (0.02 + RAND() * 0.02) / 12, 2)
                    ELSE ROUND((2000000 + RAND() * 28000000) * (0.02 + RAND() * 0.02) / 12, 2)
                END,
                -- 风险成本
                CASE
                    WHEN i <= 140 THEN ROUND((5000000 + RAND() * 45000000) * (0.005 + RAND() * 0.015), 2)
                    WHEN i <= 263 THEN ROUND((100000 + RAND() * 4900000) * (0.008 + RAND() * 0.012), 2)
                    ELSE ROUND((2000000 + RAND() * 28000000) * (0.003 + RAND() * 0.017), 2)
                END,
                -- 运营成本
                CASE
                    WHEN i <= 140 THEN ROUND((5000000 + RAND() * 45000000) * (0.003 + RAND() * 0.007), 2)
                    WHEN i <= 263 THEN ROUND((100000 + RAND() * 4900000) * (0.005 + RAND() * 0.005), 2)
                    ELSE ROUND((2000000 + RAND() * 28000000) * (0.003 + RAND() * 0.007), 2)
                END,
                0, 0, 0, 0, 0, 0,
                'ASSESS';

            SET i = i + 1;
        END WHILE;

        SET m = m + 1;
    END WHILE;
END //

-- ============================================
-- 2. 生成存款数据（每月150条，共2700条）
-- ============================================

DROP PROCEDURE IF EXISTS sp_gen_deposit//

CREATE PROCEDURE sp_gen_deposit()
BEGIN
    DECLARE m INT DEFAULT 0;
    DECLARE p VARCHAR(7);
    DECLARE pd VARCHAR(10);
    DECLARE base_id INT;
    DECLARE i INT;

    WHILE m < 18 DO
        SELECT period, DATE_FORMAT(period_date, '%Y-%m-%d') INTO p, pd FROM tmp_months WHERE idx = m + 1;
        SET base_id = m * 150;
        SET i = 1;

        WHILE i <= 150 DO
            INSERT INTO biz_ledger (
                biz_id, stat_date, account_period,
                org_id, product_id, biz_line_id, dept_id, channel_id, manager_id, customer_id,
                product_type, biz_amount, revenue, interest_income, interest_expense,
                ftp_cost, risk_cost, op_cost, net_profit,
                deposit_revenue, deposit_interest, deposit_op_cost, deposit_profit,
                caliber_type
            )
            SELECT
                CONCAT('D', LPAD(base_id + i, 6, '0')),
                STR_TO_DATE(pd, '%Y-%m-%d'),
                p,
                -- 机构
                CASE
                    WHEN i <= 38 THEN ELT(1 + FLOOR(RAND()*3), 23, 24, 25)
                    WHEN i <= 75 THEN ELT(1 + FLOOR(RAND()*2), 26, 27)
                    WHEN i <= 105 THEN ELT(1 + FLOOR(RAND()*2), 28, 29)
                    WHEN i <= 135 THEN 30
                    ELSE 31
                END,
                -- 产品
                CASE
                    WHEN i <= 45 THEN ELT(1 + FLOOR(RAND()*2), 48, 49)
                    WHEN i <= 105 THEN ELT(1 + FLOOR(RAND()*2), 50, 51)
                    ELSE ELT(1 + FLOOR(RAND()*2), 52, 53)
                END,
                -- 条线
                CASE
                    WHEN i <= 45 THEN 69
                    ELSE 72
                END,
                -- 部门
                CASE
                    WHEN i <= 45 THEN ELT(1 + FLOOR(RAND()*2), 44, 45)
                    WHEN i <= 105 THEN ELT(1 + FLOOR(RAND()*2), 48, 49)
                    ELSE 49
                END,
                -- 渠道
                CASE
                    WHEN i <= 75 THEN ELT(1 + FLOOR(RAND()*2), 84, 85)
                    ELSE ELT(1 + FLOOR(RAND()*2), 86, 87)
                END,
                -- 客户经理
                ELT(1 + FLOOR(RAND()*12), 115,116,117,118,119,120,121,122,123,124,125,126),
                -- 客户
                (SELECT id FROM customer_master ORDER BY RAND() LIMIT 1),
                'DEPOSIT',
                -- 余额
                CASE
                    WHEN i <= 45 THEN ROUND(1000000 + RAND() * 29000000, 2)
                    WHEN i <= 105 THEN ROUND(50000 + RAND() * 1950000, 2)
                    ELSE ROUND(500000 + RAND() * 9500000, 2)
                END,
                0,
                -- FTP收入
                CASE
                    WHEN i <= 45 THEN ROUND((1000000 + RAND() * 29000000) * (0.025 + RAND() * 0.015) / 12, 2)
                    WHEN i <= 105 THEN ROUND((50000 + RAND() * 1950000) * (0.025 + RAND() * 0.015) / 12, 2)
                    ELSE ROUND((500000 + RAND() * 9500000) * (0.03 + RAND() * 0.01) / 12, 2)
                END,
                -- 利息支出
                CASE
                    WHEN i <= 45 THEN ROUND((1000000 + RAND() * 29000000) * (0.01 + RAND() * 0.02) / 12, 2)
                    WHEN i <= 105 THEN ROUND((50000 + RAND() * 1950000) * (0.01 + RAND() * 0.02) / 12, 2)
                    ELSE ROUND((500000 + RAND() * 9500000) * (0.02 + RAND() * 0.01) / 12, 2)
                END,
                0, 0,
                -- 运营成本
                CASE
                    WHEN i <= 45 THEN ROUND((1000000 + RAND() * 29000000) * (0.002 + RAND() * 0.006), 2)
                    WHEN i <= 105 THEN ROUND((50000 + RAND() * 1950000) * (0.003 + RAND() * 0.005), 2)
                    ELSE ROUND((500000 + RAND() * 9500000) * (0.002 + RAND() * 0.006), 2)
                END,
                0, 0, 0, 0, 0,
                'ASSESS';

            SET i = i + 1;
        END WHILE;

        SET m = m + 1;
    END WHILE;
END //

DELIMITER ;

-- 执行生成
CALL sp_gen_loan();
CALL sp_gen_deposit();

-- 清理存储过程
DROP PROCEDURE IF EXISTS sp_gen_loan;
DROP PROCEDURE IF EXISTS sp_gen_deposit;

-- ============================================
-- 3. 回填利润字段
-- ============================================

-- 贷款利润
UPDATE biz_ledger SET
    loan_revenue = interest_income,
    loan_ftp_cost = ftp_cost,
    loan_risk_cost = risk_cost,
    loan_op_cost = op_cost,
    loan_profit = interest_income - ftp_cost - risk_cost - op_cost,
    net_profit = interest_income - ftp_cost - risk_cost - op_cost,
    revenue = interest_income
WHERE product_type = 'LOAN';

-- 存款利润
UPDATE biz_ledger SET
    deposit_revenue = interest_income,
    deposit_interest = interest_expense,
    deposit_op_cost = op_cost,
    deposit_profit = interest_income - interest_expense - op_cost,
    net_profit = interest_income - interest_expense - op_cost,
    revenue = interest_income
WHERE product_type = 'DEPOSIT';

-- ============================================
-- 4. 验证
-- ============================================
SELECT 'biz_ledger 总量' AS info, COUNT(*) AS cnt FROM biz_ledger;
SELECT '按产品类型' AS info, product_type, COUNT(*) AS cnt FROM biz_ledger GROUP BY product_type;
SELECT '按账期分布' AS info, account_period, COUNT(*) AS cnt FROM biz_ledger GROUP BY account_period ORDER BY account_period;
SELECT '按机构分布（前5）' AS info, org_id, COUNT(*) AS cnt FROM biz_ledger GROUP BY org_id ORDER BY cnt DESC LIMIT 5;

SELECT 'Step 3 完成：biz_ledger 数据生成成功' AS result;
