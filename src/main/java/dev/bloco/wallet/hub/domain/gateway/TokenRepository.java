package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.domain.model.token.TokenType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository {
    Token save(Token token);
    
    Optional<Token> findById(UUID id);
    
    List<Token> findAll();
    
    void delete(UUID id);
    
    List<Token> findByNetworkId(UUID networkId);
    
    Optional<Token> findByNetworkIdAndContractAddress(UUID networkId, String contractAddress);
    
    List<Token> findByType(TokenType type);
    
    List<Token> findBySymbol(String symbol);
    
    boolean existsById(UUID id);
}