# Pilotbetrieb

## Voraussetzungen

Der Pilot läuft ausschließlich mit separater Datenbank, separatem Archivverzeichnis und separaten Benutzer-/Backup-Secrets. Es werden keine Produktionsdaten ohne dokumentierte Rechtsgrundlage und Freigabe kopiert.

```bash
export SPRING_PROFILES_ACTIVE=pilot
export FINANCIAL_AUDIT_DB_USERNAME=...
export FINANCIAL_AUDIT_DB_PASSWORD=...
export FINANCIAL_AUDIT_TLS_KEYSTORE=...
export FINANCIAL_AUDIT_TLS_KEYSTORE_PASSWORD=...
export FINANCIAL_AUDIT_TLS_KEY_ALIAS=financial-audit-pilot
export FINANCIAL_AUDIT_REPORT_ARCHIVE_DIRECTORY=/secure/pilot/archive
```

Zusätzlich sind die Anmelde-Secrets des Pilotbetriebs erforderlich. Ohne sie schlägt der
Start fehl (Fail-Fast), damit der Pilot nicht mit erratbaren Zugangsdaten läuft:

```bash
export FINANCIAL_AUDIT_AUDITOR_PASSWORD_HASH='<bcrypt-hash>'
export FINANCIAL_AUDIT_LEAD_AUDITOR_PASSWORD_HASH='<bcrypt-hash>'
export FINANCIAL_AUDIT_ADMIN_PASSWORD_HASH='<bcrypt-hash>'
export FINANCIAL_AUDIT_DEFAULT_ADMIN_USERNAME=admin
export FINANCIAL_AUDIT_DEFAULT_ADMIN_PASSWORD='<sicheres Passwort>'
```

Bekannte Standardpassworte (`admin`, `password`, `changeme`, `changeit`) werden abgelehnt.
Details siehe `README.md`.

## Ablauf

1. Version und Liquibase-Changelog dokumentieren; Datenbank- und Archivbackup erstellen.
2. Anwendung mit dem Pilotprofil starten und `/actuator/health` prüfen.
3. Einen Test-Tenant und zwei Projekte anlegen; Benutzer nur einem Projekt zuordnen.
4. CSV-Import, Analyse, Finding, Prüfungshandlung, Workpaper, Review und Reportexport durchführen.
5. Mit einem Benutzer des ersten Projekts den Zugriff auf das zweite Projekt testen. Erwartung: `403` oder `404`, niemals Daten.
6. Report archivieren, SHA-256 und Manifest prüfen und Archivdatei wieder einlesen.
7. Backup entschlüsseln und Restore in einer isolierten Datenbank testen.
8. Fachliche Zahlen gegen den Referenzfall vergleichen und Audit-Trail prüfen.
9. Bei Erfolg Pilotbericht und Freigaben unterschreiben; bei Abweichung Betrieb stoppen, Beweise sichern und Incident eröffnen.

## Abbruchkriterien

Cross-Scope-Daten, fehlende Audit-Einträge, ungültige Prüfsumme, unverschlüsseltes Backup, Readiness `DOWN`, unklare Reportzahlen oder ein Testfehler führen zum Abbruch und NO-GO.

Fehlende Anmelde-Secrets sind vorab zu prüfen: startet die Anwendung wegen fehlender `FINANCIAL_AUDIT_*_PASSWORD_HASH`- oder `FINANCIAL_AUDIT_DEFAULT_ADMIN_PASSWORD`-Variablen nicht, ist das ein Konfigurationsfehler und kein fachliches Ergebnis. Erst nach korrekter Bereitstellung darf der Ablauf beginnen.

