# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Wallet Hub** is an event-driven cryptocurrency wallet management service built with Spring Boot 3.5.6 and Spring Cloud Stream. The service follows **Hexagonal Architecture (Ports & Adapters)** with strict separation between domain logic, use cases, and infrastructure concerns.

## Tech Stack

- **Language:** Java 24 (via Maven Wrapper)
- **Frameworks:** Spring Boot 3.5.6, Spring Cloud 2025.0.0
- **Messaging:** Apache Kafka with Spring Cloud Stream, CloudEvents specification
- **Persistence:** JPA (H2 default/PostgreSQL), R2DBC, Redis (reactive), MongoDB (reactive)
- **State Management:** Spring State Machine 4.0.1 (saga orchestration with JPA persistence)
- **Mapping:** MapStruct 1.6.3
- **Observability:** Micrometer (Prometheus/OTLP), Tracing (Brave)
- **Resilience:** Resilience4j (circuit breaker, retry)

## Architecture

### Hexagonal Architecture (Ports & Adapters)

```
src/main/java/dev/bloco/wallet/hub/
├── domain/              # Pure business logic (no Spring dependencies)
│   ├── event/          # Domain events (WalletCreated, FundsAdded, etc.)
│   ├── gateway/        # Port interfaces (repositories, external services)
│   └── model/          # Domain models (Wallet, User, Transaction, etc.)
├── usecase/            # Application layer (orchestrates domain logic)
│   └── *UseCase.java   # CreateWallet, AddFunds, TransferFunds, etc.
└── infra/              # Infrastructure layer (adapters)
    ├── adapter/
    │   └── event/
    │       ├── producer/   # KafkaEventProducer, EventProducer interface
    │       └── consumer/   # Event consumers (4 types)
    └── provider/
        ├── data/
        │   ├── config/     # SagaStateMachineConfig, state/event enums
        │   ├── entity/     # JPA entities (WalletEntity, UserEntity, etc.)
        │   └── repository/ # Spring Data JPA repositories
        └── mapper/         # MapStruct mappers (domain ↔ entity)
```

### Key Patterns

- **Event-Driven Architecture:** Domain events published to Kafka with CloudEvents format
- **Outbox Pattern:** Reliable event publishing via transactional outbox table
- **Saga Pattern:** Distributed transaction coordination using Spring State Machine
- **CQRS/Event Sourcing:** Events as primary data model
- **Repository Pattern:** Data access abstraction via gateway interfaces

## Common Development Commands

### Build & Run

```bash
# Build (skip tests)
./mvnw -DskipTests package

# Run application (H2 file DB by default)
./mvnw spring-boot:run

# Clean build artifacts
./mvnw clean
```

### Testing

```bash
# Run all tests (requires JDK 24+)
./mvnw test

# Run single test class
./mvnw -Dtest=FullyQualifiedClassName test

# Run tests on older JDK (override compiler release)
./mvnw -Dmaven.compiler.release=8 -Dtest=YourTestClass test
```

### Docker & Infrastructure

```bash
# Start MongoDB, PostgreSQL, Redis (no exposed ports by default)
docker compose up -d

# Stop infrastructure
docker compose down
```

### Advanced Build

```bash
# Build Docker image (JVM)
./mvnw spring-boot:build-image

# Build native executable (requires GraalVM)
./mvnw native:compile

# Generate SBOM (CycloneDX)
./mvnw cyclonedx:makeAggregateBom
```

## Configuration

### Default Settings (application.yml)

- **App Name:** `wallet-hub`
- **Database:** H2 file at `./db/wallet` (JPA with `ddl-auto=update`)
- **H2 Console:** http://localhost:8080/h2-console (enabled in dev)
- **Kafka Broker:** `localhost:9092`

### Kafka Topic Bindings

```yaml
walletCreatedEventProducer-out-0     → wallet-created-topic
fundsAddedEventProducer-out-0        → funds-added-topic
fundsWithdrawnEventProducer-out-0    → funds-withdrawn-topic
fundsTransferredEventProducer-out-0  → funds-transferred-topic
```

### Environment Overrides

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wallet
SPRING_DATASOURCE_USERNAME=myuser
SPRING_DATASOURCE_PASSWORD=secret
SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS=localhost:9092
SPRING_CLOUD_STREAM_DEFAULTBINDER=test  # Use test binder (no Kafka needed)
```

## Domain Models

Core business entities (in `domain/model/`):

- **Wallet:** Multi-token wallet with addresses and status
- **User:** User credentials and profile
- **Address:** Blockchain addresses with status tracking
- **Token:** ERC20 token metadata and balances
- **Transaction:** Blockchain transaction state tracking
- **Network:** Blockchain networks (from Chainlist)
- **Portfolio:** Aggregated balance summary
- **Contract:** Smart contract interactions
- **Vault:** Secure storage
- **Store:** Generic data storage

## Use Cases (20+ Application Services)

Located in `usecase/`:

- **Wallet:** CreateWallet, UpdateWallet, RecoverWallet, ActivateWallet
- **User:** CreateUser, UpdateUserProfile, ChangePassword, DeactivateUser
- **Address:** CreateAddress, UpdateAddressStatus, ImportAddress, ListAddressesByWallet
- **Funds:** AddFunds, WithdrawFunds, TransferFunds, CreateTransaction
- **Portfolio:** GetAddressBalance, GetTokenBalance, GetPortfolioSummary
- **Network:** ListNetworks, ListSupportedTokens
- **Transaction:** EstimateTransactionFee, ConfirmTransaction

## Event-Driven Messaging

### Event Producers

Located in `infra/adapter/event/producer/`:

- `EventProducer` interface with 4 methods
- `KafkaEventProducer` implementation
- `OutboxEventPublisher` for reliable delivery

### Event Consumers

Located in `infra/adapter/event/consumer/`:

- `WalletCreatedEventConsumer`
- `FundsAddedEventConsumer`
- `FundsWithdrawnEventConsumer`
- `FundsTransferredEventConsumer`

### CloudEvents

Use `CloudEventUtils` (in `infra/util/`) to create CloudEvent headers when publishing events.

## Testing Guidelines

### Messaging Tests

- Use `spring-cloud-stream-test-binder` (no real Kafka required)
- Set `spring.cloud.stream.defaultBinder=test` in test properties
- Use `InputDestination`/`OutputDestination` to send/receive messages

### Persistence Tests

- Use `@DataJpaTest` for JPA slices (H2 in-memory)
- Avoid full `@SpringBootTest` until bean duplication issues are resolved
- Use `@Import` to scope only necessary components

### Known Issue

`FundsAddedEventConsumer` has both `@Component` and `@Bean` annotations, causing `BeanDefinitionOverrideException`. Workarounds:
1. Remove `@Component`, keep only `@Bean` in `@Configuration`
2. Rename `@Bean`/binding to avoid name clash
3. Enable bean overriding in test profile (not recommended for production)

## Code Conventions

### Dependency Injection

- Constructor injection (preferred)
- Avoid field injection

### Domain Design

- Domain and events should be immutable (use Java records where appropriate)
- Never expose JPA entities; use DTOs and MapStruct for mapping
- Domain layer must have **no Spring dependencies**

### Validation & Error Handling

- Use Bean Validation for input validation
- Use `@ControllerAdvice` for centralized exception handling (if exposing REST APIs)

### Logging

- Use SLF4J with parameterized messages
- Example: `log.info("Processing wallet: {}", walletId)`

### Configuration

- Externalize configuration via `application.yml`
- Use `@ConfigurationProperties` or `@Value` (never hardcode)

### Outbox & Saga

- **Outbox Pattern:** Persist events in outbox table within same transaction as domain changes
- **Saga:** Use Spring State Machine with JPA persistence (see `SagaStateMachineConfig`)
- States: `INITIAL → WALLET_CREATED → FUNDS_ADDED → FUNDS_WITHDRAWN → FUNDS_TRANSFERRED → COMPLETED/FAILED`
- Ensure idempotency in consumers and publishers

### Reactive vs Blocking

- **Reactive:** WebFlux, Redis, MongoDB
- **Blocking:** JPA, Kafka (template-based)
- Never execute blocking calls on event-loop threads; use `boundedElastic()` scheduler when mixing paradigms

## Important Architectural Constraints

1. **Domain Purity:** The `domain/` package must remain free of framework dependencies (no Spring, no JPA annotations)
2. **Dependency Direction:** Dependencies flow inward: `infra → usecase → domain` (never reverse)
3. **Event Sourcing:** All state changes should produce domain events
4. **Transactional Consistency:** Use outbox pattern for events that require transactional guarantees
5. **Idempotency:** All event consumers must be idempotent

## Spring AI Integration

Multiple Spring AI starters are present in dependencies (Anthropic, OpenAI, Bedrock, Ollama, etc.). Usage is optional and not yet documented in the codebase.

## Development Environment Requirements

- **JDK:** Java 24+ (GraalVM CE 25 recommended)
- **Maven:** 3.x (use `./mvnw` wrapper)
- **Optional:** Docker (for MongoDB, PostgreSQL, Redis)
- **Optional:** Kafka broker at `localhost:9092` (or use test binder)

## Entry Point

Main application class:
```
dev.bloco.wallet.hub.WalletHubApplication
```

Scans JPA entities from:
- `dev.bloco.wallet.hub.infra.provider.data.entity`
- `dev.bloco.wallet.hub.infra.provider.data`
- `org.springframework.statemachine.data.jpa`

## References

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Cloud Stream](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/)
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/reference/)
- [CloudEvents](https://cloudevents.io/)
- [Spring State Machine](https://docs.spring.io/spring-statemachine/docs/current/reference/)
- [Resilience4j](https://resilience4j.readme.io/)
- [MapStruct](https://mapstruct.org/)
- [Micrometer](https://micrometer.io/)
