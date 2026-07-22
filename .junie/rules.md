# Phase 3 AML AI Rules

## 0. Core coding guideline

### MUST
- The AI must follow Clean Code guidelines in every change.

## 1. Scope and intent

### MUST
- Implement only Phase 3 AML work: rule expansion, deterministic risk scoring, and finding persistence flow support.
- Preserve existing behavior of `HighAmountRule` unless a requirement explicitly changes it.
- Keep changes aligned with current package layering: `domain.model`, `application`, `infrastructure`.

### MUST NOT
- Do not implement Phase 4+ items (finding workflow/escalation/closure).
- Do not add external watchlists or third-party AML APIs.
- Do not perform opportunistic refactors unrelated to AML Phase 3.

## 2. Java code style and formatting

### MUST
- Follow existing formatting in this repository and surrounding files.
- Keep classes focused and small; prefer one responsibility per class.
- Use explicit, descriptive names for rules, alerts, and scoring components.
- Keep method bodies straightforward and deterministic.
- Prefer immutable value objects for alert/scoring inputs where feasible.

### MUST NOT
- Do not introduce style patterns that differ from the current module conventions.
- Do not add unnecessary abstraction layers for simple deterministic logic.

## 3. Naming and package conventions

### MUST
- Place AML domain logic under `de.bohnottensen.financialaudit.domain.model`.
- Place orchestration/use-case services under `de.bohnottensen.financialaudit.application`.
- Place persistence/web adapters under `de.bohnottensen.financialaudit.infrastructure.*`.
- Use names consistent with intent:
  - Rules: `*Rule` (e.g., `StructuringRule`, `HighFrequencyRule`)
  - Alert value object: `AmlAlert`
  - Scoring component: `RiskScoringEngine`
  - Orchestrator: `AmlAuditService`

### MUST NOT
- Do not place business rules in controller or repository classes.
- Do not use ambiguous names like `Helper`, `Utils`, `Manager` for core AML behavior.

## 4. Comments policy

### MUST
- Add comments only for intent/rationale when business reasoning is not obvious.
- Keep comments concise and domain-relevant (AML detection rationale, threshold meaning).

### MUST NOT
- Do not add narration comments that restate obvious code.
- Do not leave stale comments after behavior changes.

## 5. Validation and null-safety

### MUST
- Handle missing or invalid booking fields defensively so AML evaluation does not crash.
- Keep failure behavior deterministic (same invalid input => same outcome).
- Prefer explicit checks at boundaries and keep core rule logic predictable.

### MUST NOT
- Do not hide null/invalid cases with silent random defaults.
- Do not introduce nondeterministic behavior to “recover” from invalid data.

## 6. AML rule authoring guardrails

### MUST
- Keep `AmlRule` polymorphic and composable through `AmlEngine`.
- Allow multiple alerts for a single booking across different rules.
- Emit typed alerts with explicit `ruleName` and `description` (avoid string parsing contracts).
- Ensure each rule can be unit tested in isolation with deterministic inputs.
- Keep thresholds and window logic explicit and easy to reason about.

### MUST NOT
- Do not couple rule logic to web or persistence adapters.
- Do not mix detection logic with finding persistence.
- Do not rely on side effects or mutable global state for rule outcomes.

## 7. Deterministic risk scoring rules

### MUST
- Implement risk scoring as pure mapping from alert/rule context to `LOW`, `MEDIUM`, `HIGH`.
- Ensure reproducible outputs: same input alert/context must always produce same risk level.
- Keep mapping rules explicit (no hidden heuristics).
- Default unknown/unmapped cases to a documented deterministic fallback (typically `LOW`).

### MUST NOT
- Do not use random values, current clock variance, or external mutable state in scoring.
- Do not parse human-readable alert text when structured fields are available.

## 8. Testing conventions (JUnit 5 + AssertJ)

### MUST
- Follow existing test style in repository (JUnit 5, AssertJ, clear arrange/act/assert flow).
- Use scenario-driven test names that describe behavior and expected result.
- Keep assertions deterministic and specific (`ruleName`, `description`, `riskLevel`, status behavior).
- Cover positive and negative scenarios for every new AML rule.
- Cover edge cases: empty rule lists, invalid booking inputs, and multi-alert bookings.

### MUST NOT
- Do not write flaky tests depending on wall-clock timing without control.
- Do not weaken assertions to make tests pass.

## 9. Implementation workflow guardrails for AI execution

### MUST
- Make minimal, focused changes required for the current step.
- Reuse existing project patterns before introducing new constructs.
- Keep behavior backward compatible unless requirements state otherwise.
- Validate with targeted unit/integration tests for touched behavior.

### MUST NOT
- Do not expand scope beyond requested Phase 3 delivery items.
- Do not rewrite unrelated modules.

## 10. Short style-aligned examples

```java
// Example: typed alert contract returned by rules
public record AmlAlert(String ruleName, String description) {}
```

```java
// Example: deterministic scoring mapping (no randomness, no text parsing fragility)
RiskLevel score(AmlAlert alert) {
    return switch (alert.ruleName()) {
        case "HIGH_AMOUNT" -> RiskLevel.HIGH;
        case "STRUCTURING", "HIGH_FREQUENCY" -> RiskLevel.MEDIUM;
        default -> RiskLevel.LOW;
    };
}
```

```java
// Example: test naming and deterministic assertions (JUnit 5 + AssertJ)
@Test
void shouldReturnHighRiskForHighAmountRuleAlert() {
    AmlAlert alert = new AmlAlert("HIGH_AMOUNT", "Booking amount exceeds threshold");

    RiskLevel risk = scoringEngine.score(alert);

    assertThat(risk).isEqualTo(RiskLevel.HIGH);
}
```