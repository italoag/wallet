package dev.bloco.wallet.hub.domain.model.user;

import dev.bloco.wallet.hub.domain.model.common.Entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a user session in the system.
 * Manages authentication state and session lifecycle.
 */
public class UserSession extends Entity {
    private final UUID userId;
    private final String sessionToken;
    private final Instant createdAt;
    private final Instant expiresAt;
    private SessionStatus status;
    private String ipAddress;
    private String userAgent;
    private Instant lastAccessedAt;

    public static UserSession create(UUID id, UUID userId, String sessionToken, Instant expiresAt) {
        return new UserSession(id, userId, sessionToken, expiresAt);
    }

    private UserSession(UUID id, UUID userId, String sessionToken, Instant expiresAt) {
        super(id);
        this.userId = userId;
        this.sessionToken = sessionToken;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.status = SessionStatus.ACTIVE;
        this.lastAccessedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public String getToken() {
        return sessionToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void updateLastAccessed() {
        this.lastAccessedAt = Instant.now();
    }

    public void invalidate() {
        this.status = SessionStatus.INVALIDATED;
    }

    public void expire() {
        this.status = SessionStatus.EXPIRED;
    }

    public boolean isActive() {
        return this.status == SessionStatus.ACTIVE && !isExpired();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public void validateActive() {
        if (!isActive()) {
            throw new IllegalStateException("Session is not active or has expired");
        }
    }
}