# Saga Pattern Implementation

## Overview

Wallet Hub implements the **Saga Pattern** using Spring State Machine to coordinate distributed transactions across multiple services and domains. The saga ensures that a sequence of local transactions either all succeed or compensating transactions are triggered to maintain consistency.

## What is the Saga Pattern?

The Saga Pattern is a design pattern for managing distributed transactions:

- **Local Transactions**: Each service performs its own transaction
- **Event-Driven**: Transactions trigger events to coordinate next steps
- **Compensation**: If a step fails, compensating transactions undo previous work
- **Eventual Consistency**: System reaches consistent state over time

### Why Saga Pattern?

Traditional ACID transactions don't work well in distributed systems:

```
❌ Distributed ACID Transaction:
   Service A → BEGIN TRANSACTION
   Service B → JOIN TRANSACTION  (Complex, locks resources)
   Service C → JOIN TRANSACTION
   All or nothing → COMMIT

✓ Saga Pattern:
   Service A → Complete → Emit Event
   Service B → Consume Event → Complete → Emit Event
   Service C → Consume Event → Complete
   If fail → Trigger compensation
```

## Spring State Machine Architecture

### State Machine Diagram

```
┌─────────┐
│ INITIAL │
└────┬────┘
     │ WALLET_CREATED event
     ▼
┌──────────────┐
│WALLET_CREATED│
└──────┬───────┘
       │ FUNDS_ADDED event
       ▼
┌─────────────┐
│ FUNDS_ADDED │
└──────┬──────┘
       │ FUNDS_WITHDRAWN event
       ▼
┌────────────────┐
│FUNDS_WITHDRAWN │
└──────┬─────────┘
       │ FUNDS_TRANSFERRED event
       ▼
┌──────────────────┐
│FUNDS_TRANSFERRED │
└──────┬───────────┘
       │ SAGA_COMPLETED event
       ▼
┌──────────┐
│COMPLETED │
└──────────┘

       │ SAGA_FAILED event (from any state)
       ▼
┌────────┐
│ FAILED │
└────────┘
```

### State Machine Configuration

**Location**: `infra/provider/data/config/SagaStateMachineConfig.java`

```java
@Configuration
@EnableStateMachineFactory
public class SagaStateMachineConfig extends StateMachineConfigurerAdapter<SagaState, SagaEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<SagaState, SagaEvent> states) throws Exception {
        states
            .withStates()
                .initial(SagaState.INITIAL)
                .state(SagaState.WALLET_CREATED)
                .state(SagaState.FUNDS_ADDED)
                .state(SagaState.FUNDS_WITHDRAWN)
                .state(SagaState.FUNDS_TRANSFERRED)
                .end(SagaState.COMPLETED)
                .end(SagaState.FAILED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<SagaState, SagaEvent> transitions) throws Exception {
        transitions
            .withExternal()
                .source(SagaState.INITIAL)
                .target(SagaState.WALLET_CREATED)
                .event(SagaEvent.WALLET_CREATED)
                .and()
            .withExternal()
                .source(SagaState.WALLET_CREATED)
                .target(SagaState.FUNDS_ADDED)
                .event(SagaEvent.FUNDS_ADDED)
                .and()
            .withExternal()
                .source(SagaState.FUNDS_ADDED)
                .target(SagaState.FUNDS_WITHDRAWN)
                .event(SagaEvent.FUNDS_WITHDRAWN)
                .and()
            .withExternal()
                .source(SagaState.FUNDS_WITHDRAWN)
                .target(SagaState.FUNDS_TRANSFERRED)
                .event(SagaEvent.FUNDS_TRANSFERRED)
                .and()
            .withExternal()
                .source(SagaState.FUNDS_TRANSFERRED)
                .target(SagaState.COMPLETED)
                .event(SagaEvent.SAGA_COMPLETED)
                .and()
            .withExternal()
                .source(SagaState.INITIAL)
                .target(SagaState.FAILED)
                .event(SagaEvent.SAGA_FAILED)
                .and()
            .withExternal()
                .source(SagaState.WALLET_CREATED)
                .target(SagaState.FAILED)
                .event(SagaEvent.SAGA_FAILED)
                .and()
            .withExternal()
                .source(SagaState.FUNDS_ADDED)
                .target(SagaState.FAILED)
                .event(SagaEvent.SAGA_FAILED)
                .and()
            .withExternal()
                .source(SagaState.FUNDS_WITHDRAWN)
                .target(SagaState.FAILED)
                .event(SagaEvent.SAGA_FAILED);
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<SagaState, SagaEvent> config) throws Exception {
        config
            .withConfiguration()
                .autoStartup(true)
            .and()
            .withPersistence()
                .runtimePersister(stateMachinePersist());
    }

    @Bean
    public StateMachineRuntimePersister<SagaState, SagaEvent, String> stateMachinePersist() {
        return new JpaPersistingStateMachineInterceptor<>();
    }
}
```

## State and Event Enums

### SagaState Enum

**Location**: `infra/provider/data/config/SagaState.java`

```java
public enum SagaState {
    INITIAL,             // Starting state
    WALLET_CREATED,      // Wallet has been created
    FUNDS_ADDED,         // Funds have been added
    FUNDS_WITHDRAWN,     // Funds have been withdrawn
    FUNDS_TRANSFERRED,   // Funds have been transferred
    COMPLETED,           // Saga completed successfully
    FAILED               // Saga failed, compensation needed
}
```

### SagaEvent Enum

**Location**: `infra/provider/data/config/SagaEvent.java`

```java
public enum SagaEvent {
    WALLET_CREATED,      // Wallet created successfully
    FUNDS_ADDED,         // Funds added successfully
    FUNDS_WITHDRAWN,     // Funds withdrawn successfully
    FUNDS_TRANSFERRED,   // Funds transferred successfully
    SAGA_COMPLETED,      // All steps completed
    SAGA_FAILED          // A step failed
}
```

## JPA Persistence

Spring State Machine state is persisted to database for reliability.

### State Machine Tables

```sql
-- State machine instance
CREATE TABLE state_machine (
    machine_id VARCHAR(255) PRIMARY KEY,
    state VARCHAR(255) NOT NULL,
    state_machine_context BLOB
);

-- State machine transitions (audit)
CREATE TABLE state_machine_transition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    machine_id VARCHAR(255) NOT NULL,
    source_state VARCHAR(255),
    target_state VARCHAR(255),
    event VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### JPA Configuration

**Location**: `WalletHubApplication.java`

```java
@SpringBootApplication
@EntityScan(basePackages = {
    "dev.bloco.wallet.hub.infra.provider.data.entity",
    "dev.bloco.wallet.hub.infra.provider.data",
    "org.springframework.statemachine.data.jpa"  // State machine entities
})
public class WalletHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(WalletHubApplication.class, args);
    }
}
```

## Saga Orchestration

### Example: Wallet Creation Saga

```
1. User creates wallet
   → CreateWalletUseCase executes
   → Wallet persisted
   → WalletCreatedEvent published
   → State: INITIAL → WALLET_CREATED

2. WalletCreatedEventConsumer receives event
   → Initializes default settings
   → Triggers next step
   → State: WALLET_CREATED → ...

3. Each step triggers the next via events
   → State machine tracks progress
   → If any step fails → SAGA_FAILED → Compensation
```

### Saga Coordinator Service

**Location**: `infra/provider/data/saga/SagaCoordinator.java`

```java
@Service
@RequiredArgsConstructor
public class SagaCoordinator {
    private final StateMachineFactory<SagaState, SagaEvent> factory;
    private final StateMachineRuntimePersister<SagaState, SagaEvent, String> persister;

    public StateMachine<SagaState, SagaEvent> startSaga(String sagaId) {
        StateMachine<SagaState, SagaEvent> stateMachine = factory.getStateMachine(sagaId);
        stateMachine.start();
        return stateMachine;
    }

    public void sendEvent(String sagaId, SagaEvent event) {
        StateMachine<SagaState, SagaEvent> stateMachine = factory.getStateMachine(sagaId);

        // Restore state from persistence
        persister.restore(stateMachine, sagaId);

        // Send event
        stateMachine.sendEvent(event);

        // Persist new state
        persister.persist(stateMachine, sagaId);
    }

    public SagaState getCurrentState(String sagaId) {
        StateMachine<SagaState, SagaEvent> stateMachine = factory.getStateMachine(sagaId);
        persister.restore(stateMachine, sagaId);
        return stateMachine.getState().getId();
    }
}
```

## Event Consumers Integration

Event consumers drive saga state transitions:

### WalletCreatedEventConsumer

```java
@Component
@RequiredArgsConstructor
public class WalletCreatedEventConsumer implements Consumer<Message<WalletCreatedEvent>> {
    private final SagaCoordinator sagaCoordinator;

    @Override
    public void accept(Message<WalletCreatedEvent> message) {
        WalletCreatedEvent event = message.getPayload();

        try {
            // Start saga
            String sagaId = event.getCorrelationId().toString();
            sagaCoordinator.startSaga(sagaId);

            // Transition to WALLET_CREATED state
            sagaCoordinator.sendEvent(sagaId, SagaEvent.WALLET_CREATED);

            log.info("Saga {} transitioned to WALLET_CREATED", sagaId);

        } catch (Exception e) {
            log.error("Saga failed: {}", e.getMessage());
            sagaCoordinator.sendEvent(sagaId, SagaEvent.SAGA_FAILED);
        }
    }
}
```

### FundsAddedEventConsumer

```java
@Component
@RequiredArgsConstructor
public class FundsAddedEventConsumer implements Consumer<Message<FundsAddedEvent>> {
    private final SagaCoordinator sagaCoordinator;

    @Override
    public void accept(Message<FundsAddedEvent> message) {
        FundsAddedEvent event = message.getPayload();
        String sagaId = event.getCorrelationId().toString();

        try {
            // Verify current state
            SagaState currentState = sagaCoordinator.getCurrentState(sagaId);
            if (currentState != SagaState.WALLET_CREATED) {
                log.warn("Invalid state transition: {} → FUNDS_ADDED", currentState);
                return;
            }

            // Transition to FUNDS_ADDED
            sagaCoordinator.sendEvent(sagaId, SagaEvent.FUNDS_ADDED);

            log.info("Saga {} transitioned to FUNDS_ADDED", sagaId);

        } catch (Exception e) {
            log.error("Failed to process FundsAddedEvent", e);
            sagaCoordinator.sendEvent(sagaId, SagaEvent.SAGA_FAILED);
        }
    }
}
```

## Compensation (Rollback)

When a saga fails, compensation transactions undo previous work.

### Compensation Actions

```java
@Component
@RequiredArgsConstructor
public class SagaCompensationHandler {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @StateMachineEventListener
    public void handleStateChange(StateChangedEvent<SagaState, SagaEvent> event) {
        if (event.getTarget().getId() == SagaState.FAILED) {
            String sagaId = event.getStateMachine().getId();
            compensate(sagaId, event.getSource().getId());
        }
    }

    private void compensate(String sagaId, SagaState failedAt) {
        log.info("Compensating saga {} failed at {}", sagaId, failedAt);

        switch (failedAt) {
            case FUNDS_TRANSFERRED:
                // Reverse transfer
                reverseTransfer(sagaId);
                // Fall through to compensate earlier steps
            case FUNDS_WITHDRAWN:
                // Reverse withdrawal
                reverseWithdrawal(sagaId);
                // Fall through
            case FUNDS_ADDED:
                // Reverse funds addition
                reverseFundsAddition(sagaId);
                // Fall through
            case WALLET_CREATED:
                // Delete wallet
                deleteWallet(sagaId);
                break;
        }
    }

    private void reverseTransfer(String sagaId) {
        // Find transfer transaction
        // Create reverse transaction
        // Update balances
        log.info("Reversed transfer for saga {}", sagaId);
    }

    private void reverseWithdrawal(String sagaId) {
        // Find withdrawal transaction
        // Add funds back
        log.info("Reversed withdrawal for saga {}", sagaId);
    }

    private void reverseFundsAddition(String sagaId) {
        // Remove added funds
        log.info("Reversed funds addition for saga {}", sagaId);
    }

    private void deleteWallet(String sagaId) {
        // Soft delete wallet
        log.info("Deleted wallet for saga {}", sagaId);
    }
}
```

## State Machine Listeners

Monitor state transitions:

```java
@Component
@Slf4j
public class SagaStateListener {

    @StateMachineEventListener
    public void onStateEntry(StateEntryEvent<SagaState, SagaEvent> event) {
        log.info("Entering state: {}", event.getState().getId());
    }

    @StateMachineEventListener
    public void onStateExit(StateExitEvent<SagaState, SagaEvent> event) {
        log.info("Exiting state: {}", event.getState().getId());
    }

    @StateMachineEventListener
    public void onTransition(TransitionEvent<SagaState, SagaEvent> event) {
        log.info("Transition: {} → {} via {}",
            event.getSource().getId(),
            event.getTarget().getId(),
            event.getEvent()
        );
    }

    @StateMachineEventListener
    public void onStateMachineError(StateMachineEvent<SagaState, SagaEvent> event) {
        log.error("State machine error: {}", event);
    }
}
```

## Transaction Boundaries

Each saga step has its own transaction:

```
Step 1: CreateWallet
  @Transactional
  - Insert wallet
  - Insert outbox event (WalletCreated)
  - COMMIT

Step 2: Consumer processes WalletCreated
  @Transactional
  - Update state machine
  - Perform business logic
  - Insert outbox event (next step)
  - COMMIT
```

This ensures:
- Each step is atomic
- State machine state persisted with business data
- Events reliably published via outbox

## Timeout and Retry

Handle long-running sagas:

```java
@Configuration
public class SagaTimeoutConfig {

    @Bean
    public Action<SagaState, SagaEvent> timeoutAction() {
        return context -> {
            String sagaId = context.getStateMachine().getId();
            log.warn("Saga {} timed out in state {}",
                sagaId,
                context.getSource().getId());

            // Trigger compensation
            context.getStateMachine().sendEvent(SagaEvent.SAGA_FAILED);
        };
    }

    @Bean
    public Guard<SagaState, SagaEvent> timeoutGuard() {
        return context -> {
            String sagaId = context.getStateMachine().getId();
            Instant startTime = getSagaStartTime(sagaId);
            Duration elapsed = Duration.between(startTime, Instant.now());

            return elapsed.toMinutes() < 30; // 30 minute timeout
        };
    }
}
```

## Monitoring and Observability

### Saga Metrics

```java
@Component
@RequiredArgsConstructor
public class SagaMetrics {
    private final MeterRegistry registry;

    public void recordSagaStarted(String sagaId) {
        registry.counter("saga.started",
            "saga_id", sagaId
        ).increment();
    }

    public void recordSagaCompleted(String sagaId, SagaState finalState) {
        registry.counter("saga.completed",
            "saga_id", sagaId,
            "final_state", finalState.name()
        ).increment();
    }

    public void recordSagaDuration(String sagaId, Duration duration) {
        registry.timer("saga.duration",
            "saga_id", sagaId
        ).record(duration);
    }
}
```

### Saga Dashboard Queries

```sql
-- Active sagas
SELECT machine_id, state, created_at
FROM state_machine
WHERE state NOT IN ('COMPLETED', 'FAILED')
ORDER BY created_at DESC;

-- Failed sagas
SELECT machine_id, state, created_at
FROM state_machine
WHERE state = 'FAILED'
ORDER BY created_at DESC;

-- Saga duration
SELECT
    machine_id,
    MIN(created_at) as started,
    MAX(created_at) as completed,
    TIMESTAMPDIFF(SECOND, MIN(created_at), MAX(created_at)) as duration_seconds
FROM state_machine_transition
GROUP BY machine_id;
```

## Testing Sagas

### Unit Test

```java
@ExtendWith(SpringExtension.class)
@SpringBootTest
class SagaCoordinatorTest {

    @Autowired
    private SagaCoordinator coordinator;

    @Test
    void shouldCompleteSagaSuccessfully() {
        // Start saga
        String sagaId = UUID.randomUUID().toString();
        coordinator.startSaga(sagaId);

        // Progress through states
        coordinator.sendEvent(sagaId, SagaEvent.WALLET_CREATED);
        assertEquals(SagaState.WALLET_CREATED, coordinator.getCurrentState(sagaId));

        coordinator.sendEvent(sagaId, SagaEvent.FUNDS_ADDED);
        assertEquals(SagaState.FUNDS_ADDED, coordinator.getCurrentState(sagaId));

        coordinator.sendEvent(sagaId, SagaEvent.FUNDS_WITHDRAWN);
        assertEquals(SagaState.FUNDS_WITHDRAWN, coordinator.getCurrentState(sagaId));

        coordinator.sendEvent(sagaId, SagaEvent.FUNDS_TRANSFERRED);
        assertEquals(SagaState.FUNDS_TRANSFERRED, coordinator.getCurrentState(sagaId));

        coordinator.sendEvent(sagaId, SagaEvent.SAGA_COMPLETED);
        assertEquals(SagaState.COMPLETED, coordinator.getCurrentState(sagaId));
    }

    @Test
    void shouldHandleFailureAndCompensate() {
        String sagaId = UUID.randomUUID().toString();
        coordinator.startSaga(sagaId);

        coordinator.sendEvent(sagaId, SagaEvent.WALLET_CREATED);
        coordinator.sendEvent(sagaId, SagaEvent.FUNDS_ADDED);

        // Simulate failure
        coordinator.sendEvent(sagaId, SagaEvent.SAGA_FAILED);
        assertEquals(SagaState.FAILED, coordinator.getCurrentState(sagaId));

        // Verify compensation was triggered
        // (Check that wallet was deleted, funds reversed, etc.)
    }
}
```

## Best Practices

### 1. Correlation ID

Always use correlation ID to link saga steps:

```java
UUID correlationId = UUID.randomUUID();
// Use same correlationId for all events in saga
```

### 2. Idempotency

Each saga step must be idempotent:

```java
@Transactional
public void processStep(String sagaId, Event event) {
    if (alreadyProcessed(sagaId, event.getId())) {
        return; // Skip duplicate
    }
    // Process event
    markProcessed(sagaId, event.getId());
}
```

### 3. State Validation

Always validate current state before transition:

```java
SagaState current = coordinator.getCurrentState(sagaId);
if (current != SagaState.EXPECTED_STATE) {
    log.warn("Invalid transition from {}", current);
    return;
}
```

### 4. Compensation Order

Compensate in reverse order:

```
Forward:  A → B → C → D
Backward: D → C → B → A
```

### 5. Timeouts

Set reasonable timeouts for each step and overall saga.

### 6. Monitoring

Track saga metrics: start, complete, fail, duration.

### 7. Dead Letter Queue

Failed sagas should go to DLQ for manual review.

## Advanced Scenarios

### Parallel Execution

Some steps can run in parallel:

```
       → Step B →
Step A             Step D
       → Step C →
```

### Nested Sagas

Sagas can invoke other sagas:

```
Main Saga
  → Sub-saga 1
  → Sub-saga 2
```

### Long-Running Sagas

For sagas that take days/weeks:
- Persist intermediate state
- Support pause/resume
- Implement checkpoints

## References

- [Spring State Machine Docs](https://docs.spring.io/spring-statemachine/docs/current/reference/)
- [Saga Pattern (Microsoft)](https://learn.microsoft.com/en-us/azure/architecture/reference-architectures/saga/saga)
- [Microservices Patterns](https://microservices.io/patterns/data/saga.html)
