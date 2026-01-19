# Transaction

<cite>
**Referenced Files in This Document**   
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java) - *Updated in recent commit*
- [TransactionHash.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionHash.java)
- [TransactionStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionStatus.java)
- [TransactionCreatedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/transaction/TransactionCreatedEvent.java)
- [TransactionStatusChangedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/transaction/TransactionStatusChangedEvent.java)
- [ConfirmTransactionUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/ConfirmTransactionUseCase.java)
- [FailTransactionUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/FailTransactionUseCase.java)
- [TransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/TransactionRepository.java)
- [Network.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/network/Network.java)
- [Token.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/token/Token.java)
- [FeeEstimate.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeEstimate.java) - *Added in recent commit*
- [FeeEstimateResult.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeEstimateResult.java) - *Added in recent commit*
- [FeeLevel.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeLevel.java) - *Added in recent commit*
- [BlockchainTransactionType.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/BlockchainTransactionType.java) - *Added in recent commit, replaces TransactionType*
- [TransactionType.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionType.java) - *Deprecated, replaced by BlockchainTransactionType*
- [EstimateTransactionFeeUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/EstimateTransactionFeeUseCase.java) - *Updated for new fee estimation models*
</cite>

## Update Summary
**Changes Made**   
- Added new section on Transaction Fee Estimation Models covering FeeEstimate and FeeEstimateResult
- Added new section on BlockchainTransactionType to document the renamed TransactionType enum
- Updated relationships section to include new fee estimation components
- Added new sequence diagram for fee estimation workflow
- Updated document sources to include new and modified files
- Marked TransactionType as deprecated in sources

## Table of Contents
1. [Introduction](#introduction)
2. [Core Fields](#core-fields)
3. [TransactionHash Value Object](#transactionhash-value-object)
4. [Status Lifecycle and Transitions](#status-lifecycle-and-transitions)
5. [Factory Methods](#factory-methods)
6. [Business Rules and Domain Events](#business-rules-and-domain-events)
7. [Relationships with Other Entities](#relationships-with-other-entities)
8. [Lifecycle Management Example](#lifecycle-management-example)
9. [Validation and Error Conditions](#validation-and-error-conditions)
10. [Integration with Use Cases](#integration-with-use-cases)
11. [Transaction Fee Estimation Models](#transaction-fee-estimation-models)
12. [BlockchainTransactionType](#blockchaintransactiontype)

## Introduction
The `Transaction` entity in the bloco-wallet-java application represents a blockchain transaction that occurs within a specific network. It encapsulates critical information such as sender and recipient addresses, value transferred, gas parameters, and transaction status. As an aggregate root, it manages its own lifecycle and emits domain events to reflect state changes. This document details the structure, behavior, and integration points of the Transaction entity.

**Section sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L1-L50)

## Core Fields
The Transaction entity contains the following core fields:

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier for the transaction |
| networkId | UUID | Identifier of the blockchain network where the transaction occurs |
| hash | TransactionHash | Immutable value object representing the transaction hash |
| fromAddress | String | Sender's blockchain address |
| toAddress | String | Recipient's blockchain address |
| value | BigDecimal | Value being transferred in the transaction |
| gasPrice | BigDecimal | Price per unit of gas (mutable after creation) |
| gasLimit | BigDecimal | Maximum amount of gas allowed for the transaction (mutable after creation) |
| gasUsed | BigDecimal | Actual amount of gas consumed by the transaction (set on confirmation) |
| data | String | Additional data or payload associated with the transaction |
| timestamp | Instant | Time when the transaction was created |
| blockNumber | Long | Block number in which the transaction was included (set on confirmation) |
| blockHash | String | Hash of the block containing the transaction (set on confirmation) |
| status | TransactionStatus | Current status of the transaction (PENDING, CONFIRMED, FAILED) |

These fields are initialized during transaction creation, with gas-related and block-related fields populated as the transaction progresses through its lifecycle.

**Section sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L20-L45)

## TransactionHash Value Object
The `TransactionHash` class is an immutable value object that encapsulates the transaction hash string with built-in validation.

It enforces the following rules:
- Hash value cannot be null or blank
- Implements proper equality and hashing based on the value
- Provides a clean toString() representation

The validation occurs in the constructor via the `validateHash()` method, which throws an `IllegalArgumentException` if the input is invalid. This ensures that only valid hashes can be associated with a transaction, maintaining data integrity across the system.

```mermaid
classDiagram
class TransactionHash {
-String value
+TransactionHash(String value)
+String getValue()
+boolean equals(Object o)
+int hashCode()
+String toString()
}
```

**Diagram sources**
- [TransactionHash.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionHash.java#L4-L41)

**Section sources**
- [TransactionHash.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionHash.java#L4-L41)

## Status Lifecycle and Transitions
The Transaction entity follows a strict state lifecycle defined by the `TransactionStatus` enum, which includes three states: PENDING, CONFIRMED, and FAILED.

### Status States
- **PENDING**: Initial state when a transaction is created and broadcast to the network
- **CONFIRMED**: Final state when the transaction has been successfully included in a block
- **FAILED**: Final state when the transaction execution failed

All transactions start in the PENDING state and can transition to either CONFIRMED or FAILED, but never back to PENDING once confirmed or failed.

### Transition Methods
The following methods manage state transitions:

#### confirm()
Transitions the transaction from PENDING to CONFIRMED state by:
- Setting blockNumber, blockHash, and gasUsed
- Updating status to CONFIRMED
- Registering a `TransactionStatusChangedEvent`

```mermaid
sequenceDiagram
participant UseCase as ConfirmTransactionUseCase
participant Transaction as Transaction
participant EventPublisher as DomainEventPublisher
UseCase->>Transaction : confirm(blockNumber, blockHash, gasUsed)
Transaction->>Transaction : Update block info and status
Transaction->>Transaction : registerEvent(TransactionStatusChangedEvent)
Transaction-->>UseCase : Status updated
```

**Diagram sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L145-L156)
- [ConfirmTransactionUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/ConfirmTransactionUseCase.java#L25-L38)

#### fail()
Transitions the transaction from PENDING to FAILED state by:
- Setting status to FAILED
- Recording the failure reason
- Registering a `TransactionStatusChangedEvent` with the reason

#### setGasInfo()
Updates gas parameters (gasPrice, gasLimit) while the transaction is still pending. This method does not change the transaction status but updates execution parameters.

```mermaid
stateDiagram-v2
[*] --> PENDING
PENDING --> CONFIRMED : confirm()
PENDING --> FAILED : fail(reason)
CONFIRMED --> [*]
FAILED --> [*]
```

**Diagram sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L145-L171)
- [TransactionStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionStatus.java#L2-L6)

**Section sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L145-L182)
- [TransactionStatus.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionStatus.java#L2-L6)

## Factory Methods
The Transaction class provides two static factory methods for instance creation:

### create()
Creates a new transaction with default PENDING status and emits a `TransactionCreatedEvent`. This method should be used when initiating a new transaction in the system.

It initializes:
- All core fields from parameters
- Timestamp to current time
- Status to PENDING
- Registers a `TransactionCreatedEvent` containing transaction metadata

```mermaid
flowchart TD
Start([create method]) --> ValidateInputs["Validate input parameters"]
ValidateInputs --> CreateInstance["Create Transaction instance"]
CreateInstance --> SetStatus["Set status = PENDING"]
SetStatus --> RegisterEvent["Register TransactionCreatedEvent"]
RegisterEvent --> ReturnInstance["Return new Transaction"]
```

**Diagram sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L49-L63)
- [TransactionCreatedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/transaction/TransactionCreatedEvent.java#L10-L20)

### rehydrate()
Reconstructs a Transaction instance from persisted state without emitting domain events. This method is used when loading transactions from the database through repositories.

It allows setting all fields including:
- Block information (blockNumber, blockHash)
- Gas usage (gasUsed)
- Current status (PENDING, CONFIRMED, or FAILED)
- Historical timestamp

Unlike `create()`, this method does not register any events, preserving the event stream integrity when restoring from storage.

**Section sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L49-L93)

## Business Rules and Domain Events
The Transaction entity enforces several business rules through its methods and state management:

### State Transition Guards
- Cannot confirm or fail a transaction that is already CONFIRMED or FAILED
- Status can only be changed from PENDING to either CONFIRMED or FAILED
- Once in terminal state (CONFIRMED/FAILED), no further state changes are allowed

### Domain Event Emissions
The entity emits domain events to communicate state changes:

- **TransactionCreatedEvent**: Emitted when a new transaction is created via the `create()` method
- **TransactionStatusChangedEvent**: Emitted when status changes via `confirm()` or `fail()`
- **TransactionConfirmedEvent**: Published by `ConfirmTransactionUseCase` after successful confirmation

These events enable reactive processing across the system, such as balance updates, notifications, and audit logging.

```mermaid
classDiagram
class TransactionCreatedEvent {
+UUID transactionId
+UUID networkId
+String transactionHash
+String fromAddress
+String toAddress
}
class TransactionStatusChangedEvent {
+UUID transactionId
+TransactionStatus oldStatus
+TransactionStatus newStatus
+String reason
}
class TransactionConfirmedEvent {
+UUID transactionId
+long blockNumber
+String blockHash
+BigDecimal gasUsed
}
TransactionCreatedEvent <|-- Transaction
TransactionStatusChangedEvent <|-- Transaction
TransactionConfirmedEvent <|-- ConfirmTransactionUseCase
```

**Diagram sources**
- [TransactionCreatedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/transaction/TransactionCreatedEvent.java#L6-L25)
- [TransactionStatusChangedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/transaction/TransactionStatusChangedEvent.java#L6-L20)
- [TransactionConfirmedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/transaction/TransactionConfirmedEvent.java#L6-L20)

**Section sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L49-L171)
- [TransactionCreatedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/transaction/TransactionCreatedEvent.java#L6-L25)
- [TransactionStatusChangedEvent.java](file://src/main/java/dev/bloco/wallet/hub/domain/event/transaction/TransactionStatusChangedEvent.java#L6-L20)

## Relationships with Other Entities
The Transaction entity interacts with several other domain entities:

### Network
- Associated via `networkId` field
- Transactions occur within the context of a specific blockchain network
- Network configuration affects transaction processing and validation

### Wallet
- While not directly referenced, transactions are typically initiated from and to wallet addresses
- Wallet balance changes are a consequence of transaction confirmation
- Wallet use cases (TransferFunds, AddFunds) create transactions

### Token
- For token transfers, the transaction data field contains token transfer details
- Token contract address may be the toAddress in ERC-20 transfers
- Token decimals and type affect value interpretation

### Fee Estimation Components
- **FeeEstimate**: Contains detailed fee information for a specific fee level
- **FeeEstimateResult**: Aggregates multiple FeeEstimate objects for a network
- **FeeLevel**: Enumerates different fee levels (SLOW, STANDARD, FAST, URGENT)
- **BlockchainTransactionType**: Categorizes transactions for gas estimation purposes

```mermaid
erDiagram
TRANSACTION ||--o{ NETWORK : "occurs on"
TRANSACTION }|--|| TRANSACTION_STATUS : "has"
TRANSACTION }o--o| TOKEN : "involves"
TRANSACTION }o--o| WALLET : "affects"
TRANSACTION }|--|| FEE_ESTIMATE_RESULT : "uses"
FEE_ESTIMATE_RESULT }|--|| FEE_ESTIMATE : "contains"
FEE_ESTIMATE }|--|| FEE_LEVEL : "has"
TRANSACTION }|--|| BLOCKCHAIN_TRANSACTION_TYPE : "categorized as"
TRANSACTION {
UUID id PK
UUID networkId FK
String hash
String fromAddress
String toAddress
BigDecimal value
BigDecimal gasPrice
BigDecimal gasLimit
BigDecimal gasUsed
String data
Instant timestamp
Long blockNumber
String blockHash
String status
}
NETWORK {
UUID id PK
String name
String chainId
String rpcUrl
String explorerUrl
String status
}
TRANSACTION_STATUS {
String status PK
}
FEE_ESTIMATE_RESULT {
UUID networkId PK
BigDecimal gasLimit
}
FEE_ESTIMATE {
FeeLevel level PK
BigDecimal gasPrice
BigDecimal totalCost
}
FEE_LEVEL {
String level PK
}
BLOCKCHAIN_TRANSACTION_TYPE {
String type PK
}
```

**Diagram sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L20-L45)
- [Network.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/network/Network.java#L10-L25)
- [Token.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/token/Token.java#L10-L25)
- [FeeEstimateResult.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeEstimateResult.java#L1-L20)
- [FeeEstimate.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeEstimate.java#L1-L21)
- [FeeLevel.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeLevel.java#L1-L31)
- [BlockchainTransactionType.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/BlockchainTransactionType.java#L1-L44)

**Section sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L20-L45)
- [Network.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/network/Network.java#L10-L25)
- [Token.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/token/Token.java#L10-L25)
- [FeeEstimateResult.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeEstimateResult.java#L1-L20)
- [FeeEstimate.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeEstimate.java#L1-L21)
- [FeeLevel.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeLevel.java#L1-L31)
- [BlockchainTransactionType.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/BlockchainTransactionType.java#L1-L44)

## Lifecycle Management Example
The typical transaction lifecycle follows this sequence:

1. **Creation**: `Transaction.create()` is called with basic transaction details
2. **Gas Setup**: `setGasInfo()` is called to specify execution parameters
3. **Confirmation**: `confirm()` is called when the transaction is mined
4. **Persistence**: Updated transaction is saved via `TransactionRepository.save()`

```mermaid
sequenceDiagram
participant Client as "Client"
participant UseCase as "CreateTransactionUseCase"
participant Tx as "Transaction"
participant Repo as "TransactionRepository"
participant Events as "EventPublisher"
Client->>UseCase : Initiate transaction
UseCase->>Tx : Transaction.create(...)
Tx->>Tx : Set status=PENDING
Tx->>Tx : registerEvent(TransactionCreatedEvent)
UseCase->>Tx : setGasInfo(gasPrice, gasLimit)
UseCase->>Repo : save(transaction)
Repo->>Repo : Persist to database
Repo-->>UseCase : Saved transaction
UseCase-->>Client : Transaction created
Client->>UseCase : Confirm transaction
UseCase->>Repo : findById(transactionId)
Repo-->>UseCase : Transaction
UseCase->>Tx : confirm(blockNumber, blockHash, gasUsed)
Tx->>Tx : Update block info
Tx->>Tx : Set status=CONFIRMED
Tx->>Tx : registerEvent(TransactionStatusChangedEvent)
UseCase->>Repo : save(transaction)
UseCase->>Events : publish(TransactionConfirmedEvent)
UseCase-->>Client : Transaction confirmed
```

**Diagram sources**
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L49-L156)
- [TransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/TransactionRepository.java#L10-L15)
- [ConfirmTransactionUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/ConfirmTransactionUseCase.java#L25-L38)

## Validation and Error Conditions
The system enforces validation at multiple levels:

### TransactionHash Validation
- Rejects null or blank hash values
- Throws `IllegalArgumentException` on invalid input
- Ensures hash integrity throughout the transaction lifecycle

### State Transition Validation
- Prevents status changes on already confirmed/failed transactions
- Ensures only valid state transitions (PENDING → CONFIRMED/FAILED)
- Maintains business rule consistency

### Repository-Level Validation
- `TransactionRepository` provides `existsById` and `existsByHash` methods to prevent duplicates
- Enforces uniqueness constraints at persistence layer
- Validates network and address existence through related repositories

Error conditions include:
- Attempting to confirm/fail non-existent transactions
- Invalid hash format during creation
- Duplicate transaction hash detection
- Network unavailability during processing

**Section sources**
- [TransactionHash.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionHash.java#L10-L15)
- [Transaction.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/Transaction.java#L145-L171)
- [TransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/TransactionRepository.java#L50-L55)

## Integration with Use Cases
The Transaction entity is integrated with key use cases in the system:

### ConfirmTransactionUseCase
Handles transaction confirmation by:
- Retrieving transaction from repository
- Calling `confirm()` method with block details
- Persisting updated transaction
- Publishing `TransactionConfirmedEvent`

### FailTransactionUseCase
Handles transaction failure by:
- Retrieving transaction from repository
- Calling `fail()` method with reason
- Persisting updated transaction
- Publishing `TransactionStatusChangedEvent`

These use cases enforce proper transaction lifecycle management and ensure consistent event publishing across the system.

```mermaid
classDiagram
class ConfirmTransactionUseCase {
+TransactionRepository transactionRepository
+DomainEventPublisher eventPublisher
+void confirm(UUID, long, String, BigDecimal, String)
}
class FailTransactionUseCase {
+TransactionRepository transactionRepository
+DomainEventPublisher eventPublisher
+void fail(UUID, String, String)
}
class TransactionRepository {
+Transaction save(Transaction)
+Optional<Transaction> findById(UUID)
+boolean existsByHash(String)
}
ConfirmTransactionUseCase --> Transaction : "calls confirm()"
FailTransactionUseCase --> Transaction : "calls fail()"
ConfirmTransactionUseCase --> TransactionRepository : "uses"
FailTransactionUseCase --> TransactionRepository : "uses"
ConfirmTransactionUseCase --> DomainEventPublisher : "publishes events"
FailTransactionUseCase --> DomainEventPublisher : "publishes events"
```

**Diagram sources**
- [ConfirmTransactionUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/ConfirmTransactionUseCase.java#L10-L15)
- [FailTransactionUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/FailTransactionUseCase.java#L10-L15)
- [TransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/TransactionRepository.java#L10-L15)

**Section sources**
- [ConfirmTransactionUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/ConfirmTransactionUseCase.java#L10-L38)
- [FailTransactionUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/FailTransactionUseCase.java#L10-L34)
- [TransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/domain/gateway/TransactionRepository.java#L10-L55)

## Transaction Fee Estimation Models
The system introduces new fee estimation models to provide comprehensive transaction cost analysis.

### FeeEstimate
Represents a fee estimate for a specific fee level with detailed information:

| Field | Type | Description |
|-------|------|-------------|
| level | FeeLevel | The fee level (SLOW, STANDARD, FAST, URGENT) |
| gasPrice | BigDecimal | Price per unit of gas |
| baseFee | BigDecimal | Base fee component (for EIP-1559 networks) |
| priorityFee | BigDecimal | Priority fee component (for EIP-1559 networks) |
| totalCost | BigDecimal | Total estimated transaction cost |
| estimatedTime | String | Expected confirmation time |
| description | String | User-friendly description of the fee level |

```mermaid
classDiagram
class FeeEstimate {
+FeeLevel level
+BigDecimal gasPrice
+BigDecimal baseFee
+BigDecimal priorityFee
+BigDecimal totalCost
+String estimatedTime
+String description
}
```

**Diagram sources**
- [FeeEstimate.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeEstimate.java#L1-L21)

### FeeEstimateResult
Aggregates fee estimates for all levels on a specific network:

| Field | Type | Description |
|-------|------|-------------|
| networkId | UUID | Identifier of the network |
| networkName | String | Name of the network |
| gasLimit | BigDecimal | Estimated gas limit for the transaction |
| estimates | List<FeeEstimate> | Collection of fee estimates for all levels |

```mermaid
sequenceDiagram
participant Client as "Client"
participant UseCase as "EstimateTransactionFeeUseCase"
participant NetworkRepo as "NetworkRepository"
participant FeeRepo as "TransactionFeeRepository"
participant Result as "FeeEstimateResult"
Client->>UseCase : estimateTransactionFee(networkId, gasLimit)
UseCase->>NetworkRepo : findById(networkId)
NetworkRepo-->>UseCase : Network
UseCase->>FeeRepo : findLatestByNetworkIdAndLevel() for each FeeLevel
FeeRepo-->>UseCase : TransactionFee objects
UseCase->>UseCase : Create FeeEstimate for each level
UseCase->>UseCase : Build FeeEstimateResult
UseCase-->>Client : Return FeeEstimateResult
```

**Diagram sources**
- [EstimateTransactionFeeUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/EstimateTransactionFeeUseCase.java#L49-L77)
- [FeeEstimateResult.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeEstimateResult.java#L1-L20)

**Section sources**
- [FeeEstimate.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeEstimate.java#L1-L21)
- [FeeEstimateResult.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/FeeEstimateResult.java#L1-L20)
- [EstimateTransactionFeeUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/EstimateTransactionFeeUseCase.java#L49-L77)

## BlockchainTransactionType
The `TransactionType` enum has been renamed to `BlockchainTransactionType` for consistency and enhanced with gas estimation capabilities.

### Enumeration Values
- **SIMPLE_TRANSFER**: Simple native token transfer (e.g., ETH transfer), typical gas: 21,000
- **ERC20_TRANSFER**: ERC20 token transfer, typical gas: 65,000
- **ERC721_TRANSFER**: ERC721 (NFT) token transfer, typical gas: 85,000
- **CONTRACT_INTERACTION**: Smart contract interaction (function call), typical gas: 150,000
- **CONTRACT_DEPLOYMENT**: Smart contract deployment, typical gas: 500,000
- **COMPLEX_DEFI**: Complex DeFi operations (swaps, liquidity provision, etc.), typical gas: 300,000

This enum is used by the `EstimateTransactionFeeUseCase` to determine appropriate gas limits for different transaction types.

```mermaid
classDiagram
class BlockchainTransactionType {
+SIMPLE_TRANSFER
+ERC20_TRANSFER
+ERC721_TRANSFER
+CONTRACT_INTERACTION
+CONTRACT_DEPLOYMENT
+COMPLEX_DEFI
}
```

**Diagram sources**
- [BlockchainTransactionType.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/BlockchainTransactionType.java#L1-L44)

**Section sources**
- [BlockchainTransactionType.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/BlockchainTransactionType.java#L1-L44)
- [EstimateTransactionFeeUseCase.java](file://src/main/java/dev/bloco/wallet/hub/usecase/EstimateTransactionFeeUseCase.java#L100-L108)
- [TransactionType.java](file://src/main/java/dev/bloco/wallet/hub/domain/model/transaction/TransactionType.java#L1-L7) - *Deprecated*