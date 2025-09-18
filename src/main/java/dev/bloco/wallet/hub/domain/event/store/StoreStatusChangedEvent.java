package dev.bloco.wallet.hub.domain.event.store;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import dev.bloco.wallet.hub.domain.model.store.StoreStatus;

import java.util.UUID;

public class StoreStatusChangedEvent extends DomainEvent {
    private final UUID storeId;
    private final StoreStatus oldStatus;
    private final StoreStatus newStatus;

    public StoreStatusChangedEvent(UUID storeId, StoreStatus oldStatus, StoreStatus newStatus, UUID correlationId) {
        super(correlationId);
        this.storeId = storeId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public StoreStatus getOldStatus() {
        return oldStatus;
    }

    public StoreStatus getNewStatus() {
        return newStatus;
    }
}