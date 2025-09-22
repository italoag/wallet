# Repository Pattern Implementation

<cite>
**Referenced Files in This Document**   
- [WalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/WalletRepository.java)
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java)
- [SpringDataWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataWalletRepository.java)
- [WalletMapper.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/mapper/WalletMapper.java)
- [WalletEntity.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/entity/WalletEntity.java)
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Architecture Overview](#architecture-overview)
3. [Domain Gateway Interface](#domain-gateway-interface)
4. [Infrastructure Implementation](#infrastructure-implementation)
5. [Spring Data JPA Repository](#spring-data-jpa-repository)
6. [Entity-Domain Mapping](#entity-domain-mapping)
7. [Dependency Injection and Constructor Injection](#dependency-injection-and-constructor-injection)
8. [Query Methods and Filtering Logic](#query-methods-and-filtering-logic)
9. [Transaction Management and Exception Handling](#transaction-management-and-exception-handling)
10. [Performance Implications](#performance-implications)
11. [Extending Repository Functionality](#extending-repository-functionality)

## Introduction
The repository pattern in bloco-wallet-java implements a clean separation between domain logic and data persistence concerns. This document details the implementation of the WalletRepository interface, its infrastructure-specific implementation, and the supporting components that enable data persistence while maintaining domain integrity. The architecture follows domain-driven design principles with clear boundaries between layers.

## Architecture Overview
The repository implementation follows a layered architecture with distinct responsibilities across domain and infrastructure layers. The design ensures loose coupling between business logic and data access mechanisms.

```mermaid
graph TB
subgraph "Domain Layer"
A[WalletRepository<br/>Interface]
B[Wallet<br/>Domain Model]
end
subgraph "Infrastructure Layer"
C[JpaWalletRepository<br/>Implementation]
D[SpringDataWalletRepository<br/>JPA Interface]
E[WalletMapper<br/>Mapping Component]
F[WalletEntity<br/>JPA Entity]
end
A --> C
B --> C
C --> D
C --> E
E --> F
D --> F
```

**Diagram sources**
- [WalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/WalletRepository.java)
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java)
- [SpringDataWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataWalletRepository.java)
- [WalletMapper.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/mapper/WalletMapper.java)
- [WalletEntity.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/entity/WalletEntity.java)
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java)

## Domain Gateway Interface
The WalletRepository interface defines the contract for wallet persistence operations in the domain layer. It specifies methods for CRUD operations and query functionality without exposing implementation details.

```mermaid
classDiagram
class WalletRepository {
+Wallet save(Wallet wallet)
+void update(Wallet wallet)
+Optional<Wallet> findById(UUID id)
+List<Wallet> findAll()
+void delete(UUID id)
+List<Wallet> findByName(String name)
+boolean existsById(UUID id)
}
class Wallet {
-UUID id
-String name
-String description
-BigDecimal balance
-Instant createdAt
-Instant updatedAt
}
WalletRepository --> Wallet : "uses"
```

**Diagram sources**
- [WalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/WalletRepository.java)
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java)

**Section sources**
- [WalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/WalletRepository.java)

## Infrastructure Implementation
The JpaWalletRepository class provides the concrete implementation of the WalletRepository interface, serving as an adapter between the domain layer and JPA persistence mechanisms. It delegates database operations to Spring Data JPA while handling domain-entity mapping.

```mermaid
classDiagram
class JpaWalletRepository {
-SpringDataWalletRepository springDataWalletRepository
-WalletMapper walletMapper
+Optional<Wallet> findById(UUID id)
+Wallet save(Wallet wallet)
+void update(Wallet wallet)
+List<Wallet> findAll()
+void delete(UUID id)
+List<Wallet> findByName(String name)
+boolean existsById(UUID id)
}
class SpringDataWalletRepository {
+Optional<WalletEntity> findById(UUID id)
+<S extends WalletEntity> S save(S entity)
+void deleteById(UUID id)
+List<WalletEntity> findAll()
+boolean existsById(UUID id)
}
class WalletMapper {
+Wallet toDomain(WalletEntity entity)
+WalletEntity toEntity(Wallet domain)
}
JpaWalletRepository --> SpringDataWalletRepository : "delegates to"
JpaWalletRepository --> WalletMapper : "uses"
WalletMapper --> WalletEntity : "maps to/from"
WalletMapper --> Wallet : "maps to/from"
```

**Diagram sources**
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java)
- [SpringDataWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataWalletRepository.java)
- [WalletMapper.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/mapper/WalletMapper.java)
- [WalletEntity.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/entity/WalletEntity.java)
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java)

**Section sources**
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java)

## Spring Data JPA Repository
The SpringDataWalletRepository interface extends JpaRepository to provide type-safe data access operations for WalletEntity. As a thin wrapper over JPA operations, it requires no implementation code while providing standard CRUD functionality.

```mermaid
classDiagram
class SpringDataWalletRepository {
<<interface>>
+Optional<WalletEntity> findById(UUID id)
+<S extends WalletEntity> S save(S entity)
+void deleteById(UUID id)
+List<WalletEntity> findAll()
+boolean existsById(UUID id)
}
class JpaRepository {
<<interface>>
+T findById(ID id)
+<S extends T> S save(S entity)
+void deleteById(ID id)
+List<T> findAll()
+boolean existsById(ID id)
}
SpringDataWalletRepository --|> JpaRepository : "extends"
```

**Diagram sources**
- [SpringDataWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataWalletRepository.java)

## Entity-Domain Mapping
The WalletMapper interface handles bidirectional conversion between domain models and JPA entities. Using MapStruct, it ensures data consistency while abstracting persistence details from the domain layer.

```mermaid
classDiagram
class WalletMapper {
<<interface>>
+Wallet toDomain(WalletEntity entity)
+WalletEntity toEntity(Wallet domain)
}
class Wallet {
-UUID id
-String name
-String description
-BigDecimal balance
}
class WalletEntity {
-UUID id
-UUID userId
-BigDecimal balance
}
WalletMapper --> Wallet : "converts to/from"
WalletMapper --> WalletEntity : "converts to/from"
```

**Diagram sources**
- [WalletMapper.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/mapper/WalletMapper.java)
- [WalletEntity.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/entity/WalletEntity.java)
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java)

**Section sources**
- [WalletMapper.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/mapper/WalletMapper.java)

## Dependency Injection and Constructor Injection
The JpaWalletRepository uses constructor injection to receive its dependencies, ensuring immutability and explicit dependency declaration. Spring's @Autowired annotation enables automatic wiring of required components.

```mermaid
sequenceDiagram
participant Spring as Spring Container
participant JpaRepo as JpaWalletRepository
participant SpringData as SpringDataWalletRepository
participant Mapper as WalletMapper
Spring->>JpaRepo : Instantiate
Spring->>SpringData : Get Bean
Spring->>Mapper : Get Bean
Spring->>JpaRepo : Inject dependencies via constructor
JpaRepo->>SpringData : Store reference
JpaRepo->>Mapper : Store reference
```

**Diagram sources**
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java)

## Query Methods and Filtering Logic
The repository implementation provides various query methods with different performance characteristics. The findByName method demonstrates in-memory filtering due to schema limitations, while other methods leverage database-level operations.

```mermaid
flowchart TD
Start([findByName]) --> CheckName["Check if name is null"]
CheckName --> |Name is null| ReturnAll["Return all wallets"]
CheckName --> |Name provided| LoadAll["Load all wallets from database"]
LoadAll --> Filter["Filter by name in memory"]
Filter --> ReturnResult["Return filtered list"]
Start2([findById]) --> Delegate["Delegate to SpringDataWalletRepository"]
Delegate --> Database["Database-level lookup by ID"]
Database --> Map["Map entity to domain object"]
Map --> ReturnSingle["Return Optional<Wallet>"]
```

**Diagram sources**
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java)

## Transaction Management and Exception Handling
The repository pattern integrates with Spring's transaction management system, ensuring data consistency across operations. Exception handling is transparent, with JPA exceptions propagating through the call stack while maintaining domain integrity.

```mermaid
sequenceDiagram
participant UseCase as UseCase
participant Repo as WalletRepository
participant JpaRepo as JpaWalletRepository
participant SpringData as SpringDataWalletRepository
UseCase->>Repo : save(wallet)
Repo->>JpaRepo : save(wallet)
JpaRepo->>JpaRepo : toEntity(wallet)
JpaRepo->>SpringData : save(entity)
SpringData-->>JpaRepo : savedEntity
JpaRepo->>JpaRepo : toDomain(savedEntity)
JpaRepo-->>Repo : savedWallet
Repo-->>UseCase : savedWallet
alt Exception Path
SpringData--x JpaRepo : JPA Exception
JpaRepo--x UseCase : Propagate exception
end
```

**Diagram sources**
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java)

## Performance Implications
The repository implementation has several performance considerations, particularly around query efficiency and object mapping overhead. Database-level operations are preferred over in-memory filtering to minimize data transfer and processing.

```mermaid
graph TD
A[Performance Factors] --> B[findById: Efficient<br/>Database index lookup]
A --> C[save/update: Single<br/>entity operation]
A --> D[findAll: Full table scan<br/>Potential memory impact]
A --> E[findByName: In-memory<br/>filtering after full load]
A --> F[Mapping: CPU overhead<br/>for entity-domain conversion]
G[Optimization Opportunities] --> H[Add name column to<br/>WalletEntity schema]
G --> I[Implement database-level<br/>query for findByName]
G --> J[Use projections for<br/>read-only operations]
G --> K[Consider pagination<br/>for large result sets]
```

**Diagram sources**
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java)
- [WalletEntity.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/entity/WalletEntity.java)

## Extending Repository Functionality
New query operations can be added by extending the repository interface hierarchy. Custom queries can be implemented through Spring Data JPA method naming conventions or JPQL annotations.

```mermaid
classDiagram
class WalletRepository {
+List<Wallet> findByBalanceGreaterThan(BigDecimal amount)
+List<Wallet> findByCreatedAfter(Instant date)
+Page<Wallet> findBy(Pageable pageable)
}
class SpringDataWalletRepository {
+List<WalletEntity> findByBalanceGreaterThan(BigDecimal amount)
+List<WalletEntity> findByCreatedAtAfter(Instant date)
+Page<WalletEntity> findAll(Pageable pageable)
}
class JpaWalletRepository {
+List<Wallet> findByBalanceGreaterThan(BigDecimal amount)
+List<Wallet> findByCreatedAfter(Instant date)
+Page<Wallet> findBy(Pageable pageable)
}
WalletRepository <|-- JpaWalletRepository
JpaWalletRepository --> SpringDataWalletRepository
```

**Diagram sources**
- [WalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/WalletRepository.java)
- [SpringDataWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataWalletRepository.java)
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java)