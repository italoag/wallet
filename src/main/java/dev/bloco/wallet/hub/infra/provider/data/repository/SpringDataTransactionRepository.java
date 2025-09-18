package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.model.transaction.TransactionStatus;
import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link TransactionEntity} entities through Spring Data JPA.
 * Provides CRUD operations and query methods aligned with blockchain transaction attributes.
 */
public interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    Optional<TransactionEntity> findByHash(String hash);
    List<TransactionEntity> findByNetworkId(UUID networkId);
    List<TransactionEntity> findByFromAddress(String fromAddress);
    List<TransactionEntity> findByToAddress(String toAddress);
    List<TransactionEntity> findByStatus(TransactionStatus status);
    List<TransactionEntity> findByTimestampBetween(Instant start, Instant end);
    boolean existsByHash(String hash);
}
