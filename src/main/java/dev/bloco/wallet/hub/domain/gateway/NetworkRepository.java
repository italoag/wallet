package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.network.Network;
import dev.bloco.wallet.hub.domain.model.network.NetworkStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetworkRepository {
    default Network save(Network network) {
        return save(network, null);
    }

    Network save(Network network, String correlationId);

    default Optional<Network> findById(UUID id) {
        return findById(id, null);
    }

    Optional<Network> findById(UUID id, String correlationId);

    default List<Network> findAll() {
        return findAll(null);
    }

    List<Network> findAll(String correlationId);

    default void delete(UUID id) {
        delete(id, null);
    }

    void delete(UUID id, String correlationId);

    default Optional<Network> findByChainId(String chainId) {
        return findByChainId(chainId, null);
    }

    Optional<Network> findByChainId(String chainId, String correlationId);

    default List<Network> findByStatus(NetworkStatus status) {
        return findByStatus(status, null);
    }

    List<Network> findByStatus(NetworkStatus status, String correlationId);

    default List<Network> findByName(String name) {
        return findByName(name, null);
    }

    List<Network> findByName(String name, String correlationId);

    default boolean existsById(UUID id) {
        return existsById(id, null);
    }

    boolean existsById(UUID id, String correlationId);

    default boolean existsByChainId(String chainId) {
        return existsByChainId(chainId, null);
    }

    boolean existsByChainId(String chainId, String correlationId);
}