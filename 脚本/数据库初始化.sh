#!/bin/bash
# MySQL 数据库初始化脚本
# 运行方式: bash setup-mysql.sh

echo "=== 创建数据库 ==="
sudo mysql -e "CREATE DATABASE IF NOT EXISTS multi_profit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo "=== 创建用户 ==="
sudo mysql -e "CREATE USER IF NOT EXISTS 'mpuser'@'localhost' IDENTIFIED BY '<DB_PASSWORD>';"

echo "=== 授权 ==="
sudo mysql -e "GRANT ALL ON multi_profit.* TO 'mpuser'@'localhost';"
sudo mysql -e "ALTER USER 'mpuser'@'localhost' IDENTIFIED WITH mysql_native_password BY '<DB_PASSWORD>';"
sudo mysql -e "FLUSH PRIVILEGES;"

echo "=== 测试连接 ==="
mysql -u mpuser -p<DB_PASSWORD> -e "USE multi_profit; SELECT '连接成功!' as status;"

echo "=== 完成 ==="
