package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenBalanceRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.domain.model.token.TokenBalance;
import dev.bloco.wallet.hub.domain.model.portfolio.AssetAllocation;
import dev.bloco.wallet.hub.domain.model.portfolio.PortfolioOverview;
import dev.bloco.wallet.hub.domain.model.portfolio.PortfolioSummary;
import dev.bloco.wallet.hub.domain.model.portfolio.TokenHolding;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GetPortfolioSummaryUseCase is responsible for providing comprehensive
 * portfolio analytics.
 * It aggregates wallet data to provide insights into asset allocation and
 * portfolio value.
 * <p/>
 * Business Rules:
 * - Wallet must exist and be accessible
 * - Balances are aggregated across all addresses
 * - Zero balances are excluded from summary
 * - Portfolio value calculations require price data
 * <p/>
 * No domain events are published by this read-only operation.
 */
@RequiredArgsConstructor
public class GetPortfolioSummaryUseCase {

    private final WalletRepository walletRepository;
    private final AddressRepository addressRepository;
    private final TokenBalanceRepository tokenBalanceRepository;
    private final TokenRepository tokenRepository;

    /**
     * Retrieves comprehensive portfolio summary for a wallet.
     *
     * @param walletId the unique identifier of the wallet
     * @return complete portfolio analytics
     * @throws IllegalArgumentException if wallet not found
     */
    public PortfolioSummary getPortfolioSummary(UUID walletId) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must be provided");
        }

        // Validate wallet exists
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        // Get all addresses for the wallet
        List<Address> addresses = addressRepository.findByWalletId(walletId);

        // Get all token balances across all addresses
        List<TokenBalance> allBalances = addresses.stream()
                .flatMap(address -> tokenBalanceRepository.findByAddressId(address.getId()).stream())
                .filter(balance -> balance.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        // Group balances by token
        Map<UUID, BigDecimal> aggregatedBalances = allBalances.stream()
                .collect(Collectors.groupingBy(
                        TokenBalance::getTokenId,
                        Collectors.reducing(BigDecimal.ZERO, TokenBalance::getBalance, BigDecimal::add)));

        // Get token details and create holdings
        List<TokenHolding> holdings = aggregatedBalances.entrySet().stream()
                .map(entry -> createTokenHolding(entry.getKey(), entry.getValue()))
                .toList();

        // Calculate portfolio metrics
        int totalTokens = holdings.size();
        int totalAddresses = addresses.size();
        BigDecimal totalValue = calculateTotalValue(holdings);

        // Calculate asset allocation
        List<AssetAllocation> allocation = calculateAssetAllocation(holdings, totalValue);

        return new PortfolioSummary(
                walletId,
                wallet.getName(),
                totalTokens,
                totalAddresses,
                totalValue,
                holdings,
                allocation,
                java.time.Instant.now());
    }

    /**
     * Gets simplified portfolio overview for a wallet.
     *
     * @param walletId the unique identifier of the wallet
     * @return basic portfolio metrics
     */
    public PortfolioOverview getPortfolioOverview(UUID walletId) {
        PortfolioSummary summary = getPortfolioSummary(walletId);

        return new PortfolioOverview(
                summary.walletId(),
                summary.walletName(),
                summary.totalTokens(),
                summary.totalAddresses(),
                summary.totalValue(),
                summary.lastUpdated());
    }

    /**
     * Gets asset allocation breakdown for a wallet.
     *
     * @param walletId the unique identifier of the wallet
     * @return asset allocation analysis
     */
    public List<AssetAllocation> getAssetAllocation(UUID walletId) {
        PortfolioSummary summary = getPortfolioSummary(walletId);
        return summary.assetAllocation();
    }

    private TokenHolding createTokenHolding(UUID tokenId, BigDecimal balance) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalStateException("Token not found: " + tokenId));

        String formattedBalance = token.formatAmount(balance);

        // In a real implementation, this would fetch current market price
        BigDecimal estimatedValue = calculateEstimatedValue(token, balance);

        return new TokenHolding(
                tokenId,
                token.getName(),
                token.getSymbol(),
                balance,
                formattedBalance,
                token.getDecimals(),
                estimatedValue,
                token.getType());
    }

    private BigDecimal calculateEstimatedValue(Token token, BigDecimal balance) {
        // Placeholder for price calculation
        // In a real implementation, this would use a price oracle or external API
        if (token.isNFT()) {
            return BigDecimal.ZERO; // NFTs need special valuation
        }

        // Mock price data for demonstration
        BigDecimal mockPrice = switch (token.getSymbol().toUpperCase()) {
            case "ETH" -> new BigDecimal("2000");
            case "BTC" -> new BigDecimal("45000");
            case "USDC", "USDT" -> BigDecimal.ONE;
            default -> BigDecimal.ZERO;
        };

        return balance.multiply(mockPrice);
    }

    private BigDecimal calculateTotalValue(List<TokenHolding> holdings) {
        return holdings.stream()
                .map(TokenHolding::estimatedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<AssetAllocation> calculateAssetAllocation(List<TokenHolding> holdings, BigDecimal totalValue) {
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        return holdings.stream()
                .map(holding -> {
                    BigDecimal percentage = holding.estimatedValue()
                            .divide(totalValue, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));

                    return new AssetAllocation(
                            holding.tokenId(),
                            holding.symbol(),
                            holding.estimatedValue(),
                            percentage);
                })
                .sorted((a, b) -> b.percentage().compareTo(a.percentage()))
                .toList();
    }
}