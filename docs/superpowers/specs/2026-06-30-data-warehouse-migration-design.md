# 数据仓库迁移方案设计

## 一、背景与目标

### 当前问题
1. **数据不一致**：Dashboard、维度分析、指标数据页面取数来源不同，导致同一指标显示不同值
2. **ETL数据丢失**：原始数据中同一org_id对应多个org_name，INSERT IGNORE导致数据丢失
3. **手动触发ETL**：修改原始数据后需要手动执行ETL，无法自动同步
4. **旧预计算层冗余**：indicator_precomputed与dw_indicator_fact并存，职责不清

### 设计目标
1. **统一数据源**：所有页面从数据仓库取数，确保数据一致
2. **自动同步**：原始数据变更后自动触发ETL重算
3. **分层清晰**：ODS→DWD→DWS→ADS，各层职责明确
4. **可维护性**：代码结构清晰，废弃旧逻辑

---

## 二、数据仓库分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                      ADS 应用层                              │
│  Dashboard / 维度分析 / 指标数据 / 指标库                      │
│  (从DWS层读取预计算结果)                                      │
└─────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────┐
│                      DWS 汇总层                              │
│  dw_indicator_fact (指标事实表)                               │
│  dw_dim_* (维度表)                                           │
│  按维度汇总的指标数据，供前端直接查询                            │
└─────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────┐
│                      DWD 明细层                              │
│  dwd_loan_detail / dwd_deposit_detail                        │
│  清洗、标准化后的业务明细数据，关联维度主数据                     │
└─────────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────────┐
│                      ODS 原始层                              │
│  loan_indicator_detail / deposit_indicator_detail            │
│  原始业务数据，作为数据仓库的最底层                              │
└─────────────────────────────────────────────────────────────┘
```

### 各层职责

| 层级 | 表名 | 职责 | 数据来源 |
|------|------|------|----------|
| **ODS** | loan_indicator_detail, deposit_indicator_detail | 存储原始业务数据 | 业务系统 |
| **DWD** | dwd_loan_detail, dwd_deposit_detail | 清洗、标准化、关联维度 | ODS层 |
| **DWS** | dw_indicator_fact, dw_dim_* | 按维度汇总计算指标 | DWD层 |
| **ADS** | 前端页面 | 展示指标、维度分析 | DWS层 |

---

## 三、机构主数据设计

### 表结构

```sql
CREATE TABLE dw_dim_organization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,  -- 自增主键
    org_code VARCHAR(10) NOT NULL,         -- 8位编码: 区域(2)+层级(1)+序号(5)
    org_name VARCHAR(200) NOT NULL,        -- 机构名称
    parent_id BIGINT DEFAULT 0,            -- 父机构ID
    level INT DEFAULT 1,                   -- 层级: 1=总行, 2=分行, 3=支行
    status TINYINT DEFAULT 1,              -- 状态
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (org_code)
);
```

### 编码规则

| 层级 | 编码格式 | 示例 |
|------|----------|------|
| 总行(1级) | 00-1-00001 | 00100001 |
| 分行(2级) | XX-2-00001 | 01200001(北京), 02200001(广州) |
| 支行(3级) | XX-3-00001 | 02300001(天河支行) |

### 初始数据

```sql
-- 总行
INSERT INTO dw_dim_organization (org_code, org_name, parent_id, level) 
VALUES ('00100001', '总行', 0, 1);

-- 分行
INSERT INTO dw_dim_organization (org_code, org_name, parent_id, level) VALUES
('01200001', '北京分行', 1, 2),
('02200001', '广州分行', 1, 2),
('03200001', '上海分行', 1, 2),
('04200001', '深圳分行', 1, 2),
('05200001', '杭州分行', 1, 2);
```

---

## 四、其他维度主数据设计

### 条线主数据 (dw_dim_biz_line)

```sql
-- 调整表结构，增加编码字段
ALTER TABLE dw_dim_biz_line ADD COLUMN line_code VARCHAR(10) NOT NULL AFTER id;
ALTER TABLE dw_dim_biz_line ADD UNIQUE KEY uk_code (line_code);

-- 编码规则：2位类型+3位序号
-- 01=金融市场, 02=对公, 03=零售
INSERT INTO dw_dim_biz_line (line_code, line_name, status) VALUES
('01001', '金融市场条线', 1),
('02001', '对公条线', 1),
('03001', '零售条线', 1);
```

### 产品主数据 (dw_dim_product)

```sql
-- 调整表结构
ALTER TABLE dw_dim_product ADD COLUMN product_code VARCHAR(10) NOT NULL AFTER id;
ALTER TABLE dw_dim_product ADD UNIQUE KEY uk_code (product_code);

-- 编码规则：2位类型+3位序号
-- 01=贷款类, 02=存款类
INSERT INTO dw_dim_product (product_code, product_name, product_type, status) VALUES
('01001', '住房贷款', 'LOAN', 1),
('01002', '个人贷款', 'LOAN', 1),
('01003', '公司贷款', 'LOAN', 1),
('01004', '短期贷款', 'LOAN', 1),
('01005', '中长期贷款', 'LOAN', 1);
```

### 渠道主数据 (dw_dim_channel)

```sql
-- 调整表结构
ALTER TABLE dw_dim_channel ADD COLUMN channel_code VARCHAR(10) NOT NULL AFTER id;
ALTER TABLE dw_dim_channel ADD UNIQUE KEY uk_code (channel_code);

-- 编码规则：2位类型+3位序号
-- 01=线下, 02=线上, 03=网点
INSERT INTO dw_dim_channel (channel_code, channel_name, status) VALUES
('01001', '线下渠道', 1),
('02001', '线上渠道', 1),
('03001', '网点渠道', 1);
```

### 客户经理主数据 (dw_dim_manager)

```sql
-- 调整表结构
ALTER TABLE dw_dim_manager ADD COLUMN manager_code VARCHAR(10) NOT NULL AFTER id;
ALTER TABLE dw_dim_manager ADD UNIQUE KEY uk_code (manager_code);

-- 编码规则：2位区域+3位序号
-- 需要关联所属机构
INSERT INTO dw_dim_manager (manager_code, manager_name, org_id, status) VALUES
('01001', '北京分行客户经理', 2, 1),  -- 北京分行
('02001', '广州分行客户经理', 3, 1),  -- 广州分行
('03001', '上海分行客户经理', 4, 1),  -- 上海分行
('04001', '深圳分行客户经理', 5, 1),  -- 深圳分行
('05001', '杭州分行客户经理', 6, 1);  -- 杭州分行
```

### 维度主数据汇总

| 维度 | 表名 | 编码规则 | 初始数据量 | 特殊字段 |
|------|------|----------|-----------|----------|
| 机构 | dw_dim_organization | 区域(2)+层级(1)+序号(5) | 6条 | parent_id, level |
| 条线 | dw_dim_biz_line | 类型(2)+序号(3) | 3条 | - |
| 产品 | dw_dim_product | 类型(2)+序号(3) | 5条 | product_type |
| 渠道 | dw_dim_channel | 类型(2)+序号(3) | 3条 | - |
| 客户经理 | dw_dim_manager | 区域(2)+序号(3) | 5条 | org_id |

---

## 五、DWD层设计（明细层）

### 贷款业务明细表

```sql
CREATE TABLE dwd_loan_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_id VARCHAR(50) NOT NULL,           -- 业务ID
    account_period VARCHAR(10) NOT NULL,   -- 账期
    caliber_type VARCHAR(10) DEFAULT 'ASSESS', -- 口径
    
    -- 维度信息（关联主数据）
    org_id BIGINT NOT NULL,                -- 机构ID（修正后）
    org_name VARCHAR(200),                 -- 机构名称
    biz_line_id BIGINT,                    -- 条线ID
    biz_line_name VARCHAR(200),            -- 条线名称
    product_id BIGINT,                     -- 产品ID
    product_name VARCHAR(200),             -- 产品名称
    channel_id BIGINT,                     -- 渠道ID
    channel_name VARCHAR(200),             -- 渠道名称
    manager_id BIGINT,                     -- 客户经理ID
    manager_name VARCHAR(200),             -- 客户经理名称
    customer_id BIGINT,                    -- 客户ID
    customer_name VARCHAR(200),            -- 客户名称
    
    -- 指标值
    loan_balance DECIMAL(18,4),            -- 贷款余额
    loan_monthly_interest DECIMAL(18,4),   -- 月利息收入
    ftp_cost DECIMAL(18,4),                -- FTP成本
    risk_cost DECIMAL(18,4),               -- 风险成本
    op_cost DECIMAL(18,4),                 -- 运营成本
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz (biz_id, account_period)
);
```

### 存款业务明细表

```sql
CREATE TABLE dwd_deposit_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_id VARCHAR(50) NOT NULL,           -- 业务ID
    account_period VARCHAR(10) NOT NULL,   -- 账期
    caliber_type VARCHAR(10) DEFAULT 'ASSESS', -- 口径
    
    -- 维度信息（关联主数据）
    org_id BIGINT NOT NULL,                -- 机构ID（修正后）
    org_name VARCHAR(200),                 -- 机构名称
    biz_line_id BIGINT,                    -- 条线ID
    biz_line_name VARCHAR(200),            -- 条线名称
    product_id BIGINT,                     -- 产品ID
    product_name VARCHAR(200),             -- 产品名称
    channel_id BIGINT,                     -- 渠道ID
    channel_name VARCHAR(200),             -- 渠道名称
    manager_id BIGINT,                     -- 客户经理ID
    manager_name VARCHAR(200),             -- 客户经理名称
    customer_id BIGINT,                    -- 客户ID
    customer_name VARCHAR(200),            -- 客户名称
    
    -- 指标值
    deposit_balance DECIMAL(18,4),         -- 存款余额
    deposit_monthly_interest DECIMAL(18,4), -- 月利息支出
    ftp_income DECIMAL(18,4),              -- FTP收入
    op_cost DECIMAL(18,4),                 -- 运营成本
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz (biz_id, account_period)
);
```

---

## 六、ETL自动触发机制

### 触发器实现

```sql
-- 贷款数据插入触发器
DELIMITER //
CREATE TRIGGER trg_loan_after_insert
AFTER INSERT ON loan_indicator_detail
FOR EACH ROW
BEGIN
    -- 异步调用ETL存储过程
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
DELIMITER ;
```

### ETL存储过程

```sql
DELIMITER //
CREATE PROCEDURE sp_etl_recalculate(IN p_period VARCHAR(10))
BEGIN
    -- 1. 清除该账期的DWD数据
    DELETE FROM dwd_loan_detail WHERE account_period = p_period;
    DELETE FROM dwd_deposit_detail WHERE account_period = p_period;
    
    -- 2. 从ODS层清洗数据到DWD层（关联所有维度主数据）
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
    
    -- 存款数据类似处理...
    
    -- 3. 清除该账期的DWS数据
    DELETE FROM dw_indicator_fact WHERE period = p_period;
    
    -- 4. 从DWD层按各维度计算指标到DWS层
    -- 按机构维度计算
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'ORG', org_id, org_name, 
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail
    WHERE account_period = p_period
    GROUP BY org_id, org_name;
    
    -- 按条线维度计算
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'BIZ_LINE', biz_line_id, biz_line_name, 
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail
    WHERE account_period = p_period
    GROUP BY biz_line_id, biz_line_name;
    
    -- 按产品维度计算
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'PRODUCT', product_id, product_name, 
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail
    WHERE account_period = p_period
    GROUP BY product_id, product_name;
    
    -- 按渠道维度计算
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'CHANNEL', channel_id, channel_name, 
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail
    WHERE account_period = p_period
    GROUP BY channel_id, channel_name;
    
    -- 按客户经理维度计算
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'MANAGER', manager_id, manager_name, 
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail
    WHERE account_period = p_period
    GROUP BY manager_id, manager_name;
    
    -- 5. 计算TOTAL汇总
    INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'INTEREST_INCOME' AND period = p_period AND dim_type = 'ORG';
    
    -- 其他指标（FTP_COST, RISK_COST, LOAN_PROFIT等）按相同逻辑计算...
END//
DELIMITER ;
```

---

## 七、数据迁移步骤

### 步骤1：创建机构主数据

```sql
-- 调整dw_dim_organization表结构
ALTER TABLE dw_dim_organization ADD COLUMN org_code VARCHAR(10) NOT NULL AFTER id;
ALTER TABLE dw_dim_organization ADD UNIQUE KEY uk_code (org_code);

-- 插入初始数据
INSERT INTO dw_dim_organization (org_code, org_name, parent_id, level) VALUES
('00100001', '总行', 0, 1),
('01200001', '北京分行', 1, 2),
('02200001', '广州分行', 1, 2),
('03200001', '上海分行', 1, 2),
('04200001', '深圳分行', 1, 2),
('05200001', '杭州分行', 1, 2);
```

### 步骤2：创建其他维度主数据

```sql
-- 执行第四节的条线、产品、渠道、客户经理主数据建表和初始化语句
```

### 步骤3：修正原始数据维度ID

```sql
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
```

### 步骤4：创建DWD层表

```sql
-- 执行第五节的DWD层建表语句
```

### 步骤5：创建触发器和ETL存储过程

```sql
-- 执行第六节的触发器和存储过程
```

### 步骤6：清理并重新执行ETL

```sql
-- 删除旧的dw数据
DELETE FROM dw_indicator_fact WHERE period='2026-06';

-- 手动执行一次ETL
CALL sp_etl_recalculate('2026-06');
```

### 步骤7：验证数据准确性

```sql
-- 对比原始数据和ETL结果
SELECT '原始数据' as src, SUM(loan_monthly_interest)/10000 as val 
FROM loan_indicator_detail WHERE account_period='2026-06'
UNION ALL
SELECT 'ETL结果', calc_value 
FROM dw_indicator_fact WHERE period='2026-06' AND indicator_code='INTEREST_INCOME' AND dim_type='TOTAL';
```

---

## 八、需要废弃的代码

### 后端代码

| 文件 | 处理方式 |
|------|----------|
| IndicatorPrecomputeServiceImpl.java | 删除 |
| IndicatorController.java (getIndicatorPrecomputed方法) | 删除 |
| indicator_precomputed表 | 删除 |

### 保留的代码

| 文件 | 用途 |
|------|------|
| DataWarehouseETLServiceImpl.java | 重构为调用sp_etl_recalculate |
| DashboardServiceImpl.java | 已改读dw_indicator_fact，保留 |
| DimensionServiceImpl.java | 已改读dw_indicator_fact，保留 |
| IndicatorDetailServiceImpl.java | 已改读dw_indicator_fact，保留 |
| ExpenseAllocationServiceImpl.java | 保留，费用分摊逻辑不变 |

---

## 九、前端改动

### 数据源统一

所有页面统一从 `/api/dw/*` 接口取数：

| 页面 | 当前接口 | 目标接口 |
|------|----------|----------|
| Dashboard | /api/dashboard/* | /api/dw/* |
| 维度分析 | /api/dimension/* | /api/dw/* |
| 指标数据 | /api/dw/* | /api/dw/* (已统一) |
| 指标库 | /api/indicator/* | /api/indicator/* (不变) |

### 单位显示修复

当前问题：17.95万元显示为"0.00亿元"

解决方案：根据数值大小自动选择单位
- < 10000：显示为"元"
- 10000 ~ 100000000：显示为"万元"
- >= 100000000：显示为"亿元"

---

## 十、验收标准

1. **数据一致性**：Dashboard、维度分析、指标数据页面显示相同指标值
2. **自动同步**：修改原始数据后，页面自动刷新显示新值
3. **数据准确性**：ETL结果与原始数据汇总一致
4. **性能要求**：单次ETL执行时间 < 5秒
5. **代码清理**：旧预计算层代码完全废弃

---

## 十一、客户维度处理

客户维度数据量较大（当前有40个客户），且客户信息可能来自外部系统，建议：

1. **暂不建主数据**：客户维度直接使用原始数据中的customer_id和customer_name
2. **后续扩展**：如有客户主数据系统，可后续对接
3. **ETL处理**：客户维度的指标计算与其他维度类似，直接从原始数据GROUP BY

```sql
-- 客户维度指标计算
INSERT INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'CUSTOMER', customer_id, customer_name, 
       SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
FROM dwd_loan_detail
WHERE account_period = p_period
GROUP BY customer_id, customer_name;
```

---

## 十二、风险与注意事项

1. **数据备份**：迁移前备份原始数据
2. **回滚方案**：保留旧表，出现问题可快速回滚
3. **性能考虑**：触发器可能影响写入性能，需监控
4. **并发处理**：多个账期同时变更时，需处理并发问题
