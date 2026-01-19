# Domain Layer Documentation

## Overview

The domain layer contains pure business logic with ZERO framework dependencies. This is the heart of the application where business rules live.

**Location**: `src/main/java/dev/bloco/wallet/hub/domain/`

**Key Principle**: NO Spring, JPA, or any framework imports allowed in this layer.

## Domain Models

### Core Aggregates

#### 1. Wallet
**File**: `domain/model/Wallet.java`

Multi-token cryptocurrency wallet with addresses and lifecycle management.

**Key Attributes**:
- `UUID id`: Unique identifier
- `String name`: Wallet name
- `String description`: Wallet description
- `WalletStatus status`: ACTIVE, INACTIVE, DELETED
- `Set<Address> addresses`: Associated blockchain addresses
- `Set<WalletToken> tokens`: Supported tokens
- `BigDecimal balance`: Current balance (computed)

**Methods**:
- `create(UUID id, String name, String description)`: Factory method
- `addFunds(BigDecimal amount)`: Add funds with validation
- `withdrawFunds(BigDecimal amount)`: Withdraw with validation
- `addAddress(Address address)`: Add blockchain address
- `addToken(Token token)`: Add supported token
- `activate()`, `deactivate()`, `delete()`: Lifecycle management

**Business Rules**:
- Balance cannot be negative
- Wallet must be ACTIVE for fund operations
- Cannot add duplicate addresses
- Name is required

**Example**:
```java
Wallet wallet = Wallet.create(UUID.randomUUID(), "My Wallet", "Personal wallet");
wallet.addFunds(BigDecimal.valueOf(100));
wallet.activate();
```

#### 2. User
**File**: `domain/model/user/User.java`

User entity with credentials and profile.

**Key Attributes**:
- `UUID id`: Unique identifier
- `String username`: Login username (unique)
- `String email`: Email address (unique)
- `String passwordHash`: Encrypted password
- `UserStatus status`: ACTIVE, INACTIVE, SUSPENDED
- `TwoFactorAuth twoFactorAuth`: 2FA settings
- `Instant createdAt`, `Instant updatedAt`

**Methods**:
- `create(username, email, password)`: Factory with password hashing
- `authenticate(password)`: Verify credentials
- `changePassword(oldPassword, newPassword)`: Password change with validation
- `enable2FA()`, `disable2FA()`: Two-factor authentication
- `updateProfile(email)`: Update profile information

**Business Rules**:
- Username must be unique and 3-30 characters
- Email must be valid format
- Password must meet complexity requirements (8+ chars, mixed case, number)
- Cannot authenticate if suspended

#### 3. Transaction
**File**: `domain/model/transaction/Transaction.java`

Blockchain transaction with state tracking.

**Key Attributes**:
- `UUID id`
- `TransactionHash hash`: Blockchain transaction hash
- `Address from`, `Address to`: Source and destination
- `BigDecimal amount`
- `TransactionType type`: TRANSFER, CONTRACT_CALL, DEPOSIT, WITHDRAWAL
- `TransactionStatus status`: PENDING, CONFIRMED, FAILED
- `TransactionFee fee`: Gas/network fees
- `Network network`: Blockchain network
- `Instant createdAt`, `Instant confirmedAt`

**Methods**:
- `create(...)`: Factory method
- `confirm()`: Mark as confirmed
- `fail(String reason)`: Mark as failed
- `estimateFee()`: Calculate transaction fee

**Business Rules**:
- Amount must be positive
- Cannot confirm already confirmed transaction
- Fee must be set before confirmation

#### 4. Address
**File**: `domain/model/address/Address.java`

Blockchain address (EOA or contract).

**Key Attributes**:
- `UUID id`
- `String value`: The actual blockchain address (0x...)
- `AddressType type`: EOA (Externally Owned Account) or CONTRACT
- `AddressStatus status`: ACTIVE, INACTIVE
- `Network network`: Associated blockchain network
- `PublicKey publicKey`: Cryptographic public key

**Subtypes**:
- `AccountAddress`: Regular wallet address (EOA)
- `PublicKey`: Public key representation

**Methods**:
- `create(value, network, type)`
- `activate()`, `deactivate()`
- `validate()`: Address format validation

**Business Rules**:
- Address must match network format (ETH: 42 chars, BTC: 26-35 chars)
- Cannot reuse address across networks
- Must be checksummed (Ethereum)

#### 5. Token
**File**: `domain/model/token/Token.java`

ERC20/ERC721 token metadata.

**Key Attributes**:
- `UUID id`
- `String symbol`: Token ticker (ETH, USDT, etc.)
- `String name`: Full name
- `String contractAddress`: Smart contract address
- `TokenType type`: ERC20, ERC721, NATIVE
- `Integer decimals`: Decimal places (18 for ETH)
- `Network network`: Blockchain network

**Methods**:
- `create(...)`
- `toBaseUnit(BigDecimal amount)`: Convert to smallest unit (wei, satoshi)
- `toDisplayUnit(BigInteger baseAmount)`: Convert to human-readable

**Example**:
```java
Token usdt = Token.create(
    UUID.randomUUID(),
    "USDT",
    "Tether USD",
    "0xdac17f958d2ee523a2206206994597c13d831ec7",
    TokenType.ERC20,
    6,
    ethereumNetwork
);

// Convert 100 USDT to base units
BigInteger baseUnits = usdt.toBaseUnit(BigDecimal.valueOf(100)); // 100000000
```

#### 6. Network
**File**: `domain/model/network/Network.java`

Blockchain network configuration (from Chainlist).

**Key Attributes**:
- `UUID id`
- `String name`: Ethereum Mainnet, Polygon, BSC, etc.
- `Integer chainId`: EVM chain ID
- `String rpcUrl`: RPC endpoint
- `String explorerUrl`: Block explorer (Etherscan, etc.)
- `NetworkStatus status`: ACTIVE, INACTIVE, MAINTENANCE
- `String nativeCurrency`: ETH, MATIC, BNB

**Methods**:
- `create(...)`
- `isEVMCompatible()`: Check EVM compatibility

**Common Networks**:
- Ethereum Mainnet (chainId: 1)
- Polygon (chainId: 137)
- BSC (chainId: 56)
- Arbitrum (chainId: 42161)

#### 7. Contract
**File**: `domain/model/contract/Contract.java`

Smart contract interaction.

**Key Attributes**:
- `UUID id`
- `Address address`: Contract address
- `ContractABI abi`: Contract ABI (Application Binary Interface)
- `String name`: Contract name
- `Network network`
- `Set<Address> owners`: Contract owners (multi-sig)

**Methods**:
- `deploy(code, constructor)`: Deploy contract
- `call(method, args)`: Call contract method
- `addOwner(address)`, `removeOwner(address)`: Manage owners

#### 8. Vault
**File**: `domain/model/vault/Vault.java`

Secure key storage (HSM integration).

**Key Attributes**:
- `UUID id`
- `VaultType type`: HSM, KMS, SOFTWARE
- `VaultConfiguration config`: Vault settings
- `VaultStatus status`: ACTIVE, LOCKED, DISABLED

**Methods**:
- `generateKeyPair()`: Generate cryptographic key pair
- `sign(data)`: Sign data with private key
- `encrypt(data)`, `decrypt(data)`: Encryption operations
- `lock()`, `unlock()`: Vault access control

**Integration**:
- Dinamo HSM for production
- Software vault for development

#### 9. Store
**File**: `domain/model/store/Store.java`

Generic secure data storage.

**Key Attributes**:
- `UUID id`
- `String name`
- `Map<String, String> data`: Key-value store
- `StoreStatus status`

**Use Cases**:
- Metadata storage
- Configuration storage
- Temporary data caching

#### 10. TokenBalance
**File**: `domain/model/token/TokenBalance.java`

Wallet token balance.

**Key Attributes**:
- `Address address`
- `Token token`
- `BigDecimal balance`
- `Instant lastUpdated`

**Methods**:
- `add(amount)`: Increase balance
- `subtract(amount)`: Decrease balance
- `refresh()`: Update from blockchain

## Value Objects

Value objects are immutable objects without identity.

### PublicKey
**File**: `domain/model/address/PublicKey.java`

Cryptographic public key.

**Attributes**:
- `String value`: Hex-encoded public key
- `Algorithm algorithm`: ECDSA, EdDSA, RSA

### TransactionHash
**File**: `domain/model/transaction/TransactionHash.java`

Blockchain transaction hash.

**Attributes**:
- `String value`: 0x-prefixed hex string (66 chars for Ethereum)

### TransactionFee
**File**: `domain/model/transaction/TransactionFee.java`

Transaction fee details.

**Attributes**:
- `BigDecimal gasPrice`: Gas price in Gwei
- `BigInteger gasLimit`: Gas limit
- `BigDecimal maxFee`: Maximum fee willing to pay

### VaultConfiguration
**File**: `domain/model/vault/VaultConfiguration.java`

Vault settings.

**Attributes**:
- `String endpoint`: Vault API endpoint
- `Map<String, String> properties`: Custom properties

### ContractABI
**File**: `domain/model/contract/ContractABI.java`

Smart contract ABI.

**Attributes**:
- `String json`: ABI JSON string
- `List<Function> functions`: Parsed functions
- `List<Event> events`: Parsed events

## Domain Events (40+)

All events extend `DomainEvent` base class:

```java
@Getter
public abstract class DomainEvent {
    private final UUID eventId;
    private final Instant occurredOn;
    private final UUID correlationId;
}
```

### Wallet Events (12)

| Event | Description | Payload |
|-------|-------------|---------|
| WalletCreatedEvent | Wallet created | walletId, correlationId |
| WalletUpdatedEvent | Wallet metadata updated | walletId, name, description |
| WalletDeletedEvent | Wallet soft deleted | walletId |
| WalletStatusChangedEvent | Status changed | walletId, oldStatus, newStatus |
| WalletRecoveryInitiatedEvent | Recovery started | walletId, recoveryMethod |
| FundsAddedEvent | Funds added | walletId, amount, correlationId |
| FundsWithdrawnEvent | Funds withdrawn | walletId, amount, correlationId |
| FundsTransferredEvent | Funds transferred | fromWalletId, toWalletId, amount |
| AddressAddedToWalletEvent | Address linked | walletId, addressId |
| AddressRemovedFromWalletEvent | Address unlinked | walletId, addressId |
| TokenAddedToWalletEvent | Token added | walletId, tokenId |
| TokenRemovedFromWalletEvent | Token removed | walletId, tokenId |

### User Events (4)

| Event | Description | Payload |
|-------|-------------|---------|
| UserCreatedEvent | User registered | userId, username, email |
| UserProfileUpdatedEvent | Profile updated | userId, changes |
| UserAuthenticatedEvent | Login successful | userId, sessionId, timestamp |
| UserStatusChangedEvent | Status changed | userId, oldStatus, newStatus |

### Transaction Events (3)

| Event | Description | Payload |
|-------|-------------|---------|
| TransactionCreatedEvent | Transaction initiated | transactionId, from, to, amount |
| TransactionConfirmedEvent | Transaction confirmed on-chain | transactionId, blockNumber, hash |
| TransactionStatusChangedEvent | Status updated | transactionId, oldStatus, newStatus |

### Address Events (2)

| Event | Description | Payload |
|-------|-------------|---------|
| AddressCreatedEvent | Address generated | addressId, value, network |
| AddressStatusChangedEvent | Status changed | addressId, status |

### Token Events (2)

| Event | Description | Payload |
|-------|-------------|---------|
| TokenCreatedEvent | Token added | tokenId, symbol, contract |
| TokenBalanceChangedEvent | Balance updated | addressId, tokenId, oldBalance, newBalance |

### Network Events (3)

| Event | Description | Payload |
|-------|-------------|---------|
| NetworkCreatedEvent | Network added | networkId, chainId, name |
| NetworkAddedEvent | Network enabled | networkId |
| NetworkStatusChangedEvent | Status changed | networkId, status |

### Contract Events (4)

| Event | Description | Payload |
|-------|-------------|---------|
| ContractDeployedEvent | Contract deployed | contractId, address, deployer |
| ContractInteractionEvent | Contract called | contractId, method, args |
| ContractOwnerAddedEvent | Owner added | contractId, ownerId |
| ContractOwnerRemovedEvent | Owner removed | contractId, ownerId |

### Vault Events (3)

| Event | Description | Payload |
|-------|-------------|---------|
| VaultCreatedEvent | Vault initialized | vaultId, type |
| KeyPairGeneratedEvent | Keys generated | vaultId, publicKey |
| VaultStatusChangedEvent | Status changed | vaultId, status |

### Store Events (3)

| Event | Description | Payload |
|-------|-------------|---------|
| StoreCreatedEvent | Store created | storeId, name |
| AddressAddedToStoreEvent | Address stored | storeId, addressId |
| StoreStatusChangedEvent | Status changed | storeId, status |

## Domain Gateways (Port Interfaces)

Located in `domain/gateway/`:

| Gateway | Purpose | Key Methods |
|---------|---------|-------------|
| WalletRepository | Wallet persistence | findById, save, update, delete, findByUserId |
| UserRepository | User persistence | findById, save, findByUsername, findByEmail |
| TransactionRepository | Transaction persistence | findById, save, findByWallet, findPending |
| AddressRepository | Address persistence | findById, save, findByWallet, findByValue |
| TokenRepository | Token persistence | findById, save, findBySymbol, findByNetwork |
| TokenBalanceRepository | Balance tracking | findByAddress, updateBalance |
| NetworkRepository | Network config | findById, findByChainId, findActive |
| ContractRepository | Contract data | findById, save, findByAddress |
| VaultRepository | Vault access | findById, save, findActive |
| StoreRepository | Store persistence | findById, save, findByName |
| UserSessionRepository | Session management | findById, save, findByUserId |
| TransactionFeeRepository | Fee estimation | estimateFee, findByNetwork |
| WalletTokenRepository | Wallet-token links | findByWallet, save, delete |
| DomainEventPublisher | Event publishing | publish(DomainEvent) |

**Important**: These are INTERFACES only. Implementations are in infrastructure layer.

## Design Patterns Used

### 1. Aggregate Root

Wallet, User, Transaction are aggregate roots:
- Enforce invariants
- Handle domain events
- Transactional boundaries

### 2. Value Objects

Immutable objects with no identity:
- PublicKey
- TransactionHash
- TransactionFee
- VaultConfiguration
- ContractABI

### 3. Factory Methods

Static factory methods for creation:
```java
Wallet wallet = Wallet.create(id, name, description);
User user = User.create(username, email, password);
Transaction tx = Transaction.create(from, to, amount, network);
```

### 4. Domain Events

All state changes produce events:
```java
public void addFunds(BigDecimal amount) {
    validateAmount(amount);
    this.balance = this.balance.add(amount);
    // Event published by use case layer
}
```

### 5. Repository Pattern

Abstract data access through interfaces.

## Invariants & Business Rules

### Wallet Invariants
- Balance ≥ 0
- Name not empty
- Cannot add funds to inactive wallet
- Cannot have duplicate addresses

### User Invariants
- Username unique
- Email unique and valid
- Password meets requirements (8+ chars, mixed case, number)
- Cannot authenticate if suspended

### Transaction Invariants
- Amount > 0
- From ≠ To
- Fee must be set
- Cannot confirm twice

### Address Invariants
- Value matches network format
- Cannot have multiple active addresses with same value
- Checksummed addresses for Ethereum

### Token Invariants
- Symbol not empty
- Decimals between 0 and 18
- Contract address valid for token type

### Network Invariants
- Chain ID unique
- RPC URL valid
- Native currency specified

## Testing the Domain

Since domain has no dependencies:

```java
@Test
void testAddFunds() {
    Wallet wallet = Wallet.create(UUID.randomUUID(), "Test", "");
    wallet.addFunds(BigDecimal.valueOf(100));
    assertEquals(BigDecimal.valueOf(100), wallet.getBalance());
}

@Test
void testCannotAddNegativeFunds() {
    Wallet wallet = Wallet.create(UUID.randomUUID(), "Test", "");
    assertThrows(InvalidAmountException.class, () -> {
        wallet.addFunds(BigDecimal.valueOf(-10));
    });
}

@Test
void testUserAuthentication() {
    User user = User.create("testuser", "test@example.com", "Password123");
    assertTrue(user.authenticate("Password123"));
    assertFalse(user.authenticate("WrongPassword"));
}
```

No mocking, no Spring context - pure unit tests!

## Domain Services

Domain services contain business logic that doesn't naturally fit in a single aggregate:

### AddressValidator
**Location**: `domain/service/AddressValidator.java`

Validates blockchain addresses across different networks.

**Methods**:
- `validate(String address, Network network)`: Validate address format
- `checksum(String address)`: Calculate Ethereum checksum

### TransactionFeeCalculator
**Location**: `domain/service/TransactionFeeCalculator.java`

Calculates transaction fees for different networks.

**Methods**:
- `estimate(Transaction tx)`: Estimate gas fees
- `calculateMaxFee(gasPrice, gasLimit)`: Calculate maximum fee

## Enumerations

### WalletStatus
- `ACTIVE`: Wallet is active and can be used
- `INACTIVE`: Wallet is disabled
- `DELETED`: Wallet is soft-deleted

### UserStatus
- `ACTIVE`: User can log in
- `INACTIVE`: User account disabled
- `SUSPENDED`: User temporarily suspended

### TransactionStatus
- `PENDING`: Transaction created but not confirmed
- `CONFIRMED`: Transaction confirmed on blockchain
- `FAILED`: Transaction failed

### TransactionType
- `TRANSFER`: Simple token transfer
- `CONTRACT_CALL`: Smart contract interaction
- `DEPOSIT`: Deposit to wallet
- `WITHDRAWAL`: Withdrawal from wallet

### AddressType
- `EOA`: Externally Owned Account (user wallet)
- `CONTRACT`: Smart contract address

### TokenType
- `NATIVE`: Native blockchain token (ETH, MATIC, BNB)
- `ERC20`: Fungible token
- `ERC721`: Non-fungible token (NFT)

### NetworkStatus
- `ACTIVE`: Network is operational
- `INACTIVE`: Network disabled
- `MAINTENANCE`: Network under maintenance

### VaultType
- `HSM`: Hardware Security Module
- `KMS`: Key Management Service
- `SOFTWARE`: Software-based vault (dev only)

## Best Practices

1. **Keep domain pure**: No framework dependencies ever
2. **Rich domain models**: Behavior in entities, not anemic DTOs
3. **Immutable events**: Domain events should never change
4. **Validate early**: Business rules enforced at domain level
5. **Use factories**: Static factory methods for complex creation
6. **Test thoroughly**: Domain logic should have 100% test coverage
7. **Clear invariants**: Document and enforce all business rules
8. **Domain language**: Use ubiquitous language from business
