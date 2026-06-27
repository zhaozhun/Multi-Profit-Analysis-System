-- =====================================================
-- 业务台账数据 (2025年1-12月)
-- 使用biz_ledger表，包含完整的盈利分析字段
-- =====================================================

-- 清空现有数据
DELETE FROM biz_ledger WHERE account_period LIKE '2025%';

-- 生成12个月的业务数据
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS generate_biz_ledger()
BEGIN
    DECLARE v_month INT DEFAULT 1;
    DECLARE v_period VARCHAR(7);
    DECLARE v_date DATE;
    DECLARE v_biz_id INT DEFAULT 1;

    WHILE v_month <= 12 DO
        SET v_period = CONCAT('2025-', LPAD(v_month, 2, '0'));
        SET v_date = CONCAT(v_period, '-01');

        INSERT INTO biz_ledger (biz_id, stat_date, account_period, org_id, product_id, biz_line_id, dept_id, channel_id, manager_id, customer_id, product_type, biz_amount, revenue, interest_income, interest_expense, fee_income, non_interest_income, ftp_cost, risk_cost, op_cost, net_profit, loan_revenue, loan_ftp_cost, loan_risk_cost, loan_op_cost, loan_profit, deposit_revenue, deposit_interest, deposit_op_cost, deposit_profit, caliber_type) VALUES
        -- 北京分行-零售条线-线下贷款
        (CONCAT('BIZ_', v_month, '_001'), v_date, v_period, 2, 25, 15, 15, 44, 51, 57, 'ASSET',
            150 + v_month * 5, 145000 + v_month * 6000, 130000 + v_month * 5500, 0, 0, 15000 + v_month * 500,
            35000 + v_month * 1500, 8000 + v_month * 300, 25000 + v_month * 1000,
            77000 + v_month * 3200, 130000 + v_month * 5500, 35000 + v_month * 1500, 8000 + v_month * 300, 25000 + v_month * 1000, 62000 + v_month * 2700,
            0, 0, 0, 0, 'ASSESS'),

        -- 北京分行-零售条线-线上贷款
        (CONCAT('BIZ_', v_month, '_002'), v_date, v_period, 2, 26, 15, 15, 43, 51, 57, 'ASSET',
            500 + v_month * 100, 85000 + v_month * 15000, 75000 + v_month * 13000, 0, 0, 10000 + v_month * 2000,
            18000 + v_month * 3000, 5000 + v_month * 800, 12000 + v_month * 2000,
            50000 + v_month * 9200, 75000 + v_month * 13000, 18000 + v_month * 3000, 5000 + v_month * 800, 12000 + v_month * 2000, 40000 + v_month * 7200,
            0, 0, 0, 0, 'ASSESS'),

        -- 北京分行-对公条线-线下贷款
        (CONCAT('BIZ_', v_month, '_003'), v_date, v_period, 2, 28, 16, 16, 44, 52, 64, 'ASSET',
            80 + v_month * 3, 220000 + v_month * 10000, 200000 + v_month * 9000, 0, 0, 20000 + v_month * 1000,
            55000 + v_month * 2500, 12000 + v_month * 500, 35000 + v_month * 1500,
            118000 + v_month * 5500, 200000 + v_month * 9000, 55000 + v_month * 2500, 12000 + v_month * 500, 35000 + v_month * 1500, 98000 + v_month * 4500,
            0, 0, 0, 0, 'ASSESS'),

        -- 北京分行-存款
        (CONCAT('BIZ_', v_month, '_004'), v_date, v_period, 2, 29, 15, 17, 43, 51, 57, 'LIAB',
            0, 0, 0, 180000 + v_month * 8000, 0, 0,
            0, 0, 35000 + v_month * 1500,
            -215000 - v_month * 9500, 0, 0, 0, 0, 0,
            50000 + v_month * 2000, 180000 + v_month * 8000, 35000 + v_month * 1500, -165000 - v_month * 7500, 'ASSESS'),

        -- 北京分行-理财手续费
        (CONCAT('BIZ_', v_month, '_005'), v_date, v_period, 2, 35, 15, 15, 43, 51, 57, 'OFF_BALANCE',
            200 + v_month * 15, 55000 + v_month * 3000, 0, 0, 55000 + v_month * 3000, 0,
            0, 0, 15000 + v_month * 800,
            40000 + v_month * 2200, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 'ASSESS'),

        -- 上海分行-零售条线-线下贷款
        (CONCAT('BIZ_', v_month, '_006'), v_date, v_period, 3, 27, 15, 15, 44, 55, 60, 'ASSET',
            120 + v_month * 4, 180000 + v_month * 7500, 160000 + v_month * 6500, 0, 0, 20000 + v_month * 1000,
            42000 + v_month * 1800, 9500 + v_month * 400, 28000 + v_month * 1200,
            100500 + v_month * 4100, 160000 + v_month * 6500, 42000 + v_month * 1800, 9500 + v_month * 400, 28000 + v_month * 1200, 80500 + v_month * 3100,
            0, 0, 0, 0, 'ASSESS'),

        -- 上海分行-对公条线-线下贷款
        (CONCAT('BIZ_', v_month, '_007'), v_date, v_period, 3, 28, 16, 16, 44, 56, 65, 'ASSET',
            65 + v_month * 2, 195000 + v_month * 8500, 175000 + v_month * 7500, 0, 0, 20000 + v_month * 1000,
            48000 + v_month * 2100, 10500 + v_month * 450, 32000 + v_month * 1400,
            104500 + v_month * 4550, 175000 + v_month * 7500, 48000 + v_month * 2100, 10500 + v_month * 450, 32000 + v_month * 1400, 84500 + v_month * 3550,
            0, 0, 0, 0, 'ASSESS'),

        -- 上海分行-线上贷款
        (CONCAT('BIZ_', v_month, '_008'), v_date, v_period, 3, 26, 15, 15, 43, 55, 60, 'ASSET',
            450 + v_month * 90, 78000 + v_month * 14000, 68000 + v_month * 12000, 0, 0, 10000 + v_month * 2000,
            16000 + v_month * 2800, 4500 + v_month * 700, 11000 + v_month * 1800,
            46500 + v_month * 8700, 68000 + v_month * 12000, 16000 + v_month * 2800, 4500 + v_month * 700, 11000 + v_month * 1800, 36500 + v_month * 6700,
            0, 0, 0, 0, 'ASSESS'),

        -- 广州分行-零售条线
        (CONCAT('BIZ_', v_month, '_009'), v_date, v_period, 4, 25, 15, 15, 44, 57, 62, 'ASSET',
            130 + v_month * 4, 155000 + v_month * 6500, 140000 + v_month * 5800, 0, 0, 15000 + v_month * 700,
            38000 + v_month * 1600, 8500 + v_month * 350, 26000 + v_month * 1100,
            82500 + v_month * 3450, 140000 + v_month * 5800, 38000 + v_month * 1600, 8500 + v_month * 350, 26000 + v_month * 1100, 67500 + v_month * 2750,
            0, 0, 0, 0, 'ASSESS'),

        -- 广州分行-小微条线-线上
        (CONCAT('BIZ_', v_month, '_010'), v_date, v_period, 4, 29, 17, 17, 43, 58, 68, 'ASSET',
            350 + v_month * 70, 65000 + v_month * 11000, 58000 + v_month * 9500, 0, 0, 7000 + v_month * 1500,
            14000 + v_month * 2400, 3800 + v_month * 600, 10000 + v_month * 1600,
            37200 + v_month * 6400, 58000 + v_month * 9500, 14000 + v_month * 2400, 3800 + v_month * 600, 10000 + v_month * 1600, 30200 + v_month * 4900,
            0, 0, 0, 0, 'ASSESS'),

        -- 深圳分行-零售条线-线下
        (CONCAT('BIZ_', v_month, '_011'), v_date, v_period, 5, 25, 15, 15, 44, 59, 63, 'ASSET',
            160 + v_month * 6, 170000 + v_month * 7200, 152000 + v_month * 6400, 0, 0, 18000 + v_month * 800,
            40000 + v_month * 1700, 9000 + v_month * 380, 27000 + v_month * 1150,
            94000 + v_month * 3970, 152000 + v_month * 6400, 40000 + v_month * 1700, 9000 + v_month * 380, 27000 + v_month * 1150, 76000 + v_month * 3170,
            0, 0, 0, 0, 'ASSESS'),

        -- 深圳分行-对公条线-线下
        (CONCAT('BIZ_', v_month, '_012'), v_date, v_period, 5, 28, 16, 16, 44, 60, 67, 'ASSET',
            70 + v_month * 3, 210000 + v_month * 9000, 190000 + v_month * 8000, 0, 0, 20000 + v_month * 1000,
            52000 + v_month * 2200, 11500 + v_month * 500, 34000 + v_month * 1450,
            112500 + v_month * 4850, 190000 + v_month * 8000, 52000 + v_month * 2200, 11500 + v_month * 500, 34000 + v_month * 1450, 92500 + v_month * 3850,
            0, 0, 0, 0, 'ASSESS'),

        -- 深圳分行-线上贷款
        (CONCAT('BIZ_', v_month, '_013'), v_date, v_period, 5, 26, 15, 15, 43, 59, 63, 'ASSET',
            550 + v_month * 110, 92000 + v_month * 16000, 80000 + v_month * 14000, 0, 0, 12000 + v_month * 2000,
            20000 + v_month * 3500, 5500 + v_month * 900, 13000 + v_month * 2200,
            53500 + v_month * 9400, 80000 + v_month * 14000, 20000 + v_month * 3500, 5500 + v_month * 900, 13000 + v_month * 2200, 41500 + v_month * 7400,
            0, 0, 0, 0, 'ASSESS'),

        -- 深圳分行-存款
        (CONCAT('BIZ_', v_month, '_014'), v_date, v_period, 5, 29, 15, 17, 43, 59, 63, 'LIAB',
            0, 0, 0, 160000 + v_month * 7000, 0, 0,
            0, 0, 32000 + v_month * 1400,
            -192000 - v_month * 8400, 0, 0, 0, 0, 0,
            45000 + v_month * 1800, 160000 + v_month * 7000, 32000 + v_month * 1400, -147000 - v_month * 6600, 'ASSESS'),

        -- 深圳分行-理财
        (CONCAT('BIZ_', v_month, '_015'), v_date, v_period, 5, 35, 15, 15, 43, 59, 63, 'OFF_BALANCE',
            180 + v_month * 12, 47000 + v_month * 2500, 0, 0, 47000 + v_month * 2500, 0,
            0, 0, 13000 + v_month * 700,
            34000 + v_month * 1800, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 'ASSESS');

        SET v_month = v_month + 1;
    END WHILE;
END //

DELIMITER ;

CALL generate_biz_ledger();
DROP PROCEDURE IF EXISTS generate_biz_ledger;

SELECT '业务台账数据插入完成' AS result;
