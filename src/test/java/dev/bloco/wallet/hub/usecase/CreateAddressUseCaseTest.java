package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AddressType;
import dev.bloco.wallet.hub.domain.model.network.Network;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the CreateAddressUseCase class.
 * These tests ensure that the createAddress method behaves as expected under various scenarios.
 */
@DisplayName("Create Address Use Case")
class CreateAddressUseCaseTest {

  @Test
  @DisplayName("createAddress creates and publishes AddressCreatedEvent")
  void shouldCreateAddressSuccessfully() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateAddressUseCase useCase = new CreateAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher
    );

    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String publicKeyValue = "publicKey";
    String accountAddressValue = "accountAddress";
    AddressType addressType = AddressType.EXTERNAL;
    String derivationPath = "m/44'/0'/0'/0/0";
    String correlationId = UUID.randomUUID().toString();

    Wallet wallet = mock(Wallet.class);
    Network network = mock(Network.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(networkRepository.findById(eq(networkId), anyString())).thenReturn(Optional.of(network));
    when(network.isAvailable()).thenReturn(true);
    when(addressRepository.findByNetworkIdAndAccountAddress(networkId, accountAddressValue)).thenReturn(Optional.empty());
    when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Address result = useCase.createAddress(walletId, networkId, publicKeyValue, accountAddressValue, addressType, derivationPath, correlationId);

    // Assert
    assertNotNull(result);
    assertEquals(walletId, result.getWalletId());
    assertEquals(networkId, result.getNetworkId());
    assertEquals(publicKeyValue, result.getPublicKey().getValue());
    assertEquals(accountAddressValue, result.getAccountAddress().getValue());
    verify(walletRepository, times(1)).update(wallet);
    verify(addressRepository, times(1)).save(any(Address.class));
    // Domain events are published and then cleared by the use case, so we verify the publish was called
    verify(eventPublisher, times(1)).publish(any());
  }

  @Test
  @DisplayName("createAddress throws exception when wallet is deleted")
  void shouldThrowExceptionWhenWalletNotFound() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateAddressUseCase useCase = new CreateAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher
    );

    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String publicKeyValue = "publicKey";
    String accountAddressValue = "accountAddress";
    AddressType addressType = AddressType.EXTERNAL;
    String derivationPath = "m/44'/0'/0'/0/0";
    String correlationId = UUID.randomUUID().toString();

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.createAddress(walletId, networkId, publicKeyValue, accountAddressValue, addressType, derivationPath, correlationId));

    assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
  }

  @Test
  @DisplayName("createAddress throws exception when network is deleted")
  void shouldThrowExceptionWhenNetworkNotFound() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateAddressUseCase useCase = new CreateAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher
    );

    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String publicKeyValue = "publicKey";
    String accountAddressValue = "accountAddress";
    AddressType addressType = AddressType.EXTERNAL;
    String derivationPath = "m/44'/0'/0'/0/0";
    String correlationId = UUID.randomUUID().toString();

    Wallet wallet = mock(Wallet.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(networkRepository.findById(eq(networkId), anyString())).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.createAddress(walletId, networkId, publicKeyValue, accountAddressValue, addressType, derivationPath, correlationId));

    assertEquals("Network not found with id: " + networkId, exception.getMessage());
  }

  @Test
  @DisplayName("createAddress throws exception when network is not available")
  void shouldThrowExceptionWhenNetworkUnavailable() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateAddressUseCase useCase = new CreateAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher
    );

    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String publicKeyValue = "publicKey";
    String accountAddressValue = "accountAddress";
    AddressType addressType = AddressType.EXTERNAL;
    String derivationPath = "m/44'/0'/0'/0/0";
    String correlationId = UUID.randomUUID().toString();

    Wallet wallet = mock(Wallet.class);
    Network network = mock(Network.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(networkRepository.findById(eq(networkId), anyString())).thenReturn(Optional.of(network));
    when(network.isAvailable()).thenReturn(false);

    // Act & Assert
    IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
        useCase.createAddress(walletId, networkId, publicKeyValue, accountAddressValue, addressType, derivationPath, correlationId));

    assertEquals("Network is not available: " + network.getName(), exception.getMessage());
  }

  @Test
  @DisplayName("createAddress throws exception when address already exists")
  void shouldThrowExceptionWhenAddressAlreadyExists() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateAddressUseCase useCase = new CreateAddressUseCase(
        addressRepository, walletRepository, networkRepository, eventPublisher
    );

    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    String publicKeyValue = "publicKey";
    String accountAddressValue = "accountAddress";
    AddressType addressType = AddressType.EXTERNAL;
    String derivationPath = "m/44'/0'/0'/0/0";
    String correlationId = UUID.randomUUID().toString();

    Wallet wallet = mock(Wallet.class);
    Network network = mock(Network.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(networkRepository.findById(eq(networkId), anyString())).thenReturn(Optional.of(network));
    when(network.isAvailable()).thenReturn(true);
    when(addressRepository.findByNetworkIdAndAccountAddress(networkId, accountAddressValue))
        .thenReturn(Optional.of(mock(Address.class)));

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.createAddress(walletId, networkId, publicKeyValue, accountAddressValue, addressType, derivationPath, correlationId));

    assertEquals("Address already exists on network: " + accountAddressValue, exception.getMessage());
  }
}