# 指标数据正确性修复 + 日级数据流水线 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于现有数仓分层(ODS→DWD→DWS→ADS),实现日级数据流水线,解决驾驶舱/维度分析数据错误,资产/负债严格分域,主数据分表独立管理,指标库驱动。

**Architecture:** 骨架层(dim_*分表+indicator_library)独立稳定不绑业务规则→业务层(Python生成器造日级计提数据,费用分摊引擎造月度运营成本)→ETL沿主数据层级上卷预计算→ADS直读DWS→前端按metricCode取卡。

**Tech Stack:** Python 3.12(数据生成器),Java/Spring Boot 3.2(ETL+服务),MySQL 8.0(存储),MyBatis(已有ORM),React 18+TypeScript(前端)

**Spec:** `docs/superpowers/specs/2026-07-01-indicator-correctness-design.md`

## Global Constraints

- 资产/负债严格分域,全用新命名(LOAN_*/DEPOSIT_*/FTP_*等),废弃旧命名(INTEREST_INCOME/FTP_COST/RISK_COST等)
- 主数据分表独立管理(dim_organization/dim_biz_line/dim_dept/dim_product/dim_channel/dim_manager/dim_customer_type),不再用dimension_master/dw_dim_*
- 指标库统一用indicator_library(灌入30+指标),删indicator_definition/atomic_indicator/derived_indicator等废弃指标表
- 删biz_ledger,11处引用全部改造读dw_indicator_fact/ODS明细/dim_*
- 日级计提型(利息/FTP/风险)Python生成器每日生成,月度分摊型(运营成本)费用分摊引擎月度产出
- 当日运营成本=0,月末分摊后才有
- 模拟数据500+500存量业务+每日波动,回溯2025-01-01到今天
- 每步完成commit,commit message用中文描述+英文feat/chore前缀
- 先确认后执行(全局铁律1),plan内标记[确认点]的步骤需暂停等待用户确认

---

## File Structure

```
多维盈利分析/
├── 数据库脚本/                    # SQL脚本(新建/修改)
│   ├── dim_tables.sql             # 新建 dim_* 7张表 + 迁移数据
│   ├── indicator_library_data.sql # 灌入30+指标定义
│   ├── drop_obsolete_tables.sql   # 删15张废弃表
│   └── cleanup_rebuild.sql        # 清空ODS/DWD/DWS/expense_allocation_result
├── 脚本/数据生成器/               # Python数据生成器(新建)
│   ├── config.py                  # 生成参数(利率范围/波动率,与骨架分离)
│   ├── init_biz.py                # 存量业务初始化(500+500)
│   └── generate_daily.py          # 每日数据生成
├── 后端/src/main/java/com/multiprofit/
│   ├── service/impl/
│   │   ├── DataWarehouseETLServiceImpl.java  # 重写ETL(日级+层级上卷+真实分摊)
│   │   ├── DashboardServiceImpl.java         # 直读DWS,period_type+period
│   │   ├── DimensionServiceImpl.java         # 直读DWS,period_type+period
│   │   ├── IndicatorDetailServiceImpl.java   # 直读DWS
│   │   ├── AiServiceImpl.java               # AI提示词改表名
│   │   ├── ExportServiceImpl.java            # JOIN dim_*,读新表
│   │   └── DataValidationServiceImpl.java    # 改读dw_indicator_fact
│   ├── controller/
│   │   ├── ReportController.java             # JOIN dim_*,读新表
│   │   ├── DataGovernanceController.java     # 改读dw_indicator_fact
│   │   ├── AiExploreController.java          # 改读dw_indicator_fact
│   │   └── IndicatorController.java          # 删indicator_definition引用,读indicator_library
│   ├── ai/
│   │   └── FunctionRegistry.java             # query_biz_ledger→查dw_indicator_fact
│   ├── mcp/
│   │   ├── BizDataMcpServer.java             # 改读dw_indicator_fact
│   │   ├── AnalysisMcpServer.java            # 改读dw_indicator_fact
│   │   ├── ReportMcpServer.java              # 改读dw_indicator_fact
│   │   └── GovernanceMcpServer.java          # 改读dw_indicator_fact
│   └── dto/
│       └── DashboardDTO.java                 # KPI卡新增metricCode字段
├── 前端/src/pages/
│   ├── Dashboard/index.tsx                   # 按metricCode取卡,期间参数period_type+period
│   └── DimensionAnalysis/index.tsx           # 按metricCode取卡,期间参数period_type+period
└── docs/superpowers/
    ├── specs/2026-07-01-indicator-correctness-design.md  # 设计文档(已完成)
    └── plans/2026-07-01-indicator-correctness.md         # 本文件
```

---

## Phase 1: 基础骨架(数据库清理+dim_*建表+指标库灌入)

独立可测,所有后续阶段的前置依赖。

### Task 1: 删除废弃表 + 清空重建数据

**Files:**
- Create: `数据库脚本/drop_obsolete_tables.sql`
- Create: `数据库脚本/cleanup_rebuild.sql`

**Interfaces:**
- Produces: 干净的数据库环境(保留主数据+费用原始表,删15张废弃表,清空ODS/DWD/DWS)

- [ ] **Step 1: 编写删除废弃表SQL**

```sql
-- 数据库脚本/drop_obsolete_tables.sql
-- 废弃指标表(7张)
DROP TABLE IF EXISTS indicator_definition;
DROP TABLE IF EXISTS atomic_indicator;
DROP TABLE IF EXISTS derived_indicator;
DROP TABLE IF EXISTS indicator_precomputed;
DROP TABLE IF EXISTS indicator_pre_calc;
DROP TABLE IF EXISTS indicator_summary;
DROP TABLE IF EXISTS indicator_stat_config;

-- 废弃维度主数据表(7张,数据将迁移到dim_*后删除)
-- 注:dimension_master和dw_dim_*在Task 2完成数据迁移后再删,
-- 此处先记录,Task 2最后执行
-- DROP TABLE IF EXISTS dimension_master;
-- DROP TABLE IF EXISTS dw_dim_organization;
-- DROP TABLE IF EXISTS dw_dim_biz_line;
-- DROP TABLE IF EXISTS dw_dim_product;
-- DROP TABLE IF EXISTS dw_dim_channel;
-- DROP TABLE IF EXISTS dw_dim_manager;
-- DROP TABLE IF EXISTS dw_dim_customer;

-- 业务台账(1张)
DROP TABLE IF EXISTS biz_ledger;
```

- [ ] **Step 2: 编写清空重建SQL**

```sql
-- 数据库脚本/cleanup_rebuild.sql
-- 清空ODS层
TRUNCATE TABLE loan_indicator_detail;
TRUNCATE TABLE deposit_indicator_detail;

-- 清空DWD层
TRUNCATE TABLE dwd_loan_detail;
TRUNCATE TABLE dwd_deposit_detail;

-- 清空DWS层
TRUNCATE TABLE dw_indicator_fact;

-- 清空费用分摊结果(需重跑分摊引擎)
TRUNCATE TABLE expense_allocation_result;

-- 注意:费用原始表(expense_rent/salary/it/marketing/other)保留不动
-- 主数据(customer_master)保留不动
```

- [ ] **Step 3: 执行SQL**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析/
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit < 数据库脚本/drop_obsolete_tables.sql
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit < 数据库脚本/cleanup_rebuild.sql
```

- [ ] **Step 4: 验证废弃表已删除,数据已清空**

```bash
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit -e "
SELECT '=== 确认废弃表已删除 ===' AS chk;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='multi_profit' AND table_name IN ('indicator_definition','atomic_indicator','derived_indicator','indicator_precomputed','indicator_pre_calc','indicator_summary','indicator_stat_config','biz_ledger');
SELECT '=== 确认数据已清空(应为0) ===' AS chk;
SELECT 'loan_indicator_detail' AS tbl, COUNT(*) cnt FROM loan_indicator_detail
UNION ALL SELECT 'deposit_indicator_detail', COUNT(*) FROM deposit_indicator_detail
UNION ALL SELECT 'dwd_loan_detail', COUNT(*) FROM dwd_loan_detail
UNION ALL SELECT 'dwd_deposit_detail', COUNT(*) FROM dwd_deposit_detail
UNION ALL SELECT 'dw_indicator_fact', COUNT(*) FROM dw_indicator_fact
UNION ALL SELECT 'expense_allocation_result', COUNT(*) FROM expense_allocation_result;
SELECT '=== 确认保留表未动 ===' AS chk;
SELECT 'dimension_master' AS tbl, COUNT(*) FROM dimension_master
UNION ALL SELECT 'customer_master', COUNT(*) FROM customer_master
UNION ALL SELECT 'expense_rent', COUNT(*) FROM expense_rent;
" 2>&1 | grep -v Warning
```
Expected: 废弃表计数=0,数据表全部cnt=0,保留表cnt>0

- [ ] **Step 5: Commit**

```bash
git add 数据库脚本/drop_obsolete_tables.sql 数据库脚本/cleanup_rebuild.sql
git commit -m "chore: 删除8张废弃表+清空ODS/DWD/DWS/分摊结果数据"
```

---

### Task 2: 新建 dim_* 7张规范表 + 从dimension_master迁移数据

**Files:**
- Create: `数据库脚本/dim_tables.sql`

**Interfaces:**
- Produces: dim_organization(15)/dim_biz_line(21)/dim_dept(43)/dim_product(34)/dim_channel(14)/dim_manager(26)/dim_customer_type(9) 7张表
- Consumes: dimension_master(162行,迁移后删除)

- [ ] **Step 1: 编写建表+迁移SQL**

```sql
-- 数据库脚本/dim_tables.sql
-- 统一结构: id/code/name/parent_id/level/sort_order/status/create_time/update_time

-- 1. 机构维度
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
```

- [ ] **Step 2: 执行SQL**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析/
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit < 数据库脚本/dim_tables.sql
```

- [ ] **Step 3: 验证迁移结果**

```bash
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit -e "
SELECT '=== dim_* 各表数据量 ===' AS chk;
SELECT 'dim_organization' AS tbl, COUNT(*) cnt FROM dim_organization
UNION ALL SELECT 'dim_biz_line', COUNT(*) FROM dim_biz_line
UNION ALL SELECT 'dim_dept', COUNT(*) FROM dim_dept
UNION ALL SELECT 'dim_product', COUNT(*) FROM dim_product
UNION ALL SELECT 'dim_channel', COUNT(*) FROM dim_channel
UNION ALL SELECT 'dim_manager', COUNT(*) FROM dim_manager
UNION ALL SELECT 'dim_customer_type', COUNT(*) FROM dim_customer_type;
SELECT '=== 层级结构验证(ORG为例) ===' AS chk;
SELECT CONCAT(REPEAT('  ', level-1), name, ' [level=',level,',parent=',parent_id,']') AS tree
FROM dim_organization ORDER BY parent_id, id;
SELECT '=== 确认旧表已删除 ===' AS chk;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='multi_profit' AND table_name IN ('dimension_master','dw_dim_organization','dw_dim_biz_line','dw_dim_product','dw_dim_channel','dw_dim_manager','dw_dim_customer');
" 2>&1 | grep -v Warning
```
Expected: dim_organization=15,dim_biz_line=21,dim_dept=43,dim_product=34,dim_channel=14,dim_manager=26,dim_customer_type=9; 旧表计数=0

- [ ] **Step 4: Commit**

```bash
git add 数据库脚本/dim_tables.sql
git commit -m "chore: 新建dim_* 7张规范表+从dimension_master迁移数据+删除旧维度表"
```

---

### Task 3: 灌入 indicator_library 指标定义(30+指标,资产/负债分域新命名)

**Files:**
- Create: `数据库脚本/indicator_library_data.sql`

**Interfaces:**
- Produces: indicator_library表约30+行指标定义(code/name/category/calc_formula/supported_dims/pre_calc_periods等)

- [ ] **Step 1: 清空indicator_library并灌入指标定义**

```sql
-- 数据库脚本/indicator_library_data.sql
-- 清空
TRUNCATE TABLE indicator_library;

-- ============================================
-- 一、规模类(SCALE) - 资产域
-- ============================================
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('LOAN_BALANCE', '贷款时点余额', 'SCALE', 'DECIMAL', '万元', 2,
 '期末loan_balance/10000', 'dwd_loan_detail', 'loan_balance',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 1, 1),
('LOAN_DAVG_BALANCE', '贷款日均余额', 'SCALE', 'DECIMAL', '万元', 2,
 'AVG(每日loan_balance)/10000', 'loan_indicator_detail', 'loan_balance',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 2, 1),
('LOAN_MAVG_BALANCE', '贷款月均余额', 'SCALE', 'DECIMAL', '万元', 2,
 'AVG(每日loan_balance)/10000', 'loan_indicator_detail', 'loan_balance',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 3, 1);

-- 二、规模类(SCALE) - 负债域
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('DEPOSIT_BALANCE', '存款时点余额', 'SCALE', 'DECIMAL', '万元', 2,
 '期末deposit_balance/10000', 'dwd_deposit_detail', 'deposit_balance',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 10, 1),
('DEPOSIT_DAVG_BALANCE', '存款日均余额', 'SCALE', 'DECIMAL', '万元', 2,
 'AVG(每日deposit_balance)/10000', 'deposit_indicator_detail', 'deposit_balance',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 11, 1),
('DEPOSIT_MAVG_BALANCE', '存款月均余额', 'SCALE', 'DECIMAL', '万元', 2,
 'AVG(每日deposit_balance)/10000', 'deposit_indicator_detail', 'deposit_balance',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 12, 1);

-- ============================================
-- 三、利息收入类(REVENUE) - 资产域
-- ============================================
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('LOAN_DAILY_INTEREST', '当日贷款利息收入', 'REVENUE', 'DECIMAL', '万元', 2,
 'loan_daily_interest/10000', 'loan_indicator_detail', 'loan_daily_interest',
 'DAY', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 20, 1),
('LOAN_MONTHLY_INTEREST', '当月贷款利息收入', 'REVENUE', 'DECIMAL', '万元', 2,
 'SUM(loan_daily_interest)/10000', 'dwd_loan_detail', 'loan_monthly_interest',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 21, 1),
('LOAN_YEARLY_INTEREST', '当年贷款利息收入', 'REVENUE', 'DECIMAL', '万元', 2,
 'SUM(loan_monthly_interest)/10000', 'dwd_loan_detail', 'loan_monthly_interest',
 'YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 22, 1);

-- 四、利息收入类(REVENUE) - 负债域(FTP收入)
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('FTP_DAILY_INCOME', '当日FTP收入', 'REVENUE', 'DECIMAL', '万元', 2,
 'ftp_income/10000', 'deposit_indicator_detail', 'ftp_income',
 'DAY', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 30, 1),
('FTP_MONTHLY_INCOME', '当月FTP收入', 'REVENUE', 'DECIMAL', '万元', 2,
 'SUM(ftp_income)/10000', 'dwd_deposit_detail', 'ftp_income',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 31, 1),
('FTP_YEARLY_INCOME', '当年FTP收入', 'REVENUE', 'DECIMAL', '万元', 2,
 'SUM(ftp_income)/10000', 'dwd_deposit_detail', 'ftp_income',
 'YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 32, 1);

-- 五、利息支出类(EXPENSE) - 负债域
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('INTEREST_DAILY_EXPENSE', '当日利息支出', 'EXPENSE', 'DECIMAL', '万元', 2,
 'deposit_daily_interest/10000', 'deposit_indicator_detail', 'deposit_daily_interest',
 'DAY', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 40, 1),
('INTEREST_MONTHLY_EXPENSE', '当月利息支出', 'EXPENSE', 'DECIMAL', '万元', 2,
 'SUM(deposit_daily_interest)/10000', 'dwd_deposit_detail', 'deposit_monthly_interest',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 41, 1),
('INTEREST_YEARLY_EXPENSE', '当年利息支出', 'EXPENSE', 'DECIMAL', '万元', 2,
 'SUM(deposit_monthly_interest)/10000', 'dwd_deposit_detail', 'deposit_monthly_interest',
 'YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 42, 1);

-- ============================================
-- 六、成本类(COST) - 资产域
-- ============================================
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('LOAN_FTP_COST', '贷款FTP成本', 'COST', 'DECIMAL', '万元', 2,
 'SUM(ftp_cost)/10000', 'dwd_loan_detail', 'ftp_cost',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 50, 1),
('LOAN_RISK_COST', '贷款风险成本', 'COST', 'DECIMAL', '万元', 2,
 'SUM(risk_cost)/10000', 'dwd_loan_detail', 'risk_cost',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 51, 1),
('LOAN_OP_COST', '贷款运营成本', 'COST', 'DECIMAL', '万元', 2,
 'expense_allocation_result分摊额/10000', 'expense_allocation_result', 'allocated_amount',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 52, 1);

-- 七、成本类(COST) - 负债域
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('DEPOSIT_OP_COST', '存款运营成本', 'COST', 'DECIMAL', '万元', 2,
 'expense_allocation_result分摊额/10000', 'expense_allocation_result', 'allocated_amount',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 60, 1);

-- ============================================
-- 八、利润类(PROFIT) - 派生指标
-- ============================================
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('LOAN_DAILY_PROFIT', '当日贷款利润', 'PROFIT', 'DECIMAL', '万元', 2,
 '当日利息-当日FTP-当日风险-0(运营成本当日为0)', 'derived', NULL,
 'DAY', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 70, 1),
('LOAN_MONTHLY_PROFIT', '当月贷款利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'LOAN_MONTHLY_INTEREST - LOAN_FTP_COST - LOAN_RISK_COST - LOAN_OP_COST', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 71, 1),
('LOAN_YEARLY_PROFIT', '当年贷款利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'LOAN_YEARLY_INTEREST - LOAN_FTP_COST(YEAR) - LOAN_RISK_COST(YEAR) - LOAN_OP_COST(YEAR)', 'derived', NULL,
 'YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 72, 1),

('DEPOSIT_DAILY_PROFIT', '当日存款利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'FTP_DAILY_INCOME - INTEREST_DAILY_EXPENSE - 0(运营成本当日为0)', 'derived', NULL,
 'DAY', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 80, 1),
('DEPOSIT_MONTHLY_PROFIT', '当月存款利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'FTP_MONTHLY_INCOME - INTEREST_MONTHLY_EXPENSE - DEPOSIT_OP_COST', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 81, 1),
('DEPOSIT_YEARLY_PROFIT', '当年存款利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'FTP_YEARLY_INCOME - INTEREST_YEARLY_EXPENSE - DEPOSIT_OP_COST(YEAR)', 'derived', NULL,
 'YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 82, 1),

('TOTAL_DAILY_PROFIT', '当日总利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'LOAN_DAILY_PROFIT + DEPOSIT_DAILY_PROFIT', 'derived', NULL,
 'DAY', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 90, 1),
('TOTAL_MONTHLY_PROFIT', '当月总利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'LOAN_MONTHLY_PROFIT + DEPOSIT_MONTHLY_PROFIT', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 91, 1),
('TOTAL_YEARLY_PROFIT', '当年总利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'LOAN_YEARLY_PROFIT + DEPOSIT_YEARLY_PROFIT', 'derived', NULL,
 'YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 92, 1);

-- ============================================
-- 九、率值类(RATIO) - 预计算存DWS
-- ============================================
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('LOAN_RATE', '贷款平均利率', 'RATIO', 'DECIMAL', '%', 4,
 'LOAN_YEARLY_INTEREST / LOAN_MAVG_BALANCE * 100', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 100, 1),
('LOAN_FTP_RATIO', 'FTP成本占比', 'RATIO', 'DECIMAL', '%', 2,
 'LOAN_FTP_COST / LOAN_MONTHLY_INTEREST * 100', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 101, 1),
('LOAN_RISK_RATIO', '风险成本占比', 'RATIO', 'DECIMAL', '%', 2,
 'LOAN_RISK_COST / LOAN_MONTHLY_INTEREST * 100', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 102, 1),
('DEPOSIT_RATE', '存款付息率', 'RATIO', 'DECIMAL', '%', 4,
 'INTEREST_YEARLY_EXPENSE / DEPOSIT_MAVG_BALANCE * 100', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 110, 1),
('FTP_SPREAD', 'FTP利差', 'RATIO', 'DECIMAL', '%', 4,
 '(FTP_YEARLY_INCOME - INTEREST_YEARLY_EXPENSE) / DEPOSIT_MAVG_BALANCE * 100', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 111, 1),
('COST_INCOME_RATIO', '成本收入比', 'RATIO', 'DECIMAL', '%', 2,
 '(LOAN_FTP_COST + LOAN_RISK_COST + LOAN_OP_COST + DEPOSIT_OP_COST + INTEREST_MONTHLY_EXPENSE) / (LOAN_MONTHLY_INTEREST + FTP_MONTHLY_INCOME) * 100', 'derived', NULL,
 'MONTH,YEAR', 'TOTAL', 120, 1),
('PROFIT_MARGIN', '利润率', 'RATIO', 'DECIMAL', '%', 2,
 'TOTAL_MONTHLY_PROFIT / (LOAN_MONTHLY_INTEREST + FTP_MONTHLY_INCOME) * 100', 'derived', NULL,
 'MONTH,YEAR', 'TOTAL', 121, 1);
```

- [ ] **Step 2: 执行SQL**

```bash
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit < 数据库脚本/indicator_library_data.sql
```

- [ ] **Step 3: 验证**

```bash
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit -e "
SELECT '=== 指标库总数 ===' AS chk;
SELECT COUNT(*) FROM indicator_library;
SELECT '=== 按category分布 ===' AS chk;
SELECT category, COUNT(*) FROM indicator_library GROUP BY category ORDER BY category;
SELECT '=== 指标code列表 ===' AS chk;
SELECT code, name FROM indicator_library ORDER BY sort_order;
" 2>&1 | grep -v Warning
```
Expected: 总计约33条,SCALE=6,REVENUE=6,EXPENSE=3,COST=4,PROFIT=9,RATIO=7

- [ ] **Step 4: Commit**

```bash
git add 数据库脚本/indicator_library_data.sql
git commit -m "feat: 灌入indicator_library指标库定义(33条,资产/负债分域新命名)"
```

---

**[确认点] Phase 1 完成。验证:dim_* 7表+indicator_library 33条指标+15张废弃表已删除+数据已清空。是否继续 Phase 2?**

---

## Phase 2: 数据生成器(Python脚本)

依赖Phase 1(dim_*叶子节点)。独立可测:生成ODS日级数据后可直接SQL查询验证。

### Task 4: 生成器配置 config.py

**Files:**
- Create: `脚本/数据生成器/config.py`

**Interfaces:**
- Produces: 生成参数常量(利率范围/波动率/笔数/日期),被init_biz.py和generate_daily.py引用

- [ ] **Step 1: 编写config.py**

```python
# 脚本/数据生成器/config.py
# 数据生成参数配置 —— 与骨架(主数据+指标库)分离,生成器自有

# ============================================
# 业务规模
# ============================================
LOAN_COUNT = 500          # 贷款笔数
DEPOSIT_COUNT = 500       # 存款笔数

# ============================================
# 贷款利率参数
# ============================================
LOAN_RATE_MIN = 0.03      # 最低3%
LOAN_RATE_MAX = 0.15      # 最高15%
LOAN_FTP_RATE_MIN = 0.01  # FTP利率1%-2%
LOAN_FTP_RATE_MAX = 0.02
LOAN_RISK_RATE_MIN = 0.001  # 风险成本率0.1%-0.3%
LOAN_RISK_RATE_MAX = 0.003

# ============================================
# 存款利率参数
# ============================================
DEPOSIT_RATE_MIN = 0.01   # 最低1%
DEPOSIT_RATE_MAX = 0.03   # 最高3%
DEPOSIT_FTP_RATE_MIN = 0.025  # FTP利率2.5%-3.5%
DEPOSIT_FTP_RATE_MAX = 0.035

# ============================================
# 余额参数
# ============================================
LOAN_BALANCE_MIN = 100_000      # 单笔最低10万
LOAN_BALANCE_MAX = 50_000_000   # 单笔最高5000万
DEPOSIT_BALANCE_MIN = 50_000    # 单笔最低5万
DEPOSIT_BALANCE_MAX = 30_000_000 # 单笔最高3000万
DAILY_VOLATILITY = 0.02         # 日波动±2%

# ============================================
# 日期范围
# ============================================
START_DATE = "2025-01-01"   # 回溯起始
END_DATE = "2026-07-01"     # 到今天

# ============================================
# 数据库连接
# ============================================
DB_CONFIG = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "mpuser",
    "password": "<DB_PASSWORD>",
    "database": "multi_profit",
    "charset": "utf8mb4"
}
```

- [ ] **Step 2: Commit**

```bash
git add 脚本/数据生成器/config.py
git commit -m "feat: 数据生成器配置(config.py,与骨架分离)"
```

---

### Task 5: 存量业务初始化 init_biz.py

**Files:**
- Create: `脚本/数据生成器/init_biz.py`

**Interfaces:**
- Consumes: config.py(参数), dim_*表(叶子节点), customer_master(客户)
- Produces: 500笔贷款+500笔存款业务主表(写入MySQL),每笔有固定属性(利率/余额/维度id)

- [ ] **Step 1: 编写init_biz.py**

```python
# 脚本/数据生成器/init_biz.py
"""存量业务初始化:500笔贷款+500笔存款,从dim_*叶子节点随机组合维度"""
import pymysql
import random
import datetime
from config import *

def get_leaf_nodes(cursor, table_name, level=3):
    """从dim_*表取叶子节点"""
    cursor.execute(f"SELECT id, name FROM {table_name} WHERE level = {level} AND status = 1")
    return cursor.fetchall()

def get_customers(cursor):
    """取客户清单"""
    cursor.execute("SELECT id, customer_name FROM customer_master WHERE status = 1")
    return cursor.fetchall()

def generate_loan_biz(cursor, conn, start_idx=1, count=LOAN_COUNT):
    """生成贷款存量业务"""
    orgs = get_leaf_nodes(cursor, "dim_organization")
    depts = get_leaf_nodes(cursor, "dim_dept")
    products = get_leaf_nodes(cursor, "dim_product")
    channels = get_leaf_nodes(cursor, "dim_channel")
    managers = get_leaf_nodes(cursor, "dim_manager")
    bizlines = get_leaf_nodes(cursor, "dim_biz_line")
    customers = get_customers(cursor)

    sql = """INSERT INTO loan_indicator_detail
        (biz_id, stat_date, account_period, caliber_type,
         org_id, org_name, dept_id, dept_name, product_id, product_name,
         channel_id, channel_name, manager_id, manager_name,
         biz_line_id, biz_line_name, customer_id, customer_name,
         loan_balance, loan_rate, loan_interest_calc_type,
         loan_daily_interest, loan_monthly_interest, loan_cumulative_interest,
         ftp_rate, ftp_cost, risk_cost, op_cost, expense_type)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)"""

    biz_list = []
    for i in range(start_idx, start_idx + count):
        biz_id = f"L{i:06d}"
        org = random.choice(orgs)
        dept = random.choice(depts)
        prod = random.choice(products)
        ch = random.choice(channels)
        mgr = random.choice(managers)
        bl = random.choice(bizlines)
        cust = random.choice(customers)

        balance = round(random.uniform(LOAN_BALANCE_MIN, LOAN_BALANCE_MAX), 2)
        loan_rate = round(random.uniform(LOAN_RATE_MIN, LOAN_RATE_MAX), 6)
        ftp_rate = round(random.uniform(LOAN_FTP_RATE_MIN, LOAN_FTP_RATE_MAX), 6)

        biz_list.append({
            'biz_id': biz_id,
            'org_id': org[0], 'org_name': org[1],
            'dept_id': dept[0], 'dept_name': dept[1],
            'product_id': prod[0], 'product_name': prod[1],
            'channel_id': ch[0], 'channel_name': ch[1],
            'manager_id': mgr[0], 'manager_name': mgr[1],
            'biz_line_id': bl[0], 'biz_line_name': bl[1],
            'customer_id': cust[0], 'customer_name': cust[1],
            'loan_balance': balance,
            'loan_rate': loan_rate,
            'ftp_rate': ftp_rate,
            'risk_rate': round(random.uniform(LOAN_RISK_RATE_MIN, LOAN_RISK_RATE_MAX), 6),
        })
        print(f"  Loan {biz_id}: balance={balance:,.0f} rate={loan_rate*100:.2f}%")

    # 暂不写数据库,generate_daily.py会首次写入(包含每日数据)
    # init_biz只产出业务属性列表,供generate_daily.py使用
    return biz_list

def generate_deposit_biz(cursor, conn, start_idx=1, count=DEPOSIT_COUNT):
    """生成存款存量业务"""
    orgs = get_leaf_nodes(cursor, "dim_organization")
    depts = get_leaf_nodes(cursor, "dim_dept")
    products = get_leaf_nodes(cursor, "dim_product")
    channels = get_leaf_nodes(cursor, "dim_channel")
    managers = get_leaf_nodes(cursor, "dim_manager")
    bizlines = get_leaf_nodes(cursor, "dim_biz_line")
    customers = get_customers(cursor)

    biz_list = []
    for i in range(start_idx, start_idx + count):
        biz_id = f"D{i:06d}"
        org = random.choice(orgs)
        dept = random.choice(depts)
        prod = random.choice(products)
        ch = random.choice(channels)
        mgr = random.choice(managers)
        bl = random.choice(bizlines)
        cust = random.choice(customers)

        balance = round(random.uniform(DEPOSIT_BALANCE_MIN, DEPOSIT_BALANCE_MAX), 2)
        deposit_rate = round(random.uniform(DEPOSIT_RATE_MIN, DEPOSIT_RATE_MAX), 6)
        ftp_rate = round(random.uniform(DEPOSIT_FTP_RATE_MIN, DEPOSIT_FTP_RATE_MAX), 6)

        biz_list.append({
            'biz_id': biz_id,
            'org_id': org[0], 'org_name': org[1],
            'dept_id': dept[0], 'dept_name': dept[1],
            'product_id': prod[0], 'product_name': prod[1],
            'channel_id': ch[0], 'channel_name': ch[1],
            'manager_id': mgr[0], 'manager_name': mgr[1],
            'biz_line_id': bl[0], 'biz_line_name': bl[1],
            'customer_id': cust[0], 'customer_name': cust[1],
            'deposit_balance': balance,
            'deposit_rate': deposit_rate,
            'ftp_rate': ftp_rate,
        })
        print(f"  Deposit {biz_id}: balance={balance:,.0f} rate={deposit_rate*100:.2f}%")

    return biz_list

if __name__ == "__main__":
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()
    try:
        print("=== 初始化贷款存量业务(500笔) ===")
        loan_biz = generate_loan_biz(cursor, conn)
        print(f"生成 {len(loan_biz)} 笔贷款业务")

        print("\n=== 初始化存款存量业务(500笔) ===")
        deposit_biz = generate_deposit_biz(cursor, conn)
        print(f"生成 {len(deposit_biz)} 笔存款业务")

        # 保存为Python pickle供generate_daily.py读取
        import pickle
        with open('biz_cache.pkl', 'wb') as f:
            pickle.dump({'loans': loan_biz, 'deposits': deposit_biz}, f)
        print("\n业务属性已缓存到 biz_cache.pkl")
    finally:
        cursor.close()
        conn.close()
```

- [ ] **Step 2: 安装依赖并执行**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析/脚本/数据生成器/
pip3 install pymysql --quiet
python3 init_biz.py
```
Expected: 输出500笔贷款+500笔存款,生成biz_cache.pkl文件

- [ ] **Step 3: Commit**

```bash
git add 脚本/数据生成器/init_biz.py
git commit -m "feat: 存量业务初始化脚本(500贷款+500存款,挂dim_*叶子节点)"
```

---

### Task 6: 每日数据生成 generate_daily.py

**Files:**
- Create: `脚本/数据生成器/generate_daily.py`

**Interfaces:**
- Consumes: config.py, biz_cache.pkl(业务属性), dim_*表(读name)
- Produces: loan_indicator_detail + deposit_indicator_detail 每日1行数据

- [ ] **Step 1: 编写generate_daily.py**

```python
# 脚本/数据生成器/generate_daily.py
"""每日数据生成:基于存量业务属性,生成每日时点余额/利息/FTP/风险"""
import pymysql
import random
import pickle
import datetime
import math
from config import *

def generate_daily_data(start_date_str, end_date_str):
    """生成指定日期范围的日级数据"""
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    # 加载存量业务属性
    with open('biz_cache.pkl', 'rb') as f:
        cache = pickle.load(f)
    loans = cache['loans']
    deposits = cache['deposits']

    start_date = datetime.date.fromisoformat(start_date_str)
    end_date = datetime.date.fromisoformat(end_date_str)
    delta = (end_date - start_date).days

    # 初始化每笔业务的昨日余额(首次使用初始余额)
    loan_balances = {b['biz_id']: b['loan_balance'] for b in loans}
    deposit_balances = {b['biz_id']: b['deposit_balance'] for b in deposits}

    loan_sql = """INSERT INTO loan_indicator_detail
        (biz_id, stat_date, account_period, caliber_type,
         org_id, org_name, dept_id, dept_name, product_id, product_name,
         channel_id, channel_name, manager_id, manager_name,
         biz_line_id, biz_line_name, customer_id, customer_name,
         loan_balance, loan_rate, loan_interest_calc_type,
         loan_daily_interest, loan_monthly_interest, loan_cumulative_interest,
         ftp_rate, ftp_cost, risk_cost, op_cost)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,
                %s,%s,%s,%s,%s,%s,%s,%s,%s,%s)"""

    deposit_sql = """INSERT INTO deposit_indicator_detail
        (biz_id, stat_date, account_period, caliber_type,
         org_id, org_name, dept_id, dept_name, product_id, product_name,
         channel_id, channel_name, manager_id, manager_name,
         biz_line_id, biz_line_name, customer_id, customer_name,
         deposit_balance, deposit_rate, deposit_interest_calc_type,
         deposit_daily_interest, deposit_monthly_interest, deposit_cumulative_interest,
         ftp_rate, ftp_income, op_cost)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,
                %s,%s,%s,%s,%s,%s,%s,%s,%s)"""

    loan_batch = []
    deposit_batch = []
    # 月度累计利息缓存(按月+按biz_id)
    monthly_loan_interest = {}
    monthly_deposit_interest = {}
    cumulative_loan_interest = {b['biz_id']: 0 for b in loans}
    cumulative_deposit_interest = {b['biz_id']: 0 for b in deposits}

    for day_offset in range(delta + 1):
        current_date = start_date + datetime.timedelta(days=day_offset)
        account_period = current_date.strftime("%Y-%m")

        # 月初重置月度累计
        if current_date.day == 1:
            monthly_loan_interest = {}
            monthly_deposit_interest = {}

        # === 贷款业务 ===
        for biz in loans:
            bid = biz['biz_id']
            # 日波动
            prev_balance = loan_balances[bid]
            daily_change = random.uniform(-DAILY_VOLATILITY, DAILY_VOLATILITY)
            new_balance = prev_balance * (1 + daily_change)
            new_balance = max(new_balance, 1000)  # 不低于1000
            loan_balances[bid] = new_balance

            # 当日利息 = 余额 × 利率 / 365
            daily_interest = round(new_balance * biz['loan_rate'] / 365, 4)
            # 当日FTP = 余额 × FTP利率 / 365
            daily_ftp = round(new_balance * biz['ftp_rate'] / 365, 4)
            # 当日风险 = 余额 × 风险成本率 / 365
            daily_risk = round(new_balance * biz['risk_rate'] / 365, 4)

            # 月度累计
            month_key = f"{bid}_{account_period}"
            monthly_loan_interest[month_key] = monthly_loan_interest.get(month_key, 0) + daily_interest
            cumulative_loan_interest[bid] += daily_interest

            loan_batch.append((
                bid, current_date, account_period, 'ASSESS',
                biz['org_id'], biz['org_name'], biz['dept_id'], biz['dept_name'],
                biz['product_id'], biz['product_name'],
                biz['channel_id'], biz['channel_name'],
                biz['manager_id'], biz['manager_name'],
                biz['biz_line_id'], biz['biz_line_name'],
                biz['customer_id'], biz['customer_name'],
                new_balance, biz['loan_rate'], 'DAILY_ACCUMULATED',
                daily_interest, monthly_loan_interest[month_key], cumulative_loan_interest[bid],
                biz['ftp_rate'], daily_ftp, daily_risk, 0  # op_cost=0(日级)
            ))

        # === 存款业务 ===
        for biz in deposits:
            bid = biz['biz_id']
            prev_balance = deposit_balances[bid]
            daily_change = random.uniform(-DAILY_VOLATILITY, DAILY_VOLATILITY)
            new_balance = prev_balance * (1 + daily_change)
            new_balance = max(new_balance, 1000)
            deposit_balances[bid] = new_balance

            # 当日FTP收入 = 余额 × FTP利率 / 365
            daily_ftp_income = round(new_balance * biz['ftp_rate'] / 365, 4)
            # 当日利息支出 = 余额 × 存款利率 / 365
            daily_interest_expense = round(new_balance * biz['deposit_rate'] / 365, 4)

            month_key = f"{bid}_{account_period}"
            monthly_deposit_interest[month_key] = monthly_deposit_interest.get(month_key, 0) + daily_interest_expense
            cumulative_deposit_interest[bid] += daily_interest_expense

            deposit_batch.append((
                bid, current_date, account_period, 'ASSESS',
                biz['org_id'], biz['org_name'], biz['dept_id'], biz['dept_name'],
                biz['product_id'], biz['product_name'],
                biz['channel_id'], biz['channel_name'],
                biz['manager_id'], biz['manager_name'],
                biz['biz_line_id'], biz['biz_line_name'],
                biz['customer_id'], biz['customer_name'],
                new_balance, biz['deposit_rate'], 'DAILY_ACCUMULATED',
                daily_interest_expense, monthly_deposit_interest[month_key], cumulative_deposit_interest[bid],
                biz['ftp_rate'], daily_ftp_income, 0  # op_cost=0(日级)
            ))

        # 每100天提交一次批量插入
        if len(loan_batch) >= 100 * 500:
            cursor.executemany(loan_sql, loan_batch)
            cursor.executemany(deposit_sql, deposit_batch)
            conn.commit()
            loan_batch.clear()
            deposit_batch.clear()
            print(f"  {current_date}: {500} loans + {500} deposits committed")

    # 提交剩余
    if loan_batch:
        cursor.executemany(loan_sql, loan_batch)
        cursor.executemany(deposit_sql, deposit_batch)
        conn.commit()

    total_loan = delta * 500 + 500
    total_deposit = delta * 500 + 500
    print(f"\n完成: {total_loan} 条贷款 + {total_deposit} 条存款 (共{total_loan+total_deposit}行)")

    cursor.close()
    conn.close()

if __name__ == "__main__":
    import sys
    start = sys.argv[1] if len(sys.argv) > 1 else START_DATE
    end = sys.argv[2] if len(sys.argv) > 2 else END_DATE
    print(f"生成日级数据: {start} ~ {end}")
    generate_daily_data(start, end)
```

- [ ] **Step 2: 执行首次回溯生成**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析/脚本/数据生成器/
python3 generate_daily.py 2025-01-01 2026-07-01
```
Expected: 输出进度日志,最终约54.8万行(500笔×2×548天)

- [ ] **Step 3: 验证ODS数据**

```bash
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit -e "
SELECT '=== ODS贷款总行数 ===' AS chk;
SELECT COUNT(*) FROM loan_indicator_detail;
SELECT '=== ODS存款总行数 ===' AS chk;
SELECT COUNT(*) FROM deposit_indicator_detail;
SELECT '=== 日期范围 ===' AS chk;
SELECT MIN(stat_date), MAX(stat_date) FROM loan_indicator_detail;
SELECT '=== 每日1行验证(随机抽样一笔) ===' AS chk;
SELECT biz_id, stat_date, loan_balance, loan_daily_interest, ftp_cost, risk_cost
FROM loan_indicator_detail WHERE biz_id='L000001' ORDER BY stat_date LIMIT 5;
" 2>&1 | grep -v Warning
```
Expected: 贷款约27.4万行,存款约27.4万行,日期2025-01-01~2026-07-01,同一笔每天1行

- [ ] **Step 4: Commit**

```bash
git add 脚本/数据生成器/generate_daily.py
git commit -m "feat: 每日数据生成器(日级余额/利息/FTP/风险,回溯2025-01到今天)"
```

---

**[确认点] Phase 2 完成。验证:ODS有约54.8万行日级数据,每笔每天1行,利息/FTP/风险每日有值,op_cost=0。是否继续 Phase 3?**

---

## Phase 3: ETL重写(ODS→DWD→DWS)

依赖Phase 1+2。核心改造:DataWarehouseETLServiceImpl重写,支持日级聚合→月度DWD→DWS(三期间+全维度+层级上卷+真实分摊op_cost)。

### Task 7: DWD表加dept_id字段

**Files:**
- Modify: `数据库脚本/dwd_tables.sql`(如已有)或直接ALTER
- Modify: `后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java`(ensureDwdTableStructure方法)

**Interfaces:**
- Produces: dwd_loan_detail/dwd_deposit_detail 新增 dept_id/dept_name 列

- [ ] **Step 1: ALTER TABLE 加列**

```bash
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit -e "
ALTER TABLE dwd_loan_detail ADD COLUMN dept_id BIGINT AFTER customer_id;
ALTER TABLE dwd_loan_detail ADD COLUMN dept_name VARCHAR(200) AFTER dept_id;
ALTER TABLE dwd_deposit_detail ADD COLUMN dept_id BIGINT AFTER customer_id;
ALTER TABLE dwd_deposit_detail ADD COLUMN dept_name VARCHAR(200) AFTER dept_id;
" 2>&1
```

- [ ] **Step 2: 验证**

```bash
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit -e "
SHOW COLUMNS FROM dwd_loan_detail LIKE 'dept%';
SHOW COLUMNS FROM dwd_deposit_detail LIKE 'dept%';
" 2>&1 | grep -v Warning
```
Expected: 两张表都有dept_id和dept_name列

- [ ] **Step 3: 更新ensureDwdTableStructure方法**

打开 `后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java`,在 `ensureDwdTableStructure()` 方法末尾添加:

```java
// 确保DWD表有dept字段
addColumnIfNotExists("dwd_loan_detail", "dept_id", "BIGINT", "部门ID");
addColumnIfNotExists("dwd_loan_detail", "dept_name", "VARCHAR(200)", "部门名称");
addColumnIfNotExists("dwd_deposit_detail", "dept_id", "BIGINT", "部门ID");
addColumnIfNotExists("dwd_deposit_detail", "dept_name", "VARCHAR(200)", "部门名称");
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: DWD表新增dept_id/dept_name字段"
```

---

### Task 8: 重写ETL核心逻辑(DataWarehouseETLServiceImpl)

**Files:**
- Modify: `后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java`(重写executeETLSQL方法,约200行→约300行)

**Interfaces:**
- Consumes: loan/deposit_indicator_detail(ODS日级), expense_allocation_result(真实分摊), dim_*表(层级上卷)
- Produces: dwd_loan_detail/dwd_deposit_detail(DWD月度), dw_indicator_fact(DWS DAY/MONTH/YEAR)

- [ ] **Step 1: 重写executeETLSQL方法 - DWD月度聚合部分**

将 `DataWarehouseETLServiceImpl.java` 的 `executeETLSQL` 方法替换为:

```java
private void executeETLSQL(String period) {
    // period格式: "YYYY-MM", 如 "2026-01"
    
    // === 1. 清除DWD层该期间数据 ===
    jdbcTemplate.update("DELETE FROM dwd_loan_detail WHERE account_period = ?", period);
    jdbcTemplate.update("DELETE FROM dwd_deposit_detail WHERE account_period = ?", period);

    // === 2. ODS日级 → DWD月度聚合(贷款) ===
    String loanDwdSql = 
        "INSERT INTO dwd_loan_detail (" +
        "biz_id, account_period, caliber_type, " +
        "org_id, org_name, biz_line_id, biz_line_name, " +
        "product_id, product_name, channel_id, channel_name, " +
        "manager_id, manager_name, dept_id, dept_name, " +
        "customer_id, customer_name, " +
        "loan_balance, loan_monthly_interest, ftp_cost, risk_cost, op_cost, " +
        "loan_profit, net_interest_margin" +
        ") SELECT " +
        "l.biz_id, l.account_period, l.caliber_type, " +
        "l.org_id, l.org_name, l.biz_line_id, l.biz_line_name, " +
        "l.product_id, l.product_name, l.channel_id, l.channel_name, " +
        "l.manager_id, l.manager_name, l.dept_id, l.dept_name, " +
        "l.customer_id, l.customer_name, " +
        // 时点余额 = 月末那天
        "FIRST_VALUE(l.loan_balance) OVER (PARTITION BY l.biz_id ORDER BY l.stat_date DESC), " +
        // 当月利息 = 月内每日求和
        "SUM(l.loan_daily_interest), " +
        // FTP成本 = 月内每日求和
        "SUM(l.ftp_cost), " +
        // 风险成本 = 月内每日求和
        "SUM(l.risk_cost), " +
        // 运营成本 = 从真实分摊结果读取(月度)
        "COALESCE(ea.op_cost, 0), " +
        // 贷款利润 = 利息 - FTP - 风险 - 运营
        "(SUM(l.loan_daily_interest) - SUM(l.ftp_cost) - SUM(l.risk_cost) - COALESCE(ea.op_cost, 0)), " +
        // 净利差 = 利息 - FTP
        "(SUM(l.loan_daily_interest) - SUM(l.ftp_cost)) " +
        "FROM loan_indicator_detail l " +
        "LEFT JOIN (" +
        "  SELECT biz_id, SUM(allocated_amount) as op_cost " +
        "  FROM expense_allocation_result WHERE period = ? GROUP BY biz_id" +
        ") ea ON l.biz_id = ea.biz_id " +
        "WHERE l.account_period = ? " +
        "GROUP BY l.biz_id, l.account_period, l.caliber_type, l.org_id, l.org_name, " +
        "l.biz_line_id, l.biz_line_name, l.product_id, l.product_name, " +
        "l.channel_id, l.channel_name, l.manager_id, l.manager_name, " +
        "l.dept_id, l.dept_name, l.customer_id, l.customer_name";
    jdbcTemplate.update(loanDwdSql, period, period);

    // === 3. ODS日级 → DWD月度聚合(存款) ===
    String depositDwdSql = 
        "INSERT INTO dwd_deposit_detail (" +
        "biz_id, account_period, caliber_type, " +
        "org_id, org_name, biz_line_id, biz_line_name, " +
        "product_id, product_name, channel_id, channel_name, " +
        "manager_id, manager_name, dept_id, dept_name, " +
        "customer_id, customer_name, " +
        "deposit_balance, deposit_monthly_interest, ftp_income, op_cost, deposit_profit" +
        ") SELECT " +
        "d.biz_id, d.account_period, d.caliber_type, " +
        "d.org_id, d.org_name, d.biz_line_id, d.biz_line_name, " +
        "d.product_id, d.product_name, d.channel_id, d.channel_name, " +
        "d.manager_id, d.manager_name, d.dept_id, d.dept_name, " +
        "d.customer_id, d.customer_name, " +
        "FIRST_VALUE(d.deposit_balance) OVER (PARTITION BY d.biz_id ORDER BY d.stat_date DESC), " +
        "SUM(d.deposit_daily_interest), " +
        "SUM(d.ftp_income), " +
        "COALESCE(ea.op_cost, 0), " +
        "(SUM(d.ftp_income) - SUM(d.deposit_daily_interest) - COALESCE(ea.op_cost, 0)) " +
        "FROM deposit_indicator_detail d " +
        "LEFT JOIN (" +
        "  SELECT biz_id, SUM(allocated_amount) as op_cost " +
        "  FROM expense_allocation_result WHERE period = ? GROUP BY biz_id" +
        ") ea ON d.biz_id = ea.biz_id " +
        "WHERE d.account_period = ? " +
        "GROUP BY d.biz_id, d.account_period, d.caliber_type, d.org_id, d.org_name, " +
        "d.biz_line_id, d.biz_line_name, d.product_id, d.product_name, " +
        "d.channel_id, d.channel_name, d.manager_id, d.manager_name, " +
        "d.dept_id, d.dept_name, d.customer_id, d.customer_name";
    jdbcTemplate.update(depositDwdSql, period, period);
```

- [ ] **Step 2: 继续重写 - DWS层(MONTH)全维度+层级上卷**

在 `executeETLSQL` 方法续写:

```java
    // === 4. 清除DWS层该期间MONTH数据 ===
    jdbcTemplate.update("DELETE FROM dw_indicator_fact WHERE period = ? AND period_type = 'MONTH'", period);

    // === 5. DWS MONTH: 按7维度 × 3层级上卷所有指标 ===
    String[] dimTypes = {"ORG", "BIZ_LINE", "DEPT", "PRODUCT", "CHANNEL", "MANAGER", "CUSTOMER"};
    String[] dimTables = {"dim_organization", "dim_biz_line", "dim_dept", 
                          "dim_product", "dim_channel", "dim_manager", "dim_customer_type"};

    for (int i = 0; i < dimTypes.length; i++) {
        String dimType = dimTypes[i];
        String dimTable = dimTables[i];
        String loanDimCol = getDimIdColumn(dimType);  // org_id/biz_line_id/dept_id/...
        
        // 层级上卷: level 1/2/3 全部写入
        // 从叶子节点(level=3)开始,沿parent_id上卷到level 2、level 1
        for (int level = 3; level >= 1; level--) {
            // 贷款指标
            String loanIndicatorsSql = 
                "INSERT IGNORE INTO dw_indicator_fact " +
                "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
                // 贷款利息收入
                "SELECT 'LOAN_MONTHLY_INTEREST', ?, 'MONTH', ?, d.id, d.name, " +
                "SUM(dl.loan_monthly_interest)/10000, 'ASSESS', NOW() " +
                "FROM dwd_loan_detail dl " +
                "JOIN " + dimTable + " leaf ON dl." + loanDimCol + " = leaf.id " +
                "JOIN " + dimTable + " d ON leaf.id = d.id " +
                // 如果是level 1/2,需要沿parent_id上卷
                (level < 3 ? "JOIN " + dimTable + " parent ON d.parent_id = parent.id " : "") +
                "WHERE dl.account_period = ? AND d.level = ? " +
                "GROUP BY d.id, d.name " +
                "UNION ALL " +
                // FTP成本
                "SELECT 'LOAN_FTP_COST', ?, 'MONTH', ?, d.id, d.name, " +
                "SUM(dl.ftp_cost)/10000, 'ASSESS', NOW() " +
                "FROM dwd_loan_detail dl " +
                "JOIN " + dimTable + " leaf ON dl." + loanDimCol + " = leaf.id " +
                "JOIN " + dimTable + " d ON leaf.id = d.id " +
                (level < 3 ? "JOIN " + dimTable + " parent ON d.parent_id = parent.id " : "") +
                "WHERE dl.account_period = ? AND d.level = ? " +
                "GROUP BY d.id, d.name " +
                "UNION ALL " +
                // 风险成本
                "SELECT 'LOAN_RISK_COST', ?, 'MONTH', ?, d.id, d.name, " +
                "SUM(dl.risk_cost)/10000, 'ASSESS', NOW() " +
                "FROM dwd_loan_detail dl " +
                "JOIN " + dimTable + " leaf ON dl." + loanDimCol + " = leaf.id " +
                "JOIN " + dimTable + " d ON leaf.id = d.id " +
                (level < 3 ? "JOIN " + dimTable + " parent ON d.parent_id = parent.id " : "") +
                "WHERE dl.account_period = ? AND d.level = ? " +
                "GROUP BY d.id, d.name " +
                "UNION ALL " +
                // 运营成本(贷款)
                "SELECT 'LOAN_OP_COST', ?, 'MONTH', ?, d.id, d.name, " +
                "SUM(dl.op_cost)/10000, 'ASSESS', NOW() " +
                "FROM dwd_loan_detail dl " +
                "JOIN " + dimTable + " leaf ON dl." + loanDimCol + " = leaf.id " +
                "JOIN " + dimTable + " d ON leaf.id = d.id " +
                (level < 3 ? "JOIN " + dimTable + " parent ON d.parent_id = parent.id " : "") +
                "WHERE dl.account_period = ? AND d.level = ? " +
                "GROUP BY d.id, d.name " +
                "UNION ALL " +
                // 贷款利润
                "SELECT 'LOAN_MONTHLY_PROFIT', ?, 'MONTH', ?, d.id, d.name, " +
                "SUM(dl.loan_profit)/10000, 'ASSESS', NOW() " +
                "FROM dwd_loan_detail dl " +
                "JOIN " + dimTable + " leaf ON dl." + loanDimCol + " = leaf.id " +
                "JOIN " + dimTable + " d ON leaf.id = d.id " +
                (level < 3 ? "JOIN " + dimTable + " parent ON d.parent_id = parent.id " : "") +
                "WHERE dl.account_period = ? AND d.level = ? " +
                "GROUP BY d.id, d.name";
            jdbcTemplate.update(loanIndicatorsSql, 
                period, dimType, period, level,
                period, dimType, period, level,
                period, dimType, period, level,
                period, dimType, period, level,
                period, dimType, period, level);

            // 存款指标(类似,省略重复SQL,实际代码中写全FTP_INCOME/INTEREST_EXPENSE/DEPOSIT_OP_COST/DEPOSIT_PROFIT)
            // [此处实际代码需写全存款的FTP_MONTHLY_INCOME/INTEREST_MONTHLY_EXPENSE/DEPOSIT_OP_COST/DEPOSIT_MONTHLY_PROFIT 4个指标]
        }
    }

    // === 6. DWS MONTH: TOTAL汇总 ===
    // 从DWD直接SUM(不分维度)写入dim_type='TOTAL'行
    String totalSql = 
        "INSERT IGNORE INTO dw_indicator_fact " +
        "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
        "SELECT 'LOAN_MONTHLY_INTEREST', ?, 'MONTH', 'TOTAL', 0, '全部', SUM(loan_monthly_interest)/10000, 'ASSESS', NOW() FROM dwd_loan_detail WHERE account_period = ? " +
        "UNION ALL SELECT 'LOAN_FTP_COST', ?, 'MONTH', 'TOTAL', 0, '全部', SUM(ftp_cost)/10000, 'ASSESS', NOW() FROM dwd_loan_detail WHERE account_period = ? " +
        "UNION ALL SELECT 'LOAN_RISK_COST', ?, 'MONTH', 'TOTAL', 0, '全部', SUM(risk_cost)/10000, 'ASSESS', NOW() FROM dwd_loan_detail WHERE account_period = ? " +
        "UNION ALL SELECT 'LOAN_OP_COST', ?, 'MONTH', 'TOTAL', 0, '全部', SUM(op_cost)/10000, 'ASSESS', NOW() FROM dwd_loan_detail WHERE account_period = ? " +
        "UNION ALL SELECT 'LOAN_MONTHLY_PROFIT', ?, 'MONTH', 'TOTAL', 0, '全部', SUM(loan_profit)/10000, 'ASSESS', NOW() FROM dwd_loan_detail WHERE account_period = ? " +
        "UNION ALL SELECT 'FTP_MONTHLY_INCOME', ?, 'MONTH', 'TOTAL', 0, '全部', SUM(ftp_income)/10000, 'ASSESS', NOW() FROM dwd_deposit_detail WHERE account_period = ? " +
        "UNION ALL SELECT 'INTEREST_MONTHLY_EXPENSE', ?, 'MONTH', 'TOTAL', 0, '全部', SUM(deposit_monthly_interest)/10000, 'ASSESS', NOW() FROM dwd_deposit_detail WHERE account_period = ? " +
        "UNION ALL SELECT 'DEPOSIT_OP_COST', ?, 'MONTH', 'TOTAL', 0, '全部', SUM(op_cost)/10000, 'ASSESS', NOW() FROM dwd_deposit_detail WHERE account_period = ? " +
        "UNION ALL SELECT 'DEPOSIT_MONTHLY_PROFIT', ?, 'MONTH', 'TOTAL', 0, '全部', SUM(deposit_profit)/10000, 'ASSESS', NOW() FROM dwd_deposit_detail WHERE account_period = ? " +
        "UNION ALL SELECT 'TOTAL_MONTHLY_PROFIT', ?, 'MONTH', 'TOTAL', 0, '全部', " +
        "((SELECT COALESCE(SUM(loan_profit),0) FROM dwd_loan_detail WHERE account_period = ?) + " +
        " (SELECT COALESCE(SUM(deposit_profit),0) FROM dwd_deposit_detail WHERE account_period = ?)) / 10000, 'ASSESS', NOW()";
    jdbcTemplate.update(totalSql,
        period, period, period, period, period, period, period, period, period, period,
        period, period, period, period, period, period, period, period, period, period,
        period, period);
```

- [ ] **Step 3: 添加辅助方法和YEAR累计**

```java
// 在DataWarehouseETLServiceImpl中添加
private String getDimIdColumn(String dimType) {
    switch (dimType) {
        case "ORG": return "org_id";
        case "BIZ_LINE": return "biz_line_id";
        case "DEPT": return "dept_id";
        case "PRODUCT": return "product_id";
        case "CHANNEL": return "channel_id";
        case "MANAGER": return "manager_id";
        case "CUSTOMER": return "customer_id";
        default: throw new IllegalArgumentException("Unknown dimType: " + dimType);
    }
}

// 添加YEAR累计方法
private void calculateYearlyAggregation(String year) {
    // year = "2026"
    jdbcTemplate.update("DELETE FROM dw_indicator_fact WHERE period = ? AND period_type = 'YEAR'", year);
    
    String yearlySql = 
        "INSERT IGNORE INTO dw_indicator_fact " +
        "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
        "SELECT indicator_code, ?, 'YEAR', dim_type, dim_id, dim_name, SUM(calc_value), caliber_type, NOW() " +
        "FROM dw_indicator_fact " +
        "WHERE period LIKE CONCAT(?, '-%') AND period_type = 'MONTH' " +
        "GROUP BY indicator_code, dim_type, dim_id, dim_name, caliber_type";
    jdbcTemplate.update(yearlySql, year, year);
}
```

- [ ] **Step 4: 编译验证**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析/后端/
mvn compile -q 2>&1 | tail -5
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: 重写ETL(日级聚合DWD+全维度层级上卷DWS+MONTH/YEAR+TOTAL+真实分摊op_cost)"
```

---

### Task 9: 运行ETL验证

**Files:**
- Modify: `后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java`(修改executeETL方法支持YEAR)

**Interfaces:**
- 验证ETL全流程能产出正确的dw_indicator_fact数据

- [ ] **Step 1: 更新executeETL方法加入YEAR累计**

在 `DataWarehouseETLServiceImpl.java` 的 `executeETL` 方法末尾(统计结果之后),添加:

```java
// 4. 计算年度累计
String year = period.substring(0, 4);
calculateYearlyAggregation(year);
log.info("年度累计计算完成: {}", year);
```

- [ ] **Step 2: 启动后端并调用ETL接口**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析/后端/
mvn spring-boot:run -q &
sleep 15
# 跑2025-01到2026-06的ETL
for month in 2025-01 2025-02 2025-03 2025-04 2025-05 2025-06 \
             2025-07 2025-08 2025-09 2025-10 2025-11 2025-12 \
             2026-01 2026-02 2026-03 2026-04 2026-05 2026-06; do
  curl -s "http://localhost:8080/api/datawarehouse/etl?period=$month" | head -c 200
  echo ""
done
```

- [ ] **Step 3: 验证DWS数据**

```bash
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit -e "
SELECT '=== DWS period_type分布 ===' AS chk;
SELECT period_type, COUNT(*) FROM dw_indicator_fact GROUP BY period_type;
SELECT '=== MONTH 2026-06 TOTAL行指标 ===' AS chk;
SELECT indicator_code, calc_value FROM dw_indicator_fact
WHERE period='2026-06' AND period_type='MONTH' AND dim_type='TOTAL' ORDER BY indicator_code;
SELECT '=== YEAR 2026 TOTAL行 ===' AS chk;
SELECT indicator_code, calc_value FROM dw_indicator_fact
WHERE period='2026' AND period_type='YEAR' AND dim_type='TOTAL' ORDER BY indicator_code;
SELECT '=== ORG维度 level分布(2026-06) ===' AS chk;
SELECT f.dim_id, o.name, o.level, f.indicator_code, f.calc_value
FROM dw_indicator_fact f JOIN dim_organization o ON f.dim_id=o.id
WHERE f.period='2026-06' AND f.period_type='MONTH' AND f.dim_type='ORG' AND f.indicator_code='LOAN_MONTHLY_PROFIT'
ORDER BY o.level, f.calc_value DESC;
" 2>&1 | grep -v Warning
```
Expected: MONTH和YEAR都有数据; YEAR行=MONTH各月求和; ORG维度有level 1/2/3三级数据(总行/分行/支行)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: ETL新增YEAR年度累计计算,验证DWS全维度层级数据正确"
```

---

**[确认点] Phase 3 完成。验证:DWS有MONTH/YEAR数据,全维度全层级,年度=各月累计,真实分摊op_cost。是否继续 Phase 4?**

---

## Phase 4: ADS层改造(Service直读DWS)

依赖Phase 3。DashboardServiceImpl/DimensionServiceImpl/IndicatorDetailServiceImpl改为直读DWS,期间参数period_type+period,卡片由指标库驱动。

### Task 10: 新增 IndicatorLibraryService

**Files:**
- Create: `后端/src/main/java/com/multiprofit/service/IndicatorLibraryService.java`
- Create: `后端/src/main/java/com/multiprofit/service/impl/IndicatorLibraryServiceImpl.java`

**Interfaces:**
- Produces: getCodesByCategory(String category), getAllCodes(), getIndicatorByCode(String code)

- [ ] **Step 1: 创建接口**

```java
// 后端/src/main/java/com/multiprofit/service/IndicatorLibraryService.java
package com.multiprofit.service;

import java.util.List;
import java.util.Map;

public interface IndicatorLibraryService {
    /** 按类别获取指标code列表 */
    List<String> getCodesByCategory(String category);
    /** 获取所有活跃指标code列表 */
    List<String> getAllActiveCodes();
    /** 按code获取指标详情 */
    Map<String, Object> getIndicatorByCode(String code);
}
```

- [ ] **Step 2: 创建实现**

```java
// 后端/src/main/java/com/multiprofit/service/impl/IndicatorLibraryServiceImpl.java
package com.multiprofit.service.impl;

import com.multiprofit.service.IndicatorLibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IndicatorLibraryServiceImpl implements IndicatorLibraryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<String> getCodesByCategory(String category) {
        return jdbcTemplate.queryForList(
            "SELECT code FROM indicator_library WHERE category = ? AND status = 1 ORDER BY sort_order",
            String.class, category);
    }

    @Override
    public List<String> getAllActiveCodes() {
        return jdbcTemplate.queryForList(
            "SELECT code FROM indicator_library WHERE status = 1 ORDER BY sort_order",
            String.class);
    }

    @Override
    public Map<String, Object> getIndicatorByCode(String code) {
        return jdbcTemplate.queryForMap(
            "SELECT * FROM indicator_library WHERE code = ? AND status = 1", code);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: 新增IndicatorLibraryService(按类别取code,指标库驱动)"
```

---

### Task 11: 改造 DashboardServiceImpl

**Files:**
- Modify: `后端/src/main/java/com/multiprofit/service/impl/DashboardServiceImpl.java`

**Interfaces:**
- Consumes: IndicatorLibraryService, dw_indicator_fact(period_type+period)
- Produces: DashboardDTO(KPI卡由指标库驱动,code/name统一,期间参数period_type+period)

- [ ] **Step 1: 注入IndicatorLibraryService,重写getDashboardData**

```java
@Autowired
private IndicatorLibraryService indicatorLibraryService;

@Override
public DashboardDTO getDashboardData(String startDate, String endDate, String caliberType, Long orgScope) {
    // 1. 确定期间类型和期间值
    // 如果startDate是YYYY-MM-DD格式,前端传的是DatePicker,这里统一为YEAR
    String periodType = determinePeriodType(startDate, endDate);
    String period = determinePeriod(startDate, endDate, periodType);

    // 2. 从指标库取驾驶舱需要的指标code列表
    List<String> profitCodes = indicatorLibraryService.getCodesByCategory("PROFIT");
    List<String> revenueCodes = indicatorLibraryService.getCodesByCategory("REVENUE");
    List<String> costCodes = indicatorLibraryService.getCodesByCategory("COST");

    // 3. 从DWS直读(按period_type+period)
    List<String> allCodes = new ArrayList<>();
    allCodes.addAll(profitCodes);
    allCodes.addAll(revenueCodes);
    allCodes.addAll(costCodes);

    String sql = "SELECT indicator_code, calc_value FROM dw_indicator_fact " +
        "WHERE period = ? AND period_type = ? AND dim_type = 'TOTAL' AND caliber_type = ? " +
        "AND indicator_code IN (" + placeholders(allCodes.size()) + ")";

    List<Object> params = new ArrayList<>();
    params.add(period);
    params.add(periodType);
    params.add(caliberType);
    params.addAll(allCodes);

    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

    // 4. 构建指标值映射
    Map<String, BigDecimal> indicatorMap = new HashMap<>();
    for (Map<String, Object> row : rows) {
        String code = (String) row.get("indicator_code");
        BigDecimal value = (BigDecimal) row.get("calc_value");
        indicatorMap.put(code, value);
    }

    // 5. 构建KPI卡片(由指标库驱动,code/name统一)
    List<DashboardDTO.KpiCard> kpiCards = new ArrayList<>();
    for (String code : profitCodes) {
        Map<String, Object> indicator = indicatorLibraryService.getIndicatorByCode(code);
        if (indicator != null) {
            kpiCards.add(createKpiCard(code, (String) indicator.get("name"),
                indicatorMap.getOrDefault(code, BigDecimal.ZERO), (String) indicator.get("unit")));
        }
    }
    for (String code : revenueCodes) {
        Map<String, Object> indicator = indicatorLibraryService.getIndicatorByCode(code);
        if (indicator != null) {
            kpiCards.add(createKpiCard(code, (String) indicator.get("name"),
                indicatorMap.getOrDefault(code, BigDecimal.ZERO), (String) indicator.get("unit")));
        }
    }
    for (String code : costCodes) {
        Map<String, Object> indicator = indicatorLibraryService.getIndicatorByCode(code);
        if (indicator != null) {
            kpiCards.add(createKpiCard(code, (String) indicator.get("name"),
                indicatorMap.getOrDefault(code, BigDecimal.ZERO), (String) indicator.get("unit")));
        }
    }

    // 6. 瀑布图/趋势/维度概览保持类似改造...
    // (此处省略瀑布图和趋势的完整重写,原理相同:直读DWS)

    DashboardDTO dashboard = new DashboardDTO();
    dashboard.setKpiCards(kpiCards);
    // ... 设置其他字段
    return dashboard;
}

// 辅助方法
private String determinePeriodType(String startDate, String endDate) {
    // 前端统一传period_type,从请求参数取(需Controller配合)
    // 兼容旧逻辑:通过日期范围推断
    if (startDate.endsWith("-01-01") && endDate.endsWith("-12-31")) return "YEAR";
    if (startDate.equals(endDate)) return "DAY";
    return "MONTH";
}

private String determinePeriod(String startDate, String endDate, String periodType) {
    switch (periodType) {
        case "YEAR": return startDate.substring(0, 4);
        case "MONTH": return startDate.substring(0, 7);
        case "DAY": return startDate;
        default: return startDate.substring(0, 7);
    }
}

private String placeholders(int count) {
    return String.join(",", Collections.nCopies(count, "?"));
}
```

- [ ] **Step 2: 同步改造DashboardController,增加period_type参数**

在 `DashboardController.java` 的 `getDashboardData` 方法签名中添加:

```java
@RequestParam(required = false, defaultValue = "MONTH") String periodType
```

传递给Service。

- [ ] **Step 3: 编译验证**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析/后端/
mvn compile -q 2>&1 | tail -3
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: DashboardServiceImpl改造(直读DWS,period_type+period,指标库驱动KPI卡)"
```

---

### Task 12: 改造 DimensionServiceImpl

**Files:**
- Modify: `后端/src/main/java/com/multiprofit/service/impl/DimensionServiceImpl.java`

**改造原则与DashboardServiceImpl相同**:直读DWS,period_type+period,指标库驱动。核心改动:

- `getAnalysisData`: period改为period_type+period确定,SQL从dim_name文本匹配改为JOIN dim_*表的dim_id
- `getTreeData`: indicatorSql改为按dim_id关联
- 删除 `getEffectivePeriod`/`getLatestPeriod` 方法(不再需要单月兜底)

```java
// getAnalysisData核心SQL改造后:
String sql = "SELECT f.dim_id, dm.name as dim_name, f.indicator_code, f.calc_value " +
    "FROM dw_indicator_fact f " +
    "JOIN " + getDimTable(dimType) + " dm ON f.dim_id = dm.id " +
    "WHERE f.period = ? AND f.period_type = ? AND f.dim_type = ? AND f.caliber_type = ? " +
    "AND f.indicator_code IN (" + placeholders(codes.size()) + ") " +
    "ORDER BY dm.name";
```

- [ ] **Step 1: 编译**

```bash
mvn compile -q 2>&1 | tail -3
```

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "feat: DimensionServiceImpl改造(直读DWS,dim_id关联dim_*表,period_type+period)"
```

---

### Task 13: 改造 IndicatorDetailServiceImpl + IndicatorController

**Files:**
- Modify: `后端/src/main/java/com/multiprofit/service/impl/IndicatorDetailServiceImpl.java`
- Modify: `后端/src/main/java/com/multiprofit/controller/IndicatorController.java`

**改造原则**:
- IndicatorDetailService 改为直读DWS(按period_type+period+dim_type)
- IndicatorController 删除对 indicator_definition 的引用,改用 indicator_library

- [ ] **Step 1: IndicatorController 删除 indicator_definition 引用**

搜索 `indicator_definition` 所有引用,替换为读取 `indicator_library`。

- [ ] **Step 2: 编译 + Commit**

```bash
mvn compile -q 2>&1 | tail -3
git add -A
git commit -m "feat: IndicatorController改造(删除indicator_definition引用,改用indicator_library)"
```

---

**[确认点] Phase 4 完成。验证:后端编译通过,Dashboard/Dimension/Indicator Service全部直读DWS,卡片由指标库驱动。是否继续 Phase 5?**

---

## Phase 5: biz_ledger 删除 + 11文件52处SQL改造

依赖Phase 3+4(DWS数据就绪,dim_*表就绪)。删除biz_ledger表,改造所有引用。

### Task 14: 改造 AiServiceImpl + FunctionRegistry

**Files:**
- Modify: `后端/src/main/java/com/multiprofit/service/impl/AiServiceImpl.java`
- Modify: `后端/src/main/java/com/multiprofit/ai/FunctionRegistry.java`

**AiServiceImpl 改造(3处):**
- 第88行SQL: `FROM biz_ledger WHERE account_period = '%s'` → `FROM dw_indicator_fact WHERE period = '%s' AND period_type = 'MONTH' AND dim_type = 'TOTAL'`
- 第130行提示词: `表名:biz_ledger` → `表名:dw_indicator_fact(period/period_type/dim_type/indicator_code/calc_value)`
- 第160行提示词: 同上
- 第184行getClickHouseSchema: 返回dw_indicator_fact表结构描述

**FunctionRegistry 改造(2处):**
- 第69行: `query_biz_ledger` → 重命名为 `query_indicator_fact`,描述改为"查询指标事实表数据"
- 第400行: case改为新名称

- [ ] **Step 1: 改造 + 编译 + Commit**

```bash
mvn compile -q 2>&1 | tail -3
git add -A
git commit -m "feat: AiServiceImpl+FunctionRegistry改造(biz_ledger→dw_indicator_fact)"
```

### Task 15: 改造 MCP 4个Server

**Files:**
- Modify: `后端/src/main/java/com/multiprofit/mcp/BizDataMcpServer.java`
- Modify: `后端/src/main/java/com/multiprofit/mcp/AnalysisMcpServer.java`
- Modify: `后端/src/main/java/com/multiprofit/mcp/ReportMcpServer.java`
- Modify: `后端/src/main/java/com/multiprofit/mcp/GovernanceMcpServer.java`

**改造策略**:所有 `FROM biz_ledger` + `WHERE period = ?` → `FROM dw_indicator_fact` + `WHERE period = ? AND period_type = 'MONTH' AND dim_type = 'TOTAL'`;维度分组查询改为 `dim_type = ? AND dim_id = ?`。

- [ ] **Step 1: 逐个文件改造 + 编译**

```bash
mvn compile -q 2>&1 | tail -3
```

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "feat: MCP 4个Server改造(biz_ledger→dw_indicator_fact,修正period→period+period_type)"
```

### Task 16: 改造 ReportController + DataGovernanceController + ExportServiceImpl + DataValidationServiceImpl + AiExploreController

**Files:**
- Modify: `后端/src/main/java/com/multiprofit/controller/ReportController.java`(9处)
- Modify: `后端/src/main/java/com/multiprofit/controller/DataGovernanceController.java`(8处)
- Modify: `后端/src/main/java/com/multiprofit/service/impl/ExportServiceImpl.java`(8处)
- Modify: `后端/src/main/java/com/multiprofit/service/impl/DataValidationServiceImpl.java`(2处)
- Modify: `后端/src/main/java/com/multiprofit/controller/AiExploreController.java`(8处)

**改造策略**:
- 聚合查询 → 读 dw_indicator_fact(TOTAL行或维度行)
- 明细导出 → 读 loan_indicator_detail + deposit_indicator_detail UNION
- JOIN dimension_master → JOIN 对应 dim_* 表
- AiExploreController写死'2026-05' → 去掉硬编码,用最新期间

- [ ] **Step 1: 逐个文件改造 + 编译**

```bash
mvn compile -q 2>&1 | tail -3
```

- [ ] **Step 2: 确认biz_ledger表可删除**

```bash
# 编译全部通过,确认无biz_ledger引用
cd /home/zhaoz0009/.claude/projects/多维盈利分析/后端/
grep -r "biz_ledger" src/ --include=*.java --include=*.xml 2>/dev/null | wc -l
```
Expected: 0

- [ ] **Step 3: 删除biz_ledger表 + Commit**

```bash
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit -e "DROP TABLE IF EXISTS biz_ledger;"
git add -A
git commit -m "feat: 删除biz_ledger,完成11文件全部引用改造(→dw_indicator_fact/dim_*)"
```

---

**[确认点] Phase 5 完成。验证:后端编译通过,grep biz_ledger=0,biz_ledger表已删除。是否继续 Phase 6?**

---

## Phase 6: 前端改造

依赖Phase 4+5(后端API已改)。前端按metricCode取卡,期间参数period_type+period,去掉中文名取卡,默认日期取最新有数据期间。

### Task 17: 改造 Dashboard/index.tsx

**Files:**
- Modify: `前端/src/pages/Dashboard/index.tsx`

**改造点:**
1. `getCard` 从按中文名 → 按 metricCode
2. 请求参数增加 `periodType`
3. "本年"按钮传 `periodType='YEAR', startDate='2026-01-01'`
4. 默认日期改为最新有数据期间
5. 瀑布图/趋势图数据字段更新为新code

- [ ] **Step 1: 修改KPI卡片取值**

```tsx
// 旧(行111-119): const totalProfit = getCard('总利润');
// 新:
const getCardByCode = (code: string) => kpiCards.find((c: any) => c.metricCode === code);
const totalProfit = getCardByCode('TOTAL_MONTHLY_PROFIT');
const loanProfit = getCardByCode('LOAN_MONTHLY_PROFIT');
const depositProfit = getCardByCode('DEPOSIT_MONTHLY_PROFIT');
const loanInterest = getCardByCode('LOAN_MONTHLY_INTEREST');
const ftpIncome = getCardByCode('FTP_MONTHLY_INCOME');
const loanFtpCost = getCardByCode('LOAN_FTP_COST');
const loanRiskCost = getCardByCode('LOAN_RISK_COST');
const loanOpCost = getCardByCode('LOAN_OP_COST');
const depositOpCost = getCardByCode('DEPOSIT_OP_COST');
```

- [ ] **Step 2: 修改请求参数**

```tsx
// 旧(行34-36):
const startDate = dateRange[0].format('YYYY-MM-DD');
const endDate = dateRange[1].format('YYYY-MM-DD');
// 新:
const startDate = dateRange[0].format('YYYY-MM-DD');
const endDate = dateRange[1].format('YYYY-MM-DD');
const periodType = quickSelect === 'thisYear' ? 'YEAR' : 
                   quickSelect === 'today' ? 'DAY' : 'MONTH';
// 请求增加periodType参数
api.get('/dashboard/overview', { params: { startDate, endDate, caliberType, periodType } })
```

- [ ] **Step 3: 修改默认日期**

```tsx
// 旧(行21): const [dateRange] = useState([dayjs().startOf('month'), dayjs()]);
// 新:取最新有数据期间(通过API获取或默认上个月)
const [dateRange] = useState([dayjs().subtract(1, 'month').startOf('month'), 
                               dayjs().subtract(1, 'month').endOf('month')]);
```

- [ ] **Step 4: Commit**

```bash
git add 前端/src/pages/Dashboard/index.tsx
git commit -m "feat: Dashboard前端改造(按metricCode取卡,period_type+period,默认最新有数据期间)"
```

### Task 18: 改造 DimensionAnalysis/index.tsx

**Files:**
- Modify: `前端/src/pages/DimensionAnalysis/index.tsx`

**改造点与Dashboard相同**:按code取卡,期间参数period_type+period,默认日期修正。

- [ ] **Step 1: 改造 + Commit**

```bash
git add 前端/src/pages/DimensionAnalysis/index.tsx
git commit -m "feat: DimensionAnalysis前端改造(按metricCode取卡,period_type+period)"
```

---

## 最终验收

### Task 19: 全流程验收测试

- [ ] **Step 1: 编译后端**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析/后端/
mvn clean compile -DskipTests -q 2>&1 | tail -3
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 启动后端**

```bash
mvn spring-boot:run -q &
```

- [ ] **Step 3: 测试驾驶舱API(年度)**

```bash
curl -s "http://localhost:8080/api/dashboard/overview?startDate=2026-01-01&endDate=2026-12-31&caliberType=ASSESS&periodType=YEAR" | python3 -m json.tool | head -30
```
Expected: KPI卡片数组有TOTAL_YEARLY_PROFIT/LOAN_YEARLY_PROFIT等,值不为0,且是1~6月累计

- [ ] **Step 4: 测试维度分析API(ORG,年度)**

```bash
curl -s "http://localhost:8080/api/dimension/ORG/tree?startDate=2026-01-01&endDate=2026-12-31&caliberType=ASSESS&periodType=YEAR&parentId=0" | python3 -m json.tool | head -20
```
Expected: 返回ORG维度树,level 1/2/3各级都有profit数据

- [ ] **Step 5: 测试维度分析API(DEPT,之前全空)**

```bash
curl -s "http://localhost:8080/api/dimension/DEPT/tree?startDate=2026-01-01&endDate=2026-12-31&caliberType=ASSESS&periodType=YEAR&parentId=0" | python3 -m json.tool | head -20
```
Expected: 返回DEPT维度树,节点有profit数据(不再是全0)

- [ ] **Step 6: 验证数据库**

```bash
mysql -h127.0.0.1 -P3306 -umpuser -p<DB_PASSWORD> multi_profit -e "
SELECT '=== 废弃表已全部删除 ===' AS chk;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='multi_profit'
AND table_name IN ('biz_ledger','dimension_master','dw_dim_organization','dw_dim_biz_line','dw_dim_product','dw_dim_channel','dw_dim_manager','dw_dim_customer','indicator_definition','atomic_indicator','derived_indicator','indicator_precomputed','indicator_pre_calc','indicator_summary','indicator_stat_config');
SELECT '=== dim_* 7表数据量 ===' AS chk;
SELECT 'dim_organization' AS tbl, COUNT(*) FROM dim_organization
UNION ALL SELECT 'dim_biz_line', COUNT(*) FROM dim_biz_line
UNION ALL SELECT 'dim_dept', COUNT(*) FROM dim_dept
UNION ALL SELECT 'dim_product', COUNT(*) FROM dim_product
UNION ALL SELECT 'dim_channel', COUNT(*) FROM dim_channel
UNION ALL SELECT 'dim_manager', COUNT(*) FROM dim_manager
UNION ALL SELECT 'dim_customer_type', COUNT(*) FROM dim_customer_type;
SELECT '=== indicator_library指标数 ===' AS chk;
SELECT COUNT(*) FROM indicator_library;
SELECT '=== DWS period_type分布 ===' AS chk;
SELECT period_type, COUNT(*) FROM dw_indicator_fact GROUP BY period_type;
SELECT '=== 代码中无biz_ledger残留 ===' AS chk;
" 2>&1 | grep -v Warning
```

- [ ] **Step 7: Final Commit**

```bash
git add -A
git commit -m "feat: 全流程验收通过(驾驶舱年度API正确,维度分析全维度有数据,废弃表清零)"
```

---

**实施计划结束。共6阶段19个任务,预期:驾驶舱"本年度利润"为1~6月累计(约2859亿而非282.55亿),所有指标有值,维度分析全维度全层级有数据,废弃表清零,指标库驱动。**

