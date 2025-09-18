package dev.bloco.wallet.hub.domain.event.address;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.util.UUID;

public class AddressCreatedEvent extends DomainEvent {
    private final UUID addressId;
    private final UUID walletId;
    private final UUID networkId;
    private final String accountAddress;

    public AddressCreatedEvent(UUID addressId, UUID walletId, UUID networkId, String accountAddress, UUID correlationId) {
        super(correlationId);
        this.addressId = addressId;
        this.walletId = walletId;
        this.networkId = networkId;
        this.accountAddress = accountAddress;
    }

    public UUID getAddressId() {
        return addressId;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public String getAccountAddress() {
        return accountAddress;
    }
}