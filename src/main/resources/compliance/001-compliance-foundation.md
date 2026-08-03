# Compliance Foundation (Step 1)

## 1) Capability Epics

| Capability | Epic ID | Ziel |
|---|---|---|
| Import | IMP-EP-01 | Einheitliche Import-Jobs über mehrere Quellen mit Qualitätsprüfung und Importprotokoll |
| Analyse | ANL-EP-01 | Reproduzierbare regel- und statistikbasierte Prüfungsanalysen inkl. Benford |
| Sampling | SMP-EP-01 | Deterministische Stichprobenläufe (MUS, Zufall, Schichtung) mit vollständiger Parametrierung |
| Workflow | WFP-EP-01 | Digitale Arbeitspapiere, Aufgaben- und Review-Workflow mit Freigabekette |
| Security | SEC-EP-01 | RBAC, unveränderbarer Audit-Trail, Verschlüsselungs- und DSGVO-Bausteine |
| Reporting | RPT-EP-01 | Versionierte Berichtsvorlagen und strukturierte Exporte (XBRL/ESEF-ready) |

## 2) Zielobjekte im Datenmodell

| Objekt | Zweck | Kernattribute |
|---|---|---|
| `import_runs` | Nachvollziehbarkeit jedes Imports | source_type, started_at, finished_at, record_count, checksum, status |
| `import_validation_errors` | Zeilen-/Feldfehler ohne Datenverlust | import_run_id, line_number, field_name, error_code, message |
| `analysis_runs` | Reproduzierbare Analysekontexte | run_type, rule_version, parameters_json, seed, started_at, finished_at |
| `analysis_findings` | Strukturierte Befundpersistenz | analysis_run_id, booking_id, severity, rule_key, explanation |
| `sampling_runs` | Dokumentierte Stichprobenläufe | method, parameters_json, seed, population_size, sample_size |
| `sampling_items` | Selektierte Stichprobenelemente | sampling_run_id, booking_id, selection_weight |
| `materiality_configs` | Wesentlichkeit und Grenzwerte | project_key, materiality, performance_materiality, trivial_threshold |
| `workpapers` | Digitale Arbeitspapiere | project_key, title, status, owner_user, due_date |
| `workpaper_evidence` | Verlinkte Nachweise | workpaper_id, evidence_type, reference_uri, uploaded_by |
| `review_actions` | Freigabe- und Reviewkette | workpaper_id, actor_user, actor_role, action, comment, occurred_at |
| `roles` | Rollenmodell | role_code, display_name |
| `permissions` | Rechtekatalog | permission_code, scope_type |
| `user_role` | Rollenzuweisung | user_id, role_id, scope_id |
| `audit_events` | Immutable Änderungsjournal | actor_user, entity_type, entity_id, action, before_json, after_json, occurred_at |
| `report_templates` | Versionierte Berichtsvorlagen | template_key, version, locale, content_ref |
| `report_runs` | Erzeugte Berichtsläufe | template_id, generated_at, generated_by, artifact_ref |

## 3) Liquibase-Roadmap (Release-Slices)

- Release A (Grundlage):
  - `007-create-import-runs-table.yaml`
  - `008-create-import-validation-errors-table.yaml`
  - `009-create-analysis-runs-table.yaml`
  - `010-create-analysis-findings-table.yaml`
- Release B (Sampling & Materiality):
  - `011-create-materiality-configs-table.yaml`
  - `012-create-sampling-runs-table.yaml`
  - `013-create-sampling-items-table.yaml`
- Release C (Workpaper & Review):
  - `014-create-workpapers-table.yaml`
  - `015-create-workpaper-evidence-table.yaml`
  - `016-create-review-actions-table.yaml`
- Release D (Security & Audit):
  - `017-create-roles-table.yaml`
  - `018-create-permissions-table.yaml`
  - `019-create-user-role-table.yaml`
  - `020-create-audit-events-table.yaml`
- Release E (Reporting):
  - `021-create-report-templates-table.yaml`
  - `022-create-report-runs-table.yaml`

## 4) Traceability-Matrix

| Regulatorische Anforderung | Systemfunktion | Betroffene Klassen/Endpunkte (Ist + Ziel) |
|---|---|---|
| Datenimport Multi-Format | Einheitlicher Import-Use-Case + Adapter | `TransactionSourcePort`, `CsvImportAdapter`, `OpenBankingApiClientAdapter`, `OpenBankingApiController` |
| Vollständigkeit/Konsistenz | Validierung vor Persistierung + Fehlerprotokoll | `BookingValidator`, `Booking`, `BookingWebController`, neues `application.usecase.importing` |
| Forensische Analysen/Benford | Regel- und Statistikpipeline | `AmlEngine`, `AmlRule`, `HighAmountRule`, `Finding`, neues `application.usecase.analytics` |
| Sampling (MUS/Zufall/Schichtung) | Deterministische Sampling-Engine | neues `application.usecase.sampling`, neue `sampling_runs`/`sampling_items` |
| Arbeitspapiere/Workflow | Aufgaben-/Review-Objekte und Freigabekette | neues `application.usecase.workpaper`, `review_actions`, künftige Workflow-Controller |
| Audit Trail unveränderbar | Immutable Audit-Events bei Schreiboperationen | `BookingWebController`, `DomainModelApiController`, neues `infrastructure.audit` |
| RBAC | Rollen-/Rechteprüfung auf Scope-Ebene | neue `infrastructure.security` Konfiguration + Policies |
| Reporting/Export XBRL/ESEF-ready | Vorlagenbasierte Berichtsläufe + strukturierte Artefakte | neues `application.usecase.reporting`, `report_runs` |