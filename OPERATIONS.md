# Betriebs-, Datenschutz- und Notfallkonzept

## Datenschutz

- Es werden nur für Prüfung, Nachweisführung und gesetzliche Aufbewahrung erforderliche Daten verarbeitet.
- Zugangsdaten, Tokens, API-Schlüssel und Passwörter dürfen weder in Audit-Snapshots noch in Logs erscheinen.
- Aufbewahrungsfristen werden über `financial-audit.privacy.*-retention-period` konfiguriert. Löschungen sind standardmäßig genehmigungspflichtig und müssen vor Ausführung fachlich sowie rechtlich freigegeben werden.
- Export- und Archivdateien enthalten nur den angeforderten Tenant-/Projekt-Scope. Das Archivverzeichnis muss außerhalb des öffentlich ausgelieferten Webroots liegen.
- Betroffenenanfragen, Korrekturen und Löschungen werden als kontrollierte Change-/Audit-Prozesse dokumentiert; der unveränderbare Audit-Trail selbst wird nicht überschrieben.

## Backup und Restore

Backups werden verschlüsselt und außerhalb des Produktivsystems abgelegt. Die bereitgestellten Skripte erwarten explizite Ziele und ein Secret aus einem Secret Store:

```bash
export FINANCIAL_AUDIT_BACKUP_DIR=/secure/backup/financial-audit
export FINANCIAL_AUDIT_BACKUP_PASSPHRASE_FILE=/run/secrets/financial-audit-backup-passphrase
./ops/backup-mariadb.sh
./ops/verify-backup.sh /secure/backup/financial-audit/DATE.sql.gz.enc
```

Backups sind täglich zu erstellen, mindestens 30 Tage aufzubewahren und quartalsweise testweise in einer isolierten Umgebung wiederherzustellen. Ein Backup gilt erst nach erfolgreicher Entschlüsselung, Integritätsprüfung und Restore-Probe als gültig.

## Monitoring und Alarmierung

- `/actuator/health` liefert nur den öffentlichen Liveness-Status.
- Detaillierte Health-Daten sowie `/actuator/metrics` und `/actuator/info` sind auf den Management-Port beschränkt und benötigen Authentifizierung.
- Der Readiness-Indikator prüft Datenbankverbindung und Schreibbarkeit des Archivspeichers.
- Alarme müssen mindestens bei Readiness `DOWN`, wiederholten 5xx-/Login-Fehlern, fehlendem Backup und knappem Speicher ausgelöst werden.

## Notfallbetrieb

1. Incident eröffnen, Zeitpunkt, betroffene Scopes und letzte gültige Sicherung dokumentieren.
2. Bei Verdacht auf Datenabfluss Zugänge und externe Integrationen sperren; keine Daten löschen.
3. Schreibzugriffe kontrolliert stoppen und Datenbank-/Archivzustand sichern.
4. Wiederherstellung in einer isolierten Umgebung aus dem letzten geprüften Backup durchführen.
5. Integrität von Liquibase-Schema, Audit-Trail und Report-Archiven prüfen.
6. Erst nach Freigabe durch Betrieb und Fachverantwortliche wieder in Produktion schalten und alle Maßnahmen nachdokumentieren.
