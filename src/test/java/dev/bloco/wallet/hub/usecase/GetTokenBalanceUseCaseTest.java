package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.TokenBalanceRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.domain.model.token.TokenBalance;
import dev.bloco.wallet.hub.domain.model.token.TokenBalanceDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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

  // ========== Tests for getTokenBalanceDetails method ==========

  @Test
  @DisplayName("getTokenBalanceDetails throws exception when walletId is null")
  void getTokenBalanceDetails_shouldThrowExceptionWhenWalletIdIsNull() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID tokenId = UUID.randomUUID();

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> useCase.getTokenBalanceDetails(null, tokenId)
    );
    assertEquals("Wallet ID must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("getTokenBalanceDetails throws exception when tokenId is null")
  void getTokenBalanceDetails_shouldThrowExceptionWhenTokenIdIsNull() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> useCase.getTokenBalanceDetails(walletId, null)
    );
    assertEquals("Token ID must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("getTokenBalanceDetails throws exception when wallet not found")
  void getTokenBalanceDetails_shouldThrowExceptionWhenWalletNotFound() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> useCase.getTokenBalanceDetails(walletId, tokenId)
    );
    assertTrue(exception.getMessage().contains("Wallet not found"));
  }

  @Test
  @DisplayName("getTokenBalanceDetails throws exception when token not found")
  void getTokenBalanceDetails_shouldThrowExceptionWhenTokenNotFound() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, "Test Wallet", "Test Description");

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(tokenRepository.findById(tokenId)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> useCase.getTokenBalanceDetails(walletId, tokenId)
    );
    assertTrue(exception.getMessage().contains("Token not found"));
  }

  @Test
  @DisplayName("getTokenBalanceDetails returns correct details for empty wallet")
  void getTokenBalanceDetails_shouldReturnCorrectDetailsForEmptyWallet() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, "Test Wallet", "Test Description");
    
    Token token = mock(Token.class);
    when(token.getSymbol()).thenReturn("ETH");
    when(token.getName()).thenReturn("Ethereum");
    when(token.getDecimals()).thenReturn(18);
    when(token.formatAmount(BigDecimal.ZERO)).thenReturn("0.000000000000000000");

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

    // Act
    TokenBalanceDetails details = useCase.getTokenBalanceDetails(walletId, tokenId);

    // Assert
    assertNotNull(details);
    assertEquals(walletId, details.walletId());
    assertEquals(tokenId, details.tokenId());
    assertEquals("ETH", details.tokenSymbol());
    assertEquals("Ethereum", details.tokenName());
    assertEquals(BigDecimal.ZERO, details.rawBalance());
    assertEquals("0.000000000000000000", details.formattedBalance());
    assertEquals(18, details.decimals());
    assertEquals(0, details.addressesWithBalance());
    assertEquals(0, details.totalAddresses());
  }

  @Test
  @DisplayName("getTokenBalanceDetails returns correct details when wallet has addresses but no balances")
  void getTokenBalanceDetails_shouldReturnZeroBalanceWhenAddressesHaveNoBalance() {
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
    
    Token token = mock(Token.class);
    when(token.getSymbol()).thenReturn("USDT");
    when(token.getName()).thenReturn("Tether");
    when(token.getDecimals()).thenReturn(6);
    when(token.formatAmount(BigDecimal.ZERO)).thenReturn("0.000000");

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId, tokenId)).thenReturn(Optional.empty());

    // Act
    TokenBalanceDetails details = useCase.getTokenBalanceDetails(walletId, tokenId);

    // Assert
    assertNotNull(details);
    assertEquals(walletId, details.walletId());
    assertEquals(tokenId, details.tokenId());
    assertEquals("USDT", details.tokenSymbol());
    assertEquals("Tether", details.tokenName());
    assertEquals(BigDecimal.ZERO, details.rawBalance());
    assertEquals("0.000000", details.formattedBalance());
    assertEquals(6, details.decimals());
    assertEquals(0, details.addressesWithBalance());
    assertEquals(1, details.totalAddresses());
  }

  @Test
  @DisplayName("getTokenBalanceDetails correctly aggregates balances across multiple addresses")
  void getTokenBalanceDetails_shouldAggregateBalancesAcrossMultipleAddresses() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    UUID addressId1 = UUID.randomUUID();
    UUID addressId2 = UUID.randomUUID();
    UUID addressId3 = UUID.randomUUID();
    
    Wallet wallet = new Wallet(walletId, "Multi-Address Wallet", "Test Description");
    wallet.addAddress(addressId1);
    wallet.addAddress(addressId2);
    wallet.addAddress(addressId3);

    BigDecimal balance1 = new BigDecimal("1000000000000000000"); // 1.0 ETH
    BigDecimal balance2 = new BigDecimal("2500000000000000000"); // 2.5 ETH
    BigDecimal balance3 = new BigDecimal("500000000000000000");  // 0.5 ETH
    BigDecimal totalBalance = balance1.add(balance2).add(balance3); // 4.0 ETH

    TokenBalance tokenBalance1 = TokenBalance.create(UUID.randomUUID(), addressId1, tokenId, balance1);
    TokenBalance tokenBalance2 = TokenBalance.create(UUID.randomUUID(), addressId2, tokenId, balance2);
    TokenBalance tokenBalance3 = TokenBalance.create(UUID.randomUUID(), addressId3, tokenId, balance3);
    
    Token token = mock(Token.class);
    when(token.getSymbol()).thenReturn("ETH");
    when(token.getName()).thenReturn("Ethereum");
    when(token.getDecimals()).thenReturn(18);
    when(token.formatAmount(totalBalance)).thenReturn("4.000000000000000000");

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId1, tokenId)).thenReturn(Optional.of(tokenBalance1));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId2, tokenId)).thenReturn(Optional.of(tokenBalance2));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId3, tokenId)).thenReturn(Optional.of(tokenBalance3));

    // Act
    TokenBalanceDetails details = useCase.getTokenBalanceDetails(walletId, tokenId);

    // Assert
    assertNotNull(details);
    assertEquals(walletId, details.walletId());
    assertEquals(tokenId, details.tokenId());
    assertEquals("ETH", details.tokenSymbol());
    assertEquals("Ethereum", details.tokenName());
    assertEquals(totalBalance, details.rawBalance());
    assertEquals("4.000000000000000000", details.formattedBalance());
    assertEquals(18, details.decimals());
    assertEquals(3, details.addressesWithBalance());
    assertEquals(3, details.totalAddresses());
  }

  @Test
  @DisplayName("getTokenBalanceDetails correctly counts only addresses with positive balance")
  void getTokenBalanceDetails_shouldCountOnlyAddressesWithPositiveBalance() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    UUID addressId1 = UUID.randomUUID();
    UUID addressId2 = UUID.randomUUID();
    UUID addressId3 = UUID.randomUUID();
    UUID addressId4 = UUID.randomUUID();
    
    Wallet wallet = new Wallet(walletId, "Mixed Balance Wallet", "Test Description");
    wallet.addAddress(addressId1);
    wallet.addAddress(addressId2);
    wallet.addAddress(addressId3);
    wallet.addAddress(addressId4);

    BigDecimal balance1 = new BigDecimal("1000000"); // 1.0 USDT
    BigDecimal balance2 = BigDecimal.ZERO;           // 0 USDT
    BigDecimal balance3 = new BigDecimal("500000");  // 0.5 USDT
    // addressId4 has no balance record (Optional.empty())
    
    BigDecimal totalBalance = balance1.add(balance3); // 1.5 USDT

    TokenBalance tokenBalance1 = TokenBalance.create(UUID.randomUUID(), addressId1, tokenId, balance1);
    TokenBalance tokenBalance2 = TokenBalance.create(UUID.randomUUID(), addressId2, tokenId, balance2);
    TokenBalance tokenBalance3 = TokenBalance.create(UUID.randomUUID(), addressId3, tokenId, balance3);
    
    Token token = mock(Token.class);
    when(token.getSymbol()).thenReturn("USDT");
    when(token.getName()).thenReturn("Tether");
    when(token.getDecimals()).thenReturn(6);
    when(token.formatAmount(totalBalance)).thenReturn("1.500000");

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId1, tokenId)).thenReturn(Optional.of(tokenBalance1));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId2, tokenId)).thenReturn(Optional.of(tokenBalance2));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId3, tokenId)).thenReturn(Optional.of(tokenBalance3));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId4, tokenId)).thenReturn(Optional.empty());

    // Act
    TokenBalanceDetails details = useCase.getTokenBalanceDetails(walletId, tokenId);

    // Assert
    assertNotNull(details);
    assertEquals(walletId, details.walletId());
    assertEquals(tokenId, details.tokenId());
    assertEquals("USDT", details.tokenSymbol());
    assertEquals("Tether", details.tokenName());
    assertEquals(totalBalance, details.rawBalance());
    assertEquals("1.500000", details.formattedBalance());
    assertEquals(6, details.decimals());
    assertEquals(2, details.addressesWithBalance()); // Only addressId1 and addressId3 have positive balance
    assertEquals(4, details.totalAddresses());
  }

  @Test
  @DisplayName("getTokenBalanceDetails returns all required fields populated")
  void getTokenBalanceDetails_shouldPopulateAllFields() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetTokenBalanceUseCase useCase = new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);

    UUID walletId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    UUID addressId = UUID.randomUUID();
    
    Wallet wallet = new Wallet(walletId, "Complete Test Wallet", "Description");
    wallet.addAddress(addressId);

    BigDecimal rawBalance = new BigDecimal("123456789");
    TokenBalance tokenBalance = TokenBalance.create(UUID.randomUUID(), addressId, tokenId, rawBalance);
    
    Token token = mock(Token.class);
    when(token.getSymbol()).thenReturn("BTC");
    when(token.getName()).thenReturn("Bitcoin");
    when(token.getDecimals()).thenReturn(8);
    when(token.formatAmount(rawBalance)).thenReturn("1.23456789");

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId, tokenId)).thenReturn(Optional.of(tokenBalance));

    // Act
    TokenBalanceDetails details = useCase.getTokenBalanceDetails(walletId, tokenId);

    // Assert - Verify all 9 fields are present and correct
    assertNotNull(details);
    assertNotNull(details.walletId());
    assertNotNull(details.tokenId());
    assertNotNull(details.tokenSymbol());
    assertNotNull(details.tokenName());
    assertNotNull(details.rawBalance());
    assertNotNull(details.formattedBalance());
    assertTrue(details.decimals() >= 0);
    assertTrue(details.addressesWithBalance() >= 0);
    assertTrue(details.totalAddresses() >= 0);
    
    assertEquals(walletId, details.walletId());
    assertEquals(tokenId, details.tokenId());
    assertEquals("BTC", details.tokenSymbol());
    assertEquals("Bitcoin", details.tokenName());
    assertEquals(rawBalance, details.rawBalance());
    assertEquals("1.23456789", details.formattedBalance());
    assertEquals(8, details.decimals());
    assertEquals(1, details.addressesWithBalance());
    assertEquals(1, details.totalAddresses());
  }
}