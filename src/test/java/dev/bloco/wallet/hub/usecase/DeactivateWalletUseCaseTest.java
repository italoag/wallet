package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Deactivate Wallet Use Case Tests")
class DeactivateWalletUseCaseTest {

  /**
   * Test Suite for the DeactivateWalletUseCase class. This class is responsible for testing
   * the behavior and correctness of the deactivateWallet method, which attempts to deactivate
   * a Wallet by applying specific business rules. The tests cover multiple potential scenarios
   * such as successful deactivation, non-existent wallets, invalid states, and event publishing.
   */

  @Test
  @DisplayName("deactivateWallet deactivates a wallet successfully")
  void deactivateWallet_shouldDeactivateSuccessfully() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    DeactivateWalletUseCase deactivateWalletUseCase = new DeactivateWalletUseCase(walletRepository, eventPublisher);

    UUID walletId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();
    Wallet wallet = mock(Wallet.class);
    DomainEvent mockEvent = mock(DomainEvent.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(wallet.isDeleted()).thenReturn(false);
    when(wallet.getDomainEvents()).thenReturn(List.of(mockEvent));

    // Act
    Wallet result = deactivateWalletUseCase.deactivateWallet(walletId, correlationId);

    // Assert
    verify(wallet).setCorrelationId(UUID.fromString(correlationId));
    verify(wallet).deactivate();
    verify(walletRepository).update(wallet);
    verify(wallet).getDomainEvents();
    verify(wallet).clearEvents();
    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
    verify(eventPublisher, atLeastOnce()).publish(eventCaptor.capture());

    assertNotNull(result);
    assertEquals(wallet, result);
  }

  @Test
  @DisplayName("deactivateWallet throws exception when wallet is not found")
  void deactivateWallet_shouldThrowExceptionIfWalletNotFound() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    DeactivateWalletUseCase deactivateWalletUseCase = new DeactivateWalletUseCase(walletRepository, eventPublisher);

    UUID walletId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> deactivateWalletUseCase.deactivateWallet(walletId, correlationId));
    assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
  }

  @Test
  @DisplayName("deactivateWallet throws exception when wallet is already deleted")
  void deactivateWallet_shouldThrowExceptionIfWalletIsDeleted() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    DeactivateWalletUseCase deactivateWalletUseCase = new DeactivateWalletUseCase(walletRepository, eventPublisher);

    UUID walletId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();
    Wallet wallet = mock(Wallet.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(wallet.isDeleted()).thenReturn(true);

    // Act & Assert
    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> deactivateWalletUseCase.deactivateWallet(walletId, correlationId));
    assertEquals("Deleted wallets cannot be deactivated", exception.getMessage());
  }

  @Test
  @DisplayName("deactivateWallet publishes events and clears after deactivation")
  void deactivateWallet_shouldPublishEventsAndClearAfterDeactivation() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    DeactivateWalletUseCase deactivateWalletUseCase = new DeactivateWalletUseCase(walletRepository, eventPublisher);

    UUID walletId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();
    Wallet wallet = mock(Wallet.class);
    DomainEvent mockEvent = mock(DomainEvent.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(wallet.isDeleted()).thenReturn(false);
    when(wallet.getDomainEvents()).thenReturn(List.of(mockEvent));

    // Act
    deactivateWalletUseCase.deactivateWallet(walletId, correlationId);

    // Assert
    verify(wallet).deactivate();
    verify(walletRepository).update(wallet);
    verify(wallet).setCorrelationId(UUID.fromString(correlationId));
    verify(wallet).getDomainEvents();
    verify(eventPublisher, atLeastOnce()).publish(any(DomainEvent.class));
    verify(wallet).clearEvents();
  }
}