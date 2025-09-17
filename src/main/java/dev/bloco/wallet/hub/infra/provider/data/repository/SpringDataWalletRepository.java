package dev.bloco.wallet.hub.infra.provider.data.repository;


import dev.bloco.wallet.hub.infra.provider.data.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataWalletRepository extends JpaRepository<WalletEntity, UUID> {
}
