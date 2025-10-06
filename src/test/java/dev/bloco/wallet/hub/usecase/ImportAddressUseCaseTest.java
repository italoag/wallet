// <llm-snippet-file>ImportAddressUseCaseTest.java</llm-snippet-file>
package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.network.Network;
import dev.bloco.wallet.hub.usecase.ValidateAddressUseCase.AddressValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Import Address Use Case Tests")
class ImportAddressUseCaseTest {

  @Test
  @DisplayName("importAddresses successfully imports multiple addresses")
  void importAddresses_allValid_success() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    ImportAddressUseCase.AddressImport import1 = new ImportAddressUseCase.AddressImport("validAddress1", "publicKey1", "label1", true);
    ImportAddressUseCase.AddressImport import2 = new ImportAddressUseCase.AddressImport("validAddress2", "publicKey2", "label2", false);

    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    ValidateAddressUseCase validateAddressUseCase = mock(ValidateAddressUseCase.class);

    ImportAddressUseCase useCase = new ImportAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher, validateAddressUseCase);

    Wallet wallet = mock(Wallet.class);
    Network network = mock(Network.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(networkRepository.findById(eq(networkId), anyString())).thenReturn(Optional.of(network));
    when(network.isAvailable()).thenReturn(true);

    when(validateAddressUseCase.validateAddress(eq("validAddress1"), eq(networkId), eq(correlationId)))
        .thenReturn(AddressValidationResult.builder()
            .valid(true).networkCompatible(true).address("validAddress1").build());
    when(validateAddressUseCase.validateAddress(eq("validAddress2"), eq(networkId), eq(correlationId)))
        .thenReturn(AddressValidationResult.builder()
            .valid(true).networkCompatible(true).address("validAddress2").build());

    when(addressRepository.findByNetworkIdAndAccountAddress(eq(networkId), eq("validAddress1")))
        .thenReturn(Optional.empty());
    when(addressRepository.findByNetworkIdAndAccountAddress(eq(networkId), eq("validAddress2")))
        .thenReturn(Optional.empty());

    // Act
    ImportAddressUseCase.BatchImportResult result = useCase.importAddresses(walletId, networkId,
        new ImportAddressUseCase.AddressImport[]{import1, import2}, correlationId);

    // Assert
    assertEquals(2, result.successCount());
    assertEquals(0, result.failureCount());
    assertEquals(0, result.errors().length);
    verify(addressRepository, times(2)).save(any(Address.class));
  }

  @Test
  @DisplayName("importAddresses partially fails when some addresses are invalid")
  void importAddresses_partialFailures() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    ImportAddressUseCase.AddressImport import1 = new ImportAddressUseCase.AddressImport("validAddress1", "publicKey1", "label1", true);
    ImportAddressUseCase.AddressImport import2 = new ImportAddressUseCase.AddressImport("invalidAddress", null, null, true);

    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    ValidateAddressUseCase validateAddressUseCase = mock(ValidateAddressUseCase.class);

    ImportAddressUseCase useCase = new ImportAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher, validateAddressUseCase);

    Wallet wallet = mock(Wallet.class);
    Network network = mock(Network.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(networkRepository.findById(eq(networkId), anyString())).thenReturn(Optional.of(network));
    when(network.isAvailable()).thenReturn(true);

    when(validateAddressUseCase.validateAddress(eq("validAddress1"), eq(networkId), eq(correlationId)))
        .thenReturn(AddressValidationResult.builder()
            .valid(true).networkCompatible(true).address("validAddress1").build());
    when(validateAddressUseCase.validateAddress(eq("invalidAddress"), eq(networkId), eq(correlationId)))
        .thenThrow(new IllegalArgumentException("Invalid address format"));

    when(addressRepository.findByNetworkIdAndAccountAddress(eq(networkId), eq("validAddress1")))
        .thenReturn(Optional.empty());

    // Act
    ImportAddressUseCase.BatchImportResult result = useCase.importAddresses(walletId, networkId,
        new ImportAddressUseCase.AddressImport[]{import1, import2}, correlationId);

    // Assert
    assertEquals(1, result.successCount());
    assertEquals(1, result.failureCount());
    assertEquals(1, result.errors().length);
    assertTrue(result.errors()[0].contains("Failed to import invalidAddress: Invalid address format"));
    verify(addressRepository, times(1)).save(any(Address.class));
  }

  @Test
  @DisplayName("importAddresses fails to import when all addresses are invalid")
  void importAddresses_allFailures() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    ImportAddressUseCase.AddressImport address1 = new ImportAddressUseCase.AddressImport("invalidAddress1", null, null, true);
    ImportAddressUseCase.AddressImport address2 = new ImportAddressUseCase.AddressImport("invalidAddress2", "publicKey2", "label2", false);

    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    ValidateAddressUseCase validateAddressUseCase = mock(ValidateAddressUseCase.class);

    ImportAddressUseCase useCase = new ImportAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher, validateAddressUseCase);

    Wallet wallet = mock(Wallet.class);
    Network network = mock(Network.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(networkRepository.findById(eq(networkId), anyString())).thenReturn(Optional.of(network));
    when(network.isAvailable()).thenReturn(true);

    when(validateAddressUseCase.validateAddress(eq("invalidAddress1"), eq(networkId), eq(correlationId)))
        .thenThrow(new IllegalArgumentException("Invalid address format"));
    when(validateAddressUseCase.validateAddress(eq("invalidAddress2"), eq(networkId), eq(correlationId)))
        .thenThrow(new IllegalArgumentException("Invalid public key provided"));

    // Act
    ImportAddressUseCase.BatchImportResult result = useCase.importAddresses(walletId, networkId,
        new ImportAddressUseCase.AddressImport[]{address1, address2}, correlationId);

    // Assert
    assertEquals(0, result.successCount());
    assertEquals(2, result.failureCount());
    assertEquals(2, result.errors().length);
    assertTrue(result.errors()[0].contains("Failed to import invalidAddress1: Invalid address format"));
    assertTrue(result.errors()[1].contains("Failed to import invalidAddress2: Invalid public key provided"));
    verify(addressRepository, never()).save(any(Address.class));
  }

  @Test
  @DisplayName("importAddresses returns zero for empty address import array")
  void importAddresses_emptyArray() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    ValidateAddressUseCase validateAddressUseCase = mock(ValidateAddressUseCase.class);

    ImportAddressUseCase useCase = new ImportAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher, validateAddressUseCase);

    // Act
    ImportAddressUseCase.BatchImportResult result = useCase.importAddresses(walletId, networkId,
        new ImportAddressUseCase.AddressImport[0], correlationId);

    // Assert
    assertEquals(0, result.successCount());
    assertEquals(0, result.failureCount());
    assertEquals(0, result.errors().length);
  }

  @Test
  @DisplayName("importAddress adds address to wallet and publishes event")
  void importAddress_validData_watchOnly_success() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String accountAddressValue = "validAddress";
    String correlationId = UUID.randomUUID().toString();
    boolean isWatchOnly = true;

    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    ValidateAddressUseCase validateAddressUseCase = mock(ValidateAddressUseCase.class);

    ImportAddressUseCase useCase = new ImportAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher, validateAddressUseCase);

    Wallet wallet = mock(Wallet.class);
    Network network = mock(Network.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(network));
    when(network.isAvailable()).thenReturn(true);
    when(validateAddressUseCase.validateAddress(accountAddressValue, networkId, correlationId))
        .thenReturn(AddressValidationResult.builder()
            .valid(true)
            .networkCompatible(true)
            .address(accountAddressValue)
            .format("Ethereum")
            .network("Test Network")
            .build());
    when(addressRepository.findByNetworkIdAndAccountAddress(networkId, accountAddressValue))
        .thenReturn(Optional.empty());

    // Act
    Address result = useCase.importAddress(walletId, networkId, accountAddressValue, null,
        "TestLabel", isWatchOnly, correlationId);

    // Assert
    assertNotNull(result);
    assertEquals(walletId, result.getWalletId());
    assertEquals(networkId, result.getNetworkId());
    assertEquals("watch-only-placeholder", result.getPublicKey().getValue());
    verify(addressRepository).save(any(Address.class));
    verify(walletRepository).update(wallet);
    verify(eventPublisher, atLeastOnce()).publish(any());
    verify(validateAddressUseCase).validateAddress(accountAddressValue, networkId, correlationId);
  }

  @Test
  @DisplayName("importAddress adds address to wallet and publishes event")
  void importAddress_walletNotFound_throwsException() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String accountAddressValue = "validAddress";
    String correlationId = UUID.randomUUID().toString();

    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    ValidateAddressUseCase validateAddressUseCase = mock(ValidateAddressUseCase.class);

    ImportAddressUseCase useCase = new ImportAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher, validateAddressUseCase);

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.importAddress(walletId, networkId, accountAddressValue, null, null, true, correlationId)
    );

    assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
  }

  @Test
  @DisplayName("importAddress adds address to wallet and publishes event")
  void importAddress_networkUnavailable_throwsException() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String accountAddressValue = "validAddress";
    String correlationId = UUID.randomUUID().toString();

    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    ValidateAddressUseCase validateAddressUseCase = mock(ValidateAddressUseCase.class);

    ImportAddressUseCase useCase = new ImportAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher, validateAddressUseCase);

    Wallet wallet = mock(Wallet.class);
    Network network = mock(Network.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(network));
    when(network.isAvailable()).thenReturn(false);

    // Act and Assert
    IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
        useCase.importAddress(walletId, networkId, accountAddressValue, null, null, true, correlationId)
    );

    assertTrue(exception.getMessage().contains("Network is not available"));
  }

  @Test
  @DisplayName("importAddress adds address to wallet and publishes event")
  void importAddress_invalidAddressFormat_throwsException() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String accountAddressValue = "invalidAddress";
    String correlationId = UUID.randomUUID().toString();

    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    ValidateAddressUseCase validateAddressUseCase = mock(ValidateAddressUseCase.class);

    ImportAddressUseCase useCase = new ImportAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher, validateAddressUseCase);

    Wallet wallet = mock(Wallet.class);
    Network network = mock(Network.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(network));
    when(network.isAvailable()).thenReturn(true);
    when(validateAddressUseCase.validateAddress(accountAddressValue, networkId, correlationId))
        .thenReturn(AddressValidationResult.builder()
            .valid(false)
            .networkCompatible(false)
            .address(accountAddressValue)
            .format("Invalid")
            .network("Unknown")
            .error("Invalid format")
            .build());

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.importAddress(walletId, networkId, accountAddressValue, null, null, true, correlationId)
    );

    assertTrue(exception.getMessage().contains("Invalid address format"));
  }

  @Test
  @DisplayName("importAddress adds address to wallet and publishes event")
  void importAddress_addressAlreadyExists_throwsException() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String accountAddressValue = "duplicateAddress";
    String correlationId = UUID.randomUUID().toString();

    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    ValidateAddressUseCase validateAddressUseCase = mock(ValidateAddressUseCase.class);

    ImportAddressUseCase useCase = new ImportAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher, validateAddressUseCase);

    Wallet wallet = mock(Wallet.class);
    Network network = mock(Network.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(network));
    when(network.isAvailable()).thenReturn(true);
    when(validateAddressUseCase.validateAddress(accountAddressValue, networkId, correlationId))
        .thenReturn(AddressValidationResult.builder()
            .valid(true)
            .networkCompatible(true)
            .address(accountAddressValue)
            .format("Ethereum")
            .network("Test Network")
            .build());
    when(addressRepository.findByNetworkIdAndAccountAddress(networkId, accountAddressValue))
        .thenReturn(Optional.of(mock(Address.class)));

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.importAddress(walletId, networkId, accountAddressValue, null, null, true, correlationId)
    );

    assertTrue(exception.getMessage().contains("Address already exists"));
  }
}