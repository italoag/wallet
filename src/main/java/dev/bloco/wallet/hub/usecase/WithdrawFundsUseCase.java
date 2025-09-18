package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.Transaction;
import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.event.FundsWithdrawnEvent;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use case for processing fund withdrawal operations from a wallet.
 * This class facilitates the withdrawal process by interacting with
 * the wallet repository to fetch and update wallet data, the transaction
 * repository to record the transaction, and the domain event publisher
 * to broadcast events related to fund withdrawal.
 *<p/>
 * The withdrawal process involves multiple steps:
 * - Retrieving the wallet by its unique identifier.
 * - Validating and deducting the specified amount from the wallet balance.
 * - Updating the wallet's new state in the repository.
 * - Recording the withdrawal transaction in the transaction repository.
 * - Publishing a domain event to indicate the successful withdrawal.
 *<p/>
 * This class ensures that all operations are executed in sequence
 * with proper validation and exception handling.
 *
 * @param walletRepository the repository for managing wallet persistence and updates
 * @param transactionRepository the repository for recording transactions
 * @param eventPublisher the publisher for broadcasting domain events
 */
public record WithdrawFundsUseCase(WalletRepository walletRepository, TransactionRepository transactionRepository,
                                   DomainEventPublisher eventPublisher) {

  /**
   * Withdraws a specified amount of funds from a wallet, updates the wallet's balance in the repository,
   * records the withdrawal transaction, and publishes a FundsWithdrawnEvent.
   *
   * @param walletId the unique identifier of the wallet from which funds are to be withdrawn
   * @param amount the amount to withdraw from the wallet; must be greater than zero and less than or equal to the wallet's current balance
   * @param correlationId the unique identifier used to correlate this withdrawal operation, typically for tracking purposes
   * @throws IllegalArgumentException if the wallet cannot be found or the amount is invalid or exceeds the wallet's available balance
   */
  public void withdrawFunds(UUID walletId, BigDecimal amount, String correlationId) {
    Wallet wallet = walletRepository.findById(walletId)
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    wallet.withdrawFunds(amount);
    walletRepository.update(wallet);
    transactionRepository.save(new Transaction(wallet.getId(), null, amount, Transaction.TransactionType.WITHDRAWAL));
    FundsWithdrawnEvent event = new FundsWithdrawnEvent(wallet.getId(), amount, correlationId);
    eventPublisher.publish(event);
  }
}

