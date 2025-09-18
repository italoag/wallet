package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.store.Store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreRepository {
    Store save(Store store);
    
    Optional<Store> findById(UUID id);
    
    List<Store> findAll();
    
    void delete(UUID id);
    
    List<Store> findByVaultId(UUID vaultId);
    
    List<Store> findByName(String name);
    
    boolean existsById(UUID id);
    
    List<Store> findByAddressId(UUID addressId);
}