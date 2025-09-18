package dev.bloco.wallet.hub.domain.event.contract;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.util.UUID;

public class ContractInteractionEvent extends DomainEvent {
    private final UUID contractId;
    private final UUID originAddressId;
    private final String functionName;
    private final String transactionHash;
    private final boolean succeeded;

    public ContractInteractionEvent(UUID contractId, UUID originAddressId, String functionName,
                                    String transactionHash, boolean succeeded, UUID correlationId) {
        super(correlationId);
        this.contractId = contractId;
        this.originAddressId = originAddressId;
        this.functionName = functionName;
        this.transactionHash = transactionHash;
        this.succeeded = succeeded;
    }

    public UUID getContractId() {
        return contractId;
    }

    public UUID getOriginAddressId() {
        return originAddressId;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public boolean isSucceeded() {
        return succeeded;
    }
}