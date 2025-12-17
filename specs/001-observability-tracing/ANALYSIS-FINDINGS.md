# Analysis Findings & Resolutions

**Feature**: 001-observability-tracing  
**Analysis Date**: 2025-12-15  
**Status**: Ready for Implementation with Recommended Improvements  
**Total Findings**: 10 (0 Critical, 0 High, 6 Medium, 4 Low)

## Overview

This document provides detailed explanations and resolutions for all findings from the specification consistency analysis. Each finding includes:
- Root cause analysis
- Impact assessment
- Concrete resolution steps
- File edit recommendations

---

## COVERAGE FINDINGS

### C1: Component Type Attribute Taxonomy Missing 🔶 MEDIUM

**Location**: `spec.md` FR-004 vs `contracts/span-attributes-schema.yaml`

**Issue Details**:
FR-004 states: "System MUST include span attributes for: operation name, status (ok/error), duration, **component type**, and resource identifiers"

However, the span attributes schema defines attributes for specific namespaces (db.*, messaging.*, http.*, wallet.*, statemachine.*) but doesn't define a top-level `component` or `component.type` attribute with an enumeration of valid values.

**Root Cause**:
The schema focuses on namespace-specific attributes following OpenTelemetry semantic conventions but omits a cross-cutting component classifier that would enable filtering/grouping across all span types.

**Impact**:
- **Severity**: Medium
- **Development**: Developers may implement inconsistent component tagging (e.g., "database" vs "db" vs "persistence")
- **Operations**: Querying/filtering traces by component type becomes inconsistent
- **Observability**: Dashboards grouping by component would require normalization logic

**Resolution**:

**Option A: Add top-level component attribute** (Recommended)
```yaml
## Component Classification (`component`)

**Applies to**: All spans for high-level categorization

| Attribute | Type | Cardinality | Description | Example |
|-----------|------|-------------|-------------|---------|
| `component` [REQUIRED] | string | low | High-level component type | `usecase`, `database`, `messaging`, `statemachine`, `http`, `cache` |

**Valid Values**:
- `usecase` - Business logic execution (use case/service layer)
- `database` - Data persistence operations (JPA, R2DBC)
- `messaging` - Event/message publishing and consumption (Kafka)
- `statemachine` - Saga state transitions (Spring Statemachine)
- `http` - HTTP client/server operations (WebFlux endpoints, external calls)
- `cache` - Cache operations (Redis)
- `reactive` - Reactive pipeline operations (scheduler transitions)

**Example Span**:
```yaml
span:
  name: "usecase.AddFundsUseCase"
  kind: INTERNAL
  attributes:
    component: "usecase"
    wallet.id: "wallet-123"
    wallet.operation: "add_funds"
```
```

**Option B: Derive from span name prefix**
Use span naming convention `{component}.{operation}.{resource}` and derive component from first segment. Document this in span-attributes-schema.yaml.

**Recommended Action**: **Option A**
- More explicit
- Enables consistent filtering without parsing
- Low cardinality (6-7 values)
- Aligns with FR-004 literal requirement

**File Edits Required**:
1. Add Component Classification section to `contracts/span-attributes-schema.yaml` after line 97 (after HTTP Operations section)
2. Update all instrumentation tasks (T026, T029, T063, T066, T078, T093, T110) to include component attribute
3. Update SpanAttributeBuilder (T016) to define `COMPONENT` AttributeKey constant

---

### C2: Baggage Propagation Implementation Unclear 🔶 MEDIUM

**Location**: `spec.md` FR-014 vs `tasks.md`

**Issue Details**:
FR-014 states: "System MUST include baggage propagation for high-level business context (user ID, tenant ID, operation type) across service boundaries"

However, tasks.md has no dedicated tasks for implementing baggage context. T014 implements "traceparent/tracestate extensions" which is trace context, not baggage.

**Root Cause**:
Micrometer Tracing provides automatic baggage propagation via `Observation` and baggage fields, but this requires explicit configuration of which fields to propagate. The requirement exists but implementation approach is unclear.

**Impact**:
- **Severity**: Medium
- **Development**: Risk of developers skipping baggage implementation assuming trace IDs are sufficient
- **Operations**: Business context (user ID, operation type) won't be available in logs/spans without correlation
- **Debugging**: Cross-service troubleshooting requires manual correlation instead of automatic baggage propagation

**Resolution**:

**Analysis**: Micrometer Tracing supports baggage via:
1. `BaggageField` definitions (e.g., `userId`, `operationType`)
2. Automatic propagation through `Observation.Context`
3. Extraction in consumers via `BaggagePropagationCustomizer`

**Required Implementation**:
```java
@Configuration
public class BaggageConfiguration {
    @Bean
    public List<String> baggageFields() {
        return List.of("userId", "tenantId", "operationType", "correlationId");
    }
    
    @Bean
    public BaggagePropagationCustomizer baggagePropagationCustomizer() {
        return factory -> factory
            .createBaggageField("userId")
            .createBaggageField("tenantId")
            .createBaggageField("operationType")
            .createBaggageField("correlationId");
    }
}
```

**Recommended Action**: Add explicit baggage tasks to Phase 2 (Foundational)

**New Tasks to Insert** (after T015):
```markdown
- [ ] T015a Create BaggageConfiguration in infra/adapter/tracing/config/BaggageConfiguration.java
- [ ] T015b Define baggage fields (userId, tenantId, operationType, correlationId) in BaggageConfiguration
- [ ] T015c Update CloudEventTracePropagator to propagate baggage fields in CloudEvent extensions
- [ ] T015d [P] Unit test baggage propagation (verify fields flow from producer to consumer) in infra/adapter/tracing/propagation/BaggagePropagationTest.java
```

**File Edits Required**:
1. Insert 4 new tasks after T015 in `tasks.md` Phase 2
2. Update total task count: 130 → 134 tasks
3. Update Foundational phase: 18 → 22 tasks
4. Add baggage propagation verification to T041 (end-to-end integration test)

**Alternative Resolution**:
If baggage is considered optional for single-service MVP, update FR-014:
- Change from "MUST" to "SHOULD" 
- Add note: "Future enhancement for multi-service deployment"

---

### C3: SC-009 Not Measurable Before Production 🔵 LOW

**Location**: `spec.md` SC-009

**Issue Details**:
SC-009 states: "Production incidents are resolved 40% faster (measured as time from alert to root cause identification) by using distributed traces to pinpoint failures"

This metric requires:
1. Historical baseline of incident resolution time *without* tracing
2. Production deployment with tracing enabled
3. Multiple incidents to establish statistical significance

**Root Cause**:
Success criterion is aspirational/outcome-based rather than directly testable during development or pre-production validation.

**Impact**:
- **Severity**: Low
- **Development**: Cannot be validated in unit/integration tests
- **Pre-Production**: Cannot be proven before real production incidents occur
- **Acceptance**: Risk of feature being considered "incomplete" despite full implementation

**Resolution**:

**Option A: Reframe as capability metric** (Recommended)
Replace outcome-based metric with capability-based metric:

**Original**:
> SC-009: Production incidents are resolved 40% faster (measured as time from alert to root cause identification) by using distributed traces to pinpoint failures

**Revised**:
> SC-009: Trace data for any production incident is available for analysis within 10 seconds of incident occurrence, with complete request path and timing breakdown visible in tracing UI

**Option B: Add measurable proxy metrics**
Keep aspirational goal but add testable proxy metrics:
- Time to locate relevant trace: <30 seconds
- Trace data completeness: 100% of request hops captured
- Trace query response time: <5 seconds for last 1000 traces

**Option C: Move to non-testable "Business Outcomes" section**
Accept as business goal, separate from technical success criteria

**Recommended Action**: **Option A** - Reframe as capability metric

**Justification**:
- Testable in pre-production
- Directly validates system behavior
- Achieves same business outcome (faster resolution) through proven mechanism (fast access to complete traces)

**File Edits Required**:
1. Update SC-009 in `spec.md` line 161
2. Update any references to SC-009 in `plan.md` if present
3. Update end-to-end integration tests (T041-T043) to verify trace retrieval time

---

## AMBIGUITY FINDINGS

### A1: "Critical Business Events" Undefined 🔶 MEDIUM

**Location**: `spec.md` FR-005

**Issue Details**:
FR-005 states: "System MUST support configurable sampling rates with ability to always sample specific operations (errors, slow transactions above threshold, **critical business events**)"

The term "critical business events" is not defined. Without explicit enumeration, developers will make inconsistent choices about which events qualify.

**Root Cause**:
Requirement uses domain-specific terminology without providing domain context or examples.

**Impact**:
- **Severity**: Medium
- **Development**: Different developers may implement different always-sample rules
- **Testing**: Integration tests may not cover actual critical events
- **Operations**: Important business events may be missed by sampling in production
- **Cost**: Sampling too many events as "critical" defeats purpose of sampling

**Resolution**:

**Recommended Definition** (based on wallet domain):

**Critical Business Events** = Events that:
1. Involve financial transactions above threshold (e.g., transfers ≥ $1,000)
2. Represent account lifecycle changes (create wallet, close account)
3. Trigger regulatory reporting requirements
4. Indicate potential fraud or security issues
5. Involve system-level failures (saga compensation, rollback)

**Explicit Enumeration**:
```java
public enum CriticalBusinessEvent {
    WALLET_CREATED,           // New account creation
    WALLET_CLOSED,            // Account closure
    LARGE_TRANSFER,           // Amount ≥ threshold
    FRAUD_DETECTED,           // Security trigger
    TRANSACTION_FAILED,       // Any financial operation failure
    SAGA_COMPENSATION,        // Distributed transaction rollback
    INSUFFICIENT_FUNDS,       // Withdrawal/transfer declined
    REGULATORY_REPORTING      // Compliance event
}
```

**Configuration Example**:
```yaml
# application-tracing.yml
tracing:
  sampling:
    probability: 0.1  # 10% baseline
    always-sample:
      slow-threshold-ms: 500
      events:
        - WALLET_CREATED
        - WALLET_CLOSED
        - LARGE_TRANSFER
        - FRAUD_DETECTED
        - TRANSACTION_FAILED
        - SAGA_COMPENSATION
      large-transfer-threshold-usd: 1000
```

**File Edits Required**:
1. Add "Critical Business Events" section to `research.md` Research Item 5 (Sampling Strategies)
2. Update FR-005 in `spec.md` to include examples: "critical business events (wallet creation, large transfers, fraud detection)"
3. Create CriticalBusinessEvent enum in SamplingConfiguration (T018)
4. Update TailSamplingSpanExporter (T019) to check event type against always-sample list

**Alternative Resolution**:
If "critical" definition is intentionally left flexible for future business needs, add:
- Note in FR-005: "Critical events defined by business requirements; configurable via application properties"
- Provide template configuration with placeholders

---

### A2: Multi-Backend Mode Ambiguous 🔶 MEDIUM

**Location**: `spec.md` FR-008 and Edge Cases §3

**Issue Details**:

FR-008 states: "System MUST export traces to OTLP-compatible backends (Zipkin, Jaeger, Tempo) via Micrometer Tracing bridge, **supporting multiple backends simultaneously** with **primary fallback configuration**"

Edge Case 3 states: "if primary backend fails, traces route to fallback backend"

**Ambiguity**: Does "simultaneously" mean:
- **Option A**: All backends receive every trace (broadcast)
- **Option B**: Primary receives traces; fallback only receives when primary is down (failover)
- **Option C**: Different trace samples to different backends (partitioning)

**Root Cause**:
Conflicting terminology: "simultaneously" implies parallel, "primary fallback" implies sequential.

**Impact**:
- **Severity**: Medium
- **Development**: Developers may implement wrong pattern (broadcast vs failover)
- **Cost**: Broadcasting all traces to 3 backends triples export bandwidth/storage
- **Reliability**: Failover pattern requires circuit breaker; broadcast doesn't
- **Confusion**: Task T021 mentions "ResilientCompositeSpanExporter with CircuitBreaker" (implies failover)

**Resolution**:

**Analysis of research.md Research Item 7**:
The research document describes: "Primary with circuit breaker fallback" and "If primary fails (circuit opens), route to fallback"

This clearly indicates **Option B: Failover pattern**, not broadcast.

**Clarified Behavior**:
```
Normal State:
  Primary (Tempo): ✅ Receives all traces
  Fallback (Zipkin): 💤 Idle, standby mode
  Optional (Jaeger): 💤 Idle or receives sampled subset

Circuit Open (Primary Down):
  Primary (Tempo): ❌ Circuit open, not receiving
  Fallback (Zipkin): ✅ Receives all traces
  Optional (Jaeger): 💤 Continues as configured

Circuit Half-Open (Testing Recovery):
  Primary (Tempo): 🔄 Test requests only
  Fallback (Zipkin): ✅ Continues receiving
  Optional (Jaeger): 💤 Continues as configured
```

**Recommended Action**: Update FR-008 wording for clarity

**Original**:
> FR-008: System MUST export traces to OTLP-compatible backends (Zipkin, Jaeger, Tempo) via Micrometer Tracing bridge, supporting multiple backends simultaneously with primary fallback configuration

**Revised**:
> FR-008: System MUST export traces to OTLP-compatible backends via Micrometer Tracing bridge, with primary backend (Tempo) for production traces and automatic failover to fallback backend (Zipkin) when primary becomes unavailable. Optional backends (Jaeger) MAY receive traces for team-specific analysis. Circuit breaker pattern MUST protect against primary backend failures.

**File Edits Required**:
1. Update FR-008 in `spec.md` line 126
2. Update Edge Case 3 to clarify: "When primary backend circuit opens, all traces route to fallback until primary recovers"
3. Update ResilientCompositeSpanExporter description in plan.md to emphasize failover pattern
4. Add configuration example to quickstart.md showing primary/fallback setup

---

### A3: Query Threshold vs Transaction Threshold Inconsistency 🔵 LOW

**Location**: `spec.md` FR-005 and `tasks.md` T052

**Issue Details**:

FR-005 mentions: "slow transactions above threshold" with Edge Cases clarifying ">500ms"
T052 states: "Implement threshold-based detection (>50ms) and add slow_query tag"

**Perceived Inconsistency**:
- Slow transaction threshold: >500ms
- Slow query threshold: >50ms
- 10x difference without explanation

**Root Cause**:
Different levels of granularity with valid technical rationale, but not explicitly documented.

**Impact**:
- **Severity**: Low
- **Confusion**: Developers might "fix" the inconsistency by using same threshold
- **Correctness**: Different thresholds are actually correct but need justification

**Resolution**:

**Technical Justification**:
```
Transaction Budget Analysis:
- Target transaction latency: <500ms (SC-005)
- Typical transaction composition:
  * API overhead: 10-20ms
  * Use case logic: 20-30ms  
  * Database queries (3-5): 150ms total (30-50ms each)
  * Kafka publish: 50-100ms
  * State machine: 20-30ms
  * Network: 20-50ms

Threshold Rationale:
- Single query >50ms → Consumes 10-20% of transaction budget
- 3 queries @ 50ms = 150ms → 30% of budget (concerning)
- Transaction >500ms → Violates SLA (always sample + alert)
```

**Recommended Action**: Document the relationship

**Add to research.md Research Item 5 (Sampling)**:
```markdown
### Performance Threshold Rationale

**Transaction Threshold: 500ms**
- Overall SLA target for end-to-end operations
- Triggers always-sample for detailed analysis
- Indicates potential user-impacting latency

**Query Threshold: 50ms**
- Individual component budget (10% of transaction)
- Allows 5 queries @ 50ms to stay within transaction budget
- Early warning for N+1 queries or missing indexes
- Enables proactive optimization before transactions become slow

**State Transition Threshold: 5s** (T083)
- Saga timeout detection
- Indicates stuck workflow requiring intervention
- Much longer than transaction because sagas span multiple async operations
```

**File Edits Required**:
1. Add threshold rationale section to `research.md` Research Item 5
2. Add comment to T052 in `tasks.md`: "(50ms = 10% of 500ms transaction budget; allows 5 queries to stay within SLA)"
3. Update SlowQueryDetector (T051) JavaDoc to explain threshold selection

**Alternative Resolution**:
If consistency is preferred, make thresholds configurable:
```yaml
tracing:
  thresholds:
    slow-transaction-ms: 500
    slow-query-ms: 50      # 10% of transaction budget
    slow-transition-ms: 5000
```

---

## INCONSISTENCY FINDINGS

### I1: External API Priority Mismatch 🔶 MEDIUM

**Location**: `spec.md` FR-002 vs `tasks.md` Phase 7 (US5)

**Issue Details**:

FR-002 lists "external API calls" as a critical operation requiring spans:
> FR-002: System MUST create spans for all critical operations including: use case execution, database queries, Kafka publish/consume, state machine transitions, and **external API calls**

However, User Story 5 (External API and Service Call Tracing) is marked as:
- Priority: **P3** (lowest priority)
- Phase 7 description: "**Future implementation** (no external APIs currently)"
- Tasks T091-T104 implement the feature but marked as future

**Root Cause**:
Requirement written to be comprehensive (future-proofing) while acknowledging current system has no external API integrations yet.

**Impact**:
- **Severity**: Medium
- **Confusion**: FR-002 implies external APIs exist and need immediate instrumentation
- **Acceptance**: Feature might be considered "incomplete" despite no external APIs to trace
- **Prioritization**: Unclear if US5 should be implemented in MVP or deferred

**Resolution**:

**Option A: Conditional Requirement** (Recommended)
Update FR-002 to clarify when external API tracing is required:

**Original**:
> FR-002: System MUST create spans for all critical operations including: use case execution, database queries, Kafka publish/consume, state machine transitions, and external API calls

**Revised**:
> FR-002: System MUST create spans for all critical operations including: use case execution, database queries, Kafka publish/consume, and state machine transitions. When external API integrations are added, spans MUST be created for outbound HTTP calls.

**Option B: Elevate US5 to P2**
If external APIs are planned for near-term (next 3-6 months), elevate US5 to P2 and implement as part of initial rollout.

**Option C: Defer FR-002 External API Clause**
Move external API requirement to separate FR-016 marked as future enhancement.

**Recommended Action**: **Option A** - Conditional Requirement

**Justification**:
- Maintains comprehensive requirement set
- Clarifies current scope
- Provides clear path when external APIs added
- Tasks T091-T104 remain in backlog for future use

**File Edits Required**:
1. Update FR-002 in `spec.md` line 120 with conditional clause
2. Update User Story 5 title to: "External API and Service Call Tracing (Priority: P3 - **When Implemented**)"
3. Add note to Phase 7 in `tasks.md`: "This phase implements infrastructure for future external API integrations. Can be deferred until external API dependencies are added."
4. Update constitution check in `plan.md` for Principle IX to note external APIs as future scope

---

### I2: Health Check Not in Data Model 🔵 LOW

**Location**: `data-model.md` vs `tasks.md` T120-T121

**Issue Details**:

Task T120-T121 implement TracingHealthIndicator with health check endpoint reporting:
- Backend connectivity status
- Circuit breaker states
- Buffer usage metrics

However, `data-model.md` focuses on observability artifacts (Trace, Span, TraceContext) and doesn't mention health check entity or operational monitoring.

**Root Cause**:
Data model document scoped to tracing domain entities, excluding operational/infrastructure entities.

**Impact**:
- **Severity**: Low
- **Completeness**: data-model.md appears incomplete to readers expecting health check model
- **Discoverability**: Developers implementing health check might look in data-model.md for structure guidance
- **Consistency**: Other infrastructure concerns (sampling, export) are documented but not health

**Resolution**:

**Option A: Add Operational Monitoring Section** (Recommended)
Expand data-model.md to include non-domain operational entities:

```markdown
## Operational Monitoring Entities

### 8. Tracing Health Status

**Purpose**: Represents real-time health of tracing infrastructure for monitoring and alerting.

**Attributes**:
| Attribute | Type | Description | Example |
|-----------|------|-------------|---------|
| `status` | Enum | Overall health: UP, DOWN, DEGRADED | `UP` |
| `backends` | Map<String, BackendStatus> | Per-backend connectivity | `{tempo: UP, zipkin: DOWN}` |
| `circuit_breakers` | Map<String, CircuitState> | Circuit breaker states | `{primary: CLOSED, fallback: OPEN}` |
| `buffer_usage_percent` | Double | Span buffer utilization | `23.5` |
| `spans_created_count` | Long | Total spans created since startup | `145230` |
| `spans_exported_count` | Long | Total spans exported | `14523` (10% sampling) |
| `spans_dropped_count` | Long | Spans dropped due to buffer overflow | `0` |
| `export_failures_count` | Long | Failed export attempts | `3` |
| `last_export_time` | Timestamp | Last successful export | `2025-12-15T17:30:00Z` |

**Backend Status**:
```java
enum BackendStatus {
    UP,           // Accepting traces
    DOWN,         // Unavailable
    DEGRADED,     // Slow/partial
    UNREACHABLE   // Network issue
}
```

**Circuit State**:
```java
enum CircuitState {
    CLOSED,      // Normal operation
    OPEN,        // Failures exceeded threshold
    HALF_OPEN    // Testing recovery
}
```

**Endpoint Response Example**:
```json
{
  "status": "UP",
  "components": {
    "tracing": {
      "status": "DEGRADED",
      "details": {
        "backends": {
          "tempo": "DOWN",
          "zipkin": "UP"
        },
        "circuitBreakers": {
          "primary": "OPEN",
          "fallback": "CLOSED"
        },
        "bufferUsagePercent": 23.5,
        "spansCreated": 145230,
        "spansExported": 14523
      }
    }
  }
}
```

**Lifecycle**: Computed on-demand by health check endpoint; not persisted.

---
```

**Option B: Keep Separation**
Accept that data-model.md is domain-focused. Add note:

> Note: This document covers tracing domain entities (Trace, Span, etc.). Operational monitoring entities (health checks, metrics) are infrastructure concerns defined in implementation.

**Option C: Create Separate Operations Model**
Create `specs/001-observability-tracing/operations-model.md` for health, metrics, configuration entities.

**Recommended Action**: **Option A** - Add Operational Monitoring Section

**Justification**:
- Comprehensive documentation in one place
- Helps developers implementing T120-T121
- Small addition (<50 lines)
- Maintains document cohesion

**File Edits Required**:
1. Add "Operational Monitoring Entities" section to `data-model.md` after Section 7 (Sampler Decision)
2. Update data-model.md table of contents
3. Add reference to TracingHealthIndicator in T120 task description: "Implement based on data-model.md §8"

---

## UNDERSPECIFICATION FINDINGS

### U1: Tempo Configuration Details Missing 🔶 MEDIUM

**Location**: `spec.md` FR-008 and `quickstart.md`

**Issue Details**:

FR-008 requires: "export traces to OTLP-compatible backends (Zipkin, Jaeger, **Tempo**)"

Research.md Research Item 7 identifies Tempo as the primary backend.

However, `quickstart.md` only provides Zipkin setup:
- Step 2: "Start Zipkin backend" with Docker command
- Configuration examples use `management.zipkin.tracing.endpoint`
- No Tempo endpoint format, port, or configuration

**Root Cause**:
Quickstart focused on simplest local dev setup (Zipkin) without documenting production primary backend (Tempo).

**Impact**:
- **Severity**: Medium
- **Development**: Developers can't test Tempo export locally
- **Integration**: No guidance on Tempo OTLP endpoint format (HTTP vs gRPC)
- **Production**: Gap between dev (Zipkin) and prod (Tempo) environments
- **Validation**: Can't verify primary backend works until production deployment

**Resolution**:

**Add Tempo Setup to quickstart.md**:

```markdown
## Step 2: Start Tracing Backends

### Option A: Zipkin (Simplest - Development)

**Best for**: Local development, quick setup, built-in UI

```bash
# Start Zipkin (HTTP API + UI on port 9411)
docker run -d -p 9411:9411 --name zipkin openzipkin/zipkin:latest

# Verify running
curl http://localhost:9411/health
open http://localhost:9411
```

**Spring Boot Configuration**:
```yaml
# application-tracing.yml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% in dev
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

---

### Option B: Tempo (Recommended - Production-Like)

**Best for**: Production-like testing, OTLP protocol, Grafana integration

**1. Start Tempo + Grafana Stack**:
```bash
# Create docker-compose-tempo.yml
cat > docker-compose-tempo.yml <<EOF
version: '3.8'
services:
  tempo:
    image: grafana/tempo:latest
    command: [ "-config.file=/etc/tempo.yaml" ]
    ports:
      - "4318:4318"  # OTLP HTTP
      - "4317:4317"  # OTLP gRPC
      - "3200:3200"  # Tempo HTTP
    volumes:
      - ./tempo-config.yaml:/etc/tempo.yaml
  
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
EOF

# Create minimal Tempo config
cat > tempo-config.yaml <<EOF
server:
  http_listen_port: 3200

distributor:
  receivers:
    otlp:
      protocols:
        http:
        grpc:

storage:
  trace:
    backend: local
    local:
      path: /tmp/tempo/traces

compactor:
  compaction:
    block_retention: 1h
EOF

# Start services
docker-compose -f docker-compose-tempo.yml up -d

# Verify running
curl http://localhost:3200/ready
open http://localhost:3000  # Grafana UI
```

**Spring Boot Configuration**:
```yaml
# application-tracing.yml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% in dev
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces  # OTLP HTTP
      # endpoint: http://localhost:4317  # OTLP gRPC alternative
      timeout: 10s
      compression: gzip
```

**Grafana Data Source**:
1. Open http://localhost:3000
2. Configuration → Data Sources → Add Tempo
3. URL: `http://tempo:3200`
4. Save & Test

---

### Option C: Multi-Backend (Recommended - Full Testing)

**Best for**: Testing failover, circuit breaker, production configuration

```yaml
# application-tracing.yml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces  # Tempo primary
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans  # Zipkin fallback

# Custom configuration
tracing:
  backends:
    primary: tempo
    fallback: zipkin
  resilience:
    circuit-breaker:
      failure-threshold: 5
      wait-duration-in-open-state: 60s
```

**Start Both**:
```bash
docker-compose -f docker-compose-tempo.yml up -d
docker run -d -p 9411:9411 --name zipkin openzipkin/zipkin:latest
```

**Verify Circuit Breaker**:
1. Stop Tempo: `docker-compose -f docker-compose-tempo.yml stop tempo`
2. Generate traces (run tests)
3. Verify traces route to Zipkin: http://localhost:9411
4. Check circuit breaker metrics: `curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state`
5. Restart Tempo: `docker-compose -f docker-compose-tempo.yml start tempo`
6. Verify recovery (traces route to Tempo again)

---
```

**File Edits Required**:
1. Replace "Step 2: Start Zipkin backend" in `quickstart.md` with expanded multi-option section
2. Add OTLP dependencies verification to Step 1 (already in pom.xml per T002)
3. Update T005 in `tasks.md`: "Configure Zipkin AND Tempo backends in docker-compose.yml"
4. Add Tempo verification test to T043: "Verify timing breakdown visible in Tempo/Grafana"

---

### U2: Performance Test Scope Unclear 🔵 LOW

**Location**: `tasks.md` T125

**Issue Details**:

T125 states: "Performance test: Measure overhead with/without tracing (verify <5ms per operation)"

**Ambiguity**:
- Which operations? All instrumented operations or specific high-frequency paths?
- <5ms per operation or <5ms aggregate overhead per transaction?
- How to measure "without tracing"? Disable instrumentation or just sampling?

**Root Cause**:
SC-005 defines overall goal: "System maintains performance with tracing enabled: less than 5ms overhead per operation"

But "per operation" is ambiguous in a transaction with 10+ operations (use case + 5 queries + Kafka + state machine = 7+ instrumented operations).

**Impact**:
- **Severity**: Low
- **Testing**: Developer implementing T125 may test wrong granularity
- **Acceptance**: Unclear if 5ms is per-span or per-transaction
- **Production**: Risk of unexpected cumulative overhead

**Resolution**:

**Clarify Overhead Budget**:

**Interpretation A: Per-Span Overhead** (Recommended)
Each instrumented operation adds <5ms overhead:
- Span creation/closure: ~10μs
- Attribute addition: ~1μs per attribute
- Export batching: amortized ~100μs
- **Total per span**: <1ms in practice, <5ms worst-case

**Interpretation B: Per-Transaction Overhead**
Entire transaction with all spans adds <5ms total overhead:
- Transaction with 10 spans: <0.5ms per span average
- More stringent but realistic for high-throughput systems

**Recommended Action**: **Interpretation A** with clarified task description

**Update T125**:
```markdown
- [ ] T125 [P] Performance test: Measure tracing overhead for key operations (verify <5ms per span) in infra/adapter/tracing/integration/TracingPerformanceTest.java
  - Test scenarios:
    * Use case execution (with/without tracing)
    * Database query (JPA with/without tracing)  
    * Kafka publish (with/without tracing)
    * Complete transaction (measure cumulative overhead)
  - Methodology:
    * Baseline: Run operation 1000x with tracing disabled (sampling=0)
    * Instrumented: Run operation 1000x with tracing enabled (sampling=1.0, export=mock)
    * Calculate: overhead = instrumented_p95 - baseline_p95
  - Acceptance: Each operation overhead <5ms, cumulative transaction overhead <20ms
```

**Update SC-005**:
```markdown
**Original**:
- **SC-005**: System maintains performance with tracing enabled: less than 5ms overhead per operation

**Revised**:
- **SC-005**: System maintains performance with tracing enabled: less than 5ms overhead per instrumented operation (span creation, attributes, export), with cumulative overhead less than 20ms for typical transactions (10-15 spans)
```

**File Edits Required**:
1. Expand T125 description in `tasks.md` with test scenarios and methodology
2. Update SC-005 in `spec.md` line 157 with clarified overhead budget
3. Add performance benchmark section to `quickstart.md` Step 8 with example JMH test

---

## Summary of Recommended File Edits

| File | Edits | Priority | Effort |
|------|-------|----------|--------|
| `contracts/span-attributes-schema.yaml` | Add component attribute section | Medium | 15 min |
| `tasks.md` | Insert 4 baggage tasks (T015a-T015d), update counts | Medium | 10 min |
| `spec.md` | Update FR-005, FR-008, SC-005, SC-009 (4 changes) | Medium | 20 min |
| `research.md` | Add critical events, threshold rationale sections | Medium | 15 min |
| `data-model.md` | Add operational monitoring entities section | Low | 20 min |
| `quickstart.md` | Expand Step 2 with Tempo setup (multi-option) | Medium | 30 min |
| `plan.md` | Update external API scope note | Low | 5 min |

**Total Estimated Effort**: ~2 hours for all recommendations

---

## Implementation Priority

### Must-Do Before Implementation (30-45 minutes):
1. **C1**: Add component attribute taxonomy
2. **A1**: Define critical business events
3. **A2**: Clarify multi-backend behavior (failover vs broadcast)

### Should-Do Before MVP Release (45-60 minutes):
4. **C2**: Add explicit baggage propagation tasks
5. **U1**: Add Tempo configuration to quickstart
6. **I1**: Clarify external API conditional requirement

### Nice-to-Have (30 minutes):
7. **C3**: Reframe SC-009 as capability metric
8. **A3**: Document threshold rationale
9. **I2**: Add health check to data model
10. **U2**: Clarify performance test scope

---

## Conclusion

All findings are **LOW to MEDIUM severity**. Zero CRITICAL or HIGH issues found.

**Feature status**: ✅ **READY FOR IMPLEMENTATION**

Recommended action:
1. Apply Must-Do edits (30-45 min) → Ensures consistency during Phase 1-2 implementation
2. Proceed with implementation using current tasks
3. Apply Should-Do edits during Phase 2 or before MVP release
4. Address Nice-to-Have items during polish phase (Phase 9) or as time permits

The specification is comprehensive, well-structured, and implementation-ready. These findings are quality improvements, not blockers.
