#!/usr/bin/env bash
#
# MySQL 逻辑备份：mysqldump → gzip，保留 7 天
# 安装：cp deploy/backup-mysql.sh /opt/nongjiale/backup-mysql.sh && chmod 700 /opt/nongjiale/backup-mysql.sh
# 定时：17 3 * * * /opt/nongjiale/backup-mysql.sh >> /var/log/njl-backup.log 2>&1
#
# 口令从 /etc/nongjiale/api.env 读取，避免出现在 ps 和 crontab 里。
# 恢复演练见 docs/DEPLOYMENT.md §12.1。

set -euo pipefail

ENV_FILE="${ENV_FILE:-/etc/nongjiale/api.env}"
BACKUP_DIR="${BACKUP_DIR:-/opt/nongjiale/backup}"
KEEP_DAYS="${KEEP_DAYS:-7}"
STAMP="$(date +%F-%H%M)"

[ -r "$ENV_FILE" ] || { echo "读不到 $ENV_FILE"; exit 1; }
set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

mkdir -p "$BACKUP_DIR"
OUT="${BACKUP_DIR}/njl-${STAMP}.sql.gz"

# MYSQL_PWD 不落命令行，比 -p明文 安全
export MYSQL_PWD="${MYSQL_PASSWORD}"

mysqldump \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --default-character-set=utf8mb4 \
  -h "${MYSQL_HOST:-127.0.0.1}" \
  -P "${MYSQL_PORT:-3306}" \
  -u "${MYSQL_USER}" \
  "${MYSQL_DATABASE}" | gzip > "$OUT"

# 空文件或异常小文件视为失败，避免把坏备份当成功
SIZE=$(stat -c%s "$OUT" 2>/dev/null || stat -f%z "$OUT")
if [ "$SIZE" -lt 1024 ]; then
  echo "备份文件异常（${SIZE} 字节）：$OUT"
  exit 1
fi

# 清理过期备份
find "$BACKUP_DIR" -name 'njl-*.sql.gz' -mtime +"$KEEP_DAYS" -delete

echo "[$(date '+%F %T')] 备份完成 $OUT ($(du -h "$OUT" | cut -f1))"
