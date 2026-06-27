# 指标体系设计方案

## 一、设计背景

### 1.1 当前问题
- 现有指标库（indicator_library）没有区分存款/贷款指标
- 指标没有分层设计，基础指标和派生指标混在一起
- 缺少统计口径（月日均、年日均）的支持
- 费用类型没有拆分展示
- 维度分析和指标数据没有整合

### 1.2 设计目标
- 建立完整的指标体系，支持存款/贷款指标分离
- 实现三层指标架构：原子指标 → 派生指标 → 统计口径
- 支持费用类型拆分展示
- 支持明细钻取，看到指标是怎么算出来的
- 与费用分摊模块整合

---

## 二、整体架构

### 2.1 数据流向

```
┌─────────────────────────────────────────────────────────────┐
│  第1层：原始数据（不可变）                                    │
│  • loan_contract, loan_interest_income, loan_ftp_cost       │
│  • deposit_account, deposit_interest_exp, deposit_ftp_income│
│  • expense_record, employee_expense, product_commission     │
└─────────────────────────────────────────────────────────────┘
                           ▼
                     费用分摊/加工
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  第2层：账户级数据（biz_ledger）                              │
│  • 每个账户的基础指标：                                       │
│    - 贷款：余额、利息收入、FTP成本、风险成本、运营成本        │
│    - 存款：余额、FTP收入、利息支出、运营成本                  │
└─────────────────────────────────────────────────────────────┘
                           ▼
                     指标计算/聚合
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  第3层：指标数据（聚合层）                                    │
│  • 原子指标：直接从biz_ledger聚合                            │
│  • 派生指标：基于原子指标计算                                │
│  • 统计口径：月日均、年日均                                  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 指标体系分层

```
指标体系
├── 第1层：原子指标（Atomic） - 9个
│   ├── 贷款：余额、利息收入、FTP成本、风险成本、运营成本（5个）
│   └── 存款：余额、FTP收入、利息支出、运营成本（4个）
│
├── 第2层：派生指标（Derived） - 9个
│   ├── 贷款：利润、净利息收入、成本收入比、风险成本率、FTP利差（5个）
│   └── 存款：利润、净利息收入、成本收入比、FTP利差（4个）
│
└── 第3层：统计口径（Stat Config） - 18个
    ├── 贷款：5个原子指标 × 2个口径（月日均/年日均）= 10个
    └── 存款：4个原子指标 × 2个口径（月日均/年日均）= 8个
```

---

## 三、数据库设计

### 3.1 原子指标表（atomic_indicator）

```sql
CREATE TABLE atomic_indicator (
    code VARCHAR(50) PRIMARY KEY COMMENT '指标编码',
    name VARCHAR(100) NOT NULL COMMENT '指标名称',
    business_line VARCHAR(20) NOT NULL COMMENT '业务条线：LOAN/DEPOSIT',
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
```

### 3.2 派生指标表（derived_indicator）

```sql
CREATE TABLE derived_indicator (
    code VARCHAR(50) PRIMARY KEY COMMENT '指标编码',
    name VARCHAR(100) NOT NULL COMMENT '指标名称',
    business_line VARCHAR(20) NOT NULL COMMENT '业务条线：LOAN/DEPOSIT',
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
```

### 3.3 统计口径表（indicator_stat_config）

```sql
CREATE TABLE indicator_stat_config (
    indicator_code VARCHAR(50) COMMENT '指标编码',
    stat_type VARCHAR(20) COMMENT '统计口径：MONTHLY_DAILY_AVG/YEARLY_DAILY_AVG',
    calc_formula TEXT COMMENT '计算公式',
    description VARCHAR(200) COMMENT '描述',
    PRIMARY KEY (indicator_code, stat_type)
) COMMENT='统计口径表';
```

### 3.4 指标预计算结果表（indicator_pre_calc）

```sql
CREATE TABLE indicator_pre_calc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_code VARCHAR(50) NOT NULL COMMENT '指标编码',
    indicator_type VARCHAR(20) NOT NULL COMMENT '指标类型：ATOMIC/DERIVED',
    stat_type VARCHAR(20) COMMENT '统计口径',
    calc_period VARCHAR(10) NOT NULL COMMENT '计算周期：MONTH/YEAR',
    period_value VARCHAR(20) NOT NULL COMMENT '周期值',
    calc_value DECIMAL(18,4) COMMENT '计算结果',
    calc_time DATETIME COMMENT '计算时间',
    status TINYINT DEFAULT 1 COMMENT '状态',
    INDEX idx_indicator (indicator_code),
    INDEX idx_period (calc_period, period_value)
) COMMENT='指标预计算结果表';
```

---

## 四、指标清单

### 4.1 原子指标（9个）

#### 贷款原子指标（5个）

| 编码 | 名称 | 来源表 | 来源字段 | 筛选条件 |
|-----|------|-------|---------|---------|
| LOAN_BALANCE | 贷款余额 | biz_ledger | biz_amount | product_type=LOAN |
| LOAN_INTEREST | 贷款利息收入 | biz_ledger | interest_income | product_type=LOAN |
| LOAN_FTP | 贷款FTP成本 | biz_ledger | ftp_cost | product_type=LOAN |
| LOAN_RISK | 贷款风险成本 | biz_ledger | risk_cost | product_type=LOAN |
| LOAN_OP | 贷款运营成本 | biz_ledger | op_cost | product_type=LOAN |

#### 存款原子指标（4个）

| 编码 | 名称 | 来源表 | 来源字段 | 筛选条件 |
|-----|------|-------|---------|---------|
| DEPOSIT_BALANCE | 存款余额 | biz_ledger | biz_amount | product_type=DEPOSIT |
| DEPOSIT_FTP | 存款FTP收入 | biz_ledger | interest_income | product_type=DEPOSIT |
| DEPOSIT_INTEREST | 存款利息支出 | biz_ledger | interest_expense | product_type=DEPOSIT |
| DEPOSIT_OP | 存款运营成本 | biz_ledger | op_cost | product_type=DEPOSIT |

### 4.2 派生指标（9个）

#### 贷款派生指标（5个）

| 编码 | 名称 | 计算公式 |
|-----|------|---------|
| LOAN_PROFIT | 贷款利润 | LOAN_INTEREST - LOAN_FTP - LOAN_RISK - LOAN_OP |
| LOAN_NET_INTEREST | 贷款净利息收入 | LOAN_INTEREST - LOAN_FTP |
| LOAN_COST_RATIO | 贷款成本收入比 | (LOAN_FTP + LOAN_RISK + LOAN_OP) / LOAN_INTEREST |
| LOAN_RISK_RATIO | 贷款风险成本率 | LOAN_RISK / LOAN_INTEREST |
| LOAN_FTP_SPREAD | 贷款FTP利差 | (LOAN_INTEREST - LOAN_FTP) / LOAN_BALANCE |

#### 存款派生指标（4个）

| 编码 | 名称 | 计算公式 |
|-----|------|---------|
| DEPOSIT_PROFIT | 存款利润 | DEPOSIT_FTP - DEPOSIT_INTEREST - DEPOSIT_OP |
| DEPOSIT_NET_INTEREST | 存款净利息收入 | DEPOSIT_FTP - DEPOSIT_INTEREST |
| DEPOSIT_COST_RATIO | 存款成本收入比 | (DEPOSIT_INTEREST + DEPOSIT_OP) / DEPOSIT_FTP |
| DEPOSIT_FTP_SPREAD | 存款FTP利差 | (DEPOSIT_FTP - DEPOSIT_INTEREST) / DEPOSIT_BALANCE |

### 4.3 统计口径（18个）

#### 贷款统计口径（10个）

| 指标编码 | 统计口径 | 计算公式 |
|---------|---------|---------|
| LOAN_BALANCE | MONTHLY_DAILY_AVG | SUM(LOAN_BALANCE) / 当月天数 |
| LOAN_BALANCE | YEARLY_DAILY_AVG | SUM(LOAN_BALANCE) / 当年天数 |
| LOAN_INTEREST | MONTHLY_DAILY_AVG | SUM(LOAN_INTEREST) / 当月天数 |
| LOAN_INTEREST | YEARLY_DAILY_AVG | SUM(LOAN_INTEREST) / 当年天数 |
| LOAN_FTP | MONTHLY_DAILY_AVG | SUM(LOAN_FTP) / 当月天数 |
| LOAN_FTP | YEARLY_DAILY_AVG | SUM(LOAN_FTP) / 当年天数 |
| LOAN_RISK | MONTHLY_DAILY_AVG | SUM(LOAN_RISK) / 当月天数 |
| LOAN_RISK | YEARLY_DAILY_AVG | SUM(LOAN_RISK) / 当年天数 |
| LOAN_OP | MONTHLY_DAILY_AVG | SUM(LOAN_OP) / 当月天数 |
| LOAN_OP | YEARLY_DAILY_AVG | SUM(LOAN_OP) / 当年天数 |

#### 存款统计口径（8个）

| 指标编码 | 统计口径 | 计算公式 |
|---------|---------|---------|
| DEPOSIT_BALANCE | MONTHLY_DAILY_AVG | SUM(DEPOSIT_BALANCE) / 当月天数 |
| DEPOSIT_BALANCE | YEARLY_DAILY_AVG | SUM(DEPOSIT_BALANCE) / 当年天数 |
| DEPOSIT_FTP | MONTHLY_DAILY_AVG | SUM(DEPOSIT_FTP) / 当月天数 |
| DEPOSIT_FTP | YEARLY_DAILY_AVG | SUM(DEPOSIT_FTP) / 当年天数 |
| DEPOSIT_INTEREST | MONTHLY_DAILY_AVG | SUM(DEPOSIT_INTEREST) / 当月天数 |
| DEPOSIT_INTEREST | YEARLY_DAILY_AVG | SUM(DEPOSIT_INTEREST) / 当年天数 |
| DEPOSIT_OP | MONTHLY_DAILY_AVG | SUM(DEPOSIT_OP) / 当月天数 |
| DEPOSIT_OP | YEARLY_DAILY_AVG | SUM(DEPOSIT_OP) / 当年天数 |

---

## 五、页面设计

### 5.1 左侧菜单结构

```
主数据管理
├── 维度主数据
├── 指标库
└── 指标数据（新增）
    ├── 贷款指标
    └── 存款指标
```

### 5.2 贷款指标页面

```
┌─────────────────────────────────────────────────────────────┐
│  贷款指标                                                    │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌────────┐│
│  │贷款余额  │ │利息收入  │ │FTP成本   │ │风险成本  │ │运营成本 ││
│  │月日均    │ │月日均    │ │月日均    │ │月日均    │ │月日均   ││
│  │XX万     │ │XX万     │ │XX万     │ │XX万     │ │XX万    ││
│  │年日均    │ │年日均    │ │年日均    │ │年日均    │ │年日均   ││
│  │XX万     │ │XX万     │ │XX万     │ │XX万     │ │XX万    ││
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └────────┘│
├─────────────────────────────────────────────────────────────┤
│  筛选条件：[月日均 ▼] [2024年1月 ▼] [查询] [重置]           │
├─────────────────────────────────────────────────────────────┤
│  明细数据（点击指标卡片切换）                                 │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ 📅 2024年1月                                    [展开]  ││
│  │   合同号    客户      余额      利率    利息金额         ││
│  │   LOAN001  华为      1000万    4.35%   3.63万          ││
│  │   LOAN002  腾讯      800万     4.50%   3.00万          ││
│  │   合计                              6.63万              ││
│  ├─────────────────────────────────────────────────────────┤│
│  │ 📅 2024年2月                                    [展开]  ││
│  │   ...                                                   ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │ 分页：< 上一页  1 2 3 ... 10  下一页 >                  ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 5.3 存款指标页面

```
┌─────────────────────────────────────────────────────────────┐
│  存款指标                                                    │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │
│  │存款余额  │ │FTP收入   │ │利息支出  │ │运营成本  │          │
│  │月日均    │ │月日均    │ │月日均    │ │月日均    │          │
│  │XX万     │ │XX万     │ │XX万     │ │XX万     │          │
│  │年日均    │ │年日均    │ │年日均    │ │年日均    │          │
│  │XX万     │ │XX万     │ │XX万     │ │XX万     │          │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘          │
├─────────────────────────────────────────────────────────────┤
│  筛选条件：[月日均 ▼] [2024年1月 ▼] [查询] [重置]           │
├─────────────────────────────────────────────────────────────┤
│  明细数据（点击指标卡片切换）                                 │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ 📅 2024年1月                                    [展开]  ││
│  │   账户号    客户      余额      利率    FTP收入          ││
│  │   DEP001   华为      500万     2.50%   1.04万           ││
│  │   DEP002   腾讯      300万     2.75%   0.69万           ││
│  │   合计                              1.73万               ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │ 分页：< 上一页  1 2 3 ... 10  下一页 >                  ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 5.4 明细表格列定义

#### 贷款指标明细

| 列名 | 字段 | 宽度 | 说明 |
|-----|------|------|------|
| 合同号 | contract_id | 120px | 贷款合同编号 |
| 客户名称 | customer_name | 150px | 关联客户 |
| 余额 | balance | 100px | 贷款余额 |
| 利率 | rate | 80px | 贷款利率 |
| 利息金额 | interest_amount | 100px | 利息收入 |
| FTP成本 | ftp_cost | 100px | FTP成本 |
| 风险成本 | risk_cost | 100px | 风险成本 |
| 运营成本 | op_cost | 100px | 运营成本 |
| 费用类型 | expense_type | 100px | 仅运营成本显示 |

#### 存款指标明细

| 列名 | 字段 | 宽度 | 说明 |
|-----|------|------|------|
| 账户号 | account_id | 120px | 存款账户编号 |
| 客户名称 | customer_name | 150px | 关联客户 |
| 余额 | balance | 100px | 存款余额 |
| 利率 | rate | 80px | 存款利率 |
| FTP收入 | ftp_income | 100px | FTP收入 |
| 利息支出 | interest_expense | 100px | 利息支出 |
| 运营成本 | op_cost | 100px | 运营成本 |
| 费用类型 | expense_type | 100px | 仅运营成本显示 |

---

## 六、服务层设计

### 6.1 指标计算服务（IndicatorCalcService）

```
IndicatorCalcService
├── calcAtomicIndicator(code, period)  -- 计算原子指标
├── calcDerivedIndicator(code, period) -- 计算派生指标
├── calcStatConfig(code, statType, period) -- 计算统计口径
├── calcAllIndicators(period) -- 批量计算所有指标
└── getIndicatorDetail(code, period) -- 获取指标明细
```

### 6.2 指标查询服务（IndicatorQueryService）

```
IndicatorQueryService
├── getIndicatorValue(code, period) -- 获取指标值
├── getIndicatorTrend(code, months) -- 获取指标趋势
├── getIndicatorDetail(code, period, page) -- 获取指标明细（分页）
├── getIndicatorDetailByGroup(code, period, groupValue) -- 按分组获取明细
└── compareIndicators(codes, period) -- 对比多个指标
```

### 6.3 指标定时任务（IndicatorScheduleTask）

```
IndicatorScheduleTask
├── dailyCalc() -- 每日计算（凌晨2点）
├── monthlyCalc() -- 每月计算（月初）
├── yearlyCalc() -- 每年计算（年初）
└── recalcAll() -- 手动全量重算
```

---

## 七、API接口设计

### 7.1 接口列表

| 接口 | 方法 | 说明 |
|-----|------|------|
| /api/indicator/atomic | GET | 获取所有原子指标列表 |
| /api/indicator/derived | GET | 获取所有派生指标列表 |
| /api/indicator/value/{code} | GET | 获取指标值 |
| /api/indicator/trend/{code} | GET | 获取指标趋势 |
| /api/indicator/detail/{code} | GET | 获取指标明细（分页） |
| /api/indicator/detail/{code}/group/{groupValue} | GET | 按分组获取明细 |
| /api/indicator/calc | POST | 手动触发计算 |

### 7.2 接口参数

#### GET /api/indicator/value/{code}

**参数：**
- code：指标编码
- period：计算周期（MONTH/YEAR）
- periodValue：周期值（2024-01/2024）

**返回：**
```json
{
  "code": "LOAN_BALANCE",
  "name": "贷款余额",
  "period": "MONTH",
  "periodValue": "2024-01",
  "value": 10000.00,
  "unit": "万元"
}
```

#### GET /api/indicator/detail/{code}

**参数：**
- code：指标编码
- period：计算周期（MONTH/YEAR）
- periodValue：周期值（2024-01/2024）
- page：页码（默认1）
- size：每页数量（默认50）

**返回：**
```json
{
  "code": "LOAN_INTEREST",
  "name": "贷款利息收入",
  "period": "MONTH",
  "periodValue": "2024-01",
  "total": 1000,
  "page": 1,
  "size": 50,
  "groups": [
    {
      "groupName": "2024年1月",
      "groupValue": "2024-01",
      "total": 6.63,
      "details": [
        {
          "contractId": "LOAN001",
          "customerName": "华为",
          "balance": 1000,
          "rate": 4.35,
          "interestAmount": 3.63
        },
        {
          "contractId": "LOAN002",
          "customerName": "腾讯",
          "balance": 800,
          "rate": 4.50,
          "interestAmount": 3.00
        }
      ]
    }
  ]
}
```

---

## 八、费用分摊整合

### 8.1 运营成本费用类型拆分

运营成本（LOAN_OP/DEPOSIT_OP）支持按费用类型拆分展示：

**费用类型示例：**
- 数据使用费
- 催收费
- 人工费用
- 租赁费用
- 设备费用
- 营销费用
- 行政费用

**展示方式：**
- 点击运营成本卡片，显示费用类型拆分
- 明细表格增加"费用类型"列
- 按费用类型分组展示

### 8.2 费用分摊流程

```
┌─────────────────────────────────────────────────────────────┐
│  第1步：原始费用记录                                         │
│  • expense_record：费用类型、金额、发生部门、发生日期        │
└─────────────────────────────────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  第2步：费用分摊                                             │
│  • 按分摊规则分摊到账户级                                    │
│  • 写入biz_ledger.op_cost                                   │
└─────────────────────────────────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  第3步：指标计算                                             │
│  • 从biz_ledger聚合运营成本                                  │
│  • 支持按费用类型拆分展示                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 九、实施计划

### 9.1 阶段1：数据库设计（1天）
- 创建原子指标表
- 创建派生指标表
- 创建统计口径表
- 创建指标预计算结果表
- 插入指标数据

### 9.2 阶段2：后端开发（3天）
- 实现指标计算服务
- 实现指标查询服务
- 实现指标定时任务
- 实现API接口

### 9.3 阶段3：前端开发（2天）
- 实现左侧菜单
- 实现指标卡片组件
- 实现明细表格组件
- 实现筛选条件

### 9.4 阶段4：联调测试（1天）
- 前后端联调
- 功能测试
- 性能测试

---

## 十、设计决策记录

### 10.1 关键决策

| 决策 | 理由 |
|-----|------|
| 选择方案C（重构指标库为三层架构） | 架构清晰，符合指标体系标准，易于扩展 |
| 运营成本支持费用类型拆分 | 用户要求看到费用明细 |
| 统计口径只支持月日均和年日均 | 用户明确不要日均，只要月日均和年日均 |
| 明细按月份分组 | 支持懒加载，避免数据量过大 |
| 指标预计算 | 加快查询速度，避免每次实时计算 |

### 10.2 待确认事项

- 费用类型的具体分类标准
- 费用分摊的具体规则
- 明细数据的保留期限
- 预计算任务的执行频率
