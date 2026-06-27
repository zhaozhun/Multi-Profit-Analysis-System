-- =====================================================
-- 费用分摊测试数据
-- 包含: 费用类型、分摊因子、分摊规则、费用记录、分摊结果
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 费用类型 (已有数据，补充更多)
-- ----------------------------
-- 假设已有数据，这里补充一些示例

-- ----------------------------
-- 2. 分摊因子配置
-- ----------------------------
TRUNCATE TABLE allocation_factor;
INSERT INTO allocation_factor (factor_code, factor_name, factor_type, data_source, calc_formula, description, applicable_cost_types, unit, precision_val, status) VALUES
('HEADCOUNT', '人员数量', 'HEADCOUNT', 'hr_employee.count', 'dept_count / total_count', '按部门人数占比分摊', '["SALARY","IT","ADMIN"]', '人', 4, 'ACTIVE'),
('REVENUE', '收入', 'REVENUE', 'biz_ledger.revenue', 'SUM(revenue) / TOTAL(revenue)', '按收入金额占比分摊', '["RENT","SALARY","IT","MARKETING","ADMIN"]', '元', 4, 'ACTIVE'),
('VOLUME', '业务量', 'VOLUME', 'biz_ledger.biz_amount', 'SUM(biz_amount) / TOTAL(biz_amount)', '按业务交易量占比分摊', '["MARKETING","ADMIN"]', '笔', 4, 'ACTIVE'),
('AREA', '办公面积', 'AREA', 'office_area.area', 'dept_area / total_area', '按办公面积占比分摊', '["RENT"]', '平方米', 4, 'ACTIVE'),
('ASSET', '资产规模', 'ASSET', 'asset_ledger.balance', 'SUM(balance) / TOTAL(balance)', '按资产规模占比分摊', '["RENT","SALARY","IT"]', '元', 4, 'ACTIVE'),
('WORK_HOURS', '工时', 'WORK_HOURS', 'employee_work_hours.total_hours', 'employee_hours / total_hours', '按员工实际工时占比分摊', '["OPERATION","SALARY","WELFARE"]', '小时', 4, 'ACTIVE'),
('SALARY', '薪资', 'SALARY', 'employee_master.salary', 'employee_salary / total_salary', '按员工薪资占比分摊', '["WELFARE","INSURANCE","TRAINING"]', '元', 4, 'ACTIVE'),
('EMPLOYEE_COUNT', '员工人数', 'HEADCOUNT', 'employee_master.count', 'dept_employee_count / total_employee_count', '按部门员工人数占比分摊', '["RENT","IT","ADMIN","WELFARE"]', '人', 4, 'ACTIVE'),
('WORKSTATION_AREA', '工位面积', 'AREA', 'employee_master.workstation_area', 'employee_area / total_area', '按员工工位面积占比分摊', '["RENT","PROPERTY","UTILITIES"]', '平方米', 4, 'ACTIVE'),
('PRODUCT_REVENUE', '产品收入', 'REVENUE', 'biz_ledger.revenue', 'product_revenue / total_revenue', '按产品收入占比计算分润', '["COMMISSION","CHANNEL_FEE"]', '元', 4, 'ACTIVE'),
('PRODUCT_PROFIT', '产品利润', 'PROFIT', 'biz_ledger.net_profit', 'product_profit / total_profit', '按产品利润占比计算分润', '["COMMISSION","BONUS"]', '元', 4, 'ACTIVE'),
('PRODUCT_BIZ_AMOUNT', '产品业务量', 'VOLUME', 'biz_ledger.biz_amount', 'product_amount / total_amount', '按产品业务量占比计算分润', '["COMMISSION","MARKETING"]', '笔', 4, 'ACTIVE');

-- ----------------------------
-- 3. 分摊规则配置
-- ----------------------------
TRUNCATE TABLE allocation_rule;
INSERT INTO allocation_rule (rule_name, cost_type, factor_type, algorithm_type, target_dim_type, description, priority, status) VALUES
('房租按面积分摊', 'RENT', 'AREA', 'PROPORTION', 'DEPT', '按各部门办公面积占比分摊房租', 1, 'ACTIVE'),
('水电按面积分摊', 'UTILITIES', 'AREA', 'PROPORTION', 'DEPT', '按各部门办公面积占比分摊水电费', 2, 'ACTIVE'),
('IT费用按人头分摊', 'IT', 'HEADCOUNT', 'PROPORTION', 'DEPT', '按各部门人数占比分摊IT费用', 3, 'ACTIVE'),
('工资按人头直接归属', 'SALARY', 'HEADCOUNT', 'DIRECT', 'DEPT', '工资直接归属到各部门', 4, 'ACTIVE'),
('营销费用按业务量分摊', 'MARKETING', 'VOLUME', 'WEIGHTED', 'PRODUCT', '按各产品业务量占比分摊营销费用', 5, 'ACTIVE'),
('催收费按业务量分摊', 'COLLECTION', 'VOLUME', 'WEIGHTED', 'PRODUCT', '按各产品催收业务量占比分摊催收费', 6, 'ACTIVE'),
('产品分润按收入比例', 'COMMISSION', 'PRODUCT_REVENUE', 'PROPORTION', 'PRODUCT', '按产品收入占比计算分润', 7, 'ACTIVE'),
('管理费用按收入分摊', 'MANAGEMENT', 'REVENUE', 'PROPORTION', 'ORG', '按各机构收入占比分摊管理费用', 8, 'ACTIVE'),
('培训费用按人数分摊', 'TRAINING', 'EMPLOYEE_COUNT', 'PROPORTION', 'DEPT', '按各部门人数占比分摊培训费用', 9, 'ACTIVE'),
('数据使用费按业务量', 'DATA_FEE', 'PRODUCT_BIZ_AMOUNT', 'WEIGHTED', 'PRODUCT', '按产品业务量占比分摊数据使用费', 10, 'ACTIVE');

-- ----------------------------
-- 4. 产品分润配置
-- ----------------------------
TRUNCATE TABLE product_commission_config;
INSERT INTO product_commission_config (product_code, product_name, is_commission, commission_type, commission_rate, base_field, description, status) VALUES
('LOAN_PNL', '个人经营贷', 1, 'INTEREST_REVENUE', 0.05, 'interest_income', '按利息收入5%分润', 'ACTIVE'),
('LOAN_PCL', '个人消费贷', 1, 'INTEREST_REVENUE', 0.03, 'interest_income', '按利息收入3%分润', 'ACTIVE'),
('LOAN_MORTGAGE', '住房按揭贷', 1, 'LOAN_BALANCE', 0.002, 'loan_balance', '按贷款余额0.2%分润', 'ACTIVE'),
('LOAN_CORP', '对公流动资金贷', 1, 'INTEREST_REVENUE', 0.04, 'interest_income', '按利息收入4%分润', 'ACTIVE'),
('LOAN_SME', '小微普惠贷', 1, 'INTEREST_REVENUE', 0.06, 'interest_income', '按利息收入6%分润', 'ACTIVE'),
('LOAN_AUTO', '汽车消费贷', 0, NULL, NULL, NULL, '不分润', 'ACTIVE'),
('DEPOSIT_SA', '活期存款', 0, NULL, NULL, NULL, '不分润', 'ACTIVE'),
('DEPOSIT_TD', '定期存款', 0, NULL, NULL, NULL, '不分润', 'ACTIVE'),
('WEALTH_WM', '银行理财', 0, NULL, NULL, NULL, '不分润', 'ACTIVE'),
('WEALTH_FUND', '基金代销', 0, NULL, NULL, NULL, '不分润', 'ACTIVE');

-- ----------------------------
-- 5. 费用记录 (2025年1-12月)
-- ----------------------------
TRUNCATE TABLE cost_actual_record;

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
-- 6. 分摊执行结果 (模拟已执行的分摊)
-- ----------------------------
TRUNCATE TABLE allocation_batch;
INSERT INTO allocation_batch (batch_no, period, rule_id, rule_name, cost_type, total_amount, target_dim_type, exec_time, status) VALUES
('BATCH_2025_01_RENT', '2025-01', 1, '房租按面积分摊', 'RENT', 510000, 'DEPT', '2025-02-01 10:00:00', 'COMPLETED'),
('BATCH_2025_01_IT', '2025-01', 3, 'IT费用按人头分摊', 'IT', 315000, 'DEPT', '2025-02-01 10:05:00', 'COMPLETED'),
('BATCH_2025_01_MARKETING', '2025-01', 5, '营销费用按业务量分摊', 'MARKETING', 158000, 'PRODUCT', '2025-02-01 10:10:00', 'COMPLETED'),
('BATCH_2025_01_COMMISSION', '2025-01', 7, '产品分润按收入比例', 'COMMISSION', 0, 'PRODUCT', '2025-02-01 10:15:00', 'COMPLETED');

TRUNCATE TABLE allocation_result;
INSERT INTO allocation_result (batch_id, target_dim_code, target_dim_name, cost_type, allocated_amount, factor_value, factor_ratio) VALUES
-- 房租分摊到部门
(1, 'SALES', '销售部', 'RENT', 180000, 450, 0.3529),
(1, 'MARKETING', '市场部', 'RENT', 95000, 237, 0.1863),
(1, 'RISK', '风控部', 'RENT', 75000, 188, 0.1471),
(1, 'FINANCE', '财务部', 'RENT', 60000, 150, 0.1176),
(1, 'IT', '科技部', 'RENT', 50000, 125, 0.0980),
(1, 'HR', '人力部', 'RENT', 30000, 75, 0.0588),
(1, 'ADMIN', '行政部', 'RENT', 20000, 50, 0.0392),
-- IT费用分摊到部门
(2, 'SALES', '销售部', 'IT', 95000, 45, 0.3016),
(2, 'MARKETING', '市场部', 'IT', 50000, 24, 0.1587),
(2, 'RISK', '风控部', 'IT', 40000, 18, 0.1143),
(2, 'FINANCE', '财务部', 'IT', 35000, 15, 0.0952),
(2, 'IT', '科技部', 'IT', 60000, 30, 0.1905),
(2, 'HR', '人力部', 'IT', 20000, 10, 0.0635),
(2, 'ADMIN', '行政部', 'IT', 15000, 8, 0.0508),
-- 营销费用分摊到产品
(3, 'LOAN_PNL', '个人经营贷', 'MARKETING', 35000, 165, 0.2215),
(3, 'LOAN_PCL', '个人消费贷', 'MARKETING', 42000, 550, 0.2658),
(3, 'LOAN_SME', '小微普惠贷', 'MARKETING', 28000, 385, 0.1772),
(3, 'DEPOSIT_SA', '活期存款', 'MARKETING', 18000, 0, 0.1139),
(3, 'WEALTH_WM', '银行理财', 'MARKETING', 22000, 215, 0.1392),
(3, 'FEE_SETTLE', '结算手续费', 'MARKETING', 13000, 880, 0.0823);

SET FOREIGN_KEY_CHECKS = 1;

SELECT '费用分摊测试数据插入完成' AS result;
