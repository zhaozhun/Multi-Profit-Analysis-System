# 数据仓库迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将数据仓库架构从ODS→DWS两层升级为ODS→DWD→DWS→ADS四层，统一所有维度主数据，实现ETL自动触发，确保所有页面数据一致。

**Architecture:** 原始明细表(loan_indicator_detail/deposit_indicator_detail)作为ODS层，新建DWD层(dwd_loan_detail/dwd_deposit_detail)清洗关联维度主数据，DWS层(dw_indicator_fact)按维度汇总指标，ADS层(前端页面)统一从数据仓库取数。通过数据库触发器实现ODS变更自动触发ETL重算。

**Tech Stack:** MySQL 8.0, Spring Boot, MyBatis-Plus, React, TypeScript, Ant Design

## Global Constraints

- 所有维度主数据使用自增ID做主键，业务编码做唯一键
- 机构编码8位：区域(2)+层级(1)+序号(5)
- 其他维度编码5位：类型(2)+序号(3)
- ETL使用INSERT IGNORE处理重复数据
- 所有页面统一从`/api/dw/*`接口取数
- 口径类型默认ASSESS（考核口径）

---

## 文件结构

### 数据库脚本
- `数据库脚本/dimension_master_data.sql` — 维度主数据表结构调整和初始数据
- `数据库脚本/dwd_tables.sql` — DWD层建表语句
- `数据库脚本/etl_procedure.sql` — ETL存储过程
- `数据库脚本/etl_triggers.sql` — 触发器
- `数据库脚本/fix_dimension_ids.sql` — 修正原始数据维度ID

### 后端代码
- `后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java` — 重构为调用存储过程
- `后端/src/main/java/com/multiprofit/service/impl/IndicatorPrecomputeServiceImpl.java` — 删除
- `后端/src/main/java/com/multiprofit/controller/IndicatorController.java` — 删除旧接口

### 前端代码
- `前端/src/services/api.ts` — 统一数据源
- `前端/src/utils/format.ts` — 单位格式化工具

---

## Task 1: 创建维度主数据

**Files:**
- Create: `数据库脚本/dimension_master_data.sql`

**Interfaces:**
- Produces: dw_dim_organization, dw_dim_biz_line, dw_dim_product, dw_dim_channel, dw_dim_manager 表结构和初始数据

- [ ] **Step 1: 创建维度主数据SQL脚本**

```sql
-- 数据库脚本/dimension_master_data.sql
-- 维度主数据表结构调整和初始数据
-- 创建时间：2026-06-30

-- 1. 机构维度表增加编码字段
ALTER TABLE dw_dim_organization ADD COLUMN org_code VARCHAR(10) NOT NULL AFTER id;
ALTER TABLE dw_dim_organization ADD UNIQUE KEY uk_code (org_code);

-- 插入机构初始数据
INSERT INTO dw_dim_organization (org_code, org_name, parent_id, level) VALUES
('00100001', '总行', 0, 1),
('01200001', '北京分行', 1, 2),
('02200001', '广州分行', 1, 2),
('03200001', '上海分行', 1, 2),
('04200001', '深圳分行', 1, 2),
('05200001', '杭州分行', 1, 2);

-- 2. 条线维度表增加编码字段
ALTER TABLE dw_dim_biz_line ADD COLUMN line_code VARCHAR(10) NOT NULL AFTER id;
ALTER TABLE dw_dim_biz_line ADD UNIQUE KEY uk_code (line_code);

-- 插入条线初始数据
INSERT INTO dw_dim_biz_line (line_code, line_name, status) VALUES
('01001', '金融市场条线', 1),
('02001', '对公条线', 1),
('03001', '零售条线', 1);

-- 3. 产品维度表增加编码字段
ALTER TABLE dw_dim_product ADD COLUMN product_code VARCHAR(10) NOT NULL AFTER id;
ALTER TABLE dw_dim_product ADD UNIQUE KEY uk_code (product_code);

-- 插入产品初始数据
INSERT INTO dw_dim_product (product_code, product_name, product_type, status) VALUES
('01001', '住房贷款', 'LOAN', 1),
('01002', '个人贷款', 'LOAN', 1),
('01003', '公司贷款', 'LOAN', 1),
('01004', '短期贷款', 'LOAN', 1),
('01005', '中长期贷款', 'LOAN', 1);

-- 4. 渠道维度表增加编码字段
ALTER TABLE dw_dim_channel ADD COLUMN channel_code VARCHAR(10) NOT NULL AFTER id;
ALTER TABLE dw_dim_channel ADD UNIQUE KEY uk_code (channel_code);

-- 插入渠道初始数据
INSERT INTO dw_dim_channel (channel_code, channel_name, status) VALUES
('01001', '线下渠道', 1),
('02001', '线上渠道', 1),
('03001', '网点渠道', 1);

-- 5. 客户经理维度表增加编码字段
ALTER TABLE dw_dim_manager ADD COLUMN manager_code VARCHAR(10) NOT NULL AFTER id;
ALTER TABLE dw_dim_manager ADD UNIQUE KEY uk_code (manager_code);

-- 插入客户经理初始数据
INSERT INTO dw_dim_manager (manager_code, manager_name, org_id, status) VALUES
('01001', '北京分行客户经理', 2, 1),
('02001', '广州分行客户经理', 3, 1),
('03001', '上海分行客户经理', 4, 1),
('04001', '深圳分行客户经理', 5, 1),
('05001', '杭州分行客户经理', 6, 1);
```

- [ ] **Step 2: 执行SQL脚本**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit < 数据库脚本/dimension_master_data.sql
```

- [ ] **Step 3: 验证数据插入**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "
SELECT '机构' as dim, COUNT(*) as cnt FROM dw_dim_organization
UNION ALL SELECT '条线', COUNT(*) FROM dw_dim_biz_line
UNION ALL SELECT '产品', COUNT(*) FROM dw_dim_product
UNION ALL SELECT '渠道', COUNT(*) FROM dw_dim_channel
UNION ALL SELECT '客户经理', COUNT(*) FROM dw_dim_manager;
"
```

Expected: 机构6条，条线3条，产品5条，渠道3条，客户经理5条

- [ ] **Step 4: Commit**

```bash
git add 数据库脚本/dimension_master_data.sql
git commit -m "feat: 创建维度主数据表结构和初始数据"
```

---

## Task 2: 修正原始数据维度ID

**Files:**
- Create: `数据库脚本/fix_dimension_ids.sql`

**Interfaces:**
- Consumes: dw_dim_organization, dw_dim_biz_line, dw_dim_product, dw_dim_channel, dw_dim_manager 主数据
- Produces: loan_indicator_detail, deposit_indicator_detail 维度ID已修正

- [ ] **Step 1: 创建修正SQL脚本**

```sql
-- 数据库脚本/fix_dimension_ids.sql
-- 修正原始数据中的维度ID
-- 创建时间：2026-06-30

-- 1. 修正机构org_id
CREATE TEMPORARY TABLE org_mapping AS
SELECT DISTINCT org_name, 
    CASE org_name
        WHEN '北京分行' THEN 2
        WHEN '广州分行' THEN 3
        WHEN '上海分行' THEN 4
        WHEN '深圳分行' THEN 5
        WHEN '杭州分行' THEN 6
    END as new_id
FROM loan_indicator_detail WHERE account_period='2026-06';

UPDATE loan_indicator_detail l JOIN org_mapping m ON l.org_name = m.org_name
SET l.org_id = m.new_id WHERE l.account_period='2026-06';

UPDATE deposit_indicator_detail d JOIN org_mapping m ON d.org_name = m.org_name
SET d.org_id = m.new_id WHERE d.account_period='2026-06';

DROP TEMPORARY TABLE org_mapping;

-- 2. 修正条线biz_line_id
CREATE TEMPORARY TABLE biz_line_mapping AS
SELECT DISTINCT biz_line_name, 
    CASE biz_line_name
        WHEN '金融市场条线' THEN 1
        WHEN '对公条线' THEN 2
        WHEN '零售条线' THEN 3
    END as new_id
FROM loan_indicator_detail WHERE account_period='2026-06';

UPDATE loan_indicator_detail l JOIN biz_line_mapping m ON l.biz_line_name = m.biz_line_name
SET l.biz_line_id = m.new_id WHERE l.account_period='2026-06';

UPDATE deposit_indicator_detail d JOIN biz_line_mapping m ON d.biz_line_name = m.biz_line_name
SET d.biz_line_id = m.new_id WHERE d.account_period='2026-06';

DROP TEMPORARY TABLE biz_line_mapping;

-- 3. 修正产品product_id
CREATE TEMPORARY TABLE product_mapping AS
SELECT DISTINCT product_name, 
    CASE product_name
        WHEN '住房贷款' THEN 1
        WHEN '个人贷款' THEN 2
        WHEN '公司贷款' THEN 3
        WHEN '短期贷款' THEN 4
        WHEN '中长期贷款' THEN 5
    END as new_id
FROM loan_indicator_detail WHERE account_period='2026-06';

UPDATE loan_indicator_detail l JOIN product_mapping m ON l.product_name = m.product_name
SET l.product_id = m.new_id WHERE l.account_period='2026-06';

UPDATE deposit_indicator_detail d JOIN product_mapping m ON d.product_name = m.product_name
SET d.product_id = m.new_id WHERE d.account_period='2026-06';

DROP TEMPORARY TABLE product_mapping;

-- 4. 修正渠道channel_id
CREATE TEMPORARY TABLE channel_mapping AS
SELECT DISTINCT channel_name, 
    CASE channel_name
        WHEN '线下渠道' THEN 1
        WHEN '线上渠道' THEN 2
        WHEN '网点渠道' THEN 3
    END as new_id
FROM loan_indicator_detail WHERE account_period='2026-06';

UPDATE loan_indicator_detail l JOIN channel_mapping m ON l.channel_name = m.channel_name
SET l.channel_id = m.new_id WHERE l.account_period='2026-06';

UPDATE deposit_indicator_detail d JOIN channel_mapping m ON d.channel_name = m.channel_name
SET d.channel_id = m.new_id WHERE d.account_period='2026-06';

DROP TEMPORARY TABLE channel_mapping;

-- 5. 修正客户经理manager_id
CREATE TEMPORARY TABLE manager_mapping AS
SELECT DISTINCT manager_name, 
    CASE manager_name
        WHEN '北京分行客户经理' THEN 1
        WHEN '广州分行客户经理' THEN 2
        WHEN '上海分行客户经理' THEN 3
        WHEN '深圳分行客户经理' THEN 4
        WHEN '杭州分行客户经理' THEN 5
    END as new_id
FROM loan_indicator_detail WHERE account_period='2026-06';

UPDATE loan_indicator_detail l JOIN manager_mapping m ON l.manager_name = m.manager_name
SET l.manager_id = m.new_id WHERE l.account_period='2026-06';

UPDATE deposit_indicator_detail d JOIN manager_mapping m ON d.manager_name = m.manager_name
SET d.manager_id = m.new_id WHERE d.account_period='2026-06';

DROP TEMPORARY TABLE manager_mapping;
```

- [ ] **Step 2: 执行修正脚本**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit < 数据库脚本/fix_dimension_ids.sql
```

- [ ] **Step 3: 验证修正结果**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "
-- 验证org_id已修正
SELECT org_id, org_name, COUNT(*) as cnt
FROM loan_indicator_detail 
WHERE account_period='2026-06'
GROUP BY org_id, org_name
ORDER BY org_id;
"
```

Expected: 每个org_id只对应一个org_name

- [ ] **Step 4: Commit**

```bash
git add 数据库脚本/fix_dimension_ids.sql
git commit -m "feat: 修正原始数据维度ID"
```

---

## Task 3: 创建DWD层表

**Files:**
- Create: `数据库脚本/dwd_tables.sql`

**Interfaces:**
- Produces: dwd_loan_detail, dwd_deposit_detail 表结构

- [ ] **Step 1: 创建DWD层建表脚本**

```sql
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
    
    -- 指标值
    loan_balance DECIMAL(18,4) COMMENT '贷款余额',
    loan_monthly_interest DECIMAL(18,4) COMMENT '月利息收入',
    ftp_cost DECIMAL(18,4) COMMENT 'FTP成本',
    risk_cost DECIMAL(18,4) COMMENT '风险成本',
    op_cost DECIMAL(18,4) COMMENT '运营成本',
    
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
    
    -- 指标值
    deposit_balance DECIMAL(18,4) COMMENT '存款余额',
    deposit_monthly_interest DECIMAL(18,4) COMMENT '月利息支出',
    ftp_income DECIMAL(18,4) COMMENT 'FTP收入',
    op_cost DECIMAL(18,4) COMMENT '运营成本',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz (biz_id, account_period),
    INDEX idx_period (account_period),
    INDEX idx_org (org_id),
    INDEX idx_biz_line (biz_line_id),
    INDEX idx_product (product_id),
    INDEX idx_channel (channel_id),
    INDEX idx_manager (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存款业务明细表(DWD层)';
```

- [ ] **Step 2: 执行建表脚本**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit < 数据库脚本/dwd_tables.sql
```

- [ ] **Step 3: 验证表创建**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "SHOW TABLES LIKE 'dwd_%';"
```

Expected: dwd_loan_detail, dwd_deposit_detail

- [ ] **Step 4: Commit**

```bash
git add 数据库脚本/dwd_tables.sql
git commit -m "feat: 创建DWD层明细表"
```

---

## Task 4: 创建ETL存储过程

**Files:**
- Create: `数据库脚本/etl_procedure.sql`

**Interfaces:**
- Produces: sp_etl_recalculate 存储过程

- [ ] **Step 1: 创建ETL存储过程脚本**

```sql
-- 数据库脚本/etl_procedure.sql
-- ETL存储过程
-- 创建时间：2026-06-30

DELIMITER //

DROP PROCEDURE IF EXISTS sp_etl_recalculate//

CREATE PROCEDURE sp_etl_recalculate(IN p_period VARCHAR(10))
BEGIN
    -- 1. 清除该账期的DWD数据
    DELETE FROM dwd_loan_detail WHERE account_period = p_period;
    DELETE FROM dwd_deposit_detail WHERE account_period = p_period;
    
    -- 2. 从ODS层清洗数据到DWD层（贷款）
    INSERT INTO dwd_loan_detail (
        biz_id, account_period, caliber_type,
        org_id, org_name, biz_line_id, biz_line_name,
        product_id, product_name, channel_id, channel_name,
        manager_id, manager_name, customer_id, customer_name,
        loan_balance, loan_monthly_interest, ftp_cost, risk_cost, op_cost
    )
    SELECT 
        l.biz_id, l.account_period, l.caliber_type,
        o.id, l.org_name, bl.id, l.biz_line_name,
        p.id, l.product_name, c.id, l.channel_name,
        m.id, l.manager_name, l.customer_id, l.customer_name,
        l.loan_balance, l.loan_monthly_interest, l.ftp_cost, l.risk_cost, l.op_cost
    FROM loan_indicator_detail l
    LEFT JOIN dw_dim_organization o ON l.org_name = o.org_name
    LEFT JOIN dw_dim_biz_line bl ON l.biz_line_name = bl.line_name
    LEFT JOIN dw_dim_product p ON l.product_name = p.product_name
    LEFT JOIN dw_dim_channel c ON l.channel_name = c.channel_name
    LEFT JOIN dw_dim_manager m ON l.manager_name = m.manager_name
    WHERE l.account_period = p_period;
    
    -- 3. 从ODS层清洗数据到DWD层（存款）
    INSERT INTO dwd_deposit_detail (
        biz_id, account_period, caliber_type,
        org_id, org_name, biz_line_id, biz_line_name,
        product_id, product_name, channel_id, channel_name,
        manager_id, manager_name, customer_id, customer_name,
        deposit_balance, deposit_monthly_interest, ftp_income, op_cost
    )
    SELECT 
        d.biz_id, d.account_period, d.caliber_type,
        o.id, d.org_name, bl.id, d.biz_line_name,
        p.id, d.product_name, c.id, d.channel_name,
        m.id, d.manager_name, d.customer_id, d.customer_name,
        d.deposit_balance, d.deposit_monthly_interest, d.ftp_income, d.op_cost
    FROM deposit_indicator_detail d
    LEFT JOIN dw_dim_organization o ON d.org_name = o.org_name
    LEFT JOIN dw_dim_biz_line bl ON d.biz_line_name = bl.line_name
    LEFT JOIN dw_dim_product p ON d.product_name = p.product_name
    LEFT JOIN dw_dim_channel c ON d.channel_name = c.channel_name
    LEFT JOIN dw_dim_manager m ON d.manager_name = m.manager_name
    WHERE d.account_period = p_period;
    
    -- 4. 清除该账期的DWS数据
    DELETE FROM dw_indicator_fact WHERE period = p_period;
    
    -- 5. 从DWD层按各维度计算指标到DWS层
    -- 5.1 按机构维度计算贷款指标
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'ORG', org_id, org_name, 
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;
    
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'FTP_COST', p_period, 'MONTH', 'ORG', org_id, org_name, 
           SUM(ftp_cost) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;
    
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'RISK_COST', p_period, 'MONTH', 'ORG', org_id, org_name, 
           SUM(risk_cost) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;
    
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'LOAN_PROFIT', p_period, 'MONTH', 'ORG', org_id, org_name, 
           SUM(loan_monthly_interest - ftp_cost - risk_cost - op_cost) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;
    
    -- 5.2 按机构维度计算存款指标
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'FTP_INCOME', p_period, 'MONTH', 'ORG', org_id, org_name, 
           SUM(ftp_income) / 10000, 'ASSESS', NOW()
    FROM dwd_deposit_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;
    
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'DEPOSIT_INTEREST', p_period, 'MONTH', 'ORG', org_id, org_name, 
           SUM(deposit_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_deposit_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;
    
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'DEPOSIT_PROFIT', p_period, 'MONTH', 'ORG', org_id, org_name, 
           SUM(ftp_income - deposit_monthly_interest - op_cost) / 10000, 'ASSESS', NOW()
    FROM dwd_deposit_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;
    
    -- 5.3 按条线维度计算
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'BIZ_LINE', biz_line_id, biz_line_name, 
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY biz_line_id, biz_line_name;
    
    -- 5.4 按产品维度计算
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'PRODUCT', product_id, product_name, 
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY product_id, product_name;
    
    -- 5.5 按渠道维度计算
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'CHANNEL', channel_id, channel_name, 
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY channel_id, channel_name;
    
    -- 5.6 按客户经理维度计算
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'MANAGER', manager_id, manager_name, 
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY manager_id, manager_name;
    
    -- 5.7 按客户维度计算
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'CUSTOMER', customer_id, customer_name, 
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY customer_id, customer_name;
    
    -- 5.8 计算TOTAL汇总
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'INTEREST_INCOME' AND period = p_period AND dim_type = 'ORG';
    
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'FTP_COST', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'FTP_COST' AND period = p_period AND dim_type = 'ORG';
    
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'RISK_COST', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'RISK_COST' AND period = p_period AND dim_type = 'ORG';
    
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'LOAN_PROFIT', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'LOAN_PROFIT' AND period = p_period AND dim_type = 'ORG';
    
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'FTP_INCOME', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'FTP_INCOME' AND period = p_period AND dim_type = 'ORG';
    
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'DEPOSIT_INTEREST', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'DEPOSIT_INTEREST' AND period = p_period AND dim_type = 'ORG';
    
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'DEPOSIT_PROFIT', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'DEPOSIT_PROFIT' AND period = p_period AND dim_type = 'ORG';
    
    -- 5.9 计算总利润TOTAL
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'TOTAL_PROFIT', p_period, 'MONTH', 'TOTAL', 0, '全部',
           (SELECT SUM(calc_value) FROM dw_indicator_fact WHERE indicator_code = 'LOAN_PROFIT' AND period = p_period AND dim_type = 'TOTAL')
           + (SELECT SUM(calc_value) FROM dw_indicator_fact WHERE indicator_code = 'DEPOSIT_PROFIT' AND period = p_period AND dim_type = 'TOTAL'),
           'ASSESS', NOW();
    
    -- 5.10 计算运营成本TOTAL
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'OP_COST', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(op_cost) / 10000, 'ASSESS', NOW()
    FROM (
        SELECT op_cost FROM dwd_loan_detail WHERE account_period = p_period
        UNION ALL
        SELECT op_cost FROM dwd_deposit_detail WHERE account_period = p_period
    ) t;
    
END//

DELIMITER ;
```

- [ ] **Step 2: 执行存储过程脚本**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit < 数据库脚本/etl_procedure.sql
```

- [ ] **Step 3: 验证存储过程创建**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "SHOW PROCEDURE STATUS WHERE Name = 'sp_etl_recalculate';"
```

Expected: 显示存储过程信息

- [ ] **Step 4: Commit**

```bash
git add 数据库脚本/etl_procedure.sql
git commit -m "feat: 创建ETL存储过程"
```

---

## Task 5: 创建数据库触发器

**Files:**
- Create: `数据库脚本/etl_triggers.sql`

**Interfaces:**
- Produces: trg_loan_after_insert, trg_loan_after_update, trg_loan_after_delete, trg_deposit_after_insert, trg_deposit_after_update, trg_deposit_after_delete 触发器

- [ ] **Step 1: 创建触发器脚本**

```sql
-- 数据库脚本/etl_triggers.sql
-- ETL触发器
-- 创建时间：2026-06-30

DELIMITER //

-- 删除已有触发器
DROP TRIGGER IF EXISTS trg_loan_after_insert//
DROP TRIGGER IF EXISTS trg_loan_after_update//
DROP TRIGGER IF EXISTS trg_loan_after_delete//
DROP TRIGGER IF EXISTS trg_deposit_after_insert//
DROP TRIGGER IF EXISTS trg_deposit_after_update//
DROP TRIGGER IF EXISTS trg_deposit_after_delete//

-- 贷款数据插入触发器
CREATE TRIGGER trg_loan_after_insert
AFTER INSERT ON loan_indicator_detail
FOR EACH ROW
BEGIN
    CALL sp_etl_recalculate(NEW.account_period);
END//

-- 贷款数据更新触发器
CREATE TRIGGER trg_loan_after_update
AFTER UPDATE ON loan_indicator_detail
FOR EACH ROW
BEGIN
    CALL sp_etl_recalculate(NEW.account_period);
END//

-- 贷款数据删除触发器
CREATE TRIGGER trg_loan_after_delete
AFTER DELETE ON loan_indicator_detail
FOR EACH ROW
BEGIN
    CALL sp_etl_recalculate(OLD.account_period);
END//

-- 存款数据插入触发器
CREATE TRIGGER trg_deposit_after_insert
AFTER INSERT ON deposit_indicator_detail
FOR EACH ROW
BEGIN
    CALL sp_etl_recalculate(NEW.account_period);
END//

-- 存款数据更新触发器
CREATE TRIGGER trg_deposit_after_update
AFTER UPDATE ON deposit_indicator_detail
FOR EACH ROW
BEGIN
    CALL sp_etl_recalculate(NEW.account_period);
END//

-- 存款数据删除触发器
CREATE TRIGGER trg_deposit_after_delete
AFTER DELETE ON deposit_indicator_detail
FOR EACH ROW
BEGIN
    CALL sp_etl_recalculate(OLD.account_period);
END//

DELIMITER ;
```

- [ ] **Step 2: 执行触发器脚本**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit < 数据库脚本/etl_triggers.sql
```

- [ ] **Step 3: 验证触发器创建**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "SHOW TRIGGERS WHERE Table IN ('loan_indicator_detail', 'deposit_indicator_detail');"
```

Expected: 显示6个触发器

- [ ] **Step 4: Commit**

```bash
git add 数据库脚本/etl_triggers.sql
git commit -m "feat: 创建ETL触发器"
```

---

## Task 6: 执行ETL并验证数据

**Files:**
- Modify: 数据库数据

**Interfaces:**
- Consumes: sp_etl_recalculate 存储过程
- Produces: dwd_loan_detail, dwd_deposit_detail, dw_indicator_fact 数据

- [ ] **Step 1: 清理旧的dw数据**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "DELETE FROM dw_indicator_fact WHERE period='2026-06';"
```

- [ ] **Step 2: 执行ETL**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "CALL sp_etl_recalculate('2026-06');"
```

- [ ] **Step 3: 验证DWD层数据**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "
SELECT 'dwd_loan_detail' as tbl, COUNT(*) as cnt FROM dwd_loan_detail WHERE account_period='2026-06'
UNION ALL SELECT 'dwd_deposit_detail', COUNT(*) FROM dwd_deposit_detail WHERE account_period='2026-06';
"
```

Expected: dwd_loan_detail 50条，dwd_deposit_detail 50条

- [ ] **Step 4: 验证DWS层数据准确性**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "
-- 对比原始数据和ETL结果
SELECT '原始贷款利息' as src, SUM(loan_monthly_interest)/10000 as val FROM loan_indicator_detail WHERE account_period='2026-06'
UNION ALL
SELECT 'ETL贷款利息TOTAL', calc_value FROM dw_indicator_fact WHERE period='2026-06' AND indicator_code='INTEREST_INCOME' AND dim_type='TOTAL';
"
```

Expected: 两个值应该一致（约199.27）

- [ ] **Step 5: 验证各维度数据**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "
SELECT dim_type, COUNT(*) as cnt
FROM dw_indicator_fact
WHERE period='2026-06' AND indicator_code='INTEREST_INCOME'
GROUP BY dim_type;
"
```

Expected: ORG 5条，BIZ_LINE 3条，PRODUCT 5条，CHANNEL 3条，MANAGER 5条，CUSTOMER 40条，TOTAL 1条

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: 执行ETL并验证数据准确性"
```

---

## Task 7: 重构后端ETL服务

**Files:**
- Modify: `后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java`
- Delete: `后端/src/main/java/com/multiprofit/service/impl/IndicatorPrecomputeServiceImpl.java`
- Modify: `后端/src/main/java/com/multiprofit/controller/IndicatorController.java`

**Interfaces:**
- Consumes: sp_etl_recalculate 存储过程
- Produces: DataWarehouseETLServiceImpl.executeETL() 调用存储过程

- [ ] **Step 1: 重构DataWarehouseETLServiceImpl**

```java
// 后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java
package com.multiprofit.service.impl;

import com.multiprofit.service.DataWarehouseETLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据仓库ETL服务实现
 * 调用数据库存储过程sp_etl_recalculate完成数据清洗和指标计算
 */
@Service
public class DataWarehouseETLServiceImpl implements DataWarehouseETLService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> executeETL(String period) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            // 调用ETL存储过程
            jdbcTemplate.execute("CALL sp_etl_recalculate('" + period + "')");
            
            // 查询结果统计
            Integer dwdLoanCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_loan_detail WHERE account_period = ?", 
                Integer.class, period);
            Integer dwdDepositCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_deposit_detail WHERE account_period = ?", 
                Integer.class, period);
            Integer dwFactCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dw_indicator_fact WHERE period = ?", 
                Integer.class, period);
            
            long endTime = System.currentTimeMillis();
            
            result.put("success", true);
            result.put("period", period);
            result.put("dwdLoanCount", dwdLoanCount);
            result.put("dwdDepositCount", dwdDepositCount);
            result.put("dwFactCount", dwFactCount);
            result.put("duration", endTime - startTime);
            result.put("message", "ETL执行成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "ETL执行失败: " + e.getMessage());
        }
        
        return result;
    }
}
```

- [ ] **Step 2: 删除IndicatorPrecomputeServiceImpl**

```bash
rm 后端/src/main/java/com/multiprofit/service/impl/IndicatorPrecomputeServiceImpl.java
```

- [ ] **Step 3: 删除IndicatorController中的旧接口**

从 `IndicatorController.java` 中删除 `getIndicatorPrecomputed` 方法（第205-215行左右）

- [ ] **Step 4: 编译验证**

```bash
cd 后端 && mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "refactor: 重构ETL服务调用存储过程，删除旧预计算代码"
```

---

## Task 8: 统一前端数据源

**Files:**
- Modify: `前端/src/services/api.ts`
- Create: `前端/src/utils/format.ts`

**Interfaces:**
- Produces: formatValue() 工具函数

- [ ] **Step 1: 创建单位格式化工具**

```typescript
// 前端/src/utils/format.ts
/**
 * 格式化数值显示
 * 根据数值大小自动选择单位：元、万元、亿元
 */
export function formatValue(value: number): { text: string; unit: string } {
    const absValue = Math.abs(value);
    
    if (absValue >= 100000000) {
        // >= 1亿：显示为亿元
        return {
            text: (value / 100000000).toFixed(2),
            unit: '亿元'
        };
    } else if (absValue >= 10000) {
        // >= 1万：显示为万元
        return {
            text: (value / 10000).toFixed(2),
            unit: '万元'
        };
    } else {
        // < 1万：显示为元
        return {
            text: value.toFixed(2),
            unit: '元'
        };
    }
}

/**
 * 格式化数值为带单位的字符串
 */
export function formatValueWithUnit(value: number): string {
    const { text, unit } = formatValue(value);
    return `${text} ${unit}`;
}
```

- [ ] **Step 2: 验证Dashboard页面数据源**

检查 `前端/src/pages/Dashboard/index.tsx` 确认已使用 `/api/dw/*` 接口

- [ ] **Step 3: 验证DimensionAnalysis页面数据源**

检查 `前端/src/pages/DimensionAnalysis/index.tsx` 确认已使用 `/api/dw/*` 接口

- [ ] **Step 4: 编译验证**

```bash
cd 前端 && npm run build
```

Expected: 构建成功

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: 添加数值格式化工具，统一前端数据源"
```

---

## Task 9: 端到端验证

**Files:**
- Modify: 无

**Interfaces:**
- Consumes: 所有已完成的任务

- [ ] **Step 1: 启动后端服务**

```bash
cd 后端 && mvn spring-boot:run
```

- [ ] **Step 2: 验证ETL API**

```bash
curl -X POST "http://localhost:8080/api/dw/etl/execute?period=2026-06"
```

Expected: 返回success: true

- [ ] **Step 3: 验证指标列表API**

```bash
curl "http://localhost:8080/api/dw/indicator/list"
```

Expected: 返回指标列表

- [ ] **Step 4: 验证指标汇总API**

```bash
curl "http://localhost:8080/api/dw/indicator/summary?period=2026-06&caliberType=ASSESS"
```

Expected: 返回指标汇总数据，INTEREST_INCOME值约199.27

- [ ] **Step 5: 验证维度分析API**

```bash
curl "http://localhost:8080/api/dw/indicator/dimension?period=2026-06&indicatorCode=INTEREST_INCOME&dimType=ORG&caliberType=ASSESS"
```

Expected: 返回5个机构的利息收入数据

- [ ] **Step 6: 启动前端并验证页面**

```bash
cd 前端 && npm start
```

访问 http://localhost:3000 验证：
1. Dashboard页面显示正确的指标值
2. 维度分析页面显示正确的维度数据
3. 指标数据页面显示正确的指标列表

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "test: 端到端验证数据仓库迁移"
```

---

## 自检清单

- [ ] 所有维度主数据已创建并初始化
- [ ] 原始数据维度ID已修正
- [ ] DWD层表已创建
- [ ] ETL存储过程已创建
- [ ] 触发器已创建
- [ ] ETL执行成功，数据准确
- [ ] 后端代码已重构
- [ ] 前端数据源已统一
- [ ] 端到端验证通过
