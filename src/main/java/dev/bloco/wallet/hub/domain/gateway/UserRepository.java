package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);
    User save(User user);
}
