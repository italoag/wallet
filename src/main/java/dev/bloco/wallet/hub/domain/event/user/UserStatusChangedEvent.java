package dev.bloco.wallet.hub.domain.event.user;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import dev.bloco.wallet.hub.domain.model.user.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain event published when a user's status changes.
 */
@Getter
public class UserStatusChangedEvent extends DomainEvent {
    private final UUID userId;
    private final UserStatus oldStatus;
    private final UserStatus newStatus;
    private final String reason;

    @Builder
    public UserStatusChangedEvent(UUID userId, UserStatus oldStatus, UserStatus newStatus, String reason, UUID correlationId) {
        super(correlationId);
        this.userId = userId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }
}