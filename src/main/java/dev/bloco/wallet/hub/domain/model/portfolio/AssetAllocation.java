package dev.bloco.wallet.hub.domain.model.portfolio;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Asset allocation information for portfolio analysis.
 * Represents the distribution of value across different tokens
 * in a wallet's portfolio.
 */
@Builder
public record AssetAllocation(
    UUID tokenId,
    String symbol,
    BigDecimal value,
    BigDecimal percentage
) {}
