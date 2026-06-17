-- ============================================
-- 多维盈利分析系统 - MySQL Schema（星型模型）
-- ============================================

CREATE DATABASE IF NOT EXISTS multi_profit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE multi_profit;

-- 维度主数据
CREATE TABLE IF NOT EXISTS dimension_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL COMMENT '维度编码',
    name VARCHAR(200) NOT NULL COMMENT '维度名称',
    dim_type VARCHAR(20) NOT NULL COMMENT '维度类型：ORG/BIZ_LINE/DEPT/PRODUCT/CHANNEL/MANAGER',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    level INT DEFAULT 1 COMMENT '层级',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    product_type VARCHAR(20) DEFAULT NULL COMMENT '产品类型（仅PRODUCT维度）：LOAN/DEPOSIT',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用 0-停用',
    ext_attrs TEXT COMMENT '扩展属性JSON',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_dim_type (dim_type),
    INDEX idx_parent (parent_id),
    INDEX idx_product_type (product_type),
    UNIQUE KEY uk_code_type (code, dim_type)
) ENGINE=InnoDB COMMENT='维度主数据';

-- 业务明细台账（核心事实表 - 星型模型）
CREATE TABLE IF NOT EXISTS biz_ledger (
    biz_id VARCHAR(30) NOT NULL COMMENT '业务编号',
    stat_date DATE NOT NULL COMMENT '业务日期',
    account_period VARCHAR(7) NOT NULL COMMENT '账期月份',

    -- 维度外键（关联 dimension_master.id）
    org_id BIGINT NOT NULL COMMENT '机构ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    biz_line_id BIGINT NOT NULL COMMENT '条线ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    channel_id BIGINT NOT NULL COMMENT '渠道ID',
    manager_id BIGINT NOT NULL COMMENT '客户经理ID',
    customer_id BIGINT COMMENT '客户ID',

    -- 产品类型（冗余，用于快速筛选）
    product_type VARCHAR(20) NOT NULL COMMENT '产品类型：LOAN/DEPOSIT',

    -- 金额指标
    biz_amount DECIMAL(18,2) COMMENT '业务金额/余额',
    revenue DECIMAL(18,2) COMMENT '业务总收入',
    interest_income DECIMAL(18,2) COMMENT '利息收入/FTP收入',
    interest_expense DECIMAL(18,2) DEFAULT 0 COMMENT '利息支出（存款）',
    fee_income DECIMAL(18,2) DEFAULT 0 COMMENT '手续费收入',
    non_interest_income DECIMAL(18,2) DEFAULT 0 COMMENT '非息收入',
    ftp_cost DECIMAL(18,2) DEFAULT 0 COMMENT 'FTP成本',
    risk_cost DECIMAL(18,2) DEFAULT 0 COMMENT '风险成本',
    op_cost DECIMAL(18,2) DEFAULT 0 COMMENT '运营成本',
    net_profit DECIMAL(18,2) COMMENT '净利润',

    -- 贷款/存款拆分（冗余，用于快速聚合）
    loan_revenue DECIMAL(18,2) DEFAULT 0,
    loan_ftp_cost DECIMAL(18,2) DEFAULT 0,
    loan_risk_cost DECIMAL(18,2) DEFAULT 0,
    loan_op_cost DECIMAL(18,2) DEFAULT 0,
    loan_profit DECIMAL(18,2) DEFAULT 0,
    deposit_revenue DECIMAL(18,2) DEFAULT 0,
    deposit_interest DECIMAL(18,2) DEFAULT 0,
    deposit_op_cost DECIMAL(18,2) DEFAULT 0,
    deposit_profit DECIMAL(18,2) DEFAULT 0,

    -- 口径
    caliber_type VARCHAR(10) DEFAULT 'ASSESS' COMMENT '口径：BOOK/ASSESS',
    currency VARCHAR(10) DEFAULT 'CNY' COMMENT '币种',

    PRIMARY KEY (biz_id),
    INDEX idx_stat_date (stat_date),
    INDEX idx_period (account_period),
    INDEX idx_org_id (org_id),
    INDEX idx_product_id (product_id),
    INDEX idx_biz_line_id (biz_line_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_channel_id (channel_id),
    INDEX idx_manager_id (manager_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_product_type (product_type),
    INDEX idx_caliber (caliber_type),
    INDEX idx_date_caliber (stat_date, caliber_type)
) ENGINE=InnoDB COMMENT='业务明细台账（星型模型）';

-- 客户主数据
CREATE TABLE IF NOT EXISTS customer_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_code VARCHAR(50) NOT NULL UNIQUE COMMENT '客户编码',
    customer_name VARCHAR(200) NOT NULL COMMENT '客户名称',
    customer_type VARCHAR(20) NOT NULL COMMENT '客户类型：CORP/PERSON',
    industry VARCHAR(50) COMMENT '行业',
    region VARCHAR(50) COMMENT '地区',
    credit_rating VARCHAR(10) COMMENT '信用评级',
    manager_id BIGINT COMMENT '管户客户经理ID',
    manager_name VARCHAR(100) COMMENT '管户客户经理名称',
    org_id BIGINT COMMENT '归属机构ID',
    org_name VARCHAR(100) COMMENT '归属机构名称',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='客户主数据';

-- 利润计算公式
CREATE TABLE IF NOT EXISTS profit_formula (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '公式名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '公式编码',
    expression TEXT NOT NULL COMMENT '公式表达式',
    caliber_type VARCHAR(20) NOT NULL COMMENT '口径类型：BOOK/ASSESS',
    version VARCHAR(20) DEFAULT '1.0' COMMENT '版本号',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='利润计算公式';

-- 成本分摊规则
CREATE TABLE IF NOT EXISTS cost_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    cost_type VARCHAR(20) NOT NULL COMMENT '成本类型：FTP/RISK/OP',
    dimension VARCHAR(20) NOT NULL COMMENT '分摊维度',
    factor VARCHAR(50) NOT NULL COMMENT '分摊因子',
    ratio DECIMAL(10,4) COMMENT '分摊比例',
    version VARCHAR(20) DEFAULT '1.0',
    effect_date DATE COMMENT '生效日期',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cost_type (cost_type)
) ENGINE=InnoDB COMMENT='成本分摊规则';

-- 预警规则
CREATE TABLE IF NOT EXISTS alert_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    alert_type VARCHAR(20) NOT NULL COMMENT '预警类型：PROFIT/COST/SUBJECT',
    metric_code VARCHAR(50) NOT NULL COMMENT '监控指标编码',
    threshold DECIMAL(10,2) NOT NULL COMMENT '阈值',
    threshold_type VARCHAR(20) DEFAULT 'PERCENT' COMMENT '阈值类型：PERCENT/ABSOLUTE',
    level VARCHAR(20) DEFAULT 'WARNING' COMMENT '预警等级：CRITICAL/WARNING',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='预警规则';

-- 预警记录
CREATE TABLE IF NOT EXISTS alert_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT COMMENT '关联规则ID',
    level VARCHAR(20) NOT NULL COMMENT '预警等级',
    content TEXT NOT NULL COMMENT '预警内容',
    dim_type VARCHAR(20) COMMENT '涉及维度类型',
    dim_id BIGINT COMMENT '涉及维度ID',
    dim_name VARCHAR(200) COMMENT '涉及维度名称',
    anomaly_value DECIMAL(10,2) COMMENT '异常幅度',
    ai_analysis TEXT COMMENT 'AI根因分析',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSING/CLOSED',
    handler VARCHAR(100) COMMENT '处理人',
    handle_note TEXT COMMENT '处理备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    handle_time DATETIME COMMENT '处理时间',
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='预警记录';

-- 系统配置
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(500) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='系统配置';

-- 指标库
CREATE TABLE IF NOT EXISTS indicator_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '指标编码',
    name VARCHAR(100) NOT NULL COMMENT '指标名称',
    category VARCHAR(20) NOT NULL COMMENT '指标分类',
    data_type VARCHAR(20) DEFAULT 'DECIMAL' COMMENT '数据类型',
    unit VARCHAR(20) COMMENT '单位',
    precision_val INT DEFAULT 2 COMMENT '精度',
    calc_formula TEXT COMMENT '计算公式',
    data_source VARCHAR(50) COMMENT '数据来源',
    source_field VARCHAR(50) COMMENT '来源字段',
    filter_condition TEXT COMMENT '筛选条件',
    pre_calc_periods VARCHAR(100) COMMENT '预计算周期',
    supported_dims TEXT COMMENT '支持的维度',
    description TEXT COMMENT '描述',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='指标库';

-- 指标预计算结果
CREATE TABLE IF NOT EXISTS indicator_pre_calc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_code VARCHAR(50) NOT NULL COMMENT '指标编码',
    calc_period VARCHAR(10) NOT NULL COMMENT '计算周期：MONTH/QUARTER/YEAR',
    period_value VARCHAR(20) NOT NULL COMMENT '周期值',
    dim_type VARCHAR(20) COMMENT '维度类型',
    dim_code VARCHAR(50) COMMENT '维度编码',
    dim_name VARCHAR(100) COMMENT '维度名称',
    calc_value DECIMAL(18,4) COMMENT '计算结果',
    calc_time DATETIME COMMENT '计算时间',
    status TINYINT DEFAULT 1 COMMENT '状态',
    INDEX idx_indicator (indicator_code),
    INDEX idx_period (calc_period, period_value)
) ENGINE=InnoDB COMMENT='指标预计算结果';

-- 自定义报表模板
CREATE TABLE IF NOT EXISTS custom_report_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    row_dims TEXT COMMENT '行维度配置',
    col_metrics TEXT COMMENT '列指标配置',
    filter_config TEXT COMMENT '筛选配置',
    sort_config TEXT COMMENT '排序配置',
    is_system TINYINT DEFAULT 0 COMMENT '是否系统模板',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='自定义报表模板';
