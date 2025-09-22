package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.wallet.WalletStatus;

import java.util.List;
import java.util.UUID;

/**
 * ListWalletsUseCase is responsible for retrieving wallets for a user.
 * It supports filtering by status and provides different views of wallet data.
 * <p/>
 * Business Rules:
 * - User ID must be provided
 * - Deleted wallets are excluded by default unless explicitly requested
 * - Results can be filtered by wallet status
 * <p/>
 * No domain events are published by this read-only operation.
 */
public record ListWalletsUseCase(WalletRepository walletRepository) {

    /**
     * Retrieves all active wallets for a user.
     *
     * @param userId the unique identifier of the user
     * @return list of active wallets belonging to the user
     * @throws IllegalArgumentException if userId is null
     */
    public List<Wallet> listActiveWallets(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }
        
        return walletRepository.findActiveByUserId(userId);
    }

    /**
     * Retrieves all wallets for a user, excluding deleted ones.
     *
     * @param userId the unique identifier of the user
     * @return list of wallets belonging to the user (excluding deleted)
     * @throws IllegalArgumentException if userId is null
     */
    public List<Wallet> listWallets(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }
        
        return walletRepository.findByUserId(userId)
                .stream()
                .filter(wallet -> !wallet.isDeleted())
                .toList();
    }

    /**
     * Retrieves wallets for a user with a specific status.
     *
     * @param userId the unique identifier of the user
     * @param status the wallet status to filter by
     * @return list of wallets with the specified status
     * @throws IllegalArgumentException if userId or status is null
     */
    public List<Wallet> listWalletsByStatus(UUID userId, WalletStatus status) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status must be provided");
        }
        
        return walletRepository.findByUserIdAndStatus(userId, status);
    }

    /**
     * Retrieves all wallets for a user, including deleted ones.
     * This method is typically used for administrative purposes.
     *
     * @param userId the unique identifier of the user
     * @return list of all wallets belonging to the user
     * @throws IllegalArgumentException if userId is null
     */
    public List<Wallet> listAllWallets(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided");
        }
        
        return walletRepository.findByUserId(userId);
    }
}