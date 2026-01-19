package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.TokenBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataTokenBalanceRepository extends JpaRepository<TokenBalanceEntity, UUID> {
    List<TokenBalanceEntity> findByAddressId(UUID addressId);
    List<TokenBalanceEntity> findByTokenId(UUID tokenId);
    Optional<TokenBalanceEntity> findByAddressIdAndTokenId(UUID addressId, UUID tokenId);
}
