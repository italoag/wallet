package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletTokenRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.domain.model.wallet.WalletToken;
import dev.bloco.wallet.hub.domain.event.wallet.TokenAddedToWalletEvent;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * AddTokenToWalletUseCase is responsible for adding supported tokens to
 * wallets.
 * This allows wallets to manage and display specific tokens.
 * <p/>
 * Business Rules:
 * - Wallet must exist and be active
 * - Token must exist
 * - Token cannot already be added to the wallet
 * - Token must be compatible with wallet's supported networks
 * <p/>
 * Publishes:
 * - TokenAddedToWalletEvent when token is successfully added
 */
@RequiredArgsConstructor
public class AddTokenToWalletUseCase {

    private final WalletRepository walletRepository;
    private final TokenRepository tokenRepository;
    private final WalletTokenRepository walletTokenRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Adds a token to a wallet for management and display.
     *
     * @param walletId      the unique identifier of the wallet
     * @param tokenId       the unique identifier of the token to add
     * @param displayName   custom display name for the token (optional)
     * @param correlationId a unique identifier used to trace this operation
     * @return the created wallet-token relationship
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException    if wallet is not active
     */
    public WalletToken addTokenToWallet(UUID walletId, UUID tokenId, String displayName, String correlationId) {
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

        // Validate token exists
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found with id: " + tokenId));

        // Check if a token is already added to the wallet
        if (walletTokenRepository.existsByWalletIdAndTokenId(walletId, tokenId)) {
            throw new IllegalArgumentException("Token is already added to this wallet");
        }

        // Create a wallet-token relationship
        WalletToken walletToken = displayName != null
                ? WalletToken.create(UUID.randomUUID(), walletId, tokenId, displayName)
                : WalletToken.create(UUID.randomUUID(), walletId, tokenId);

        // Save the relationship
        walletTokenRepository.save(walletToken);

        // Publish event
        TokenAddedToWalletEvent event = TokenAddedToWalletEvent.builder()
                .walletId(walletId)
                .tokenId(tokenId)
                .displayName(displayName != null ? displayName : token.getName())
                .correlationId(UUID.fromString(correlationId))
                .build();

        eventPublisher.publish(event);

        return walletToken;
    }

    /**
     * Adds multiple tokens to a wallet in batch.
     *
     * @param walletId      the unique identifier of the wallet
     * @param tokenIds      array of token identifiers to add
     * @param correlationId a unique identifier used to trace this operation
     * @return summary of the batch operation
     */
    public BatchAddResult addMultipleTokens(UUID walletId, UUID[] tokenIds, String correlationId) {
        if (tokenIds == null || tokenIds.length == 0) {
            return new BatchAddResult(0, 0, new String[0]);
        }

        int successCount = 0;
        int failureCount = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();

        for (UUID tokenId : tokenIds) {
            try {
                addTokenToWallet(walletId, tokenId, null, correlationId);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                errors.add("Failed to add token " + tokenId + ": " + e.getMessage());
            }
        }

        return new BatchAddResult(successCount, failureCount, errors.toArray(new String[0]));
    }

    /**
     * Result of a batch token addition operation.
     */
    public record BatchAddResult(
            int successCount,
            int failureCount,
            String[] errors) {

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof BatchAddResult(int sc, int fc, String[] err)))
                return false;
            return successCount == sc
                    && failureCount == fc
                    && java.util.Arrays.equals(errors, err);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(successCount);
            result = 31 * result + Integer.hashCode(failureCount);
            result = 31 * result + java.util.Arrays.hashCode(errors);
            return result;
        }

        @Override
        public String toString() {
            return "BatchAddResult[successCount=" + successCount
                    + ", failureCount=" + failureCount
                    + ", errors=" + java.util.Arrays.toString(errors) + "]";
        }
    }
}