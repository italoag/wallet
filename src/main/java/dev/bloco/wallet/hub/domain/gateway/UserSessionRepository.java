package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.user.UserSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing user sessions.
 */
public interface UserSessionRepository {
    UserSession save(UserSession session);
    
    Optional<UserSession> findById(UUID id);
    
    Optional<UserSession> findBySessionToken(String sessionToken);
    
    List<UserSession> findByUserId(UUID userId);
    
    List<UserSession> findActiveByUserId(UUID userId);
    
    void delete(UUID id);
    
    void deleteExpiredSessions(Instant before);
    
    void invalidateAllUserSessions(UUID userId);
    
    boolean existsActiveSessionForUser(UUID userId);
    
    void update(UserSession session);
}