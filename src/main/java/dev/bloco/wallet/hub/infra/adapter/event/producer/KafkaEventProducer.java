package dev.bloco.wallet.hub.infra.adapter.event.producer;

import dev.bloco.wallet.hub.domain.event.wallet.FundsAddedEvent;
import dev.bloco.wallet.hub.domain.event.wallet.FundsTransferredEvent;
import dev.bloco.wallet.hub.domain.event.wallet.FundsWithdrawnEvent;
import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import dev.bloco.wallet.hub.infra.provider.data.OutboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * KafkaEventProducer is responsible for producing domain events related to wallet operations
 * and sending them to Kafka through the spring-cloud `StreamBridge`. Events are also stored
 * in an outbox for reliable delivery and subsequently sent in batches.
 *<p/>
 * This class supports the production and processing of the following events:
 * - WalletCreatedEvent
 * - FundsAddedEvent
 * - FundsWithdrawnEvent
 * - FundsTransferredEvent
 *<p/>
 * The process includes saving the events into an outbox for transactional safety before sending
 * them to Kafka using a scheduled task.
 */
@Component
@Slf4j
public class KafkaEventProducer implements EventProducer {

    private final OutboxService outboxService;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

  /**
   * Constructs a KafkaEventProducer instance.
   *<p/>
   * This constructor initializes the KafkaEventProducer with the necessary dependencies,
   * including an outbox service for managing event persistence, a stream bridge for sending
   * events to Kafka, and an object mapper for serializing events.
   *
   * @param outboxService the service responsible for storing and retrieving events from the outbox
   * @param streamBridge the Spring Cloud Stream bridge used for sending serialized events to Kafka
   * @param objectMapper the Jackson object mapper used for converting event objects to JSON
   */
    @Autowired
    public KafkaEventProducer(OutboxService outboxService, StreamBridge streamBridge, ObjectMapper objectMapper) {
        this.outboxService = outboxService;
        this.streamBridge = streamBridge;
        // Ensure Java Time (Instant, etc.) is supported during serialization in tests and non-Spring contexts
        objectMapper.findAndRegisterModules();
        this.objectMapper = objectMapper;
    }

  /**
   * Produces a wallet-created event, persisting it into the outbox for further processing.
   *<p/>
   * This method is responsible for saving a wallet creation event with the appropriate
   * event type. The event is serialized and stored in the outbox for eventual delivery
   * to external systems or services.
   *
   * @param event the wallet created event to be produced and saved to the outbox
   */
  @Override
    public void produceWalletCreatedEvent(WalletCreatedEvent event) {
        saveEventToOutbox("walletCreatedEventProducer", event);
    }

  /**
   * Produces an event indicating that funds have been added to a wallet and persists it into the outbox.
   *<p/>
   * This method is responsible for saving a funds-added event with the appropriate event type.
   * The event is serialized and stored in the outbox for eventual delivery to external systems or services.
   *
   * @param event the funds-added event containing information about the wallet, the amount added, and the correlation ID
   */
  @Override
    public void produceFundsAddedEvent(FundsAddedEvent event) {
        saveEventToOutbox("fundsAddedEventProducer", event);
    }

  /**
   * Produces an event indicating that funds have been withdrawn from a wallet
   * and persists it into the outbox for further processing.
   *<p/>
   * This method is responsible for saving a funds-withdrawn event with the
   * appropriate event type. The event is serialized and stored in the outbox
   * for eventual delivery to external systems or services.
   *
   * @param event the funds-withdrawn event containing information about the wallet,
   *              the amount withdrawn, and the correlation ID
   */
  @Override
    public void produceFundsWithdrawnEvent(FundsWithdrawnEvent event) {
        saveEventToOutbox("fundsWithdrawnEventProducer", event);
    }

  /**
   * Produces an event indicating that funds have been transferred between wallets
   * and persists it into the outbox for further processing.
   *<p/>
   * This method is responsible for saving a funds-transferred event with the
   * appropriate event type. The event is serialized and stored in the outbox
   * for eventual delivery to external systems or services.
   *
   * @param event the funds-transferred event containing information about the
   *              source wallet, destination wallet, transferred amount,
   *              and the correlation ID for the operation
   */
  @Override
    public void produceFundsTransferredEvent(FundsTransferredEvent event) {
        saveEventToOutbox("fundsTransferredEventProducer", event);
    }

    private void saveEventToOutbox(String eventType, Object event) {
        try {
            var payload = objectMapper.writeValueAsString(event);
            String correlationId = extractCorrelationId(event);
            outboxService.saveOutboxEvent(eventType, payload, correlationId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event", e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    private String extractCorrelationId(Object event) {
        try {
            if (event instanceof dev.bloco.wallet.hub.domain.event.common.DomainEvent de && de.getCorrelationId() != null) {
                return de.getCorrelationId().toString();
            }
            if (event instanceof FundsAddedEvent fa) {
                return fa.correlationId();
            }
            if (event instanceof FundsWithdrawnEvent fw) {
                return fw.correlationId();
            }
            if (event instanceof FundsTransferredEvent ft) {
                return ft.correlationId();
            }
        } catch (Exception ignored) {
            // ignore and return null
        }
        return null;
    }

  /**
   * Processes unsent events from the outbox and sends them to Kafka using the appropriate stream channel.
   *<p/>
   * This method is scheduled to run at a fixed interval, defined as 5 seconds. It retrieves all unsent
   * events from the outbox, attempts to send each event's payload to Kafka via a dynamically determined
   * channel, and marks successfully sent events as sent in the outbox. Events that fail to send are left
   * unmodified in the outbox for potential retry in later runs.
   *<p/>
   * The event's channel is determined by appending "-out-0" to the event type, ensuring that it is directed
   * to the correct destination in Kafka.
   *<p/>
   * Filtering logic ensures that only events successfully sent to Kafka are marked as sent in the outbox.
   *<p/>
   * Dependencies:
   * - `outboxService` is used to fetch unsent events and update their status.
   * - `streamBridge` is used to send the serialized event payload to Kafka.
   */
}