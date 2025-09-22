package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.wallet.WalletToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing the relationship between wallets and tokens.
 */
public interface WalletTokenRepository {
    WalletToken save(WalletToken walletToken);
    
    Optional<WalletToken> findById(UUID id);
    
    List<WalletToken> findAll();
    
    void delete(UUID id);
    
    List<WalletToken> findByWalletId(UUID walletId);
    
    List<WalletToken> findByTokenId(UUID tokenId);
    
    Optional<WalletToken> findByWalletIdAndTokenId(UUID walletId, UUID tokenId);
    
    List<WalletToken> findEnabledByWalletId(UUID walletId);
    
    List<WalletToken> findVisibleByWalletId(UUID walletId);
    
    boolean existsByWalletIdAndTokenId(UUID walletId, UUID tokenId);
    
    void update(WalletToken walletToken);
}