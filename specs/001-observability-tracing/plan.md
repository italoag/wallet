# Implementation Plan: Comprehensive Distributed Tracing with OpenTelemetry

**Branch**: `001-observability-tracing` | **Date**: 2025-12-15 | **Updated**: 2025-12-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-observability-tracing/spec.md`

**Spec Clarifications Applied** (2025-12-16):
- Identifier handling: Technical IDs (transaction ID, saga ID, event type, span kind) included as-is; user-related IDs (wallet ID, user ID) hashed
- Specification versions: OTLP 1.0 (stable), W3C Trace Context 1.0, CloudEvents 1.0

## Summary

Implement comprehensive distributed tracing using Micrometer Tracing and OpenTelemetry to provide end-to-end visibility across all system components including API endpoints, use case execution, database operations (JPA/R2DBC), Kafka event publishing/consumption, state machine transitions, and reactive pipelines. The solution will propagate trace context via W3C Trace Context 1.0 headers through CloudEvents 1.0, support configurable sampling with priority-based always-sample rules, sanitize sensitive data (hash user-related IDs, include technical IDs as-is), and export to multiple OTLP 1.0-compatible backends (Zipkin, Jaeger, Tempo) with primary fallback configuration for resilience. Feature flags enable granular runtime control to enable/disable tracing per component (database, Kafka, state machine, external APIs, reactive operations) without service restart.

**Technical Approach**: Leverage Spring Boot auto-configuration for Micrometer Tracing with Brave bridge, implement custom ObservationHandler for state machine transitions, configure reactive context propagation for WebFlux/R2DBC/Redis pipelines, extend CloudEvent 1.0 producers/consumers with W3C Trace Context 1.0 propagation (traceparent/tracestate headers), create aspect-based instrumentation for use cases with sensitive data sanitization filters (SHA-256 hashing for wallet IDs/user IDs, technical IDs as-is), export via OTLP 1.0 to multiple backends, and implement feature flag configuration using Spring Boot @ConditionalOnProperty with Spring Cloud Config refresh capability for runtime updates without downtime.

## Technical Context

**Language/Version**: Java 24 (maven.compiler.release 24)  
**Primary Dependencies**: 
- Spring Boot 3.5.5 (actuator, webflux, data-jpa, data-r2dbc)
- Spring Boot Actuator (for runtime configuration refresh endpoint)
- Spring Cloud Config (optional, for centralized configuration management)
- Micrometer Tracing 1.4.x with Brave bridge
- Micrometer Observation API
- Spring Cloud Stream 4.x with Kafka binder
- CloudEvents 1.0 (io.cloudevents:cloudevents-spring)
- OpenTelemetry OTLP Exporter 1.0 (stable)
- W3C Trace Context 1.0 propagation
- Reactor Core context propagation
- Spring Statemachine 4.x with JPA persistence

**Storage**: 
- JPA (H2/PostgreSQL) for blocking persistence
- R2DBC (H2/Postgres) for reactive persistence
- Redis (reactive) for caching
- MongoDB (reactive) for document storage

**Testing**: 
- JUnit 5 with Spring Boot Test
- Reactor Test for reactive flows
- Spring Cloud Stream Test Binder
- @DataJpaTest slices for persistence
- Testcontainers for integration tests
- OTel SDK test exporter for trace validation

**Target Platform**: JVM (Linux/macOS), containerized (Docker), Kubernetes-ready  

**Project Type**: Backend event-driven microservice (single Maven project)

**Performance Goals**: 
- <5ms tracing overhead per operation
- 1,000 events/second trace throughput
- 10% baseline sampling rate in production
- <100ms p95 for span creation and closure

**Constraints**: 
- No blocking calls in reactive event-loop threads
- Trace context must survive all async boundaries (Kafka, reactive operators, thread hops)
- Zero sensitive data (PII, secrets) in trace spans; user-related identifiers (wallet ID, user ID) must be hashed; technical identifiers (transaction ID, saga ID, event type, span kind) included as-is
- Sampling must capture 100% of errors and slow transactions (>500ms)
- Tracing infrastructure failures must not impact application performance
- Memory overhead <50MB for trace buffers
- Use W3C Trace Context 1.0, CloudEvents 1.0, and OTLP 1.0 specifications for maximum compatibility

**Scale/Scope**: 
- 6 user stories (3 P1, 2 P2, 1 P3)
- ~15 instrumentation points (API, use cases, DB, Kafka, state machine, reactive ops)
- Support for 3 tracing backends (Tempo primary, Zipkin fallback, Jaeger optional)
- 10-20 custom span attributes per operation
- 100+ state machine transitions per saga workflow
- 5 component-level feature flags (database, Kafka, state machine, external APIs, reactive)
- Runtime configuration refresh via Actuator endpoint (<5s update time)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ Principle I: Clean Architecture
**Status**: PASS  
**Rationale**: Tracing instrumentation is infrastructure concern. Implementation will reside in infra/adapter layer using Spring AOP and ObservationHandlers. Domain layer remains unchanged. Trace context propagation via gateway interfaces maintains dependency inversion.

### ✅ Principle II: Event-Driven First
**Status**: PASS  
**Rationale**: Feature enhances event-driven capabilities by adding trace context to CloudEvents. No changes to event immutability or outbox pattern. Trace IDs added as CloudEvent extensions maintain backward compatibility.

### ✅ Principle III: Reactive-Blocking Isolation
**Status**: PASS with vigilance required  
**Rationale**: Feature requires careful handling of reactive context propagation. All span creation in reactive pipelines will use non-blocking operators. Micrometer's reactor-context-propagation library ensures trace context flows through reactive operators without blocking.  
**Risk**: Improper span instrumentation could introduce blocking calls. Mitigation: Use `Observation.start()` with non-blocking scopes, validate with Reactor's debug mode.

### ✅ Principle IV: Test-First Discipline
**Status**: PASS  
**Rationale**: Implementation will follow TDD. Tests will verify: trace ID propagation, span parent-child relationships, context continuity across Kafka, sensitive data sanitization, and sampling rules. OTel SDK test exporter enables asserting on captured spans without real backend.

### ✅ Principle V: Immutability by Default
**Status**: PASS  
**Rationale**: Trace context objects (TraceContext, Span) are immutable. Span attributes added via builders. No mutable state introduced.

### ✅ Principle VI: Observability as First-Class Feature
**Status**: PASS - This feature directly implements this principle  
**Rationale**: Primary purpose is to enhance observability infrastructure. Aligns perfectly with constitution requirement for distributed tracing with Micrometer Tracing (Brave) and correlation ID propagation.

### ✅ Principle VII: Security by Default
**Status**: PASS with implementation required  
**Rationale**: Feature includes FR-006 requiring sensitive data sanitization. Technical identifiers (transaction ID, saga ID, event type, span kind) are safe for inclusion as-is. User-related identifiers (wallet ID, user ID) will be hashed using SHA-256 to prevent user tracking while preserving trace correlation. SQL parameters, HTTP headers, event payloads will be masked. Implementation in dedicated SensitiveDataSanitizer component with configurable safelist.

### ✅ Principle VIII: Idempotency Always
**Status**: PASS  
**Rationale**: Tracing is observational and doesn't affect business logic. Spans can be created multiple times (e.g., retries) without side effects. Trace IDs remain consistent across retries.

### ✅ Principle IX: Fail Fast and Explicit
**Status**: PASS  
**Rationale**: Tracing failures (e.g., backend unavailable, invalid trace headers) log warnings but don't fail operations. Feature includes error span creation with exception details (FR-010).

### ✅ Principle X: Simplicity Over Cleverness
**Status**: PASS  
**Rationale**: Using Spring Boot auto-configuration and standard Micrometer APIs. No custom tracing framework. Instrumentation via well-established patterns (AOP, ObservationHandler).

### ✅ Principle XI: Dependency Discipline
**Status**: PASS - requires formal approval  
**Rationale**: New dependencies required:
- `micrometer-tracing-bridge-brave` (already in pom.xml per README)
- `io.micrometer:context-propagation` (for reactive)
- `io.opentelemetry:opentelemetry-exporter-otlp` (OTLP export)
- All dependencies follow Spring Boot BOM versions
- No duplicate functionality introduced

### ✅ Principle XII: Performance Awareness
**Status**: PASS with measurement required  
**Rationale**: Feature defines performance budget (<5ms overhead per operation in FR-005). Implementation will include benchmarks comparing operation latency with/without tracing. Sampling reduces overhead in production.

**Overall Constitution Compliance**: ✅ PASS - All principles satisfied. Proceed to Phase 0 research.

## Project Structure

### Documentation (this feature)

```text
specs/001-observability-tracing/
├── plan.md              # This file
├── research.md          # Phase 0: Micrometer patterns, reactive context, OpenTelemetry conventions
├── data-model.md        # Phase 1: Trace/Span entities, context propagation model
├── quickstart.md        # Phase 1: Quick setup guide for developers
├── contracts/           # Phase 1: Span attribute schemas, trace header formats
│   └── span-attributes-schema.yaml
└── tasks.md             # Phase 2: (generated by /speckit.tasks)
```

### Source Code (repository root)

```text
src/main/java/dev/bloco/wallet/hub/
├── domain/
│   └── gateway/
│       └── TracingContext.java          # Interface for trace context access (if needed)
├── usecase/
│   └── [existing use cases]             # No changes, instrumented via AOP
├── infra/
│   ├── adapter/
│   │   ├── event/
│   │   │   ├── producer/
│   │   │   │   └── TracingKafkaEventProducer.java     # Wrap producer with trace context
│   │   │   └── consumer/
│   │   │       └── TracingEventConsumerConfig.java    # Extract trace from CloudEvent
│   │   └── tracing/                     # NEW: Tracing infrastructure
│   │       ├── aspect/
│   │       │   ├── UseCaseTracingAspect.java          # Instrument use cases
│   │       │   └── RepositoryTracingAspect.java       # Instrument repository calls
│   │       ├── filter/
│   │       │   ├── WebFluxTracingFilter.java          # Inbound trace context
│   │       │   └── SensitiveDataSanitizer.java        # Mask PII/secrets
│   │       ├── handler/
│   │       │   └── StateMachineObservationHandler.java # State transition spans
│   │       ├── propagation/
│   │       │   ├── CloudEventTracePropagator.java     # W3C trace context to CloudEvent
│   │       │   └── ReactiveContextPropagator.java     # Reactor context propagation
│   │       └── config/
│   │           ├── TracingConfiguration.java          # Main tracing config
│   │           ├── TracingFeatureFlags.java           # Feature flag properties
│   │           ├── SamplingConfiguration.java         # Sampling rules
│   │           └── ObservabilityConfiguration.java    # Observation handlers
│   └── provider/
│       └── data/
│           └── config/
│               └── [existing saga config]             # No changes, observed via handler
└── config/
    └── [existing config classes]                      # No changes

src/main/resources/
├── application.yml                      # Enhanced with tracing config
└── application-tracing.yml              # NEW: Tracing-specific profile

src/test/java/dev/bloco/wallet/hub/
└── infra/
    └── adapter/
        └── tracing/
            ├── aspect/
            │   ├── UseCaseTracingAspectTest.java
            │   └── RepositoryTracingAspectTest.java
            ├── filter/
            │   └── SensitiveDataSanitizerTest.java
            ├── handler/
            │   └── StateMachineObservationHandlerTest.java
            ├── propagation/
            │   ├── CloudEventTracePropagatorTest.java
            │   └── ReactiveContextPropagatorTest.java
            ├── config/
            │   └── TracingFeatureFlagsTest.java       # Feature flag behavior tests
            └── integration/
                ├── EndToEndTracingTest.java          # US1: Complete trace validation
                ├── FeatureFlagIntegrationTest.java   # US1 AS4: Feature flag validation
                ├── KafkaTracePropagationTest.java     # US3: Kafka boundary test
                └── ReactiveTracingTest.java           # US6: Reactive context test
```

**Structure Decision**: Single Maven project structure maintained. All tracing infrastructure added to `infra/adapter/tracing` package to keep instrumentation separate from business logic. Uses existing Spring Boot configuration patterns. Tests organized by component with dedicated integration test suite for end-to-end trace validation.

## Identifier Handling & Sanitization Strategy

**Requirement**: FR-004 and FR-006 require selective inclusion of identifiers based on security implications.

### Safe Identifiers (Include As-Is)

Technical identifiers that don't enable user tracking:
- **transaction.id**: UUID for each transaction (low cardinality per trace)
- **saga.id**: UUID for saga workflow instances
- **event.type**: Event class name (e.g., "FundsAddedEvent")
- **span.kind**: Span type (CLIENT, SERVER, PRODUCER, CONSUMER)
- **operation.name**: Use case or operation type (e.g., "AddFundsUseCase")
- **component.name**: Component identifier (e.g., "database", "kafka", "state-machine")
- **db.statement**: SQL with parameters masked (structure preserved)
- **kafka.topic**: Topic name
- **kafka.partition**: Partition number
- **kafka.offset**: Message offset

### Sensitive Identifiers (Hash with SHA-256)

User-related identifiers that could enable tracking across traces:
- **wallet.id**: Hash to prevent wallet owner identification
- **user.id**: Hash to prevent user tracking
- **customer.name**: Hash or redact completely
- **email**: Hash or mask domain
- **authentication.token**: Never include; redact completely

### Hashing Implementation

```java
public class IdentifierHasher {
    private static final MessageDigest SHA256 = MessageDigest.getInstance("SHA-256");
    
    public static String hashIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "null";
        }
        byte[] hash = SHA256.digest(identifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 16);
    }
}
```

**Example span attributes**:
```json
{
  "transaction.id": "550e8400-e29b-41d4-a716-446655440000",
  "wallet.id.hash": "a3f5b1c2d4e6f7g8",
  "user.id.hash": "x9y8z7w6v5u4t3s2",
  "operation.name": "AddFundsUseCase",
  "event.type": "FundsAddedEvent"
}
```

### Configuration

```yaml
# application-tracing.yml
tracing:
  sanitization:
    safe-identifiers:
      - transaction.id
      - saga.id
      - event.type
      - span.kind
      - operation.name
      - component.name
    hash-identifiers:
      - wallet.id
      - user.id
      - customer.name
    redact-completely:
      - password
      - token
      - secret
      - api-key
```

## Feature Flag Implementation Strategy

**Requirement**: FR-016 requires runtime control of tracing per component without service restart or redeployment.

### Configuration Structure

Component-level feature flags will be implemented using Spring Boot's `@ConfigurationProperties` and `@ConditionalOnProperty`:

```yaml
# application-tracing.yml
tracing:
  features:
    database:
      enabled: true        # JPA and R2DBC tracing
    kafka:
      enabled: true        # Event producer/consumer tracing
    state-machine:
      enabled: true        # State transition tracing
    external-api:
      enabled: true        # HTTP client tracing (future)
    reactive:
      enabled: true        # Reactive pipeline tracing
```

### Runtime Update Mechanism

1. **Spring Boot Actuator Refresh Endpoint**: `/actuator/refresh`
   - Enabled by default with `management.endpoints.web.exposure.include=refresh`
   - Triggers `@RefreshScope` beans to reload configuration
   - No service restart required
   - Changes apply immediately to new operations (existing in-flight operations complete with original config)

2. **Spring Cloud Config Integration** (optional):
   - Centralized configuration server for multi-instance deployments
   - Config changes propagate via `/actuator/refresh` or Spring Cloud Bus
   - Enables consistent feature flags across all service instances

### Implementation Pattern

**TracingFeatureFlags Configuration Class**:
```java
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "tracing.features")
public class TracingFeatureFlags {
    private boolean database = true;
    private boolean kafka = true;
    private boolean stateMachine = true;
    private boolean externalApi = true;
    private boolean reactive = true;
}
```

**Conditional Aspect Activation**:
```java
@Aspect
@Component
@ConditionalOnProperty(value = "tracing.features.database", havingValue = "true", matchIfMissing = true)
public class RepositoryTracingAspect {
    // Database tracing logic
}
```

**Runtime Check in Observability Handler**:
```java
@Component
public class StateMachineObservationHandler implements ObservationHandler<Observation.Context> {
    private final TracingFeatureFlags flags;
    
    @Override
    public void onStart(Observation.Context context) {
        if (!flags.isStateMachine()) {
            return; // Skip tracing when disabled
        }
        // Create state machine span
    }
}
```

### Refresh Workflow

1. **Update Configuration**:
   - Modify `application-tracing.yml` (local development)
   - Update Spring Cloud Config repository (production)

2. **Trigger Refresh**:
   ```bash
   curl -X POST http://localhost:8080/actuator/refresh
   ```

3. **Verification**:
   - Configuration change logs appear in application output
   - New operations reflect updated feature flags immediately
   - Health check endpoint shows current flag states

### Performance Impact

- **Flag Check Overhead**: <1μs per check (simple boolean field access)
- **No Restart Downtime**: Zero-downtime configuration updates
- **Immediate Effect**: New operations use updated flags within milliseconds
- **In-Flight Safety**: Operations started before refresh complete with original configuration

### Testing Strategy

- **Unit Tests**: Verify each aspect/handler respects feature flags
- **Integration Tests**: Validate runtime refresh behavior (US1 AS4)
- **Performance Tests**: Measure overhead of disabled tracing vs no instrumentation

### Security Considerations

- Actuator refresh endpoint secured via Spring Security
- Feature flags visible via `/actuator/env` (requires authentication)
- Audit log entries for configuration changes

**Decision Rationale**: Using Spring Boot's native refresh capability avoids introducing external feature flag services (LaunchDarkly, Unleash) while providing the required runtime control. `@RefreshScope` ensures thread-safe configuration updates without custom synchronization logic.

## W3C Trace Context & CloudEvents Propagation

**Requirement**: FR-003 requires trace context propagation across Kafka using W3C Trace Context 1.0 and CloudEvents 1.0.

### W3C Trace Context 1.0 Format

**traceparent header format**:
```
version-trace_id-parent_id-trace_flags
00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
```

- **version**: `00` (W3C Trace Context 1.0)
- **trace_id**: 32 hex characters (128 bits)
- **parent_id**: 16 hex characters (64 bits - current span ID)
- **trace_flags**: 2 hex characters (8 bits - sampled=01, not sampled=00)

**tracestate header format** (optional):
```
vendor1=value1,vendor2=value2
```

### CloudEvents 1.0 Extensions

Trace context propagated via CloudEvents extension attributes:

```json
{
  "specversion": "1.0",
  "type": "dev.bloco.wallet.funds.added",
  "source": "/wallet-hub/add-funds",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "time": "2025-12-16T10:30:00Z",
  "datacontenttype": "application/json",
  "data": {
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "walletIdHash": "a3f5b1c2d4e6f7g8",
    "amount": 100.00
  },
  "traceparent": "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
  "tracestate": "brave=value1"
}
```

### Implementation in CloudEventTracePropagator

```java
@Component
public class CloudEventTracePropagator {
    private final Tracer tracer;
    
    public CloudEvent injectTraceContext(CloudEvent event) {
        TraceContext context = tracer.currentSpan().context();
        return CloudEventBuilder.from(event)
            .withExtension("traceparent", formatTraceparent(context))
            .withExtension("tracestate", formatTracestate(context))
            .build();
    }
    
    public TraceContext extractTraceContext(CloudEvent event) {
        String traceparent = (String) event.getExtension("traceparent");
        String tracestate = (String) event.getExtension("tracestate");
        return parseW3CTraceContext(traceparent, tracestate);
    }
    
    private String formatTraceparent(TraceContext context) {
        return String.format("00-%s-%s-%02x",
            context.traceIdString(),
            context.spanIdString(),
            context.sampled() ? 1 : 0);
    }
}
```

### OTLP 1.0 Export Configuration

```yaml
# application-tracing.yml
management:
  otlp:
    tracing:
      endpoint: http://tempo:4318/v1/traces  # OTLP 1.0 HTTP endpoint
      protocol: http/protobuf                 # OTLP 1.0 protocol
  tracing:
    sampling:
      probability: 0.1
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans  # Fallback backend
```

### Compatibility Matrix

| Backend | OTLP 1.0 | W3C Trace Context 1.0 | Notes |
|---------|----------|------------------------|-------|
| Grafana Tempo | ✓ | ✓ | Primary backend, native OTLP support |
| Jaeger | ✓ | ✓ | Full compatibility via OTLP collector |
| Zipkin | via bridge | ✓ | Fallback via Micrometer Zipkin reporter |

**Decision Rationale**: Using stable W3C Trace Context 1.0, CloudEvents 1.0, and OTLP 1.0 ensures compatibility with current production observability infrastructure while avoiding instability of draft specifications.

## Complexity Tracking

> No constitution violations detected. This section is intentionally empty.
