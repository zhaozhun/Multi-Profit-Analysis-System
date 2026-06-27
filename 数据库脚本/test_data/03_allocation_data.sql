-- =====================================================
-- 费用分摊测试数据
-- 包含: 费用记录、分摊批次、分摊结果
-- =====================================================

-- ----------------------------
-- 1. 费用记录 (2025年1-12月)
-- ----------------------------
DELETE FROM cost_actual_record WHERE period LIKE '2025%';

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS generate_cost_records()
BEGIN
    DECLARE v_month INT DEFAULT 1;
    DECLARE v_period VARCHAR(7);

    WHILE v_month <= 12 DO
        SET v_period = CONCAT('2025-', LPAD(v_month, 2, '0'));

        INSERT INTO cost_actual_record (period, cost_code, cost_name, cost_type, amount, dept_code, occurrence_date, status) VALUES
        -- 房租
        (v_period, 'RENT', '房租费用', 'RENT', 500000 + v_month * 10000, 'ADMIN', CONCAT(v_period, '-01'), 'CONFIRMED'),
        -- 水电
        (v_period, 'UTILITIES', '水电费用', 'UTILITIES', 80000 + v_month * 2000, 'ADMIN', CONCAT(v_period, '-01'), 'CONFIRMED'),
        -- IT费用
        (v_period, 'IT', 'IT运维费用', 'IT', 300000 + v_month * 15000, 'IT', CONCAT(v_period, '-01'), 'CONFIRMED'),
        -- 工资
        (v_period, 'SALARY', '员工工资', 'SALARY', 2000000 + v_month * 50000, 'HR', CONCAT(v_period, '-05'), 'CONFIRMED'),
        -- 营销费用
        (v_period, 'MARKETING', '营销推广费用', 'MARKETING', 150000 + v_month * 8000, 'SALES', CONCAT(v_period, '-10'), 'CONFIRMED'),
        -- 催收费
        (v_period, 'COLLECTION', '催收费用', 'COLLECTION', 60000 + v_month * 3000, 'RISK', CONCAT(v_period, '-15'), 'CONFIRMED'),
        -- 管理费用
        (v_period, 'MANAGEMENT', '行政管理费用', 'MANAGEMENT', 120000 + v_month * 5000, 'ADMIN', CONCAT(v_period, '-01'), 'CONFIRMED'),
        -- 培训费用
        (v_period, 'TRAINING', '员工培训费用', 'TRAINING', 40000 + v_month * 2000, 'HR', CONCAT(v_period, '-20'), 'CONFIRMED'),
        -- 数据使用费
        (v_period, 'DATA_FEE', '数据使用费', 'DATA_FEE', 90000 + v_month * 4000, 'IT', CONCAT(v_period, '-01'), 'CONFIRMED'),
        -- 福利费
        (v_period, 'WELFARE', '员工福利费', 'WELFARE', 100000 + v_month * 5000, 'HR', CONCAT(v_period, '-15'), 'CONFIRMED'),
        -- 保险费
        (v_period, 'INSURANCE', '保险费用', 'INSURANCE', 80000 + v_month * 3000, 'FINANCE', CONCAT(v_period, '-01'), 'CONFIRMED'),
        -- 物业费
        (v_period, 'PROPERTY', '物业管理费', 'PROPERTY', 60000 + v_month * 2000, 'ADMIN', CONCAT(v_period, '-01'), 'CONFIRMED');

        SET v_month = v_month + 1;
    END WHILE;
END //

DELIMITER ;

CALL generate_cost_records();
DROP PROCEDURE IF EXISTS generate_cost_records;

-- ----------------------------
-- 2. 分摊批次记录
-- ----------------------------
DELETE FROM allocation_batch WHERE period LIKE '2025%';

INSERT INTO allocation_batch (batch_no, period, cost_type, total_amount, allocated_amount, record_count, status, start_time, end_time, trigger_type, triggered_by) VALUES
('BATCH_2025_01_RENT', '2025-01', 'RENT', 510000, 510000, 7, 'COMPLETED', '2025-02-01 10:00:00', '2025-02-01 10:02:00', 'MANUAL', 'admin'),
('BATCH_2025_01_IT', '2025-01', 'IT', 315000, 315000, 7, 'COMPLETED', '2025-02-01 10:05:00', '2025-02-01 10:07:00', 'MANUAL', 'admin'),
('BATCH_2025_01_MARKETING', '2025-01', 'MARKETING', 158000, 158000, 6, 'COMPLETED', '2025-02-01 10:10:00', '2025-02-01 10:12:00', 'MANUAL', 'admin'),
('BATCH_2025_01_SALARY', '2025-01', 'SALARY', 2050000, 2050000, 7, 'COMPLETED', '2025-02-01 10:15:00', '2025-02-01 10:18:00', 'MANUAL', 'admin'),
('BATCH_2025_02_RENT', '2025-02', 'RENT', 520000, 520000, 7, 'COMPLETED', '2025-03-01 10:00:00', '2025-03-01 10:02:00', 'AUTO', 'system'),
('BATCH_2025_02_IT', '2025-02', 'IT', 330000, 330000, 7, 'COMPLETED', '2025-03-01 10:05:00', '2025-03-01 10:07:00', 'AUTO', 'system'),
('BATCH_2025_03_RENT', '2025-03', 'RENT', 530000, 530000, 7, 'COMPLETED', '2025-04-01 10:00:00', '2025-04-01 10:02:00', 'AUTO', 'system'),
('BATCH_2025_03_MARKETING', '2025-03', 'MARKETING', 174000, 174000, 6, 'COMPLETED', '2025-04-01 10:10:00', '2025-04-01 10:12:00', 'AUTO', 'system');

-- ----------------------------
-- 3. 分摊结果明细 (cost_allocation_result)
-- ----------------------------
DELETE FROM cost_allocation_result WHERE period LIKE '2025%';

INSERT INTO cost_allocation_result (batch_no, period, cost_code, cost_name, cost_category, original_amount, target_type, target_code, target_name, dept_code, org_code, allocated_amount, allocation_method, allocation_factor, factor_value, factor_ratio, status) VALUES
-- 2025-01 房租分摊到部门
('BATCH_2025_01_RENT', '2025-01', 'RENT', '房租费用', 'FIXED', 510000, 'DEPT', 'SALES', '销售部', 'SALES', NULL, 180000, 'PROPORTION', 'AREA', 450, 0.3529, 'CALCULATED'),
('BATCH_2025_01_RENT', '2025-01', 'RENT', '房租费用', 'FIXED', 510000, 'DEPT', 'MARKETING', '市场部', 'MARKETING', NULL, 95000, 'PROPORTION', 'AREA', 237, 0.1863, 'CALCULATED'),
('BATCH_2025_01_RENT', '2025-01', 'RENT', '房租费用', 'FIXED', 510000, 'DEPT', 'RISK', '风控部', 'RISK', NULL, 75000, 'PROPORTION', 'AREA', 188, 0.1471, 'CALCULATED'),
('BATCH_2025_01_RENT', '2025-01', 'RENT', '房租费用', 'FIXED', 510000, 'DEPT', 'FINANCE', '财务部', 'FINANCE', NULL, 60000, 'PROPORTION', 'AREA', 150, 0.1176, 'CALCULATED'),
('BATCH_2025_01_RENT', '2025-01', 'RENT', '房租费用', 'FIXED', 510000, 'DEPT', 'IT', '科技部', 'IT', NULL, 50000, 'PROPORTION', 'AREA', 125, 0.0980, 'CALCULATED'),
('BATCH_2025_01_RENT', '2025-01', 'RENT', '房租费用', 'FIXED', 510000, 'DEPT', 'HR', '人力部', 'HR', NULL, 30000, 'PROPORTION', 'AREA', 75, 0.0588, 'CALCULATED'),
('BATCH_2025_01_RENT', '2025-01', 'RENT', '房租费用', 'FIXED', 510000, 'DEPT', 'ADMIN', '行政部', 'ADMIN', NULL, 20000, 'PROPORTION', 'AREA', 50, 0.0392, 'CALCULATED'),

-- 2025-01 IT费用分摊到部门
('BATCH_2025_01_IT', '2025-01', 'IT', 'IT运维费用', 'VARIABLE', 315000, 'DEPT', 'SALES', '销售部', 'SALES', NULL, 95000, 'PROPORTION', 'HEADCOUNT', 45, 0.3016, 'CALCULATED'),
('BATCH_2025_01_IT', '2025-01', 'IT', 'IT运维费用', 'VARIABLE', 315000, 'DEPT', 'MARKETING', '市场部', 'MARKETING', NULL, 50000, 'PROPORTION', 'HEADCOUNT', 24, 0.1587, 'CALCULATED'),
('BATCH_2025_01_IT', '2025-01', 'IT', 'IT运维费用', 'VARIABLE', 315000, 'DEPT', 'RISK', '风控部', 'RISK', NULL, 40000, 'PROPORTION', 'HEADCOUNT', 18, 0.1143, 'CALCULATED'),
('BATCH_2025_01_IT', '2025-01', 'IT', 'IT运维费用', 'VARIABLE', 315000, 'DEPT', 'FINANCE', '财务部', 'FINANCE', NULL, 35000, 'PROPORTION', 'HEADCOUNT', 15, 0.0952, 'CALCULATED'),
('BATCH_2025_01_IT', '2025-01', 'IT', 'IT运维费用', 'VARIABLE', 315000, 'DEPT', 'IT', '科技部', 'IT', NULL, 60000, 'PROPORTION', 'HEADCOUNT', 30, 0.1905, 'CALCULATED'),
('BATCH_2025_01_IT', '2025-01', 'IT', 'IT运维费用', 'VARIABLE', 315000, 'DEPT', 'HR', '人力部', 'HR', NULL, 20000, 'PROPORTION', 'HEADCOUNT', 10, 0.0635, 'CALCULATED'),
('BATCH_2025_01_IT', '2025-01', 'IT', 'IT运维费用', 'VARIABLE', 315000, 'DEPT', 'ADMIN', '行政部', 'ADMIN', NULL, 15000, 'PROPORTION', 'HEADCOUNT', 8, 0.0508, 'CALCULATED'),

-- 2025-01 营销费用分摊到产品
('BATCH_2025_01_MARKETING', '2025-01', 'MARKETING', '营销推广费用', 'VARIABLE', 158000, 'PRODUCT', 'LOAN_PNL', '个人经营贷', NULL, NULL, 35000, 'WEIGHTED', 'VOLUME', 165, 0.2215, 'CALCULATED'),
('BATCH_2025_01_MARKETING', '2025-01', 'MARKETING', '营销推广费用', 'VARIABLE', 158000, 'PRODUCT', 'LOAN_PCL', '个人消费贷', NULL, NULL, 42000, 'WEIGHTED', 'VOLUME', 550, 0.2658, 'CALCULATED'),
('BATCH_2025_01_MARKETING', '2025-01', 'MARKETING', '营销推广费用', 'VARIABLE', 158000, 'PRODUCT', 'LOAN_SME', '小微普惠贷', NULL, NULL, 28000, 'WEIGHTED', 'VOLUME', 385, 0.1772, 'CALCULATED'),
('BATCH_2025_01_MARKETING', '2025-01', 'MARKETING', '营销推广费用', 'VARIABLE', 158000, 'PRODUCT', 'DEPOSIT_SA', '活期存款', NULL, NULL, 18000, 'WEIGHTED', 'VOLUME', 0, 0.1139, 'CALCULATED'),
('BATCH_2025_01_MARKETING', '2025-01', 'MARKETING', '营销推广费用', 'VARIABLE', 158000, 'PRODUCT', 'WEALTH_WM', '银行理财', NULL, NULL, 22000, 'WEIGHTED', 'VOLUME', 215, 0.1392, 'CALCULATED'),
('BATCH_2025_01_MARKETING', '2025-01', 'MARKETING', '营销推广费用', 'VARIABLE', 158000, 'PRODUCT', 'FEE_SETTLE', '结算手续费', NULL, NULL, 13000, 'WEIGHTED', 'VOLUME', 880, 0.0823, 'CALCULATED'),

-- 2025-01 工资分摊到部门
('BATCH_2025_01_SALARY', '2025-01', 'SALARY', '员工工资', 'VARIABLE', 2050000, 'DEPT', 'SALES', '销售部', 'SALES', NULL, 620000, 'DIRECT', 'HEADCOUNT', 45, 0.3024, 'CALCULATED'),
('BATCH_2025_01_SALARY', '2025-01', 'SALARY', '员工工资', 'VARIABLE', 2050000, 'DEPT', 'MARKETING', '市场部', 'MARKETING', NULL, 310000, 'DIRECT', 'HEADCOUNT', 24, 0.1512, 'CALCULATED'),
('BATCH_2025_01_SALARY', '2025-01', 'SALARY', '员工工资', 'VARIABLE', 2050000, 'DEPT', 'RISK', '风控部', 'RISK', NULL, 245000, 'DIRECT', 'HEADCOUNT', 18, 0.1122, 'CALCULATED'),
('BATCH_2025_01_SALARY', '2025-01', 'SALARY', '员工工资', 'VARIABLE', 2050000, 'DEPT', 'FINANCE', '财务部', 'FINANCE', NULL, 205000, 'DIRECT', 'HEADCOUNT', 15, 0.0976, 'CALCULATED'),
('BATCH_2025_01_SALARY', '2025-01', 'SALARY', '员工工资', 'VARIABLE', 2050000, 'DEPT', 'IT', '科技部', 'IT', NULL, 385000, 'DIRECT', 'HEADCOUNT', 30, 0.1878, 'CALCULATED'),
('BATCH_2025_01_SALARY', '2025-01', 'SALARY', '员工工资', 'VARIABLE', 2050000, 'DEPT', 'HR', '人力部', 'HR', NULL, 135000, 'DIRECT', 'HEADCOUNT', 10, 0.0659, 'CALCULATED'),
('BATCH_2025_01_SALARY', '2025-01', 'SALARY', '员工工资', 'VARIABLE', 2050000, 'DEPT', 'ADMIN', '行政部', 'ADMIN', NULL, 150000, 'DIRECT', 'HEADCOUNT', 8, 0.0732, 'CALCULATED');

SELECT '费用分摊测试数据插入完成' AS result;
