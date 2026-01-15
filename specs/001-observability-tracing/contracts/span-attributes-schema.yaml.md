# Span Attributes Schema

**Feature**: 001-observability-tracing  
**Version**: 1.0.0  
**Date**: 2025-12-15  
**Specification**: OpenTelemetry Semantic Conventions v1.24.0

## Purpose

Defines standardized span attribute naming conventions for Wallet Hub tracing instrumentation. Following OpenTelemetry semantic conventions ensures compatibility with standard observability tooling and enables consistent querying across traces.

---

## Attribute Naming Rules

1. **Lowercase with dots**: `db.system`, not `dbSystem` or `DB_SYSTEM`
2. **Namespace prefixes**: 
   - Standard operations: Use OTel namespaces (`db.*`, `messaging.*`, `http.*`)
   - Domain-specific: Use `wallet.*` prefix
3. **No high-cardinality in tags**: User IDs, transaction IDs are attributes only, not metric tags
4. **Value types**: string, number, boolean, or arrays thereof
5. **Max length**: 1024 characters (truncate with "..." if exceeded)
6. **Required attributes**: Mark with `[REQUIRED]`

---

## Database Operations (`db.*`)

**Applies to**: JPA queries, R2DBC operations, connection acquisition

| Attribute | Type | Cardinality | Description | Example |
|-----------|------|-------------|-------------|---------|
| `db.system` [REQUIRED] | string | low | Database system name | `postgresql`, `h2` |
| `db.name` | string | low | Database name | `wallet_db` |
| `db.operation` | string | low | SQL operation type | `SELECT`, `INSERT`, `UPDATE` |
| `db.statement` | string | high | Parameterized SQL statement (sanitized) | `SELECT * FROM wallet WHERE id = ?` |
| `db.connection_string` | string | medium | JDBC URL (credentials removed) | `jdbc:postgresql://localhost:5432/wallet_db` |
| `db.user` | string | medium | Database username (not password) | `wallet_app_user` |
| `db.sql.table` | string | medium | Primary table accessed | `wallet`, `transaction` |
| `db.rows_affected` | number | - | Rows returned or modified | `42` |

**Sanitization Rules**:
- `db.statement`: Replace literal values with `?` placeholders
- `db.connection_string`: Remove username/password if present
- Never include actual query parameter values

**Example Span**:
```yaml
span:
  name: "db.query.wallet"
  kind: CLIENT
  attributes:
    db.system: "postgresql"
    db.operation: "SELECT"
    db.statement: "SELECT * FROM wallet WHERE id = ?"
    db.rows_affected: 1
```

---

## Messaging Operations (`messaging.*`)

**Applies to**: Kafka produce/consume, CloudEvent publishing

| Attribute | Type | Cardinality | Description | Example |
|-----------|------|-------------|-------------|---------|
| `messaging.system` [REQUIRED] | string | low | Messaging system | `kafka` |
| `messaging.destination` [REQUIRED] | string | medium | Topic or queue name | `funds-added-topic` |
| `messaging.destination_kind` | string | low | Destination type | `topic`, `queue` |
| `messaging.operation` | string | low | Operation type | `publish`, `receive`, `process` |
| `messaging.message_id` | string | high | Event/message identifier | `abc123-def456-789` |
| `messaging.conversation_id` | string | high | Correlation ID for related messages | `saga-xyz-123` |
| `messaging.kafka.partition` | number | medium | Kafka partition number | `2` |
| `messaging.kafka.consumer_group` | string | medium | Consumer group ID | `wallet-consumer-group` |
| `messaging.kafka.offset` | number | high | Message offset | `12345` |
| `messaging.kafka.key` | string | high | Message key (sanitized) | `wallet:123` |

**CloudEvent Integration**:
- `messaging.message_id` maps to CloudEvent `id`
- CloudEvent `type` stored as `messaging.cloudevents.type`
- CloudEvent `source` stored as `messaging.cloudevents.source`

**Example Span** (Producer):
```yaml
span:
  name: "messaging.publish.funds-added-topic"
  kind: PRODUCER
  attributes:
    messaging.system: "kafka"
    messaging.destination: "funds-added-topic"
    messaging.operation: "publish"
    messaging.message_id: "evt-123"
    messaging.kafka.partition: 2
    messaging.cloudevents.type: "dev.bloco.wallet.FundsAddedEvent"
```

**Example Span** (Consumer):
```yaml
span:
  name: "messaging.process.funds-added-topic"
  kind: CONSUMER
  attributes:
    messaging.system: "kafka"
    messaging.destination: "funds-added-topic"
    messaging.operation: "process"
    messaging.message_id: "evt-123"
    messaging.kafka.consumer_group: "wallet-consumer-group"
    messaging.kafka.offset: 12345
```

---

## HTTP Operations (`http.*`)

**Applies to**: WebFlux endpoints, external API calls

| Attribute | Type | Cardinality | Description | Example |
|-----------|------|-------------|-------------|---------|
| `http.method` [REQUIRED] | string | low | HTTP method | `GET`, `POST`, `PUT` |
| `http.url` | string | high | Full URL (query params sanitized) | `https://api.wallet.com/api/wallets/transfer` |
| `http.route` | string | medium | Route pattern | `/api/wallets/{action}` |
| `http.status_code` [REQUIRED] | number | low | HTTP status code | `200`, `404`, `500` |
| `http.request_content_length` | number | - | Request body size (bytes) | `1024` |
| `http.response_content_length` | number | - | Response body size (bytes) | `256` |
| `http.user_agent` | string | high | Client user agent | `WalletApp/1.0` |
| `http.client_ip` | string | high | Client IP address (sanitized) | `192.168.1.100` |

**Sanitization Rules**:
- `http.url`: Mask query parameters like `?token=xxx` → `?token=***`
- `http.client_ip`: Optionally mask last octet for privacy
- Remove Authorization, Cookie headers from attributes

**Example Span** (Server):
```yaml
span:
  name: "POST /api/wallets/transfer"
  kind: SERVER
  attributes:
    http.method: "POST"
    http.route: "/api/wallets/{action}"
    http.status_code: 200
    http.request_content_length: 512
    http.response_content_length: 128
```

---

## Wallet Domain Operations (`wallet.*`)

**Applies to**: Use cases, business operations, domain events

| Attribute | Type | Cardinality | Description | Example |
|-----------|------|-------------|-------------|---------|
| `wallet.id` | string | high | Wallet business identifier | `wallet-abc123` |
| `wallet.operation` [REQUIRED] | string | low | Business operation type | `create`, `add_funds`, `withdraw`, `transfer` |
| `wallet.currency` | string | low | Currency code (ISO 4217) | `USD`, `EUR`, `BRL` |
| `transaction.id` | string | high | Transaction identifier | `tx-def456` |
| `transaction.type` | string | low | Transaction type | `credit`, `debit` |
| `transaction.amount` | number | - | Transaction amount (sanitized, no currency symbol) | `100.00` |
| `transaction.status` | string | low | Transaction status | `pending`, `completed`, `failed` |

**Use Case Naming Convention**:
- Span name: `usecase.{UseCaseClassName}`
- Example: `usecase.AddFundsUseCase`, `usecase.TransferFundsUseCase`

**Example Span**:
```yaml
span:
  name: "usecase.AddFundsUseCase"
  kind: INTERNAL
  attributes:
    wallet.id: "wallet-123"
    wallet.operation: "add_funds"
    transaction.id: "tx-456"
    transaction.type: "credit"
    transaction.amount: 100.00
    transaction.status: "completed"
```

---

## State Machine Operations (`statemachine.*`)

**Applies to**: Spring Statemachine transitions, saga workflows

| Attribute | Type | Cardinality | Description | Example |
|-----------|------|-------------|-------------|---------|
| `statemachine.id` | string | high | State machine instance ID | `saga-789` |
| `statemachine.type` | string | medium | State machine type | `TransferSaga`, `WithdrawalSaga` |
| `statemachine.state.from` [REQUIRED] | string | low | Source state | `PENDING`, `VALIDATING` |
| `statemachine.state.to` [REQUIRED] | string | low | Target state | `COMPLETED`, `FAILED` |
| `statemachine.event` [REQUIRED] | string | low | Triggering event | `FUNDS_ADDED`, `VALIDATION_FAILED` |
| `statemachine.action` | string | medium | Action executed during transition | `creditWallet`, `sendNotification` |
| `statemachine.guard` | string | medium | Guard condition evaluated | `hasAvailableFunds` |
| `statemachine.guard.result` | boolean | - | Guard evaluation result | `true`, `false` |
| `statemachine.compensation` | boolean | - | Whether this is a compensation flow | `true`, `false` |

**Transition Naming Convention**:
- Span name: `statemachine.transition.{StateMachineType}`
- Example: `statemachine.transition.TransferSaga`

**Example Span** (Normal Transition):
```yaml
span:
  name: "statemachine.transition.TransferSaga"
  kind: INTERNAL
  attributes:
    statemachine.id: "saga-789"
    statemachine.type: "TransferSaga"
    statemachine.state.from: "PENDING"
    statemachine.state.to: "COMPLETED"
    statemachine.event: "FUNDS_ADDED"
    statemachine.action: "creditWallet"
```

**Example Span** (Compensation):
```yaml
span:
  name: "statemachine.transition.TransferSaga"
  kind: INTERNAL
  attributes:
    statemachine.id: "saga-789"
    statemachine.type: "TransferSaga"
    statemachine.state.from: "FAILED"
    statemachine.state.to: "COMPENSATING"
    statemachine.event: "ROLLBACK_INITIATED"
    statemachine.action: "revertTransaction"
    statemachine.compensation: true
```

---

## Error Attributes (`error.*`)

**Applies to**: Exception handling, failed operations

| Attribute | Type | Cardinality | Description | Example |
|-----------|------|-------------|-------------|---------|
| `error` | boolean | - | Whether operation failed | `true` |
| `error.type` | string | medium | Exception class name | `InsufficientFundsException` |
| `error.message` | string | high | Exception message (sanitized) | `Insufficient funds for withdrawal` |
| `error.stack` | string | high | Stack trace (first 10 lines) | `at dev.bloco.wallet...` |

**Sanitization Rules**:
- `error.message`: Remove user input, PII, secrets
- `error.stack`: Truncate to first 10 lines, remove local file paths

**Example Span**:
```yaml
span:
  name: "usecase.WithdrawFundsUseCase"
  kind: INTERNAL
  status: ERROR
  attributes:
    wallet.id: "wallet-123"
    wallet.operation: "withdraw"
    error: true
    error.type: "InsufficientFundsException"
    error.message: "Insufficient funds for withdrawal"
```

---

## Reactive Operations (`reactor.*`)

**Applies to**: Reactive pipelines, scheduler transitions

| Attribute | Type | Cardinality | Description | Example |
|-----------|------|-------------|-------------|---------|
| `reactor.scheduler` | string | low | Reactor scheduler type | `parallel`, `boundedElastic`, `immediate` |
| `reactor.operator` | string | medium | Reactive operator | `flatMap`, `map`, `filter` |
| `reactor.context.keys` | array | - | Context keys present | `["traceId", "userId"]` |

**Example Span**:
```yaml
span:
  name: "reactor.pipeline.walletQuery"
  kind: INTERNAL
  attributes:
    reactor.scheduler: "boundedElastic"
    reactor.operator: "flatMap"
```

---

## Custom Events

**Common Span Events** (not attributes, point-in-time annotations):

| Event Name | Attributes | Description |
|------------|------------|-------------|
| `cache.hit` | `cache.key: string` | Cache hit occurred |
| `cache.miss` | `cache.key: string` | Cache miss occurred |
| `retry.attempt` | `attempt_number: number`, `delay_ms: number` | Retry attempt |
| `guard.evaluated` | `guard_name: string`, `result: boolean` | State machine guard evaluated |
| `circuit_breaker.opened` | `service: string` | Circuit breaker opened |
| `saga.compensating` | `reason: string` | Compensation triggered |
| `validation.failed` | `field: string`, `reason: string` | Validation failure |

---

## Span Naming Conventions

**Pattern**: `{namespace}.{operation}.{resource}`

**Examples**:
- Database: `db.query.wallet`, `db.insert.transaction`
- Messaging: `messaging.publish.funds-added-topic`, `messaging.process.wallet-created-topic`
- HTTP: `POST /api/wallets`, `GET /api/wallets/{id}`
- Use Cases: `usecase.AddFundsUseCase`, `usecase.CreateWalletUseCase`
- State Machine: `statemachine.transition.TransferSaga`

**Rules**:
- Keep names concise (< 50 characters)
- Use lowercase with dots for internal operations
- HTTP spans use `{METHOD} {route}` format
- Include resource type when relevant

---

## Implementation Example

```java
@Component
public class SpanAttributeBuilder {
    public static final AttributeKey<String> DB_SYSTEM = 
        AttributeKey.stringKey("db.system");
    public static final AttributeKey<String> WALLET_OPERATION = 
        AttributeKey.stringKey("wallet.operation");
    
    public Attributes buildDatabaseAttributes(
        String dbSystem, 
        String operation,
        String statement
    ) {
        return Attributes.builder()
            .put(DB_SYSTEM, dbSystem)
            .put("db.operation", operation)
            .put("db.statement", sanitizer.sanitizeSql(statement))
            .build();
    }
    
    public Attributes buildWalletAttributes(
        String walletId,
        String operation,
        String transactionId
    ) {
        return Attributes.builder()
            .put("wallet.id", walletId)
            .put(WALLET_OPERATION, operation)
            .put("transaction.id", transactionId)
            .build();
    }
}
```

---

## Validation Rules

**Automated Validation** (during span export):
1. Check required attributes present for span kind
2. Validate attribute key naming (lowercase, dots)
3. Enforce cardinality limits (reject high-cardinality tags)
4. Sanitize sensitive data in attribute values
5. Truncate values exceeding 1024 characters

**Span Rejection Criteria**:
- Missing required attributes
- Malformed attribute keys
- Detected PII/secrets without sanitization
- Excessive cardinality (>128 attributes)

---

## References

- [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)
- [W3C Trace Context](https://www.w3.org/TR/trace-context/)
- [CloudEvents Specification](https://cloudevents.io/)
- [Micrometer Observation](https://micrometer.io/docs/observation)

---

**Version History**:
- v1.0.0 (2025-12-15): Initial schema definition
