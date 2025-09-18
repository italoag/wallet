package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.vault.Vault;
import dev.bloco.wallet.hub.domain.model.vault.VaultType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VaultRepository {
    Vault save(Vault vault);
    
    Optional<Vault> findById(UUID id);
    
    List<Vault> findAll();
    
    void delete(UUID id);
    
    List<Vault> findByType(VaultType type);
    
    List<Vault> findByName(String name);
    
    boolean existsById(UUID id);
}