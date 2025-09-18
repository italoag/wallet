package dev.bloco.wallet.hub.domain.event.contract;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.util.UUID;

public class ContractDeployedEvent extends DomainEvent {
    private final UUID contractId;
    private final UUID networkId;
    private final String contractAddress;
    private final String deploymentTransactionHash;

    public ContractDeployedEvent(UUID contractId, UUID networkId, String contractAddress,
                                 String deploymentTransactionHash, UUID correlationId) {
        super(correlationId);
        this.contractId = contractId;
        this.networkId = networkId;
        this.contractAddress = contractAddress;
        this.deploymentTransactionHash = deploymentTransactionHash;
    }

    public UUID getContractId() {
        return contractId;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String getDeploymentTransactionHash() {
        return deploymentTransactionHash;
    }
}