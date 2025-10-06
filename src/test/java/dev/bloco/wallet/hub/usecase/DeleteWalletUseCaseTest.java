package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("Delete Wallet Use Case Tests")
class DeleteWalletUseCaseTest {

  /**
   * Test scenario: Successfully delete a wallet.
   * Expected outcome: `walletRepository.update` is called, events are published, and no exception is thrown.
   */
  @Test
  @DisplayName("deleteWallet successfully deletes a wallet")
  void deleteWallet_successfully() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    String reason = "User Request";
    String correlationId = UUID.randomUUID().toString();

    Wallet wallet = mock(Wallet.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    DomainEvent mockEvent = mock(DomainEvent.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(wallet.isDeleted()).thenReturn(false);
    when(wallet.getBalance()).thenReturn(BigDecimal.ZERO);
    when(wallet.getDomainEvents()).thenReturn(List.of(mockEvent));

    DeleteWalletUseCase useCase = new DeleteWalletUseCase(walletRepository, eventPublisher);

    // Act
    Wallet deletedWallet = useCase.deleteWallet(walletId, reason, correlationId);

    // Assert
    assertNotNull(deletedWallet);
    verify(wallet).delete(reason);
    verify(wallet).setCorrelationId(UUID.fromString(correlationId));
    verify(walletRepository).update(wallet);
    verify(wallet).getDomainEvents();
    verify(eventPublisher, times(1)).publish(any(DomainEvent.class));
    verify(wallet).clearEvents();
  }

  /**
   * Test scenario: Wallet deletion with a null or empty reason.
   * Expected outcome: IllegalArgumentException is thrown.
   */
  @Test
  @DisplayName("deleteWallet throws exception when reason is null or empty")
  void deleteWallet_throwsException_whenReasonIsNullOrEmpty() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    DeleteWalletUseCase useCase = new DeleteWalletUseCase(walletRepository, eventPublisher);

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.deleteWallet(walletId, null, correlationId));
    assertThrows(IllegalArgumentException.class, () -> useCase.deleteWallet(walletId, "   ", correlationId));
  }

  /**
   * Test scenario: Wallet not found by ID.
   * Expected outcome: IllegalArgumentException is thrown.
   */
  @Test
  @DisplayName("deleteWallet throws exception when wallet is not found")
  void deleteWallet_throwsException_whenWalletNotFound() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    String reason = "Invalid Wallet";
    String correlationId = UUID.randomUUID().toString();

    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    DeleteWalletUseCase useCase = new DeleteWalletUseCase(walletRepository, eventPublisher);

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.deleteWallet(walletId, reason, correlationId));
  }

  /**
   * Test scenario: Wallet is already deleted.
   * Expected outcome: IllegalStateException is thrown.
   */
  @Test
  @DisplayName("deleteWallet throws exception when wallet is already deleted")
  void deleteWallet_throwsException_whenWalletAlreadyDeleted() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    String reason = "Already Deleted";
    String correlationId = UUID.randomUUID().toString();

    Wallet wallet = mock(Wallet.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(wallet.isDeleted()).thenReturn(true);

    DeleteWalletUseCase useCase = new DeleteWalletUseCase(walletRepository, eventPublisher);

    // Act & Assert
    assertThrows(IllegalStateException.class, () -> useCase.deleteWallet(walletId, reason, correlationId));
  }

  /**
   * Test scenario: Wallet cannot be deleted because the balance is not zero.
   * Expected outcome: IllegalStateException is thrown.
   */
  @Test
  @DisplayName("deleteWallet throws exception when wallet balance is not zero")
  void deleteWallet_throwsException_whenWalletBalanceIsNotZero() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    String reason = "Non-Zero Balance";
    String correlationId = UUID.randomUUID().toString();

    Wallet wallet = mock(Wallet.class);
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(wallet.isDeleted()).thenReturn(false);
    when(wallet.getBalance()).thenReturn(BigDecimal.TEN);

    DeleteWalletUseCase useCase = new DeleteWalletUseCase(walletRepository, eventPublisher);

    // Act & Assert
    assertThrows(IllegalStateException.class, () -> useCase.deleteWallet(walletId, reason, correlationId));
  }
}