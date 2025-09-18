package dev.bloco.wallet.hub.domain.event.transaction;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.util.UUID;

public class TransactionCreatedEvent extends DomainEvent {
    private final UUID transactionId;
    private final UUID networkId;
    private final String transactionHash;
    private final String fromAddress;
    private final String toAddress;

    public TransactionCreatedEvent(UUID transactionId, UUID networkId, String transactionHash,
                                   String fromAddress, String toAddress, UUID correlationId) {
        super(correlationId);
        this.transactionId = transactionId;
        this.networkId = networkId;
        this.transactionHash = transactionHash;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }
}