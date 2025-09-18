package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.network.Network;
import dev.bloco.wallet.hub.domain.model.network.NetworkStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetworkRepository {
    Network save(Network network);
    
    Optional<Network> findById(UUID id);
    
    List<Network> findAll();
    
    void delete(UUID id);
    
    Optional<Network> findByChainId(String chainId);
    
    List<Network> findByStatus(NetworkStatus status);
    
    List<Network> findByName(String name);
    
    boolean existsById(UUID id);
    
    boolean existsByChainId(String chainId);
}