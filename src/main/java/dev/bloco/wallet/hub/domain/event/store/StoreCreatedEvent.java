package dev.bloco.wallet.hub.domain.event.store;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.util.UUID;

public class StoreCreatedEvent extends DomainEvent {
    private final UUID storeId;
    private final UUID vaultId;
    private final String storeName;

    public StoreCreatedEvent(UUID storeId, UUID vaultId, String storeName, UUID correlationId) {
        super(correlationId);
        this.storeId = storeId;
        this.vaultId = vaultId;
        this.storeName = storeName;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public UUID getVaultId() {
        return vaultId;
    }

    public String getStoreName() {
        return storeName;
    }
}