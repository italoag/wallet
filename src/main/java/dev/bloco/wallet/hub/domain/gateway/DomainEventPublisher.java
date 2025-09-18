package dev.bloco.wallet.hub.domain.gateway;

/**
 * Represents a publisher for domain events in the system. Implementations of this interface
 * handle the mechanism to publish events, enabling decoupled communication between different
 * parts of the application.
 *<p/>
 * A domain event signifies a meaningful business change or occurrence within the domain.
 * Implementations may choose to persist, log, or transmit events to external systems for
 * processing.
 */
public interface DomainEventPublisher {
    void publish(Object event);
}
