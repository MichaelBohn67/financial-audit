# Financial Audit Application Plan

## 1. Feature Catalog

A production-ready financial audit application should cover:

- Data management: CSV, Excel, XML, JSON, and API imports; validation; duplicate and missing-value detection; reconciliation.
- Analytical procedures: Benford analysis, pattern recognition, and rule-based compliance checks such as AML thresholds.
- Risk and sampling: materiality, transaction risk scoring, and random, stratified, and Monetary Unit Sampling (MUS).
- Audit execution: workpapers, review workflow, and an immutable audit trail.
- Reporting and communication: standardized reports, management letters, findings summaries, and audit-progress/risk dashboards.

## 2. Current Implementation Status

Last validated: 2026-08-17. `mvn test` passes with 92 tests, but passing tests do not establish completion of the full plan.

| Feature | Status | Details |
| :--- | :--- | :--- |
| Importing | Completed | CSV and OpenBanking adapters, validation, and import-job tracking exist. |
| Benford analysis | Completed | `BenfordAnalysisService` and persisted analysis models exist. |
| Pattern analysis | Completed | Repeated-amount and timing analysis are implemented. |
| Rule engine | Completed | `AmlEngine` and `AnalyticsRuleService` provide rule-based checks. |
| Reconciliation | Completed | `ReconciliationService` compares account balances with booking sums. |
| Sampling | Partial | Random, stratified, and deterministic MUS sampling persist runs/items. MUS edge handling, exposure, and audit logging remain incomplete. |
| Workpapers | Partial | Lifecycle and review actions exist, but authorization includes legacy roles and there is no separate sign-off state/action. |
| Reporting | Completed | `ReportService`, `ReportExportService`, and Thymeleaf report views exist. |
| Materiality | Partial | Configuration, active-config retrieval, validation, and booking classification exist. Exceeded thresholds do not create persisted findings; feature-specific tests are missing. |
| Finding remediation | Partial | Workpaper linking, remediation fields, lifecycle service, API, and explicit audit writes exist. Validation and controller/integration coverage are incomplete. |
| Dashboard | Partial | Authenticated metrics API and a basic Thymeleaf page exist, but the planned metric set and visualizations are incomplete. |
| Audit trail | Partial | `AuditTrailWriter` and `AuditEvent` exist, with explicit business-operation events. Generic automatic JPA coverage is not implemented. |

## 3. Implemented Infrastructure

- Liquibase migration `019-audit-plan-remediation-materiality` adds materiality activation/de minimis fields and finding remediation/workpaper-link fields.
- Spring Security provides `AUDITOR`, `LEAD_AUDITOR`, and `ADMIN` users, although service-level workpaper authorization still accepts additional legacy roles.
- Existing integration tests cover importing, analytics, sampling persistence, workpaper workflow, and explicit audit-event writing.

## 4. Remaining Work

### A. Materiality framework

1. Persist a materiality-related finding whenever a booking exceeds the applicable threshold.
2. Define the finding metadata and relation to the active materiality configuration.
3. Add unit tests for below/de minimis/performance/overall classifications.
4. Add integration tests for persisted findings and audit events.
5. Add API validation and documentation for the materiality behavior.

### B. Automatic audit trail coverage

1. Choose Spring AOP or JPA entity listeners for generic persistence interception.
2. Cover `Booking`, `Finding`, `Workpaper`, `ReviewAction`, `SamplingRun`, `ReportRun`, `MaterialityConfig`, and `ImportJob`.
3. Record create, update, delete, and workflow-transition events where applicable.
4. Add reusable previous/current snapshot serialization without credentials or secrets.
5. Capture the authenticated actor and use a safe system actor fallback.
6. Prevent recursive auditing of `AuditEvent`.
7. Add tests for event coverage, actor propagation, and snapshots; retain explicit domain events where they add meaning.

### C. Review workflow and roles

1. Restrict `WorkpaperService` to the canonical roles `AUDITOR`, `LEAD_AUDITOR`, and `ADMIN`.
2. Map create/start/submit to all three roles; request-changes and approve/sign-off to `LEAD_AUDITOR` and `ADMIN`.
3. Decide whether approval and sign-off are distinct. If so, add `SIGN_OFF`, `SIGNED_OFF`, and legal state transitions.
4. Add allowed/denied service and controller authorization tests.
5. Update test/seed users and workflow documentation.

### D. Finding remediation

1. Complete validation for assignment, plan updates, and all remediation transitions.
2. Ensure the lifecycle supports `OPEN`, `IN_PROGRESS`, `READY_FOR_REVIEW`, `RESOLVED`, `REJECTED`, and `CLOSED` consistently.
3. Validate required fields and resolution metadata at each transition.
4. Add unit, persistence, audit-trail, and controller tests.

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

- `mvn test`: 92 tests passed, 0 failures, 0 errors.
- Liquibase migrations apply successfully to the H2 test database from an empty schema.
- No dedicated tests currently cover materiality, finding remediation, dashboard metrics/UI, or generic automatic audit interception.
- The current audit trail is explicit and operation-specific; it does not guarantee an event for every persistence change.

## 6. Suggested Implementation Order

1. Persist and test materiality findings.
2. Align workpaper authorization and implement the sign-off decision.
3. Harden finding remediation validation and tests.
4. Implement automatic audit interception and snapshot/actor tests.
5. Complete dashboard metrics, visualizations, protection, and tests.
6. Harden and expose MUS, then run final verification and update this plan.
