# Quickstart Guide: Distributed Tracing Setup

**Feature**: 001-observability-tracing  
**Audience**: Developers  
**Time**: 15 minutes  
**Date**: 2025-12-15

## Prerequisites

- Java 24+ installed
- Maven 3.9+ (or use `./mvnw`)
- Docker (for Zipkin/Jaeger backend)
- IDE with Lombok support (IntelliJ IDEA / VS Code + Java extensions)

---

## Step 1: Add Dependencies

Dependencies are already included in `pom.xml` (Spring Boot 3.5.5 includes Micrometer Tracing). Verify:

```xml
<dependencies>
    <!-- Already present in pom.xml -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-brave</artifactId>
    </dependency>
    
    <!-- Add for reactive context propagation -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>context-propagation</artifactId>
    </dependency>
    
    <!-- Add for OTLP export -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>
    
    <!-- Zipkin exporter (already in pom per README) -->
    <dependency>
        <groupId>io.zipkin.reporter2</groupId>
        <artifactId>zipkin-reporter-brave</artifactId>
    </dependency>
</dependencies>
```

**Action**: Run `./mvnw clean install` to ensure dependencies resolve.

---

## Step 2: Start Tracing Backend (Zipkin)

**Option A: Docker Compose** (recommended for local dev)

Add to existing `compose.yaml`:

```yaml
services:
  zipkin:
    image: openzipkin/zipkin:latest
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem
```

Start:
```bash
docker compose up -d zipkin
```

**Option B: Standalone Docker**:
```bash
docker run -d -p 9411:9411 --name zipkin openzipkin/zipkin:latest
```

**Verify**: Open http://localhost:9411 - you should see Zipkin UI.

---

## Step 3: Configure Tracing

Create `src/main/resources/application-tracing.yml`:

```yaml
spring:
  application:
    name: wallet-hub

management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # 100% sampling for dev (use 0.1 in production)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces  # If using Tempo/Jaeger

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

**Activate profile** in `application.yml`:
```yaml
spring:
  profiles:
    active: default,tracing
```

---

## Step 4: Enable Reactive Context Propagation

Create `src/main/java/dev/bloco/wallet/hub/config/ReactiveContextConfig.java`:

```java
package dev.bloco.wallet.hub.config;

import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration
public class ReactiveContextConfig {
    
    static {
        // Enable automatic context propagation for reactive pipelines
        Hooks.enableAutomaticContextPropagation();
    }
}
```

**Why**: Ensures trace context flows through `flatMap`, `map`, `subscribeOn`, etc.

---

## Step 5: Verify Tracing Works

### Test 1: HTTP Request Tracing

**Start application**:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=tracing
```

**Make test request**:
```bash
curl -X POST http://localhost:8080/api/wallets/create \
  -H "Content-Type: application/json" \
  -d '{"userId": "user123", "initialBalance": 100.0}'
```

**View trace**:
1. Open http://localhost:9411
2. Click "Run Query"
3. You should see a trace for `POST /api/wallets/create`
4. Click the trace to see span breakdown

### Test 2: Trace ID in Logs

**Check logs** - you should see trace IDs:
```
INFO [wallet-hub,4bf92f3577b34da6a3ce929d0e0e4736,00f067aa0ba902b7] Processing wallet creation
```
Format: `[app-name, traceId, spanId]`

### Test 3: Database Query Spans

Execute operation that queries database:
```bash
curl http://localhost:8080/api/wallets/wallet-123
```

**In Zipkin**: Trace should show spans for:
- HTTP request (SERVER span)
- Use case execution (INTERNAL span)
- Database query (CLIENT span with `db.statement` attribute)

---

## Step 6: Instrument Your Code (Optional)

Most tracing is **automatic** via Spring Boot. To add custom spans:

### Manual Span Creation

```java
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Service;

@Service
public class CustomService {
    private final ObservationRegistry registry;
    
    public CustomService(ObservationRegistry registry) {
        this.registry = registry;
    }
    
    public void businessOperation() {
        Observation observation = Observation.createNotStarted(
            "business.custom_operation", 
            registry
        )
        .lowCardinalityKeyValue("operation.type", "custom")
        .highCardinalityKeyValue("resource.id", "res-123");
        
        observation.observe(() -> {
            // Your business logic here
            performWork();
        });
    }
}
```

### Add Span Events

```java
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@Service
public class EventService {
    private final Tracer tracer;
    
    public void operationWithEvent() {
        Span span = tracer.currentSpan();
        
        if (span != null) {
            span.event("cache.miss");
            // or with attributes
            span.event("retry.attempt", 
                Map.of("attempt", 2, "delay_ms", 1000));
        }
    }
}
```

---

## Step 7: Test Kafka Trace Propagation (Advanced)

### Ensure Kafka is Running

```bash
docker compose up -d # Assumes Kafka configured in compose.yaml
```

### Publish Event

Trigger operation that publishes to Kafka (e.g., add funds):
```bash
curl -X POST http://localhost:8080/api/wallets/wallet-123/add-funds \
  -H "Content-Type: application/json" \
  -d '{"amount": 50.0}'
```

### Verify Trace Spans Across Kafka

**In Zipkin**: Trace should show:
1. HTTP request span
2. Use case span
3. Kafka producer span (kind: PRODUCER)
4. Kafka consumer span (kind: CONSUMER) - **separate but linked by trace ID**

**Check CloudEvent**: Consumer logs should show trace context in CloudEvent extensions:
```json
{
  "traceparent": "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
}
```

---

## Step 8: Configure Sampling (Production)

For production, reduce sampling to 10% baseline:

```yaml
# application-prod.yml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of traces
```

**Tail-based sampling** (always sample errors/slow ops) implemented in Phase 2.

---

## Troubleshooting

### No traces in Zipkin

**Check**:
1. Zipkin is running: `docker ps | grep zipkin`
2. Endpoint configured: `management.zipkin.tracing.endpoint` in application.yml
3. Sampling enabled: `management.tracing.sampling.probability > 0`
4. Application logs for errors: `grep -i "trace" logs/app.log`

**Test connectivity**:
```bash
curl http://localhost:9411/api/v2/services
# Should return list of services
```

### Trace IDs not in logs

**Solution**: Verify log pattern includes trace ID:
```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### Reactive context not propagating

**Check**:
1. `Hooks.enableAutomaticContextPropagation()` called on startup
2. Using Reactor 3.5+ (included in Spring Boot 3.5.5)
3. `context-propagation` dependency present

**Debug**: Enable reactor debug:
```java
Hooks.onOperatorDebug();
```

### CloudEvent trace headers missing

**Check**:
1. CloudEventTracePropagator implemented and used in producer
2. Consumer extracts from `event.getExtension("traceparent")`
3. CloudEvent version 1.0+ (supports extensions)

---

## Next Steps

**Development**:
- Review [data-model.md](data-model.md) for trace entity structure
- Read [span-attributes-schema.yaml](contracts/span-attributes-schema.yaml.md) for attribute conventions
- Implement custom instrumentation for use cases (Phase 2 tasks)

**Production**:
- Configure tail-based sampling for always-sample rules
- Set up Tempo or Jaeger for long-term trace storage
- Integrate with existing monitoring (Grafana dashboards)
- Configure sensitive data sanitization

**Testing**:
- Write integration tests with OTel SDK test exporter
- Validate trace context propagation across all boundaries
- Test sampling rules capture errors and slow transactions

---

## Useful Commands

```bash
# Start application with tracing
./mvnw spring-boot:run -Dspring-boot.run.profiles=tracing

# View Zipkin UI
open http://localhost:9411

# Check application metrics (includes trace stats)
curl http://localhost:8080/actuator/metrics/tracing

# Search traces by tag in Zipkin
# UI: Add tag filter, e.g., wallet.operation=transfer

# Export trace data (Zipkin API)
curl "http://localhost:9411/api/v2/traces?serviceName=wallet-hub&limit=10"
```

---

## Reference Documentation

- **Spring Boot Observability**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.micrometer-tracing
- **Micrometer Tracing**: https://micrometer.io/docs/tracing
- **Zipkin**: https://zipkin.io/
- **OpenTelemetry**: https://opentelemetry.io/
- **Constitution Principle VI**: [Observability as First-Class Feature](.specify/memory/constitution.md#vi-observability-as-first-class-feature)

---

**Quick Reference Card**:

| Task | Command/Config |
|------|----------------|
| Start Zipkin | `docker compose up -d zipkin` |
| View traces | http://localhost:9411 |
| Enable tracing | `management.tracing.enabled=true` |
| Sampling rate | `management.tracing.sampling.probability=0.1` |
| Manual span | `Observation.createNotStarted().observe(() -> {...})` |
| Span event | `span.event("event.name")` |
| Trace ID in logs | `%X{traceId:-}` in logging pattern |

---

✅ **Setup complete! You now have distributed tracing running.**

Traces are visible in Zipkin, trace IDs appear in logs, and context propagates through reactive pipelines and Kafka.
