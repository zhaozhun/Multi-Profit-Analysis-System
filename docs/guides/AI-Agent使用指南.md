# AI Agent 使用指南

## 一、功能概述

多维盈利分析系统集成了5个AI Agent，提供智能化的数据分析和业务支持：

| Agent | 图标 | 功能 |
|-------|------|------|
| 📥 数据接入Agent | 导入、上传、同步 | 自动化数据导入全流程 |
| 🔍 专项分析Agent | 分析、归因、为什么 | 复杂业务问题深度分析 |
| 💰 费用分摊Agent | 分摊、规则、因子 | 费用分摊全流程管理 |
| ⚠️ 风险预警Agent | 风险、预警、监控 | 风险指标监控预警 |
| 💬 智能助手Agent | 默认 | 快速响应简单查询 |

## 二、使用方式

### 2.1 访问AI助手

1. 登录系统后，点击左侧菜单"AI助手"
2. 在底部输入框输入问题
3. 系统会自动匹配对应的Agent处理

### 2.2 示例对话

#### 数据查询
```
用户: 本月哪个分行利润最高？

AI: 📊 查询结果

本月利润排名：
1. 北京分行 - 净利润 2,850万
2. 上海分行 - 净利润 2,580万
3. 深圳分行 - 净利润 2,210万

💡 如需深入分析，可以问我：
• "深圳分行利润为什么下降？"
• "各产品线利润对比"
```

#### 深度分析
```
用户: Q3利润为什么下降了？

AI: 🔍 专项分析Agent

📊 Q3利润下降归因分析

【分析摘要】
Q3净利润同比下降5.2%，主要受深圳分行拖累。

【详细分析】
1. 深圳分行利润下降1,200万（贡献度60%）
   • A产品利润下降40%
   • 原因：原材料成本上涨22%

2. 杭州分行利润下降800万（贡献度40%）
   • 营销费用同比增长30%

【建议】
• 与供应商重新谈判A产品原材料价格
• 优化深圳仓储布局降低物流成本
• 考虑A产品适度提价3-5%
```

#### 费用分摊
```
用户: 检查一下分摊规则有没有问题？

AI: 💰 费用分摊Agent

📋 分摊规则诊断报告

【整体评分】75分

【问题1】规则覆盖度不足 (严重)
• 14种费用类型未配置分摊规则
• 催收费用、数据使用费、IT运维费等

【问题2】规则冲突 (警告)
• 营销费用同时配置了RATIO和WEIGHTED规则

【优化建议】
1. 优先补充14种未配置规则
2. 删除冲突的重复规则
```

## 三、Agent配置

### 3.1 配置文件位置

Agent配置文件位于：
```
后端/src/main/resources/agents/
├── data-ingestion.md      # 数据接入Agent
├── deep-analysis.md       # 专项分析Agent
├── allocation.md          # 费用分摊Agent
├── risk-alert.md          # 风险预警Agent
└── smart-assistant.md     # 智能助手Agent
```

### 3.2 配置文件格式

```markdown
---
name: Agent名称
icon: 图标
description: 描述
triggers:
  - 触发词1
  - 触发词2
tools:
  - 工具1
  - 工具2
max_iterations: 10
---

# 系统提示词

Agent的系统提示词内容...
```

### 3.3 自定义Agent

1. 在 `agents/` 目录下创建新的MD文件
2. 按照格式填写配置
3. 重启后端服务

## 四、API接口

### 4.1 Agent对话

**请求**
```http
POST /api/agent/chat
Content-Type: application/json
X-User-Id: user001
X-User-Name: 张三
X-User-Role: ANALYST

{
  "message": "Q3利润为什么下降了？",
  "sessionId": "optional-session-id"
}
```

**响应**
```json
{
  "code": 200,
  "data": {
    "agentName": "专项分析Agent",
    "agentIcon": "🔍",
    "answer": "📊 Q3利润下降归因分析...",
    "sessionId": "uuid-session-id",
    "status": "COMPLETED",
    "confidence": 85,
    "suggestions": [
      "按产品维度下钻分析",
      "查看同比变化趋势",
      "生成分析报告"
    ]
  }
}
```

### 4.2 获取Agent列表

**请求**
```http
GET /api/agent/list
```

**响应**
```json
{
  "code": 200,
  "data": [
    {
      "name": "数据接入Agent",
      "icon": "📥",
      "description": "自动化数据导入、清洗、转换、校验全流程",
      "triggers": ["导入", "上传", "同步", "接入", "ETL", "批量导入"]
    },
    ...
  ]
}
```

## 五、权限控制

### 5.1 用户角色

| 角色 | 权限 |
|------|------|
| ADMIN | 全部权限 |
| BRANCH_MANAGER | 本行数据 |
| CUSTOMER_MANAGER | 本客户数据 |
| ANALYST | 全部数据（只读） |

### 5.2 权限传递

通过HTTP Header传递用户信息：
- `X-User-Id`: 用户ID
- `X-User-Name`: 用户名称
- `X-User-Role`: 用户角色

## 六、会话管理

### 6.1 会话ID

- 首次对话不传sessionId，系统自动生成
- 后续追问使用返回的sessionId
- 会话30分钟无活动自动过期

### 6.2 追问场景

```
用户: 各机构利润排名
AI: [返回排名结果]

用户: 深圳分行为什么这么低？（追问）
AI: [基于会话上下文，自动下钻分析深圳分行]
```

## 七、常见问题

### Q1: Agent没有响应？
A: 检查Claude API Key是否配置正确。

### Q2: Agent响应不准确？
A: 可以修改对应的MD配置文件，调整系统提示词。

### Q3: 如何添加新的Agent？
A: 在 `agents/` 目录下创建新的MD配置文件即可。

### Q4: 会话历史丢失？
A: 会话缓存30分钟过期，过期后需要重新开始对话。
