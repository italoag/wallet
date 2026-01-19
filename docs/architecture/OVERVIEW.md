# Architecture Overview

## Hexagonal Architecture (Ports & Adapters)

Wallet Hub follows hexagonal architecture with clear boundaries:

```
┌─────────────────────────────────────────────────────────┐
│                    External Systems                     │
│         (Kafka, PostgreSQL, Redis, MongoDB)             │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│              Infrastructure Layer (Adapters)            │
│                                                         │
│  ┌────────────────┐  ┌────────────────┐                 │
│  │ Event Adapters │  │ Data Providers │                 │
│  │ • Producers    │  │ • JPA Repos    │                 │
│  │ • Consumers    │  │ • Mappers      │                 │
│  │ • Outbox       │  │ • State Machine│                 │
│  └────────────────┘  └────────────────┘                 │
└───────────────────────┬─────────────────────────────────┘
                        │ Implements Ports
┌───────────────────────▼─────────────────────────────────┐
│              Use Case Layer (Application)               │
│                                                         │
│  33 Use Cases:                                          │
│  • Wallet Management (8)                                │
│  • User Management (6)                                  │
│  • Transaction Management (7)                           │
│  • Address Management (5)                               │
│  • Token/Network Management (5)                         │
│  • Portfolio & Balance (2)                              │
└───────────────────────┬─────────────────────────────────┘
                        │ Orchestrates
┌───────────────────────▼─────────────────────────────────┐
│                Domain Layer (Core Business)             │
│                                                         │
│  ┌─────────┐  ┌──────────┐  ┌─────────────┐             │
│  │ Models  │  │  Events  │  │  Gateways   │             │
│  │ (10+)   │  │  (40+)   │  │ (Interfaces)│             │
│  └─────────┘  └──────────┘  └─────────────┘             │
│                                                         │
│  NO FRAMEWORK DEPENDENCIES (Pure Java)                  │
└─────────────────────────────────────────────────────────┘
```

## Dependency Rule

**Critical**: Dependencies always point INWARD:

```
Infrastructure → Use Case → Domain
```

- Domain layer has ZERO external dependencies
- Use cases depend only on domain
- Infrastructure depends on use cases and domain (implements ports)

## Layer Responsibilities

### Domain Layer (`domain/`)

**Pure business logic - no Spring, no JPA, no frameworks**

- **Models** (`model/`): Core business entities
  - Wallet, User, Transaction, Token, Address, Network, Contract, Vault, Store
  - Immutable value objects and aggregates
  - Rich domain behavior

- **Events** (`event/`): Domain events (40+)
  - Wallet events (10): WalletCreated, FundsAdded, FundsWithdrawn, etc.
  - User events (4): UserCreated, UserAuthenticated, etc.
  - Transaction events (3): TransactionCreated, TransactionConfirmed, etc.
  - Address, Token, Network, Contract, Vault, Store events

- **Gateways** (`gateway/`): Port interfaces (14)
  - Repository interfaces (never implementations)
  - DomainEventPublisher interface
  - External service interfaces

### Use Case Layer (`usecase/`)

**Application services - orchestrate domain logic**

33 Use Cases organized by feature:

**Wallet Management (8 use cases)**:
- CreateWalletUseCase
- UpdateWalletUseCase
- DeleteWalletUseCase
- GetWalletDetailsUseCase
- ListWalletsUseCase
- ActivateWalletUseCase
- DeactivateWalletUseCase
- RecoverWalletUseCase

**Funds Management (3 use cases)**:
- AddFundsUseCase
- WithdrawFundsUseCase
- TransferFundsUseCase

**Transaction Management (4 use cases)**:
- CreateTransactionUseCase
- ConfirmTransactionUseCase
- FailTransactionUseCase
- EstimateTransactionFeeUseCase

**User Management (6 use cases)**:
- CreateUserUseCase
- UpdateUserProfileUseCase
- AuthenticateUserUseCase
- ChangePasswordUseCase
- DeactivateUserUseCase

**Address Management (5 use cases)**:
- CreateAddressUseCase
- ImportAddressUseCase
- UpdateAddressStatusUseCase
- ListAddressesByWalletUseCase
- ValidateAddressUseCase

**Portfolio & Balance (4 use cases)**:
- CheckBalanceUseCase
- GetAddressBalanceUseCase
- GetTokenBalanceUseCase
- GetPortfolioSummaryUseCase

**Network & Token Management (3+ use cases)**:
- ListNetworksUseCase
- ListSupportedTokensUseCase
- AddNetworkUseCase
- AddTokenToWalletUseCase
- RemoveTokenFromWalletUseCase

### Infrastructure Layer (`infra/`)

**Adapters to external systems**

**Event Adapters** (`adapter/event/`):
- `EventProducer` interface (port)
- `KafkaEventProducer` implementation
- `OutboxEventPublisher` (transactional outbox)
- 4 Event Consumers:
  - WalletCreatedEventConsumer
  - FundsAddedEventConsumer
  - FundsWithdrawnEventConsumer
  - FundsTransferredEventConsumer

**Data Providers** (`provider/data/`):
- JPA Entities (WalletEntity, UserEntity, TransactionEntity)
- Spring Data Repositories (implements domain gateways)
- MapStruct Mappers (domain ↔ entity)
- Saga State Machine configuration
- Outbox Service and Worker

## Architectural Patterns

### 1. Event-Driven Architecture

```
Use Case → Domain Event → Outbox → Kafka → Event Consumer → Use Case
```

- All state changes produce events
- Events follow CloudEvents specification
- Asynchronous communication between bounded contexts

### 2. Outbox Pattern

```
Domain Operation + Event → TRANSACTION → Database
                                       → Outbox Table

Outbox Worker (scheduled) → Reads Outbox → Publishes to Kafka
```

**Guarantees**:
- Atomicity: Event only published if domain operation succeeds
- At-least-once delivery
- No lost events

### 3. Saga Pattern

```
State Machine:
INITIAL → WALLET_CREATED → FUNDS_ADDED → FUNDS_WITHDRAWN
       → FUNDS_TRANSFERRED → COMPLETED/FAILED
```

**Events trigger transitions**:
- WALLET_CREATED event → WALLET_CREATED state
- FUNDS_ADDED event → FUNDS_ADDED state
- SAGA_FAILED event → FAILED state

**Persistence**: State machine state persisted in JPA

### 4. CQRS (Implicit)

- Write: Use cases modify domain models
- Read: Event consumers can build read models
- Eventual consistency accepted

### 5. Repository Pattern

```java
// Domain defines the port (interface)
public interface WalletRepository {
    Optional<Wallet> findById(UUID id);
    void save(Wallet wallet);
}

// Infrastructure provides the adapter (implementation)
public class JpaWalletRepository implements WalletRepository {
    // Uses Spring Data + MapStruct
}
```

## Technology Decisions

| Concern | Technology | Rationale |
|---------|-----------|-----------|
| Messaging | Kafka + Spring Cloud Stream | Industry standard, high throughput |
| Events | CloudEvents | Standardization, interoperability |
| Persistence | JPA (primary) + R2DBC/Redis/Mongo | Flexibility, blocking + reactive |
| State Machine | Spring State Machine | Saga orchestration, JPA persistence |
| Mapping | MapStruct | Compile-time, type-safe, fast |
| Observability | Micrometer + Brave | Prometheus metrics, distributed tracing |
| Resilience | Resilience4j | Circuit breaker, retry, rate limiting |
| Security | Spring Security + Vault | OAuth2, HSM integration |

## Cross-Cutting Concerns

### Observability

- **Metrics**: Micrometer → Prometheus
- **Tracing**: Brave → Zipkin/Jaeger
- **Logging**: SLF4J with correlation IDs
- **Health Checks**: Spring Actuator

### Security

- Spring Security (OAuth2 client)
- Vault integration for secrets
- Dinamo HSM for key storage
- No sensitive data in logs/events

### Resilience

- Circuit breaker on external calls
- Retry with exponential backoff
- Timeout configuration
- Bulkhead pattern (thread pools)

### Data Consistency

- Transactional boundaries clearly defined
- Outbox pattern for event delivery
- Idempotent event consumers
- Optimistic locking (JPA versioning)

## Design Principles

1. **Domain Purity**: Domain has no framework dependencies
2. **Dependency Inversion**: Depend on abstractions (ports), not concretions
3. **Single Responsibility**: Each use case does one thing
4. **Event Sourcing**: Events as first-class citizens
5. **Immutability**: Domain events and value objects are immutable
6. **Testability**: Easy to test without full context

## Architecture Validation

### Static Analysis

```bash
# Check domain has no Spring dependencies
find src/main/java/dev/bloco/wallet/hub/domain -name "*.java" | xargs grep -l "org.springframework" && echo "VIOLATION!" || echo "PASS"
```

### Dependency Graph

See `docs/temp/dependency_graphs/bloco_wallet_java_dependency_graph.json`

### Module Tree

See `docs/module_tree.json`

## Package Structure

```
dev.bloco.wallet.hub
├── domain
│   ├── event
│   │   ├── wallet (10 events)
│   │   ├── user (4 events)
│   │   ├── transaction (3 events)
│   │   ├── address (2 events)
│   │   ├── token (2 events)
│   │   ├── network (3 events)
│   │   ├── contract (4 events)
│   │   ├── vault (3 events)
│   │   └── store (3 events)
│   ├── gateway (14 interfaces)
│   └── model
│       ├── wallet
│       ├── user
│       ├── transaction
│       ├── address
│       ├── token
│       ├── network
│       ├── contract
│       ├── vault
│       └── store
├── usecase (33 classes)
└── infra
    ├── adapter
    │   └── event
    │       ├── producer
    │       └── consumer
    └── provider
        ├── data
        │   ├── config
        │   ├── entity
        │   └── repository
        └── mapper
```

## Communication Patterns

### Synchronous
- REST API (if exposed) → Use Case → Domain
- Direct method calls within layers

### Asynchronous
- Kafka Events (CloudEvents format)
- Outbox Pattern for reliability
- Event Consumers trigger use cases

## State Management

### Application State
- JPA entities (WalletEntity, UserEntity, etc.)
- Optimistic locking with @Version
- Database transactions via @Transactional

### Saga State
- Spring State Machine with JPA persistence
- State transitions triggered by domain events
- Compensation actions for failures

### Cache State
- Redis for distributed caching
- Session management
- Rate limiting counters

## Error Handling Strategy

### Domain Layer
- Domain exceptions (InvalidWalletStateException, InsufficientFundsException)
- Validation in aggregate roots
- Fail fast principle

### Use Case Layer
- Application exceptions wrapping domain exceptions
- Transaction rollback on errors
- Compensation via saga pattern

### Infrastructure Layer
- Technical exceptions (DatabaseException, KafkaException)
- Retry with Resilience4j
- Circuit breaker for external services
- Dead letter queues for failed events

## Testing Strategy

### Domain Layer
- Pure unit tests (no mocking needed)
- Test business rules directly
- Fast, isolated tests

### Use Case Layer
- Mock domain gateways
- Verify orchestration logic
- Test transaction boundaries

### Infrastructure Layer
- Integration tests with Testcontainers
- Spring Boot test slices (@DataJpaTest, @WebMvcTest)
- End-to-end tests with test binder (no real Kafka)

## Performance Considerations

### Database
- Connection pooling (HikariCP)
- Batch inserts for events
- Indexes on foreign keys
- Query optimization with @EntityGraph

### Messaging
- Batch consumption from Kafka
- Async processing
- Partitioning for parallelism

### Caching
- Redis for frequently accessed data
- Cache-aside pattern
- TTL configuration

## Scalability

### Horizontal Scaling
- Stateless application design
- Kafka consumer groups for load balancing
- Distributed state machine via JPA

### Vertical Scaling
- Thread pool tuning
- JVM heap optimization
- Connection pool sizing

## Future Enhancements

- GraphQL API for flexible queries
- WebSocket for real-time updates
- Read models (CQRS) with MongoDB
- Event replay capabilities
- Polyglot persistence expansion
