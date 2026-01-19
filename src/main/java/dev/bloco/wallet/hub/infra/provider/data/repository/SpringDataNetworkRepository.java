package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.model.network.NetworkStatus;
import dev.bloco.wallet.hub.infra.provider.data.entity.NetworkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataNetworkRepository extends JpaRepository<NetworkEntity, UUID> {
    Optional<NetworkEntity> findByChainId(String chainId);

    List<NetworkEntity> findByStatus(NetworkStatus status);

    boolean existsByChainId(String chainId);
}
