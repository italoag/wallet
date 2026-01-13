package dev.bloco.wallet.hub.domain.model.transaction;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Complete fee estimation result.
 * Aggregates fee estimates for all fee levels on a specific network,
 * providing comprehensive transaction cost analysis.
 */
@Builder
public record FeeEstimateResult(
    UUID networkId,
    String networkName,
    BigDecimal gasLimit,
    List<FeeEstimate> estimates
) {}
