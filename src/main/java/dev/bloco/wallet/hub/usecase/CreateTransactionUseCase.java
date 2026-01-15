package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.transaction.TransactionCreatedEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.model.transaction.Transaction;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionHash;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use case to create and persist a blockchain transaction in the system.
 * It also publishes a TransactionCreatedEvent for downstream processing.
 */
public record CreateTransactionUseCase(TransactionRepository transactionRepository,
                                    DomainEventPublisher eventPublisher) {

    /**
     * Creates a transaction, persists it and publishes a TransactionCreatedEvent.
     *
     * @param networkId   the network where the transaction occurs
     * @param hash        the transaction hash
     * @param fromAddress the sender address
     * @param toAddress   the recipient address
     * @param value       the value being transferred
     * @param data        optional data payload
     * @param correlationId correlation id (UUID string) used to correlate this operation
     * @return the persisted Transaction aggregate
     */
    public Transaction createTransaction(UUID networkId,
                                         String hash,
                                         String fromAddress,
                                         String toAddress,
                                         BigDecimal value,
                                         String data,
                                         String correlationId) {
        UUID id = UUID.randomUUID();
        Transaction tx = Transaction.create(id, networkId, new TransactionHash(hash), fromAddress, toAddress, value, data);
        Transaction persisted = transactionRepository.save(tx);
        UUID corr = correlationId != null ? UUID.fromString(correlationId) : null;
        eventPublisher.publish(new TransactionCreatedEvent(id, networkId, hash, fromAddress, toAddress, corr));
        return persisted;
    }
}
