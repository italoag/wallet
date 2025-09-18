package dev.bloco.wallet.hub.infra.adapter.event;

import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.infra.provider.data.OutboxEvent;
import dev.bloco.wallet.hub.infra.provider.data.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * OutboxEventPublisher is a component responsible for publishing domain events
 * into an outbox table for eventual processing and delivery. It ensures reliable
 * event handling by persisting events into a database, allowing for later retrieval
 * and processing by an external or downstream system.
 *<p/>
 * This class implements the DomainEventPublisher interface and provides the
 * functionality to serialize domain events and store them as OutboxEvent entities.
 *<p/>
 * Dependencies:
 * - OutboxRepository: Handles the persistence of OutboxEvent entities in the database.
 * - ObjectMapper: Used for serializing event objects into JSON format for storage.
 *<p/>
 * The publish method is transactional, ensuring database consistency during the
 * serialization and persistence process. On a failure, such as a JSON serialization
 * error, a RuntimeException is thrown and logged, preventing the event from being
 * stored with corrupted data.
 */
@Component
@Slf4j
public class OutboxEventPublisher implements DomainEventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

  /**
   * Constructs a new instance of OutboxEventPublisher with the required dependencies.
   *
   * @param outboxRepository the repository used to persist OutboxEvent entities in the database.
   * @param objectMapper the object mapper used for serializing domain events into JSON format.
   */
  @Autowired
    public OutboxEventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

  /**
   * Publishes a domain event by serializing it into JSON format and storing it
   * as an entity in the outbox table for reliable processing and eventual delivery.
   * This method ensures transactional consistency and logs any serialization errors encountered.
   *
   * @param event the domain event to be published. The event must be serializable as it
   *              will be converted into JSON format and persisted in the database.
   *              The event's class name will be used as the event type.
   * @throws RuntimeException if the event fails to serialize into JSON format.
   */
  @Override
    @Transactional
    public void publish(Object event) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setEventType(event.getClass().getSimpleName());
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event", e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
