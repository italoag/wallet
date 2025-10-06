package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.domain.gateway.UserSessionRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.user.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * ChangePasswordUseCase is responsible for updating user passwords.
 * It handles password validation, hashing, and session invalidation for security.
 * <p/>
 * Business Rules:
 * - User must exist and be active
 * - Current password must be verified
 * - New password must meet security requirements
 * - All user sessions are invalidated after password change
 * <p/>
 * No domain events are published for security reasons.
 */
public record ChangePasswordUseCase(
    UserRepository userRepository,
    UserSessionRepository sessionRepository,
    DomainEventPublisher eventPublisher) {

    /**
     * Changes a user's password.
     *
     * @param userId the unique identifier of the user
     * @param currentPassword the user's current password
     * @param newPassword the new password
     * @param correlationId a unique identifier used to trace this operation
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if user is not active
     */
    public void changePassword(UUID userId, String currentPassword, String newPassword, String correlationId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }

        // Validate inputs
        validatePasswords(currentPassword, newPassword);

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Validate user can perform operations
        user.validateOperationAllowed();

        // Verify current password
        if (!verifyPassword(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Hash new password
        String newPasswordHash = hashPassword(newPassword);

        // Update password
        user.changePassword(newPasswordHash);
        userRepository.update(user);

        // Invalidate all user sessions for security
        sessionRepository.invalidateAllUserSessions(userId);
    }

    /**
     * Resets a user's password (admin operation).
     *
     * @param userId the unique identifier of the user
     * @param newPassword the new password
     * @param correlationId a unique identifier used to trace this operation
     * @throws IllegalArgumentException if validation fails
     */
    public void resetPassword(UUID userId, String newPassword, String correlationId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!isStrongPassword(newPassword)) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Hash new password
        String newPasswordHash = hashPassword(newPassword);

        // Update password
        user.changePassword(newPasswordHash);
        userRepository.update(user);

        // Invalidate all user sessions for security
        sessionRepository.invalidateAllUserSessions(userId);
    }

    private void validatePasswords(String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Current password must be provided");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }
        if (!isStrongPassword(newPassword)) {
            throw new IllegalArgumentException("New password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }
        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password must be different from current password");
        }
    }

    private boolean isStrongPassword(String password) {
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&].*$");
    }

    private boolean verifyPassword(String password, String hashedPassword) {
        try {
            byte[] combined = Base64.getDecoder().decode(hashedPassword);
            
            // Extract salt (first 16 bytes)
            byte[] salt = Arrays.copyOfRange(combined, 0, 16);
            
            // Extract hash (remaining bytes)
            byte[] storedHash = Arrays.copyOfRange(combined, 16, combined.length);
            
            // Hash the provided password with the extracted salt
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] computedHash = md.digest(password.getBytes());
            
            // Compare hashes
            return Arrays.equals(storedHash, computedHash);
        } catch (Exception e) {
            return false;
        }
    }

    String hashPassword(String password) {
        try {
            // Generate salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Hash password with salt
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // Combine salt and hash
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}