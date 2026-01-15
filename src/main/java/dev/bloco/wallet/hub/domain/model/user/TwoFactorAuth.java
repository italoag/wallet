package dev.bloco.wallet.hub.domain.model.user;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * Represents two-factor authentication settings for a user.
 * Manages TOTP secrets, backup codes, and 2FA status.
 */
public class TwoFactorAuth {
    private final UUID userId;
    private String totpSecret;
    private String[] backupCodes;
    private boolean isEnabled;
    private Instant enabledAt;
    private Instant lastUsedAt;
    private int usedBackupCodesCount;

    public static TwoFactorAuth create(UUID userId) {
        return new TwoFactorAuth(userId);
    }

    private TwoFactorAuth(UUID userId) {
        this.userId = userId;
        this.isEnabled = false;
        this.usedBackupCodesCount = 0;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public String[] getBackupCodes() {
        return backupCodes != null ? Arrays.copyOf(backupCodes, backupCodes.length) : new String[0];
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Instant getEnabledAt() {
        return enabledAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public int getUsedBackupCodesCount() {
        return usedBackupCodesCount;
    }

    public int getAvailableBackupCodesCount() {
        return backupCodes != null ? backupCodes.length - usedBackupCodesCount : 0;
    }

    /**
     * Enable 2FA with a TOTP secret and generate backup codes.
     */
    public void enable(String totpSecret) {
        if (totpSecret == null || totpSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("TOTP secret must be provided");
        }
        
        this.totpSecret = totpSecret;
        this.backupCodes = generateBackupCodes();
        this.isEnabled = true;
        this.enabledAt = Instant.now();
        this.usedBackupCodesCount = 0;
    }

    /**
     * Disable 2FA and clear all secrets.
     */
    public void disable() {
        this.isEnabled = false;
        this.totpSecret = null;
        this.backupCodes = null;
        this.usedBackupCodesCount = 0;
    }

    /**
     * Mark 2FA as used (for rate limiting and analytics).
     */
    public void markAsUsed() {
        this.lastUsedAt = Instant.now();
    }

    /**
     * Use a backup code and mark it as consumed.
     */
    public boolean useBackupCode(String code) {
        if (!isEnabled || backupCodes == null || code == null) {
            return false;
        }

        for (int i = 0; i < backupCodes.length; i++) {
            if (code.equals(backupCodes[i])) {
                backupCodes[i] = null; // Mark as used
                usedBackupCodesCount++;
                markAsUsed();
                return true;
            }
        }
        
        return false;
    }

    /**
     * Generate new backup codes, invalidating old ones.
     */
    public String[] regenerateBackupCodes() {
        if (!isEnabled) {
            throw new IllegalStateException("2FA must be enabled to regenerate backup codes");
        }
        
        this.backupCodes = generateBackupCodes();
        this.usedBackupCodesCount = 0;
        return getBackupCodes();
    }

    /**
     * Generate a set of backup codes.
     */
    private String[] generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        String[] codes = new String[10]; // Generate 10 backup codes
        
        for (int i = 0; i < codes.length; i++) {
            codes[i] = "%08d".formatted(random.nextInt(100000000));
        }
        
        return codes;
    }

    /**
     * Check if backup codes are running low.
     */
    public boolean isBackupCodesLow() {
        return getAvailableBackupCodesCount() <= 2;
    }

    /**
     * Validate the 2FA state for operations.
     */
    public void validateEnabled() {
        if (!isEnabled) {
            throw new IllegalStateException("Two-factor authentication is not enabled");
        }
    }
}