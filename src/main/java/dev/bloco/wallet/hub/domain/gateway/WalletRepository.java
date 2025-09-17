package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.Wallet;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
    Optional<Wallet> findById(UUID id);
    Wallet save(Wallet wallet);
    void update(Wallet wallet);
}
