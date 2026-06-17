#!/bin/bash
# ============================================
# 多维盈利分析系统 - 健康检查脚本
# 用法: crontab 每分钟执行一次
# 安装: (crontab -l; echo "* * * * * /opt/multiprofit/health-check.sh") | crontab -
# ============================================

# 配置
HEALTH_URL="http://localhost:8080/api/health"
SERVICE_NAME="multiprofit"
CRASH_LOG="/var/log/multiprofit/crash.log"
MAX_FAILURES=3
STATE_FILE="/var/run/multiprofit-health-failures"

# 确保日志目录存在
mkdir -p /var/log/multiprofit

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
    echo "[$(date)] Max failures reached, restarting $SERVICE_NAME service..." >> "$CRASH_LOG"

    # 记录重启前的服务状态
    systemctl status "$SERVICE_NAME" --no-pager >> "$CRASH_LOG" 2>&1

    # 执行重启
    systemctl restart "$SERVICE_NAME"

    # 等待启动
    sleep 10

    # 检查重启是否成功
    NEW_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "$HEALTH_URL" 2>/dev/null)
    if [ "$NEW_CODE" = "200" ]; then
        echo "[$(date)] Restart successful, service is healthy" >> "$CRASH_LOG"
    else
        echo "[$(date)] Restart failed, service still unhealthy (HTTP $NEW_CODE)" >> "$CRASH_LOG"
    fi

    # 重置计数
    echo "0" > "$STATE_FILE"
fi
