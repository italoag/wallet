package dev.bloco.wallet.hub.domain.event.vault;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.util.UUID;

public class KeyPairGeneratedEvent extends DomainEvent {
    private final UUID vaultId;
    private final String keyId;
    private final String publicKey;
    private final String derivationPath;

    public KeyPairGeneratedEvent(UUID vaultId, String keyId, String publicKey, String derivationPath, UUID correlationId) {
        super(correlationId);
        this.vaultId = vaultId;
        this.keyId = keyId;
        this.publicKey = publicKey;
        this.derivationPath = derivationPath;
    }

    public UUID getVaultId() {
        return vaultId;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getDerivationPath() {
        return derivationPath;
    }
}