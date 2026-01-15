# Data Model: Distributed Tracing Entities

**Feature**: 001-observability-tracing  
**Phase**: 1 (Design)  
**Date**: 2025-12-15

## Overview

This document defines the conceptual data model for distributed tracing entities. These are **observability** artifacts, not business domain entities. They exist transiently in memory and are exported to tracing backends, not persisted in application database.

---

## Core Entities

### 1. Trace

**Purpose**: Represents a single end-to-end request flow through the distributed system.

**Attributes**:
| Attribute | Type | Description | Example |
|-----------|------|-------------|---------|
| `trace_id` | String (32 hex chars) | Unique identifier for entire trace | `4bf92f3577b34da6a3ce929d0e0e4736` |
| `root_span_id` | String (16 hex chars) | ID of the root span (entry point) | `00f067aa0ba902b7` |
| `sampling_decision` | Enum | SAMPLED, NOT_SAMPLED, DEFERRED | `SAMPLED` |
| `started_at` | Timestamp (epoch nanos) | When trace began | `1734278400000000000` |
| `duration_nanos` | Long | Total trace duration | `245000000` (245ms) |
| `status` | Enum | OK, ERROR, UNSET | `OK` |
| `spans` | List<Span> | All spans within this trace | [span1, span2, ...] |

**Relationships**:
- Contains 1 to N spans
- Root span represents trace entry point (e.g., HTTP request received)
- Child spans represent nested operations

**Cardinality**: One trace per user request/event. System may generate 1000+ traces/second at peak load.

---

### 2. Span

**Purpose**: Represents a single unit of work within a trace (e.g., database query, event publish, use case execution).

**Attributes**:
| Attribute | Type | Description | Example |
|-----------|------|-------------|---------|
| `span_id` | String (16 hex chars) | Unique identifier for this span | `00f067aa0ba902b7` |
| `trace_id` | String (32 hex chars) | Parent trace identifier | `4bf92f3577b34da6a3ce929d0e0e4736` |
| `parent_span_id` | String (16 hex chars) | Parent span ID (null for root) | `a3c2f98b1d4e5f60` |
| `name` | String | Operation name | `usecase.AddFundsUseCase` |
| `kind` | Enum | INTERNAL, SERVER, CLIENT, PRODUCER, CONSUMER | `INTERNAL` |
| `started_at` | Timestamp (epoch nanos) | When span started | `1734278400050000000` |
| `ended_at` | Timestamp (epoch nanos) | When span ended | `1734278400075000000` |
| `duration_nanos` | Long | Span duration (ended_at - started_at) | `25000000` (25ms) |
| `status` | Enum | OK, ERROR, UNSET | `OK` |
| `attributes` | Map<String, Object> | Key-value metadata | See Span Attributes section |
| `events` | List<SpanEvent> | Point-in-time annotations | [event1, event2, ...] |
| `links` | List<SpanLink> | Links to other spans (for sagas) | [link1, link2, ...] |

**Span Kinds**:
- **INTERNAL**: Internal operation (use case, repository call, state transition)
- **SERVER**: Inbound request handling (HTTP endpoint, Kafka consumer)
- **CLIENT**: Outbound request (external API call, HTTP client)
- **PRODUCER**: Message publishing (Kafka producer)
- **CONSUMER**: Message consumption (Kafka consumer)

**Relationships**:
- Belongs to exactly one trace
- May have zero or one parent span
- May have zero to many child spans
- May link to other spans across traces (saga correlation)

**Cardinality**: Average 10-20 spans per trace. High-complexity sagas may have 100+ spans.

---

### 3. Trace Context

**Purpose**: Propagation metadata that flows across process and network boundaries to maintain trace continuity.

**Attributes**:
| Attribute | Type | Description | Example |
|-----------|------|-------------|---------|
| `trace_id` | String (32 hex chars) | Trace identifier | `4bf92f3577b34da6a3ce929d0e0e4736` |
| `span_id` | String (16 hex chars) | Current span identifier | `00f067aa0ba902b7` |
| `trace_flags` | Byte | Sampling and feature flags | `01` (sampled) |
| `trace_state` | String | Vendor-specific state | `congo=t61rcWkgMzE` |

**Serialization Formats**:

**W3C traceparent header**:
```
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
             │  │                                │                  │
             │  trace-id (32 hex)               span-id (16 hex)   flags
             version (00)
```

**CloudEvent extensions**:
```json
{
  "specversion": "1.0",
  "type": "dev.bloco.wallet.FundsAddedEvent",
  "traceparent": "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
  "tracestate": "congo=t61rcWkgMzE",
  "data": {...}
}
```

**Reactor Context**:
```java
Mono.deferContextual(ctx -> {
    TraceContext traceContext = ctx.get(TraceContext.class);
    // Use trace context
})
```

**Propagation Points**:
- HTTP headers (inbound/outbound)
- Kafka CloudEvent extensions
- Reactor Context (reactive pipelines)
- ThreadLocal (blocking operations)

---

### 4. Span Attribute

**Purpose**: Key-value metadata attached to spans for filtering, grouping, and analysis.

**Constraints**:
- Keys must be lowercase with dots as separators (e.g., `db.statement`)
- Values must be primitives (string, number, boolean) or arrays thereof
- Total attributes per span: limit 128
- Attribute value max length: 1024 characters (truncate with "..." suffix)
- High-cardinality attributes (user IDs, transaction IDs) go in attributes, not tags

**Cardinality Classifications**:
- **Low cardinality** (suitable for tags/metrics): `status`, `operation`, `component`
- **Medium cardinality** (attributes only): `db.system`, `messaging.destination`, `http.method`
- **High cardinality** (attributes only): `wallet.id`, `transaction.id`, `db.statement`

**Standard Attributes** (OpenTelemetry semantic conventions):

**Database operations**:
```yaml
db.system: postgresql          # Database type
db.name: wallet_db             # Database name
db.operation: SELECT           # SQL operation
db.statement: "SELECT * FROM wallet WHERE id = ?" # Parameterized query
db.connection_string: "jdbc:postgresql://localhost:5432/wallet_db"
```

**Messaging operations** (Kafka):
```yaml
messaging.system: kafka
messaging.destination: funds-added-topic
messaging.destination_kind: topic
messaging.operation: publish | receive
messaging.message_id: abc123-def456
messaging.kafka.partition: 2
messaging.kafka.consumer_group: wallet-consumer-group
```

**HTTP operations**:
```yaml
http.method: POST
http.url: https://api.wallet.com/api/wallets/transfer
http.route: /api/wallets/{action}
http.status_code: 200
http.request_content_length: 1024
http.response_content_length: 256
```

**Custom Attributes** (Wallet Hub domain):
```yaml
wallet.id: wallet-123                  # Wallet identifier
wallet.operation: add_funds            # Business operation
transaction.id: tx-456                 # Transaction identifier
transaction.type: credit | debit       # Transaction type
transaction.amount: 100.00             # Amount (sanitized)
statemachine.id: saga-789              # Saga instance ID
statemachine.state.from: PENDING       # Source state
statemachine.state.to: COMPLETED       # Target state
statemachine.event: FUNDS_ADDED        # Triggering event
statemachine.action: creditWallet      # Action executed
```

---

### 5. Span Event

**Purpose**: Point-in-time annotation within a span (does not create new span).

**Attributes**:
| Attribute | Type | Description | Example |
|-----------|------|-------------|---------|
| `timestamp` | Timestamp (epoch nanos) | When event occurred | `1734278400060000000` |
| `name` | String | Event name | `cache.miss` |
| `attributes` | Map<String, Object> | Event-specific metadata | `{cache.key: "wallet:123"}` |

**Common Use Cases**:
- Cache hits/misses: `cache.hit`, `cache.miss`
- Retry attempts: `retry.attempt` (attributes: `attempt_number`, `delay_ms`)
- State machine guard evaluations: `guard.evaluated` (attributes: `guard_name`, `result`)
- Circuit breaker state changes: `circuit_breaker.opened`
- Compensation triggers: `saga.compensating` (attributes: `reason`)

**Example**:
```java
span.addEvent("retry.attempt", 
    Attributes.of(
        AttributeKey.longKey("attempt_number"), 2L,
        AttributeKey.longKey("delay_ms"), 1000L
    )
);
```

---

### 6. Span Link

**Purpose**: Links a span to other spans, potentially in different traces (for saga correlation).

**Attributes**:
| Attribute | Type | Description | Example |
|-----------|------|-------------|---------|
| `trace_id` | String (32 hex chars) | Linked trace ID | `a1b2c3d4e5f6...` |
| `span_id` | String (16 hex chars) | Linked span ID | `1234567890abcdef` |
| `trace_state` | String | Linked trace state | Optional |
| `attributes` | Map<String, Object> | Link metadata | `{link.type: "saga_correlation"}` |

**Use Cases**:
- Saga workflow correlation: Link compensation spans to original forward transaction
- Batch processing: Link child transactions to batch parent
- Async workflows: Link callback span to originating request span

**Example**:
```java
// In compensation span, link to original transaction span
span.addLink(SpanContext.create(
    originalTraceId, 
    originalSpanId,
    TraceFlags.getSampled(),
    TraceState.getDefault(),
    /* remote */ true
));
```

---

### 7. Sampler Decision

**Purpose**: Determines whether a trace should be recorded and exported.

**Attributes**:
| Attribute | Type | Description | Example |
|-----------|------|-------------|---------|
| `decision` | Enum | RECORD_AND_SAMPLE, DROP, RECORD_ONLY | `RECORD_AND_SAMPLE` |
| `attributes` | Map<String, Object> | Additional context | `{sampling.rule: "error"}` |

**Sampling Strategies** (from research):
- **Probability-based**: 10% of all traces
- **Error-based**: 100% of traces with errors
- **Duration-based**: 100% of traces >500ms
- **Custom rules**: Business-critical operations always sampled

**Implementation**:
```java
public SamplingResult shouldSample(
    Context parentContext,
    String traceId,
    String name,
    SpanKind spanKind,
    Attributes attributes,
    List<LinkData> parentLinks
) {
    // Always sample errors (detected later via tail-sampling)
    // Always sample slow operations (detected later via tail-sampling)
    // Otherwise, probability sample (10%)
    
    if (traceId.hashCode() % 10 == 0) {
        return SamplingResult.recordAndSample();
    }
    return SamplingResult.drop();
}
```

---

## Entity Relationships Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                            Trace                                 │
│  - trace_id: 4bf92f3577b34da6a3ce929d0e0e4736                   │
│  - sampling_decision: SAMPLED                                    │
│  - duration_nanos: 245000000                                     │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            │ contains
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │              Span (Root)               │
        │  - span_id: 00f067aa0ba902b7          │
        │  - name: "POST /api/wallets/transfer" │
        │  - kind: SERVER                        │
        │  - duration: 245ms                     │
        └────────────┬──────────────────────────┘
                     │
                     │ parent-child
                     │
         ┌───────────┴──────────────┬────────────────────┐
         │                          │                     │
         ▼                          ▼                     ▼
┌─────────────────┐       ┌─────────────────┐   ┌─────────────────┐
│  Span (Child 1) │       │  Span (Child 2) │   │  Span (Child 3) │
│  - Use Case     │       │  - JPA Query    │   │  - Kafka Publish│
│  - 50ms         │       │  - 20ms         │   │  - 150ms        │
└─────────────────┘       └─────────────────┘   └─────────────────┘
         │                         │                      │
         │                         │                      │
         ▼                         ▼                      ▼
   [Attributes]              [Attributes]            [Attributes]
   [Events]                  [Events]                [Events]
```

---

## Trace Context Flow Example

### Scenario: Wallet Transfer with Kafka Event

```
1. HTTP Request Arrives
   ┌─────────────────────────────────────────┐
   │ HTTP Headers:                            │
   │ traceparent: 00-{trace-id}-{span-id}-01 │
   └─────────────────────────────────────────┘
                    │
                    ▼
2. WebFlux Filter Extracts Context
   TraceContext ctx = propagator.extract(headers)
   Span rootSpan = tracer.nextSpan(ctx).kind(SERVER).start()
                    │
                    ▼
3. Use Case Execution (AOP creates child span)
   Observation obs = Observation.start("usecase.TransferFunds")
   Span useCaseSpan = childOf(rootSpan)
                    │
                    ▼
4. JPA Transaction (auto-instrumented)
   Span jpaSpan = childOf(useCaseSpan)
   // Executes: INSERT INTO outbox ...
                    │
                    ▼
5. Outbox Worker Publishes to Kafka
   CloudEvent event = eventBuilder()
       .withExtension("traceparent", formatTraceparent(ctx))
       .build()
   Span kafkaSpan = childOf(useCaseSpan).kind(PRODUCER)
                    │
                    ▼ (across network)
6. Consumer Receives Event
   TraceContext ctx = propagator.extract(event.extensions)
   Span consumerSpan = tracer.nextSpan(ctx).kind(CONSUMER).start()
                    │
                    ▼
7. State Machine Transition
   Span transitionSpan = childOf(consumerSpan)
   // Attributes: state.from=PENDING, state.to=COMPLETED
```

**Result**: Single trace with 6 spans showing complete flow from HTTP request to state machine completion.

---

## Span Lifecycle

```
1. Creation:    Span span = tracer.nextSpan().name("operation").start()
2. In Progress: span.tag("key", "value")
                span.event("checkpoint")
3. Completion:  span.end()
4. Export:      spanExporter.export([span])
                            │
                            ▼
                    ┌───────────────────┐
                    │ Tail Sampling     │
                    │ (evaluate rules)  │
                    └───────────────────┘
                            │
                ┌───────────┴──────────┐
                ▼                      ▼
         [Primary Backend]      [Fallback Backend]
         (Tempo/Jaeger)         (Zipkin)
```

---

## Data Retention

**In-Memory** (application):
- Active spans: until span.end() called
- Completed spans: buffered for 5 seconds (tail sampling evaluation)
- Context objects: garbage collected after request completes

**Backend** (Zipkin/Jaeger/Tempo):
- Configured per backend
- Typical: 7-30 days for Tempo, 24 hours for Zipkin (dev)
- Long-term: export to object storage (S3) for compliance/analysis

---

## Performance Characteristics

**Memory**:
- Span object: ~1KB (with attributes, events)
- Active spans per request: 10-20 → 10-20KB
- Buffer (5 seconds @ 1000 req/s): 50-100MB
- Total tracing overhead: <50MB (within constraint)

**CPU**:
- Span creation: ~10μs
- Attribute addition: ~1μs
- Span serialization: ~100μs
- Total overhead: <5ms per operation (within constraint)

**Network**:
- Span export batch size: 100 spans
- Export frequency: every 5 seconds
- Bandwidth: ~10KB/span * 100 spans * 12 batches/min = ~120KB/min (negligible)

---

## Next Steps

✅ Data model complete. Ready for Phase 1 contracts definition.

**Next Deliverable**: `contracts/span-attributes-schema.yaml`
