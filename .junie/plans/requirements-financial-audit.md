---
sessionId: session-260719-183245-1fo8
---

# Requirements

### Overview & Goals
Extend the requirements catalog with a **BaFin-oriented regulatory framework** for financial audit software (focused on supervised payment and e-money institutions), a structured **Anti-Money Laundering (AML) transaction review process**, and a mandatory **TDD development model (Test Driven Development) for the entire software**.

### Scope
#### In Scope
- Derivation of a BaFin-relevant requirements catalog for audit, control, and AML capabilities.
- Mapping regulatory topics to implementable product requirements.
- Extension of existing roles/processes with compliance, evidence, and control aspects.
- Definition of business AML requirements for transaction reviews (scenarios, thresholds, hit handling, escalation).
- Architectural guideline for **easily integrable external system interfaces** via a uniform pattern (adapter/port principle).
- Concrete requirements for **outbound REST API integrations** (calling, authentication, error handling, traceability), including external risk/watchlist services.
- Mandatory anchoring of **TDD (Red-Green-Refactor)** as the development standard for all functional and technical features.

#### Out of Scope
- Legally binding interpretation of individual regulations (legal advice).
- Final legal approval by Compliance/Legal.
- Complete CI/CD tool selection and build infrastructure implementation (separate implementation track).

### Document: Requirements Catalog – Part 1 (Domain & Audit Scope)

#### 1. Vision & Goals
The software supports internal and external financial audits through structured data capture, traceable review steps, and standardized reporting.

**Goal contributions:**
- Reduction of manual audit effort.
- Earlier detection of anomalies and risks.
- Audit-proof traceability of decisions.
- Unified reporting baseline for management and auditors.

#### 2. Stakeholders & Roles

 Role | Primary goals | Responsibilities |
---|---|---|
 Auditor | Execute audits efficiently and completely | Filter data, evaluate audit results, document findings |
 Finance Manager | Transparency on risks and actions | Evaluate reports, manage approvals/follow-ups |
 Admin | Secure and stable operations | User/role management, system settings, access control |

#### 3. Functional Audit Process (Level 1)
1. **Data ingestion**: Import transaction and master data from source systems.
2. **Data validation**: Formal and business plausibility checks.
3. **AML rule checks**: Apply money-laundering indicators (e.g., unusual amounts, suspicious frequency, high-risk countries, smurfing patterns).
4. **Audit & compliance checks**: Additional business and regulatory rules.
5. **Finding handling**: Evaluation, commenting, risk classification, and escalation of anomalies.
6. **Reporting**: Prepare results for different target groups.
7. **Closure & archiving**: Versioned archiving of audit results.

#### 4. Scope Boundaries (MVP vs. Expansion)

**MVP (Must):**
- CSV-based import.
- Basic validation rules.
- Rule-based anomaly marking.
- Exportable audit report.
- Basic roles (Auditor, Manager, Admin).

**Expansion stages (Should/Could):**
- Direct ERP/third-party interfaces via standardized adapters (e.g., REST APIs, DMS, BI).
- Advanced risk-scoring models.
- Collaborative processing and task workflow.
- Dashboards with trend and benchmark analytics.

#### 5. BaFin-Oriented Regulatory Requirements (for the catalog)
Note: The following points are a functional working basis and must be validated project-specifically by Compliance/Legal.

- **Governance & control system**
    - Verifiable internal control system (ICS) with clear responsibilities.
    - Documented policies/procedures for audit-relevant processes.
    - Regular effectiveness checks of controls.

- **Risk management**
    - Systematic risk identification, assessment, and monitoring (including operational/IT risks).
    - Traceable linkage between risk, control, and audit rule.
    - Escalation and action tracking for anomalies.

- **Money laundering prevention (AML) for transactions**
    - Rule-based detection of potential money-laundering patterns at transaction level.
    - Risk-based scoring of hits (low/medium/high) with traceable rationale.
    - Processing workflow for hits including the four-eyes principle for critical cases.
    - Documentation duty for decisions (false positive, confirmed, escalated) with audit-proof timestamp.

- **Information security & IT governance**
    - Role-based access control (least privilege, separation of critical functions).
    - Complete, tamper-protected logging of security- and audit-relevant actions.
    - Protection-level-appropriate measures for confidentiality, integrity, and availability.

- **Outsourcing / third parties / interfaces**
    - Traceable governance of external service providers and connected systems.
    - Auditable interfaces including technical and business data lineage.
    - Uniform port-adapter pattern so new external systems can be integrated in a controlled way.
    - Outbound REST calls to third-party systems with standardized security and logging profile.

- **Data retention, traceability, archiving**
    - Audit-proof historization of findings, decisions, and changes (audit trail).
    - Immutable or verifiably versioned reporting states.
    - Regulation-compliant retention and deletion concepts.

- **Regulatory reporting / incident management / audit readiness**
    - Structured capture and handling of critical incidents.
    - Evidence management for internal/external audits and supervisory requests.
    - Exportable reports for management, internal audit, and supervisors.

#### 6. Acceptance Criteria (updated)
- BaFin-relevant topic blocks are fully translated into product requirements.
- Each regulatory requirement has acceptance criteria and ownership.
- The audit process is described end-to-end and in an audit-ready manner.
- The integration principle allows new third-party systems without changing core logic.
- For every new functional requirement, the related test is specified before implementation (TDD evidence).
- Unit, integration, and API tests are defined as quality gates per feature category.
- The document is directly usable as the basis for steps 2 and 3.

#### 7. Development Principles (TDD mandatory)
- **Red-Green-Refactor as the standard flow:**
    - Red: A failing test describes the desired behavior first.
    - Green: Minimal code implements the requirement.
    - Refactor: Improve structure/readability while keeping tests green.
- **Test types by architecture boundary:**
    - Domain logic (e.g., AML rules, risk scoring): primarily unit tests.
    - Port-adapter integration (e.g., REST adapters): integration/contract tests.
    - End-to-end audit workflows: selective API/system tests for critical paths.
- **Definition of Done per requirement:**
    - Functional test case exists and is traceably linked to REQ-ID.
    - Implementation passes all tests without manual special paths.
    - Refactoring step documented when structural changes were made.
- **Traceability:**
    - Each requirement (REQ-*) references at least one automated test case.
    - Test reports are versioned in an audit-proof manner and stored for auditability.

# Technical Design

### Current Implementation
Current project status is a **Spring Boot skeleton**:
- `pom.xml` (lines 1–43): `spring-boot-starter-parent` 3.5.3, dependencies `spring-boot-starter-web` and `spring-boot-starter-test`, packaging via `spring-boot-maven-plugin`.
- `src/main/java/de/bohnottensen/Main.java` (lines 1–12): single bootstrap class with `@SpringBootApplication`.
- `src/main/resources/application.properties` (line 1): only `spring.application.name=financial_audit`.

### Key Decisions
- The catalog is extended with a **BaFin-oriented compliance section** (governance, risk, security, outsourcing, audit evidence).
- AML is modeled as a dedicated business stream: **detection → assessment → case handling → escalation → evidence**.
- Requirements remain in Markdown as an independent artifact so they can be transformed into tickets/controls/test cases.
- For external integrations, a **port-adapter pattern** is the standard: core business processes stay system-independent, and concrete third-party software is connected through replaceable adapters.
- Each regulatory requirement is modeled as a REQ entry with ownership and testable acceptance criteria.
- **TDD is mandatory for all development streams**: implementation starts only after a failing test (Red), then minimal implementation (Green), then refactoring.
- The test strategy follows a risk-oriented pyramid: many unit tests for domain rules, targeted integration/contract tests for interfaces, and few critical end-to-end tests.

### Proposed Changes
- Extend the document with dedicated sections for **“BaFin-oriented regulatory requirements”**, **“AML transaction review”**, and **“TDD quality requirements”**.
- Translate regulatory topics into concrete requirement classes:
    - GOV-* (Governance/ICS)
    - RSK-* (Risk management)
    - AML-* (Money laundering checks)
    - SEC-* (Information security)
    - INT-* (Third parties/interfaces)
    - AUD-* (Audit trail/evidence/reporting)
    - QLT-* (Test/quality requirements in TDD style)
- Specify the target AML process as an implementable chain:
    - Normalize transactions and extract risk-relevant features.
    - Apply AML rules (thresholds, patterns, country/account risk, frequency).
    - Score hits and map them to case classes.
    - Route critical hits to manual escalation.
- Strengthen the integration chapter for outbound REST communication via port-adapter guardrails:
    - Core system defines stable business ports (e.g., `TransactionSourcePort`, `AmlScreeningPort`, `ReportExportPort`, `ExternalRiskApiPort`).
    - Separate adapter per external system (e.g., `SapImportAdapter`, `DatevImportAdapter`, `ExternalRiskRestAdapter`, `WatchlistRestAdapter`).
    - Technical REST standards per adapter: auth (at least OAuth2/API key depending on target), timeouts, retry with backoff, idempotency strategy, structured error classes.
    - Onboard new systems without changing core processes.
- Add concrete integration requirements for monitoring and auditability:
    - Request/response metadata (without sensitive payload) is logged in an audit-proof manner.
    - Correlation via trace/correlation ID per external call.
- Introduce a TDD implementation framework for later Spring Boot implementation:
    - Create test cases per REQ-ID before functional implementation.
    - Naming conventions link test classes to domain components (`*ServiceTest`, `*AdapterIT`, `*ControllerIT`).
    - Establish contract tests for ports/adapters as an integration barrier.
    - Regression tests for AML rules are mandatory for every rule change.
- Prepare steps 2/3 with regulatorily traceable acceptance criteria and testable quality gates.

### Data Models / Contracts
Template for the next steps:

```text
REQ-<Area>-<Number>
Title
Description
Role/Stakeholder
Priority (Must/Should/Could)
Acceptance criteria
Test case ID(s) / test type (Unit|Integration|API)
Dependencies
Interface port (optional)
Adapter strategy (optional)
```

Example integration requirement:

```text
REQ-INT-001
Standardized data import via source port
New source systems can be connected through adapters without changing the audit core logic.
Auditor/Admin
Should
When a new adapter is registered, the system can use the same import use case unchanged.
Depends on REQ-IMP-001
Interface port: TransactionSourcePort
Adapter strategy: Port-adapter
```

```text
REQ-INT-002
Outbound REST API integration of external systems
The system can call external REST endpoints via a dedicated port and process responses for audit workflows.
Auditor/Admin
Must
On successful calls, business data is processed; on errors, classified error events are logged with correlation ID.
Depends on REQ-SEC-001, REQ-AUD-001
Interface port: ExternalRiskApiPort
Adapter strategy: Port-adapter (REST)
```

### File Structure
No changes to `src/main` are required.
The document is maintained in the existing plan artifact.

# Delivery Steps

###   Step 1: Define a TDD quality framework for the entire requirements catalog
A binding TDD framework is anchored in the catalog as an overarching quality standard.
- Add QLT requirements for Red-Green-Refactor, Definition of Done, and mandatory tests per REQ-ID.
- Define test types per requirement category (unit for domain, integration/contract for adapters, API for critical end-to-end flows).
- Document traceability rules between requirements, test cases, and audit-proof evidence storage.

###   Step 2: Specify AML and compliance requirements in a TDD-ready way
AML and regulatory requirements are described so they can be implemented directly in a test-driven way.
- Convert AML/BaFin requirements into testable acceptance criteria with clear Given-When-Then expectations.
- Add test case references and priorities (Must/Should/Could) per requirement for incremental implementation.
- Anchor four-eyes principle, escalation paths, and audit trail evidence as explicitly testable targets.

###   Step 3: Structure TDD-oriented implementation preparation for Spring Boot and integrations
The future technical implementation is split into TDD-compliant slices for core logic and interfaces.
- Define an implementation view for future Spring Boot components (Controller/Service/Repository + Port/Adapter) including corresponding test levels.
- Define integration quality rules for outbound REST adapters (contract tests, error classes, retry/timeout behavior, correlation ID).
- Structure a prioritized implementation backlog where each work item starts with a test to be implemented first.