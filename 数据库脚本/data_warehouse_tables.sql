-- 数据仓库表结构
-- 创建时间：2026-06-30

-- 指标事实表（统一存储所有指标）
CREATE TABLE IF NOT EXISTS dw_indicator_fact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    indicator_code VARCHAR(50) NOT NULL COMMENT '指标编码',
    period VARCHAR(10) NOT NULL COMMENT '期间（2025-06）',
    period_type VARCHAR(20) NOT NULL COMMENT '期间类型（MONTH）',
    dim_type VARCHAR(20) NOT NULL COMMENT '维度类型（ORG/BIZ_LINE/PRODUCT等）',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标事实表';

-- 机构维度表
CREATE TABLE IF NOT EXISTS dw_dim_organization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_code VARCHAR(50) NOT NULL COMMENT '机构编码',
    org_name VARCHAR(200) NOT NULL COMMENT '机构名称',
    parent_id BIGINT DEFAULT 0 COMMENT '上级机构ID',
    level INT DEFAULT 1 COMMENT '层级',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用，0禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (org_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构维度表';

-- 条线维度表
CREATE TABLE IF NOT EXISTS dw_dim_biz_line (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    line_code VARCHAR(50) NOT NULL COMMENT '条线编码',
    line_name VARCHAR(200) NOT NULL COMMENT '条线名称',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用，0禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (line_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='条线维度表';

-- 产品维度表
CREATE TABLE IF NOT EXISTS dw_dim_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_code VARCHAR(50) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(200) NOT NULL COMMENT '产品名称',
    product_type VARCHAR(20) COMMENT '产品类型',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用，0禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品维度表';

-- 渠道维度表
CREATE TABLE IF NOT EXISTS dw_dim_channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_code VARCHAR(50) NOT NULL COMMENT '渠道编码',
    channel_name VARCHAR(200) NOT NULL COMMENT '渠道名称',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用，0禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道维度表';

-- 客户经理维度表
CREATE TABLE IF NOT EXISTS dw_dim_manager (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    manager_code VARCHAR(50) NOT NULL COMMENT '客户经理编码',
    manager_name VARCHAR(200) NOT NULL COMMENT '客户经理名称',
    org_id BIGINT COMMENT '所属机构ID',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用，0禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (manager_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户经理维度表';

-- 客户维度表
CREATE TABLE IF NOT EXISTS dw_dim_customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_code VARCHAR(50) NOT NULL COMMENT '客户编码',
    customer_name VARCHAR(200) NOT NULL COMMENT '客户名称',
    customer_type VARCHAR(20) COMMENT '客户类型',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用，0禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户维度表';
