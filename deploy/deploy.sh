#!/usr/bin/env bash
#
# 发布脚本：本机构建 → 上传 → 备份旧包 → 重启 → 健康检查
# 运行环境：Windows Git Bash / macOS / Linux，需本机有 mvn、ssh、scp
#
# 用法：
#   export SERVER_IP=1.2.3.4
#   export SSH_USER=root
#   ./deploy/deploy.sh
#
# 注意：
#   1. 若本次带了新的 Flyway 迁移，回滚 jar 不会回滚表结构，必须先恢复数据库备份。
#   2. 脚本不跑测试；发版前请先在本地执行 mvn test。

set -euo pipefail

# Git Bash 会把远端路径 /opt/... 错误地转成 Windows 路径，必须关闭转换
export MSYS_NO_PATHCONV=1

SERVER_IP="${SERVER_IP:?请先 export SERVER_IP=你的公网IP}"
SSH_USER="${SSH_USER:-root}"
APP_DIR="${APP_DIR:-/opt/nongjiale/api}"
SERVICE="${SERVICE:-nongjiale-api}"
JAR="target/nongjiale-api-0.0.1-SNAPSHOT.jar"
STAMP="$(date +%Y%m%d-%H%M%S)"

command -v mvn >/dev/null || { echo "找不到 mvn，请先安装 Maven 并加入 PATH"; exit 1; }

echo "==> 1/4 构建"
mvn -q clean package -DskipTests
[ -f "$JAR" ] || { echo "构建产物不存在：$JAR"; exit 1; }
ls -lh "$JAR"

echo "==> 2/4 上传到 ${SSH_USER}@${SERVER_IP}:${APP_DIR}"
scp -C "$JAR" "${SSH_USER}@${SERVER_IP}:${APP_DIR}/app.jar.new"

echo "==> 3/4 备份旧包并切换"
ssh "${SSH_USER}@${SERVER_IP}" "set -e
  cd ${APP_DIR}
  if [ -f app.jar ]; then cp app.jar app.jar.bak.${STAMP}; fi
  mv app.jar.new app.jar
  chown njl:njl app.jar
  systemctl restart ${SERVICE}
"

echo "==> 4/4 健康检查（最多等 30 秒）"
for i in $(seq 1 10); do
  if ssh "${SSH_USER}@${SERVER_IP}" "curl -fsS http://127.0.0.1:8080/health"; then
    echo
    echo "发布完成。旧包：app.jar.bak.${STAMP}"
    exit 0
  fi
  sleep 3
done

echo
echo "健康检查失败，最近日志："
ssh "${SSH_USER}@${SERVER_IP}" "journalctl -u ${SERVICE} -n 80 --no-pager"
echo
echo "回滚：ssh ${SSH_USER}@${SERVER_IP} 'cd ${APP_DIR} && cp app.jar.bak.${STAMP} app.jar && systemctl restart ${SERVICE}'"
exit 1
