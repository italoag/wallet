package dev.bloco.wallet.hub.domain.model.transaction;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Fee estimate for a specific fee level.
 * Provides comprehensive fee information including gas price breakdown,
 * total cost estimation, and expected confirmation time.
 */
@Builder
public record FeeEstimate(
    FeeLevel level,
    BigDecimal gasPrice,
    BigDecimal baseFee,
    BigDecimal priorityFee,
    BigDecimal totalCost,
    String estimatedTime,
    String description
) {}
