#!/bin/bash
# ============================================
# 多维盈利分析系统 - 火山云服务器部署脚本
# 使用方法: sudo bash deploy.sh
# ============================================

set -e

echo "=========================================="
echo "  多维盈利分析系统 - 自动部署脚本"
echo "=========================================="

# 检查是否以 root 运行
if [ "$EUID" -ne 0 ]; then
    echo "❌ 请使用 sudo 运行此脚本: sudo bash deploy.sh"
    exit 1
fi

# 配置区（请修改为你的实际值）
DB_PASSWORD="Mp@2026secure"           # 数据库密码
APP_PORT=8080                          # 后端端口
FRONTEND_PORT=80                       # 前端端口（Nginx）
APP_DIR="/opt/multiprofit"             # 应用目录
FRONTEND_DIR="/var/www/multiprofit"    # 前端目录

# 检测系统类型
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
else
    echo "无法检测操作系统"
    exit 1
fi

echo "[1/8] 安装系统依赖..."
if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
    apt update -y
    apt install -y openjdk-17-jdk nginx mysql-server curl

    # 启动 MySQL
    systemctl start mysql
    systemctl enable mysql

elif [ "$OS" = "centos" ] || [ "$OS" = "rhel" ] || [ "$OS" = "rocky" ]; then
    yum update -y
    yum install -y java-17-openjdk java-17-openjdk-devel nginx mysql-server curl

    # 启动 MySQL
    systemctl start mysqld
    systemctl enable mysqld
else
    echo "不支持的操作系统: $OS"
    exit 1
fi

echo "[2/8] 配置 MySQL 数据库..."
# 创建数据库和用户
mysql -u root << EOF
CREATE DATABASE IF NOT EXISTS multi_profit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'mpuser'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON multi_profit.* TO 'mpuser'@'localhost';
FLUSH PRIVILEGES;
EOF

echo "[3/8] 创建应用目录..."
mkdir -p ${APP_DIR}
mkdir -p ${FRONTEND_DIR}
mkdir -p /var/log/multiprofit

echo "[4/8] 创建后端配置文件..."
cat > ${APP_DIR}/application-prod.yml << EOF
server:
  port: ${APP_PORT}
  # 优雅停机
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
  datasource:
    url: jdbc:mysql://localhost:3306/multi_profit?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: mpuser
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    # HikariCP 连接池
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      connection-test-query: SELECT 1
      validation-timeout: 5000
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

# Actuator 监控
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: always
  health:
    db:
      enabled: true
    diskspace:
      enabled: true

logging:
  level:
    com.multiprofit: info
  file:
    name: /var/log/multiprofit/app.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 1GB

# AI 配置（可选）
ai:
  anthropic:
    api-key: \${ANTHROPIC_API_KEY:}
    model: claude-sonnet-4-20250514
    max-tokens: 4096
EOF

echo "[5/8] 创建 systemd 服务..."
cat > /etc/systemd/system/multiprofit.service << EOF
[Unit]
Description=Multi Profit Analysis Backend
After=network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=${APP_DIR}

# JVM 内存与 GC 配置
Environment="JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/multiprofit/heapdump.hprof -XX:+ExitOnOutOfMemoryError"

ExecStart=/usr/bin/java \$JAVA_OPTS -jar ${APP_DIR}/multi-profit-analysis.jar \\
    --spring.profiles.active=prod \\
    --spring.config.location=${APP_DIR}/application-prod.yml

# ===== 崩溃自动重启 =====
# 任何非正常退出都自动重启
Restart=always
# 重启间隔：首次5秒，后续递增
RestartSec=5
# 60秒内最多重启5次，超过则停止尝试（防止无限重启风暴）
StartLimitIntervalSec=60
StartLimitBurst=5

# ===== 超时控制 =====
# 启动超时：120秒（Spring Boot 启动可能较慢）
TimeoutStartSec=120
# 停止超时：30秒（优雅停机后强制 kill）
TimeoutStopSec=30

# ===== Watchdog 看门狗 =====
# 60秒内进程必须发送 keepalive，否则视为卡死并重启
WatchdogSec=60

# ===== 资源限制 =====
# 最大打开文件数
LimitNOFILE=65536
# 内存上限 1.5GB（防止 OOM 影响其他服务）
MemoryMax=1536M

# ===== 崩溃后执行 =====
# 服务异常退出后记录崩溃信息
ExecStopPost=/bin/bash -c 'echo "[\$(date)] multiprofit crashed with exit code \$EXIT_STATUS" >> /var/log/multiprofit/crash.log'

# 日志输出
StandardOutput=journal
StandardError=journal
SyslogIdentifier=multiprofit

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload

echo "[6/8] 配置 Nginx..."
# 获取服务器公网 IP
PUBLIC_IP=$(curl -s ifconfig.me || curl -s ip.sb || echo "localhost")

# 确保目录存在
mkdir -p /etc/nginx/sites-available
mkdir -p /etc/nginx/sites-enabled

cat > /etc/nginx/sites-available/multiprofit << EOF
server {
    listen ${FRONTEND_PORT};
    server_name _;

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml;
    gzip_min_length 1000;

    # 前端静态文件
    location / {
        root ${FRONTEND_DIR};
        index index.html;
        try_files \$uri \$uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:${APP_PORT}/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf)$ {
        root ${FRONTEND_DIR};
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
}
EOF

# 启用站点
if [ -d /etc/nginx/sites-enabled ]; then
    ln -sf /etc/nginx/sites-available/multiprofit /etc/nginx/sites-enabled/
    rm -f /etc/nginx/sites-enabled/default
else
    # CentOS 风格
    mkdir -p /etc/nginx/conf.d/
    cp /etc/nginx/sites-available/multiprofit /etc/nginx/conf.d/multiprofit.conf
fi

# 测试并启用 Nginx 配置
nginx -t && systemctl reload nginx
systemctl enable nginx 2>/dev/null || true

echo "[7/8] 配置健康检查 & 日志轮转..."
# 健康检查脚本
cat > ${APP_DIR}/health-check.sh << 'HEALTHEOF'
#!/bin/bash
# ============================================
# 多维盈利分析系统 - 健康检查脚本
# 由 crontab 每分钟执行一次
# ============================================

HEALTH_URL="http://localhost:8080/api/health"
CRASH_LOG="/var/log/multiprofit/crash.log"
MAX_FAILURES=3
STATE_FILE="/var/run/multiprofit-health-failures"

# 发起健康检查
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "$HEALTH_URL" 2>/dev/null)

if [ "$HTTP_CODE" = "200" ]; then
    # 健康：重置失败计数
    echo "0" > "$STATE_FILE"
    exit 0
fi

# 不健康：累加失败计数
FAILURES=$(cat "$STATE_FILE" 2>/dev/null || echo "0")
FAILURES=$((FAILURES + 1))
echo "$FAILURES" > "$STATE_FILE"

echo "[$(date)] Health check failed ($FAILURES/$MAX_FAILURES), HTTP code: $HTTP_CODE" >> "$CRASH_LOG"

if [ "$FAILURES" -ge "$MAX_FAILURES" ]; then
    echo "[$(date)] Max failures reached, restarting multiprofit service..." >> "$CRASH_LOG"
    systemctl restart multiprofit
    echo "0" > "$STATE_FILE"
fi
HEALTHEOF
chmod +x ${APP_DIR}/health-check.sh

# 添加 crontab（每分钟检查一次）
(crontab -l 2>/dev/null | grep -v "health-check.sh"; echo "* * * * * ${APP_DIR}/health-check.sh") | crontab -

# 配置日志轮转
cat > /etc/logrotate.d/multiprofit << EOF
/var/log/multiprofit/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 root root
}
EOF

echo "[8/8] 开放防火墙端口..."
if command -v ufw &> /dev/null; then
    ufw allow 80/tcp
    ufw allow 443/tcp
    ufw allow ${APP_PORT}/tcp
elif command -v firewall-cmd &> /dev/null; then
    firewall-cmd --permanent --add-port=80/tcp
    firewall-cmd --permanent --add-port=443/tcp
    firewall-cmd --permanent --add-port=${APP_PORT}/tcp
    firewall-cmd --reload
fi

echo ""
echo "=========================================="
echo "  ✅ 服务器环境部署完成！"
echo "=========================================="
echo ""
echo "📋 接下来你需要："
echo ""
echo "1. 上传后端 JAR 包:"
echo "   scp target/multi-profit-analysis-1.0.0-SNAPSHOT.jar root@${PUBLIC_IP}:${APP_DIR}/multi-profit-analysis.jar"
echo ""
echo "2. 上传前端打包文件:"
echo "   scp -r dist/* root@${PUBLIC_IP}:${FRONTEND_DIR}/"
echo ""
echo "3. 导入数据库:"
echo "   scp create-tables.sql master-data-3level.sql root@${PUBLIC_IP}:/tmp/"
echo "   ssh root@${PUBLIC_IP} 'mysql -u mpuser -p${DB_PASSWORD} multi_profit < /tmp/create-tables.sql'"
echo "   ssh root@${PUBLIC_IP} 'mysql -u mpuser -p${DB_PASSWORD} multi_profit < /tmp/master-data-3level.sql'"
echo ""
echo "4. 启动后端服务:"
echo "   ssh root@${PUBLIC_IP}"
echo "   systemctl start multiprofit"
echo "   systemctl status multiprofit"
echo ""
echo "5. 访问系统:"
echo "   http://${PUBLIC_IP}"
echo ""
echo "🛡️ 崩溃防护已配置："
echo "   - JVM 内存: 512MB~1GB，G1GC"
echo "   - OOM 自动 dump + 重启"
echo "   - systemd 看门狗: 60秒心跳"
echo "   - 健康检查: 每分钟 cron 检测，连续3次失败自动重启"
echo "   - 崩溃日志: /var/log/multiprofit/crash.log"
echo "   - 日志轮转: 每天，保留30天"
echo ""
echo "📋 常用命令:"
echo "   查看服务状态:   systemctl status multiprofit"
echo "   查看后端日志:   journalctl -u multiprofit -f"
echo "   查看崩溃日志:   tail -f /var/log/multiprofit/crash.log"
echo "   健康检查:       curl http://localhost:8080/api/health"
echo "   重启服务:       systemctl restart multiprofit"
echo ""
echo "=========================================="
