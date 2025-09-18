package dev.bloco.wallet.hub.domain.event.address;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import dev.bloco.wallet.hub.domain.model.address.AddressStatus;

import java.util.UUID;

public class AddressStatusChangedEvent extends DomainEvent {
    private final UUID addressId;
    private final AddressStatus oldStatus;
    private final AddressStatus newStatus;

    public AddressStatusChangedEvent(UUID addressId, AddressStatus oldStatus, AddressStatus newStatus, UUID correlationId) {
        super(correlationId);
        this.addressId = addressId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public UUID getAddressId() {
        return addressId;
    }

    public AddressStatus getOldStatus() {
        return oldStatus;
    }

    public AddressStatus getNewStatus() {
        return newStatus;
    }
}