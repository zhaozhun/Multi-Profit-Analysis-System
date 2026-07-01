-- 生成1000条账户级数据
-- 时间范围：2025-01 到 2026-06（17个月）
-- 资产类（贷款）：700条，负债类（存款）：300条

DELIMITER //

CREATE PROCEDURE generate_mock_data()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE month_idx INT DEFAULT 0;
    DECLARE current_period VARCHAR(7);
    DECLARE biz_type VARCHAR(10);
    DECLARE product_code VARCHAR(10);
    DECLARE org_code VARCHAR(10);
    DECLARE dept_code VARCHAR(10);
    DECLARE channel_code VARCHAR(10);
    DECLARE manager_code VARCHAR(10);
    DECLARE biz_line_code VARCHAR(10);
    DECLARE biz_amount DECIMAL(18,2);
    DECLARE interest_income DECIMAL(18,2);
    DECLARE interest_expense DECIMAL(18,2);
    DECLARE ftp_cost DECIMAL(18,2);
    DECLARE risk_cost DECIMAL(18,2);
    DECLARE op_cost DECIMAL(18,2);
    DECLARE net_profit DECIMAL(18,2);

    -- 清空现有数据
    DELETE FROM biz_ledger;

    -- 生成1000条数据
    WHILE i < 1000 DO
        -- 随机选择月份（2025-01 到 2026-06）
        SET month_idx = FLOOR(RAND() * 17);
        SET current_period = DATE_FORMAT(DATE_ADD('2025-01-01', INTERVAL month_idx MONTH), '%Y-%m');

        -- 随机选择业务类型（70%资产，30%负债）
        IF RAND() < 0.7 THEN
            SET biz_type = 'ASSET';
            -- 资产类产品
            SET product_code = ELT(FLOOR(RAND() * 4) + 1, 'P001', 'P002', 'P003', 'P004');
            -- 资产类金额
            SET biz_amount = ROUND(10000 + RAND() * 490000, 2);
            SET interest_income = ROUND(biz_amount * (0.03 + RAND() * 0.05), 2);
            SET interest_expense = 0;
            SET ftp_cost = ROUND(interest_income * (0.4 + RAND() * 0.2), 2);
            SET risk_cost = ROUND(interest_income * (0.05 + RAND() * 0.15), 2);
            SET op_cost = ROUND(interest_income * (0.1 + RAND() * 0.1), 2);
            SET net_profit = interest_income - ftp_cost - risk_cost - op_cost;
        ELSE
            SET biz_type = 'LIABILITY';
            -- 负债类产品
            SET product_code = ELT(FLOOR(RAND() * 4) + 1, 'P005', 'P006', 'P007', 'P008');
            -- 负债类金额
            SET biz_amount = ROUND(10000 + RAND() * 490000, 2);
            SET interest_income = 0;
            SET interest_expense = ROUND(biz_amount * (0.015 + RAND() * 0.025), 2);
            SET ftp_cost = 0;
            SET risk_cost = 0;
            SET op_cost = ROUND(interest_expense * (0.1 + RAND() * 0.1), 2);
            SET net_profit = interest_expense - op_cost;
        END IF;

        -- 随机选择维度
        SET org_code = ELT(FLOOR(RAND() * 6) + 1, 'ORG001', 'ORG002', 'ORG003', 'ORG004', 'ORG005', 'ORG006');
        SET dept_code = ELT(FLOOR(RAND() * 5) + 1, 'DEPT001', 'DEPT002', 'DEPT003', 'DEPT004', 'DEPT005');
        SET channel_code = ELT(FLOOR(RAND() * 4) + 1, 'CH001', 'CH002', 'CH003', 'CH004');
        SET manager_code = ELT(FLOOR(RAND() * 10) + 1, 'MGR001', 'MGR002', 'MGR003', 'MGR004', 'MGR005', 'MGR006', 'MGR007', 'MGR008', 'MGR009', 'MGR010');

        IF biz_type = 'ASSET' THEN
            SET biz_line_code = ELT(FLOOR(RAND() * 2) + 1, 'BL001', 'BL002');
        ELSE
            SET biz_line_code = 'BL001';
        END IF;

        -- 插入数据
        INSERT INTO biz_ledger (
            biz_id, stat_date, account_period,
            org_id, product_id, biz_line_id, dept_id, channel_id, manager_id,
            product_type, biz_amount, revenue,
            interest_income, interest_expense, ftp_cost, risk_cost, op_cost, net_profit,
            caliber_type, currency
        ) VALUES (
            CONCAT('BIZ_', LPAD(i + 1, 4, '0'), '_', REPLACE(current_period, '-', '')),
            CONCAT(current_period, '-01'),
            current_period,
            (SELECT id FROM dimension_master WHERE dim_type = 'ORG' AND code = org_code LIMIT 1),
            (SELECT id FROM dimension_master WHERE dim_type = 'PRODUCT' AND code = product_code LIMIT 1),
            (SELECT id FROM dimension_master WHERE dim_type = 'BIZ_LINE' AND code = biz_line_code LIMIT 1),
            (SELECT id FROM dimension_master WHERE dim_type = 'DEPT' AND code = dept_code LIMIT 1),
            (SELECT id FROM dimension_master WHERE dim_type = 'CHANNEL' AND code = channel_code LIMIT 1),
            (SELECT id FROM dimension_master WHERE dim_type = 'MANAGER' AND code = manager_code LIMIT 1),
            biz_type,
            biz_amount,
            interest_income + interest_expense,
            interest_income,
            interest_expense,
            ftp_cost,
            risk_cost,
            op_cost,
            net_profit,
            'BOOK',
            'CNY'
        );

        SET i = i + 1;
    END WHILE;
END //

DELIMITER ;

-- 执行存储过程
CALL generate_mock_data();

-- 删除存储过程
DROP PROCEDURE IF EXISTS generate_mock_data;
