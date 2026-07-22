---
sessionId: session-260721-112155-13gg
---

# Requirements

### Overview & Goals
Implement **Phase 3 first** from your roadmap: expand AML detection beyond the existing high-amount check, add deterministic risk scoring, and define a complete AI rules file in `.junie/rules.md` so implementation stays consistent and faster across all AML changes.

### Scope
**In Scope**
- Add multiple AML rules in the current `domain.model` style (`AmlRule` + `AmlEngine`).
- Introduce a risk scoring component that maps rule hits to `LOW/MEDIUM/HIGH`.
- Persist generated alerts as `Finding` records via existing `FindingRepository`.
- Populate `.junie/rules.md` with all AI working rules needed for this phase: clean code formatting, naming, comments, planning/implementation guardrails, and testing conventions.
- Keep behavior test-driven in line with existing unit/integration test patterns.

**Out of Scope**
- CSSF full-catalog coverage (all regulations on the website).
- Workflow actions on findings (escalation/closure logic from Phase 4).
- External watchlist/API integrations (Phase 5).

### Functional Requirements
- A booking can trigger **multiple AML alerts** from different rules.
- Each alert is transformed into a `Finding` with:
  - `ruleName`
  - `alertDescription`
  - `riskLevel`
  - default `status=NEW` via entity lifecycle (already present in `Finding`).
- Risk level assignment must be reproducible (same input => same output).
- Existing high-amount behavior remains backward compatible.
- `.junie/rules.md` exists and is fully populated for this phase, including all rules needed by the AI agent:
  - Java formatting/style conventions aligned to the current codebase,
  - naming and package conventions for `domain`, `application`, `infrastructure`,
  - comment policy (what to comment and what to avoid),
  - guardrails for writing AML rules and tests consistently,
  - testing conventions (JUnit 5 + AssertJ, clear scenario naming, deterministic assertions),
  - risk/scoring implementation constraints (pure mapping rules, reproducible outputs),
  - concise AI execution rules (scope discipline, deterministic behavior, and no unnecessary refactors).

# Technical Design

### Current Implementation
- AML extension point already exists:
  - `src/main/java/de/bohnottensen/financialaudit/domain/model/AmlRule.java`
  - `src/main/java/de/bohnottensen/financialaudit/domain/model/AmlEngine.java`
  - `src/main/java/de/bohnottensen/financialaudit/domain/model/HighAmountRule.java`
- Transaction payload currently available in `Booking`:
  - amount/currency/timestamp/sourceAccount/destinationAccount (no account-holder link yet).
- Persistence for generated findings already exists:
  - `src/main/java/de/bohnottensen/financialaudit/domain/model/Finding.java`
  - `src/main/java/de/bohnottensen/financialaudit/infrastructure/persistence/FindingRepository.java`
- Existing architectural pattern is lightweight layered Spring Boot:
  - domain rules as plain Java classes
  - Spring Data repositories in `infrastructure.persistence`
  - web controller in `infrastructure.web`.

### Key Decisions
1. **Keep rule execution inside `AmlEngine` and extend via new `AmlRule` implementations**
   - Aligns with existing polymorphic rule pattern and `AmlEngineTest` style.
2. **Introduce a small alert contract for scoring context**
   - Replace raw `String` alert pipeline with a richer domain result (rule key + message) to avoid fragile string parsing for risk mapping.
3. **Add an application-level audit orchestration service**
   - Service loads bookings (or processes one booking), runs AML engine, scores hits, persists `Finding` entries through `FindingRepository`.

### Proposed Changes
- **AI rules file (for faster execution consistency)**
  - Populate `.junie/rules.md` as the central rulebook used while implementing this phase.
  - Add complete rule sections: code style/formatting, naming, comments, null-safety/validation, AML rule authoring pattern, scoring determinism, test structure, and refactor boundaries.
  - Define "must" and "must not" rules so agent behavior is explicit and enforceable during implementation.
  - Include examples based on existing files (`AmlEngine.java`, `HighAmountRule.java`, `AmlEngineTest.java`) so guidance matches current patterns.

- **Domain additions**
  - Add new rule classes under `domain.model` (Phase-3 set):
    - `StructuringRule` (many sub-threshold transactions pattern)
    - `HighFrequencyRule` (unusual repeated transfers in time window)
    - keep `HighAmountRule` and normalize messages/rule identifiers.
  - Add a value object (e.g. `AmlAlert`) containing:
    - `ruleName`
    - `description`
    - optional severity hint.
  - Update `AmlRule` and `AmlEngine` to return typed alerts instead of plain strings.

- **Risk scoring**
  - Add `RiskScoringEngine` in `domain.model` (or `application` if orchestration-focused).
  - Rule-based mapping table, e.g.:
    - very high amount => HIGH
    - structuring/high frequency => MEDIUM or HIGH depending on counts/amount
    - fallback => LOW.

- **Application orchestration**
  - Add a service (e.g. `AmlAuditService`) to:
    1. run engine,
    2. compute risk per alert,
    3. create `Finding` entities,
    4. persist with `FindingRepository`.

- **Data model alignment**
  - Reuse `Finding` fields directly; no schema redesign required for MVP.
  - `AccountHolder` (`domain/model/AccountHolder.java`) remains unchanged in Phase 3; can feed future KYC/geography rules in later phases.

### File Structure
- **Modify**
  - `src/main/java/de/bohnottensen/financialaudit/domain/model/AmlRule.java`
  - `src/main/java/de/bohnottensen/financialaudit/domain/model/AmlEngine.java`
  - `src/main/java/de/bohnottensen/financialaudit/domain/model/HighAmountRule.java`
- **Add/Modify**
  - `.junie/rules.md`
  - `src/main/java/de/bohnottensen/financialaudit/domain/model/AmlAlert.java`
  - `src/main/java/de/bohnottensen/financialaudit/domain/model/StructuringRule.java`
  - `src/main/java/de/bohnottensen/financialaudit/domain/model/HighFrequencyRule.java`
  - `src/main/java/de/bohnottensen/financialaudit/domain/model/RiskScoringEngine.java`
  - `src/main/java/de/bohnottensen/financialaudit/application/AmlAuditService.java`
- **Potentially modify/add integration point**
  - Existing web/persistence flow where booking creation triggers AML evaluation.

### Risks
- **Data sufficiency risk**: Some CSSF-inspired patterns need historical context; current booking-centric model may require repository queries for prior transactions.
- **False positives**: Threshold-only logic can over-flag; mitigate by making thresholds configurable.
- **Refactor impact**: Changing `AmlRule` return type affects existing tests; mitigate with staged migration and adapter compatibility during transition.
- **Rules drift risk**: A rules file can become stale; mitigate by keeping rules narrowly scoped to Phase 3 and updating it when implementation decisions change.

# Testing

### Validation Approach
Use the same style already present (`AmlEngineTest`, adapter tests, repository tests) and expand with focused unit + integration checks.

### Key Scenarios
- `AmlEngine` triggers multiple rule alerts for a single booking.
- `StructuringRule` detects repeated near-threshold transactions.
- `HighFrequencyRule` detects unusual transaction burst behavior.
- `RiskScoringEngine` maps rule outputs to expected levels.
- `AmlAuditService` persists correct `Finding` rows with expected `ruleName`, `alertDescription`, `riskLevel`, and default `status`.

### Edge Cases
- Null/invalid booking fields do not crash AML evaluation.
- Empty rule list returns no alerts/findings.
- Duplicate processing safeguards (if same booking is evaluated multiple times in short sequence) are verified for expected current behavior.

# Delivery Steps

###   Step 1: Populate `.junie/rules.md` with all AI rules needed for Phase 3
A complete, structured rules file exists in `.junie/rules.md` and is ready to guide consistent implementation.
- Add concrete conventions for Java formatting, naming, validation/null-safety, and test structure.
- Define comment policy (intent/rationale comments only; avoid redundant narration).
- Add explicit "must" / "must not" guardrails for AML rule implementations and deterministic risk scoring behavior.
- Add AI execution guardrails: scope discipline, reuse of existing project patterns, and no opportunistic refactors.
- Include short examples aligned with current code style from `AmlEngine`, `HighAmountRule`, and `AmlEngineTest`.

###   Step 2: Refactor AML result contract and stabilize current rule pipeline
AML rule execution produces typed alerts suitable for downstream scoring.
- Introduce a domain alert object (e.g. `AmlAlert`) with explicit `ruleName` and `description`.
- Update `AmlRule` and `AmlEngine` to operate on typed alerts instead of raw strings.
- Adapt `HighAmountRule` and `AmlEngineTest` to the new contract while preserving existing detection semantics.

###   Step 3: Implement Phase-3 rule set for pattern detection
The engine detects additional suspicious patterns beyond high amount.
- Add `StructuringRule` for repeated sub-threshold/smurfing-like transfers using booking history inputs.
- Add `HighFrequencyRule` for unusual burst activity in defined time windows.
- Add/extend unit tests for each rule with positive and negative scenarios using project’s current JUnit + AssertJ style.

###   Step 4: Add deterministic risk scoring and finding persistence orchestration
AML alerts are transformed into persisted findings with risk levels.
- Implement `RiskScoringEngine` to map alert/rule context to `LOW/MEDIUM/HIGH` consistently.
- Add `AmlAuditService` that runs engine + scoring and creates `Finding` entities.
- Persist through `FindingRepository` and verify end-to-end behavior with integration-level tests around booking-to-finding generation.

###   Step 5: Wire Phase-3 flow into existing booking lifecycle
New bookings can trigger AML analysis in the current application flow.
- Integrate `AmlAuditService` at the existing booking entry point (current `BookingWebController` flow or adjacent service layer if introduced).
- Ensure transaction timestamp defaults and validation behavior remain intact with AML processing added.
- Extend web/service tests to verify that suspicious bookings now result in stored findings without breaking current booking CRUD behavior.