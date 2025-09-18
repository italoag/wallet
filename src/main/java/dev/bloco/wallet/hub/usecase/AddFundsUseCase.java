package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.Transaction;
import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.event.FundsAddedEvent;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * AddFundsUseCase enables the addition of funds to a specific wallet in the system.
 * It encapsulates the process of validating the wallet, updating the balance, saving
 * the associated transaction, and publishing a corresponding domain event.
 *<p/>
 * Responsibilities:
 * - Retrieve the wallet based on its unique identifier.
 * - Add the specified amount to the wallet's balance, ensuring the amount is valid.
 * - Persist the updated wallet and the associated deposit transaction.
 * - Publish a FundsAddedEvent for system-wide or external process handling.
 *<p/>
 * Uses:
 * - WalletRepository for retrieving and updating wallet entities.
 * - TransactionRepository for persisting transactions.
 * - DomainEventPublisher for publishing domain events after the operation.
 *<p/>
 * Exceptions:
 * - IllegalArgumentException is thrown if the wallet cannot be found or if
 *   an invalid amount is provided.
 *
 * @param walletRepository Repository for wallet data access.
 * @param transactionRepository Repository for transaction data access.
 * @param eventPublisher Publisher for domain events.
 */
public record AddFundsUseCase(WalletRepository walletRepository, TransactionRepository transactionRepository,
                              DomainEventPublisher eventPublisher) {

  /**
   * Adds funds to a specified wallet by its unique identifier. This method performs the following operations:
   * - Validates the wallet by its unique identifier and throws an exception if the wallet is not found.
   * - Adds the specified amount to the wallet's balance.
   * - Updates the wallet in the repository with the new balance.
   * - Saves a transaction record representing the deposit operation.
   * - Publishes a FundsAddedEvent to notify other components or systems.
   *
   * @param walletId the unique identifier of the wallet to which funds will be added.
   * @param amount the monetary value to be added to the wallet. This must be greater than zero.
   * @param correlationId a unique identifier to associate this operation with other related processes.
   * @throws IllegalArgumentException if the wallet is not found or the amount is invalid.
   */
  public void addFunds(UUID walletId, BigDecimal amount, String correlationId) {
    Wallet wallet = walletRepository.findById(walletId)
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    wallet.addFunds(amount);
    walletRepository.update(wallet);
    transactionRepository.save(new Transaction(null, wallet.getId(), amount, Transaction.TransactionType.DEPOSIT));
    FundsAddedEvent event = new FundsAddedEvent(wallet.getId(), amount, correlationId);
    eventPublisher.publish(event);
  }
}
