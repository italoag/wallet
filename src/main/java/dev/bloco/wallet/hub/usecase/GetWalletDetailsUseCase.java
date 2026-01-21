package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * GetWalletDetailsUseCase is responsible for retrieving comprehensive wallet
 * information.
 * It provides detailed wallet data including associated addresses and metadata.
 * <p/>
 * Business Rules:
 * - Wallet must exist
 * - Deleted wallets can still be retrieved for audit purposes
 * <p/>
 * No domain events are published by this read-only operation.
 */
@RequiredArgsConstructor
public class GetWalletDetailsUseCase {

    private final WalletRepository walletRepository;

    /**
     * Retrieves detailed information about a wallet.
     *
     * @param walletId the unique identifier of the wallet
     * @return the wallet with detailed information
     * @throws IllegalArgumentException if wallet not found
     */
    public Wallet getWalletDetails(UUID walletId) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must be provided");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        return wallet;
    }

    /**
     * Retrieves wallet information with validation that it exists and is
     * accessible.
     *
     * @param walletId       the unique identifier of the wallet
     * @param includeDeleted whether to include deleted wallets in the search
     * @return the wallet instance
     * @throws IllegalArgumentException if wallet not found or is deleted when
     *                                  includeDeleted is false
     */
    public Wallet getWallet(UUID walletId, boolean includeDeleted) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must be provided");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        if (!includeDeleted && wallet.isDeleted()) {
            throw new IllegalArgumentException("Wallet is deleted and not accessible");
        }

        return wallet;
    }

    /**
     * Checks if a wallet exists and is accessible.
     *
     * @param walletId the unique identifier of the wallet
     * @return true if wallet exists and is not deleted, false otherwise
     */
    public boolean isWalletAccessible(UUID walletId) {
        if (walletId == null) {
            return false;
        }

        return walletRepository.findById(walletId)
                .map(wallet -> !wallet.isDeleted())
                .orElse(false);
    }
}