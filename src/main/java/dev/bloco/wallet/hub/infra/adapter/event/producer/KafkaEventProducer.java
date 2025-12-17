package dev.bloco.wallet.hub.infra.adapter.event.producer;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.bloco.wallet.hub.domain.event.wallet.FundsAddedEvent;
import dev.bloco.wallet.hub.domain.event.wallet.FundsTransferredEvent;
import dev.bloco.wallet.hub.domain.event.wallet.FundsWithdrawnEvent;
import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import dev.bloco.wallet.hub.infra.adapter.tracing.propagation.CloudEventTracePropagator;
import dev.bloco.wallet.hub.infra.provider.data.OutboxService;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * KafkaEventProducer is responsible for producing domain events related to wallet operations
 * and sending them to Kafka through spring-cloud StreamBridge with W3C Trace Context propagation.
 * 
 * <p>Events are stored in an outbox for reliable delivery and subsequently sent in batches
 * with trace context injected as CloudEvents 1.0 extensions.</p>
 *
 * <p>This class supports the production and processing of the following events:</p>
 * <ul>
 *   <li>WalletCreatedEvent</li>
 *   <li>FundsAddedEvent</li>
 *   <li>FundsWithdrawnEvent</li>
 *   <li>FundsTransferredEvent</li>
 * </ul>
 *
 * <p>The process includes:</p>
 * <ol>
 *   <li>Saving events into an outbox for transactional safety</li>
 *   <li>Processing outbox events on a schedule (every 5 seconds)</li>
 *   <li>Wrapping events as CloudEvents 1.0</li>
 *   <li>Injecting W3C Trace Context via CloudEventTracePropagator</li>
 *   <li>Sending enriched CloudEvents to Kafka</li>
 *   <li>Marking successfully sent events</li>
 * </ol>
 */
@Component
@Slf4j
public class KafkaEventProducer implements EventProducer {

    private final OutboxService outboxService;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;
    private final CloudEventTracePropagator tracePropagator;

  /**
   * Constructs a KafkaEventProducer instance.
   *
   * <p>This constructor initializes the KafkaEventProducer with the necessary dependencies:</p>
   * <ul>
   *   <li>Outbox service for managing event persistence</li>
   *   <li>Stream bridge for sending events to Kafka</li>
   *   <li>Object mapper for serializing events</li>
   *   <li>Trace propagator for injecting W3C Trace Context</li>
   * </ul>
   *
   * @param outboxService the service responsible for storing and retrieving events from the outbox
   * @param streamBridge the Spring Cloud Stream bridge used for sending CloudEvents to Kafka
   * @param objectMapper the Jackson object mapper used for converting event objects to JSON
   * @param tracePropagator the CloudEvent trace propagator for W3C Trace Context injection
   */
    @Autowired
    public KafkaEventProducer(OutboxService outboxService, StreamBridge streamBridge, 
                              ObjectMapper objectMapper, CloudEventTracePropagator tracePropagator) {
        this.outboxService = outboxService;
        this.streamBridge = streamBridge;
        // Ensure Java Time (Instant, etc.) is supported during serialization in tests and non-Spring contexts
        objectMapper.findAndRegisterModules();
        this.objectMapper = objectMapper;
        this.tracePropagator = tracePropagator;
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
            outboxService.saveOutboxEvent(eventType, payload, null);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event", e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

  /**
   * Processes unsent events from the outbox and sends them to Kafka with W3C Trace Context.
   *
   * <p>This method is scheduled to run at a fixed interval of 5 seconds. It:</p>
   * <ol>
   *   <li>Retrieves all unsent events from the outbox</li>
   *   <li>Wraps each event as a CloudEvent 1.0</li>
   *   <li>Injects W3C Trace Context 1.0 via CloudEventTracePropagator</li>
   *   <li>Sends the enriched CloudEvent to Kafka via StreamBridge</li>
   *   <li>Marks successfully sent events as sent in the outbox</li>
   * </ol>
   *
   * <p>The CloudEvent structure includes:</p>
   * <ul>
   *   <li>Standard CloudEvents attributes (id, type, source, datacontenttype)</li>
   *   <li>Event payload as data</li>
   *   <li>W3C Trace Context as extensions (traceparent, tracestate)</li>
   * </ul>
   *
   * <p>The event's Kafka channel is determined by appending "-out-0" to the event type.</p>
   *
   * <p>Dependencies:</p>
   * <ul>
   *   <li>{@code outboxService} - fetches unsent events and updates their status</li>
   *   <li>{@code streamBridge} - sends CloudEvents to Kafka</li>
   *   <li>{@code tracePropagator} - injects W3C Trace Context into CloudEvents</li>
   * </ul>
   */
  @Scheduled(fixedRate = 5000)
    public void processOutbox() {
        var unsentEvents = outboxService.getUnsentEvents();
        
        unsentEvents.forEach(event -> {
            try {
                // Wrap as CloudEvent 1.0
                CloudEvent cloudEvent = CloudEventBuilder.v1()
                        .withId(event.getId().toString())
                        .withType(event.getEventType())
                        .withSource(URI.create("/wallet-hub"))
                        .withDataContentType("application/json")
                        .withData(event.getPayload().getBytes())
                        .build();
                
                // Inject W3C Trace Context 1.0 via CloudEvents extensions
                CloudEvent enrichedEvent = tracePropagator.injectTraceContext(cloudEvent);
                
                // Send to Kafka with appropriate channel binding
                String channel = event.getEventType() + "-out-0";
                boolean sent = streamBridge.send(channel, enrichedEvent);
                
                if (sent) {
                    outboxService.markEventAsSent(event);
                    log.debug("Sent CloudEvent with trace context [type={}, id={}, channel={}]",
                             event.getEventType(), event.getId(), channel);
                } else {
                    log.warn("Failed to send CloudEvent [type={}, id={}], will retry",
                            event.getEventType(), event.getId());
                }
                
            } catch (Exception e) {
                log.error("Error processing outbox event [type={}, id={}]: {}",
                         event.getEventType(), event.getId(), e.getMessage(), e);
            }
        });
    }
}