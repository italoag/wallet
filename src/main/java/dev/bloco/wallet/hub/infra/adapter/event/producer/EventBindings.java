package dev.bloco.wallet.hub.infra.adapter.event.producer;

import java.util.Map;
import java.util.Optional;

/**
 * Centralized constants and lookup for StreamBridge binding names used by event producers.
 * This avoids string coupling and makes it easy to evolve channel names in one place.
 */
public final class EventBindings {

    private EventBindings() {}

    // Binding names (match application.yml bindings)
    public static final String WALLET_CREATED_BINDING = "walletCreatedEventProducer-out-0";
    public static final String FUNDS_ADDED_BINDING = "fundsAddedEventProducer-out-0";
    public static final String FUNDS_WITHDRAWN_BINDING = "fundsWithdrawnEventProducer-out-0";
    public static final String FUNDS_TRANSFERRED_BINDING = "fundsTransferredEventProducer-out-0";

    // Event type (as persisted in OutboxEvent.eventType) -> binding name mapping
    private static final Map<String, String> EVENT_TYPE_TO_BINDING = Map.of(
            "walletCreatedEventProducer", WALLET_CREATED_BINDING,
            "fundsAddedEventProducer", FUNDS_ADDED_BINDING,
            "fundsWithdrawnEventProducer", FUNDS_WITHDRAWN_BINDING,
            "fundsTransferredEventProducer", FUNDS_TRANSFERRED_BINDING
    );

    public static Optional<String> bindingForEventType(String eventType) {
        return Optional.ofNullable(EVENT_TYPE_TO_BINDING.get(eventType));
    }
}
