# Developer Quick Start Guide

**Version**: 1.0
**Last Updated**: 2026-01-12

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Getting Started](#getting-started)
3. [Development Workflow](#development-workflow)
4. [Common Tasks](#common-tasks)
5. [Architecture Overview](#architecture-overview)
6. [Code Patterns](#code-patterns)
7. [Testing](#testing)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required

- **JDK 24+** (GraalVM CE 25 recommended)
  - Download: https://www.graalvm.org/downloads/
  - Verify: `java -version`

- **Maven 3.8+** (use Maven Wrapper provided)
  - Verify: `./mvnw -version`

### Optional (for full stack)

- **Docker** (for infrastructure services)
  - PostgreSQL, Redis, MongoDB, Kafka
  - Install: https://www.docker.com/get-started

- **Kafka** (or use test binder)
  - Local broker at `localhost:9092`
  - Or run via `docker-compose up -d`

### IDE Setup

**Recommended**: IntelliJ IDEA or VS Code with Java extensions

**IntelliJ IDEA**:
1. Open project (Maven auto-import)
2. Enable annotation processing (Settings → Build → Compiler → Annotation Processors)
3. Install Lombok plugin
4. Install MapStruct plugin

**VS Code**:
1. Install Extension Pack for Java
2. Install Lombok Annotations Support
3. Open folder, Maven auto-detect

---

## Getting Started

### 1. Clone Repository

```bash
git clone https://github.com/your-org/wallet-hub.git
cd wallet-hub
```

### 2. Build Project

```bash
# Clean build (skip tests for speed)
./mvnw clean package -DskipTests

# With tests
./mvnw clean verify
```

**Expected Output**:
```
[INFO] BUILD SUCCESS
[INFO] Total time: 45.678 s
```

### 3. Run Application

**Option A: H2 In-Memory (No Dependencies)**

```bash
./mvnw spring-boot:run
```

**Option B: Full Stack (Docker)**

```bash
# Start infrastructure
docker-compose up -d

# Run application with PostgreSQL
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### 4. Verify Startup

**Console Output**:
```
Started WalletHubApplication in 8.234 seconds (JVM running for 8.567)
```

**H2 Console**: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./db/wallet`
- Username: `sa`
- Password: (empty)

**Health Check**: http://localhost:8080/actuator/health

```json
{
  "status": "UP"
}
```

---

## Development Workflow

### Project Structure

```
wallet-hub/
├── src/main/java/dev/bloco/wallet/hub/
│   ├── domain/              # Core business logic (PURE)
│   │   ├── model/          # Entities, value objects
│   │   ├── event/          # Domain events
│   │   └── gateway/        # Repository interfaces (ports)
│   │
│   ├── usecase/             # Application services
│   │   ├── CreateWalletUseCase.java
│   │   ├── AddFundsUseCase.java
│   │   └── ... (34 use cases)
│   │
│   └── infra/               # Infrastructure (adapters)
│       ├── adapter/
│       │   ├── event/      # Kafka producers/consumers
│       │   ├── security/   # OAuth2, JWT
│       │   └── tracing/    # Observability
│       └── provider/
│           ├── data/       # JPA repositories, entities
│           └── mapper/     # MapStruct mappers
│
├── src/main/resources/
│   ├── application.yml     # Main configuration
│   └── application-*.yml   # Profile-specific configs
│
├── src/test/java/          # Tests mirror main structure
├── docker-compose.yml      # Infrastructure services
├── pom.xml                 # Maven dependencies
└── CLAUDE.md               # Project-specific AI instructions
```

### Layer Responsibilities

| Layer | Purpose | Dependencies | Example |
|-------|---------|--------------|---------|
| **Domain** | Business logic | NONE (pure Java) | `Wallet.addFunds()` |
| **Use Case** | Orchestration | Domain + Gateways | `CreateWalletUseCase` |
| **Infrastructure** | Technical concerns | Domain + Use Case + Frameworks | `KafkaEventProducer` |

**Dependency Rule**: `Infrastructure → Use Case → Domain` (NEVER reverse)

---

## Common Tasks

### Task 1: Add New Domain Entity

**Example**: Add `Portfolio` entity

**Step 1: Create Domain Model**

```bash
touch src/main/java/dev/bloco/wallet/hub/domain/model/Portfolio.java
```

```java
package dev.bloco.wallet.hub.domain.model;

import dev.bloco.wallet.hub.domain.model.common.AggregateRoot;
import java.math.BigDecimal;
import java.util.UUID;

public class Portfolio extends AggregateRoot {
    private UUID userId;
    private BigDecimal totalValue;

    public Portfolio(UUID id, UUID userId) {
        super(id);
        this.userId = userId;
        this.totalValue = BigDecimal.ZERO;
    }

    public void updateTotalValue(BigDecimal value) {
        this.totalValue = value;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }
}
```

**Step 2: Create Repository Interface (Port)**

```bash
touch src/main/java/dev/bloco/wallet/hub/domain/gateway/PortfolioRepository.java
```

```java
package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.Portfolio;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository {
    Portfolio save(Portfolio portfolio);
    Optional<Portfolio> findById(UUID id);
    Optional<Portfolio> findByUserId(UUID userId);
}
```

**Step 3: Create JPA Entity**

```bash
touch src/main/java/dev/bloco/wallet/hub/infra/provider/data/entity/PortfolioEntity.java
```

```java
package dev.bloco.wallet.hub.infra.provider.data.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "portfolio")
public class PortfolioEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(precision = 36, scale = 18)
    private BigDecimal totalValue;

    // Getters and setters
}
```

**Step 4: Create Repository Implementation**

```bash
touch src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataPortfolioRepository.java
touch src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaPortfolioRepository.java
```

```java
// Spring Data interface
public interface SpringDataPortfolioRepository extends JpaRepository<PortfolioEntity, UUID> {
    Optional<PortfolioEntity> findByUserId(UUID userId);
}

// Implementation
@Component
public class JpaPortfolioRepository implements PortfolioRepository {
    private final SpringDataPortfolioRepository jpaRepository;
    private final PortfolioMapper mapper;

    @Override
    public Portfolio save(Portfolio portfolio) {
        PortfolioEntity entity = mapper.toEntity(portfolio);
        PortfolioEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Portfolio> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
            .map(mapper::toDomain);
    }
}
```

**Step 5: Create MapStruct Mapper**

```bash
touch src/main/java/dev/bloco/wallet/hub/infra/provider/mapper/PortfolioMapper.java
```

```java
@Mapper(componentModel = "spring")
public interface PortfolioMapper {
    PortfolioEntity toEntity(Portfolio portfolio);
    Portfolio toDomain(PortfolioEntity entity);
}
```

**Step 6: Rebuild**

```bash
./mvnw clean compile  # Generates MapStruct implementations
```

---

### Task 2: Add New Use Case

**Example**: `GetPortfolioSummaryUseCase`

**Step 1: Create Use Case**

```bash
touch src/main/java/dev/bloco/wallet/hub/usecase/GetPortfolioSummaryUseCase.java
```

```java
package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.PortfolioRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Portfolio;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public record GetPortfolioSummaryUseCase(
    PortfolioRepository portfolioRepository,
    WalletRepository walletRepository
) {
    public Portfolio getPortfolioSummary(UUID userId) {
        // Calculate total value across all wallets
        BigDecimal totalValue = walletRepository.findByUserId(userId)
            .stream()
            .map(wallet -> wallet.getBalance())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create or update portfolio
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
            .orElseGet(() -> new Portfolio(UUID.randomUUID(), userId));

        portfolio.updateTotalValue(totalValue);
        return portfolioRepository.save(portfolio);
    }
}
```

**Step 2: Create Test**

```bash
touch src/test/java/dev/bloco/wallet/hub/usecase/GetPortfolioSummaryUseCaseTest.java
```

```java
class GetPortfolioSummaryUseCaseTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private GetPortfolioSummaryUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCalculateTotalValueFromWallets() {
        UUID userId = UUID.randomUUID();
        Wallet wallet1 = new Wallet(UUID.randomUUID(), "Wallet 1", "", userId);
        wallet1.addFunds(BigDecimal.valueOf(100));

        Wallet wallet2 = new Wallet(UUID.randomUUID(), "Wallet 2", "", userId);
        wallet2.addFunds(BigDecimal.valueOf(200));

        when(walletRepository.findByUserId(userId)).thenReturn(List.of(wallet1, wallet2));
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());

        Portfolio result = useCase.getPortfolioSummary(userId);

        assertThat(result.getTotalValue()).isEqualByComparingTo(BigDecimal.valueOf(300));
        verify(portfolioRepository).save(any(Portfolio.class));
    }
}
```

**Step 3: Run Tests**

```bash
./mvnw test -Dtest=GetPortfolioSummaryUseCaseTest
```

---

### Task 3: Add Domain Event

**Example**: `PortfolioUpdatedEvent`

**Step 1: Create Event**

```bash
mkdir -p src/main/java/dev/bloco/wallet/hub/domain/event/portfolio
touch src/main/java/dev/bloco/wallet/hub/domain/event/portfolio/PortfolioUpdatedEvent.java
```

```java
package dev.bloco.wallet.hub.domain.event.portfolio;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import java.math.BigDecimal;
import java.util.UUID;

public class PortfolioUpdatedEvent extends DomainEvent {
    private final UUID portfolioId;
    private final UUID userId;
    private final BigDecimal totalValue;

    public PortfolioUpdatedEvent(UUID portfolioId, UUID userId, BigDecimal totalValue, UUID correlationId) {
        super(correlationId);
        this.portfolioId = portfolioId;
        this.userId = userId;
        this.totalValue = totalValue;
    }

    public UUID getPortfolioId() {
        return portfolioId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }
}
```

**Step 2: Register Event in Domain**

```java
public class Portfolio extends AggregateRoot {
    public void updateTotalValue(BigDecimal value) {
        this.totalValue = value;
        registerEvent(new PortfolioUpdatedEvent(getId(), userId, value, null));
    }
}
```

**Step 3: Publish in Use Case**

```java
public record GetPortfolioSummaryUseCase(
    PortfolioRepository portfolioRepository,
    WalletRepository walletRepository,
    DomainEventPublisher eventPublisher
) {
    public Portfolio getPortfolioSummary(UUID userId) {
        // ... calculate total value ...

        portfolio.updateTotalValue(totalValue);
        Portfolio saved = portfolioRepository.save(portfolio);

        // Publish events
        saved.getDomainEvents().forEach(eventPublisher::publish);
        saved.clearEvents();

        return saved;
    }
}
```

---

### Task 4: Add Event Consumer

**Example**: `PortfolioUpdatedEventConsumer`

**Step 1: Add Kafka Binding**

Edit `src/main/resources/application.yml`:

```yaml
spring:
  cloud:
    stream:
      bindings:
        portfolioUpdatedEventProducer-out-0:
          destination: portfolio-updated-topic
```

**Step 2: Update Event Producer Interface**

```java
public interface EventProducer {
    void produceWalletCreatedEvent(WalletCreatedEvent event);
    void produceFundsAddedEvent(FundsAddedEvent event);
    void produceFundsWithdrawnEvent(FundsWithdrawnEvent event);
    void produceFundsTransferredEvent(FundsTransferredEvent event);
    void producePortfolioUpdatedEvent(PortfolioUpdatedEvent event);  // NEW
}
```

**Step 3: Implement in KafkaEventProducer**

```java
@Override
public void producePortfolioUpdatedEvent(PortfolioUpdatedEvent event) {
    saveEventToOutbox("portfolioUpdatedEventProducer", event);
}
```

**Step 4: Create Consumer**

```bash
touch src/main/java/dev/bloco/wallet/hub/infra/adapter/event/consumer/PortfolioUpdatedEventConsumer.java
```

```java
@Component
@Slf4j
public class PortfolioUpdatedEventConsumer {

    @Bean
    public Consumer<Message<CloudEvent>> portfolioUpdatedEventConsumerFunction() {
        return message -> {
            CloudEvent cloudEvent = message.getPayload();
            String payload = new String(cloudEvent.getData().toBytes());
            log.info("Portfolio updated: {}", payload);

            // Process event (e.g., send notification, update analytics)
        };
    }
}
```

**Step 5: Test with Test Binder**

```java
@SpringBootTest
class PortfolioUpdatedEventConsumerTest {

    @Autowired
    private InputDestination inputDestination;

    @Test
    void shouldConsumePortfolioUpdatedEvent() {
        PortfolioUpdatedEvent event = new PortfolioUpdatedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(5000),
            UUID.randomUUID()
        );

        CloudEvent cloudEvent = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType("portfolioUpdatedEventProducer")
            .withSource(URI.create("/test"))
            .withDataContentType("application/json")
            .withData(objectMapper.writeValueAsBytes(event))
            .build();

        inputDestination.send(MessageBuilder.withPayload(cloudEvent).build(), "portfolio-updated-topic");

        // Verify processing (check logs or side effects)
    }
}
```

---

## Architecture Overview

### Hexagonal Architecture in 60 Seconds

```
┌──────────────────────────────────────────────────────┐
│ INFRASTRUCTURE (infra/)                              │
│   Kafka, JPA, Redis, Security, Tracing               │
│                                                      │
│   ┌──────────────────────────────────────────────┐   │
│   │ USE CASES (usecase/)                         │   │
│   │   CreateWallet, AddFunds, etc.               │   │
│   │                                              │   │
│   │   ┌─────────────────────────────────────┐    │   │
│   │   │ DOMAIN (domain/)                    │    │   │
│   │   │   Wallet, User, Transaction         │    │   │
│   │   │   Pure business logic, NO Spring    │    │   │
│   │   └─────────────────────────────────────┘    │   │
│   └──────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────┘
```

**Key Principles**:
1. **Domain** has ZERO external dependencies
2. **Use Cases** orchestrate domain logic
3. **Infrastructure** adapts external systems to domain interfaces

### Request Flow Example

**User creates wallet**:

```
HTTP Request
    ↓
[REST Controller] (infrastructure)
    ↓
[CreateWalletUseCase] (use case)
    ↓
[Wallet.create()] (domain)
    ↓ registers event
[WalletRepository.save()] (domain interface, infrastructure implementation)
    ↓
[JPA] saves to database
    ↓
[EventPublisher.publish()] (domain interface, infrastructure implementation)
    ↓
[Outbox] saves event
    ↓
[OutboxWorker] processes event (scheduled)
    ↓
[Kafka] publishes CloudEvent
    ↓
[Consumer] processes event
    ↓
[State Machine] transitions state
```

---

## Code Patterns

### Pattern 1: Factory Method for Entities

**Always use factory methods** to ensure events are registered:

```java
// ✅ GOOD
Wallet wallet = Wallet.create(id, name, description);

// ❌ BAD
Wallet wallet = new Wallet(id, name, description);
```

### Pattern 2: Use Case as Record

**Use Java records** for immutability and conciseness:

```java
@Service
public record CreateWalletUseCase(
    WalletRepository walletRepository,
    DomainEventPublisher eventPublisher
) {
    public Wallet createWallet(UUID userId, String correlationId) {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "Default", "");
        walletRepository.save(wallet);
        wallet.getDomainEvents().forEach(eventPublisher::publish);
        wallet.clearEvents();
        return wallet;
    }
}
```

### Pattern 3: Domain Events

**Register in domain, publish in use case**:

```java
// Domain
public void activate() {
    WalletStatus oldStatus = this.status;
    this.status = WalletStatus.ACTIVE;
    registerEvent(new WalletStatusChangedEvent(getId(), oldStatus, this.status, "Activated", correlationId));
}

// Use case
wallet.activate();
walletRepository.save(wallet);
wallet.getDomainEvents().forEach(eventPublisher::publish);
wallet.clearEvents();
```

### Pattern 4: Repository Pattern

**Interface in domain, implementation in infrastructure**:

```java
// domain/gateway/WalletRepository.java (PORT)
public interface WalletRepository {
    Wallet save(Wallet wallet);
    Optional<Wallet> findById(UUID id);
    List<Wallet> findByUserId(UUID userId);
}

// infra/provider/data/repository/JpaWalletRepository.java (ADAPTER)
@Component
public class JpaWalletRepository implements WalletRepository {
    private final SpringDataWalletRepository jpaRepo;
    private final WalletMapper mapper;

    @Override
    public Wallet save(Wallet wallet) {
        WalletEntity entity = mapper.toEntity(wallet);
        WalletEntity saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);
    }
}
```

### Pattern 5: Value Objects

**Use Java records for immutability**:

```java
public record TransactionHash(String value) {
    public TransactionHash {
        if (value == null || !value.matches("^0x[a-fA-F0-9]{64}$")) {
            throw new IllegalArgumentException("Invalid transaction hash");
        }
    }
}
```

---

## Testing

### Unit Tests (Domain)

**Location**: `src/test/java/dev/bloco/wallet/hub/domain/`

**Characteristics**:
- No Spring context
- Fast (milliseconds)
- Test pure business logic

**Example**:

```java
class WalletTest {
    @Test
    void shouldAddFundsWhenAmountIsPositive() {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");

        wallet.addFunds(BigDecimal.valueOf(100));

        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void shouldThrowExceptionWhenAddingNegativeAmount() {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");

        assertThatThrownBy(() -> wallet.addFunds(BigDecimal.valueOf(-10)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

**Run**:

```bash
./mvnw test -Dtest=WalletTest
```

### Integration Tests (Use Cases)

**Location**: `src/test/java/dev/bloco/wallet/hub/usecase/`

**Characteristics**:
- Spring context loaded
- Test binder (no real Kafka)
- Slower (seconds)

**Example**:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.stream.defaultBinder=test"
})
class CreateWalletUseCaseIntegrationTest {

    @Autowired
    private CreateWalletUseCase useCase;

    @Autowired
    private WalletRepository repository;

    @Test
    void shouldCreateWalletAndPublishEvent() {
        UUID userId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();

        Wallet wallet = useCase.createWallet(userId, correlationId);

        assertThat(wallet.getId()).isNotNull();
        assertThat(repository.findById(wallet.getId())).isPresent();
    }
}
```

**Run**:

```bash
./mvnw test -Dtest=CreateWalletUseCaseIntegrationTest
```

### Messaging Tests

**Use Test Binder**:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.stream.defaultBinder=test"
})
class WalletCreatedEventConsumerTest {

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private OutputDestination outputDestination;

    @Test
    void shouldProcessWalletCreatedEvent() {
        WalletCreatedEvent event = new WalletCreatedEvent(UUID.randomUUID(), UUID.randomUUID());

        CloudEvent cloudEvent = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType("walletCreatedEventProducer")
            .withSource(URI.create("/test"))
            .withDataContentType("application/json")
            .withData(objectMapper.writeValueAsBytes(event))
            .build();

        inputDestination.send(MessageBuilder.withPayload(cloudEvent).build(), "wallet-created-topic");

        // Verify processing
        Message<byte[]> received = outputDestination.receive(1000, "output-topic");
        assertThat(received).isNotNull();
    }
}
```

### Run All Tests

```bash
# All tests
./mvnw test

# Specific package
./mvnw test -Dtest="dev.bloco.wallet.hub.domain.*"

# Specific class
./mvnw test -Dtest=WalletTest

# Specific method
./mvnw test -Dtest=WalletTest#shouldAddFundsWhenAmountIsPositive
```

---

## Troubleshooting

### Issue 1: Bean Definition Override Exception

**Error**:

```
BeanDefinitionOverrideException: Invalid bean definition 'fundsAddedEventConsumer'
```

**Cause**: Both `@Component` and `@Bean` on same class.

**Solution**:

```java
// Remove @Component
@Slf4j
public class FundsAddedEventConsumer {

    @Bean  // Keep only this
    public Consumer<Message<CloudEvent>> fundsAddedEventConsumerFunction() {
        // ...
    }
}
```

**Or enable override** (not recommended for production):

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
```

---

### Issue 2: MapStruct Implementation Not Generated

**Error**:

```
No qualifying bean of type 'WalletMapperImpl'
```

**Solution**:

```bash
# Rebuild to generate MapStruct implementations
./mvnw clean compile
```

**Or configure IDE**:
- IntelliJ: Settings → Build → Compiler → Annotation Processors → Enable

---

### Issue 3: H2 Database File Locked

**Error**:

```
Database may be already in use: "Locked by another process"
```

**Solution**:

```bash
# Kill Java processes
pkill -f java

# Or delete database file
rm -rf ./db/wallet*

# Restart application
./mvnw spring-boot:run
```

---

### Issue 4: Kafka Connection Refused

**Error**:

```
org.apache.kafka.common.errors.TimeoutException: Failed to update metadata
```

**Solution 1**: Use test binder (no Kafka needed)

```yaml
spring:
  cloud:
    stream:
      defaultBinder: test
```

**Solution 2**: Start Kafka via Docker

```bash
docker-compose up -d kafka zookeeper
```

---

### Issue 5: Port Already in Use

**Error**:

```
Web server failed to start. Port 8080 was already in use.
```

**Solution**:

```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port
./mvnw spring-boot:run -Dserver.port=8081
```

---

## Next Steps

1. **Read Full Documentation**:
   - [TECHNICAL_ARCHITECTURE.md](./TECHNICAL_ARCHITECTURE.md) - Complete system overview
   - [DOMAIN_MODEL_GUIDE.md](./DOMAIN_MODEL_GUIDE.md) - Deep dive into domain

2. **Explore Codebase**:
   - Start with `domain/model/Wallet.java` (core aggregate)
   - Follow to `usecase/CreateWalletUseCase.java` (orchestration)
   - Then `infra/adapter/event/producer/KafkaEventProducer.java` (infrastructure)

3. **Try Examples**:
   - Create a wallet
   - Add funds
   - Transfer funds
   - Check events in H2 console

4. **Join Team**:
   - Review pull requests
   - Attend architecture discussions
   - Contribute to documentation

---

## Useful Commands Cheat Sheet

```bash
# Build
./mvnw clean package -DskipTests      # Fast build
./mvnw clean verify                    # With tests

# Run
./mvnw spring-boot:run                 # Dev mode (H2)
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod  # Prod profile

# Test
./mvnw test                            # All tests
./mvnw test -Dtest=WalletTest         # Specific class
./mvnw test -Dtest="*.usecase.*"      # Package pattern

# Database
docker-compose up -d postgres          # Start PostgreSQL
docker-compose down                    # Stop all services

# Native Image
./mvnw native:compile -Pnative        # Build native executable

# SBOM
./mvnw cyclonedx:makeAggregateBom     # Generate SBOM
```

---

**Document Version**: 1.0
**Author**: Developer Experience Team
**Maintained By**: Development Team
