package dev.bloco.wallet.hub.domain.model;

import com.blocotech.common.domain.AggregateRoot;
import com.blocotech.vault.domain.event.VaultCreatedEvent;
import com.blocotech.vault.domain.event.VaultStatusChangedEvent;

import java.util.UUID;

public class Vault extends AggregateRoot {
    private String name;
    private VaultType type;
    private VaultConfiguration configuration;
    private VaultStatus status;

    public static Vault create(
            UUID id,
            String name,
            VaultType type,
            VaultConfiguration configuration) {
        
        Vault vault = new Vault(id, name, type, configuration);
        vault.registerEvent(new VaultCreatedEvent(id, type));
        return vault;
    }

    private Vault(
            UUID id,
            String name,
            VaultType type,
            VaultConfiguration configuration) {
        super(id);
        this.name = name;
        this.type = type;
        this.configuration = configuration;
        this.status = VaultStatus.ACTIVE;
    }

    public String getName() {
        return name;
    }

    public VaultType getType() {
        return type;
    }

    public VaultConfiguration getConfiguration() {
        return configuration;
    }

    public VaultStatus getStatus() {
        return status;
    }

    public void updateConfiguration(VaultConfiguration configuration) {
        this.configuration = configuration;
    }

    public void activate() {
        if (this.status != VaultStatus.ACTIVE) {
            VaultStatus oldStatus = this.status;
            this.status = VaultStatus.ACTIVE;
            registerEvent(new VaultStatusChangedEvent(getId(), oldStatus, this.status));
        }
    }

    public void deactivate() {
        if (this.status != VaultStatus.INACTIVE) {
            VaultStatus oldStatus = this.status;
            this.status = VaultStatus.INACTIVE;
            registerEvent(new VaultStatusChangedEvent(getId(), oldStatus, this.status));
        }
    }

    public boolean isAvailable() {
        return this.status == VaultStatus.ACTIVE;
    }

    public KeyGenerationResult generateKeyPair(String path) {
        if (!isAvailable()) {
            throw new IllegalStateException("Vault is not available");
        }
        
        // This would be implemented according to the specific vault type
        // In a real implementation, this would call an appropriate service
        return new KeyGenerationResult("generated-public-key", "generated-key-id");
    }

    public byte[] sign(String keyId, byte[] data) {
        if (!isAvailable()) {
            throw new IllegalStateException("Vault is not available");
        }
        
        // This would be implemented according to the specific vault type
        // In a real implementation, this would call an appropriate service
        return "signed-data".getBytes();
    }
}