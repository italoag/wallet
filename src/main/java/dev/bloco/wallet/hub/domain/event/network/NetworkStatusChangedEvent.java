package dev.bloco.wallet.hub.domain.event.network;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import dev.bloco.wallet.hub.domain.model.network.NetworkStatus;

import java.util.UUID;

public class NetworkStatusChangedEvent extends DomainEvent {
    private final UUID networkId;
    private final NetworkStatus oldStatus;
    private final NetworkStatus newStatus;

    public NetworkStatusChangedEvent(UUID networkId, NetworkStatus oldStatus, NetworkStatus newStatus, UUID correlationId) {
        super(correlationId);
        this.networkId = networkId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public NetworkStatus getOldStatus() {
        return oldStatus;
    }

    public NetworkStatus getNewStatus() {
        return newStatus;
    }
}