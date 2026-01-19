package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionFeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataTransactionFeeRepository extends JpaRepository<TransactionFeeEntity, UUID> {
    List<TransactionFeeEntity> findByNetworkId(UUID networkId);

    List<TransactionFeeEntity> findByNetworkIdAndFeeLevel(UUID networkId,
            dev.bloco.wallet.hub.domain.model.transaction.FeeLevel feeLevel);

    java.util.Optional<TransactionFeeEntity> findFirstByNetworkIdAndFeeLevelOrderByTimestampDesc(UUID networkId,
            dev.bloco.wallet.hub.domain.model.transaction.FeeLevel feeLevel);

    void deleteByTimestampBefore(java.time.Instant timestamp);
}
