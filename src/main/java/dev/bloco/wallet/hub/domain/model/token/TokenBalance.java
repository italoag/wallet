package dev.bloco.wallet.hub.domain.model.token;


import dev.bloco.wallet.hub.domain.event.common.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.event.token.TokenBalanceChangedEvent;
import dev.bloco.wallet.hub.domain.model.common.Entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TokenBalance extends Entity {
    private final UUID addressId;
    private final UUID tokenId;
    private BigDecimal balance;
    private Instant lastUpdated;

    public static TokenBalance create(
            UUID id,
            UUID addressId,
            UUID tokenId,
            BigDecimal initialBalance) {
        
        TokenBalance tokenBalance = new TokenBalance(id, addressId, tokenId, initialBalance);
        DomainEventPublisher.publish(new TokenBalanceChangedEvent(id, addressId, tokenId, initialBalance, null));
        return tokenBalance;
    }

    private TokenBalance(
            UUID id,
            UUID addressId,
            UUID tokenId,
            BigDecimal balance) {
        super(id);
        this.addressId = addressId;
        this.tokenId = tokenId;
        this.balance = balance;
        this.lastUpdated = Instant.now();
    }

    public UUID getAddressId() {
        return addressId;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void updateBalance(BigDecimal newBalance) {
        BigDecimal oldBalance = this.balance;
        this.balance = newBalance;
        this.lastUpdated = Instant.now();
        
        DomainEventPublisher.publish(
            new TokenBalanceChangedEvent(getId(), addressId, tokenId, newBalance, null)
        );
    }

    public void addToBalance(BigDecimal amount) {
        updateBalance(this.balance.add(amount));
    }

    public void subtractFromBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        updateBalance(this.balance.subtract(amount));
    }
}