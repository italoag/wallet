package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletTokenRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.wallet.WalletToken;
import dev.bloco.wallet.hub.domain.event.wallet.TokenRemovedFromWalletEvent;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * RemoveTokenFromWalletUseCase is responsible for removing tokens from wallets.
 * This removes the token from wallet management but doesn't affect existing
 * balances.
 * <p/>
 * Business Rules:
 * - Wallet must exist and be active
 * - Token must be currently added to the wallet
 * - Removal reason should be provided for audit purposes
 * <p/>
 * Publishes:
 * - TokenRemovedFromWalletEvent when token is successfully removed
 */
@RequiredArgsConstructor
public class RemoveTokenFromWalletUseCase {

    private final WalletRepository walletRepository;
    private final WalletTokenRepository walletTokenRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Removes a token from a wallet's managed tokens.
     *
     * @param walletId      the unique identifier of the wallet
     * @param tokenId       the unique identifier of the token to remove
     * @param reason        the reason for removal (for audit purposes)
     * @param correlationId a unique identifier used to trace this operation
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException    if the wallet is not active
     */
    public void removeTokenFromWallet(UUID walletId, UUID tokenId, String reason, String correlationId) {
        // Validate inputs
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must be provided");
        }
        if (tokenId == null) {
            throw new IllegalArgumentException("Token ID must be provided");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason for removal must be provided");
        }

        // Validate wallet exists and is active
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));
        wallet.validateOperationAllowed();

        // Find the wallet-token relationship
        WalletToken walletToken = walletTokenRepository.findByWalletIdAndTokenId(walletId, tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token is not added to this wallet"));

        // Remove the relationship
        walletTokenRepository.delete(walletToken.getId());

        // Publish event
        TokenRemovedFromWalletEvent event = TokenRemovedFromWalletEvent.builder()
                .walletId(walletId)
                .tokenId(tokenId)
                .reason(reason)
                .correlationId(UUID.fromString(correlationId))
                .build();

        eventPublisher.publish(event);
    }

    /**
     * Hides a token from wallet display without completely removing it.
     * The token remains in the wallet but is not visible in the UI.
     *
     * @param walletId      the unique identifier of the wallet
     * @param tokenId       the unique identifier of the token to hide
     * @param correlationId a unique identifier used to trace this operation
     * @return the updated wallet-token relationship
     * @throws IllegalArgumentException if validation fails
     */
    public WalletToken hideTokenFromWallet(UUID walletId, UUID tokenId, String correlationId) {
        // Validate inputs
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must be provided");
        }
        if (tokenId == null) {
            throw new IllegalArgumentException("Token ID must be provided");
        }

        // Validate wallet exists and is active
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));
        wallet.validateOperationAllowed();

        // Find the wallet-token relationship
        WalletToken walletToken = walletTokenRepository.findByWalletIdAndTokenId(walletId, tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token is not added to this wallet"));

        // Hide the token
        walletToken.hide();
        walletTokenRepository.update(walletToken);

        return walletToken;
    }

    /**
     * Shows a previously hidden token in the wallet.
     *
     * @param walletId      the unique identifier of the wallet
     * @param tokenId       the unique identifier of the token to show
     * @param correlationId a unique identifier used to trace this operation
     * @return the updated wallet-token relationship
     * @throws IllegalArgumentException if validation fails
     */
    public WalletToken showTokenInWallet(UUID walletId, UUID tokenId, String correlationId) {
        // Validate inputs
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must be provided");
        }
        if (tokenId == null) {
            throw new IllegalArgumentException("Token ID must be provided");
        }

        // Validate wallet exists and is active
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));
        wallet.validateOperationAllowed();

        // Find the wallet-token relationship
        WalletToken walletToken = walletTokenRepository.findByWalletIdAndTokenId(walletId, tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token is not added to this wallet"));

        // Show the token
        walletToken.show();
        walletTokenRepository.update(walletToken);

        return walletToken;
    }
}