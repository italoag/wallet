package dev.bloco.wallet.hub.domain.event.contract;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.util.UUID;

public class ContractOwnerRemovedEvent extends DomainEvent {
    private final UUID contractId;
    private final UUID addressId;

    public ContractOwnerRemovedEvent(UUID contractId, UUID addressId, UUID correlationId) {
        super(correlationId);
        this.contractId = contractId;
        this.addressId = addressId;
    }

    public UUID getContractId() {
        return contractId;
    }

    public UUID getAddressId() {
        return addressId;
    }
}