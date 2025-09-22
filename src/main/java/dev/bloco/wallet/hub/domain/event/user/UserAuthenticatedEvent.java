package dev.bloco.wallet.hub.domain.event.user;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain event published when a user successfully authenticates.
 */
@Getter
public class UserAuthenticatedEvent extends DomainEvent {
    private final UUID userId;
    private final String email;
    private final UUID sessionId;
    private final String ipAddress;

    @Builder
    public UserAuthenticatedEvent(UUID userId, String email, UUID sessionId, String ipAddress, UUID correlationId) {
        super(correlationId);
        this.userId = userId;
        this.email = email;
        this.sessionId = sessionId;
        this.ipAddress = ipAddress;
    }
}