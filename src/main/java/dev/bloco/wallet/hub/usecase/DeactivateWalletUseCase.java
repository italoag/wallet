package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.Wallet;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * DeactivateWalletUseCase is responsible for deactivating a wallet.
 * A deactivated wallet has restricted operations but can be reactivated later.
 * <p/>
 * Business Rules:
 * - Wallet must exist
 * - Only active wallets can be deactivated
 * - Deleted wallets cannot be deactivated
 * <p/>
 * Publishes:
 * - WalletStatusChangedEvent when wallet is successfully deactivated
 */
@RequiredArgsConstructor
public class DeactivateWalletUseCase {

    private final WalletRepository walletRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Deactivates a wallet, restricting operations.
     *
     * @param walletId      the unique identifier of the wallet to deactivate
     * @param correlationId a unique identifier used to trace this operation
     * @return the deactivated wallet instance
     * @throws IllegalArgumentException if wallet not found
     * @throws IllegalStateException    if wallet is deleted
     */
    public Wallet deactivateWallet(UUID walletId, String correlationId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        if (wallet.isDeleted()) {
            throw new IllegalStateException("Deleted wallets cannot be deactivated");
        }

        wallet.setCorrelationId(UUID.fromString(correlationId));
        wallet.deactivate();

        walletRepository.update(wallet);
        wallet.getDomainEvents().forEach(eventPublisher::publish);
        wallet.clearEvents();

        return wallet;
    }
}