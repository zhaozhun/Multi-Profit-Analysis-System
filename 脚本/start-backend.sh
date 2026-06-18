#!/bin/bash
# 后端启动脚本
# 自动加载环境变量并启动Spring Boot应用

# 获取脚本所在目录的上级目录（项目根目录）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 加载环境变量
ENV_FILE="$PROJECT_DIR/.env"
if [ -f "$ENV_FILE" ]; then
    echo "加载环境变量: $ENV_FILE"
    source "$ENV_FILE"
else
    echo "警告: 未找到 .env 文件，请先创建"
    echo "示例: cp $PROJECT_DIR/.env.example $PROJECT_DIR/.env"
    exit 1
fi

# 检查必要的环境变量
if [ -z "$DB_PASSWORD" ]; then
    echo "错误: 未设置 DB_PASSWORD 环境变量"
    exit 1
fi

if [ -z "$AI_API_KEY" ]; then
    echo "错误: 未设置 AI_API_KEY 环境变量"
    exit 1
fi

# 启动后端
echo "启动后端服务..."
cd "$PROJECT_DIR/后端"
mvn spring-boot:run
