package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.domain.gateway.UserSessionRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.user.User;
import dev.bloco.wallet.hub.domain.model.user.UserSession;
import dev.bloco.wallet.hub.domain.event.user.UserAuthenticatedEvent;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * AuthenticateUserUseCase is responsible for user authentication and session management.
 * It handles login validation, session creation, and failed login tracking.
 * <p/>
 * Business Rules:
 * - User must exist and be active
 * - Password must match the stored hash
 * - Account lockout after failed attempts
 * - Session expiration management
 * <p/>
 * Publishes:
 * - UserAuthenticatedEvent when login is successful
 */
public record AuthenticateUserUseCase(
    UserRepository userRepository,
    UserSessionRepository sessionRepository,
    DomainEventPublisher eventPublisher) {

    /**
     * Authenticates a user and creates a session.
     *
     * @param email the user's email address
     * @param password the plain text password
     * @param ipAddress the client's IP address
     * @param userAgent the client's user agent
     * @param correlationId a unique identifier used to trace this operation
     * @return authentication result with session information
     * @throws IllegalArgumentException if credentials are invalid
     * @throws IllegalStateException if an account is locked or inactive
     */
    public AuthenticationResult authenticate(String email, String password, String ipAddress, 
                                           String userAgent, String correlationId) {
        // Validate inputs
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email must be provided");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password must be provided");
        }

        // Find the user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // Check if an account is locked
        if (user.isLocked()) {
            throw new IllegalStateException("Account is temporarily locked due to failed login attempts");
        }

        // Validate password
        if (!verifyPassword(password, user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.update(user);
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Check if the user is active
        user.validateOperationAllowed();

        // Record successful login
        user.recordSuccessfulLogin();
        userRepository.update(user);

        // Create session
        UserSession session = createSession(user.getId(), ipAddress, userAgent);
        sessionRepository.save(session);

        // Publish authentication event
        UserAuthenticatedEvent event = UserAuthenticatedEvent.builder()
                .userId(user.getId())
                .email(email)
                .sessionId(session.getId())
                .ipAddress(ipAddress)
                .correlationId(UUID.fromString(correlationId))
                .build();
        eventPublisher.publish(event);

        return new AuthenticationResult(
            user.getId(),
            user.getName(),
            user.getEmail(),
            session.getSessionToken(),
            session.getExpiresAt(),
            user.isTwoFactorEnabled()
        );
    }

    /**
     * Validates a session token and returns user information.
     *
     * @param sessionToken the session token to validate
     * @return user session information
     * @throws IllegalArgumentException if session is invalid or expired
     */
    public SessionValidationResult validateSession(String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Session token must be provided");
        }

        UserSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid session token"));

        session.validateActive();
        session.updateLastAccessed();
        sessionRepository.update(session);

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for session"));

        user.validateOperationAllowed();

        return new SessionValidationResult(
            user.getId(),
            user.getName(),
            user.getEmail(),
            session.getId(),
            session.getExpiresAt()
        );
    }

    /**
     * Invalidates a user session (logout).
     *
     * @param sessionToken the session token to invalidate
     * @param correlationId a unique identifier used to trace this operation
     */
    public void logout(String sessionToken, String correlationId) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return; // Silently ignore invalid tokens for logout
        }

        sessionRepository.findBySessionToken(sessionToken)
                .ifPresent(session -> {
                    session.invalidate();
                    sessionRepository.update(session);
                });
    }

    /**
     * Invalidates all sessions for a user.
     *
     * @param userId the user ID
     * @param correlationId a unique identifier used to trace this operation
     */
    public void logoutAllSessions(UUID userId, String correlationId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }

        sessionRepository.invalidateAllUserSessions(userId);
    }

    UserSession createSession(UUID userId, String ipAddress, String userAgent) {
        String sessionToken = generateSessionToken();
        Instant expiresAt = Instant.now().plusSeconds(24 * 60 * 60); // 24 hours

        UserSession session = UserSession.create(UUID.randomUUID(), userId, sessionToken, expiresAt);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);

        return session;
    }

    private String generateSessionToken() {
        SecureRandom random = new SecureRandom();
        byte[] token = new byte[32];
        random.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    boolean verifyPassword(String password, String hashedPassword) {
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

    /**
     * Result of a successful authentication.
     */
    public record AuthenticationResult(
        UUID userId,
        String name,
        String email,
        String sessionToken,
        Instant expiresAt,
        boolean twoFactorEnabled
    ) {}

    /**
     * Result of session validation.
     */
    public record SessionValidationResult(
        UUID userId,
        String name,
        String email,
        UUID sessionId,
        Instant expiresAt
    ) {}
}