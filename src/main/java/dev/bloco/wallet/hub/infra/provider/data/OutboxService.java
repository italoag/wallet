package dev.bloco.wallet.hub.infra.provider.data;

import dev.bloco.wallet.hub.infra.provider.data.repository.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class responsible for managing outbox events to implement an eventual
 * consistency pattern. This service provides operations for saving, updating,
 * and retrieving outbox events from the database.
 *<p/>
 * The primary purpose of this service is to facilitate the persistence and
 * management of events that need to be processed or sent to external systems.
 * It collaborates with {@link OutboxRepository} to interact with the database layer
 * and performs transactional operations where necessary.
 *<p/>
 * Responsibilities:
 * - Persist new outbox events to the database.
 * - Retrieve events that have not been marked as sent.
 * - Mark events as sent after successful processing.
 *<p/>
 * Design Notes:
 * - Transactions are applied to ensure consistency for save and update operations.
 * - The unsent events are fetched based on the "sent" state from the repository.
 *<p/>
 * Dependencies:
 * - The {@link OutboxRepository} interface is used to handle database interactions.
 */
@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;

    public OutboxService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

  /**
   * Persists a new outbox event to the database with the specified event type, payload,
   * and correlation ID. This method is transactional to ensure the consistency of the
   * operation, meaning all changes are committed or rolled back as a single unit.
   *
   * @param eventType the type or category of the event being recorded must not be null
   * @param payload the data associated with the event must not be null
   * @param correlationId an optional identifier to associate the event with other records or workflows can be null
   */
    @Transactional
    public void saveOutboxEvent(String eventType, String payload, String correlationId) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setCorrelationId(correlationId);
        outboxRepository.save(event);
    }

  /**
   * Marks the specified outbox event as sent by updating its status and persisting the change
   * to the database. This method ensures that the event is updated within a transactional
   * context to maintain consistency.
   *
   * @param event the outbox event to be marked as sent, must not be null
   */
  @Transactional
    public void markEventAsSent(OutboxEvent event) {
        event.setSent(true);
        outboxRepository.save(event);
    }

  /**
   * Retrieves a list of outbox events that have not yet been marked as sent.
   * <p/>
   * This method queries the database through the {@code OutboxRepository}
   * to fetch all events where the "sent" field is set to {@code false}.
   * The returned list represents events that are pending processing and
   * require further action.
   *
   * @return a list of {@code OutboxEvent} objects that are unsent; if no events are unsent, returns an empty list
   */
  public List<OutboxEvent> getUnsentEvents() {
        return outboxRepository.findBySentFalse();
    }
}
