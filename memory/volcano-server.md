---
name: volcano-server
description: 火山云服务器信息，用于多维盈利分析系统部署
metadata:
  type: reference
---

火山云服务器：
- IP: 101.96.197.75
- OS: Ubuntu 24.04 LTS
- SSH: root 密码登录
- 已部署服务：多维盈利分析系统

已部署组件：
- Java 17 (OpenJDK)
- MySQL 8.0 (用户 mpuser，密码 multipass2026，数据库 multi_profit)
- Nginx (端口 80，反向代理到 8080)
- 后端 JAR: /opt/multiprofit/multi-profit-analysis.jar
- 前端: /var/www/multiprofit/
- systemd 服务: multiprofit

注意事项：
- MySQL root 使用 auth_socket，应用通过 mpuser 连接
- biz_ledger 表需要 loan_revenue/loan_profit 等额外列（create-tables.sql 不完整）
- SSH 连接偶尔不稳定，多次重试即可
