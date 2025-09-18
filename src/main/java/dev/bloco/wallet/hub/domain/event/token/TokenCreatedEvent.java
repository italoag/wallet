package dev.bloco.wallet.hub.domain.event.token;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import dev.bloco.wallet.hub.domain.model.token.TokenType;

import java.util.UUID;

public class TokenCreatedEvent extends DomainEvent {
    private final UUID tokenId;
    private final UUID networkId;
    private final String contractAddress;
    private final String symbol;
    private final TokenType tokenType;

    public TokenCreatedEvent(UUID tokenId, UUID networkId, String contractAddress, 
                           String symbol, TokenType tokenType, UUID correlationId) {
        super(correlationId);
        this.tokenId = tokenId;
        this.networkId = networkId;
        this.contractAddress = contractAddress;
        this.symbol = symbol;
        this.tokenType = tokenType;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String getSymbol() {
        return symbol;
    }

    public TokenType getTokenType() {
        return tokenType;
    }
}