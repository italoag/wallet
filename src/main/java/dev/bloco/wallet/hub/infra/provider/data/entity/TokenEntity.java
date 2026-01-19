package dev.bloco.wallet.hub.infra.provider.data.entity;

import dev.bloco.wallet.hub.domain.model.token.TokenType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * JPA entity representing a Token in the database.
 */
@Setter
@Getter
@Entity
@Table(name = "tokens", indexes = {
        @Index(name = "idx_network_id", columnList = "network_id"),
        @Index(name = "idx_contract_address", columnList = "contract_address"),
        @Index(name = "idx_symbol", columnList = "symbol"),
        @Index(name = "idx_type", columnList = "type")
})
public class TokenEntity {

    @Id
    private UUID id;

    @Column(name = "network_id", nullable = false)
    private UUID networkId;

    @Column(name = "contract_address", length = 256)
    private String contractAddress;

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "decimals", nullable = false)
    private int decimals;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TokenType type;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

}
