package dev.bloco.wallet.hub.domain.event.wallet;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import dev.bloco.wallet.hub.domain.model.wallet.WalletStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain event published when a wallet's status changes.
 * This event captures important state transitions for audit and integration purposes.
 */
@Getter
public class WalletStatusChangedEvent extends DomainEvent {
    private final UUID walletId;
    private final WalletStatus oldStatus;
    private final WalletStatus newStatus;
    private final String reason;

    @Builder
    public WalletStatusChangedEvent(UUID walletId, WalletStatus oldStatus, WalletStatus newStatus, String reason, UUID correlationId) {
        super(correlationId);
        this.walletId = walletId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }
}