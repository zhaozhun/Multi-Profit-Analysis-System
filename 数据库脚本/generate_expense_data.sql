-- ============================================
-- 生成费用数据（每月都有）
-- ============================================

USE multi_profit;

DELIMITER //

CREATE PROCEDURE generate_expense_data()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE month_val VARCHAR(7);
    DECLARE months_arr VARCHAR(200) DEFAULT '2025-01,2025-02,2025-03,2025-04,2025-05,2025-06,2025-07,2025-08,2025-09,2025-10,2025-11,2025-12,2026-01,2026-02,2026-03,2026-04,2026-05,2026-06';

    WHILE i < 18 DO
        SET month_val = SUBSTRING_INDEX(SUBSTRING_INDEX(months_arr, ',', i+1), ',', -1);

        -- 房租物业（部门维度）
        INSERT INTO expense_rent (period, dept_id, dept_name, amount) VALUES
        (month_val, 55, '科技部', 80000 + RAND() * 40000),
        (month_val, 56, '市场部', 60000 + RAND() * 30000),
        (month_val, 57, '运营部', 50000 + RAND() * 25000),
        (month_val, 58, '风控部', 40000 + RAND() * 20000);

        -- 人力成本（部门+人员维度）
        INSERT INTO expense_salary (period, dept_id, dept_name, manager_id, manager_name, amount) VALUES
        (month_val, 55, '科技部', 177, '北京分行客户经理', 15000 + RAND() * 10000),
        (month_val, 55, '科技部', 178, '上海分行客户经理', 15000 + RAND() * 10000),
        (month_val, 56, '市场部', 179, '深圳分行客户经理', 12000 + RAND() * 8000),
        (month_val, 56, '市场部', 180, '广州分行客户经理', 12000 + RAND() * 8000),
        (month_val, 57, '运营部', 181, '杭州分行客户经理', 10000 + RAND() * 6000);

        -- IT系统费用（产品维度）
        INSERT INTO expense_it (period, product_id, product_name, amount) VALUES
        (month_val, 108, '公司贷款', 40000 + RAND() * 20000),
        (month_val, 109, '个人贷款', 30000 + RAND() * 15000),
        (month_val, 110, '公司存款', 20000 + RAND() * 10000);

        -- 营销费用（机构维度）
        INSERT INTO expense_marketing (period, org_id, org_name, amount) VALUES
        (month_val, 94, '北京分行', 150000 + RAND() * 100000),
        (month_val, 95, '上海分行', 120000 + RAND() * 80000),
        (month_val, 96, '深圳分行', 100000 + RAND() * 60000);

        -- 行政费用（部门维度）
        INSERT INTO expense_other (period, expense_type, dim_type, dim_id, dim_name, amount) VALUES
        (month_val, 'ADMIN', 'DEPT', 55, '科技部', 20000 + RAND() * 10000),
        (month_val, 'ADMIN', 'DEPT', 56, '市场部', 15000 + RAND() * 8000),
        (month_val, 'ADMIN', 'DEPT', 57, '运营部', 12000 + RAND() * 6000),
        (month_val, 'ADMIN', 'DEPT', 58, '风控部', 10000 + RAND() * 5000);

        SET i = i + 1;
    END WHILE;
END //

DELIMITER ;

CALL generate_expense_data();
DROP PROCEDURE generate_expense_data;

SELECT '费用数据生成完成' AS result;
SELECT 'expense_rent' as table_name, COUNT(*) as count FROM expense_rent
UNION ALL
SELECT 'expense_salary', COUNT(*) FROM expense_salary
UNION ALL
SELECT 'expense_it', COUNT(*) FROM expense_it
UNION ALL
SELECT 'expense_marketing', COUNT(*) FROM expense_marketing
UNION ALL
SELECT 'expense_other', COUNT(*) FROM expense_other;
