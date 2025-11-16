#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: $0 <backup-directory>

Environment variables (with defaults):
  DB_HOST (localhost)
  DB_PORT (3306)
  DB_USER (root)
  DB_PASSWORD (empty)
USAGE
}

if [[ $# -lt 1 || "$1" == "-h" || "$1" == "--help" ]]; then
  usage
  exit $([[ $# -lt 1 ]] && echo 1 || echo 0)
fi

BACKUP_DIR=$1
if [[ ! -d "$BACKUP_DIR" ]]; then
  echo "Backup directory '$BACKUP_DIR' not found" >&2
  exit 1
fi

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-root}
DB_PASSWORD=${DB_PASSWORD:-}

restore_file() {
  local file=$1
  local schema=$2
  echo "Restoring $file -> $schema"
  if [[ -n "$DB_PASSWORD" ]]; then
    MYSQL_PWD="$DB_PASSWORD" mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$schema" < "$file"
  else
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$schema" < "$file"
  fi
}

for sql in "$BACKUP_DIR"/*.sql; do
  [[ -e "$sql" ]] || { echo "No .sql files found in $BACKUP_DIR"; exit 1; }
  schema=$(basename "$sql" | awk -F'_' '{print $1}')
  restore_file "$sql" "$schema"

done

echo "Restore complete."
