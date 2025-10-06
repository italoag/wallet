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
import dev.bloco.wallet.hub.usecase.GetPortfolioSummaryUseCase.AssetAllocation;
import dev.bloco.wallet.hub.usecase.GetPortfolioSummaryUseCase.PortfolioSummary;
import dev.bloco.wallet.hub.usecase.GetPortfolioSummaryUseCase.TokenHolding;
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
}