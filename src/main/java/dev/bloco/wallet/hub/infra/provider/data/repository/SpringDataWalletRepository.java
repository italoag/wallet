package dev.bloco.wallet.hub.infra.provider.data.repository;


import dev.bloco.wallet.hub.infra.provider.data.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository interface for managing {@link WalletEntity} entities. Extends the
 * {@link JpaRepository} to provide basic CRUD operations and additional query
 * capabilities specific to the application.
 *<p/>
 * This interface serves as a mechanism for interacting with the database
 * to manage persistence and retrieval of wallet information.
 *<p/>
 * Key Features:
 * - Leverages Spring Data JPA's capabilities for data access.
 * - Supports standard CRUD operations (Create, Read, Update, Delete) for {@link WalletEntity}.
 * - Executes queries in a type-safe and flexible manner using the data provided by {@link JpaRepository}.
 *<p/>
 * Notes:
 * - The `WalletEntity` entity is mapped to the "wallets" table in the database.
 * - The primary key for this entity is of type {@link UUID}.
 */
public interface SpringDataWalletRepository extends JpaRepository<WalletEntity, UUID> {
}
