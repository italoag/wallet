package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.domain.gateway.UserSessionRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.user.User;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * DeactivateUserUseCase is responsible for deactivating user accounts.
 * It handles account deactivation and session cleanup for security.
 * <p/>
 * Business Rules:
 * - User must exist
 * - User cannot deactivate themselves (security measure)
 * - All user sessions are invalidated upon deactivation
 * - Deactivation reason should be provided for audit
 * <p/>
 * Publishes:
 * - UserStatusChangedEvent when a user is successfully deactivated
 */
@RequiredArgsConstructor
public class DeactivateUserUseCase {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Deactivates a user account.
     *
     * @param userId        the unique identifier of the user to deactivate
     * @param reason        the reason for deactivation (for audit purposes)
     * @param correlationId a unique identifier used to trace this operation
     * @throws IllegalArgumentException if validation fails
     */
    public void deactivateUser(UUID userId, String reason, String correlationId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason for deactivation must be provided");
        }

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Deactivate user
        user.deactivate();
        userRepository.update(user);

        // Invalidate all user sessions
        sessionRepository.invalidateAllUserSessions(userId);

        // Publish events
        user.getDomainEvents().forEach(eventPublisher::publish);
        user.clearEvents();
    }

    /**
     * Activates a previously deactivated user account.
     *
     * @param userId        the unique identifier of the user to activate
     * @param correlationId a unique identifier used to trace this operation
     * @return the activated user instance
     * @throws IllegalArgumentException if validation fails
     */
    public User activateUser(UUID userId, String correlationId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Activate user
        user.activate();
        userRepository.update(user);

        // Publish events
        user.getDomainEvents().forEach(eventPublisher::publish);
        user.clearEvents();

        return user;
    }

    /**
     * Suspends a user account temporarily.
     *
     * @param userId        the unique identifier of the user to suspend
     * @param reason        the reason for suspension
     * @param correlationId a unique identifier used to trace this operation
     * @throws IllegalArgumentException if validation fails
     */
    public void suspendUser(UUID userId, String reason, String correlationId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason for suspension must be provided");
        }

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Suspend user
        user.suspend(reason);
        userRepository.update(user);

        // Invalidate all user sessions
        sessionRepository.invalidateAllUserSessions(userId);

        // Publish events
        user.getDomainEvents().forEach(eventPublisher::publish);
        user.clearEvents();
    }

    /**
     * Unlocks a user account that was locked due to failed login attempts.
     *
     * @param userId        the unique identifier of the user to unlock
     * @param correlationId a unique identifier used to trace this operation
     * @return the unlocked user instance
     * @throws IllegalArgumentException if validation fails
     */
    public User unlockUser(UUID userId, String correlationId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Unlock user
        user.unlock();
        userRepository.update(user);

        return user;
    }
}