package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.event.WalletCreatedEvent;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;


import java.util.UUID;

/**
 * CreateWalletUseCase is responsible for creating a new wallet for a specified user.
 * It encapsulates the process of instantiating a wallet, persisting it to the repository,
 * and publishing a domain event to signal the wallet's creation.
 *<p/>
 * Responsibilities:
 * - Create an instance of a Wallet associated with a specific user.
 * - Persist the newly created wallet using the WalletRepository.
 * - Publish a WalletCreatedEvent to notify other system components about the wallet creation.
 *<p/>
 * Uses:
 * - WalletRepository for saving the wallet entity to the persistent storage.
 * - DomainEventPublisher for publishing a domain event to notify other parts of the system.
 *<p/>
 * Exceptions:
 * - None directly, though exceptions may arise depending on the behavior of the
 *   WalletRepository and DomainEventPublisher implementations.
 *
 * @param walletRepository Repository for wallet data access.
 * @param eventPublisher Publisher for domain events.
 */
public record CreateWalletUseCase(WalletRepository walletRepository, DomainEventPublisher eventPublisher) {

  /**
   * Creates a new wallet for the specified user, persists it, and publishes a wallet creation event.
   * This method involves creating a wallet instance, saving it to the wallet repository, and
   * publishing a domain event to notify other system components about the wallet creation.
   *
   * @param userId the unique identifier of the user for whom the wallet is being created
   * @param correlationId a unique identifier used to trace this operation and related processes
   * @return the newly created wallet instance
   */
  public Wallet createWallet(UUID userId, String correlationId) {
    Wallet wallet = new Wallet(userId);
    walletRepository.save(wallet);
    WalletCreatedEvent event = new WalletCreatedEvent(wallet.getId(), userId, correlationId);
    eventPublisher.publish(event);
    return wallet;
  }
}
