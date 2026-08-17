# Financial Audit Application Plan

## 1. Feature Catalog

A production-ready financial audit application should cover:

- Data management: CSV, Excel, XML, JSON, and API imports; validation; duplicate and missing-value detection; reconciliation.
- Analytical procedures: Benford analysis, pattern recognition, and rule-based compliance checks such as AML thresholds.
- Risk and sampling: materiality, transaction risk scoring, and random, stratified, and Monetary Unit Sampling (MUS).
- Audit execution: workpapers, review workflow, and an immutable audit trail.
- Reporting and communication: standardized reports, management letters, findings summaries, and audit-progress/risk dashboards.

## 2. Current Implementation Status

Last validated: 2026-08-17. `mvn test` passes with 105 tests, but passing tests do not establish completion of the full plan.

| Feature | Status | Details |
| :--- | :--- | :--- |
| Importing | Completed | CSV and OpenBanking adapters, validation, and import-job tracking exist. |
| Benford analysis | Completed | `BenfordAnalysisService` and persisted analysis models exist. |
| Pattern analysis | Completed | Repeated-amount and timing analysis are implemented. |
| Rule engine | Completed | `AmlEngine` and `AnalyticsRuleService` provide rule-based checks. |
| Reconciliation | Completed | `ReconciliationService` compares account balances with booking sums. |
| Sampling | Partial | Random, stratified, and deterministic MUS sampling persist runs/items. MUS edge handling, exposure, and audit logging remain incomplete. |
| Workpapers | Completed | Canonical `AUDITOR`/`LEAD_AUDITOR`/`ADMIN` authorization, approval, distinct sign-off, legal transitions, audit events, and review actions are implemented. |
| Reporting | Completed | `ReportService`, `ReportExportService`, and Thymeleaf report views exist. |
| Materiality | Partial | Configuration, active-config retrieval, validation, classification, idempotent finding persistence, and audit events exist. API documentation and broader scope behavior remain to be completed. |
| Finding remediation | Completed | Workpaper linking, required remediation fields, validated lifecycle transitions, API operations, persistence, and explicit audit snapshots/events are implemented and tested. |
| Dashboard | Partial | Authenticated metrics API and a basic Thymeleaf page exist, but the planned metric set and visualizations are incomplete. |
| Audit trail | Partial | Hibernate post-insert/update/delete interception now covers the planned entities with sanitized snapshots and actor fallback. Explicit domain-event deduplication/review remains. |

## 3. Implemented Infrastructure

- Liquibase migration `019-audit-plan-remediation-materiality` adds materiality activation/de minimis fields and finding remediation/workpaper-link fields.
- Spring Security provides `AUDITOR`, `LEAD_AUDITOR`, and `ADMIN` users, although service-level workpaper authorization still accepts additional legacy roles.
- Existing integration tests cover importing, analytics, sampling persistence, workpaper workflow, explicit audit-event writing, and automatic persistence audit events.

## 4. Remaining Work

### A. Materiality framework

1. Document the materiality API, threshold semantics, and the relation between materiality findings and configurations.
2. Review whether project-level (rather than account-only) evaluation is required and add that scope if needed.

### B. Automatic audit trail coverage — implemented

Hibernate post-insert, post-update, and post-delete listeners cover `Booking`, `Finding`, `Workpaper`, `ReviewAction`, `SamplingRun`, `ReportRun`, `MaterialityConfig`, and `ImportJob`. They write sanitized previous/current snapshots, propagate the authenticated actor, use `SYSTEM` for background work, and exclude `AuditEvent` itself. Integration tests cover create/update/delete events, actor propagation, and snapshot values. Existing explicit domain events remain where they provide business meaning; duplicate-event review is still a follow-up.

### C. Review workflow and roles — completed

Canonical roles are enforced in the workpaper service and API. Approval transitions to `APPROVED`; a separate `SIGN_OFF` action transitions to `SIGNED_OFF`. Allowed/denied authorization, transition, controller, persistence, and audit-event behavior are covered by tests.

### D. Finding remediation — completed

The remediation service validates owners, due dates, plans, resolution comments, terminal states, legal transitions, and actor identity. The API exposes assignment, plan update, transition, and workpaper-link operations. Unit, persistence, audit-trail, and controller tests cover the lifecycle.

### E. Dashboard

1. Add total bookings/findings, findings by risk and status, open/overdue remediation, workpapers by status, sampling-run count, latest reports, audit progress, and recent audit events.
2. Add repository queries/service aggregation and a typed dashboard DTO.
3. Add risk, workpaper-progress, remediation-status, and recent-event visualizations.
4. Protect the dashboard page consistently with its API and add controller/service/UI smoke tests.

### F. Sampling hardening

1. Define behavior when MUS sample size exceeds the effective population; do not silently create duplicate selections without an explicit documented policy.
2. Test zero/negative amounts, duplicate high-value selections, oversized samples, and deterministic seeds.
3. Expose MUS through an API/UI, validate parameters, document assumptions, and audit sampling-run creation.

## 5. Verification Notes

- `mvn test`: 105 tests passed, 0 failures, 0 errors.
- Liquibase migrations apply successfully to the H2 test database from an empty schema.
- Materiality classification, persisted findings, idempotency, migration behavior, and audit events are now covered by dedicated unit/integration tests.
- No dedicated tests currently cover dashboard metrics/UI.
- Generic automatic persistence coverage is implemented; explicit domain events remain alongside generic events until deduplication is reviewed.

## 6. Suggested Implementation Order

1. [x] Implement automatic audit interception and snapshot/actor tests.
2. Complete dashboard metrics, visualizations, protection, and tests.
3. Harden and expose MUS, then run final verification and update this plan.
