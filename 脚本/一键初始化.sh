#!/bin/bash
# ============================================
# 多维盈利分析系统 - 一键初始化脚本
# 运行方式: cd /home/zhaoz0009/multi-profit-analysis && bash setup-all.sh
# ============================================

set -e

echo "=========================================="
echo "  多维盈利分析系统 - 数据库初始化"
echo "=========================================="

# 1. 创建数据库和用户
echo ""
echo "[1/5] 创建数据库和用户..."
sudo mysql -e "CREATE DATABASE IF NOT EXISTS multi_profit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'mpuser'@'localhost' IDENTIFIED BY '<DB_PASSWORD>';"
sudo mysql -e "GRANT ALL ON multi_profit.* TO 'mpuser'@'localhost';"
sudo mysql -e "ALTER USER 'mpuser'@'localhost' IDENTIFIED WITH mysql_native_password BY '<DB_PASSWORD>';"
sudo mysql -e "FLUSH PRIVILEGES;"
echo "  ✅ 数据库和用户创建完成"

# 2. 建表
echo ""
echo "[2/5] 创建数据表..."
mysql -u mpuser -p<DB_PASSWORD> multi_profit < /home/zhaoz0009/multi-profit-analysis/create-tables.sql
echo "  ✅ 数据表创建完成"

# 3. 导入主数据
echo ""
echo "[3/5] 导入主数据..."
mysql -u mpuser -p<DB_PASSWORD> multi_profit < /home/zhaoz0009/multi-profit-analysis/backend/src/main/resources/data-mock.sql
mysql -u mpuser -p<DB_PASSWORD> multi_profit < /home/zhaoz0009/multi-profit-analysis/backend/src/main/resources/indicator-data.sql
echo "  ✅ 主数据导入完成"

# 4. 生成30万条业务数据
echo ""
echo "[4/5] 生成30万条业务数据（约3分钟）..."
cd /home/zhaoz0009/multi-profit-analysis/backend/src/main/resources
python3 mock-data-generator.py
echo "  ✅ 数据生成完成"

# 5. 导入业务数据
echo ""
echo "[5/5] 导入业务数据到MySQL（约3分钟）..."

# 启用local_infile
sudo mysql -e "SET GLOBAL local_infile = 1;"

# 用Python脚本分批导入（避免内存问题）
python3 -c "
import csv
import mysql.connector

conn = mysql.connector.connect(
    host='localhost', user='mpuser', password='<DB_PASSWORD>',
    database='multi_profit', allow_local_infile=True
)
cursor = conn.cursor()

csv_path = '/home/zhaoz0009/multi-profit-analysis/backend/src/main/resources/biz_ledger_mock.csv'

with open(csv_path, 'r') as f:
    reader = csv.reader(f)
    headers = next(reader)  # skip header

    batch = []
    count = 0
    for row in reader:
        batch.append(row)
        if len(batch) >= 5000:
            placeholders = ','.join(['%s'] * len(headers))
            sql = f'INSERT INTO biz_ledger ({','.join(headers)}) VALUES ({placeholders})'
            cursor.executemany(sql, batch)
            conn.commit()
            count += len(batch)
            print(f'  已导入 {count:,} 条...')
            batch = []

    if batch:
        placeholders = ','.join(['%s'] * len(headers))
        sql = f'INSERT INTO biz_ledger ({','.join(headers)}) VALUES ({placeholders})'
        cursor.executemany(sql, batch)
        conn.commit()
        count += len(batch)

cursor.close()
conn.close()
print(f'  总计导入 {count:,} 条')
"
echo "  ✅ 业务数据导入完成"

# 验证
echo ""
echo "=========================================="
echo "  验证数据"
echo "=========================================="
mysql -u mpuser -p<DB_PASSWORD> multi_profit -e "
SELECT '维度主数据' as 类型, count(*) as 数量 FROM dimension_master
UNION ALL
SELECT '客户主数据', count(*) FROM customer_master
UNION ALL
SELECT '指标数据', count(*) FROM indicator_library
UNION ALL
SELECT '业务台账', count(*) FROM biz_ledger;
"

echo ""
echo "=========================================="
echo "  ✅ 初始化完成!"
echo "=========================================="
echo ""
echo "数据库连接信息："
echo "  Host: localhost"
echo "  Port: 3306"
echo "  Database: multi_profit"
echo "  Username: mpuser"
echo "  Password: <DB_PASSWORD>"
echo ""
