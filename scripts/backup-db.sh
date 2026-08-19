#!/usr/bin/env bash
#
# 배달온도 MySQL 백업 스크립트
#
# DB는 compose가 띄운 컨테이너 안에 있고 호스트로 포트를 열지 않는다.
# 그래서 호스트에서 127.0.0.1:3306으로 붙는 방식은 동작하지 않는다.
# 컨테이너 안에서 mysqldump를 실행하고 결과만 호스트로 받는다.
#
# 비밀번호는 호스트에 두지 않는다. db 컨테이너가 이미 MYSQL_USER와 MYSQL_PASSWORD를
# 환경변수로 갖고 있으므로 그것을 컨테이너 안에서 그대로 쓴다.
# 호스트 ps 목록이나 스크립트 인자에 비밀번호가 남지 않는다.
#
# 사용 예:
#   ./scripts/backup-db.sh
#
# 크론 등록 예 (매일 새벽 4시):
#   0 4 * * * /home/ubuntu/baedal-ondo-api/scripts/backup-db.sh >> /var/log/baedalondo-backup.log 2>&1

set -euo pipefail

# compose 파일이 있는 프로젝트 루트에서 실행해야 한다.
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

BACKUP_DIR="${BACKUP_DIR:-/var/backups/baedalondo}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
DB_SERVICE="${DB_SERVICE:-db}"

log() {
    echo "[$(date '+%F %T')] $*"
}

if ! docker compose ps --status running --services 2>/dev/null | grep -qx "$DB_SERVICE"; then
    log "db 컨테이너가 실행 중이 아닙니다. docker compose up -d $DB_SERVICE 후 다시 실행하세요." >&2
    exit 1
fi

mkdir -p "$BACKUP_DIR"

TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"
TARGET="$BACKUP_DIR/baedalondo-${TIMESTAMP}.sql.gz"

# --single-transaction: InnoDB를 잠그지 않고 일관된 시점의 스냅샷을 뜬다.
# --no-tablespaces: MySQL 8의 mysqldump는 기본으로 테이블스페이스를 덤프하려 하고
#   그때 PROCESS 권한을 요구한다. 앱 계정에는 그 권한이 없고 줄 이유도 없다.
# flyway_schema_history도 함께 담기므로 복원 후 앱이 마이그레이션을 다시 돌리지 않는다.
docker compose exec -T "$DB_SERVICE" sh -c '
    MYSQL_PWD="$MYSQL_PASSWORD" mysqldump \
        --user="$MYSQL_USER" \
        --single-transaction \
        --no-tablespaces \
        --routines \
        --triggers \
        --default-character-set=utf8mb4 \
        "$MYSQL_DATABASE"
' | gzip > "$TARGET"

# mysqldump는 일부 오류에서 경고만 내고 exit 0으로 끝나기도 한다.
# 파일 크기만 보면 중간에 잘린 덤프를 성공으로 오인하므로 정상 종료 표식을 확인한다.
# --no-tablespaces가 없을 때 실제로 PROCESS 권한 오류가 나면서도 exit 0으로 끝났다.
if [ ! -s "$TARGET" ] || ! gunzip -c "$TARGET" 2>/dev/null | tail -5 | grep -q '^-- Dump completed'; then
    log "덤프가 정상 종료되지 않았습니다: $TARGET" >&2
    rm -f "$TARGET"
    exit 1
fi

log "백업 완료: $TARGET ($(du -h "$TARGET" | cut -f1))"

DELETED="$(find "$BACKUP_DIR" -name 'baedalondo-*.sql.gz' -type f -mtime "+${RETENTION_DAYS}" -print -delete | wc -l)"
log "오래된 백업 ${DELETED}건 삭제 (보관 ${RETENTION_DAYS}일)"

# 서버 디스크가 통째로 날아가면 여기 있는 백업도 같이 사라진다.
# 외부 저장소 복사는 배포 단계에서 붙인다. TODO의 배포 트랙 참고.
