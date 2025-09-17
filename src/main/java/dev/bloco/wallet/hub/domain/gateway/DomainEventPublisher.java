package dev.bloco.wallet.hub.domain.gateway;

public interface DomainEventPublisher {
    void publish(Object event);
}
