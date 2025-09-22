package dev.bloco.wallet.hub.domain.model.user;

/**
 * Enumeration representing the possible statuses of a user in the system.
 * The user status controls access to system features and determines
 * what operations are allowed.
 */
public enum UserStatus {
    /**
     * The user is active and can perform all operations.
     */
    ACTIVE,
    
    /**
     * The user account is temporarily inactive.
     * Login is restricted but account can be reactivated.
     */
    INACTIVE,
    
    /**
     * The user account is suspended due to security or policy violations.
     * Administrative action required to reactivate.
     */
    SUSPENDED,
    
    /**
     * The user account is pending email verification.
     * Limited functionality until verified.
     */
    PENDING_VERIFICATION,
    
    /**
     * The user account has been deactivated.
     * All operations are restricted.
     */
    DEACTIVATED
}