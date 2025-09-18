package dev.bloco.wallet.hub.domain.event.wallet;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
public class WalletUpdatedEvent extends DomainEvent {
    private final UUID walletId;
    private final String newName;
    private final String newDescription;
    private UUID correlationId;

    public WalletUpdatedEvent(UUID walletId, String newName, String newDescription, UUID correlationId) {
      super(correlationId);
      this.walletId = walletId;
        this.newName = newName;
        this.newDescription = newDescription;
    }
}