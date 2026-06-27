-- ============================================
-- 指标体系数据库表结构和初始数据
-- 创建时间：2026-06-27
-- 说明：资产（原贷款）、负债（原存款）
-- ============================================

USE multi_profit;

-- ============================================
-- 1. 原子指标表
-- ============================================
CREATE TABLE IF NOT EXISTS atomic_indicator (
    code VARCHAR(50) PRIMARY KEY COMMENT '指标编码',
    name VARCHAR(100) NOT NULL COMMENT '指标名称',
    business_line VARCHAR(20) NOT NULL COMMENT '业务条线：ASSET/LIABILITY',
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
    business_line VARCHAR(20) NOT NULL COMMENT '业务条线：ASSET/LIABILITY',
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

-- 资产原子指标（原贷款）
INSERT INTO atomic_indicator (code, name, business_line, source_table, source_field, filter_condition, detail_table, detail_dimension, detail_display_fields, detail_group_by, unit, precision_val, description, sort_order) VALUES
('ASSET_BALANCE', '资产余额', 'ASSET', 'biz_ledger', 'biz_amount', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","biz_amount","product_name"]', 'stat_date', '万元', 2, '资产本金余额', 1),
('ASSET_INTEREST', '资产利息收入', 'ASSET', 'biz_ledger', 'interest_income', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","interest_income","product_name"]', 'stat_date', '万元', 2, '资产对客利息收入', 2),
('ASSET_FTP', '资产FTP成本', 'ASSET', 'biz_ledger', 'ftp_cost', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","ftp_cost","product_name"]', 'stat_date', '万元', 2, '资产FTP资金成本', 3),
('ASSET_RISK', '资产风险成本', 'ASSET', 'biz_ledger', 'risk_cost', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","risk_cost","product_name"]', 'stat_date', '万元', 2, '资产风险计提成本', 4),
('ASSET_OP', '资产运营成本', 'ASSET', 'biz_ledger', 'op_cost', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","op_cost","product_name","expense_type"]', 'stat_date', '万元', 2, '资产运营成本（含费用类型拆分）', 5);

-- 负债原子指标（原存款）
INSERT INTO atomic_indicator (code, name, business_line, source_table, source_field, filter_condition, detail_table, detail_dimension, detail_display_fields, detail_group_by, unit, precision_val, description, sort_order) VALUES
('LIABILITY_BALANCE', '负债余额', 'LIABILITY', 'biz_ledger', 'biz_amount', 'product_type=DEPOSIT', 'biz_ledger', 'product_id', '["biz_id","customer_name","biz_amount","product_name"]', 'stat_date', '万元', 2, '负债本金余额', 1),
('LIABILITY_FTP', '负债FTP收入', 'LIABILITY', 'biz_ledger', 'interest_income', 'product_type=DEPOSIT', 'biz_ledger', 'product_id', '["biz_id","customer_name","interest_income","product_name"]', 'stat_date', '万元', 2, '负债FTP资金收入', 2),
('LIABILITY_INTEREST', '负债利息支出', 'LIABILITY', 'biz_ledger', 'interest_expense', 'product_type=DEPOSIT', 'biz_ledger', 'product_id', '["biz_id","customer_name","interest_expense","product_name"]', 'stat_date', '万元', 2, '负债对客利息支出', 3),
('LIABILITY_OP', '负债运营成本', 'LIABILITY', 'biz_ledger', 'op_cost', 'product_type=DEPOSIT', 'biz_ledger', 'product_id', '["biz_id","customer_name","op_cost","product_name","expense_type"]', 'stat_date', '万元', 2, '负债运营成本（含费用类型拆分）', 4);

-- ============================================
-- 6. 插入派生指标初始数据
-- ============================================

-- 资产派生指标（原贷款）
INSERT INTO derived_indicator (code, name, business_line, calc_formula, formula_vars, unit, precision_val, description, sort_order) VALUES
('ASSET_PROFIT', '资产利润', 'ASSET', 'ASSET_INTEREST - ASSET_FTP - ASSET_RISK - ASSET_OP', '["ASSET_INTEREST","ASSET_FTP","ASSET_RISK","ASSET_OP"]', '万元', 2, '资产净利润', 1),
('ASSET_NET_INTEREST', '资产净利息收入', 'ASSET', 'ASSET_INTEREST - ASSET_FTP', '["ASSET_INTEREST","ASSET_FTP"]', '万元', 2, '扣除FTP后的利息收入', 2),
('ASSET_COST_RATIO', '资产成本收入比', 'ASSET', '(ASSET_FTP + ASSET_RISK + ASSET_OP) / ASSET_INTEREST', '["ASSET_FTP","ASSET_RISK","ASSET_OP","ASSET_INTEREST"]', '%', 2, '成本占收入比', 3),
('ASSET_RISK_RATIO', '资产风险成本率', 'ASSET', 'ASSET_RISK / ASSET_INTEREST', '["ASSET_RISK","ASSET_INTEREST"]', '%', 2, '风险成本占比', 4),
('ASSET_FTP_SPREAD', '资产FTP利差', 'ASSET', '(ASSET_INTEREST - ASSET_FTP) / ASSET_BALANCE', '["ASSET_INTEREST","ASSET_FTP","ASSET_BALANCE"]', '%', 2, '净息差', 5);

-- 负债派生指标（原存款）
INSERT INTO derived_indicator (code, name, business_line, calc_formula, formula_vars, unit, precision_val, description, sort_order) VALUES
('LIABILITY_PROFIT', '负债利润', 'LIABILITY', 'LIABILITY_FTP - LIABILITY_INTEREST - LIABILITY_OP', '["LIABILITY_FTP","LIABILITY_INTEREST","LIABILITY_OP"]', '万元', 2, '负债净利润', 1),
('LIABILITY_NET_INTEREST', '负债净利息收入', 'LIABILITY', 'LIABILITY_FTP - LIABILITY_INTEREST', '["LIABILITY_FTP","LIABILITY_INTEREST"]', '万元', 2, '扣除利息支出后的收入', 2),
('LIABILITY_COST_RATIO', '负债成本收入比', 'LIABILITY', '(LIABILITY_INTEREST + LIABILITY_OP) / LIABILITY_FTP', '["LIABILITY_INTEREST","LIABILITY_OP","LIABILITY_FTP"]', '%', 2, '成本占收入比', 3),
('LIABILITY_FTP_SPREAD', '负债FTP利差', 'LIABILITY', '(LIABILITY_FTP - LIABILITY_INTEREST) / LIABILITY_BALANCE', '["LIABILITY_FTP","LIABILITY_INTEREST","LIABILITY_BALANCE"]', '%', 2, '净息差', 4);

-- ============================================
-- 7. 插入统计口径初始数据
-- ============================================

-- 资产统计口径（原贷款）
INSERT INTO indicator_stat_config (indicator_code, stat_type, calc_formula, description) VALUES
('ASSET_BALANCE', 'MONTHLY_DAILY_AVG', 'SUM(ASSET_BALANCE) / DAY(LAST_DAY(日期))', '资产月日均余额'),
('ASSET_BALANCE', 'YEARLY_DAILY_AVG', 'SUM(ASSET_BALANCE) / DAYOFYEAR(日期)', '资产年日均余额'),
('ASSET_INTEREST', 'MONTHLY_DAILY_AVG', 'SUM(ASSET_INTEREST) / DAY(LAST_DAY(日期))', '资产月日均利息收入'),
('ASSET_INTEREST', 'YEARLY_DAILY_AVG', 'SUM(ASSET_INTEREST) / DAYOFYEAR(日期)', '资产年日均利息收入'),
('ASSET_FTP', 'MONTHLY_DAILY_AVG', 'SUM(ASSET_FTP) / DAY(LAST_DAY(日期))', '资产月日均FTP成本'),
('ASSET_FTP', 'YEARLY_DAILY_AVG', 'SUM(ASSET_FTP) / DAYOFYEAR(日期)', '资产年日均FTP成本'),
('ASSET_RISK', 'MONTHLY_DAILY_AVG', 'SUM(ASSET_RISK) / DAY(LAST_DAY(日期))', '资产月日均风险成本'),
('ASSET_RISK', 'YEARLY_DAILY_AVG', 'SUM(ASSET_RISK) / DAYOFYEAR(日期)', '资产年日均风险成本'),
('ASSET_OP', 'MONTHLY_DAILY_AVG', 'SUM(ASSET_OP) / DAY(LAST_DAY(日期))', '资产月日均运营成本'),
('ASSET_OP', 'YEARLY_DAILY_AVG', 'SUM(ASSET_OP) / DAYOFYEAR(日期)', '资产年日均运营成本');

-- 负债统计口径（原存款）
INSERT INTO indicator_stat_config (indicator_code, stat_type, calc_formula, description) VALUES
('LIABILITY_BALANCE', 'MONTHLY_DAILY_AVG', 'SUM(LIABILITY_BALANCE) / DAY(LAST_DAY(日期))', '负债月日均余额'),
('LIABILITY_BALANCE', 'YEARLY_DAILY_AVG', 'SUM(LIABILITY_BALANCE) / DAYOFYEAR(日期)', '负债年日均余额'),
('LIABILITY_FTP', 'MONTHLY_DAILY_AVG', 'SUM(LIABILITY_FTP) / DAY(LAST_DAY(日期))', '负债月日均FTP收入'),
('LIABILITY_FTP', 'YEARLY_DAILY_AVG', 'SUM(LIABILITY_FTP) / DAYOFYEAR(日期)', '负债年日均FTP收入'),
('LIABILITY_INTEREST', 'MONTHLY_DAILY_AVG', 'SUM(LIABILITY_INTEREST) / DAY(LAST_DAY(日期))', '负债月日均利息支出'),
('LIABILITY_INTEREST', 'YEARLY_DAILY_AVG', 'SUM(LIABILITY_INTEREST) / DAYOFYEAR(日期)', '负债年日均利息支出'),
('LIABILITY_OP', 'MONTHLY_DAILY_AVG', 'SUM(LIABILITY_OP) / DAY(LAST_DAY(日期))', '负债月日均运营成本'),
('LIABILITY_OP', 'YEARLY_DAILY_AVG', 'SUM(LIABILITY_OP) / DAYOFYEAR(日期)', '负债年日均运营成本');

-- ============================================
-- 完成
-- ============================================
SELECT '指标体系数据库表创建完成' AS result;
