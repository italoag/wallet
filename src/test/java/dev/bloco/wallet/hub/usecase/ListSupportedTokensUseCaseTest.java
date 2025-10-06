// File: ListSupportedTokensUseCaseTest.java

package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenRepository;
import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.domain.model.token.TokenType;
import dev.bloco.wallet.hub.usecase.ListSupportedTokensUseCase.TokenListingResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("List Supported Tokens Use Case Tests")
class ListSupportedTokensUseCaseTest {

  @Test
  @DisplayName("getTokenListing returns all tokens when no network or token type provided")
  void shouldReturnAllTokensWhenNoNetworkOrTokenTypeProvided() {
    TokenRepository tokenRepository = mock(TokenRepository.class);
    when(tokenRepository.findAll()).thenReturn(List.of(mock(Token.class), mock(Token.class)));

    ListSupportedTokensUseCase useCase = new ListSupportedTokensUseCase(tokenRepository, mock(NetworkRepository.class));

    TokenListingResult result = useCase.getTokenListing(null, null, null);

    assertEquals(2, result.totalCount());
    verify(tokenRepository, times(1)).findAll();
  }

  @Test
  @DisplayName("getTokenListing returns tokens for specific network when no token type provided")
  void shouldReturnTokensForSpecificNetwork() {
    UUID networkId = UUID.randomUUID();
    TokenRepository tokenRepository = mock(TokenRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);

    when(networkRepository.findById(eq(networkId), anyString()))
        .thenReturn(java.util.Optional.of(mock(dev.bloco.wallet.hub.domain.model.network.Network.class)));
    when(tokenRepository.findByNetworkId(networkId)).thenReturn(List.of(mock(Token.class), mock(Token.class)));

    ListSupportedTokensUseCase useCase = new ListSupportedTokensUseCase(tokenRepository, networkRepository);

    TokenListingResult result = useCase.getTokenListing(networkId, null, UUID.randomUUID().toString());

    assertEquals(2, result.totalCount());
    verify(tokenRepository, times(1)).findByNetworkId(networkId);
  }

  @Test
  @DisplayName("getTokenListing returns tokens for specific token type when no network provided")
  void shouldReturnTokensForSpecificTokenType() {
    TokenType tokenType = TokenType.ERC20;
    TokenRepository tokenRepository = mock(TokenRepository.class);

    when(tokenRepository.findByType(tokenType)).thenReturn(List.of(mock(Token.class), mock(Token.class), mock(Token.class)));

    ListSupportedTokensUseCase useCase = new ListSupportedTokensUseCase(tokenRepository, mock(NetworkRepository.class));

    TokenListingResult result = useCase.getTokenListing(null, tokenType, null);

    assertEquals(3, result.totalCount());
    verify(tokenRepository, times(1)).findByType(tokenType);
  }

  @Test
  @DisplayName("getTokenListing returns tokens for specific network and token type")
  void shouldReturnTokensForSpecificNetworkAndType() {
    UUID networkId = UUID.randomUUID();
    TokenType tokenType = TokenType.NATIVE;
    Token token = mock(Token.class);
    when(token.getType()).thenReturn(TokenType.NATIVE);

    TokenRepository tokenRepository = mock(TokenRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);

    when(networkRepository.findById(eq(networkId), anyString()))
        .thenReturn(java.util.Optional.of(mock(dev.bloco.wallet.hub.domain.model.network.Network.class)));
    when(tokenRepository.findByNetworkId(networkId)).thenReturn(List.of(token, mock(Token.class)));

    ListSupportedTokensUseCase useCase = new ListSupportedTokensUseCase(tokenRepository, networkRepository);

    TokenListingResult result = useCase.getTokenListing(networkId, tokenType, UUID.randomUUID().toString());

    assertEquals(1, result.nativeTokens());
    assertEquals(1, result.totalCount());
    verify(tokenRepository, times(1)).findByNetworkId(networkId);
  }

  @Test
  @DisplayName("getTokenListing throws exception when network and token type are both provided")
  void shouldThrowExceptionWhenNetworkNotFound() {
    UUID networkId = UUID.randomUUID();
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);

    when(networkRepository.findById(eq(networkId), anyString())).thenReturn(java.util.Optional.empty());

    ListSupportedTokensUseCase useCase = new ListSupportedTokensUseCase(tokenRepository, networkRepository);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> useCase.getTokenListing(networkId, null, UUID.randomUUID().toString())
    );

    assertTrue(exception.getMessage().contains("Network not found with id: " + networkId));
    verify(networkRepository, times(1)).findById(eq(networkId), anyString());
  }

  @Test
  @DisplayName("getTokenListing throws exception when invalid network id provided")
  void shouldThrowExceptionWhenInvalidCorrelationId() {
    UUID networkId = UUID.randomUUID();
    TokenRepository tokenRepository = mock(TokenRepository.class);
    NetworkRepository networkRepository = mock(NetworkRepository.class);

    ListSupportedTokensUseCase useCase = new ListSupportedTokensUseCase(tokenRepository, networkRepository);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> useCase.getTokenListing(networkId, null, "invalid-id")
    );

    assertTrue(exception.getMessage().contains("Correlation ID must be a valid UUID"));
  }

  @Test
  @DisplayName("getTokenListing returns empty token list when no tokens found")
  void shouldReturnEmptyTokenListIfRepositoryReturnsNothing() {
    TokenRepository tokenRepository = mock(TokenRepository.class);
    when(tokenRepository.findAll()).thenReturn(List.of());

    ListSupportedTokensUseCase useCase = new ListSupportedTokensUseCase(tokenRepository, mock(NetworkRepository.class));

    TokenListingResult result = useCase.getTokenListing(null, null, null);

    assertEquals(0, result.totalCount());
    assertTrue(result.tokens().isEmpty());
  }
}