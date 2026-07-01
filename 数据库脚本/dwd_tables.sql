-- 数据库脚本/dwd_tables.sql
-- DWD层明细表
-- 创建时间：2026-06-30

-- 贷款业务明细表
CREATE TABLE IF NOT EXISTS dwd_loan_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_id VARCHAR(50) NOT NULL COMMENT '业务ID',
    account_period VARCHAR(10) NOT NULL COMMENT '账期',
    caliber_type VARCHAR(10) DEFAULT 'ASSESS' COMMENT '口径类型',

    -- 维度信息（关联主数据）
    org_id BIGINT NOT NULL COMMENT '机构ID',
    org_name VARCHAR(200) COMMENT '机构名称',
    biz_line_id BIGINT COMMENT '条线ID',
    biz_line_name VARCHAR(200) COMMENT '条线名称',
    product_id BIGINT COMMENT '产品ID',
    product_name VARCHAR(200) COMMENT '产品名称',
    channel_id BIGINT COMMENT '渠道ID',
    channel_name VARCHAR(200) COMMENT '渠道名称',
    manager_id BIGINT COMMENT '客户经理ID',
    manager_name VARCHAR(200) COMMENT '客户经理名称',
    customer_id BIGINT COMMENT '客户ID',
    customer_name VARCHAR(200) COMMENT '客户名称',

    -- 原始指标值
    loan_balance DECIMAL(18,4) COMMENT '贷款余额',
    loan_monthly_interest DECIMAL(18,4) COMMENT '月利息收入',
    ftp_cost DECIMAL(18,4) COMMENT 'FTP成本',
    risk_cost DECIMAL(18,4) COMMENT '风险成本',
    op_cost DECIMAL(18,4) COMMENT '运营成本',

    -- 派生指标值（在DWD层计算）
    loan_profit DECIMAL(18,4) COMMENT '贷款利润=利息收入-FTP成本-风险成本-运营成本',
    net_interest_margin DECIMAL(18,4) COMMENT '净利差=利息收入-FTP成本',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz (biz_id, account_period),
    INDEX idx_period (account_period),
    INDEX idx_org (org_id),
    INDEX idx_biz_line (biz_line_id),
    INDEX idx_product (product_id),
    INDEX idx_channel (channel_id),
    INDEX idx_manager (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款业务明细表(DWD层)';

-- 存款业务明细表
CREATE TABLE IF NOT EXISTS dwd_deposit_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_id VARCHAR(50) NOT NULL COMMENT '业务ID',
    account_period VARCHAR(10) NOT NULL COMMENT '账期',
    caliber_type VARCHAR(10) DEFAULT 'ASSESS' COMMENT '口径类型',

    -- 维度信息（关联主数据）
    org_id BIGINT NOT NULL COMMENT '机构ID',
    org_name VARCHAR(200) COMMENT '机构名称',
    biz_line_id BIGINT COMMENT '条线ID',
    biz_line_name VARCHAR(200) COMMENT '条线名称',
    product_id BIGINT COMMENT '产品ID',
    product_name VARCHAR(200) COMMENT '产品名称',
    channel_id BIGINT COMMENT '渠道ID',
    channel_name VARCHAR(200) COMMENT '渠道名称',
    manager_id BIGINT COMMENT '客户经理ID',
    manager_name VARCHAR(200) COMMENT '客户经理名称',
    customer_id BIGINT COMMENT '客户ID',
    customer_name VARCHAR(200) COMMENT '客户名称',

    -- 原始指标值
    deposit_balance DECIMAL(18,4) COMMENT '存款余额',
    deposit_monthly_interest DECIMAL(18,4) COMMENT '月利息支出',
    ftp_income DECIMAL(18,4) COMMENT 'FTP收入',
    op_cost DECIMAL(18,4) COMMENT '运营成本',

    -- 派生指标值（在DWD层计算）
    deposit_profit DECIMAL(18,4) COMMENT '存款利润=FTP收入-利息支出-运营成本',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz (biz_id, account_period),
    INDEX idx_period (account_period),
    INDEX idx_org (org_id),
    INDEX idx_biz_line (biz_line_id),
    INDEX idx_product (product_id),
    INDEX idx_channel (channel_id),
    INDEX idx_manager (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存款业务明细表(DWD层)';
