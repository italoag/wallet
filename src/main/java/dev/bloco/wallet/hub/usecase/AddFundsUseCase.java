package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.wallet.FundsAddedEvent;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.Wallet;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * AddFundsUseCase enables the addition of funds to a specific wallet in the
 * system.
 * It encapsulates the process of validating the wallet, updating the balance
 * and publishing a corresponding domain event.
 * <p/>
 * Responsibilities:
 * - Retrieve the wallet based on its unique identifier.
 * - Add the specified amount to the wallet's balance, ensuring the amount is
 * valid.
 * - Persist the updated wallet state.
 * - Publish a FundsAddedEvent for system-wide or external process handling.
 * <p/>
 * Uses:
 * - WalletRepository for retrieving and updating wallet entities.
 * - DomainEventPublisher for publishing domain events after the operation.
 * <p/>
 * Exceptions:
 * - IllegalArgumentException is thrown if the wallet cannot be found or if
 * an invalid amount is provided.
 */
@RequiredArgsConstructor
public class AddFundsUseCase {

  private final WalletRepository walletRepository;
  private final DomainEventPublisher eventPublisher;

  /**
   * Adds funds to a specified wallet by its unique identifier. This method
   * performs the following operations:
   * - Validates the wallet by its unique identifier and throws an exception if
   * the wallet is not found.
   * - Adds the specified amount to the wallet's balance.
   * - Updates the wallet in the repository with the new balance.
   * - Publishes a FundsAddedEvent to notify other components or systems.
   *
   * @param walletId      the unique identifier of the wallet to which funds will
   *                      be added.
   * @param amount        the monetary value to be added to the wallet. This must
   *                      be greater than zero.
   * @param correlationId a unique identifier to associate this operation with
   *                      other related processes.
   * @throws IllegalArgumentException if the wallet is not found or the amount is
   *                                  invalid.
   */
  public void addFunds(UUID walletId, BigDecimal amount, String correlationId) {
    Wallet wallet = walletRepository.findById(walletId)
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    wallet.addFunds(amount);
    walletRepository.update(wallet);
    FundsAddedEvent event = new FundsAddedEvent(wallet.getId(), amount, correlationId);
    eventPublisher.publish(event);
  }
}
