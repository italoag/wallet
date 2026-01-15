package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.transaction.TransactionConfirmedEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.model.transaction.Transaction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use case to confirm a pending transaction, persist the new state,
 * and publish a TransactionConfirmedEvent.
 */
public record ConfirmTransactionUseCase(TransactionRepository transactionRepository,
                                     DomainEventPublisher eventPublisher) {

    /**
     * Confirms a transaction and publishes a confirmation event.
     *
     * @param transactionId the id of the transaction to confirm
     * @param blockNumber   the block number including the transaction
     * @param blockHash     the hash of the block including the transaction
     * @param gasUsed       gas used by the transaction
     * @param correlationId correlation id (UUID string) used to correlate this operation
     */
    public void confirm(UUID transactionId,
                        long blockNumber,
                        String blockHash,
                        BigDecimal gasUsed,
                        String correlationId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        tx.confirm(blockNumber, blockHash, gasUsed);
        transactionRepository.save(tx);
        UUID corr = correlationId != null ? UUID.fromString(correlationId) : null;
        eventPublisher.publish(new TransactionConfirmedEvent(transactionId, blockNumber, blockHash, gasUsed, corr));
    }
}
