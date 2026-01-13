package dev.bloco.wallet.hub.domain.model.portfolio;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Complete portfolio summary with comprehensive analytics.
 * Provides a full view of a wallet's holdings, value distribution,
 * and asset allocation.
 */
@Builder
public record PortfolioSummary(
    UUID walletId,
    String walletName,
    int totalTokens,
    int totalAddresses,
    BigDecimal totalValue,
    List<TokenHolding> holdings,
    List<AssetAllocation> assetAllocation,
    Instant lastUpdated
) {}
