package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.wallet.TokenAddedToWalletEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.TokenRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletTokenRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.domain.model.wallet.WalletToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Add Token To Wallet Use Case Tests")
class AddTokenToWalletUseCaseTest {

  @Test
  @DisplayName("addTokenToWallet adds token to wallet and publishes event")
  void addTokenToWallet_Successful_Addition() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    TokenRepository tokenRepositoryMock = mock(TokenRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    AddTokenToWalletUseCase useCase = new AddTokenToWalletUseCase(
        walletRepositoryMock, tokenRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    String displayName = "CustomTokenName";
    String correlationId = UUID.randomUUID().toString();

    Wallet mockWallet = mock(Wallet.class);
    when(walletRepositoryMock.findById(walletId)).thenReturn(Optional.of(mockWallet));
    doNothing().when(mockWallet).validateOperationAllowed();

    Token mockToken = mock(Token.class);
    when(tokenRepositoryMock.findById(tokenId)).thenReturn(Optional.of(mockToken));
    when(walletTokenRepositoryMock.existsByWalletIdAndTokenId(walletId, tokenId)).thenReturn(false);

    WalletToken expectedWalletToken = WalletToken.create(UUID.randomUUID(), walletId, tokenId, displayName);
    when(walletTokenRepositoryMock.save(Mockito.any(WalletToken.class))).thenReturn(expectedWalletToken);

    WalletToken result = useCase.addTokenToWallet(walletId, tokenId, displayName, correlationId);

    assertNotNull(result);
    assertEquals(walletId, result.getWalletId());
    assertEquals(tokenId, result.getTokenId());
    assertEquals(displayName, result.getDisplayName());

    verify(walletRepositoryMock).findById(walletId);
    verify(tokenRepositoryMock).findById(tokenId);
    verify(walletTokenRepositoryMock).existsByWalletIdAndTokenId(walletId, tokenId);
    verify(walletTokenRepositoryMock).save(Mockito.any(WalletToken.class));
    verify(eventPublisherMock).publish(Mockito.any(TokenAddedToWalletEvent.class));
  }

  @Test
  @DisplayName("addTokenToWallet throws exception when wallet is not active")
  void addTokenToWallet_ThrowsException_WhenWalletIdIsNull() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    TokenRepository tokenRepositoryMock = mock(TokenRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    AddTokenToWalletUseCase useCase = new AddTokenToWalletUseCase(
        walletRepositoryMock, tokenRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID tokenId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.addTokenToWallet(null, tokenId, null, correlationId));

    assertEquals("Wallet ID must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("addTokenToWallet throws exception when token is not active")
  void addTokenToWallet_ThrowsException_WhenTokenIdIsNull() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    TokenRepository tokenRepositoryMock = mock(TokenRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    AddTokenToWalletUseCase useCase = new AddTokenToWalletUseCase(
        walletRepositoryMock, tokenRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.addTokenToWallet(walletId, null, null, correlationId));

    assertEquals("Token ID must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("addTokenToWallet throws exception when wallet is not active")
  void addTokenToWallet_ThrowsException_WhenWalletNotFound() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    TokenRepository tokenRepositoryMock = mock(TokenRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    AddTokenToWalletUseCase useCase = new AddTokenToWalletUseCase(
        walletRepositoryMock, tokenRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    when(walletRepositoryMock.findById(walletId)).thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.addTokenToWallet(walletId, tokenId, null, correlationId));

    assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
  }

  @Test
  @DisplayName("addTokenToWallet throws exception when token is not active")
  void addTokenToWallet_ThrowsException_WhenTokenNotFound() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    TokenRepository tokenRepositoryMock = mock(TokenRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    AddTokenToWalletUseCase useCase = new AddTokenToWalletUseCase(
        walletRepositoryMock, tokenRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    Wallet mockWallet = mock(Wallet.class);
    when(walletRepositoryMock.findById(walletId)).thenReturn(Optional.of(mockWallet));
    doNothing().when(mockWallet).validateOperationAllowed();
    when(tokenRepositoryMock.findById(tokenId)).thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.addTokenToWallet(walletId, tokenId, null, correlationId));

    assertEquals("Token not found with id: " + tokenId, exception.getMessage());
  }

  @Test
  @DisplayName("addTokenToWallet throws exception when token is already added to wallet")
  void addTokenToWallet_ThrowsException_WhenTokenAlreadyExistsInWallet() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    TokenRepository tokenRepositoryMock = mock(TokenRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    AddTokenToWalletUseCase useCase = new AddTokenToWalletUseCase(
        walletRepositoryMock, tokenRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    Wallet mockWallet = mock(Wallet.class);
    when(walletRepositoryMock.findById(walletId)).thenReturn(Optional.of(mockWallet));
    doNothing().when(mockWallet).validateOperationAllowed();

    Token mockToken = mock(Token.class);
    when(tokenRepositoryMock.findById(tokenId)).thenReturn(Optional.of(mockToken));
    when(walletTokenRepositoryMock.existsByWalletIdAndTokenId(walletId, tokenId)).thenReturn(true);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.addTokenToWallet(walletId, tokenId, null, correlationId));

    assertEquals("Token is already added to this wallet", exception.getMessage());
  }
}