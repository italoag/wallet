package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.Wallet;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * RecoverWalletUseCase is responsible for recovering a wallet from backup or
 * seed phrase.
 * This use case handles the wallet recovery process and manages the recovery
 * state.
 * <p/>
 * Business Rules:
 * - User ID must be provided
 * - Recovery method must be specified
 * - A new wallet is created in RECOVERING status
 * - A recovery process is tracked via correlation ID
 * <p/>
 * Publishes:
 * - WalletCreatedEvent when the recovery wallet is created
 * - WalletRecoveryInitiatedEvent when a recovery process starts
 * - WalletStatusChangedEvent when wallet enters recovery state
 */
@RequiredArgsConstructor
public class RecoverWalletUseCase {

    private final WalletRepository walletRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Initiates a wallet recovery process by creating a wallet in recovery state.
     *
     * @param userId         the unique identifier of the user recovering the wallet
     * @param walletName     the name for the recovered wallet
     * @param recoveryMethod the method used for recovery (e.g., "seed_phrase",
     *                       "backup")
     * @param correlationId  a unique identifier used to trace this operation
     * @return the wallet in recovery state
     * @throws IllegalArgumentException if required parameters are missing
     */
    public Wallet recoverWallet(UUID userId, String walletName, String recoveryMethod, String correlationId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }
        if (walletName == null || walletName.trim().isEmpty()) {
            throw new IllegalArgumentException("Wallet name must be provided");
        }
        if (recoveryMethod == null || recoveryMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Recovery method must be specified");
        }

        // Create a new wallet for recovery
        Wallet wallet = Wallet.create(UUID.randomUUID(), walletName, recoveryMethod);
        wallet.setUserId(userId);
        wallet.setCorrelationId(UUID.fromString(correlationId));

        // Set wallet to recovery state
        wallet.initiateRecovery(recoveryMethod);

        // Save and publish events
        walletRepository.save(wallet);
        wallet.getDomainEvents().forEach(eventPublisher::publish);
        wallet.clearEvents();

        return wallet;
    }

    /**
     * Completes wallet recovery by activating the wallet.
     *
     * @param walletId      the unique identifier of the wallet being recovered
     * @param correlationId a unique identifier used to trace this operation
     * @return the activated wallet
     * @throws IllegalArgumentException if wallet not found
     * @throws IllegalStateException    if the wallet is not in recovery state
     */
    public Wallet completeRecovery(UUID walletId, String correlationId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        if (wallet.getStatus() != dev.bloco.wallet.hub.domain.model.wallet.WalletStatus.RECOVERING) {
            throw new IllegalStateException("Wallet is not in recovery state");
        }

        wallet.setCorrelationId(UUID.fromString(correlationId));
        wallet.activate();

        walletRepository.update(wallet);
        wallet.getDomainEvents().forEach(eventPublisher::publish);
        wallet.clearEvents();

        return wallet;
    }
}