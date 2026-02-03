# Use Cases - Quick Reference Guide

## Overview
This guide provides quick reference information for all use cases in the Wallet Hub system. For detailed documentation, see [use_cases.md](use_cases.md).

## Use Case Matrix

### Wallet Management
| Use Case | Method Signature | Returns | Events | Dependencies |
|----------|-----------------|---------|--------|--------------|
| `CreateWalletUseCase` | `createWallet(UUID userId, String correlationId)` | `Wallet` | `WalletCreatedEvent` | `WalletRepository`, `DomainEventPublisher` |
| `UpdateWalletUseCase` | `updateWallet(UUID walletId, String name, String description, String correlationId)` | `Wallet` | `WalletUpdatedEvent` | `WalletRepository`, `DomainEventPublisher` |
| `ActivateWalletUseCase` | `activateWallet(UUID walletId, String correlationId)` | `void` | `WalletStatusChangedEvent` | `WalletRepository`, `DomainEventPublisher` |
| `DeactivateWalletUseCase` | `deactivateWallet(UUID walletId, String correlationId)` | `void` | `WalletStatusChangedEvent` | `WalletRepository`, `DomainEventPublisher` |
| `DeleteWalletUseCase` | `deleteWallet(UUID walletId, String correlationId)` | `void` | `WalletDeletedEvent` | `WalletRepository`, `DomainEventPublisher` |
| `ListWalletsUseCase` | `listWallets(String correlationId)` | `List<Wallet>` | None | `WalletRepository` |
| `GetWalletDetailsUseCase` | `getWalletDetails(UUID walletId, String correlationId)` | `WalletDetails` | None | `WalletRepository`, `AddressRepository` |
| `RecoverWalletUseCase` | `recoverWallet(UUID walletId, String correlationId)` | `void` | `WalletRecoveryInitiatedEvent` | `WalletRepository`, `DomainEventPublisher` |

### Address Management
| Use Case | Method Signature | Returns | Events | Dependencies |
|----------|-----------------|---------|--------|--------------|
| `CreateAddressUseCase` | `createAddress(UUID walletId, UUID networkId, String correlationId)` | `Address` | `AddressCreatedEvent` | `AddressRepository`, `WalletRepository`, `NetworkRepository`, `DomainEventPublisher` |
| **`ValidateAddressUseCase`** | **`validateAddress(String address, UUID networkId, String correlationId)`** | **`AddressValidationResult`** | **None** | **`NetworkRepository`** |
| | **`validateAddresses(String[] addresses, UUID networkId, String correlationId)`** | **`AddressValidationResult[]`** | **None** | **`NetworkRepository`** |
| `GetAddressBalanceUseCase` | `getAddressBalance(UUID addressId, String correlationId)` | `AddressBalance` | None | `AddressRepository`, `TokenBalanceRepository` |
| `ListAddressesByWalletUseCase` | `listAddressesByWallet(UUID walletId, String correlationId)` | `List<Address>` | None | `AddressRepository`, `WalletRepository` |
| `UpdateAddressStatusUseCase` | `updateAddressStatus(UUID addressId, AddressStatus status, String correlationId)` | `void` | `AddressStatusChangedEvent` | `AddressRepository`, `DomainEventPublisher` |
| `ImportAddressUseCase` | `importAddress(String addressValue, UUID walletId, UUID networkId, String correlationId)` | `Address` | `AddressAddedToWalletEvent` | `AddressRepository`, `WalletRepository`, `NetworkRepository`, `DomainEventPublisher`, `ValidateAddressUseCase` |

### Transaction Management
| Use Case | Method Signature | Returns | Events | Dependencies |
|----------|-----------------|---------|--------|--------------|
| `AddFundsUseCase` | `addFunds(UUID walletId, BigDecimal amount, String correlationId)` | `void` | `FundsAddedEvent` | `WalletRepository`, `TransactionRepository`, `DomainEventPublisher` |
| `WithdrawFundsUseCase` | `withdrawFunds(UUID walletId, BigDecimal amount, String correlationId)` | `void` | `FundsWithdrawnEvent` | `WalletRepository`, `TransactionRepository`, `DomainEventPublisher` |
| `TransferFundsUseCase` | `transferFunds(UUID fromWalletId, UUID toWalletId, BigDecimal amount, String correlationId)` | `void` | `FundsTransferredEvent` | `WalletRepository`, `TransactionRepository`, `DomainEventPublisher` |
| `CreateTransactionUseCase` | `createTransaction(UUID walletId, TransactionType type, BigDecimal amount, String correlationId)` | `Transaction` | `TransactionCreatedEvent` | `TransactionRepository`, `DomainEventPublisher` |
| `ConfirmTransactionUseCase` | `confirmTransaction(UUID transactionId, String correlationId)` | `void` | `TransactionConfirmedEvent` | `TransactionRepository`, `DomainEventPublisher` |
| `FailTransactionUseCase` | `failTransaction(UUID transactionId, String error, String correlationId)` | `void` | `TransactionStatusChangedEvent` | `TransactionRepository`, `DomainEventPublisher` |

### User Management
| Use Case | Method Signature | Returns | Events | Dependencies |
|----------|-----------------|---------|--------|--------------|
| `CreateUserUseCase` | `createUser(String email, String password, String correlationId)` | `User` | `UserCreatedEvent` | `UserRepository`, `DomainEventPublisher` |
| `AuthenticateUserUseCase` | `authenticateUser(String email, String password, String correlationId)` | `UserSession` | `UserAuthenticatedEvent` | `UserRepository`, `UserSessionRepository`, `DomainEventPublisher` |
| `UpdateUserProfileUseCase` | `updateUserProfile(UUID userId, UserProfile profile, String correlationId)` | `User` | `UserProfileUpdatedEvent` | `UserRepository`, `DomainEventPublisher` |
| `ChangePasswordUseCase` | `changePassword(UUID userId, String oldPassword, String newPassword, String correlationId)` | `void` | None | `UserRepository`, `UserSessionRepository`, `DomainEventPublisher` |
| `DeactivateUserUseCase` | `deactivateUser(UUID userId, String correlationId)` | `void` | `UserStatusChangedEvent` | `UserRepository`, `UserSessionRepository`, `DomainEventPublisher` |

### Token Management
| Use Case | Method Signature | Returns | Events | Dependencies |
|----------|-----------------|---------|--------|--------------|
| `AddTokenToWalletUseCase` | `addTokenToWallet(UUID walletId, UUID tokenId, String correlationId)` | `void` | `TokenAddedToWalletEvent` | `WalletRepository`, `TokenRepository`, `WalletTokenRepository`, `DomainEventPublisher` |
| `RemoveTokenFromWalletUseCase` | `removeTokenFromWallet(UUID walletId, UUID tokenId, String correlationId)` | `void` | `TokenRemovedFromWalletEvent` | `WalletRepository`, `WalletTokenRepository`, `DomainEventPublisher` |
| `GetTokenBalanceUseCase` | `getTokenBalance(UUID walletId, UUID tokenId, String correlationId)` | `TokenBalance` | None | `WalletRepository`, `TokenRepository`, `TokenBalanceRepository` |
| `ListSupportedTokensUseCase` | `listSupportedTokens(UUID networkId, String correlationId)` | `List<Token>` | None | `TokenRepository`, `NetworkRepository` |

### Network Management
| Use Case | Method Signature | Returns | Events | Dependencies |
|----------|-----------------|---------|--------|--------------|
| `AddNetworkUseCase` | `addNetwork(String name, String chainId, String rpcUrl, String explorerUrl, String correlationId)` | `Network` | `NetworkCreatedEvent` | `NetworkRepository`, `DomainEventPublisher` |
| `ListNetworksUseCase` | `listNetworks(String correlationId)` | `List<Network>` | None | `NetworkRepository` |

### Analytics and Portfolio
| Use Case | Method Signature | Returns | Events | Dependencies |
|----------|-----------------|---------|--------|--------------|
| `CheckBalanceUseCase` | `checkBalance(UUID walletId, String correlationId)` | `BigDecimal` | None | `WalletRepository` |
| `EstimateTransactionFeeUseCase` | `estimateTransactionFee(UUID networkId, TransactionType type, String correlationId)` | `TransactionFee` | None | `NetworkRepository`, `TransactionFeeRepository` |
| `GetPortfolioSummaryUseCase` | `getPortfolioSummary(UUID userId, String correlationId)` | `PortfolioSummary` | None | `WalletRepository`, `AddressRepository`, `TokenBalanceRepository`, `TokenRepository` |

## Common Patterns

### 1. Correlation ID Pattern
All use cases accept a `correlationId` parameter for distributed tracing:
```java
String correlationId = UUID.randomUUID().toString();
useCase.someOperation(parameters, correlationId);
```

### 2. Error Handling Pattern
```java
try {
    Result result = useCase.operation(params, correlationId);
    // Handle success
} catch (IllegalArgumentException e) {
    // Handle validation errors
} catch (RuntimeException e) {
    // Handle system errors
}
```

### 3. Event Publishing Pattern
```java
public void someOperation(UUID id, String correlationId) {
    // Business logic
    Entity entity = repository.findById(id);
    entity.performAction();
    repository.save(entity);
    
    // Publish event
    DomainEvent event = new SomeEvent(entity.getId(), correlationId);
    eventPublisher.publish(event);
}
```

## Configuration Reference

### Bean Definitions
All use cases are defined in `UseCaseConfig.java`:
```java
@Bean
public SomeUseCase someUseCase(Dependency1 dep1, Dependency2 dep2) {
    return new SomeUseCase(dep1, dep2);
}
```

### Properties
```yaml
app:
  usecases:
    enabled: true  # Enable/disable all use cases
    tracing:
      enabled: true  # Enable use case tracing
```

## Quick Start Examples

### Creating a Wallet
```java
@Autowired
private CreateWalletUseCase createWalletUseCase;

public Wallet createNewWallet(UUID userId) {
    String correlationId = UUID.randomUUID().toString();
    return createWalletUseCase.createWallet(userId, correlationId);
}
```

### Validating an Address
```java
@Autowired
private ValidateAddressUseCase validateAddressUseCase;

public boolean isValidAddress(String address, UUID networkId) {
    String correlationId = UUID.randomUUID().toString();
    AddressValidationResult result = validateAddressUseCase.validateAddress(
        address, networkId, correlationId);
    return result.isValid() && result.isNetworkCompatible();
}
```

### Transferring Funds
```java
@Autowired
private TransferFundsUseCase transferFundsUseCase;

public void transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
    String correlationId = UUID.randomUUID().toString();
    transferFundsUseCase.transferFunds(
        fromWalletId, toWalletId, amount, correlationId);
}
```

## Common Error Messages

### Validation Errors
- `"Address value must be provided"` - Empty address
- `"Correlation ID must be provided"` - Missing correlation ID
- `"Correlation ID must be a valid UUID"` - Invalid correlation ID format
- `"Wallet not found"` - Invalid wallet ID
- `"Transfer amount must be greater than zero"` - Invalid amount

### Business Rule Violations
- `"Insufficient funds"` - Wallet balance too low
- `"Wallet is not active"` - Wallet in inactive state
- `"Network is not available"` - Network in maintenance
- `"Invalid address format"` - Address doesn't match expected pattern

## Performance Tips

### 1. Use Batch Operations
```java
// Instead of looping
for (String address : addresses) {
    validator.validateAddress(address, networkId, correlationId);
}

// Use batch validation
validator.validateAddresses(addresses, networkId, correlationId);
```

### 2. Cache Frequently Used Data
```java
// Cache network lookups if validating many addresses against same network
Network network = networkCache.get(networkId);
if (network == null) {
    network = networkRepository.findById(networkId, correlationId).orElse(null);
    networkCache.put(networkId, network);
}
```

### 3. Parallel Processing
```java
// For independent operations
List<CompletableFuture<Result>> futures = addresses.stream()
    .map(address -> CompletableFuture.supplyAsync(() -> 
        validator.validateAddress(address, networkId, correlationId)))
    .collect(Collectors.toList());

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

## Testing Guidelines

### Unit Test Structure
```java
@Test
void operationName_validInput_returnsExpectedResult() {
    // Given
    MockDependency dependency = mock(Dependency.class);
    when(dependency.someMethod()).thenReturn(expectedValue);
    
    SomeUseCase useCase = new SomeUseCase(dependency);
    
    // When
    Result result = useCase.operation(input, correlationId);
    
    // Then
    assertNotNull(result);
    assertEquals(expectedValue, result.getProperty());
    verify(dependency).someMethod();
}
```

### Integration Test Structure
```java
@SpringBootTest
class SomeUseCaseIntegrationTest {
    
    @Autowired
    private SomeUseCase useCase;
    
    @Autowired
    private SomeRepository repository;
    
    @Test
    void operationName_integrationTest() {
        // Setup test data
        Entity entity = createTestEntity();
        repository.save(entity);
        
        // Execute use case
        Result result = useCase.operation(entity.getId(), correlationId);
        
        // Verify results
        assertNotNull(result);
        // Additional assertions
    }
}
```

## Monitoring Metrics

### Key Metrics to Monitor
- **Success Rate**: Percentage of successful operations
- **Error Rate**: Percentage of failed operations by error type
- **Latency**: Average operation execution time
- **Throughput**: Operations per second
- **Batch Size**: Average batch operation size

### Logging Context
Always include in logs:
- Correlation ID
- Use case name
- Operation parameters (sanitized)
- Execution time
- Result status

## Related Documentation

- [Detailed Use Cases Documentation](use_cases.md)
- [ValidateAddressUseCase Details](validate_address_usecase.md)
- [Domain Models](domain_models.md)
- [Domain Events](domain_events.md)
- [Domain Repositories](domain_repositories.md)
- [UseCaseConfig Source](../src/main/java/dev/bloco/wallet/hub/config/UseCaseConfig.java)