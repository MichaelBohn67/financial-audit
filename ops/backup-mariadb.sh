#!/usr/bin/env bash
set -Eeuo pipefail

: "${FINANCIAL_AUDIT_BACKUP_DIR:?backup directory is required}"
: "${FINANCIAL_AUDIT_BACKUP_PASSPHRASE_FILE:?passphrase file is required}"
: "${FINANCIAL_AUDIT_DB_USERNAME:?database username is required}"
: "${FINANCIAL_AUDIT_DB_PASSWORD:?database password is required}"
backup_dir="$(realpath -m -- "$FINANCIAL_AUDIT_BACKUP_DIR")"
passphrase_file="$(realpath -- "$FINANCIAL_AUDIT_BACKUP_PASSPHRASE_FILE")"
[[ -f "$passphrase_file" && -r "$passphrase_file" ]] || { echo "passphrase file is not readable" >&2; exit 1; }
umask 077
mkdir -p -- "$backup_dir"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
output="$backup_dir/financial-audit-$timestamp.sql.gz.enc"
tmp="$output.tmp"
trap 'rm -f -- "$tmp"' EXIT
MYSQL_PWD="$FINANCIAL_AUDIT_DB_PASSWORD" mariadb-dump --single-transaction --routines --triggers \
  --host="${FINANCIAL_AUDIT_DB_HOST:-localhost}" --port="${FINANCIAL_AUDIT_DB_PORT:-3306}" \
  --user="$FINANCIAL_AUDIT_DB_USERNAME" "${FINANCIAL_AUDIT_DB_NAME:-financial_audit}" \
  | gzip -9 | openssl enc -aes-256-cbc -pbkdf2 -salt -pass "file:$passphrase_file" -out "$tmp"
mv -- "$tmp" "$output"
sha256sum -- "$output" > "$output.sha256"
echo "$output"
