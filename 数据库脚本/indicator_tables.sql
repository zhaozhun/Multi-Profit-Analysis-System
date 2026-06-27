-- ============================================
-- 指标体系数据库表结构和初始数据
-- 创建时间：2026-06-27
-- ============================================

USE multi_profit;

-- ============================================
-- 1. 原子指标表
-- ============================================
CREATE TABLE IF NOT EXISTS atomic_indicator (
    code VARCHAR(50) PRIMARY KEY COMMENT '指标编码',
    name VARCHAR(100) NOT NULL COMMENT '指标名称',
    business_line VARCHAR(20) NOT NULL COMMENT '业务条线：LOAN/DEPOSIT',
    source_table VARCHAR(50) NOT NULL COMMENT '数据来源表',
    source_field VARCHAR(50) NOT NULL COMMENT '数据来源字段',
    filter_condition TEXT COMMENT '筛选条件',
    detail_table VARCHAR(50) COMMENT '明细来源表',
    detail_dimension VARCHAR(50) COMMENT '明细维度字段',
    detail_display_fields TEXT COMMENT '明细展示字段JSON',
    detail_group_by VARCHAR(50) COMMENT '明细分组字段',
    unit VARCHAR(20) DEFAULT '万元' COMMENT '单位',
    precision_val INT DEFAULT 2 COMMENT '精度',
    description TEXT COMMENT '描述',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='原子指标表';

-- ============================================
-- 2. 派生指标表
-- ============================================
CREATE TABLE IF NOT EXISTS derived_indicator (
    code VARCHAR(50) PRIMARY KEY COMMENT '指标编码',
    name VARCHAR(100) NOT NULL COMMENT '指标名称',
    business_line VARCHAR(20) NOT NULL COMMENT '业务条线：LOAN/DEPOSIT',
    calc_formula TEXT NOT NULL COMMENT '计算公式',
    formula_vars TEXT COMMENT '公式变量JSON',
    unit VARCHAR(20) DEFAULT '万元' COMMENT '单位',
    precision_val INT DEFAULT 2 COMMENT '精度',
    description TEXT COMMENT '描述',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='派生指标表';

-- ============================================
-- 3. 统计口径表
-- ============================================
CREATE TABLE IF NOT EXISTS indicator_stat_config (
    indicator_code VARCHAR(50) COMMENT '指标编码',
    stat_type VARCHAR(20) COMMENT '统计口径：MONTHLY_DAILY_AVG/YEARLY_DAILY_AVG',
    calc_formula TEXT COMMENT '计算公式',
    description VARCHAR(200) COMMENT '描述',
    PRIMARY KEY (indicator_code, stat_type)
) COMMENT='统计口径表';

-- ============================================
-- 4. 指标预计算结果表
-- ============================================
CREATE TABLE IF NOT EXISTS indicator_pre_calc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_code VARCHAR(50) NOT NULL COMMENT '指标编码',
    indicator_type VARCHAR(20) NOT NULL COMMENT '指标类型：ATOMIC/DERIVED',
    stat_type VARCHAR(20) COMMENT '统计口径',
    calc_period VARCHAR(10) NOT NULL COMMENT '计算周期：MONTH/YEAR',
    period_value VARCHAR(20) NOT NULL COMMENT '周期值',
    calc_value DECIMAL(18,4) COMMENT '计算结果',
    calc_time DATETIME COMMENT '计算时间',
    status TINYINT DEFAULT 1 COMMENT '状态',
    INDEX idx_indicator (indicator_code),
    INDEX idx_period (calc_period, period_value)
) COMMENT='指标预计算结果表';

-- ============================================
-- 5. 插入原子指标初始数据
-- ============================================

-- 贷款原子指标
INSERT INTO atomic_indicator (code, name, business_line, source_table, source_field, filter_condition, detail_table, detail_dimension, detail_display_fields, detail_group_by, unit, precision_val, description, sort_order) VALUES
('LOAN_BALANCE', '贷款余额', 'LOAN', 'biz_ledger', 'biz_amount', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","biz_amount","product_name"]', 'stat_date', '万元', 2, '贷款本金余额', 1),
('LOAN_INTEREST', '贷款利息收入', 'LOAN', 'biz_ledger', 'interest_income', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","interest_income","product_name"]', 'stat_date', '万元', 2, '贷款对客利息收入', 2),
('LOAN_FTP', '贷款FTP成本', 'LOAN', 'biz_ledger', 'ftp_cost', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","ftp_cost","product_name"]', 'stat_date', '万元', 2, '贷款FTP资金成本', 3),
('LOAN_RISK', '贷款风险成本', 'LOAN', 'biz_ledger', 'risk_cost', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","risk_cost","product_name"]', 'stat_date', '万元', 2, '贷款风险计提成本', 4),
('LOAN_OP', '贷款运营成本', 'LOAN', 'biz_ledger', 'op_cost', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","op_cost","product_name","expense_type"]', 'stat_date', '万元', 2, '贷款运营成本（含费用类型拆分）', 5);

-- 存款原子指标
INSERT INTO atomic_indicator (code, name, business_line, source_table, source_field, filter_condition, detail_table, detail_dimension, detail_display_fields, detail_group_by, unit, precision_val, description, sort_order) VALUES
('DEPOSIT_BALANCE', '存款余额', 'DEPOSIT', 'biz_ledger', 'biz_amount', 'product_type=DEPOSIT', 'biz_ledger', 'product_id', '["biz_id","customer_name","biz_amount","product_name"]', 'stat_date', '万元', 2, '存款本金余额', 1),
('DEPOSIT_FTP', '存款FTP收入', 'DEPOSIT', 'biz_ledger', 'interest_income', 'product_type=DEPOSIT', 'biz_ledger', 'product_id', '["biz_id","customer_name","interest_income","product_name"]', 'stat_date', '万元', 2, '存款FTP资金收入', 2),
('DEPOSIT_INTEREST', '存款利息支出', 'DEPOSIT', 'biz_ledger', 'interest_expense', 'product_type=DEPOSIT', 'biz_ledger', 'product_id', '["biz_id","customer_name","interest_expense","product_name"]', 'stat_date', '万元', 2, '存款对客利息支出', 3),
('DEPOSIT_OP', '存款运营成本', 'DEPOSIT', 'biz_ledger', 'op_cost', 'product_type=DEPOSIT', 'biz_ledger', 'product_id', '["biz_id","customer_name","op_cost","product_name","expense_type"]', 'stat_date', '万元', 2, '存款运营成本（含费用类型拆分）', 4);

-- ============================================
-- 6. 插入派生指标初始数据
-- ============================================

-- 贷款派生指标
INSERT INTO derived_indicator (code, name, business_line, calc_formula, formula_vars, unit, precision_val, description, sort_order) VALUES
('LOAN_PROFIT', '贷款利润', 'LOAN', 'LOAN_INTEREST - LOAN_FTP - LOAN_RISK - LOAN_OP', '["LOAN_INTEREST","LOAN_FTP","LOAN_RISK","LOAN_OP"]', '万元', 2, '贷款净利润', 1),
('LOAN_NET_INTEREST', '贷款净利息收入', 'LOAN', 'LOAN_INTEREST - LOAN_FTP', '["LOAN_INTEREST","LOAN_FTP"]', '万元', 2, '扣除FTP后的利息收入', 2),
('LOAN_COST_RATIO', '贷款成本收入比', 'LOAN', '(LOAN_FTP + LOAN_RISK + LOAN_OP) / LOAN_INTEREST', '["LOAN_FTP","LOAN_RISK","LOAN_OP","LOAN_INTEREST"]', '%', 2, '成本占收入比', 3),
('LOAN_RISK_RATIO', '贷款风险成本率', 'LOAN', 'LOAN_RISK / LOAN_INTEREST', '["LOAN_RISK","LOAN_INTEREST"]', '%', 2, '风险成本占比', 4),
('LOAN_FTP_SPREAD', '贷款FTP利差', 'LOAN', '(LOAN_INTEREST - LOAN_FTP) / LOAN_BALANCE', '["LOAN_INTEREST","LOAN_FTP","LOAN_BALANCE"]', '%', 2, '净息差', 5);

-- 存款派生指标
INSERT INTO derived_indicator (code, name, business_line, calc_formula, formula_vars, unit, precision_val, description, sort_order) VALUES
('DEPOSIT_PROFIT', '存款利润', 'DEPOSIT', 'DEPOSIT_FTP - DEPOSIT_INTEREST - DEPOSIT_OP', '["DEPOSIT_FTP","DEPOSIT_INTEREST","DEPOSIT_OP"]', '万元', 2, '存款净利润', 1),
('DEPOSIT_NET_INTEREST', '存款净利息收入', 'DEPOSIT', 'DEPOSIT_FTP - DEPOSIT_INTEREST', '["DEPOSIT_FTP","DEPOSIT_INTEREST"]', '万元', 2, '扣除利息支出后的收入', 2),
('DEPOSIT_COST_RATIO', '存款成本收入比', 'DEPOSIT', '(DEPOSIT_INTEREST + DEPOSIT_OP) / DEPOSIT_FTP', '["DEPOSIT_INTEREST","DEPOSIT_OP","DEPOSIT_FTP"]', '%', 2, '成本占收入比', 3),
('DEPOSIT_FTP_SPREAD', '存款FTP利差', 'DEPOSIT', '(DEPOSIT_FTP - DEPOSIT_INTEREST) / DEPOSIT_BALANCE', '["DEPOSIT_FTP","DEPOSIT_INTEREST","DEPOSIT_BALANCE"]', '%', 2, '净息差', 4);

-- ============================================
-- 7. 插入统计口径初始数据
-- ============================================

-- 贷款统计口径
INSERT INTO indicator_stat_config (indicator_code, stat_type, calc_formula, description) VALUES
('LOAN_BALANCE', 'MONTHLY_DAILY_AVG', 'SUM(LOAN_BALANCE) / DAY(LAST_DAY(日期))', '贷款月日均余额'),
('LOAN_BALANCE', 'YEARLY_DAILY_AVG', 'SUM(LOAN_BALANCE) / DAYOFYEAR(日期)', '贷款年日均余额'),
('LOAN_INTEREST', 'MONTHLY_DAILY_AVG', 'SUM(LOAN_INTEREST) / DAY(LAST_DAY(日期))', '贷款月日均利息收入'),
('LOAN_INTEREST', 'YEARLY_DAILY_AVG', 'SUM(LOAN_INTEREST) / DAYOFYEAR(日期)', '贷款年日均利息收入'),
('LOAN_FTP', 'MONTHLY_DAILY_AVG', 'SUM(LOAN_FTP) / DAY(LAST_DAY(日期))', '贷款月日均FTP成本'),
('LOAN_FTP', 'YEARLY_DAILY_AVG', 'SUM(LOAN_FTP) / DAYOFYEAR(日期)', '贷款年日均FTP成本'),
('LOAN_RISK', 'MONTHLY_DAILY_AVG', 'SUM(LOAN_RISK) / DAY(LAST_DAY(日期))', '贷款月日均风险成本'),
('LOAN_RISK', 'YEARLY_DAILY_AVG', 'SUM(LOAN_RISK) / DAYOFYEAR(日期)', '贷款年日均风险成本'),
('LOAN_OP', 'MONTHLY_DAILY_AVG', 'SUM(LOAN_OP) / DAY(LAST_DAY(日期))', '贷款月日均运营成本'),
('LOAN_OP', 'YEARLY_DAILY_AVG', 'SUM(LOAN_OP) / DAYOFYEAR(日期)', '贷款年日均运营成本');

-- 存款统计口径
INSERT INTO indicator_stat_config (indicator_code, stat_type, calc_formula, description) VALUES
('DEPOSIT_BALANCE', 'MONTHLY_DAILY_AVG', 'SUM(DEPOSIT_BALANCE) / DAY(LAST_DAY(日期))', '存款月日均余额'),
('DEPOSIT_BALANCE', 'YEARLY_DAILY_AVG', 'SUM(DEPOSIT_BALANCE) / DAYOFYEAR(日期)', '存款年日均余额'),
('DEPOSIT_FTP', 'MONTHLY_DAILY_AVG', 'SUM(DEPOSIT_FTP) / DAY(LAST_DAY(日期))', '存款月日均FTP收入'),
('DEPOSIT_FTP', 'YEARLY_DAILY_AVG', 'SUM(DEPOSIT_FTP) / DAYOFYEAR(日期)', '存款年日均FTP收入'),
('DEPOSIT_INTEREST', 'MONTHLY_DAILY_AVG', 'SUM(DEPOSIT_INTEREST) / DAY(LAST_DAY(日期))', '存款月日均利息支出'),
('DEPOSIT_INTEREST', 'YEARLY_DAILY_AVG', 'SUM(DEPOSIT_INTEREST) / DAYOFYEAR(日期)', '存款年日均利息支出'),
('DEPOSIT_OP', 'MONTHLY_DAILY_AVG', 'SUM(DEPOSIT_OP) / DAY(LAST_DAY(日期))', '存款月日均运营成本'),
('DEPOSIT_OP', 'YEARLY_DAILY_AVG', 'SUM(DEPOSIT_OP) / DAYOFYEAR(日期)', '存款年日均运营成本');

-- ============================================
-- 完成
-- ============================================
SELECT '指标体系数据库表创建完成' AS result;
