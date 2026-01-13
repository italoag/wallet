package dev.bloco.wallet.hub.infra.provider.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Component responsible for processing unsent outbox events and streaming them
 * to an external system. The {@code OutboxWorker} periodically checks for
 * events stored in the outbox, sends them using a messaging service, and marks
 * them as sent upon successful transmission.
 *<p/>
 * This class is designed to implement an eventual consistency pattern, enabling
 * asynchronous communication between different parts of a distributed system.
 * It relies on {@link OutboxService} to manage outbox events and on {@link StreamBridge}
 * to send payloads to the appropriate message channels.
 *<p/>
 * Key Responsibilities:
 * - Periodically retrieve unsent outbox events from the database.
 * - Send each event's payload to a messaging system using the appropriate channel.
 * - Mark events as sent upon successful delivery to the messaging system.
 *<p/>
 * Design Notes:
 * - The {@code processOutbox()} method is annotated with {@code @Scheduled} to ensure
 *   periodic execution at a fixed interval.
 * - Events are fetched and processed in a loop to ensure all pending events are handled.
 * - The stream channel name is dynamically determined based on the event type.
 *<p/>
 * Dependencies:
 * - {@link OutboxService}: Provides operations for retrieving and updating the state
 *   of outbox events.
 * - {@link StreamBridge}: Handles the interaction with a messaging system for sending
 *   event payloads.
 *<p/>
 * Thread Safety:
 * - The {@code processOutbox()} method is expected to be single-threaded by design,
 *   given the periodic scheduling mechanism.
 *<p/>
 * Scheduling Details:
 * - The {@code @Scheduled(fixedRate = 5000)} annotation indicates that the
 *   {@code processOutbox()} method is to be executed every 5000 milliseconds (5 seconds).
 */
@Component
public class OutboxWorker {

    private final OutboxService outboxService;
    private final StreamBridge streamBridge;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @Autowired
    public OutboxWorker(OutboxService outboxService, StreamBridge streamBridge) {
        this(outboxService, streamBridge, null);
    }

    public OutboxWorker(OutboxService outboxService, StreamBridge streamBridge, io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.outboxService = outboxService;
        this.streamBridge = streamBridge;
        this.meterRegistry = meterRegistry;
    }

  /**
   * Periodically processes unsent outbox events and streams them to an external system.
   *<p/>
   * The method retrieves all unsent events from the outbox using {@code outboxService.getUnsentEvents()}.
   * For each event, the corresponding payload is sent to the messaging channel using
   * {@code streamBridge.send()}, where the channel name is resolved from a centralized
   * mapping of event types to binding names. If the send operation is successful,
   * the event is marked as sent using {@code outboxService.markEventAsSent()} to ensure
   * that it is not processed again in future iterations.
   *<p/>
   * This method is designed to run periodically at a fixed rate of 5 seconds, ensuring
   * that new events are promptly processed and sent to the external system.
   *<p/>
   * The execution schedule is managed by the {@code @Scheduled(fixedRate = 5000)}
   * annotation, which configures the method to be invoked at regular intervals.
   *<p/>
   * Key tasks performed:
   * - Fetch unsent events from the database.
   * - Send the event payloads to their respective messaging channels.
   * - Mark successfully processed events as sent to avoid duplication.
   *<p/>
   * Dependencies:
   * - {@code OutboxService} for interacting with the outbox table.
   * - {@code StreamBridge} for sending payloads to external messaging channels.
   *<p/>
   * Thread Safety:
   * This method is intended to execute in a single-threaded context as governed by the
   * scheduling mechanism.
   */
  @Scheduled(fixedRate = 5000)
    public void processOutbox() {
        for (OutboxEvent event : outboxService.getUnsentEvents()) {
            String eventType = event.getEventType();
            java.util.Optional<String> bindingOpt = dev.bloco.wallet.hub.infra.adapter.event.producer.EventBindings.bindingForEventType(eventType);
            if (bindingOpt.isEmpty()) {
                org.slf4j.LoggerFactory.getLogger(OutboxWorker.class).warn("Unknown event type '{}', skipping outbox id={}", eventType, event.getId());
                if (meterRegistry != null) meterRegistry.counter("outbox.unknown.type", "eventType", eventType).increment();
                continue;
            }
            String binding = bindingOpt.get();
            boolean success = streamBridge.send(binding, event.getPayload());
            if (success) {
                outboxService.markEventAsSent(event);
                if (meterRegistry != null) meterRegistry.counter("outbox.sent", "binding", binding).increment();
            } else {
                org.slf4j.LoggerFactory.getLogger(OutboxWorker.class).warn("Failed to send outbox id={} to binding {}", event.getId(), binding);
                if (meterRegistry != null) meterRegistry.counter("outbox.send.failed", "binding", binding).increment();
            }
        }
    }
}
