package dev.bloco.wallet.hub.domain.model.transaction;

import dev.bloco.wallet.hub.domain.model.common.Entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents transaction fee information for different networks and fee levels.
 * Provides gas price estimation and fee calculation capabilities.
 */
public class TransactionFee extends Entity {
    private final UUID networkId;
    private final FeeLevel level;
    private final BigDecimal gasPrice;
    private final BigDecimal baseFee;
    private final BigDecimal priorityFee;
    private final BigDecimal maxFeePerGas;
    private final Instant timestamp;
    private final boolean isEstimate;

    public static TransactionFee create(UUID id, UUID networkId, FeeLevel level, 
                                      BigDecimal gasPrice, BigDecimal baseFee, 
                                      BigDecimal priorityFee, boolean isEstimate) {
        return new TransactionFee(id, networkId, level, gasPrice, baseFee, priorityFee, isEstimate);
    }

    private TransactionFee(UUID id, UUID networkId, FeeLevel level, 
                          BigDecimal gasPrice, BigDecimal baseFee, 
                          BigDecimal priorityFee, boolean isEstimate) {
        super(id);
        this.networkId = networkId;
        this.level = level;
        this.gasPrice = gasPrice;
        this.baseFee = baseFee;
        this.priorityFee = priorityFee;
        this.maxFeePerGas = baseFee != null && priorityFee != null ? baseFee.add(priorityFee) : gasPrice;
        this.timestamp = Instant.now();
        this.isEstimate = isEstimate;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public FeeLevel getLevel() {
        return level;
    }

    public BigDecimal getGasPrice() {
        return gasPrice;
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public BigDecimal getPriorityFee() {
        return priorityFee;
    }

    public BigDecimal getMaxFeePerGas() {
        return maxFeePerGas;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isEstimate() {
        return isEstimate;
    }

    /**
     * Calculate total transaction cost for a given gas limit.
     */
    public BigDecimal calculateTotalCost(BigDecimal gasLimit) {
        if (gasLimit == null || gasLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Gas limit must be positive");
        }
        
        return maxFeePerGas.multiply(gasLimit);
    }

    /**
     * Check if the fee estimate is still valid (not too old).
     */
    public boolean isValid(int maxAgeSeconds) {
        Instant now = Instant.now();
        Instant maxAge = timestamp.plusSeconds(maxAgeSeconds);
        return now.isBefore(maxAge);
    }

    /**
     * Get fee level description for UI display.
     */
    public String getLevelDescription() {
        return switch (level) {
            case SLOW -> "Economy (slower confirmation)";
            case STANDARD -> "Standard (normal confirmation)";
            case FAST -> "Fast (quick confirmation)";
            case URGENT -> "Urgent (fastest confirmation)";
        };
    }
}