-- =====================================================
-- 产品分润配置精细化 + 运营费用分摊规则细化
-- 创建日期: 2026-06-26
-- =====================================================

USE multi_profit;

-- -----------------------------------------------------
-- 1. 产品分润配置表（精细化）
-- -----------------------------------------------------
DROP TABLE IF EXISTS product_commission_config;
CREATE TABLE product_commission_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_code VARCHAR(50) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    product_type VARCHAR(50) NOT NULL COMMENT '产品类型',
    need_commission TINYINT(1) DEFAULT 0 COMMENT '是否需要分润(0-否,1-是)',
    commission_type VARCHAR(50) COMMENT '分润类型(REVENUE_SHARE/PROFIT_SHARE/BALANCE_SHARE)',
    calc_base VARCHAR(50) COMMENT '计算基数(INTEREST_INCOME/LOAN_BALANCE/FEE_INCOME/NET_PROFIT)',
    commission_rate DECIMAL(8,6) COMMENT '分润费率',
    min_commission DECIMAL(18,2) COMMENT '最低分润金额',
    max_commission DECIMAL(18,2) COMMENT '最高分润金额',
    receiver_type VARCHAR(50) COMMENT '接收方类型(BRANCH/DEPT/PERSON)',
    receiver_code VARCHAR(50) COMMENT '接收方编码',
    receiver_name VARCHAR(100) COMMENT '接收方名称',
    effective_date DATE COMMENT '生效日期',
    expire_date DATE COMMENT '失效日期',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    remark VARCHAR(500) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product (product_code),
    INDEX idx_need_commission (need_commission),
    INDEX idx_product_type (product_type),
    INDEX idx_status (status)
) COMMENT '产品分润配置表';

-- -----------------------------------------------------
-- 2. 运营费用分摊规则表（精细化）
-- -----------------------------------------------------
DROP TABLE IF EXISTS operation_cost_allocation_rule;
CREATE TABLE operation_cost_allocation_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    cost_code VARCHAR(50) NOT NULL UNIQUE COMMENT '费用编码',
    cost_name VARCHAR(100) NOT NULL COMMENT '费用名称',
    cost_category VARCHAR(50) NOT NULL COMMENT '费用类别(FIXED/VARIABLE/DIRECT)',
    cost_type VARCHAR(50) NOT NULL COMMENT '费用类型(RENT/UTILITIES/WORKSTATION/REIMBURSE/COLLECTION/DATA_FEE/IT_OPS/MARKETING/TRAINING/ADMIN)',
    allocation_method VARCHAR(50) NOT NULL COMMENT '分摊方法(EMPLOYEE_COUNT/WORK_HOURS/SALARY/AREA/BIZ_VOLUME/DEPT_DIRECT/CUSTOM)',
    allocation_factor VARCHAR(100) COMMENT '分摊因子配置(JSON)',
    target_scope VARCHAR(50) NOT NULL COMMENT '分摊范围(ALL/DEPT/TEAM)',
    target_dept_codes JSON COMMENT '目标部门编码列表',
    default_amount DECIMAL(18,2) COMMENT '默认金额(用于预算)',
    calc_formula VARCHAR(500) COMMENT '自定义计算公式',
    description VARCHAR(500) COMMENT '费用描述',
    effective_date DATE COMMENT '生效日期',
    expire_date DATE COMMENT '失效日期',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_cost_type (cost_type),
    INDEX idx_cost_category (cost_category),
    INDEX idx_status (status)
) COMMENT '运营费用分摊规则表';

-- -----------------------------------------------------
-- 3. 运营费用实际发生表
-- -----------------------------------------------------
DROP TABLE IF EXISTS operation_cost_actual;
CREATE TABLE operation_cost_actual (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    period VARCHAR(10) NOT NULL COMMENT '期间(YYYY-MM)',
    cost_code VARCHAR(50) NOT NULL COMMENT '费用编码',
    cost_name VARCHAR(100) COMMENT '费用名称',
    cost_type VARCHAR(50) NOT NULL COMMENT '费用类型',
    amount DECIMAL(18,2) NOT NULL COMMENT '实际金额',
    dept_code VARCHAR(50) COMMENT '归属部门(直接归属时)',
    vendor VARCHAR(200) COMMENT '供应商',
    invoice_no VARCHAR(100) COMMENT '发票号',
    remark VARCHAR(500) COMMENT '备注',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/ALLOCATED/CONFIRMED)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_period (period),
    INDEX idx_cost_code (cost_code),
    INDEX idx_cost_type (cost_type),
    INDEX idx_status (status)
) COMMENT '运营费用实际发生表';

-- -----------------------------------------------------
-- 4. 运营费用分摊结果表
-- -----------------------------------------------------
DROP TABLE IF EXISTS operation_cost_allocation_result;
CREATE TABLE operation_cost_allocation_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    batch_no VARCHAR(50) NOT NULL COMMENT '批次号',
    period VARCHAR(10) NOT NULL COMMENT '期间',
    cost_code VARCHAR(50) NOT NULL COMMENT '费用编码',
    cost_name VARCHAR(100) COMMENT '费用名称',
    cost_type VARCHAR(50) NOT NULL COMMENT '费用类型',
    total_amount DECIMAL(18,2) NOT NULL COMMENT '总金额',
    target_type VARCHAR(50) NOT NULL COMMENT '分摊目标类型(EMPLOYEE/DEPT/ORG)',
    target_code VARCHAR(50) NOT NULL COMMENT '目标编码',
    target_name VARCHAR(100) COMMENT '目标名称',
    allocated_amount DECIMAL(18,2) NOT NULL COMMENT '分摊金额',
    allocation_factor VARCHAR(50) COMMENT '分摊因子',
    factor_value DECIMAL(18,4) COMMENT '因子值',
    factor_ratio DECIMAL(12,8) COMMENT '因子占比',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_period (period),
    INDEX idx_batch (batch_no),
    INDEX idx_cost_type (cost_type),
    INDEX idx_target (target_type, target_code)
) COMMENT '运营费用分摊结果表';

-- -----------------------------------------------------
-- 5. 产品收入数据表（用于分润计算）
-- -----------------------------------------------------
DROP TABLE IF EXISTS product_income_data;
CREATE TABLE product_income_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    period VARCHAR(10) NOT NULL COMMENT '期间(YYYY-MM)',
    product_code VARCHAR(50) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(100) COMMENT '产品名称',
    product_type VARCHAR(50) COMMENT '产品类型',
    interest_income DECIMAL(18,2) DEFAULT 0 COMMENT '利息收入',
    fee_income DECIMAL(18,2) DEFAULT 0 COMMENT '手续费收入',
    total_revenue DECIMAL(18,2) DEFAULT 0 COMMENT '总收入',
    loan_balance DECIMAL(18,2) DEFAULT 0 COMMENT '贷款余额',
    biz_amount DECIMAL(18,2) DEFAULT 0 COMMENT '业务量(笔数)',
    net_profit DECIMAL(18,2) DEFAULT 0 COMMENT '净利润',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_period_product (period, product_code),
    INDEX idx_period (period),
    INDEX idx_product_code (product_code),
    INDEX idx_product_type (product_type)
) COMMENT '产品收入数据表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 初始化产品分润配置（10个产品，5个需要分润）
INSERT INTO product_commission_config (product_code, product_name, product_type, need_commission, commission_type, calc_base, commission_rate, receiver_type, receiver_code, receiver_name, status) VALUES
('LOAN001', '个人消费贷款', 'LOAN', 1, 'REVENUE_SHARE', 'INTEREST_INCOME', 0.150000, 'BRANCH', 'ORG001', '总行营业部', 'ACTIVE'),
('LOAN002', '个人经营贷款', 'LOAN', 1, 'REVENUE_SHARE', 'INTEREST_INCOME', 0.120000, 'BRANCH', 'ORG001', '总行营业部', 'ACTIVE'),
('LOAN003', '信用卡分期', 'LOAN', 1, 'BALANCE_SHARE', 'LOAN_BALANCE', 0.005000, 'BRANCH', 'ORG002', '华东分行', 'ACTIVE'),
('LOAN004', '小微贷款', 'LOAN', 1, 'REVENUE_SHARE', 'INTEREST_INCOME', 0.180000, 'BRANCH', 'ORG003', '华南分行', 'ACTIVE'),
('LOAN005', '汽车贷款', 'LOAN', 1, 'REVENUE_SHARE', 'INTEREST_INCOME', 0.100000, 'BRANCH', 'ORG001', '总行营业部', 'ACTIVE'),
('DEPOSIT001', '活期存款', 'DEPOSIT', 0, NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE'),
('DEPOSIT002', '定期存款', 'DEPOSIT', 0, NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE'),
('WEALTH001', '理财产品', 'WEALTH', 0, NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE'),
('FUND001', '基金代销', 'FUND', 0, NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE'),
('INSURANCE001', '保险代销', 'INSURANCE', 0, NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE');

-- 初始化产品收入数据
INSERT INTO product_income_data (period, product_code, product_name, product_type, interest_income, fee_income, total_revenue, loan_balance, biz_amount, net_profit) VALUES
('2026-06', 'LOAN001', '个人消费贷款', 'LOAN', 500000.00, 50000.00, 550000.00, 10000000.00, 200, 200000.00),
('2026-06', 'LOAN002', '个人经营贷款', 'LOAN', 800000.00, 80000.00, 880000.00, 20000000.00, 150, 350000.00),
('2026-06', 'LOAN003', '信用卡分期', 'LOAN', 300000.00, 100000.00, 400000.00, 6000000.00, 500, 150000.00),
('2026-06', 'LOAN004', '小微贷款', 'LOAN', 600000.00, 60000.00, 660000.00, 15000000.00, 100, 280000.00),
('2026-06', 'LOAN005', '汽车贷款', 'LOAN', 400000.00, 40000.00, 440000.00, 8000000.00, 180, 180000.00),
('2026-06', 'DEPOSIT001', '活期存款', 'DEPOSIT', -100000.00, 10000.00, -90000.00, 50000000.00, 1000, -50000.00),
('2026-06', 'DEPOSIT002', '定期存款', 'DEPOSIT', -200000.00, 5000.00, -195000.00, 80000000.00, 500, -80000.00),
('2026-06', 'WEALTH001', '理财产品', 'WEALTH', 0.00, 150000.00, 150000.00, 0.00, 300, 100000.00),
('2026-06', 'FUND001', '基金代销', 'FUND', 0.00, 200000.00, 200000.00, 0.00, 200, 120000.00),
('2026-06', 'INSURANCE001', '保险代销', 'INSURANCE', 0.00, 180000.00, 180000.00, 0.00, 150, 110000.00);

-- 初始化运营费用分摊规则
INSERT INTO operation_cost_allocation_rule (cost_code, cost_name, cost_category, cost_type, allocation_method, allocation_factor, target_scope, description, status) VALUES
('COST_RENT', '房租物业', 'FIXED', 'RENT', 'AREA', '{"factor":"WORKSTATION_AREA","unit":"square_meter"}', 'ALL', '办公场地租金和物业管理费', 'ACTIVE'),
('COST_UTILITIES', '水电费', 'VARIABLE', 'UTILITIES', 'EMPLOYEE_COUNT', '{"factor":"EMPLOYEE_COUNT"}', 'ALL', '水费、电费、空调费等', 'ACTIVE'),
('COST_WORKSTATION', '工位费', 'FIXED', 'WORKSTATION', 'EMPLOYEE_COUNT', '{"factor":"WORKSTATION_COUNT"}', 'ALL', '工位使用费、办公家具等', 'ACTIVE'),
('COST_REIMBURSE', '报销费用', 'VARIABLE', 'REIMBURSE', 'DEPT_DIRECT', '{"factor":"DEPT_CODE"}', 'DEPT', '员工报销费用，按部门直接归属', 'ACTIVE'),
('COST_COLLECTION', '催收费用', 'VARIABLE', 'COLLECTION', 'BIZ_VOLUME', '{"factor":"LOAN_OVERDUE_AMOUNT"}', 'ALL', '逾期贷款催收费用', 'ACTIVE'),
('COST_DATA_FEE', '数据使用费', 'FIXED', 'DATA_FEE', 'EMPLOYEE_COUNT', '{"factor":"SYSTEM_USER_COUNT"}', 'ALL', '数据查询、系统使用费', 'ACTIVE'),
('COST_IT_OPS', 'IT运维费', 'FIXED', 'IT_OPS', 'EMPLOYEE_COUNT', '{"factor":"DEVICE_COUNT"}', 'ALL', 'IT设备维护、系统运维', 'ACTIVE'),
('COST_MARKETING', '营销费用', 'VARIABLE', 'MARKETING', 'BIZ_VOLUME', '{"factor":"BIZ_AMOUNT"}', 'ALL', '广告、推广、活动费用', 'ACTIVE'),
('COST_TRAINING', '培训费用', 'VARIABLE', 'TRAINING', 'EMPLOYEE_COUNT', '{"factor":"TRAINING_PARTICIPANTS"}', 'ALL', '员工培训、技能提升', 'ACTIVE'),
('COST_ADMIN', '行政办公', 'FIXED', 'ADMIN', 'EMPLOYEE_COUNT', '{"factor":"EMPLOYEE_COUNT"}', 'ALL', '办公用品、差旅、招待', 'ACTIVE');

-- 初始化运营费用实际发生数据
INSERT INTO operation_cost_actual (period, cost_code, cost_name, cost_type, amount, dept_code, status) VALUES
('2026-06', 'COST_RENT', '房租物业', 'RENT', 150000.00, NULL, 'PENDING'),
('2026-06', 'COST_UTILITIES', '水电费', 'UTILITIES', 25000.00, NULL, 'PENDING'),
('2026-06', 'COST_WORKSTATION', '工位费', 'WORKSTATION', 30000.00, NULL, 'PENDING'),
('2026-06', 'COST_REIMBURSE', '报销费用', 'REIMBURSE', 45000.00, NULL, 'PENDING'),
('2026-06', 'COST_COLLECTION', '催收费用', 'COLLECTION', 18000.00, NULL, 'PENDING'),
('2026-06', 'COST_DATA_FEE', '数据使用费', 'DATA_FEE', 20000.00, NULL, 'PENDING'),
('2026-06', 'COST_IT_OPS', 'IT运维费', 'IT_OPS', 35000.00, NULL, 'PENDING'),
('2026-06', 'COST_MARKETING', '营销费用', 'MARKETING', 50000.00, NULL, 'PENDING'),
('2026-06', 'COST_TRAINING', '培训费用', 'TRAINING', 15000.00, NULL, 'PENDING'),
('2026-06', 'COST_ADMIN', '行政办公', 'ADMIN', 28000.00, NULL, 'PENDING');

-- =====================================================
-- 创建视图：产品分润汇总
-- =====================================================
CREATE OR REPLACE VIEW v_product_commission_config AS
SELECT
    pcc.product_code,
    pcc.product_name,
    pcc.product_type,
    pcc.need_commission,
    pcc.commission_type,
    pcc.calc_base,
    pcc.commission_rate,
    pcc.receiver_type,
    pcc.receiver_code,
    pcc.receiver_name,
    pid.interest_income,
    pid.loan_balance,
    pid.total_revenue,
    pid.net_profit,
    CASE
        WHEN pcc.calc_base = 'INTEREST_INCOME' THEN pid.interest_income * pcc.commission_rate
        WHEN pcc.calc_base = 'LOAN_BALANCE' THEN pid.loan_balance * pcc.commission_rate
        WHEN pcc.calc_base = 'FEE_INCOME' THEN pid.fee_income * pcc.commission_rate
        WHEN pcc.calc_base = 'NET_PROFIT' THEN pid.net_profit * pcc.commission_rate
        ELSE 0
    END AS estimated_commission
FROM product_commission_config pcc
LEFT JOIN product_income_data pid ON pcc.product_code = pid.product_code AND pid.period = '2026-06'
WHERE pcc.need_commission = 1 AND pcc.status = 'ACTIVE';

-- =====================================================
-- 创建视图：运营费用分摊规则汇总
-- =====================================================
CREATE OR REPLACE VIEW v_operation_cost_rule_summary AS
SELECT
    ocr.cost_code,
    ocr.cost_name,
    ocr.cost_category,
    ocr.cost_type,
    ocr.allocation_method,
    ocr.target_scope,
    oca.amount AS actual_amount,
    oca.period
FROM operation_cost_allocation_rule ocr
LEFT JOIN operation_cost_actual oca ON ocr.cost_code = oca.cost_code
WHERE ocr.status = 'ACTIVE';
