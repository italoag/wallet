# Outbox System Quick Reference

## Core Components

### 1. OutboxEvent Entity
**Purpose:** Persistent record of events needing delivery
**Location:** `infrastructure_data/outbox_system`
**Key Properties:**
- `id`: Auto-generated primary key
- `eventType`: Event category (e.g., "WalletCreatedEvent")
- `payload`: JSON-serialized event data
- `correlationId`: Optional tracing identifier
- `createdAt`: Event creation timestamp
- `sent`: Delivery status flag

### 2. OutboxService
**Purpose:** Business logic for event management
**Location:** `infrastructure_data/outbox_system`
**Key Methods:**
- `saveOutboxEvent()`: Persist new event (transactional)
- `markEventAsSent()`: Update event status (transactional)
- `getUnsentEvents()`: Retrieve pending events

### 3. OutboxWorker
**Purpose:** Scheduled event processor
**Location:** `infrastructure_data/outbox_system`
**Key Features:**
- Runs every 5 seconds (`@Scheduled(fixedRate = 5000)`)
- Fetches unsent events from database
- Maps event types to message bindings
- Sends events via `StreamBridge`
- Updates status upon successful delivery

### 4. OutboxRepository
**Purpose:** Data access for outbox events
**Location:** `infrastructure_data/outbox_system`
**Key Method:**
- `findBySentFalse()`: Custom query for unsent events

### 5. OutboxEventPublisher
**Purpose:** Bridge between domain events and outbox
**Location:** `infrastructure_events`
**Key Features:**
- Implements `DomainEventPublisher` interface
- Serializes events to JSON using Jackson
- Persists events within same transaction
- Uses event class name as `eventType`

## Key Dependencies

### Internal Dependencies
1. **EventBindings** (`infrastructure_events`)
   - Maps event types to Spring Cloud Stream bindings
   - Centralized configuration to avoid string coupling

2. **StreamBridge** (Spring Cloud Stream)
   - Sends messages to configured bindings
   - Abstracts underlying messaging system (Kafka)

3. **DomainEventPublisher Interface** (`domain_gateways`)
   - Contract for event publishing
   - Implemented by `OutboxEventPublisher`

### External Dependencies
1. **Database** (PostgreSQL/MySQL)
   - `outbox` table for event persistence
   - Transaction support for consistency

2. **Message Broker** (Kafka)
   - Event distribution to consumers
   - At-least-once delivery guarantees

## Configuration

### Database Schema
```sql
CREATE TABLE outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    correlation_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_outbox_created_at ON outbox(created_at);
CREATE INDEX idx_outbox_sent ON outbox(sent);
```

### Application Properties
```yaml
spring:
  cloud:
    stream:
      bindings:
        walletCreatedEventProducer-out-0:
          destination: wallet-created-events
        fundsAddedEventProducer-out-0:
          destination: funds-added-events
      
      kafka:
        binder:
          brokers: ${KAFKA_BROKERS:localhost:9092}
```

### Event Bindings Mapping
```java
// EventBindings.java
private static final Map<String, String> EVENT_TYPE_TO_BINDING = Map.of(
    "walletCreatedEventProducer", "walletCreatedEventProducer-out-0",
    "fundsAddedEventProducer", "fundsAddedEventProducer-out-0",
    "fundsWithdrawnEventProducer", "fundsWithdrawnEventProducer-out-0",
    "fundsTransferredEventProducer", "fundsTransferredEventProducer-out-0"
);
```

## Usage Patterns

### 1. Publishing Events
```java
// In a use case or domain service
@Transactional
public Wallet createWallet(String name, UUID userId) {
    Wallet wallet = walletRepository.save(new Wallet(name, userId));
    
    // Publish domain event
    domainEventPublisher.publish(
        new WalletCreatedEvent(wallet.getId(), correlationId)
    );
    
    return wallet;
}
```

### 2. Custom Event Types
To add support for new event types:

1. **Define domain event** in `domain_events` module
2. **Add binding mapping** in `EventBindings.java`:
   ```java
   private static final Map<String, String> EVENT_TYPE_TO_BINDING = Map.of(
       // ... existing mappings
       "newEventType", "newEventProducer-out-0"
   );
   ```
3. **Configure binding** in `application.yml`:
   ```yaml
   spring:
     cloud:
       stream:
         bindings:
           newEventProducer-out-0:
             destination: new-event-topic
   ```
4. **Create consumer** (optional) in `infrastructure_events/consumer`

### 3. Monitoring Events
```java
// Check outbox status
List<OutboxEvent> unsentEvents = outboxService.getUnsentEvents();
int queueSize = unsentEvents.size();
Instant oldestEvent = unsentEvents.stream()
    .map(OutboxEvent::getCreatedAt)
    .min(Instant::compareTo)
    .orElse(Instant.now());
```

## Error Handling

### Common Issues and Solutions

1. **Event Not Being Sent**
   - Check `EventBindings` mapping for event type
   - Verify Kafka broker connectivity
   - Check outbox worker logs for errors

2. **High Outbox Queue**
   - Monitor `outbox.queue.size` metric
   - Check outbox worker is running (scheduled task)
   - Verify database connectivity

3. **Serialization Errors**
   - Ensure domain events are Jackson-serializable
   - Check for circular references in event objects
   - Verify Jackson configuration

4. **Duplicate Event Processing**
   - Ensure consumers are idempotent
   - Check correlation ID usage
   - Monitor for duplicate event IDs

## Metrics and Monitoring

### Key Metrics to Monitor
- `outbox.queue.size`: Number of unsent events
- `outbox.queue.age`: Age of oldest unsent event
- `events.published.total`: Total events published
- `events.published.by_type`: Events by type
- `outbox.processing.time`: Worker processing time

### Health Checks
```bash
# Check outbox table size
SELECT COUNT(*) FROM outbox WHERE sent = false;

# Check oldest unsent event
SELECT MIN(created_at) FROM outbox WHERE sent = false;

# Check event distribution
SELECT event_type, COUNT(*) 
FROM outbox 
WHERE sent = false 
GROUP BY event_type;
```

## Testing

### Unit Tests
```java
@Test
void testSaveOutboxEvent() {
    outboxService.saveOutboxEvent("TestEvent", "{}", "corr-123");
    
    List<OutboxEvent> unsent = outboxService.getUnsentEvents();
    assertEquals(1, unsent.size());
    assertEquals("TestEvent", unsent.get(0).getEventType());
}

@Test
void testMarkEventAsSent() {
    OutboxEvent event = new OutboxEvent();
    // ... setup
    
    outboxService.markEventAsSent(event);
    assertTrue(event.isSent());
}
```

### Integration Tests
```java
@Test
@Transactional
void testCompleteEventFlow() {
    // 1. Publish event
    domainEventPublisher.publish(new WalletCreatedEvent(walletId, correlationId));
    
    // 2. Verify event in outbox
    List<OutboxEvent> unsent = outboxService.getUnsentEvents();
    assertEquals(1, unsent.size());
    
    // 3. Trigger outbox worker
    outboxWorker.processOutbox();
    
    // 4. Verify event marked as sent
    unsent = outboxService.getUnsentEvents();
    assertEquals(0, unsent.size());
}
```

## Performance Tuning

### Configuration Options
1. **Polling Interval**: Adjust `@Scheduled(fixedRate)` in `OutboxWorker`
2. **Batch Size**: Consider implementing batch fetching for large volumes
3. **Connection Pool**: Configure database connection pool size
4. **Kafka Producer**: Tune Kafka producer batch size and linger time

### Optimization Tips
1. **Index Optimization**: Ensure proper indexes on `outbox` table
2. **Batch Processing**: Process events in batches for efficiency
3. **Connection Reuse**: Reuse database connections within batch
4. **Payload Size**: Keep event payloads small and efficient

## Related Documentation

### Comprehensive Guides
- [outbox_system.md](outbox_system.md): Complete module documentation
- [event_driven_architecture_summary.md](event_driven_architecture_summary.md): Overall architecture
- [infrastructure_events.md](infrastructure_events.md): Event publishing/consumption
- [domain_events.md](domain_events.md): Domain event definitions

### Reference Documentation
- [repository_layer.md](repository_layer.md): Data access patterns
- [infrastructure_data.md](infrastructure_data.md): Parent module overview
- [infrastructure_tracing.md](infrastructure_tracing.md): Distributed tracing

## Support and Troubleshooting

### Common Questions

**Q: How often are events processed?**
A: Every 5 seconds by default (configurable in `OutboxWorker`)

**Q: What happens if Kafka is down?**
A: Events remain in outbox table and retry on next cycle

**Q: How are duplicate events prevented?**
A: Consumers should be idempotent; events have unique IDs

**Q: Can I change the messaging system?**
A: Yes, `StreamBridge` abstracts the underlying system

**Q: How do I add a new event type?**
A: See "Custom Event Types" section above

### Getting Help
1. Check application logs for error messages
2. Monitor metrics for abnormal patterns
3. Review database outbox table directly
4. Consult comprehensive documentation links above