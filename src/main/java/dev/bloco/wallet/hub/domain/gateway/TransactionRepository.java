package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.Transaction;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    List<Transaction> findByWalletId(UUID walletId);
}
