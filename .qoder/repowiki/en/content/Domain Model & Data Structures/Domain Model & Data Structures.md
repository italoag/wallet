# Domain Model & Data Structures

<cite>
**Referenced Files in This Document**   
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java)
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java)
- [User.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/user/User.java)
- [Token.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/token/Token.java)
- [Vault.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/vault/Vault.java)
- [Entity.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/common/Entity.java)
- [AggregateRoot.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/common/AggregateRoot.java)
- [TransactionHash.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionHash.java)
- [PublicKey.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/address/PublicKey.java)
- [Address.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/address/Address.java)
- [TransactionStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionStatus.java)
- [WalletStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/wallet/WalletStatus.java)
- [VaultStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/vault/VaultStatus.java)
- [AddressStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/address/AddressStatus.java)
- [UserStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/user/UserStatus.java)
- [Network.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/network/Network.java)
- [Store.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/store/Store.java)
- [Contract.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/contract/Contract.java)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Core Domain Entities](#core-domain-entities)
3. [Base Classes and Inheritance](#base-classes-and-inheritance)
4. [Value Objects](#value-objects)
5. [Status Enums and Lifecycle Management](#status-enums-and-lifecycle-management)
6. [Entity Relationships and Aggregations](#entity-relationships-and-aggregations)
7. [Persistence and Infrastructure Integration](#persistence-and-infrastructure-integration)
8. [Validation and Business Rules](#validation-and-business-rules)
9. [Encapsulation and Immutability Patterns](#encapsulation-and-immutability-patterns)
10. [Sample JSON Representations](#sample-json-representations)

## Introduction

The bloco-wallet-java application implements a domain-driven design (DDD) architecture for managing digital wallets, transactions, and related blockchain entities. This document details the core domain model, focusing on the primary entities—Wallet, Transaction, User, Token, and Vault—along with their relationships, business rules, and design patterns. The domain model emphasizes encapsulation, immutability where appropriate, and clear separation from infrastructure concerns. Entities extend base classes (Entity and AggregateRoot) to provide common functionality, while value objects like TransactionHash and PublicKey ensure data integrity. Status enums govern lifecycle transitions, and domain events capture state changes for eventual consistency.

**Section sources**
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java#L1-L242)
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L1-L210)
- [User.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/user/User.java#L1-L235)

## Core Domain Entities

### Wallet
The Wallet entity represents a user's digital wallet, serving as an aggregate root that manages funds and associated addresses. It contains fields for `name`, `description`, `balance`, and a set of `addressIds` that reference Address entities. The Wallet is created with a zero balance and can be updated via `updateInfo()`. Funds can be added or withdrawn using `addFunds()` and `withdrawFunds()`, which include validation to prevent negative amounts or overdrafts. The Wallet also tracks creation and update timestamps. The Wallet has multiple status states including ACTIVE, INACTIVE, DELETED, RECOVERING, and LOCKED, allowing for comprehensive lifecycle management. Operations are only permitted when the wallet is active, enforced by the `validateOperationAllowed()` method.

```mermaid
classDiagram
class Wallet {
+String name
+String description
+Set<UUID> addressIds
+Instant createdAt
+Instant updatedAt
+BigDecimal balance
+UUID correlationId
+WalletStatus status
+UUID userId
+static Wallet create(UUID, String, String)
+void updateInfo(String, String)
+void addAddress(UUID)
+void removeAddress(UUID)
+boolean containsAddress(UUID)
+void addFunds(BigDecimal)
+void withdrawFunds(BigDecimal)
+void activate()
+void deactivate()
+void delete(String)
+void lock(String)
+void initiateRecovery(String)
+boolean isActive()
+boolean isDeleted()
+void validateOperationAllowed()
}
```

**Diagram sources**
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java#L27-L242)

**Section sources**
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java#L27-L242)

### Transaction
The Transaction entity represents a blockchain transaction, capturing details such as the sender (`fromAddress`), recipient (`toAddress`), value, gas information, and status. It is an aggregate root with a lifecycle that begins in a PENDING state and transitions to CONFIRMED or FAILED. The `confirm()` method updates the transaction with block details and changes its status, while `fail()` marks it as failed with a reason. The `getHash()` method returns the transaction hash as a string, delegating to the TransactionHash value object.

```mermaid
classDiagram
class Transaction {
+UUID networkId
+TransactionHash hash
+String fromAddress
+String toAddress
+BigDecimal value
+BigDecimal gasPrice
+BigDecimal gasLimit
+BigDecimal gasUsed
+String data
+Instant timestamp
+Long blockNumber
+String blockHash
+TransactionStatus status
+static Transaction create(UUID, UUID, TransactionHash, String, String, BigDecimal, String)
+static Transaction rehydrate(...)
+void confirm(long, String, BigDecimal)
+void fail(String)
+void setGasInfo(BigDecimal, BigDecimal)
+boolean isConfirmed()
+boolean isPending()
+boolean isFailed()
}
```

**Diagram sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L20-L210)

**Section sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L20-L210)

### User
The User entity represents a system user with a unique identifier, name, email, and password hash. It extends AggregateRoot and includes additional fields for authentication and security management such as `status`, `emailVerified`, `failedLoginAttempts`, and `lockedUntil`. The User supports operations like profile updates, password changes, activation/deactivation, suspension, two-factor authentication, and login attempt tracking. Account locking occurs after 5 failed login attempts for 30 minutes, providing built-in security measures. The User maintains timestamps for creation, updates, and last login.

```mermaid
classDiagram
class User {
+String name
+String email
+String passwordHash
+UserStatus status
+Instant createdAt
+Instant updatedAt
+Instant lastLoginAt
+boolean emailVerified
+String emailVerificationToken
+TwoFactorAuth twoFactorAuth
+int failedLoginAttempts
+Instant lockedUntil
+static User create(UUID, String, String, String)
+static User create(String, String, String)
+void updateProfile(String, String)
+void changePassword(String)
+void activate()
+void deactivate()
+void suspend(String)
+void verifyEmail()
+void setEmailVerificationToken(String)
+void recordSuccessfulLogin()
+void recordFailedLogin()
+boolean isLocked()
+void unlock()
+boolean isActive()
+void validateOperationAllowed()
+void enableTwoFactorAuth(String)
+void disableTwoFactorAuth()
+boolean isTwoFactorEnabled()
+int getAvailableBackupCodesCount()
}
```

**Diagram sources**
- [User.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/user/User.java#L18-L235)

**Section sources**
- [User.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/user/User.java#L18-L235)

### Token
The Token entity represents a cryptocurrency token (e.g., ERC-20, ERC-721) on a specific network. It includes fields for `name`, `symbol`, `decimals`, `type`, and `contractAddress`. The Token class provides methods to determine the token type (`isFungible()`, `isNFT()`, `isNative()`) and to format/parse amounts according to the token's decimal precision. For fungible tokens, amounts are scaled by 10^decimals, while NFTs are treated as whole numbers.

```mermaid
classDiagram
class Token {
+UUID networkId
+String contractAddress
+String name
+String symbol
+int decimals
+TokenType type
+UUID getNetworkId()
+String getContractAddress()
+String getName()
+String getSymbol()
+int getDecimals()
+TokenType getType()
+boolean isNative()
+boolean isNFT()
+boolean isFungible()
+String formatAmount(BigDecimal)
+BigDecimal parseAmount(String)
}
```

**Diagram sources**
- [Token.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/token/Token.java#L9-L105)

**Section sources**
- [Token.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/token/Token.java#L9-L105)

### Vault
The Vault entity represents a secure storage system for cryptographic keys (e.g., HSM, AWS KMS). It has a `name`, `type`, `configuration`, and `status`. The Vault can be activated or deactivated, with status changes recorded as domain events. Methods like `generateKeyPair()` and `sign()` are provided, but their implementation is stubbed in the domain model, deferring to infrastructure services. The Vault ensures operations are only allowed when active.

```mermaid
classDiagram
class Vault {
+String name
+VaultType type
+VaultConfiguration configuration
+VaultStatus status
+static Vault create(UUID, String, VaultType, VaultConfiguration)
+String getName()
+VaultType getType()
+VaultConfiguration getConfiguration()
+VaultStatus getStatus()
+void updateConfiguration(VaultConfiguration)
+void activate()
+void deactivate()
+boolean isAvailable()
+KeyGenerationResult generateKeyPair(String)
+byte[] sign(String, byte[])
}
```

**Diagram sources**
- [Vault.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/vault/Vault.java#L8-L96)

**Section sources**
- [Vault.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/vault/Vault.java#L8-L96)

### Network
The Network entity represents a blockchain network (e.g., Ethereum, Polygon) with attributes like `name`, `chainId`, `rpcUrl`, and `explorerUrl`. It has a status that can be ACTIVE, INACTIVE, or MAINTENANCE. The Network provides utility methods to generate URLs for transaction and address exploration. Status changes trigger NetworkStatusChangedEvent domain events.

```mermaid
classDiagram
class Network {
+String name
+String chainId
+String rpcUrl
+String explorerUrl
+NetworkStatus status
+static Network create(UUID, String, String, String, String)
+String getName()
+String getChainId()
+String getRpcUrl()
+String getExplorerUrl()
+NetworkStatus getStatus()
+void updateName(String)
+void updateRpcUrl(String)
+void updateExplorerUrl(String)
+void activate()
+void deactivate()
+void setMaintenance()
+boolean isAvailable()
+String getTransactionExplorerUrl(String)
+String getAddressExplorerUrl(String)
}
```

**Diagram sources**
- [Network.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/network/Network.java#L7-L114)

**Section sources**
- [Network.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/network/Network.java#L7-L114)

### Store
The Store entity represents a logical grouping of addresses within a vault. It has a `name`, `description`, and references a `vaultId`. Stores can be activated or deactivated and contain a set of address IDs. The Store allows adding and removing addresses, with corresponding domain events emitted.

```mermaid
classDiagram
class Store {
+String name
+UUID vaultId
+String description
+StoreStatus status
+Set<UUID> addressIds
+static Store create(UUID, String, UUID, String)
+String getName()
+UUID getVaultId()
+String getDescription()
+StoreStatus getStatus()
+Set<UUID> getAddressIds()
+void updateInfo(String, String)
+void activate()
+void deactivate()
+void addAddress(UUID)
+void removeAddress(UUID)
+boolean containsAddress(UUID)
+boolean isActive()
}
```

**Diagram sources**
- [Store.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/store/Store.java#L12-L100)

**Section sources**
- [Store.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/store/Store.java#L12-L100)

### Contract
The Contract entity represents a smart contract deployed on a blockchain network. It contains metadata like `address`, `name`, `abi`, `bytecode`, and deployment information. Contracts have owners represented by address IDs, and support adding/removing owners with corresponding domain events. The Contract provides methods to retrieve function and event signatures from its ABI.

```mermaid
classDiagram
class Contract {
+UUID networkId
+String address
+String name
+ContractABI abi
+String bytecode
+String deploymentTxHash
+Instant deploymentTimestamp
+Set<UUID> ownerAddressIds
+static Contract create(UUID, UUID, String, String, ContractABI, String, String)
+UUID getNetworkId()
+String getAddress()
+String getName()
+ContractABI getAbi()
+String getBytecode()
+String getDeploymentTxHash()
+Instant getDeploymentTimestamp()
+Set<UUID> getOwnerAddressIds()
+void addOwner(UUID)
+void removeOwner(UUID)
+boolean isOwnedBy(UUID)
+String getFunctionSignature(String)
+String getEventSignature(String)
}
```

**Diagram sources**
- [Contract.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/contract/Contract.java#L13-L113)

**Section sources**
- [Contract.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/contract/Contract.java#L13-L113)

## Base Classes and Inheritance

### Entity
The Entity class is an abstract base class that provides a common identity mechanism for all domain entities. It contains a private `id` field of type UUID, which is set in the constructor and exposed via a getter. The `equals()` and `hashCode()` methods are overridden to compare entities based on their ID, ensuring that two entities with the same ID are considered equal regardless of other field values.

```mermaid
classDiagram
class Entity {
-UUID id
+Entity(UUID)
+UUID getId()
+boolean equals(Object)
+int hashCode()
}
```

**Diagram sources**
- [Entity.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/common/Entity.java#L5-L28)

**Section sources**
- [Entity.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/common/Entity.java#L5-L28)

### AggregateRoot
The AggregateRoot class extends Entity and adds support for domain events. It maintains a list of `domainEvents` that are registered via `registerEvent()`. These events can be retrieved with `getDomainEvents()` and cleared after processing with `clearEvents()`. This pattern allows the domain model to publish events (e.g., WalletCreatedEvent) when state changes occur, enabling eventual consistency and decoupling of business logic.

```mermaid
classDiagram
class AggregateRoot {
-List<DomainEvent> domainEvents
+AggregateRoot(UUID)
+void registerEvent(DomainEvent)
+List<DomainEvent> getDomainEvents()
+void clearEvents()
}
AggregateRoot --|> Entity
```

**Diagram sources**
- [AggregateRoot.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/common/AggregateRoot.java#L9-L27)

**Section sources**
- [AggregateRoot.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/common/AggregateRoot.java#L9-L27)

## Value Objects

### TransactionHash
The TransactionHash class is a final value object that encapsulates a blockchain transaction hash. It validates the hash in the constructor, ensuring it is not null or blank. The `getValue()` method returns the hash string. The class overrides `equals()` and `hashCode()` to compare based on the value, making it suitable for use as a key in collections.

```mermaid
classDiagram
class TransactionHash {
-String value
+TransactionHash(String)
+String getValue()
+boolean equals(Object)
+int hashCode()
+String toString()
}
```

**Diagram sources**
- [TransactionHash.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionHash.java#L4-L41)

**Section sources**
- [TransactionHash.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionHash.java#L4-L41)

### PublicKey
The PublicKey class is a final value object that represents a cryptographic public key. Like TransactionHash, it validates the input in the constructor and provides a `getValue()` method. It ensures data integrity by preventing null or blank values and supports equality based on the key value.

```mermaid
classDiagram
class PublicKey {
-String value
+PublicKey(String)
+String getValue()
+boolean equals(Object)
+int hashCode()
+String toString()
}
```

**Diagram sources**
- [PublicKey.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/address/PublicKey.java#L4-L41)

**Section sources**
- [PublicKey.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/address/PublicKey.java#L4-L41)

### AccountAddress
The AccountAddress class is a final value object representing a blockchain account address. It validates the address in the constructor and provides a `getValue()` method. The class ensures data integrity by preventing null or blank values and supports equality based on the address value.

```mermaid
classDiagram
class AccountAddress {
-String value
+AccountAddress(String)
+String getValue()
+boolean equals(Object)
+int hashCode()
+String toString()
}
```

**Diagram sources**
- [AccountAddress.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/address/AccountAddress.java#L4-L41)

**Section sources**
- [AccountAddress.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/address/AccountAddress.java#L4-L41)

## Status Enums and Lifecycle Management

### TransactionStatus
The TransactionStatus enum defines the lifecycle states of a transaction: PENDING, CONFIRMED, and FAILED. A transaction starts as PENDING and can transition to CONFIRMED upon successful blockchain confirmation or to FAILED if it fails. The Transaction class provides methods like `isConfirmed()`, `isPending()`, and `isFailed()` to check the current status.

```mermaid
classDiagram
class TransactionStatus {
+PENDING
+CONFIRMED
+FAILED
}
```

**Diagram sources**
- [TransactionStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionStatus.java#L2-L6)

**Section sources**
- [TransactionStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionStatus.java#L2-L6)

### WalletStatus
The WalletStatus enum manages the lifecycle states of a wallet: ACTIVE, INACTIVE, DELETED, RECOVERING, and LOCKED. The wallet starts as ACTIVE and can transition through various states based on user actions or system events. The `activate()` and `deactivate()` methods control the ACTIVE/INACTIVE states, while `delete()` moves the wallet to DELETED state. Security concerns can trigger the LOCKED state, and recovery processes use the RECOVERING state. Each status transition registers a WalletStatusChangedEvent.

```mermaid
classDiagram
class WalletStatus {
+ACTIVE
+INACTIVE
+DELETED
+RECOVERING
+LOCKED
}
```

**Diagram sources**
- [WalletStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/wallet/WalletStatus.java#L7-L37)

**Section sources**
- [WalletStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/wallet/WalletStatus.java#L7-L37)

### UserStatus
The UserStatus enum defines the operational states of a user account: ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION, and DEACTIVATED. New users start in PENDING_VERIFICATION state until their email is verified. After verification, they become ACTIVE. Administrative actions can move users to INACTIVE, SUSPENDED, or DEACTIVATED states. The User entity enforces that operations are only allowed for active accounts that are not locked due to failed login attempts.

```mermaid
classDiagram
class UserStatus {
+ACTIVE
+INACTIVE
+SUSPENDED
+PENDING_VERIFICATION
+DEACTIVATED
}
```

**Diagram sources**
- [UserStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/user/UserStatus.java#L7-L36)

**Section sources**
- [UserStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/user/UserStatus.java#L7-L36)

### VaultStatus
The VaultStatus enum defines the operational states of a vault: ACTIVE and INACTIVE. The Vault class provides `activate()` and `deactivate()` methods to change the status, with checks to prevent redundant transitions. Status changes are recorded as VaultStatusChangedEvent.

```mermaid
classDiagram
class VaultStatus {
+ACTIVE
+INACTIVE
}
```

**Diagram sources**
- [VaultStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/vault/VaultStatus.java#L2-L5)

**Section sources**
- [VaultStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/vault/VaultStatus.java#L2-L5)

### AddressStatus
The AddressStatus enum manages the lifecycle of an address: ACTIVE or ARCHIVED. An address can be archived to prevent further use while retaining historical data. The Address class provides `archive()` and `activate()` methods for status transitions.

```mermaid
classDiagram
class AddressStatus {
+ACTIVE
+ARCHIVED
}
```

**Diagram sources**
- [AddressStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/address/AddressStatus.java#L2-L5)

**Section sources**
- [AddressStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/address/AddressStatus.java#L2-L5)

### NetworkStatus
The NetworkStatus enum defines the operational states of a blockchain network: ACTIVE, INACTIVE, and MAINTENANCE. Networks can be activated, deactivated, or placed in maintenance mode. The Network class provides corresponding methods and checks availability.

```mermaid
classDiagram
class NetworkStatus {
+ACTIVE
+INACTIVE
+MAINTENANCE
}
```

**Diagram sources**
- [NetworkStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/network/NetworkStatus.java#L2-L6)

**Section sources**
- [NetworkStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/network/NetworkStatus.java#L2-L6)

### StoreStatus
The StoreStatus enum defines the operational states of a store: ACTIVE and INACTIVE. The Store class provides activation and deactivation methods with corresponding domain events.

```mermaid
classDiagram
class StoreStatus {
+ACTIVE
+INACTIVE
}
```

**Diagram sources**
- [StoreStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/store/StoreStatus.java#L2-L5)

**Section sources**
- [StoreStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/store/StoreStatus.java#L2-L5)

### SessionStatus
The SessionStatus enum defines the states of a user session: ACTIVE, INVALIDATED, and EXPIRED. Sessions are validated for activity and expiration.

```mermaid
classDiagram
class SessionStatus {
+ACTIVE
+INVALIDATED
+EXPIRED
}
```

**Diagram sources**
- [SessionStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/user/SessionStatus.java#L5-L20)

**Section sources**
- [SessionStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/user/SessionStatus.java#L5-L20)

## Entity Relationships and Aggregations

The domain model uses aggregations and associations to define relationships between entities. The Wallet is an aggregate root that owns a set of Address entities, referenced by their UUIDs. Similarly, the Vault owns a set of addresses and potentially other entities. The Transaction is associated with a network (via networkId) and involves addresses (fromAddress, toAddress). The Token is linked to a network and has a contract address. These relationships are managed through UUID references rather than direct object references, promoting loose coupling.

```mermaid
erDiagram
Wallet ||--o{ Address : "contains"
Vault ||--o{ Address : "manages"
Transaction }|--|| Network : "on"
Token }|--|| Network : "on"
Transaction }|--|| Wallet : "from/to"
TokenBalance }|--|| Token : "of"
TokenBalance }|--|| Address : "held by"
User ||--o{ Wallet : "owns"
User ||--o{ Session : "has"
Vault ||--o{ Key : "stores"
Store ||--o{ Address : "contains"
Contract ||--o{ Address : "owned by"
```

**Diagram sources**
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java#L27-L242)
- [Address.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/address/Address.java#L11-L132)
- [Vault.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/vault/Vault.java#L8-L96)
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L20-L210)
- [Token.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/token/Token.java#L9-L105)
- [User.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/user/User.java#L18-L235)
- [Network.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/network/Network.java#L7-L114)
- [Store.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/store/Store.java#L12-L100)
- [Contract.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/contract/Contract.java#L13-L113)

**Section sources**
- [Wallet.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/Wallet.java#L27-L242)
- [Address.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/address/Address.java#L11-L132)
- [Vault.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/vault/Vault.java#L8-L96)
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L20-L210)
- [Token.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/token/Token.java#L9-L105)
- [User.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/user/User.java#L18-L235)
- [Network.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/network/Network.java#L7-L114)
- [Store.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/store/Store.java#L12-L100)
- [Contract.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/contract/Contract.java#L13-L113)