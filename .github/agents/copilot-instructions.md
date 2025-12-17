# Wallet Hub Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-12-15

## Active Technologies

### Core Stack
- **Language**: Java 24 (maven.compiler.release 24)
- **Framework**: Spring Boot 3.5.5, Spring Cloud 2025.0.x
- **Reactive**: Project Reactor, WebFlux, R2DBC, Reactive Redis/MongoDB
- **Messaging**: Spring Cloud Stream + Kafka (functional bindings + CloudEvents)
- **Persistence**: JPA (H2/PostgreSQL), R2DBC (reactive)
- **State Machine**: Spring Statemachine 4.x with JPA persistence
- **Build**: Maven (use `./mvnw`)

### Observability Stack (Feature 001)
- **Tracing**: Micrometer Tracing with Brave bridge
- **Observation API**: Micrometer Observation for unified instrumentation
- **Export**: OpenTelemetry Protocol (OTLP) to Zipkin/Jaeger/Tempo
- **Context Propagation**: W3C Trace Context via CloudEvent extensions
- **Reactive Context**: Automatic propagation via `Hooks.enableAutomaticContextPropagation()`
- **Sampling**: Tail-based with 10% baseline, 100% errors/slow transactions
- **Sensitive Data**: Safelist-based sanitization for span attributes

## Project Structure

### Source Code
```text
src/main/java/dev/bloco/wallet/hub/
├── domain/                           # Clean Architecture: Pure business logic
│   ├── event/                        # Domain events (immutable records)
│   ├── gateway/                      # Port interfaces (no implementation)
│   └── [entities]                    # Wallet, User, Transaction
├── usecase/                          # Orchestration layer (depends only on domain)
│   └── [use-cases]                   # CreateWallet, AddFunds, Transfer, etc.
├── infra/
│   ├── adapter/                      # External integrations
│   │   ├── event/
│   │   │   ├── producer/             # Kafka producers (CloudEvents)
│   │   │   └── consumer/             # Kafka consumers (functional bindings)
│   │   └── tracing/                  # NEW: Tracing infrastructure (Feature 001)
│   │       ├── aspect/               # AOP instrumentation (use cases, repos)
│   │       ├── filter/               # WebFlux tracing filter, data sanitization
│   │       ├── handler/              # ObservationHandler for state machine
│   │       ├── propagation/          # Trace context propagation (Kafka, reactive)
│   │       └── config/               # Tracing, sampling, observation config
│   └── provider/                     # Infrastructure implementations
│       ├── data/
│       │   ├── config/               # Saga state machine, DB config
│       │   ├── entity/               # JPA entities (WalletEntity, etc.)
│       │   └── repository/           # Spring Data repositories + impls
│       └── mapper/                   # MapStruct mappers (DTO ↔ Entity)
└── config/                           # Spring configuration classes
```

### Testing
```text
src/test/java/dev/bloco/wallet/hub/
├── domain/                           # Pure unit tests (no Spring context)
├── usecase/                          # Use case tests with mocked gateways
└── infra/
    ├── adapter/tracing/              # NEW: Tracing tests (Feature 001)
    │   ├── aspect/                   # AOP instrumentation tests
    │   ├── filter/                   # Sanitization tests
    │   ├── handler/                  # State machine observation tests
    │   ├── propagation/              # Context propagation tests
    │   └── integration/              # End-to-end trace validation
    └── provider/data/
        ├── repository/               # @DataJpaTest with H2
        └── config/                   # State machine tests
```

## Commands

### Build & Run
```bash
# Clean build
./mvnw clean install

# Run application (dev with H2)
./mvnw spring-boot:run

# Run with tracing profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=tracing

# Run tests
./mvnw test

# Run specific test
./mvnw -Dtest=FullyQualifiedTestName test

# Build without tests
./mvnw -DskipTests package
```

### Tracing (Feature 001)
```bash
# Start Zipkin backend
docker compose up -d zipkin

# View traces in Zipkin
open http://localhost:9411

# Check application metrics (includes trace stats)
curl http://localhost:8080/actuator/metrics/tracing

# Export trace data (Zipkin API)
curl "http://localhost:9411/api/v2/traces?serviceName=wallet-hub&limit=10"
```

## Code Style

### Java 24 Conventions
- **Records**: Use for immutable value objects, domain events
- **Pattern matching**: Leverage for type checks and switches
- **Text blocks**: Use for multi-line SQL, JSON, YAML
- **Sealed classes**: Use for restricted hierarchies (state enums, event types)

### Clean Architecture Rules (NON-NEGOTIABLE)
- Domain layer: NO framework dependencies (no Spring, JPA, Kafka)
- Use cases: Depend ONLY on domain gateways (interfaces)
- Adapters: Implement gateway interfaces, handle framework integration
- Dependencies flow inward: `infra → usecase → domain` (NEVER reversed)
- Use constructor injection everywhere (no field injection)

### Reactive vs Blocking Isolation (NON-NEGOTIABLE)
- WebFlux, R2DBC, Redis/Mongo: Run on event-loop threads
- JPA, Kafka Template: Run on bounded elastic scheduler
- **NEVER** call blocking code from reactive pipeline without `publishOn(Schedulers.boundedElastic())`
- Document thread hops clearly in code comments

### Observability Patterns (Feature 001)
- **Use Case Instrumentation**: Use AOP with `@Aspect`, wrap execution in `Observation.observe()`
- **Manual Spans**: Use `Observation.createNotStarted(...).observe(() -> {...})` for custom operations
- **Span Attributes**: Follow OpenTelemetry semantic conventions (see below)
- **Span Events**: Use `span.event("event.name")` for point-in-time annotations
- **Reactive Context**: Already auto-propagates via `Hooks.enableAutomaticContextPropagation()`
- **Kafka Propagation**: Use CloudEvent extensions (`traceparent`, `tracestate`)

### OpenTelemetry Span Attribute Naming
```java
// Database operations
attributes.put("db.system", "postgresql");
attributes.put("db.operation", "SELECT");
attributes.put("db.statement", sanitizer.sanitizeSql(sql)); // Parameterized, no values

// Messaging (Kafka)
attributes.put("messaging.system", "kafka");
attributes.put("messaging.destination", "funds-added-topic");
attributes.put("messaging.operation", "publish");
attributes.put("messaging.message_id", eventId);

// Wallet domain
attributes.put("wallet.id", walletId);
attributes.put("wallet.operation", "transfer");
attributes.put("transaction.id", txId);

// State machine
attributes.put("statemachine.state.from", "PENDING");
attributes.put("statemachine.state.to", "COMPLETED");
attributes.put("statemachine.event", "FUNDS_ADDED");

// Errors
attributes.put("error", true);
attributes.put("error.type", exception.getClass().getSimpleName());
attributes.put("error.message", sanitizer.sanitizeMessage(exception.getMessage()));
```

### Sensitive Data Sanitization
```java
// ALWAYS sanitize before adding to spans
String sanitizedSql = sql.replaceAll("= '[^']*'", "= '***'");
String sanitizedEmail = text.replaceAll("\\b[A-Za-z0-9._%+-]+@[^\\s]+", "***@***.***");

// Safelist approach for HTTP headers, DB fields
Set<String> SAFE_HEADERS = Set.of("content-type", "accept", "user-agent");
```

### Testing Patterns
- **Domain tests**: Pure unit tests, no frameworks, 100% coverage of invariants
- **Use case tests**: Mock gateways, verify orchestration logic
- **Messaging tests**: Spring Cloud Stream test binder (no real Kafka)
- **Persistence tests**: `@DataJpaTest` with H2 in-memory
- **Tracing tests**: Use OTel SDK test exporter to assert on captured spans
- **TDD**: Write test → See it FAIL → Implement → See it PASS → Refactor

## Recent Changes

### Feature 001: Observability Tracing (2025-12-15)
- Added comprehensive distributed tracing with Micrometer Tracing
- Implemented W3C Trace Context propagation via CloudEvents
- Configured reactive context auto-propagation
- Created AOP-based instrumentation for use cases and repositories
- Implemented state machine ObservationHandler for transition tracking
- Configured tail-based sampling (10% baseline, 100% errors/slow ops)
- Added sensitive data sanitization for span attributes
- Set up multi-backend export (Tempo primary, Zipkin fallback)

**Key Files**:
- `infra/adapter/tracing/aspect/UseCaseTracingAspect.java`: AOP instrumentation
- `infra/adapter/tracing/propagation/CloudEventTracePropagator.java`: Kafka trace propagation
- `infra/adapter/tracing/filter/SensitiveDataSanitizer.java`: PII/secrets masking
- `infra/adapter/tracing/handler/StateMachineObservationHandler.java`: State machine spans
- `config/ReactiveContextConfig.java`: Enables automatic context propagation

**Testing**:
- `infra/adapter/tracing/integration/EndToEndTracingTest.java`: Complete trace validation
- `infra/adapter/tracing/propagation/CloudEventTracePropagatorTest.java`: Kafka boundary test
- `infra/adapter/tracing/propagation/ReactiveContextPropagatorTest.java`: Reactive context test

**Documentation**:
- [Spec](specs/001-observability-tracing/spec.md): User stories and requirements
- [Plan](specs/001-observability-tracing/plan.md): Technical design and architecture
- [Research](specs/001-observability-tracing/research.md): Technology decisions and patterns
- [Data Model](specs/001-observability-tracing/data-model.md): Trace/Span entity definitions
- [Contracts](specs/001-observability-tracing/contracts/span-attributes-schema.yaml): Attribute naming conventions
- [Quickstart](specs/001-observability-tracing/quickstart.md): Developer setup guide

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
