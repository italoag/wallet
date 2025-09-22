package dev.bloco.wallet.hub.domain.event.wallet;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain event published when a token is added to a wallet.
 */
@Getter
public class TokenAddedToWalletEvent extends DomainEvent {
    private final UUID walletId;
    private final UUID tokenId;
    private final String displayName;

    @Builder
    public TokenAddedToWalletEvent(UUID walletId, UUID tokenId, String displayName, UUID correlationId) {
        super(correlationId);
        this.walletId = walletId;
        this.tokenId = tokenId;
        this.displayName = displayName;
    }
}