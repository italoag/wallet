package dev.bloco.wallet.hub.domain.event.wallet;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.util.UUID;

public class AddressAddedToWalletEvent extends DomainEvent {
    private final UUID walletId;
    private final UUID addressId;

    public AddressAddedToWalletEvent(UUID walletId, UUID addressId, UUID correlationId) {
        super(correlationId);
        this.walletId = walletId;
        this.addressId = addressId;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getAddressId() {
        return addressId;
    }
}