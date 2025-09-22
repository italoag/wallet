package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DeleteWalletUseCase is responsible for soft-deleting a wallet.
 * Deleted wallets are hidden from normal operations but retained for audit purposes.
 * <p/>
 * Business Rules:
 * - Wallet must exist
 * - Wallet must not already be deleted
 * - Wallet balance must be zero before deletion
 * - A reason for deletion must be provided
 * <p/>
 * Publishes:
 * - WalletStatusChangedEvent when wallet status changes to deleted
 * - WalletDeletedEvent when wallet is successfully deleted
 */
public record DeleteWalletUseCase(WalletRepository walletRepository, DomainEventPublisher eventPublisher) {

    /**
     * Soft deletes a wallet, making it unavailable for operations.
     *
     * @param walletId the unique identifier of the wallet to delete
     * @param reason the reason for deleting the wallet
     * @param correlationId a unique identifier used to trace this operation
     * @return the deleted wallet instance
     * @throws IllegalArgumentException if wallet not found or reason is blank
     * @throws IllegalStateException if wallet already deleted or has non-zero balance
     */
    public Wallet deleteWallet(UUID walletId, String reason, String correlationId) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason for deletion must be provided");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        if (wallet.isDeleted()) {
            throw new IllegalStateException("Wallet is already deleted");
        }

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot delete wallet with non-zero balance: " + wallet.getBalance());
        }

        wallet.setCorrelationId(UUID.fromString(correlationId));
        wallet.delete(reason);

        walletRepository.update(wallet);
        wallet.getDomainEvents().forEach(eventPublisher::publish);
        wallet.clearEvents();

        return wallet;
    }
}