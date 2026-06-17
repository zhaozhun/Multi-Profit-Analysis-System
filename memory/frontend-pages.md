---
name: frontend-pages
description: 前端页面结构和路由
metadata:
  type: reference
---

## 路由结构

| 路径 | 页面 | 组件 |
|------|------|------|
| /dashboard | 经营驾驶舱 | Dashboard/index.tsx |
| /analysis/:dimType | 维度分析 | DimensionAnalysis/index.tsx |
| /ai | AI助手 | AiAssistant/index.tsx |
| /indicator | 指标管理 | Indicator/index.tsx |
| /master-data | 主数据管理 | MasterData/index.tsx |
| /report/ledger | 台账报表 | Report/Ledger.tsx |
| /report/profit | 利润报表 | Report/ProfitReport.tsx |
| /report/custom | 自定义报表 | Report/CustomReport.tsx |
| /report/ai | AI报表 | Report/AiReport.tsx |
| /data-governance | 数据治理 | DataGovernance/index.tsx |

## 技术细节
- 使用 React Router v6
- Ant Design 5 组件库
- ECharts 5 图表
- 中文本地化 (zhCN)
- 默认路由重定向到 /dashboard
