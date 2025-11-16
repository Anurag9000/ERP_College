#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: $0 [output-directory]

Environment variables (with defaults):
  DB_HOST (localhost)
  DB_PORT (3306)
  DB_USER (root)
  DB_PASSWORD (empty)
  AUTH_DB (college_auth)
  ERP_DB  (college_erp)

Examples:
  DB_USER=erp_admin DB_PASSWORD=secret ./scripts/db-backup.sh
  ./scripts/db-backup.sh backups/nightly
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-root}
DB_PASSWORD=${DB_PASSWORD:-}
AUTH_DB=${AUTH_DB:-college_auth}
ERP_DB=${ERP_DB:-college_erp}
OUTPUT_DIR=${1:-backups/$(date +%Y%m%d-%H%M%S)}

mkdir -p "$OUTPUT_DIR"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

backup_schema() {
  local schema=$1
  local target="$OUTPUT_DIR/${schema}_${TIMESTAMP}.sql"
  echo "Backing up $schema -> $target"
  if [[ -n "$DB_PASSWORD" ]]; then
    MYSQL_PWD="$DB_PASSWORD" mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$schema" > "$target"
  else
    mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$schema" > "$target"
  fi
}

backup_schema "$AUTH_DB"
backup_schema "$ERP_DB"

echo "Backup complete. Files in $OUTPUT_DIR"
