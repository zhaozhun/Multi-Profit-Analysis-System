-- ============================================
-- 指标数据测试数据插入脚本
-- 创建时间：2026-06-27
-- 说明：插入指标汇总数据和费用分摊结果数据
-- ============================================

USE multi_profit;

-- ============================================
-- 1. 插入原子指标配置（如果不存在）
-- ============================================

-- 资产原子指标
INSERT IGNORE INTO atomic_indicator (code, name, business_line, source_table, source_field, filter_condition, unit, precision_val, description, sort_order) VALUES
('INTEREST_INCOME', '对客利息收入', 'ASSET', 'biz_ledger', 'interest_income', 'product_type=LOAN', '万元', 2, '资产对客利息收入', 1),
('FTP_COST', 'FTP成本', 'ASSET', 'biz_ledger', 'ftp_cost', 'product_type=LOAN', '万元', 2, '资产FTP资金成本', 2),
('RISK_COST', '风险成本', 'ASSET', 'biz_ledger', 'risk_cost', 'product_type=LOAN', '万元', 2, '资产风险计提成本', 3),
('OP_COST', '运营成本', 'ASSET', 'biz_ledger', 'op_cost', 'product_type=LOAN', '万元', 2, '资产运营成本', 4);

-- 负债原子指标
INSERT IGNORE INTO atomic_indicator (code, name, business_line, source_table, source_field, filter_condition, unit, precision_val, description, sort_order) VALUES
('FTP_INCOME', 'FTP收入', 'LIABILITY', 'biz_ledger', 'interest_income', 'product_type=DEPOSIT', '万元', 2, '负债FTP资金收入', 1),
('INTEREST_EXPENSE', '对客利息支出', 'LIABILITY', 'biz_ledger', 'interest_expense', 'product_type=DEPOSIT', '万元', 2, '负债对客利息支出', 2),
('LIABILITY_OP_COST', '运营成本', 'LIABILITY', 'biz_ledger', 'op_cost', 'product_type=DEPOSIT', '万元', 2, '负债运营成本', 3);

-- ============================================
-- 2. 插入指标汇总数据 - 资产
-- ============================================

-- 2026年1月资产指标汇总
-- 运营成本 = 数据使用费(954.20) + 催收费(530.00) + 系统使用费(242.70) = 1726.90
INSERT INTO indicator_summary (period, indicator_code, indicator_type, business_line, calc_value, calc_time, status) VALUES
('2026-01', 'INTEREST_INCOME', 'ATOMIC', 'ASSET', 12568.50, NOW(), 1),
('2026-01', 'FTP_COST', 'ATOMIC', 'ASSET', 4832.20, NOW(), 1),
('2026-01', 'RISK_COST', 'ATOMIC', 'ASSET', 2156.80, NOW(), 1),
('2026-01', 'OP_COST', 'ATOMIC', 'ASSET', 1726.90, NOW(), 1);

-- 2025年12月资产指标汇总（运营成本略低于1月）
INSERT INTO indicator_summary (period, indicator_code, indicator_type, business_line, calc_value, calc_time, status) VALUES
('2025-12', 'INTEREST_INCOME', 'ATOMIC', 'ASSET', 11892.30, NOW(), 1),
('2025-12', 'FTP_COST', 'ATOMIC', 'ASSET', 4658.90, NOW(), 1),
('2025-12', 'RISK_COST', 'ATOMIC', 'ASSET', 2089.40, NOW(), 1),
('2025-12', 'OP_COST', 'ATOMIC', 'ASSET', 1652.80, NOW(), 1);

-- 2025年11月资产指标汇总（运营成本略低于12月）
INSERT INTO indicator_summary (period, indicator_code, indicator_type, business_line, calc_value, calc_time, status) VALUES
('2025-11', 'INTEREST_INCOME', 'ATOMIC', 'ASSET', 11245.80, NOW(), 1),
('2025-11', 'FTP_COST', 'ATOMIC', 'ASSET', 4425.60, NOW(), 1),
('2025-11', 'RISK_COST', 'ATOMIC', 'ASSET', 1987.30, NOW(), 1),
('2025-11', 'OP_COST', 'ATOMIC', 'ASSET', 1578.50, NOW(), 1);

-- ============================================
-- 3. 插入指标汇总数据 - 负债
-- ============================================

-- 2026年1月负债指标汇总
-- 运营成本 = 柜面服务费(791.60) + 账户管理费(395.60) = 1187.20
INSERT INTO indicator_summary (period, indicator_code, indicator_type, business_line, calc_value, calc_time, status) VALUES
('2026-01', 'FTP_INCOME', 'ATOMIC', 'LIABILITY', 8965.40, NOW(), 1),
('2026-01', 'INTEREST_EXPENSE', 'ATOMIC', 'LIABILITY', 3256.80, NOW(), 1),
('2026-01', 'LIABILITY_OP_COST', 'ATOMIC', 'LIABILITY', 1187.20, NOW(), 1);

-- 2025年12月负债指标汇总（运营成本略低于1月）
INSERT INTO indicator_summary (period, indicator_code, indicator_type, business_line, calc_value, calc_time, status) VALUES
('2025-12', 'FTP_INCOME', 'ATOMIC', 'LIABILITY', 8542.30, NOW(), 1),
('2025-12', 'INTEREST_EXPENSE', 'ATOMIC', 'LIABILITY', 3125.40, NOW(), 1),
('2025-12', 'LIABILITY_OP_COST', 'ATOMIC', 'LIABILITY', 1125.80, NOW(), 1);

-- 2025年11月负债指标汇总（运营成本略低于12月）
INSERT INTO indicator_summary (period, indicator_code, indicator_type, business_line, calc_value, calc_time, status) VALUES
('2025-11', 'FTP_INCOME', 'ATOMIC', 'LIABILITY', 8125.60, NOW(), 1),
('2025-11', 'INTEREST_EXPENSE', 'ATOMIC', 'LIABILITY', 2987.50, NOW(), 1),
('2025-11', 'LIABILITY_OP_COST', 'ATOMIC', 'LIABILITY', 1068.40, NOW(), 1);

-- ============================================
-- 4. 插入费用分摊结果数据
-- ============================================

-- 资产运营成本分摊 - 数据使用费
INSERT INTO cost_allocation_result (batch_no, period, cost_code, cost_name, cost_category, original_amount, target_type, target_code, target_name, dept_code, org_code, allocated_amount, allocation_method, allocation_factor, factor_value, factor_ratio, status) VALUES
('BATCH001', '2026-01', 'DATA_FEE', '数据使用费', 'OPERATING', 954.20, 'ACCOUNT', 'ACC001', '账户1', 'DEPT001', 'ORG001', 125.50, 'BY_DEPT_RATIO', 'WORK_HOUR', 0.15, 0.1316, 'CALCULATED'),
('BATCH001', '2026-01', 'DATA_FEE', '数据使用费', 'OPERATING', 954.20, 'ACCOUNT', 'ACC002', '账户2', 'DEPT001', 'ORG001', 98.30, 'BY_DEPT_RATIO', 'WORK_HOUR', 0.12, 0.1030, 'CALCULATED'),
('BATCH001', '2026-01', 'DATA_FEE', '数据使用费', 'OPERATING', 954.20, 'ACCOUNT', 'ACC003', '账户3', 'DEPT001', 'ORG001', 87.60, 'BY_DEPT_RATIO', 'WORK_HOUR', 0.10, 0.0918, 'CALCULATED'),
('BATCH001', '2026-01', 'DATA_FEE', '数据使用费', 'OPERATING', 954.20, 'ACCOUNT', 'ACC004', '账户4', 'DEPT002', 'ORG001', 156.80, 'BY_DEPT_RATIO', 'WORK_HOUR', 0.18, 0.1643, 'CALCULATED'),
('BATCH001', '2026-01', 'DATA_FEE', '数据使用费', 'OPERATING', 954.20, 'ACCOUNT', 'ACC005', '账户5', 'DEPT002', 'ORG002', 132.40, 'BY_DEPT_RATIO', 'WORK_HOUR', 0.15, 0.1388, 'CALCULATED'),
('BATCH001', '2026-01', 'DATA_FEE', '数据使用费', 'OPERATING', 954.20, 'ACCOUNT', 'ACC006', '账户6', 'DEPT003', 'ORG002', 145.20, 'BY_DEPT_RATIO', 'WORK_HOUR', 0.17, 0.1522, 'CALCULATED'),
('BATCH001', '2026-01', 'DATA_FEE', '数据使用费', 'OPERATING', 954.20, 'ACCOUNT', 'ACC007', '账户7', 'DEPT003', 'ORG003', 112.80, 'BY_DEPT_RATIO', 'WORK_HOUR', 0.13, 0.1182, 'CALCULATED'),
('BATCH001', '2026-01', 'DATA_FEE', '数据使用费', 'OPERATING', 954.20, 'ACCOUNT', 'ACC008', '账户8', 'DEPT003', 'ORG003', 95.60, 'BY_DEPT_RATIO', 'WORK_HOUR', 0.11, 0.1002, 'CALCULATED');

-- 资产运营成本分摊 - 催收费
INSERT INTO cost_allocation_result (batch_no, period, cost_code, cost_name, cost_category, original_amount, target_type, target_code, target_name, dept_code, org_code, allocated_amount, allocation_method, allocation_factor, factor_value, factor_ratio, status) VALUES
('BATCH002', '2026-01', 'COLLECTION_FEE', '催收费', 'OPERATING', 530.00, 'ACCOUNT', 'ACC001', '账户1', 'DEPT001', 'ORG001', 85.40, 'BY_ORG_RATIO', 'BUSINESS_AMOUNT', 0.16, 0.1611, 'CALCULATED'),
('BATCH002', '2026-01', 'COLLECTION_FEE', '催收费', 'OPERATING', 530.00, 'ACCOUNT', 'ACC002', '账户2', 'DEPT001', 'ORG001', 67.80, 'BY_ORG_RATIO', 'BUSINESS_AMOUNT', 0.13, 0.1279, 'CALCULATED'),
('BATCH002', '2026-01', 'COLLECTION_FEE', '催收费', 'OPERATING', 530.00, 'ACCOUNT', 'ACC003', '账户3', 'DEPT002', 'ORG002', 112.50, 'BY_ORG_RATIO', 'BUSINESS_AMOUNT', 0.21, 0.2123, 'CALCULATED'),
('BATCH002', '2026-01', 'COLLECTION_FEE', '催收费', 'OPERATING', 530.00, 'ACCOUNT', 'ACC004', '账户4', 'DEPT002', 'ORG002', 98.60, 'BY_ORG_RATIO', 'BUSINESS_AMOUNT', 0.19, 0.1860, 'CALCULATED'),
('BATCH002', '2026-01', 'COLLECTION_FEE', '催收费', 'OPERATING', 530.00, 'ACCOUNT', 'ACC005', '账户5', 'DEPT003', 'ORG003', 76.30, 'BY_ORG_RATIO', 'BUSINESS_AMOUNT', 0.14, 0.1440, 'CALCULATED'),
('BATCH002', '2026-01', 'COLLECTION_FEE', '催收费', 'OPERATING', 530.00, 'ACCOUNT', 'ACC006', '账户6', 'DEPT003', 'ORG003', 89.40, 'BY_ORG_RATIO', 'BUSINESS_AMOUNT', 0.17, 0.1687, 'CALCULATED');

-- 资产运营成本分摊 - 系统使用费
INSERT INTO cost_allocation_result (batch_no, period, cost_code, cost_name, cost_category, original_amount, target_type, target_code, target_name, dept_code, org_code, allocated_amount, allocation_method, allocation_factor, factor_value, factor_ratio, status) VALUES
('BATCH003', '2026-01', 'SYSTEM_FEE', '系统使用费', 'OPERATING', 242.70, 'ACCOUNT', 'ACC001', '账户1', 'DEPT001', 'ORG001', 45.60, 'BY_PRODUCT_RATIO', 'PRODUCT_AMOUNT', 0.19, 0.1879, 'CALCULATED'),
('BATCH003', '2026-01', 'SYSTEM_FEE', '系统使用费', 'OPERATING', 242.70, 'ACCOUNT', 'ACC002', '账户2', 'DEPT001', 'ORG001', 38.90, 'BY_PRODUCT_RATIO', 'PRODUCT_AMOUNT', 0.16, 0.1603, 'CALCULATED'),
('BATCH003', '2026-01', 'SYSTEM_FEE', '系统使用费', 'OPERATING', 242.70, 'ACCOUNT', 'ACC003', '账户3', 'DEPT002', 'ORG002', 52.30, 'BY_PRODUCT_RATIO', 'PRODUCT_AMOUNT', 0.22, 0.2155, 'CALCULATED'),
('BATCH003', '2026-01', 'SYSTEM_FEE', '系统使用费', 'OPERATING', 242.70, 'ACCOUNT', 'ACC004', '账户4', 'DEPT002', 'ORG002', 41.20, 'BY_PRODUCT_RATIO', 'PRODUCT_AMOUNT', 0.17, 0.1698, 'CALCULATED'),
('BATCH003', '2026-01', 'SYSTEM_FEE', '系统使用费', 'OPERATING', 242.70, 'ACCOUNT', 'ACC005', '账户5', 'DEPT003', 'ORG003', 35.80, 'BY_PRODUCT_RATIO', 'PRODUCT_AMOUNT', 0.15, 0.1475, 'CALCULATED'),
('BATCH003', '2026-01', 'SYSTEM_FEE', '系统使用费', 'OPERATING', 242.70, 'ACCOUNT', 'ACC006', '账户6', 'DEPT003', 'ORG003', 28.90, 'BY_PRODUCT_RATIO', 'PRODUCT_AMOUNT', 0.12, 0.1191, 'CALCULATED');

-- 负债运营成本分摊 - 柜面服务费
INSERT INTO cost_allocation_result (batch_no, period, cost_code, cost_name, cost_category, original_amount, target_type, target_code, target_name, dept_code, org_code, allocated_amount, allocation_method, allocation_factor, factor_value, factor_ratio, status) VALUES
('BATCH004', '2026-01', 'COUNTER_FEE', '柜面服务费', 'OPERATING', 791.60, 'ACCOUNT', 'DEP001', '存款账户1', 'DEPT001', 'ORG001', 156.80, 'BY_ORG_RATIO', 'TRANSACTION_COUNT', 0.20, 0.1981, 'CALCULATED'),
('BATCH004', '2026-01', 'COUNTER_FEE', '柜面服务费', 'OPERATING', 791.60, 'ACCOUNT', 'DEP002', '存款账户2', 'DEPT001', 'ORG001', 132.50, 'BY_ORG_RATIO', 'TRANSACTION_COUNT', 0.17, 0.1674, 'CALCULATED'),
('BATCH004', '2026-01', 'COUNTER_FEE', '柜面服务费', 'OPERATING', 791.60, 'ACCOUNT', 'DEP003', '存款账户3', 'DEPT002', 'ORG002', 189.40, 'BY_ORG_RATIO', 'TRANSACTION_COUNT', 0.24, 0.2393, 'CALCULATED'),
('BATCH004', '2026-01', 'COUNTER_FEE', '柜面服务费', 'OPERATING', 791.60, 'ACCOUNT', 'DEP004', '存款账户4', 'DEPT002', 'ORG002', 145.60, 'BY_ORG_RATIO', 'TRANSACTION_COUNT', 0.18, 0.1839, 'CALCULATED'),
('BATCH004', '2026-01', 'COUNTER_FEE', '柜面服务费', 'OPERATING', 791.60, 'ACCOUNT', 'DEP005', '存款账户5', 'DEPT003', 'ORG003', 167.30, 'BY_ORG_RATIO', 'TRANSACTION_COUNT', 0.21, 0.2113, 'CALCULATED');

-- 负债运营成本分摊 - 账户管理费
INSERT INTO cost_allocation_result (batch_no, period, cost_code, cost_name, cost_category, original_amount, target_type, target_code, target_name, dept_code, org_code, allocated_amount, allocation_method, allocation_factor, factor_value, factor_ratio, status) VALUES
('BATCH005', '2026-01', 'ACCOUNT_MGMT_FEE', '账户管理费', 'OPERATING', 395.60, 'ACCOUNT', 'DEP001', '存款账户1', 'DEPT001', 'ORG001', 78.40, 'BY_DEPT_RATIO', 'ACCOUNT_COUNT', 0.20, 0.1982, 'CALCULATED'),
('BATCH005', '2026-01', 'ACCOUNT_MGMT_FEE', '账户管理费', 'OPERATING', 395.60, 'ACCOUNT', 'DEP002', '存款账户2', 'DEPT001', 'ORG001', 65.30, 'BY_DEPT_RATIO', 'ACCOUNT_COUNT', 0.17, 0.1651, 'CALCULATED'),
('BATCH005', '2026-01', 'ACCOUNT_MGMT_FEE', '账户管理费', 'OPERATING', 395.60, 'ACCOUNT', 'DEP003', '存款账户3', 'DEPT002', 'ORG002', 92.50, 'BY_DEPT_RATIO', 'ACCOUNT_COUNT', 0.23, 0.2338, 'CALCULATED'),
('BATCH005', '2026-01', 'ACCOUNT_MGMT_FEE', '账户管理费', 'OPERATING', 395.60, 'ACCOUNT', 'DEP004', '存款账户4', 'DEPT002', 'ORG002', 73.80, 'BY_DEPT_RATIO', 'ACCOUNT_COUNT', 0.19, 0.1865, 'CALCULATED'),
('BATCH005', '2026-01', 'ACCOUNT_MGMT_FEE', '账户管理费', 'OPERATING', 395.60, 'ACCOUNT', 'DEP005', '存款账户5', 'DEPT003', 'ORG003', 85.60, 'BY_DEPT_RATIO', 'ACCOUNT_COUNT', 0.22, 0.2164, 'CALCULATED');

-- ============================================
-- 5. 插入费用类型配置数据（用于前端显示）
-- ============================================

-- 插入资产费用类型
INSERT INTO cost_type_config (cost_type_code, cost_type_name, parent_code, level, description, default_algorithm, default_factor, accounting_code, status) VALUES
('DATA_FEE', '数据使用费', 'OP_COST', 2, '数据服务和数据采购费用', 'BY_DEPT_RATIO', 'WORK_HOUR', '5001', 'ACTIVE'),
('COLLECTION_FEE', '催收费', 'OP_COST', 2, '逾期贷款催收服务费用', 'BY_ORG_RATIO', 'BUSINESS_AMOUNT', '5002', 'ACTIVE'),
('SYSTEM_FEE', '系统使用费', 'OP_COST', 2, '业务系统使用和维护费用', 'BY_PRODUCT_RATIO', 'PRODUCT_AMOUNT', '5003', 'ACTIVE');

-- 插入负债费用类型
INSERT INTO cost_type_config (cost_type_code, cost_type_name, parent_code, level, description, default_algorithm, default_factor, accounting_code, status) VALUES
('COUNTER_FEE', '柜面服务费', 'LIABILITY_OP_COST', 2, '柜面人工服务成本', 'BY_ORG_RATIO', 'TRANSACTION_COUNT', '5004', 'ACTIVE'),
('ACCOUNT_MGMT_FEE', '账户管理费', 'LIABILITY_OP_COST', 2, '账户维护和管理成本', 'BY_DEPT_RATIO', 'ACCOUNT_COUNT', '5005', 'ACTIVE');

-- ============================================
-- 6. 插入费用原始数据
-- ============================================

-- 插入资产费用原始数据
INSERT INTO cost_actual_record (period, cost_code, cost_name, cost_type, amount, dept_code, org_code, vendor, invoice_no, occurrence_date, description, status, created_by) VALUES
('2026-01', 'DATA_FEE', '数据使用费', 'OPERATING', 311.40, 'DEPT001', 'ORG001', '数据服务商A', 'INV202601001', '2026-01-15', '1月数据使用费-零售部', 'CONFIRMED', 'admin'),
('2026-01', 'DATA_FEE', '数据使用费', 'OPERATING', 289.20, 'DEPT002', 'ORG001', '数据服务商A', 'INV202601002', '2026-01-15', '1月数据使用费-公司部', 'CONFIRMED', 'admin'),
('2026-01', 'DATA_FEE', '数据使用费', 'OPERATING', 353.60, 'DEPT003', 'ORG002', '数据服务商B', 'INV202601003', '2026-01-15', '1月数据使用费-金融部', 'CONFIRMED', 'admin'),
('2026-01', 'COLLECTION_FEE', '催收费', 'OPERATING', 153.20, 'DEPT001', 'ORG001', '催收公司A', 'INV202601004', '2026-01-20', '1月催收费-总行营业部', 'CONFIRMED', 'admin'),
('2026-01', 'COLLECTION_FEE', '催收费', 'OPERATING', 211.10, 'DEPT002', 'ORG002', '催收公司A', 'INV202601005', '2026-01-20', '1月催收费-城北支行', 'CONFIRMED', 'admin'),
('2026-01', 'COLLECTION_FEE', '催收费', 'OPERATING', 165.70, 'DEPT003', 'ORG003', '催收公司B', 'INV202601006', '2026-01-20', '1月催收费-城南支行', 'CONFIRMED', 'admin'),
('2026-01', 'SYSTEM_FEE', '系统使用费', 'OPERATING', 84.50, 'DEPT001', 'ORG001', '系统供应商', 'INV202601007', '2026-01-25', '1月系统使用费-个人住房贷款', 'CONFIRMED', 'admin'),
('2026-01', 'SYSTEM_FEE', '系统使用费', 'OPERATING', 93.50, 'DEPT002', 'ORG002', '系统供应商', 'INV202601008', '2026-01-25', '1月系统使用费-个人消费贷款', 'CONFIRMED', 'admin'),
('2026-01', 'SYSTEM_FEE', '系统使用费', 'OPERATING', 64.70, 'DEPT003', 'ORG003', '系统供应商', 'INV202601009', '2026-01-25', '1月系统使用费-个人经营贷款', 'CONFIRMED', 'admin');

-- 插入负债费用原始数据
INSERT INTO cost_actual_record (period, cost_code, cost_name, cost_type, amount, dept_code, org_code, vendor, invoice_no, occurrence_date, description, status, created_by) VALUES
('2026-01', 'COUNTER_FEE', '柜面服务费', 'OPERATING', 289.30, 'DEPT001', 'ORG001', '人力外包公司', 'INV202601010', '2026-01-18', '1月柜面服务费-总行营业部', 'CONFIRMED', 'admin'),
('2026-01', 'COUNTER_FEE', '柜面服务费', 'OPERATING', 335.00, 'DEPT002', 'ORG002', '人力外包公司', 'INV202601011', '2026-01-18', '1月柜面服务费-城北支行', 'CONFIRMED', 'admin'),
('2026-01', 'COUNTER_FEE', '柜面服务费', 'OPERATING', 167.30, 'DEPT003', 'ORG003', '人力外包公司', 'INV202601012', '2026-01-18', '1月柜面服务费-城南支行', 'CONFIRMED', 'admin'),
('2026-01', 'ACCOUNT_MGMT_FEE', '账户管理费', 'OPERATING', 143.70, 'DEPT001', 'ORG001', '系统供应商', 'INV202601013', '2026-01-22', '1月账户管理费-零售部', 'CONFIRMED', 'admin'),
('2026-01', 'ACCOUNT_MGMT_FEE', '账户管理费', 'OPERATING', 166.30, 'DEPT002', 'ORG002', '系统供应商', 'INV202601014', '2026-01-22', '1月账户管理费-公司部', 'CONFIRMED', 'admin'),
('2026-01', 'ACCOUNT_MGMT_FEE', '账户管理费', 'OPERATING', 85.60, 'DEPT003', 'ORG003', '系统供应商', 'INV202601015', '2026-01-22', '1月账户管理费-金融部', 'CONFIRMED', 'admin');

-- ============================================
-- 完成
-- ============================================
SELECT '测试数据插入完成' AS result;
