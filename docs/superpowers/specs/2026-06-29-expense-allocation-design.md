# 运营成本分摊系统设计文档

## 1. 背景与目标

### 1.1 背景
当前系统中，运营成本数据分散在不同维度（部门、产品、机构），无法直接关联到每笔业务。需要设计一套分摊系统，将各种费用按规则分摊到每笔业务上，从而计算每笔业务的利润。

### 1.2 目标
- 将各种运营成本按规则分摊到每笔业务
- 支持不同费用类型使用不同的分摊因子和规则
- 支持从汇总到明细的钻取式查看
- 支持分摊规则的可视化配置

## 2. 数据模型设计

### 2.1 业务数据表（已有）
- `loan_indicator_detail`：贷款业务明细
- `deposit_indicator_detail`：存款业务明细

每笔业务包含：利息收入、FTP成本、风险成本（直接取值）

### 2.2 费用原始表（每种费用独立）

```sql
-- 房租物业表（部门维度）
CREATE TABLE expense_rent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间(YYYY-MM)',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    dept_name VARCHAR(200) COMMENT '部门名称',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_dept (dept_id)
) COMMENT='房租物业费用表';

-- 人力成本表（部门+人员维度）
CREATE TABLE expense_salary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    dept_name VARCHAR(200) COMMENT '部门名称',
    manager_id BIGINT COMMENT '客户经理ID',
    manager_name VARCHAR(200) COMMENT '客户经理名称',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_dept (dept_id),
    INDEX idx_manager (manager_id)
) COMMENT='人力成本费用表';

-- IT系统费用表（产品维度）
CREATE TABLE expense_it (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_name VARCHAR(200) COMMENT '产品名称',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_product (product_id)
) COMMENT='IT系统费用表';

-- 营销费用表（机构维度）
CREATE TABLE expense_marketing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间',
    org_id BIGINT NOT NULL COMMENT '机构ID',
    org_name VARCHAR(200) COMMENT '机构名称',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_org (org_id)
) COMMENT='营销费用表';

-- 其他费用表（可扩展）
CREATE TABLE expense_other (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间',
    expense_type VARCHAR(50) NOT NULL COMMENT '费用类型',
    dim_type VARCHAR(20) NOT NULL COMMENT '维度类型',
    dim_id BIGINT NOT NULL COMMENT '维度ID',
    dim_name VARCHAR(200) COMMENT '维度名称',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_type (expense_type),
    INDEX idx_dim (dim_type, dim_id)
) COMMENT='其他费用表';
```

### 2.3 分摊因子表

```sql
CREATE TABLE allocation_factor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    factor_code VARCHAR(50) NOT NULL UNIQUE COMMENT '因子编码',
    factor_name VARCHAR(100) NOT NULL COMMENT '因子名称',
    factor_type VARCHAR(50) NOT NULL COMMENT '因子类型(MANAGER_COUNT/BIZ_AMOUNT/BALANCE/REVENUE)',
    source_table VARCHAR(100) NOT NULL COMMENT '数据来源表',
    source_field VARCHAR(100) NOT NULL COMMENT '数据来源字段',
    calc_formula VARCHAR(500) COMMENT '计算公式',
    description VARCHAR(500) COMMENT '描述',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='分摊因子表';

-- 初始化数据
INSERT INTO allocation_factor (factor_code, factor_name, factor_type, source_table, source_field, description) VALUES
('MANAGER_COUNT', '客户经理人数', 'MANAGER_COUNT', 'dimension_master', 'id', '按客户经理人数分摊'),
('BIZ_AMOUNT', '业务金额', 'BIZ_AMOUNT', 'biz_ledger', 'biz_amount', '按业务金额分摊'),
('LOAN_BALANCE', '贷款余额', 'BALANCE', 'loan_indicator_detail', 'loan_balance', '按贷款余额分摊'),
('DEPOSIT_BALANCE', '存款余额', 'BALANCE', 'deposit_indicator_detail', 'deposit_balance', '按存款余额分摊'),
('BIZ_COUNT', '业务笔数', 'BIZ_COUNT', 'biz_ledger', 'biz_id', '按业务笔数分摊'),
('REVENUE', '收入金额', 'REVENUE', 'biz_ledger', 'revenue', '按收入金额分摊');
```

### 2.4 分摊规则表

```sql
CREATE TABLE allocation_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    expense_table VARCHAR(100) NOT NULL COMMENT '费用表名',
    expense_type VARCHAR(50) NOT NULL COMMENT '费用类型',
    source_dim_type VARCHAR(20) NOT NULL COMMENT '源维度类型(DEPT/PRODUCT/ORG)',
    target_type VARCHAR(20) NOT NULL DEFAULT 'BIZ' COMMENT '目标类型(固定为BIZ)',
    factor_code VARCHAR(50) NOT NULL COMMENT '分摊因子编码',
    description VARCHAR(500) COMMENT '描述',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (factor_code) REFERENCES allocation_factor(factor_code)
) COMMENT='分摊规则表';

-- 初始化数据
INSERT INTO allocation_rule (rule_code, rule_name, expense_table, expense_type, source_dim_type, factor_code, description) VALUES
('RENT_BY_MANAGER', '房租按客户经理分摊', 'expense_rent', 'RENT', 'DEPT', 'MANAGER_COUNT', '部门房租按人数分摊到客户经理，再按业务量分摊到业务'),
('SALARY_BY_MANAGER', '人力成本按客户经理分摊', 'expense_salary', 'SALARY', 'DEPT', 'MANAGER_COUNT', '人力成本按人数分摊到客户经理，再按业务量分摊到业务'),
('IT_BY_PRODUCT', 'IT费用按产品分摊', 'expense_it', 'IT', 'PRODUCT', 'BIZ_AMOUNT', 'IT费用按业务金额分摊到该产品的每笔业务'),
('MARKETING_BY_ORG', '营销费用按机构分摊', 'expense_marketing', 'MARKETING', 'ORG', 'BIZ_AMOUNT', '营销费用按业务金额分摊到该机构的每笔业务');
```

### 2.5 分摊结果表

```sql
CREATE TABLE expense_allocation_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间',
    biz_id VARCHAR(30) NOT NULL COMMENT '业务编号',
    biz_type VARCHAR(20) NOT NULL COMMENT '业务类型(LOAN/DEPOSIT)',
    expense_type VARCHAR(50) NOT NULL COMMENT '费用类型',
    expense_name VARCHAR(100) COMMENT '费用名称',
    allocated_amount DECIMAL(18,4) NOT NULL COMMENT '分摊金额',
    factor_value DECIMAL(18,4) COMMENT '因子值',
    factor_ratio DECIMAL(12,8) COMMENT '分摊比例',
    rule_code VARCHAR(50) COMMENT '规则编码',
    batch_no VARCHAR(50) COMMENT '批次号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_biz (biz_id),
    INDEX idx_expense (expense_type),
    INDEX idx_batch (batch_no)
) COMMENT='费用分摊结果表';
```

### 2.6 业务利润汇总表

```sql
CREATE TABLE biz_profit_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(10) NOT NULL COMMENT '期间',
    biz_id VARCHAR(30) NOT NULL COMMENT '业务编号',
    biz_type VARCHAR(20) NOT NULL COMMENT '业务类型(LOAN/DEPOSIT)',
    customer_id BIGINT COMMENT '客户ID',
    customer_name VARCHAR(200) COMMENT '客户名称',
    org_id BIGINT COMMENT '机构ID',
    org_name VARCHAR(200) COMMENT '机构名称',
    biz_line_id BIGINT COMMENT '条线ID',
    biz_line_name VARCHAR(200) COMMENT '条线名称',
    product_id BIGINT COMMENT '产品ID',
    product_name VARCHAR(200) COMMENT '产品名称',
    channel_id BIGINT COMMENT '渠道ID',
    channel_name VARCHAR(200) COMMENT '渠道名称',
    manager_id BIGINT COMMENT '客户经理ID',
    manager_name VARCHAR(200) COMMENT '客户经理名称',
    -- 金额字段
    balance DECIMAL(18,2) COMMENT '余额',
    interest_income DECIMAL(18,4) COMMENT '利息收入',
    ftp_cost DECIMAL(18,4) COMMENT 'FTP成本',
    risk_cost DECIMAL(18,4) COMMENT '风险成本',
    op_cost DECIMAL(18,4) COMMENT '运营成本(各项费用之和)',
    profit DECIMAL(18,4) COMMENT '利润(利息收入-FTP成本-风险成本-运营成本)',
    -- 口径
    caliber_type VARCHAR(10) DEFAULT 'ASSESS' COMMENT '口径',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_biz (biz_id),
    INDEX idx_org (org_id),
    INDEX idx_product (product_id),
    INDEX idx_manager (manager_id),
    UNIQUE KEY uk_period_biz_caliber (period, biz_id, caliber_type)
) COMMENT='业务利润汇总表';
```

## 3. 分摊流程设计

### 3.1 分摊执行流程

```
1. 触发分摊（手动/定时/数据变更）
   ↓
2. 读取费用原始数据
   ↓
3. 对于每种费用：
   a. 读取分摊规则
   b. 按源维度汇总费用
   c. 按分摊因子计算每笔业务的分摊比例
   d. 计算每笔业务的分摊金额
   e. 写入分摊结果表
   ↓
4. 汇总每笔业务的运营成本
   ↓
5. 计算每笔业务利润
   ↓
6. 写入业务利润汇总表
   ↓
7. 按维度汇总得到维度利润
```

### 3.2 分摊算法示例

**场景1：部门房租 → 客户经理 → 业务**

```
原始数据：科技部房租 = 10万
第一步：按客户经理人数分摊
  - 科技部有5个客户经理
  - 每个客户经理分摊 = 10万 / 5 = 2万
第二步：按业务量分摊到业务
  - 客户经理A有10笔业务，总余额1000万
  - 业务1余额100万，分摊比例 = 100/1000 = 10%
  - 业务1分摊金额 = 2万 × 10% = 2000元
```

**场景2：IT费用 → 产品 → 业务**

```
原始数据：贷款系统IT费用 = 5万
第一步：按业务金额分摊
  - 该产品有100笔业务，总余额5000万
  - 业务1余额100万，分摊比例 = 100/5000 = 2%
  - 业务1分摊金额 = 5万 × 2% = 1000元
```

## 4. 页面设计

### 4.1 运营成本页面

```
┌─────────────────────────────────────────────────────────┐
│ 运营成本                                            [配置] │
├─────────────────────────────────────────────────────────┤
│ 账期：[2026-06 ▼]  口径：[考核 ▼]  维度：[机构 ▼]  [查询] │
├─────────────────────────────────────────────────────────┤
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐        │
│ │运营成本  │ │平均成本率│ │业务笔数  │ │平均单笔  │        │
│ │ 1234万  │ │  2.5%   │ │  500    │ │ 2.47万  │        │
│ └─────────┘ └─────────┘ └─────────┘ └─────────┘        │
├─────────────────────────────────────────────────────────┤
│ 机构维度汇总                                              │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 机构名称 │ 业务笔数 │ 余额合计 │ 运营成本 │ 平均成本 │ │
│ ├──────────┼─────────┼─────────┼─────────┼─────────┤ │
│ │ 海淀支行 │   150   │ 15000万 │  350万  │  2.33%  │ │
│ │ 西城支行 │   120   │ 12000万 │  280万  │  2.33%  │ │
│ │ 朝阳支行 │   100   │ 10000万 │  230万  │  2.30%  │ │
│ └──────────┴─────────┴─────────┴─────────┴─────────┘ │
│ （点击行可过滤下方明细）                                   │
├─────────────────────────────────────────────────────────┤
│ 业务明细                                                  │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 业务编号 │ 客户名称 │ 机构 │ 产品 │ 余额 │ 运营成本 │ │
│ ├──────────┼─────────┼─────┼─────┼─────┼─────────┤ │
│ │ BIZ001   │ 客户A   │海淀 │贷款 │100万│  2.5万  │ │
│ │ BIZ002   │ 客户B   │西城 │贷款 │200万│  4.8万  │ │
│ └──────────┴─────────┴─────┴─────┴─────┴─────────┘ │
│ （点击运营成本数字 → 弹窗显示费用组成）                     │
└─────────────────────────────────────────────────────────┘
```

### 4.2 费用组成弹窗

```
┌─────────────────────────────────────────────────────────┐
│ 业务BIZ001 - 运营成本组成                           [关闭] │
├─────────────────────────────────────────────────────────┤
│ 运营成本合计：2.5万                                       │
├─────────────────────────────────────────────────────────┤
│ 费用类型      │ 金额    │ 占比   │ 分摊规则              │
│─────────────┼────────┼──────┼─────────────────────│
│ 房租物业     │ 0.8万  │ 32%  │ 按客户经理人数分摊     │
│ 人力成本     │ 1.0万  │ 40%  │ 按客户经理人数分摊     │
│ IT系统       │ 0.3万  │ 12%  │ 按业务金额分摊        │
│ 营销费用     │ 0.4万  │ 16%  │ 按业务金额分摊        │
└─────────────┴────────┴──────┴─────────────────────┘
```

### 4.3 费用原始表页面

```
┌─────────────────────────────────────────────────────────┐
│ 费用原始数据                                        [返回] │
├─────────────────────────────────────────────────────────┤
│ 账期：[2026-06 ▼]                                        │
├─────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 费用类型   │ 维度类型 │ 维度名称 │ 金额    │ 操作    │ │
│ ├────────────┼─────────┼─────────┼────────┼───────┤ │
│ │ 房租物业   │ 部门    │ 科技部   │ 10万   │ [配置] │ │
│ │ 房租物业   │ 部门    │ 市场部   │  8万   │ [配置] │ │
│ │ 人力成本   │ 部门    │ 科技部   │ 50万   │ [配置] │ │
│ │ 人力成本   │ 人员    │ 张三     │  2万   │ [配置] │ │
│ │ IT系统     │ 产品    │ 贷款系统 │  5万   │ [配置] │ │
│ │ 营销费用   │ 机构    │ 北京分行 │ 20万   │ [配置] │ │
│ └────────────┴─────────┴─────────┴────────┴───────┘ │
└─────────────────────────────────────────────────────────┘
```

### 4.4 分摊规则配置弹窗

```
┌─────────────────────────────────────────────────────────┐
│ 分摊规则配置                                        [关闭] │
├─────────────────────────────────────────────────────────┤
│ 费用类型：房租物业                                        │
│ 维度类型：部门                                            │
│ 维度名称：科技部                                          │
│ 金额：10万                                               │
├─────────────────────────────────────────────────────────┤
│ 分摊规则：                                                │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 分摊因子：[客户经理人数 ▼]                            │ │
│ │ 分摊路径：部门 → 客户经理 → 业务                      │ │
│ │ 说明：先按部门人数分摊到客户经理，再按业务量分摊到业务  │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                         │
│ [保存] [取消]                                            │
└─────────────────────────────────────────────────────────┘
```

### 4.5 分摊因子页面

```
┌─────────────────────────────────────────────────────────┐
│ 分摊因子管理                                        [新增] │
├─────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 因子编码   │ 因子名称      │ 因子类型   │ 数据来源  │ │
│ ├────────────┼──────────────┼───────────┼─────────┤ │
│ │MANAGER_COUNT│ 客户经理人数 │ 人员数量  │dimension │ │
│ │BIZ_AMOUNT  │ 业务金额     │ 业务量    │biz_ledger│ │
│ │LOAN_BALANCE│ 贷款余额     │ 资产规模  │loan_...  │ │
│ │BIZ_COUNT   │ 业务笔数     │ 业务量    │biz_ledger│ │
│ │REVENUE     │ 收入金额     │ 收入      │biz_ledger│ │
│ └────────────┴──────────────┴───────────┴─────────┘ │
└─────────────────────────────────────────────────────────┘
```

## 5. API设计

### 5.1 运营成本汇总API

```
GET /api/expense/summary
参数：period, caliberType, dimension
返回：总额、维度汇总、业务明细
```

### 5.2 费用组成API

```
GET /api/expense/biz-composition
参数：period, bizId
返回：该业务的各项费用组成
```

### 5.3 费用原始数据API

```
GET /api/expense/original
参数：period, expenseType
返回：费用原始数据列表
```

### 5.4 分摊规则API

```
GET /api/expense/rules
参数：expenseType
返回：分摊规则列表

POST /api/expense/rules
参数：rule配置
返回：保存结果
```

### 5.5 分摊因子API

```
GET /api/expense/factors
返回：分摊因子列表

POST /api/expense/factors
参数：factor配置
返回：保存结果
```

### 5.6 执行分摊API

```
POST /api/expense/execute
参数：period, expenseType(可选)
返回：执行结果
```

## 6. 实施计划

### 阶段1：数据库设计（0.5天）
- 创建费用原始表
- 创建分摊因子表
- 创建分摊规则表
- 创建分摊结果表
- 创建业务利润汇总表

### 阶段2：后端开发（2天）
- 费用数据CRUD
- 分摊因子CRUD
- 分摊规则CRUD
- 分摊执行引擎
- 汇总查询API

### 阶段3：前端开发（1.5天）
- 运营成本页面
- 费用原始表页面
- 分摊因子页面
- 分摊规则配置弹窗

### 阶段4：联调验收（1天）
- 数据联调
- 功能验收
- 性能优化
