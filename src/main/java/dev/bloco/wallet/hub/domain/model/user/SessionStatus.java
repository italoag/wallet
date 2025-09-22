package dev.bloco.wallet.hub.domain.model.user;

/**
 * Enumeration representing the possible statuses of a user session.
 */
public enum SessionStatus {
    /**
     * The session is active and can be used for authentication.
     */
    ACTIVE,
    
    /**
     * The session has been manually invalidated.
     */
    INVALIDATED,
    
    /**
     * The session has expired based on the expiration time.
     */
    EXPIRED
}