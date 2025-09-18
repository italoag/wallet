package dev.bloco.wallet.hub.domain.event.vault;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import dev.bloco.wallet.hub.domain.model.vault.VaultStatus;

import java.util.UUID;

public class VaultStatusChangedEvent extends DomainEvent {
    private final UUID vaultId;
    private final VaultStatus oldStatus;
    private final VaultStatus newStatus;

    public VaultStatusChangedEvent(UUID vaultId, VaultStatus oldStatus, VaultStatus newStatus, UUID correlationId) {
        super(correlationId);
        this.vaultId = vaultId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public UUID getVaultId() {
        return vaultId;
    }

    public VaultStatus getOldStatus() {
        return oldStatus;
    }

    public VaultStatus getNewStatus() {
        return newStatus;
    }
}