#!/usr/bin/env bash
set -Eeuo pipefail
: "${FINANCIAL_AUDIT_BACKUP_PASSPHRASE_FILE:?passphrase file is required}"
backup="${1:?encrypted backup path is required}"
[[ -f "$backup" && -f "$backup.sha256" ]] || { echo "backup or checksum missing" >&2; exit 1; }
sha256sum -c "$backup.sha256"
openssl enc -d -aes-256-cbc -pbkdf2 -pass "file:$(realpath -- "$FINANCIAL_AUDIT_BACKUP_PASSPHRASE_FILE")" -in "$backup" -out /dev/null
echo "backup integrity and decryption check passed"
