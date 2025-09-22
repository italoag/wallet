package dev.bloco.wallet.hub.domain.event.user;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain event published when a user's profile is updated.
 */
@Getter
public class UserProfileUpdatedEvent extends DomainEvent {
    private final UUID userId;
    private final String newName;
    private final String newEmail;

    @Builder
    public UserProfileUpdatedEvent(UUID userId, String newName, String newEmail, UUID correlationId) {
        super(correlationId);
        this.userId = userId;
        this.newName = newName;
        this.newEmail = newEmail;
    }
}