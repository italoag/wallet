# Infrastructure Layer Documentation

**Module:** `dev.bloco.wallet.hub.infra`
**Version:** 1.0
**Last Updated:** 2025-12-10

## Table of Contents

1. [Overview](#overview)
2. [Architecture Principles](#architecture-principles)
3. [Event Adapters](#event-adapters)
4. [Data Providers](#data-providers)
5. [State Machine Configuration](#state-machine-configuration)
6. [Configuration](#configuration)
7. [Best Practices](#best-practices)
8. [Testing Guidelines](#testing-guidelines)

---

## Overview

The Infrastructure Layer implements the **Ports & Adapters (Hexagonal Architecture)** pattern by providing concrete implementations of domain gateway interfaces and managing external system integrations. This layer contains all framework-specific code (Spring, JPA, Kafka) and keeps the domain layer pure.

### Package Structure

```
src/main/java/dev/bloco/wallet/hub/infra/
├── adapter/
│   └── event/
│       ├── consumer/           # Kafka event consumers
│       ├── producer/           # Event producer implementations
│       └── OutboxEventPublisher.java
├── provider/
│   ├── data/
│   │   ├── config/            # State machine configuration
│   │   ├── entity/            # JPA entities
│   │   ├── repository/        # Spring Data repositories
│   │   ├── OutboxEvent.java
│   │   ├── OutboxService.java
│   │   └── OutboxWorker.java
│   └── mapper/                # MapStruct mappers
└── util/
    └── CloudEventUtils.java
```

### Key Responsibilities

- **Event Adapters**: Produce and consume domain events via Kafka
- **Data Providers**: Persist and retrieve domain objects using JPA
- **State Management**: Orchestrate distributed transactions using Spring State Machine
- **Outbox Pattern**: Ensure reliable event delivery with transactional guarantees
- **Mapping**: Convert between domain models and persistence entities

---

## Architecture Principles

### Dependency Direction

```
Domain Layer (Pure Java)
    ↑
Use Case Layer (Application Logic)
    ↑
Infrastructure Layer (Framework Integration)
```

**Key Rules:**

1. Infrastructure depends on Domain, never the reverse
2. Domain layer has zero Spring/JPA dependencies
3. Use gateway interfaces (Ports) to decouple layers
4. MapStruct handles domain ↔ entity conversion

### Hexagonal Architecture Implementation

```
┌─────────────────────────────────────────────────────────┐
│                    Domain Layer                         │
│  ┌──────────────┐        ┌──────────────┐               │
│  │ Domain Model │        │   Gateways   │ (Ports)       │
│  │  - Wallet    │        │  - Repos     │               │
│  │  - User      │        │  - Producers │               │
│  └──────────────┘        └──────────────┘               │
└─────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────┐
│              Infrastructure Layer                       │
│  ┌──────────────┐        ┌──────────────┐               │
│  │   Adapters   │        │  Providers   │               │
│  │  - Kafka     │        │  - JPA       │               │
│  │  - Events    │        │  - Mappers   │               │
│  └──────────────┘        └──────────────┘               │
└─────────────────────────────────────────────────────────┘
```

---

## Event Adapters

### Overview

Event adapters implement event-driven communication using Spring Cloud Stream with Kafka, following the **CloudEvents** specification for standardized event formats.

### Architecture

```
┌──────────────┐    ┌─────────────┐    ┌──────────────┐
│  Use Cases   │───>│  Producer   │───>│   Outbox     │
└──────────────┘    └─────────────┘    └──────────────┘
                                              │
                                              v
                                        ┌──────────────┐
                                        │OutboxWorker  │
                                        │  (Scheduled) │
                                        └──────────────┘
                                              │
                                              v
                                        ┌──────────────┐
                                        │    Kafka     │
                                        └──────────────┘
                                              │
                                              v
                                        ┌──────────────┐     ┌───────────────┐
                                        │  Consumers   │───> │ State Machine │
                                        └──────────────┘     └───────────────┘
```

---

### Event Producer Interface

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/adapter/event/producer/EventProducer.java`

```java
public interface EventProducer {
    void produceWalletCreatedEvent(WalletCreatedEvent event);
    void produceFundsAddedEvent(FundsAddedEvent event);
    void produceFundsWithdrawnEvent(FundsWithdrawnEvent event);
    void produceFundsTransferredEvent(FundsTransferredEvent event);
}
```

**Purpose:** Defines the contract for publishing domain events to external systems.

**Design Pattern:** Port Interface (Hexagonal Architecture)

---

### Kafka Event Producer

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/adapter/event/producer/KafkaEventProducer.java`

**Implementation:** Concrete adapter that publishes events via Spring Cloud Stream.

```java
@Component
@Slf4j
public class KafkaEventProducer implements EventProducer {
    private final OutboxService outboxService;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    @Override
    public void produceWalletCreatedEvent(WalletCreatedEvent event) {
        saveEventToOutbox("walletCreatedEventProducer", event);
    }

    private void saveEventToOutbox(String eventType, Object event) {
        try {
            var payload = objectMapper.writeValueAsString(event);
            String correlationId = extractCorrelationId(event);
            outboxService.saveOutboxEvent(eventType, payload, correlationId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event", e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
```

**Key Features:**

- **Two-Phase Publishing**: Events saved to outbox first, then sent by worker
- **JSON Serialization**: Uses Jackson with Java Time module support
- **Correlation ID Tracking**: Extracts and stores correlation IDs for distributed tracing
- **Error Handling**: Logs and throws on serialization failures

**Flow:**

1. Use case calls `produceXxxEvent()`
2. Event serialized to JSON
3. Saved to `OutboxEvent` table (transactional)
4. `OutboxWorker` picks up unsent events (scheduled)
5. Events sent to Kafka via `StreamBridge`
6. Marked as sent on success

---

### Event Bindings Configuration

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/adapter/event/producer/EventBindings.java`

```java
public final class EventBindings {
    public static final String WALLET_CREATED_BINDING = "walletCreatedEventProducer-out-0";
    public static final String FUNDS_ADDED_BINDING = "fundsAddedEventProducer-out-0";
    public static final String FUNDS_WITHDRAWN_BINDING = "fundsWithdrawnEventProducer-out-0";
    public static final String FUNDS_TRANSFERRED_BINDING = "fundsTransferredEventProducer-out-0";

    private static final Map<String, String> EVENT_TYPE_TO_BINDING = Map.of(
        "walletCreatedEventProducer", WALLET_CREATED_BINDING,
        "fundsAddedEventProducer", FUNDS_ADDED_BINDING,
        "fundsWithdrawnEventProducer", FUNDS_WITHDRAWN_BINDING,
        "fundsTransferredEventProducer", FUNDS_TRANSFERRED_BINDING
    );

    public static Optional<String> bindingForEventType(String eventType) {
        return Optional.ofNullable(EVENT_TYPE_TO_BINDING.get(eventType));
    }
}
```

**Purpose:** Centralized mapping between event types and Spring Cloud Stream bindings.

**Benefits:**

- Avoids string coupling throughout codebase
- Single source of truth for binding names
- Easy to evolve channel names

---

### Outbox Pattern Implementation

#### OutboxEvent Entity

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/OutboxEvent.java`

```java
@Entity
@Table(name = "outbox", indexes = {@Index(name = "idx_outbox_created_at", columnList = "created_at")})
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "correlation_id")
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent", nullable = false)
    private boolean sent = false;
}
```

**Schema:**

| Column         | Type      | Constraints      | Description                      |
|----------------|-----------|------------------|----------------------------------|
| id             | BIGINT    | PRIMARY KEY      | Auto-generated identifier        |
| event_type     | VARCHAR   | NOT NULL         | Event type identifier            |
| payload        | TEXT      | NOT NULL         | JSON serialized event            |
| correlation_id | VARCHAR   | NULLABLE         | Correlation ID for tracing       |
| created_at     | TIMESTAMP | NOT NULL         | Timestamp when event was created |
| sent           | BOOLEAN   | NOT NULL, DEFAULT false | Delivery status         |

**Index:** `idx_outbox_created_at` on `created_at` for efficient polling.

---

#### OutboxService

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/OutboxService.java`

```java
@Service
public class OutboxService {
    private final OutboxRepository outboxRepository;

    @Transactional
    public void saveOutboxEvent(String eventType, String payload, String correlationId) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setCorrelationId(correlationId);
        outboxRepository.save(event);
    }

    @Transactional
    public void markEventAsSent(OutboxEvent event) {
        event.setSent(true);
        outboxRepository.save(event);
    }

    public List<OutboxEvent> getUnsentEvents() {
        return outboxRepository.findBySentFalse();
    }
}
```

**Responsibilities:**

- Save events to outbox table (transactional)
- Mark events as sent after delivery
- Retrieve unsent events for worker

**Transactional Guarantees:**

- Domain changes and outbox writes in same transaction
- Events never lost due to failures
- At-least-once delivery semantics

---

#### OutboxWorker

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/OutboxWorker.java`

```java
@Component
public class OutboxWorker {
    private final OutboxService outboxService;
    private final StreamBridge streamBridge;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 5000)
    public void processOutbox() {
        for (OutboxEvent event : outboxService.getUnsentEvents()) {
            String eventType = event.getEventType();
            Optional<String> bindingOpt = EventBindings.bindingForEventType(eventType);

            if (bindingOpt.isEmpty()) {
                log.warn("Unknown event type '{}', skipping outbox id={}",
                         eventType, event.getId());
                if (meterRegistry != null)
                    meterRegistry.counter("outbox.unknown.type", "eventType", eventType).increment();
                continue;
            }

            String binding = bindingOpt.get();
            boolean success = streamBridge.send(binding, event.getPayload());

            if (success) {
                outboxService.markEventAsSent(event);
                if (meterRegistry != null)
                    meterRegistry.counter("outbox.sent", "binding", binding).increment();
            } else {
                log.warn("Failed to send outbox id={} to binding {}",
                         event.getId(), binding);
                if (meterRegistry != null)
                    meterRegistry.counter("outbox.send.failed", "binding", binding).increment();
            }
        }
    }
}
```

**Configuration:**

- **Schedule:** Runs every 5 seconds (`fixedRate = 5000`)
- **Processing:** Retrieves unsent events, sends via StreamBridge, marks as sent
- **Monitoring:** Publishes Micrometer metrics for observability

**Metrics:**

| Metric                 | Type    | Description                           |
|------------------------|---------|---------------------------------------|
| `outbox.sent`          | Counter | Successfully sent events by binding   |
| `outbox.send.failed`   | Counter | Failed send attempts by binding       |
| `outbox.unknown.type`  | Counter | Unknown event types encountered       |

---

#### OutboxEventPublisher

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/adapter/event/OutboxEventPublisher.java`

```java
@Component
@Slf4j
public class OutboxEventPublisher implements DomainEventPublisher {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publish(Object event) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setEventType(event.getClass().getSimpleName());
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event", e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
```

**Purpose:** Alternative publisher that uses simple class name as event type.

**Use Case:** Generic event publishing when event type is derived from class name.

---

### Event Consumers

Event consumers receive messages from Kafka topics and interact with the state machine to coordinate saga workflows.

#### Architecture Pattern

All consumers follow the same pattern:

1. Receive message via Spring Cloud Stream function binding
2. Extract payload from message
3. Validate correlation ID
4. Send appropriate saga event to state machine
5. Log operation result

---

#### WalletCreatedEventConsumer

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/adapter/event/consumer/WalletCreatedEventConsumer.java`

```java
@Configuration
@Slf4j
public class WalletCreatedEventConsumer {
    private final StateMachine<SagaStates, SagaEvents> stateMachine;

    @Bean
    public Consumer<Message<WalletCreatedEvent>> walletCreatedEventConsumerFunction() {
        return message -> {
            var event = message.getPayload();
            if (event.getCorrelationId() != null) {
                var stateMachineMessage = MessageBuilder
                    .withPayload(SagaEvents.WALLET_CREATED)
                    .setHeader("correlationId", event.getCorrelationId())
                    .build();
                stateMachine.sendEvent(Mono.just(stateMachineMessage)).subscribe();
                log.info("Wallet created: {}", event.getWalletId());
            } else {
                stateMachine.sendEvent(Mono.just(
                    MessageBuilder.withPayload(SagaEvents.SAGA_FAILED).build()
                )).subscribe();
                log.info("Failed to create wallet: Missing correlationId");
            }
        };
    }
}
```

**Binding:** `walletCreatedEventConsumerFunction-in-0` → `wallet-created-topic`

**State Transition:** `INITIAL` → `WALLET_CREATED` (on success)

---

#### FundsAddedEventConsumer

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/adapter/event/consumer/FundsAddedEventConsumer.java`

```java
@Configuration
@Slf4j
public class FundsAddedEventConsumer {
    private final StateMachine<SagaStates, SagaEvents> stateMachine;

    @Bean
    public Consumer<Message<FundsAddedEvent>> fundsAddedEventConsumerFunction() {
        return message -> {
            var event = message.getPayload();
            String corr = event.correlationId();

            if (corr == null || corr.isBlank()) {
                stateMachine.sendEvent(Mono.just(
                    MessageBuilder.withPayload(SagaEvents.SAGA_FAILED).build()
                )).subscribe();
                log.warn("Failed to add funds due to missing correlationId for wallet {}",
                         event.walletId());
                return;
            }

            var stateMachineMessage = MessageBuilder
                .withPayload(SagaEvents.FUNDS_ADDED)
                .setHeader("correlationId", corr)
                .build();
            stateMachine.sendEvent(Mono.just(stateMachineMessage)).subscribe();
            log.info("Funds added: {} to wallet {}", event.amount(), event.walletId());
        };
    }
}
```

**Binding:** `fundsAddedEventConsumerFunction-in-0` → `funds-added-topic`

**State Transition:** `WALLET_CREATED` → `FUNDS_ADDED`

---

#### FundsWithdrawnEventConsumer

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/adapter/event/consumer/FundsWithdrawnEventConsumer.java`

```java
@Configuration
@Slf4j
public class FundsWithdrawnEventConsumer {
    private final StateMachine<SagaStates, SagaEvents> stateMachine;

    @Bean
    public Consumer<Message<FundsWithdrawnEvent>> fundsWithdrawnEventConsumerFunction() {
        return message -> {
            var event = message.getPayload();
            if (event.correlationId() != null) {
                var stateMachineMessage = MessageBuilder
                    .withPayload(SagaEvents.FUNDS_WITHDRAWN)
                    .setHeader("correlationId", event.correlationId())
                    .build();
                stateMachine.sendEvent(Mono.just(stateMachineMessage)).subscribe();
                log.info("Funds withdrawn: {} from wallet {}",
                         event.amount(), event.walletId());
            } else {
                stateMachine.sendEvent(Mono.just(
                    MessageBuilder.withPayload(SagaEvents.SAGA_FAILED).build()
                )).subscribe();
                log.info("Failed to withdraw funds: Missing correlationId");
            }
        };
    }
}
```

**Binding:** `fundsWithdrawnEventConsumerFunction-in-0` → `funds-withdrawn-topic`

**State Transition:** `FUNDS_ADDED` → `FUNDS_WITHDRAWN`

---

#### FundsTransferredEventConsumer

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/adapter/event/consumer/FundsTransferredEventConsumer.java`

```java
@Configuration
@Slf4j
public class FundsTransferredEventConsumer {
    private final StateMachine<SagaStates, SagaEvents> stateMachine;

    @Bean
    public Consumer<Message<FundsTransferredEvent>> fundsTransferredEventConsumerFunction() {
        return message -> {
            var event = message.getPayload();
            var stateMachineMessage = MessageBuilder
                .withPayload(SagaEvents.FUNDS_TRANSFERRED)
                .setHeader("correlationId", event.correlationId())
                .build();
            stateMachine.sendEvent(Mono.just(stateMachineMessage)).subscribe();
            log.info("Funds transferred: {} from wallet {} to wallet {}",
                     event.amount(), event.fromWalletId(), event.toWalletId());
        };
    }
}
```

**Binding:** `fundsTransferredEventConsumerFunction-in-0` → `funds-transferred-topic`

**State Transition:** `FUNDS_WITHDRAWN` → `FUNDS_TRANSFERRED` → `COMPLETED`

---

### CloudEvents Integration

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/util/CloudEventUtils.java`

```java
public class CloudEventUtils {
    public static <T> CloudEvent createCloudEvent(T data, String type, String source) {
        byte[] json = toJsonBytes(data);
        return CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType(type)
            .withSource(URI.create(source))
            .withDataContentType("application/json")
            .withData(json)
            .build();
    }

    public static <T> CloudEvent createCloudEvent(T data, String type,
                                                   String source, String correlationId) {
        byte[] json = toJsonBytes(data);
        return CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType(type)
            .withSource(URI.create(source))
            .withExtension("correlationid", correlationId)
            .withDataContentType("application/json")
            .withData(json)
            .build();
    }
}
```

**CloudEvents Specification:**

- **Version:** 1.0
- **Content Type:** `application/json`
- **Extension Attributes:** `correlationid` for distributed tracing

**Usage Example:**

```java
CloudEvent cloudEvent = CloudEventUtils.createCloudEvent(
    walletCreatedEvent,
    "dev.bloco.wallet.WalletCreated",
    "wallet-hub",
    correlationId.toString()
);
```

**Benefits:**

- Standardized event format across systems
- Vendor-neutral event representation
- Built-in metadata (ID, type, source, timestamp)
- Extension attributes for custom metadata

---

## Data Providers

### Overview

Data providers implement the repository pattern using Spring Data JPA, providing persistence for domain models while maintaining clean architecture boundaries.

### Architecture

```
┌──────────────┐
│  Use Cases   │
└──────┬───────┘
       │ Uses
       v
┌──────────────┐ (Port Interface)
│   Gateway    │
│  Interface   │
└──────┬───────┘
       │ Implemented by
       v
┌──────────────┐
│JpaRepository │ (Adapter)
│Implementation│
└──────┬───────┘
       │ Uses
       v
┌──────────────┐    ┌──────────────┐
│   Mapper     │───>│   Entity     │
└──────────────┘    └──────────────┘
       │                   │
       v                   v
┌──────────────┐    ┌──────────────┐
│Domain Model  │    │  JPA Table   │
└──────────────┘    └──────────────┘
```

---

### JPA Entities

#### WalletEntity

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/entity/WalletEntity.java`

```java
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "wallets")
public class WalletEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal balance;

    @Override
    public final boolean equals(Object o) {
        // Hibernate proxy-safe equals implementation
    }

    @Override
    public final int hashCode() {
        // Hibernate proxy-safe hashCode implementation
    }
}
```

**Schema:**

| Column  | Type         | Constraints      | Description                |
|---------|--------------|------------------|----------------------------|
| id      | UUID         | PRIMARY KEY      | Wallet unique identifier   |
| user_id | UUID         | NOT NULL         | Owner user identifier      |
| balance | DECIMAL      | NOT NULL         | Current wallet balance     |

---

#### UserEntity

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/entity/UserEntity.java`

```java
@Setter
@Getter
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;
}
```

**Schema:**

| Column | Type    | Constraints | Description            |
|--------|---------|-------------|------------------------|
| id     | UUID    | PRIMARY KEY | User unique identifier |
| name   | VARCHAR | NOT NULL    | User full name         |
| email  | VARCHAR | NOT NULL    | User email address     |

---

#### TransactionEntity

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/entity/TransactionEntity.java`

```java
@Setter
@Getter
@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID networkId;

    @Column(nullable = false, unique = true)
    private String hash;

    @Column(nullable = false)
    private String fromAddress;

    @Column(nullable = false)
    private String toAddress;

    @Column(nullable = false)
    private BigDecimal value;

    private BigDecimal gasPrice;
    private BigDecimal gasLimit;
    private BigDecimal gasUsed;

    @Lob
    private String data;

    @Column(nullable = false)
    private Instant timestamp;

    private Long blockNumber;
    private String blockHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;
}
```

**Schema:**

| Column       | Type             | Constraints           | Description                    |
|--------------|------------------|-----------------------|--------------------------------|
| id           | UUID             | PRIMARY KEY           | Transaction identifier         |
| network_id   | UUID             | NOT NULL              | Blockchain network ID          |
| hash         | VARCHAR          | NOT NULL, UNIQUE      | Transaction hash               |
| from_address | VARCHAR          | NOT NULL              | Sender address                 |
| to_address   | VARCHAR          | NOT NULL              | Recipient address              |
| value        | DECIMAL          | NOT NULL              | Transaction value              |
| gas_price    | DECIMAL          | NULLABLE              | Gas price in wei               |
| gas_limit    | DECIMAL          | NULLABLE              | Gas limit                      |
| gas_used     | DECIMAL          | NULLABLE              | Actual gas used                |
| data         | TEXT             | NULLABLE              | Transaction input data         |
| timestamp    | TIMESTAMP        | NOT NULL              | Transaction timestamp          |
| block_number | BIGINT           | NULLABLE              | Block number (when mined)      |
| block_hash   | VARCHAR          | NULLABLE              | Block hash (when mined)        |
| status       | VARCHAR (ENUM)   | NOT NULL              | Transaction status             |

**Status Enum Values:** `PENDING`, `CONFIRMED`, `FAILED`, `DROPPED`

---

### Spring Data Repositories

#### SpringDataWalletRepository

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataWalletRepository.java`

```java
public interface SpringDataWalletRepository extends JpaRepository<WalletEntity, UUID> {
}
```

**Inherited Methods:**

- `save(WalletEntity)` - Create or update wallet
- `findById(UUID)` - Find by primary key
- `findAll()` - Retrieve all wallets
- `deleteById(UUID)` - Delete wallet
- `existsById(UUID)` - Check existence

---

#### JpaWalletRepository

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java`

```java
@Repository
public class JpaWalletRepository implements WalletRepository {
    private final SpringDataWalletRepository springDataWalletRepository;
    private final WalletMapper walletMapper;

    @Override
    public Optional<Wallet> findById(UUID id) {
        return springDataWalletRepository.findById(id)
            .map(walletMapper::toDomain);
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletEntity entity = walletMapper.toEntity(wallet);
        return walletMapper.toDomain(springDataWalletRepository.save(entity));
    }

    @Override
    public void update(Wallet wallet) {
        WalletEntity entity = walletMapper.toEntity(wallet);
        springDataWalletRepository.save(entity);
    }

    @Override
    public List<Wallet> findAll() {
        return springDataWalletRepository.findAll().stream()
            .map(walletMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Wallet> findByUserId(UUID userId) {
        return findAll().stream()
            .filter(wallet -> userId != null && userId.equals(wallet.getUserId()))
            .collect(Collectors.toList());
    }
}
```

**Pattern:** Adapter that implements domain gateway interface.

**Responsibilities:**

- Delegate to Spring Data repository
- Use mapper to convert between domain and entity
- Provide domain-friendly API

---

#### OutboxRepository

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/OutboxRepository.java`

```java
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findBySentFalse();
}
```

**Custom Query:** `findBySentFalse()` retrieves all unsent events for worker processing.

---

#### StateMachineRepository

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/StateMachineRepository.java`

```java
public interface StateMachineRepository extends JpaStateMachineRepository {
}
```

**Purpose:** Spring State Machine JPA persistence.

**Managed Entities:**

- `JpaRepositoryAction`
- `JpaRepositoryGuard`
- `JpaRepositoryState`
- `JpaRepositoryStateMachine`
- `JpaRepositoryTransition`

---

### MapStruct Mappers

MapStruct generates type-safe, high-performance mapping code at compile time.

#### WalletMapper

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/mapper/WalletMapper.java`

```java
@Mapper(componentModel = "spring")
public interface WalletMapper {
    default Wallet toDomain(WalletEntity entity) {
        if (entity == null) return null;
        Wallet wallet = new Wallet(entity.getId(), "Wallet", "");
        wallet.setBalance(entity.getBalance());
        return wallet;
    }

    default WalletEntity toEntity(Wallet domain) {
        if (domain == null) return null;
        WalletEntity entity = new WalletEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getId()); // Placeholder
        entity.setBalance(domain.getBalance());
        return entity;
    }
}
```

**Configuration:**

- `componentModel = "spring"` - Generates Spring `@Component`
- Custom `default` methods for complex mappings

---

#### UserMapper

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/mapper/UserMapper.java`

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "name", source = "name"),
        @Mapping(target = "email", source = "email")
    })
    User toDomain(UserEntity entity);

    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "name", source = "name"),
        @Mapping(target = "email", source = "email")
    })
    UserEntity toEntity(User domain);
}
```

**Features:**

- Explicit field mappings via `@Mapping`
- Bidirectional conversion
- Compile-time type checking

---

#### TransactionMapper

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/mapper/TransactionMapper.java`

```java
@Mapper(componentModel = "spring")
public interface TransactionMapper {
    default Transaction toDomain(TransactionEntity entity) {
        if (entity == null) return null;
        return Transaction.rehydrate(
            entity.getId(),
            entity.getNetworkId(),
            new TransactionHash(entity.getHash()),
            entity.getFromAddress(),
            entity.getToAddress(),
            entity.getValue(),
            entity.getData(),
            entity.getTimestamp(),
            entity.getBlockNumber(),
            entity.getBlockHash(),
            entity.getStatus(),
            entity.getGasPrice(),
            entity.getGasLimit(),
            entity.getGasUsed()
        );
    }

    default TransactionEntity toEntity(Transaction domain) {
        if (domain == null) return null;
        TransactionEntity entity = new TransactionEntity();
        entity.setId(domain.getId());
        entity.setNetworkId(domain.getNetworkId());
        entity.setHash(domain.getHash());
        entity.setFromAddress(domain.getFromAddress());
        entity.setToAddress(domain.getToAddress());
        entity.setValue(domain.getValue());
        entity.setGasPrice(domain.getGasPrice());
        entity.setGasLimit(domain.getGasLimit());
        entity.setGasUsed(domain.getGasUsed());
        entity.setData(domain.getData());
        entity.setTimestamp(domain.getTimestamp());
        entity.setBlockNumber(domain.getBlockNumber());
        entity.setBlockHash(domain.getBlockHash());
        entity.setStatus(domain.getStatus());
        return entity;
    }
}
```

**Pattern:** Uses domain factory method `rehydrate()` to reconstruct immutable domain object.

---

## State Machine Configuration

### Overview

Spring State Machine orchestrates saga workflows for distributed transactions, with JPA persistence ensuring state survives restarts.

### Architecture

```
┌───────────────────────────────────────────────────────────┐
│                    Saga Lifecycle                         │
└───────────────────────────────────────────────────────────┘

INITIAL
   │
   │ WALLET_CREATED event
   v
WALLET_CREATED
   │
   │ FUNDS_ADDED event
   v
FUNDS_ADDED
   │
   │ FUNDS_WITHDRAWN event
   v
FUNDS_WITHDRAWN
   │
   │ FUNDS_TRANSFERRED event
   v
FUNDS_TRANSFERRED
   │
   │ SAGA_COMPLETED event
   v
COMPLETED

(Any state) ──SAGA_FAILED event──> FAILED
```

---

### SagaStates Enum

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/config/SagaStates.java`

```java
public enum SagaStates {
    INITIAL,            // Starting state
    WALLET_CREATED,     // Wallet created successfully
    FUNDS_ADDED,        // Funds added to wallet
    FUNDS_WITHDRAWN,    // Funds withdrawn from wallet
    FUNDS_TRANSFERRED,  // Funds transferred between wallets
    COMPLETED,          // Saga completed successfully (end state)
    FAILED,             // Saga failed (end state)
    ANY                 // Generic state for wildcard transitions
}
```

**State Types:**

- **Initial State:** `INITIAL`
- **Intermediate States:** `WALLET_CREATED`, `FUNDS_ADDED`, `FUNDS_WITHDRAWN`, `FUNDS_TRANSFERRED`
- **End States:** `COMPLETED`, `FAILED`

---

### SagaEvents Enum

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/config/SagaEvents.java`

```java
public enum SagaEvents {
    WALLET_CREATED,      // Wallet creation event
    FUNDS_ADDED,         // Funds addition event
    FUNDS_WITHDRAWN,     // Funds withdrawal event
    FUNDS_TRANSFERRED,   // Funds transfer event
    SAGA_COMPLETED,      // Saga completion event
    SAGA_FAILED          // Saga failure event
}
```

**Event Categories:**

- **Success Events:** Trigger forward transitions
- **Failure Event:** `SAGA_FAILED` - transitions to FAILED state from any state

---

### SagaStateMachineConfig

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/java/dev/bloco/wallet/hub/infra/provider/data/config/SagaStateMachineConfig.java`

```java
@Configuration
@EnableStateMachine
public class SagaStateMachineConfig
        extends StateMachineConfigurerAdapter<SagaStates, SagaEvents> {

    private final StateMachineRepository stateMachineRepository;

    @Override
    public void configure(StateMachineStateConfigurer<SagaStates, SagaEvents> states)
            throws Exception {
        states.withStates()
            .initial(SagaStates.INITIAL)
            .state(SagaStates.WALLET_CREATED)
            .state(SagaStates.FUNDS_ADDED)
            .state(SagaStates.FUNDS_WITHDRAWN)
            .state(SagaStates.FUNDS_TRANSFERRED)
            .end(SagaStates.COMPLETED)
            .end(SagaStates.FAILED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<SagaStates, SagaEvents> transitions)
            throws Exception {
        transitions
            // Happy path transitions
            .withExternal()
                .source(SagaStates.INITIAL)
                .target(SagaStates.WALLET_CREATED)
                .event(SagaEvents.WALLET_CREATED)
            .and()
            .withExternal()
                .source(SagaStates.WALLET_CREATED)
                .target(SagaStates.FUNDS_ADDED)
                .event(SagaEvents.FUNDS_ADDED)
            .and()
            .withExternal()
                .source(SagaStates.FUNDS_ADDED)
                .target(SagaStates.FUNDS_WITHDRAWN)
                .event(SagaEvents.FUNDS_WITHDRAWN)
            .and()
            .withExternal()
                .source(SagaStates.FUNDS_WITHDRAWN)
                .target(SagaStates.FUNDS_TRANSFERRED)
                .event(SagaEvents.FUNDS_TRANSFERRED)
            .and()
            .withExternal()
                .source(SagaStates.FUNDS_TRANSFERRED)
                .target(SagaStates.COMPLETED)
                .event(SagaEvents.SAGA_COMPLETED)
            .and()
            // Failure transitions from each state
            .withExternal()
                .source(SagaStates.INITIAL)
                .target(SagaStates.FAILED)
                .event(SagaEvents.SAGA_FAILED)
            .and()
            .withExternal()
                .source(SagaStates.WALLET_CREATED)
                .target(SagaStates.FAILED)
                .event(SagaEvents.SAGA_FAILED)
            .and()
            .withExternal()
                .source(SagaStates.FUNDS_ADDED)
                .target(SagaStates.FAILED)
                .event(SagaEvents.SAGA_FAILED)
            .and()
            .withExternal()
                .source(SagaStates.FUNDS_WITHDRAWN)
                .target(SagaStates.FAILED)
                .event(SagaEvents.SAGA_FAILED)
            .and()
            .withExternal()
                .source(SagaStates.FUNDS_TRANSFERRED)
                .target(SagaStates.FAILED)
                .event(SagaEvents.SAGA_FAILED);
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<SagaStates, SagaEvents> config)
            throws Exception {
        config
            .withConfiguration()
                .machineId("sagaStateMachine")
            .and()
            .withPersistence()
                .runtimePersister(
                    new JpaPersistingStateMachineInterceptor<>(stateMachineRepository)
                );
    }
}
```

---

### State Transition Table

| From State         | Event               | To State           | Description                        |
|--------------------|---------------------|--------------------|-------------------------------------|
| INITIAL            | WALLET_CREATED      | WALLET_CREATED     | Wallet successfully created         |
| WALLET_CREATED     | FUNDS_ADDED         | FUNDS_ADDED        | Funds added to wallet               |
| FUNDS_ADDED        | FUNDS_WITHDRAWN     | FUNDS_WITHDRAWN    | Funds withdrawn from wallet         |
| FUNDS_WITHDRAWN    | FUNDS_TRANSFERRED   | FUNDS_TRANSFERRED  | Funds transferred between wallets   |
| FUNDS_TRANSFERRED  | SAGA_COMPLETED      | COMPLETED          | Saga workflow completed             |
| (Any state)        | SAGA_FAILED         | FAILED             | Saga workflow failed                |

---

### Persistence Configuration

**Persister:** `JpaPersistingStateMachineInterceptor`

**Benefits:**

- State machine state persisted to database
- Survives application restarts
- Enables distributed saga coordination
- Audit trail of state transitions

**Database Tables:**

- `jpa_repository_state_machine` - Machine instances
- `jpa_repository_state` - State definitions
- `jpa_repository_transition` - Transition definitions
- `jpa_repository_action` - Action definitions
- `jpa_repository_guard` - Guard conditions

---

## Configuration

### Application Configuration

**Location:** `/Users/italo/Projects/lab/wallet/bloco-wallet-java/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: wallet-hub

  datasource:
    url: jdbc:h2:file:./db/wallet;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true  # http://localhost:8080/h2-console

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    database-platform: org.hibernate.dialect.H2Dialect

  cloud:
    function:
      definition: walletCreatedEventConsumerFunction;fundsAddedEventConsumerFunction;fundsWithdrawnEventConsumerFunction;fundsTransferredEventConsumerFunction

    stream:
      bindings:
        # Producer bindings
        walletCreatedEventProducer-out-0:
          destination: wallet-created-topic
        fundsAddedEventProducer-out-0:
          destination: funds-added-topic
        fundsWithdrawnEventProducer-out-0:
          destination: funds-withdrawn-topic
        fundsTransferredEventProducer-out-0:
          destination: funds-transferred-topic

        # Consumer bindings
        walletCreatedEventConsumerFunction-in-0:
          destination: wallet-created-topic
        fundsAddedEventConsumerFunction-in-0:
          destination: funds-added-topic
        fundsWithdrawnEventConsumerFunction-in-0:
          destination: funds-withdrawn-topic
        fundsTransferredEventConsumerFunction-in-0:
          destination: funds-transferred-topic

      kafka:
        binder:
          brokers: localhost:9092

wallet:
  networks:
    chainlist-url: https://chainlist.org/rpcs.json
    cache-ttl: PT5M
```

---

### Kafka Binding Configuration

**Producer Bindings:**

| Binding Name                        | Topic                      | Direction |
|-------------------------------------|----------------------------|-----------|
| walletCreatedEventProducer-out-0    | wallet-created-topic       | OUT       |
| fundsAddedEventProducer-out-0       | funds-added-topic          | OUT       |
| fundsWithdrawnEventProducer-out-0   | funds-withdrawn-topic      | OUT       |
| fundsTransferredEventProducer-out-0 | funds-transferred-topic    | OUT       |

**Consumer Bindings:**

| Binding Name                              | Topic                      | Direction |
|-------------------------------------------|----------------------------|-----------|
| walletCreatedEventConsumerFunction-in-0   | wallet-created-topic       | IN        |
| fundsAddedEventConsumerFunction-in-0      | funds-added-topic          | IN        |
| fundsWithdrawnEventConsumerFunction-in-0  | funds-withdrawn-topic      | IN        |
| fundsTransferredEventConsumerFunction-in-0| funds-transferred-topic    | IN        |

**Binding Naming Convention:**

- **Producers:** `{functionName}-out-{index}`
- **Consumers:** `{functionName}-in-{index}`

---

### Environment Overrides

**PostgreSQL:**

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wallet
export SPRING_DATASOURCE_USERNAME=wallet_user
export SPRING_DATASOURCE_PASSWORD=secure_password
export SPRING_JPA_HIBERNATE_DDL_AUTO=validate
export SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
```

**Kafka:**

```bash
export SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS=kafka1:9092,kafka2:9092
export SPRING_CLOUD_STREAM_KAFKA_BINDER_AUTO_CREATE_TOPICS=false
```

**Test Binder (No Kafka):**

```bash
export SPRING_CLOUD_STREAM_DEFAULTBINDER=test
```

---

## Best Practices

### Event Production

1. **Always use Outbox Pattern** for transactional events
2. **Include Correlation IDs** for distributed tracing
3. **Serialize to JSON** with Jackson, register Java Time module
4. **Log event production** with INFO level
5. **Handle serialization errors** gracefully

**Example:**

```java
@Transactional
public void createWallet(CreateWalletCommand command) {
    // 1. Execute domain logic
    Wallet wallet = walletService.createWallet(command);

    // 2. Save domain entity
    walletRepository.save(wallet);

    // 3. Publish event (saved to outbox in same transaction)
    WalletCreatedEvent event = new WalletCreatedEvent(
        wallet.getId(),
        command.getCorrelationId()
    );
    eventProducer.produceWalletCreatedEvent(event);

    // Transaction commits: both wallet and outbox event persisted atomically
}
```

---

### Event Consumption

1. **Validate Correlation ID** before processing
2. **Use idempotent operations** (consumers may receive duplicates)
3. **Send State Machine events** asynchronously with `.subscribe()`
4. **Log successful operations** with context (IDs, amounts)
5. **Handle missing correlation ID** by sending SAGA_FAILED event

**Example:**

```java
@Bean
public Consumer<Message<FundsAddedEvent>> fundsAddedEventConsumerFunction() {
    return message -> {
        var event = message.getPayload();

        // Validate
        if (event.correlationId() == null || event.correlationId().isBlank()) {
            stateMachine.sendEvent(Mono.just(
                MessageBuilder.withPayload(SagaEvents.SAGA_FAILED).build()
            )).subscribe();
            log.warn("Missing correlationId for wallet {}", event.walletId());
            return;
        }

        // Process
        var smMessage = MessageBuilder
            .withPayload(SagaEvents.FUNDS_ADDED)
            .setHeader("correlationId", event.correlationId())
            .build();
        stateMachine.sendEvent(Mono.just(smMessage)).subscribe();

        // Log
        log.info("Funds added: {} to wallet {}", event.amount(), event.walletId());
    };
}
```

---

### Repository Implementation

1. **Implement Gateway Interface** (Port)
2. **Delegate to Spring Data Repository**
3. **Use Mapper** for domain ↔ entity conversion
4. **Return Optional** for find operations
5. **Throw Domain Exceptions** for business rule violations

**Example:**

```java
@Repository
public class JpaUserRepository implements UserRepository {
    private final SpringDataUserRepository springDataRepo;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findById(UUID id) {
        return springDataRepo.findById(id)
            .map(userMapper::toDomain);
    }

    @Override
    public User save(User user) {
        // Validate business rules
        if (user.getEmail() == null) {
            throw new InvalidUserException("Email is required");
        }

        // Convert and persist
        UserEntity entity = userMapper.toEntity(user);
        UserEntity saved = springDataRepo.save(entity);
        return userMapper.toDomain(saved);
    }
}
```

---

### MapStruct Mapper Design

1. **Use `componentModel = "spring"`** for Spring integration
2. **Implement bidirectional mapping** (toDomain, toEntity)
3. **Handle null values** explicitly
4. **Use custom `default` methods** for complex mappings
5. **Leverage domain factory methods** for immutable objects

**Example:**

```java
@Mapper(componentModel = "spring")
public interface AddressMapper {
    default Address toDomain(AddressEntity entity) {
        if (entity == null) return null;

        // Use domain factory method for immutable object
        return Address.create(
            entity.getId(),
            entity.getWalletId(),
            entity.getNetworkId(),
            entity.getAddress(),
            entity.getStatus()
        );
    }

    default AddressEntity toEntity(Address domain) {
        if (domain == null) return null;

        AddressEntity entity = new AddressEntity();
        entity.setId(domain.getId());
        entity.setWalletId(domain.getWalletId());
        entity.setNetworkId(domain.getNetworkId());
        entity.setAddress(domain.getAddressValue());
        entity.setStatus(domain.getStatus());
        return entity;
    }
}
```

---

### State Machine Usage

1. **Always include Correlation ID** in event headers
2. **Send events asynchronously** with `Mono.just()` and `.subscribe()`
3. **Handle state transitions** in consumer functions
4. **Persist state machine** for long-running sagas
5. **Monitor state transitions** with listeners

**Example:**

```java
// Send event to state machine
var message = MessageBuilder
    .withPayload(SagaEvents.FUNDS_ADDED)
    .setHeader("correlationId", correlationId)
    .setHeader("walletId", walletId)
    .build();

Flux<StateMachineEventResult<SagaStates, SagaEvents>> result =
    stateMachine.sendEvent(Mono.just(message));

result.subscribe(
    eventResult -> log.info("State transition: {}", eventResult.getResultType()),
    error -> log.error("State machine error", error)
);
```

---

## Testing Guidelines

### Testing Event Producers

**Use Test Binder:**

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.stream.defaultBinder=test"
})
class KafkaEventProducerTest {
    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private OutputDestination output;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void shouldSaveEventToOutbox() {
        // Given
        var event = new WalletCreatedEvent(UUID.randomUUID(), "corr-123");

        // When
        eventProducer.produceWalletCreatedEvent(event);

        // Then
        List<OutboxEvent> events = outboxRepository.findBySentFalse();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("walletCreatedEventProducer");
        assertThat(events.get(0).getCorrelationId()).isEqualTo("corr-123");
    }
}
```

---

### Testing Event Consumers

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.stream.defaultBinder=test"
})
class FundsAddedEventConsumerTest {
    @Autowired
    private InputDestination input;

    @Autowired
    private StateMachine<SagaStates, SagaEvents> stateMachine;

    @Test
    void shouldTransitionStateOnValidEvent() throws Exception {
        // Given
        stateMachine.startReactively().block();
        stateMachine.sendEvent(Mono.just(
            MessageBuilder.withPayload(SagaEvents.WALLET_CREATED).build()
        )).blockLast();

        var event = new FundsAddedEvent(
            UUID.randomUUID(),
            BigDecimal.valueOf(100),
            "corr-123"
        );

        // When
        input.send(MessageBuilder
            .withPayload(event)
            .build(),
            "funds-added-topic"
        );

        // Wait for processing
        Thread.sleep(500);

        // Then
        assertThat(stateMachine.getState().getId()).isEqualTo(SagaStates.FUNDS_ADDED);
    }
}
```

---

### Testing Repositories

**Use @DataJpaTest:**

```java
@DataJpaTest
@Import({JpaWalletRepository.class, WalletMapperImpl.class})
class JpaWalletRepositoryTest {
    @Autowired
    private JpaWalletRepository walletRepository;

    @Test
    void shouldSaveAndFindWallet() {
        // Given
        Wallet wallet = new Wallet(null, "Test Wallet", "Description");
        wallet.setBalance(BigDecimal.valueOf(1000));

        // When
        Wallet saved = walletRepository.save(wallet);
        Optional<Wallet> found = walletRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }
}
```

---

### Testing Mappers

```java
@SpringBootTest
class WalletMapperTest {
    @Autowired
    private WalletMapper walletMapper;

    @Test
    void shouldMapDomainToEntity() {
        // Given
        Wallet domain = new Wallet(UUID.randomUUID(), "Test Wallet", "Desc");
        domain.setBalance(BigDecimal.valueOf(500));

        // When
        WalletEntity entity = walletMapper.toEntity(domain);

        // Then
        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getBalance()).isEqualByComparingTo(domain.getBalance());
    }

    @Test
    void shouldMapEntityToDomain() {
        // Given
        WalletEntity entity = new WalletEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(UUID.randomUUID());
        entity.setBalance(BigDecimal.valueOf(500));

        // When
        Wallet domain = walletMapper.toDomain(entity);

        // Then
        assertThat(domain.getId()).isEqualTo(entity.getId());
        assertThat(domain.getBalance()).isEqualByComparingTo(entity.getBalance());
    }
}
```

---

## Summary

The Infrastructure Layer provides:

1. **Event Adapters**: Reliable event production/consumption via Kafka with Outbox pattern
2. **Data Providers**: JPA-based persistence with clean domain/entity separation
3. **State Machine**: Saga orchestration for distributed transactions
4. **Configuration**: Spring Boot/Cloud configuration for messaging and persistence
5. **Mapping**: Type-safe bidirectional conversion between domain and persistence models

**Key Architectural Benefits:**

- Pure domain layer with zero framework dependencies
- Testable with in-memory test doubles
- Swappable infrastructure implementations
- Clear separation of concerns
- Reliable event delivery with transactional guarantees

**Related Documentation:**

- [Domain Layer Documentation](/Users/italo/Projects/lab/wallet/bloco-wallet-java/docs/modules/DOMAIN.md)
- [Database Schema](/Users/italo/Projects/lab/wallet/bloco-wallet-java/docs/database.md)
- [CLAUDE.md Project Guidelines](/Users/italo/Projects/lab/wallet/bloco-wallet-java/CLAUDE.md)

---

**Document Version:** 1.0
**Last Updated:** 2025-12-10
**Maintainer:** Wallet Hub Development Team
