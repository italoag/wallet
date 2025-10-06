package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.TokenBalanceRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.domain.model.token.TokenBalance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("Get Token Balance Use Case Tests")
class GetTokenBalanceUseCaseTest {

  @Test
  @DisplayName("getWalletTokenBalance returns wallet token balance")
  void shouldReturnZeroBalanceWhenWalletHasNoAddresses() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, "Test Wallet", "Test Description");

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(mock(Token.class)));

    // Act
    BigDecimal balance = useCase.getWalletTokenBalance(walletId, tokenId);

    // Assert
    assertEquals(BigDecimal.ZERO, balance);
  }

  @Test
  @DisplayName("getWalletTokenBalance returns wallet token balance")
  void shouldThrowExceptionWhenWalletNotFound() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.getWalletTokenBalance(walletId, tokenId));
  }

  @Test
  void shouldThrowExceptionWhenTokenNotFound() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, "Test Wallet", "Test Description");
    wallet.addAddress(UUID.randomUUID());

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(tokenRepository.findById(tokenId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.getWalletTokenBalance(walletId, tokenId));
  }

  @Test
  @DisplayName("getWalletTokenBalance returns wallet token balance")
  void shouldCorrectlyCalculateTotalBalanceAcrossAddresses() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    UUID addressId1 = UUID.randomUUID();
    UUID addressId2 = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, "Test Wallet", "Test Description");
    wallet.addAddress(addressId1);
    wallet.addAddress(addressId2);

    TokenBalance balance1 = TokenBalance.create(UUID.randomUUID(), addressId1, tokenId, BigDecimal.valueOf(50));
    TokenBalance balance2 = TokenBalance.create(UUID.randomUUID(), addressId2, tokenId, BigDecimal.valueOf(70));

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(mock(Token.class)));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId1, tokenId)).thenReturn(Optional.of(balance1));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId2, tokenId)).thenReturn(Optional.of(balance2));

    // Act
    BigDecimal balance = useCase.getWalletTokenBalance(walletId, tokenId);

    // Assert
    assertEquals(BigDecimal.valueOf(120), balance);
  }

  @Test
  @DisplayName("getWalletTokenBalance returns wallet token balance")
  void shouldReturnZeroWhenNoBalanceExistsForAddresses() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    UUID addressId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, "Test Wallet", "Test Description");
    wallet.addAddress(addressId);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(mock(Token.class)));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(any(), any())).thenReturn(Optional.empty());

    // Act
    BigDecimal balance = useCase.getWalletTokenBalance(walletId, tokenId);

    // Assert
    assertEquals(BigDecimal.ZERO, balance);
  }

  @Test
  @DisplayName("getWalletTokenBalance throws exception when walletId is null")
  void shouldThrowExceptionWhenWalletIdIsNull() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID tokenId = UUID.randomUUID();

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.getWalletTokenBalance(null, tokenId));
  }

  @Test
  @DisplayName("getWalletTokenBalance throws exception when tokenId is null")
  void shouldThrowExceptionWhenTokenIdIsNull() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.getWalletTokenBalance(walletId, null));
  }
}