package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.user.User;
import dev.bloco.wallet.hub.domain.model.user.UserStatus;

import java.util.List;
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
    
    void update(User user);
    
    void delete(UUID id);
    
    List<User> findAll();
    
    Optional<User> findByEmail(String email);
    
    List<User> findByStatus(UserStatus status);
    
    boolean existsById(UUID id);
    
    boolean existsByEmail(String email);
    
    List<User> findActiveUsers();
    
    Optional<User> findByEmailVerificationToken(String token);
}
