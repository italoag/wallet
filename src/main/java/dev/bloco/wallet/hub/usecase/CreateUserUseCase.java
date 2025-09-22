package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.user.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * CreateUserUseCase is responsible for user registration.
 * It handles user creation with proper password hashing and email verification setup.
 * <p/>
 * Business Rules:
 * - Email must be unique in the system
 * - Password must meet security requirements
 * - User starts in PENDING_VERIFICATION status
 * - Email verification token is generated
 * <p/>
 * Publishes:
 * - UserCreatedEvent when user is successfully created
 */
public record CreateUserUseCase(UserRepository userRepository, DomainEventPublisher eventPublisher) {

    /**
     * Creates a new user account.
     *
     * @param name the user's full name
     * @param email the user's email address
     * @param password the plain text password
     * @param correlationId a unique identifier used to trace this operation
     * @return the created user instance
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if email already exists
     */
    public User createUser(String name, String email, String password, String correlationId) {
        // Validate inputs
        validateInputs(name, email, password);

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email already exists: " + email);
        }

        // Hash the password
        String passwordHash = hashPassword(password);

        // Create user
        User user = User.create(name, email, passwordHash);
        
        // Generate email verification token
        String verificationToken = generateEmailVerificationToken();
        user.setEmailVerificationToken(verificationToken);

        // Save user
        userRepository.save(user);

        // Publish events
        user.getDomainEvents().forEach(eventPublisher::publish);
        user.clearEvents();

        return user;
    }

    /**
     * Verifies a user's email using the verification token.
     *
     * @param verificationToken the email verification token
     * @param correlationId a unique identifier used to trace this operation
     * @return the verified user
     * @throws IllegalArgumentException if token is invalid
     */
    public User verifyEmail(String verificationToken, String correlationId) {
        if (verificationToken == null || verificationToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification token must be provided");
        }

        User user = userRepository.findByEmailVerificationToken(verificationToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        user.verifyEmail();
        userRepository.update(user);

        // Publish events
        user.getDomainEvents().forEach(eventPublisher::publish);
        user.clearEvents();

        return user;
    }

    private void validateInputs(String name, String email, String password) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name must be provided");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email must be provided");
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!isStrongPassword(password)) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isStrongPassword(String password) {
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&].*$");
    }

    private String hashPassword(String password) {
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

    private String generateEmailVerificationToken() {
        SecureRandom random = new SecureRandom();
        byte[] token = new byte[32];
        random.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
}