#!/bin/bash
# ============================================
# 本地打包上传脚本
# 在本地开发机器执行
# ============================================

set -e

# 配置区（请修改为你的实际值）
SERVER_IP="101.96.197.75"
SERVER_USER="root"
SSH_KEY=""  # 如果用密钥登录，填写密钥路径，如 ~/.ssh/id_rsa

echo "=========================================="
echo "  本地打包上传脚本"
echo "=========================================="

# SSH 命令
SSH_CMD="ssh ${SSH_KEY:+-i $SSH_KEY} ${SERVER_USER}@${SERVER_IP}"
SCP_CMD="scp ${SSH_KEY:+-i $SSH_KEY}"

echo "[1/4] 打包后端..."
cd backend
mvn clean package -DskipTests -q
echo "✅ 后端打包完成: target/multi-profit-analysis-1.0.0-SNAPSHOT.jar"
cd ..

echo "[2/4] 打包前端..."
cd frontend
# 创建生产环境配置
cat > .env.production << EOF
VITE_API_BASE=http://${SERVER_IP}/api
EOF

npm run build
echo "✅ 前端打包完成: frontend/dist/"
cd ..

echo "[3/4] 上传文件到服务器..."
# 上传后端 JAR
${SCP_CMD} backend/target/multi-profit-analysis-1.0.0-SNAPSHOT.jar ${SERVER_USER}@${SERVER_IP}:/opt/multiprofit/multi-profit-analysis.jar
echo "✅ 后端 JAR 已上传"

# 上传前端文件
${SCP_CMD} -r frontend/dist/* ${SERVER_USER}@${SERVER_IP}:/var/www/multiprofit/
echo "✅ 前端文件已上传"

# 上传 SQL 文件
${SCP_CMD} create-tables.sql master-data-3level.sql ${SERVER_USER}@${SERVER_IP}:/tmp/
echo "✅ SQL 文件已上传"

echo "[4/4] 初始化数据库并启动服务..."
${SSH_CMD} << 'REMOTE_SCRIPT'
# 导入数据库（如果表不存在则创建）
mysql -u mpuser -p$(grep 'password:' /opt/multiprofit/application-prod.yml | awk '{print $2}' | tr -d '"') multi_profit < /tmp/create-tables.sql 2>/dev/null || true
mysql -u mpuser -p$(grep 'password:' /opt/multiprofit/application-prod.yml | awk '{print $2}' | tr -d '"') multi_profit < /tmp/master-data-3level.sql 2>/dev/null || true

# 启动/重启后端服务
systemctl daemon-reload
systemctl restart multiprofit
systemctl enable multiprofit

# 检查服务状态
sleep 3
if systemctl is-active --quiet multiprofit; then
    echo "✅ 后端服务启动成功"
else
    echo "❌ 后端服务启动失败，查看日志:"
    journalctl -u multiprofit --no-pager -n 20
fi

# 重启 Nginx
systemctl reload nginx
REMOTE_SCRIPT

echo ""
echo "=========================================="
echo "  ✅ 部署完成！"
echo "=========================================="
echo ""
echo "🌐 访问地址: http://${SERVER_IP}"
echo ""
echo "📋 常用命令:"
echo "  查看后端日志: ssh ${SERVER_USER}@${SERVER_IP} 'journalctl -u multiprofit -f'"
echo "  重启后端:     ssh ${SERVER_USER}@${SERVER_IP} 'systemctl restart multiprofit'"
echo "  查看 Nginx:   ssh ${SERVER_USER}@${SERVER_IP} 'nginx -t && systemctl status nginx'"
echo ""
