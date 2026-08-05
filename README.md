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

## Datenbankschema

- Liquibase Master-Changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Zusätzliches SQL-Skript im Root: `ddl_Booking.sql`