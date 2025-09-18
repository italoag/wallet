package dev.bloco.wallet.hub.domain.event.transaction;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionConfirmedEvent extends DomainEvent {
    private final UUID transactionId;
    private final long blockNumber;
    private final String blockHash;
    private final BigDecimal gasUsed;

    public TransactionConfirmedEvent(UUID transactionId, long blockNumber, String blockHash, BigDecimal gasUsed, UUID correlationId) {
        super(correlationId);
        this.transactionId = transactionId;
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.gasUsed = gasUsed;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public BigDecimal getGasUsed() {
        return gasUsed;
    }
}