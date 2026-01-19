package dev.bloco.wallet.hub.infra.provider.repository;

import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.domain.model.user.User;
import dev.bloco.wallet.hub.domain.model.user.UserStatus;
import dev.bloco.wallet.hub.infra.provider.data.repository.SpringDataUserRepository;
import dev.bloco.wallet.hub.infra.provider.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional
public class JpaUserRepository implements UserRepository {
    private final SpringDataUserRepository springDataRepository;
    private final UserMapper mapper;

    @Override
    public User save(User user) {
        var entity = mapper.toEntity(user);
        var savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return springDataRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        springDataRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return springDataRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return springDataRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByStatus(UserStatus status) {
        return Collections.emptyList(); // TODO: implement when UserEntity has status field
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return springDataRepository.findByEmail(email).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findActiveUsers() {
        return Collections.emptyList(); // TODO: implement when UserEntity has status field
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmailVerificationToken(String token) {
        return Optional.empty(); // TODO: implement when UserEntity has verification token field
    }

    @Override
    public void update(User user) {
        save(user);
    }
}
