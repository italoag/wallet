package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link OutboxEvent} entities. Extends the
 * {@link JpaRepository} to provide basic CRUD operations and additional query
 * capabilities specific to the application.
 * <p/>
 * This interface provides functionality to interact with the database for
 * managing the persistence of {@link OutboxEvent} objects. It is primarily used
 * for
 * retrieving, creating, updating, and deleting outbox events in support of
 * eventual
 * consistency patterns.
 * <p/>
 * Key Responsibilities:
 * - Leverage Spring Data JPA to interact with the database.
 * - Provide methods for querying unsent outbox events based on the "sent"
 * state.
 * <p/>
 * Notes:
 * - The `OutboxEvent` entity is mapped to a database table named "outbox".
 * - The `id` property serves as the primary key for the entity.
 * - Only records with the `sent` field set to `false` are retrieved by the
 * provided custom query.
 */
@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findBySentFalse();
}