package dev.bloco.wallet.hub.domain.event.store;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.util.UUID;

public class AddressAddedToStoreEvent extends DomainEvent {
    private final UUID storeId;
    private final UUID addressId;

    public AddressAddedToStoreEvent(UUID storeId, UUID addressId, UUID correlationId) {
        super(correlationId);
        this.storeId = storeId;
        this.addressId = addressId;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public UUID getAddressId() {
        return addressId;
    }
}