package dev.bloco.wallet.hub.domain.model.portfolio;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Simplified portfolio overview with basic metrics.
 * Provides a lightweight summary of a wallet's portfolio
 * without detailed holdings or allocation information.
 */
@Builder
public record PortfolioOverview(
    UUID walletId,
    String walletName,
    int totalTokens,
    int totalAddresses,
    BigDecimal totalValue,
    Instant lastUpdated
) {}
