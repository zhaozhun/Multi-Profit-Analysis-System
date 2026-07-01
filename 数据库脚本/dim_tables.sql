-- 数据库脚本/dim_tables.sql
-- 新建 dim_* 7张规范表 + 从dimension_master迁移数据 + 删除旧维度表
-- 统一结构: id/code/name/parent_id/level/sort_order/status/create_time/update_time

-- ============================================
-- 1. 机构维度
-- ============================================
CREATE TABLE dim_organization (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_parent (parent_id)
) COMMENT='机构维度表';

-- 2. 条线维度
CREATE TABLE dim_biz_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_parent (parent_id)
) COMMENT='条线维度表';

-- 3. 部门维度
CREATE TABLE dim_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_parent (parent_id)
) COMMENT='部门维度表';

-- 4. 产品维度
CREATE TABLE dim_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_parent (parent_id)
) COMMENT='产品维度表';

-- 5. 渠道维度
CREATE TABLE dim_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_parent (parent_id)
) COMMENT='渠道维度表';

-- 6. 客户经理维度
CREATE TABLE dim_manager (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_parent (parent_id)
) COMMENT='客户经理维度表';

-- 7. 客户分类维度
CREATE TABLE dim_customer_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_parent (parent_id)
) COMMENT='客户分类维度表';

-- ============================================
-- 从dimension_master迁移数据
-- ============================================

INSERT INTO dim_organization (id, code, name, parent_id, level, sort_order)
SELECT id, code, name, parent_id, level, sort_order
FROM dimension_master WHERE dim_type='ORG';

INSERT INTO dim_biz_line (id, code, name, parent_id, level, sort_order)
SELECT id, code, name, parent_id, level, sort_order
FROM dimension_master WHERE dim_type='BIZ_LINE';

INSERT INTO dim_dept (id, code, name, parent_id, level, sort_order)
SELECT id, code, name, parent_id, level, sort_order
FROM dimension_master WHERE dim_type='DEPT';

INSERT INTO dim_product (id, code, name, parent_id, level, sort_order)
SELECT id, code, name, parent_id, level, sort_order
FROM dimension_master WHERE dim_type='PRODUCT';

INSERT INTO dim_channel (id, code, name, parent_id, level, sort_order)
SELECT id, code, name, parent_id, level, sort_order
FROM dimension_master WHERE dim_type='CHANNEL';

INSERT INTO dim_manager (id, code, name, parent_id, level, sort_order)
SELECT id, code, name, parent_id, level, sort_order
FROM dimension_master WHERE dim_type='MANAGER';

INSERT INTO dim_customer_type (id, code, name, parent_id, level, sort_order)
SELECT id, code, name, parent_id, level, sort_order
FROM dimension_master WHERE dim_type='CUSTOMER';

-- ============================================
-- 迁移验证通过后,删除旧表
-- ============================================
DROP TABLE IF EXISTS dimension_master;
DROP TABLE IF EXISTS dw_dim_organization;
DROP TABLE IF EXISTS dw_dim_biz_line;
DROP TABLE IF EXISTS dw_dim_product;
DROP TABLE IF EXISTS dw_dim_channel;
DROP TABLE IF EXISTS dw_dim_manager;
DROP TABLE IF EXISTS dw_dim_customer;
