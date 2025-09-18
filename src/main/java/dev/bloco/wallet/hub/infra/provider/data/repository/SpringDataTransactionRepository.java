package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link TransactionEntity} entities through Spring Data JPA.
 * This interface provides basic CRUD operations and custom query methods specific to transaction data.
 *<p/>
 * Responsibilities:
 * - Provides methods to interact with the database for managing persistence of {@link TransactionEntity}.
 * - Supports retrieving transaction data based on the originator or recipient wallet IDs.
 *<p/>
 * Notes:
 * - Extends {@link JpaRepository} to leverage Spring Data JPA for CRUD operations.
 * - Accepts {@link TransactionEntity} as the entity class and {@link UUID} as the ID type.
 *<p/>
 * Custom Query Method:
 * - {@code findByFromWalletIdOrToWalletId}: Retrieves a list of transactions associated with either
 *   the specified originator wallet ID or the recipient wallet ID.
 */
public interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findByFromWalletIdOrToWalletId(UUID fromWalletId, UUID toWalletId);
}
