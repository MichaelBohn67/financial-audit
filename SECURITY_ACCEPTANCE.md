# Sicherheitsprüfung und fachliche Abnahme

Stand: 2026-09-02

## Ergebnis

Der technische Abnahme-Gate ist aktuell **NO-GO**. Der Produktionsstart darf erst nach Beseitigung der unten genannten Testfehler und erneuter Prüfung erfolgen.

## Durchgeführte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| `mvn -DskipTests compile` | Bestanden |
| Backup-Skripte mit `bash -n` | Bestanden |
| Suche nach fest eingebetteten Produktionssecrets | Keine Treffer; Beispiele/Secret-Referenzen sind bewusst vorhanden |
| `mvn test` | Nicht bestanden: 232 Tests, 36 Failures, 2 Errors |
| Produktivdatenbank-/Produktiv-Pilotbetrieb | Nicht ausgeführt; kein freigegebenes Zielsystem angegeben |

Die Testfehler betreffen überwiegend bestehende Controller-Tests, die die inzwischen verpflichtenden Tenant-/Projektparameter und die scope-gefilterten Repository-Aufrufe noch nicht abbilden. Die zwei Fehler betreffen ebenfalls veraltete Tests. Diese Tests müssen vor der Abnahme aktualisiert oder durch gleichwertige Regressionstests ersetzt werden.

## Sicherheits-Gates vor Go

- Keine Klartext-Credentials in Repository, Logs oder Exporten.
- TLS-Keystore, Datenbankzugang, Backup-Passphrase und Benutzer-Hashes kommen aus dem Secret-Management.
- Scope-Crossing-Tests für Tenant, Projekt, Report, Sampling, Finding, Workpaper und Archiv bestehen.
- CSRF, Session-Cookie-Flags und nicht authentifizierte Management-Endpunkte sind geprüft.
- Vollständiger Testlauf: 0 Failures, 0 Errors.
- Liquibase-Upgrade auf einer Kopie der Pilotdatenbank ist erfolgreich.
- Backup erstellen, Prüfsumme/Entschlüsselung prüfen und Restore in isolierte Datenbank durchführen.
- Fachverantwortliche bestätigen Reportzahlen, Prüfungsakte, Audit-Trail und Archivartefakte anhand eines Referenzfalls.

## Abnahmeprotokoll

Freigabe erteilen erst nach Eintragung von Datum, Version, Prüfer, Testergebnis und fachlicher Signatur. Ein fehlgeschlagener Gate führt automatisch zu NO-GO und darf nicht durch manuelle Ausnahme übergangen werden.
