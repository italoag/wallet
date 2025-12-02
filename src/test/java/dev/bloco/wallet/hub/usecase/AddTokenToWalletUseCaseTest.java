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

  @Test
  @DisplayName("addMultipleTokens successfully adds all tokens when all are valid")
  void addMultipleTokens_SuccessfullyAddsAllTokens() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    TokenRepository tokenRepositoryMock = mock(TokenRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    AddTokenToWalletUseCase useCase = new AddTokenToWalletUseCase(
        walletRepositoryMock, tokenRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    UUID tokenId1 = UUID.randomUUID();
    UUID tokenId2 = UUID.randomUUID();
    UUID tokenId3 = UUID.randomUUID();
    UUID[] tokenIds = {tokenId1, tokenId2, tokenId3};
    String correlationId = UUID.randomUUID().toString();

    Wallet mockWallet = mock(Wallet.class);
    when(walletRepositoryMock.findById(walletId)).thenReturn(Optional.of(mockWallet));
    doNothing().when(mockWallet).validateOperationAllowed();

    Token mockToken1 = mock(Token.class);
    Token mockToken2 = mock(Token.class);
    Token mockToken3 = mock(Token.class);
    when(tokenRepositoryMock.findById(tokenId1)).thenReturn(Optional.of(mockToken1));
    when(tokenRepositoryMock.findById(tokenId2)).thenReturn(Optional.of(mockToken2));
    when(tokenRepositoryMock.findById(tokenId3)).thenReturn(Optional.of(mockToken3));

    when(walletTokenRepositoryMock.existsByWalletIdAndTokenId(walletId, tokenId1)).thenReturn(false);
    when(walletTokenRepositoryMock.existsByWalletIdAndTokenId(walletId, tokenId2)).thenReturn(false);
    when(walletTokenRepositoryMock.existsByWalletIdAndTokenId(walletId, tokenId3)).thenReturn(false);

    when(walletTokenRepositoryMock.save(any(WalletToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

    AddTokenToWalletUseCase.BatchAddResult result = useCase.addMultipleTokens(walletId, tokenIds, correlationId);

    assertNotNull(result);
    assertEquals(3, result.successCount());
    assertEquals(0, result.failureCount());
    assertEquals(0, result.errors().length);

    verify(walletRepositoryMock, times(3)).findById(walletId);
    verify(tokenRepositoryMock).findById(tokenId1);
    verify(tokenRepositoryMock).findById(tokenId2);
    verify(tokenRepositoryMock).findById(tokenId3);
    verify(walletTokenRepositoryMock, times(3)).save(any(WalletToken.class));
    verify(eventPublisherMock, times(3)).publish(any(TokenAddedToWalletEvent.class));
  }

  @Test
  @DisplayName("addMultipleTokens handles partial failures correctly")
  void addMultipleTokens_HandlesPartialFailures() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    TokenRepository tokenRepositoryMock = mock(TokenRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    AddTokenToWalletUseCase useCase = new AddTokenToWalletUseCase(
        walletRepositoryMock, tokenRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    UUID tokenId1 = UUID.randomUUID();
    UUID tokenId2 = UUID.randomUUID();
    UUID tokenId3 = UUID.randomUUID();
    UUID[] tokenIds = {tokenId1, tokenId2, tokenId3};
    String correlationId = UUID.randomUUID().toString();

    Wallet mockWallet = mock(Wallet.class);
    when(walletRepositoryMock.findById(walletId)).thenReturn(Optional.of(mockWallet));
    doNothing().when(mockWallet).validateOperationAllowed();

    // First token succeeds
    Token mockToken1 = mock(Token.class);
    when(tokenRepositoryMock.findById(tokenId1)).thenReturn(Optional.of(mockToken1));
    when(walletTokenRepositoryMock.existsByWalletIdAndTokenId(walletId, tokenId1)).thenReturn(false);

    // The second token isn't found (fails)
    when(tokenRepositoryMock.findById(tokenId2)).thenReturn(Optional.empty());

    // The third token already exists (fails)
    Token mockToken3 = mock(Token.class);
    when(tokenRepositoryMock.findById(tokenId3)).thenReturn(Optional.of(mockToken3));
    when(walletTokenRepositoryMock.existsByWalletIdAndTokenId(walletId, tokenId3)).thenReturn(true);

    when(walletTokenRepositoryMock.save(any(WalletToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

    AddTokenToWalletUseCase.BatchAddResult result = useCase.addMultipleTokens(walletId, tokenIds, correlationId);

    assertNotNull(result);
    assertEquals(1, result.successCount());
    assertEquals(2, result.failureCount());
    assertEquals(2, result.errors().length);
    assertTrue(result.errors()[0].contains(tokenId2.toString()));
    assertTrue(result.errors()[0].contains("Token not found"));
    assertTrue(result.errors()[1].contains(tokenId3.toString()));
    assertTrue(result.errors()[1].contains("already added"));

    verify(walletTokenRepositoryMock, times(1)).save(any(WalletToken.class));
    verify(eventPublisherMock, times(1)).publish(any(TokenAddedToWalletEvent.class));
  }

  @Test
  @DisplayName("addMultipleTokens returns empty result when tokenIds is null")
  void addMultipleTokens_ReturnsEmptyResult_WhenTokenIdsIsNull() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    TokenRepository tokenRepositoryMock = mock(TokenRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    AddTokenToWalletUseCase useCase = new AddTokenToWalletUseCase(
        walletRepositoryMock, tokenRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    AddTokenToWalletUseCase.BatchAddResult result = useCase.addMultipleTokens(walletId, null, correlationId);

    assertNotNull(result);
    assertEquals(0, result.successCount());
    assertEquals(0, result.failureCount());
    assertEquals(0, result.errors().length);

    verifyNoInteractions(walletRepositoryMock);
    verifyNoInteractions(tokenRepositoryMock);
    verifyNoInteractions(walletTokenRepositoryMock);
    verifyNoInteractions(eventPublisherMock);
  }

  @Test
  @DisplayName("addMultipleTokens returns empty result when tokenIds is empty array")
  void addMultipleTokens_ReturnsEmptyResult_WhenTokenIdsIsEmpty() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    TokenRepository tokenRepositoryMock = mock(TokenRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    AddTokenToWalletUseCase useCase = new AddTokenToWalletUseCase(
        walletRepositoryMock, tokenRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    UUID[] tokenIds = {};
    String correlationId = UUID.randomUUID().toString();

    AddTokenToWalletUseCase.BatchAddResult result = useCase.addMultipleTokens(walletId, tokenIds, correlationId);

    assertNotNull(result);
    assertEquals(0, result.successCount());
    assertEquals(0, result.failureCount());
    assertEquals(0, result.errors().length);

    verifyNoInteractions(walletRepositoryMock);
    verifyNoInteractions(tokenRepositoryMock);
    verifyNoInteractions(walletTokenRepositoryMock);
    verifyNoInteractions(eventPublisherMock);
  }

  @Test
  @DisplayName("addMultipleTokens continues processing remaining tokens after failures")
  void addMultipleTokens_ContinuesAfterFailures() {
    WalletRepository walletRepositoryMock = mock(WalletRepository.class);
    TokenRepository tokenRepositoryMock = mock(TokenRepository.class);
    WalletTokenRepository walletTokenRepositoryMock = mock(WalletTokenRepository.class);
    DomainEventPublisher eventPublisherMock = mock(DomainEventPublisher.class);

    AddTokenToWalletUseCase useCase = new AddTokenToWalletUseCase(
        walletRepositoryMock, tokenRepositoryMock, walletTokenRepositoryMock, eventPublisherMock);

    UUID walletId = UUID.randomUUID();
    UUID tokenId1 = UUID.randomUUID();
    UUID tokenId2 = UUID.randomUUID();
    UUID[] tokenIds = {tokenId1, tokenId2};
    String correlationId = UUID.randomUUID().toString();

    Wallet mockWallet = mock(Wallet.class);
    when(walletRepositoryMock.findById(walletId)).thenReturn(Optional.of(mockWallet));
    doNothing().when(mockWallet).validateOperationAllowed();

    // First token fails
    when(tokenRepositoryMock.findById(tokenId1)).thenReturn(Optional.empty());

    // Second token succeeds
    Token mockToken2 = mock(Token.class);
    when(tokenRepositoryMock.findById(tokenId2)).thenReturn(Optional.of(mockToken2));
    when(walletTokenRepositoryMock.existsByWalletIdAndTokenId(walletId, tokenId2)).thenReturn(false);
    when(walletTokenRepositoryMock.save(any(WalletToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

    AddTokenToWalletUseCase.BatchAddResult result = useCase.addMultipleTokens(walletId, tokenIds, correlationId);

    assertNotNull(result);
    assertEquals(1, result.successCount());
    assertEquals(1, result.failureCount());
    assertEquals(1, result.errors().length);

    // Verify the second token was processed despite the first failure
    verify(tokenRepositoryMock).findById(tokenId1);
    verify(tokenRepositoryMock).findById(tokenId2);
    verify(walletTokenRepositoryMock, times(1)).save(any(WalletToken.class));
    verify(eventPublisherMock, times(1)).publish(any(TokenAddedToWalletEvent.class));
  }
}

