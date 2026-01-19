package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.WalletTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataWalletTokenRepository extends JpaRepository<WalletTokenEntity, UUID> {
    List<WalletTokenEntity> findByWalletId(UUID walletId);
    List<WalletTokenEntity> findByTokenId(UUID tokenId);
    Optional<WalletTokenEntity> findByWalletIdAndTokenId(UUID walletId, UUID tokenId);
    List<WalletTokenEntity> findByWalletIdAndIsEnabled(UUID walletId, boolean isEnabled);
}
