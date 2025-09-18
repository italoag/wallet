package dev.bloco.wallet.hub.domain.event.vault;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import dev.bloco.wallet.hub.domain.model.vault.VaultType;

import java.util.UUID;

public class VaultCreatedEvent extends DomainEvent {
    private final UUID vaultId;
    private final VaultType vaultType;

    public VaultCreatedEvent(UUID vaultId, VaultType vaultType, UUID correlationId) {
        super(correlationId);
        this.vaultId = vaultId;
        this.vaultType = vaultType;
    }

    public UUID getVaultId() {
        return vaultId;
    }

    public VaultType getVaultType() {
        return vaultType;
    }
}