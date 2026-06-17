-- ============================================
-- 多维盈利分析系统 - H2 Schema (兼容MySQL语法)
-- ============================================

-- 维度主数据
CREATE TABLE IF NOT EXISTS dimension_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    dim_type VARCHAR(20) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    ext_attrs TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 利润计算公式
CREATE TABLE IF NOT EXISTS profit_formula (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    expression TEXT NOT NULL,
    caliber_type VARCHAR(20) NOT NULL,
    version VARCHAR(20) DEFAULT '1.0',
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 成本分摊规则
CREATE TABLE IF NOT EXISTS cost_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    cost_type VARCHAR(20) NOT NULL,
    dimension VARCHAR(20) NOT NULL,
    factor VARCHAR(50) NOT NULL,
    ratio DECIMAL(10,4),
    version VARCHAR(20) DEFAULT '1.0',
    effect_date DATE,
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 预警规则
CREATE TABLE IF NOT EXISTS alert_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    alert_type VARCHAR(20) NOT NULL,
    metric_code VARCHAR(50) NOT NULL,
    threshold DECIMAL(10,2) NOT NULL,
    threshold_type VARCHAR(20) DEFAULT 'PERCENT',
    level VARCHAR(20) DEFAULT 'WARNING',
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 预警记录
CREATE TABLE IF NOT EXISTS alert_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT,
    level VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    dim_type VARCHAR(20),
    dim_id BIGINT,
    dim_name VARCHAR(200),
    anomaly_value DECIMAL(10,2),
    ai_analysis TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    handler VARCHAR(100),
    handle_note TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    handle_time TIMESTAMP
);

-- 业务明细台账（ClickHouse表改用H2存储）
CREATE TABLE IF NOT EXISTS biz_ledger (
    biz_id VARCHAR(30) NOT NULL,
    stat_date DATE,
    account_period VARCHAR(7),
    org_id BIGINT,
    org_code VARCHAR(20),
    org_name VARCHAR(100),
    product_id BIGINT,
    product_code VARCHAR(20),
    product_name VARCHAR(100),
    biz_line_id BIGINT,
    biz_line_code VARCHAR(20),
    biz_line_name VARCHAR(100),
    dept_id BIGINT,
    dept_code VARCHAR(20),
    dept_name VARCHAR(100),
    channel_id BIGINT,
    channel_code VARCHAR(20),
    channel_name VARCHAR(100),
    manager_id BIGINT,
    manager_code VARCHAR(20),
    manager_name VARCHAR(100),
    customer_name VARCHAR(100),
    product_type VARCHAR(20) DEFAULT 'LOAN',  -- LOAN-贷款 DEPOSIT-存款 OTHER-其他
    biz_amount DECIMAL(18,2),
    revenue DECIMAL(18,2),
    interest_income DECIMAL(18,2),
    interest_expense DECIMAL(18,2) DEFAULT 0,
    fee_income DECIMAL(18,2),
    non_interest_income DECIMAL(18,2),
    ftp_cost DECIMAL(18,2),
    risk_cost DECIMAL(18,2),
    op_cost DECIMAL(18,2),
    net_profit DECIMAL(18,2),
    caliber_type VARCHAR(10) DEFAULT 'BOOK',
    currency VARCHAR(10) DEFAULT 'CNY',
    PRIMARY KEY (biz_id)
);

CREATE INDEX IF NOT EXISTS idx_ledger_period ON biz_ledger(account_period);
CREATE INDEX IF NOT EXISTS idx_ledger_org ON biz_ledger(org_name);
CREATE INDEX IF NOT EXISTS idx_ledger_product ON biz_ledger(product_name);

-- ============================================
-- 指标库
-- ============================================
CREATE TABLE IF NOT EXISTS indicator_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL,
    data_type VARCHAR(20) DEFAULT 'DECIMAL',
    unit VARCHAR(20),
    precision_val INT DEFAULT 2,
    calc_formula TEXT,
    data_source VARCHAR(50),
    source_field VARCHAR(50),
    filter_condition TEXT,
    pre_calc_periods VARCHAR(100),
    supported_dims TEXT,
    description TEXT,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 指标预计算结果
CREATE TABLE IF NOT EXISTS indicator_pre_calc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_code VARCHAR(50) NOT NULL,
    calc_period VARCHAR(10) NOT NULL,
    period_value VARCHAR(20) NOT NULL,
    dim_type VARCHAR(20),
    dim_code VARCHAR(50),
    dim_name VARCHAR(100),
    calc_value DECIMAL(18,4),
    calc_time TIMESTAMP,
    status TINYINT DEFAULT 1
);

-- 自定义报表模板
CREATE TABLE IF NOT EXISTS custom_report_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    row_dims TEXT,
    col_metrics TEXT,
    filter_config TEXT,
    sort_config TEXT,
    is_system TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 客户主数据
CREATE TABLE IF NOT EXISTS customer_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    customer_name VARCHAR(200) NOT NULL,
    customer_type VARCHAR(20) NOT NULL,
    industry VARCHAR(50),
    region VARCHAR(50),
    credit_rating VARCHAR(10),
    manager_id BIGINT,
    manager_name VARCHAR(100),
    org_id BIGINT,
    org_name VARCHAR(100),
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
