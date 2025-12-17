# Distributed Tracing Guide

Complete guide to distributed tracing in Wallet Hub using OpenTelemetry and Micrometer.

## Table of Contents
- [Overview](#overview)
- [Span Attributes Schema](#span-attributes-schema)
- [Feature Flags](#feature-flags)
- [Troubleshooting](#troubleshooting)
- [Health Checks and Metrics](#health-checks-and-metrics)

---

## Overview

Wallet Hub implements distributed tracing using Micrometer Tracing (backed by Brave) with support for multiple exporters (OTLP, Zipkin). All traces follow OpenTelemetry semantic conventions with additional domain-specific attributes.

### Architecture Components

- **Tracer**: Brave Tracer for span creation and propagation
- **Context Propagation**: W3C Trace Context for HTTP and CloudEvents for Kafka
- **Exporters**: ResilientCompositeSpanExporter with primary/fallback backends
- **Sampling**: TailSamplingSpanExporter for intelligent span filtering
- **Sanitization**: SensitiveDataSanitizer for PII removal

---

## Span Attributes Schema

Complete reference for all span attributes emitted by Wallet Hub. See [contracts/span-attributes-schema.yaml](../specs/001-observability-tracing/contracts/span-attributes-schema.yaml) for the full schema.

### Database Operations

| Attribute | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `db.system` | string | Yes | Database type | `postgresql`, `h2`, `redis`, `mongodb` |
| `db.operation` | string | Yes | Operation name | `INSERT`, `SELECT`, `UPDATE`, `DELETE` |
| `db.statement` | string | No | Sanitized SQL/query | `INSERT INTO wallet (id, balance) VALUES (?, ?)` |
| `db.table` | string | Yes | Table/collection name | `wallet`, `transaction` |
| `db.method` | string | Yes | Repository method | `save`, `findById`, `deleteById` |
| `db.result.count` | integer | No | Rows affected | `1`, `0`, `42` |
| `db.transaction.active` | boolean | Yes | Transaction context | `true`, `false` |

**Example JPA Span:**
```json
{
  "name": "wallet.save",
  "attributes": {
    "db.system": "postgresql",
    "db.operation": "INSERT",
    "db.table": "wallet",
    "db.method": "save",
    "db.statement": "INSERT INTO wallet (id, balance, status) VALUES (?, ?, ?)",
    "db.result.count": 1,
    "db.transaction.active": true
  }
}
```

### Kafka Events

| Attribute | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `messaging.system` | string | Yes | Messaging system | `kafka` |
| `messaging.operation` | string | Yes | Operation type | `publish`, `receive` |
| `messaging.destination` | string | Yes | Topic name | `wallet-created-topic` |
| `messaging.kafka.partition` | integer | No | Partition number | `0`, `1`, `2` |
| `messaging.kafka.offset` | integer | No | Message offset | `12345` |
| `messaging.message.id` | string | No | CloudEvents ID | `uuid-123` |
| `cloudevents.type` | string | Yes | Event type | `WalletCreatedEvent` |
| `cloudevents.source` | string | Yes | Event source | `/wallet-hub/wallet` |

**Example Producer Span:**
```json
{
  "name": "wallet-created-topic publish",
  "attributes": {
    "messaging.system": "kafka",
    "messaging.operation": "publish",
    "messaging.destination": "wallet-created-topic",
    "messaging.kafka.partition": 0,
    "cloudevents.type": "WalletCreatedEvent",
    "cloudevents.source": "/wallet-hub/wallet",
    "event.payload.size": 256
  }
}
```

### State Machine Transitions

| Attribute | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `statemachine.id` | string | Yes | State machine ID | `saga-uuid-123` |
| `statemachine.from.state` | string | Yes | Source state | `PENDING` |
| `statemachine.to.state` | string | Yes | Target state | `COMPLETED` |
| `statemachine.event` | string | Yes | Trigger event | `FUNDS_ADDED` |
| `statemachine.transition.type` | string | Yes | Transition type | `EXTERNAL`, `INTERNAL` |
| `statemachine.guard.result` | boolean | No | Guard evaluation | `true`, `false` |

### Reactive Pipeline Operations

| Attribute | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `reactor.type` | string | Yes | Publisher type | `Mono`, `Flux` |
| `reactor.operator` | string | Yes | Operator name | `map`, `flatMap`, `filter` |
| `reactor.scheduler` | string | No | Scheduler name | `boundedElastic`, `parallel` |
| `reactor.context.propagated` | boolean | Yes | Context present | `true`, `false` |

### Cache Operations (Redis)

| Attribute | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `cache.system` | string | Yes | Cache system | `redis` |
| `cache.operation` | string | Yes | Operation type | `cache.get`, `cache.set`, `cache.delete` |
| `cache.key` | string | Yes | Cache key | `user:123` |
| `cache.hit` | boolean | No | Cache hit/miss | `true`, `false` |
| `cache.ttl` | integer | No | TTL in seconds | `3600` |
| `cache.value.size` | integer | No | Value size bytes | `256` |

---

## Feature Flags

Control which components are instrumented for tracing at runtime without service restart.

### Configuration

**application-tracing.yml:**
```yaml
tracing:
  features:
    database: true        # JPA, R2DBC operations
    kafka: true           # Kafka producer/consumer
    state-machine: true   # State machine transitions
    external-api: true    # External HTTP calls
    reactive: true        # Reactor pipelines
    use-case: true        # Use case execution
```

### Feature Flag Descriptions

| Flag | Scope | Impact | Use Case |
|------|-------|--------|----------|
| `database` | JPA, R2DBC, Redis, MongoDB operations | High volume | Disable to reduce overhead in read-heavy workloads |
| `kafka` | Kafka producers and consumers | Medium volume | Disable to isolate event flow issues |
| `state-machine` | State machine transitions and guards | Low volume | Enable for saga debugging |
| `external-api` | HTTP clients (WebClient, RestClient) | Low-medium | Enable when debugging integrations |
| `reactive` | Reactor operators (map, flatMap, etc.) | High volume | Disable to reduce reactive overhead |
| `use-case` | Use case entry/exit points | Low volume | Enable for business logic tracing |

### Runtime Updates

Feature flags support hot reload via Spring Boot Actuator:

```bash
# Update configuration in application-tracing.yml or Config Server
# Then trigger refresh:
curl -X POST http://localhost:8080/actuator/refresh \
  -H "Content-Type: application/json"
```

**Refresh behavior:**
- New operations use updated flags immediately (typically <5 seconds)
- In-flight operations complete with original configuration
- No service restart required

### Performance Recommendations

**High-throughput scenarios:**
```yaml
tracing:
  features:
    database: false      # Disable for bulk operations
    reactive: false      # Disable for streaming endpoints
    kafka: true          # Keep for event correlation
    use-case: true       # Keep for business logic
```

**Debugging specific issues:**
```yaml
tracing:
  features:
    database: true       # Enable to debug persistence
    state-machine: true  # Enable to debug saga flows
    kafka: true          # Enable for event tracing
```

---

## Troubleshooting

### Common Issues

#### 1. Missing Spans

**Symptom:** Expected spans not appearing in tracing backend.

**Diagnosis:**
```bash
# Check health endpoint
curl http://localhost:8080/actuator/health/tracing

# Expected response:
{
  "status": "UP",
  "details": {
    "tracer.available": true,
    "tracer.type": "BraveTracer",
    "features.database": true,
    "span.creation.test": "success"
  }
}
```

**Possible causes:**
1. **Feature flag disabled** - Check `tracing.features.*` configuration
2. **Sampling decision** - Tail sampling may have filtered the span
3. **Export failure** - Check logs for "export failed" messages

**Resolution:**
```yaml
# Enable all features temporarily
tracing:
  features:
    database: true
    kafka: true
    state-machine: true
    external-api: true
    reactive: true
    use-case: true

# Increase sampling for debugging
management:
  tracing:
    sampling:
      probability: 1.0  # Sample 100% of traces
```

#### 2. Broken Trace Context

**Symptom:** Parent-child relationships missing, disconnected spans.

**Diagnosis:**
```bash
# Check for context propagation errors in logs
grep "trace context" logs/application.log

# Look for:
# - "Failed to propagate trace context"
# - "No trace context in Reactor Context"
# - "CloudEvents header missing"
```

**Possible causes:**
1. **Reactive pipeline** - Context not propagated via `.contextWrite()`
2. **Kafka events** - CloudEvents headers not set
3. **Thread boundaries** - Context lost during async operations

**Resolution:**
```java
// Reactive pipelines - always use contextWrite
return Mono.just("data")
    .map(String::toUpperCase)
    .contextWrite(reactiveContextPropagator.captureTraceContext());

// Kafka producers - use CloudEventUtils
CloudEvent<WalletCreatedEvent> event = CloudEventUtils.createCloudEvent(
    "WalletCreatedEvent",
    "/wallet-hub/wallet",
    walletCreatedEvent
);
```

#### 3. High Performance Overhead

**Symptom:** Increased latency (>5ms per operation) with tracing enabled.

**Diagnosis:**
```bash
# Run performance tests
./mvnw test -Dtest=TracingPerformanceTest

# Check metrics
curl http://localhost:8080/actuator/metrics/tracing.spans.created
```

**Resolution:**
```yaml
# Disable high-volume features
tracing:
  features:
    database: false    # Disable if many DB calls
    reactive: false    # Disable for streaming
    
# Reduce sampling
management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of traces
```

#### 4. Sensitive Data in Traces

**Symptom:** PII or credentials visible in exported spans.

**Diagnosis:**
```bash
# Run security audit tests
./mvnw test -Dtest=SensitiveDataAuditTest
```

**Resolution:**
- SensitiveDataSanitizer automatically masks:
  - Email addresses → `***@***.***`
  - Credit cards → `****-****-****-****`
  - SQL literals → `?` placeholders
  - URL parameters (token, password, api_key) → `***`
  - Exception messages with secrets → `password=***`

**Verify sanitization:**
```java
// Example sanitized span attributes:
db.statement: "SELECT * FROM users WHERE email = ?" // not "user@example.com"
http.url: "https://api.example.com/login?token=***" // not actual token
error.message: "Auth failed for ***@***.***" // not real email
```

#### 5. Export Backend Failures

**Symptom:** Logs show "export failed" or "fallback activated".

**Diagnosis:**
```bash
# Check exporter metrics
curl http://localhost:8080/actuator/metrics/tracing.spans.exported
curl http://localhost:8080/actuator/metrics/tracing.spans.dropped

# Check circuit breaker state
curl http://localhost:8080/actuator/health/tracing
```

**Possible causes:**
1. **Primary backend down** - Circuit breaker opens, fallback used
2. **Network timeout** - Check backend connectivity
3. **Authentication failure** - Verify OTLP auth headers

**Resolution:**
```yaml
# Configure resilient exporter
tracing:
  exporter:
    primary: otlp
    fallback: logging
    circuit-breaker:
      failure-rate-threshold: 50
      slow-call-rate-threshold: 50
      wait-duration-in-open-state: 10s
```

### Verification Steps

**1. Verify tracing infrastructure:**
```bash
# Health check
curl http://localhost:8080/actuator/health/tracing

# Metrics
curl http://localhost:8080/actuator/metrics | grep tracing
```

**2. Verify span creation:**
```bash
# Check span creation rate
curl http://localhost:8080/actuator/metrics/tracing.spans.created

# Check feature flags
curl http://localhost:8080/actuator/metrics/tracing.feature.flags.state
```

**3. Verify trace propagation:**
```bash
# Make a request with traceparent header
curl -H "traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01" \
  http://localhost:8080/api/wallet

# Check logs for trace-id in MDC
grep "4bf92f3577b34da6a3ce929d0e0e4736" logs/application.log
```

**4. Verify span export:**
```bash
# Check export metrics
curl http://localhost:8080/actuator/metrics/tracing.spans.exported

# Check for export errors in logs
grep -i "export.*fail" logs/application.log
```

### Performance Tuning

**Baseline measurements (from T143-T144):**
- Span creation overhead: **<5ms** per operation
- Feature flag check: **<1μs** per check
- Reactive context propagation: **<2ms** per operator

**Tuning recommendations:**

1. **Disable high-volume operations:**
   ```yaml
   tracing:
     features:
       database: false  # If >1000 queries/sec
       reactive: false  # If heavy streaming
   ```

2. **Adjust sampling:**
   ```yaml
   management:
     tracing:
       sampling:
         probability: 0.1  # 10% for production
   ```

3. **Use tail sampling:**
   ```yaml
   tracing:
     sampling:
       tail:
         enabled: true
         error-threshold: 1  # Always keep errors
   ```

### Debug Logging

Enable debug logging for detailed tracing information:

```yaml
logging:
  level:
    dev.bloco.wallet.hub.infra.adapter.tracing: DEBUG
    io.micrometer.tracing: DEBUG
    brave: DEBUG
```

**Key log messages:**
- `Exporting span [id=xxx, name=xxx, duration=xxx]` - Span export
- `Circuit breaker state: OPEN` - Primary backend unavailable
- `Fallback backend activated` - Using fallback exporter
- `Span exported successfully to primary backend` - Normal operation

---

## Health Checks and Metrics

### Health Endpoint

**Endpoint:** `GET /actuator/health/tracing`

**Response:**
```json
{
  "status": "UP",
  "details": {
    "tracer.available": true,
    "tracer.type": "BraveTracer",
    "features.database": true,
    "features.kafka": true,
    "features.stateMachine": true,
    "features.externalApi": true,
    "features.reactive": true,
    "features.useCase": true,
    "span.creation.test": "success"
  }
}
```

**Status codes:**
- `UP` - Tracing fully functional
- `DOWN` - Tracer unavailable or span creation failed
- `UNKNOWN` - Unable to determine state

### Metrics

All tracing metrics are exposed via Actuator Prometheus endpoint.

**Endpoint:** `GET /actuator/prometheus`

#### Span Metrics

```prometheus
# Total spans created
tracing_spans_created_total

# Total spans exported successfully
tracing_spans_exported_total

# Total spans dropped (sampling or export failure)
tracing_spans_dropped_total
```

#### Feature Flag Metrics

```prometheus
# Current state of each feature flag (1=enabled, 0=disabled)
tracing_feature_flags_state{feature="database"} 1.0
tracing_feature_flags_state{feature="kafka"} 1.0
tracing_feature_flags_state{feature="reactive"} 1.0

# Total feature flag changes (hot reloads)
tracing_feature_flags_changes_total
```

#### Example Prometheus Queries

```prometheus
# Span creation rate
rate(tracing_spans_created_total[5m])

# Span export success rate
rate(tracing_spans_exported_total[5m]) / rate(tracing_spans_created_total[5m])

# Dropped span percentage
(rate(tracing_spans_dropped_total[5m]) / rate(tracing_spans_created_total[5m])) * 100

# Feature flag changes in last hour
increase(tracing_feature_flags_changes_total[1h])
```

#### Grafana Dashboard Example

```json
{
  "panels": [
    {
      "title": "Span Creation Rate",
      "targets": [
        {"expr": "rate(tracing_spans_created_total[5m])"}
      ]
    },
    {
      "title": "Export Success Rate",
      "targets": [
        {"expr": "rate(tracing_spans_exported_total[5m]) / rate(tracing_spans_created_total[5m])"}
      ]
    },
    {
      "title": "Feature Flags State",
      "targets": [
        {"expr": "tracing_feature_flags_state"}
      ]
    }
  ]
}
```

---

## Additional Resources

- [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)
- [Micrometer Tracing Documentation](https://micrometer.io/docs/tracing)
- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Brave Tracer Documentation](https://github.com/openzipkin/brave)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)

---

## Support

For issues or questions:
1. Check [Troubleshooting](#troubleshooting) section
2. Review logs with `logging.level.dev.bloco.wallet.hub.infra.adapter.tracing=DEBUG`
3. Run diagnostic tests: `./mvnw test -Dtest=TracingPerformanceTest,SensitiveDataAuditTest`
4. Check health endpoint: `curl http://localhost:8080/actuator/health/tracing`
