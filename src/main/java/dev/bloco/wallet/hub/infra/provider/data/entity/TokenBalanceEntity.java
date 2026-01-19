package dev.bloco.wallet.hub.infra.provider.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "token_balances", indexes = {
        @Index(name = "idx_address_id", columnList = "address_id"),
        @Index(name = "idx_token_id", columnList = "token_id"),
        @Index(name = "idx_address_token", columnList = "address_id,token_id", unique = true)
})
public class TokenBalanceEntity {
    @Id
    private UUID id;

    @Column(name = "address_id", nullable = false)
    private UUID addressId;

    @Column(name = "token_id", nullable = false)
    private UUID tokenId;

    @Column(name = "balance", nullable = false, precision = 78, scale = 0)
    private BigDecimal balance;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAddressId() {
        return addressId;
    }

    public void setAddressId(UUID addressId) {
        this.addressId = addressId;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public void setTokenId(UUID tokenId) {
        this.tokenId = tokenId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

}
