# financial_audit

`financial_audit` ist eine Spring-Boot-Anwendung zur Analyse und Prüfung von Finanztransaktionen.

## Tech-Stack

- Java 26
- Spring Boot 3.5.3
- Spring Web, Spring Security, Spring Data JPA, Thymeleaf
- Liquibase
- H2 (Standardprofil)
- MariaDB (Profil `mariadb`)
- Maven

## Audit-plan coverage

The application now includes active materiality configuration and booking evaluation, finding-to-workpaper links with remediation lifecycle tracking, canonical `AUDITOR`/`LEAD_AUDITOR`/`ADMIN` workflow authorization, and an authenticated dashboard at `/dashboard` (`/api/dashboard`). Monetary Unit Sampling is implemented with deterministic seeds and persisted sampling items.

Compliance note: materiality configuration and remediation lifecycle changes are written to the immutable audit-event table through `AuditTrailWriter`. Credentials are never included in these business snapshots. Generic persistence-wide automatic audit interception remains a follow-up hardening item.

## Voraussetzungen

- JDK 26
- Maven 3.9+

## Projekt starten

Im Projektverzeichnis ausführen:

```bash
mvn spring-boot:run
```

Damit startet die Anwendung standardmäßig mit einer H2-In-Memory-Datenbank.

## Mit MariaDB starten

Wenn eine lokale MariaDB verfügbar ist, kann das MariaDB-Profil verwendet werden:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mariadb
```

Die Profilwerte liegen in `src/main/resources/application-mariadb.properties`.

## Tests ausführen

```bash
mvn test
```

## Standard-Admin-Benutzer

Beim Start der Applikation wird automatisch ein Admin-Benutzer angelegt, sofern er noch
nicht existiert. Benutzername und Passwort müssen über Umgebungsvariablen bereitgestellt
werden:

```bash
export FINANCIAL_AUDIT_DEFAULT_ADMIN_USERNAME=admin
export FINANCIAL_AUDIT_DEFAULT_ADMIN_PASSWORD='<sicheres Passwort>'
```

Der Startvorgang schlägt fehl, wenn `FINANCIAL_AUDIT_DEFAULT_ADMIN_PASSWORD` nicht gesetzt
ist oder ein bekanntes Standardpasswort (`admin`, `password`, `changeme`, `changeit`)
verwendet wird. Damit kann die Anwendung nicht versehentlich mit erratbaren
Admin-Zugangsdaten betrieben werden.

Ebenfalls erforderlich sind die BCrypt-Hashes der Anmeldebenutzer:

```bash
export FINANCIAL_AUDIT_AUDITOR_PASSWORD_HASH='<bcrypt-hash>'
export FINANCIAL_AUDIT_LEAD_AUDITOR_PASSWORD_HASH='<bcrypt-hash>'
export FINANCIAL_AUDIT_ADMIN_PASSWORD_HASH='<bcrypt-hash>'
```

## Datenbankschema

- Liquibase Master-Changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Zusätzliches SQL-Skript im Root: `ddl_Booking.sql`
