package dev.bloco.wallet.hub.domain.model.contract;

import dev.bloco.wallet.hub.domain.event.contract.ContractDeployedEvent;
import dev.bloco.wallet.hub.domain.event.contract.ContractOwnerAddedEvent;
import dev.bloco.wallet.hub.domain.event.contract.ContractOwnerRemovedEvent;
import dev.bloco.wallet.hub.domain.model.common.AggregateRoot;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Contract extends AggregateRoot {
    private final UUID networkId;
    private final String address;
    private final String name;
    private final ContractABI abi;
    private final String bytecode;
    private final String deploymentTxHash;
    private final Instant deploymentTimestamp;
    private final Set<UUID> ownerAddressIds = new HashSet<>();

    public static Contract create(
            UUID id,
            UUID networkId,
            String address,
            String name,
            ContractABI abi,
            String bytecode,
            String deploymentTxHash) {
        
        Contract contract = new Contract(
            id, networkId, address, name, abi, bytecode, deploymentTxHash, Instant.now()
        );
        contract.registerEvent(new ContractDeployedEvent(id, networkId, address, deploymentTxHash, null));
        return contract;
    }

    private Contract(
            UUID id,
            UUID networkId,
            String address,
            String name,
            ContractABI abi,
            String bytecode,
            String deploymentTxHash,
            Instant deploymentTimestamp) {
        super(id);
        this.networkId = networkId;
        this.address = address;
        this.name = name;
        this.abi = abi;
        this.bytecode = bytecode;
        this.deploymentTxHash = deploymentTxHash;
        this.deploymentTimestamp = deploymentTimestamp;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public ContractABI getAbi() {
        return abi;
    }

    public String getBytecode() {
        return bytecode;
    }

    public String getDeploymentTxHash() {
        return deploymentTxHash;
    }

    public Instant getDeploymentTimestamp() {
        return deploymentTimestamp;
    }

    public Set<UUID> getOwnerAddressIds() {
        return Collections.unmodifiableSet(ownerAddressIds);
    }

    public void addOwner(UUID addressId) {
        if (ownerAddressIds.add(addressId)) {
            registerEvent(new ContractOwnerAddedEvent(getId(), addressId, null));
        }
    }

    public void removeOwner(UUID addressId) {
        if (ownerAddressIds.remove(addressId)) {
            registerEvent(new ContractOwnerRemovedEvent(getId(), addressId, null));
        }
    }

    public boolean isOwnedBy(UUID addressId) {
        return ownerAddressIds.contains(addressId);
    }

    public String getFunctionSignature(String functionName) {
        return abi.getFunctionSignature(functionName);
    }

    public String getEventSignature(String eventName) {
        return abi.getEventSignature(eventName);
    }
}