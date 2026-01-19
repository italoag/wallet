# Wallet Hub - Complete Documentation Index

**Version**: 0.0.1-SNAPSHOT
**Last Updated**: 2026-01-12
**Spring Boot**: 3.5.5 | **Java**: 24

---

## Welcome

Welcome to the **Wallet Hub** comprehensive documentation. This is an event-driven cryptocurrency wallet management service built with **Spring Boot**, following **Hexagonal Architecture**, **Event-Driven Design**, and **Domain-Driven Design** principles.

---

## 🚀 Quick Start (Start Here!)

### New to the Project?

**Begin with**: [Developer Quick Start Guide](./DEVELOPER_QUICKSTART.md) (25KB)

This guide gets you from zero to running in minutes:
- Prerequisites & setup
- Building & running the application
- Common development tasks
- Code patterns & examples
- Testing basics
- Troubleshooting

**Time to complete**: 15-30 minutes

---

## 📚 Core Documentation

### 1. Technical Architecture (60KB)

**[TECHNICAL_ARCHITECTURE.md](./TECHNICAL_ARCHITECTURE.md)**

**Complete system overview covering**:
- System overview & capabilities
- Architectural patterns (Hexagonal, Event-Driven, Saga, Outbox)
- Technology stack (Spring Boot, Kafka, JPA, State Machine)
- Domain model overview
- Use cases & application layer (34 services)
- Event-driven architecture details
- Infrastructure layer (adapters, providers)
- Data persistence (JPA, R2DBC, Redis, MongoDB)
- State management & sagas
- Observability & distributed tracing
- Security (OAuth2, 2FA, Vault)
- Testing strategy
- Configuration & deployment

**Read if**: You want a comprehensive understanding of the entire system

**Time to read**: 60-90 minutes

---

### 2. Domain Model Guide (30KB)

**[DOMAIN_MODEL_GUIDE.md](./DOMAIN_MODEL_GUIDE.md)**

**Deep dive into business logic**:
- Domain-Driven Design principles
- Core concepts (Entities, Value Objects, Aggregates)
- Base classes (Entity, AggregateRoot)
- 3 main aggregate roots:
  - **Wallet**: Multi-token wallet management
  - **User**: Identity & authentication
  - **Transaction**: Blockchain transaction tracking
- Value objects (TransactionHash, PublicKey, etc.)
- Domain events (36 events across 9 categories)
- Business rules & invariants
- Entity relationships
- Best practices

**Read if**: You need to understand or modify business logic

**Time to read**: 45-60 minutes

---

### 3. Event-Driven Architecture Guide (24KB)

**[EVENT_DRIVEN_GUIDE.md](./EVENT_DRIVEN_GUIDE.md)**

**Complete event architecture documentation**:
- Event types & categories
- Outbox pattern implementation
- CloudEvents 1.0 integration
- Complete event flow (with diagrams)
- Saga pattern for distributed transactions
- Distributed tracing (W3C Trace Context)
- Event producers & consumers
- State machine configuration
- Best practices (idempotency, error handling)
- Monitoring & metrics
- Troubleshooting

**Read if**: You're working with events, Kafka, or distributed transactions

**Time to read**: 45-60 minutes

---

### 4. Developer Quick Start (26KB)

**[DEVELOPER_QUICKSTART.md](./DEVELOPER_QUICKSTART.md)**

**Hands-on development guide**:
- Prerequisites & IDE setup
- Getting started (clone, build, run)
- Development workflow
- Common tasks (with step-by-step examples):
  - Add new domain entity
  - Add new use case
  - Add domain event
  - Add event consumer
- Architecture overview in 60 seconds
- Code patterns
- Testing (unit, integration, messaging)
- Troubleshooting (5 common issues)
- Command cheat sheet

**Read if**: You're actively developing features

**Time to complete**: 30-45 minutes (with hands-on)

---

## 📖 Additional Documentation

### Legacy/Supplementary Docs

These documents exist from previous documentation efforts and may contain additional context:

| Document | Size | Focus | Status |
|----------|------|-------|--------|
| [CONSTITUTION.md](./CONSTITUTION.md) | 10KB | Project principles | Supplementary |
| [DATABASE.md](./DATABASE.md) | 2KB | Database setup | Supplementary |
| [TRACING.md](./TRACING.md) | 17KB | Observability details | Supplementary |
| [task.md](./task.md) | 15KB | Task tracking | Supplementary |

**Note**: The 4 core documents above (TECHNICAL_ARCHITECTURE, DOMAIN_MODEL_GUIDE, EVENT_DRIVEN_GUIDE, DEVELOPER_QUICKSTART) are the primary, authoritative documentation. Supplementary docs may contain older or overlapping information.

---

## 🎯 Documentation by Role

### For New Developers

**Path**: Quick Start → Technical Architecture → Domain Model

1. Start: [DEVELOPER_QUICKSTART.md](./DEVELOPER_QUICKSTART.md)
2. Next: [TECHNICAL_ARCHITECTURE.md](./TECHNICAL_ARCHITECTURE.md) (skim for overview)
3. Deep dive: [DOMAIN_MODEL_GUIDE.md](./DOMAIN_MODEL_GUIDE.md)
4. When working with events: [EVENT_DRIVEN_GUIDE.md](./EVENT_DRIVEN_GUIDE.md)

**Goal**: Productive within 1-2 days

---

### For Architects

**Path**: Technical Architecture → Event-Driven → Domain Model

1. Start: [TECHNICAL_ARCHITECTURE.md](./TECHNICAL_ARCHITECTURE.md) (complete read)
2. Next: [EVENT_DRIVEN_GUIDE.md](./EVENT_DRIVEN_GUIDE.md)
3. Deep dive: [DOMAIN_MODEL_GUIDE.md](./DOMAIN_MODEL_GUIDE.md)
4. Reference: [DEVELOPER_QUICKSTART.md](./DEVELOPER_QUICKSTART.md) (patterns section)

**Goal**: Complete system understanding in 3-4 hours

---

### For Backend Engineers

**Path**: Quick Start → Domain Model → Event-Driven

1. Start: [DEVELOPER_QUICKSTART.md](./DEVELOPER_QUICKSTART.md) (get running)
2. Next: [DOMAIN_MODEL_GUIDE.md](./DOMAIN_MODEL_GUIDE.md) (business logic)
3. Deep dive: [EVENT_DRIVEN_GUIDE.md](./EVENT_DRIVEN_GUIDE.md) (messaging)
4. Reference: [TECHNICAL_ARCHITECTURE.md](./TECHNICAL_ARCHITECTURE.md) (specific sections as needed)

**Goal**: Implement first feature within 1 day

---

### For QA Engineers

**Path**: Quick Start → Technical Architecture (Testing) → Use Cases

1. Start: [DEVELOPER_QUICKSTART.md](./DEVELOPER_QUICKSTART.md) (setup, testing section)
2. Next: [TECHNICAL_ARCHITECTURE.md](./TECHNICAL_ARCHITECTURE.md) (testing strategy section)
3. Reference: [DOMAIN_MODEL_GUIDE.md](./DOMAIN_MODEL_GUIDE.md) (business rules to test)
4. Reference: [EVENT_DRIVEN_GUIDE.md](./EVENT_DRIVEN_GUIDE.md) (event flows to test)

**Goal**: Write comprehensive tests within 1-2 days

---

### For DevOps

**Path**: Technical Architecture (Deployment) → Quick Start (Commands)

1. Start: [TECHNICAL_ARCHITECTURE.md](./TECHNICAL_ARCHITECTURE.md) (Configuration & Deployment)
2. Next: [DEVELOPER_QUICKSTART.md](./DEVELOPER_QUICKSTART.md) (troubleshooting, commands)
3. Reference: [EVENT_DRIVEN_GUIDE.md](./EVENT_DRIVEN_GUIDE.md) (monitoring & metrics)

**Goal**: Deploy to production environment

---

## 🔍 Find Information Fast

### I want to...

**Understand the system architecture**
→ [TECHNICAL_ARCHITECTURE.md](./TECHNICAL_ARCHITECTURE.md)

**Learn about business logic**
→ [DOMAIN_MODEL_GUIDE.md](./DOMAIN_MODEL_GUIDE.md)

**Work with events and Kafka**
→ [EVENT_DRIVEN_GUIDE.md](./EVENT_DRIVEN_GUIDE.md)

**Get started developing**
→ [DEVELOPER_QUICKSTART.md](./DEVELOPER_QUICKSTART.md)

**Add a new domain entity**
→ [DEVELOPER_QUICKSTART.md#task-1-add-new-domain-entity](./DEVELOPER_QUICKSTART.md#task-1-add-new-domain-entity)

**Add a new use case**
→ [DEVELOPER_QUICKSTART.md#task-2-add-new-use-case](./DEVELOPER_QUICKSTART.md#task-2-add-new-use-case)

**Add a domain event**
→ [DEVELOPER_QUICKSTART.md#task-3-add-domain-event](./DEVELOPER_QUICKSTART.md#task-3-add-domain-event)

**Understand Wallet aggregate**
→ [DOMAIN_MODEL_GUIDE.md#1-wallet](./DOMAIN_MODEL_GUIDE.md#1-wallet)

**Understand User aggregate**
→ [DOMAIN_MODEL_GUIDE.md#2-user](./DOMAIN_MODEL_GUIDE.md#2-user)

**Understand Transaction aggregate**
→ [DOMAIN_MODEL_GUIDE.md#3-transaction](./DOMAIN_MODEL_GUIDE.md#3-transaction)

**Learn about the Outbox pattern**
→ [EVENT_DRIVEN_GUIDE.md#outbox-pattern](./EVENT_DRIVEN_GUIDE.md#outbox-pattern)

**Learn about Saga pattern**
→ [EVENT_DRIVEN_GUIDE.md#saga-pattern](./EVENT_DRIVEN_GUIDE.md#saga-pattern)

**Configure Kafka topics**
→ [TECHNICAL_ARCHITECTURE.md#event-driven-architecture](./TECHNICAL_ARCHITECTURE.md#event-driven-architecture)

**Set up tracing**
→ [TECHNICAL_ARCHITECTURE.md#observability--tracing](./TECHNICAL_ARCHITECTURE.md#observability--tracing)

**Configure security**
→ [TECHNICAL_ARCHITECTURE.md#security](./TECHNICAL_ARCHITECTURE.md#security)

**Write tests**
→ [DEVELOPER_QUICKSTART.md#testing](./DEVELOPER_QUICKSTART.md#testing)

**Troubleshoot issues**
→ [DEVELOPER_QUICKSTART.md#troubleshooting](./DEVELOPER_QUICKSTART.md#troubleshooting)

---

## 📊 System Quick Reference

### Technology Stack

| Category | Technology |
|----------|------------|
| Language | Java 24 |
| Framework | Spring Boot 3.5.5, Spring Cloud 2025.0.0 |
| Messaging | Apache Kafka, Spring Cloud Stream, CloudEvents 4.0.1 |
| Persistence | JPA/Hibernate 6.6.29, R2DBC, PostgreSQL/H2, Redis, MongoDB |
| State Machine | Spring State Machine 4.0.1 (Saga orchestration) |
| Observability | Micrometer, Prometheus, OTLP, W3C Trace Context |
| Security | Spring Security, OAuth2, Vault |
| Mapping | MapStruct 1.6.3 |
| Resilience | Resilience4j |

### Domain Model

**3 Aggregate Roots**:
1. **Wallet** (5 states: ACTIVE, INACTIVE, LOCKED, RECOVERING, DELETED)
2. **User** (4 states: PENDING_VERIFICATION, ACTIVE, DEACTIVATED, SUSPENDED)
3. **Transaction** (3 states: PENDING, CONFIRMED, FAILED)

**10 Value Objects/Entities**: Address, Token, Network, Vault, Contract, Store, etc.

**36 Domain Events** across 9 categories (Wallet, User, Transaction, Address, Token, Network, Vault, Contract, Store)

### Application Layer

**34 Use Cases** organized in categories:
- Wallet Management (8 use cases)
- Funds Management (4 use cases)
- Transaction Management (4 use cases)
- User Management (5 use cases)
- Address Management (5 use cases)
- Network & Token Management (5 use cases)
- Portfolio Management (3 use cases)

### Event Infrastructure

**4 Kafka Topics** (actively published):
- `wallet-created-topic`
- `funds-added-topic`
- `funds-withdrawn-topic`
- `funds-transferred-topic`

**4 Event Consumers**:
- WalletCreatedEventConsumer
- FundsAddedEventConsumer
- FundsWithdrawnEventConsumer
- FundsTransferredEventConsumer

**Saga States**: INITIAL → WALLET_CREATED → FUNDS_ADDED → FUNDS_WITHDRAWN → FUNDS_TRANSFERRED → COMPLETED/FAILED

---

## 🎨 Architecture Diagrams

### Hexagonal Architecture

```
┌───────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                   │
│  (Adapters: Kafka, JPA, REST, Redis, MongoDB, Security)   │
│                                                           │
│  ┌───────────────────────────────────────────────────┐    │
│  │           APPLICATION LAYER (Use Cases)           │    │
│  │  (Orchestrates domain logic, no business rules)   │    │
│  │                                                   │    │
│  │  ┌────────────────────────────────────────────┐   │    │
│  │  │         DOMAIN LAYER (Core)                │   │    │
│  │  │  (Pure business logic, entities, events)   │   │    │
│  │  │  NO framework dependencies                 │   │    │
│  │  └────────────────────────────────────────────    │    │
│  └───────────────────────────────────────────────────┘    │
└───────────────────────────────────────────────────────────┘
```

### Event Flow

```
Use Case → Domain Model → Repository (save)
                ↓
         register event
                ↓
      Event Publisher → Outbox Table
                ↓
      Outbox Worker (polls every 5s)
                ↓
         wrap as CloudEvent
                ↓
      inject W3C Trace Context
                ↓
              Kafka
                ↓
         Event Consumer
                ↓
        State Machine
```

---

## 🚀 Quick Commands

```bash
# Build
./mvnw clean package -DskipTests

# Run (H2 in-memory)
./mvnw spring-boot:run

# Run (with PostgreSQL)
docker-compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# Test
./mvnw test
./mvnw test -Dtest=WalletTest
./mvnw test -Dtest="*.usecase.*"

# Native image
./mvnw native:compile -Pnative

# Infrastructure
docker-compose up -d
docker-compose down
```

---

## 📝 Documentation Statistics

| Document | Size | Sections | Topics |
|----------|------|----------|--------|
| TECHNICAL_ARCHITECTURE.md | 60KB | 13 | Complete system overview |
| DOMAIN_MODEL_GUIDE.md | 30KB | 8 | Domain model deep dive |
| EVENT_DRIVEN_GUIDE.md | 24KB | 9 | Event architecture |
| DEVELOPER_QUICKSTART.md | 26KB | 8 | Development guide |
| **Total** | **140KB** | **38** | **Comprehensive** |

**Code Examples**: 100+ practical examples
**Diagrams**: Architecture, state machines, sequence diagrams, ER diagrams
**Coverage**: Domain (100%), Use Cases (100%), Infrastructure (90%)

---

## 🔄 Documentation Maintenance

### Version Information

- **Current Version**: 0.0.1-SNAPSHOT
- **Documentation Version**: 1.0
- **Last Updated**: 2026-01-12
- **Spring Boot**: 3.5.5
- **Java**: 24

### Review Schedule

- **Frequency**: Quarterly
- **Next Review**: 2026-04-12
- **Responsible**: Development Team

### When to Update

Update documentation when:
- Adding new domain entities or use cases
- Changing architectural patterns
- Modifying event flows
- Adding new infrastructure components
- Updating technology versions
- Fixing bugs that affect documented behavior

---

## 🤝 Contributing to Documentation

### Guidelines

1. **Follow Structure**: Use existing document structure and formatting
2. **Include Examples**: Add code examples for complex concepts
3. **Update Index**: Update this index when adding new sections
4. **Keep Synchronized**: Ensure examples match actual code
5. **Add Diagrams**: Use Mermaid for architectural concepts
6. **Test Examples**: Verify all code examples compile and run

### Formatting Standards

- **Headings**: Use hierarchical headings (H1 → H6)
- **Code Blocks**: Always specify language for syntax highlighting
- **Tables**: Use for structured data comparisons
- **Lists**: Ordered for sequential steps, unordered for collections
- **Links**: Use descriptive link text, not "click here"

---

## 🐛 Troubleshooting Documentation Issues

### Broken Links

If you encounter a broken link:
1. Check if the target file exists
2. Verify the path is correct (absolute vs relative)
3. Update the link in both locations (source and index)

### Outdated Information

If documentation doesn't match the code:
1. Identify the outdated section
2. Review the current code implementation
3. Update the documentation
4. Add a note about when the change was made

### Missing Information

If a topic isn't covered:
1. Create an issue describing what's missing
2. Propose where it should be documented
3. Draft the content if possible
4. Submit for review

---

## 📞 Getting Help

### For Questions

1. **Check Documentation**: Search this index and the 4 core documents
2. **Review Examples**: Look at code examples in the guides
3. **Check Troubleshooting**: Review troubleshooting sections
4. **Ask Team**: Reach out on Slack or create an issue

### For Issues

1. **Check Logs**: Review application logs for errors
2. **Verify Configuration**: Check application.yml settings
3. **Database State**: Use H2 console to inspect data
4. **Kafka Topics**: Verify topics exist and have messages
5. **State Machine**: Check state machine state in database

---

## 📚 External Resources

### Spring Framework

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Cloud Stream](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/)
- [Spring State Machine](https://docs.spring.io/spring-statemachine/docs/current/reference/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)

### Messaging & Events

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [CloudEvents Specification](https://cloudevents.io/)
- [W3C Trace Context](https://www.w3.org/TR/trace-context/)

### Architecture Patterns

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design](https://martinfowler.com/tags/domain%20driven%20design.html)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)

### Tools

- [MapStruct](https://mapstruct.org/)
- [Resilience4j](https://resilience4j.readme.io/)
- [Micrometer](https://micrometer.io/)
- [Lombok](https://projectlombok.org/)

---

## 📄 License

This documentation is part of the Wallet Hub project and follows the same license as the codebase.

---

**Thank you for reading!** 🎉

For questions or feedback about this documentation:
- Open an issue in the project repository
- Contact the documentation team
- Suggest improvements via pull request

---

**Document Version**: 1.0
**Authors**: Technical Documentation Team
**Maintainers**: Development Team
**Last Review**: 2026-01-12
**Next Review**: 2026-04-12
