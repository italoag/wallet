# Wallet Hub - Complete Documentation

## Executive Summary

Wallet Hub is an enterprise-grade, event-driven cryptocurrency wallet management service built with Spring Boot 3.5.6 and Spring Cloud Stream. The service implements hexagonal architecture (ports and adapters pattern) with strict separation between domain logic, application services, and infrastructure concerns.

### Key Highlights

- **33 Use Cases**: Comprehensive wallet, user, transaction, and network management
- **40+ Domain Events**: Full event sourcing capabilities
- **Saga Pattern**: Distributed transaction coordination with Spring State Machine
- **Outbox Pattern**: Reliable event delivery guarantees
- **Multi-persistence**: JPA, R2DBC, Redis, MongoDB support
- **Production-ready**: Observability, resilience, security built-in

### Quick Navigation

| Category | Documentation |
|----------|--------------|
| Architecture | [Overview](architecture/OVERVIEW.md) • [Event-Driven](architecture/EVENT_DRIVEN.md) • [Saga Pattern](architecture/SAGA.md) |
| Domain | [Models](modules/DOMAIN.md) • [Events](modules/DOMAIN_EVENTS.md) • [Gateways](modules/DOMAIN_GATEWAYS.md) |
| Application | [Use Cases](modules/USECASE.md) • [API Reference](api/API_REFERENCE.md) |
| Infrastructure | [Adapters](modules/INFRASTRUCTURE.md) • [Repositories](modules/REPOSITORIES.md) • [Mappers](modules/MAPPERS.md) |
| Guides | [Setup](guides/SETUP.md) • [Development](guides/DEVELOPMENT.md) • [Testing](guides/TESTING.md) • [Deployment](guides/DEPLOYMENT.md) |

## Quick Start

```bash
# Build the project
./mvnw -DskipTests package

# Run with H2 in-memory database
./mvnw spring-boot:run

# Run with PostgreSQL
docker compose up -d
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wallet ./mvnw spring-boot:run
```

## Project Structure

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

## Core Concepts

### Hexagonal Architecture

Dependencies flow inward: `Infrastructure → Use Case → Domain`

- **Domain Layer**: Pure business logic with no framework dependencies
- **Use Case Layer**: Application services orchestrating domain logic
- **Infrastructure Layer**: Adapters for external systems (Kafka, databases, etc.)

### Event-Driven Design

All state changes produce domain events that are:
- Published to Kafka with CloudEvents specification
- Stored in outbox table for reliable delivery
- Consumed asynchronously by event handlers
- Used for saga orchestration and eventual consistency

### Saga Pattern

Distributed transactions coordinated via Spring State Machine:
```
INITIAL → WALLET_CREATED → FUNDS_ADDED → FUNDS_WITHDRAWN → FUNDS_TRANSFERRED → COMPLETED/FAILED
```

## Technology Stack

- **Java 24** with Maven
- **Spring Boot 3.5.6** and Spring Cloud 2025.0.0
- **Apache Kafka** with Spring Cloud Stream
- **JPA/Hibernate** (H2/PostgreSQL)
- **Spring State Machine 4.0.1** for saga orchestration
- **MapStruct 1.6.3** for object mapping
- **Micrometer** for observability
- **Resilience4j** for resilience patterns

## Documentation Sections

### Architecture
- [Architecture Overview](architecture/OVERVIEW.md) - Hexagonal architecture, patterns, and design principles
- [Event-Driven Architecture](architecture/EVENT_DRIVEN.md) - Event sourcing, outbox pattern, CloudEvents
- [Saga Pattern](architecture/SAGA.md) - Distributed transaction coordination

### Domain
- [Domain Models](modules/DOMAIN.md) - Core business entities and value objects
- [Domain Events](modules/DOMAIN_EVENTS.md) - 40+ domain events catalog
- [Domain Gateways](modules/DOMAIN_GATEWAYS.md) - Repository and service interfaces

### Application
- [Use Cases](modules/USECASE.md) - 33 application services
- [API Reference](api/API_REFERENCE.md) - Complete API documentation

### Infrastructure
- [Infrastructure Adapters](modules/INFRASTRUCTURE.md) - Kafka, JPA, Redis, MongoDB adapters
- [Repositories](modules/REPOSITORIES.md) - Data access implementations
- [Mappers](modules/MAPPERS.md) - Domain-to-entity mapping

### Guides
- [Setup Guide](guides/SETUP.md) - Installation and configuration
- [Development Guide](guides/DEVELOPMENT.md) - Development workflow and best practices
- [Testing Guide](guides/TESTING.md) - Unit, integration, and e2e testing
- [Deployment Guide](guides/DEPLOYMENT.md) - Docker, Kubernetes, cloud deployment

## Key Features

### Wallet Management
- Multi-token wallet creation and lifecycle management
- Address generation and import
- Token addition/removal
- Wallet recovery mechanisms

### Transaction Management
- Transaction creation and confirmation
- Fee estimation
- Transaction status tracking
- Multi-signature support

### User Management
- User registration and authentication
- Profile management
- Two-factor authentication
- Session management

### Portfolio Management
- Real-time balance tracking
- Multi-token portfolio summary
- Address-level balance queries

### Network Management
- Multi-chain support (Ethereum, Polygon, BSC, etc.)
- Network configuration from Chainlist
- RPC endpoint management

## Contributing

See [Development Guide](guides/DEVELOPMENT.md) for contribution guidelines.

## License

[Add license information]

## Support

For issues and questions, please open an issue in the project repository.
