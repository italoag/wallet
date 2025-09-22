package dev.bloco.wallet.hub.domain.event.user;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain event published when a new user is created.
 */
@Getter
public class UserCreatedEvent extends DomainEvent {
    private final UUID userId;
    private final String name;
    private final String email;

    @Builder
    public UserCreatedEvent(UUID userId, String name, String email, UUID correlationId) {
        super(correlationId);
        this.userId = userId;
        this.name = name;
        this.email = email;
    }
}