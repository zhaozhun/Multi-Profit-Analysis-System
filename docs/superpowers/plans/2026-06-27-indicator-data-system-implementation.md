# 指标数据系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现指标数据系统，支持不同颗粒度的费用数据展示，优化左侧菜单结构

**Architecture:** 采用预计算汇总表方案，原始数据不动，加工后的表和原始表关联。指标数据页面支持二级卡片导航，每个费用类型有独立详情页面。

**Tech Stack:** Spring Boot 3.2 + MyBatis-Plus + MySQL 8.0 + React 18 + Ant Design 5

## Global Constraints

- 原始数据不可变，加工数据可追溯
- 使用中文命名目录和文件
- 遵循现有代码风格和架构
- 每个任务完成后提交代码

---

## 文件结构

### 后端文件
- `后端/src/main/java/com/multiprofit/entity/IndicatorSummary.java` - 指标汇总实体
- `后端/src/main/java/com/multiprofit/entity/CostAllocationResult.java` - 费用分摊结果实体
- `后端/src/main/java/com/multiprofit/mapper/IndicatorSummaryMapper.java` - 指标汇总Mapper
- `后端/src/main/java/com/multiprofit/mapper/CostAllocationResultMapper.java` - 费用分摊结果Mapper
- `后端/src/main/java/com/multiprofit/service/IndicatorSummaryService.java` - 指标汇总服务接口
- `后端/src/main/java/com/multiprofit/service/impl/IndicatorSummaryServiceImpl.java` - 指标汇总服务实现
- `后端/src/main/java/com/multiprofit/controller/IndicatorController.java` - 修改现有控制器

### 前端文件
- `前端/src/pages/BaseData/IndicatorData/index.tsx` - 修改指标数据页面
- `前端/src/pages/BaseData/IndicatorData/components/ExpenseCard.tsx` - 费用卡片组件
- `前端/src/pages/BaseData/IndicatorData/ExpenseDetail.tsx` - 费用详情页面
- `前端/src/components/MainLayout.tsx` - 修改左侧菜单
- `前端/src/App.tsx` - 修改路由配置

### 数据库脚本
- `数据库脚本/indicator_summary_table.sql` - 指标汇总表建表脚本
- `数据库脚本/cost_allocation_result_table.sql` - 费用分摊结果表建表脚本

---

## Task 1: 创建数据库表结构

**Files:**
- Create: `数据库脚本/indicator_summary_table.sql`
- Create: `数据库脚本/cost_allocation_result_table.sql`

**Interfaces:**
- Produces: indicator_summary 和 cost_allocation_result 表结构

- [ ] **Step 1: 创建指标汇总表脚本**

```sql
-- 指标汇总表
CREATE TABLE IF NOT EXISTS indicator_summary (
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

- [ ] **Step 2: 创建费用分摊结果表脚本**

```sql
-- 费用分摊结果表
CREATE TABLE IF NOT EXISTS cost_allocation_result (
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

- [ ] **Step 3: 执行建表脚本**

```bash
mysql -u root -p multi_profit < 数据库脚本/indicator_summary_table.sql
mysql -u root -p multi_profit < 数据库脚本/cost_allocation_result_table.sql
```

- [ ] **Step 4: 验证表创建成功**

```bash
mysql -u root -p multi_profit -e "SHOW TABLES LIKE 'indicator_summary';"
mysql -u root -p multi_profit -e "SHOW TABLES LIKE 'cost_allocation_result';"
```

- [ ] **Step 5: 提交代码**

```bash
git add 数据库脚本/indicator_summary_table.sql 数据库脚本/cost_allocation_result_table.sql
git commit -m "feat: 创建指标汇总表和费用分摊结果表"
```

---

## Task 2: 创建后端实体类

**Files:**
- Create: `后端/src/main/java/com/multiprofit/entity/IndicatorSummary.java`
- Create: `后端/src/main/java/com/multiprofit/entity/CostAllocationResult.java`

**Interfaces:**
- Produces: 实体类，用于MyBatis-Plus映射

- [ ] **Step 1: 创建指标汇总实体类**

```java
package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("indicator_summary")
public class IndicatorSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String period;
    private String indicatorCode;
    private String indicatorType;
    private String businessLine;
    private BigDecimal calcValue;
    private LocalDateTime calcTime;
    private Integer status;
}
```

- [ ] **Step 2: 创建费用分摊结果实体类**

```java
package com.multiprofit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cost_allocation_result")
public class CostAllocationResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String period;
    private String costType;
    private String sourceDimType;
    private String sourceDimCode;
    private String targetAccountId;
    private BigDecimal allocatedAmount;
    private Long allocationRuleId;
    private LocalDateTime createTime;
}
```

- [ ] **Step 3: 提交代码**

```bash
git add 后端/src/main/java/com/multiprofit/entity/IndicatorSummary.java 后端/src/main/java/com/multiprofit/entity/CostAllocationResult.java
git commit -m "feat: 创建指标汇总和费用分摊结果实体类"
```

---

## Task 3: 创建后端Mapper接口

**Files:**
- Create: `后端/src/main/java/com/multiprofit/mapper/IndicatorSummaryMapper.java`
- Create: `后端/src/main/java/com/multiprofit/mapper/CostAllocationResultMapper.java`

**Interfaces:**
- Produces: Mapper接口，用于数据库操作

- [ ] **Step 1: 创建指标汇总Mapper接口**

```java
package com.multiprofit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.entity.IndicatorSummary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IndicatorSummaryMapper extends BaseMapper<IndicatorSummary> {
}
```

- [ ] **Step 2: 创建费用分摊结果Mapper接口**

```java
package com.multiprofit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.multiprofit.entity.CostAllocationResult;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CostAllocationResultMapper extends BaseMapper<CostAllocationResult> {
}
```

- [ ] **Step 3: 提交代码**

```bash
git add 后端/src/main/java/com/multiprofit/mapper/IndicatorSummaryMapper.java 后端/src/main/java/com/multiprofit/mapper/CostAllocationResultMapper.java
git commit -m "feat: 创建指标汇总和费用分摊结果Mapper接口"
```

---

## Task 4: 创建后端服务接口和实现

**Files:**
- Create: `后端/src/main/java/com/multiprofit/service/IndicatorSummaryService.java`
- Create: `后端/src/main/java/com/multiprofit/service/impl/IndicatorSummaryServiceImpl.java`

**Interfaces:**
- Produces: 服务接口和实现，提供指标汇总和费用分摊结果查询功能

- [ ] **Step 1: 创建指标汇总服务接口**

```java
package com.multiprofit.service;

import java.util.List;
import java.util.Map;

public interface IndicatorSummaryService {
    /**
     * 获取指标汇总数据
     * @param businessLine 业务条线：ASSET/LIABILITY
     * @param period 账期月份
     * @param statType 统计类型：MONTHLY_DAILY_AVG/YEARLY_DAILY_AVG
     * @return 指标汇总数据列表
     */
    List<Map<String, Object>> getIndicatorSummary(String businessLine, String period, String statType);

    /**
     * 获取费用类型列表
     * @param businessLine 业务条线：ASSET/LIABILITY
     * @return 费用类型列表
     */
    List<Map<String, Object>> getCostTypes(String businessLine);

    /**
     * 获取费用分摊结果
     * @param costType 费用类型
     * @param period 账期月份
     * @param page 页码
     * @param size 每页大小
     * @return 费用分摊结果
     */
    Map<String, Object> getCostAllocationResult(String costType, String period, int page, int size);

    /**
     * 获取费用原始数据
     * @param costType 费用类型
     * @param period 账期月份
     * @param dimType 维度类型
     * @return 费用原始数据
     */
    Map<String, Object> getCostOriginalData(String costType, String period, String dimType);

    /**
     * 触发指标预计算
     * @param period 账期月份
     * @param indicatorCode 指标编码（可选）
     * @return 计算结果
     */
    Map<String, Object> calculateIndicators(String period, String indicatorCode);
}
```

- [ ] **Step 2: 创建指标汇总服务实现**

```java
package com.multiprofit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.multiprofit.entity.AtomicIndicator;
import com.multiprofit.entity.IndicatorSummary;
import com.multiprofit.entity.CostAllocationResult;
import com.multiprofit.mapper.AtomicIndicatorMapper;
import com.multiprofit.mapper.IndicatorSummaryMapper;
import com.multiprofit.mapper.CostAllocationResultMapper;
import com.multiprofit.service.IndicatorSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorSummaryServiceImpl implements IndicatorSummaryService {

    private final IndicatorSummaryMapper indicatorSummaryMapper;
    private final CostAllocationResultMapper costAllocationResultMapper;
    private final AtomicIndicatorMapper atomicIndicatorMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> getIndicatorSummary(String businessLine, String period, String statType) {
        QueryWrapper<IndicatorSummary> wrapper = new QueryWrapper<>();
        wrapper.eq("business_line", businessLine)
               .eq("period", period)
               .eq("status", 1);
        
        List<IndicatorSummary> summaries = indicatorSummaryMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (IndicatorSummary summary : summaries) {
            Map<String, Object> item = new HashMap<>();
            AtomicIndicator indicator = atomicIndicatorMapper.selectById(summary.getIndicatorCode());
            item.put("code", summary.getIndicatorCode());
            item.put("name", indicator != null ? indicator.getName() : summary.getIndicatorCode());
            item.put("value", summary.getCalcValue());
            item.put("unit", indicator != null ? indicator.getUnit() : "万元");
            item.put("period", summary.getPeriod());
            result.add(item);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getCostTypes(String businessLine) {
        // 从atomic_indicator表获取费用类型
        QueryWrapper<AtomicIndicator> wrapper = new QueryWrapper<>();
        wrapper.eq("business_line", businessLine)
               .like("name", "成本")
               .eq("status", 1);
        
        List<AtomicIndicator> indicators = atomicIndicatorMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (AtomicIndicator indicator : indicators) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", indicator.getCode());
            item.put("name", indicator.getName());
            item.put("businessLine", indicator.getBusinessLine());
            result.add(item);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getCostAllocationResult(String costType, String period, int page, int size) {
        QueryWrapper<CostAllocationResult> wrapper = new QueryWrapper<>();
        wrapper.eq("cost_type", costType)
               .eq("period", period)
               .orderByDesc("allocated_amount");
        
        List<CostAllocationResult> results = costAllocationResultMapper.selectList(wrapper);
        
        // 分页处理
        int start = (page - 1) * size;
        int end = Math.min(start + size, results.size());
        List<CostAllocationResult> pageResults = results.subList(start, end);
        
        // 计算总金额
        BigDecimal totalAmount = results.stream()
                .map(CostAllocationResult::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> result = new HashMap<>();
        result.put("costType", costType);
        result.put("totalAmount", totalAmount);
        result.put("page", page);
        result.put("size", size);
        result.put("total", results.size());
        result.put("details", pageResults);
        
        return result;
    }

    @Override
    public Map<String, Object> getCostOriginalData(String costType, String period, String dimType) {
        // 查询原始费用数据
        String sql = String.format("""
            SELECT 
                source_dim_code as dimCode,
                source_dim_name as dimName,
                SUM(amount) as amount
            FROM cost_actual_record
            WHERE cost_type = '%s' 
              AND period = '%s'
              AND source_dim_type = '%s'
            GROUP BY source_dim_code, source_dim_name
            ORDER BY amount DESC
            """, costType, period, dimType);
        
        List<Map<String, Object>> details = jdbcTemplate.queryForList(sql);
        
        Map<String, Object> result = new HashMap<>();
        result.put("costType", costType);
        result.put("dimType", dimType);
        result.put("details", details);
        
        return result;
    }

    @Override
    public Map<String, Object> calculateIndicators(String period, String indicatorCode) {
        try {
            List<AtomicIndicator> indicators;
            if (indicatorCode != null && !indicatorCode.isEmpty()) {
                AtomicIndicator indicator = atomicIndicatorMapper.selectById(indicatorCode);
                indicators = indicator != null ? List.of(indicator) : Collections.emptyList();
            } else {
                indicators = atomicIndicatorMapper.selectList(null);
            }
            
            int calculatedCount = 0;
            for (AtomicIndicator indicator : indicators) {
                // 计算指标汇总值
                BigDecimal value = calculateIndicatorValue(indicator, period);
                
                // 保存到indicator_summary表
                IndicatorSummary summary = new IndicatorSummary();
                summary.setPeriod(period);
                summary.setIndicatorCode(indicator.getCode());
                summary.setIndicatorType("ATOMIC");
                summary.setBusinessLine(indicator.getBusinessLine());
                summary.setCalcValue(value);
                summary.setCalcTime(LocalDateTime.now());
                summary.setStatus(1);
                
                indicatorSummaryMapper.insert(summary);
                calculatedCount++;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "计算完成");
            result.put("calculatedCount", calculatedCount);
            return result;
            
        } catch (Exception e) {
            log.error("指标预计算失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return result;
        }
    }

    private BigDecimal calculateIndicatorValue(AtomicIndicator indicator, String period) {
        // 根据指标配置计算汇总值
        String sql = String.format("""
            SELECT SUM(%s) as value
            FROM %s
            WHERE DATE_FORMAT(stat_date, '%%Y-%%m') = '%s'
            """, indicator.getSourceField(), indicator.getSourceTable(), period);
        
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(sql);
            Object value = row.get("value");
            return value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("计算指标{}失败: {}", indicator.getCode(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
```

- [ ] **Step 3: 提交代码**

```bash
git add 后端/src/main/java/com/multiprofit/service/IndicatorSummaryService.java 后端/src/main/java/com/multiprofit/service/impl/IndicatorSummaryServiceImpl.java
git commit -m "feat: 创建指标汇总服务接口和实现"
```

---

## Task 5: 修改后端控制器

**Files:**
- Modify: `后端/src/main/java/com/multiprofit/controller/IndicatorController.java`

**Interfaces:**
- Consumes: IndicatorSummaryService
- Produces: 新增API端点

- [ ] **Step 1: 添加新的API端点**

在现有的IndicatorController中添加以下方法：

```java
@Autowired
private IndicatorSummaryService indicatorSummaryService;

@GetMapping("/summary")
public Result<List<Map<String, Object>>> getIndicatorSummary(
        @RequestParam String businessLine,
        @RequestParam String period,
        @RequestParam(defaultValue = "MONTHLY_DAILY_AVG") String statType) {
    try {
        List<Map<String, Object>> result = indicatorSummaryService.getIndicatorSummary(businessLine, period, statType);
        return Result.success(result);
    } catch (Exception e) {
        log.error("获取指标汇总数据失败", e);
        return Result.error("获取指标汇总数据失败: " + e.getMessage());
    }
}

@GetMapping("/cost-types")
public Result<List<Map<String, Object>>> getCostTypes(
        @RequestParam String businessLine) {
    try {
        List<Map<String, Object>> result = indicatorSummaryService.getCostTypes(businessLine);
        return Result.success(result);
    } catch (Exception e) {
        log.error("获取费用类型列表失败", e);
        return Result.error("获取费用类型列表失败: " + e.getMessage());
    }
}

@GetMapping("/cost-allocation/{costType}")
public Result<Map<String, Object>> getCostAllocationResult(
        @PathVariable String costType,
        @RequestParam String period,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {
    try {
        Map<String, Object> result = indicatorSummaryService.getCostAllocationResult(costType, period, page, size);
        return Result.success(result);
    } catch (Exception e) {
        log.error("获取费用分摊结果失败", e);
        return Result.error("获取费用分摊结果失败: " + e.getMessage());
    }
}

@GetMapping("/cost-original/{costType}")
public Result<Map<String, Object>> getCostOriginalData(
        @PathVariable String costType,
        @RequestParam String period,
        @RequestParam String dimType) {
    try {
        Map<String, Object> result = indicatorSummaryService.getCostOriginalData(costType, period, dimType);
        return Result.success(result);
    } catch (Exception e) {
        log.error("获取费用原始数据失败", e);
        return Result.error("获取费用原始数据失败: " + e.getMessage());
    }
}

@PostMapping("/calculate")
public Result<Map<String, Object>> calculateIndicators(
        @RequestParam String period,
        @RequestParam(required = false) String indicatorCode) {
    try {
        Map<String, Object> result = indicatorSummaryService.calculateIndicators(period, indicatorCode);
        return Result.success(result);
    } catch (Exception e) {
        log.error("触发指标预计算失败", e);
        return Result.error("触发指标预计算失败: " + e.getMessage());
    }
}
```

- [ ] **Step 2: 添加必要的import**

```java
import com.multiprofit.service.IndicatorSummaryService;
```

- [ ] **Step 3: 提交代码**

```bash
git add 后端/src/main/java/com/multiprofit/controller/IndicatorController.java
git commit -m "feat: 添加指标汇总和费用分摊API端点"
```

---

## Task 6: 创建前端API服务

**Files:**
- Modify: `前端/src/services/indicatorApi.ts`

**Interfaces:**
- Produces: 前端API服务函数

- [ ] **Step 1: 添加新的API函数**

在现有的indicatorApi.ts中添加以下函数：

```typescript
// 获取指标汇总数据
export const getIndicatorSummary = async (businessLine: string, period: string, statType: string = 'MONTHLY_DAILY_AVG') => {
  const response = await api.get('/indicator/summary', {
    params: { businessLine, period, statType }
  });
  return response.data;
};

// 获取费用类型列表
export const getCostTypes = async (businessLine: string) => {
  const response = await api.get('/indicator/cost-types', {
    params: { businessLine }
  });
  return response.data;
};

// 获取费用分摊结果
export const getCostAllocationResult = async (costType: string, period: string, page: number = 1, size: number = 20) => {
  const response = await api.get(`/indicator/cost-allocation/${costType}`, {
    params: { period, page, size }
  });
  return response.data;
};

// 获取费用原始数据
export const getCostOriginalData = async (costType: string, period: string, dimType: string) => {
  const response = await api.get(`/indicator/cost-original/${costType}`, {
    params: { period, dimType }
  });
  return response.data;
};

// 触发指标预计算
export const calculateIndicators = async (period: string, indicatorCode?: string) => {
  const response = await api.post('/indicator/calculate', null, {
    params: { period, indicatorCode }
  });
  return response.data;
};
```

- [ ] **Step 2: 提交代码**

```bash
git add 前端/src/services/indicatorApi.ts
git commit -m "feat: 添加指标汇总和费用分摊API服务函数"
```

---

## Task 7: 修改指标数据页面

**Files:**
- Modify: `前端/src/pages/BaseData/IndicatorData/index.tsx`

**Interfaces:**
- Consumes: getIndicatorSummary, getCostTypes
- Produces: 指标数据页面，支持二级卡片导航

- [ ] **Step 1: 修改指标数据页面**

```tsx
import React, { useState, useEffect } from 'react';
import { Tabs, Card, Row, Col, Spin, Empty } from 'antd';
import { getIndicatorSummary, getCostTypes } from '../../../services/indicatorApi';

const IndicatorDataPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('ASSET');
  const [indicators, setIndicators] = useState<any[]>([]);
  const [costTypes, setCostTypes] = useState<any[]>([]);
  const [selectedIndicator, setSelectedIndicator] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadIndicators();
  }, [activeTab]);

  const loadIndicators = async () => {
    setLoading(true);
    try {
      const data = await getIndicatorSummary(activeTab, '2026-01');
      setIndicators(data);
    } catch (error) {
      console.error('加载指标数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleIndicatorClick = async (code: string) => {
    setSelectedIndicator(code);
    if (code.includes('OP')) {
      // 如果是运营成本，加载费用类型
      const types = await getCostTypes(activeTab);
      setCostTypes(types);
    } else {
      setCostTypes([]);
    }
  };

  return (
    <div>
      <h2>指标数据</h2>
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <Tabs.TabPane tab="资产" key="ASSET" />
        <Tabs.TabPane tab="负债" key="LIABILITY" />
      </Tabs>
      
      <Spin spinning={loading}>
        <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
          {indicators.map((indicator) => (
            <Col span={6} key={indicator.code}>
              <Card
                hoverable
                onClick={() => handleIndicatorClick(indicator.code)}
                style={{ 
                  borderColor: selectedIndicator === indicator.code ? '#1890ff' : undefined 
                }}
              >
                <Card.Meta
                  title={indicator.name}
                  description={
                    <div>
                      <div>月日均: {indicator.value} {indicator.unit}</div>
                    </div>
                  }
                />
              </Card>
            </Col>
          ))}
        </Row>
        
        {selectedIndicator && selectedIndicator.includes('OP') && costTypes.length > 0 && (
          <div style={{ marginTop: 24 }}>
            <h3>费用类型</h3>
            <Row gutter={[16, 16]}>
              {costTypes.map((type) => (
                <Col span={6} key={type.code}>
                  <Card hoverable>
                    <Card.Meta
                      title={type.name}
                      description={`业务条线: ${type.businessLine === 'ASSET' ? '资产' : '负债'}`}
                    />
                  </Card>
                </Col>
              ))}
            </Row>
          </div>
        )}
        
        {selectedIndicator && !selectedIndicator.includes('OP') && (
          <div style={{ marginTop: 24 }}>
            <Card>
              <p>指标详情区域</p>
            </Card>
          </div>
        )}
        
        {!selectedIndicator && (
          <div style={{ marginTop: 24 }}>
            <Empty description="请选择指标查看详情" />
          </div>
        )}
      </Spin>
    </div>
  );
};

export default IndicatorDataPage;
```

- [ ] **Step 2: 提交代码**

```bash
git add 前端/src/pages/BaseData/IndicatorData/index.tsx
git commit -m "feat: 修改指标数据页面支持二级卡片导航"
```

---

## Task 8: 创建费用详情页面

**Files:**
- Create: `前端/src/pages/BaseData/IndicatorData/ExpenseDetail.tsx`

**Interfaces:**
- Consumes: getCostAllocationResult, getCostOriginalData
- Produces: 费用详情页面

- [ ] **Step 1: 创建费用详情页面**

```tsx
import React, { useState, useEffect } from 'react';
import { Card, Table, Collapse, Spin, Button, Descriptions } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { getCostAllocationResult, getCostOriginalData } from '../../../services/indicatorApi';

const { Panel } = Collapse;

const ExpenseDetailPage: React.FC = () => {
  const { costType } = useParams<{ costType: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [allocationData, setAllocationData] = useState<any>(null);
  const [originalData, setOriginalData] = useState<any>(null);

  useEffect(() => {
    if (costType) {
      loadData();
    }
  }, [costType]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [allocation, original] = await Promise.all([
        getCostAllocationResult(costType!, '2026-01'),
        getCostOriginalData(costType!, '2026-01', 'DEPT')
      ]);
      setAllocationData(allocation);
      setOriginalData(original);
    } catch (error) {
      console.error('加载费用数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const allocationColumns = [
    { title: '账户ID', dataIndex: 'targetAccountId', key: 'targetAccountId' },
    { title: '账户名称', dataIndex: 'accountName', key: 'accountName' },
    { title: '分摊金额', dataIndex: 'allocatedAmount', key: 'allocatedAmount' },
    { title: '分摊规则', dataIndex: 'allocationRule', key: 'allocationRule' },
  ];

  const originalColumns = [
    { title: '维度编码', dataIndex: 'dimCode', key: 'dimCode' },
    { title: '维度名称', dataIndex: 'dimName', key: 'dimName' },
    { title: '金额', dataIndex: 'amount', key: 'amount' },
  ];

  return (
    <div>
      <Button 
        icon={<ArrowLeftOutlined />} 
        onClick={() => navigate(-1)}
        style={{ marginBottom: 16 }}
      >
        返回
      </Button>
      
      <h2>费用详情 - {costType}</h2>
      
      <Spin spinning={loading}>
        {allocationData && (
          <Card title="汇总数据" style={{ marginBottom: 16 }}>
            <Descriptions>
              <Descriptions.Item label="费用类型">{allocationData.costType}</Descriptions.Item>
              <Descriptions.Item label="总金额">{allocationData.totalAmount} 万元</Descriptions.Item>
              <Descriptions.Item label="记录数">{allocationData.total}</Descriptions.Item>
            </Descriptions>
          </Card>
        )}
        
        {allocationData && (
          <Card title="账户级明细" style={{ marginBottom: 16 }}>
            <Table
              columns={allocationColumns}
              dataSource={allocationData.details}
              rowKey="id"
              pagination={{ pageSize: 10 }}
            />
          </Card>
        )}
        
        {originalData && (
          <Card title="原始数据">
            <Collapse>
              <Panel header="按部门汇总" key="dept">
                <Table
                  columns={originalColumns}
                  dataSource={originalData.details}
                  rowKey="dimCode"
                  pagination={false}
                />
              </Panel>
            </Collapse>
          </Card>
        )}
      </Spin>
    </div>
  );
};

export default ExpenseDetailPage;
```

- [ ] **Step 2: 提交代码**

```bash
git add 前端/src/pages/BaseData/IndicatorData/ExpenseDetail.tsx
git commit -m "feat: 创建费用详情页面"
```

---

## Task 9: 修改路由配置

**Files:**
- Modify: `前端/src/App.tsx`

**Interfaces:**
- Produces: 添加费用详情页面路由

- [ ] **Step 1: 添加路由配置**

在App.tsx中添加以下路由：

```tsx
import ExpenseDetailPage from './pages/BaseData/IndicatorData/ExpenseDetail';

// 在路由配置中添加
<Route path="/indicator-data/expense/:costType" element={<ExpenseDetailPage />} />
```

- [ ] **Step 2: 提交代码**

```bash
git add 前端/src/App.tsx
git commit -m "feat: 添加费用详情页面路由"
```

---

## Task 10: 优化左侧菜单结构

**Files:**
- Modify: `前端/src/components/MainLayout.tsx`

**Interfaces:**
- Produces: 优化后的左侧菜单结构

- [ ] **Step 1: 修改左侧菜单**

```tsx
const menuItems = [
  {
    key: '/dashboard',
    icon: <DashboardOutlined />,
    label: '驾驶舱',
  },
  {
    key: 'analysis',
    icon: <BarChartOutlined />,
    label: '维度分析',
    children: [
      { key: '/analysis/ORG', label: '机构分析' },
      { key: '/analysis/BIZ_LINE', label: '条线分析' },
      { key: '/analysis/PRODUCT', label: '产品分析' },
      { key: '/analysis/CHANNEL', label: '渠道分析' },
      { key: '/analysis/DEPT', label: '部门分析' },
      { key: '/analysis/MANAGER', label: '客户经理分析' },
    ],
  },
  {
    key: '/indicator-data',
    icon: <LineChartOutlined />,
    label: '指标数据',
  },
  {
    key: 'allocation',
    icon: <AccountBookOutlined />,
    label: '费用分摊',
    children: [
      {
        key: 'allocation-config',
        label: '分摊配置',
        children: [
          { key: '/allocation/cost-type', label: '费用类型' },
          { key: '/allocation/factor', label: '分摊因子' },
          { key: '/allocation/rule', label: '分摊规则' },
        ],
      },
      {
        key: 'allocation-manage',
        label: '费用管理',
        children: [
          { key: '/allocation/cost-record', label: '费用记录' },
          { key: '/allocation/employee', label: '员工费用分摊' },
          { key: '/allocation/product-commission', label: '产品分润' },
          { key: '/allocation/operation-cost', label: '运营费用分摊' },
        ],
      },
      {
        key: 'allocation-result',
        label: '分摊结果',
        children: [
          { key: '/allocation/execution', label: '分摊执行' },
          { key: '/allocation/result', label: '结果查询' },
          { key: '/allocation/statistics', label: '统计分析' },
        ],
      },
    ],
  },
  {
    key: 'base-data',
    icon: <DatabaseOutlined />,
    label: '主数据管理',
    children: [
      {
        key: 'master-data',
        label: '维度主数据',
        children: [
          { key: '/base-data/master/org', label: '机构' },
          { key: '/base-data/master/biz-line', label: '条线' },
          { key: '/base-data/master/dept', label: '部门' },
          { key: '/base-data/master/product', label: '产品' },
          { key: '/base-data/master/channel', label: '渠道' },
          { key: '/base-data/master/manager', label: '客户经理' },
          { key: '/base-data/master/customer', label: '客户' },
        ],
      },
      {
        key: 'indicator',
        label: '指标库',
        children: [
          { key: '/base-data/indicator/scale', label: '规模指标' },
          { key: '/base-data/indicator/revenue', label: '资产指标' },
          { key: '/base-data/indicator/cost', label: '负债指标' },
          { key: '/base-data/indicator/profit', label: '利润指标' },
          { key: '/base-data/indicator/efficiency', label: '效率指标' },
        ],
      },
    ],
  },
  {
    key: 'report',
    icon: <FileTextOutlined />,
    label: '报表中心',
    children: [
      { key: '/report/ledger', label: '台账报表' },
      { key: '/report/profit', label: '盈利报表' },
      { key: '/report/custom', label: '自定义报表' },
      { key: '/report/ai', label: 'AI报表' },
    ],
  },
  {
    key: '/data-governance',
    icon: <SafetyOutlined />,
    label: '数据治理',
  },
  {
    key: '/ai',
    icon: <RobotOutlined />,
    label: 'AI助手',
  },
];
```

- [ ] **Step 2: 提交代码**

```bash
git add 前端/src/components/MainLayout.tsx
git commit -m "feat: 优化左侧菜单结构"
```

---

## 验证清单

### 功能验证
- [ ] 指标数据页面正确显示资产/负债指标
- [ ] 二级卡片导航正常工作
- [ ] 费用详情页面显示正确数据
- [ ] 左侧菜单结构正确

### 性能验证
- [ ] 指标汇总查询响应时间 < 1秒
- [ ] 费用明细查询响应时间 < 2秒
- [ ] 页面加载时间 < 3秒

### 数据验证
- [ ] 分摊结果与原始数据一致
- [ ] 指标汇总值正确
- [ ] 维度分析数据正确
