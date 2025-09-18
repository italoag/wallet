package dev.bloco.wallet.hub.domain.event.token;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

public class TokenBalanceChangedEvent extends DomainEvent {
    private final UUID tokenBalanceId;
    private final UUID addressId;
    private final UUID tokenId;
    private final BigDecimal newBalance;

    public TokenBalanceChangedEvent(UUID tokenBalanceId, UUID addressId, UUID tokenId, BigDecimal newBalance, UUID correlationId) {
        super(correlationId);
        this.tokenBalanceId = tokenBalanceId;
        this.addressId = addressId;
        this.tokenId = tokenId;
        this.newBalance = newBalance;
    }

    public UUID getTokenBalanceId() {
        return tokenBalanceId;
    }

    public UUID getAddressId() {
        return addressId;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }
}