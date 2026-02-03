# Common Foundation - Quick Reference

## Core Classes

### Entity
**Purpose**: Base class for all domain objects with identity management
**Key Methods**:
- `getId(): UUID` - Get unique identifier
- `equals(Object): boolean` - Type-aware equality comparison
- `hashCode(): int` - Consistent hash based on ID

### AggregateRoot
**Purpose**: Extends Entity with domain event handling capabilities
**Key Methods**:
- `registerEvent(DomainEvent): void` - Record domain event
- `getDomainEvents(): List<DomainEvent>` - Get collected events
- `clearEvents(): void` - Clear event collection

## Quick Start Examples

### Creating an Entity
```java
public class MyEntity extends AggregateRoot {
    // Factory method pattern
    public static MyEntity create(UUID id, String name) {
        MyEntity entity = new MyEntity(id, name);
        entity.registerEvent(new EntityCreatedEvent(id));
        return entity;
    }
    
    public MyEntity(UUID id, String name) {
        super(id);  // Must call parent constructor
        // Initialize fields
    }
}
```

### Registering Domain Events
```java
public void updateStatus(Status newStatus, String reason) {
    Status oldStatus = this.status;
    this.status = newStatus;
    this.updatedAt = Instant.now();
    
    // Register domain event
    registerEvent(new StatusChangedEvent(
        getId(), oldStatus, newStatus, reason
    ));
}
```

## Design Patterns

### 1. Factory Method Pattern
Always use static `create()` methods for entity instantiation.

### 2. Event Registration Pattern
Register events immediately after state changes.

### 3. Business Rule Pattern
Validate rules before state changes, register events after.

## Best Practices Checklist

### ✅ Do
- Extend `AggregateRoot` for entities with business logic
- Use factory methods (`create()`) for instantiation
- Register domain events for state changes
- Reference other entities by ID only
- Use UUIDs for entity identifiers

### ❌ Don't
- Override `equals()` or `hashCode()` in subclasses
- Change entity IDs after creation
- Store object references to other entities
- Skip event registration for state changes
- Create entities without UUIDs

## Common Use Cases

### 1. Simple Value Object
```java
public class SimpleValue extends Entity {
    private final String value;
    
    public SimpleValue(UUID id, String value) {
        super(id);
        this.value = value;
    }
}
```

### 2. Aggregate with Business Logic
```java
public class BusinessEntity extends AggregateRoot {
    private Status status;
    
    public void performOperation() {
        validateOperationAllowed();  // Business rules
        updateState();               // State change
        registerEvent(new OperationPerformedEvent(getId()));
    }
}
```

## Integration Points

### With Repositories
```java
public interface MyRepository {
    MyEntity save(MyEntity entity);  // Works with AggregateRoot
    Optional<MyEntity> findById(UUID id);
}
```

### With Domain Events
```java
// Events are automatically collected
List<DomainEvent> events = aggregate.getDomainEvents();
DomainEventPublisher.publish(events);
aggregate.clearEvents();
```

## Testing Tips

### Unit Testing
```java
@Test
void testEntityEquality() {
    UUID id = UUID.randomUUID();
    Entity entity1 = new MyEntity(id, "test");
    Entity entity2 = new MyEntity(id, "test");
    
    assertEquals(entity1, entity2);  // Same ID = equal
    assertEquals(entity1.hashCode(), entity2.hashCode());
}

@Test
void testEventRegistration() {
    MyEntity entity = MyEntity.create(UUID.randomUUID(), "test");
    entity.updateStatus(Status.ACTIVE, "activated");
    
    List<DomainEvent> events = entity.getDomainEvents();
    assertEquals(2, events.size());  // Created + StatusChanged
}
```

## Performance Notes

- Entity base classes are lightweight
- Event lists grow with state changes (clear after publishing)
- UUIDs work well with distributed systems
- Consider batch event processing for high-volume systems

---

*See full documentation: [common_foundation.md](common_foundation.md)*