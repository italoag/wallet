# Wallet Hub Engineering Constitution

<!--
═══════════════════════════════════════════════════════════════════════════════
SYNC IMPACT REPORT
Version: 1.0.0 → 2.0.0 (MAJOR - complete rewrite with expanded principles)
Date: 2025-12-15
═══════════════════════════════════════════════════════════════════════════════

CHANGES SUMMARY:
- VERSION BUMP: 1.0.0 → 2.0.0 (MAJOR)
  Rationale: Complete restructuring of constitution with expanded governance model
  and comprehensive architectural principles. Breaking change from previous informal
  structure to formal constitutional framework.

MODIFIED PRINCIPLES:
- Expanded from 10 to 12 core principles with detailed rationales
- Added specific rules for reactive programming
- Enhanced security and observability requirements
- Added performance and production-readiness standards

ADDED SECTIONS:
- Formal Governance structure with amendment procedures
- Compliance verification framework
- Detailed architectural constraints
- Testing pyramid requirements
- Dependency management policy
- Performance benchmarks
- Security requirements catalog
- Deployment standards

REMOVED SECTIONS:
- N/A (first formal version)

TEMPLATE CONSISTENCY STATUS:
✅ .specify/templates/plan-template.md - Reviewed, aligns with constitution principles
✅ .specify/templates/spec-template.md - Reviewed, aligns with requirements framework
✅ .specify/templates/tasks-template.md - Reviewed, aligns with testing requirements
⚠ .specify/templates/commands/*.md - Not found, no updates required

FOLLOW-UP TODOs:
- Review README.md to ensure alignment with new constitution version
- Consider creating ADR template for architectural decisions
- Establish compliance review cadence with team

═══════════════════════════════════════════════════════════════════════════════
-->

## Core Principles

### I. Clean Architecture (NON-NEGOTIABLE)

**Principle**: The system MUST adhere to Clean Architecture (Ports & Adapters) with strict layer separation and dependency inversion.

**Rules**:
- Domain layer contains ONLY business entities, events, and gateway interfaces
- Domain MUST NOT depend on any framework or infrastructure
- Use cases orchestrate domain logic and depend ONLY on domain gateways
- Adapters implement gateway interfaces and translate between frameworks and domain
- Dependencies flow inward: infra → usecase → domain (NEVER reversed)
- Infrastructure frameworks (Spring, JPA, Kafka) exist ONLY in adapter/provider layers

**Rationale**: Isolating business logic from frameworks ensures testability, maintainability, and technology independence. This is fundamental to long-term system evolution and prevents framework lock-in.

---

### II. Event-Driven First (NON-NEGOTIABLE)

**Principle**: All significant state changes MUST be modeled as domain events and published reliably.

**Rules**:
- Every domain state mutation generates a corresponding domain event
- Events MUST be immutable (prefer Java `record` types)
- Outbox pattern is MANDATORY for transactional event publishing
- CloudEvents specification MUST be used for event envelopes
- Events MUST include: id, source, type, subject, timestamp, correlation/trace IDs
- Consumers MUST be idempotent (safe to process same event multiple times)
- Event contracts MUST be versioned and backward compatible

**Rationale**: Event-driven architecture enables loose coupling, audit trails, eventual consistency, and system observability. The outbox pattern ensures no events are lost during failures, critical for financial operations.

---

### III. Reactive-Blocking Isolation (NON-NEGOTIABLE)

**Principle**: Reactive and blocking operations MUST be properly isolated to prevent thread starvation.

**Rules**:
- WebFlux, R2DBC, Redis/MongoDB reactive clients run on event-loop threads
- JPA operations, Kafka Template, blocking I/O MUST run on bounded elastic scheduler
- NEVER call blocking code from reactive pipeline without `publishOn(Schedulers.boundedElastic())`
- Use `@Transactional` ONLY in blocking contexts (JPA services)
- Reactive transactions use `TransactionalOperator`
- When bridging: reactive → blocking → reactive, document clearly in code

**Rationale**: Mixing blocking calls in reactive event-loop threads causes thread starvation and system deadlock. This is a common production failure mode that must be prevented at design time.

---

### IV. Test-First Discipline (NON-NEGOTIABLE)

**Principle**: Tests MUST be written and validated to FAIL before implementation begins.

**Rules**:
- Follow Test Pyramid: Unit (fast, no Spring) → Integration (slices) → E2E (minimal)
- Domain tests: pure unit tests, no frameworks, 100% coverage of invariants
- Use case tests: mock gateways, verify orchestration logic
- Messaging tests: Spring Cloud Stream test binder (no real Kafka)
- Persistence tests: `@DataJpaTest` with H2 in-memory
- Saga tests: import only state machine config, verify state transitions
- Contract tests: verify producer/consumer message schemas match
- TDD cycle: Write test → See it FAIL → Implement → See it PASS → Refactor

**Rationale**: Test-first ensures specifications are clear, prevents regressions, and provides living documentation. For financial systems, test coverage is non-negotiable for correctness.

---

### V. Immutability by Default

**Principle**: Prefer immutable data structures; mutability requires explicit justification.

**Rules**:
- Domain events MUST be immutable (use `record`)
- Value objects SHOULD be immutable
- Entity state changes via explicit methods, not direct field mutation
- DTOs and API models SHOULD be immutable where possible
- When mutability needed (JPA entities), use private setters with business methods
- Collections returned from domain methods MUST be unmodifiable

**Rationale**: Immutability eliminates entire classes of bugs (shared state, concurrency), makes code easier to reason about, and is essential for event sourcing patterns.

---

### VI. Observability as First-Class Feature

**Principle**: Every component MUST emit structured logs, metrics, and traces suitable for production monitoring.

**Rules**:
- Structured logging: JSON in production, parameterized messages (SLF4J)
- Correlation IDs propagate through all async boundaries (events, HTTP, database)
- Distributed tracing: Micrometer Tracing (Brave) with propagation to Kafka headers
- Custom metrics: prefix `wallet.` with low-cardinality labels
- Health checks: separate readiness (dependencies) from liveness (application)
- Log levels: DEBUG for detailed flow, INFO for business events, WARN for recoverable issues, ERROR for failures requiring action
- NEVER log PII, secrets, or tokens; mask sensitive fields
- Performance: emit timers for use case execution, event processing, and external calls

**Rationale**: Production systems must be observable to diagnose issues quickly. In distributed event-driven systems, tracing is essential for understanding causality across service boundaries.

---

### VII. Security by Default

**Principle**: Security measures MUST be built-in, not bolted-on.

**Rules**:
- NO secrets in code, configuration files, or version control
- Use Vault, environment variables, or encrypted config for secrets
- Input validation: Bean Validation at entry points, domain invariants in entities
- Authorization: verify ownership/permissions in domain layer when applicable
- Audit logging: security events (auth failures, permission denials) to dedicated log stream
- Dependencies: automated vulnerability scanning (Dependabot), critical CVEs patched within 7 days
- TLS: required for external communication in production
- Rate limiting: protect public endpoints from abuse
- Data minimization: only collect and persist necessary data

**Rationale**: Security breaches in financial systems have catastrophic consequences. Defense-in-depth with multiple layers prevents single points of failure.

---

### VIII. Idempotency Always

**Principle**: All event consumers and API operations MUST be idempotent.

**Rules**:
- Event consumers check deduplication keys before processing
- Use natural business keys (wallet ID, transaction ID) or store processed event IDs
- Database constraints (unique indexes) enforce idempotency at data layer
- Retry mechanisms safe: no double-charging, no duplicate transfers
- APIs: use idempotency keys for write operations (POST, PUT, DELETE)
- Document idempotency strategy for each consumer in code comments

**Rationale**: In distributed systems, messages may be delivered multiple times (at-least-once semantics). Idempotency prevents duplicate financial transactions and maintains system correctness.

---

### IX. Fail Fast and Explicit

**Principle**: Errors MUST be detected early and communicated clearly; NO silent failures.

**Rules**:
- Validate inputs at boundaries; reject invalid data immediately
- Domain methods throw explicit exceptions for business rule violations
- Use checked exceptions ONLY for expected alternate flows
- Use unchecked exceptions for programming errors and unrecoverable failures
- Provide actionable error messages with context (what, why, how to fix)
- Map exceptions to appropriate HTTP status codes at API boundary
- NEVER catch and ignore exceptions without logging
- Circuit breakers fail fast for downstream service issues

**Rationale**: Failing fast reduces debugging time and prevents cascading failures. Explicit error handling makes system behavior predictable and maintainable.

---

### X. Simplicity Over Cleverness

**Principle**: Code clarity MUST take precedence over cleverness; complexity requires justification.

**Rules**:
- Follow YAGNI: implement features only when needed
- Abstraction after 3rd use case (avoid premature abstraction)
- Avoid "framework" creation; use established Spring patterns
- No reflection unless unavoidable (e.g., MapStruct code generation acceptable)
- No overly generic solutions; solve the problem at hand
- Favor composition over inheritance
- Keep methods under 20 lines when possible; extract named methods for clarity
- Cyclomatic complexity: warn at 10, fail at 15

**Rationale**: Simple code is maintainable code. Clever solutions may impress but become maintenance nightmares. This principle protects long-term velocity.

---

### XI. Dependency Discipline

**Principle**: Every dependency MUST be justified; unused dependencies MUST be removed.

**Rules**:
- New dependency checklist (see Governance section)
- Use Spring Boot BOM versions; don't override without ADR
- Avoid duplicate functionality (e.g., multiple JSON libraries)
- Transitive dependencies: verify and document critical ones
- License compatibility: must be compatible with project license
- SBOM (Software Bill of Materials) generated and reviewed
- Quarterly dependency audit: remove unused, update stale
- Critical vulnerabilities: patch within 1 week, review others monthly

**Rationale**: Each dependency is a maintenance liability (security, upgrades, conflicts). Discipline prevents dependency hell and keeps the system lean.

---

### XII. Performance Awareness

**Principle**: Performance requirements MUST be defined and validated; optimization follows measurement.

**Rules**:
- Define performance budgets for critical paths (e.g., event processing < 100ms p95)
- Profile before optimizing; no premature optimization
- Use appropriate data structures (avoid N+1 queries, use pagination)
- Configure connection pools based on load testing, not guesses
- JVM tuning only after measurement (heap, GC pauses)
- Cache judiciously: measure hit rate, invalidate correctly
- Load tests before production: concurrent users, event throughput
- Monitor production metrics: latency percentiles, throughput, error rates

**Rationale**: Performance issues often only surface under load. Defining and testing performance requirements prevents production surprises in high-traffic financial systems.

---

## Architectural Constraints

### Technology Stack (Fixed)

| Component | Technology | Version Policy |
|-----------|-----------|----------------|
| Language | Java | 24 (minimum 21 required) |
| Framework | Spring Boot | 3.5.x (currently 3.5.5) |
| Cloud | Spring Cloud | 2025.0.x |
| Messaging | Spring Cloud Stream + Kafka | Functional bindings only |
| Events | CloudEvents | io.cloudevents:cloudevents-spring |
| Persistence | JPA (H2/PostgreSQL), R2DBC (reactive) | Hibernate 6.x |
| Reactive Data | Redis (reactive), MongoDB (reactive) | Spring Data Reactive |
| Saga | Spring Statemachine | 4.x with JPA persistence |
| Mapping | MapStruct | 1.6.x (code generation) |
| Observability | Micrometer + Brave | Prometheus + OTLP exporters |
| Resilience | Resilience4j | Circuit Breaker pattern |
| Security | Spring Security + OAuth2 | Vault integration for secrets |
| AI (Optional) | Spring AI | Experimental, not in critical paths |

**Version policy**: Follow Spring Boot BOM. Overrides require ADR and team approval.

### Project Structure (Enforced)

```
src/main/java/dev/bloco/wallet/hub/
├── domain/              # Pure business logic, NO framework dependencies
│   ├── event/          # Domain events (immutable records)
│   ├── gateway/        # Port interfaces (repositories, publishers)
│   └── [entities]      # Wallet, User, Transaction (domain models)
├── usecase/            # Orchestration, depends only on domain
│   └── [use-cases]     # CreateWallet, AddFunds, etc.
├── infra/
│   ├── adapter/        # External integrations
│   │   ├── event/
│   │   │   ├── producer/   # Kafka producers
│   │   │   └── consumer/   # Kafka consumers (functional bindings)
│   │   └── [other-adapters]
│   └── provider/       # Infrastructure implementations
│       ├── data/
│       │   ├── config/     # Saga state machine, DB config
│       │   ├── entity/     # JPA entities (WalletEntity, etc.)
│       │   └── repository/ # Spring Data repositories
│       └── mapper/     # MapStruct mappers (DTO ↔ Entity)
└── config/             # Spring configuration classes
```

**Enforcement**: Code reviews MUST verify layer boundaries. No circular dependencies.

---

## Messaging and Event Standards

### Event Publishing Rules

1. **Outbox Pattern (MANDATORY)**:
   - Write domain change + outbox entry in SAME transaction
   - Background worker polls outbox and publishes to Kafka
   - Update outbox status: NEW → PUBLISHED/FAILED
   - Retry with exponential backoff for failures

2. **CloudEvents Envelope**:
   ```java
   CloudEvent.builder()
       .withId(UUID.randomUUID().toString())
       .withSource(URI.create("/wallet-hub"))
       .withType("dev.bloco.wallet.FundsAddedEvent")
       .withSubject("wallet/" + walletId)
       .withTime(OffsetDateTime.now())
       .withData("application/json", eventData)
       .withExtension("traceparent", traceContext)
       .withExtension("correlationid", correlationId)
       .build();
   ```

3. **Kafka Topic Naming**:
   - Pattern: `{domain}-{event-name}-topic`
   - Examples: `wallet-created-topic`, `funds-added-topic`
   - One topic per event type; no multiplexing

4. **Event Versioning**:
   - Include version in `type` or `dataschema` field
   - Example: `dev.bloco.wallet.FundsAddedEvent.v2`
   - Maintain backward compatibility when evolving

### Event Consumption Rules

1. **Functional Bindings**:
   ```java
   @Bean
   public Consumer<CloudEvent<FundsAddedEvent>> fundsAddedConsumer() {
       return event -> { /* idempotent processing */ };
   }
   ```

2. **Idempotency**:
   - Check deduplication key (event ID or business ID)
   - Use database unique constraints as safety net
   - Log duplicate detections for monitoring

3. **Error Handling**:
   - Retryable errors: throw exception (let binder retry)
   - Non-retryable: log and send to DLQ
   - Configure retry: max attempts, backoff interval
   - Dead-letter queue for manual intervention

---

## Testing Requirements

### Test Pyramid (Enforced)

```
       ┌─────────────────┐
       │   E2E (5%)      │  Full system, critical paths only
       ├─────────────────┤
       │ Integration(25%)│  Spring slices, test binder, testcontainers
       ├─────────────────┤
       │  Unit (70%)     │  Pure logic, no Spring context
       └─────────────────┘
```

### Coverage Requirements

- **Domain entities**: 100% coverage of invariants and business rules
- **Use cases**: 95% coverage of orchestration logic
- **Adapters**: 80% coverage (focus on mapping and error handling)
- **Overall project**: Minimum 80% line coverage, 90% branch coverage

### Test Organization

```
src/test/java/dev/bloco/wallet/hub/
├── domain/                    # Pure unit tests
│   ├── WalletTest.java
│   └── event/
│       └── FundsAddedEventTest.java
├── usecase/                   # Use case tests with mocked gateways
│   └── AddFundsUseCaseTest.java
└── infra/
    ├── adapter/event/
    │   ├── producer/          # Test binder tests
    │   └── consumer/          # Consumer idempotency tests
    └── provider/data/
        ├── repository/        # @DataJpaTest with H2
        └── config/            # State machine transition tests
```

### Testing Checklist

- [ ] Test written BEFORE implementation
- [ ] Test fails initially (validates test correctness)
- [ ] Test name follows GivenWhenThen or given_when_then convention
- [ ] No test interdependencies (can run in any order)
- [ ] No test flakiness (deterministic outcomes)
- [ ] Mocks used appropriately (integration points, not domain logic)
- [ ] Test data meaningful (not random strings)
- [ ] Edge cases covered (null, empty, boundary values)

---

## Code Quality Standards

### Naming Conventions

- **Packages**: lowercase, no underscores (`domain`, `usecase`, `infra.adapter.event`)
- **Classes**: PascalCase, descriptive nouns (`CreateWalletUseCase`, `KafkaEventProducer`)
- **Interfaces**: PascalCase, no "I" prefix (`WalletRepository`, not `IWalletRepository`)
- **Methods**: camelCase, verb+noun (`addFunds`, `publishEvent`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_RETRY_ATTEMPTS`)
- **Variables**: camelCase, descriptive (`walletId`, not `wid`)
- **Test methods**: given_when_then or givenWhenThen

### Code Style

- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters maximum
- **Braces**: required even for single statements
- **Imports**: no wildcards, organize by: java, javax, third-party, project
- **Lombok**: use sparingly (avoid `@Data` on JPA entities)
- **Comments**: explain WHY, not WHAT (code should be self-documenting)
- **TODOs**: must include ticket number and date (`// TODO(WALL-123, 2025-12-15): reason`)

### Method Complexity

- **Max lines per method**: 20 (guideline, not hard rule)
- **Cyclomatic complexity**: warn at 10, fail at 15
- **Max parameters**: 5 (consider parameter object if exceeded)
- **Nesting depth**: 3 levels maximum

---

## Performance Benchmarks

### Latency Targets

| Operation | Target (p95) | Max (p99) |
|-----------|--------------|-----------|
| Event processing | 100ms | 250ms |
| Use case execution | 50ms | 150ms |
| Database query | 20ms | 50ms |
| API response | 200ms | 500ms |

### Throughput Requirements

- Event consumption: 1,000 events/second sustained
- API requests: 500 req/second per instance
- Database connections: pool sized for 2x peak load

### Resource Limits

- Heap size: 512MB minimum, 2GB maximum
- Thread pools: bounded elastic (100 threads), parallel (CPU cores * 2)
- Connection pools: DB (20), Redis (10), Kafka (5)

---

## Security Requirements

### Authentication & Authorization

- OAuth2 client configuration for external identity providers
- JWT validation for API access
- Service-to-service: mTLS or signed tokens
- Authorization checks in domain layer for ownership validation

### Secret Management

- Use Vault for production secrets (DB passwords, API keys)
- Environment variables for non-sensitive config
- NO secrets in: code, `application.yml`, Git history
- Secret rotation: automated, < 90 days for critical credentials

### Audit Logging

- Security events: authentication, authorization failures, permission changes
- Structured format: JSON with timestamp, user, action, resource, outcome
- Separate log stream for security audit (cannot be disabled)
- Retention: minimum 1 year for compliance

### Vulnerability Management

- Automated scanning: Dependabot, OWASP Dependency Check
- Critical vulnerabilities: patch within 7 days
- High vulnerabilities: patch within 30 days
- SBOM reviewed quarterly

---

## Deployment Standards

### Container Image

- Build with Spring Boot buildpacks or GraalVM native image
- Base image: updated monthly for security patches
- Image scanning: fail build on critical vulnerabilities
- Size target: < 200MB (JVM), < 100MB (native)

### Configuration

- Externalize ALL environment-specific settings
- 12-factor app principles: config in environment
- Configuration precedence: env vars > application.yml > defaults
- Sensitive config: Vault, not config files

### Database Migrations

- Use Flyway or Liquibase (to be added per ADR)
- Versioned migration scripts in `src/main/resources/db/migration/`
- Applied automatically on startup (dev) or manually (prod)
- Backward-compatible migrations (support rolling deployments)

### Health Checks

- Readiness: checks dependencies (DB, Kafka, Redis)
- Liveness: checks application (no circular dependencies)
- Startup: extended timeout for initial connection setup
- Endpoints: `/actuator/health/readiness`, `/actuator/health/liveness`

### Observability Integration

- Metrics: Prometheus scrape endpoint `/actuator/prometheus`
- Traces: OTLP export to collector (Jaeger, Tempo, Zipkin)
- Logs: structured JSON to stdout (collected by platform)
- Correlation: trace ID propagated in logs, metrics, events

---

## Governance

### Constitution Authority

This Constitution represents the supreme technical governance document for the Wallet Hub project. All technical decisions, architectural choices, code reviews, and engineering practices MUST align with the principles and rules defined herein.

**Hierarchy**:
1. Constitution (this document) - highest authority
2. Architecture Decision Records (ADRs) - specific decisions within constitutional bounds
3. README and other docs - implementation guidance

### Amendment Process

**Requirements for Constitution Amendment**:

1. **Proposal Phase**:
   - Create Pull Request with proposed changes to this document
   - PR description MUST include:
     - Rationale for change (what problem does it solve?)
     - Impact analysis (which systems/teams affected?)
     - Migration plan (how to transition existing code?)
     - Alternative approaches considered
   - Minimum 2 reviewers required, one must be architect/tech lead

2. **Review Phase**:
   - Team discussion: minimum 3 business days for feedback
   - Address concerns, revise proposal as needed
   - Document consensus or voting outcome

3. **Approval Phase**:
   - Requires approval from: 2/3 of senior engineers + tech lead
   - For MAJOR changes (new principle, removing principle): unanimous senior approval

4. **Implementation Phase**:
   - Update constitution version (semantic versioning):
     - MAJOR: backward-incompatible changes (new/removed principles)
     - MINOR: new sections, expanded guidance
     - PATCH: clarifications, typo fixes
   - Update Sync Impact Report (HTML comment at top of file)
   - Create ADR documenting the amendment and migration plan
   - Update dependent templates, README, documentation

### Version Semantics

- **Version format**: MAJOR.MINOR.PATCH (e.g., 2.0.0)
- **MAJOR increment**: Breaking changes (new required principles, removed principles, redefined architectural constraints)
- **MINOR increment**: Additive changes (new optional principles, expanded guidance, new sections)
- **PATCH increment**: Non-substantive (typos, clarifications, formatting)

### Compliance Verification

**Code Review Checklist** (reviewers MUST verify):

- [ ] Layer boundaries respected (no framework in domain)
- [ ] Events published via outbox pattern
- [ ] Reactive-blocking isolation maintained
- [ ] Tests written first and cover requirements
- [ ] Immutability used where appropriate
- [ ] Logging includes correlation/trace IDs, no PII
- [ ] Security: no secrets, input validated, authorization checked
- [ ] Idempotency verified for event consumers
- [ ] Error handling explicit and actionable
- [ ] Code simple and clear, complexity justified
- [ ] No new dependencies without checklist completion
- [ ] Performance implications considered

**Automated Checks** (CI pipeline):

- [ ] Test coverage meets minimums (80% line, 90% branch)
- [ ] Build succeeds with no test failures
- [ ] Static analysis passes (SpotBugs, ErrorProne if configured)
- [ ] No critical/high vulnerabilities in dependencies
- [ ] Code formatting consistent (google-java-format)

**Periodic Reviews**:

- **Monthly**: Dependency audit (vulnerabilities, unused deps)
- **Quarterly**: Architecture review against constitution
- **Per Release**: Constitution compliance report

### Dependency Approval Checklist

Before introducing a new dependency, the following MUST be documented in PR:

1. [ ] **Problem Statement**: What problem does this solve?
2. [ ] **Native Alternative**: Can Spring Boot/Cloud solve this? Why insufficient?
3. [ ] **License**: Compatible with project license? (Apache 2.0, MIT, BSD acceptable)
4. [ ] **Maturity**: Project active? Recent commits (< 6 months)?
5. [ ] **Size**: JAR size justified? (> 10MB requires strong justification)
6. [ ] **Maintenance**: Who maintains it? Corporate or community backed?
7. [ ] **Security**: Known vulnerabilities? OWASP check passed?
8. [ ] **Alternatives**: What other options considered? Why this one?
9. [ ] **Integration**: Test coverage for integration provided?
10. [ ] **Exit Strategy**: If experimental, how to remove? Abstraction layer?

**Rejection Criteria** (any one fails the dependency):

- Incompatible license (GPL, AGPL, proprietary without approval)
- No activity in > 12 months (abandoned project)
- Known critical vulnerabilities with no fix available
- Duplicates existing functionality (without compelling advantage)
- Size > 50MB without exceptional justification

### Variance and Exceptions

**When Constitution principles cannot be followed**:

1. Document exception in code with `@Constitution.Variance` annotation:
   ```java
   /**
    * @Constitution.Variance
    * Principle: Reactive-Blocking Isolation
    * Reason: Legacy third-party library requires blocking call
    * Mitigation: Isolated to adapter layer, uses boundedElastic scheduler
    * Approved: 2025-12-15, Tech Lead John Doe
    * Review: 2026-06-15 (revisit in 6 months)
    */
   @Override
   public Mono<Result> processLegacyCall() {
       return Mono.fromCallable(() -> legacyLibrary.blockingCall())
           .subscribeOn(Schedulers.boundedElastic());
   }
   ```

2. Track variances in ADR for review and elimination

3. Variances are temporary; plan to resolve within 6 months

---

## References

### Framework Documentation

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Cloud Stream](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/)
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/reference/)
- [CloudEvents Spec](https://cloudevents.io/)
- [Spring Statemachine](https://docs.spring.io/spring-statemachine/docs/current/reference/)
- [Resilience4j](https://resilience4j.readme.io/)
- [MapStruct](https://mapstruct.org/)
- [Micrometer](https://micrometer.io/)

### Design Patterns

- [Clean Architecture (Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Outbox Pattern (Microservices.io)](https://microservices.io/patterns/data/transactional-outbox.html)
- [Saga Pattern (Microservices.io)](https://microservices.io/patterns/data/saga.html)
- [Circuit Breaker (Martin Fowler)](https://martinfowler.com/bliki/CircuitBreaker.html)

### Best Practices

- [12-Factor App](https://12factor.net/)
- [Reactive Manifesto](https://www.reactivemanifesto.org/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)

---

**Version**: 2.0.0 | **Ratified**: 2025-12-15 | **Last Amended**: 2025-12-15

---

## Glossary

**Aggregate**: A cluster of domain objects treated as a single unit for consistency (e.g., Wallet is aggregate root for its transactions).

**Bounded Context**: A boundary within which a particular domain model applies (aligns with microservice boundaries).

**Circuit Breaker**: Resilience pattern that prevents cascading failures by stopping requests to failing services.

**CloudEvents**: Vendor-neutral specification for event envelope format ensuring interoperability.

**Domain Event**: Immutable fact representing something significant that happened in the domain.

**Gateway**: Interface (port) defining contract for external dependencies (infrastructure abstraction).

**Idempotency**: Property where operation produces same result regardless of how many times executed.

**Outbox Pattern**: Transactional pattern ensuring reliable event publishing by storing events in database alongside state changes.

**Reactive Streams**: Asynchronous stream processing with non-blocking backpressure (Project Reactor implementation).

**Saga**: Long-running transaction pattern coordinating multiple services with compensation logic.

**State Machine**: Formal model of system states and transitions for saga orchestration.

**Use Case**: Application-specific business rule (interactor) orchestrating domain logic.

---

*This constitution is a living document. Propose improvements via Pull Request following the Amendment Process.*
