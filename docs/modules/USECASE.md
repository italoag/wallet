# Use Case Layer Documentation

## Overview

The Use Case layer implements the application business logic for the Wallet Hub system. It orchestrates domain operations, coordinates infrastructure components, and enforces business rules while maintaining complete independence from delivery mechanisms (REST, events, etc.).

**Package:** `dev.bloco.wallet.hub.usecase`
**Architecture Pattern:** Hexagonal Architecture (Application Layer)
**Total Use Cases:** 33

### Architectural Principles

1. **Pure Business Logic**: Use cases contain application-specific business rules
2. **Dependency Inversion**: Depends on domain abstractions (gateway interfaces)
3. **Framework Independence**: No Spring annotations in use case logic (records are used)
4. **Single Responsibility**: Each use case handles one business operation
5. **Event-Driven**: Publishes domain events for cross-cutting concerns
6. **Transaction Boundaries**: Defines transactional consistency boundaries

### Common Patterns

All use cases follow these patterns:

```java
public record XxxUseCase(
    Repository1 repository1,
    Repository2 repository2,
    DomainEventPublisher eventPublisher) {

    public Result execute(Input input, String correlationId) {
        // 1. Validate inputs
        // 2. Retrieve domain entities
        // 3. Execute business logic
        // 4. Persist changes
        // 5. Publish domain events
        // 6. Return result
    }
}
```

### Dependency Structure

```
UseCase → Domain Gateway (Port) ← Infrastructure Adapter
       ↓
   Domain Models & Events
```

---

## Use Case Categories

### 1. Wallet Management (10 use cases)

#### 1.1 CreateWalletUseCase

**Purpose:** Creates new wallets for users with default configuration.

**Input Parameters:**
- `userId` (UUID) - User requesting the wallet
- `correlationId` (String) - Trace identifier

**Output:** `Wallet` - Newly created wallet instance

**Dependencies:**
- `WalletRepository` - Wallet persistence
- `DomainEventPublisher` - Event publishing

**Domain Events:**
- `WalletCreatedEvent` - Published after successful creation

**Business Rules:**
- Wallet ID is auto-generated (UUID)
- Default name: "Default Wallet"
- Initial status: ACTIVE
- Balance starts at zero

**Example Usage:**
```java
CreateWalletUseCase useCase = new CreateWalletUseCase(walletRepo, eventPublisher);
Wallet wallet = useCase.createWallet(userId, correlationId);
```

**Error Handling:**
- No validation errors (minimal input requirements)

---

#### 1.2 UpdateWalletUseCase

**Purpose:** Updates wallet metadata (name and description).

**Input Parameters:**
- `walletId` (UUID) - Wallet to update
- `name` (String, nullable) - New name
- `description` (String, nullable) - New description
- `correlationId` (String) - Trace identifier

**Output:** `Wallet` - Updated wallet instance

**Dependencies:**
- `WalletRepository`
- `DomainEventPublisher`

**Domain Events:**
- `WalletUpdatedEvent` - Published after successful update

**Business Rules:**
- Wallet must exist
- Wallet must be ACTIVE
- At least one field (name or description) must be provided
- Null values preserve current values

**Example Usage:**
```java
UpdateWalletUseCase useCase = new UpdateWalletUseCase(walletRepo, eventPublisher);
Wallet updated = useCase.updateWallet(walletId, "My Savings", null, correlationId);
```

**Error Handling:**
- `IllegalArgumentException` - Wallet not found or both fields null
- `IllegalStateException` - Wallet not active

---

#### 1.3 ActivateWalletUseCase

**Purpose:** Activates a wallet, enabling all operations.

**Input Parameters:**
- `walletId` (UUID) - Wallet to activate
- `correlationId` (String) - Trace identifier

**Output:** `Wallet` - Activated wallet

**Dependencies:**
- `WalletRepository`
- `DomainEventPublisher`

**Domain Events:**
- `WalletStatusChangedEvent` - Published on status change

**Business Rules:**
- Wallet must exist
- Deleted wallets cannot be activated
- Already active wallets remain active

**Example Usage:**
```java
ActivateWalletUseCase useCase = new ActivateWalletUseCase(walletRepo, eventPublisher);
Wallet active = useCase.activateWallet(walletId, correlationId);
```

---

#### 1.4 DeactivateWalletUseCase

**Purpose:** Deactivates a wallet, restricting operations.

**Input Parameters:**
- `walletId` (UUID) - Wallet to deactivate
- `correlationId` (String) - Trace identifier

**Output:** `Wallet` - Deactivated wallet

**Dependencies:**
- `WalletRepository`
- `DomainEventPublisher`

**Domain Events:**
- `WalletStatusChangedEvent`

**Business Rules:**
- Wallet must exist
- Deleted wallets cannot be deactivated
- Deactivated wallets can be reactivated

---

#### 1.5 DeleteWalletUseCase

**Purpose:** Soft-deletes a wallet (retains for audit).

**Input Parameters:**
- `walletId` (UUID) - Wallet to delete
- `reason` (String) - Deletion reason (required)
- `correlationId` (String) - Trace identifier

**Output:** `Wallet` - Deleted wallet

**Dependencies:**
- `WalletRepository`
- `DomainEventPublisher`

**Domain Events:**
- `WalletStatusChangedEvent`
- `WalletDeletedEvent`

**Business Rules:**
- Wallet must exist
- Balance must be zero
- Reason must be provided
- Deletion is irreversible (soft delete)

**Example Usage:**
```java
DeleteWalletUseCase useCase = new DeleteWalletUseCase(walletRepo, eventPublisher);
Wallet deleted = useCase.deleteWallet(walletId, "User request", correlationId);
```

**Error Handling:**
- `IllegalArgumentException` - Wallet not found or reason missing
- `IllegalStateException` - Non-zero balance or already deleted

---

#### 1.6 RecoverWalletUseCase

**Purpose:** Initiates and completes wallet recovery from seed phrase or backup.

**Input Parameters (Initiate):**
- `userId` (UUID) - User recovering the wallet
- `walletName` (String) - Name for recovered wallet
- `recoveryMethod` (String) - Recovery method ("seed_phrase", "backup")
- `correlationId` (String) - Trace identifier

**Input Parameters (Complete):**
- `walletId` (UUID) - Wallet being recovered
- `correlationId` (String) - Trace identifier

**Output:** `Wallet` - Recovered wallet instance

**Dependencies:**
- `WalletRepository`
- `DomainEventPublisher`

**Domain Events:**
- `WalletCreatedEvent`
- `WalletRecoveryInitiatedEvent`
- `WalletStatusChangedEvent`

**Business Rules:**
- User ID must be provided
- Recovery method must be specified
- Wallet starts in RECOVERING status
- Completion activates the wallet

**Example Usage:**
```java
RecoverWalletUseCase useCase = new RecoverWalletUseCase(walletRepo, eventPublisher);
Wallet recovering = useCase.recoverWallet(userId, "Recovered Wallet", "seed_phrase", correlationId);
// ... recovery process ...
Wallet active = useCase.completeRecovery(recovering.getId(), correlationId);
```

---

#### 1.7 GetWalletDetailsUseCase

**Purpose:** Retrieves comprehensive wallet information.

**Input Parameters:**
- `walletId` (UUID) - Wallet to retrieve
- `includeDeleted` (boolean, optional) - Include deleted wallets

**Output:** `Wallet` - Wallet with full details

**Dependencies:**
- `WalletRepository`
- `AddressRepository`

**Domain Events:** None (read-only)

**Business Rules:**
- Wallet must exist
- Deleted wallets included only if requested
- Provides accessibility check

**Methods:**
- `getWalletDetails(UUID walletId)` - Get basic wallet info
- `getWallet(UUID walletId, boolean includeDeleted)` - Get with deletion filter
- `isWalletAccessible(UUID walletId)` - Check accessibility

---

#### 1.8 ListWalletsUseCase

**Purpose:** Retrieves wallets for a user with filtering.

**Input Parameters:**
- `userId` (UUID) - User identifier
- `status` (WalletStatus, optional) - Status filter

**Output:** `List<Wallet>` - Matching wallets

**Dependencies:**
- `WalletRepository`

**Domain Events:** None (read-only)

**Business Rules:**
- User ID must be provided
- Deleted wallets excluded by default
- Status filtering available

**Methods:**
- `listActiveWallets(UUID userId)` - Only ACTIVE wallets
- `listWallets(UUID userId)` - All non-deleted wallets
- `listWalletsByStatus(UUID userId, WalletStatus status)` - Filtered by status
- `listAllWallets(UUID userId)` - Including deleted (admin)

**Example Usage:**
```java
ListWalletsUseCase useCase = new ListWalletsUseCase(walletRepo);
List<Wallet> active = useCase.listActiveWallets(userId);
List<Wallet> recovering = useCase.listWalletsByStatus(userId, WalletStatus.RECOVERING);
```

---

#### 1.9 AddTokenToWalletUseCase

**Purpose:** Adds supported tokens to wallets for management.

**Input Parameters:**
- `walletId` (UUID) - Wallet identifier
- `tokenId` (UUID) - Token identifier
- `displayName` (String, nullable) - Custom display name
- `correlationId` (String) - Trace identifier

**Output:** `WalletToken` - Wallet-token relationship

**Dependencies:**
- `WalletRepository`
- `TokenRepository`
- `WalletTokenRepository`
- `DomainEventPublisher`

**Domain Events:**
- `TokenAddedToWalletEvent`

**Business Rules:**
- Wallet must exist and be active
- Token must exist
- Token cannot already be added
- Supports batch addition

**Example Usage:**
```java
AddTokenToWalletUseCase useCase = new AddTokenToWalletUseCase(walletRepo, tokenRepo, walletTokenRepo, eventPublisher);
WalletToken wt = useCase.addTokenToWallet(walletId, tokenId, "USDC Stablecoin", correlationId);

// Batch operation
UUID[] tokens = {token1Id, token2Id, token3Id};
BatchAddResult result = useCase.addMultipleTokens(walletId, tokens, correlationId);
```

**Batch Result:**
```java
public record BatchAddResult(
    int successCount,
    int failureCount,
    String[] errors
) {}
```

---

#### 1.10 RemoveTokenFromWalletUseCase

**Purpose:** Removes tokens from wallet management (soft delete).

**Input Parameters:**
- `walletId` (UUID) - Wallet identifier
- `tokenId` (UUID) - Token identifier
- `reason` (String) - Removal reason
- `correlationId` (String) - Trace identifier

**Output:** `void` (delete) or `WalletToken` (hide/show)

**Dependencies:**
- `WalletRepository`
- `WalletTokenRepository`
- `DomainEventPublisher`

**Domain Events:**
- `TokenRemovedFromWalletEvent`

**Business Rules:**
- Wallet must exist and be active
- Token must be currently added
- Reason required for audit
- Supports hide/show for UI control

**Methods:**
- `removeTokenFromWallet(...)` - Permanently removes token
- `hideTokenFromWallet(...)` - Hides from UI
- `showTokenInWallet(...)` - Shows in UI

**Example Usage:**
```java
RemoveTokenFromWalletUseCase useCase = new RemoveTokenFromWalletUseCase(walletRepo, walletTokenRepo, eventPublisher);
useCase.removeTokenFromWallet(walletId, tokenId, "User request", correlationId);

// Or just hide from display
WalletToken hidden = useCase.hideTokenFromWallet(walletId, tokenId, correlationId);
```

---

### 2. User Management (6 use cases)

#### 2.1 CreateUserUseCase

**Purpose:** Registers new users with secure password hashing.

**Input Parameters:**
- `name` (String) - Full name
- `email` (String) - Email address
- `password` (String) - Plain text password
- `correlationId` (String) - Trace identifier

**Output:** `User` - Created user instance

**Dependencies:**
- `UserRepository`
- `DomainEventPublisher`

**Domain Events:**
- `UserCreatedEvent`

**Business Rules:**
- Email must be unique
- Password minimum 8 characters
- Password must contain: uppercase, lowercase, digit, special character
- Email format validation
- User starts in PENDING_VERIFICATION status
- Email verification token generated

**Security:**
- SHA-256 password hashing with 16-byte salt
- Salt stored with hash (Base64 encoded)
- Verification tokens: 32-byte secure random

**Example Usage:**
```java
CreateUserUseCase useCase = new CreateUserUseCase(userRepo, eventPublisher);
User user = useCase.createUser("John Doe", "john@example.com", "SecureP@ss123", correlationId);

// Email verification
User verified = useCase.verifyEmail(verificationToken, correlationId);
```

**Error Handling:**
- `IllegalArgumentException` - Invalid email format or weak password
- `IllegalStateException` - Email already exists

**Password Requirements:**
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character (@$!%*?&)

---

#### 2.2 UpdateUserProfileUseCase

**Purpose:** Updates user profile information (name and email).

**Input Parameters:**
- `userId` (UUID) - User identifier
- `name` (String, nullable) - New name
- `email` (String, nullable) - New email
- `correlationId` (String) - Trace identifier

**Output:** `User` - Updated user

**Dependencies:**
- `UserRepository`
- `DomainEventPublisher`

**Domain Events:**
- `UserProfileUpdatedEvent`

**Business Rules:**
- User must exist and be active
- Email must be unique if changed
- Email format validation
- Null values preserve current values
- Email change requires re-verification

**Methods:**
- `updateProfile(...)` - Update both fields
- `updateName(...)` - Update name only
- `updateEmail(...)` - Update email only

**Example Usage:**
```java
UpdateUserProfileUseCase useCase = new UpdateUserProfileUseCase(userRepo, eventPublisher);
User updated = useCase.updateProfile(userId, "Jane Doe", null, correlationId);
```

---

#### 2.3 AuthenticateUserUseCase

**Purpose:** Handles user authentication and session management.

**Input Parameters:**
- `email` (String) - User email
- `password` (String) - Plain text password
- `ipAddress` (String) - Client IP
- `userAgent` (String) - Client user agent
- `correlationId` (String) - Trace identifier

**Output:** `AuthenticationResult` - Session information

**Dependencies:**
- `UserRepository`
- `UserSessionRepository`
- `DomainEventPublisher`

**Domain Events:**
- `UserAuthenticatedEvent`

**Business Rules:**
- User must exist and be active
- Account lockout after failed attempts
- Session expiration: 24 hours
- Failed login tracking
- Password verification with salt

**Methods:**
- `authenticate(...)` - Login and create session
- `validateSession(String sessionToken)` - Validate existing session
- `logout(String sessionToken)` - Invalidate session
- `logoutAllSessions(UUID userId)` - Invalidate all sessions

**Result Types:**
```java
public record AuthenticationResult(
    UUID userId,
    String name,
    String email,
    String sessionToken,
    Instant expiresAt,
    boolean twoFactorEnabled
) {}

public record SessionValidationResult(
    UUID userId,
    String name,
    String email,
    UUID sessionId,
    Instant expiresAt
) {}
```

**Example Usage:**
```java
AuthenticateUserUseCase useCase = new AuthenticateUserUseCase(userRepo, sessionRepo, eventPublisher);
AuthenticationResult auth = useCase.authenticate(email, password, ipAddress, userAgent, correlationId);

// Later: validate session
SessionValidationResult session = useCase.validateSession(auth.sessionToken());

// Logout
useCase.logout(auth.sessionToken(), correlationId);
```

**Security Features:**
- Account lockout on failed attempts
- Session token: 32-byte secure random (Base64 URL-safe)
- Password hashing verification with timing attack protection
- IP address and user agent tracking

---

#### 2.4 ChangePasswordUseCase

**Purpose:** Updates user passwords with security validation.

**Input Parameters:**
- `userId` (UUID) - User identifier
- `currentPassword` (String) - Current password for verification
- `newPassword` (String) - New password
- `correlationId` (String) - Trace identifier

**Output:** `void`

**Dependencies:**
- `UserRepository`
- `UserSessionRepository`
- `DomainEventPublisher`

**Domain Events:** None (security sensitive)

**Business Rules:**
- User must exist and be active
- Current password must be verified
- New password must meet requirements
- New password must differ from current
- All sessions invalidated after change

**Methods:**
- `changePassword(...)` - User-initiated change
- `resetPassword(...)` - Admin-initiated reset

**Example Usage:**
```java
ChangePasswordUseCase useCase = new ChangePasswordUseCase(userRepo, sessionRepo, eventPublisher);
useCase.changePassword(userId, "OldP@ss123", "NewSecureP@ss456", correlationId);

// All sessions are now invalid
```

**Security:**
- Password verification before change
- Session invalidation prevents hijacking
- Same password strength requirements
- Admin reset bypasses current password check

---

#### 2.5 DeactivateUserUseCase

**Purpose:** Manages user account status (deactivate, activate, suspend, unlock).

**Input Parameters:**
- `userId` (UUID) - User identifier
- `reason` (String, for deactivation/suspension) - Audit reason
- `correlationId` (String) - Trace identifier

**Output:** `void` or `User` (depending on method)

**Dependencies:**
- `UserRepository`
- `UserSessionRepository`
- `DomainEventPublisher`

**Domain Events:**
- `UserStatusChangedEvent`

**Business Rules:**
- User must exist
- Deactivation requires reason
- All sessions invalidated on deactivation/suspension
- Locked accounts can be unlocked

**Methods:**
- `deactivateUser(...)` - Permanent deactivation
- `activateUser(...)` - Reactivate account
- `suspendUser(...)` - Temporary suspension
- `unlockUser(...)` - Unlock after failed logins

**Example Usage:**
```java
DeactivateUserUseCase useCase = new DeactivateUserUseCase(userRepo, sessionRepo, eventPublisher);
useCase.deactivateUser(userId, "User request", correlationId);

// Later: reactivate
User active = useCase.activateUser(userId, correlationId);

// Suspend for investigation
useCase.suspendUser(userId, "Suspicious activity", correlationId);

// Unlock after failed logins
User unlocked = useCase.unlockUser(userId, correlationId);
```

---

### 3. Transaction Management (7 use cases)

#### 3.1 AddFundsUseCase

**Purpose:** Adds funds to wallet balance.

**Input Parameters:**
- `walletId` (UUID) - Wallet identifier
- `amount` (BigDecimal) - Amount to add
- `correlationId` (String) - Trace identifier

**Output:** `void`

**Dependencies:**
- `WalletRepository`
- `TransactionRepository`
- `DomainEventPublisher`

**Domain Events:**
- `FundsAddedEvent`

**Business Rules:**
- Wallet must exist
- Amount must be greater than zero
- Balance updated atomically

**Example Usage:**
```java
AddFundsUseCase useCase = new AddFundsUseCase(walletRepo, txRepo, eventPublisher);
useCase.addFunds(walletId, new BigDecimal("100.50"), correlationId);
```

---

#### 3.2 WithdrawFundsUseCase

**Purpose:** Withdraws funds from wallet balance.

**Input Parameters:**
- `walletId` (UUID) - Wallet identifier
- `amount` (BigDecimal) - Amount to withdraw
- `correlationId` (String) - Trace identifier

**Output:** `void`

**Dependencies:**
- `WalletRepository`
- `TransactionRepository`
- `DomainEventPublisher`

**Domain Events:**
- `FundsWithdrawnEvent`

**Business Rules:**
- Wallet must exist
- Amount must be greater than zero
- Sufficient balance required
- Balance updated atomically

**Example Usage:**
```java
WithdrawFundsUseCase useCase = new WithdrawFundsUseCase(walletRepo, txRepo, eventPublisher);
useCase.withdrawFunds(walletId, new BigDecimal("50.25"), correlationId);
```

**Error Handling:**
- `IllegalArgumentException` - Wallet not found or invalid amount
- `IllegalStateException` - Insufficient balance

---

#### 3.3 TransferFundsUseCase

**Purpose:** Transfers funds between two wallets.

**Input Parameters:**
- `fromWalletId` (UUID) - Source wallet
- `toWalletId` (UUID) - Destination wallet
- `amount` (BigDecimal) - Amount to transfer
- `correlationId` (String) - Trace identifier

**Output:** `void`

**Dependencies:**
- `WalletRepository`
- `TransactionRepository`
- `DomainEventPublisher`

**Domain Events:**
- `FundsTransferredEvent`

**Business Rules:**
- Both wallets must exist
- Amount must be greater than zero
- Source wallet must have sufficient balance
- Atomic transfer (both wallets updated)

**Example Usage:**
```java
TransferFundsUseCase useCase = new TransferFundsUseCase(walletRepo, txRepo, eventPublisher);
useCase.transferFunds(fromWalletId, toWalletId, new BigDecimal("75.00"), correlationId);
```

**Transaction Integrity:**
- Updates are atomic
- Both withdrawals and deposits must succeed
- On failure, no changes persisted

---

#### 3.4 CheckBalanceUseCase

**Purpose:** Retrieves current wallet balance.

**Input Parameters:**
- `walletId` (UUID) - Wallet identifier

**Output:** `BigDecimal` - Current balance

**Dependencies:**
- `WalletRepository`

**Domain Events:** None (read-only)

**Business Rules:**
- Wallet must exist
- Returns current balance snapshot

**Example Usage:**
```java
CheckBalanceUseCase useCase = new CheckBalanceUseCase(walletRepo);
BigDecimal balance = useCase.checkBalance(walletId);
```

---

#### 3.5 CreateTransactionUseCase

**Purpose:** Creates blockchain transaction records.

**Input Parameters:**
- `networkId` (UUID) - Network identifier
- `hash` (String) - Transaction hash
- `fromAddress` (String) - Sender address
- `toAddress` (String) - Recipient address
- `value` (BigDecimal) - Transfer value
- `data` (String, nullable) - Transaction data
- `correlationId` (String) - Trace identifier

**Output:** `Transaction` - Created transaction

**Dependencies:**
- `TransactionRepository`
- `DomainEventPublisher`

**Domain Events:**
- `TransactionCreatedEvent`

**Business Rules:**
- Transaction ID auto-generated
- Initial status: PENDING
- Hash must be unique per network

**Example Usage:**
```java
CreateTransactionUseCase useCase = new CreateTransactionUseCase(txRepo, eventPublisher);
Transaction tx = useCase.createTransaction(
    networkId,
    "0x123abc...",
    "0xfrom...",
    "0xto...",
    new BigDecimal("1.5"),
    null,
    correlationId
);
```

---

#### 3.6 ConfirmTransactionUseCase

**Purpose:** Confirms pending transactions with block information.

**Input Parameters:**
- `transactionId` (UUID) - Transaction identifier
- `blockNumber` (long) - Block number
- `blockHash` (String) - Block hash
- `gasUsed` (BigDecimal) - Gas consumed
- `correlationId` (String) - Trace identifier

**Output:** `void`

**Dependencies:**
- `TransactionRepository`
- `DomainEventPublisher`

**Domain Events:**
- `TransactionConfirmedEvent`

**Business Rules:**
- Transaction must exist
- Status changes to CONFIRMED
- Block information recorded

**Example Usage:**
```java
ConfirmTransactionUseCase useCase = new ConfirmTransactionUseCase(txRepo, eventPublisher);
useCase.confirm(txId, 12345678L, "0xblock...", new BigDecimal("21000"), correlationId);
```

---

#### 3.7 FailTransactionUseCase

**Purpose:** Marks transactions as failed with reason.

**Input Parameters:**
- `transactionId` (UUID) - Transaction identifier
- `reason` (String) - Failure reason
- `correlationId` (String) - Trace identifier

**Output:** `void`

**Dependencies:**
- `TransactionRepository`
- `DomainEventPublisher`

**Domain Events:**
- `TransactionStatusChangedEvent`

**Business Rules:**
- Transaction must exist
- Status changes to FAILED
- Reason recorded for audit

**Example Usage:**
```java
FailTransactionUseCase useCase = new FailTransactionUseCase(txRepo, eventPublisher);
useCase.fail(txId, "Insufficient gas", correlationId);
```

---

#### 3.8 EstimateTransactionFeeUseCase

**Purpose:** Calculates transaction fees for different speed levels.

**Input Parameters:**
- `networkId` (UUID) - Network identifier
- `gasLimit` (BigDecimal) - Estimated gas limit
- `transactionType` (BlockchainTransactionType, optional) - For gas estimation
- `correlationId` (String) - Trace identifier

**Output:** `FeeEstimateResult` - Fee estimates for all levels

**Dependencies:**
- `NetworkRepository`
- `TransactionFeeRepository`

**Domain Events:** None (read-only)

**Business Rules:**
- Network must be active
- Provides SLOW, STANDARD, FAST, URGENT estimates
- Cached fee data for performance
- Default fees if no recent data

**Fee Levels:**
- SLOW: 5-10 minutes (20 Gwei default)
- STANDARD: 2-5 minutes (25 Gwei default)
- FAST: 1-2 minutes (35 Gwei default)
- URGENT: <1 minute (50 Gwei default)

**Gas Limit Estimates:**
- SIMPLE_TRANSFER: 21,000
- ERC20_TRANSFER: 65,000
- ERC721_TRANSFER: 85,000
- CONTRACT_INTERACTION: 150,000
- CONTRACT_DEPLOYMENT: 500,000
- COMPLEX_DEFI: 300,000

**Methods:**
- `estimateTransactionFee(...)` - Get all fee levels
- `estimateGasLimit(...)` - Get gas estimate by type
- `getLatestFee(...)` - Get specific fee level

**Result Types:**
```java
public record FeeEstimateResult(
    UUID networkId,
    String networkName,
    BigDecimal gasLimit,
    List<FeeEstimate> estimates
) {}

public record FeeEstimate(
    FeeLevel level,
    BigDecimal gasPrice,
    BigDecimal baseFee,
    BigDecimal priorityFee,
    BigDecimal totalCost,
    String estimatedTime,
    String description
) {}
```

**Example Usage:**
```java
EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepo, feeRepo);

// Estimate gas limit
BigDecimal gasLimit = useCase.estimateGasLimit(networkId, BlockchainTransactionType.ERC20_TRANSFER, correlationId);

// Get fee estimates
FeeEstimateResult fees = useCase.estimateTransactionFee(networkId, gasLimit, correlationId);
```

---

### 4. Address Management (5 use cases)

#### 4.1 CreateAddressUseCase

**Purpose:** Generates new blockchain addresses for wallets.

**Input Parameters:**
- `walletId` (UUID) - Wallet identifier
- `networkId` (UUID) - Network identifier
- `publicKeyValue` (String) - Public key
- `accountAddressValue` (String) - Address value
- `addressType` (AddressType) - EXTERNAL, INTERNAL, or CONTRACT
- `derivationPath` (String) - BIP44 derivation path
- `correlationId` (String) - Trace identifier

**Output:** `Address` - Created address

**Dependencies:**
- `AddressRepository`
- `WalletRepository`
- `NetworkRepository`
- `DomainEventPublisher`

**Domain Events:**
- `AddressCreatedEvent`

**Business Rules:**
- Wallet must exist and be active
- Network must exist and be active
- Address must be unique per network
- Public key required and valid

**Example Usage:**
```java
CreateAddressUseCase useCase = new CreateAddressUseCase(addressRepo, walletRepo, networkRepo, eventPublisher);
Address address = useCase.createAddress(
    walletId,
    networkId,
    "0x04abc...",
    "0x123...",
    AddressType.EXTERNAL,
    "m/44'/60'/0'/0/0",
    correlationId
);
```

---

#### 4.2 ImportAddressUseCase

**Purpose:** Imports existing addresses into wallets (watch-only support).

**Input Parameters:**
- `walletId` (UUID) - Wallet identifier
- `networkId` (UUID) - Network identifier
- `accountAddressValue` (String) - Address value
- `publicKeyValue` (String, nullable) - Public key (optional for watch-only)
- `label` (String, nullable) - User-friendly label
- `isWatchOnly` (boolean) - Watch-only flag
- `correlationId` (String) - Trace identifier

**Output:** `Address` - Imported address

**Dependencies:**
- `AddressRepository`
- `WalletRepository`
- `NetworkRepository`
- `DomainEventPublisher`
- `ValidateAddressUseCase`

**Domain Events:**
- `AddressCreatedEvent`

**Business Rules:**
- Wallet must exist and be active
- Network must exist and be active
- Address must not already exist
- Address format validated against network
- Public key optional for watch-only
- Supports batch import

**Methods:**
- `importAddress(...)` - Single address import
- `importAddresses(...)` - Batch import

**Result Types:**
```java
public record AddressImport(
    String accountAddress,
    String publicKey,
    String label,
    boolean isWatchOnly
) {}

public record BatchImportResult(
    int successCount,
    int failureCount,
    String[] errors
) {}
```

**Example Usage:**
```java
ImportAddressUseCase useCase = new ImportAddressUseCase(addressRepo, walletRepo, networkRepo, eventPublisher, validateUseCase);

// Import watch-only address
Address watchOnly = useCase.importAddress(
    walletId,
    networkId,
    "0xabc...",
    null,
    "Exchange Wallet",
    true,
    correlationId
);

// Batch import
AddressImport[] imports = {
    new AddressImport("0x123...", null, "Addr1", true),
    new AddressImport("0x456...", null, "Addr2", true)
};
BatchImportResult result = useCase.importAddresses(walletId, networkId, imports, correlationId);
```

---

#### 4.3 UpdateAddressStatusUseCase

**Purpose:** Manages address lifecycle status changes.

**Input Parameters:**
- `addressId` (UUID) - Address identifier
- `newStatus` (AddressStatus, optional) - Target status
- `correlationId` (String) - Trace identifier

**Output:** `Address` - Updated address

**Dependencies:**
- `AddressRepository`
- `DomainEventPublisher`

**Domain Events:**
- `AddressStatusChangedEvent`

**Business Rules:**
- Address must exist
- Valid status transitions
- Supports batch updates

**Methods:**
- `activateAddress(...)` - Make active
- `archiveAddress(...)` - Archive (keep for history)
- `updateStatus(...)` - Generic status update
- `batchUpdateStatus(...)` - Batch operation

**Example Usage:**
```java
UpdateAddressStatusUseCase useCase = new UpdateAddressStatusUseCase(addressRepo, eventPublisher);

// Activate address
Address active = useCase.activateAddress(addressId, correlationId);

// Archive address
Address archived = useCase.archiveAddress(addressId, correlationId);

// Batch archive
UUID[] addressIds = {id1, id2, id3};
int updated = useCase.batchUpdateStatus(addressIds, AddressStatus.ARCHIVED, correlationId);
```

---

#### 4.4 ValidateAddressUseCase

**Purpose:** Validates address formats and network compatibility.

**Input Parameters:**
- `addressValue` (String) - Address to validate
- `networkId` (UUID, nullable) - Network for compatibility check
- `correlationId` (String) - Trace identifier

**Output:** `AddressValidationResult` - Validation details

**Dependencies:**
- `NetworkRepository`

**Domain Events:** None (read-only)

**Business Rules:**
- Address format validation
- Network compatibility checking
- Supports multiple blockchain formats
- Batch validation available

**Supported Formats:**
- Ethereum: `0x` + 40 hex chars
- Bitcoin Legacy: Starts with 1 or 3
- Bitcoin Bech32: Starts with bc1
- Hexadecimal: Generic hex format

**Methods:**
- `validateAddress(...)` - Single validation
- `validateAddresses(...)` - Batch validation

**Result Type:**
```java
public class AddressValidationResult {
    boolean valid;
    String address;
    String format;
    String network;
    boolean networkCompatible;
    String error;
}
```

**Example Usage:**
```java
ValidateAddressUseCase useCase = new ValidateAddressUseCase(networkRepo);

// Validate format only
AddressValidationResult result = useCase.validateAddress("0x123abc...", null, correlationId);

// Validate with network compatibility
AddressValidationResult ethResult = useCase.validateAddress("0x123abc...", ethereumNetworkId, correlationId);

// Batch validation
String[] addresses = {"0x123...", "bc1q..."};
AddressValidationResult[] results = useCase.validateAddresses(addresses, networkId, correlationId);
```

---

#### 4.5 ListAddressesByWalletUseCase

**Purpose:** Retrieves addresses associated with wallets.

**Input Parameters:**
- `walletId` (UUID) - Wallet identifier
- `status` (AddressStatus, optional) - Status filter

**Output:** `List<Address>` or `AddressCountSummary`

**Dependencies:**
- `AddressRepository`
- `WalletRepository`

**Domain Events:** None (read-only)

**Business Rules:**
- Wallet must exist
- Status filtering available
- Provides count summaries

**Methods:**
- `listAddresses(...)` - All addresses
- `listActiveAddresses(...)` - Active only
- `listAddressesByStatus(...)` - Filtered by status
- `getAddressCountSummary(...)` - Statistics

**Result Type:**
```java
public record AddressCountSummary(
    UUID walletId,
    String walletName,
    int totalAddresses,
    long activeAddresses,
    long archivedAddresses
) {}
```

**Example Usage:**
```java
ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepo, walletRepo);

// Get all addresses
List<Address> all = useCase.listAddresses(walletId);

// Get active only
List<Address> active = useCase.listActiveAddresses(walletId);

// Get summary
AddressCountSummary summary = useCase.getAddressCountSummary(walletId);
```

---

### 5. Token & Network Management (5 use cases)

#### 5.1 AddNetworkUseCase

**Purpose:** Adds new blockchain networks to the system.

**Input Parameters:**
- `name` (String) - Display name
- `chainId` (String) - Unique chain identifier
- `rpcUrl` (String) - RPC endpoint URL
- `explorerUrl` (String) - Block explorer URL
- `correlationId` (String) - Trace identifier

**Output:** `Network` - Created network

**Dependencies:**
- `NetworkRepository`
- `DomainEventPublisher`

**Domain Events:**
- `NetworkCreatedEvent`

**Business Rules:**
- Network name must be unique
- Chain ID must be unique
- RPC URL must be valid HTTP/HTTPS
- Explorer URL must be valid HTTP/HTTPS

**Example Usage:**
```java
AddNetworkUseCase useCase = new AddNetworkUseCase(networkRepo, eventPublisher);
Network network = useCase.addNetwork(
    "Ethereum Mainnet",
    "1",
    "https://mainnet.infura.io/v3/...",
    "https://etherscan.io",
    correlationId
);
```

**Error Handling:**
- `IllegalArgumentException` - Invalid inputs or chain ID in use

---

#### 5.2 ListNetworksUseCase

**Purpose:** Retrieves blockchain networks with filtering.

**Input Parameters:**
- `namePattern` (String, optional) - Search pattern
- `correlationId` (String) - Trace identifier

**Output:** `List<Network>` or `NetworkHealthInfo`

**Dependencies:**
- `NetworkRepository`

**Domain Events:** None (read-only)

**Business Rules:**
- Active networks returned by default
- Health status monitoring
- Name-based search

**Methods:**
- `listActiveNetworks(...)` - Active only
- `listAllNetworks(...)` - All networks
- `getNetworkDetails(...)` - Specific network
- `getNetworkHealthStatus(...)` - Health monitoring
- `searchNetworksByName(...)` - Search

**Result Type:**
```java
public record NetworkHealthInfo(
    UUID networkId,
    String name,
    String chainId,
    NetworkStatus status,
    boolean isHealthy,
    String healthStatus,
    String rpcUrl
) {}
```

**Example Usage:**
```java
ListNetworksUseCase useCase = new ListNetworksUseCase(networkRepo);

// Get active networks
List<Network> active = useCase.listActiveNetworks(correlationId);

// Get specific network
Network ethereum = useCase.getNetworkDetails(networkId, correlationId);

// Health check
List<NetworkHealthInfo> health = useCase.getNetworkHealthStatus(correlationId);

// Search
List<Network> matches = useCase.searchNetworksByName("Ethereum", correlationId);
```

---

#### 5.3 ListSupportedTokensUseCase

**Purpose:** Retrieves available tokens with comprehensive filtering.

**Input Parameters:**
- `networkId` (UUID, nullable) - Network filter
- `tokenType` (TokenType, nullable) - Type filter
- `symbol` (String, nullable) - Symbol search
- `correlationId` (String, optional) - Trace identifier

**Output:** `List<Token>` or `TokenListingResult`

**Dependencies:**
- `TokenRepository`
- `NetworkRepository`

**Domain Events:** None (read-only)

**Business Rules:**
- Multiple filter combinations
- Type-specific queries
- NFT grouping

**Token Types:**
- NATIVE - Native blockchain tokens (ETH, BTC)
- ERC20 - Fungible tokens
- ERC721 - NFTs (unique)
- ERC1155 - Multi-token standard
- CUSTOM - Custom token standards

**Methods:**
- `listAllTokens()` - All tokens
- `listTokensByNetwork(...)` - By network
- `listTokensByType(...)` - By type
- `listNativeTokens()` - Native only
- `listERC20Tokens()` - ERC20 only
- `listNFTTokens()` - ERC721 + ERC1155
- `searchTokensBySymbol(...)` - Symbol search
- `listTokensOnActiveNetworks(...)` - Active networks only
- `getTokenListing(...)` - Comprehensive listing

**Result Type:**
```java
public record TokenListingResult(
    List<Token> tokens,
    int totalCount,
    long nativeTokens,
    long erc20Tokens,
    long nftTokens,
    long customTokens
) {}
```

**Example Usage:**
```java
ListSupportedTokensUseCase useCase = new ListSupportedTokensUseCase(tokenRepo, networkRepo);

// All tokens
List<Token> all = useCase.listAllTokens();

// Ethereum tokens
List<Token> ethTokens = useCase.listTokensByNetwork(ethereumNetworkId, correlationId);

// ERC20 only
List<Token> erc20 = useCase.listERC20Tokens();

// Search by symbol
List<Token> usdc = useCase.searchTokensBySymbol("USDC");

// Comprehensive listing with filters
TokenListingResult listing = useCase.getTokenListing(ethereumNetworkId, TokenType.ERC20, correlationId);
```

---

### 6. Portfolio & Balance (3 use cases)

#### 6.1 GetAddressBalanceUseCase

**Purpose:** Retrieves token balances for addresses.

**Input Parameters:**
- `addressId` (UUID) - Address identifier
- `tokenId` (UUID, optional) - Specific token

**Output:** `AddressBalanceResult` or `BigDecimal`

**Dependencies:**
- `AddressRepository`
- `TokenBalanceRepository`

**Domain Events:** None (read-only)

**Business Rules:**
- Address must exist
- Returns all token balances
- Zero balances included
- Supports multi-address queries

**Methods:**
- `getAddressBalance(...)` - All balances
- `getTokenBalance(...)` - Specific token
- `getMultipleAddressBalances(...)` - Batch query

**Result Type:**
```java
public class AddressBalanceResult {
    UUID addressId;
    String address;
    UUID walletId;
    UUID networkId;
    BigDecimal totalValue;
    Map<UUID, BigDecimal> tokenBalances;
    int balanceCount;
}
```

**Example Usage:**
```java
GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepo, balanceRepo);

// Get all balances
AddressBalanceResult balances = useCase.getAddressBalance(addressId);

// Get specific token balance
BigDecimal usdcBalance = useCase.getTokenBalance(addressId, usdcTokenId);

// Batch query
List<UUID> addressIds = Arrays.asList(id1, id2, id3);
Map<UUID, AddressBalanceResult> results = useCase.getMultipleAddressBalances(addressIds);
```

---

#### 6.2 GetTokenBalanceUseCase

**Purpose:** Retrieves token balances aggregated by wallet.

**Input Parameters:**
- `walletId` (UUID) - Wallet identifier
- `tokenId` (UUID) - Token identifier

**Output:** `BigDecimal` or `TokenBalanceDetails`

**Dependencies:**
- `WalletRepository`
- `TokenRepository`
- `TokenBalanceRepository`

**Domain Events:** None (read-only)

**Business Rules:**
- Wallet must exist
- Token must exist
- Aggregates across all addresses
- Returns zero if no balance

**Methods:**
- `getWalletTokenBalance(...)` - Total balance
- `getTokenBalanceDetails(...)` - Detailed information

**Result Type:**
```java
public record TokenBalanceDetails(
    UUID walletId,
    UUID tokenId,
    String symbol,
    String name,
    BigDecimal totalBalance,
    String formattedBalance,
    int decimals,
    int addressCount,
    int totalAddresses
) {}
```

**Example Usage:**
```java
GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepo, tokenRepo, balanceRepo);

// Get total balance
BigDecimal balance = useCase.getWalletTokenBalance(walletId, tokenId);

// Get detailed info
TokenBalanceDetails details = useCase.getTokenBalanceDetails(walletId, tokenId);
```

---

#### 6.3 GetPortfolioSummaryUseCase

**Purpose:** Provides comprehensive portfolio analytics and asset allocation.

**Input Parameters:**
- `walletId` (UUID) - Wallet identifier

**Output:** `PortfolioSummary`, `PortfolioOverview`, or `List<AssetAllocation>`

**Dependencies:**
- `WalletRepository`
- `AddressRepository`
- `TokenBalanceRepository`
- `TokenRepository`

**Domain Events:** None (read-only)

**Business Rules:**
- Wallet must exist and be accessible
- Aggregates across all addresses
- Zero balances excluded
- Value calculations require price data
- Asset allocation percentages calculated

**Methods:**
- `getPortfolioSummary(...)` - Complete analytics
- `getPortfolioOverview(...)` - Basic metrics
- `getAssetAllocation(...)` - Allocation breakdown

**Result Types:**
```java
public record PortfolioSummary(
    UUID walletId,
    String walletName,
    int totalTokens,
    int totalAddresses,
    BigDecimal totalValue,
    List<TokenHolding> holdings,
    List<AssetAllocation> assetAllocation,
    Instant lastUpdated
) {}

public record PortfolioOverview(
    UUID walletId,
    String walletName,
    int totalTokens,
    int totalAddresses,
    BigDecimal totalValue,
    Instant lastUpdated
) {}

public record TokenHolding(
    UUID tokenId,
    String name,
    String symbol,
    BigDecimal balance,
    String formattedBalance,
    int decimals,
    BigDecimal estimatedValue,
    TokenType type
) {}

public record AssetAllocation(
    UUID tokenId,
    String symbol,
    BigDecimal estimatedValue,
    BigDecimal percentage
) {}
```

**Example Usage:**
```java
GetPortfolioSummaryUseCase useCase = new GetPortfolioSummaryUseCase(walletRepo, addressRepo, balanceRepo, tokenRepo);

// Get complete portfolio
PortfolioSummary portfolio = useCase.getPortfolioSummary(walletId);

// Get overview only
PortfolioOverview overview = useCase.getPortfolioOverview(walletId);

// Get allocation
List<AssetAllocation> allocation = useCase.getAssetAllocation(walletId);
```

**Value Calculation:**
Currently uses mock prices for demonstration:
- ETH: $2,000
- BTC: $45,000
- USDC/USDT: $1
- Others: $0

In production, this would integrate with price oracles or market data APIs.

---

## Use Case Orchestration Patterns

### 1. Command-Query Separation

**Commands** (Write Operations):
- Modify state
- Publish domain events
- Return created/updated entities
- Examples: Create, Update, Activate, Delete

**Queries** (Read Operations):
- Read-only operations
- No domain events
- Return DTOs or domain models
- Examples: List, Get, Check

### 2. Event Publishing Pattern

```java
// Standard event publishing pattern
public Wallet updateWallet(...) {
    // 1. Load aggregate
    Wallet wallet = repository.findById(id).orElseThrow(...);

    // 2. Execute business logic (domain events created internally)
    wallet.updateInfo(name, description);

    // 3. Persist changes
    repository.update(wallet);

    // 4. Publish events
    wallet.getDomainEvents().forEach(eventPublisher::publish);
    wallet.clearEvents();

    // 5. Return result
    return wallet;
}
```

### 3. Transaction Boundaries

Each use case method defines a transaction boundary:

```java
@Transactional // Applied at infrastructure layer
public void transferFunds(UUID from, UUID to, BigDecimal amount, String correlationId) {
    // Entire operation is atomic
    Wallet fromWallet = repository.findById(from).orElseThrow(...);
    Wallet toWallet = repository.findById(to).orElseThrow(...);

    fromWallet.withdrawFunds(amount);
    toWallet.addFunds(amount);

    repository.update(fromWallet);
    repository.update(toWallet);

    eventPublisher.publish(new FundsTransferredEvent(...));
}
```

### 4. Validation Strategy

**Three-tier validation:**

1. **Input Validation** (Use Case Layer)
```java
if (walletId == null) {
    throw new IllegalArgumentException("Wallet ID must be provided");
}
```

2. **Business Rule Validation** (Domain Layer)
```java
wallet.validateOperationAllowed(); // Throws if not ACTIVE
```

3. **Constraint Validation** (Infrastructure Layer)
```java
@NotNull
@Column(unique = true)
private String email;
```

### 5. Correlation ID Pattern

All use cases accept `correlationId` for distributed tracing:

```java
public Wallet createWallet(UUID userId, String correlationId) {
    Wallet wallet = Wallet.create(...);
    wallet.setCorrelationId(UUID.fromString(correlationId));

    // Events include correlation ID
    WalletCreatedEvent event = new WalletCreatedEvent(
        wallet.getId(),
        UUID.fromString(correlationId)
    );

    eventPublisher.publish(event);
    return wallet;
}
```

---

## Error Handling Approach

### Exception Hierarchy

```
RuntimeException
├── IllegalArgumentException
│   ├── Validation failures
│   ├── Entity not found
│   └── Invalid input formats
│
└── IllegalStateException
    ├── Business rule violations
    ├── Invalid state transitions
    └── Constraint violations
```

### Common Error Scenarios

| Error Type | Exception | Example |
|------------|-----------|---------|
| Entity not found | `IllegalArgumentException` | "Wallet not found with id: ..." |
| Invalid input | `IllegalArgumentException` | "Amount must be greater than zero" |
| Business rule | `IllegalStateException` | "Wallet must be ACTIVE to perform operations" |
| Constraint | `IllegalStateException` | "Email already exists: ..." |
| State transition | `IllegalStateException` | "Deleted wallets cannot be activated" |

### Error Messages

Use case exceptions include:
- Clear, user-friendly messages
- Contextual information (IDs, values)
- No sensitive data exposure
- Formatted templates for consistency

```java
private static final String ERROR_WALLET_NOT_FOUND_TEMPLATE = "Wallet not found with id: %s";

throw new IllegalArgumentException(ERROR_WALLET_NOT_FOUND_TEMPLATE.formatted(walletId));
```

---

## Testing Strategies for Use Cases

### 1. Unit Testing Pattern

```java
@ExtendWith(MockitoExtension.class)
class CreateWalletUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private CreateWalletUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateWalletUseCase(walletRepository, eventPublisher);
    }

    @Test
    void shouldCreateWalletSuccessfully() {
        // Given
        UUID userId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();

        // When
        Wallet wallet = useCase.createWallet(userId, correlationId);

        // Then
        assertNotNull(wallet);
        verify(walletRepository).save(any(Wallet.class));
        verify(eventPublisher).publish(any(WalletCreatedEvent.class));
    }
}
```

### 2. Integration Testing

```java
@DataJpaTest
@Import({CreateWalletUseCase.class, KafkaEventProducer.class})
class CreateWalletUseCaseIntegrationTest {

    @Autowired
    private CreateWalletUseCase useCase;

    @Autowired
    private WalletRepository repository;

    @Test
    void shouldPersistWalletToDatabase() {
        // Test with real database
        Wallet wallet = useCase.createWallet(userId, correlationId);

        Optional<Wallet> found = repository.findById(wallet.getId());
        assertTrue(found.isPresent());
        assertEquals("Default Wallet", found.get().getName());
    }
}
```

### 3. Test Coverage Goals

- **Unit Tests:** 100% use case logic
- **Integration Tests:** Happy path + critical error paths
- **Contract Tests:** Gateway interfaces
- **E2E Tests:** Complete user workflows

### 4. Testing Checklist

For each use case:

- [ ] Happy path test
- [ ] Validation error tests
- [ ] Business rule violation tests
- [ ] State transition tests
- [ ] Event publishing verification
- [ ] Repository interaction verification
- [ ] Null/edge case handling
- [ ] Concurrent access (where applicable)

---

## Performance Considerations

### 1. Repository Queries

Use cases should:
- Minimize database round trips
- Use batch operations where possible
- Leverage repository caching
- Avoid N+1 query problems

```java
// Bad: N+1 problem
for (UUID addressId : addressIds) {
    Address address = addressRepository.findById(addressId).orElse(null);
}

// Good: Batch query
List<Address> addresses = addressRepository.findByIds(addressIds);
```

### 2. Event Publishing

- Events published after persistence (eventual consistency)
- Async event processing recommended
- Outbox pattern for reliability

### 3. Read-Heavy Operations

Query use cases should:
- Use read-optimized repositories
- Leverage caching where appropriate
- Consider read replicas for scale
- Use projections for large datasets

### 4. Batch Operations

Several use cases provide batch methods:
- `AddTokenToWalletUseCase.addMultipleTokens(...)`
- `ImportAddressUseCase.importAddresses(...)`
- `UpdateAddressStatusUseCase.batchUpdateStatus(...)`
- `GetAddressBalanceUseCase.getMultipleAddressBalances(...)`

---

## Security Considerations

### 1. Password Security

**CreateUserUseCase** and **ChangePasswordUseCase**:
- SHA-256 hashing with salt
- 16-byte secure random salt
- Salt stored with hash
- No plaintext password storage
- Password strength validation

### 2. Session Management

**AuthenticateUserUseCase**:
- 32-byte secure session tokens
- 24-hour expiration
- IP address and user agent tracking
- Session invalidation on password change
- Account lockout on failed attempts

### 3. Input Validation

All use cases validate:
- Required parameters (non-null)
- Format constraints (email, URLs)
- Business constraints (positive amounts)
- State preconditions (wallet active)

### 4. Correlation ID Validation

Network-related use cases validate correlation IDs:
```java
private String normalizeCorrelationId(String correlationId) {
    if (!StringUtils.hasText(correlationId)) {
        throw new IllegalArgumentException("Correlation ID must be provided");
    }
    try {
        UUID parsed = UUID.fromString(correlationId.trim());
        return parsed.toString();
    } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException("Correlation ID must be a valid UUID", ex);
    }
}
```

---

## Integration with Infrastructure

### 1. Spring Configuration

Use cases are instantiated as beans:

```java
@Configuration
public class UseCaseConfiguration {

    @Bean
    public CreateWalletUseCase createWalletUseCase(
            WalletRepository walletRepository,
            DomainEventPublisher eventPublisher) {
        return new CreateWalletUseCase(walletRepository, eventPublisher);
    }

    // ... other use case beans
}
```

### 2. Repository Adapters

Repositories are implemented in infrastructure layer:

```java
// Domain gateway (port)
public interface WalletRepository {
    Optional<Wallet> findById(UUID id);
    Wallet save(Wallet wallet);
    void update(Wallet wallet);
}

// Infrastructure adapter
@Repository
public class JpaWalletRepository implements WalletRepository {
    // JPA implementation
}
```

### 3. Event Publishing

```java
// Domain gateway (port)
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}

// Infrastructure adapter
@Component
public class KafkaEventProducer implements DomainEventPublisher {
    @Override
    public void publish(DomainEvent event) {
        // Kafka publishing logic
    }
}
```

---

## Domain Events Reference

### Wallet Events

- `WalletCreatedEvent` - New wallet created
- `WalletUpdatedEvent` - Wallet info updated
- `WalletStatusChangedEvent` - Status transition
- `WalletDeletedEvent` - Wallet soft deleted
- `WalletRecoveryInitiatedEvent` - Recovery started
- `FundsAddedEvent` - Funds deposited
- `FundsWithdrawnEvent` - Funds withdrawn
- `FundsTransferredEvent` - Transfer between wallets
- `TokenAddedToWalletEvent` - Token management
- `TokenRemovedFromWalletEvent` - Token removal
- `AddressAddedToWalletEvent` - Address association
- `AddressRemovedFromWalletEvent` - Address removal

### User Events

- `UserCreatedEvent` - User registration
- `UserProfileUpdatedEvent` - Profile changes
- `UserStatusChangedEvent` - Status transitions
- `UserAuthenticatedEvent` - Login success

### Transaction Events

- `TransactionCreatedEvent` - Transaction recorded
- `TransactionConfirmedEvent` - On-chain confirmation
- `TransactionStatusChangedEvent` - Status update

### Address Events

- `AddressCreatedEvent` - New address
- `AddressStatusChangedEvent` - Status change

### Network/Token Events

- `NetworkCreatedEvent` - Network added
- `TokenCreatedEvent` - Token registered
- `TokenBalanceChangedEvent` - Balance update

---

## Best Practices

### 1. Use Case Design

- **Single Responsibility**: One business operation per use case
- **No Side Effects**: Read operations don't modify state
- **Event Consistency**: Always publish events after persistence
- **Correlation Tracking**: Always include correlation IDs

### 2. Error Handling

- **Fail Fast**: Validate early, fail with clear messages
- **Meaningful Exceptions**: Use appropriate exception types
- **Context Preservation**: Include entity IDs in error messages
- **No Swallowing**: Let exceptions propagate to infrastructure

### 3. Testing

- **Mock Gateways**: Test use case logic in isolation
- **Verify Events**: Ensure domain events are published
- **Test Boundaries**: Validate all error conditions
- **Integration Tests**: Test with real repositories

### 4. Performance

- **Batch Operations**: Provide batch methods for bulk operations
- **Lazy Loading**: Don't load unnecessary data
- **Query Optimization**: Use specific repository methods
- **Event Batching**: Consider batching events where appropriate

### 5. Security

- **Input Validation**: Never trust user input
- **Password Security**: Always hash, never store plaintext
- **Session Management**: Invalidate on critical changes
- **Audit Trails**: Log security-relevant operations

---

## Migration Guide

### From Procedural to Use Case Pattern

**Before:**
```java
@Service
public class WalletService {
    public Wallet createWallet(UUID userId) {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUserId(userId);
        walletRepository.save(wallet);
        kafkaTemplate.send("wallet-created", wallet);
        return wallet;
    }
}
```

**After:**
```java
public record CreateWalletUseCase(
    WalletRepository walletRepository,
    DomainEventPublisher eventPublisher) {

    public Wallet createWallet(UUID userId, String correlationId) {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "Default Wallet", "");
        walletRepository.save(wallet);
        WalletCreatedEvent event = new WalletCreatedEvent(
            wallet.getId(),
            UUID.fromString(correlationId)
        );
        eventPublisher.publish(event);
        return wallet;
    }
}
```

### Benefits

1. **Testability**: Easy to mock dependencies
2. **Reusability**: Use cases can be composed
3. **Clarity**: Business logic clearly separated
4. **Maintainability**: Single responsibility
5. **Framework Independence**: No Spring dependencies in logic

---

## Summary

The Use Case layer provides:

- **33 well-defined business operations** organized by domain
- **Consistent patterns** for command/query operations
- **Strong validation** with clear error messages
- **Event-driven architecture** for decoupling
- **Comprehensive test coverage** for reliability
- **Security best practices** for sensitive operations
- **Performance optimizations** through batch operations

This layer serves as the **application core**, orchestrating domain logic while remaining **independent of delivery mechanisms** (REST, GraphQL, events) and **infrastructure details** (databases, messaging).
