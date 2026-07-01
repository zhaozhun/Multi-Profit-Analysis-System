# 指标数据正确性修复 + 日级数据流水线设计

> 日期：2026-07-01
> 状态：待审阅
> 关联记忆：data-warehouse-migration、expense-allocation-design、multi-profit-indicator-design

## 一、背景与问题

### 1.1 现象（已用数据验证，非推测）

1. **驾驶舱"本年度利润282.55亿"实为2026-01单月利润**
   - `dw_indicator_fact` 中 `period='2026-01'`、`dim_type='TOTAL'`、`indicator_code='TOTAL_PROFIT'` 的 `calc_value=2,825,450.33`（万元）
   - 前端单位换算 `值/10000 + '亿'` → 282.545 ≈ 282.55亿
   - 根因：前端"本年"按钮传 `startDate=2026-01-01`，后端 `getEffectivePeriod` 做 `startDate.substring(0,7)` 截成 `'2026-01'` 单月取数。系统无"年度累计"口径。

2. **驾驶舱其它指标空，是前后端 KPI 卡命名对不上**
   - 前端按中文名取卡：`getCard('贷款收入')`/`getCard('FTP成本')`/`getCard('风险成本')`/`getCard('运营成本')`/`getCard('存款收入')`
   - 后端 `DashboardServiceImpl` 只产出4张卡：`总利润/贷款利润/存款利润/利息收入`
   - 名称对不上 → `getCard` 返回 undefined → 渲染为空。数据其实在表里都有（2026-01 的 FTP_COST/RISK_COST/OP_COST 都有值）。

3. **维度分析各页面"本年度"空，三个原因叠加**
   - DEPT/CUSTOMER 两维度在 `dw_indicator_fact` 完全无数据（ETL 从 ODS→DWD 漏带 dept_id 字段）
   - 同样是单月口径，"本年"只取到1月
   - ORG 等维度只存叶子节点，分行/总行级节点全空（ETL 不沿 parent_id 上卷）

### 1.2 代码核查发现的与记忆/假设不符之处

| 项 | 记忆/假设 | 实际代码核实 |
|---|---|---|
| ODS数据量 | 1800条(900+900) | **9000条(6300+2700)** |
| ODS粒度 | 支持日级 | **每月15号1行，纯月度快照** |
| 贷款笔数 | 900 | **350** |
| DWD dept_id | 应该有 | **没有，ETL漏带（ODS有dept_id，DWD表无此字段）** |
| DWD org_id关联 | 文本匹配 | **org_id已正确，文本JOIN多余** |
| DEPT/CUSTOMER主数据 | 缺失 | **完整且ODS已关联（0未匹配）** |
| ODS dept_id/customer_id | 未确认 | **全部有效** |
| 贷款利率范围 | 3%-15% | **0.12%-382%，有异常值** |
| 运营成本来源 | 真实分摊 | **ETL用RAND()随机生成，与expense_allocation_result脱节** |

### 1.3 核心结论

数据基础比预想的好（主数据完整、ODS维度id已正确关联）。问题根因都是**ETL代码逻辑缺陷**（漏带字段、文本关联、不上卷、单月口径、随机运营成本）+ **缺少日级数据**。

## 二、核心架构原则：骨架独立稳定

**主数据和指标库是"骨架"，独立、稳定，不与具体业务属性/规则强绑定。**

```
┌─────────────────────────────────────────────┐
│  骨架层（独立、稳定，不绑业务规则）              │
│  ├ 主数据 dim_* 分表：维度层级树（每维度独立表） │
│  └ 指标库 indicator_library：指标定义契约        │
│  → 定义"有哪些维度""有哪些指标"                  │
│  → 不关心利率多少、数据怎么造                    │
└─────────────────────────────────────────────┘
                    ↑ 业务挂载其上
┌─────────────────────────────────────────────┐
│  业务层（每笔业务携带主数据字段）                 │
│  每笔贷款/存款 = {                              │
│    biz_id,                                      │
│    org_id, dept_id, product_id, ... (主数据id)  │
│    余额, 利率, 利息, FTP, 风险, 运营, 利润       │
│  }                                              │
│  → 利润天然可沿主数据骨架拆解到任意维度          │
└─────────────────────────────────────────────┘
                    ↑ 数据由生成器产出
┌─────────────────────────────────────────────┐
│  生成规则层（与骨架分离，生成器自有）            │
│  ├ 利率范围：贷款3%-8%，存款1%-2.5%              │
│  ├ 波动率：±2%/日                              │
│  └ 风险成本率：0.1%-0.3%                       │
│  → 放生成器 config.py，不污染骨架               │
└─────────────────────────────────────────────┘
```

### 骨架层边界
- ✅ 指标库可引用业务字段名（source_field）、定义计算公式、指定支持维度和期间
- ✅ 主数据定义维度层级树（parent_id 链）
- ❌ 指标库不写死业务参数（利率范围、波动率）
- ❌ 主数据不背业务规则
- 利率/波动率等生成参数放生成器 `config.py`，与骨架解耦

### 解耦收益
将来接真实数据源时，主数据和指标库不动，只换掉生成器。

## 三、已确认的决策点

1. 走正道（补指标库 + 重写ETL + ADS直读 + 前端按code取卡）
2. 资产/负债严格分域
3. 余额三口径：时点、日均、月均（月均 = 月内日均）
4. 利息/成本/利润时间三态：当日、当月、当年
5. 日级计提型（利息/FTP/风险）由Python生成器每日生成；月度分摊型（运营成本）由费用分摊引擎月度产出，ETL读取真实分摊结果（废弃RAND()）
6. 当日运营成本 = 0，月末分摊后才有
7. 模拟数据：存量500+500业务 + 每日波动
8. 回溯2025-01-01到今天，之后每日增量
9. 本地手动脚本触发，不做定时（上云再考虑）
10. DWD只存月度，日级只在DWS聚合
11. DWD加 dept_id 字段
12. 现有ODS数据全删重建，下游表一并清空，主数据和费用原始表保留
13. 异常贷款利率修正到3%-15%
14. 率值类指标也预计算存入DWS（层级上卷时先汇总分子分母再相除）
15. 利率范围：贷款3%-15%/FTP 1%-2%/风险0.1%-0.3%，存款1%-3%/FTP 2.5%-3.5%
16. 指标库不扩展生成规则字段，保持纯净；生成规则放生成器config.py
17. 骨架（主数据+指标库）独立稳定，不与业务规则强绑定
18. 数据全删重建（ODS/DWD/DWS/分摊结果清空，主数据和费用原始表保留）
19. 全用新命名（资产/负债分域），系统统一按新表新命名
20. 废弃表清理：删15张（指标库7+维度主数据7+biz_ledger 1），新建7张dim_*规范表
21. 维度主数据分表独立管理（dim_organization/dim_biz_line/dim_dept/dim_product/dim_channel/dim_manager/dim_customer_type），不再用dimension_master合一表
22. biz_ledger 方案B：删除，11处引用全部改造改读dw_indicator_fact/ODS明细/dim_*，改全改彻底
23. CUSTOMER维度两层：dim_customer_type（客户分类层级9条）+ customer_master（客户实体200条，带customer_type映射）

## 四、数据模型层设计

### 4.1 ODS层（改造：从月度→日级）
表名不变（`loan_indicator_detail`/`deposit_indicator_detail`），字段已齐备（`stat_date`/`loan_daily_interest`/`loan_cumulative_interest`），改造灌数据方式：
- 现状：每笔业务每月1行（stat_date=当月15号）
- 改造：每笔业务**每日1行**（stat_date=每一天，account_period=当月）
- 字段含义校准：
  - `loan_balance` = 当日时点余额（每日波动）
  - `loan_daily_interest` = 当日计提利息（余额×利率/365）
  - `loan_monthly_interest` = 当月累计利息（月内每日求和）
  - `loan_cumulative_interest` = 业务存续期累计利息

### 4.2 DWD层（改造：补dept_id + 用dim_id关联）
- `dwd_loan_detail`/`dwd_deposit_detail` **新增字段**：`dept_id`、`dept_name`（从ODS带入）
- ETL关联从 `org_name` 文本匹配 → 改为 `dim_id` 直接关联（已验证org_id正确）
- DWD保持月度粒度（每笔业务每月1行）
- 月度聚合规则：
  - 时点余额 = 月末那天
  - 日均余额 = 月内每日时点余额平均
  - 当月利息 = 月内每日 loan_daily_interest 求和
  - op_cost = 从 expense_allocation_result 读取（真实分摊）

### 4.3 DWS层（改造：三期间 + 全维度 + 层级上卷）
`dw_indicator_fact` 表结构基本不变，改造写入逻辑：
- `period_type` 新增值：`DAY`（日级）、`YEAR`（年度累计），保留 `MONTH`
- `dim_type` 补全：`DEPT`、`CUSTOMER`（数据已具备）
- **层级上卷**：每个维度不只写叶子节点，沿 `parent_id` 写到各级（level 1/2/3）。如 ORG 维度既写9个支行，也写5个分行（下属支行汇总）、1个总行（全行汇总）
- `indicator_code` 由 `indicator_library` 驱动，不再硬编码

### 4.4 指标库（新建数据，表结构不变）
灌入约30+指标，资产/负债分域。表结构沿用现有 `indicator_library`，不扩展生成规则字段。

## 五、指标体系（资产/负债分域 × 时间三态）

### 5.1 资产域（贷款）

**规模类（category=SCALE）**
| code | name | unit | 口径 |
|---|---|---|---|
| LOAN_BALANCE | 贷款时点余额 | 万元 | 期末瞬时余额 |
| LOAN_DAVG_BALANCE | 贷款日均余额 | 万元 | 期间每日时点余额算术平均 |
| LOAN_MAVG_BALANCE | 贷款月均余额 | 万元 | 月内每日时点余额算术平均（=月日均） |

**利息收入类（category=REVENUE）**
| code | name | unit | 口径 |
|---|---|---|---|
| LOAN_DAILY_INTEREST | 当日贷款利息收入 | 万元 | 当日余额×利率/365 |
| LOAN_MONTHLY_INTEREST | 当月贷款利息收入 | 万元 | 月内每日求和 |
| LOAN_YEARLY_INTEREST | 当年贷款利息收入 | 万元 | 年内各月求和 |

**成本类（category=COST）**
| code | name | unit | 口径 |
|---|---|---|---|
| LOAN_FTP_COST | 贷款FTP成本 | 万元 | 日/月/年（日级计提） |
| LOAN_RISK_COST | 贷款风险成本 | 万元 | 日/月/年（日级计提） |
| LOAN_OP_COST | 贷款运营成本 | 万元 | 月/年（月度分摊，日级为0） |

**利润类（category=PROFIT，派生）**
| code | name | unit | 公式 |
|---|---|---|---|
| LOAN_DAILY_PROFIT | 当日贷款利润 | 万元 | 当日利息−当日FTP−当日风险−当日运营(0) |
| LOAN_MONTHLY_PROFIT | 当月贷款利润 | 万元 | 当月利息−当月FTP−当月风险−当月运营 |
| LOAN_YEARLY_PROFIT | 当年贷款利润 | 万元 | 当年累计 |

**率值类（category=RATIO，预计算存DWS）**
| code | name | 公式 |
|---|---|---|
| LOAN_RATE | 贷款平均利率 | 年化利息收入/年均余额 |
| LOAN_FTP_RATIO | FTP成本占比 | FTP成本/利息收入 |
| LOAN_RISK_RATIO | 风险成本占比 | 风险成本/利息收入 |

### 5.2 负债域（存款）

**规模类**
| code | name | unit | 口径 |
|---|---|---|---|
| DEPOSIT_BALANCE | 存款时点余额 | 万元 | 期末瞬时 |
| DEPOSIT_DAVG_BALANCE | 存款日均余额 | 万元 | 期间每日平均 |
| DEPOSIT_MAVG_BALANCE | 存款月均余额 | 万元 | 月内每日平均 |

**收入类（FTP收入）**
| code | name | unit | 口径 |
|---|---|---|---|
| FTP_DAILY_INCOME | 当日FTP收入 | 万元 | 当日余额×FTP利率/365 |
| FTP_MONTHLY_INCOME | 当月FTP收入 | 万元 | 月内每日求和 |
| FTP_YEARLY_INCOME | 当年FTP收入 | 万元 | 年内各月求和 |

**支出类（对客利息支出）**
| code | name | unit | 口径 |
|---|---|---|---|
| INTEREST_DAILY_EXPENSE | 当日利息支出 | 万元 | 当日余额×存款利率/365 |
| INTEREST_MONTHLY_EXPENSE | 当月利息支出 | 万元 | 月内每日求和 |
| INTEREST_YEARLY_EXPENSE | 当年利息支出 | 万元 | 年内各月求和 |

**成本类（负债无FTP/风险成本，只有运营）**
| code | name | unit | 口径 |
|---|---|---|---|
| DEPOSIT_OP_COST | 存款运营成本 | 万元 | 月/年（月度分摊） |

**利润类（派生）**
| code | name | unit | 公式 |
|---|---|---|---|
| DEPOSIT_DAILY_PROFIT | 当日存款利润 | 万元 | FTP收入−利息支出−运营(0) |
| DEPOSIT_MONTHLY_PROFIT | 当月存款利润 | 万元 | FTP收入−利息支出−运营 |
| DEPOSIT_YEARLY_PROFIT | 当年存款利润 | 万元 | 当年累计 |

**率值类**
| code | name | 公式 |
|---|---|---|
| DEPOSIT_RATE | 存款付息率 | 年化利息支出/年均余额 |
| FTP_SPREAD | FTP利差 | (FTP收入−利息支出)/年均余额 |

### 5.3 汇总域
| code | name | 公式 |
|---|---|---|
| TOTAL_PROFIT | 总利润 | 贷款利润+存款利润（日/月/年） |
| COST_INCOME_RATIO | 成本收入比 | 总成本/总收入 |
| PROFIT_MARGIN | 利润率 | 总利润/总收入 |

### 5.4 率值类层级上卷规则（重要）
率值不能直接相加汇总。层级上卷时必须**先汇总分子分母再相除**：
- 支行A FTP成本占比10% + 支行B 15% → 分行占比 ≠ 12.5%
- 分行占比 = 分行FTP成本合计 / 分行利息收入合计

## 六、两类成本的生成逻辑（本质区别）

### 第一类：日级计提型（利息收入、FTP成本、风险成本）
- **每天都在发生**，由业务属性（余额×利率）日积月累产生
- 数据生成器每日生成，写进ODS每日1行
- 和"当日余额"强绑定，余额波动则跟着变
- 月度值 = 月内每日求和；年度值 = 年内各月求和（纯累加，无分摊）

```
每笔业务、每一天：
  当日利息收入 = 当日时点余额 × 贷款利率 / 365
  当日FTP成本   = 当日时点余额 × FTP利率 / 365    （贷款侧）
  当日FTP收入   = 当日时点余额 × FTP利率 / 365    （存款侧）
  当日风险成本   = 当日时点余额 × 风险成本率 / 365  （贷款侧，存款无）
```

### 第二类：月度分摊型（运营成本）
- **不是业务自身产生**，是后台费用（房租/人力/IT/营销）按规则分摊到业务
- 分摊一月一次（月末跑分摊引擎），月中为0
- 和业务余额无日级关系

```
每月末：
  费用原始表 → 按分摊规则 → 分摊到每笔业务 → expense_allocation_result（月度）
```

| 维度 | 利息/FTP/风险（日级计提） | 运营成本（月度分摊） |
|---|---|---|
| 产生主体 | 业务自身（余额×率） | 后台费用按规则摊下来 |
| 发生频率 | 每日 | 每月一次 |
| 和余额关系 | 强绑定，日级联动 | 无日级关系 |
| 数据生成者 | Python数据生成器（每日） | 费用分摊引擎（每月） |
| 月度值来源 | 月内每日求和 | 分摊引擎直接产出 |
| 日级值 | 每日有值 | 0 |
| ODS字段 | loan_daily_interest等（每日1行） | op_cost（月度，从分摊表读） |

## 七、ETL逻辑层设计

### 7.1 ETL执行顺序（月度批次）
```
每月末执行：
① 费用分摊引擎 → expense_allocation_result（已有，月度）
② 日级数据生成器补全当月每日ODS数据（Python，每日1行）
③ ETL_MONTHLY：ODS日级 → DWD月度聚合
   - 时点余额 = 月末那天
   - 日均余额 = 月内每日时点余额平均
   - 当月利息 = 月内每日 loan_daily_interest 求和
   - op_cost = 从 expense_allocation_result 读取（真实分摊）
④ ETL_DWS：DWD月度 → dw_indicator_fact（MONTH）
   - 按7维度 × 3层级上卷
   - 写入所有指标code（指标库驱动）
⑤ ETL_YEAR：各月MONTH → YEAR累计
   - 年度利息 = 1~当月各月利息求和
   - 年度余额 = 用日均（年均余额口径）
⑥ 日级DWS（可选）：ODS → dw_indicator_fact（DAY）
   - 当日利息/当日余额，当日op_cost=0
```

### 7.2 关键改造点（对照现有代码）
| 现有代码 | 改造 |
|---|---|
| `DataWarehouseETLServiceImpl` 用RAND()造op_cost | 改为JOIN `expense_allocation_result` 读真实分摊 |
| ETL用 `org_name` 文本JOIN | 改用 `org_id` 等dim_id直接关联 |
| DWD INSERT漏dept_id | 补 dept_id/dept_name 字段 |
| DWS只写叶子节点 | 沿 parent_id 上卷到各级（level 1/2/3） |
| DWS只有period_type=MONTH | 新增 DAY、YEAR |
| indicator_code硬编码 | 从 indicator_library 读取驱动 |
| `substring(0,7)` 取单月 | ADS层按 period_type+period范围直读 |

### 7.3 指标计算公式落地（ETL内）
```
贷款月度：
  LOAN_BALANCE        = 月末loan_balance
  LOAN_DAVG_BALANCE   = avg(每日loan_balance)   -- 月内日均
  LOAN_MAVG_BALANCE   = avg(每日loan_balance)   -- =月日均，同值
  LOAN_MONTHLY_INTEREST = sum(每日loan_daily_interest)
  LOAN_FTP_COST       = sum(每日ftp_cost)
  LOAN_RISK_COST      = sum(每日risk_cost)
  LOAN_OP_COST        = expense_allocation_result当月分摊额
  LOAN_MONTHLY_PROFIT = 利息 - FTP - 风险 - 运营

贷款年度：
  LOAN_YEARLY_INTEREST = sum(1~当月 monthly_interest)
  LOAN_YAVG_BALANCE    = avg(各月日均)  -- 年均余额
  LOAN_YEARLY_PROFIT   = sum(各月利润)
```

## 八、数据生成器设计（Python脚本）

### 8.1 脚本职责（只造日级计提型4样）
```
生成内容：当日时点余额、当日利息、当日FTP、当日风险
不生成：运营成本（月度分摊，另有引擎）
```

### 8.2 脚本结构
```
脚本/数据生成器/
├── generate_daily_data.py    # 主脚本
├── config.py                 # 参数配置（利率范围、波动率、日期范围）—— 与骨架分离
└── 存量业务初始化.py          # 首次初始化500+500业务
```

### 8.3 生成器与骨架的接口

**接口1：生成器读主数据（挂骨架，只读不修改）**
```python
# 从主数据读叶子节点（只读，不修改主数据；每维度独立表）
org_leaves     = query("SELECT id,name FROM dim_organization WHERE level=3")
dept_leaves    = query("SELECT id,name FROM dim_dept WHERE level=3")
product_leaves = query("SELECT id,name FROM dim_product WHERE level=3")
channel_leaves = query("SELECT id,name FROM dim_channel WHERE level=3")
manager_leaves = query("SELECT id,name FROM dim_manager WHERE level=3")
bizline_leaves = query("SELECT id,name FROM dim_biz_line WHERE level=3")
customers      = query("SELECT id,name FROM customer_master")

# 生成一笔业务 = 从叶子节点随机组合（全部用主数据id，不造name）
一笔贷款 = {
    biz_id: "L000351",
    org_id:      random_choice(org_leaves),       # 100（海淀支行）
    dept_id:     random_choice(dept_leaves),      # 57（科技创新处）
    product_id:  random_choice(product_leaves),
    channel_id:  random_choice(channel_leaves),
    manager_id:  random_choice(manager_leaves),
    biz_line_id: random_choice(bizline_leaves),
    customer_id: random_choice(customers),
    # 业务属性（从生成器config读，不从主数据读）
    loan_balance: random(10万, 5000万),
    loan_rate:    random(0.03, 0.15),   # ← 来自config.py，不来自骨架
}
```
关键：业务表里存主数据 `id`（外键），不是name。ETL上卷直接用 parent_id 链。

**接口2：生成器守指标库契约（按公式造数）**
```python
# 读指标库（只读契约）
indicators = query("SELECT code, calc_formula, source_field FROM indicator_library WHERE status=1")

# 按契约造数（公式来自指标库，参数来自config）
# 指标库：LOAN_DAILY_INTEREST = 当日余额 × 贷款利率 / 365
当日利息 = 当日余额 × loan_rate / 365    # 公式守指标库，rate来自config

# 指标库：LOAN_FTP_COST(日) = 当日余额 × FTP利率 / 365
当日FTP = 当日余额 × ftp_rate / 365

# 指标库：LOAN_RISK_COST(日) = 当日余额 × 风险成本率 / 365
当日风险 = 当日余额 × risk_rate / 365
```
指标库公式变了，生成器跟着变；config参数变了，数据范围变但公式不变。两者解耦。

**接口3：ETL上卷沿主数据骨架**
```sql
-- 支行(level=3)汇总到分行(level=2)到总行(level=1)
-- 用主数据自连接，沿parent_id上卷（每维度独立表）
INSERT dw_indicator_fact (dim_type, dim_id, dim_name, indicator_code, calc_value...)
SELECT 'ORG', dm.id, dm.name, 'LOAN_MONTHLY_PROFIT', SUM(d.loan_profit)/10000
FROM dwd_loan_detail d
JOIN dim_organization dm ON d.org_id = dm.id          -- 本级
GROUP BY dm.id
UNION ALL
SELECT 'ORG', dp.id, dp.name, 'LOAN_MONTHLY_PROFIT', SUM(d.loan_profit)/10000
FROM dwd_loan_detail d
JOIN dim_organization dm ON d.org_id = dm.id
JOIN dim_organization dp ON dm.parent_id = dp.id      -- 父级
GROUP BY dp.id;
-- 递归到根
```

### 8.4 核心生成逻辑

**存量业务初始化（一次性）**
```
读取主数据叶子节点（7维度）
生成500笔贷款（L000001~L000500，全删重建后全新编号）：
  - 随机分配维度组合（全部用主数据id）
  - 贷款余额：10万~5000万随机
  - 贷款利率：3%-15%（config.py）
  - FTP利率：1%-2%
  - 风险成本率：0.1%-0.3%
  - 起始日期：2025-01-01前随机
生成500笔存款（D000001~D000500，利率1%-3%，FTP利率2.5%-3.5%，无风险成本）
写入：存量业务主表（记录每笔业务固定属性）
```

**每日数据生成（核心）**
```python
对每个日期 stat_date (2025-01-01 ~ 今天):
  对每笔存量业务:
    # 1. 当日时点余额（基于昨日余额+日内波动）
    当日余额 = 昨日余额 × (1 + 随机波动±2%)
    # 2. 当日利息计提
    当日利息 = 当日余额 × 贷款利率 / 365
    # 3. 当日FTP成本
    当日FTP = 当日余额 × FTP利率 / 365
    # 4. 当日风险成本
    当日风险 = 当日余额 × 风险成本率 / 365
    # 写入ODS（stat_date, account_period=当月）
    INSERT loan_indicator_detail (biz_id, stat_date, account_period,
      loan_balance, loan_rate, loan_daily_interest, ftp_rate, ftp_cost, risk_cost, ...)
```

**月度/累计字段维护**
```
loan_monthly_interest = 月内当日利息求和（到当天为止）
loan_cumulative_interest = 业务存续期当日利息求和
```

### 8.5 触发方式
```bash
# 首次回溯（2025-01-01到今天）
python generate_daily_data.py --start 2025-01-01 --end 2026-07-01

# 之后每日增量
python generate_daily_data.py --date today
```

### 8.6 数据量预估
- 1000笔业务 × 548天 = **54.8万行** ODS数据

### 8.7 config.py 参数（与骨架分离）
```python
# 业务生成参数（不进指标库/主数据）
LOAN_RATE_RANGE = (0.03, 0.08)        # 贷款利率3%-8%(均值~5%,年化)
LOAN_FTP_RATE_RANGE = (0.01, 0.02)    # 贷款FTP利率1%-2%
LOAN_RISK_RATE_RANGE = (0.001, 0.003) # 风险成本率0.1%-0.3%
DEPOSIT_RATE_RANGE = (0.01, 0.025)    # 存款利率1%-2.5%
DEPOSIT_FTP_RATE_RANGE = (0.025, 0.035) # 存款FTP利率2.5%-3.5%
# 目标:年利润~15亿,2026YTD~8亿
# 500笔×均余额~5000万×利润率~5%/12≈月贷款利润1亿,存款利润占30%,月总~1.3亿
LOAN_BALANCE_RANGE = (20000000, 80000000) # 单笔2000万~8000万
DEPOSIT_BALANCE_RANGE = (10000000, 50000000) # 单笔1000万~5000万
DAILY_VOLATILITY = 0.01               # 日波动±1%
START_DATE = "2025-01-01"
LOAN_COUNT = 500
DEPOSIT_COUNT = 500
```

## 九、ADS层 + 前端改造

### 9.1 ADS层改造（Service直读DWS）
**改造原则：Service不再写业务SQL，只按 period_type+period 直读 `dw_indicator_fact`。**

```java
// 现状（错误）：硬编码+substring取单月
String sql = "...WHERE period = ? AND indicator_code IN ('TOTAL_PROFIT',...)";

// 改造后：按指标库驱动+期间类型直读
List<String> codes = indicatorLibraryService.getCodesByCategory("PROFIT");
String sql = "...WHERE period_type = ? AND period BETWEEN ? AND ?
              AND indicator_code IN (?) AND dim_type = ? AND caliber_type = ?";
```

三个Service改造点：
| Service | 现状问题 | 改造 |
|---|---|---|
| DashboardServiceImpl | substring(0,7)取单月；只产4张卡，命名和前端对不上 | 按 period_type(YEAR)直读年度累计；卡片由指标库驱动，code/name统一 |
| DimensionServiceImpl | 同上；DEPT/CUSTOMER无数据；无层级上卷 | 直读DWS（已补全维度+层级）；按dim_type+level取 |
| 指标数据页Service | 待查 | 同样改直读DWS |

### 9.2 前端改造
- KPI卡片按 `metricCode` 取值，不再按中文名（消除命名漂移）
- "本年"按钮传 `period_type=YEAR, period=2026`，后端直读年度累计
- 默认日期取最新有数据期间（不再落到无数据的当月）

### 9.3 期间参数统一协议
```
period_type: DAY / MONTH / YEAR
period:
  DAY   → '2026-07-01'
  MONTH → '2026-06'
  YEAR  → '2026'
```
"本年" = `period_type=YEAR, period=2026` → 后端读 `dw_indicator_fact WHERE period_type='YEAR' AND period='2026'`，一次拿到全年累计，不再 substring。

## 十、数据重建范围

```
清空（重建）：
  loan_indicator_detail, deposit_indicator_detail (ODS)
  dwd_loan_detail, dwd_deposit_detail (DWD)
  dw_indicator_fact (DWS)
  expense_allocation_result (分摊结果，需重跑分摊)

保留（不动）：
  dimension_master, customer_master (主数据)
  indicator_library (本次新建灌入)
  expense_rent/salary/it/marketing/other (费用原始表，分摊引擎重算时用)
```

## 十一、执行顺序（整体）

```
① 指标库灌入（indicator_library）—— 定义所有指标的计算规则
    ↓ （指标库定义了计算公式，不定义生成参数）
② 主数据已就绪（dimension_master, customer_master）—— 不动，已存在
    ↓ （主数据提供维度骨架）
③ 费用分摊引擎重跑 → expense_allocation_result（月度）
    ↓
④ 存量业务初始化 —— 从主数据叶子节点组合，属性受config约束
    ↓
⑤ 每日数据生成 —— 按指标库公式造日级数，挂在主数据维度上
    ↓
⑥ ETL —— 按指标库定义汇总，沿主数据层级上卷，读真实分摊运营成本
    ↓
⑦ DWS —— 存指标库定义的所有指标（DAY/MONTH/YEAR × 全维度 × 全层级）
    ↓
⑧ ADS+前端 —— 直读DWS，按code取卡，期间参数统一
```

## 十二、数据清理与表结构统一

### 12.1 数据库现状核查（已查实）
系统共37张表，存在大量废弃表、重复表、双轨制表，必须清理统一。全用新命名（资产/负债分域），系统统一按新表新命名。

### 12.2 要删除的表（15张）

**指标库废弃表（7张，后端零引用或被indicator_library取代）：**
- `indicator_definition`（26行，旧指标定义半成品，被indicator_library取代）
- `atomic_indicator`（9行，零引用）
- `derived_indicator`（9行，零引用）
- `indicator_precomputed`（1743行，零引用，旧预计算）
- `indicator_pre_calc`（0行，零引用，旧预计算）
- `indicator_summary`（0行，零引用）
- `indicator_stat_config`（18行，零引用）

**维度主数据废弃表（7张，被新建dim_*规范表取代）：**
- `dimension_master`（162行，7维度合一，被dim_*分表取代）
- `dw_dim_organization`（6行，数据不完整缺层级）
- `dw_dim_biz_line`、`dw_dim_product`、`dw_dim_channel`、`dw_dim_manager`、`dw_dim_customer`（与dimension_master数据重复的冗余副本）

**业务台账（1张，被资产/负债分域ODS表取代）：**
- `biz_ledger`（8865行，贷款存款合一宽表，与新设计资产/负债分域冲突）

### 12.3 要新建的表（7张，维度主数据分表）

每张表统一结构：`id`/`code`/`name`/`parent_id`/`level`/`sort_order`/`status`/`create_time`/`update_time`

| 表名 | 内容 | 数据来源 |
|---|---|---|
| `dim_organization` | 机构层级（总行→分行→支行，15条，3级） | 从 dimension_master WHERE dim_type='ORG' 迁移 |
| `dim_biz_line` | 条线层级（21条） | 迁移 dim_type='BIZ_LINE' |
| `dim_dept` | 部门层级（43条） | 迁移 dim_type='DEPT' |
| `dim_product` | 产品层级（34条） | 迁移 dim_type='PRODUCT' |
| `dim_channel` | 渠道层级（14条） | 迁移 dim_type='CHANNEL' |
| `dim_manager` | 客户经理层级（26条） | 迁移 dim_type='MANAGER' |
| `dim_customer_type` | 客户分类层级（对公/个人/大型企业等，9条） | 迁移 dim_type='CUSTOMER' |

`customer_master`（200条客户实体）保留不动，补 `customer_type` 到 `dim_customer_type` 的映射。

### 12.4 要改造的表（6张，保留改逻辑）

| 表名 | 改造内容 |
|---|---|
| `loan_indicator_detail`（ODS） | 日级化（每日1行），字段含义校准 |
| `deposit_indicator_detail`（ODS） | 日级化（每日1行），字段含义校准 |
| `dwd_loan_detail`（DWD） | 新增dept_id/dept_name字段，关联改用dim_id |
| `dwd_deposit_detail`（DWD） | 新增dept_id/dept_name字段，关联改用dim_id |
| `dw_indicator_fact`（DWS） | 新增DAY/YEAR期间，补DEPT/CUSTOMER维度，沿parent_id层级上卷，indicator_code由指标库驱动 |
| `indicator_library`（指标库） | 灌入30+指标定义（资产/负债分域新命名） |

### 12.5 要保留不动的表

- `customer_master`（客户实体，200条）
- `expense_allocation_result`、`expense_allocation_rule`、`allocation_factor`（费用分摊）
- `cost_type_config`、`cost_allocation_result`、`cost_actual_record`（成本配置）
- `expense_rent`、`expense_salary`、`expense_it`、`expense_marketing`、`expense_other`（费用原始表）
- `custom_report_template`、`alert_record`（功能表）

### 12.6 biz_ledger 删除后的代码改造（11个文件，约52处SQL）

**改造核心原则：大部分聚合查询改读 DWS层 `dw_indicator_fact`（已按维度+期间预聚合）；明细导出才读 ODS loan/deposit_indicator_detail；JOIN dimension_master 改为 JOIN 对应 dim_* 表。**

**改造中发现的问题（必须一并修复）：**
1. MCP代码用 `WHERE period = ?`，但biz_ledger实际字段是 `account_period`——这些MCP本来就是坏的，改造时统一对接 dw_indicator_fact 的 `period`+`period_type`
2. biz_ledger混合字段语义需重新映射：`revenue`(贷款)=loan_monthly_interest、`revenue`(存款)=ftp_income；`net_profit`(贷款)=loan_profit、`net_profit`(存款)=deposit_profit；ftp_cost/risk_cost只贷款有
3. ReportController/ExportServiceImpl 里 `JOIN dimension_master` 同步改为 `JOIN dim_organization` 等

**11处改造清单：**

| 文件 | 引用数 | 改造策略 |
|---|---|---|
| AiServiceImpl | 3处 | AI提示词里"表名biz_ledger"改为引导AI查dw_indicator_fact，schema描述同步更新 |
| FunctionRegistry | 2处 | `query_biz_ledger`函数改为查dw_indicator_fact |
| BizDataMcpServer | 4处 | period→对接dw_indicator_fact的period+period_type |
| AnalysisMcpServer | 1处 | 维度分组查询改读dw_indicator_fact |
| ReportMcpServer | 1处 | 汇总查询改读dw_indicator_fact的TOTAL行 |
| GovernanceMcpServer | 6处 | 数据治理检查改读dw_indicator_fact |
| ReportController | 9处 | JOIN dimension_master→dim_*，聚合改读dw_indicator_fact |
| DataGovernanceController | 8处 | 同上 |
| DataValidationServiceImpl | 2处 | 数据校验改读dw_indicator_fact |
| ExportServiceImpl | 8处 | 明细导出读loan/deposit_indicator_detail，JOIN改dim_* |
| AiExploreController | 8处 | AI探索（写死2026-05）改读dw_indicator_fact，去掉硬编码期间 |

### 12.7 指标命名统一（全用新命名）

资产/负债分域新命名，废弃旧命名：

| 旧命名（废弃） | 新命名 | 说明 |
|---|---|---|
| INTEREST_INCOME | LOAN_DAILY/MONTHLY/YEARLY_INTEREST | 贷款利息收入，资产侧 |
| FTP_INCOME | FTP_DAILY/MONTHLY/YEARLY_INCOME | FTP收入，负债侧 |
| FTP_COST | LOAN_FTP_COST | 贷款FTP成本（资产侧，负债无） |
| RISK_COST | LOAN_RISK_COST | 贷款风险成本（资产侧，负债无） |
| OP_COST | LOAN_OP_COST / DEPOSIT_OP_COST | 运营成本按资产负债分 |
| LOAN_PROFIT | LOAN_DAILY/MONTHLY/YEARLY_PROFIT | 贷款利润三态 |
| DEPOSIT_PROFIT | DEPOSIT_DAILY/MONTHLY/YEARLY_PROFIT | 存款利润三态 |
| TOTAL_PROFIT | TOTAL_PROFIT（保留，分日/月/年） | 总利润 |
| INTEREST_EXPENSE/DEPOSIT_INTEREST | INTEREST_DAILY/MONTHLY/YEARLY_EXPENSE | 存款利息支出三态 |
| LIABILITY_OP_COST | DEPOSIT_OP_COST | 负债运营成本（并入分域命名） |

余额类新增：LOAN_BALANCE/LOAN_DAVG_BALANCE/LOAN_MAVG_BALANCE、DEPOSIT_BALANCE/DEPOSIT_DAVG_BALANCE/DEPOSIT_MAVG_BALANCE

## 十三、执行顺序（完整，含清理）

```
① 指标库灌入（indicator_library）—— 30+指标新命名定义
    ↓
② 维度主数据迁移：dimension_master → 新建dim_* 6张表 + dim_customer_type
    ↓
③ 删除废弃表15张（指标库7 + 维度主数据7 + biz_ledger 1）
    ↓
④ biz_ledger 11处引用改造（改读dw_indicator_fact / ODS明细 / dim_*）
    ↓
⑤ 数据全删重建（ODS/DWD/DWS/expense_allocation_result清空）
    ↓
⑥ 费用分摊引擎重跑 → expense_allocation_result（月度）
    ↓
⑦ 存量业务初始化（500+500，挂dim_*叶子节点，属性受config约束）
    ↓
⑧ 每日数据生成（Python，按指标库公式造日级数，日级计提型4样）
    ↓
⑨ ETL（按指标库定义汇总，沿dim_*层级上卷，读真实分摊运营成本）
    ↓
⑩ DWS（存指标库定义的所有指标，DAY/MONTH/YEAR × 全维度 × 全层级）
    ↓
⑪ ADS+前端（直读DWS，按metricCode取卡，期间参数period_type+period统一）
```

## 十四、待定/风险

1. **日级DWS数据量**：1000笔×548天×多维度多指标，dw_indicator_fact DAY粒度行数可能较大，需评估（可只对核心指标存DAY，其余只存MONTH/YEAR）
2. **业务生命周期**：存量业务到期后余额归零的处理（本次简化为持续存续，不做到期）
3. **费用分摊引擎现状**：需确认现有分摊引擎能否按月重跑产出 expense_allocation_result（前面查到该表有数据，引擎可用）
