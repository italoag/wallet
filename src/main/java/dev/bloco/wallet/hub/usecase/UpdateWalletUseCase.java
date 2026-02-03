package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.Wallet;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * UpdateWalletUseCase is responsible for updating wallet information.
 * It allows modification of wallet metadata such as name and description.
 * <p/>
 * Business Rules:
 * - Wallet must exist
 * - Wallet must be active to be updated
 * - At least one field (name or description) must be provided
 * <p/>
 * Publishes:
 * - WalletUpdatedEvent when wallet information is successfully updated
 */
@RequiredArgsConstructor
public class UpdateWalletUseCase {

    private final WalletRepository walletRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Updates wallet information such as name and description.
     *
     * @param walletId      the unique identifier of the wallet to update
     * @param name          the new name for the wallet (optional, can be null to
     *                      keep current)
     * @param description   the new description for the wallet (optional, can be
     *                      null to keep current)
     * @param correlationId a unique identifier used to trace this operation
     * @return the updated wallet instance
     * @throws IllegalArgumentException if wallet not found or both name and
     *                                  description are null
     * @throws IllegalStateException    if wallet is not active
     */
    public Wallet updateWallet(UUID walletId, String name, String description, String correlationId) {
        if (name == null && description == null) {
            throw new IllegalArgumentException("At least one field (name or description) must be provided");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        wallet.validateOperationAllowed();

        String newName = name != null ? name : wallet.getName();
        String newDescription = description != null ? description : wallet.getDescription();

        wallet.setCorrelationId(UUID.fromString(correlationId));
        wallet.updateInfo(newName, newDescription);

        walletRepository.update(wallet);
        wallet.getDomainEvents().forEach(eventPublisher::publish);
        wallet.clearEvents();

        return wallet;
    }
}