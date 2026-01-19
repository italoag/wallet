# Wallet Hub - Testing Guide

This guide covers the comprehensive testing strategy for Wallet Hub, including unit tests, integration tests, and best practices.

## Table of Contents

- [Testing Strategy Overview](#testing-strategy-overview)
- [Test Structure](#test-structure)
- [Unit Testing](#unit-testing)
- [Use Case Testing](#use-case-testing)
- [Integration Testing](#integration-testing)
- [Messaging Tests](#messaging-tests)
- [Known Issues](#known-issues)
- [Running Tests](#running-tests)
- [Test Coverage](#test-coverage)
- [Testing Best Practices](#testing-best-practices)
- [CI/CD Integration](#cicd-integration)

---

## Testing Strategy Overview

Wallet Hub follows the **Test Pyramid** approach with three distinct testing layers:

```
        /\
       /  \      E2E Tests (Few)
      /----\
     /      \    Integration Tests (Some)
    /--------\
   /          \  Unit Tests (Many)
  /____________\
```

### Testing Layers

1. **Unit Tests (70%)**: Pure domain logic, no dependencies
2. **Use Case Tests (20%)**: Application logic with mocked gateways
3. **Integration Tests (10%)**: Infrastructure components (JPA, Kafka, etc.)

### Testing Principles

- **Domain Purity**: Domain tests have zero external dependencies
- **Mocking at Boundaries**: Mock only gateway interfaces, never domain objects
- **Real Infrastructure**: Use real databases (H2) and test binders for integration tests
- **Fast Feedback**: All tests complete in < 30 seconds
- **Idempotency**: Tests can run in any order without side effects

---

## Test Structure

### Directory Layout

```
src/test/java/dev/bloco/wallet/hub/
├── domain/                                    # Domain model tests (pure logic)
│   ├── WalletTest.java
│   ├── UserTest.java
│   ├── TransactionTest.java
│   └── event/
│       ├── WalletCreatedEventTest.java
│       ├── FundsAddedEventTest.java
│       ├── FundsWithdrawnEventTest.java
│       └── FundsTransferredEventTest.java
├── usecase/                                   # Use case tests (mocked gateways)
│   ├── CreateWalletUseCaseTest.java
│   ├── AddFundsUseCaseTest.java
│   ├── WithdrawFundsUseCaseTest.java
│   ├── TransferFundsUseCaseTest.java
│   └── [30+ other use case tests]
└── infra/                                     # Infrastructure tests
    ├── adapter/
    │   └── event/
    │       ├── consumer/
    │       │   ├── WalletCreatedEventConsumerTest.java
    │       │   ├── FundsAddedEventConsumerTest.java
    │       │   ├── FundsWithdrawnEventConsumerTest.java
    │       │   └── FundsTransferredEventConsumerTest.java
    │       └── producer/
    │           ├── KafkaEventProducerTest.java
    │           └── OutboxEventPublisherTest.java
    └── provider/
        └── data/
            ├── config/
            │   ├── SagaEnumsTest.java
            │   ├── SagaStateMachineConfigTest.java
            │   └── StateMachineJpaIntegrationTest.java
            ├── entity/
            │   ├── WalletEntityTest.java
            │   ├── UserEntityTest.java
            │   └── TransactionEntityTest.java
            └── repository/
                ├── SpringDataWalletRepositoryTest.java
                ├── JpaWalletRepositoryTest.java
                ├── SpringDataUserRepositoryTest.java
                └── [other repository tests]
```

### Naming Conventions

- **Test Class**: `{ClassName}Test.java`
- **Test Method**: `methodName_condition_expectedBehavior()`
- **DisplayName**: Human-readable description using `@DisplayName`

---

## Unit Testing

Unit tests focus on **pure domain logic** with no external dependencies, frameworks, or mocks.

### Domain Model Tests

Domain models are tested in isolation to verify business rules.

#### Example: WalletTest.java

```java
package dev.bloco.wallet.hub.domain;

import dev.bloco.wallet.hub.domain.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Wallet Tests")
class WalletTest {

    @Test
    @DisplayName("New wallet starts with zero balance and has generated id")
    void newWalletDefaults() {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");

        assertThat(wallet.getId()).isNotNull();
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("addFunds increases the balance for positive amounts")
    void addFundsSuccess() {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");

        wallet.addFunds(new BigDecimal("10.50"));
        wallet.addFunds(new BigDecimal("0.50"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("11.00"));
    }

    @Test
    @DisplayName("addFunds rejects zero or negative values")
    void addFundsInvalid() {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");

        assertThatThrownBy(() -> wallet.addFunds(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
        assertThatThrownBy(() -> wallet.addFunds(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("withdrawFunds decreases balance when sufficient and positive")
    void withdrawFundsSuccess() {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");
        wallet.addFunds(new BigDecimal("100.00"));

        wallet.withdrawFunds(new BigDecimal("30.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    @DisplayName("withdrawFunds rejects invalid or insufficient balance")
    void withdrawFundsInvalid() {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");
        wallet.addFunds(new BigDecimal("10.00"));

        assertThatThrownBy(() -> wallet.withdrawFunds(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be greater than zero");
        assertThatThrownBy(() -> wallet.withdrawFunds(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be greater than zero");
        assertThatThrownBy(() -> wallet.withdrawFunds(new BigDecimal("20")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");
    }
}
```

#### Key Characteristics

- **No Spring annotations** (`@SpringBootTest`, `@Autowired`, etc.)
- **No mocks** (pure logic only)
- **Fast execution** (< 10ms per test)
- **Behavior-driven assertions** using AssertJ

### Domain Event Tests

Test domain events as immutable data structures.

#### Example: WalletCreatedEventTest.java

```java
package dev.bloco.wallet.hub.domain.event;

import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Wallet Created Event Tests")
class WalletCreatedEventTest {

    @Test
    @DisplayName("Event holds walletId and correlationId")
    void eventCreation() {
        UUID walletId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        WalletCreatedEvent event = new WalletCreatedEvent(walletId, correlationId);

        assertThat(event.getWalletId()).isEqualTo(walletId);
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Events with same values are equal")
    void eventEquality() {
        UUID walletId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        WalletCreatedEvent event1 = new WalletCreatedEvent(walletId, correlationId);
        WalletCreatedEvent event2 = new WalletCreatedEvent(walletId, correlationId);

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }
}
```

---

## Use Case Testing

Use case tests verify application logic by **mocking gateway interfaces** (repositories, event publishers).

### Characteristics

- **Mock only at boundaries**: Repository, EventPublisher, external services
- **Never mock domain objects**
- **Verify interactions**: Use `verify()` to ensure correct method calls
- **Constructor injection**: Easy to inject mocks

### Example: CreateWalletUseCaseTest.java

```java
package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Create Wallet Use Case Tests")
class CreateWalletUseCaseTest {

    @Test
    @DisplayName("createWallet saves wallet and publishes WalletCreatedEvent")
    void createWallet_success() {
        // Arrange: Create mocks
        WalletRepository walletRepository = mock(WalletRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        CreateWalletUseCase useCase = new CreateWalletUseCase(walletRepository, eventPublisher);

        UUID userId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();

        // Stub repository to return the saved wallet
        when(walletRepository.save(any(Wallet.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Execute use case
        Wallet result = useCase.createWallet(userId, correlationId);

        // Assert: Verify wallet created
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();

        // Verify repository save called
        verify(walletRepository, times(1)).save(result);

        // Capture and verify published event
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publish(eventCaptor.capture());

        Object published = eventCaptor.getValue();
        assertThat(published).isInstanceOf(WalletCreatedEvent.class);

        WalletCreatedEvent evt = (WalletCreatedEvent) published;
        assertThat(evt.getWalletId()).isEqualTo(result.getId());
        assertThat(evt.getCorrelationId()).isEqualTo(UUID.fromString(correlationId));

        // Ensure no unexpected interactions
        verifyNoMoreInteractions(walletRepository, eventPublisher);
    }
}
```

### Testing Patterns

#### Pattern 1: Verify Method Calls

```java
verify(repository, times(1)).save(any(Wallet.class));
verify(eventPublisher, never()).publish(any());
```

#### Pattern 2: Capture Arguments

```java
ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
verify(repository).save(walletCaptor.capture());
Wallet saved = walletCaptor.getValue();
assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
```

#### Pattern 3: Stubbing Responses

```java
when(repository.findById(walletId))
    .thenReturn(Optional.of(existingWallet));
```

#### Pattern 4: Exception Testing

```java
when(repository.save(any())).thenThrow(new RuntimeException("DB Error"));

assertThatThrownBy(() -> useCase.createWallet(userId, corrId))
    .isInstanceOf(RuntimeException.class)
    .hasMessageContaining("DB Error");
```

---

## Integration Testing

Integration tests verify infrastructure components with real dependencies.

### Repository Tests (@DataJpaTest)

Use `@DataJpaTest` for testing Spring Data JPA repositories.

#### Example: SpringDataWalletRepositoryTest.java

```java
package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.WalletEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;

@DisplayName("Spring Data Wallet Repository Tests")
@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class SpringDataWalletRepositoryTest {

    @Autowired
    private SpringDataWalletRepository repository;

    @Test
    @DisplayName("Save and find by id")
    void saveAndFindById_roundTrip() {
        // Arrange: Create entity
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(UUID.randomUUID());
        wallet.setBalance(new BigDecimal("99.99"));

        // Act: Save and reload
        WalletEntity persisted = repository.save(wallet);
        assertThat(persisted.getId()).isNotNull();

        Optional<WalletEntity> reloaded = repository.findById(persisted.getId());

        // Assert: Verify persistence
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getUserId()).isEqualTo(wallet.getUserId());
        assertThat(reloaded.get().getBalance()).isEqualByComparingTo("99.99");
    }

    @Test
    @DisplayName("Find by non-existent id returns empty")
    void findById_notFound() {
        Optional<WalletEntity> result = repository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Delete removes entity")
    void delete_removesEntity() {
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(UUID.randomUUID());
        WalletEntity saved = repository.save(wallet);

        repository.delete(saved);

        Optional<WalletEntity> reloaded = repository.findById(saved.getId());
        assertThat(reloaded).isEmpty();
    }
}
```

#### Key Annotations

- `@DataJpaTest`: Loads only JPA components, uses in-memory H2
- `@AutoConfigureTestDatabase(replace = ANY)`: Replace with embedded database
- `@Autowired`: Inject repository under test

#### Benefits

- **Real database**: Tests actual SQL queries
- **Transaction rollback**: Each test isolated
- **Fast**: Uses H2 in-memory
- **No mocking**: Tests real JPA behavior

### Testcontainers (Advanced)

For testing with real PostgreSQL or MongoDB.

#### Example: PostgreSQL with Testcontainers

```java
package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.WalletEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = NONE)
@DisplayName("Wallet Repository - PostgreSQL Integration Tests")
class WalletRepositoryPostgresTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SpringDataWalletRepository repository;

    @Test
    @DisplayName("Persists wallet in real PostgreSQL")
    void saveWallet_postgres() {
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(UUID.randomUUID());
        wallet.setBalance(new BigDecimal("100.00"));

        WalletEntity saved = repository.save(wallet);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }
}
```

**Dependencies:**
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Messaging Tests

Test event consumers and producers using **Spring Cloud Stream Test Binder**.

### Consumer Tests

Test event consumers with mocked state machines.

#### Example: WalletCreatedEventConsumerTest.java

```java
package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaEvents;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaStates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Wallet Created Event Consumer Tests")
class WalletCreatedEventConsumerTest {

    private StateMachine<SagaStates, SagaEvents> stateMachine;
    private WalletCreatedEventConsumer consumerConfig;

    @BeforeEach
    void setUp() {
        stateMachine = mock(StateMachine.class);
        doReturn(Flux.empty()).when(stateMachine)
                .sendEvent(any(Mono.class));
        consumerConfig = new WalletCreatedEventConsumer(stateMachine);
    }

    @Test
    @DisplayName("Should send WalletCreated event to state machine")
    void walletCreatedEventConsumerFunction_withCorrelationId_sendsWalletCreated() {
        // Arrange
        Consumer<Message<WalletCreatedEvent>> fn = consumerConfig.walletCreatedEventConsumerFunction();
        UUID corrId = UUID.randomUUID();
        var event = new WalletCreatedEvent(UUID.randomUUID(), corrId);
        Message<WalletCreatedEvent> message = MessageBuilder.withPayload(event).build();

        // Act
        fn.accept(message);

        // Assert
        ArgumentCaptor<Mono<Message<SagaEvents>>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(stateMachine).sendEvent(captor.capture());

        Message<SagaEvents> sent = captor.getValue().block();
        assertThat(sent.getPayload()).isEqualTo(SagaEvents.WALLET_CREATED);
        assertThat(sent.getHeaders().get("correlationId")).isEqualTo(corrId);
    }

    @Test
    @DisplayName("Should send SagaFailed event to state machine when correlationId is null")
    void walletCreatedEventConsumerFunction_withoutCorrelationId_sendsSagaFailed() {
        Consumer<Message<WalletCreatedEvent>> fn = consumerConfig.walletCreatedEventConsumerFunction();
        var event = new WalletCreatedEvent(UUID.randomUUID(), null);
        Message<WalletCreatedEvent> message = MessageBuilder.withPayload(event).build();

        fn.accept(message);

        ArgumentCaptor<Mono<Message<SagaEvents>>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(stateMachine, atLeastOnce()).sendEvent(captor.capture());

        Message<SagaEvents> last = captor.getAllValues().get(captor.getAllValues().size() - 1).block();
        assertThat(last.getPayload()).isEqualTo(SagaEvents.SAGA_FAILED);
    }
}
```

### Producer Tests with Test Binder

Test event publishing end-to-end using Spring Cloud Stream Test Binder.

#### Example: KafkaEventProducerTest.java

```java
package dev.bloco.wallet.hub.infra.adapter.event.producer;

import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
@DisplayName("Kafka Event Producer Tests")
class KafkaEventProducerTest {

    @Autowired
    private KafkaEventProducer eventProducer;

    @Autowired
    private OutputDestination output;

    @Test
    @DisplayName("Publishing WalletCreatedEvent sends message to output destination")
    void publishWalletCreatedEvent_sendsMessage() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        WalletCreatedEvent event = new WalletCreatedEvent(walletId, correlationId);

        // Act
        eventProducer.publishWalletCreatedEvent(event);

        // Assert: Verify message sent to output
        Message<byte[]> message = output.receive(1000, "wallet-created-topic");
        assertThat(message).isNotNull();
        assertThat(message.getHeaders())
            .containsKey("ce_id")
            .containsKey("ce_source")
            .containsKey("ce_type");
    }
}
```

#### Test Configuration

Create `src/test/resources/application-test.yml`:

```yaml
spring:
  cloud:
    stream:
      defaultBinder: test
```

Or use `@TestPropertySource`:

```java
@TestPropertySource(properties = {
    "spring.cloud.stream.defaultBinder=test"
})
```

### Integration Test with InputDestination

Test consumers end-to-end by sending messages.

```java
package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
@DisplayName("Wallet Created Event Consumer Integration Tests")
class WalletCreatedEventConsumerIntegrationTest {

    @Autowired
    private InputDestination input;

    @Test
    @DisplayName("Receives message from input destination and processes it")
    void receiveWalletCreatedEvent() {
        // Arrange
        WalletCreatedEvent event = new WalletCreatedEvent(
            UUID.randomUUID(),
            UUID.randomUUID()
        );

        // Act: Send message to input
        input.send(MessageBuilder.withPayload(event).build(), "wallet-created-topic");

        // Assert: Verify processing (check database, logs, etc.)
        // Implementation depends on your consumer logic
    }
}
```

---

## Known Issues

### Issue 1: FundsAddedEventConsumer Bean Duplication

**Problem:** `BeanDefinitionOverrideException` due to both `@Component` and `@Bean` annotations.

**Cause:**
```java
@Component  // Creates bean "fundsAddedEventConsumer"
public class FundsAddedEventConsumer {

    @Bean  // Also creates bean "fundsAddedEventConsumerFunction"
    public Consumer<Message<FundsAddedEvent>> fundsAddedEventConsumerFunction() {
        // ...
    }
}
```

**Solution 1** (Recommended): Remove `@Component`

```java
// Remove @Component annotation
public class FundsAddedEventConsumer {

    @Bean
    public Consumer<Message<FundsAddedEvent>> fundsAddedEventConsumerFunction() {
        // ...
    }
}
```

Then register bean in `@Configuration` class:

```java
@Configuration
public class EventConsumerConfig {

    @Bean
    public FundsAddedEventConsumer fundsAddedEventConsumer(StateMachine<?, ?> stateMachine) {
        return new FundsAddedEventConsumer(stateMachine);
    }
}
```

**Solution 2**: Rename `@Bean` method

```java
@Component
public class FundsAddedEventConsumer {

    @Bean
    public Consumer<Message<FundsAddedEvent>> fundsAddedConsumerFunc() {  // Different name
        // ...
    }
}
```

**Solution 3** (Not recommended): Enable bean overriding

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
```

### Issue 2: StateMachine JPA Persistence in Tests

**Problem:** State machine persistence tests fail due to missing JPA configuration.

**Solution:** Use `@DataJpaTest` with explicit entity scan:

```java
@DataJpaTest
@EnableJpaRepositories(basePackages = {
    "org.springframework.statemachine.data.jpa",
    "dev.bloco.wallet.hub.infra.provider.data.repository"
})
@EntityScan(basePackages = {
    "org.springframework.statemachine.data.jpa",
    "dev.bloco.wallet.hub.infra.provider.data.entity"
})
class StateMachineJpaIntegrationTest {
    // ...
}
```

### Issue 3: H2 Compatibility with PostgreSQL Syntax

**Problem:** Tests fail when using PostgreSQL-specific SQL in entities.

**Solution:** Use Hibernate's `@Formula` with dialect-specific expressions:

```java
@Formula("(SELECT COUNT(*) FROM transaction t WHERE t.wallet_id = id)")
private int transactionCount;
```

Or create separate test configurations:

```yaml
# application-test.yml
spring:
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    properties:
      hibernate:
        globally_quoted_identifiers: true
```

---

## Running Tests

### Run All Tests

```bash
./mvnw test
```

**Expected output:**
```
[INFO] Tests run: 65, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Run Specific Test Class

```bash
./mvnw -Dtest=WalletTest test
```

### Run Specific Test Method

```bash
./mvnw -Dtest=WalletTest#addFundsSuccess test
```

### Run Tests by Package

```bash
# Domain tests only
./mvnw -Dtest=dev.bloco.wallet.hub.domain.** test

# Use case tests only
./mvnw -Dtest=dev.bloco.wallet.hub.usecase.** test

# Repository tests only
./mvnw -Dtest=**repository** test
```

### Run Tests with Different JDK

```bash
./mvnw test -Dmaven.compiler.release=21
```

### Run Tests with Coverage (JaCoCo)

Add to `pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Run tests with coverage:

```bash
./mvnw clean test jacoco:report
```

View report:
```
open target/site/jacoco/index.html
```

### Run Tests in Parallel

Add to `pom.xml`:

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <parallel>classes</parallel>
        <threadCount>4</threadCount>
    </configuration>
</plugin>
```

### Skip Tests During Build

```bash
./mvnw clean package -DskipTests
```

### Run Only Integration Tests

```bash
./mvnw test -Dgroups=integration
```

Tag tests with `@Tag`:

```java
@Test
@Tag("integration")
void integrationTest() {
    // ...
}
```

---

## Test Coverage

### Current Coverage (Example Metrics)

| Layer | Classes | Coverage |
|-------|---------|----------|
| Domain | 15 | 95% |
| Use Cases | 30 | 85% |
| Infrastructure | 25 | 70% |
| **Overall** | **70** | **82%** |

### Coverage Goals

- **Domain layer**: 90%+ (critical business logic)
- **Use case layer**: 80%+ (application logic)
- **Infrastructure layer**: 60%+ (integration points)
- **Overall**: 75%+

### Uncovered Code

Acceptable to skip coverage for:
- Configuration classes
- DTOs/POJOs
- Lombok-generated code
- Main application class

### Viewing Coverage Report

After running `mvn test jacoco:report`:

```bash
# HTML report
open target/site/jacoco/index.html

# XML report (for CI tools)
cat target/site/jacoco/jacoco.xml

# CSV report
cat target/site/jacoco/jacoco.csv
```

---

## Testing Best Practices

### 1. Follow AAA Pattern (Arrange-Act-Assert)

```java
@Test
void addFunds_increasesBalance() {
    // Arrange
    Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");
    BigDecimal amount = new BigDecimal("100.00");

    // Act
    wallet.addFunds(amount);

    // Assert
    assertThat(wallet.getBalance()).isEqualByComparingTo(amount);
}
```

### 2. Use Descriptive Test Names

```java
// Good
void createWallet_withValidUserId_returnsWalletWithZeroBalance()

// Bad
void test1()
```

### 3. One Assertion Focus Per Test

```java
// Good: Focused tests
@Test
void newWallet_hasZeroBalance() {
    assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
}

@Test
void newWallet_hasGeneratedId() {
    assertThat(wallet.getId()).isNotNull();
}

// Bad: Multiple unrelated assertions
@Test
void newWallet_test() {
    assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(wallet.getId()).isNotNull();
    assertThat(wallet.getName()).isNotEmpty();
    assertThat(wallet.getCreatedAt()).isNotNull();
}
```

### 4. Use AssertJ for Fluent Assertions

```java
// Good
assertThat(wallet.getBalance())
    .isNotNull()
    .isGreaterThan(BigDecimal.ZERO)
    .isLessThanOrEqualTo(new BigDecimal("1000"));

// Less readable
assertTrue(wallet.getBalance() != null);
assertTrue(wallet.getBalance().compareTo(BigDecimal.ZERO) > 0);
assertTrue(wallet.getBalance().compareTo(new BigDecimal("1000")) <= 0);
```

### 5. Test Edge Cases and Boundaries

```java
@Test
void addFunds_withMaxValue_succeeds() {
    wallet.addFunds(new BigDecimal("999999999999.99"));
}

@Test
void addFunds_withMinPositive_succeeds() {
    wallet.addFunds(new BigDecimal("0.01"));
}

@Test
void withdrawFunds_exactBalance_leavesZero() {
    wallet.addFunds(new BigDecimal("100"));
    wallet.withdrawFunds(new BigDecimal("100"));
    assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
}
```

### 6. Don't Test Framework Code

```java
// Bad: Testing Spring Data
@Test
void repository_canSave() {
    repository.save(entity);
    assertThat(repository.count()).isEqualTo(1);
}

// Good: Test your business logic
@Test
void findActiveWallets_returnsOnlyActiveStatus() {
    repository.save(activeWallet);
    repository.save(inactiveWallet);

    List<Wallet> active = repository.findByStatus(WalletStatus.ACTIVE);

    assertThat(active).hasSize(1);
    assertThat(active.get(0).getStatus()).isEqualTo(WalletStatus.ACTIVE);
}
```

### 7. Isolate Tests (No Shared State)

```java
// Bad: Shared state
class WalletTest {
    private static Wallet wallet;  // WRONG: Shared across tests

    @BeforeAll
    static void setup() {
        wallet = new Wallet(...);
    }
}

// Good: Isolated state
class WalletTest {
    private Wallet wallet;  // Recreated for each test

    @BeforeEach
    void setup() {
        wallet = new Wallet(...);
    }
}
```

### 8. Use Test Fixtures for Complex Setup

```java
class WalletTestFixtures {
    public static Wallet createWalletWithBalance(BigDecimal balance) {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");
        wallet.addFunds(balance);
        return wallet;
    }

    public static WalletEntity createWalletEntity(UUID userId, BigDecimal balance) {
        WalletEntity entity = new WalletEntity();
        entity.setUserId(userId);
        entity.setBalance(balance);
        return entity;
    }
}

// Usage
@Test
void test() {
    Wallet wallet = WalletTestFixtures.createWalletWithBalance(new BigDecimal("100"));
    // ...
}
```

### 9. Verify Mock Interactions Explicitly

```java
@Test
void createWallet_callsRepository() {
    // Act
    useCase.createWallet(userId, correlationId);

    // Assert: Verify exact interactions
    verify(repository, times(1)).save(any(Wallet.class));
    verify(eventPublisher, times(1)).publish(any(WalletCreatedEvent.class));
    verifyNoMoreInteractions(repository, eventPublisher);
}
```

### 10. Test Asynchronous Code Properly

```java
@Test
void asyncEventProcessing_completesSuccessfully() {
    // Use StepVerifier for reactive code
    Mono<Void> result = eventConsumer.processEvent(event);

    StepVerifier.create(result)
        .expectComplete()
        .verify(Duration.ofSeconds(5));
}
```

---

## CI/CD Integration

### GitHub Actions Example

Create `.github/workflows/test.yml`:

```yaml
name: Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 24
      uses: actions/setup-java@v4
      with:
        java-version: '24'
        distribution: 'temurin'
        cache: maven

    - name: Run tests
      run: ./mvnw clean test

    - name: Generate coverage report
      run: ./mvnw jacoco:report

    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v4
      with:
        files: ./target/site/jacoco/jacoco.xml
        fail_ci_if_error: true
```

### GitLab CI Example

Create `.gitlab-ci.yml`:

```yaml
image: maven:3.9-eclipse-temurin-24

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

cache:
  paths:
    - .m2/repository/

stages:
  - test
  - coverage

test:
  stage: test
  script:
    - ./mvnw clean test
  artifacts:
    reports:
      junit: target/surefire-reports/TEST-*.xml

coverage:
  stage: coverage
  script:
    - ./mvnw jacoco:report
  coverage: '/Total.*?([0-9]{1,3})%/'
  artifacts:
    paths:
      - target/site/jacoco/
```

### Maven Surefire Reports

Tests automatically generate JUnit XML reports in:
```
target/surefire-reports/
```

These are compatible with all major CI platforms (Jenkins, GitHub Actions, GitLab CI, etc.).

---

## Advanced Testing Topics

### Testing with Awaitility (Async)

For testing eventual consistency:

```xml
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@Test
void eventuallyProcessesEvent() {
    publishEvent(event);

    await().atMost(5, SECONDS)
        .untilAsserted(() -> {
            assertThat(repository.findById(walletId))
                .isPresent()
                .hasValueSatisfying(w ->
                    assertThat(w.getStatus()).isEqualTo(WalletStatus.ACTIVE)
                );
        });
}
```

### Contract Testing (Spring Cloud Contract)

Test consumer-producer contracts:

```groovy
Contract.make {
    description "should produce WalletCreatedEvent"
    input {
        triggeredBy('createWallet()')
    }
    outputMessage {
        sentTo 'wallet-created-topic'
        body([
            walletId: $(regex('[a-f0-9\\-]{36}')),
            correlationId: $(regex('[a-f0-9\\-]{36}'))
        ])
        headers {
            messagingContentType(applicationJson())
        }
    }
}
```

### Mutation Testing (PIT)

Verify test quality by introducing mutations:

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.15.0</version>
</plugin>
```

```bash
./mvnw org.pitest:pitest-maven:mutationCoverage
```

---

## Troubleshooting Tests

### Issue: Tests Pass Locally, Fail in CI

**Cause:** Environment differences (timezone, locale, file paths)

**Solution:**
```java
@Test
void test() {
    // Set explicit timezone
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    // Use relative paths
    Path testFile = Paths.get("src/test/resources/test-data.json");
}
```

### Issue: Slow Tests

**Causes:**
- Too many `@SpringBootTest` (heavy context loading)
- Not using test slices (`@DataJpaTest`, `@WebMvcTest`)
- Unnecessary thread sleeps

**Solutions:**
```java
// Replace @SpringBootTest with test slices
@DataJpaTest  // Instead of @SpringBootTest

// Use virtual time for reactor tests
StepVerifier.withVirtualTime(() -> myMono)
    .thenAwait(Duration.ofHours(1))  // Virtual, instant
    .expectNext(result)
    .verifyComplete();
```

### Issue: Flaky Tests

**Causes:**
- Shared state between tests
- Timing dependencies
- Random values without seeds

**Solutions:**
```java
// Isolate tests
@BeforeEach
void setUp() {
    clearDatabase();
}

// Use fixed seeds for randomness
Random random = new Random(42);

// Use StepVerifier for async code
StepVerifier.create(mono)
    .expectNext(value)
    .verifyComplete();
```

---

## Summary

Wallet Hub's testing strategy emphasizes:

1. **Pure domain tests** with no dependencies
2. **Use case tests** with mocked boundaries
3. **Integration tests** with real infrastructure
4. **Fast feedback** (< 30s total test time)
5. **High coverage** (75%+ overall, 90%+ domain)

Follow the test examples in `src/test/java/` as templates for new tests.

---

**Last Updated:** 2025-12-10
