---
sessionId: session-260803-111533-1rl9
---

# Requirements

### Overview & Goals
Die bestehende Spring-Boot-Anwendung soll von einem technischen Prototypen zu einer prüfungsfähigen Audit-Plattform weiterentwickelt werden, die die geforderten Kernbereiche (Import/Analytik, Sampling, Arbeitspapiere/Workflow, Compliance/Sicherheit, Reporting/Export) belastbar abdeckt.

### Scope
#### In Scope
- Ausbau der Importstrecke von `TransactionSourcePort` über zusätzliche Adapter (ERP-/Dateiformate) inkl. Qualitätsprüfungen.
- Aufbau einer regel- und statistikbasierten Prüfungsanalyse (inkl. Benford und Mustererkennung) auf Basis von `Booking`/`Finding`.
- Einführung eines Moduls für Risikobeurteilung, Wesentlichkeit und Stichprobenverfahren (MUS, Zufall, Schichtung).
- Einführung digitaler Arbeitspapiere, Aufgabenstatus und rollenbasierter Freigabe-Workflows.
- Vollständiger Audit-Trail, RBAC, Verschlüsselung und DSGVO-relevante Protokollierung.
- Berichtsgenerierung und strukturierte Exporte (inkl. XBRL/ESEF-Schnittstelle).

#### Out of Scope
- Zertifizierung durch externe Prüforgane (nur technische Voraussetzungen im System).
- Vollständige Produktivintegration zu allen ERP-Anbietern in einem Schritt (zuerst standardisierte Adapter-/Connector-Architektur).

### Functional Requirements
- Jeder Importlauf muss Quelle, Zeitpunkt, Datensatzanzahl, Validierungsfehler und Prüfsummen nachvollziehbar speichern.
- Analysen müssen reproduzierbar sein (Regelversion, Parameter, Laufkontext).
- Sampling muss aus Prüfungsparametern deterministisch dokumentierbar erzeugt werden.
- Jede fachliche Änderung (Buchung, Finding, Arbeitspapier, Freigabe) muss im Audit-Trail mit `wer/was/wann/vorher/nachher` landen.
- Rollen müssen mindestens `Assistant`, `SeniorAuditor`, `Wirtschaftspruefer`, `Admin` abbilden.
- Reporting muss versionierte Vorlagen und exportierbare Ergebnisartefakte erzeugen.

### Non-Functional Requirements
- Nachvollziehbarkeit: vollständige Historisierung und unveränderbare Ereigniskette.
- Sicherheit: Verschlüsselung at-rest und in-transit, harte Authentisierung/Autorisierung.
- Erweiterbarkeit: neue Importquellen/Prüfregeln ohne Controller-Umbau ergänzbar.
- Performance: Massendatenanalysen sollen asynchron und batchfähig laufen.

# Technical Design

### Current Implementation
- Architektur ist aktuell stark `infrastructure`-zentriert: Controller greifen direkt auf JPA-Repositories zu (z. B. `infrastructure/web/DomainModelApiController.java`, `BookingWebController.java`, `OpenBankingApiController.java`).
- Importabstraktion ist vorhanden: `application/ports/TransactionSourcePort.java` mit Implementierungen `infrastructure/adapter/CsvImportAdapter.java` und `OpenBankingApiClientAdapter.java`.
- Datenbasis vorhanden: Kernentitäten (`domain/model/Booking.java`, `Finding.java`, `Account*.java`) und Liquibase-Changelogs (`src/main/resources/db/changelog/**`).
- Analyse-Startpunkt vorhanden: `AmlEngine`, `AmlRule`, `HighAmountRule`, aktuell ohne prüfungsstandardspezifische Verfahren.
- Sicherheit/Compliance fehlen weitgehend: kein RBAC, kein vollständiges Änderungsjournal, kein Workflow-Modell.

### Key Decisions
- **Hexagonal weiterführen:** `TransactionSourcePort` als Muster für weitere fachliche Ports (Sampling, Reporting, AuditTrailWriter).
- **Application-Service-Schicht einführen:** Controller delegieren an Use-Case-Services statt direkter Repository-Nutzung.
- **Event-basierter Audit-Trail:** fachliche Änderungen erzeugen immutable Audit-Events plus Snapshots.
- **Regel- und Statistikpipeline trennen:** deterministische Regelchecks (`AmlRule`-artig) + statistische Jobs (Benford, Zeitmuster, Schwellennahe Umbuchungen).
- **Liquibase-first Datenmodellierung:** alle neuen Compliance-Objekte ausschließlich über neue Changelogs.

### Proposed Changes
- Neue Module/Packages ergänzen:
  - `application/usecase/importing` (Import-Orchestrierung, Validierungs- und Vollständigkeitschecks)
  - `application/usecase/analytics` (Regel-/Statistikläufe)
  - `application/usecase/sampling` (MUS, Zufall, Schichtung)
  - `application/usecase/workpaper` (Arbeitspapiere, Aufgaben, Review)
  - `application/usecase/reporting` (Berichte, Exporte)
  - `infrastructure/security` (AuthN/AuthZ, Rollen, Policy-Enforcement)
  - `infrastructure/audit` (Audit-Trail Persistenz, Event-Store)
- Controller-Refactoring:
  - `DomainModelApiController` und `BookingWebController` schrittweise von direktem Repository-Zugriff auf Services umstellen.
  - Import-Endpunkte für mehrere Quellen als einheitliche Job-API gestalten.
- Analyseausbau:
  - `AmlRule`-Konzept für zusätzliche Regeln erweitern (Zeitfenster, Nutzer-/Schwellenregeln).
  - Statistikkomponenten (Benford, Duplikat-/Lückenprüfung) als eigene Services mit Ergebnispersistenz in `Finding`-nahen Tabellen.
- Compliance-Datenmodell (Liquibase):
  - Tabellen für `audit_events`, `roles`, `permissions`, `user_role`, `workpapers`, `review_actions`, `sampling_runs`, `materiality_configs`, `report_runs`.
- Security:
  - Spring-Security-Konfiguration mit rollenbasierten Zugriffsregeln auf Mandant/Projekt/Dokument.
  - TLS erzwungen, Datenbank-/Feldverschlüsselung für sensible Felder.

### File Structure
- Bestehend, wird erweitert:
  - `src/main/java/de/bohnottensen/financialaudit/application/ports/TransactionSourcePort.java`
  - `src/main/java/de/bohnottensen/financialaudit/infrastructure/adapter/CsvImportAdapter.java`
  - `src/main/java/de/bohnottensen/financialaudit/infrastructure/adapter/OpenBankingApiClientAdapter.java`
  - `src/main/java/de/bohnottensen/financialaudit/infrastructure/web/DomainModelApiController.java`
  - `src/main/java/de/bohnottensen/financialaudit/infrastructure/web/BookingWebController.java`
  - `src/main/resources/db/changelog/db.changelog-master.yaml`
- Neu vorgesehen:
  - `src/main/java/.../application/usecase/**`
  - `src/main/java/.../infrastructure/security/**`
  - `src/main/java/.../infrastructure/audit/**`
  - `src/main/resources/db/changelog/changes/00x-*.yaml`

### Risks
- Große Domänenausweitung kann ohne klare Priorisierung zu langen Lieferzeiten führen.
- Direkte Controller-Repository-Kopplung erschwert Compliance-Policies; daher zuerst Service-Layer einziehen.
- Statistikverfahren benötigen Datenqualität; ohne robuste Importvalidierung entstehen False Positives.

# Testing

### Validation Approach
- API-/Service-Tests pro Compliance-Baustein mit reproduzierbaren Fixtures.
- Integrationsprüfungen gegen MariaDB + Liquibase-Migrationen für neue Compliance-Tabellen.
- Nachweisprüfungen für Audit-Trail-Vollständigkeit (`wer/was/wann/vorher/nachher`) bei allen kritischen Workflows.

### Key Scenarios
- Multi-Format-Import erzeugt vollständiges Importprotokoll und erkennt Duplikate/Lücken.
- Analysepipeline erzeugt nachvollziehbare Findings für Benford- und Musterregeln.
- Sampling-Läufe liefern dokumentierte Population, Seed/Parameter und selektierte Stichprobe.
- Review-Workflow erzwingt Rollenfreigaben (Assistant → SeniorAuditor → Wirtschaftspruefer).
- Berichtslauf erzeugt Vorlagenbericht inkl. exportierbarer Strukturdaten.

### Edge Cases
- Teilfehler im Import (Datei ok, einzelne Zeilen fehlerhaft) mit sauberem Fehlerreport ohne Datenverlust.
- Gleichzeitige Bearbeitung desselben Arbeitspapiers mit konsistenter Historie.
- Unautorisierter Zugriff auf Mandanten-/Projektartefakte wird strikt blockiert.
- Re-Run derselben Analyse mit gleicher Konfiguration liefert identische Ergebnisse (Determinismus).

# Atomic Task Breakdown

Die Roadmap wurde in kleine, atomare Umsetzungsaufgaben zerlegt, damit jede Aufgabe einen klaren Lieferumfang und eine einfache Priorisierung hat.

1. [x] Definiere den initialen Compliance-Umfang und erstelle eine Traceability-Matrix für Import, Analyse, Sampling, Workflow, Security und Reporting.
2. [x] Ergänze die Liquibase-Changelog-Struktur um die ersten Compliance-Migrationen für die Basis-Tabellen.
3. [x] Erstelle die Domain-Entitäten für Audit-Events und zugehörige Metadaten.
4. [x] Implementiere einen Audit-Trail-Writer für kritische Schreiboperationen von Buchungen, Findings und Arbeitspapieren.
5. [x] Ergänze Rollen- und Berechtigungsmodelle inklusive Seed-Daten für die Mindestrollen.
6. [x] Konfiguriere Spring Security mit rollenbasierten Zugriffregeln für Mandant-, Projekt- und Dokumentebene.
7. [x] Refaktoriere den Import-Controller so, dass er an einen Import-Use-Case-Service delegiert.
8. [x] Implementiere Import-Job-Tracking mit Status, Laufkontext und Protokollpersistenz.
9. [x] Ergänze CSV-Importvalidierungen für Vollständigkeit, Konsistenz, Duplikate und Lücken in Belegnummern.
10. Ergänze OpenBanking-Importvalidierungen für Vollständigkeit, Konsistenz und Datenqualitätsfehler.
11. Implementiere ein regelbasiertes Analytics-Modul für Zeitfenster-, Schwellen- und Musterregeln.
12. Implementiere eine separate Benford-Analyse mit Ergebnispersistenz in findingsnahen Tabellen.
13. Implementiere eine Muster-/Duplikat-/Lückenanalyse mit nachvollziehbarer Ergebnisstruktur.
14. Baue eine Sampling-Engine für deterministische MUS-Stichproben auf.
15. Ergänze Zufalls- und geschichtete Sampling-Strategien mit dokumentierten Parametern und Seeds.
16. Modelliere Workpapers, Aufgabenstatus und Review-Aktionen inklusive Zustandsübergängen.
17. Implementiere einen rollenbasierten Review-Workflow von Assistant über SeniorAuditor bis Wirtschaftspruefer.
18. Ergänze Report-Template-Versionierung und Report-Run-Persistenz.
19. Implementiere Reporting- und Export-Use-Cases für strukturierte Berichte und Exportartefakte.
20. Ergänze Integrationstests für Import, Analyse, Sampling, Workflow und Audit-Trail-Szenarien.

## Traceability Matrix

### Task-1-Lieferartefakt: Initialer Compliance-Umfang

- **Import & Qualitätsprüfung:** Nachvollziehbare Importläufe je Quelle, inkl. Vollständigkeits-/Konsistenzfehler, Prüfsumme und Datensatzmetrik.
- **Analyse & Findings:** Deterministische Regel- und Statistikläufe mit versionierter Regelbasis und reproduzierbarem Laufkontext.
- **Sampling:** Dokumentierte, deterministische Stichprobenerzeugung (MUS, Zufall, Schichtung) mit Parametern, Seed und Population.
- **Workflow & Arbeitspapiere:** Digitale Arbeitspapiere mit Aufgabenstatus, Review-Aktionen und rollenbasierten Freigaben.
- **Security & Compliance:** RBAC für Mindestrollen, unveränderbarer Audit-Trail, revisionssichere Änderungsnachweise.
- **Reporting & Export:** Versionierte Berichtsvorlagen, Report-Runs und exportierbare Ergebnisartefakte (strukturiert/XBRL-ready).

| Requirement | Capability | Primary Objects / Endpoints | Gap / Folgetasks |
| --- | --- | --- | --- |
| Jeder Importlauf muss Quelle, Zeitpunkt, Datensatzanzahl, Validierungsfehler und Prüfsummen nachvollziehbar speichern. | Import & Qualitätsprüfung | `TransactionSourcePort`, `CsvImportAdapter`, `OpenBankingApiClientAdapter`, `ImportOrchestratorService` (neu), `import_jobs`/`audit_events` (neu) | Task 7, 8, 9, 10 |
| Analysen müssen reproduzierbar sein (Regelversion, Parameter, Laufkontext). | Analytics & Findings | `AmlEngine`, `AmlRule`, `Finding`, `BookingAnalysisService` (neu), findings-nahe Analyse-Tabellen (neu) | Task 11, 12, 13 |
| Sampling muss aus Prüfungsparametern deterministisch dokumentierbar erzeugt werden. | Sampling | `SamplingEngine` (neu), `SamplingRun` (neu), `materiality_configs` (neu), `sampling_runs` (neu) | Task 14, 15 |
| Jede fachliche Änderung muss im Audit-Trail mit wer/was/wann/vorher/nachher landen. | Audit & Workflow | `AuditTrailWriter` (neu), `AuditEvent` (neu), `workpapers` (neu), `review_actions` (neu) | Task 3, 4, 16, 17 |
| Rollen müssen mindestens Assistant, SeniorAuditor, Wirtschaftspruefer, Admin abbilden. | Security & RBAC | `Role` (neu), `Permission` (neu), `UserRole` (neu), `SecurityConfig` (neu) | Task 5, 6 |
| Reporting muss versionierte Vorlagen und exportierbare Ergebnisartefakte erzeugen. | Reporting & Export | `ReportService` (neu), `ReportRun` (neu), `report_runs` (neu), Report-Export-Endpoints (neu) | Task 18, 19 |

# Delivery Steps

### ✓ Step 1: Compliance-Zielbild und Domänenfundament definieren
Ein belastbares Compliance-Domänenmodell und priorisierte Umsetzungsreihenfolge sind festgelegt.
- Anforderungen 1–5 in umsetzbare Capability-Epics zerlegen (Import, Analyse, Sampling, Workflow, Security, Reporting).
- Für jede Capability konkrete Zielobjekte im Datenmodell definieren (z. B. `audit_events`, `sampling_runs`, `workpapers`).
- Liquibase-Roadmap im bestehenden Changelog-System (`db.changelog-master.yaml` + neue `changes/*.yaml`) in Releasescheiben planen.
- Traceability-Matrix erstellen: regulatorische Anforderung → Systemfunktion → betroffene Klassen/Endpunkte.

### * Step 2: Import- und Analyse-Pipeline auf Prüfungsstandard erweitern
Eine erweiterbare Pipeline importiert Massendaten und erzeugt reproduzierbare Prüfungsbefunde.
- `TransactionSourcePort`-Muster auf weitere Quellen/Dateiformate ausbauen und Importorchestrierung als Use-Case-Service einführen.
- Qualitätsprüfungen ergänzen (Vollständigkeit, Konsistenz, Duplikate, Belegnummernlücken) vor Persistierung.
- Bestehendes Regelmuster (`AmlEngine`/`AmlRule`) für forensische Regeln und Benford-/Musteranalysen erweitern.
- Findings-Persistenz strukturieren (Regelversion, Laufkontext, Schweregrad, Nachvollziehbarkeit).

###   Step 3: Risikobeurteilung, Sampling und digitale Arbeitspapiere implementieren
Risikologik, mathematische Stichproben und Review-fähige Arbeitspapiere sind funktional integriert.
- Application-Services für Wesentlichkeit, Nichtaufgriffsgrenzen und Risikoscoring aufsetzen.
- Sampling-Engine für MUS, Zufalls- und geschichtete Stichproben mit dokumentierten Parametern/Seeds implementieren.
- Workflow-Objekte für Arbeitspapiere, Aufgabenstatus, Nachweise und Freigabeschritte modellieren.
- Web/API-Schicht schrittweise von direktem Repository-Zugriff auf neue Use-Case-Services umstellen.

###   Step 4: Security, Audit-Trail und Reporting produktionsreif machen
Das System erzwingt Rollenrechte, protokolliert Änderungen revisionssicher und liefert Prüfungsberichte/Exporte.
- Spring-Security-basiertes Rollen-/Rechtekonzept für Mandant-, Projekt- und Dokumentebene einführen.
- Immutable Audit-Trail für alle kritischen Schreiboperationen mit `wer/was/wann/vorher/nachher` implementieren.
- Verschlüsselungskonzept für at-rest und in-transit umsetzen (inkl. Konfigurationshärtung in `application.properties`/Deployment).
- Reporting- und Export-Use-Cases für vorlagenbasierte Berichte sowie strukturierte Ausgaben (XBRL/ESEF-ready) bereitstellen.