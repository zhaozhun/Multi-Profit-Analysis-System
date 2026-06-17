---
name: project-overview
description: 多维盈利分析系统项目概况、技术栈、目录结构
metadata:
  type: project
---

## 项目名称
多维盈利分析系统（multi-profit-analysis）

## 定位
基于 React + Spring Boot + Claude AI 的银行多维盈利分析系统。

## 技术栈
| 层级 | 技术 |
|------|------|
| 前端 | React 18 + TypeScript + Ant Design 5 + ECharts 5 |
| 后端 | Java 17 + Spring Boot 3 + MyBatis Plus |
| 数据库 | MySQL 8.0 |
| AI | Claude API (LangChain4j) |
| 缓存 | Redis |

## 目录结构
```
/home/zhaoz0009/multi-profit-analysis/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/multiprofit/
│   │   ├── controller/        # REST API（9个控制器）
│   │   ├── service/           # 业务服务
│   │   ├── model/             # 数据模型
│   │   ├── dto/               # DTO
│   │   ├── ai/                # AI集成（ClaudeClient, ReportGenerator）
│   │   └── config/            # 配置
│   └── src/main/resources/
│       ├── application.yml    # 配置文件
│       ├── schema.sql         # 建表语句
│       ├── data-mock.sql      # Mock数据
│       ├── indicator-data.sql # 指标数据
│       └── mock-data-generator.py  # 数据生成器
├── frontend/                   # React 前端
│   └── src/
│       ├── pages/             # Dashboard, DimensionAnalysis, AiAssistant, Report, Indicator, MasterData, DataGovernance
│       ├── components/        # MainLayout
│       └── services/          # api.ts
├── create-tables.sql          # MySQL 建表脚本
├── master-data-3level.sql     # 3级主数据
├── setup-all.sh               # 一键初始化脚本
├── setup-mysql.sh             # MySQL 初始化
├── deploy/                    # 部署脚本
└── docs/                      # 文档
```

## 启动方式
```bash
# 数据库初始化
cd /home/zhaoz0009/multi-profit-analysis
bash setup-all.sh

# 启动后端
cd backend && mvn spring-boot:run

# 启动前端
cd frontend && npm install && npm run dev
```

前端地址：http://localhost:3000
后端地址：http://localhost:8080
