---
name: api-design
description: REST API 端点清单及功能说明
metadata:
  type: reference
---

## 控制器列表（9个）
1. DashboardController - 驾驶舱
2. DimensionController - 维度分析
3. AiController - AI问答
4. AiExploreController - AI探索
5. IndicatorController - 指标管理
6. MasterDataController - 主数据管理
7. ReportController - 报表
8. DataValidationController - 数据校验
9. DataGovernanceController - 数据治理

## 核心API

### 驾驶舱
- GET /api/dashboard/overview - 全量数据（KPI、瀑布图、趋势、维度概览）
- GET /api/dashboard/waterfall - 瀑布图数据
- GET /api/dashboard/trend - 趋势数据

### 维度分析
- GET /api/dimension/{dimType}/analysis - 维度分析数据
- 支持 dimType: org, product, biz_line, dept, channel, manager

### AI功能
- POST /api/ai/chat - AI问答
- POST /api/ai/brief - 生成经营简报

### 数据校验
- POST /api/validation/detect - 异常检测

## 核心计算逻辑
```
净利润 = 业务收入 - FTP资金成本 - 风险成本 - 运营成本
```
