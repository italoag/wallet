package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link UserEntity} entity.
 *<p/>
 * This interface is responsible for providing CRUD operations and
 * query mechanisms for {@link UserEntity} objects in the persistence layer.
 * By extending {@link JpaRepository}, it inherits several methods for working
 * with UserEntity persistence, including methods for saving, deleting, and
 * finding entities.
 *<p/>
 * Key Features:
 * - Automatically provides implementations for common database operations
 *   such as saving, deleting, and retrieving entities by their primary key.
 * - Uses {@link UserEntity} as the entity type and {@link UUID} as the primary
 *   key type.
 * - Leverages Spring Data JPA's capabilities for query generation and
 *   customization.
 *<p/>
 * Notes:
 * - The {@link UserEntity} class is mapped to the "users" table in the database.
 * - This interface serves as the primary mechanism for database interactions
 *   for user-related data.
 *<p/>
 * Intended Usage:
 * - To be used in service layer or other repository implementations
 *   for performing user-related persistence operations.
 */
public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {
}
