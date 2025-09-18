package dev.bloco.wallet.hub.domain.event.transaction;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionStatus;

import java.util.UUID;

public class TransactionStatusChangedEvent extends DomainEvent {
    private final UUID transactionId;
    private final TransactionStatus oldStatus;
    private final TransactionStatus newStatus;
    private final String reason;

    public TransactionStatusChangedEvent(UUID transactionId, TransactionStatus oldStatus,
                                         TransactionStatus newStatus, String reason, UUID correlationId) {
        super(correlationId);
        this.transactionId = transactionId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public TransactionStatus getOldStatus() {
        return oldStatus;
    }

    public TransactionStatus getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }
}