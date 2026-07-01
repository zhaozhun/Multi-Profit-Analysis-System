# 数据仓库设计方案

## 1. 背景与目标

### 1.1 背景
当前系统存在数据不统一的问题：
- 数据源分散：贷款从loan_indicator_detail读取，存款从deposit_indicator_detail读取
- 计算逻辑不统一：各页面独立计算，可能重复/遗漏
- 修改风险高：修改数据需要修改多个地方，容易出错
- 数据不一致：不同页面展示的数据可能不一致

### 1.2 目标
引入数据仓库，统一数据源，解决数据不统一问题。

## 2. 数据架构

### 2.1 整体架构
```
原始数据（loan_indicator_detail、deposit_indicator_detail）
    ↓ ETL（统一计算）
数据仓库（统一数据模型）
    ↓
所有页面从数据仓库读取
```

### 2.2 数据模型

#### 事实表
```sql
-- 指标事实表（统一存储所有指标）
CREATE TABLE dw_indicator_fact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    indicator_code VARCHAR(50) NOT NULL,      -- 指标编码
    period VARCHAR(10) NOT NULL,               -- 期间（2025-06）
    period_type VARCHAR(20) NOT NULL,          -- 期间类型（MONTH）
    dim_type VARCHAR(20) NOT NULL,             -- 维度类型（ORG/BIZ_LINE/PRODUCT等）
    dim_id BIGINT NOT NULL,                    -- 维度ID
    dim_name VARCHAR(200),                     -- 维度名称
    calc_value DECIMAL(18,4),                  -- 指标值
    caliber_type VARCHAR(10) DEFAULT 'ASSESS', -- 口径类型
    calc_time TIMESTAMP,                       -- 计算时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_calc (indicator_code, period, period_type, dim_type, dim_id, caliber_type)
);
```

#### 维度表
```sql
-- 机构维度表
CREATE TABLE dw_dim_organization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_code VARCHAR(50) NOT NULL,
    org_name VARCHAR(200) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 条线维度表
CREATE TABLE dw_dim_biz_line (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    line_code VARCHAR(50) NOT NULL,
    line_name VARCHAR(200) NOT NULL,
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 产品维度表
CREATE TABLE dw_dim_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_type VARCHAR(20),
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 渠道维度表
CREATE TABLE dw_dim_channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_code VARCHAR(50) NOT NULL,
    channel_name VARCHAR(200) NOT NULL,
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 客户经理维度表
CREATE TABLE dw_dim_manager (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    manager_code VARCHAR(50) NOT NULL,
    manager_name VARCHAR(200) NOT NULL,
    org_id BIGINT,
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 客户维度表
CREATE TABLE dw_dim_customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_code VARCHAR(50) NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    customer_type VARCHAR(20),
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 3. ETL流程

### 3.1 流程概述
```
┌─────────────────────────────────────────────────────────────┐
│  ETL流程（每月执行）                                        │
├─────────────────────────────────────────────────────────────┤
│  1. 数据抽取（Extract）                                     │
│     - 从 loan_indicator_detail 读取贷款数据                 │
│     - 从 deposit_indicator_detail 读取存款数据              │
├─────────────────────────────────────────────────────────────┤
│  2. 数据转换（Transform）                                   │
│     - 计算26个指标                                          │
│     - 按7个维度汇总                                         │
│     - 数据清洗和校验                                        │
├─────────────────────────────────────────────────────────────┤
│  3. 数据加载（Load）                                        │
│     - 写入 dw_indicator_fact（指标事实表）                  │
│     - 更新维度表（如有新增）                                │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 指标计算公式

| 指标编码 | 指标名称 | 计算公式 |
|----------|----------|----------|
| LOAN_BALANCE | 在贷余额 | SUM(loan_balance) |
| LOAN_COUNT | 贷款笔数 | COUNT(biz_id) |
| INTEREST_INCOME | 利息收入 | SUM(loan_monthly_interest) |
| FTP_COST | FTP成本 | SUM(ftp_cost) |
| RISK_COST | 风险成本 | SUM(risk_cost) |
| LOAN_PROFIT | 贷款利润 | 利息收入 - FTP成本 - 风险成本 - 运营成本 |
| DEPOSIT_BALANCE | 存款余额 | SUM(deposit_balance) |
| DEPOSIT_COUNT | 存款笔数 | COUNT(biz_id) |
| FTP_INCOME | FTP收入 | SUM(ftp_income) |
| DEPOSIT_INTEREST | 利息支出 | SUM(deposit_monthly_interest) |
| DEPOSIT_PROFIT | 存款利润 | FTP收入 - 利息支出 - 运营成本 |
| OP_COST | 运营成本 | SUM(op_cost) |
| TOTAL_PROFIT | 总利润 | 贷款利润 + 存款利润 |

### 3.3 ETL执行时机

| 时机 | 说明 |
|------|------|
| 每月月初 | 上月数据结算后执行 |
| 手动触发 | 数据修正后手动执行 |
| 定时任务 | 每天凌晨检查并执行 |

## 4. API设计

### 4.1 统一API接口

| API路径 | 方法 | 说明 | 参数 |
|---------|------|------|------|
| `/api/dw/indicator/list` | GET | 获取指标列表 | 无 |
| `/api/dw/indicator/summary` | GET | 获取指标汇总 | indicatorCode, period, caliberType |
| `/api/dw/indicator/dimension` | GET | 获取指标维度数据 | indicatorCode, period, dimType, caliberType |
| `/api/dw/indicator/detail` | GET | 获取指标明细数据 | indicatorCode, period, dimType, dimId, caliberType |
| `/api/dw/indicator/trend` | GET | 获取指标趋势 | indicatorCode, months, caliberType |
| `/api/dw/etl/execute` | POST | 执行ETL | period |
| `/api/dw/etl/status` | GET | 查询ETL状态 | period |

### 4.2 API响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "indicatorCode": "INTEREST_INCOME",
    "indicatorName": "利息收入",
    "period": "2025-06",
    "totalValue": 200.6072,
    "unit": "万元",
    "dimensions": [
      {
        "dimType": "ORG",
        "dimName": "上海分行",
        "value": 16.2046
      }
    ]
  }
}
```

## 5. 前端页面设计

### 5.1 指标库页面

**功能**：
- 树形展示：业务线 → 指标类型 → 指标
- 指标详情：展示指标基本信息
- 关联关系：展示指标之间的依赖关系
- 增删改查：支持指标管理

**页面结构**：
```
┌─────────────────────────────────────────────────────────────┐
│  主数据管理                                                  │
├──────────────┬──────────────────────────────────────────────┤
│  左侧树形    │  右侧内容                                    │
├──────────────┼──────────────────────────────────────────────┤
│  ▼ 主数据管理 │  指标库 / 指标详情                           │
│    ▼ 维度主数据│                                              │
│      · 机构   │                                              │
│      · 条线   │                                              │
│      · 部门   │                                              │
│      · 产品   │                                              │
│      · 渠道   │                                              │
│      · 客户经理│                                              │
│      · 客户   │                                              │
│    ▼ 指标库   │                                              │
│      ▼ 资产类 │                                              │
│        · 规模类│                                              │
│        · 收入类│                                              │
│        · 成本类│                                              │
│        · 利润类│                                              │
│        · 效率类│                                              │
│      ▼ 负债类 │                                              │
│        · 规模类│                                              │
│        · 收入类│                                              │
│        · 利润类│                                              │
│      ▼ 全部指标│                                              │
│        · 成本类│                                              │
│        · 利润类│                                              │
│        · 效率类│                                              │
└──────────────┴──────────────────────────────────────────────┘
```

### 5.2 指标数据页面

**功能**：
- 汇总数据：展示指标汇总值
- 维度拆分：支持7个维度切换
- 明细数据：支持钻取查看明细
- 图表展示：柱状图、折线图

**页面结构**：
```
┌─────────────────────────────────────────────────────────────┐
│  指标数据 - 利息收入                                         │
├─────────────────────────────────────────────────────────────┤
│  筛选条件：期间 | 口径 | 维度 | 维度值                       │
├─────────────────────────────────────────────────────────────┤
│  汇总卡片：总值 | 同比 | 环比                                │
├─────────────────────────────────────────────────────────────┤
│  维度图表：柱状图（按维度展示）                               │
├─────────────────────────────────────────────────────────────┤
│  明细表格：维度名称 | 指标值 | 占比 | 操作                   │
└─────────────────────────────────────────────────────────────┘
```

### 5.3 维度分析页面

**现状**：维持当前设计，不做修改

### 5.4 驾驶舱页面

**现状**：维持当前设计，不做修改

### 5.5 主数据管理页面

**功能**：
- 维度主数据：机构、条线、部门、产品、渠道、客户经理、客户
- 指标库：指标定义管理
- 增删改查：支持数据管理

## 6. 指标定义

### 6.1 资产类指标

| 指标编码 | 指标名称 | 指标类型 | 计算公式 | 单位 |
|----------|----------|----------|----------|------|
| LOAN_BALANCE | 在贷余额 | 规模类 | SUM(loan_balance) | 万元 |
| LOAN_COUNT | 贷款笔数 | 规模类 | COUNT(biz_id) | 笔 |
| INTEREST_INCOME | 利息收入 | 收入类 | SUM(loan_monthly_interest) | 万元 |
| FTP_COST | FTP成本 | 成本类 | SUM(ftp_cost) | 万元 |
| RISK_COST | 风险成本 | 成本类 | SUM(risk_cost) | 万元 |
| LOAN_PROFIT | 贷款利润 | 利润类 | 利息收入-FTP成本-风险成本-运营成本 | 万元 |
| RISK_COST_RATIO | 风险成本率 | 效率类 | 风险成本/利息收入 | % |
| FTP_SPREAD | FTP利差 | 效率类 | (利息收入-FTP成本)/余额 | % |

### 6.2 负债类指标

| 指标编码 | 指标名称 | 指标类型 | 计算公式 | 单位 |
|----------|----------|----------|----------|------|
| DEPOSIT_BALANCE | 存款余额 | 规模类 | SUM(deposit_balance) | 万元 |
| DEPOSIT_COUNT | 存款笔数 | 规模类 | COUNT(biz_id) | 笔 |
| FTP_INCOME | FTP收入 | 收入类 | SUM(ftp_income) | 万元 |
| DEPOSIT_INTEREST | 利息支出 | 收入类 | SUM(deposit_monthly_interest) | 万元 |
| DEPOSIT_PROFIT | 存款利润 | 利润类 | FTP收入-利息支出-运营成本 | 万元 |

### 6.3 全部指标

| 指标编码 | 指标名称 | 指标类型 | 计算公式 | 单位 |
|----------|----------|----------|----------|------|
| OP_COST | 运营成本 | 成本类 | SUM(op_cost) | 万元 |
| TOTAL_COST | 总成本 | 成本类 | FTP成本+风险成本+运营成本 | 万元 |
| TOTAL_PROFIT | 总利润 | 利润类 | 贷款利润+存款利润 | 万元 |
| PROFIT_PER_MANAGER | 人均利润 | 利润类 | 总利润/客户经理人数 | 万元 |
| NET_INTEREST_INCOME | 净利息收入 | 收入类 | 利息收入-FTP成本 | 万元 |
| COST_INCOME_RATIO | 成本收入比 | 效率类 | 总成本/总收入 | % |
| AVG_RATE | 平均利率 | 效率类 | 加权平均利率 | % |

## 7. 实施计划

### 7.1 开发周期
- 第1周：数据模型设计 + ETL开发
- 第2周：API开发 + 单元测试
- 第3周：前端页面开发
- 第4周：集成测试 + 性能优化

### 7.2 里程碑
- M1：数据模型和ETL完成
- M2：API接口完成
- M3：前端页面完成
- M4：测试验收完成

## 8. 风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| 数据迁移失败 | 数据丢失 | 备份数据，分步迁移 |
| 性能问题 | 查询慢 | 优化索引，预计算 |
| 数据不一致 | 业务错误 | 数据校验，人工核对 |
