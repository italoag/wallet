package dev.bloco.wallet.hub.domain.event.common;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class DomainEvent {
    private final UUID eventId;
    private final Instant occurredOn;
    private final UUID correlationId;

    protected DomainEvent(UUID correlationId) {
      this.correlationId = correlationId;
      this.eventId = UUID.randomUUID();
      this.occurredOn = Instant.now();
    }
}