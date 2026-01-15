# Feature Specification: Comprehensive Distributed Tracing with OpenTelemetry

**Feature Branch**: `001-observability-tracing`  
**Created**: 2025-12-15  
**Status**: Draft  
**Input**: User description: "Implement OpenTelemetry traces and spans in a wide range scenario, like connections, critical operations and state machine stage change process, including event broker communications, database communications and API communications. Use micrometer, micrometer tracing, and micrometer observability components."

## Clarifications

### Session 2025-12-16

- Q: Which resource identifiers should be included as span attributes vs sanitized/masked? → A: Include technical IDs as-is (transaction ID, saga ID, event type, span kind) but mask user-related IDs (wallet ID → hash, user ID → hash)
- Q: Which specification versions should the implementation target for maximum compatibility? → A: OTLP 1.0 (stable), W3C Trace Context 1.0, CloudEvents 1.0 (current production-ready standards)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - End-to-End Transaction Trace Visibility (Priority: P1)

As an operations engineer, I need to trace a complete wallet transaction flow from API request through all system components (database, Kafka, state machine) so that I can quickly identify performance bottlenecks and failures in production.

**Why this priority**: This is the foundation for production observability. Without complete traces of critical business operations, debugging distributed system issues is nearly impossible. Financial transactions must be traceable end-to-end for compliance and troubleshooting.

**Independent Test**: Can be fully tested by executing a wallet transfer operation and verifying that a single trace ID flows through the API endpoint, use case execution, database writes, event publishing, and state machine transitions. Success means viewing the complete trace with all spans in a tracing UI (e.g., Zipkin/Jaeger).

**Acceptance Scenarios**:

1. **Given** a wallet transfer is initiated via API, **When** the operation executes successfully, **Then** a single trace captures all spans (API → UseCase → JPA → Outbox → Kafka → StateMachine → Consumer) with parent-child relationships
2. **Given** a trace is captured for a transaction, **When** viewing in tracing UI, **Then** timing information shows latency breakdown by component (API: Xms, DB: Yms, Kafka: Zms)
3. **Given** a transaction fails at any stage, **When** viewing the trace, **Then** the error span includes exception details and marks the trace as failed
4. **Given** database tracing is disabled via feature flag, **When** a transaction executes, **Then** the trace shows API, UseCase, and Kafka spans but no database spans, and the system operates without performance impact from database tracing

---

### User Story 2 - Database Operations Instrumentation (Priority: P2)

As a database administrator, I need detailed traces of all database operations (queries, connections, transactions) so that I can identify slow queries, connection pool issues, and optimize database performance.

**Why this priority**: Database performance is critical for system responsiveness. Tracing all JPA operations, connection acquisition, and query execution provides visibility into the most common performance bottleneck. This enables proactive optimization before users are impacted.

**Independent Test**: Can be fully tested by executing CRUD operations on wallet entities and verifying that each database interaction generates spans with SQL statements (sanitized), execution times, row counts, and connection pool metrics. Test with both blocking (JPA) and reactive (R2DBC) approaches.

**Acceptance Scenarios**:

1. **Given** a use case executes JPA operations, **When** querying wallet data, **Then** traces show spans for: connection acquisition, query execution, result set processing, and connection release
2. **Given** multiple queries execute in a transaction, **When** viewing the trace, **Then** all queries are grouped under a transaction span showing total duration and isolation level
3. **Given** a query exceeds performance threshold (e.g., >50ms), **When** viewing traces, **Then** the slow query span is tagged for easy filtering and includes query parameters (masked for PII)

---

### User Story 3 - Kafka Event Publishing and Consumption Tracing (Priority: P1)

As a platform engineer, I need complete tracing across Kafka event flows so that I can track message propagation delays, identify consumer lag, and debug event-driven workflows.

**Why this priority**: Event-driven architecture is core to the system. Losing tracing context across Kafka boundaries makes it impossible to correlate producer actions with consumer effects. This is critical for debugging saga workflows and eventual consistency issues.

**Independent Test**: Can be fully tested by publishing a domain event (e.g., FundsAddedEvent) and verifying that the trace context propagates from producer to consumer, with separate spans for: serialization, sending, broker acknowledgment, reception, deserialization, and processing.

**Acceptance Scenarios**:

1. **Given** a domain event is published via outbox pattern, **When** the event flows to Kafka, **Then** trace context propagates through CloudEvent headers (traceparent, tracestate) maintaining parent trace ID
2. **Given** an event is consumed by a functional consumer, **When** processing begins, **Then** a new span is created as a child of the producer span, showing consumer lag (time between send and receive)
3. **Given** a consumer processes an event that triggers another event, **When** viewing traces, **Then** the cascade of events shows clear parent-child relationships across all hops

---

### User Story 4 - State Machine Transition Tracking (Priority: P2)

As a business analyst, I need visibility into saga state machine transitions so that I can understand transaction lifecycles, identify stuck workflows, and analyze compensation patterns.

**Why this priority**: State machines orchestrate complex distributed transactions. Tracing each state transition (including guard evaluations, actions executed, and transition reasons) provides insight into saga health and helps identify when compensations are triggered.

**Independent Test**: Can be fully tested by initiating a transfer saga and verifying that each state transition generates a span with: current state, event trigger, next state, action executed, and transition duration. Failed transitions or compensations should be clearly marked.

**Acceptance Scenarios**:

1. **Given** a saga state machine processes a transaction, **When** transitioning between states, **Then** each transition generates a span tagged with: saga ID, current state, target state, triggering event, and success/failure status
2. **Given** a saga enters compensation flow, **When** rollback occurs, **Then** compensation spans are clearly marked and linked to the original forward transaction spans
3. **Given** a state machine times out or deadlocks, **When** viewing traces, **Then** the last active span shows the stuck state and time spent waiting

---

### User Story 5 - External API and Service Call Tracing (Priority: P3)

As a service owner, I need tracing for all external HTTP calls and third-party integrations so that I can monitor external dependency health and attribute failures correctly.

**Why this priority**: External dependencies (future blockchain APIs, payment gateways, identity providers) can impact system reliability. Tracing these calls separately allows SLA monitoring and helps distinguish internal vs external failures.

**Independent Test**: Can be fully tested by making an outbound HTTP call (when implemented) and verifying that the span captures: URL (sanitized), HTTP method, status code, response time, and whether circuit breaker was triggered.

**Acceptance Scenarios**:

1. **Given** the system calls an external service, **When** the call executes, **Then** a span captures the full URL (query params masked), HTTP method, headers (secrets masked), and response status
2. **Given** an external call times out or fails, **When** viewing the trace, **Then** the span is marked as error with timeout duration or exception details
3. **Given** circuit breaker opens for an external service, **When** calls are rejected, **Then** spans show circuit breaker state (open/closed/half-open) and fallback execution

---

### User Story 6 - Reactive Pipeline Tracing (Priority: P2)

As a reactive systems engineer, I need accurate tracing through reactive (Project Reactor) pipelines so that I can debug async flows without losing trace context across thread boundaries.

**Why this priority**: The system uses WebFlux and reactive data access. Standard tracing breaks across reactive operators and thread hops. Micrometer's context propagation must be configured correctly to maintain trace continuity in async pipelines.

**Independent Test**: Can be fully tested by executing a reactive operation (e.g., R2DBC query or Redis cache lookup) and verifying that trace context propagates through operators like `flatMap`, `map`, `subscribeOn`, and `publishOn`, maintaining the original trace ID.

**Acceptance Scenarios**:

1. **Given** a reactive WebFlux endpoint processes a request, **When** the flow uses multiple async operators, **Then** all operations within the reactive chain appear as child spans under the original request span
2. **Given** a reactive operation switches threads via `subscribeOn` or `publishOn`, **When** execution continues on a different scheduler, **Then** trace context is preserved without generating orphaned spans
3. **Given** multiple reactive streams run in parallel (e.g., `Flux.zip`), **When** combining results, **Then** each parallel stream shows as a separate branch in the trace tree

---

### Edge Cases

- **What happens when trace context is missing or corrupted?** System should generate a new root trace and log a warning, not fail the operation. Trace headers with invalid format are ignored.
- **How does the system handle high cardinality tags?** Only low-cardinality tags (status, operation type, not user IDs or transaction IDs) are added to spans. High-cardinality data goes into span attributes or logs, not tags.
- **What happens when tracing backend (Zipkin/Jaeger) is unavailable?** Traces are sampled and buffered; if buffer fills, oldest traces are dropped. System supports multiple backends with primary fallback - if primary backend fails, traces route to fallback backend. Application performance must not be impacted by tracing infrastructure failures.
- **How are sensitive data (PII, secrets) handled in traces?** SQL parameters, HTTP headers, and event payloads are masked/sanitized before inclusion in spans. A safelist defines which fields are safe to include.
- **What happens when a span is never closed?** Micrometer auto-closes spans on application shutdown or after a configurable timeout (e.g., 5 minutes) to prevent memory leaks.
- **How does tracing interact with sampling rates?** Sampling is configurable (default: 10% in production, 100% in dev/test). High-priority operations (e.g., errors, slow transactions) are always sampled regardless of rate.
- **How can tracing be selectively disabled for specific components in production?** Feature flags allow granular control: operations engineers can disable database tracing while keeping Kafka tracing active, or disable all tracing temporarily during high-load periods. Changes are applied via Spring Boot Actuator refresh endpoint or Spring Cloud Config without requiring service restart. This enables troubleshooting specific component issues without full system tracing overhead.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST generate a unique trace ID for every incoming API request and propagate it through all synchronous and asynchronous operations
- **FR-002**: System MUST create spans for all critical operations including: use case execution, database queries, Kafka publish/consume, state machine transitions, and external API calls
- **FR-003**: System MUST propagate trace context across Kafka boundaries using CloudEvent 1.0 extension headers (traceparent, tracestate per W3C Trace Context 1.0 specification)
- **FR-004**: System MUST include span attributes for: operation name, status (ok/error), duration, component type, and resource identifiers. Technical identifiers (transaction ID, saga ID, event type, span kind) are included as-is. User-related identifiers (wallet ID, user ID) are hashed to prevent user tracking while preserving trace correlation.
- **FR-005**: System MUST support configurable sampling rates with ability to always sample specific operations (errors, slow transactions above threshold, critical business events)
- **FR-006**: System MUST sanitize sensitive data (SQL parameters, HTTP headers, event payloads) before including in span attributes, using a configurable allow-list approach. Technical identifiers safe for inclusion: transaction ID, saga ID, event type, operation name, component name, span kind. User-related identifiers requiring masking: wallet ID (hash), user ID (hash), customer name, email, authentication tokens.
- **FR-007**: System MUST integrate with existing Micrometer metrics infrastructure, correlating traces with metrics using shared tags (operation, status, component)
- **FR-008**: System MUST export traces to OTLP 1.0-compatible backends (Zipkin, Jaeger, Tempo) via Micrometer Tracing bridge, supporting multiple backends simultaneously with primary fallback configuration (primary backend for production traces, with fallback backends for resilience and team flexibility)
- **FR-009**: System MUST maintain trace context across reactive operators (Project Reactor) using Micrometer's Context Propagation API
- **FR-010**: System MUST create error spans with exception details (type, message, stack trace) when operations fail, marking the parent trace as failed
- **FR-011**: System MUST generate spans for state machine transitions including: current state, target state, trigger event, action executed, and transition duration
- **FR-012**: System MUST measure and report key timing metrics as span attributes: JPA transaction duration, Kafka send latency, consumer lag, state transition time
- **FR-013**: System MUST support manual span creation for custom business operations not covered by auto-instrumentation
- **FR-014**: System MUST include baggage propagation for high-level business context (user ID, tenant ID, operation type) across service boundaries
- **FR-015**: System MUST provide health check endpoint indicating tracing infrastructure status (connected, sampling rate, buffer usage)
- **FR-016**: System MUST provide feature flags to enable/disable tracing for specific components (database, Kafka, state machine, external APIs, reactive operations) independently, configurable via Spring Boot properties or Spring Cloud Config, allowing runtime changes without service restart or redeployment

### Key Entities *(include if feature involves data)*

- **Trace**: Represents a single end-to-end request flow through the system with a unique trace ID, composed of multiple spans. Includes metadata like sampling decision, root operation, and total duration.

- **Span**: Represents a single unit of work within a trace (e.g., database query, Kafka publish, state transition). Contains: span ID, parent span ID, operation name, start/end timestamps, status, and attributes (tags, events, logs).

- **Trace Context**: Propagation metadata carrying trace ID, span ID, and sampling decision across process and network boundaries. Serialized as W3C traceparent header or CloudEvent extensions.

- **Span Attribute**: Key-value metadata attached to spans for filtering and analysis. Examples: db.statement, kafka.topic, statemachine.state, http.status_code. Subject to cardinality limits.

- **Span Event**: Point-in-time annotation within a span (e.g., "cache miss", "retry attempt", "guard evaluation"). Does not create a new span but adds timestamped details to existing span.

- **Sampler Decision**: Determines whether a trace should be recorded based on sampling strategy (rate-based, priority-based, adaptive). Propagated with trace context to ensure consistent sampling across services.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Operations engineers can identify the slowest component in any transaction within 30 seconds by querying traces filtered by operation type and viewing latency breakdown
- **SC-002**: 100% of wallet transactions (create, add funds, withdraw, transfer) generate complete traces with all spans present (API, use case, database, outbox, Kafka, state machine)
- **SC-003**: Trace context successfully propagates across Kafka boundaries with less than 1% trace ID mismatches (measured by comparing producer and consumer spans)
- **SC-004**: Database query spans include SQL statement (sanitized) and execution time for 95% of JPA operations, enabling identification of N+1 queries and slow queries
- **SC-005**: System maintains performance with tracing enabled: less than 5ms overhead per operation (measured via comparison of operation latency with and without tracing)
- **SC-006**: Sampling correctly captures 100% of error traces and slow transactions (>500ms) while sampling only 10% of successful fast transactions in production
- **SC-007**: State machine saga workflows show complete state transition history with less than 2% missing transitions, enabling root cause analysis of compensation triggers
- **SC-008**: Reactive operations maintain trace context across thread hops with 98% continuity (no orphaned spans in reactive pipelines)
- **SC-009**: Production incidents are resolved 40% faster (measured as time from alert to root cause identification) by using distributed traces to pinpoint failures
- **SC-010**: Zero sensitive data (PII, secrets, tokens) appears in trace span attributes, verified by automated scanning of exported traces
- **SC-011**: Operations engineers can enable/disable tracing for specific components (e.g., disable database tracing, enable only Kafka tracing) within 5 seconds via configuration refresh without service downtime, and changes take effect immediately for new operations
