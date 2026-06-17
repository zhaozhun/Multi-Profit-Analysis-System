---
name: setup-guide
description: 环境准备和初始化步骤
metadata:
  type: reference
---

## 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0
- Redis
- Python 3 (用于数据生成)

## 一键初始化
```bash
cd /home/zhaoz0009/multi-profit-analysis
bash setup-all.sh
```

## 手动初始化步骤

### 1. 数据库初始化
```bash
# 创建数据库和用户
sudo mysql -e "CREATE DATABASE IF NOT EXISTS multi_profit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'mpuser'@'localhost' IDENTIFIED BY '<DB_PASSWORD>';"
sudo mysql -e "GRANT ALL ON multi_profit.* TO 'mpuser'@'localhost';"

# 建表
mysql -u mpuser -p<DB_PASSWORD> multi_profit < create-tables.sql

# 导入主数据
mysql -u mpuser -p<DB_PASSWORD> multi_profit < backend/src/main/resources/data-mock.sql
mysql -u mpuser -p<DB_PASSWORD> multi_profit < backend/src/main/resources/indicator-data.sql
mysql -u mpuser -p<DB_PASSWORD> multi_profit < master-data-3level.sql
```

### 2. 生成业务数据
```bash
cd backend/src/main/resources
python3 mock-data-generator.py
# 生成 biz_ledger_mock.csv（约30万条）
```

### 3. 导入业务数据
```bash
# 启用 local_infile
sudo mysql -e "SET GLOBAL local_infile = 1;"

# 使用Python脚本分批导入
python3 -c "
import csv, mysql.connector
conn = mysql.connector.connect(host='localhost', user='mpuser', password='<DB_PASSWORD>', database='multi_profit', allow_local_infile=True)
cursor = conn.cursor()
# ... 分批导入逻辑
"
```

### 4. 配置AI（可选）
```bash
export ANTHROPIC_API_KEY=sk-ant-xxxxx
```

### 5. 启动服务
```bash
# 后端
cd backend && mvn spring-boot:run

# 前端
cd frontend && npm install && npm run dev
```

## 数据库连接信息
- Host: localhost
- Port: 3306
- Database: multi_profit
- Username: mpuser
- Password: <DB_PASSWORD>
