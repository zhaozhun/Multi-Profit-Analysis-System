# 指标数据系统设计文档

## 1. 概述

### 1.1 背景
多维盈利分析系统需要完善指标数据展示功能，支持不同颗粒度的费用数据展示，并优化左侧菜单结构。

### 1.2 核心需求
1. 原始数据不动，加工后的表和原始表关联
2. 指标数据页面支持二级卡片导航
3. 每个费用类型有独立详情页面
4. 维度数据逻辑保持现有实现
5. 左侧菜单保持现有风格

### 1.3 设计原则
- 原始数据不可变
- 加工数据可追溯
- 页面结构清晰
- 查询性能优化

---

## 2. 数据层设计

### 2.1 数据表结构

#### indicator_summary（指标汇总表）
```sql
CREATE TABLE indicator_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(7) NOT NULL COMMENT '账期月份（2026-01）',
    indicator_code VARCHAR(50) NOT NULL COMMENT '指标编码',
    indicator_type VARCHAR(20) NOT NULL COMMENT '指标类型：ATOMIC/DERIVED',
    business_line VARCHAR(20) NOT NULL COMMENT '业务条线：ASSET/LIABILITY',
    calc_value DECIMAL(18,4) COMMENT '汇总值',
    calc_time DATETIME COMMENT '计算时间',
    status TINYINT DEFAULT 1 COMMENT '状态',
    INDEX idx_period (period),
    INDEX idx_indicator (indicator_code),
    INDEX idx_business_line (business_line)
) COMMENT='指标汇总表';
```

#### cost_allocation_result（费用分摊结果表）
```sql
CREATE TABLE cost_allocation_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(7) NOT NULL COMMENT '账期月份',
    cost_type VARCHAR(50) NOT NULL COMMENT '费用类型',
    source_dim_type VARCHAR(20) NOT NULL COMMENT '来源维度类型',
    source_dim_code VARCHAR(50) NOT NULL COMMENT '来源维度编码',
    target_account_id VARCHAR(30) NOT NULL COMMENT '目标账户ID',
    allocated_amount DECIMAL(18,4) COMMENT '分摊金额',
    allocation_rule_id BIGINT COMMENT '分摊规则ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_cost_type (cost_type),
    INDEX idx_target_account (target_account_id)
) COMMENT='费用分摊结果表';
```

### 2.2 数据关联关系

```
原始表（不动）
├── cost_actual_record（费用实际发生记录）
├── employee_work_hours（员工工时记录）
└── ...其他原始数据

加工表（新增）
├── indicator_summary（指标汇总表）
│   └── 存储预计算的指标汇总值
├── cost_allocation_result（费用分摊结果表）
│   └── 存储分摊到账户的结果

现有表
├── biz_ledger（账户级数据）
│   └── op_cost 字段存储分摊后的运营成本
└── dimension_master（维度主数据）
```

### 2.3 查询关联方式

```
指标数据查询：
├── 汇总值：从 indicator_summary 查询
└── 账户级明细：从 biz_ledger 查询

费用明细查询：
├── 分摊结果：从 cost_allocation_result 查询
└── 原始数据：从 cost_actual_record 等原始表查询

维度分析查询：
└── 直接从 biz_ledger GROUP BY 维度字段
```

---

## 3. 前端页面设计

### 3.1 指标数据页面结构

```
指标数据页面（/indicator-data）
├── 顶部：资产/负债 Tab 切换
├── 中部：一级指标卡片区域
│   ├── 资产Tab：
│   │   ├── 对客利息收入卡片
│   │   ├── FTP成本卡片
│   │   ├── 风险成本卡片
│   │   └── 运营成本卡片
│   └── 负债Tab：
│       ├── FTP收入卡片
│       ├── 利息支出卡片
│       └── 运营成本卡片
└── 下方：内容区域
    ├── 默认：显示选中指标的汇总数据
    └── 点击"运营成本"后：
        └── 显示二级卡片网格
            ├── 数据使用费卡片
            ├── 催收费卡片
            ├── 人工成本卡片
            ├── IT成本卡片
            ├── 营销费用卡片
            └── ...（更多费用类型）
```

### 3.2 二级卡片详情页面

```
费用详情页面（/indicator-data/expense/:costType）
├── 顶部：费用类型名称 + 返回按钮
├── 汇总区域：
│   ├── 月日均值
│   ├── 年日均值
│   └── 当月累计值
├── 账户级明细表格：
│   ├── 账户ID
│   ├── 账户名称
│   ├── 分摊金额
│   └── 分摊规则
└── 原始数据折叠面板：
    ├── 按部门汇总
    ├── 按机构汇总
    └── 按员工汇总
```

### 3.3 页面交互流程

```
用户操作流程：
1. 进入指标数据页面
2. 选择资产/负债Tab
3. 看到一级指标卡片
4. 点击"运营成本"卡片
5. 看到二级费用类型卡片网格
6. 点击某个费用类型（如"数据使用费"）
7. 进入费用详情页面
8. 查看汇总数据和明细
```

---

## 4. 后端API设计

### 4.1 指标数据API

#### 获取指标汇总数据
```
GET /api/indicator/summary
参数：
  - businessLine: ASSET/LIABILITY（业务条线）
  - period: 2026-01（账期月份）
  - statType: MONTHLY_DAILY_AVG/YEARLY_DAILY_AVG（统计类型）

返回：
{
  "code": "ASSET_INTEREST",
  "name": "对客利息收入",
  "value": 12345.67,
  "unit": "万元",
  "period": "2026-01"
}
```

#### 获取指标明细数据
```
GET /api/indicator/detail/{indicatorCode}
参数：
  - period: 2026-01
  - page: 1
  - size: 20

返回：
{
  "code": "ASSET_INTEREST",
  "name": "对客利息收入",
  "total": 12345.67,
  "details": [
    {
      "accountId": "ACC001",
      "accountName": "账户1",
      "amount": 100.00
    },
    ...
  ]
}
```

### 4.2 费用分摊API

#### 获取费用类型列表
```
GET /api/allocation/cost-types
参数：
  - businessLine: ASSET/LIABILITY

返回：
[
  {
    "code": "DATA_USAGE",
    "name": "数据使用费",
    "businessLine": "ASSET"
  },
  {
    "code": "COLLECTION",
    "name": "催收费",
    "businessLine": "ASSET"
  },
  ...
]
```

#### 获取费用分摊结果
```
GET /api/allocation/result/{costType}
参数：
  - period: 2026-01
  - page: 1
  - size: 20

返回：
{
  "costType": "DATA_USAGE",
  "costTypeName": "数据使用费",
  "totalAmount": 500.00,
  "details": [
    {
      "accountId": "ACC001",
      "accountName": "账户1",
      "allocatedAmount": 50.00,
      "allocationRule": "按工时分摊"
    },
    ...
  ]
}
```

#### 获取费用原始数据
```
GET /api/allocation/original/{costType}
参数：
  - period: 2026-01
  - dimType: DEPT/ORG（维度类型）

返回：
{
  "costType": "DATA_USAGE",
  "dimType": "DEPT",
  "details": [
    {
      "dimCode": "DEPT001",
      "dimName": "部门A",
      "amount": 150.00
    },
    ...
  ]
}
```

### 4.3 指标预计算API

#### 触发指标预计算
```
POST /api/indicator/calculate
参数：
  - period: 2026-01
  - indicatorCode: ASSET_INTEREST（可选，不传则计算所有指标）

返回：
{
  "success": true,
  "message": "计算完成",
  "calculatedCount": 9
}
```

---

## 5. 费用分摊流程设计

### 5.1 分摊流程概述

```
费用分摊流程：
1. 原始费用数据录入（cost_actual_record）
   └── 颗粒度：部门/机构/员工等

2. 分摊规则配置（allocation_rule_config）
   └── 定义分摊因子、权重、算法

3. 执行分摊计算
   └── 按规则将费用分摊到账户级

4. 分摊结果存储
   ├── cost_allocation_result（分摊明细）
   └── biz_ledger.op_cost（账户级汇总）

5. 指标预计算
   └── 汇总到 indicator_summary 表
```

### 5.2 分摊规则示例

#### 数据使用费（按部门工时分摊）
```
规则配置：
- 费用类型：DATA_USAGE
- 来源维度：DEPT（部门）
- 目标维度：ACCOUNT（账户）
- 分摊因子：employee_work_hours（员工工时）
- 分摊算法：WEIGHTED（加权）

分摊逻辑：
1. 获取部门A的数据使用费：100万元
2. 获取部门A所有员工的工时记录
3. 按工时比例分摊到各员工负责的账户
4. 汇总到账户级
```

#### 催收费（按机构逾期金额分摊）
```
规则配置：
- 费用类型：COLLECTION
- 来源维度：ORG（机构）
- 目标维度：ACCOUNT（账户）
- 分摊因子：biz_ledger.risk_cost（风险成本）
- 分摊算法：RATIO（比例）

分摊逻辑：
1. 获取机构X的催收费：200万元
2. 获取机构X所有账户的风险成本
3. 按风险成本比例分摊到各账户
4. 汇总到账户级
```

### 5.3 分摊结果关联

#### 分摊结果存储
```
cost_allocation_result 表：
├── period: 2026-01
├── cost_type: DATA_USAGE
├── source_dim_type: DEPT
├── source_dim_code: DEPT001
├── target_account_id: ACC001
├── allocated_amount: 50.00
└── allocation_rule_id: 1
```

#### 账户级汇总
```
biz_ledger 表：
├── biz_id: ACC001
├── stat_date: 2026-01-31
├── op_cost: 150.00（分摊后的运营成本）
└── ...其他字段
```

#### 指标汇总
```
indicator_summary 表：
├── period: 2026-01
├── indicator_code: ASSET_OP
├── indicator_type: ATOMIC
├── business_line: ASSET
├── calc_value: 1200.00（所有账户运营成本汇总）
└── calc_time: 2026-02-01 02:00:00
```

---

## 6. 左侧菜单结构设计

### 6.1 优化后的菜单结构

```
左侧菜单：
├── 驾驶舱（一级）
├── 维度分析（一级，展开显示子菜单）
│   ├── 机构分析
│   ├── 条线分析
│   ├── 产品分析
│   ├── 渠道分析
│   ├── 部门分析
│   └── 客户经理分析
├── 指标数据（一级，点击进入页面）
├── 费用分摊（一级，展开显示子菜单）
│   ├── 分摊配置
│   │   ├── 费用类型
│   │   ├── 分摊因子
│   │   └── 分摊规则
│   ├── 费用管理
│   │   ├── 费用记录
│   │   ├── 员工费用分摊
│   │   ├── 产品分润
│   │   └── 运营费用分摊
│   └── 分摊结果
│       ├── 分摊执行
│       ├── 结果查询
│       └── 统计分析
├── 主数据管理（一级，展开显示子菜单）
│   ├── 维度主数据
│   │   ├── 机构
│   │   ├── 条线
│   │   ├── 部门
│   │   ├── 产品
│   │   ├── 渠道
│   │   ├── 客户经理
│   │   └── 客户
│   └── 指标库
│       ├── 规模指标
│       ├── 资产指标
│       ├── 负债指标
│       ├── 利润指标
│       └── 效率指标
├── 报表中心（一级，展开显示子菜单）
│   ├── 台账报表
│   ├── 盈利报表
│   ├── 自定义报表
│   └── AI报表
├── 数据治理（一级）
└── AI助手（一级）
```

### 6.2 菜单交互说明

**菜单行为：**
- 一级菜单：点击展开/折叠子菜单
- 二级菜单：点击进入对应页面
- 三级菜单：点击进入对应页面
- 当前页面高亮显示

**菜单样式：**
- 使用 Ant Design Menu 组件
- 支持折叠（collapsed状态）
- 深色主题（theme="dark"）

---

## 7. 实现计划

### 7.1 后端实现
1. 创建 indicator_summary 表
2. 创建 cost_allocation_result 表
3. 实现指标汇总API
4. 实现费用分摊结果API
5. 实现指标预计算任务

### 7.2 前端实现
1. 优化指标数据页面
2. 实现二级卡片导航
3. 实现费用详情页面
4. 优化左侧菜单结构

### 7.3 数据迁移
1. 执行建表脚本
2. 初始化费用类型数据
3. 配置分摊规则
4. 执行首次指标预计算

---

## 8. 验证标准

### 8.1 功能验证
- [ ] 指标数据页面正确显示资产/负债指标
- [ ] 二级卡片导航正常工作
- [ ] 费用详情页面显示正确数据
- [ ] 左侧菜单结构正确

### 8.2 性能验证
- [ ] 指标汇总查询响应时间 < 1秒
- [ ] 费用明细查询响应时间 < 2秒
- [ ] 页面加载时间 < 3秒

### 8.3 数据验证
- [ ] 分摊结果与原始数据一致
- [ ] 指标汇总值正确
- [ ] 维度分析数据正确
