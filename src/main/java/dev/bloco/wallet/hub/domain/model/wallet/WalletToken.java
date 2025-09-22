package dev.bloco.wallet.hub.domain.model.wallet;

import dev.bloco.wallet.hub.domain.model.common.Entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents the relationship between a wallet and a token.
 * This entity tracks which tokens are supported and managed by each wallet.
 */
public class WalletToken extends Entity {
    private final UUID walletId;
    private final UUID tokenId;
    private final Instant addedAt;
    private boolean isEnabled;
    private String displayName;
    private boolean isVisible;

    public static WalletToken create(UUID id, UUID walletId, UUID tokenId) {
        return new WalletToken(id, walletId, tokenId);
    }

    public static WalletToken create(UUID id, UUID walletId, UUID tokenId, String displayName) {
        WalletToken walletToken = new WalletToken(id, walletId, tokenId);
        walletToken.displayName = displayName;
        return walletToken;
    }

    private WalletToken(UUID id, UUID walletId, UUID tokenId) {
        super(id);
        this.walletId = walletId;
        this.tokenId = tokenId;
        this.addedAt = Instant.now();
        this.isEnabled = true;
        this.isVisible = true;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void show() {
        this.isVisible = true;
    }

    public void hide() {
        this.isVisible = false;
    }
}