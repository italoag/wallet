# Research: Comprehensive Distributed Tracing with OpenTelemetry

**Feature**: 001-observability-tracing  
**Phase**: 0 (Outline & Research)  
**Date**: 2025-12-15

## Purpose

Resolve all technical unknowns and clarifications from the implementation plan before proceeding to design phase. This research covers Micrometer Tracing patterns, reactive context propagation, OpenTelemetry semantic conventions, and Spring Boot auto-configuration options.

---

## Research Item 1: Micrometer Observation API Patterns

### Question
How should we instrument use cases, repository operations, and state machine transitions using Micrometer Observation API without introducing framework dependencies into domain layer?

### Research Findings

**Micrometer Observation API** provides a unified abstraction for creating spans (traces), metrics, and logs from a single instrumentation point. Key components:

1. **Observation**: Represents an observed operation (e.g., use case execution)
2. **ObservationRegistry**: Central registry for observation handlers
3. **ObservationHandler**: Processes observations (converts to spans, metrics, logs)
4. **ObservationConvention**: Defines span names and attributes

**Best Practice for Clean Architecture**:
- Use **AOP (AspectJ)** to intercept use case methods and wrap execution in `Observation.start()`
- Keep domain layer free of observability code
- Create custom `ObservationConvention` for business-specific span naming

**Example Pattern**:
```java
@Aspect
@Component
public class UseCaseTracingAspect {
    private final ObservationRegistry registry;
    
    @Around("@within(dev.bloco.wallet.hub.usecase.*)")
    public Object traceUseCase(ProceedingJoinPoint pjp) throws Throwable {
        Observation observation = Observation.createNotStarted(
            "usecase." + pjp.getSignature().getName(),
            registry
        ).lowCardinalityKeyValue("usecase.class", pjp.getTarget().getClass().getSimpleName());
        
        return observation.observe(() -> pjp.proceed());
    }
}
```

**Decision**: Use AOP-based instrumentation with custom ObservationConvention for use cases and repository operations. State machine transitions require custom ObservationHandler integrated with Spring Statemachine listeners.

**Alternatives Considered**:
- Manual instrumentation in each use case → Rejected (violates Clean Architecture, high maintenance)
- Bytecode weaving → Rejected (adds complexity, harder to debug)

---

## Research Item 2: Reactive Context Propagation

### Question
How to maintain trace context across reactive operators (flatMap, subscribeOn, publishOn) and thread boundaries in WebFlux, R2DBC, and Redis operations?

### Research Findings

**Challenge**: Reactor's `Context` is immutable and thread-local. Standard ThreadLocal-based tracing breaks across async boundaries.

**Solution**: Micrometer's `reactor-context-propagation` library + Reactor 3.5+ automatic context propagation

**Key Components**:
1. **ContextSnapshot**: Captures current trace context
2. **ContextRegistry**: Registers context propagation strategies
3. **Hooks.enableAutomaticContextPropagation()**: Automatically propagates context through reactive operators

**Configuration**:
```java
@Configuration
public class ReactiveContextConfig {
    static {
        // Enable automatic context propagation globally
        Hooks.enableAutomaticContextPropagation();
    }
    
    @Bean
    public ObservationRegistry observationRegistry() {
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig()
            .observationHandler(new PropagatingReceiverTracingObservationHandler<>(
                tracer, propagator
            ));
        return registry;
    }
}
```

**WebFlux Integration**: Spring Boot 3.2+ automatically configures WebFlux filters to extract trace context from HTTP headers and populate Reactor Context.

**R2DBC/Redis Integration**: Use `DatabaseClient.sql().contextWrite()` to inject trace context:
```java
databaseClient.sql("SELECT * FROM wallet")
    .fetch()
    .all()
    .contextWrite(ctx -> ctx.putAll(ContextSnapshot.captureAll()))
```

**Decision**: Enable automatic context propagation globally via Hooks. Use Spring Boot auto-configuration for WebFlux. Manually inject context for R2DBC/Redis operations in repository implementations.

**Alternatives Considered**:
- Manual context propagation with .contextWrite() everywhere → Rejected (error-prone, verbose)
- ThreadLocal propagation with scheduler decorators → Rejected (doesn't work with WebFlux)

---

## Research Item 3: CloudEvent Trace Context Propagation

### Question
How to propagate W3C Trace Context through Kafka using CloudEvent extension attributes while maintaining CloudEvents specification compliance?

### Research Findings

**W3C Trace Context Specification** defines two headers:
- `traceparent`: `00-{trace-id}-{span-id}-{flags}`
- `tracestate`: vendor-specific trace information

**CloudEvents Integration**: Use CloudEvent extension attributes (key-value pairs that extend the envelope):
```json
{
  "specversion": "1.0",
  "type": "dev.bloco.wallet.FundsAddedEvent",
  "source": "/wallet-hub",
  "id": "abc123",
  "data": {...},
  "traceparent": "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
  "tracestate": "congo=t61rcWkgMzE"
}
```

**Implementation Pattern**:
```java
public class CloudEventTracePropagator {
    private final Tracer tracer;
    
    public CloudEvent injectTraceContext(CloudEvent event) {
        TraceContext context = tracer.currentSpan().context();
        return CloudEventBuilder.from(event)
            .withExtension("traceparent", formatTraceparent(context))
            .withExtension("tracestate", context.traceState())
            .build();
    }
    
    public void extractTraceContext(CloudEvent event) {
        String traceparent = event.getExtension("traceparent");
        if (traceparent != null) {
            TraceContextOrSamplingFlags extracted = TraceContext.extract(traceparent);
            tracer.nextSpan(extracted).start();
        }
    }
}
```

**Kafka Header Alternative**: Spring Cloud Stream supports Kafka headers. However, CloudEvents is the standardized event envelope already in use. Adding trace context as CloudEvent extensions maintains consistency.

**Decision**: Use CloudEvent extension attributes (`traceparent`, `tracestate`) for trace propagation. Create CloudEventTracePropagator utility to inject/extract trace context. Update existing KafkaEventProducer and consumer configuration.

**Alternatives Considered**:
- Raw Kafka headers → Rejected (breaks CloudEvents abstraction)
- Event payload modification → Rejected (pollutes domain events with infrastructure concerns)

---

## Research Item 4: OpenTelemetry Semantic Conventions

### Question
What attribute naming conventions should be used for span attributes to ensure compatibility with standard observability tooling?

### Research Findings

**OpenTelemetry Semantic Conventions** provide standardized attribute names for common operations:

**Database Operations**:
- `db.system`: Database system (e.g., "postgresql", "h2")
- `db.connection_string`: Connection string (sanitized)
- `db.statement`: SQL statement (parameterized, no actual values)
- `db.operation`: Operation type (e.g., "SELECT", "INSERT")
- `db.name`: Database name

**Messaging (Kafka)**:
- `messaging.system`: "kafka"
- `messaging.destination`: Topic name
- `messaging.destination_kind`: "topic"
- `messaging.operation`: "publish" or "receive"
- `messaging.message_id`: Event/message ID
- `messaging.kafka.partition`: Partition number
- `messaging.kafka.consumer_group`: Consumer group ID

**HTTP/API**:
- `http.method`: HTTP method (GET, POST, etc.)
- `http.url`: Full URL (sanitized)
- `http.status_code`: Response status code
- `http.route`: Route pattern (e.g., "/api/wallets/{id}")

**Custom (Wallet Hub specific)**:
- `wallet.id`: Wallet identifier (business ID, not PII)
- `wallet.operation`: Operation type (create, add_funds, withdraw, transfer)
- `transaction.id`: Transaction identifier
- `statemachine.id`: Saga instance ID
- `statemachine.state.from`: Source state
- `statemachine.state.to`: Target state
- `statemachine.event`: Triggering event

**Decision**: Follow OpenTelemetry semantic conventions for standard operations (DB, messaging, HTTP). Create custom namespace `wallet.*` for domain-specific attributes. Document conventions in `/contracts/span-attributes-schema.yaml`.

**Alternatives Considered**:
- Custom naming for everything → Rejected (loses tool compatibility)
- No conventions → Rejected (inconsistent, hard to query)

---

## Research Item 5: Sampling Strategies

### Question
How to implement configurable sampling that captures 100% of errors/slow transactions while sampling only 10% of normal operations in production?

### Research Findings

**Micrometer Tracing Sampling Types**:
1. **Probability-based**: Sample X% of all traces
2. **Rate-limiting**: Sample up to N traces per second
3. **Rule-based**: Sample based on span attributes (custom)

**Brave Sampler Composition**:
```java
@Bean
public Sampler sampler() {
    return Sampler.create(new SamplingRule() {
        @Override
        public Boolean matches(TraceContext context) {
            // Always sample errors
            if (context.hasError()) return true;
            
            // Always sample slow operations (requires custom logic)
            Long duration = getDuration(context);
            if (duration != null && duration > 500_000_000L) return true; // 500ms
            
            // Sample 10% of everything else
            return context.traceId() % 10 == 0;
        }
    });
}
```

**Challenge**: Sampler decision happens at span creation, before duration is known. Slow transaction detection requires post-sampling adjustment.

**Solution**: Use **tail-based sampling** with custom Span exporter:
1. Buffer all spans for short period (e.g., 5 seconds)
2. After trace completes, evaluate: errors, duration, custom rules
3. If trace matches "always sample" rules, mark for export
4. Otherwise, apply probability sampling (10%)

**Implementation with Micrometer**:
```java
@Bean
public SpanExporter spanExporter(List<SpanExporter> backends) {
    return new TailSamplingSpanExporter(backends, samplingConfig);
}

public class TailSamplingSpanExporter implements SpanExporter {
    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        for (SpanData span : spans) {
            if (shouldAlwaysSample(span)) {
                exportToAllBackends(span);
            } else if (probabilitySample(span)) {
                exportToPrimaryBackend(span);
            }
        }
    }
    
    private boolean shouldAlwaysSample(SpanData span) {
        return span.getStatus().getStatusCode() == StatusCode.ERROR
            || span.getEndEpochNanos() - span.getStartEpochNanos() > 500_000_000L;
    }
}
```

**Decision**: Implement tail-based sampling with custom SpanExporter. Configure 10% probability baseline, always sample errors and operations >500ms. Buffer spans for 5 seconds to evaluate complete trace before export decision.

**Alternatives Considered**:
- Head-based sampling only → Rejected (can't detect slow transactions at start)
- Probabilistic per-span → Rejected (doesn't guarantee capture of important traces)
- Always sample everything → Rejected (too expensive at scale)

---

## Research Item 6: Sensitive Data Sanitization

### Question
How to prevent PII and secrets from appearing in span attributes while allowing useful debugging information?

### Research Findings

**Risk Areas**:
1. SQL statements with parameters (e.g., `WHERE email = 'user@example.com'`)
2. HTTP headers (Authorization, Cookie)
3. Event payloads (user data, payment info)
4. Exception messages (may contain user input)

**Approach**: Safelist + Pattern-based masking

**Safelist Strategy**:
```java
public class SensitiveDataSanitizer {
    private static final Set<String> SAFE_DB_FIELDS = Set.of(
        "id", "wallet_id", "transaction_id", "status", "created_at"
    );
    
    private static final Set<String> SAFE_HTTP_HEADERS = Set.of(
        "content-type", "accept", "user-agent"
    );
    
    public String sanitizeSql(String sql) {
        // Remove parameter values, keep structure
        return sql.replaceAll("= '[^']*'", "= '***'")
                  .replaceAll("= [0-9]+", "= ***");
    }
    
    public Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        return headers.entrySet().stream()
            .filter(e -> SAFE_HTTP_HEADERS.contains(e.getKey().toLowerCase()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    public String maskEmail(String text) {
        return text.replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", 
            "***@***.***");
    }
}
```

**Integration with Micrometer**:
```java
@Bean
public ObservationHandler<Observation.Context> sanitizingHandler() {
    return new ObservationHandler<>() {
        @Override
        public void onStop(Observation.Context context) {
            Span span = getSpan(context);
            span.getAttributes().forEach((key, value) -> {
                if (key.startsWith("db.statement")) {
                    span.tag(key, sanitizer.sanitizeSql(value));
                } else if (key.startsWith("http.header")) {
                    span.tag(key, "***");
                }
            });
        }
    };
}
```

**Decision**: Implement SensitiveDataSanitizer with safelist approach. Apply sanitization in custom ObservationHandler before spans export. Use regex patterns for email/phone/credit card masking. Document safe fields in configuration.

**Alternatives Considered**:
- Blocklist approach → Rejected (unsafe, easy to miss fields)
- No sanitization, rely on backend → Rejected (data already leaked)
- Encrypt sensitive data → Rejected (defeats purpose of debugging)

---

## Research Item 7: Multiple Backend Export with Fallback

### Question
How to export traces to multiple backends (Tempo primary, Zipkin fallback, Jaeger optional) with automatic failover?

### Research Findings

**Micrometer Tracing** supports multiple exporters via `CompositeSpanExporter`.

**Spring Boot Auto-Configuration**: Detects available exporters on classpath and configures them automatically.

**Configuration Pattern**:
```yaml
management:
  tracing:
    sampling:
      probability: 0.1
  otlp:
    tracing:
      endpoint: http://tempo:4318/v1/traces
      timeout: 10s
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
  
# Custom configuration for fallback behavior
tracing:
  backends:
    primary: tempo
    fallback: zipkin
    optional:
      - jaeger
  resilience:
    circuit-breaker:
      failure-threshold: 5
      wait-duration-in-open-state: 60s
```

**Implementation**:
```java
@Configuration
public class MultiBackendTracingConfig {
    @Bean
    public SpanExporter compositeExporter(
        OtlpHttpSpanExporter tempoExporter,
        ZipkinSpanExporter zipkinExporter,
        @Qualifier("jaegerExporter") SpanExporter jaegerExporter
    ) {
        return new ResilientCompositeSpanExporter(
            List.of(tempoExporter, zipkinExporter, jaegerExporter),
            circuitBreakerRegistry
        );
    }
}

public class ResilientCompositeSpanExporter implements SpanExporter {
    private final List<SpanExporter> exporters;
    private final CircuitBreakerRegistry cbRegistry;
    
    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        SpanExporter primary = exporters.get(0);
        CircuitBreaker cb = cbRegistry.circuitBreaker("tracing-primary");
        
        return Try.ofSupplier(
            CircuitBreaker.decorateSupplier(cb, () -> primary.export(spans))
        ).recover(throwable -> {
            log.warn("Primary exporter failed, using fallback", throwable);
            return exporters.get(1).export(spans);
        }).get();
    }
}
```

**Decision**: Use CompositeSpanExporter with Resilience4j Circuit Breaker for primary backend. If primary fails (circuit opens), route to fallback. Optional backends receive traces asynchronously (fire-and-forget). Health check reports circuit breaker states.

**Alternatives Considered**:
- Single backend → Rejected (single point of failure)
- Load balancing → Rejected (want specific primary for production queries)
- Retry only → Rejected (doesn't handle sustained outages)

---

## Consolidated Decisions Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Use Case Instrumentation** | AOP with ObservationRegistry | Clean separation, no domain pollution |
| **Reactive Context** | Automatic propagation via Hooks | Simplest, handles all operators |
| **Kafka Trace Propagation** | CloudEvent extensions | Standards-compliant, consistent with current architecture |
| **Attribute Naming** | OpenTelemetry semantic conventions | Tool compatibility, industry standard |
| **Sampling Strategy** | Tail-based with 10% baseline, 100% errors/slow | Balances cost with observability |
| **Data Sanitization** | Safelist + regex masking | Secure by default, allows debugging |
| **Multi-Backend Export** | Primary with circuit breaker fallback | Resilient, team flexibility |

---

## Next Phase Actions

✅ All clarifications resolved. Ready for Phase 1: Design

**Phase 1 Deliverables**:
1. **data-model.md**: Trace/Span/Context entity definitions
2. **contracts/span-attributes-schema.yaml**: Attribute naming schema
3. **quickstart.md**: Developer setup guide

**Implementation can begin after Phase 1 completes.**
