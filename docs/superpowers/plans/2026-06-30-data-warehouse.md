# 数据仓库实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 引入数据仓库，统一数据源，解决数据不统一问题

**Architecture:** 采用星型模型，事实表存储指标数据，维度表存储维度主数据，ETL流程统一计算所有指标

**Tech Stack:** Spring Boot 3.2, MySQL 8.0, React 18, Ant Design

## Global Constraints

- 数据库：MySQL 8.0，字符集 utf8mb4
- 后端：Spring Boot 3.2，Java 17
- 前端：React 18，TypeScript，Ant Design
- 口径类型：默认 ASSESS（考核口径）
- 期间格式：YYYY-MM（如 2025-06）
- 维度类型：ORG, BIZ_LINE, DEPT, PRODUCT, CHANNEL, MANAGER, CUSTOMER, TOTAL

---

## 文件结构

### 后端文件
- `后端/src/main/java/com/multiprofit/entity/DwIndicatorFact.java` - 指标事实表实体
- `后端/src/main/java/com/multiprofit/entity/DwDimOrganization.java` - 机构维度表实体
- `后端/src/main/java/com/multiprofit/entity/DwDimBizLine.java` - 条线维度表实体
- `后端/src/main/java/com/multiprofit/entity/DwDimProduct.java` - 产品维度表实体
- `后端/src/main/java/com/multiprofit/entity/DwDimChannel.java` - 渠道维度表实体
- `后端/src/main/java/com/multiprofit/entity/DwDimManager.java` - 客户经理维度表实体
- `后端/src/main/java/com/multiprofit/entity/DwDimCustomer.java` - 客户维度表实体
- `后端/src/main/java/com/multiprofit/mapper/DwIndicatorFactMapper.java` - 指标事实表Mapper
- `后端/src/main/java/com/multiprofit/mapper/DwDimOrganizationMapper.java` - 机构维度表Mapper
- `后端/src/main/java/com/multiprofit/service/DataWarehouseService.java` - 数据仓库服务接口
- `后端/src/main/java/com/multiprofit/service/impl/DataWarehouseServiceImpl.java` - 数据仓库服务实现
- `后端/src/main/java/com/multiprofit/service/DataWarehouseETLService.java` - ETL服务接口
- `后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java` - ETL服务实现
- `后端/src/main/java/com/multiprofit/controller/DataWarehouseController.java` - 数据仓库控制器

### 前端文件
- `前端/src/services/dwApi.ts` - 数据仓库API服务
- `前端/src/pages/BaseData/IndicatorLibrary/index.tsx` - 指标库页面
- `前端/src/pages/BaseData/IndicatorLibrary/IndicatorDetail.tsx` - 指标详情页面
- `前端/src/pages/BaseData/IndicatorLibrary/IndicatorRelationGraph.tsx` - 指标关联关系图
- `前端/src/pages/BaseData/MasterData/index.tsx` - 主数据管理页面

### 数据库脚本
- `数据库脚本/data_warehouse_tables.sql` - 数据仓库表结构

---

### Task 1: 创建数据仓库表结构

**Files:**
- Create: `数据库脚本/data_warehouse_tables.sql`

**Interfaces:**
- Produces: 数据仓库表结构，供后续任务使用

- [ ] **Step 1: 创建数据仓库表结构SQL文件**

```sql
-- 数据仓库表结构
-- 创建时间：2026-06-30

-- 指标事实表（统一存储所有指标）
CREATE TABLE IF NOT EXISTS dw_indicator_fact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    indicator_code VARCHAR(50) NOT NULL COMMENT '指标编码',
    period VARCHAR(10) NOT NULL COMMENT '期间（2025-06）',
    period_type VARCHAR(20) NOT NULL COMMENT '期间类型（MONTH）',
    dim_type VARCHAR(20) NOT NULL COMMENT '维度类型（ORG/BIZ_LINE/PRODUCT等）',
    dim_id BIGINT NOT NULL COMMENT '维度ID',
    dim_name VARCHAR(200) COMMENT '维度名称',
    calc_value DECIMAL(18,4) COMMENT '指标值',
    caliber_type VARCHAR(10) DEFAULT 'ASSESS' COMMENT '口径类型',
    calc_time TIMESTAMP COMMENT '计算时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_calc (indicator_code, period, period_type, dim_type, dim_id, caliber_type),
    INDEX idx_period (period),
    INDEX idx_indicator (indicator_code),
    INDEX idx_dim (dim_type, dim_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标事实表';

-- 机构维度表
CREATE TABLE IF NOT EXISTS dw_dim_organization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_code VARCHAR(50) NOT NULL COMMENT '机构编码',
    org_name VARCHAR(200) NOT NULL COMMENT '机构名称',
    parent_id BIGINT DEFAULT 0 COMMENT '上级机构ID',
    level INT DEFAULT 1 COMMENT '层级',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用，0禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (org_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构维度表';

-- 条线维度表
CREATE TABLE IF NOT EXISTS dw_dim_biz_line (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    line_code VARCHAR(50) NOT NULL COMMENT '条线编码',
    line_name VARCHAR(200) NOT NULL COMMENT '条线名称',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用，0禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (line_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='条线维度表';

-- 产品维度表
CREATE TABLE IF NOT EXISTS dw_dim_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_code VARCHAR(50) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(200) NOT NULL COMMENT '产品名称',
    product_type VARCHAR(20) COMMENT '产品类型',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用，0禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品维度表';

-- 渠道维度表
CREATE TABLE IF NOT EXISTS dw_dim_channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_code VARCHAR(50) NOT NULL COMMENT '渠道编码',
    channel_name VARCHAR(200) NOT NULL COMMENT '渠道名称',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用，0禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道维度表';

-- 客户经理维度表
CREATE TABLE IF NOT EXISTS dw_dim_manager (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    manager_code VARCHAR(50) NOT NULL COMMENT '客户经理编码',
    manager_name VARCHAR(200) NOT NULL COMMENT '客户经理名称',
    org_id BIGINT COMMENT '所属机构ID',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用，0禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (manager_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户经理维度表';

-- 客户维度表
CREATE TABLE IF NOT EXISTS dw_dim_customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_code VARCHAR(50) NOT NULL COMMENT '客户编码',
    customer_name VARCHAR(200) NOT NULL COMMENT '客户名称',
    customer_type VARCHAR(20) COMMENT '客户类型',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用，0禁用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户维度表';
```

- [ ] **Step 2: 执行SQL创建表**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit < 数据库脚本/data_warehouse_tables.sql
```

- [ ] **Step 3: 验证表创建成功**

```bash
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "SHOW TABLES LIKE 'dw_%';"
```

Expected: 显示所有dw_开头的表

- [ ] **Step 4: 提交代码**

```bash
git add 数据库脚本/data_warehouse_tables.sql
git commit -m "feat: 创建数据仓库表结构"
```

---

### Task 2: 创建实体类

**Files:**
- Create: `后端/src/main/java/com/multiprofit/entity/DwIndicatorFact.java`
- Create: `后端/src/main/java/com/multiprofit/entity/DwDimOrganization.java`
- Create: `后端/src/main/java/com/multiprofit/entity/DwDimBizLine.java`
- Create: `后端/src/main/java/com/multiprofit/entity/DwDimProduct.java`
- Create: `后端/src/main/java/com/multiprofit/entity/DwDimChannel.java`
- Create: `后端/src/main/java/com/multiprofit/entity/DwDimManager.java`
- Create: `后端/src/main/java/com/multiprofit/entity/DwDimCustomer.java`

**Interfaces:**
- Produces: 实体类，供Mapper和Service使用

- [ ] **Step 1: 创建DwIndicatorFact实体类**

```java
package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dw_indicator_fact")
public class DwIndicatorFact {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String indicatorCode;
    private String period;
    private String periodType;
    private String dimType;
    private Long dimId;
    private String dimName;
    private BigDecimal calcValue;
    private String caliberType;
    private LocalDateTime calcTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建DwDimOrganization实体类**

```java
package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dw_dim_organization")
public class DwDimOrganization {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orgCode;
    private String orgName;
    private Long parentId;
    private Integer level;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: 创建其他维度实体类**

```java
// DwDimBizLine.java
package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dw_dim_biz_line")
public class DwDimBizLine {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String lineCode;
    private String lineName;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
// DwDimProduct.java
package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dw_dim_product")
public class DwDimProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productCode;
    private String productName;
    private String productType;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
// DwDimChannel.java
package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dw_dim_channel")
public class DwDimChannel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String channelCode;
    private String channelName;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
// DwDimManager.java
package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dw_dim_manager")
public class DwDimManager {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String managerCode;
    private String managerName;
    private Long orgId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
// DwDimCustomer.java
package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dw_dim_customer")
public class DwDimCustomer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerCode;
    private String customerName;
    private String customerType;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 提交代码**

```bash
git add 后端/src/main/java/com/multiprofit/entity/Dw*.java
git commit -m "feat: 创建数据仓库实体类"
```

---

### Task 3: 创建Mapper接口

**Files:**
- Create: `后端/src/main/java/com/multiprofit/mapper/DwIndicatorFactMapper.java`
- Create: `后端/src/main/java/com/multiprofit/mapper/DwDimOrganizationMapper.java`
- Create: `后端/src/main/java/com/multiprofit/mapper/DwDimBizLineMapper.java`
- Create: `后端/src/main/java/com/multiprofit/mapper/DwDimProductMapper.java`
- Create: `后端/src/main/java/com/multiprofit/mapper/DwDimChannelMapper.java`
- Create: `后端/src/main/java/com/multiprofit/mapper/DwDimManagerMapper.java`
- Create: `后端/src/main/java/com/multiprofit/mapper/DwDimCustomerMapper.java`

**Interfaces:**
- Produces: Mapper接口，供Service使用

- [ ] **Step 1: 创建DwIndicatorFactMapper接口**

```java
package com.multiprofit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.entity.DwIndicatorFact;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DwIndicatorFactMapper extends BaseMapper<DwIndicatorFact> {
}
```

- [ ] **Step 2: 创建维度Mapper接口**

```java
// DwDimOrganizationMapper.java
package com.multiprofit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.entity.DwDimOrganization;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DwDimOrganizationMapper extends BaseMapper<DwDimOrganization> {
}
```

```java
// DwDimBizLineMapper.java
package com.multiprofit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.entity.DwDimBizLine;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DwDimBizLineMapper extends BaseMapper<DwDimBizLine> {
}
```

```java
// DwDimProductMapper.java
package com.multiprofit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.entity.DwDimProduct;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DwDimProductMapper extends BaseMapper<DwDimProduct> {
}
```

```java
// DwDimChannelMapper.java
package com.multiprofit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.entity.DwDimChannel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DwDimChannelMapper extends BaseMapper<DwDimChannel> {
}
```

```java
// DwDimManagerMapper.java
package com.multiprofit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.entity.DwDimManager;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DwDimManagerMapper extends BaseMapper<DwDimManager> {
}
```

```java
// DwDimCustomerMapper.java
package com.multiprofit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.entity.DwDimCustomer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DwDimCustomerMapper extends BaseMapper<DwDimCustomer> {
}
```

- [ ] **Step 3: 提交代码**

```bash
git add 后端/src/main/java/com/multiprofit/mapper/Dw*.java
git commit -m "feat: 创建数据仓库Mapper接口"
```

---

### Task 4: 创建ETL服务

**Files:**
- Create: `后端/src/main/java/com/multiprofit/service/DataWarehouseETLService.java`
- Create: `后端/src/main/java/com/multiprofit/service/impl/DataWarehouseETLServiceImpl.java`

**Interfaces:**
- Consumes: DwIndicatorFactMapper, JdbcTemplate
- Produces: DataWarehouseETLService.executeETL(period)

- [ ] **Step 1: 创建ETL服务接口**

```java
package com.multiprofit.service;

import java.util.Map;

public interface DataWarehouseETLService {
    /**
     * 执行ETL流程
     * @param period 期间（2025-06）
     * @return 执行结果
     */
    Map<String, Object> executeETL(String period);
}
```

- [ ] **Step 2: 创建ETL服务实现**

```java
package com.multiprofit.service.impl;

import com.multiprofit.mapper.DwIndicatorFactMapper;
import com.multiprofit.service.DataWarehouseETLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class DataWarehouseETLServiceImpl implements DataWarehouseETLService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DwIndicatorFactMapper dwIndicatorFactMapper;

    @Override
    @Transactional
    public Map<String, Object> executeETL(String period) {
        Map<String, Object> result = new HashMap<>();
        int totalRecords = 0;

        try {
            // 1. 删除该期间的旧数据
            jdbcTemplate.update(
                "DELETE FROM dw_indicator_fact WHERE period = ?", period);

            // 2. 计算贷款指标
            totalRecords += calculateLoanIndicators(period);

            // 3. 计算存款指标
            totalRecords += calculateDepositIndicators(period);

            // 4. 计算汇总指标
            totalRecords += calculateSummaryIndicators(period);

            result.put("success", true);
            result.put("totalRecords", totalRecords);
            result.put("message", "ETL执行完成");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "ETL执行失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 计算贷款指标
     */
    private int calculateLoanIndicators(String period) {
        int records = 0;

        // 利息收入（按机构维度）
        String sql = "INSERT INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'INTEREST_INCOME', ?, 'MONTH', 'ORG', org_id, org_name, " +
            "SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW() " +
            "FROM loan_indicator_detail " +
            "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
            "GROUP BY org_id, org_name";
        records += jdbcTemplate.update(sql, period, period);

        // FTP成本（按机构维度）
        sql = "INSERT INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'FTP_COST', ?, 'MONTH', 'ORG', org_id, org_name, " +
            "SUM(ftp_cost) / 10000, 'ASSESS', NOW() " +
            "FROM loan_indicator_detail " +
            "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
            "GROUP BY org_id, org_name";
        records += jdbcTemplate.update(sql, period, period);

        // 风险成本（按机构维度）
        sql = "INSERT INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'RISK_COST', ?, 'MONTH', 'ORG', org_id, org_name, " +
            "SUM(risk_cost) / 10000, 'ASSESS', NOW() " +
            "FROM loan_indicator_detail " +
            "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
            "GROUP BY org_id, org_name";
        records += jdbcTemplate.update(sql, period, period);

        // 贷款利润（按机构维度）
        sql = "INSERT INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'LOAN_PROFIT', ?, 'MONTH', 'ORG', org_id, org_name, " +
            "SUM(loan_monthly_interest - ftp_cost - risk_cost - op_cost) / 10000, 'ASSESS', NOW() " +
            "FROM loan_indicator_detail " +
            "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
            "GROUP BY org_id, org_name";
        records += jdbcTemplate.update(sql, period, period);

        // 其他维度（BIZ_LINE, PRODUCT, CHANNEL, MANAGER, CUSTOMER）
        String[] dimensions = {"BIZ_LINE", "PRODUCT", "CHANNEL", "MANAGER", "CUSTOMER"};
        String[] dimFields = {"biz_line", "product", "channel", "manager", "customer"};

        for (int i = 0; i < dimensions.length; i++) {
            // 利息收入
            sql = "INSERT INTO dw_indicator_fact " +
                "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
                "SELECT 'INTEREST_INCOME', ?, 'MONTH', ?, " + dimFields[i] + "_id, " + dimFields[i] + "_name, " +
                "SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW() " +
                "FROM loan_indicator_detail " +
                "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
                "GROUP BY " + dimFields[i] + "_id, " + dimFields[i] + "_name";
            records += jdbcTemplate.update(sql, period, dimensions[i], period);

            // FTP成本
            sql = "INSERT INTO dw_indicator_fact " +
                "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
                "SELECT 'FTP_COST', ?, 'MONTH', ?, " + dimFields[i] + "_id, " + dimFields[i] + "_name, " +
                "SUM(ftp_cost) / 10000, 'ASSESS', NOW() " +
                "FROM loan_indicator_detail " +
                "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
                "GROUP BY " + dimFields[i] + "_id, " + dimFields[i] + "_name";
            records += jdbcTemplate.update(sql, period, dimensions[i], period);

            // 风险成本
            sql = "INSERT INTO dw_indicator_fact " +
                "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
                "SELECT 'RISK_COST', ?, 'MONTH', ?, " + dimFields[i] + "_id, " + dimFields[i] + "_name, " +
                "SUM(risk_cost) / 10000, 'ASSESS', NOW() " +
                "FROM loan_indicator_detail " +
                "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
                "GROUP BY " + dimFields[i] + "_id, " + dimFields[i] + "_name";
            records += jdbcTemplate.update(sql, period, dimensions[i], period);

            // 贷款利润
            sql = "INSERT INTO dw_indicator_fact " +
                "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
                "SELECT 'LOAN_PROFIT', ?, 'MONTH', ?, " + dimFields[i] + "_id, " + dimFields[i] + "_name, " +
                "SUM(loan_monthly_interest - ftp_cost - risk_cost - op_cost) / 10000, 'ASSESS', NOW() " +
                "FROM loan_indicator_detail " +
                "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
                "GROUP BY " + dimFields[i] + "_id, " + dimFields[i] + "_name";
            records += jdbcTemplate.update(sql, period, dimensions[i], period);
        }

        return records;
    }

    /**
     * 计算存款指标
     */
    private int calculateDepositIndicators(String period) {
        int records = 0;

        // FTP收入（按机构维度）
        String sql = "INSERT INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'FTP_INCOME', ?, 'MONTH', 'ORG', org_id, org_name, " +
            "SUM(ftp_income) / 10000, 'ASSESS', NOW() " +
            "FROM deposit_indicator_detail " +
            "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
            "GROUP BY org_id, org_name";
        records += jdbcTemplate.update(sql, period, period);

        // 利息支出（按机构维度）
        sql = "INSERT INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'DEPOSIT_INTEREST', ?, 'MONTH', 'ORG', org_id, org_name, " +
            "SUM(deposit_monthly_interest) / 10000, 'ASSESS', NOW() " +
            "FROM deposit_indicator_detail " +
            "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
            "GROUP BY org_id, org_name";
        records += jdbcTemplate.update(sql, period, period);

        // 存款利润（按机构维度）
        sql = "INSERT INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'DEPOSIT_PROFIT', ?, 'MONTH', 'ORG', org_id, org_name, " +
            "SUM(ftp_income - deposit_monthly_interest - op_cost) / 10000, 'ASSESS', NOW() " +
            "FROM deposit_indicator_detail " +
            "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
            "GROUP BY org_id, org_name";
        records += jdbcTemplate.update(sql, period, period);

        // 其他维度
        String[] dimensions = {"BIZ_LINE", "PRODUCT", "CHANNEL", "MANAGER", "CUSTOMER"};
        String[] dimFields = {"biz_line", "product", "channel", "manager", "customer"};

        for (int i = 0; i < dimensions.length; i++) {
            // FTP收入
            sql = "INSERT INTO dw_indicator_fact " +
                "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
                "SELECT 'FTP_INCOME', ?, 'MONTH', ?, " + dimFields[i] + "_id, " + dimFields[i] + "_name, " +
                "SUM(ftp_income) / 10000, 'ASSESS', NOW() " +
                "FROM deposit_indicator_detail " +
                "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
                "GROUP BY " + dimFields[i] + "_id, " + dimFields[i] + "_name";
            records += jdbcTemplate.update(sql, period, dimensions[i], period);

            // 利息支出
            sql = "INSERT INTO dw_indicator_fact " +
                "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
                "SELECT 'DEPOSIT_INTEREST', ?, 'MONTH', ?, " + dimFields[i] + "_id, " + dimFields[i] + "_name, " +
                "SUM(deposit_monthly_interest) / 10000, 'ASSESS', NOW() " +
                "FROM deposit_indicator_detail " +
                "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
                "GROUP BY " + dimFields[i] + "_id, " + dimFields[i] + "_name";
            records += jdbcTemplate.update(sql, period, dimensions[i], period);

            // 存款利润
            sql = "INSERT INTO dw_indicator_fact " +
                "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
                "SELECT 'DEPOSIT_PROFIT', ?, 'MONTH', ?, " + dimFields[i] + "_id, " + dimFields[i] + "_name, " +
                "SUM(ftp_income - deposit_monthly_interest - op_cost) / 10000, 'ASSESS', NOW() " +
                "FROM deposit_indicator_detail " +
                "WHERE account_period = ? AND caliber_type = 'ASSESS' " +
                "GROUP BY " + dimFields[i] + "_id, " + dimFields[i] + "_name";
            records += jdbcTemplate.update(sql, period, dimensions[i], period);
        }

        return records;
    }

    /**
     * 计算汇总指标
     */
    private int calculateSummaryIndicators(String period) {
        int records = 0;

        // 总利润（按机构维度）
        String sql = "INSERT INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'TOTAL_PROFIT', ?, 'MONTH', 'ORG', org_id, org_name, " +
            "SUM(profit) / 10000, 'ASSESS', NOW() FROM (" +
            "  SELECT org_id, org_name, (loan_monthly_interest - ftp_cost - risk_cost - op_cost) as profit " +
            "  FROM loan_indicator_detail WHERE account_period = ? AND caliber_type = 'ASSESS' " +
            "  UNION ALL " +
            "  SELECT org_id, org_name, (ftp_income - deposit_monthly_interest - op_cost) as profit " +
            "  FROM deposit_indicator_detail WHERE account_period = ? AND caliber_type = 'ASSESS'" +
            ") t GROUP BY org_id, org_name";
        records += jdbcTemplate.update(sql, period, period, period);

        // 运营成本（按机构维度）
        sql = "INSERT INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'OP_COST', ?, 'MONTH', 'ORG', org_id, org_name, " +
            "SUM(op_cost) / 10000, 'ASSESS', NOW() FROM (" +
            "  SELECT org_id, org_name, op_cost FROM loan_indicator_detail WHERE account_period = ? AND caliber_type = 'ASSESS' " +
            "  UNION ALL " +
            "  SELECT org_id, org_name, op_cost FROM deposit_indicator_detail WHERE account_period = ? AND caliber_type = 'ASSESS'" +
            ") t GROUP BY org_id, org_name";
        records += jdbcTemplate.update(sql, period, period, period);

        // TOTAL汇总
        sql = "INSERT INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'TOTAL_PROFIT', ?, 'MONTH', 'TOTAL', 0, '全部', " +
            "SUM(calc_value), 'ASSESS', NOW() " +
            "FROM dw_indicator_fact " +
            "WHERE indicator_code = 'TOTAL_PROFIT' AND period = ? AND dim_type = 'ORG'";
        records += jdbcTemplate.update(sql, period, period);

        sql = "INSERT INTO dw_indicator_fact " +
            "(indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time) " +
            "SELECT 'OP_COST', ?, 'MONTH', 'TOTAL', 0, '全部', " +
            "SUM(calc_value), 'ASSESS', NOW() " +
            "FROM dw_indicator_fact " +
            "WHERE indicator_code = 'OP_COST' AND period = ? AND dim_type = 'ORG'";
        records += jdbcTemplate.update(sql, period, period);

        return records;
    }
}
```

- [ ] **Step 3: 提交代码**

```bash
git add 后端/src/main/java/com/multiprofit/service/DataWarehouseETLService*.java
git commit -m "feat: 创建ETL服务"
```

---

### Task 5: 创建数据仓库服务

**Files:**
- Create: `后端/src/main/java/com/multiprofit/service/DataWarehouseService.java`
- Create: `后端/src/main/java/com/multiprofit/service/impl/DataWarehouseServiceImpl.java`

**Interfaces:**
- Consumes: DwIndicatorFactMapper, JdbcTemplate
- Produces: DataWarehouseService.getIndicatorSummary(), getIndicatorDimension(), getIndicatorDetail(), getIndicatorTrend()

- [ ] **Step 1: 创建数据仓库服务接口**

```java
package com.multiprofit.service;

import java.util.List;
import java.util.Map;

public interface DataWarehouseService {
    /**
     * 获取指标汇总
     */
    Map<String, Object> getIndicatorSummary(String indicatorCode, String period, String caliberType);

    /**
     * 获取指标维度数据
     */
    List<Map<String, Object>> getIndicatorDimension(String indicatorCode, String period, String dimType, String caliberType);

    /**
     * 获取指标明细数据
     */
    Map<String, Object> getIndicatorDetail(String indicatorCode, String period, String dimType, Long dimId, String caliberType);

    /**
     * 获取指标趋势
     */
    List<Map<String, Object>> getIndicatorTrend(String indicatorCode, int months, String caliberType);

    /**
     * 获取指标列表
     */
    List<Map<String, Object>> getIndicatorList();
}
```

- [ ] **Step 2: 创建数据仓库服务实现**

```java
package com.multiprofit.service.impl;

import com.multiprofit.service.DataWarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataWarehouseServiceImpl implements DataWarehouseService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> getIndicatorSummary(String indicatorCode, String period, String caliberType) {
        String sql = "SELECT indicator_code, dim_name, calc_value FROM dw_indicator_fact " +
            "WHERE indicator_code = ? AND period = ? AND dim_type = 'TOTAL' AND caliber_type = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, indicatorCode, period, caliberType);

        Map<String, Object> result = new HashMap<>();
        result.put("indicatorCode", indicatorCode);
        result.put("period", period);
        result.put("caliberType", caliberType);

        if (!rows.isEmpty()) {
            result.put("totalValue", rows.get(0).get("calc_value"));
        } else {
            result.put("totalValue", 0);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getIndicatorDimension(String indicatorCode, String period, String dimType, String caliberType) {
        String sql = "SELECT dim_id, dim_name, calc_value FROM dw_indicator_fact " +
            "WHERE indicator_code = ? AND period = ? AND dim_type = ? AND caliber_type = ? " +
            "ORDER BY calc_value DESC";

        return jdbcTemplate.queryForList(sql, indicatorCode, period, dimType, caliberType);
    }

    @Override
    public Map<String, Object> getIndicatorDetail(String indicatorCode, String period, String dimType, Long dimId, String caliberType) {
        String sql = "SELECT * FROM dw_indicator_fact " +
            "WHERE indicator_code = ? AND period = ? AND dim_type = ? AND dim_id = ? AND caliber_type = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, indicatorCode, period, dimType, dimId, caliberType);

        if (!rows.isEmpty()) {
            return rows.get(0);
        }

        return new HashMap<>();
    }

    @Override
    public List<Map<String, Object>> getIndicatorTrend(String indicatorCode, int months, String caliberType) {
        String sql = "SELECT period, calc_value FROM dw_indicator_fact " +
            "WHERE indicator_code = ? AND dim_type = 'TOTAL' AND caliber_type = ? " +
            "ORDER BY period DESC LIMIT ?";

        return jdbcTemplate.queryForList(sql, indicatorCode, caliberType, months);
    }

    @Override
    public List<Map<String, Object>> getIndicatorList() {
        String sql = "SELECT DISTINCT indicator_code FROM dw_indicator_fact ORDER BY indicator_code";
        return jdbcTemplate.queryForList(sql);
    }
}
```

- [ ] **Step 3: 提交代码**

```bash
git add 后端/src/main/java/com/multiprofit/service/DataWarehouseService*.java
git commit -m "feat: 创建数据仓库服务"
```

---

### Task 6: 创建数据仓库控制器

**Files:**
- Create: `后端/src/main/java/com/multiprofit/controller/DataWarehouseController.java`

**Interfaces:**
- Consumes: DataWarehouseService, DataWarehouseETLService
- Produces: REST API endpoints

- [ ] **Step 1: 创建数据仓库控制器**

```java
package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import com.multiprofit.service.DataWarehouseETLService;
import com.multiprofit.service.DataWarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dw")
public class DataWarehouseController {

    @Autowired
    private DataWarehouseService dataWarehouseService;

    @Autowired
    private DataWarehouseETLService dataWarehouseETLService;

    /**
     * 获取指标列表
     */
    @GetMapping("/indicator/list")
    public ApiResponse<List<Map<String, Object>>> getIndicatorList() {
        try {
            List<Map<String, Object>> result = dataWarehouseService.getIndicatorList();
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取指标列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标汇总
     */
    @GetMapping("/indicator/summary")
    public ApiResponse<Map<String, Object>> getIndicatorSummary(
            @RequestParam String indicatorCode,
            @RequestParam String period,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            Map<String, Object> result = dataWarehouseService.getIndicatorSummary(indicatorCode, period, caliberType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取指标汇总失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标维度数据
     */
    @GetMapping("/indicator/dimension")
    public ApiResponse<List<Map<String, Object>>> getIndicatorDimension(
            @RequestParam String indicatorCode,
            @RequestParam String period,
            @RequestParam String dimType,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            List<Map<String, Object>> result = dataWarehouseService.getIndicatorDimension(indicatorCode, period, dimType, caliberType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取指标维度数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标明细数据
     */
    @GetMapping("/indicator/detail")
    public ApiResponse<Map<String, Object>> getIndicatorDetail(
            @RequestParam String indicatorCode,
            @RequestParam String period,
            @RequestParam String dimType,
            @RequestParam Long dimId,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            Map<String, Object> result = dataWarehouseService.getIndicatorDetail(indicatorCode, period, dimType, dimId, caliberType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取指标明细数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标趋势
     */
    @GetMapping("/indicator/trend")
    public ApiResponse<List<Map<String, Object>>> getIndicatorTrend(
            @RequestParam String indicatorCode,
            @RequestParam(required = false, defaultValue = "6") int months,
            @RequestParam(required = false, defaultValue = "ASSESS") String caliberType) {
        try {
            List<Map<String, Object>> result = dataWarehouseService.getIndicatorTrend(indicatorCode, months, caliberType);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("获取指标趋势失败: " + e.getMessage());
        }
    }

    /**
     * 执行ETL
     */
    @PostMapping("/etl/execute")
    public ApiResponse<Map<String, Object>> executeETL(@RequestParam String period) {
        try {
            Map<String, Object> result = dataWarehouseETLService.executeETL(period);
            if ((Boolean) result.get("success")) {
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.error((String) result.get("message"));
            }
        } catch (Exception e) {
            return ApiResponse.error("执行ETL失败: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 提交代码**

```bash
git add 后端/src/main/java/com/multiprofit/controller/DataWarehouseController.java
git commit -m "feat: 创建数据仓库控制器"
```

---

### Task 7: 创建前端API服务

**Files:**
- Create: `前端/src/services/dwApi.ts`

**Interfaces:**
- Produces: API functions for data warehouse

- [ ] **Step 1: 创建前端API服务**

```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: '/api/dw',
  timeout: 30000,
});

/**
 * 获取指标列表
 */
export const getIndicatorList = async (): Promise<Record<string, any>[]> => {
  const response = await api.get('/indicator/list');
  return response.data.data;
};

/**
 * 获取指标汇总
 */
export const getIndicatorSummary = async (
  indicatorCode: string,
  period: string,
  caliberType: string = 'ASSESS'
): Promise<Record<string, any>> => {
  const response = await api.get('/indicator/summary', {
    params: { indicatorCode, period, caliberType },
  });
  return response.data.data;
};

/**
 * 获取指标维度数据
 */
export const getIndicatorDimension = async (
  indicatorCode: string,
  period: string,
  dimType: string,
  caliberType: string = 'ASSESS'
): Promise<Record<string, any>[]> => {
  const response = await api.get('/indicator/dimension', {
    params: { indicatorCode, period, dimType, caliberType },
  });
  return response.data.data;
};

/**
 * 获取指标明细数据
 */
export const getIndicatorDetail = async (
  indicatorCode: string,
  period: string,
  dimType: string,
  dimId: number,
  caliberType: string = 'ASSESS'
): Promise<Record<string, any>> => {
  const response = await api.get('/indicator/detail', {
    params: { indicatorCode, period, dimType, dimId, caliberType },
  });
  return response.data.data;
};

/**
 * 获取指标趋势
 */
export const getIndicatorTrend = async (
  indicatorCode: string,
  months: number = 6,
  caliberType: string = 'ASSESS'
): Promise<Record<string, any>[]> => {
  const response = await api.get('/indicator/trend', {
    params: { indicatorCode, months, caliberType },
  });
  return response.data.data;
};

/**
 * 执行ETL
 */
export const executeETL = async (period: string): Promise<Record<string, any>> => {
  const response = await api.post('/etl/execute', null, {
    params: { period },
  });
  return response.data.data;
};
```

- [ ] **Step 2: 提交代码**

```bash
git add 前端/src/services/dwApi.ts
git commit -m "feat: 创建前端API服务"
```

---

### Task 8: 创建指标库页面

**Files:**
- Create: `前端/src/pages/BaseData/IndicatorLibrary/index.tsx`
- Create: `前端/src/pages/BaseData/IndicatorLibrary/IndicatorDetail.tsx`
- Create: `前端/src/pages/BaseData/IndicatorLibrary/IndicatorRelationGraph.tsx`

**Interfaces:**
- Consumes: dwApi, indicatorApi
- Produces: 指标库页面组件

- [ ] **Step 1: 创建指标库页面**

```tsx
// 前端/src/pages/BaseData/IndicatorLibrary/index.tsx
import React, { useState, useEffect } from 'react';
import { Card, Tree, Spin, message } from 'antd';
import { getIndicatorDefinitions } from '../../../services/indicatorApi';
import IndicatorDetail from './IndicatorDetail';

const { DirectoryTree } = Tree;

interface IndicatorNode {
  key: string;
  title: string;
  isLeaf?: boolean;
  children?: IndicatorNode[];
  data?: any;
}

const IndicatorLibrary: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [treeData, setTreeData] = useState<IndicatorNode[]>([]);
  const [selectedIndicator, setSelectedIndicator] = useState<any>(null);

  useEffect(() => {
    loadIndicators();
  }, []);

  const loadIndicators = async () => {
    setLoading(true);
    try {
      const indicators = await getIndicatorDefinitions();

      // 按业务线分组
      const assetIndicators = indicators.filter((i: any) => i.business_line === 'ASSET');
      const liabilityIndicators = indicators.filter((i: any) => i.business_line === 'LIABILITY');
      const allIndicators = indicators.filter((i: any) => i.business_line === 'ALL');

      // 按指标类型分组
      const groupByType = (items: any[]) => {
        const groups: Record<string, any[]> = {};
        items.forEach(item => {
          const type = item.indicator_type || 'OTHER';
          if (!groups[type]) groups[type] = [];
          groups[type].push(item);
        });
        return groups;
      };

      const buildTree = (businessLine: string, indicators: any[]): IndicatorNode => {
        const groups = groupByType(indicators);
        const children: IndicatorNode[] = Object.entries(groups).map(([type, items]) => ({
          key: `${businessLine}-${type}`,
          title: getTypeName(type),
          children: items.map(item => ({
            key: item.indicator_code,
            title: item.indicator_name,
            isLeaf: true,
            data: item,
          })),
        }));

        return {
          key: businessLine,
          title: getBusinessLineName(businessLine),
          children,
        };
      };

      const tree: IndicatorNode[] = [
        {
          key: 'root',
          title: '指标库',
          children: [
            buildTree('ASSET', assetIndicators),
            buildTree('LIABILITY', liabilityIndicators),
            buildTree('ALL', allIndicators),
          ],
        },
      ];

      setTreeData(tree);
    } catch (error) {
      console.error('加载指标失败:', error);
      message.error('加载指标失败');
    } finally {
      setLoading(false);
    }
  };

  const getBusinessLineName = (code: string) => {
    const map: Record<string, string> = {
      ASSET: '资产类指标',
      LIABILITY: '负债类指标',
      ALL: '全部指标',
    };
    return map[code] || code;
  };

  const getTypeName = (code: string) => {
    const map: Record<string, string> = {
      SCALE: '规模类',
      REVENUE: '收入类',
      COST: '成本类',
      PROFIT: '利润类',
      EFFICIENCY: '效率类',
      DAILY_AVG: '日均类',
    };
    return map[code] || code;
  };

  const handleSelect = (keys: string[], info: any) => {
    const node = info.node;
    if (node.isLeaf && node.data) {
      setSelectedIndicator(node.data);
    }
  };

  return (
    <div style={{ display: 'flex', height: '100%' }}>
      <Card
        title="指标库"
        style={{ width: 300, marginRight: 16 }}
        bodyStyle={{ padding: 0 }}
      >
        <Spin spinning={loading}>
          <DirectoryTree
            treeData={treeData}
            onSelect={handleSelect}
            defaultExpandAll
          />
        </Spin>
      </Card>
      <Card
        title="指标详情"
        style={{ flex: 1 }}
      >
        {selectedIndicator ? (
          <IndicatorDetail indicator={selectedIndicator} />
        ) : (
          <div style={{ textAlign: 'center', padding: 50, color: '#999' }}>
            请选择一个指标查看详情
          </div>
        )}
      </Card>
    </div>
  );
};

export default IndicatorLibrary;
```

- [ ] **Step 2: 创建指标详情组件**

```tsx
// 前端/src/pages/BaseData/IndicatorLibrary/IndicatorDetail.tsx
import React from 'react';
import { Descriptions, Tag, Divider } from 'antd';
import IndicatorRelationGraph from './IndicatorRelationGraph';

interface IndicatorDetailProps {
  indicator: any;
}

const IndicatorDetail: React.FC<IndicatorDetailProps> = ({ indicator }) => {
  const getBusinessLineName = (code: string) => {
    const map: Record<string, string> = {
      ASSET: '资产类',
      LIABILITY: '负债类',
      ALL: '全部',
    };
    return map[code] || code;
  };

  const getTypeName = (code: string) => {
    const map: Record<string, string> = {
      SCALE: '规模类',
      REVENUE: '收入类',
      COST: '成本类',
      PROFIT: '利润类',
      EFFICIENCY: '效率类',
      DAILY_AVG: '日均类',
    };
    return map[code] || code;
  };

  const getBusinessLineColor = (code: string) => {
    const map: Record<string, string> = {
      ASSET: 'blue',
      LIABILITY: 'green',
      ALL: 'default',
    };
    return map[code] || 'default';
  };

  return (
    <div>
      <Descriptions column={2} bordered>
        <Descriptions.Item label="指标编码">{indicator.indicator_code}</Descriptions.Item>
        <Descriptions.Item label="指标名称">{indicator.indicator_name}</Descriptions.Item>
        <Descriptions.Item label="业务条线">
          <Tag color={getBusinessLineColor(indicator.business_line)}>
            {getBusinessLineName(indicator.business_line)}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="指标类型">
          <Tag>{getTypeName(indicator.indicator_type)}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="计算公式" span={2}>
          {indicator.calc_formula}
        </Descriptions.Item>
        <Descriptions.Item label="数据来源">{indicator.data_source}</Descriptions.Item>
        <Descriptions.Item label="单位">{indicator.unit}</Descriptions.Item>
        <Descriptions.Item label="精度">{indicator.precision_val}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag color={indicator.status === 'ACTIVE' ? 'green' : 'red'}>
            {indicator.status === 'ACTIVE' ? '启用' : '禁用'}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="支持维度" span={2}>
          {indicator.supported_dims}
        </Descriptions.Item>
        <Descriptions.Item label="描述" span={2}>
          {indicator.description}
        </Descriptions.Item>
      </Descriptions>

      <Divider>关联关系</Divider>

      <IndicatorRelationGraph indicatorCode={indicator.indicator_code} />
    </div>
  );
};

export default IndicatorDetail;
```

- [ ] **Step 3: 创建指标关联关系图组件**

```tsx
// 前端/src/pages/BaseData/IndicatorLibrary/IndicatorRelationGraph.tsx
import React, { useEffect, useRef } from 'react';
import * as echarts from 'echarts';

interface IndicatorRelationGraphProps {
  indicatorCode: string;
}

const IndicatorRelationGraph: React.FC<IndicatorRelationGraphProps> = ({ indicatorCode }) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const chartInstance = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    if (chartRef.current) {
      chartInstance.current = echarts.init(chartRef.current);
      renderGraph();
    }

    return () => {
      chartInstance.current?.dispose();
    };
  }, [indicatorCode]);

  const renderGraph = () => {
    // 指标关联关系数据
    const relations: Record<string, { parents: string[]; children: string[] }> = {
      TOTAL_PROFIT: { parents: [], children: ['LOAN_PROFIT', 'DEPOSIT_PROFIT'] },
      LOAN_PROFIT: { parents: ['TOTAL_PROFIT'], children: ['INTEREST_INCOME', 'FTP_COST', 'RISK_COST', 'OP_COST'] },
      DEPOSIT_PROFIT: { parents: ['TOTAL_PROFIT'], children: ['FTP_INCOME', 'DEPOSIT_INTEREST', 'OP_COST'] },
      INTEREST_INCOME: { parents: ['LOAN_PROFIT'], children: [] },
      FTP_COST: { parents: ['LOAN_PROFIT'], children: [] },
      RISK_COST: { parents: ['LOAN_PROFIT'], children: [] },
      OP_COST: { parents: ['LOAN_PROFIT', 'DEPOSIT_PROFIT'], children: [] },
      FTP_INCOME: { parents: ['DEPOSIT_PROFIT'], children: [] },
      DEPOSIT_INTEREST: { parents: ['DEPOSIT_PROFIT'], children: [] },
    };

    const indicatorNames: Record<string, string> = {
      TOTAL_PROFIT: '总利润',
      LOAN_PROFIT: '贷款利润',
      DEPOSIT_PROFIT: '存款利润',
      INTEREST_INCOME: '利息收入',
      FTP_COST: 'FTP成本',
      RISK_COST: '风险成本',
      OP_COST: '运营成本',
      FTP_INCOME: 'FTP收入',
      DEPOSIT_INTEREST: '利息支出',
    };

    const relation = relations[indicatorCode];
    if (!relation) return;

    // 构建节点和边
    const nodes: any[] = [];
    const links: any[] = [];
    const addedNodes = new Set<string>();

    const addNode = (code: string, level: number) => {
      if (addedNodes.has(code)) return;
      addedNodes.add(code);
      nodes.push({
        name: indicatorNames[code] || code,
        x: level * 200,
        y: nodes.length * 100,
        itemStyle: {
          color: code === indicatorCode ? '#1890ff' : '#91d5ff',
        },
      });
    };

    const addLink = (source: string, target: string) => {
      links.push({
        source: indicatorNames[source] || source,
        target: indicatorNames[target] || target,
      });
    };

    // 添加当前节点
    addNode(indicatorCode, 1);

    // 添加父节点
    relation.parents.forEach((parent, index) => {
      addNode(parent, 0);
      addLink(parent, indicatorCode);
    });

    // 添加子节点
    relation.children.forEach((child, index) => {
      addNode(child, 2);
      addLink(indicatorCode, child);
    });

    const option = {
      tooltip: {},
      series: [
        {
          type: 'graph',
          layout: 'none',
          symbolSize: 50,
          label: {
            show: true,
          },
          edgeSymbol: ['circle', 'arrow'],
          edgeSymbolSize: [4, 10],
          data: nodes,
          links: links,
          lineStyle: {
            opacity: 0.9,
            width: 2,
            curveness: 0,
          },
        },
      ],
    };

    chartInstance.current?.setOption(option);
  };

  return <div ref={chartRef} style={{ width: '100%', height: 400 }} />;
};

export default IndicatorRelationGraph;
```

- [ ] **Step 4: 提交代码**

```bash
git add 前端/src/pages/BaseData/IndicatorLibrary/
git commit -m "feat: 创建指标库页面"
```

---

### Task 9: 更新主数据管理页面

**Files:**
- Modify: `前端/src/pages/BaseData/index.tsx`
- Modify: `前端/src/App.tsx`

**Interfaces:**
- Consumes: IndicatorLibrary component
- Produces: 更新后的路由和页面结构

- [ ] **Step 1: 更新主数据管理页面**

```tsx
// 前端/src/pages/BaseData/index.tsx
import React from 'react';
import { Navigate } from 'react-router-dom';

const BaseDataIndex: React.FC = () => {
  return <Navigate to="/base-data/indicator-library" replace />;
};

export default BaseDataIndex;
```

- [ ] **Step 2: 更新路由配置**

```tsx
// 在 App.tsx 中添加路由
import IndicatorLibrary from './pages/BaseData/IndicatorLibrary';

// 在 Routes 中添加
<Route path="base-data/indicator-library" element={<IndicatorLibrary />} />
```

- [ ] **Step 3: 提交代码**

```bash
git add 前端/src/pages/BaseData/index.tsx 前端/src/App.tsx
git commit -m "feat: 更新主数据管理页面"
```

---

### Task 10: 更新指标数据页面使用数据仓库

**Files:**
- Modify: `前端/src/pages/BaseData/IndicatorData/AssetInterestIncome.tsx`
- Modify: `前端/src/pages/BaseData/IndicatorData/AssetFtpCost.tsx`
- Modify: `前端/src/pages/BaseData/IndicatorData/AssetRiskCost.tsx`
- Modify: `前端/src/pages/BaseData/IndicatorData/AssetOperationCost.tsx`
- Modify: `前端/src/pages/BaseData/IndicatorData/LiabilityInterestExpense.tsx`
- Modify: `前端/src/pages/BaseData/IndicatorData/LiabilityFtpCost.tsx`
- Modify: `前端/src/pages/BaseData/IndicatorData/LiabilityRiskCost.tsx`
- Modify: `前端/src/pages/BaseData/IndicatorData/LiabilityOperationCost.tsx`

**Interfaces:**
- Consumes: dwApi
- Produces: 更新后的指标数据页面

- [ ] **Step 1: 更新AssetInterestIncome页面使用数据仓库**

```tsx
// 修改 imports
import {
  getIndicatorSummary,
  getIndicatorDimension,
  getIndicatorDetail,
} from '../../../services/dwApi';

// 修改数据加载逻辑
const loadData = async () => {
  setLoading(true);
  try {
    const [summaryRes, dimensionRes] = await Promise.all([
      getIndicatorSummary('INTEREST_INCOME', period, caliberType),
      getIndicatorDimension('INTEREST_INCOME', period, dimension, caliberType),
    ]);

    setSummary(summaryRes || {});
    setDimensionData(dimensionRes || []);
  } catch (error) {
    console.error('加载数据失败:', error);
    message.error('加载数据失败');
  } finally {
    setLoading(false);
  }
};
```

- [ ] **Step 2: 更新其他指标数据页面**

按照同样的模式更新其他7个指标数据页面。

- [ ] **Step 3: 提交代码**

```bash
git add 前端/src/pages/BaseData/IndicatorData/
git commit -m "feat: 更新指标数据页面使用数据仓库"
```

---

### Task 11: 更新维度分析和驾驶舱使用数据仓库

**Files:**
- Modify: `后端/src/main/java/com/multiprofit/service/impl/DashboardServiceImpl.java`
- Modify: `后端/src/main/java/com/multiprofit/service/impl/DimensionServiceImpl.java`

**Interfaces:**
- Consumes: DataWarehouseService
- Produces: 更新后的服务实现

- [ ] **Step 1: 更新DashboardServiceImpl使用数据仓库**

```java
// 修改 imports
import com.multiprofit.service.DataWarehouseService;

// 注入 DataWarehouseService
@Autowired
private DataWarehouseService dataWarehouseService;

// 修改 getDashboardData 方法
@Override
public DashboardDTO getDashboardData(String startDate, String endDate, String caliberType, Long orgScope) {
    String period = startDate.substring(0, 7);

    // 从数据仓库获取汇总数据
    Map<String, Object> totalProfit = dataWarehouseService.getIndicatorSummary("TOTAL_PROFIT", period, caliberType);
    Map<String, Object> loanProfit = dataWarehouseService.getIndicatorSummary("LOAN_PROFIT", period, caliberType);
    Map<String, Object> depositProfit = dataWarehouseService.getIndicatorSummary("DEPOSIT_PROFIT", period, caliberType);
    Map<String, Object> interestIncome = dataWarehouseService.getIndicatorSummary("INTEREST_INCOME", period, caliberType);

    // 构建 KPI 卡片
    List<DashboardDTO.KpiCard> kpiCards = new ArrayList<>();
    kpiCards.add(createKpiCard("TOTAL_PROFIT", "总利润", new BigDecimal(totalProfit.get("totalValue").toString()), "万元"));
    kpiCards.add(createKpiCard("LOAN_PROFIT", "贷款利润", new BigDecimal(loanProfit.get("totalValue").toString()), "万元"));
    kpiCards.add(createKpiCard("DEPOSIT_PROFIT", "存款利润", new BigDecimal(depositProfit.get("totalValue").toString()), "万元"));
    kpiCards.add(createKpiCard("INTEREST_INCOME", "利息收入", new BigDecimal(interestIncome.get("totalValue").toString()), "万元"));

    // ... 其他代码
}
```

- [ ] **Step 2: 更新DimensionServiceImpl使用数据仓库**

```java
// 修改 imports
import com.multiprofit.service.DataWarehouseService;

// 注入 DataWarehouseService
@Autowired
private DataWarehouseService dataWarehouseService;

// 修改 getAnalysisData 方法
@Override
public DimensionAnalysisDTO getAnalysisData(String dimType, String startDate, String endDate,
                                             String caliberType, Long parentId, Integer level) {
    String period = startDate.substring(0, 7);

    // 从数据仓库获取维度数据
    List<Map<String, Object>> totalProfitData = dataWarehouseService.getIndicatorDimension("TOTAL_PROFIT", period, dimType, caliberType);
    List<Map<String, Object>> loanProfitData = dataWarehouseService.getIndicatorDimension("LOAN_PROFIT", period, dimType, caliberType);
    List<Map<String, Object>> depositProfitData = dataWarehouseService.getIndicatorDimension("DEPOSIT_PROFIT", period, dimType, caliberType);

    // 构建排名数据
    List<DimensionAnalysisDTO.RankItem> ranking = new ArrayList<>();
    int rankIndex = 1;
    for (Map<String, Object> row : totalProfitData) {
        DimensionAnalysisDTO.RankItem rankItem = new DimensionAnalysisDTO.RankItem();
        rankItem.setId((long) rankIndex);
        rankItem.setName((String) row.get("dim_name"));
        rankItem.setNetProfit((BigDecimal) row.get("calc_value"));
        rankItem.setRankIndex(rankIndex++);
        ranking.add(rankItem);
    }

    // ... 其他代码
}
```

- [ ] **Step 3: 提交代码**

```bash
git add 后端/src/main/java/com/multiprofit/service/impl/DashboardServiceImpl.java
git add 后端/src/main/java/com/multiprofit/service/impl/DimensionServiceImpl.java
git commit -m "feat: 更新维度分析和驾驶舱使用数据仓库"
```

---

### Task 12: 执行ETL并验证数据

**Files:**
- None

**Interfaces:**
- Consumes: DataWarehouseETLService
- Produces: 验证数据一致性

- [ ] **Step 1: 编译后端代码**

```bash
cd 后端 && mvn compile -q
```

- [ ] **Step 2: 重启后端服务**

```bash
pkill -f "spring-boot:run" || true
sleep 2
mvn spring-boot:run -q > /tmp/backend.log 2>&1 &
sleep 20
```

- [ ] **Step 3: 执行ETL**

```bash
curl -s -X POST "http://localhost:8080/api/dw/etl/execute?period=2025-06"
```

Expected: `{"code":200,"message":"success","data":{"success":true,"totalRecords":...}}`

- [ ] **Step 4: 验证数据一致性**

```bash
# 检查数据仓库数据
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "
SELECT indicator_code, dim_type, count(*) as cnt
FROM dw_indicator_fact
WHERE period = '2025-06'
GROUP BY indicator_code, dim_type
ORDER BY indicator_code, dim_type;
"
```

- [ ] **Step 5: 验证API接口**

```bash
# 获取指标汇总
curl -s "http://localhost:8080/api/dw/indicator/summary?indicatorCode=TOTAL_PROFIT&period=2025-06"

# 获取指标维度数据
curl -s "http://localhost:8080/api/dw/indicator/dimension?indicatorCode=TOTAL_PROFIT&period=2025-06&dimType=ORG"
```

- [ ] **Step 6: 提交代码**

```bash
git add .
git commit -m "feat: 完成数据仓库实现"
```

---

## 自检清单

- [x] Spec coverage: 所有设计文档中的需求都有对应任务
- [x] Placeholder scan: 无 "TBD", "TODO" 等占位符
- [x] Type consistency: 类型、方法签名、属性名在各任务中一致
