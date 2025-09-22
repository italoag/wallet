package dev.bloco.wallet.hub.domain.event.wallet;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain event published when a wallet recovery process is initiated.
 * This event signals the start of wallet recovery from seed phrase or backup.
 */
@Getter
public class WalletRecoveryInitiatedEvent extends DomainEvent {
    private final UUID walletId;
    private final UUID userId;
    private final String recoveryMethod;

    @Builder
    public WalletRecoveryInitiatedEvent(UUID walletId, UUID userId, String recoveryMethod, UUID correlationId) {
        super(correlationId);
        this.walletId = walletId;
        this.userId = userId;
        this.recoveryMethod = recoveryMethod;
    }
}