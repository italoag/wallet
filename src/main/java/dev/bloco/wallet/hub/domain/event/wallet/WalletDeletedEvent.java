package dev.bloco.wallet.hub.domain.event.wallet;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain event published when a wallet is deleted (soft delete).
 * This event notifies other system components that the wallet is no longer active.
 */
@Getter
public class WalletDeletedEvent extends DomainEvent {
    private final UUID walletId;
    private final String reason;

    @Builder
    public WalletDeletedEvent(UUID walletId, String reason, UUID correlationId) {
        super(correlationId);
        this.walletId = walletId;
        this.reason = reason;
    }
}