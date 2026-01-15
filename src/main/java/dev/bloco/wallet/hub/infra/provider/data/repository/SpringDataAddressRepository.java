package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAddressRepository extends JpaRepository<AddressEntity, UUID> {
    List<AddressEntity> findByWalletId(UUID walletId);
    List<AddressEntity> findByNetworkId(UUID networkId);
    Optional<AddressEntity> findByAccountAddress(String accountAddress);
    List<AddressEntity> findByWalletIdAndStatus(UUID walletId, String status);
    Optional<AddressEntity> findByNetworkIdAndAccountAddress(UUID networkId, String accountAddress);
}
