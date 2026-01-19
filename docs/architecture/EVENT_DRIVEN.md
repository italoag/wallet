# Event-Driven Architecture

## Table of Contents

1. [Overview](#overview)
2. [Architecture Patterns](#architecture-patterns)
3. [Event Flow](#event-flow)
4. [CloudEvents Specification](#cloudevents-specification)
5. [Outbox Pattern](#outbox-pattern)
6. [Kafka Integration](#kafka-integration)
7. [Event Catalog](#event-catalog)
8. [Best Practices](#best-practices)
9. [Testing](#testing)

## Overview

Wallet Hub implements a comprehensive event-driven architecture using **Spring Cloud Stream**, **Apache Kafka**, and the **CloudEvents** specification. The system leverages asynchronous messaging patterns to achieve:

- **Decoupling**: Producers and consumers operate independently
- **Scalability**: Events can be processed asynchronously and in parallel
- **Reliability**: Transactional outbox pattern ensures guaranteed delivery
- **Traceability**: Correlation IDs enable end-to-end tracking
- **Saga Orchestration**: Distributed transactions coordinated via Spring State Machine

### Key Components

```
┌─────────────────┐
│   Use Cases     │  (Application Layer)
│  - CreateWallet │
│  - AddFunds     │
└────────┬────────┘
         │ publishes
         ▼
┌─────────────────┐
│ EventProducer   │  (Port Interface)
│  KafkaEvent     │
│  Producer       │
└────────┬────────┘
         │ saves to
         ▼
┌─────────────────┐
│ Outbox Table    │  (Transactional Outbox Pattern)
│  OutboxEvent    │
└────────┬────────┘
         │ polls
         ▼
┌─────────────────┐
│ OutboxWorker    │  (Scheduled @5s)
│  StreamBridge   │
└────────┬────────┘
         │ sends to
         ▼
┌─────────────────┐
│ Kafka Topics    │
│  wallet-created │
│  funds-added    │
│  funds-withdrawn│
│  funds-transfer │
└────────┬────────┘
         │ consumes from
         ▼
┌─────────────────┐
│ Event Consumers │
│  @Bean Function │
│  StateMachine   │
└─────────────────┘
```

## Architecture Patterns

### 1. Asynchronous Messaging Patterns

#### Publish-Subscribe Pattern

Domain events are published to Kafka topics, and multiple consumers can subscribe to the same events independently.

**Example Flow**:
```java
// Use Case publishes event
WalletCreatedEvent event = new WalletCreatedEvent(walletId, correlationId);
eventProducer.produceWalletCreatedEvent(event);

// Multiple consumers can subscribe
// - Audit Service
// - Analytics Service
// - Notification Service
```

#### Event Sourcing

Events represent immutable facts that have occurred in the system. All state changes produce domain events that serve as the source of truth.

**Benefits**:
- Complete audit trail
- Ability to replay events
- Time-travel debugging
- Event-driven projections

### 2. Domain Events

All domain events extend the `DomainEvent` base class:

```java
@Getter
public abstract class DomainEvent {
    private final UUID eventId;         // Unique event identifier
    private final Instant occurredOn;   // Event timestamp
    private final UUID correlationId;   // Correlation for tracking

    protected DomainEvent(UUID correlationId) {
        this.correlationId = correlationId;
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
    }
}
```

**Key Properties**:
- **eventId**: Unique identifier for this specific event instance
- **occurredOn**: Timestamp when the event was created
- **correlationId**: Links related events across the distributed system

### 3. Domain Event Publisher

Thread-local event publishing mechanism for in-process event handling:

```java
public class DomainEventPublisher {
    private static final ThreadLocal<List<Consumer<DomainEvent>>> SUBSCRIBERS = new ThreadLocal<>();

    public static void subscribe(Consumer<DomainEvent> subscriber) {
        // Register event subscribers
    }

    public static void publish(DomainEvent event) {
        // Publish to all subscribers
    }

    public static void reset() {
        // Clean up thread-local state
    }
}
```

**Usage Pattern**:
```java
// In use case
DomainEventPublisher.subscribe(event -> {
    if (event instanceof WalletCreatedEvent walletEvent) {
        // Handle event in same transaction
        eventProducer.produceWalletCreatedEvent(walletEvent);
    }
});

// Publish event
var event = new WalletCreatedEvent(walletId, correlationId);
DomainEventPublisher.publish(event);

// Clean up
DomainEventPublisher.reset();
```

## Event Flow

### From Use Case to Kafka

```
┌────────────────────────────────────────────────────────────────────┐
│ 1. Use Case Layer                                                  │
│    CreateWalletUseCase.execute()                                   │
│    - Creates domain aggregate (Wallet)                             │
│    - Publishes domain event                                        │
└──────────┬─────────────────────────────────────────────────────────┘
           │
           ▼
┌────────────────────────────────────────────────────────────────────┐
│ 2. Event Producer (KafkaEventProducer)                             │
│    - Serializes event to JSON                                      │
│    - Saves to OutboxEvent table                                    │
│    - Transaction commits                                           │
└──────────┬─────────────────────────────────────────────────────────┘
           │
           ▼
┌────────────────────────────────────────────────────────────────────┐
│ 3. Outbox Worker (Scheduled @5s)                                   │
│    - Polls unsent events from outbox                               │
│    - Resolves binding name (e.g., walletCreatedEventProducer-out-0)│
│    - Sends via StreamBridge                                        │
│    - Marks event as sent                                           │
└──────────┬─────────────────────────────────────────────────────────┘
           │
           ▼
┌────────────────────────────────────────────────────────────────────┐
│ 4. Spring Cloud Stream                                             │
│    - Serializes to CloudEvents format                              │
│    - Sends to Kafka topic (wallet-created-topic)                   │
└──────────┬─────────────────────────────────────────────────────────┘
           │
           ▼
┌────────────────────────────────────────────────────────────────────┐
│ 5. Kafka Broker                                                    │
│    - Persists message to topic partition                           │
│    - Replicates across brokers                                     │
└──────────┬─────────────────────────────────────────────────────────┘
           │
           ▼
┌────────────────────────────────────────────────────────────────────┐
│ 6. Event Consumer (walletCreatedEventConsumerFunction)             │
│    - Deserializes CloudEvent                                       │
│    - Extracts correlation ID                                       │
│    - Sends event to State Machine                                  │
│    - Updates saga state                                            │
└────────────────────────────────────────────────────────────────────┘
```

### Consumer Processing

Consumers are implemented as Spring Cloud Function beans:

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
                // Send to state machine for saga coordination
                var stateMachineMessage = MessageBuilder
                    .withPayload(SagaEvents.WALLET_CREATED)
                    .setHeader("correlationId", event.getCorrelationId())
                    .build();

                stateMachine.sendEvent(Mono.just(stateMachineMessage)).subscribe();
                log.info("Wallet created: {}", event.getWalletId());
            } else {
                // Handle missing correlation ID
                stateMachine.sendEvent(
                    Mono.just(MessageBuilder.withPayload(SagaEvents.SAGA_FAILED).build())
                ).subscribe();
                log.warn("Failed to create wallet: Missing correlationId");
            }
        };
    }
}
```

**Consumer Best Practices**:
- Validate correlation ID presence
- Handle failures gracefully
- Update state machine for saga coordination
- Log important events
- Implement idempotency checks

## CloudEvents Specification

### Overview

Wallet Hub uses the [CloudEvents](https://cloudevents.io/) specification (v1.0) to standardize event metadata across all published events.

**Benefits**:
- Interoperability between systems
- Standardized event structure
- Cloud-native integration
- Consistent metadata

### CloudEvent Format

```json
{
  "specversion": "1.0",
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "type": "dev.bloco.wallet.hub.wallet.created",
  "source": "wallet-hub-service",
  "datacontenttype": "application/json",
  "correlationid": "550e8400-e29b-41d4-a716-446655440000",
  "time": "2025-12-10T10:30:00Z",
  "data": {
    "walletId": "123e4567-e89b-12d3-a456-426614174000",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "occurredOn": "2025-12-10T10:30:00Z"
  }
}
```

### CloudEvent Attributes

| Attribute | Required | Description | Example |
|-----------|----------|-------------|---------|
| `id` | Yes | Unique identifier for the event | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |
| `source` | Yes | Identifies the context in which the event occurred | `wallet-hub-service` |
| `type` | Yes | Describes the type of event | `dev.bloco.wallet.hub.wallet.created` |
| `specversion` | Yes | CloudEvents specification version | `1.0` |
| `datacontenttype` | No | Content type of the data attribute | `application/json` |
| `correlationid` | No | Extension: Links related events | `550e8400-e29b-41d4-a716-446655440000` |
| `time` | No | Timestamp of when the event occurred | `2025-12-10T10:30:00Z` |
| `data` | No | Event payload | `{ "walletId": "...", ... }` |

### CloudEventUtils Usage

The `CloudEventUtils` utility class provides convenient methods for creating CloudEvents:

```java
package dev.bloco.wallet.hub.infra.util;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.util.UUID;

public class CloudEventUtils {

    /**
     * Creates a CloudEvent without correlation ID
     */
    public static <T> CloudEvent createCloudEvent(
        T data,
        String type,
        String source
    ) {
        byte[] json = toJsonBytes(data);
        return CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType(type)
            .withSource(URI.create(source))
            .withDataContentType("application/json")
            .withData(json)
            .build();
    }

    /**
     * Creates a CloudEvent with correlation ID extension
     */
    public static <T> CloudEvent createCloudEvent(
        T data,
        String type,
        String source,
        String correlationId
    ) {
        byte[] json = toJsonBytes(data);
        return CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType(type)
            .withSource(URI.create(source))
            .withExtension("correlationid", correlationId)  // Custom extension
            .withDataContentType("application/json")
            .withData(json)
            .build();
    }
}
```

**Usage Example**:
```java
// Create CloudEvent with correlation ID
var event = new WalletCreatedEvent(walletId, correlationId);
CloudEvent cloudEvent = CloudEventUtils.createCloudEvent(
    event,
    "dev.bloco.wallet.hub.wallet.created",
    "wallet-hub-service",
    correlationId.toString()
);
```

### Type Naming Convention

CloudEvent type names follow the reverse DNS notation pattern:

```
{domain}.{service}.{aggregate}.{action}

Examples:
- dev.bloco.wallet.hub.wallet.created
- dev.bloco.wallet.hub.funds.added
- dev.bloco.wallet.hub.transaction.confirmed
```

## Outbox Pattern

### Overview

The **Transactional Outbox Pattern** ensures reliable event delivery by:

1. Storing events in the same database transaction as domain changes
2. Using a background worker to poll and publish events
3. Guaranteeing at-least-once delivery semantics
4. Preventing dual-write problems

### Database Schema

```sql
CREATE TABLE outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    correlation_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_outbox_sent ON outbox(sent);
CREATE INDEX idx_outbox_created_at ON outbox(created_at);
```

### OutboxEvent Entity

```java
@Entity
@Table(name = "outbox", indexes = {
    @Index(name = "idx_outbox_created_at", columnList = "created_at")
})
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;  // e.g., "walletCreatedEventProducer"

    @Column(name = "payload", nullable = false)
    private String payload;  // Serialized JSON

    @Column(name = "correlation_id")
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent", nullable = false)
    private boolean sent = false;
}
```

### OutboxService

Manages outbox event persistence and retrieval:

```java
@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;

    /**
     * Saves event to outbox within same transaction as domain changes
     */
    @Transactional
    public void saveOutboxEvent(
        String eventType,
        String payload,
        String correlationId
    ) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setCorrelationId(correlationId);
        outboxRepository.save(event);
    }

    /**
     * Retrieves all events that haven't been sent yet
     */
    public List<OutboxEvent> getUnsentEvents() {
        return outboxRepository.findBySentFalse();
    }

    /**
     * Marks event as sent after successful Kafka delivery
     */
    @Transactional
    public void markEventAsSent(OutboxEvent event) {
        event.setSent(true);
        outboxRepository.save(event);
    }
}
```

### OutboxWorker

Scheduled background worker that polls and publishes events:

```java
@Component
public class OutboxWorker {

    private final OutboxService outboxService;
    private final StreamBridge streamBridge;
    private final MeterRegistry meterRegistry;  // For metrics

    /**
     * Polls outbox every 5 seconds and publishes unsent events
     */
    @Scheduled(fixedRate = 5000)  // 5 seconds
    public void processOutbox() {
        for (OutboxEvent event : outboxService.getUnsentEvents()) {
            String eventType = event.getEventType();

            // Resolve binding name from event type
            Optional<String> bindingOpt = EventBindings.bindingForEventType(eventType);

            if (bindingOpt.isEmpty()) {
                log.warn("Unknown event type '{}', skipping outbox id={}",
                    eventType, event.getId());
                meterRegistry.counter("outbox.unknown.type",
                    "eventType", eventType).increment();
                continue;
            }

            String binding = bindingOpt.get();

            // Send to Kafka via StreamBridge
            boolean success = streamBridge.send(binding, event.getPayload());

            if (success) {
                // Mark as sent only after successful delivery
                outboxService.markEventAsSent(event);
                meterRegistry.counter("outbox.sent", "binding", binding).increment();
            } else {
                log.warn("Failed to send outbox id={} to binding {}",
                    event.getId(), binding);
                meterRegistry.counter("outbox.send.failed",
                    "binding", binding).increment();
            }
        }
    }
}
```

### EventBindings

Centralized mapping of event types to Spring Cloud Stream bindings:

```java
public final class EventBindings {

    // Binding names (must match application.yml)
    public static final String WALLET_CREATED_BINDING = "walletCreatedEventProducer-out-0";
    public static final String FUNDS_ADDED_BINDING = "fundsAddedEventProducer-out-0";
    public static final String FUNDS_WITHDRAWN_BINDING = "fundsWithdrawnEventProducer-out-0";
    public static final String FUNDS_TRANSFERRED_BINDING = "fundsTransferredEventProducer-out-0";

    // Event type -> binding mapping
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

### Reliability Guarantees

#### At-Least-Once Delivery

The outbox pattern guarantees **at-least-once delivery**:

1. Events are saved transactionally with domain changes
2. Worker polls until event is successfully sent
3. Events are marked as sent only after Kafka acknowledgment
4. Failed sends are automatically retried on next poll

**Implications**:
- Consumers must implement idempotency
- Duplicate events may occur (e.g., network failures, restarts)
- Event ordering within a partition is guaranteed

#### Idempotency Handling

Consumers should implement idempotency using:

```java
// Strategy 1: Store processed event IDs
@Service
public class EventIdempotencyService {

    private final Set<UUID> processedEventIds = new ConcurrentHashSet<>();

    public boolean isProcessed(UUID eventId) {
        return !processedEventIds.add(eventId);
    }
}

// Strategy 2: Use database unique constraints
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"event_id"})
})
public class ProcessedEvent {
    @Column(name = "event_id", unique = true)
    private UUID eventId;
}

// Usage in consumer
@Bean
public Consumer<Message<WalletCreatedEvent>> walletCreatedEventConsumerFunction() {
    return message -> {
        var event = message.getPayload();

        // Check if already processed
        if (idempotencyService.isProcessed(event.getEventId())) {
            log.info("Event {} already processed, skipping", event.getEventId());
            return;
        }

        // Process event
        processWalletCreated(event);

        // Mark as processed
        idempotencyService.markAsProcessed(event.getEventId());
    };
}
```

### Failure Scenarios

| Scenario | Behavior | Recovery |
|----------|----------|----------|
| Database failure during save | Transaction rolls back, domain change not persisted | User retries operation |
| App crashes after domain save but before outbox save | Transaction rolls back, no event published | User retries operation |
| App crashes after outbox save | Event remains unsent in outbox | Worker publishes on restart |
| Kafka unavailable | Event remains unsent in outbox | Worker retries every 5 seconds |
| Network failure after send | Event may be duplicated | Consumer handles via idempotency |

## Kafka Integration

### Spring Cloud Stream Configuration

**application.yml**:
```yaml
spring:
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
          group: wallet-hub-consumer-group
        fundsAddedEventConsumerFunction-in-0:
          destination: funds-added-topic
          group: wallet-hub-consumer-group
        fundsWithdrawnEventConsumerFunction-in-0:
          destination: funds-withdrawn-topic
          group: wallet-hub-consumer-group
        fundsTransferredEventConsumerFunction-in-0:
          destination: funds-transferred-topic
          group: wallet-hub-consumer-group

      kafka:
        binder:
          brokers: localhost:9092
          auto-create-topics: true
          replication-factor: 1
          min-partition-count: 3
```

### Binding Naming Convention

Spring Cloud Stream uses a specific naming convention:

```
{functionName}-{in|out}-{index}

Examples:
- walletCreatedEventProducer-out-0      (Producer)
- walletCreatedEventConsumerFunction-in-0  (Consumer)
```

### Topic Bindings

| Binding Name | Direction | Topic | Description |
|--------------|-----------|-------|-------------|
| `walletCreatedEventProducer-out-0` | OUT | `wallet-created-topic` | Wallet creation events |
| `fundsAddedEventProducer-out-0` | OUT | `funds-added-topic` | Funds addition events |
| `fundsWithdrawnEventProducer-out-0` | OUT | `funds-withdrawn-topic` | Funds withdrawal events |
| `fundsTransferredEventProducer-out-0` | OUT | `funds-transferred-topic` | Funds transfer events |
| `walletCreatedEventConsumerFunction-in-0` | IN | `wallet-created-topic` | Consumes wallet creation events |
| `fundsAddedEventConsumerFunction-in-0` | IN | `funds-added-topic` | Consumes funds addition events |
| `fundsWithdrawnEventConsumerFunction-in-0` | IN | `funds-withdrawn-topic` | Consumes funds withdrawal events |
| `fundsTransferredEventConsumerFunction-in-0` | IN | `funds-transferred-topic` | Consumes funds transfer events |

### Producer Configuration

**KafkaEventProducer** implements the `EventProducer` interface:

```java
@Component
public class KafkaEventProducer implements EventProducer {

    private final OutboxService outboxService;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    @Override
    public void produceWalletCreatedEvent(WalletCreatedEvent event) {
        saveEventToOutbox("walletCreatedEventProducer", event);
    }

    @Override
    public void produceFundsAddedEvent(FundsAddedEvent event) {
        saveEventToOutbox("fundsAddedEventProducer", event);
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

### Consumer Groups

All consumers use the same consumer group (`wallet-hub-consumer-group`) to ensure:

- Only one consumer instance receives each message
- Load balancing across multiple instances
- Partition assignment coordination

**Advanced Configuration**:
```yaml
spring:
  cloud:
    stream:
      bindings:
        walletCreatedEventConsumerFunction-in-0:
          destination: wallet-created-topic
          group: wallet-hub-consumer-group
          consumer:
            max-attempts: 3          # Retry attempts
            back-off-initial-interval: 1000   # 1 second
            back-off-multiplier: 2.0
            concurrency: 3           # Parallel consumer threads
      kafka:
        bindings:
          walletCreatedEventConsumerFunction-in-0:
            consumer:
              enable-dlq: true       # Dead Letter Queue
              dlq-name: wallet-created-dlq
              auto-commit-offset: false
              ack-mode: MANUAL
```

### Error Handling and DLQ

#### Dead Letter Queue (DLQ)

Failed messages are sent to a DLQ for manual inspection:

```yaml
spring:
  cloud:
    stream:
      kafka:
        bindings:
          walletCreatedEventConsumerFunction-in-0:
            consumer:
              enable-dlq: true
              dlq-name: wallet-created-topic-dlq
              dlq-partitions: 1
```

#### Consumer Error Handling

```java
@Bean
public Consumer<Message<WalletCreatedEvent>> walletCreatedEventConsumerFunction() {
    return message -> {
        try {
            var event = message.getPayload();

            // Validate event
            if (event.getCorrelationId() == null) {
                log.error("Missing correlation ID, sending to DLQ");
                throw new IllegalArgumentException("Missing correlation ID");
            }

            // Process event
            processWalletCreated(event);

        } catch (Exception e) {
            log.error("Failed to process event: {}", message, e);
            // Exception causes retry or DLQ routing based on config
            throw e;
        }
    };
}
```

#### Retry Configuration

```yaml
spring:
  cloud:
    stream:
      bindings:
        walletCreatedEventConsumerFunction-in-0:
          consumer:
            max-attempts: 3               # Total attempts (1 + 2 retries)
            back-off-initial-interval: 1000   # 1 second
            back-off-multiplier: 2.0      # Exponential backoff
            back-off-max-interval: 10000  # Max 10 seconds
```

**Retry Timing**:
- Attempt 1: Immediate
- Attempt 2: After 1 second
- Attempt 3: After 2 seconds
- After 3 failures: Send to DLQ (if enabled)

### Partition Strategy

Kafka topics should be partitioned for scalability:

```yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          min-partition-count: 3

        bindings:
          walletCreatedEventProducer-out-0:
            producer:
              partition-key-expression: headers['walletId']
              partition-count: 3
```

**Partitioning Benefits**:
- Parallel processing across consumers
- Ordering guarantees within a partition
- Scalability for high-throughput scenarios

## Event Catalog

### Complete Event Inventory

Wallet Hub defines **30+ domain events** across 7 aggregate roots:

#### Wallet Events (10 events)

| Event | Description | Key Fields |
|-------|-------------|------------|
| `WalletCreatedEvent` | Wallet successfully created | `walletId`, `correlationId` |
| `WalletUpdatedEvent` | Wallet metadata updated | `walletId`, `correlationId` |
| `WalletStatusChangedEvent` | Wallet status changed | `walletId`, `status`, `correlationId` |
| `WalletDeletedEvent` | Wallet soft-deleted | `walletId`, `correlationId` |
| `WalletRecoveryInitiatedEvent` | Wallet recovery started | `walletId`, `correlationId` |
| `FundsAddedEvent` | Funds added to wallet | `walletId`, `amount`, `correlationId` |
| `FundsWithdrawnEvent` | Funds withdrawn from wallet | `walletId`, `amount`, `correlationId` |
| `FundsTransferredEvent` | Funds transferred between wallets | `fromWalletId`, `toWalletId`, `amount`, `correlationId` |
| `AddressAddedToWalletEvent` | Address linked to wallet | `walletId`, `addressId`, `correlationId` |
| `AddressRemovedFromWalletEvent` | Address unlinked from wallet | `walletId`, `addressId`, `correlationId` |
| `TokenAddedToWalletEvent` | Token added to wallet | `walletId`, `tokenId`, `correlationId` |
| `TokenRemovedFromWalletEvent` | Token removed from wallet | `walletId`, `tokenId`, `correlationId` |

#### User Events (4 events)

| Event | Description | Key Fields |
|-------|-------------|------------|
| `UserCreatedEvent` | User account created | `userId`, `name`, `email`, `correlationId` |
| `UserProfileUpdatedEvent` | User profile modified | `userId`, `correlationId` |
| `UserStatusChangedEvent` | User status changed | `userId`, `status`, `correlationId` |
| `UserAuthenticatedEvent` | User logged in | `userId`, `correlationId` |

#### Address Events (2 events)

| Event | Description | Key Fields |
|-------|-------------|------------|
| `AddressCreatedEvent` | Blockchain address created | `addressId`, `walletId`, `networkId`, `accountAddress`, `correlationId` |
| `AddressStatusChangedEvent` | Address status changed | `addressId`, `status`, `correlationId` |

#### Token Events (2 events)

| Event | Description | Key Fields |
|-------|-------------|------------|
| `TokenCreatedEvent` | ERC20 token registered | `tokenId`, `networkId`, `contractAddress`, `symbol`, `tokenType`, `correlationId` |
| `TokenBalanceChangedEvent` | Token balance updated | `tokenId`, `addressId`, `balance`, `correlationId` |

#### Transaction Events (3 events)

| Event | Description | Key Fields |
|-------|-------------|------------|
| `TransactionCreatedEvent` | Blockchain transaction initiated | `transactionId`, `networkId`, `transactionHash`, `fromAddress`, `toAddress`, `correlationId` |
| `TransactionConfirmedEvent` | Transaction confirmed on-chain | `transactionId`, `blockNumber`, `correlationId` |
| `TransactionStatusChangedEvent` | Transaction status updated | `transactionId`, `status`, `correlationId` |

#### Network Events (3 events)

| Event | Description | Key Fields |
|-------|-------------|------------|
| `NetworkCreatedEvent` | Blockchain network added | `networkId`, `chainId`, `name`, `correlationId` |
| `NetworkAddedEvent` | Network enabled | `networkId`, `correlationId` |
| `NetworkStatusChangedEvent` | Network status changed | `networkId`, `status`, `correlationId` |

#### Vault Events (3 events)

| Event | Description | Key Fields |
|-------|-------------|------------|
| `VaultCreatedEvent` | Secure vault created | `vaultId`, `correlationId` |
| `KeyPairGeneratedEvent` | Cryptographic key pair generated | `vaultId`, `publicKey`, `correlationId` |
| `VaultStatusChangedEvent` | Vault status changed | `vaultId`, `status`, `correlationId` |

#### Store Events (3 events)

| Event | Description | Key Fields |
|-------|-------------|------------|
| `StoreCreatedEvent` | Generic store created | `storeId`, `correlationId` |
| `AddressAddedToStoreEvent` | Address added to store | `storeId`, `addressId`, `correlationId` |
| `StoreStatusChangedEvent` | Store status changed | `storeId`, `status`, `correlationId` |

#### Contract Events (4 events)

| Event | Description | Key Fields |
|-------|-------------|------------|
| `ContractDeployedEvent` | Smart contract deployed | `contractId`, `contractAddress`, `networkId`, `correlationId` |
| `ContractInteractionEvent` | Contract method invoked | `contractId`, `method`, `parameters`, `correlationId` |
| `ContractOwnerAddedEvent` | Contract owner added | `contractId`, `ownerAddress`, `correlationId` |
| `ContractOwnerRemovedEvent` | Contract owner removed | `contractId`, `ownerAddress`, `correlationId` |

### Event Schemas

#### WalletCreatedEvent

```java
@Getter
public class WalletCreatedEvent extends DomainEvent {
    private final UUID walletId;

    public WalletCreatedEvent(UUID walletId, UUID correlationId) {
        super(correlationId);
        this.walletId = walletId;
    }
}
```

**JSON Schema**:
```json
{
  "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "occurredOn": "2025-12-10T10:30:00Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "walletId": "123e4567-e89b-12d3-a456-426614174000"
}
```

#### FundsAddedEvent

```java
@Builder
public record FundsAddedEvent(
    UUID walletId,
    BigDecimal amount,
    String correlationId
) {}
```

**JSON Schema**:
```json
{
  "walletId": "123e4567-e89b-12d3-a456-426614174000",
  "amount": 100.50,
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### FundsTransferredEvent

```java
@Builder
public record FundsTransferredEvent(
    UUID fromWalletId,
    UUID toWalletId,
    BigDecimal amount,
    String correlationId
) {}
```

**JSON Schema**:
```json
{
  "fromWalletId": "123e4567-e89b-12d3-a456-426614174000",
  "toWalletId": "987fcdeb-51d2-43a1-b567-123456789abc",
  "amount": 50.25,
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Publishing Patterns

#### Pattern 1: Domain Event + Producer

```java
// 1. Use case creates event
var event = new WalletCreatedEvent(walletId, correlationId);

// 2. Subscribe to domain events
DomainEventPublisher.subscribe(domainEvent -> {
    if (domainEvent instanceof WalletCreatedEvent walletEvent) {
        eventProducer.produceWalletCreatedEvent(walletEvent);
    }
});

// 3. Publish to subscribers
DomainEventPublisher.publish(event);

// 4. Clean up
DomainEventPublisher.reset();
```

#### Pattern 2: Direct Producer Call

```java
// Directly call producer (use case layer)
var event = FundsAddedEvent.builder()
    .walletId(walletId)
    .amount(amount)
    .correlationId(correlationId.toString())
    .build();

eventProducer.produceFundsAddedEvent(event);
```

### Consumption Patterns

#### Pattern 1: State Machine Integration

```java
@Bean
public Consumer<Message<WalletCreatedEvent>> walletCreatedEventConsumerFunction() {
    return message -> {
        var event = message.getPayload();

        // Send to state machine for saga orchestration
        var stateMachineMessage = MessageBuilder
            .withPayload(SagaEvents.WALLET_CREATED)
            .setHeader("correlationId", event.getCorrelationId())
            .build();

        stateMachine.sendEvent(Mono.just(stateMachineMessage)).subscribe();
    };
}
```

#### Pattern 2: Direct Processing

```java
@Bean
public Consumer<Message<TransactionConfirmedEvent>> transactionConfirmedEventConsumerFunction() {
    return message -> {
        var event = message.getPayload();

        // Update read model / projection
        transactionReadService.updateTransactionStatus(
            event.getTransactionId(),
            TransactionStatus.CONFIRMED
        );

        // Send notification
        notificationService.sendConfirmation(event);
    };
}
```

#### Pattern 3: Reactive Processing

```java
@Bean
public Function<Flux<Message<TokenBalanceChangedEvent>>, Mono<Void>> tokenBalanceProcessor() {
    return flux -> flux
        .map(Message::getPayload)
        .buffer(Duration.ofSeconds(5))  // Batch events
        .flatMap(events -> portfolioService.recalculateBalances(events))
        .then();
}
```

## Best Practices

### Event Versioning

#### Versioning Strategy

Use **additive changes** to maintain backward compatibility:

```java
// Version 1
public record WalletCreatedEvent(
    UUID walletId,
    String correlationId
) {}

// Version 2 (backward compatible)
public record WalletCreatedEvent(
    UUID walletId,
    String correlationId,
    String walletType  // New optional field
) {
    // Constructor with defaults for backward compatibility
    public WalletCreatedEvent(UUID walletId, String correlationId) {
        this(walletId, correlationId, "STANDARD");
    }
}
```

#### Breaking Changes

For breaking changes, create a new event type:

```java
// Old event (deprecated)
@Deprecated
public record WalletCreatedEvent(UUID walletId, String correlationId) {}

// New event (version 2)
public record WalletCreatedEventV2(
    UUID walletId,
    String correlationId,
    WalletType type,
    Currency defaultCurrency
) {}
```

**Migration Strategy**:
1. Deploy consumers that handle both V1 and V2
2. Deploy producers that emit V2
3. Wait for all V1 events to be processed
4. Remove V1 consumer logic

### Schema Evolution

#### Safe Changes

- Adding optional fields
- Adding new event types
- Adding metadata/headers
- Deprecating fields (mark as `@Deprecated`)

#### Unsafe Changes

- Removing fields
- Renaming fields
- Changing field types
- Changing event semantics

#### Example: Safe Evolution

```java
// Before
public class UserCreatedEvent extends DomainEvent {
    private final UUID userId;
    private final String name;
    private final String email;
}

// After (safe)
public class UserCreatedEvent extends DomainEvent {
    private final UUID userId;
    private final String name;
    private final String email;
    private final String phoneNumber;  // New optional field
    private final String country;      // New optional field

    // Builder supports optional fields
    @Builder
    public UserCreatedEvent(
        UUID userId,
        String name,
        String email,
        String phoneNumber,
        String country,
        UUID correlationId
    ) {
        super(correlationId);
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;  // May be null
        this.country = country;          // May be null
    }
}
```

### Event Replay

#### Use Cases

- Rebuilding read models
- Recovering from data corruption
- Migrating to new event schemas
- Testing and debugging

#### Replay Implementation

```java
@Service
public class EventReplayService {

    private final OutboxRepository outboxRepository;
    private final StreamBridge streamBridge;

    /**
     * Replays events within a time range
     */
    @Transactional(readOnly = true)
    public void replayEvents(Instant from, Instant to, String eventType) {
        log.info("Replaying events from {} to {} for type {}", from, to, eventType);

        List<OutboxEvent> events = outboxRepository
            .findByEventTypeAndCreatedAtBetween(eventType, from, to);

        events.forEach(event -> {
            String binding = EventBindings.bindingForEventType(event.getEventType())
                .orElseThrow();

            streamBridge.send(binding, event.getPayload());
            log.debug("Replayed event id={}", event.getId());
        });

        log.info("Replayed {} events", events.size());
    }

    /**
     * Replays all events for specific correlation ID
     */
    public void replayByCorrelationId(String correlationId) {
        List<OutboxEvent> events = outboxRepository
            .findByCorrelationId(correlationId);

        events.stream()
            .sorted(Comparator.comparing(OutboxEvent::getCreatedAt))
            .forEach(event -> {
                String binding = EventBindings.bindingForEventType(event.getEventType())
                    .orElseThrow();
                streamBridge.send(binding, event.getPayload());
            });
    }
}
```

#### Replay Safeguards

```java
// Prevent accidental replay in production
@Service
public class ReplayGuard {

    @Value("${event.replay.enabled:false}")
    private boolean replayEnabled;

    public void checkReplayAllowed() {
        if (!replayEnabled) {
            throw new IllegalStateException(
                "Event replay is disabled. Set event.replay.enabled=true"
            );
        }
    }
}
```

### Monitoring and Observability

#### Metrics

```java
@Component
public class OutboxWorker {

    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 5000)
    public void processOutbox() {
        for (OutboxEvent event : outboxService.getUnsentEvents()) {
            Timer.Sample sample = Timer.start(meterRegistry);

            try {
                boolean success = streamBridge.send(binding, event.getPayload());

                if (success) {
                    meterRegistry.counter("outbox.sent",
                        "binding", binding).increment();
                    sample.stop(meterRegistry.timer("outbox.send.duration",
                        "binding", binding, "status", "success"));
                } else {
                    meterRegistry.counter("outbox.send.failed",
                        "binding", binding).increment();
                    sample.stop(meterRegistry.timer("outbox.send.duration",
                        "binding", binding, "status", "failed"));
                }
            } catch (Exception e) {
                meterRegistry.counter("outbox.error",
                    "binding", binding).increment();
                sample.stop(meterRegistry.timer("outbox.send.duration",
                    "binding", binding, "status", "error"));
            }
        }

        // Track outbox size
        long unsentCount = outboxService.getUnsentEvents().size();
        meterRegistry.gauge("outbox.unsent.count", unsentCount);
    }
}
```

#### Logging

```java
@Slf4j
@Bean
public Consumer<Message<WalletCreatedEvent>> walletCreatedEventConsumerFunction() {
    return message -> {
        var event = message.getPayload();

        // Structured logging with correlation ID
        MDC.put("correlationId", event.getCorrelationId().toString());
        MDC.put("eventId", event.getEventId().toString());

        try {
            log.info("Processing WalletCreatedEvent: walletId={}",
                event.getWalletId());

            processWalletCreated(event);

            log.info("Successfully processed WalletCreatedEvent");
        } catch (Exception e) {
            log.error("Failed to process WalletCreatedEvent", e);
            throw e;
        } finally {
            MDC.clear();
        }
    };
}
```

## Testing

### Testing with Test Binder

Spring Cloud Stream provides a **test binder** for integration testing without a real Kafka broker.

**Test Configuration**:
```yaml
# src/test/resources/application-test.yml
spring:
  cloud:
    stream:
      defaultBinder: test  # Use test binder instead of Kafka
```

### Producer Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.stream.defaultBinder=test"
})
class KafkaEventProducerTest {

    @Autowired
    private KafkaEventProducer eventProducer;

    @Autowired
    private OutputDestination outputDestination;

    @Test
    void shouldPublishWalletCreatedEvent() {
        // Given
        UUID walletId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        var event = new WalletCreatedEvent(walletId, correlationId);

        // When
        eventProducer.produceWalletCreatedEvent(event);

        // Wait for outbox worker to process
        await().atMost(10, SECONDS).until(() ->
            outputDestination.receive(1000, "wallet-created-topic") != null
        );

        // Then
        Message<byte[]> message = outputDestination.receive(0, "wallet-created-topic");
        assertThat(message).isNotNull();

        String payload = new String(message.getPayload());
        assertThat(payload).contains(walletId.toString());
        assertThat(payload).contains(correlationId.toString());
    }
}
```

### Consumer Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.stream.defaultBinder=test"
})
class WalletCreatedEventConsumerTest {

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private StateMachine<SagaStates, SagaEvents> stateMachine;

    @Test
    void shouldConsumeWalletCreatedEvent() throws Exception {
        // Given
        UUID walletId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        var event = new WalletCreatedEvent(walletId, correlationId);

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        byte[] payload = mapper.writeValueAsBytes(event);

        // When
        inputDestination.send(
            MessageBuilder.withPayload(payload).build(),
            "wallet-created-topic"
        );

        // Then
        await().atMost(5, SECONDS).until(() -> {
            // Verify state machine received event
            return stateMachine.getState().getId() == SagaStates.WALLET_CREATED;
        });
    }
}
```

### Outbox Testing

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutboxServiceTest {

    @Autowired
    private OutboxRepository outboxRepository;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        outboxService = new OutboxService(outboxRepository);
    }

    @Test
    void shouldSaveEventToOutbox() {
        // Given
        String eventType = "walletCreatedEventProducer";
        String payload = "{\"walletId\":\"123\"}";
        String correlationId = UUID.randomUUID().toString();

        // When
        outboxService.saveOutboxEvent(eventType, payload, correlationId);

        // Then
        List<OutboxEvent> unsent = outboxService.getUnsentEvents();
        assertThat(unsent).hasSize(1);
        assertThat(unsent.get(0).getEventType()).isEqualTo(eventType);
        assertThat(unsent.get(0).getPayload()).isEqualTo(payload);
        assertThat(unsent.get(0).getCorrelationId()).isEqualTo(correlationId);
        assertThat(unsent.get(0).isSent()).isFalse();
    }

    @Test
    void shouldMarkEventAsSent() {
        // Given
        outboxService.saveOutboxEvent("test", "{}", null);
        OutboxEvent event = outboxService.getUnsentEvents().get(0);

        // When
        outboxService.markEventAsSent(event);

        // Then
        assertThat(outboxService.getUnsentEvents()).isEmpty();
    }
}
```

### Integration Testing

```java
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${kafka.bootstrapServers}"
})
class EventDrivenIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaEventProducer eventProducer;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void endToEndEventFlow() {
        // Given
        UUID walletId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        var event = new WalletCreatedEvent(walletId, correlationId);

        // When
        eventProducer.produceWalletCreatedEvent(event);

        // Then - verify message in Kafka
        ConsumerRecord<String, String> record = KafkaTestUtils
            .getSingleRecord(consumer, "wallet-created-topic");

        assertThat(record.value()).contains(walletId.toString());
    }
}
```

### Idempotency Testing

```java
@SpringBootTest
class IdempotencyTest {

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void shouldHandleDuplicateEvents() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        var event = new WalletCreatedEvent(walletId, UUID.randomUUID());

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        byte[] payload = mapper.writeValueAsBytes(event);

        // When - send same event twice
        inputDestination.send(
            MessageBuilder.withPayload(payload).build(),
            "wallet-created-topic"
        );
        inputDestination.send(
            MessageBuilder.withPayload(payload).build(),
            "wallet-created-topic"
        );

        // Then - only one wallet created
        await().atMost(5, SECONDS).until(() ->
            walletRepository.findById(walletId).isPresent()
        );

        long count = walletRepository.count();
        assertThat(count).isEqualTo(1);
    }
}
```

---

## Summary

Wallet Hub's event-driven architecture provides:

1. **Reliability**: Transactional outbox pattern ensures guaranteed delivery
2. **Scalability**: Kafka partitioning enables horizontal scaling
3. **Traceability**: Correlation IDs and CloudEvents enable end-to-end tracking
4. **Flexibility**: Decoupled producers and consumers evolve independently
5. **Resilience**: Retry mechanisms and DLQ handle failures gracefully

**Key Takeaways**:
- Use the outbox pattern for transactional consistency
- Implement idempotency in all consumers
- Leverage CloudEvents for interoperability
- Monitor outbox size and send metrics
- Test with the test binder for fast feedback
- Version events carefully to maintain backward compatibility

For questions or contributions, refer to the main [README](/Users/italo/Projects/lab/wallet/bloco-wallet-java/README.md) and [CLAUDE.md](/Users/italo/Projects/lab/wallet/bloco-wallet-java/CLAUDE.md).
