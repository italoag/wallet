package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.token.TokenBalance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenBalanceRepository {
    TokenBalance save(TokenBalance tokenBalance);
    
    Optional<TokenBalance> findById(UUID id);
    
    List<TokenBalance> findAll();
    
    void delete(UUID id);
    
    List<TokenBalance> findByAddressId(UUID addressId);
    
    List<TokenBalance> findByTokenId(UUID tokenId);
    
    Optional<TokenBalance> findByAddressIdAndTokenId(UUID addressId, UUID tokenId);
    
    boolean existsById(UUID id);
}