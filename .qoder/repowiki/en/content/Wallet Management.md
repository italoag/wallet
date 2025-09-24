<docs>
# Wallet Management

<cite>
**Referenced Files in This Document**   
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java)
- [CreateWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/CreateWalletUseCase.java)
- [UpdateWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/UpdateWalletUseCase.java)
- [ActivateWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/ActivateWalletUseCase.java)
- [DeactivateWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/DeactivateWalletUseCase.java)
- [DeleteWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/DeleteWalletUseCase.java)
- [ListWalletsUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/ListWalletsUseCase.java)
- [GetWalletDetailsUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/GetWalletDetailsUseCase.java)
- [RecoverWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/RecoverWalletUseCase.java)
- [WalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/WalletRepository.java)
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java)
- [WalletStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/wallet/WalletStatus.java)
- [WalletCreatedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/wallet/WalletCreatedEvent.java)
- [WalletUpdatedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/wallet/WalletUpdatedEvent.java)
- [WalletStatusChangedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/wallet/WalletStatusChangedEvent.java)
- [WalletDeletedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/wallet/WalletDeletedEvent.java)
- [WalletRecoveryInitiatedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/wallet/WalletRecoveryInitiatedEvent.java)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Wallet Lifecycle Operations](#wallet-lifecycle-operations)
3. [Core Components](#core-components)
4. [API Interfaces](#api-interfaces)
5. [Integration Patterns](#integration-patterns)
6. [Practical Examples](#practical-examples)
7. [Troubleshooting Guide](#troubleshooting-guide)
8. [Conclusion](#conclusion)

## Introduction

The Wallet Management system provides comprehensive functionality for managing digital wallets throughout their lifecycle. This documentation covers the implementation details, API interfaces, and integration patterns for wallet operations including creation, update, activation, deactivation, deletion, listing, details retrieval, and recovery. The system follows domain-driven design principles with clear separation between use cases, domain models, and infrastructure components.

The wallet system supports various states including ACTIVE, INACTIVE, DELETED, RECOVERING, and LOCKED, allowing for flexible management of wallet operations while maintaining audit trails through domain events. All operations are designed to be idempotent and follow consistent error handling patterns.

**Section sources**
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java#L27-L242)
- [WalletStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/wallet/WalletStatus.java#L7-L37)

## Wallet Lifecycle Operations

The wallet system supports a comprehensive set of lifecycle operations that enable full management of wallet instances. Each operation follows a consistent pattern of validation, state transition, persistence, and event publishing.

### Wallet Creation
Wallet creation initializes a new wallet instance with default values and ACTIVE status. The system generates a unique identifier and sets initial metadata including creation timestamp and zero balance.

### Wallet Update
Wallet update operations modify wallet metadata such as name and description. The system validates that the wallet exists and is in an active state before applying updates.

### Wallet Activation and Deactivation
Wallets can be activated and deactivated to control their operational state. Activation enables all wallet operations, while deactivation restricts functionality. Only non-deleted wallets can be activated or deactivated.

### Wallet Deletion
The system implements soft deletion, where wallets are marked as DELETED but retained for audit purposes. A wallet must have zero balance before it can be deleted.

### Wallet Recovery
Wallet recovery allows users to restore wallets from backup or seed phrase. The recovery process creates a wallet in RECOVERING state, which can later be activated.

### Wallet Status Management
Wallets can exist in multiple states: ACTIVE, INACTIVE, DELETED, RECOVERING, and LOCKED. Each state transition is recorded through domain events for audit and integration purposes.

**Section sources**
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java#L144-L180)
- [WalletStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/wallet/WalletStatus.java#L7-L37)

## Core Components

The wallet management system consists of several core components that work together to provide comprehensive wallet functionality.

### Wallet Domain Model
The Wallet class represents the core domain entity that encapsulates wallet data and behavior. It extends AggregateRoot to support domain event publishing and maintains key properties including:

- **id**: Unique identifier (UUID)
- **name**: Wallet name
- **description**: Wallet description
- **balance**: Current balance (BigDecimal)
- **status**: Current wallet status (WalletStatus)
- **createdAt/updatedAt**: Timestamps for creation and last update
- **addressIds**: Set of associated address identifiers

The domain model implements business logic for state transitions and validates operations based on current wallet status.

```mermaid
classDiagram
class Wallet {
+UUID id
+String name
+String description
+BigDecimal balance
+WalletStatus status
+Instant createdAt
+Instant updatedAt
+Set<UUID> addressIds
+UUID userId
+UUID correlationId
+create(UUID, String, String) Wallet
+updateInfo(String, String) void
+activate() void
+deactivate() void
+delete(String) void
+lock(String) void
+initiateRecovery(String) void
+isActive() boolean
+isDeleted() boolean
+validateOperationAllowed() void
}
class AggregateRoot {
+UUID id
+List<DomainEvent> domainEvents
+registerEvent(DomainEvent) void
+getDomainEvents() List<DomainEvent>
+clearEvents() void
}
Wallet --> AggregateRoot : "extends"
```

**Diagram sources**
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java#L27-L242)
- [AggregateRoot.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/common/AggregateRoot.java#L9-L27)

### Wallet Status Enumeration
The WalletStatus enum defines the possible states a wallet can be in:

- **ACTIVE**: Wallet is fully operational
- **INACTIVE**: Wallet is temporarily disabled
- **DELETED**: Wallet is soft-deleted (retained for audit)
- **RECOVERING**: Wallet is being restored from backup
- **LOCKED**: Wallet is locked due to security concerns

Each status transition is validated to ensure business rules are enforced.

```mermaid
stateDiagram-v2
[*] --> ACTIVE
ACTIVE --> INACTIVE : deactivate()
INACTIVE --> ACTIVE : activate()
ACTIVE --> LOCKED : lock()
LOCKED --> ACTIVE : activate()
ACTIVE --> DELETED : delete()
INACTIVE --> DELETED : delete()
ACTIVE --> RECOVERING : initiateRecovery()
RECOVERING --> ACTIVE : completeRecovery()
DELETED --> [*]
```

**Diagram sources**
- [WalletStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/wallet/WalletStatus.java#L7-L37)
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java#L144-L180)

### Domain Events
The system uses domain events to communicate state changes and support integration with other components. Key events include:

- **WalletCreatedEvent**: Published when a new wallet is created
- **WalletUpdatedEvent**: Published when wallet metadata is updated
- **WalletStatusChangedEvent**: Published when wallet status changes
- **WalletDeletedEvent**: Published when a wallet is deleted
- **WalletRecoveryInitiatedEvent**: Published when wallet recovery begins

These events contain correlation IDs for tracing operations across the system.

```mermaid
classDiagram
class DomainEvent {
+UUID eventId
+Instant occurredOn
+UUID correlationId
}
class WalletCreatedEvent {
+UUID walletId
}
class WalletUpdatedEvent {
+UUID walletId
+String newName
+String newDescription
}
class WalletStatusChangedEvent {
+UUID walletId
+WalletStatus oldStatus
+WalletStatus newStatus
+String reason
}
class WalletDeletedEvent {
+UUID walletId
+String reason
}
class WalletRecoveryInitiatedEvent {
+UUID walletId
+UUID userId
+String recoveryMethod
}
WalletCreatedEvent --> DomainEvent : "extends"
WalletUpdatedEvent --> DomainEvent : "extends"
WalletStatusChangedEvent --> DomainEvent : "extends"
WalletDeletedEvent --> DomainEvent : "extends"
WalletRecoveryInitiatedEvent --> DomainEvent : "extends"
```

**Diagram sources**
- [WalletCreatedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/wallet/WalletCreatedEvent.java#L18-L38)
- [WalletUpdatedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/wallet/WalletUpdatedEvent.java#L8-L21)
- [WalletStatusChangedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/wallet/WalletStatusChangedEvent.java#L13-L28)
- [WalletDeletedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/wallet/WalletDeletedEvent.java#L12-L23)
- [WalletRecoveryInitiatedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/wallet/WalletRecoveryInitiatedEvent.java#L12-L25)

## API Interfaces

The wallet management system provides a set of use case classes that define the API interfaces for wallet operations.

### CreateWalletUseCase
Handles wallet creation operations.

```mermaid
flowchart TD
A[createWallet] --> B{Validate Parameters}
B --> |Valid| C[Create Wallet Instance]
C --> D[Set Initial State]
D --> E[Persist Wallet]
E --> F[Publish WalletCreatedEvent]
F --> G[Return Wallet]
B --> |Invalid| H[Throw IllegalArgumentException]
```

**Section sources**
- [CreateWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/CreateWalletUseCase.java#L1-L42)

### UpdateWalletUseCase
Handles wallet update operations.

```mermaid
flowchart TD
A[updateWallet] --> B{Validate Input}
B --> |Invalid| C[Throw IllegalArgumentException]
B --> |Valid| D[Find Wallet by ID]
D --> E{Wallet Found?}
E --> |No| F[Throw IllegalArgumentException]
E --> |Yes| G[Validate Active State]
G --> H[Update Wallet Info]
H --> I[Persist Changes]
I --> J[Publish WalletUpdatedEvent]
J --> K[Return Updated Wallet]
```

**Section sources**
- [UpdateWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/UpdateWalletUseCase.java#L1-L56)

### ActivateWalletUseCase
Handles wallet activation operations.

```mermaid
flowchart TD
A[activateWallet] --> B[Find Wallet by ID]
B --> C{Wallet Found?}
C --> |No| D[Throw IllegalArgumentException]
C --> |Yes| E{Wallet Deleted?}
E --> |Yes| F[Throw IllegalStateException]
E --> |No| G[Activate Wallet]
G --> H[Persist Changes]
H --> I[Publish WalletStatusChangedEvent]
I --> J[Return Activated Wallet]
```

**Section sources**
- [ActivateWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/ActivateWalletUseCase.java#L1-L49)

### DeactivateWalletUseCase
Handles wallet deactivation operations.

```mermaid
flowchart TD
A[deactivateWallet] --> B[Find Wallet by ID]
B --> C{Wallet Found?}
C --> |No| D[Throw IllegalArgumentException]
C --> |Yes| E{Wallet Deleted?}
E --> |Yes| F[Throw IllegalStateException]
E --> |No| G[Deactivate Wallet]
G --> H[Persist Changes]
H --> I[Publish WalletStatusChangedEvent]
I --> J[Return Deactivated Wallet]
```

**Section sources**
- [DeactivateWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/DeactivateWalletUseCase.java#L1-L49)

### DeleteWalletUseCase
Handles wallet deletion operations.

```mermaid
flowchart TD
A[deleteWallet] --> B{Validate Reason}
B --> |Invalid| C[Throw IllegalArgumentException]
B --> |Valid| D[Find Wallet by ID]
D --> E{Wallet Found?}
E --> |No| F[Throw IllegalArgumentException]
E --> |Yes| G{Already Deleted?}
G --> |Yes| H[Throw IllegalStateException]
G --> |No| I{Balance Zero?}
I --> |No| J[Throw IllegalStateException]
I --> |Yes| K[Delete Wallet]
K --> L[Persist Changes]
L --> M[Publish WalletStatusChangedEvent]
M --> N[Publish WalletDeletedEvent]
N --> O[Return Deleted Wallet]
```

**Section sources**
- [DeleteWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/DeleteWalletUseCase.java#L1-L61)

### ListWalletsUseCase
Handles wallet listing operations with various filtering options.

```mermaid
flowchart TD
A[listActiveWallets] --> B{Validate User ID}
B --> |Invalid| C[Throw IllegalArgumentException]
B --> |Valid| D[Query Repository]
D --> E[Filter Active Wallets]
E --> F[Return Wallet List]
G[listWallets] --> H{Validate User ID}
H --> |Invalid| I[Throw IllegalArgumentException]
H --> |Valid| J[Query Repository]
J --> K[Filter Non-Deleted Wallets]
K --> L[Return Wallet List]
M[listWalletsByStatus] --> N{Validate Parameters}
N --> |Invalid| O[Throw IllegalArgumentException]
N --> |Valid| P[Query Repository]
P --> Q[Filter by Status]
Q --> R[Return Wallet List]
```

**Section sources**
- [ListWalletsUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/ListWalletsUseCase.java#L1-L90)

### GetWalletDetailsUseCase
Handles wallet details retrieval operations.

```mermaid
flowchart TD
A[getWalletDetails] --> B{Validate Wallet ID}
B --> |Invalid| C[Throw IllegalArgumentException]
B --> |Valid| D[Find Wallet by ID]
D --> E{Wallet Found?}
E --> |No| F[Throw IllegalArgumentException]
E --> |Yes| G[Return Wallet Details]
H[getWallet] --> I{Validate Wallet ID}
I --> |Invalid| J[Throw IllegalArgumentException]
I --> |Valid| K[Find Wallet by ID]
K --> L{Wallet Found?}
L --> |No| M[Throw IllegalArgumentException]
L --> |Yes| N{Include Deleted?}
N --> |No| O{Wallet Deleted?}
O --> |Yes| P[Throw IllegalArgumentException]
O --> |No| Q[Return Wallet]
N --> |Yes| Q
```

**Section sources**
- [GetWalletDetailsUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/GetWalletDetailsUseCase.java#L1-L77)

### RecoverWalletUseCase
Handles wallet recovery operations.

```mermaid
flowchart TD
A[recoverWallet] --> B{Validate Parameters}
B --> |Invalid| C[Throw IllegalArgumentException]
B --> |Valid| D[Create Recovery Wallet]
D --> E[Set Recovery State]
E --> F[Persist Wallet]
F --> G[Publish Events]
G --> H[Return Recovery Wallet]
I[completeRecovery] --> J[Find Wallet by ID]
J --> K{Wallet Found?}
K --> |No| L[Throw IllegalArgumentException]
K --> |Yes| M{In Recovery State?}
M --> |No| N[Throw IllegalStateException]
M --> |Yes| O[Activate Wallet]
O --> P[Persist Changes]
P --> Q[Publish WalletStatusChangedEvent]
Q --> R[Return Activated Wallet]
```

**Section sources**
- [RecoverWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/RecoverWalletUseCase.java#L1-L89)

## Integration Patterns

The wallet management system follows several key integration patterns to ensure reliability and maintainability.

### Repository Pattern
The WalletRepository interface defines the contract for wallet persistence operations, with JpaWalletRepository providing the JPA implementation. This pattern separates domain logic from data access concerns.

```mermaid
classDiagram
class WalletRepository {
<<interface>>
+save(Wallet) Wallet
+update(Wallet) void
+findById(UUID) Optional<Wallet>
+findAll() List<Wallet>
+delete(UUID) void
+findByName(String) List<Wallet>
+existsById(UUID) boolean
+findByUserId(UUID) List<Wallet>
+findByUserIdAndStatus(UUID, WalletStatus) List<Wallet>
+findActiveByUserId(UUID) List<Wallet>
}
class JpaWalletRepository {
-springDataWalletRepository
-walletMapper
+save(Wallet) Wallet
+update(Wallet) void
+findById(UUID) Optional<Wallet>
+findAll() List<Wallet>
+delete(UUID) void
+findByName(String) List<Wallet>
+existsById(UUID) boolean
+findByUserId(UUID) List<Wallet>
+findByUserIdAndStatus(UUID, WalletStatus) List<Wallet>
+findActiveByUserId(UUID) List<Wallet>
}
class SpringDataWalletRepository {
<<interface>>
+findById(UUID) Optional<WalletEntity>
+save(WalletEntity) WalletEntity
+deleteById(UUID) void
+findAll() List<WalletEntity>
+existsById(UUID) boolean
}
class WalletMapper {
<<interface>>
+toDomain(WalletEntity) Wallet
+toEntity(Wallet) WalletEntity
}
JpaWalletRepository --> WalletRepository : "implements"
JpaWalletRepository --> SpringDataWalletRepository : "uses"
JpaWalletRepository --> WalletMapper : "uses"
WalletMapper --> Wallet : "maps to"
WalletMapper --> WalletEntity : "maps to"
```

**Diagram sources**
- [WalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/WalletRepository.java#L18-L39)
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java#L36-L140)
- [SpringDataWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataWalletRepository.java#L25-L26)
- [WalletMapper.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/mapper/WalletMapper.java#L31-L50)

### Domain Events and Event Sourcing
The system uses domain events to communicate state changes. Events are published through the DomainEventPublisher and can be consumed by other components.

```mermaid
sequenceDiagram
participant UseCase as UseCase
participant Wallet as Wallet
participant Repository as WalletRepository
participant Publisher as DomainEventPublisher
participant Consumer as EventConsumer
UseCase->>Wallet : Perform Operation
Wallet->>Wallet : registerEvent()
UseCase->>Repository : update()
Repository->>Repository : Persist Changes
UseCase->>Wallet : getDomainEvents()
UseCase->>Publisher : publish(event)
Publisher->>Consumer : Deliver Event
Consumer->>Consumer : Process Event
```

**Diagram sources**
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java#L27-L242)
- [DomainEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/common/DomainEvent.java#L7-L18)
- [DomainEventPublisher.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/DomainEventPublisher.java)

### Use Case Pattern
Each use case is implemented as a record class that encapsulates a specific business operation. This pattern provides clear separation of concerns and makes the API explicit.

```mermaid
classDiagram
class UseCase {
<<interface>>
}
class CreateWalletUseCase {
+WalletRepository walletRepository
+DomainEventPublisher eventPublisher
+createWallet(UUID, String) Wallet
}
class UpdateWalletUseCase {
+WalletRepository walletRepository
+DomainEventPublisher eventPublisher
+updateWallet(UUID, String, String, String) Wallet
}
class ActivateWalletUseCase {
+WalletRepository walletRepository
+DomainEventPublisher eventPublisher
+activateWallet(UUID, String) Wallet
}
class DeactivateWalletUseCase {
+WalletRepository walletRepository
+DomainEventPublisher eventPublisher
+deactivateWallet(UUID, String) Wallet
}
class DeleteWalletUseCase {
+WalletRepository walletRepository
+DomainEventPublisher eventPublisher
+deleteWallet(UUID, String, String) Wallet
}
class ListWalletsUseCase {
+WalletRepository walletRepository
+listActiveWallets(UUID) List<Wallet>
+listWallets(UUID) List<Wallet>
+listWalletsByStatus(UUID, WalletStatus) List<Wallet>
+listAllWallets(UUID) List<Wallet>
}
class GetWalletDetailsUseCase {
+WalletRepository walletRepository
+AddressRepository addressRepository
+getWalletDetails(UUID) Wallet
+getWallet(UUID, boolean) Wallet
+isWalletAccessible(UUID) boolean
}
class RecoverWalletUseCase {
+WalletRepository walletRepository
+DomainEventPublisher eventPublisher
+recoverWallet(UUID, String, String, String) Wallet
+completeRecovery(UUID, String) Wallet
}
CreateWalletUseCase --> UseCase : "implements"
UpdateWalletUseCase --> UseCase : "implements"
ActivateWalletUseCase --> UseCase : "implements"
DeactivateWalletUseCase --> UseCase : "implements"
DeleteWalletUseCase --> UseCase : "implements"
ListWalletsUseCase --> UseCase : "implements"
GetWalletDetailsUseCase --> UseCase : "implements"
RecoverWalletUseCase --> UseCase : "implements"
```

**Diagram sources**
- [CreateWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/CreateWalletUseCase.java#L1-L42)
- [UpdateWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/UpdateWalletUseCase.java#L1-L56)
- [ActivateWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/ActivateWalletUseCase.java#L1-L49)
- [DeactivateWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/DeactivateWalletUseCase.java#L1-L49)
- [DeleteWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/DeleteWalletUseCase.java#L1-L61)
- [ListWalletsUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/ListWalletsUseCase.java#L1-L90)
- [GetWalletDetailsUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/GetWalletDetailsUseCase.java#L1-L77)
- [RecoverWalletUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/RecoverWalletUseCase.java#L1-L89)

## Practical Examples

### Creating a Wallet
```java
// Create an instance of CreateWalletUseCase
CreateWalletUseCase createWalletUseCase = new CreateWalletUseCase(walletRepository, eventPublisher);

// Create a new wallet
UUID userId = UUID.randomUUID();
String correlationId = UUID.randomUUID().toString();
Wallet wallet = createWalletUseCase.createWallet(userId, correlationId);

// The wallet is now created with default values
System.out.println("Created wallet: " + wallet.getId());
System.out.println("Status: " + wallet.getStatus()); // ACTIVE
System.out.println("Balance: " + wallet.getBalance()); // 0.00
```

### Updating Wallet Information
```java
// Create an instance of UpdateWalletUseCase
UpdateWalletUseCase updateWalletUseCase = new UpdateWalletUseCase(walletRepository, eventPublisher);

// Update wallet name and description
UUID walletId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
String newName = "My Savings Wallet";
String newDescription = "Wallet for saving money";
String correlationId = UUID.randomUUID().toString();

try {
    Wallet updatedWallet = updateWalletUseCase.updateWallet(walletId, newName, newDescription, correlationId);
    System.out.println("Updated wallet: " + updatedWallet.getName());
    System.out.println("New description: " + updatedWallet.getDescription());
} catch (IllegalArgumentException e) {
    System.err.println("Failed to update wallet: " + e.getMessage());
} catch (IllegalStateException e) {
    System.err.println("Wallet is not active: " + e.getMessage());
}
```

### Activating a Wallet
```java
// Create an instance of ActivateWalletUseCase
ActivateWalletUseCase activateWalletUseCase = new ActivateWalletUseCase(walletRepository, eventPublisher);

// Activate a wallet
UUID walletId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
String correlationId = UUID.randomUUID().toString();

try {
    Wallet activatedWallet = activateWalletUseCase.activateWallet(walletId, correlationId);
    System.out.println("Wallet activated: " + activatedWallet.getId());
    System.out.println("Status: " + activatedWallet.getStatus()); // ACTIVE
} catch (IllegalArgumentException e) {
    System.err.println("Wallet not found: " + e.getMessage());
} catch (IllegalStateException e) {
    System.err.println("Cannot activate deleted wallet: " + e.getMessage());
}
```

### Deactivating a Wallet
```java
// Create an instance of DeactivateWalletUseCase
DeactivateWalletUseCase deactivateWalletUseCase = new DeactivateWalletUseCase(walletRepository, eventPublisher);

// Deactivate a wallet
UUID walletId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
String correlationId = UUID.randomUUID().toString();

try {
    Wallet deactivatedWallet = deactivateWalletUseCase.deactivateWallet(walletId, correlationId);
    System.out.println("Wallet deactivated: " + deactivatedWallet.getId());
    System.out.println("Status: " + deactivatedWallet.getStatus()); // INACTIVE
} catch (IllegalArgumentException e) {
    System.err.println("Wallet not found: " + e.getMessage());
} catch (IllegalStateException e) {
    System.err.println("Cannot deactivate deleted wallet: " + e.getMessage());
}
```

### Deleting a Wallet
```java
// Create an instance of DeleteWalletUseCase
DeleteWalletUseCase deleteWalletUseCase = new DeleteWalletUseCase(walletRepository, eventPublisher);

// Delete a wallet
UUID walletId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
String reason = "User requested deletion";
String correlationId = UUID.randomUUID().toString();

try {
    Wallet deletedWallet = deleteWalletUseCase.deleteWallet(walletId, reason, correlationId);
    System.out.println("Wallet deleted: " + deletedWallet.getId());
    System.out.println("Status: " + deletedWallet.getStatus()); // DELETED
    System.out.println("Reason: " + reason);
} catch (IllegalArgumentException e) {
    System.err.println("Invalid parameters: " + e.getMessage());
} catch (IllegalStateException e) {
    System.err.println("Cannot delete wallet: " + e.getMessage());
}
```

### Listing Wallets
```java
// Create an instance of ListWalletsUseCase
ListWalletsUseCase listWalletsUseCase = new ListWalletsUseCase(walletRepository);

// List all active wallets for a user
UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
List<Wallet> activeWallets = listWalletsUseCase.listActiveWallets(userId);

System.out.println("Active wallets: " + activeWallets.size());
for (Wallet wallet : activeWallets) {
    System.out.println("- " + wallet.getName() + " (" + wallet.getId() + ")");
}

// List wallets by status
List<Wallet> inactiveWallets = listWalletsUseCase.listWalletsByStatus(userId, WalletStatus.INACTIVE);
System.out.println("Inactive wallets: " + inactiveWallets.size());
```

### Recovering a Wallet
```java
// Create an instance of RecoverWalletUseCase
RecoverWalletUseCase recoverWalletUseCase = new RecoverWalletUseCase(walletRepository, eventPublisher);

// Initiate wallet recovery
UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
String walletName = "Recovered Wallet";
String recoveryMethod = "seed_phrase";
String correlationId = UUID.randomUUID().toString();

try {
    Wallet recoveryWallet = recoverWalletUseCase.recoverWallet(userId, walletName, recoveryMethod, correlationId);
    System.out.println("Wallet recovery initiated: " + recoveryWallet.getId());
    System.out.println("Status: " + recoveryWallet.getStatus()); // RECOVERING
    System.out.println("Recovery method: " + recoveryMethod);
    
    // Later, complete the recovery process
    Wallet completedWallet = recoverWalletUseCase.completeRecovery(recoveryWallet.getId(), UUID.randomUUID().toString());
    System.out.println("Wallet recovery completed: " + completedWallet.getId());
    System.out.println("