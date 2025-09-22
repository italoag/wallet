package dev.bloco.wallet.hub.domain.model.wallet;

/**
 * Enumeration representing the possible statuses of a wallet in the system.
 * The wallet status controls the wallet's lifecycle and determines what operations
 * are allowed to be performed on it.
 */
public enum WalletStatus {
    /**
     * The wallet is active and can perform all operations including
     * transactions, balance checks, and address management.
     */
    ACTIVE,
    
    /**
     * The wallet is temporarily inactive. Operations may be restricted
     * but the wallet can be reactivated.
     */
    INACTIVE,
    
    /**
     * The wallet has been soft deleted. It's hidden from normal operations
     * but maintained for audit and recovery purposes.
     */
    DELETED,
    
    /**
     * The wallet is being recovered from backup or seed phrase.
     * Limited operations are available during this state.
     */
    RECOVERING,
    
    /**
     * The wallet is locked due to security concerns or administrative action.
     * No operations are allowed until unlocked.
     */
    LOCKED
}