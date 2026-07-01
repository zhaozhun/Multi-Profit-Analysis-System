-- ============================================
-- 生成存款数据（每月50条，共900条）
-- ============================================

USE multi_profit;

DELIMITER //

CREATE PROCEDURE generate_deposit_data()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE month_val VARCHAR(7);
    DECLARE month_date DATE;
    DECLARE months_arr VARCHAR(200) DEFAULT '2025-01,2025-02,2025-03,2025-04,2025-05,2025-06,2025-07,2025-08,2025-09,2025-10,2025-11,2025-12,2026-01,2026-02,2026-03,2026-04,2026-05,2026-06';

    WHILE i < 18 DO
        SET month_val = SUBSTRING_INDEX(SUBSTRING_INDEX(months_arr, ',', i+1), ',', -1);
        SET month_date = STR_TO_DATE(CONCAT(month_val, '-15'), '%Y-%m-%d');

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
            CONCAT('DEP', LPAD((i * 50 + seq), 6, '0')),
            month_date,
            month_val,
            FLOOR(1 + RAND() * 100),
            CONCAT('客户', FLOOR(1 + RAND() * 100)),
            ELT(FLOOR(1 + RAND() * 5), 94, 95, 96, 97, 98),
            ELT(FLOOR(1 + RAND() * 5), '北京分行', '上海分行', '深圳分行', '广州分行', '杭州分行'),
            ELT(FLOOR(1 + RAND() * 3), 142, 143, 144),
            ELT(FLOOR(1 + RAND() * 3), '对公条线', '零售条线', '金融市场条线'),
            FLOOR(55 + RAND() * 4),
            ELT(FLOOR(1 + RAND() * 4), '科技部', '市场部', '运营部', '风控部'),
            ELT(FLOOR(1 + RAND() * 3), 110, 111, 112),
            ELT(FLOOR(1 + RAND() * 3), '公司存款', '个人存款', '理财产品'),
            ELT(FLOOR(1 + RAND() * 3), 163, 164, 165),
            ELT(FLOOR(1 + RAND() * 3), '线下渠道', '线上渠道', '网点渠道'),
            ELT(FLOOR(1 + RAND() * 5), 177, 178, 179, 180, 181),
            ELT(FLOOR(1 + RAND() * 5), '北京分行客户经理', '上海分行客户经理', '深圳分行客户经理', '广州分行客户经理', '杭州分行客户经理'),
            ROUND(50000 + RAND() * 4950000, 2),
            ROUND(0.01 + RAND() * 0.02, 6),
            'DAILY_ACCUMULATED',
            ROUND((50000 + RAND() * 4950000) * (0.01 + RAND() * 0.02) / 365, 4),
            ROUND((50000 + RAND() * 4950000) * (0.01 + RAND() * 0.02) / 365 * 30, 4),
            ROUND((50000 + RAND() * 4950000) * (0.01 + RAND() * 0.02) / 365 * 90, 4),
            ROUND(0.025 + RAND() * 0.015, 6),
            ROUND((50000 + RAND() * 4950000) * (0.025 + RAND() * 0.015) / 365 * 30, 2),
            ROUND((50000 + RAND() * 4950000) * (0.002 + RAND() * 0.006), 2),
            ELT(FLOOR(1 + RAND() * 2), '人工费用', '系统费用'),
            'ASSESS'
        FROM (
            SELECT a.N + b.N * 10 + 1 as seq
            FROM
                (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
                (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) b
        ) nums
        WHERE seq <= 50;

        SET i = i + 1;
    END WHILE;
END //

DELIMITER ;

CALL generate_deposit_data();
DROP PROCEDURE generate_deposit_data;

SELECT '存款数据生成完成' AS result, COUNT(*) as count FROM deposit_indicator_detail;
