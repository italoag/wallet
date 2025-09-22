package dev.bloco.wallet.hub.domain.model.transaction;

/**
 * Enumeration representing different fee levels for transactions.
 * Each level represents a trade-off between confirmation speed and cost.
 */
public enum FeeLevel {
    /**
     * Slow confirmation, lowest cost.
     * Suitable for non-urgent transactions.
     */
    SLOW,
    
    /**
     * Standard confirmation speed and cost.
     * Recommended for most transactions.
     */
    STANDARD,
    
    /**
     * Fast confirmation, higher cost.
     * Suitable for time-sensitive transactions.
     */
    FAST,
    
    /**
     * Urgent confirmation, highest cost.
     * For critical transactions requiring immediate confirmation.
     */
    URGENT
}