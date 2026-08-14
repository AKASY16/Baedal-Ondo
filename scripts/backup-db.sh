#!/usr/bin/env bash
#
# 배달온도 MySQL 일일 백업 스크립트 (EC2용)
#
# 사용 예:
#   DB_PASSWORD=... ./scripts/backup-db.sh
#
# 크론 등록 예 (매일 새벽 4시, 한국시간 기준 서버):
#   0 4 * * * DB_PASSWORD=... /home/ubuntu/baedal-ondo-api/scripts/backup-db.sh >> /var/log/baedalondo-backup.log 2>&1

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/baedalondo}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-baedalondo}"
DB_USER="${DB_USERNAME:-baedalondo_app}"

if [ -z "${DB_PASSWORD:-}" ]; then
    echo "[$(date '+%F %T')] DB_PASSWORD 환경변수가 필요합니다." >&2
    exit 1
fi

mkdir -p "$BACKUP_DIR"

TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"
TARGET="$BACKUP_DIR/${DB_NAME}-${TIMESTAMP}.sql.gz"

# 비밀번호를 인자로 넘기면 ps 목록에 노출되므로 MYSQL_PWD로 전달한다.
# --single-transaction: InnoDB를 잠그지 않고 일관된 시점의 스냅샷을 뜬다.
MYSQL_PWD="$DB_PASSWORD" mysqldump \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --user="$DB_USER" \
    --single-transaction \
    --routines \
    --triggers \
    --default-character-set=utf8mb4 \
    "$DB_NAME" \
    | gzip > "$TARGET"

# 파이프 중간의 mysqldump 실패를 잡는다. set -o pipefail이 있어 여기까지 오면 성공이다.
SIZE="$(du -h "$TARGET" | cut -f1)"
echo "[$(date '+%F %T')] 백업 완료: $TARGET ($SIZE)"

# 보관 기간이 지난 백업 정리
DELETED="$(find "$BACKUP_DIR" -name "${DB_NAME}-*.sql.gz" -type f -mtime "+${RETENTION_DAYS}" -print -delete | wc -l)"
echo "[$(date '+%F %T')] 오래된 백업 ${DELETED}건 삭제 (보관 ${RETENTION_DAYS}일)"
