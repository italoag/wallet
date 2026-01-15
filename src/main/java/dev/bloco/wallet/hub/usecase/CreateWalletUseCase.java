package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * CreateWalletUseCase is responsible for creating a new wallet.
 * It encapsulates the process of instantiating a wallet, persisting it to the repository,
 * and publishing a domain event to signal the wallet's creation.
 *<p/>
 * Responsibilities:
 * - Create an instance of a Wallet.
 * - Persist the newly created wallet using the WalletRepository.
 * - Publish a WalletCreatedEvent to notify other system components about the wallet creation.
 *<p/>
 * Uses:
 * - WalletRepository for saving the wallet entity to the persistent storage.
 * - DomainEventPublisher for publishing a domain event to notify other parts of the system.
 */
public record CreateWalletUseCase(WalletRepository walletRepository, DomainEventPublisher eventPublisher) {

  /**
   * Creates a new wallet, persists it, and publishes a wallet creation event.
   *
   * @param userId the unique identifier of the user requesting the wallet (kept for API compatibility)
   * @param correlationId a unique identifier used to trace this operation and related processes
   * @return the newly created wallet instance
   */
  public Wallet createWallet(UUID userId, String correlationId) {
    // Create a wallet with a generated id and default metadata (name/description can be updated later)
    Wallet wallet = Wallet.create(UUID.randomUUID(), "Default Wallet", "");
    walletRepository.save(wallet);
    WalletCreatedEvent event = new WalletCreatedEvent(wallet.getId(), UUID.fromString(correlationId));
    eventPublisher.publish(event);
    return wallet;
  }
}
