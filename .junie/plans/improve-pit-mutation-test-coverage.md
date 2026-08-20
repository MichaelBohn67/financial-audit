---
sessionId: session-260820-172709-52lc
---

# Requirements

### Overview & Goals
The objective is to systematically address the surviving mutants identified in the latest PIT mutation testing report (`target/pit-reports/index.html`). Currently, the test suite achieves 98% line coverage (2135/2187 lines) but only 85% mutation coverage (945/1110 mutants killed), leaving 165 mutation gaps across 11 packages (with 145 surviving mutants in core logic and controllers). By enhancing existing test cases with stronger assertions, testing boundary conditions, exercising missing lifecycle methods, and adding targeted unit tests, the test suite will reach >= 95% mutation score across all application and domain modules.

### Scope
#### In Scope
- **Sampling Use Case (`SamplingServiceTest`)**: Fix 36 surviving mutants in Monetary Unit Sampling (MUS), Random Sampling, and Stratified Sampling logic, boundary checks, and entity mapping.
- **Importing Use Case (`ImportOrchestratorServiceTest`, `CsvImportAdapterTest`)**: Fix 24 surviving mutants covering job context initialization, protocol entry persistence, checksum calculation, and validation error indexing.
- **Analytics Use Case (`PatternAnalysisServiceTest`, `BenfordAnalysisServiceTest`, `AnalyticsRulesUnitTest`)**: Fix 32 surviving mutants covering pattern detection thresholds, Benford chi-square statistics, first-digit parsing edge cases, and rule boundaries.
- **Finding & Materiality Use Cases (`FindingManagementServiceTest`, `MaterialityServiceTest`)**: Fix 21 surviving mutants covering actor validation, entity mutability rules, status transitions, materiality calculation boundaries, and snapshot generation.
- **Reporting & Reconciliation Use Cases (`ReportExportServiceTest`, `ReportServiceTest`, `DashboardServiceTest`, `ReconciliationServiceTest`)**: Fix 9 surviving mutants covering summary aggregations, template registration flags, and filtering predicates.
- **Web & Infrastructure Layers (`BookingWebControllerTest`, `FindingRemediationApiControllerTest`, `MaterialityApiControllerTest`, `AutomaticAuditEventListenerTest`, `SecurityConfigTest`, `DomainEntitiesTest`)**: Fix 23 surviving mutants covering controller endpoints, timestamp and ID mutations, snapshot truncation boundaries, and entity `@PrePersist` lifecycle methods.

#### Out of Scope
- Modifying production application source code under `src/main/java/` (except if a bug is uncovered during test enhancement that prevents valid tests).
- Changing database schemas, Liquibase migration scripts, or external third-party library dependencies.

### User Stories
- **As an Audit Platform Developer**, I want comprehensive mutation test coverage so that regressions in critical financial calculations (sampling, Benford analysis, materiality thresholds) are immediately caught.
- **As a Compliance Auditor**, I want finding status transitions, audit trail listeners, and import validation protocols strictly asserted in tests to ensure reliable audit evidence.

### Functional Requirements
- **FR-1**: All test assertions must verify the state mutations performed by use cases, including ID references, timestamps, strategy names, sample unit indexes, and JSON parameters.
- **FR-2**: All boundary conditions (e.g. `sampleSize <= 0`, `sampleSize == population.size()`, zero amount filtering, chi-square critical values, materiality percentage bounds) must have dedicated test cases.
- **FR-3**: Error handling and validation failure paths (e.g. invalid actor, unmodifiable finding status, unsupported import sources) must verify the exact failure conditions and absence of invalid state updates.
- **FR-4**: Controller tests must assert the returned HTTP response payloads and verify that underlying service interactions mutate model attributes as expected.

### Non-Functional Requirements
- **Performance**: Test execution time for `mvn test` must remain under 30 seconds.
- **Determinism**: All sampling and analytics tests must use deterministic test data and fixed seeds to avoid flaky test execution.
- **Compatibility**: All test enhancements must conform to JUnit 5 and Mockito 5 standards currently used in the project.

# Technical Design

### Current Implementation
The financial audit system follows a clean/hexagonal architecture:
- `application.usecase.*`: Orchestrates business logic (sampling, importing, analytics, finding management, reporting, materiality).
- `domain.model.*`: Encapsulates entities, validators, and domain rules.
- `infrastructure.adapter.*` and `infrastructure.web.*`: Adapters for external sources, REST APIs, and Thymeleaf web controllers.

While line coverage is high (98%), existing tests frequently mock repositories and assert only method execution counts (`verify(..., times(1))`) without verifying object state, or omit assertions on returned entity fields, boundary values (such as `<=` vs `<`), and entity lifecycle methods (`@PrePersist`).

### Key Decisions
- **Decision 1: Use ArgumentCaptor & Deep State Assertions**: Instead of simple `verify(repo).save(any())`, use `ArgumentCaptor` in Mockito unit tests to assert every field assigned to entities (`SamplingRun`, `SamplingRunItem`, `ImportJob`, `ImportJobProtocolEntry`, `Finding`).
  - *Rationale*: Kills `VoidMethodCallMutator` and property removal mutations without requiring expensive integration tests.
- **Decision 2: Boundary-Focused Parameterized and Unit Tests**: Add explicit boundary tests for comparison operations (`<`, `<=`, `>`, `>=`, `==`) in `MaterialityService`, `SamplingService`, `AutomaticAuditEventListener`, and `PatternAnalysisService`.
  - *Rationale*: Directly targets `ConditionalsBoundaryMutator` and `NegateConditionalsMutator`.
- **Decision 3: Unit Tests for Entity Lifecycle & Getters**: Add lightweight unit tests in `DomainEntitiesTest` covering `@PrePersist` methods (both null and non-null initial timestamps) and domain model getters/setters.
  - *Rationale*: Eliminates remaining surviving mutants in `Finding`, `MaterialityConfig`, `Workpaper`, and `Booking`.

### Proposed Changes

#### 1. Sampling Test Suite (`SamplingServiceTest.java`)
- Assert all fields of saved `SamplingRun` (`runName`, `samplingStrategy`, `seed`, `populationSize`, `sampleSize`, `parametersJson`) using `ArgumentCaptor`.
- Assert all fields of saved `SamplingRunItem` (`sampleUnitIndex`, `selectionPoint`, `bookingId`, `bookingAmount`, `cumulativeAmount`).
- Add tests for population filtering: bookings with null IDs, bookings with zero or negative amounts, and ordering by ID.
- Add test for `sampleSize == population.size()` boundary and verify interval calculation precision.

#### 2. Importing Test Suite (`ImportOrchestratorServiceTest.java`, `CsvImportAdapterTest.java`)
- Capture and assert `ImportJob` fields on initialization and update (`tenantId`, `projectId`, `documentId`, `startedAt`, `recordCount`, `importedCount`, `invalidCount`).
- Capture and assert `ImportJobProtocolEntry` records (`jobId`, `entryIndex`, `level`, `message`).
- Add test asserting checksum computation with various booking attribute combinations.

#### 3. Analytics Test Suite (`PatternAnalysisServiceTest.java`, `BenfordAnalysisServiceTest.java`, `AnalyticsRulesUnitTest.java`)
- In `PatternAnalysisServiceTest`, assert `PatternAnalysisIssue` fields (`issueType`, `severity`, `amount`, `affectedBookingIds`).
- In `BenfordAnalysisServiceTest`, test edge-case inputs (leading zero, numbers < 1, decimals), chi-square calculations, digit statistics population, and threshold boundaries.
- In `AnalyticsRulesUnitTest`, test boundary conditions for `RepeatedAmountPatternAnalyticsRule` (e.g. threshold counts matching exactly vs exceeding).

#### 4. Finding & Materiality Test Suite (`FindingManagementServiceTest.java`, `MaterialityServiceTest.java`)
- In `FindingManagementServiceTest`, test missing actor exception, transition state checks (verifying `allowed()` logic for all valid and invalid transitions), `resolutionComment`, `resolvedAt`, and `resolvedBy` setting.
- In `MaterialityServiceTest`, test percentage boundaries (`0`, `0.01`, `1.0`, `100.0`), `active` flag handling, and finding snapshot generation.

#### 5. Web & Infrastructure Test Suites
- In `BookingWebControllerTest`, verify that updating a booking sets the ID and timestamp when missing, and check `bookingSnapshot` content.
- In `FindingRemediationApiControllerTest` and `MaterialityApiControllerTest`, assert returned response entities and status codes.
- In `AutomaticAuditEventListenerTest`, test snapshot truncation when JSON exceeds max character limits (`substring(0, 4000)`).
- In `DomainEntitiesTest`, test `Workpaper.onCreate()`, `Finding.onCreate()`, and getters.

### File Structure
- Modified test files:
  - `src/test/java/de/bohnottensen/financialaudit/application/usecase/sampling/SamplingServiceTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/application/usecase/importing/ImportOrchestratorServiceTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/application/usecase/analytics/PatternAnalysisServiceTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/application/usecase/analytics/BenfordAnalysisServiceTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/application/usecase/analytics/AnalyticsRulesUnitTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/application/usecase/finding/FindingManagementServiceTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/application/usecase/materiality/MaterialityServiceTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/application/usecase/reporting/ReportExportServiceTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/application/usecase/reporting/ReportServiceTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/infrastructure/web/BookingWebControllerTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/infrastructure/web/FindingRemediationApiControllerTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/infrastructure/web/MaterialityApiControllerTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/application/usecase/audit/AutomaticAuditEventListenerTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/domain/model/DomainEntitiesTest.java`
  - `src/test/java/de/bohnottensen/financialaudit/infrastructure/security/SecurityConfigTest.java`

### Risks & Mitigations
- **Risk**: Over-specifying Mockito matchers causing tests to become brittle to refactoring.
  - *Mitigation*: Capture and assert semantic domain values rather than exact method call ordering or unrelated internal mocks.
- **Risk**: Slowing down test execution during PIT mutation analysis.
  - *Mitigation*: Target unit test files rather than heavy Spring context integration tests for fine-grained mutation killing.

# Testing

### Validation Approach
Verification will be conducted in two stages:
1. **Unit & Integration Test Execution**: Run `mvn clean test` to ensure all existing and newly added test cases compile and pass with 0 failures and 0 errors.
2. **PIT Mutation Test Analysis**: Run `mvn pitest:mutationCoverage` and verify that the mutation coverage across all packages improves from 85% to >= 95%, with surviving mutants reduced from 145 to < 10.

### Key Scenarios
- **Sampling Verification**:
  - Run `SamplingServiceTest` to ensure MUS, Random, and Stratified runs correctly populate all database fields, reject invalid boundaries (0 items, negative sample sizes), and correctly filter non-positive bookings.
- **Import Orchestration**:
  - Run `ImportOrchestratorServiceTest` to verify that job contexts, protocol error entries, and checksum hashes are captured and asserted accurately.
- **Analytics & Benford Analysis**:
  - Run `BenfordAnalysisServiceTest` and `PatternAnalysisServiceTest` to verify that chi-square values, digit distributions, and pattern issue severity logic are asserted under various input sets.
- **Finding Lifecycle & Materiality**:
  - Run `FindingManagementServiceTest` to verify that status transitions (`OPEN` -> `IN_REVIEW` -> `RESOLVED` / `CLOSED`) enforce authorized actors, update resolution comments, and set timestamps.

### Edge Cases
- **Zero & Negative Numbers in Sampling / Benford**: Bookings with zero or negative amounts, leading zeros, or non-numeric descriptions.
- **Boundary Conditions on Thresholds**: Materiality percentages at exactly 0.0, 1.0, or 100.0; audit snapshot strings at exactly 4000 characters and 4001 characters.
- **Null Safety in Entity Lifecycle**: Calling `@PrePersist` (`onCreate`) when `createdAt` is null vs when `createdAt` is already populated.
- **Controller Error & Null Returns**: Verifying that controller endpoints return valid responses and do not return null payloads or drop essential request parameters.

# Delivery Steps

### ✓ Step 1: Strengthen Unit Tests for Sampling and Importing Use Cases
`SamplingServiceTest`, `ImportOrchestratorServiceTest`, and `CsvImportAdapterTest` thoroughly verify all boundary conditions, entity property assignments, and error branches.

- Enhance `SamplingServiceTest` to assert on all persisted `SamplingRun` and `SamplingRunItem` fields (`seed`, `populationSize`, `sampleUnitIndex`, `selectionPoint`, `cumulativeAmount`, and `parametersJson`).
- Add tests in `SamplingServiceTest` for population filtering boundaries: null booking IDs, zero or negative amounts, population ordering, and exact boundary checks (`sampleSize == population.size()`).
- Enhance `ImportOrchestratorServiceTest` to verify `ImportJob` metadata initialization (`tenantId`, `projectId`, `documentId`, `startedAt`, `recordCount`, `invalidCount`).
- Add assertions in `ImportOrchestratorServiceTest` for `ImportJobProtocolEntry` creation (verifying level, index, and formatted error message).
- Add tests verifying checksum calculation accuracy and empty/malformed input handling in `ImportOrchestratorServiceTest` and `CsvImportAdapterTest`.

### ✓ Step 2: Strengthen Unit Tests for Analytics, Reporting, and Reconciliation Use Cases
`PatternAnalysisServiceTest`, `BenfordAnalysisServiceTest`, `AnalyticsRulesUnitTest`, `ReportExportServiceTest`, `ReportServiceTest`, `DashboardServiceTest`, and `ReconciliationServiceTest` eliminate all surviving arithmetic, predicate, and filter mutants.

- Expand `PatternAnalysisServiceTest` to assert on issue attributes (severity, amount, booking IDs) for round amount detection, high frequency split transactions, and weekend booking detection.
- Add test cases in `BenfordAnalysisServiceTest` covering chi-square deviation calculation, non-positive or decimal digit filtering, digit distribution statistics, and significant anomaly thresholds.
- Add tests in `AnalyticsRulesUnitTest` for `RepeatedAmountPatternAnalyticsRule` boundary conditions (minimum repetition threshold and exact amount matching).
- Extend `ReportExportServiceTest` to assert on finding summaries, status distributions, booking statistics calculations, and date filter boundaries.
- Add assertions in `ReportServiceTest` for template metadata (`description`, `active`).
- Add edge case tests in `DashboardServiceTest` and `ReconciliationServiceTest` for account matching predicates and KPI metric aggregations.

### ✓ Step 3: Strengthen Unit Tests for Finding Management, Materiality, and Domain Entities
`FindingManagementServiceTest`, `MaterialityServiceTest`, and `DomainEntitiesTest` kill all state transition, mutation check, threshold calculation, and entity lifecycle mutants.

- Expand `FindingManagementServiceTest` to test authorization and immutability checks (`requireActor`, `ensureMutable`), valid status transition paths, `resolutionComment`, `resolvedAt`, and `resolvedBy` property tracking.
- Test invalid/disallowed status transitions in `FindingManagementServiceTest` to verify that `allowed()` method returns false for unsupported state shifts.
- Enhance `MaterialityServiceTest` to test exact numeric boundary conditions for overall materiality, performance materiality, and tolerable error rates (`MaterialityConfig`), as well as finding snapshot formatting and active config flags.
- Add entity unit tests in `DomainEntitiesTest` for `@PrePersist` (`onCreate()`) null vs non-null timestamps, and getters/setters across `Finding`, `MaterialityConfig`, `Workpaper`, and `Booking`.

### ✓ Step 4: Strengthen Web Controller, Security, and Audit Listener Tests
`BookingWebControllerTest`, `FindingRemediationApiControllerTest`, `MaterialityApiControllerTest`, `AutomaticAuditEventListenerTest`, and `SecurityConfigTest` kill all surviving controller return, mutation, and listener boundary mutants.

- Update `BookingWebControllerTest` to assert on `Booking.setTransactionTimestamp`, ID assignment during updates, conditional null-checking branches, and `bookingSnapshot` return values.
- Enhance `FindingRemediationApiControllerTest` and `MaterialityApiControllerTest` to verify response bodies and non-null returned entity representations on remediation and materiality evaluation endpoints.
- Add tests in `AutomaticAuditEventListenerTest` for audit payload string truncation boundaries (`snapshot()` length limits and character boundaries).
- Strengthen `SecurityConfigTest` to verify the configured `SecurityFilterChain` bean structure and permissions.
- Run `mvn test` and execute PIT mutation analysis (`mvn pitest:mutationCoverage`) to verify that the target mutation coverage threshold (>= 95%) is achieved without regressions.