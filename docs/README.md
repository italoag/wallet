# Wallet Hub Documentation

Welcome to the Wallet Hub comprehensive documentation. This directory contains detailed technical documentation for developers, architects, and operators.

## Quick Navigation

### 📚 Start Here
- **[Main Documentation Index](DOCUMENTATION.md)** - Complete overview and navigation guide

### 🏗️ Architecture (83KB)
Understand the system design and patterns:
- **[Architecture Overview](architecture/OVERVIEW.md)** (13KB) - Hexagonal architecture, layers, and design principles
- **[Event-Driven Architecture](architecture/EVENT_DRIVEN.md)** (50KB) - Kafka integration, CloudEvents, and outbox pattern
- **[Saga Pattern](architecture/SAGA.md)** (20KB) - Distributed transaction coordination with Spring State Machine

### 🎯 Domain & Application (127KB)
Core business logic and use cases:
- **[Domain Layer](modules/DOMAIN.md)** (18KB) - 10 domain models, 40+ events, business rules, and value objects
- **[Use Cases](modules/USECASE.md)** (54KB) - All 33 application services with examples
- **[Infrastructure](modules/INFRASTRUCTURE.md)** (55KB) - Event adapters, JPA repositories, and mappers

### 🚀 Getting Started (57KB)
Setup and development guides:
- **[Setup Guide](guides/SETUP.md)** (20KB) - Installation, configuration, and running the application
- **[Testing Guide](guides/TESTING.md)** (37KB) - Testing strategy, examples, and CI/CD integration

## Documentation Statistics

- **Total Documentation**: ~295KB across 10 markdown files
- **Domain Models**: 10 core aggregates
- **Domain Events**: 40+ event types
- **Use Cases**: 33 application services
- **Code Examples**: 100+ practical examples
- **Diagrams**: Architecture, state machines, and flow diagrams

## Documentation Structure

```
docs/
├── README.md                       # This file
├── DOCUMENTATION.md                # Main index
├── architecture/
│   ├── OVERVIEW.md                 # Hexagonal architecture
│   ├── EVENT_DRIVEN.md             # Kafka and events
│   └── SAGA.md                     # Saga pattern
├── modules/
│   ├── DOMAIN.md                   # Domain layer
│   ├── USECASE.md                  # Application layer
│   └── INFRASTRUCTURE.md           # Infrastructure layer
└── guides/
    ├── SETUP.md                    # Installation guide
    └── TESTING.md                  # Testing guide
```

## Key Concepts

### Hexagonal Architecture
The system follows hexagonal architecture (ports & adapters) with three layers:
- **Domain**: Pure business logic (no framework dependencies)
- **Use Case**: Application orchestration
- **Infrastructure**: Adapters for external systems

### Event-Driven Design
All state changes produce domain events:
- Published to Kafka with CloudEvents format
- Reliable delivery via outbox pattern
- Asynchronous processing by consumers

### Saga Pattern
Distributed transactions coordinated via Spring State Machine:
- INITIAL → WALLET_CREATED → FUNDS_ADDED → ... → COMPLETED
- Compensation on failure
- JPA-persisted state

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 24 |
| Framework | Spring Boot 3.5.6 |
| Messaging | Apache Kafka + Spring Cloud Stream |
| Persistence | JPA (H2/PostgreSQL), R2DBC, Redis, MongoDB |
| State Machine | Spring State Machine 4.0.1 |
| Mapping | MapStruct 1.6.3 |
| Observability | Micrometer + Brave |
| Resilience | Resilience4j |

## For Different Audiences

### 👨‍💻 New Developers
Start with:
1. [Setup Guide](guides/SETUP.md) - Get the project running
2. [Architecture Overview](architecture/OVERVIEW.md) - Understand the system structure
3. [Domain Layer](modules/DOMAIN.md) - Learn the business models
4. [Testing Guide](guides/TESTING.md) - Write your first test

### 🏛️ Architects
Focus on:
1. [Architecture Overview](architecture/OVERVIEW.md) - Design principles and patterns
2. [Event-Driven Architecture](architecture/EVENT_DRIVEN.md) - Messaging architecture
3. [Saga Pattern](architecture/SAGA.md) - Distributed transactions
4. [Infrastructure](modules/INFRASTRUCTURE.md) - Technical implementation

### 🧪 QA Engineers
Review:
1. [Testing Guide](guides/TESTING.md) - Testing strategy and examples
2. [Use Cases](modules/USECASE.md) - What to test
3. [Setup Guide](guides/SETUP.md) - Environment setup

### 🚀 DevOps
Check:
1. [Setup Guide](guides/SETUP.md) - Deployment options
2. [Architecture Overview](architecture/OVERVIEW.md) - Infrastructure needs
3. [Event-Driven Architecture](architecture/EVENT_DRIVEN.md) - Kafka configuration

## Quick Reference

### Domain Models
- Wallet, User, Transaction, Address, Token, Network, Contract, Vault, Store, TokenBalance

### Use Case Categories
- Wallet Management (8 use cases)
- User Management (6 use cases)
- Transaction Management (7 use cases)
- Address Management (5 use cases)
- Token/Network Management (5 use cases)
- Portfolio & Balance (2 use cases)

### Event Categories
- Wallet Events (12 types)
- User Events (4 types)
- Transaction Events (3 types)
- Address, Token, Network, Contract, Vault, Store Events (15 types)

### Kafka Topics
- wallet-created-topic
- funds-added-topic
- funds-withdrawn-topic
- funds-transferred-topic

## Contributing to Documentation

When updating documentation:
1. Follow the existing structure and formatting
2. Include code examples for complex concepts
3. Update the main index when adding new sections
4. Keep examples aligned with actual code
5. Add diagrams for architectural concepts

## Documentation Maintenance

This documentation was generated on **2025-12-11** and reflects the current state of the codebase. As the project evolves:
- Update docs when adding new features
- Keep code examples synchronized
- Review and update quarterly
- Archive deprecated information

## Support

For questions or issues:
1. Check the relevant documentation section
2. Review code examples and patterns
3. Consult the troubleshooting sections
4. Open an issue in the project repository

## License

This documentation is part of the Wallet Hub project and follows the same license as the codebase.

---

**Happy coding!** 🎉
