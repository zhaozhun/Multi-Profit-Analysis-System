#!/bin/bash

# =====================================================
# 测试数据执行脚本
# 执行顺序: 维度数据 -> 业务台账 -> 费用分摊
# =====================================================

# 切换到脚本所在目录
cd "$(dirname "$0")"

DB_HOST="localhost"
DB_PORT="3306"
DB_USER="mpuser"
DB_PASS="<DB_PASSWORD>"
DB_NAME="multi_profit"

echo "=========================================="
echo "开始生成测试数据..."
echo "=========================================="

# 检查MySQL连接
echo "检查数据库连接..."
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS -e "SELECT 1" > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "错误: 无法连接到数据库"
    exit 1
fi
echo "数据库连接成功"

# 执行SQL文件
echo ""
echo "1. 插入维度数据..."
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME < 01_dimension_data.sql
if [ $? -eq 0 ]; then
    echo "   ✓ 维度数据插入成功"
else
    echo "   ✗ 维度数据插入失败"
    exit 1
fi

echo ""
echo "2. 插入业务台账数据..."
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME < 02_biz_ledger.sql
if [ $? -eq 0 ]; then
    echo "   ✓ 业务台账插入成功"
else
    echo "   ✗ 业务台账插入失败"
    exit 1
fi

echo ""
echo "3. 插入费用分摊数据..."
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME < 03_allocation_data.sql
if [ $? -eq 0 ]; then
    echo "   ✓ 费用分摊数据插入成功"
else
    echo "   ✗ 费用分摊数据插入失败"
    exit 1
fi

echo ""
echo "=========================================="
echo "测试数据生成完成!"
echo "=========================================="

# 验证数据
echo ""
echo "数据验证:"
echo "----------------------------------------"
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME -e "
SELECT 'dimension_master' AS table_name, COUNT(*) AS record_count FROM dimension_master WHERE dim_type IN ('ORG','BIZ_LINE','DEPT','PRODUCT','CHANNEL','MANAGER','CUSTOMER')
UNION ALL
SELECT 'biz_ledger', COUNT(*) FROM biz_ledger WHERE account_period LIKE '2025%'
UNION ALL
SELECT 'cost_actual_record', COUNT(*) FROM cost_actual_record WHERE period LIKE '2025%'
UNION ALL
SELECT 'allocation_batch', COUNT(*) FROM allocation_batch WHERE period LIKE '2025%'
UNION ALL
SELECT 'allocation_result', COUNT(*) FROM allocation_result;
"
echo "----------------------------------------"

echo ""
echo "维度数据分布:"
echo "----------------------------------------"
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME -e "
SELECT dim_type, COUNT(*) AS count FROM dimension_master GROUP BY dim_type ORDER BY dim_type;
"

echo ""
echo "业务数据分布:"
echo "----------------------------------------"
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME -e "
SELECT account_period, COUNT(*) AS records, SUM(revenue) AS total_revenue, SUM(net_profit) AS total_profit FROM biz_ledger WHERE account_period LIKE '2025%' GROUP BY account_period ORDER BY account_period;
"

echo "----------------------------------------"
echo "执行完成!"
