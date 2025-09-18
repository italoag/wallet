package dev.bloco.wallet.hub.domain.event.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DomainEventPublisher {
    private static final ThreadLocal<List<Consumer<DomainEvent>>> SUBSCRIBERS = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PUBLISHING = new ThreadLocal<>();

    public static void subscribe(Consumer<DomainEvent> subscriber) {
        if (PUBLISHING.get() != null && PUBLISHING.get()) {
            throw new IllegalStateException("Cannot subscribe during event publishing");
        }

        List<Consumer<DomainEvent>> subscribers = SUBSCRIBERS.get();
        if (subscribers == null) {
            subscribers = new ArrayList<>();
            SUBSCRIBERS.set(subscribers);
        }
        subscribers.add(subscriber);
    }

    public static void publish(DomainEvent event) {
        if (PUBLISHING.get() != null && PUBLISHING.get()) {
            return;
        }

        List<Consumer<DomainEvent>> subscribers = SUBSCRIBERS.get();
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        try {
            PUBLISHING.set(true);
            subscribers.forEach(subscriber -> subscriber.accept(event));
        } finally {
            PUBLISHING.set(false);
        }
    }

    public static void reset() {
        SUBSCRIBERS.remove();
        PUBLISHING.remove();
    }
}