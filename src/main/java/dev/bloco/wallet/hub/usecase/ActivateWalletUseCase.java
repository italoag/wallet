package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.Wallet;

import java.util.UUID;

/**
 * ActivateWalletUseCase is responsible for activating a wallet.
 * An activated wallet can perform all operations including transactions and balance management.
 * <p/>
 * Business Rules:
 * - Wallet must exist
 * - Only inactive or locked wallets can be activated
 * - Deleted wallets cannot be activated
 * <p/>
 * Publishes:
 * - WalletStatusChangedEvent when wallet is successfully activated
 */
public record ActivateWalletUseCase(WalletRepository walletRepository, DomainEventPublisher eventPublisher) {

    /**
     * Activates a wallet, allowing all operations to be performed.
     *
     * @param walletId the unique identifier of the wallet to activate
     * @param correlationId a unique identifier used to trace this operation
     * @return the activated wallet instance
     * @throws IllegalArgumentException if wallet not found
     * @throws IllegalStateException if the wallet is deleted and cannot be activated
     */
    public Wallet activateWallet(UUID walletId, String correlationId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        if (wallet.isDeleted()) {
            throw new IllegalStateException("Deleted wallets cannot be activated");
        }

        wallet.setCorrelationId(UUID.fromString(correlationId));
        wallet.activate();

        walletRepository.update(wallet);
        wallet.getDomainEvents().forEach(eventPublisher::publish);
        wallet.clearEvents();

        return wallet;
    }
}