package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenBalanceRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.address.AccountAddress;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AddressType;
import dev.bloco.wallet.hub.domain.model.address.PublicKey;
import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.domain.model.token.TokenBalance;
import dev.bloco.wallet.hub.domain.model.portfolio.AssetAllocation;
import dev.bloco.wallet.hub.domain.model.portfolio.PortfolioOverview;
import dev.bloco.wallet.hub.domain.model.portfolio.PortfolioSummary;
import dev.bloco.wallet.hub.domain.model.portfolio.TokenHolding;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Get Portfolio Summary Use Case Tests")
class GetPortfolioSummaryUseCaseTest {

  /**
   * Tests that the method handles a null walletId and throws an exception.
   */
  @Test
  @DisplayName("getPortfolioSummary throws exception when walletId is null")
  void test_getPortfolioSummary_withNullWalletId_throwsException() {
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);

    GetPortfolioSummaryUseCase useCase = new GetPortfolioSummaryUseCase(walletRepository, addressRepository, tokenBalanceRepository, tokenRepository);

    IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.getPortfolioSummary(null));
    Assertions.assertEquals("Wallet ID must be provided", exception.getMessage());
  }

  /**
   * Tests that the method throws an exception when the wallet is not found.
   */
  @Test
  @DisplayName("getPortfolioSummary throws exception when wallet is not found")
  void test_getPortfolioSummary_withNonExistentWallet_throwsException() {
    UUID walletId = UUID.randomUUID();
    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    GetPortfolioSummaryUseCase useCase = new GetPortfolioSummaryUseCase(walletRepository, addressRepository, tokenBalanceRepository, tokenRepository);

    IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.getPortfolioSummary(walletId));
    Assertions.assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
  }

  /**
   * Tests the happy path of getting a portfolio summary, with valid balances and token details.
   */
  @Test
  @DisplayName("getPortfolioSummary returns a valid summary")
  void test_getPortfolioSummary_withValidData_returnsSummary() {
    UUID walletId = UUID.randomUUID();
    UUID address1Id = UUID.randomUUID();
    UUID address2Id = UUID.randomUUID();
    UUID token1Id = UUID.randomUUID();
    UUID token2Id = UUID.randomUUID();

    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);

    UUID networkId = UUID.randomUUID();
    
    Wallet wallet = new Wallet(walletId, "Test Wallet", "Description");
    Address address1 = new Address(
        address1Id,
        walletId,
        networkId,
        new PublicKey("pubkey1"),
        new AccountAddress("0xAddress1"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/0"
    );
    Address address2 = new Address(
        address2Id,
        walletId,
        networkId,
        new PublicKey("pubkey2"),
        new AccountAddress("0xAddress2"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/1"
    );

    TokenBalance token1Balance1 = new TokenBalance(UUID.randomUUID(), address1Id, token1Id, new BigDecimal("10"));
    TokenBalance token1Balance2 = new TokenBalance(UUID.randomUUID(), address2Id, token1Id, new BigDecimal("5"));
    TokenBalance token2Balance1 = new TokenBalance(UUID.randomUUID(), address1Id, token2Id, new BigDecimal("2"));

    Token token1 = mock(Token.class);
    when(token1.getName()).thenReturn("Ethereum");
    when(token1.getSymbol()).thenReturn("ETH");
    when(token1.getDecimals()).thenReturn(18);
    when(token1.getType()).thenReturn(dev.bloco.wallet.hub.domain.model.token.TokenType.ERC20);
    when(token1.formatAmount(new BigDecimal("15"))).thenReturn("15.00000");
    when(token1.isNFT()).thenReturn(false);

    Token token2 = mock(Token.class);
    when(token2.getName()).thenReturn("Bitcoin");
    when(token2.getSymbol()).thenReturn("BTC");
    when(token2.getDecimals()).thenReturn(8);
    when(token2.getType()).thenReturn(dev.bloco.wallet.hub.domain.model.token.TokenType.ERC20);
    when(token2.formatAmount(new BigDecimal("2"))).thenReturn("2.00000");
    when(token2.isNFT()).thenReturn(false);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(List.of(address1, address2));
    when(tokenBalanceRepository.findByAddressId(address1Id)).thenReturn(List.of(token1Balance1, token2Balance1));
    when(tokenBalanceRepository.findByAddressId(address2Id)).thenReturn(List.of(token1Balance2));
    when(tokenRepository.findById(token1Id)).thenReturn(Optional.of(token1));
    when(tokenRepository.findById(token2Id)).thenReturn(Optional.of(token2));

    GetPortfolioSummaryUseCase useCase = new GetPortfolioSummaryUseCase(walletRepository, addressRepository, tokenBalanceRepository, tokenRepository);

    PortfolioSummary summary = useCase.getPortfolioSummary(walletId);

    Assertions.assertNotNull(summary);
    Assertions.assertEquals(walletId, summary.walletId());
    Assertions.assertEquals("Test Wallet", summary.walletName());
    Assertions.assertEquals(2, summary.totalTokens()); // Token 1 and Token 2
    Assertions.assertEquals(2, summary.totalAddresses()); // Address 1 and Address 2
    Assertions.assertEquals(new BigDecimal("120000"), summary.totalValue()); // (ETH: 15 * 2000 + BTC: 2 * 45000)

    List<TokenHolding> holdings = summary.holdings();
    Assertions.assertEquals(2, holdings.size());
    TokenHolding ethHolding = holdings.stream().filter(h -> h.tokenId().equals(token1Id)).findFirst().orElse(null);
    Assertions.assertNotNull(ethHolding);
    Assertions.assertEquals("Ethereum", ethHolding.name());
    Assertions.assertEquals("ETH", ethHolding.symbol());
    Assertions.assertEquals(new BigDecimal("15"), ethHolding.rawBalance());
    Assertions.assertEquals("15.00000", ethHolding.formattedBalance());
    Assertions.assertEquals(new BigDecimal("30000"), ethHolding.estimatedValue());

    TokenHolding btcHolding = holdings.stream().filter(h -> h.tokenId().equals(token2Id)).findFirst().orElse(null);
    Assertions.assertNotNull(btcHolding);
    Assertions.assertEquals("Bitcoin", btcHolding.name());
    Assertions.assertEquals("BTC", btcHolding.symbol());
    Assertions.assertEquals(new BigDecimal("2"), btcHolding.rawBalance());
    Assertions.assertEquals("2.00000", btcHolding.formattedBalance());
    Assertions.assertEquals(new BigDecimal("90000"), btcHolding.estimatedValue());

    List<AssetAllocation> allocation = summary.assetAllocation();
    Assertions.assertEquals(2, allocation.size());
    Assertions.assertEquals("BTC", allocation.get(0).symbol()); // Sorted by percentage
    Assertions.assertEquals("ETH", allocation.get(1).symbol());
  }

  /**
   * Tests getPortfolioOverview returns simplified overview with correct data.
   */
  @Test
  @DisplayName("getPortfolioOverview returns a valid overview")
  void test_getPortfolioOverview_withValidData_returnsOverview() {
    UUID walletId = UUID.randomUUID();
    UUID addressId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();

    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);

    UUID networkId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, "Overview Wallet", "Test Description");
    Address address = new Address(
        addressId,
        walletId,
        networkId,
        new PublicKey("pubkey"),
        new AccountAddress("0xAddress"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/0"
    );

    TokenBalance tokenBalance = new TokenBalance(UUID.randomUUID(), addressId, tokenId, new BigDecimal("10"));

    Token token = mock(Token.class);
    when(token.getName()).thenReturn("Ethereum");
    when(token.getSymbol()).thenReturn("ETH");
    when(token.getDecimals()).thenReturn(18);
    when(token.getType()).thenReturn(dev.bloco.wallet.hub.domain.model.token.TokenType.ERC20);
    when(token.formatAmount(new BigDecimal("10"))).thenReturn("10.00000");
    when(token.isNFT()).thenReturn(false);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(List.of(address));
    when(tokenBalanceRepository.findByAddressId(addressId)).thenReturn(List.of(tokenBalance));
    when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

    GetPortfolioSummaryUseCase useCase = new GetPortfolioSummaryUseCase(walletRepository, addressRepository, tokenBalanceRepository, tokenRepository);

    PortfolioOverview overview = useCase.getPortfolioOverview(walletId);

    Assertions.assertNotNull(overview);
    Assertions.assertEquals(walletId, overview.walletId());
    Assertions.assertEquals("Overview Wallet", overview.walletName());
    Assertions.assertEquals(1, overview.totalTokens());
    Assertions.assertEquals(1, overview.totalAddresses());
    Assertions.assertEquals(new BigDecimal("20000"), overview.totalValue()); // ETH: 10 * 2000
    Assertions.assertNotNull(overview.lastUpdated());
  }

  /**
   * Tests getAssetAllocation returns correct allocation breakdown.
   */
  @Test
  @DisplayName("getAssetAllocation returns valid allocation breakdown")
  void test_getAssetAllocation_withValidData_returnsAllocation() {
    UUID walletId = UUID.randomUUID();
    UUID addressId = UUID.randomUUID();
    UUID token1Id = UUID.randomUUID();
    UUID token2Id = UUID.randomUUID();

    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);

    UUID networkId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, "Allocation Wallet", "Test");
    Address address = new Address(
        addressId,
        walletId,
        networkId,
        new PublicKey("pubkey"),
        new AccountAddress("0xAddress"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/0"
    );

    TokenBalance token1Balance = new TokenBalance(UUID.randomUUID(), addressId, token1Id, new BigDecimal("10"));
    TokenBalance token2Balance = new TokenBalance(UUID.randomUUID(), addressId, token2Id, new BigDecimal("1"));

    Token token1 = mock(Token.class);
    when(token1.getName()).thenReturn("Ethereum");
    when(token1.getSymbol()).thenReturn("ETH");
    when(token1.getDecimals()).thenReturn(18);
    when(token1.getType()).thenReturn(dev.bloco.wallet.hub.domain.model.token.TokenType.ERC20);
    when(token1.formatAmount(new BigDecimal("10"))).thenReturn("10.00000");
    when(token1.isNFT()).thenReturn(false);

    Token token2 = mock(Token.class);
    when(token2.getName()).thenReturn("Bitcoin");
    when(token2.getSymbol()).thenReturn("BTC");
    when(token2.getDecimals()).thenReturn(8);
    when(token2.getType()).thenReturn(dev.bloco.wallet.hub.domain.model.token.TokenType.ERC20);
    when(token2.formatAmount(new BigDecimal("1"))).thenReturn("1.00000");
    when(token2.isNFT()).thenReturn(false);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(List.of(address));
    when(tokenBalanceRepository.findByAddressId(addressId)).thenReturn(List.of(token1Balance, token2Balance));
    when(tokenRepository.findById(token1Id)).thenReturn(Optional.of(token1));
    when(tokenRepository.findById(token2Id)).thenReturn(Optional.of(token2));

    GetPortfolioSummaryUseCase useCase = new GetPortfolioSummaryUseCase(walletRepository, addressRepository, tokenBalanceRepository, tokenRepository);

    List<AssetAllocation> allocation = useCase.getAssetAllocation(walletId);

    Assertions.assertNotNull(allocation);
    Assertions.assertEquals(2, allocation.size());
    
    // Should be sorted by percentage descending (BTC: 69.23%, ETH: 30.77%)
    AssetAllocation firstAllocation = allocation.getFirst();
    Assertions.assertEquals("BTC", firstAllocation.symbol());
    Assertions.assertEquals(new BigDecimal("45000"), firstAllocation.value());
    Assertions.assertTrue(firstAllocation.percentage().compareTo(new BigDecimal("69")) > 0);

    AssetAllocation secondAllocation = allocation.get(1);
    Assertions.assertEquals("ETH", secondAllocation.symbol());
    Assertions.assertEquals(new BigDecimal("20000"), secondAllocation.value());
    Assertions.assertTrue(secondAllocation.percentage().compareTo(new BigDecimal("30")) > 0);
  }

  /**
   * Tests portfolio with empty balances (all zero).
   */
  @Test
  @DisplayName("getPortfolioSummary with zero balances returns empty holdings")
  void test_getPortfolioSummary_withZeroBalances_returnsEmptyHoldings() {
    UUID walletId = UUID.randomUUID();
    UUID addressId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();

    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);

    UUID networkId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, "Empty Wallet", "Test");
    Address address = new Address(
        addressId,
        walletId,
        networkId,
        new PublicKey("pubkey"),
        new AccountAddress("0xAddress"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/0"
    );

    // Balance is zero, should be filtered out
    TokenBalance zeroBalance = new TokenBalance(UUID.randomUUID(), addressId, tokenId, BigDecimal.ZERO);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(List.of(address));
    when(tokenBalanceRepository.findByAddressId(addressId)).thenReturn(List.of(zeroBalance));

    GetPortfolioSummaryUseCase useCase = new GetPortfolioSummaryUseCase(walletRepository, addressRepository, tokenBalanceRepository, tokenRepository);

    PortfolioSummary summary = useCase.getPortfolioSummary(walletId);

    Assertions.assertNotNull(summary);
    Assertions.assertEquals(0, summary.totalTokens());
    Assertions.assertEquals(1, summary.totalAddresses());
    Assertions.assertEquals(BigDecimal.ZERO, summary.totalValue());
    Assertions.assertTrue(summary.holdings().isEmpty());
    Assertions.assertTrue(summary.assetAllocation().isEmpty());
  }

  /**
   * Tests NFT token valuation returns zero.
   */
  @Test
  @DisplayName("getPortfolioSummary with NFT tokens values them as zero")
  void test_getPortfolioSummary_withNFTToken_valuesAsZero() {
    UUID walletId = UUID.randomUUID();
    UUID addressId = UUID.randomUUID();
    UUID nftTokenId = UUID.randomUUID();

    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);

    UUID networkId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, "NFT Wallet", "Test");
    Address address = new Address(
        addressId,
        walletId,
        networkId,
        new PublicKey("pubkey"),
        new AccountAddress("0xAddress"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/0"
    );

    TokenBalance nftBalance = new TokenBalance(UUID.randomUUID(), addressId, nftTokenId, BigDecimal.ONE);

    Token nftToken = mock(Token.class);
    when(nftToken.getName()).thenReturn("CryptoPunk");
    when(nftToken.getSymbol()).thenReturn("PUNK");
    when(nftToken.getDecimals()).thenReturn(0);
    when(nftToken.getType()).thenReturn(dev.bloco.wallet.hub.domain.model.token.TokenType.ERC721);
    when(nftToken.formatAmount(BigDecimal.ONE)).thenReturn("1");
    when(nftToken.isNFT()).thenReturn(true);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(List.of(address));
    when(tokenBalanceRepository.findByAddressId(addressId)).thenReturn(List.of(nftBalance));
    when(tokenRepository.findById(nftTokenId)).thenReturn(Optional.of(nftToken));

    GetPortfolioSummaryUseCase useCase = new GetPortfolioSummaryUseCase(walletRepository, addressRepository, tokenBalanceRepository, tokenRepository);

    PortfolioSummary summary = useCase.getPortfolioSummary(walletId);

    Assertions.assertNotNull(summary);
    Assertions.assertEquals(1, summary.totalTokens());
    Assertions.assertEquals(BigDecimal.ZERO, summary.totalValue());
    
    TokenHolding nftHolding = summary.holdings().getFirst();
    Assertions.assertEquals("CryptoPunk", nftHolding.name());
    Assertions.assertEquals("PUNK", nftHolding.symbol());
    Assertions.assertEquals(BigDecimal.ZERO, nftHolding.estimatedValue());
  }

  /**
   * Tests unknown token symbol gets zero price.
   */
  @Test
  @DisplayName("getPortfolioSummary with unknown token symbol values as zero")
  void test_getPortfolioSummary_withUnknownToken_valuesAsZero() {
    UUID walletId = UUID.randomUUID();
    UUID addressId = UUID.randomUUID();
    UUID unknownTokenId = UUID.randomUUID();

    WalletRepository walletRepository = mock(WalletRepository.class);
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);

    UUID networkId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, "Unknown Token Wallet", "Test");
    Address address = new Address(
        addressId,
        walletId,
        networkId,
        new PublicKey("pubkey"),
        new AccountAddress("0xAddress"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/0"
    );

    TokenBalance unknownBalance = new TokenBalance(UUID.randomUUID(), addressId, unknownTokenId, new BigDecimal("100"));

    Token unknownToken = mock(Token.class);
    when(unknownToken.getName()).thenReturn("Unknown Token");
    when(unknownToken.getSymbol()).thenReturn("UNK");
    when(unknownToken.getDecimals()).thenReturn(18);
    when(unknownToken.getType()).thenReturn(dev.bloco.wallet.hub.domain.model.token.TokenType.ERC20);
    when(unknownToken.formatAmount(new BigDecimal("100"))).thenReturn("100.00000");
    when(unknownToken.isNFT()).thenReturn(false);

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
    when(addressRepository.findByWalletId(walletId)).thenReturn(List.of(address));
    when(tokenBalanceRepository.findByAddressId(addressId)).thenReturn(List.of(unknownBalance));
    when(tokenRepository.findById(unknownTokenId)).thenReturn(Optional.of(unknownToken));

    GetPortfolioSummaryUseCase useCase = new GetPortfolioSummaryUseCase(walletRepository, addressRepository, tokenBalanceRepository, tokenRepository);

    PortfolioSummary summary = useCase.getPortfolioSummary(walletId);

    Assertions.assertNotNull(summary);
    Assertions.assertEquals(1, summary.totalTokens());
    Assertions.assertEquals(BigDecimal.ZERO, summary.totalValue());
    
    TokenHolding unknownHolding = summary.holdings().getFirst();
    Assertions.assertEquals("Unknown Token", unknownHolding.name());
    Assertions.assertEquals("UNK", unknownHolding.symbol());
    Assertions.assertEquals(BigDecimal.ZERO, unknownHolding.estimatedValue());
  }
}