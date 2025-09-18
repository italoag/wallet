package dev.bloco.wallet.hub.domain.model.network;

import dev.bloco.wallet.hub.domain.event.network.NetworkStatusChangedEvent;
import dev.bloco.wallet.hub.domain.model.common.AggregateRoot;

import java.util.UUID;

public class Network extends AggregateRoot {
    private String name;
    private final String chainId;
    private String rpcUrl;
    private String explorerUrl;
    private NetworkStatus status;

    public static Network create(
            UUID id,
            String name,
            String chainId,
            String rpcUrl,
            String explorerUrl) {
        
        return new Network(id, name, chainId, rpcUrl, explorerUrl);
    }

    private Network(
            UUID id,
            String name,
            String chainId,
            String rpcUrl,
            String explorerUrl) {
        super(id);
        this.name = name;
        this.chainId = chainId;
        this.rpcUrl = rpcUrl;
        this.explorerUrl = explorerUrl;
        this.status = NetworkStatus.ACTIVE;
    }

    public String getName() {
        return name;
    }

    public String getChainId() {
        return chainId;
    }

    public String getRpcUrl() {
        return rpcUrl;
    }

    public String getExplorerUrl() {
        return explorerUrl;
    }

    public NetworkStatus getStatus() {
        return status;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public void updateExplorerUrl(String explorerUrl) {
        this.explorerUrl = explorerUrl;
    }

    public void activate() {
        if (this.status != NetworkStatus.ACTIVE) {
            NetworkStatus oldStatus = this.status;
            this.status = NetworkStatus.ACTIVE;
            
            registerEvent(new NetworkStatusChangedEvent(
                getId(), oldStatus, this.status, null
            ));
        }
    }

    public void deactivate() {
        if (this.status != NetworkStatus.INACTIVE) {
            NetworkStatus oldStatus = this.status;
            this.status = NetworkStatus.INACTIVE;
            
            registerEvent(new NetworkStatusChangedEvent(
                getId(), oldStatus, this.status, null
            ));
        }
    }

    public void setMaintenance() {
        if (this.status != NetworkStatus.MAINTENANCE) {
            NetworkStatus oldStatus = this.status;
            this.status = NetworkStatus.MAINTENANCE;
            
            registerEvent(new NetworkStatusChangedEvent(
                getId(), oldStatus, this.status, null
            ));
        }
    }

    public boolean isAvailable() {
        return this.status == NetworkStatus.ACTIVE;
    }

    public String getTransactionExplorerUrl(String txHash) {
        return String.format("%s/tx/%s", this.explorerUrl, txHash);
    }

    public String getAddressExplorerUrl(String address) {
        return String.format("%s/address/%s", this.explorerUrl, address);
    }
}