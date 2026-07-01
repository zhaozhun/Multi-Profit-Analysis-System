# ETL与费用分摊集成实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将费用分摊结果集成到数据仓库ETL流程中，实现完整的数据处理链路

**Architecture:** 应用层编排模式，先执行费用分摊，再调用ETL存储过程生成DWD层和DWS层

**Tech Stack:** MySQL, Spring Boot, MyBatis-Plus, JdbcTemplate

## Global Constraints

- 原始数据不直接修改，加工后的数据单独保存
- 当前阶段使用模拟运营成本，后续集成真实分摊结果
- 维度关联通过biz_id实现

---

### Task 1: 修改ETL存储过程 - 集成费用分摊结果

**Files:**
- Modify: `数据库脚本/etl_procedure.sql`

**Interfaces:**
- Consumes: expense_allocation_result表（biz_id, allocated_amount）
- Produces: dwd_loan_detail, dwd_deposit_detail（含op_cost字段）

- [ ] **Step 1: 备份当前ETL存储过程**

```bash
cp /home/zhaoz0009/.claude/projects/多维盈利分析/数据库脚本/etl_procedure.sql /home/zhaoz0009/.claude/projects/多维盈利分析/数据库脚本/etl_procedure.sql.bak
```

- [ ] **Step 2: 修改ETL存储过程 - 贷款部分**

修改sp_etl_recalculate，从expense_allocation_result读取运营成本：

```sql
-- 在贷款INSERT语句中，将op_cost从固定值改为从分摊结果获取
INSERT INTO dwd_loan_detail (
    biz_id, account_period, caliber_type,
    org_id, org_name, biz_line_id, biz_line_name,
    product_id, product_name, channel_id, channel_name,
    manager_id, manager_name, customer_id, customer_name,
    loan_balance, loan_monthly_interest, ftp_cost, risk_cost, op_cost,
    loan_profit, net_interest_margin
)
SELECT
    l.biz_id, l.account_period, l.caliber_type,
    o.id, l.org_name, bl.id, l.biz_line_name,
    p.id, l.product_name, c.id, l.channel_name,
    m.id, l.manager_name, l.customer_id, l.customer_name,
    l.loan_balance, l.loan_monthly_interest, l.ftp_cost, l.risk_cost,
    COALESCE(ear.op_cost, 0),  -- 从分摊结果获取，如果没有则为0
    (l.loan_monthly_interest - l.ftp_cost - l.risk_cost - COALESCE(ear.op_cost, 0)),
    (l.loan_monthly_interest - l.ftp_cost)
FROM loan_indicator_detail l
LEFT JOIN dw_dim_organization o ON l.org_name = o.org_name
LEFT JOIN dw_dim_biz_line bl ON l.biz_line_name = bl.line_name
LEFT JOIN dw_dim_product p ON l.product_name = p.product_name
LEFT JOIN dw_dim_channel c ON l.channel_name = c.channel_name
LEFT JOIN dw_dim_manager m ON l.manager_name = m.manager_name
LEFT JOIN (
    SELECT biz_id, SUM(allocated_amount) as op_cost
    FROM expense_allocation_result
    WHERE period = p_period
    GROUP BY biz_id
) ear ON l.biz_id = ear.biz_id
WHERE l.account_period = p_period;
```

- [ ] **Step 3: 修改ETL存储过程 - 存款部分**

```sql
-- 存款部分类似修改
INSERT INTO dwd_deposit_detail (
    biz_id, account_period, caliber_type,
    org_id, org_name, biz_line_id, biz_line_name,
    product_id, product_name, channel_id, channel_name,
    manager_id, manager_name, customer_id, customer_name,
    deposit_balance, deposit_monthly_interest, ftp_income, op_cost,
    deposit_profit
)
SELECT
    d.biz_id, d.account_period, d.caliber_type,
    o.id, d.org_name, bl.id, d.biz_line_name,
    p.id, d.product_name, c.id, d.channel_name,
    m.id, d.manager_name, d.customer_id, d.customer_name,
    d.deposit_balance, d.deposit_monthly_interest, d.ftp_income,
    COALESCE(ear.op_cost, 0),  -- 从分摊结果获取
    (d.ftp_income - d.deposit_monthly_interest - COALESCE(ear.op_cost, 0))
FROM deposit_indicator_detail d
LEFT JOIN dw_dim_organization o ON d.org_name = o.org_name
LEFT JOIN dw_dim_biz_line bl ON d.biz_line_name = bl.line_name
LEFT JOIN dw_dim_product p ON d.product_name = p.product_name
LEFT JOIN dw_dim_channel c ON d.channel_name = c.channel_name
LEFT JOIN dw_dim_manager m ON d.manager_name = m.manager_name
LEFT JOIN (
    SELECT biz_id, SUM(allocated_amount) as op_cost
    FROM expense_allocation_result
    WHERE period = p_period
    GROUP BY biz_id
) ear ON d.biz_id = ear.biz_id
WHERE d.account_period = p_period;
```

- [ ] **Step 4: 验证SQL语法**

```bash
mysql -u root -p'Zhaoz0009!' multi_profit < /home/zhaoz0009/.claude/projects/多维盈利分析/数据库脚本/etl_procedure.sql
```

- [ ] **Step 5: 提交更改**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析
git add 数据库脚本/etl_procedure.sql
git commit -m "feat: 修改ETL存储过程，集成费用分摊结果"
```

---

### Task 2: 创建模拟数据生成脚本

**Files:**
- Create: `数据库脚本/generate_expense_allocation_data.sql`

**Interfaces:**
- Produces: expense_allocation_result表数据

- [ ] **Step 1: 创建模拟数据生成脚本**

```sql
-- 生成模拟的费用分摊结果数据
-- 按业务金额的1%-3%生成运营成本

DELETE FROM expense_allocation_result WHERE period = '2026-06';

-- 贷款业务的运营成本
INSERT INTO expense_allocation_result (period, biz_id, biz_type, expense_type, expense_name, allocated_amount, rule_code, batch_no)
SELECT
    '2026-06' as period,
    l.biz_id,
    'LOAN' as biz_type,
    'OP_COST' as expense_type,
    '运营成本' as expense_name,
    l.loan_balance * (0.01 + RAND() * 0.02) as allocated_amount,
    'SIMULATED' as rule_code,
    'BATCH_202606' as batch_no
FROM loan_indicator_detail l
WHERE l.account_period = '2026-06';

-- 存款业务的运营成本
INSERT INTO expense_allocation_result (period, biz_id, biz_type, expense_type, expense_name, allocated_amount, rule_code, batch_no)
SELECT
    '2026-06' as period,
    d.biz_id,
    'DEPOSIT' as biz_type,
    'OP_COST' as expense_type,
    '运营成本' as expense_name,
    d.deposit_balance * (0.01 + RAND() * 0.02) as allocated_amount,
    'SIMULATED' as rule_code,
    'BATCH_202606' as batch_no
FROM deposit_indicator_detail d
WHERE d.account_period = '2026-06';

SELECT '模拟费用分摊数据生成完成' AS result;
```

- [ ] **Step 2: 执行模拟数据生成**

```bash
mysql -u root -p'Zhaoz0009!' multi_profit < /home/zhaoz0009/.claude/projects/多维盈利分析/数据库脚本/generate_expense_allocation_data.sql
```

- [ ] **Step 3: 验证数据**

```bash
mysql -u root -p'Zhaoz0009!' multi_profit -e "
SELECT COUNT(*) as cnt, SUM(allocated_amount) as total
FROM expense_allocation_result
WHERE period = '2026-06';
"
```

- [ ] **Step 4: 提交更改**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析
git add 数据库脚本/generate_expense_allocation_data.sql
git commit -m "feat: 创建模拟费用分摊数据生成脚本"
```

---

### Task 3: 修改后端代码 - 应用层编排

**Files:**
- Modify: `后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java`

**Interfaces:**
- Consumes: ExpenseAllocationService.execute(period)
- Produces: ETLResult

- [ ] **Step 1: 读取当前DataWarehouseETLServiceImpl代码**

```bash
cat /home/zhaoz0009/.claude/projects/多维盈利分析/后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java
```

- [ ] **Step 2: 修改execute方法，集成费用分摊**

```java
@Override
public Map<String, Object> executeEtl(String period) {
    Map<String, Object> result = new HashMap<>();
    long startTime = System.currentTimeMillis();

    try {
        // 1. 执行费用分摊（当前用模拟数据）
        log.info("开始执行费用分摊: {}", period);
        generateExpenseAllocationData(period);

        // 2. 调用ETL存储过程
        log.info("开始执行ETL存储过程: {}", period);
        jdbcTemplate.execute("CALL sp_etl_recalculate('" + period + "')");

        // 3. 统计结果
        long timeCost = System.currentTimeMillis() - startTime;
        result.put("success", true);
        result.put("message", "ETL执行成功");
        result.put("period", period);
        result.put("timeCost", timeCost);

        log.info("ETL执行完成: {}, 耗时: {}ms", period, timeCost);
    } catch (Exception e) {
        log.error("ETL执行失败: {}", period, e);
        result.put("success", false);
        result.put("message", "ETL执行失败: " + e.getMessage());
    }

    return result;
}

/**
 * 生成模拟费用分摊数据
 */
private void generateExpenseAllocationData(String period) {
    // 清除旧数据
    jdbcTemplate.update("DELETE FROM expense_allocation_result WHERE period = ?", period);

    // 生成贷款业务的运营成本
    String loanSql = "INSERT INTO expense_allocation_result " +
        "(period, biz_id, biz_type, expense_type, expense_name, allocated_amount, rule_code, batch_no) " +
        "SELECT ?, l.biz_id, 'LOAN', 'OP_COST', '运营成本', " +
        "l.loan_balance * (0.01 + RAND() * 0.02), 'SIMULATED', 'BATCH_' + ? " +
        "FROM loan_indicator_detail l WHERE l.account_period = ?";
    jdbcTemplate.update(loanSql, period, period, period);

    // 生成存款业务的运营成本
    String depositSql = "INSERT INTO expense_allocation_result " +
        "(period, biz_id, biz_type, expense_type, expense_name, allocated_amount, rule_code, batch_no) " +
        "SELECT ?, d.biz_id, 'DEPOSIT', 'OP_COST', '运营成本', " +
        "d.deposit_balance * (0.01 + RAND() * 0.02), 'SIMULATED', 'BATCH_' + ? " +
        "FROM deposit_indicator_detail d WHERE d.account_period = ?";
    jdbcTemplate.update(depositSql, period, period, period);

    log.info("模拟费用分摊数据生成完成: {}", period);
}
```

- [ ] **Step 3: 编译验证**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析/后端
mvn compile -q
```

- [ ] **Step 4: 提交更改**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析
git add 后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java
git commit -m "feat: 修改ETL服务，集成费用分摊"
```

---

### Task 4: 测试验证 - 完整流程

**Files:**
- Test: 无新文件，使用现有API

**Interfaces:**
- Consumes: POST /api/etl/execute?period=2026-06
- Produces: ETL执行结果

- [ ] **Step 1: 重启后端服务**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析/后端
mvn spring-boot:run &
```

- [ ] **Step 2: 调用ETL执行API**

```bash
curl -X POST "http://localhost:8080/api/etl/execute?period=2026-06"
```

- [ ] **Step 3: 验证DWD层数据**

```bash
mysql -u root -p'Zhaoz0009!' multi_profit -e "
SELECT COUNT(*) as cnt, SUM(op_cost) as total_op_cost, SUM(loan_profit) as total_profit
FROM dwd_loan_detail WHERE account_period = '2026-06';
"
```

- [ ] **Step 4: 验证DWS层数据**

```bash
mysql -u root -p'Zhaoz0009!' multi_profit -e "
SELECT indicator_code, dim_type, SUM(calc_value) as total
FROM dw_indicator_fact WHERE period = '2026-06'
GROUP BY indicator_code, dim_type
ORDER BY indicator_code, dim_type;
"
```

- [ ] **Step 5: 验证前端页面**

打开浏览器访问：http://localhost:5173/dimension-analysis

- [ ] **Step 6: 提交最终更改**

```bash
cd /home/zhaoz0009/.claude/projects/多维盈利分析
git add .
git commit -m "feat: ETL与费用分摊集成完成"
```

---

## 执行顺序

1. Task 1: 修改ETL存储过程
2. Task 2: 创建模拟数据生成脚本
3. Task 3: 修改后端代码
4. Task 4: 测试验证

## 预期结果

- DWD层每笔业务包含完整的成本信息（利息收入、FTP成本、风险成本、运营成本）
- DWS层按维度汇总正确
- 前端页面显示正确的运营成本数据
