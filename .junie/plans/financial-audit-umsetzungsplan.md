---
sessionId: session-260721-160148-hwpd
---

# Requirements

### Overview & Goals
Die Software soll Finanztransaktionen erfassen, validieren, auf Compliance-/AML-Risiken prüfen und prüfbare Findings für Auditor:innen bereitstellen.

### Scope
**In Scope**
- Transaktionsimport (CSV und erweiterbar über Ports)
- Fachliche Validierung von Buchungen
- Regelbasierte AML-Prüfung mit deterministischem Ergebnis
- Persistenz von Buchungen und Findings
- Auditor-freundliche Web-Oberfläche für Erfassung und Sichtung

**Out of Scope**
- Externe Watchlist-/Sanktionslisten-Integrationen
- Vollständiger Eskalations-/Case-Management-Workflow (z. B. Closure-Prozesse)
- Nicht-deterministische ML-Scoring-Logik

### User Stories
- Als Auditor:in möchte ich Buchungen zentral sehen, damit ich verdächtige Aktivitäten schnell prüfen kann.
- Als Compliance-Team möchte ich reproduzierbare AML-Regelprüfungen, damit Entscheidungen nachvollziehbar sind.
- Als Operations-Team möchte ich Transaktionen aus Dateien importieren, damit manuelle Eingaben reduziert werden.

### Functional Requirements
- Buchungen müssen mit Pflichtfeldern gespeichert werden (Beschreibung, Betrag, Währung, Konten, Zeitstempel).
- Importierte Buchungen müssen vor Verarbeitung validiert werden.
- AML-Engine muss mehrere Regeln pro Buchung ausführen und mehrere Alerts liefern können.
- Aus Alerts müssen strukturierte Findings mit `ruleName`, `alertDescription`, `riskLevel`, `status` gespeichert werden.
- UI muss Listen-/Erfassungs-/Bearbeitungsflows für Buchungen bereitstellen und später Findings sichtbar machen.

### Non-Functional Requirements
- Deterministisches Verhalten: gleiche Eingaben liefern gleiche Alerts/Risiken.
- Erweiterbarkeit über Ports/Adapter (z. B. weitere Importquellen).
- Testbarkeit auf Unit- und Integrationsniveau mit JUnit 5 + AssertJ.

# Technical Design

### Current Implementation
- **Stack**: Spring Boot 3.5.3, Spring Web, Thymeleaf, Spring Data JPA, H2 (`pom.xml`).
- **Startpunkt**: `src/main/java/de/bohnottensen/financialaudit/Main.java`.
- **Domäne**:
  - `Booking` als JPA-Entity mit Pflichtfeldern (`domain/model/Booking.java`).
  - `Finding` als JPA-Entity mit `ruleName`, `alertDescription`, `riskLevel`, `status` (`domain/model/Finding.java`).
  - `AmlRule` + `AmlEngine` vorhanden, aktuell Rückgabe `List<String>` (`domain/model/AmlRule.java`, `AmlEngine.java`).
  - Erste konkrete Regel `HighAmountRule` (`domain/model/HighAmountRule.java`).
  - `BookingValidator` für Basis-Validierung (`domain/model/BookingValidator.java`).
- **Infrastruktur**:
  - CSV-Import via Port/Adapter (`application/ports/TransactionSourcePort.java`, `infrastructure/adapter/CsvImportAdapter.java`).
  - Persistenz via Spring Data Repositories (`infrastructure/persistence/*Repository.java`).
  - Web-Controller für Buchungen (`infrastructure/web/BookingWebController.java`).
- **Tests/Pattern**:
  - AML-Engine-Tests für High-Amount-Szenario (`src/test/.../AmlEngineTest.java`).
  - Validator- und MVC-Tests (`BookingValidatorTest.java`, `BookingWebControllerTest.java`).

### Key Decisions
- Bestehende Schichtung beibehalten: **domain.model** (Regeln/Modelle), **application** (Orchestrierung), **infrastructure** (Adapter/Persistenz/Web).
- AML von String-Alerts auf strukturierte Alerts/Findings weiterentwickeln, um Risiko-Scoring robust abzubilden.
- Risikologik explizit regelbasiert (keine Heuristik/Randomness), damit Auditierbarkeit gewährleistet ist.

### Proposed Changes
1. **Fachlicher Kern erweitern**
   - Einführung eines strukturierten Alert-Modells (z. B. `AmlAlert`) im Domain-Layer.
   - `AmlRule`/`AmlEngine` so erweitern, dass mehrere strukturierte Alerts pro Buchung unterstützt werden.
   - Zusätzliche AML-Regeln ergänzen (z. B. Strukturierung, hohe Frequenz), analog zum bestehenden `HighAmountRule`-Pattern.

2. **Anwendungs-Orchestrierung ergänzen**
   - Neuer Application Service (z. B. `AmlAuditService`) koordiniert:
     - Import/Entgegennahme von Buchungen
     - Validierung (`BookingValidator`)
     - AML-Check (`AmlEngine`)
     - Mapping auf `Finding` + Persistenz (`FindingRepository`)

3. **Web- und Persistenzfluss vervollständigen**
   - Bestehenden `BookingWebController` um Trigger/Flow zur AML-Prüfung erweitern (oder dedizierten Controller ergänzen).
   - Findings in UI bereitstellen (Liste/Detailfilter nach Risiko/Status).

### Data Models / Contracts
- Bestehende `Finding`-Felder nutzen als Persistenzziel:
  - `ruleName`, `alertDescription`, `riskLevel`, `status`.
- Port bleibt erweiterbar:
  - `TransactionSourcePort#importTransactions(Object source)` als Import-Einstieg.

### Components
- **Betroffen (bestehend)**:
  - `domain/model/AmlEngine.java`
  - `domain/model/AmlRule.java`
  - `domain/model/HighAmountRule.java`
  - `domain/model/BookingValidator.java`
  - `domain/model/Finding.java`
  - `infrastructure/web/BookingWebController.java`
  - `infrastructure/persistence/FindingRepository.java`
- **Neu (geplant)**:
  - Application-Orchestrator-Service für Audit-Pipeline
  - Weitere Rule-Klassen im Domain-Bereich
  - Optional: Findings-Webcontroller/View

### Architecture Diagram
```mermaid
graph LR
  A[CSV oder UI Booking Input] --> B[BookingValidator]
  B --> C[AmlEngine mit AmlRule]
  C --> D[Risk Mapping]
  D --> E[FindingRepository]
  E --> F[Auditor UI Findings]
```

### Risks
- Bruch bestehender Tests bei API-Änderung von `AmlEngine` (String -> strukturierte Alerts).
- Datenqualität aus CSV (fehlende/ungültige Spalten) kann zu fehlerhaften Findings führen.
- Gefahr von Scope-Creep in Richtung Case-Management; deshalb klare Begrenzung auf Erkennung + Finding-Erzeugung.

# Testing

### Validation Approach
- Vorhandene Unit-/Integrationstests als Basis erhalten und gezielt erweitern.
- Für jede neue Regel deterministische Positiv-/Negativfälle.
- Für Orchestrierungsfluss End-to-End-nahe Service-Tests mit in-memory Persistenz.

### Key Scenarios
- `AmlEngine` erkennt High-Amount weiterhin korrekt (bestehende Erwartung aus `AmlEngineTest`).
- Neue Regeln liefern bei passenden Buchungen erwartete Alerts und bei unkritischen keine.
- `AmlAuditService` erzeugt für Alerts korrekt gemappte `Finding`-Einträge inkl. `riskLevel` und Default-`status`.
- Web-Flow speichert Buchung und stellt erzeugte Findings abrufbar dar.

### Edge Cases
- Leere Regel-Liste in `AmlEngine`.
- Buchungen mit fehlenden Pflichtfeldern (Validator-Fehler statt Crash).
- Mehrere Alerts aus mehreren Regeln für dieselbe Buchung.
- Unbekannte Regelzuordnung im Risk-Mapping -> deterministischer Fallback (LOW).

# Delivery Steps

###   Step 1: Domain-Kern für AML-Erkennung stabilisieren und erweitern
Ergebnis: Die Domäne kann mehrere deterministische AML-Alerts pro Buchung strukturiert erzeugen.
- `AmlRule`, `AmlEngine` und bestehende Regelimplementierungen im Package `domain.model` auf ein strukturiertes Alert-Modell ausrichten.
- Zusätzliche AML-Regeln nach dem Muster von `HighAmountRule` ergänzen (klarer Schwellwert-/Fensterbezug, keine Seiteneffekte).
- Kompatibilität der bestehenden High-Amount-Logik sicherstellen, sodass aktuelle Fachsemantik erhalten bleibt.

###   Step 2: Audit-Orchestrierung im Application-Layer einführen
Ergebnis: Ein zentraler Service verarbeitet Buchungen von Validierung bis persistiertem Finding.
- Neuen Service im `application`-Layer einplanen, der `BookingValidator`, `AmlEngine` und `FindingRepository` orchestriert.
- Regel-Alerts in `Finding`-Objekte mappen (`ruleName`, `alertDescription`, `riskLevel`, `status`) und persistieren.
- Fehler-/Ungültigkeitsfälle deterministisch behandeln (validierungsbasierter Abbruch statt inkonsistenter Speicherung).

###   Step 3: Web- und Datenfluss für Auditor-Nutzung vervollständigen
Ergebnis: Auditor:innen können AML-relevante Ergebnisse über die bestehende Webschicht nutzen.
- `BookingWebController` bzw. dedizierte Web-Komponente so erweitern, dass AML-Prüfung im Buchungsfluss ausführbar ist.
- Finding-Ausgabe in der Webschicht ergänzen (Liste/Filter nach Risiko/Status) unter Nutzung der bestehenden Repository-Struktur.
- An bestehende MVC-Patterns aus `BookingWebController` und dessen Tests anschließen, um konsistente Bedienbarkeit sicherzustellen.

###   Step 4: Testabdeckung für neue Audit-Fähigkeiten erweitern
Ergebnis: Die neuen AML- und Audit-Flows sind durch reproduzierbare Unit- und Integrationstests abgesichert.
- `AmlEngineTest` um Multi-Rule-, Multi-Alert- und Negativfälle erweitern.
- Neue Tests für Orchestrierungsservice (Validierung, Mapping, Persistenz) ergänzen.
- Web-Tests nach Vorbild `BookingWebControllerTest` um AML/Finding-bezogene Nutzerflüsse erweitern.