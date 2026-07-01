# ETL与费用分摊集成设计文档

## 一、设计背景

### 1.1 当前问题

1. **费用分摊模块独立**：费用分摊结果（expense_allocation_result）与数据仓库（DWD/DWS）没有集成
2. **运营成本来源不明确**：DWD层的运营成本字段没有数据来源
3. **ETL流程不完整**：缺少费用分摊这一步骤

### 1.2 设计目标

1. **集成费用分摊**：将费用分摊结果写入DWD层
2. **完整数据流程**：原始数据 → 费用分摊 → DWD层 → DWS层
3. **维度关联**：在业务表上打维度标，通过biz_id关联费用分摊结果

---

## 二、数据流程设计

### 2.1 完整流程

```
┌─────────────────────────────────────────────────────────────┐
│                        ODS层（原始层）                        │
│  loan_indicator_detail                                       │
│  ├── biz_id, 利息收入, FTP成本, 风险成本                      │
│  └── org_id, biz_line_id, product_id, channel_id, manager_id│
│                                                              │
│  deposit_indicator_detail                                    │
│  ├── biz_id, 利息收入, FTP成本                               │
│  └── org_id, biz_line_id, product_id, channel_id, manager_id│
│                                                              │
│  expense_*（费用原始数据）                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
                    步骤1：费用分摊
                    按维度筛选业务，计算分摊比例
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   expense_allocation_result                  │
│  biz_id, allocated_amount（每笔业务的运营成本）                │
└─────────────────────────────────────────────────────────────┘
                            ↓
                    步骤2：关联生成DWD层
                    业务表 LEFT JOIN 分摊结果 ON biz_id
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                        DWD层（明细层）                        │
│  dwd_loan_detail                                             │
│  ├── biz_id, 利息收入, FTP成本, 风险成本, 运营成本             │
│  └── org_id, biz_line_id, product_id, channel_id, manager_id│
└─────────────────────────────────────────────────────────────┘
                            ↓
                    步骤3：汇总到DWS层
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                        DWS层（汇总层）                        │
│  dw_indicator_fact（按维度汇总）                              │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 各层职责

| 层级 | 职责 | 数据来源 |
|------|------|----------|
| ODS层 | 存储原始数据，不修改 | 业务系统 |
| 费用分摊 | 将费用分摊到每笔业务 | ODS层 + 分摊规则 |
| DWD层 | 存储完整明细（含运营成本） | ODS层 + 分摊结果 |
| DWS层 | 按维度汇总 | DWD层 |

---

## 三、表结构设计

### 3.1 ODS层（已有）

**loan_indicator_detail**：
- biz_id：业务编号
- loan_monthly_interest：利息收入
- ftp_cost：FTP成本
- risk_cost：风险成本
- org_id, biz_line_id, product_id, channel_id, manager_id：维度字段

**deposit_indicator_detail**：
- biz_id：业务编号
- deposit_monthly_interest：利息收入
- ftp_income：FTP成本
- org_id, biz_line_id, product_id, channel_id, manager_id：维度字段

**expense_*（费用原始表）**：
- 已有维度字段（dept_id, product_id, org_id等）

### 3.2 费用分摊结果表（已有）

**expense_allocation_result**：
- biz_id：业务编号
- allocated_amount：分摊金额
- expense_type：费用类型
- period：期间

### 3.3 DWD层（已有）

**dwd_loan_detail**：
- biz_id：业务编号
- loan_monthly_interest：利息收入
- ftp_cost：FTP成本
- risk_cost：风险成本
- op_cost：运营成本（从分摊结果获取）
- loan_profit：贷款利润（派生指标）
- org_id, biz_line_id, product_id, channel_id, manager_id：维度字段

**dwd_deposit_detail**：
- biz_id：业务编号
- deposit_monthly_interest：利息收入
- ftp_income：FTP成本
- op_cost：运营成本（从分摊结果获取）
- deposit_profit：存款利润（派生指标）
- org_id, biz_line_id, product_id, channel_id, manager_id：维度字段

### 3.4 DWS层（已有）

**dw_indicator_fact**：
- indicator_code：指标编码
- period：期间
- dim_type：维度类型
- dim_id：维度ID
- calc_value：指标值

---

## 四、ETL流程设计

### 4.1 应用层编排

```java
// DataWarehouseETLServiceImpl.java
public ETLResult execute(String period) {
    // 1. 执行费用分摊（当前用模拟数据）
    expenseAllocationService.execute(period);
    
    // 2. 调用ETL存储过程
    etlProcedure(period);
    
    // 3. 返回结果
    return new ETLResult(...);
}
```

### 4.2 ETL存储过程

```sql
-- sp_etl_recalculate(period)
-- 1. 清除DWD层数据
-- 2. 从ODS层读取业务明细
-- 3. 从expense_allocation_result读取运营成本
-- 4. 关联生成DWD层完整明细
-- 5. 汇总到DWS层
```

### 4.3 当前阶段：模拟数据

由于当前阶段费用分摊模块还未完全集成，使用模拟数据：

```sql
-- 模拟运营成本：按业务金额的1%-3%随机生成
op_cost = loan_balance * (0.01 + RAND() * 0.02)
```

---

## 五、后端代码改动

### 5.1 DataWarehouseETLServiceImpl.java

```java
@Service
public class DataWarehouseETLServiceImpl implements DataWarehouseETLService {
    
    @Autowired
    private ExpenseAllocationService expenseAllocationService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public ETLResult execute(String period) {
        // 1. 执行费用分摊
        expenseAllocationService.execute(period);
        
        // 2. 调用ETL存储过程
        jdbcTemplate.execute("CALL sp_etl_recalculate('" + period + "')");
        
        // 3. 返回结果
        return new ETLResult(true, "执行成功");
    }
}
```

### 5.2 ExpenseAllocationService.java

```java
public interface ExpenseAllocationService {
    void execute(String period);
}
```

---

## 六、实施计划

### 6.1 阶段划分

| 阶段 | 内容 | 工作量 |
|------|------|--------|
| 阶段1 | 修改ETL存储过程，集成费用分摊 | 0.5天 |
| 阶段2 | 修改后端代码，应用层编排 | 0.5天 |
| 阶段3 | 测试验证 | 0.5天 |

### 6.2 详细任务

#### 阶段1：修改ETL存储过程
1. 修改sp_etl_recalculate，从expense_allocation_result读取运营成本
2. 当前阶段使用模拟数据
3. 验证数据正确性

#### 阶段2：修改后端代码
1. 修改DataWarehouseETLServiceImpl，调用费用分摊服务
2. 返回执行结果

#### 阶段3：测试验证
1. 验证费用分摊结果正确写入DWD层
2. 验证DWS层汇总数据正确
3. 验证前端页面显示正确

---

## 七、风险与注意事项

### 7.1 数据一致性
- 确保费用分摊结果与业务明细正确关联
- 确保DWD层数据完整

### 7.2 性能考虑
- 费用分摊可能影响ETL性能
- 需要监控执行时间

### 7.3 后续扩展
- 当前阶段使用模拟数据
- 后续集成真实费用分摊结果

---

## 八、相关文件

- ETL存储过程：`数据库脚本/etl_procedure.sql`
- 费用分摊表：`数据库脚本/expense_allocation_tables.sql`
- 后端代码：`后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java`
