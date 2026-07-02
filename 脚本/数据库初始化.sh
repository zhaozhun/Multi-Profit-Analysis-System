#!/bin/bash
# MySQL 数据库初始化脚本
# 运行方式: DB_PASSWORD=xxx bash setup-mysql.sh
# 数据库密码通过环境变量 DB_PASSWORD 注入(不硬编码)

DB_USER="${DB_USER:-mpuser}"
DB_PASS="${DB_PASSWORD:-}"
if [ -z "$DB_PASS" ]; then echo "错误:请通过环境变量 DB_PASSWORD 提供数据库密码"; exit 1; fi

echo "=== 创建数据库 ==="
sudo mysql -e "CREATE DATABASE IF NOT EXISTS multi_profit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo "=== 创建用户 ==="
sudo mysql -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';"

echo "=== 授权 ==="
sudo mysql -e "GRANT ALL ON multi_profit.* TO '${DB_USER}'@'localhost';"
sudo mysql -e "ALTER USER '${DB_USER}'@'localhost' IDENTIFIED WITH mysql_native_password BY '${DB_PASS}';"
sudo mysql -e "FLUSH PRIVILEGES;"

echo "=== 测试连接 ==="
mysql -u "${DB_USER}" -p"${DB_PASS}" -e "USE multi_profit; SELECT '连接成功!' as status;"

echo "=== 完成 ==="
