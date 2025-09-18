package dev.bloco.wallet.hub.domain.event.network;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.util.UUID;

public class NetworkAddedEvent extends DomainEvent {
    private final UUID networkId;
    private final String chainId;
    private final String networkName;

    public NetworkAddedEvent(UUID networkId, String chainId, String networkName, UUID correlationId) {
        super(correlationId);
        this.networkId = networkId;
        this.chainId = chainId;
        this.networkName = networkName;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public String getChainId() {
        return chainId;
    }

    public String getNetworkName() {
        return networkName;
    }
}