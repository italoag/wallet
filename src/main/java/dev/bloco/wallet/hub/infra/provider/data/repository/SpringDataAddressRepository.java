package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.model.address.AddressStatus;
import dev.bloco.wallet.hub.infra.provider.data.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for AddressEntity.
 */
@Repository
public interface SpringDataAddressRepository extends JpaRepository<AddressEntity, UUID> {

    List<AddressEntity> findByWalletId(UUID walletId);

    List<AddressEntity> findByNetworkId(UUID networkId);

    Optional<AddressEntity> findByAccountAddress(String accountAddress);

    List<AddressEntity> findByWalletIdAndStatus(UUID walletId, AddressStatus status);

    Optional<AddressEntity> findByNetworkIdAndAccountAddress(UUID networkId, String accountAddress);
}
