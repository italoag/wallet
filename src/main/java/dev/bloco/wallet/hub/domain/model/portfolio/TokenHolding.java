package dev.bloco.wallet.hub.domain.model.portfolio;

import dev.bloco.wallet.hub.domain.model.token.TokenType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Individual token-holding information within a portfolio.
 * Represents the aggregated balance and valuation of a specific token
 * across all addresses in a wallet.
 */
@Builder
public record TokenHolding(
    UUID tokenId,
    String name,
    String symbol,
    BigDecimal rawBalance,
    String formattedBalance,
    int decimals,
    BigDecimal estimatedValue,
    TokenType type
) {}
