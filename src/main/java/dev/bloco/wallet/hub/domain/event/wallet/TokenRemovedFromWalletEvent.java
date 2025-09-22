package dev.bloco.wallet.hub.domain.event.wallet;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain event published when a token is removed from a wallet.
 */
@Getter
public class TokenRemovedFromWalletEvent extends DomainEvent {
    private final UUID walletId;
    private final UUID tokenId;
    private final String reason;

    @Builder
    public TokenRemovedFromWalletEvent(UUID walletId, UUID tokenId, String reason, UUID correlationId) {
        super(correlationId);
        this.walletId = walletId;
        this.tokenId = tokenId;
        this.reason = reason;
    }
}