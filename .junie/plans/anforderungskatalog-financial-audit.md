---
sessionId: session-260719-183245-1fo8
---

# Requirements

### Overview & Goals
Erweiterung des Anforderungskatalogs um einen **BaFin-orientierten regulatorischen Rahmen** für eine Financial-Audit-Software (mit Fokus auf beaufsichtigte Zahlungs-/E-Geld-Institute), einen strukturierten **Anti-Geldwäsche-(AML)-Prüfprozess für Buchungen** sowie ein verbindliches **TDD-Entwicklungsmodell (Test Driven Development) für die gesamte Software**.

### Scope
#### In Scope
- Ableitung eines BaFin-relevanten Anforderungskatalogs für Audit-, Kontroll- und AML-Fähigkeit.
- Zuordnung regulatorischer Themen zu umsetzbaren Produktanforderungen.
- Ergänzung bestehender Rollen/Prozesse um Compliance-, Nachweis- und Kontrollaspekte.
- Definition fachlicher AML-Anforderungen für Buchungsprüfung (Szenarien, Schwellenwerte, Trefferbearbeitung, Eskalation).
- Architekturleitlinie für **leicht integrierbare Fremdsystem-Schnittstellen** über ein einheitliches Pattern (Adapter/Port-Prinzip).
- Konkrete Anforderungen für **ausgehende REST-API-Integrationen** (Aufruf, Authentifizierung, Fehlerbehandlung, Nachvollziehbarkeit), inklusive externer Risiko-/Watchlist-Dienste.
- Verbindliche Verankerung von **TDD (Red-Green-Refactor)** als Entwicklungsstandard für alle fachlichen und technischen Features.

#### Out of Scope
- Rechtsverbindliche Auslegung einzelner Normen (Rechtsberatung).
- Finale juristische Freigabe durch Compliance/Legal.
- Vollständige CI/CD-Toolauswahl und Build-Infrastruktur-Implementierung (separater Umsetzungsstrang).

### Dokument: Anforderungskatalog – Teil 1 (Domäne & Prüfumfang)

#### 1. Vision & Ziele
Die Software unterstützt interne und externe Finanzprüfungen durch strukturierte Datenerfassung, nachvollziehbare Prüfschritte und standardisiertes Reporting.

**Zielbeiträge:**
- Reduktion manueller Prüfaufwände.
- Früheres Erkennen von Auffälligkeiten und Risiken.
- Revisionssichere Nachvollziehbarkeit von Entscheidungen.
- Einheitliche Berichtsgrundlage für Management und Auditoren.

#### 2. Stakeholder & Rollen

 Rolle | Hauptziele | Verantwortlichkeiten |
---|---|---|
 Auditor | Prüfungen effizient und vollständig durchführen | Daten filtern, Prüfergebnisse bewerten, Findings dokumentieren |
 Finance Manager | Transparenz über Risiken und Maßnahmen | Berichte auswerten, Freigaben/Follow-ups steuern |
 Admin | Sicherer und stabiler Betrieb | Nutzer-/Rollenverwaltung, Systemeinstellungen, Zugriffskontrolle |

#### 3. Fachlicher Auditprozess (Level-1)
1. **Datenaufnahme**: Import von Buchungs- und Stammdaten aus Quellsystemen.
2. **Datenvalidierung**: Formale und fachliche Plausibilitätsprüfungen.
3. **AML-Regelprüfung**: Anwendung von Geldwäsche-Indikatoren (z. B. ungewöhnliche Beträge, auffällige Frequenz, Hochrisiko-Länder, Smurfing-Muster).
4. **Audit- & Compliance-Prüfung**: Ergänzende fachliche und regulatorische Prüfregeln.
5. **Befundbearbeitung**: Bewertung, Kommentierung, Risikoklassifikation und Eskalation von Auffälligkeiten.
6. **Reporting**: Ergebnisaufbereitung für verschiedene Zielgruppen.
7. **Abschluss & Ablage**: Versionierte Archivierung der Prüfergebnisse.

#### 4. Scope-Grenzen (MVP vs. Ausbau)

**MVP (Must):**
- CSV-basierter Import.
- Grundlegende Validierungsregeln.
- Regelbasierte Markierung von Auffälligkeiten.
- Exportierbarer Auditbericht.
- Einfache Rollen (Auditor, Manager, Admin).

**Ausbaustufen (Should/Could):**
- Direkte ERP-/Drittsystem-Schnittstellen über standardisierte Adapter (z. B. REST-APIs, DMS, BI).
- Erweiterte Risiko-Scoring-Modelle.
- Kollaborative Bearbeitung und Aufgaben-Workflow.
- Dashboards mit Trend- und Benchmarkanalysen.

#### 5. BaFin-orientierte regulatorische Anforderungen (für den Katalog)
Hinweis: Die folgenden Punkte sind eine fachliche Arbeitsgrundlage und müssen projektspezifisch durch Compliance/Legal validiert werden.

- **Governance & Kontrollsystem**
  - Nachweisbares internes Kontrollsystem (IKS) mit klaren Verantwortlichkeiten.
  - Dokumentierte Richtlinien/Verfahren für prüfungsrelevante Prozesse.
  - Regelmäßige Wirksamkeitsprüfung von Kontrollen.

- **Risikomanagement**
  - Systematische Risikoidentifikation, -bewertung und -überwachung (inkl. operationeller/IT-Risiken).
  - Nachvollziehbare Verknüpfung zwischen Risiko, Kontrolle und Prüfregel.
  - Eskalations- und Maßnahmenverfolgung bei Auffälligkeiten.

- **Geldwäscheprävention (AML) für Buchungen**
  - Regelbasierte Erkennung potenzieller Geldwäsche-Muster auf Transaktionsebene.
  - Einstufung von Treffern über einen risikobasierten Score (niedrig/mittel/hoch) mit nachvollziehbaren Begründungen.
  - Bearbeitungsworkflow für Treffer inkl. Vier-Augen-Prinzip bei kritischen Fällen.
  - Dokumentationspflicht für Entscheidungen (False Positive, bestätigt, eskaliert) mit revisionssicherem Zeitstempel.

- **Informationssicherheit & IT-Governance**
  - Rollenbasierte Zugriffssteuerung (Least Privilege, Trennung kritischer Funktionen).
  - Vollständige, manipulationsgeschützte Protokollierung sicherheits- und prüfungsrelevanter Aktionen.
  - Schutzbedarfsgerechte Maßnahmen für Vertraulichkeit, Integrität, Verfügbarkeit.

- **Auslagerung / Drittparteien / Schnittstellen**
  - Nachvollziehbare Steuerung externer Dienstleister und angebundener Systeme.
  - Auditierbare Schnittstellen inkl. technischer und fachlicher Datenherkunft (Data Lineage).
  - Einheitliches Port-Adapter-Pattern, damit neue Fremdsysteme kontrolliert integrierbar sind.
  - Ausgehende REST-Aufrufe zu Fremdsystemen mit standardisiertem Sicherheits- und Protokollierungsprofil.

- **Datenhaltung, Nachvollziehbarkeit, Aufbewahrung**
  - Revisionssichere Historisierung von Befunden, Entscheidungen und Änderungen (Audit Trail).
  - Unveränderbare oder nachweisbar versionierte Berichtsstände.
  - Regelkonforme Aufbewahrungs- und Löschkonzepte.

- **Meldewesen / Vorfallmanagement / Prüfungsfähigkeit**
  - Strukturierte Erfassung und Bearbeitung kritischer Vorfälle.
  - Nachweisführung für interne/externe Prüfungen und Aufsichtsanfragen.
  - Exportfähige Berichte für Management, Revision und Aufsicht.

#### 6. Abnahmekriterien (aktualisiert)
- BaFin-relevante Themenblöcke sind vollständig in Produktanforderungen überführt.
- Jede regulatorische Anforderung besitzt Akzeptanzkriterien und Verantwortlichkeit.
- Der Auditprozess ist fachlich durchgängig und revisionsfähig beschrieben.
- Das Integrationsprinzip erlaubt neue Drittsysteme ohne Änderung der Kernlogik.
- Für jede neue Fachanforderung ist der zugehörige Test vor der Implementierung spezifiziert (TDD-Nachweis).
- Unit-, Integrations- und API-Tests sind als Qualitätsgates je Featurekategorie definiert.
- Das Dokument ist direkt als Grundlage für Schritt 2 und 3 nutzbar.

#### 7. Entwicklungsprinzipien (TDD verpflichtend)
- **Red-Green-Refactor als Standardablauf:**
  - Red: Ein fehlschlagender Test beschreibt zuerst das gewünschte Verhalten.
  - Green: Minimaler Code implementiert die Anforderung.
  - Refactor: Verbesserung von Struktur/Lesbarkeit bei konstant grünem Teststatus.
- **Testarten nach Architekturgrenze:**
  - Domänenlogik (z. B. AML-Regeln, Risikobewertung): primär Unit-Tests.
  - Port-Adapter-Integration (z. B. REST-Adapter): Integrations-/Contract-Tests.
  - End-to-End Audit-Workflows: selektive API-/Systemtests für kritische Pfade.
- **Definition of Done pro Requirement:**
  - Fachlicher Testfall vorhanden und nachvollziehbar mit REQ-ID verknüpft.
  - Implementierung erfüllt alle Tests ohne manuelle Sonderpfade.
  - Refactoring-Schritt dokumentiert, sofern Strukturänderungen erfolgt sind.
- **Traceability:**
  - Jede Anforderung (REQ-*) referenziert mindestens einen automatisierten Testfall.
  - Testreports sind revisionsfähig versioniert und Auditierbarkeit entsprechend ablegbar.

# Technical Design

### Current Implementation
Aktueller Projektstand ist ein **Spring-Boot-Skeleton**:
- `pom.xml` (Zeilen 1–43): `spring-boot-starter-parent` 3.5.3, Dependencies `spring-boot-starter-web` und `spring-boot-starter-test`, Packaging via `spring-boot-maven-plugin`.
- `src/main/java/de/bohnottensen/Main.java` (Zeilen 1–12): einzige Startklasse mit `@SpringBootApplication`.
- `src/main/resources/application.properties` (Zeile 1): nur `spring.application.name=financial_audit`.

### Key Decisions
- Der Katalog wird um einen **BaFin-orientierten Compliance-Teil** erweitert (Governance, Risiko, Sicherheit, Auslagerung, Nachweisfähigkeit).
- AML wird als eigener fachlicher Strang modelliert: **Erkennung → Bewertung → Fallbearbeitung → Eskalation → Nachweis**.
- Anforderungen bleiben in Markdown als eigenständiges Artefakt, um sie in Tickets/Controls/Testfälle überführen zu können.
- Für externe Anbindungen wird ein **Port-Adapter-Pattern** als Standard festgelegt: fachliche Kernprozesse bleiben systemunabhängig, konkrete Drittsoftware wird über austauschbare Adapter angebunden.
- Jede regulatorische Anforderung wird als REQ-Eintrag mit Verantwortlichkeit und prüfbaren Akzeptanzkriterien modelliert.
- **TDD ist für alle Entwicklungsstränge verpflichtend**: Implementierung startet erst nach fehlschlagendem Test (Red), anschließend minimale Implementierung (Green), dann Refactoring.
- Teststrategie folgt einer risikoorientierten Pyramide: viele Unit-Tests für Domänenregeln, gezielte Integrations-/Contract-Tests für Schnittstellen, wenige kritische End-to-End-Tests.

### Proposed Changes
- Ergänzung des Dokuments um dedizierte Abschnitte **„BaFin-orientierte regulatorische Anforderungen“**, **„AML-Prüfung von Buchungen“** und **„TDD-Qualitätsvorgaben“**.
- Übersetzung der regulatorischen Themen in konkrete Requirement-Klassen:
  - GOV-* (Governance/IKS)
  - RSK-* (Risikomanagement)
  - AML-* (Geldwäscheprüfung)
  - SEC-* (Informationssicherheit)
  - INT-* (Drittparteien/Schnittstellen)
  - AUD-* (Audit-Trail/Nachweis/Reporting)
  - QLT-* (Test-/Qualitätsanforderungen im TDD-Stil)
- Fachlicher AML-Zielprozess wird als umsetzbare Kette spezifiziert:
  - Buchung normalisieren und risikorelevante Merkmale extrahieren.
  - AML-Regeln anwenden (Schwellenwert, Muster, Länder-/Konto-Risiko, Häufigkeit).
  - Treffer mit Risikoscore bewerten und in Fallklassen überführen.
  - Kritische Treffer zur manuellen Eskalation bereitstellen.
- Schärfung des Integrationskapitels für ausgehende REST-Kommunikation über Port-Adapter-Leitplanken:
  - Kernsystem definiert stabile fachliche Ports (z. B. `TransactionSourcePort`, `AmlScreeningPort`, `ReportExportPort`, `ExternalRiskApiPort`).
  - Je Fremdsystem separater Adapter (z. B. `SapImportAdapter`, `DatevImportAdapter`, `ExternalRiskRestAdapter`, `WatchlistRestAdapter`).
  - Technische REST-Standards je Adapter: Auth (mind. OAuth2/API-Key je Zielsystem), Timeouts, Retry mit Backoff, Idempotenz-Strategie, strukturierte Fehlerklassen.
  - Onboarding neuer Systeme ohne Änderung der Kernprozesse.
- Ergänzung um konkrete Integrationsanforderungen für Monitoring und Auditierbarkeit:
  - Request-/Response-Metadaten (ohne sensible Payload) werden revisionsfähig protokolliert.
  - Korrelation über Trace-/Correlation-ID pro externem Aufruf.
- Einführung eines TDD-Umsetzungsrahmens für spätere Spring-Boot-Implementierung:
  - Testfälle werden vor der Fachimplementierung je REQ-ID angelegt.
  - Namenskonventionen koppeln Testklassen an Domänenbausteine (`*ServiceTest`, `*AdapterIT`, `*ControllerIT`).
  - Für Ports/Adapter werden Contract-Tests als Integrationsbarriere etabliert.
  - Regressionstests für AML-Regeln sind Pflicht bei jeder Regeländerung.
- Vorbereitung von Schritt 2/3 durch regulatorisch rückverfolgbare Akzeptanzkriterien und testbare Qualitätsgates.

### Data Models / Contracts
Template für die nächsten Schritte:

```text
REQ-<Bereich>-<Nummer>
Titel
Beschreibung
Rolle/Stakeholder
Priorität (Must/Should/Could)
Akzeptanzkriterien
Testfall-ID(s) / Testtyp (Unit|Integration|API)
Abhängigkeiten
Schnittstellen-Port (optional)
Adapter-Strategie (optional)
```

Beispiel für Integrationsanforderung:

```text
REQ-INT-001
Standardisierter Datenimport über Source-Port
Neue Quellsysteme können über Adapter angebunden werden, ohne die Audit-Kernlogik zu ändern.
Auditor/Admin
Should
Wenn ein neuer Adapter registriert wird, kann das System denselben Import-Use-Case unverändert nutzen.
Abhängig von REQ-IMP-001
Schnittstellen-Port: TransactionSourcePort
Adapter-Strategie: Port-Adapter
```

```text
REQ-INT-002
Ausgehende REST-API-Anbindung externer Systeme
Das System kann externe REST-Endpunkte über einen dedizierten Port aufrufen und Antworten für Auditprozesse verarbeiten.
Auditor/Admin
Must
Bei erfolgreichem Aufruf werden fachliche Daten verarbeitet; bei Fehlern werden klassifizierte Fehlerereignisse mit Korrelations-ID protokolliert.
Abhängig von REQ-SEC-001, REQ-AUD-001
Schnittstellen-Port: ExternalRiskApiPort
Adapter-Strategie: Port-Adapter (REST)
```

### File Structure
Keine Änderungen an `src/main` erforderlich.
Das Dokument wird im bestehenden Plan-Artefakt geführt.

# Delivery Steps

###   Step 1: TDD-Qualitätsrahmen für den gesamten Anforderungskatalog definieren
Ein verbindlicher TDD-Rahmen ist als übergreifender Qualitätsstandard im Katalog verankert.
- Ergänze QLT-Requirements für Red-Green-Refactor, Definition of Done und Testpflicht pro REQ-ID.
- Definiere Testtypen je Anforderungskategorie (Unit für Domäne, Integration/Contract für Adapter, API für kritische End-to-End-Flows).
- Dokumentiere Traceability-Regeln zwischen Anforderungen, Testfällen und revisionssicherer Nachweisablage.

###   Step 2: AML- und Compliance-Anforderungen TDD-fähig spezifizieren
AML- und regulatorische Anforderungen sind so beschrieben, dass sie direkt testgetrieben implementiert werden können.
- Überführe AML-/BaFin-Anforderungen in testbare Akzeptanzkriterien mit klaren Given-When-Then-Erwartungen.
- Ergänze pro Requirement Testfall-Referenzen und Prioritäten (Must/Should/Could) für inkrementelle Umsetzung.
- Verankere Vier-Augen-Prinzip, Eskalationspfade und Audit-Trail-Nachweise als explizit prüfbare Testziele.

###   Step 3: TDD-orientierte Implementierungsvorbereitung für Spring Boot und Integrationen strukturieren
Die spätere technische Umsetzung ist in TDD-konforme Entwicklungsscheiben für Kernlogik und Schnittstellen aufgeteilt.
- Definiere Umsetzungssicht für spätere Spring-Boot-Komponenten (Controller/Service/Repository + Port/Adapter) inklusive zugehöriger Testebenen.
- Lege Integrationsqualitätsregeln für ausgehende REST-Adapter fest (Contract-Tests, Fehlerklassen, Retry/Timeout-Verhalten, Korrelations-ID).
- Strukturiere ein priorisiertes Umsetzungsbacklog, in dem jeder Arbeitsschritt mit einem zuerst zu implementierenden Test startet.