package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.Transaction;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;
import dev.bloco.wallet.hub.infra.provider.mapper.TransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JpaTransactionRepository is an implementation of the TransactionRepository interface.
 * This class provides mechanisms to save and retrieve transaction data using JPA (Java Persistence API).
 *<p/>
 * This implementation uses a combination of a Spring Data JPA repository
 * (SpringDataTransactionRepository) and a mapper (TransactionMapper) to handle
 * the conversion between the domain model (Transaction) and the database entity model
 * (TransactionEntity). The repository acts as a mediator between the domain logic
 * and the persistence layer, ensuring data consistency and transforming data as needed.
 *<p/>
 * Responsibilities:
 * - Persisting transaction data by mapping domain objects to database entities.
 * - Retrieving transaction data from the database and mapping it back to domain objects.
 *<p/>
 * Dependencies:
 * - SpringDataTransactionRepository: Provides direct interaction with the database
 *   via JPA methods for storing and querying transaction entities.
 * - TransactionMapper: Handles the transformation between Transaction domain objects
 *   and TransactionEntity objects.
 *<p/>
 * Methods:
 * - save: Persists a transaction to the database, converting a domain Transaction object
 *   to a TransactionEntity, and returning the saved transaction as a domain object.
 * - findByWalletId: Fetches transactions associated with a specific wallet ID.
 *   Searches for transactions where the given wallet ID is either the source or the
 *   destination wallet, maps the resulting list of entities back to domain objects,
 *   and returns the list.
 */
@Repository
public class JpaTransactionRepository implements TransactionRepository {
    private final SpringDataTransactionRepository springDataTransactionRepository;
    private final TransactionMapper transactionMapper;

    @Autowired
    public JpaTransactionRepository(SpringDataTransactionRepository springDataTransactionRepository, TransactionMapper transactionMapper) {
        this.springDataTransactionRepository = springDataTransactionRepository;
        this.transactionMapper = transactionMapper;
    }

  /**
   * Persists a provided transaction into the database and returns the saved transaction.
   * This method uses a mapper to convert the domain model transaction to its corresponding
   * entity representation for database operations, then maps the saved entity back
   * to the domain model.
   *
   * @param transaction the transaction to be saved, represented as a domain model object.
   *                     It includes details such as wallet IDs, transaction amount, type,
   *                     and timestamp.
   * @return the saved transaction, represented as a domain model object. It includes
   *         the updated state after being persisted into the database.
   */
  @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = transactionMapper.toEntity(transaction);
        return transactionMapper.toDomain(springDataTransactionRepository.save(entity));
    }

  /**
   * Retrieves a list of transactions associated with the given wallet ID.
   * This includes transactions where the wallet is either the source or the destination.
   *
   * @param walletId the unique identifier of the wallet whose transactions are to be retrieved.
   * @return a list of transactions related to the specified wallet, represented as domain model objects.
   */
  @Override
    public List<Transaction> findByWalletId(UUID walletId) {
        return springDataTransactionRepository.findByFromWalletIdOrToWalletId(walletId, walletId).stream()
                .map(transactionMapper::toDomain)
                .collect(Collectors.toList());
    }
}
