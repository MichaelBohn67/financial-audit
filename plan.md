# PIT coverage execution plan

Goal: reach 100% line coverage, mutation coverage, and test strength for all 67 classes in `target/pit-reports`.

Baseline from the report before this execution: 98% line coverage (2138/2187), 95% mutation coverage (1049/1110), and 96% test strength (1049/1090). The report listed 41 surviving mutants and 20 mutations without a covering test.

Latest full PIT report after this execution: 99% line coverage (2171/2187), 96% mutation coverage (1064/1110), and 96% test strength (1064/1103). It still contains 38 surviving mutants, so the 100% gate remains open.

## Completed and verified

- [x] Add Benford tests for all leading digits, fractional amounts, zero-only values, and signed amounts. Focused PIT for `BenfordAnalysisService`: 48/55 killed, 87% mutation coverage and 89% test strength.
- [x] Cover `ReportService.findRunById`, template-version lookup, and legacy run generation, asserting all returned fields.
- [x] Cover report export failure handling and failed-run response in `ReportApiControllerTest`.
- [x] Cover empty CSV rejection in `ImportApiControllerTest`.
- [x] Cover blank tenant/project/document validation and default import context labels.
- [x] Cover blank `OpenBankingImportSource` tenant/project validation.
- [x] Cover `Permission`, `Role`, and `RolePermission` creation callbacks and timestamps.
- [x] Cover `ReportRun` default `PENDING` status and generated timestamp.
- [x] Cover `AccountHolder` address getter/setter.
- [x] Cover `MaterialityConfig.tolerableErrorRate` round-trip.
- [x] Confirm the focused batch tests pass:
  `DomainEntitiesTest`, `ReportServiceTest`, `ImportOrchestratorServiceTest`, `ReportApiControllerTest`, `ImportApiControllerTest`, and `AutomaticAuditEventListenerTest`.

## Remaining work

### 1. Rebuild a reproducible full PIT baseline

- [x] Run full PIT after clearing stale reports; keep focused reports in a separate directory.
- [x] Record the new class/package summary and enumerate all `SURVIVED`, `NO_COVERAGE`, `TIMED_OUT`, and `RUN_ERROR` mutants.
- [ ] Do not change PIT thresholds or exclude classes to improve percentages.

### 2. SamplingService — 12 surviving mutants and uncovered branches

- [ ] Add exact cumulative/selection-point boundary tests for MUS, including final selection and multiple selections on one high-value booking.
- [ ] Add one-record random sampling and exact sample-size boundary tests.
- [ ] Add stratified cases where amount ordering changes strata, strata are empty, remainder allocation differs, and arithmetic mutants change the selected allocation.
- [ ] Execute the MUS interval guard, MUS loop break, and empty-stratum path. If the interval guard is mathematically unreachable, refactor validation into a directly testable helper.

### 3. ImportOrchestratorService — surviving validation/key mutants

- [x] Assert final persisted record/imported/invalid counts and returned counts independently.
- [x] Test no-gap, single-gap, multiple-gap, and boundary-adjacent foreign transaction IDs.
- [x] Test Open Banking account IDs that are null, short, lowercase, invalid, minimum-valid, and maximum-valid.
- [x] Create payload pairs differing in each business-key field and assert only identical payloads are duplicates.
- [ ] Introduce a deterministic digest seam for the SHA-256 fallback and cover the exception branch.

### 4. Analytics

- [ ] `BenfordAnalysisService`: assert the final persisted suspicious count, exact threshold values just below/at/above `0.08`, and the `-1` leading-digit exclusion path.
- [x] `PatternAnalysisService`: assert persisted issue count, exact repeated-transfer time boundaries, finding fields, and audit snapshots for both persistence outcomes.
- [x] `RepeatedAmountPatternAnalyticsRule`: cover records skipped for null/invalid amounts.

### 5. Reporting, materiality, security, and finding lifecycle

- [x] `ReportService`: cover active-template replacement and assert `active == true` on the new saved template.
- [x] `MaterialityService`: add threshold tests just below/equal/above every save boundary and assert old configurations become inactive.
- [x] `SecurityConfig`: assert a non-null filter chain and verify representative authorization rules through security tests.
- [x] `BookingWebController`: assert update requests apply the path ID to the submitted booking.
- [x] `FindingManagementService`: verify resolved timestamp/user in persisted audit snapshots and cover unsupported-status defaults.

### 6. Audit listener and remaining uncovered lines

- [ ] Cover null, boundary, and populated snapshot values; reflective entities with and without `getId`; and reflection failure fallback.
- [ ] Iterate every `uncovered` line in each PIT `*.java.html` report and add a behavior assertion for the branch.
- [ ] Cover `WorkpaperService` unsupported status, `ReportApiController` and `ImportApiController` error paths, and all remaining domain callback branches.

### 7. Final gate

- [x] `mvn -q test` passes after the first execution batch; rerun after the remaining PIT work.
- [ ] Full PIT reports 100% line coverage for all 67 classes.
- [ ] Full PIT reports 0 surviving, no-coverage, timed-out, run-error, or not-started mutants.
- [ ] Update this checklist only after the full report verifies each item.

## Verification commands

```bash
mvn -q -Dtest=<TestClass> test
mvn -q org.pitest:pitest-maven:mutationCoverage \
  -DtargetClasses=<production-class> \
  -DtargetTests=<test-class>
mvn -q test
mvn -q org.pitest:pitest-maven:mutationCoverage
```
