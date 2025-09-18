package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.transaction.Transaction;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TransactionRepository defines the contract for managing transaction entities
 * within the system. This interface provides methods for persisting, retrieving,
 * and querying transaction data, and supports operations related to transactions
 * occurring on various networks.
 *<p/>
 * A transaction is uniquely identified by a UUID and often associated with a
 * network identifier, addresses, and timestamps. This repository enables precise
 * querying of transactions based on multiple attributes such as network, addresses,
 * status, and time ranges. Furthermore, it facilitates the checking of existence
 * by ID or hash, as well as the removal of transaction entries.
 *<p/>
 * Key Operations:
 * - Persist and retrieve transaction data.
 * - Query transactions by network, addresses, status, and temporal attributes.
 * - Validate the existence of transactions by their ID or hash.
 * - Delete transactions by their unique identifier.
 */
public interface TransactionRepository {
    Transaction save(Transaction transaction);
    
    Optional<Transaction> findById(UUID id);
    
    List<Transaction> findAll();
    
    void delete(UUID id);
    
    Optional<Transaction> findByHash(String hash);
    
    List<Transaction> findByNetworkId(UUID networkId);
    
    List<Transaction> findByFromAddress(String fromAddress);
    
    List<Transaction> findByToAddress(String toAddress);
    
    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByWalletId(UUID walletId);

    List<Transaction> findByTimestampBetween(Instant start, Instant end);
    
    boolean existsById(UUID id);
    
    boolean existsByHash(String hash);
}