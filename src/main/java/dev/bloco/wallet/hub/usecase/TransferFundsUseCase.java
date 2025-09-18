package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.Transaction;
import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.event.FundsTransferredEvent;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The TransferFundsUseCase class is responsible for handling the business logic
 * to transfer funds between two wallets. It ensures that the required steps,
 * such as validation, updating the wallets, saving the transaction, and
 * publishing relevant events, are performed in the correct sequence.
 *<p/>
 * This use case interacts with the following:
 * - WalletRepository for retrieving and updating wallet entities.
 * - TransactionRepository for persisting the transaction details.
 * - DomainEventPublisher for publishing events after a successful transfer.
 *<p/>
 * The process includes:
 * - Validating that both source and destination wallets exist.
 * - Performing the withdrawal from the source wallet and a deposit to the destination wallet.
 * - Persisting the updated states of both wallets in the data store.
 * - Recording the transaction for audit and traceability purposes.
 * - Publishing a FundsTransferredEvent to signify a successful transfer operation within the domain.
 *<p/>
 * Exceptions:
 * - IllegalArgumentException is thrown if either the source or the destination wallet is not found.
 *   Additionally, exceptions may propagate from the Wallet class if the withdrawal or deposit
 *   operation fails due to invalid amounts or insufficient balance.
 *
 * @param walletRepository the repository for managing wallet persistence and updates
 * @param transactionRepository the repository for managing transaction persistence
 * @param eventPublisher the publisher for domain events
 */
public record TransferFundsUseCase(WalletRepository walletRepository, TransactionRepository transactionRepository,
                                   DomainEventPublisher eventPublisher) {

  /**
   * Transfers a specified monetary amount from one wallet to another. The process involves
   * withdrawing funds from the source wallet, adding the same amount to the destination wallet,
   * updating the wallets in the repository, saving the transaction details, and publishing
   * a FundsTransferredEvent to signal that the transfer was successful.
   *
   * @param fromWalletId  the unique identifier of the wallet from which funds will be debited.
   * @param toWalletId    the unique identifier of the wallet to which funds will be credited.
   * @param amount        the monetary amount to be transferred. This must be a positive number.
   * @param correlationId a unique identifier used to correlate the transfer operation across multiple systems.
   * @throws IllegalArgumentException if any of the wallets cannot be found, the amount is not greater than zero,
   *                                  or the source wallet has insufficient funds for the transfer.
   */
  public void transferFunds(UUID fromWalletId, UUID toWalletId, BigDecimal amount, String correlationId) {
    Wallet fromWallet = walletRepository.findById(fromWalletId)
        .orElseThrow(() -> new IllegalArgumentException("From Wallet not found"));
    Wallet toWallet = walletRepository.findById(toWalletId)
        .orElseThrow(() -> new IllegalArgumentException("To Wallet not found"));
    fromWallet.withdrawFunds(amount);
    toWallet.addFunds(amount);
    walletRepository.update(fromWallet);
    walletRepository.update(toWallet);
    transactionRepository.save(new Transaction(fromWallet.getId(), toWallet.getId(), amount, Transaction.TransactionType.TRANSFER));
    FundsTransferredEvent event = new FundsTransferredEvent(fromWallet.getId(), toWallet.getId(), amount, correlationId);
    eventPublisher.publish(event);
  }
}
