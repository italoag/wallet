package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.wallet.WalletStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ActivateWalletUseCase class.
 * This test class is designed to verify the correct behavior of the activateWallet method,
 * ensuring compliance with the described business rules and constraints.
 */
@DisplayName("Activate Wallet Use Case Tests")
class ActivateWalletUseCaseTest {

  @Test
  @DisplayName("activateWallet updates wallet status and publishes events")
  void shouldActivateWalletSuccessfully() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();
    Wallet wallet = new Wallet(walletId, "Test Wallet", "Description");

    WalletRepository walletRepository = mock(WalletRepository.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    ActivateWalletUseCase useCase = new ActivateWalletUseCase(walletRepository, eventPublisher);

    // Act
    Wallet activatedWallet = useCase.activateWallet(walletId, correlationId);

    // Assert
    assertNotNull(activatedWallet);
    assertEquals(walletId, activatedWallet.getId());
    assertEquals(WalletStatus.ACTIVE, activatedWallet.getStatus());
    verify(walletRepository, times(1)).update(wallet);
    verify(eventPublisher, times(activatedWallet.getDomainEvents().size())).publish(any());
  }

  @Test
  @DisplayName("activateWallet throws exception when wallet not found")
  void shouldThrowExceptionWhenWalletNotFound() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    WalletRepository walletRepository = mock(WalletRepository.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    ActivateWalletUseCase useCase = new ActivateWalletUseCase(walletRepository, eventPublisher);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.activateWallet(walletId, correlationId));

    assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
    verify(walletRepository, never()).update(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  @DisplayName("activateWallet throws exception when wallet is deleted")
  void shouldThrowExceptionWhenWalletIsDeleted() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();
    Wallet wallet = new Wallet(walletId, "Test Wallet", "Description");
    wallet.delete("Deleted wallet");

    WalletRepository walletRepository = mock(WalletRepository.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    ActivateWalletUseCase useCase = new ActivateWalletUseCase(walletRepository, eventPublisher);

    // Act & Assert
    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> useCase.activateWallet(walletId, correlationId));

    assertEquals("Deleted wallets cannot be activated", exception.getMessage());
    verify(walletRepository, never()).update(any());
    verify(eventPublisher, never()).publish(any());
  }
}