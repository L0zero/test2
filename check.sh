#!/bin/bash

# 配置参数（需要用户修改以下值）
PROJECT_NAME="$PROJECT_NAME"
SONAR_TOKEN="$SONAR_TOKEN"
TOKEN="$TOKEN"
SERVER_ADDRESS="$SERVER_ADDRESS"
ZIP_FILE="JiHub-FrontEnd.zip"

# 1. 打包当前目录代码为 ZIP 文件
echo "正在打包代码为 $ZIP_FILE ..."
zip -r "$ZIP_FILE" . -x "*.git*" "*.venv*" "*__pycache__*"  # 排除无关文件

# 2. 发送 HTTP POST 请求
echo "正在发送到服务器 $SERVER_ADDRESS:7000 ..."
curl -X POST \
  --max-time 300 \
  --connect-timeout 5 \
  -F "project_name=$PROJECT_NAME" \
  -F "sonar_token=$SONAR_TOKEN" \
  -F "token=$TOKEN" \
  -F "file=@$ZIP_FILE" \
  "http://$SERVER_ADDRESS:7000/"

# 3. 清理临时 ZIP 文件（可选）
echo "清理临时文件..."
rm -f "$ZIP_FILE"

echo "操作完成！"