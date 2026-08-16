# Financial Audit Application Plan

## Introduction
This document outlines the features required for a robust financial audit application and assesses the current implementation state of the project.

## 1. Feature Catalog for Financial Audit Software

A professional financial audit application should cover the following functional areas:

### A. Data Management & Integration
- **Importing:** Support for various formats (CSV, Excel, XML, JSON) and direct API integrations (e.g., Open Banking).
- **Data Validation:** Automatic detection of duplicates, missing values, and formatting errors.
- **Reconciliation:** Tools to compare internal records against external statements (bank, tax, etc.).

### B. Analytical Procedures (DQA - Data Quality Analytics)
- **Statistical Analysis:** Benford's Law analysis to detect anomalies in digit distributions.
- **Pattern Recognition:** Identification of repeated amounts, unusual timing, or suspicious transaction sequences.
- **Rule-based Engines:** Compliance checks against specific legal or internal thresholds (e.g., AML rules).

### C. Risk Assessment & Sampling
- **Materiality Calculation:** Defining thresholds for significant financial errors.
- **Risk Scoring:** Automated scoring of transactions based on multiple risk factors.
- **Audit Sampling:** Statistical sampling methods (Random, Stratified, Monetized Unit Sampling) to select items for testing.

### D. Audit Execution & Documentation
- **Workpapers:** Digital folders for documenting audit steps, findings, and conclusions.
- **Review Workflow:** Multi-level approval process (Auditor -> Senior -> Partner).
- **Audit Trail:** Immutable log of all actions taken within the system for compliance (ISO 27001/IDW PS 880).

### E. Reporting & Communication
- **Standardized Reports:** Generation of audit reports, management letters, and findings summaries.
- **Visualization:** Dashboards for real-time monitoring of audit progress and risk distribution.

---

## 2. Current Implementation Status

Based on the current codebase, the following features are already implemented or partially available:

| Feature | Status | Details |
| :--- | :--- | :--- |
| **Importing** | ✓ Completed | CSV and OpenBanking adapters implemented. |
| **Benford Analysis** | ✓ Completed | `BenfordAnalysisService` and related domain models. |
| **Pattern Analysis** | ✓ Completed | `PatternAnalysisService` (Repeated amounts, etc.). |
| **Rule Engine** | ✓ Completed | `AmlEngine` and `AnalyticsRuleService` with several rules. |
| **Sampling** | ✓ Completed | `SamplingService` with random, stratified, and Monetary Unit Sampling. |
| **Workpapers** | ✓ Completed | `WorkpaperService` and `Workpaper` domain model. |
| **Reporting** | ✓ Completed | `ReportService`, `ReportExportService` and Thymeleaf-based `ReportWebController`. |
| **Reconciliation** | ✓ Completed | `ReconciliationService` for balance matching. |
| **Audit Trail** | ~ Partial | `AuditTrailWriter` and `AuditEvent` model exist. |

---

## 3. Roadmap: Features to Implement

The following features are missing and should be prioritized for a production-ready audit tool:

### Phase 1: Core Enhancements
1.  **Reconciliation Module:** ✓ Completed - Implemented `ReconciliationService` to match `Booking` records against account balances.
2.  **Materiality Framework:** Add a configuration module to define `Materiality` (Overall, Performance, De Minimis) and flag bookings exceeding these.
3.  **Advanced Audit Trail:** Ensure all domain model changes (e.g., Workpaper status changes) are automatically persisted via an Aspect or Entity Listener.

### Phase 2: Collaboration & Workflow
4.  **Multi-User Review Workflow:** Enhance `ReviewAction` to support a formal "Sign-off" process with role-based permissions (Auditor/Reviewer).
5.  **Finding Management:** Link `Finding` objects more closely to `Workpapers` and implement a "Remediation Tracking" system.

### Phase 3: Analytics & Visualization
6.  **Dashboard UI:** Create a frontend view (Thymeleaf or React) to visualize risk clusters and audit progress.
7.  **Monetary Unit Sampling (MUS):** ✓ Completed - deterministic, persisted MUS sampling is implemented.

---

## 4. Implementation Plan (Next Steps)

1.  **Refine Security & Roles:** ✓ Completed - Implemented `AUDITOR` and `LEAD_AUDITOR` roles with Spring Security.
2.  **Develop Reconciliation Logic:** ✓ Completed - Created `ReconciliationService` to compare `Account` balances with `Booking` sums.
3.  **Enhance Reporting:** Added `ReportWebController` for HTML report viewing and dynamic Thymeleaf templates for "Management Letters" (management-letter.html). ✓

---

# Remaining Audit Plan Implementation

## Last verified (2026-08-16)

Implemented in this iteration: materiality configuration/evaluation API with threshold validation, finding remediation fields and lifecycle API, finding/workpaper linking, canonical workflow roles, dashboard metrics/API/page, and Liquibase migration `019-audit-plan-remediation-materiality`. Existing random, stratified, and MUS sampling remain implemented. The remaining hardening gap is generic automatic audit interception for every JPA entity; existing business operations continue to write explicit audit events.

## Status

The original audit plan is partially implemented. Core import, analytics, reconciliation, reporting, workpapers, and sampling capabilities exist, including Monetary Unit Sampling. The remaining work is concentrated around materiality, automatic audit trail coverage, workflow role consistency, finding remediation management, dashboard visualization, and plan/status cleanup.

## Atomic Remaining Implementation Steps

### 1. Align the audit plan with current implementation

1. Update `AUDIT_PLAN.md` to mark Monetary Unit Sampling as completed.
2. Update the sampling feature status from "basic selection logic" to include random, stratified, and MUS sampling.
3. Keep only genuinely missing items in the roadmap.
4. Add a short "Last verified" section describing the implemented modules and remaining gaps.

### 2. Complete the Materiality Framework

1. Define the required materiality levels:
   - overall materiality
   - performance materiality
   - de minimis threshold
2. Extend `MaterialityConfig` if needed to explicitly store all required thresholds.
3. Add or adjust the Liquibase changelog for any missing materiality columns.
4. Create a `MaterialityService`.
5. Add a method to create/update materiality configurations.
6. Add a method to retrieve the active materiality configuration.
7. Add a method to evaluate a single booking against materiality thresholds.
8. Add a method to evaluate all bookings for a project/account scope.
9. Define a materiality result DTO or domain object.
10. Persist materiality-related findings when thresholds are exceeded.
11. Add repository queries needed for scoped materiality evaluation.
12. Add unit tests for threshold classification.
13. Add integration tests for persisted materiality findings.
14. Add an API controller for materiality configuration and evaluation.
15. Add validation for invalid threshold combinations.
16. Add documentation to README or compliance docs.

### 3. Implement automatic audit trail coverage

1. Decide between Spring AOP and JPA entity listeners for automatic audit logging.
2. Define the audited entity list:
   - Booking
   - Finding
   - Workpaper
   - ReviewAction
   - SamplingRun
   - ReportRun
   - MaterialityConfig
   - ImportJob
3. Define event types for create, update, delete, and workflow transition events.
4. Create a reusable audit snapshot serializer.
5. Ensure snapshots do not include sensitive credentials or secrets.
6. Implement the audit aspect/entity listener.
7. Capture actor information from Spring Security where available.
8. Use a safe fallback actor for system/background operations.
9. Ensure audit logging does not recursively audit `AuditEvent` itself.
10. Add tests for create audit events.
11. Add tests for update audit events.
12. Add tests for delete audit events if deletes are supported.
13. Add tests verifying actor propagation.
14. Add tests verifying previous/current value snapshots.
15. Review existing manual audit writes and remove duplicates where automatic coverage supersedes them.
16. Keep explicit business audit events where they add domain meaning beyond generic persistence changes.

### 4. Fix review workflow role consistency

1. Compare roles configured in `SecurityConfig` with roles used in service-level `@PreAuthorize` rules.
2. Choose one canonical role model.
3. Recommended canonical roles:
   - `AUDITOR`
   - `LEAD_AUDITOR`
   - `ADMIN`
4. Replace obsolete role checks in `WorkpaperService`.
5. Map workpaper permissions:
   - create/start/submit: `AUDITOR`, `LEAD_AUDITOR`, `ADMIN`
   - request changes: `LEAD_AUDITOR`, `ADMIN`
   - approve/sign off: `LEAD_AUDITOR`, `ADMIN`
6. Add a formal sign-off action if approval and sign-off must be distinct.
7. Extend `ReviewActionType` if needed with `SIGN_OFF`.
8. Extend `WorkpaperStatus` if needed with `SIGNED_OFF`.
9. Validate legal state transitions for the new workflow.
10. Add tests for allowed role transitions.
11. Add tests for denied role transitions.
12. Add controller/API tests for workflow endpoints.
13. Update seed users or test users to reflect the canonical role model.
14. Update documentation describing the review workflow.

### 5. Add Finding Management and Remediation Tracking

1. Add a nullable `workpaper_id` relation from `Finding` to `Workpaper`.
2. Add remediation fields to `Finding` or introduce a dedicated `FindingRemediation` entity.
3. Required remediation fields:
   - owner
   - due date
   - remediation status
   - remediation plan
   - resolution comment
   - resolved at
   - resolved by
4. Define remediation statuses:
   - `OPEN`
   - `IN_PROGRESS`
   - `READY_FOR_REVIEW`
   - `RESOLVED`
   - `REJECTED`
   - `CLOSED`
5. Add Liquibase migration for the new fields/table.
6. Add repository methods to query findings by workpaper, status, owner, and due date.
7. Create a `FindingManagementService`.
8. Add method to link a finding to a workpaper.
9. Add method to assign remediation ownership.
10. Add method to update remediation plan.
11. Add method to mark remediation ready for review.
12. Add method to resolve remediation.
13. Add method to reject remediation.
14. Add method to close finding.
15. Record audit events for all remediation lifecycle changes.
16. Add validation for invalid remediation transitions.
17. Add API endpoints for finding remediation operations.
18. Add unit tests for transition validation.
19. Add integration tests for persistence and audit trail records.
20. Add web/controller tests for remediation endpoints.

### 6. Build Dashboard UI and dashboard data API

1. Define dashboard metrics:
   - total bookings
   - total findings
   - findings by risk level
   - findings by status
   - open remediation count
   - overdue remediation count
   - workpapers by status
   - sampling runs count
   - latest report runs
   - audit progress percentage
2. Create a dashboard DTO.
3. Create a `DashboardService`.
4. Add repository queries or service aggregation methods for each metric.
5. Add an authenticated dashboard API endpoint.
6. Add a Thymeleaf dashboard page or React/static page.
7. Visualize risk distribution.
8. Visualize workpaper progress.
9. Visualize remediation status.
10. Visualize recent audit events.
11. Add navigation from the home/index page to the dashboard.
12. Add controller tests for the dashboard endpoint.
13. Add service tests for metric calculation.
14. Add basic UI smoke test if existing test setup supports it.

### 7. Harden sampling implementation and documentation

1. Keep MUS marked as implemented.
2. Review MUS edge cases:
   - zero or negative amounts
   - duplicate selection of high-value bookings
   - sample size larger than effective population
   - deterministic seed behavior
3. Add or expand tests for MUS edge cases.
4. Document the MUS method and assumptions.
5. Expose MUS through API/UI if not already exposed.
6. Add request validation for MUS parameters.
7. Add audit events for sampling run creation if not already covered by the automatic audit trail.

### 8. Final verification

1. Run all unit tests.
2. Run all integration tests.
3. Verify Liquibase migrations apply from an empty database.
4. Verify Spring Security method authorization works for canonical roles.
5. Verify audit events are produced for materiality, findings, workpapers, sampling, and reporting.
6. Verify dashboard loads with empty data.
7. Verify dashboard loads with representative seeded data.
8. Update `AUDIT_PLAN.md` with final completion statuses.
9. Update README with new feature coverage.
10. Add a short compliance note describing audit trail and remediation tracking behavior.

## Suggested Implementation Order

1. Fix role consistency in the review workflow.
2. Complete materiality service/API/tests.
3. Add finding remediation model and lifecycle.
4. Implement automatic audit trail coverage.
5. Add dashboard service/API/UI.
6. Harden/document MUS and update the original plan.
7. Run final verification and update project documentation.
