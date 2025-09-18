package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.User;

import java.util.Optional;
import java.util.UUID;

/**
 * UserRepository defines the interface for managing user entities within the system.
 * Implementations of this interface are responsible for handling the persistence
 * and retrieval of user data as well as providing methods for common user operations.
 *<p/>
 * A user in the context of this repository is uniquely identified by its UUID.
 */
public interface UserRepository {
    Optional<User> findById(UUID id);
    User save(User user);
}
