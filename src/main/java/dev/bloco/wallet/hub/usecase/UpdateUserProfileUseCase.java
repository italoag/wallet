package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.user.User;

import java.util.UUID;

/**
 * UpdateUserProfileUseCase is responsible for updating user profile information.
 * It allows modification of user details such as name and email.
 * <p/>
 * Business Rules:
 * - User must exist and be active
 * - Email must be unique if changed
 * - Email verification required for new email addresses
 * <p/>
 * Publishes:
 * - UserProfileUpdatedEvent when the profile is successfully updated
 */
public record UpdateUserProfileUseCase(UserRepository userRepository, DomainEventPublisher eventPublisher) {

    /**
     * Updates user profile information.
     *
     * @param userId the unique identifier of the user
     * @param name the new name (optional can be null to keep current)
     * @param email the new email (optional, can be null to keep current)
     * @param correlationId a unique identifier used to trace this operation
     * @return the updated user instance
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if a user is not active or email already exists
     */
    public User updateProfile(UUID userId, String name, String email, String correlationId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Validate user can perform operations
        user.validateOperationAllowed();

        // Validate inputs
        if (name != null && name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (email != null && !isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Check if new email already exists
        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalStateException("Email already exists: " + email);
            }
        }

        // Update profile
        user.updateProfile(name, email);
        userRepository.update(user);

        // Publish events
        user.getDomainEvents().forEach(eventPublisher::publish);
        user.clearEvents();

        return user;
    }

    /**
     * Updates only the user's name.
     *
     * @param userId the unique identifier of the user
     * @param name the new name
     * @param correlationId a unique identifier used to trace this operation
     * @return the updated user instance
     */
    public User updateName(UUID userId, String name, String correlationId) {
        return updateProfile(userId, name, null, correlationId);
    }

    /**
     * Updates only the user's email.
     *
     * @param userId the unique identifier of the user
     * @param email the new email address
     * @param correlationId a unique identifier used to trace this operation
     * @return the updated user instance
     */
    public User updateEmail(UUID userId, String email, String correlationId) {
        return updateProfile(userId, null, email, correlationId);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}