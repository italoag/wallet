package dev.bloco.wallet.hub.domain.event.network;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain event published when a new network is created.
 */
@Getter
public class NetworkCreatedEvent extends DomainEvent {
    private final UUID networkId;
    private final String name;
    private final String chainId;

    @Builder
    public NetworkCreatedEvent(UUID networkId, String name, String chainId, UUID correlationId) {
        super(correlationId);
        this.networkId = networkId;
        this.name = name;
        this.chainId = chainId;
    }
}