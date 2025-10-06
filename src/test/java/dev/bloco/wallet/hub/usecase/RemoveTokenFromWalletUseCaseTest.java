// File: RemoveTokenFromWalletUseCaseTest.java

package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.wallet.TokenRemovedFromWalletEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletTokenRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.wallet.WalletToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Remove Token From Wallet Use Case Tests")
class RemoveTokenFromWalletUseCaseTest {

  @Test
  @DisplayName("removeTokenFromWallet successfully removes token and publishes event")
  void removeTokenFromWallet_Successful_Removal() {
    // Initialize mocks
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    // Create use case
    RemoveTokenFromWalletUseCase useCase = new RemoveTokenFromWalletUseCase(
        walletRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    // Test data
    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    UUID walletTokenEntityId = UUID.randomUUID();
    String reason = "User request";
    String correlationId = UUID.randomUUID().toString();

    Wallet mockWallet = mock(Wallet.class);
    WalletToken mockToken = mock(WalletToken.class);

    // Mocking behavior
    when(walletRepositoryMock.findById(walletId)).thenReturn(Optional.of(mockWallet));
    doNothing().when(mockWallet).validateOperationAllowed();
    when(walletTokenRepositoryMock.findByWalletIdAndTokenId(walletId, tokenId))
        .thenReturn(Optional.of(mockToken));
    when(mockToken.getId()).thenReturn(walletTokenEntityId);

    doNothing().when(walletTokenRepositoryMock).delete(walletTokenEntityId);
    doNothing().when(eventPublisherMock).publish(any(TokenRemovedFromWalletEvent.class));

    // Execute
    assertDoesNotThrow(() -> useCase.removeTokenFromWallet(walletId, tokenId, reason, correlationId));

    // Verify
    verify(walletRepositoryMock).findById(walletId);
    verify(walletTokenRepositoryMock).findByWalletIdAndTokenId(walletId, tokenId);
    verify(walletTokenRepositoryMock).delete(mockToken.getId());
    verify(eventPublisherMock).publish(any(TokenRemovedFromWalletEvent.class));
  }

  @Test
  @DisplayName("removeTokenFromWallet throws exception when wallet ID is null")
  void removeTokenFromWallet_ThrowsException_WhenWalletIdIsNull() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    RemoveTokenFromWalletUseCase useCase = new RemoveTokenFromWalletUseCase(
        walletRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID tokenId = UUID.randomUUID();
    String reason = "User request";
    String correlationId = UUID.randomUUID().toString();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.removeTokenFromWallet(null, tokenId, reason, correlationId));

    assertEquals("Wallet ID must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("removeTokenFromWallet throws exception when token ID is null")
  void removeTokenFromWallet_ThrowsException_WhenTokenIdIsNull() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    RemoveTokenFromWalletUseCase useCase = new RemoveTokenFromWalletUseCase(
        walletRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    String reason = "User request";
    String correlationId = UUID.randomUUID().toString();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.removeTokenFromWallet(walletId, null, reason, correlationId));

    assertEquals("Token ID must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("removeTokenFromWallet throws exception when reason is empty or null")
  void removeTokenFromWallet_ThrowsException_WhenReasonIsEmpty() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    RemoveTokenFromWalletUseCase useCase = new RemoveTokenFromWalletUseCase(
        walletRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.removeTokenFromWallet(walletId, tokenId, "   ", correlationId));

    assertEquals("Reason for removal must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("removeTokenFromWallet throws exception when wallet does not exist")
  void removeTokenFromWallet_ThrowsException_WhenWalletNotFound() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    RemoveTokenFromWalletUseCase useCase = new RemoveTokenFromWalletUseCase(
        walletRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    String reason = "User request";
    String correlationId = UUID.randomUUID().toString();

    when(walletRepositoryMock.findById(walletId)).thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.removeTokenFromWallet(walletId, tokenId, reason, correlationId));

    assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
  }

  @Test
  @DisplayName("removeTokenFromWallet throws exception when token is not found in wallet")
  void removeTokenFromWallet_ThrowsException_WhenTokenNotFoundInWallet() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    RemoveTokenFromWalletUseCase useCase = new RemoveTokenFromWalletUseCase(
        walletRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    String reason = "User request";
    String correlationId = UUID.randomUUID().toString();

    Wallet mockWallet = mock(Wallet.class);

    when(walletRepositoryMock.findById(walletId)).thenReturn(Optional.of(mockWallet));
    doNothing().when(mockWallet).validateOperationAllowed();
    when(walletTokenRepositoryMock.findByWalletIdAndTokenId(walletId, tokenId)).thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.removeTokenFromWallet(walletId, tokenId, reason, correlationId));

    assertEquals("Token is not added to this wallet", exception.getMessage());
  }
}