#!/usr/bin/env bash
#
# 배달온도 MySQL 복원 스크립트
#
# 백업은 복원해 봐야 백업이다. 이 스크립트로 실제 복구 절차를 정기적으로 확인한다.
#
# 사용 예:
#   ./scripts/restore-db.sh /var/backups/baedalondo/baedalondo-20260819-040000.sql.gz
#
# 대상 DB의 기존 데이터를 덮어쓴다. 확인 없이 실행하려면 FORCE=1 을 준다.
#   FORCE=1 ./scripts/restore-db.sh <파일>

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

DB_SERVICE="${DB_SERVICE:-db}"

log() {
    echo "[$(date '+%F %T')] $*"
}

BACKUP_FILE="${1:-}"

if [ -z "$BACKUP_FILE" ]; then
    echo "사용법: $0 <백업파일.sql.gz>" >&2
    exit 1
fi

if [ ! -s "$BACKUP_FILE" ]; then
    echo "백업 파일이 없거나 비어 있습니다: $BACKUP_FILE" >&2
    exit 1
fi

if ! docker compose ps --status running --services 2>/dev/null | grep -qx "$DB_SERVICE"; then
    log "db 컨테이너가 실행 중이 아닙니다. docker compose up -d $DB_SERVICE 후 다시 실행하세요." >&2
    exit 1
fi

if [ "${FORCE:-0}" != "1" ]; then
    echo "대상 DB의 기존 데이터를 덮어씁니다."
    echo "  프로젝트: ${COMPOSE_PROJECT_NAME:-$(basename "$PROJECT_DIR")}"
    echo "  백업파일: $BACKUP_FILE"
    read -r -p "계속하시겠습니까? (yes 입력) " answer
    if [ "$answer" != "yes" ]; then
        log "취소했습니다."
        exit 1
    fi
fi

# 앱이 붙어 있는 상태로 복원하면 중간 상태를 읽는다. 먼저 내려 두는 편이 안전하다.
log "복원 시작: $BACKUP_FILE"

gunzip -c "$BACKUP_FILE" | docker compose exec -T "$DB_SERVICE" sh -c '
    MYSQL_PWD="$MYSQL_PASSWORD" mysql \
        --user="$MYSQL_USER" \
        --default-character-set=utf8mb4 \
        "$MYSQL_DATABASE"
'

log "복원 완료"

# 덤프에 flyway_schema_history가 함께 들어 있으므로 앱은 마이그레이션을 다시 돌리지 않고
# validate만 수행한다. 앱을 재기동해 정상 부팅되는지 확인한다.
log "앱을 재기동해 스키마가 유효한지 확인하세요: docker compose up -d app"
