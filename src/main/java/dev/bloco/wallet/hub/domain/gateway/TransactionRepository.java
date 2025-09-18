package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.Transaction;

import java.util.List;
import java.util.UUID;

/**
 * TransactionRepository defines the interface for managing transactions in the system.
 * Implementations of this interface handle the persistence and retrieval of transaction data.
 *<p/>
 * A transaction represents a financial operation, such as a deposit, withdrawal, or transfer,
 * between wallets. The repository provides methods to save a transaction and fetch transactions
 * based on a wallet's identifier.
 */
public interface TransactionRepository {
    Transaction save(Transaction transaction);
    List<Transaction> findByWalletId(UUID walletId);
}
