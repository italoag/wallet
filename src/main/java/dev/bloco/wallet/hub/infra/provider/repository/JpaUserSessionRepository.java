package dev.bloco.wallet.hub.infra.provider.repository;

import dev.bloco.wallet.hub.domain.gateway.UserSessionRepository;
import dev.bloco.wallet.hub.domain.model.user.UserSession;
import dev.bloco.wallet.hub.infra.provider.data.repository.SpringDataUserSessionRepository;
import dev.bloco.wallet.hub.infra.provider.mapper.UserSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional
public class JpaUserSessionRepository implements UserSessionRepository {
    private final SpringDataUserSessionRepository springDataRepository;
    private final UserSessionMapper mapper;

    @Override
    public UserSession save(UserSession session) {
        var entity = mapper.toEntity(session);
        var savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSession> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void delete(UUID id) {
        springDataRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSession> findBySessionToken(String sessionToken) {
        return springDataRepository.findByToken(sessionToken).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSession> findByUserId(UUID userId) {
        return springDataRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSession> findActiveByUserId(UUID userId) {
        return springDataRepository.findByUserIdAndIsActive(userId, true).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteExpiredSessions(Instant before) {
        // Implementation would require custom query or logic
        // For now, no-op or implementation if repo supports it
    }

    @Override
    public void invalidateAllUserSessions(UUID userId) {
        List<UserSession> sessions = findByUserId(userId);
        sessions.forEach(session -> {
            session.invalidate();
            save(session);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveSessionForUser(UUID userId) {
        return !findActiveByUserId(userId).isEmpty();
    }

    @Override
    public void update(UserSession session) {
        save(session);
    }
}
