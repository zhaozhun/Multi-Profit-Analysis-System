-- 生成费用分摊数据
-- 为每月每种费用生成分摊结果

DELIMITER //

CREATE PROCEDURE generate_allocation_data()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_period VARCHAR(7);
    DECLARE v_cost_code VARCHAR(50);
    DECLARE v_cost_name VARCHAR(100);
    DECLARE v_algorithm VARCHAR(50);
    DECLARE v_factor VARCHAR(50);
    DECLARE v_total_amount DECIMAL(18,2);
    DECLARE v_account_count INT;
    DECLARE v_avg_amount DECIMAL(18,2);
    DECLARE v_biz_id VARCHAR(30);
    DECLARE v_biz_amount DECIMAL(18,2);
    DECLARE v_ratio DECIMAL(18,8);
    DECLARE v_allocated DECIMAL(18,2);
    DECLARE v_batch_no VARCHAR(50);
    DECLARE v_idx INT;

    -- 获取所有账期
    DECLARE period_cursor CURSOR FOR
        SELECT DISTINCT account_period FROM biz_ledger ORDER BY account_period;

    -- 获取费用类型配置
    DECLARE cost_cursor CURSOR FOR
        SELECT cost_type_code, cost_type_name, default_algorithm, default_factor
        FROM cost_type_config WHERE status = 'ACTIVE';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 清空现有分摊数据
    DELETE FROM cost_allocation_result;

    -- 遍历每个账期
    OPEN period_cursor;
    period_loop: LOOP
        FETCH period_cursor INTO v_period;
        IF done THEN
            LEAVE period_loop;
        END IF;

        -- 遍历每种费用类型
        SET done = FALSE;
        OPEN cost_cursor;
        cost_loop: LOOP
            FETCH cost_cursor INTO v_cost_code, v_cost_name, v_algorithm, v_factor;
            IF done THEN
                LEAVE cost_loop;
            END IF;

            -- 计算该费用类型的总金额（基于账户级运营成本的一定比例）
            SELECT COALESCE(SUM(op_cost), 0) * (0.2 + RAND() * 0.3)
            INTO v_total_amount
            FROM biz_ledger
            WHERE account_period = v_period
            AND product_type = IF(v_cost_code IN ('DATA_FEE', 'COLLECTION_FEE', 'SYSTEM_FEE'), 'ASSET', 'LIABILITY');

            IF v_total_amount > 0 THEN
                -- 选择10-20个账户进行分摊
                SET v_account_count = 10 + FLOOR(RAND() * 11);
                SET v_avg_amount = v_total_amount / v_account_count;
                SET v_batch_no = CONCAT('BATCH_', REPLACE(v_period, '-', ''), '_', v_cost_code);
                SET v_idx = 0;

                -- 为每个账户生成分摊结果
                BEGIN
                    DECLARE account_done INT DEFAULT FALSE;
                    DECLARE account_cursor CURSOR FOR
                        SELECT biz_id, biz_amount
                        FROM biz_ledger
                        WHERE account_period = v_period
                        AND product_type = IF(v_cost_code IN ('DATA_FEE', 'COLLECTION_FEE', 'SYSTEM_FEE'), 'ASSET', 'LIABILITY')
                        ORDER BY RAND()
                        LIMIT v_account_count;
                    DECLARE CONTINUE HANDLER FOR NOT FOUND SET account_done = TRUE;

                    OPEN account_cursor;
                    account_loop: LOOP
                        FETCH account_cursor INTO v_biz_id, v_biz_amount;
                        IF account_done THEN
                            LEAVE account_loop;
                        END IF;

                        -- 计算分摊比例
                        SET v_ratio = v_biz_amount / (SELECT SUM(biz_amount) FROM biz_ledger WHERE account_period = v_period AND product_type = IF(v_cost_code IN ('DATA_FEE', 'COLLECTION_FEE', 'SYSTEM_FEE'), 'ASSET', 'LIABILITY'));
                        SET v_allocated = ROUND(v_total_amount * v_ratio, 2);

                        -- 插入分摊结果
                        INSERT INTO cost_allocation_result (
                            batch_no, period, cost_code, cost_name, original_amount,
                            target_type, target_code, target_name,
                            allocated_amount, allocation_method, allocation_factor,
                            factor_value, factor_ratio, status, created_at
                        ) VALUES (
                            v_batch_no, v_period, v_cost_code, v_cost_name, v_total_amount,
                            'ACCOUNT', v_biz_id, CONCAT('账户', v_biz_id),
                            v_allocated, v_algorithm, v_factor,
                            v_biz_amount, v_ratio, 'CALCULATED', NOW()
                        );

                        SET v_idx = v_idx + 1;
                    END LOOP;
                    CLOSE account_cursor;
                END;
            END IF;

            -- 重置done标志
            SET done = FALSE;
        END LOOP;
        CLOSE cost_cursor;

        -- 重置done标志
        SET done = FALSE;
    END LOOP;
    CLOSE period_cursor;
END //

DELIMITER ;

-- 执行存储过程
CALL generate_allocation_data();

-- 删除存储过程
DROP PROCEDURE IF EXISTS generate_allocation_data;
