package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.transaction.TransactionFee;
import dev.bloco.wallet.hub.domain.model.transaction.FeeLevel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing transaction fees.
 */
public interface TransactionFeeRepository {
    TransactionFee save(TransactionFee transactionFee);
    
    Optional<TransactionFee> findById(UUID id);
    
    List<TransactionFee> findByNetworkId(UUID networkId);
    
    List<TransactionFee> findByNetworkIdAndLevel(UUID networkId, FeeLevel level);
    
    Optional<TransactionFee> findLatestByNetworkIdAndLevel(UUID networkId, FeeLevel level);
    
    List<TransactionFee> findAll();
    
    void delete(UUID id);
    
    void deleteOldEstimates(int maxAgeSeconds);
    
    boolean existsById(UUID id);
}