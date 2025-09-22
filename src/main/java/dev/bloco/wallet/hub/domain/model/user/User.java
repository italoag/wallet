package dev.bloco.wallet.hub.domain.model.user;

import dev.bloco.wallet.hub.domain.event.user.UserCreatedEvent;
import dev.bloco.wallet.hub.domain.event.user.UserStatusChangedEvent;
import dev.bloco.wallet.hub.domain.event.user.UserProfileUpdatedEvent;
import dev.bloco.wallet.hub.domain.model.common.AggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a user in the system with comprehensive authentication and profile management.
 * This class manages user identity, security settings, and account lifecycle.
 * <p/>
 * The User entity is an aggregate root that publishes domain events for
 * user creation, profile updates, and status changes.
 */
@Getter
public class User extends AggregateRoot {
    private String name;
    private String email;
    private String passwordHash;
    private UserStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
    private boolean emailVerified;
    private String emailVerificationToken;
    private TwoFactorAuth twoFactorAuth;
    private int failedLoginAttempts;
    private Instant lockedUntil;

  /**
   * Creates a new User with the specified details and registers a UserCreatedEvent.
   *
   * @param id the unique identifier for the user
   * @param name the name of the user
   * @param email the email address of the user
   * @param passwordHash the hashed password
   * @return a new User instance
   */
    public static User create(UUID id, String name, String email, String passwordHash) {
        User user = new User(id, name, email, passwordHash);
        user.registerEvent(new UserCreatedEvent(id, name, email, null));
        return user;
    }

    /**
     * Creates a new User and automatically generates an ID.
     */
    public static User create(String name, String email, String passwordHash) {
        return create(UUID.randomUUID(), name, email, passwordHash);
    }

    private User(UUID id, String name, String email, String passwordHash) {
        super(id);
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = UserStatus.PENDING_VERIFICATION;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.emailVerified = false;
        this.twoFactorAuth = TwoFactorAuth.create(id);
        this.failedLoginAttempts = 0;
    }

    /**
     * Updates user profile information.
     */
    public void updateProfile(String name, String email) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
        if (email != null && !email.trim().isEmpty()) {
            this.email = email;
            this.emailVerified = false; // Require re-verification for new email
        }
        this.updatedAt = Instant.now();
        registerEvent(new UserProfileUpdatedEvent(getId(), this.name, this.email, null));
    }

    /**
     * Changes the user's password.
     */
    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be null or empty");
        }
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
        this.failedLoginAttempts = 0; // Reset failed attempts on password change
    }

    /**
     * Activates the user account.
     */
    public void activate() {
        if (this.status != UserStatus.ACTIVE) {
            UserStatus oldStatus = this.status;
            this.status = UserStatus.ACTIVE;
            this.updatedAt = Instant.now();
            registerEvent(new UserStatusChangedEvent(getId(), oldStatus, this.status, "User activated", null));
        }
    }

    /**
     * Deactivates the user account.
     */
    public void deactivate() {
        if (this.status != UserStatus.DEACTIVATED) {
            UserStatus oldStatus = this.status;
            this.status = UserStatus.DEACTIVATED;
            this.updatedAt = Instant.now();
            registerEvent(new UserStatusChangedEvent(getId(), oldStatus, this.status, "User deactivated", null));
        }
    }

    /**
     * Suspends the user account.
     */
    public void suspend(String reason) {
        if (this.status != UserStatus.SUSPENDED) {
            UserStatus oldStatus = this.status;
            this.status = UserStatus.SUSPENDED;
            this.updatedAt = Instant.now();
            registerEvent(new UserStatusChangedEvent(getId(), oldStatus, this.status, reason, null));
        }
    }

    /**
     * Verifies the user's email address.
     */
    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerificationToken = null;
        if (this.status == UserStatus.PENDING_VERIFICATION) {
            activate();
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Sets email verification token.
     */
    public void setEmailVerificationToken(String token) {
        this.emailVerificationToken = token;
        this.updatedAt = Instant.now();
    }

    /**
     * Records a successful login.
     */
    public void recordSuccessfulLogin() {
        this.lastLoginAt = Instant.now();
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    /**
     * Records a failed login attempt.
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        this.updatedAt = Instant.now();
        
        // Lock account after 5 failed attempts for 30 minutes
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = Instant.now().plusSeconds(1800); // 30 minutes
        }
    }

    /**
     * Checks if the account is locked due to failed login attempts.
     */
    public boolean isLocked() {
        return this.lockedUntil != null && Instant.now().isBefore(this.lockedUntil);
    }

    /**
     * Unlocks the account manually.
     */
    public void unlock() {
        this.lockedUntil = null;
        this.failedLoginAttempts = 0;
        this.updatedAt = Instant.now();
    }

    /**
     * Checks if the user is active and can perform operations.
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE && !isLocked();
    }

    /**
     * Validates if operations can be performed by this user.
     */
    public void validateOperationAllowed() {
        if (!isActive()) {
            throw new IllegalStateException("User is not active or account is locked. Status: " + this.status);
        }
    }

    /**
     * Enables two-factor authentication.
     */
    public void enableTwoFactorAuth(String totpSecret) {
        this.twoFactorAuth.enable(totpSecret);
        this.updatedAt = Instant.now();
    }

    /**
     * Disables two-factor authentication.
     */
    public void disableTwoFactorAuth() {
        this.twoFactorAuth.disable();
        this.updatedAt = Instant.now();
    }

    /**
     * Checks if two-factor authentication is enabled.
     */
    public boolean isTwoFactorEnabled() {
        return this.twoFactorAuth.isEnabled();
    }

    /**
     * Gets available backup codes count.
     */
    public int getAvailableBackupCodesCount() {
        return this.twoFactorAuth.getAvailableBackupCodesCount();
    }
}
