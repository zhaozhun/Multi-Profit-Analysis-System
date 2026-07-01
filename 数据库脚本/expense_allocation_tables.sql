-- ============================================
-- 运营成本分摊系统 - 数据库表结构
-- 创建时间：2026-06-29
-- ============================================

USE multi_profit;

-- ============================================
-- 1. 费用原始表（每种费用独立）
-- ============================================

-- 房租物业表（部门维度）
CREATE TABLE IF NOT EXISTS expense_rent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间(YYYY-MM)',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    dept_name VARCHAR(200) COMMENT '部门名称',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_dept (dept_id)
) COMMENT='房租物业费用表';

-- 人力成本表（部门+人员维度）
CREATE TABLE IF NOT EXISTS expense_salary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    dept_name VARCHAR(200) COMMENT '部门名称',
    manager_id BIGINT COMMENT '客户经理ID',
    manager_name VARCHAR(200) COMMENT '客户经理名称',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_dept (dept_id),
    INDEX idx_manager (manager_id)
) COMMENT='人力成本费用表';

-- IT系统费用表（产品维度）
CREATE TABLE IF NOT EXISTS expense_it (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(200) COMMENT '产品名称',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_product (product_id)
) COMMENT='IT系统费用表';

-- 营销费用表（机构维度）
CREATE TABLE IF NOT EXISTS expense_marketing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间',
    org_id BIGINT NOT NULL COMMENT '机构ID',
    org_name VARCHAR(200) COMMENT '机构名称',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_org (org_id)
) COMMENT='营销费用表';

-- 其他费用表（可扩展）
CREATE TABLE IF NOT EXISTS expense_other (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间',
    expense_type VARCHAR(50) NOT NULL COMMENT '费用类型',
    dim_type VARCHAR(20) NOT NULL COMMENT '维度类型(DEPT/PRODUCT/ORG)',
    dim_id BIGINT NOT NULL COMMENT '维度ID',
    dim_name VARCHAR(200) COMMENT '维度名称',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_type (expense_type),
    INDEX idx_dim (dim_type, dim_id)
) COMMENT='其他费用表';

-- ============================================
-- 2. 分摊因子表
-- ============================================

CREATE TABLE IF NOT EXISTS allocation_factor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    factor_code VARCHAR(50) NOT NULL UNIQUE COMMENT '因子编码',
    factor_name VARCHAR(100) NOT NULL COMMENT '因子名称',
    factor_type VARCHAR(50) NOT NULL COMMENT '因子类型(MANAGER_COUNT/BIZ_AMOUNT/BALANCE/BIZ_COUNT/REVENUE)',
    source_table VARCHAR(100) NOT NULL COMMENT '数据来源表',
    source_field VARCHAR(100) NOT NULL COMMENT '数据来源字段',
    calc_formula VARCHAR(500) COMMENT '计算公式',
    description VARCHAR(500) COMMENT '描述',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='分摊因子表';

-- ============================================
-- 3. 分摊规则表
-- ============================================

CREATE TABLE IF NOT EXISTS expense_allocation_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    expense_table VARCHAR(100) NOT NULL COMMENT '费用表名',
    expense_type VARCHAR(50) NOT NULL COMMENT '费用类型',
    source_dim_type VARCHAR(20) NOT NULL COMMENT '源维度类型(DEPT/PRODUCT/ORG)',
    target_type VARCHAR(20) NOT NULL DEFAULT 'BIZ' COMMENT '目标类型(固定为BIZ)',
    factor_code VARCHAR(50) NOT NULL COMMENT '分摊因子编码',
    description VARCHAR(500) COMMENT '描述',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (factor_code) REFERENCES allocation_factor(factor_code)
) COMMENT='分摊规则表';

-- ============================================
-- 4. 分摊结果表
-- ============================================

CREATE TABLE IF NOT EXISTS expense_allocation_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间',
    biz_id VARCHAR(30) NOT NULL COMMENT '业务编号',
    biz_type VARCHAR(20) NOT NULL COMMENT '业务类型(LOAN/DEPOSIT)',
    expense_type VARCHAR(50) NOT NULL COMMENT '费用类型',
    expense_name VARCHAR(100) COMMENT '费用名称',
    allocated_amount DECIMAL(18,4) NOT NULL COMMENT '分摊金额',
    factor_value DECIMAL(18,4) COMMENT '因子值',
    factor_ratio DECIMAL(12,8) COMMENT '分摊比例',
    rule_code VARCHAR(50) COMMENT '规则编码',
    batch_no VARCHAR(50) COMMENT '批次号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_biz (biz_id),
    INDEX idx_expense (expense_type),
    INDEX idx_batch (batch_no)
) COMMENT='费用分摊结果表';

-- ============================================
-- 5. 业务利润汇总表
-- ============================================

CREATE TABLE IF NOT EXISTS biz_profit_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间',
    biz_id VARCHAR(30) NOT NULL COMMENT '业务编号',
    biz_type VARCHAR(20) NOT NULL COMMENT '业务类型(LOAN/DEPOSIT)',
    customer_id BIGINT COMMENT '客户ID',
    customer_name VARCHAR(200) COMMENT '客户名称',
    org_id BIGINT COMMENT '机构ID',
    org_name VARCHAR(200) COMMENT '机构名称',
    biz_line_id BIGINT COMMENT '条线ID',
    biz_line_name VARCHAR(200) COMMENT '条线名称',
    product_id BIGINT COMMENT '产品ID',
    product_name VARCHAR(200) COMMENT '产品名称',
    channel_id BIGINT COMMENT '渠道ID',
    channel_name VARCHAR(200) COMMENT '渠道名称',
    manager_id BIGINT COMMENT '客户经理ID',
    manager_name VARCHAR(200) COMMENT '客户经理名称',
    -- 金额字段
    balance DECIMAL(18,2) COMMENT '余额',
    interest_income DECIMAL(18,4) COMMENT '利息收入',
    ftp_cost DECIMAL(18,4) COMMENT 'FTP成本',
    risk_cost DECIMAL(18,4) COMMENT '风险成本',
    op_cost DECIMAL(18,4) COMMENT '运营成本(各项费用之和)',
    profit DECIMAL(18,4) COMMENT '利润(利息收入-FTP成本-风险成本-运营成本)',
    -- 口径
    caliber_type VARCHAR(10) DEFAULT 'ASSESS' COMMENT '口径',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_biz (biz_id),
    INDEX idx_org (org_id),
    INDEX idx_product (product_id),
    INDEX idx_manager (manager_id),
    UNIQUE KEY uk_period_biz_caliber (period, biz_id, caliber_type)
) COMMENT='业务利润汇总表';

-- ============================================
-- 6. 初始化数据
-- ============================================

-- 初始化分摊因子
INSERT INTO allocation_factor (factor_code, factor_name, factor_type, source_table, source_field, description) VALUES
('MANAGER_COUNT', '客户经理人数', 'MANAGER_COUNT', 'dimension_master', 'id', '按客户经理人数分摊'),
('BIZ_AMOUNT', '业务金额', 'BIZ_AMOUNT', 'biz_ledger', 'biz_amount', '按业务金额分摊'),
('LOAN_BALANCE', '贷款余额', 'BALANCE', 'loan_indicator_detail', 'loan_balance', '按贷款余额分摊'),
('DEPOSIT_BALANCE', '存款余额', 'BALANCE', 'deposit_indicator_detail', 'deposit_balance', '按存款余额分摊'),
('BIZ_COUNT', '业务笔数', 'BIZ_COUNT', 'biz_ledger', 'biz_id', '按业务笔数分摊'),
('REVENUE', '收入金额', 'REVENUE', 'biz_ledger', 'revenue', '按收入金额分摊');

-- 初始化分摊规则
INSERT INTO expense_allocation_rule (rule_code, rule_name, expense_table, expense_type, source_dim_type, factor_code, description) VALUES
('RENT_BY_MANAGER', '房租按客户经理分摊', 'expense_rent', 'RENT', 'DEPT', 'MANAGER_COUNT', '部门房租按人数分摊到客户经理，再按业务量分摊到业务'),
('SALARY_BY_MANAGER', '人力成本按客户经理分摊', 'expense_salary', 'SALARY', 'DEPT', 'MANAGER_COUNT', '人力成本按人数分摊到客户经理，再按业务量分摊到业务'),
('IT_BY_PRODUCT', 'IT费用按产品分摊', 'expense_it', 'IT', 'PRODUCT', 'BIZ_AMOUNT', 'IT费用按业务金额分摊到该产品的每笔业务'),
('MARKETING_BY_ORG', '营销费用按机构分摊', 'expense_marketing', 'MARKETING', 'ORG', 'BIZ_AMOUNT', '营销费用按业务金额分摊到该机构的每笔业务');

-- 初始化费用数据（模拟数据）
-- 房租物业（部门维度）
INSERT INTO expense_rent (period, dept_id, dept_name, amount) VALUES
('2026-06', 55, '科技部', 100000),
('2026-06', 56, '市场部', 80000),
('2026-06', 57, '运营部', 60000),
('2026-06', 58, '风控部', 50000);

-- 人力成本（部门+人员维度）
INSERT INTO expense_salary (period, dept_id, dept_name, manager_id, manager_name, amount) VALUES
('2026-06', 55, '科技部', 177, '北京分行客户经理', 20000),
('2026-06', 55, '科技部', 178, '上海分行客户经理', 20000),
('2026-06', 56, '市场部', 179, '深圳分行客户经理', 18000),
('2026-06', 56, '市场部', 180, '广州分行客户经理', 18000);

-- IT系统费用（产品维度）
INSERT INTO expense_it (period, product_id, product_name, amount) VALUES
('2026-06', 108, '公司贷款', 50000),
('2026-06', 109, '个人贷款', 30000),
('2026-06', 110, '公司存款', 20000);

-- 营销费用（机构维度）
INSERT INTO expense_marketing (period, org_id, org_name, amount) VALUES
('2026-06', 94, '北京分行', 200000),
('2026-06', 95, '上海分行', 150000),
('2026-06', 96, '深圳分行', 120000);

SELECT '运营成本分摊系统表创建完成' AS result;
