package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.transaction.TransactionStatusChangedEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.model.transaction.Transaction;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionStatus;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Use case to fail a pending transaction, persist the new state,
 * and publish a TransactionStatusChangedEvent with the failure reason.
 */
@RequiredArgsConstructor
public class FailTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Fails a transaction and publishes a status changed event with reason.
     *
     * @param transactionId the id of the transaction to fail
     * @param reason        reason for failure
     * @param correlationId correlation id (UUID string) used to correlate this
     *                      operation
     */
    public void fail(UUID transactionId, String reason, String correlationId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        TransactionStatus oldStatus = tx.getStatus();
        tx.fail(reason);
        transactionRepository.save(tx);
        UUID corr = correlationId != null ? UUID.fromString(correlationId) : null;
        eventPublisher
                .publish(new TransactionStatusChangedEvent(transactionId, oldStatus, tx.getStatus(), reason, corr));
    }
}
