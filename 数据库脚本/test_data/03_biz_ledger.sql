-- =====================================================
-- 业务台账数据 (2025年1-12月)
-- 包含: 利息收入、手续费收入、存款成本、运营成本等
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE biz_ledger;

-- 生成12个月的业务数据
-- 使用存储过程简化数据生成

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS generate_biz_ledger()
BEGIN
    DECLARE v_month INT DEFAULT 1;
    DECLARE v_period VARCHAR(7);

    WHILE v_month <= 12 DO
        SET v_period = CONCAT('2025-', LPAD(v_month, 2, '0'));

        -- 插入各机构的业务数据
        INSERT INTO biz_ledger (period, org_id, product_id, biz_line_id, channel_type,
            loan_balance, deposit_balance, interest_income, fee_income, interest_cost,
            operation_cost, credit_cost, net_profit, biz_amount) VALUES
        -- 北京分行-线下贷款
        (v_period, 2, 5, 1, 'OFFLINE',
            50000000 + v_month * 2000000, 0,
            350000 + v_month * 15000, 0,
            0, 180000 + v_month * 5000, 25000,
            145000 + v_month * 10000, 150 + v_month * 5),
        (v_period, 2, 7, 1, 'OFFLINE',
            80000000 + v_month * 3000000, 0,
            480000 + v_month * 20000, 0,
            0, 220000 + v_month * 8000, 35000,
            225000 + v_month * 12000, 200 + v_month * 8),
        (v_period, 2, 8, 2, 'OFFLINE',
            120000000 + v_month * 5000000, 0,
            720000 + v_month * 30000, 0,
            0, 350000 + v_month * 12000, 50000,
            320000 + v_month * 18000, 80 + v_month * 3),

        -- 北京分行-线上贷款
        (v_period, 2, 6, 1, 'ONLINE',
            30000000 + v_month * 5000000, 0,
            210000 + v_month * 35000, 0,
            0, 80000 + v_month * 15000, 12000,
            118000 + v_month * 20000, 500 + v_month * 100),
        (v_period, 2, 10, 1, 'ONLINE',
            25000000 + v_month * 4000000, 0,
            175000 + v_month * 28000, 0,
            0, 60000 + v_month * 12000, 10000,
            105000 + v_month * 16000, 400 + v_month * 80),

        -- 北京分行-存款
        (v_period, 2, 11, 1, 'ONLINE',
            0, 150000000 + v_month * 8000000,
            0, 0, 450000 + v_month * 24000,
            120000 + v_month * 5000, 0,
            -570000 - v_month * 29000, 0),
        (v_period, 2, 12, 1, 'OFFLINE',
            0, 200000000 + v_month * 10000000,
            0, 0, 800000 + v_month * 40000,
            150000 + v_month * 6000, 0,
            -950000 - v_month * 46000, 0),

        -- 北京分行-理财手续费
        (v_period, 2, 14, 1, 'ONLINE',
            0, 0, 0,
            85000 + v_month * 5000, 0,
            30000 + v_month * 2000, 0,
            55000 + v_month * 3000, 200 + v_month * 15),
        (v_period, 2, 17, 2, 'ONLINE',
            0, 0, 0,
            45000 + v_month * 3000, 0,
            15000 + v_month * 1000, 0,
            30000 + v_month * 2000, 800 + v_month * 50),

        -- 上海分行
        (v_period, 3, 5, 1, 'OFFLINE',
            45000000 + v_month * 1800000, 0,
            315000 + v_month * 12600, 0,
            0, 160000 + v_month * 4500, 22000,
            133000 + v_month * 8100, 130 + v_month * 4),
        (v_period, 3, 8, 2, 'OFFLINE',
            100000000 + v_month * 4000000, 0,
            600000 + v_month * 24000, 0,
            0, 300000 + v_month * 10000, 42000,
            258000 + v_month * 14000, 65 + v_month * 2),
        (v_period, 3, 6, 1, 'ONLINE',
            28000000 + v_month * 4500000, 0,
            196000 + v_month * 31500, 0,
            0, 75000 + v_month * 13500, 11000,
            110000 + v_month * 18000, 450 + v_month * 90),
        (v_period, 3, 11, 1, 'ONLINE',
            0, 130000000 + v_month * 7000000,
            0, 0, 390000 + v_month * 21000,
            110000 + v_month * 4500, 0,
            -500000 - v_month * 25500, 0),
        (v_period, 3, 15, 1, 'ONLINE',
            0, 0, 0,
            65000 + v_month * 4000, 0,
            25000 + v_month * 1500, 0,
            40000 + v_month * 2500, 150 + v_month * 10),

        -- 广州分行
        (v_period, 4, 5, 1, 'OFFLINE',
            40000000 + v_month * 1500000, 0,
            280000 + v_month * 10500, 0,
            0, 140000 + v_month * 4000, 18000,
            122000 + v_month * 6500, 110 + v_month * 3),
        (v_period, 4, 8, 2, 'OFFLINE',
            90000000 + v_month * 3500000, 0,
            540000 + v_month * 21000, 0,
            0, 270000 + v_month * 9000, 38000,
            232000 + v_month * 12000, 55 + v_month * 2),
        (v_period, 4, 9, 3, 'ONLINE',
            20000000 + v_month * 3000000, 0,
            140000 + v_month * 21000, 0,
            0, 50000 + v_month * 9000, 8000,
            82000 + v_month * 12000, 350 + v_month * 70),
        (v_period, 4, 11, 1, 'ONLINE',
            0, 120000000 + v_month * 6000000,
            0, 0, 360000 + v_month * 18000,
            100000 + v_month * 4000, 0,
            -460000 - v_month * 22000, 0),

        -- 深圳分行
        (v_period, 5, 5, 1, 'OFFLINE',
            55000000 + v_month * 2200000, 0,
            385000 + v_month * 15400, 0,
            0, 190000 + v_month * 5500, 28000,
            167000 + v_month * 9900, 170 + v_month * 6),
        (v_period, 5, 8, 2, 'OFFLINE',
            110000000 + v_month * 4500000, 0,
            660000 + v_month * 27000, 0,
            0, 330000 + v_month * 11000, 46000,
            284000 + v_month * 16000, 70 + v_month * 3),
        (v_period, 5, 6, 1, 'ONLINE',
            32000000 + v_month * 5000000, 0,
            224000 + v_month * 35000, 0,
            0, 85000 + v_month * 15000, 13000,
            126000 + v_month * 20000, 550 + v_month * 110),
        (v_period, 5, 9, 3, 'ONLINE',
            18000000 + v_month * 2500000, 0,
            126000 + v_month * 17500, 0,
            0, 45000 + v_month * 8000, 7000,
            74000 + v_month * 9500, 300 + v_month * 60),
        (v_period, 5, 11, 1, 'ONLINE',
            0, 140000000 + v_month * 7500000,
            0, 0, 420000 + v_month * 22500,
            115000 + v_month * 5000, 0,
            -535000 - v_month * 27500, 0),
        (v_period, 5, 14, 1, 'ONLINE',
            0, 0, 0,
            75000 + v_month * 4500, 0,
            28000 + v_month * 1800, 0,
            47000 + v_month * 2700, 180 + v_month * 12);

        SET v_month = v_month + 1;
    END WHILE;
END //

DELIMITER ;

CALL generate_biz_ledger();
DROP PROCEDURE IF EXISTS generate_biz_ledger;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '业务台账数据插入完成' AS result;
