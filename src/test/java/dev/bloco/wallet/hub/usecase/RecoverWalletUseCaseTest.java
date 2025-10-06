// src/test/java/dev/bloco/wallet/hub/usecase/RecoverWalletUseCaseTest.java
package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Recover Wallet Use Case Tests")
class RecoverWalletUseCaseTest {

  @Test
  @DisplayName("recoverWallet creates wallet and publishes events")
  void shouldRecoverWalletSuccessfully() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    RecoverWalletUseCase useCase = new RecoverWalletUseCase(walletRepository, eventPublisher);

    UUID userId = UUID.randomUUID();
    String walletName = "Test Wallet";
    String recoveryMethod = "email";
    String correlationId = UUID.randomUUID().toString();

    // Act
    Wallet result = useCase.recoverWallet(userId, walletName, recoveryMethod, correlationId);

    // Assert
    assertNotNull(result);
    assertEquals(walletName, result.getName());
    assertEquals(recoveryMethod, result.getDescription());
    assertEquals(userId, result.getUserId());
    assertEquals(UUID.fromString(correlationId), result.getCorrelationId());

    verify(walletRepository).save(result);
    verify(eventPublisher, times(3)).publish(any());
    assertTrue(result.getDomainEvents().isEmpty());
  }

  @Test
  @DisplayName("recoverWallet throws exception when wallet already exists")
  void shouldThrowExceptionWhenUserIdIsNull() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    RecoverWalletUseCase useCase = new RecoverWalletUseCase(walletRepository, eventPublisher);

    String walletName = "Test Wallet";
    String recoveryMethod = "email";
    String correlationId = UUID.randomUUID().toString();

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.recoverWallet(null, walletName, recoveryMethod, correlationId));
    assertEquals("User ID must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("recoverWallet throws exception when wallet already exists")
  void shouldThrowExceptionWhenWalletNameIsNull() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    RecoverWalletUseCase useCase = new RecoverWalletUseCase(walletRepository, eventPublisher);

    UUID userId = UUID.randomUUID();
    String recoveryMethod = "email";
    String correlationId = UUID.randomUUID().toString();

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.recoverWallet(userId, null, recoveryMethod, correlationId));
    assertEquals("Wallet name must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("recoverWallet throws exception when wallet already exists")
  void shouldThrowExceptionWhenWalletNameIsEmpty() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    RecoverWalletUseCase useCase = new RecoverWalletUseCase(walletRepository, eventPublisher);

    UUID userId = UUID.randomUUID();
    String walletName = " ";
    String recoveryMethod = "email";
    String correlationId = UUID.randomUUID().toString();

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.recoverWallet(userId, walletName, recoveryMethod, correlationId));
    assertEquals("Wallet name must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("recoverWallet throws exception when wallet already exists")
  void shouldThrowExceptionWhenRecoveryMethodIsNull() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    RecoverWalletUseCase useCase = new RecoverWalletUseCase(walletRepository, eventPublisher);

    UUID userId = UUID.randomUUID();
    String walletName = "Test Wallet";
    String correlationId = UUID.randomUUID().toString();

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.recoverWallet(userId, walletName, null, correlationId));
    assertEquals("Recovery method must be specified", exception.getMessage());
  }

  @Test
  @DisplayName("recoverWallet throws exception when wallet already exists")
  void shouldThrowExceptionWhenRecoveryMethodIsEmpty() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    RecoverWalletUseCase useCase = new RecoverWalletUseCase(walletRepository, eventPublisher);

    UUID userId = UUID.randomUUID();
    String walletName = "Test Wallet";
    String recoveryMethod = " ";
    String correlationId = UUID.randomUUID().toString();

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.recoverWallet(userId, walletName, recoveryMethod, correlationId));
    assertEquals("Recovery method must be specified", exception.getMessage());
  }

  @Test
  @DisplayName("recoverWallet publishes events and clears after recovery")
  void shouldPublishEventsAndClearAfterRecovery() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    RecoverWalletUseCase useCase = new RecoverWalletUseCase(walletRepository, eventPublisher);

    UUID userId = UUID.randomUUID();
    String walletName = "Test Wallet";
    String recoveryMethod = "email";
    String correlationId = UUID.randomUUID().toString();

    ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);

    // Act
    Wallet result = useCase.recoverWallet(userId, walletName, recoveryMethod, correlationId);

    // Assert
    verify(walletRepository).save(walletCaptor.capture());
    verify(eventPublisher, times(3)).publish(any());
    assertTrue(walletCaptor.getValue().getDomainEvents().isEmpty());
    assertTrue(result.getDomainEvents().isEmpty());
  }
}