-- ============================================
-- 指标库表结构和初始数据
-- 创建时间：2026-06-29
-- ============================================

USE multi_profit;

-- ============================================
-- 1. 指标定义表
-- ============================================

CREATE TABLE IF NOT EXISTS indicator_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_code VARCHAR(50) NOT NULL UNIQUE COMMENT '指标编码',
    indicator_name VARCHAR(100) NOT NULL COMMENT '指标名称',
    indicator_type VARCHAR(20) NOT NULL COMMENT '指标类型：SCALE/REVENUE/COST/EFFICIENCY/PROFIT/DAILY_AVG',
    business_line VARCHAR(20) NOT NULL COMMENT '业务条线：ASSET/LIABILITY/ALL',
    calc_formula VARCHAR(500) COMMENT '计算公式',
    data_source VARCHAR(100) COMMENT '数据来源',
    unit VARCHAR(20) COMMENT '单位',
    precision_val INT DEFAULT 2 COMMENT '精度',
    supported_dims TEXT COMMENT '支持的维度',
    description VARCHAR(500) COMMENT '描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='指标定义表';

-- ============================================
-- 2. 指标预计算结果表
-- ============================================

CREATE TABLE IF NOT EXISTS indicator_precomputed (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_code VARCHAR(50) NOT NULL COMMENT '指标编码',
    period VARCHAR(10) NOT NULL COMMENT '期间(YYYY-MM)',
    period_type VARCHAR(20) NOT NULL COMMENT '期间类型：DAY/MONTH/QUARTER/YEAR',
    dim_type VARCHAR(20) COMMENT '维度类型',
    dim_code VARCHAR(50) COMMENT '维度编码',
    dim_name VARCHAR(200) COMMENT '维度名称',
    calc_value DECIMAL(18,4) COMMENT '计算结果',
    caliber_type VARCHAR(10) DEFAULT 'ASSESS' COMMENT '口径',
    calc_time TIMESTAMP COMMENT '计算时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_indicator (indicator_code),
    INDEX idx_period (period, period_type),
    INDEX idx_dim (dim_type, dim_code),
    UNIQUE KEY uk_calc (indicator_code, period, period_type, dim_type, dim_code, caliber_type)
) COMMENT='指标预计算结果表';

-- ============================================
-- 3. 初始化指标定义数据
-- ============================================

-- 规模指标
INSERT INTO indicator_definition (indicator_code, indicator_name, indicator_type, business_line, calc_formula, data_source, unit, precision_val, supported_dims, description, sort_order) VALUES
('LOAN_BALANCE', '在贷余额', 'SCALE', 'ASSET', 'SUM(loan_balance)', 'loan_indicator_detail', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '贷款本金余额', 1),
('DEPOSIT_BALANCE', '存款余额', 'SCALE', 'LIABILITY', 'SUM(deposit_balance)', 'deposit_indicator_detail', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '存款本金余额', 2),
('LOAN_COUNT', '贷款笔数', 'SCALE', 'ASSET', 'COUNT(biz_id)', 'loan_indicator_detail', '笔', 0, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '贷款业务笔数', 3),
('DEPOSIT_COUNT', '存款笔数', 'SCALE', 'LIABILITY', 'COUNT(biz_id)', 'deposit_indicator_detail', '笔', 0, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '存款业务笔数', 4);

-- 收入指标
INSERT INTO indicator_definition (indicator_code, indicator_name, indicator_type, business_line, calc_formula, data_source, unit, precision_val, supported_dims, description, sort_order) VALUES
('INTEREST_INCOME', '利息收入', 'REVENUE', 'ASSET', 'SUM(loan_monthly_interest)', 'loan_indicator_detail', '万元', 4, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '贷款利息收入', 5),
('FTP_INCOME', 'FTP收入', 'REVENUE', 'LIABILITY', 'SUM(ftp_income)', 'deposit_indicator_detail', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '存款FTP收入', 6),
('NET_INTEREST_INCOME', '净利息收入', 'REVENUE', 'ALL', '利息收入-FTP成本', '计算', '万元', 4, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '扣除FTP后的利息收入', 7);

-- 成本指标
INSERT INTO indicator_definition (indicator_code, indicator_name, indicator_type, business_line, calc_formula, data_source, unit, precision_val, supported_dims, description, sort_order) VALUES
('FTP_COST', 'FTP成本', 'COST', 'ASSET', 'SUM(ftp_cost)', 'loan_indicator_detail', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '贷款FTP资金成本', 8),
('RISK_COST', '风险成本', 'COST', 'ASSET', 'SUM(risk_cost)', 'loan_indicator_detail', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '贷款风险计提成本', 9),
('OP_COST', '运营成本', 'COST', 'ALL', 'SUM(op_cost)', 'biz_profit_summary', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '运营成本（含分摊）', 10),
('TOTAL_COST', '总成本', 'COST', 'ALL', 'FTP成本+风险成本+运营成本', '计算', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '各项成本之和', 11);

-- 效率指标
INSERT INTO indicator_definition (indicator_code, indicator_name, indicator_type, business_line, calc_formula, data_source, unit, precision_val, supported_dims, description, sort_order) VALUES
('COST_INCOME_RATIO', '成本收入比', 'EFFICIENCY', 'ALL', '总成本/总收入', '计算', '%', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '成本占收入比', 12),
('RISK_COST_RATIO', '风险成本率', 'EFFICIENCY', 'ASSET', '风险成本/利息收入', '计算', '%', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '风险成本占比', 13),
('FTP_SPREAD', 'FTP利差', 'EFFICIENCY', 'ASSET', '(利息收入-FTP成本)/余额', '计算', '%', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '净息差', 14);

-- 利润指标
INSERT INTO indicator_definition (indicator_code, indicator_name, indicator_type, business_line, calc_formula, data_source, unit, precision_val, supported_dims, description, sort_order) VALUES
('LOAN_PROFIT', '贷款利润', 'PROFIT', 'ASSET', '利息收入-FTP成本-风险成本-运营成本', '计算', '万元', 4, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '贷款业务利润', 15),
('DEPOSIT_PROFIT', '存款利润', 'PROFIT', 'LIABILITY', 'FTP收入-利息支出-运营成本', '计算', '万元', 4, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '存款业务利润', 16),
('TOTAL_PROFIT', '总利润', 'PROFIT', 'ALL', '贷款利润+存款利润', '计算', '万元', 4, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '总业务利润', 17),
('PROFIT_PER_MANAGER', '人均利润', 'PROFIT', 'ALL', '总利润/客户经理人数', '计算', '万元', 4, 'ORG,BIZ_LINE', '客户经理人均利润', 18);

-- 日均指标
INSERT INTO indicator_definition (indicator_code, indicator_name, indicator_type, business_line, calc_formula, data_source, unit, precision_val, supported_dims, description, sort_order) VALUES
('LOAN_BALANCE_MONTHLY_AVG', '贷款余额月日均', 'DAILY_AVG', 'ASSET', 'SUM(loan_balance)/当月天数', '计算', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '贷款余额月日均', 19),
('LOAN_BALANCE_YEARLY_AVG', '贷款余额年日均', 'DAILY_AVG', 'ASSET', 'SUM(loan_balance)/当年天数', '计算', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '贷款余额年日均', 20),
('DEPOSIT_BALANCE_MONTHLY_AVG', '存款余额月日均', 'DAILY_AVG', 'LIABILITY', 'SUM(deposit_balance)/当月天数', '计算', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '存款余额月日均', 21),
('DEPOSIT_BALANCE_YEARLY_AVG', '存款余额年日均', 'DAILY_AVG', 'LIABILITY', 'SUM(deposit_balance)/当年天数', '计算', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '存款余额年日均', 22),
('INTEREST_MONTHLY_AVG', '利息收入月日均', 'DAILY_AVG', 'ASSET', 'SUM(利息)/当月天数', '计算', '万元', 4, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '利息收入月日均', 23);

SELECT '指标库初始化完成' AS result;
