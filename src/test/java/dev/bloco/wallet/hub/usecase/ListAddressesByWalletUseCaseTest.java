package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AddressStatus;
import dev.bloco.wallet.hub.usecase.ListAddressesByWalletUseCase.AddressCountSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("List Addresses By Wallet Use Case Tests")
class ListAddressesByWalletUseCaseTest {

  @Test
  @DisplayName("listAddresses returns all addresses for a wallet")
  void shouldReturnAllAddressesForWallet() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    Wallet wallet = mock(Wallet.class);

    Address address1 = mock(Address.class);
    Address address2 = mock(Address.class);
    List<Address> addresses = List.of(address1, address2);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(addresses);

    // Act
    List<Address> result = useCase.listAddresses(walletId);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.contains(address1));
    assertTrue(result.contains(address2));

    verify(walletRepository, times(1)).findById(walletId);
    verify(addressRepository, times(1)).findByWalletId(walletId);
  }

  @Test
  @DisplayName("listAddresses throws exception when walletId is null")
  void shouldThrowExceptionWhenWalletIdIsNull() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.listAddresses(null));

    assertEquals("Wallet ID must be provided", exception.getMessage());
    verifyNoInteractions(walletRepository, addressRepository);
  }

  @Test
  @DisplayName("listAddresses throws exception when wallet not found")
  void shouldThrowExceptionWhenWalletNotFound() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.listAddresses(walletId));

    assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
    verify(walletRepository, times(1)).findById(walletId);
    verifyNoInteractions(addressRepository);
  }

  @Test
  @DisplayName("listAddresses returns an empty list when no addresses exist")
  void shouldReturnEmptyListWhenNoAddressesExist() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    Wallet wallet = mock(Wallet.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(List.of());

    // Act
    List<Address> result = useCase.listAddresses(walletId);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(walletRepository, times(1)).findById(walletId);
    verify(addressRepository, times(1)).findByWalletId(walletId);
  }

  @Test
  @DisplayName("listAddresses only interacts with repositories as expected")
  void shouldVerifyRepositoryInteractions() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    Wallet wallet = mock(Wallet.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(List.of());

    // Act
    List<Address> result = useCase.listAddresses(walletId);

    // Assert
    verify(walletRepository, times(1)).findById(walletId);
    verify(addressRepository, times(1)).findByWalletId(walletId);
    assertTrue(result.isEmpty());
  }

  /**
   * Tests for ListAddressesByWalletUseCase#listActiveAddresses method.
   * The method returns only active addresses for a wallet.
   */

  @Test
  @DisplayName("listActiveAddresses returns only active addresses")
  void shouldReturnOnlyActiveAddresses() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    Wallet wallet = mock(Wallet.class);

    Address activeAddress1 = mock(Address.class);
    Address activeAddress2 = mock(Address.class);
    List<Address> activeAddresses = List.of(activeAddress1, activeAddress2);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletIdAndStatus(walletId, AddressStatus.ACTIVE)).thenReturn(activeAddresses);

    // Act
    List<Address> result = useCase.listActiveAddresses(walletId);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.contains(activeAddress1));
    assertTrue(result.contains(activeAddress2));

    verify(walletRepository, times(1)).findById(walletId);
    verify(addressRepository, times(1)).findByWalletIdAndStatus(walletId, AddressStatus.ACTIVE);
  }

  @Test
  @DisplayName("listActiveAddresses throws exception when walletId is null")
  void listActiveAddresses_shouldThrowExceptionWhenWalletIdIsNull() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.listActiveAddresses(null));

    assertEquals("Wallet ID must be provided", exception.getMessage());
    verifyNoInteractions(walletRepository, addressRepository);
  }

  @Test
  @DisplayName("listActiveAddresses throws exception when wallet not found")
  void listActiveAddresses_shouldThrowExceptionWhenWalletNotFound() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.listActiveAddresses(walletId));

    assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
    verify(walletRepository, times(1)).findById(walletId);
    verifyNoInteractions(addressRepository);
  }

  @Test
  @DisplayName("listActiveAddresses returns empty list when no active addresses exist")
  void listActiveAddresses_shouldReturnEmptyListWhenNoActiveAddressesExist() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    Wallet wallet = mock(Wallet.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletIdAndStatus(walletId, AddressStatus.ACTIVE)).thenReturn(List.of());

    // Act
    List<Address> result = useCase.listActiveAddresses(walletId);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(walletRepository, times(1)).findById(walletId);
    verify(addressRepository, times(1)).findByWalletIdAndStatus(walletId, AddressStatus.ACTIVE);
  }

  /**
   * Tests for ListAddressesByWalletUseCase#listAddressesByStatus method.
   * The method returns addresses filtered by a specific status.
   */

  @Test
  @DisplayName("listAddressesByStatus returns addresses with ACTIVE status")
  void shouldReturnAddressesWithActiveStatus() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    Wallet wallet = mock(Wallet.class);

    Address activeAddress = mock(Address.class);
    List<Address> activeAddresses = List.of(activeAddress);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletIdAndStatus(walletId, AddressStatus.ACTIVE)).thenReturn(activeAddresses);

    // Act
    List<Address> result = useCase.listAddressesByStatus(walletId, AddressStatus.ACTIVE);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.contains(activeAddress));

    verify(walletRepository, times(1)).findById(walletId);
    verify(addressRepository, times(1)).findByWalletIdAndStatus(walletId, AddressStatus.ACTIVE);
  }

  @Test
  @DisplayName("listAddressesByStatus returns addresses with ARCHIVED status")
  void shouldReturnAddressesWithArchivedStatus() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    Wallet wallet = mock(Wallet.class);

    Address archivedAddress = mock(Address.class);
    List<Address> archivedAddresses = List.of(archivedAddress);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletIdAndStatus(walletId, AddressStatus.ARCHIVED)).thenReturn(archivedAddresses);

    // Act
    List<Address> result = useCase.listAddressesByStatus(walletId, AddressStatus.ARCHIVED);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.contains(archivedAddress));

    verify(walletRepository, times(1)).findById(walletId);
    verify(addressRepository, times(1)).findByWalletIdAndStatus(walletId, AddressStatus.ARCHIVED);
  }

  @Test
  @DisplayName("listAddressesByStatus throws exception when walletId is null")
  void listAddressesByStatus_shouldThrowExceptionWhenWalletIdIsNull() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.listAddressesByStatus(null, AddressStatus.ACTIVE));

    assertEquals("Wallet ID must be provided", exception.getMessage());
    verifyNoInteractions(walletRepository, addressRepository);
  }

  @Test
  @DisplayName("listAddressesByStatus throws exception when status is null")
  void listAddressesByStatus_shouldThrowExceptionWhenStatusIsNull() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.listAddressesByStatus(walletId, null));

    assertEquals("Status must be provided", exception.getMessage());
    verifyNoInteractions(walletRepository, addressRepository);
  }

  @Test
  @DisplayName("listAddressesByStatus throws exception when wallet not found")
  void listAddressesByStatus_shouldThrowExceptionWhenWalletNotFound() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.listAddressesByStatus(walletId, AddressStatus.ACTIVE));

    assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
    verify(walletRepository, times(1)).findById(walletId);
    verifyNoInteractions(addressRepository);
  }

  @Test
  @DisplayName("listAddressesByStatus returns empty list when no addresses match status")
  void listAddressesByStatus_shouldReturnEmptyListWhenNoAddressesMatchStatus() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    Wallet wallet = mock(Wallet.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletIdAndStatus(walletId, AddressStatus.ARCHIVED)).thenReturn(List.of());

    // Act
    List<Address> result = useCase.listAddressesByStatus(walletId, AddressStatus.ARCHIVED);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(walletRepository, times(1)).findById(walletId);
    verify(addressRepository, times(1)).findByWalletIdAndStatus(walletId, AddressStatus.ARCHIVED);
  }

  /**
   * Tests for ListAddressesByWalletUseCase#getAddressCountSummary method.
   * The method returns a summary with counts of addresses by status.
   */

  @Test
  @DisplayName("getAddressCountSummary returns correct counts for mixed statuses")
  void shouldReturnCorrectCountsForMixedStatuses() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    Wallet wallet = mock(Wallet.class);
    when(wallet.getName()).thenReturn("Test Wallet");

    Address activeAddress1 = mock(Address.class);
    when(activeAddress1.getStatus()).thenReturn(AddressStatus.ACTIVE);
    Address activeAddress2 = mock(Address.class);
    when(activeAddress2.getStatus()).thenReturn(AddressStatus.ACTIVE);
    Address archivedAddress = mock(Address.class);
    when(archivedAddress.getStatus()).thenReturn(AddressStatus.ARCHIVED);

    List<Address> allAddresses = List.of(activeAddress1, activeAddress2, archivedAddress);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(allAddresses);

    // Act
    AddressCountSummary result = useCase.getAddressCountSummary(walletId);

    // Assert
    assertNotNull(result);
    assertEquals(walletId, result.walletId());
    assertEquals("Test Wallet", result.walletName());
    assertEquals(3, result.totalAddresses());
    assertEquals(2, result.activeAddresses());
    assertEquals(1, result.archivedAddresses());

    verify(walletRepository, times(1)).findById(walletId);
    verify(addressRepository, times(1)).findByWalletId(walletId);
  }

  @Test
  @DisplayName("getAddressCountSummary returns zero counts for empty address list")
  void shouldReturnZeroCountsForEmptyAddressList() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    Wallet wallet = mock(Wallet.class);
    when(wallet.getName()).thenReturn("Empty Wallet");

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(List.of());

    // Act
    AddressCountSummary result = useCase.getAddressCountSummary(walletId);

    // Assert
    assertNotNull(result);
    assertEquals(walletId, result.walletId());
    assertEquals("Empty Wallet", result.walletName());
    assertEquals(0, result.totalAddresses());
    assertEquals(0, result.activeAddresses());
    assertEquals(0, result.archivedAddresses());

    verify(walletRepository, times(1)).findById(walletId);
    verify(addressRepository, times(1)).findByWalletId(walletId);
  }

  @Test
  @DisplayName("getAddressCountSummary returns correct counts for all active addresses")
  void shouldReturnCorrectCountsForAllActiveAddresses() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    Wallet wallet = mock(Wallet.class);
    when(wallet.getName()).thenReturn("Active Wallet");

    Address activeAddress1 = mock(Address.class);
    when(activeAddress1.getStatus()).thenReturn(AddressStatus.ACTIVE);
    Address activeAddress2 = mock(Address.class);
    when(activeAddress2.getStatus()).thenReturn(AddressStatus.ACTIVE);
    Address activeAddress3 = mock(Address.class);
    when(activeAddress3.getStatus()).thenReturn(AddressStatus.ACTIVE);

    List<Address> allAddresses = List.of(activeAddress1, activeAddress2, activeAddress3);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(allAddresses);

    // Act
    AddressCountSummary result = useCase.getAddressCountSummary(walletId);

    // Assert
    assertNotNull(result);
    assertEquals(walletId, result.walletId());
    assertEquals("Active Wallet", result.walletName());
    assertEquals(3, result.totalAddresses());
    assertEquals(3, result.activeAddresses());
    assertEquals(0, result.archivedAddresses());

    verify(walletRepository, times(1)).findById(walletId);
    verify(addressRepository, times(1)).findByWalletId(walletId);
  }

  @Test
  @DisplayName("getAddressCountSummary returns correct counts for all archived addresses")
  void shouldReturnCorrectCountsForAllArchivedAddresses() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    Wallet wallet = mock(Wallet.class);
    when(wallet.getName()).thenReturn("Archived Wallet");

    Address archivedAddress1 = mock(Address.class);
    when(archivedAddress1.getStatus()).thenReturn(AddressStatus.ARCHIVED);
    Address archivedAddress2 = mock(Address.class);
    when(archivedAddress2.getStatus()).thenReturn(AddressStatus.ARCHIVED);

    List<Address> allAddresses = List.of(archivedAddress1, archivedAddress2);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(allAddresses);

    // Act
    AddressCountSummary result = useCase.getAddressCountSummary(walletId);

    // Assert
    assertNotNull(result);
    assertEquals(walletId, result.walletId());
    assertEquals("Archived Wallet", result.walletName());
    assertEquals(2, result.totalAddresses());
    assertEquals(0, result.activeAddresses());
    assertEquals(2, result.archivedAddresses());

    verify(walletRepository, times(1)).findById(walletId);
    verify(addressRepository, times(1)).findByWalletId(walletId);
  }

  @Test
  @DisplayName("getAddressCountSummary throws exception when walletId is null")
  void getAddressCountSummary_shouldThrowExceptionWhenWalletIdIsNull() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.getAddressCountSummary(null));

    assertEquals("Wallet ID must be provided", exception.getMessage());
    verifyNoInteractions(walletRepository, addressRepository);
  }

  @Test
  @DisplayName("getAddressCountSummary throws exception when wallet not found")
  void getAddressCountSummary_shouldThrowExceptionWhenWalletNotFound() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);

    ListAddressesByWalletUseCase useCase = new ListAddressesByWalletUseCase(addressRepository, walletRepository);

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.getAddressCountSummary(walletId));

    assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
    verify(walletRepository, times(1)).findById(walletId);
    verifyNoInteractions(addressRepository);
  }

  /**
   * Tests for AddressCountSummary record.
   * Verifies the record creation and field access.
   */

  @Test
  @DisplayName("AddressCountSummary record creates instance with all fields")
  void shouldCreateAddressCountSummaryWithAllFields() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    String walletName = "Test Wallet";
    int totalAddresses = 10;
    long activeAddresses = 7;
    long archivedAddresses = 3;

    // Act
    AddressCountSummary summary = new AddressCountSummary(walletId, walletName, totalAddresses, activeAddresses, archivedAddresses);

    // Assert
    assertNotNull(summary);
    assertEquals(walletId, summary.walletId());
    assertEquals(walletName, summary.walletName());
    assertEquals(totalAddresses, summary.totalAddresses());
    assertEquals(activeAddresses, summary.activeAddresses());
    assertEquals(archivedAddresses, summary.archivedAddresses());
  }

  @Test
  @DisplayName("AddressCountSummary record handles zero counts")
  void shouldHandleZeroCounts() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    String walletName = "Empty Wallet";

    // Act
    AddressCountSummary summary = new AddressCountSummary(walletId, walletName, 0, 0, 0);

    // Assert
    assertNotNull(summary);
    assertEquals(walletId, summary.walletId());
    assertEquals(walletName, summary.walletName());
    assertEquals(0, summary.totalAddresses());
    assertEquals(0, summary.activeAddresses());
    assertEquals(0, summary.archivedAddresses());
  }

  @Test
  @DisplayName("AddressCountSummary record handles large counts")
  void shouldHandleLargeCounts() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    String walletName = "Large Wallet";
    int totalAddresses = 1000;
    long activeAddresses = 750;
    long archivedAddresses = 250;

    // Act
    AddressCountSummary summary = new AddressCountSummary(walletId, walletName, totalAddresses, activeAddresses, archivedAddresses);

    // Assert
    assertNotNull(summary);
    assertEquals(walletId, summary.walletId());
    assertEquals(walletName, summary.walletName());
    assertEquals(1000, summary.totalAddresses());
    assertEquals(750, summary.activeAddresses());
    assertEquals(250, summary.archivedAddresses());
  }
}