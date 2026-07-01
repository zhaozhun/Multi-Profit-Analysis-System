-- ============================================
-- Step 0: 创建数据库 + 全部表结构
-- 说明：IF NOT EXISTS 保证幂等，重复执行不会报错
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS multi_profit
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE multi_profit;

-- ============================================
-- 1. 维度主数据表
-- ============================================
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

-- ============================================
-- 2. 客户主数据表
-- ============================================
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

-- ============================================
-- 3. 业务明细台账（核心事实表）
-- ============================================
CREATE TABLE IF NOT EXISTS biz_ledger (
    biz_id VARCHAR(30) NOT NULL COMMENT '业务编号',
    stat_date DATE NOT NULL COMMENT '业务日期',
    account_period VARCHAR(7) NOT NULL COMMENT '账期月份',
    org_id BIGINT NOT NULL COMMENT '机构ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    biz_line_id BIGINT NOT NULL COMMENT '条线ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    channel_id BIGINT NOT NULL COMMENT '渠道ID',
    manager_id BIGINT NOT NULL COMMENT '客户经理ID',
    customer_id BIGINT COMMENT '客户ID',
    product_type VARCHAR(20) NOT NULL COMMENT '产品类型：LOAN/DEPOSIT',
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
    loan_revenue DECIMAL(18,2) DEFAULT 0,
    loan_ftp_cost DECIMAL(18,2) DEFAULT 0,
    loan_risk_cost DECIMAL(18,2) DEFAULT 0,
    loan_op_cost DECIMAL(18,2) DEFAULT 0,
    loan_profit DECIMAL(18,2) DEFAULT 0,
    deposit_revenue DECIMAL(18,2) DEFAULT 0,
    deposit_interest DECIMAL(18,2) DEFAULT 0,
    deposit_op_cost DECIMAL(18,2) DEFAULT 0,
    deposit_profit DECIMAL(18,2) DEFAULT 0,
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

-- ============================================
-- 4. 原子指标表
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
-- 5. 派生指标表
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
-- 6. 统计口径表
-- ============================================
CREATE TABLE IF NOT EXISTS indicator_stat_config (
    indicator_code VARCHAR(50) COMMENT '指标编码',
    stat_type VARCHAR(20) COMMENT '统计口径：MONTHLY_DAILY_AVG/YEARLY_DAILY_AVG',
    calc_formula TEXT COMMENT '计算公式',
    description VARCHAR(200) COMMENT '描述',
    PRIMARY KEY (indicator_code, stat_type)
) COMMENT='统计口径表';

-- ============================================
-- 7. 指标定义表
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
-- 8. 指标预计算结果表（indicator_pre_calc）
-- ============================================
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
) COMMENT='指标预计算结果表';

-- ============================================
-- 9. 指标预计算结果表（indicator_precomputed，另一套）
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
-- 10. 指标汇总表
-- ============================================
CREATE TABLE IF NOT EXISTS indicator_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '账期',
    indicator_code VARCHAR(50) NOT NULL COMMENT '指标编码',
    indicator_type VARCHAR(20) COMMENT '指标类型：ATOMIC/DERIVED',
    business_line VARCHAR(20) COMMENT '业务条线',
    calc_value DECIMAL(18,4) COMMENT '汇总值',
    calc_time DATETIME COMMENT '计算时间',
    status TINYINT DEFAULT 1 COMMENT '状态',
    INDEX idx_period (period),
    INDEX idx_indicator (indicator_code)
) COMMENT='指标汇总表';

-- ============================================
-- 11. 贷款指标明细表（ODS层）
-- ============================================
CREATE TABLE IF NOT EXISTS loan_indicator_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_id VARCHAR(50) NOT NULL COMMENT '业务ID',
    stat_date DATE COMMENT '业务日期',
    account_period VARCHAR(10) NOT NULL COMMENT '账期',
    customer_id BIGINT COMMENT '客户ID',
    customer_name VARCHAR(200) COMMENT '客户名称',
    org_id BIGINT COMMENT '机构ID',
    org_name VARCHAR(200) COMMENT '机构名称',
    biz_line_id BIGINT COMMENT '条线ID',
    biz_line_name VARCHAR(200) COMMENT '条线名称',
    dept_id BIGINT COMMENT '部门ID',
    dept_name VARCHAR(200) COMMENT '部门名称',
    product_id BIGINT COMMENT '产品ID',
    product_name VARCHAR(200) COMMENT '产品名称',
    channel_id BIGINT COMMENT '渠道ID',
    channel_name VARCHAR(200) COMMENT '渠道名称',
    manager_id BIGINT COMMENT '客户经理ID',
    manager_name VARCHAR(200) COMMENT '客户经理名称',
    loan_balance DECIMAL(18,4) COMMENT '贷款余额',
    loan_rate DECIMAL(10,6) COMMENT '贷款利率',
    loan_interest_calc_type VARCHAR(20) COMMENT '计息方式',
    loan_daily_interest DECIMAL(18,4) COMMENT '当日利息',
    loan_monthly_interest DECIMAL(18,4) COMMENT '当月利息',
    loan_cumulative_interest DECIMAL(18,4) COMMENT '累计利息',
    ftp_rate DECIMAL(10,6) COMMENT 'FTP利率',
    ftp_cost DECIMAL(18,4) COMMENT 'FTP成本',
    risk_cost DECIMAL(18,4) COMMENT '风险成本',
    op_cost DECIMAL(18,4) COMMENT '运营成本',
    expense_type VARCHAR(20) COMMENT '费用类型',
    caliber_type VARCHAR(10) DEFAULT 'ASSESS' COMMENT '口径',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_period (account_period),
    INDEX idx_org (org_id),
    INDEX idx_product (product_id),
    INDEX idx_biz_line (biz_line_id),
    INDEX idx_channel (channel_id),
    INDEX idx_manager (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款指标明细表(ODS层)';

-- ============================================
-- 12. 存款指标明细表（ODS层）
-- ============================================
CREATE TABLE IF NOT EXISTS deposit_indicator_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_id VARCHAR(50) NOT NULL COMMENT '业务ID',
    stat_date DATE COMMENT '业务日期',
    account_period VARCHAR(10) NOT NULL COMMENT '账期',
    customer_id BIGINT COMMENT '客户ID',
    customer_name VARCHAR(200) COMMENT '客户名称',
    org_id BIGINT COMMENT '机构ID',
    org_name VARCHAR(200) COMMENT '机构名称',
    biz_line_id BIGINT COMMENT '条线ID',
    biz_line_name VARCHAR(200) COMMENT '条线名称',
    dept_id BIGINT COMMENT '部门ID',
    dept_name VARCHAR(200) COMMENT '部门名称',
    product_id BIGINT COMMENT '产品ID',
    product_name VARCHAR(200) COMMENT '产品名称',
    channel_id BIGINT COMMENT '渠道ID',
    channel_name VARCHAR(200) COMMENT '渠道名称',
    manager_id BIGINT COMMENT '客户经理ID',
    manager_name VARCHAR(200) COMMENT '客户经理名称',
    deposit_balance DECIMAL(18,4) COMMENT '存款余额',
    deposit_rate DECIMAL(10,6) COMMENT '存款利率',
    deposit_interest_calc_type VARCHAR(20) COMMENT '计息方式',
    deposit_daily_interest DECIMAL(18,4) COMMENT '当日利息',
    deposit_monthly_interest DECIMAL(18,4) COMMENT '当月利息',
    deposit_cumulative_interest DECIMAL(18,4) COMMENT '累计利息',
    ftp_rate DECIMAL(10,6) COMMENT 'FTP利率',
    ftp_income DECIMAL(18,4) COMMENT 'FTP收入',
    op_cost DECIMAL(18,4) COMMENT '运营成本',
    expense_type VARCHAR(20) COMMENT '费用类型',
    caliber_type VARCHAR(10) DEFAULT 'ASSESS' COMMENT '口径',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_period (account_period),
    INDEX idx_org (org_id),
    INDEX idx_product (product_id),
    INDEX idx_biz_line (biz_line_id),
    INDEX idx_channel (channel_id),
    INDEX idx_manager (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存款指标明细表(ODS层)';

-- ============================================
-- 13. 贷款业务明细表（DWD层）
-- ============================================
CREATE TABLE IF NOT EXISTS dwd_loan_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_id VARCHAR(50) NOT NULL COMMENT '业务ID',
    account_period VARCHAR(10) NOT NULL COMMENT '账期',
    caliber_type VARCHAR(10) DEFAULT 'ASSESS' COMMENT '口径类型',
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
    loan_balance DECIMAL(18,4) COMMENT '贷款余额',
    loan_monthly_interest DECIMAL(18,4) COMMENT '月利息收入',
    ftp_cost DECIMAL(18,4) COMMENT 'FTP成本',
    risk_cost DECIMAL(18,4) COMMENT '风险成本',
    op_cost DECIMAL(18,4) COMMENT '运营成本',
    loan_profit DECIMAL(18,4) COMMENT '贷款利润',
    net_interest_margin DECIMAL(18,4) COMMENT '净利差',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz (biz_id, account_period),
    INDEX idx_period (account_period),
    INDEX idx_org (org_id),
    INDEX idx_biz_line (biz_line_id),
    INDEX idx_product (product_id),
    INDEX idx_channel (channel_id),
    INDEX idx_manager (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款业务明细表(DWD层)';

-- ============================================
-- 14. 存款业务明细表（DWD层）
-- ============================================
CREATE TABLE IF NOT EXISTS dwd_deposit_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_id VARCHAR(50) NOT NULL COMMENT '业务ID',
    account_period VARCHAR(10) NOT NULL COMMENT '账期',
    caliber_type VARCHAR(10) DEFAULT 'ASSESS' COMMENT '口径类型',
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
    deposit_balance DECIMAL(18,4) COMMENT '存款余额',
    deposit_monthly_interest DECIMAL(18,4) COMMENT '月利息支出',
    ftp_income DECIMAL(18,4) COMMENT 'FTP收入',
    op_cost DECIMAL(18,4) COMMENT '运营成本',
    deposit_profit DECIMAL(18,4) COMMENT '存款利润',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz (biz_id, account_period),
    INDEX idx_period (account_period),
    INDEX idx_org (org_id),
    INDEX idx_biz_line (biz_line_id),
    INDEX idx_product (product_id),
    INDEX idx_channel (channel_id),
    INDEX idx_manager (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存款业务明细表(DWD层)';

-- ============================================
-- 15. 指标事实表（DWS层）
-- ============================================
CREATE TABLE IF NOT EXISTS dw_indicator_fact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    indicator_code VARCHAR(50) NOT NULL COMMENT '指标编码',
    period VARCHAR(10) NOT NULL COMMENT '期间',
    period_type VARCHAR(20) NOT NULL COMMENT '期间类型',
    dim_type VARCHAR(20) NOT NULL COMMENT '维度类型',
    dim_id BIGINT NOT NULL COMMENT '维度ID',
    dim_name VARCHAR(200) COMMENT '维度名称',
    calc_value DECIMAL(18,4) COMMENT '指标值',
    caliber_type VARCHAR(10) DEFAULT 'ASSESS' COMMENT '口径类型',
    calc_time TIMESTAMP COMMENT '计算时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_calc (indicator_code, period, period_type, dim_type, dim_id, caliber_type),
    INDEX idx_period (period),
    INDEX idx_indicator (indicator_code),
    INDEX idx_dim (dim_type, dim_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标事实表(DWS层)';

-- ============================================
-- 16. 数据仓库维度表
-- ============================================
CREATE TABLE IF NOT EXISTS dw_dim_organization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_code VARCHAR(50) NOT NULL COMMENT '机构编码',
    org_name VARCHAR(200) NOT NULL COMMENT '机构名称',
    parent_id BIGINT DEFAULT 0 COMMENT '上级机构ID',
    level INT DEFAULT 1 COMMENT '层级',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (org_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构维度表';

CREATE TABLE IF NOT EXISTS dw_dim_biz_line (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    line_code VARCHAR(50) NOT NULL COMMENT '条线编码',
    line_name VARCHAR(200) NOT NULL COMMENT '条线名称',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (line_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='条线维度表';

CREATE TABLE IF NOT EXISTS dw_dim_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_code VARCHAR(50) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(200) NOT NULL COMMENT '产品名称',
    product_type VARCHAR(20) COMMENT '产品类型',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品维度表';

CREATE TABLE IF NOT EXISTS dw_dim_channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_code VARCHAR(50) NOT NULL COMMENT '渠道编码',
    channel_name VARCHAR(200) NOT NULL COMMENT '渠道名称',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道维度表';

CREATE TABLE IF NOT EXISTS dw_dim_manager (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    manager_code VARCHAR(50) NOT NULL COMMENT '客户经理编码',
    manager_name VARCHAR(200) NOT NULL COMMENT '客户经理名称',
    org_id BIGINT COMMENT '所属机构ID',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (manager_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户经理维度表';

CREATE TABLE IF NOT EXISTS dw_dim_customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_code VARCHAR(50) NOT NULL COMMENT '客户编码',
    customer_name VARCHAR(200) NOT NULL COMMENT '客户名称',
    customer_type VARCHAR(20) COMMENT '客户类型',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户维度表';

-- ============================================
-- 17. 利润计算公式表
-- ============================================
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

-- ============================================
-- 18. 成本分摊规则表
-- ============================================
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

-- ============================================
-- 19. 预警规则表
-- ============================================
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

-- ============================================
-- 20. 预警记录表
-- ============================================
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
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '处理状态',
    handler VARCHAR(100) COMMENT '处理人',
    handle_note TEXT COMMENT '处理备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    handle_time DATETIME COMMENT '处理时间',
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='预警记录';

-- ============================================
-- 21. 指标库表
-- ============================================
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

-- ============================================
-- 22. 自定义报表模板表
-- ============================================
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

-- ============================================
-- 23. 费用分摊结果表
-- ============================================
CREATE TABLE IF NOT EXISTS cost_allocation_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no VARCHAR(50) COMMENT '批次号',
    period VARCHAR(10) COMMENT '账期',
    cost_code VARCHAR(50) COMMENT '费用编码',
    cost_name VARCHAR(100) COMMENT '费用名称',
    cost_category VARCHAR(20) COMMENT '费用类别',
    original_amount DECIMAL(18,2) COMMENT '原始金额',
    target_type VARCHAR(20) COMMENT '目标类型',
    target_code VARCHAR(50) COMMENT '目标编码',
    target_name VARCHAR(200) COMMENT '目标名称',
    dept_code VARCHAR(50) COMMENT '部门编码',
    org_code VARCHAR(50) COMMENT '机构编码',
    allocated_amount DECIMAL(18,2) COMMENT '分摊金额',
    allocation_method VARCHAR(50) COMMENT '分摊方法',
    allocation_factor VARCHAR(50) COMMENT '分摊因子',
    factor_value DECIMAL(18,4) COMMENT '因子值',
    factor_ratio DECIMAL(10,4) COMMENT '因子比例',
    status VARCHAR(20) DEFAULT 'CALCULATED' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='费用分摊结果表';

-- ============================================
-- 24. 费用类型配置表
-- ============================================
CREATE TABLE IF NOT EXISTS cost_type_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cost_type_code VARCHAR(50) NOT NULL COMMENT '费用类型编码',
    cost_type_name VARCHAR(100) NOT NULL COMMENT '费用类型名称',
    parent_code VARCHAR(50) COMMENT '父级编码',
    level INT DEFAULT 1 COMMENT '层级',
    description VARCHAR(500) COMMENT '描述',
    default_algorithm VARCHAR(50) COMMENT '默认算法',
    default_factor VARCHAR(50) COMMENT '默认因子',
    accounting_code VARCHAR(50) COMMENT '会计科目编码',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='费用类型配置表';

-- ============================================
-- 25. 费用原始记录表
-- ============================================
CREATE TABLE IF NOT EXISTS cost_actual_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) COMMENT '账期',
    cost_code VARCHAR(50) COMMENT '费用编码',
    cost_name VARCHAR(100) COMMENT '费用名称',
    cost_type VARCHAR(20) COMMENT '费用类型',
    amount DECIMAL(18,2) COMMENT '金额',
    dept_code VARCHAR(50) COMMENT '部门编码',
    org_code VARCHAR(50) COMMENT '机构编码',
    vendor VARCHAR(200) COMMENT '供应商',
    invoice_no VARCHAR(50) COMMENT '发票号',
    occurrence_date DATE COMMENT '发生日期',
    description VARCHAR(500) COMMENT '说明',
    status VARCHAR(20) DEFAULT 'CONFIRMED' COMMENT '状态',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='费用原始记录表';

-- 完成
SELECT 'Step 0 完成：数据库和全部表结构已创建' AS result;
