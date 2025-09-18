package dev.bloco.wallet.hub.domain.model.token;

import dev.bloco.wallet.hub.domain.event.token.TokenCreatedEvent;
import dev.bloco.wallet.hub.domain.model.common.AggregateRoot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class Token extends AggregateRoot {
    private final UUID networkId;
    private final String contractAddress;
    private final String name;
    private final String symbol;
    private final int decimals;
    private final TokenType type;

    public static Token create(
            UUID id,
            UUID networkId,
            String name,
            String symbol,
            int decimals,
            TokenType type,
            String contractAddress) {
        
        Token token = new Token(id, networkId, name, symbol, decimals, type, contractAddress);
        token.registerEvent(new TokenCreatedEvent(id, networkId, contractAddress, symbol, type, null));
        return token;
    }

    private Token(
            UUID id,
            UUID networkId,
            String name,
            String symbol,
            int decimals,
            TokenType type,
            String contractAddress) {
        super(id);
        this.networkId = networkId;
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
        this.type = type;
        this.contractAddress = contractAddress;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDecimals() {
        return decimals;
    }

    public TokenType getType() {
        return type;
    }

    public boolean isNative() {
        return this.type == TokenType.NATIVE;
    }

    public boolean isNFT() {
        return this.type == TokenType.ERC721 || this.type == TokenType.ERC1155;
    }

    public boolean isFungible() {
        return this.type == TokenType.NATIVE || this.type == TokenType.ERC20;
    }

    public String formatAmount(BigDecimal amount) {
        if (isNFT()) {
            return amount.toBigInteger().toString();
        }
        
        return amount.divide(
            BigDecimal.TEN.pow(decimals), 
            decimals, 
            RoundingMode.HALF_DOWN
        ).toPlainString();
    }

    public BigDecimal parseAmount(String formattedAmount) {
        if (isNFT()) {
            return new BigDecimal(formattedAmount);
        }
        
        return new BigDecimal(formattedAmount).multiply(
            BigDecimal.TEN.pow(decimals)
        ).setScale(0, RoundingMode.DOWN);
    }
}