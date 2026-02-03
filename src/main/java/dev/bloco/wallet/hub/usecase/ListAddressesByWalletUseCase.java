package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AddressStatus;
import dev.bloco.wallet.hub.domain.model.Wallet;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * ListAddressesByWalletUseCase is responsible for retrieving addresses
 * associated with a wallet.
 * It provides filtering and management capabilities for wallet addresses.
 * <p/>
 * Business Rules:
 * - Wallet must exist
 * - Addresses can be filtered by status
 * - Deleted wallets can still have their addresses listed for audit purposes
 * <p/>
 * No domain events are published by this read-only operation.
 */
@RequiredArgsConstructor
public class ListAddressesByWalletUseCase {

    private final AddressRepository addressRepository;
    private final WalletRepository walletRepository;

    /**
     * Retrieves all addresses for a wallet.
     *
     * @param walletId the unique identifier of the wallet
     * @return list of addresses belonging to the wallet
     * @throws IllegalArgumentException if wallet not found
     */
    public List<Address> listAddresses(UUID walletId) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must be provided");
        }

        // Verify wallet exists
        walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        return addressRepository.findByWalletId(walletId);
    }

    /**
     * Retrieves active addresses for a wallet.
     *
     * @param walletId the unique identifier of the wallet
     * @return list of active addresses belonging to the wallet
     * @throws IllegalArgumentException if wallet not found
     */
    public List<Address> listActiveAddresses(UUID walletId) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must be provided");
        }

        // Verify wallet exists
        walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        return addressRepository.findByWalletIdAndStatus(walletId, AddressStatus.ACTIVE);
    }

    /**
     * Retrieves addresses for a wallet with a specific status.
     *
     * @param walletId the unique identifier of the wallet
     * @param status   the address status to filter by
     * @return list of addresses with the specified status
     * @throws IllegalArgumentException if wallet not found or status is null
     */
    public List<Address> listAddressesByStatus(UUID walletId, AddressStatus status) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must be provided");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status must be provided");
        }

        // Verify wallet exists
        walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        return addressRepository.findByWalletIdAndStatus(walletId, status);
    }

    /**
     * Counts addresses for a wallet by status.
     *
     * @param walletId the unique identifier of the wallet
     * @return summary of address counts by status
     * @throws IllegalArgumentException if wallet not found
     */
    public AddressCountSummary getAddressCountSummary(UUID walletId) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must be provided");
        }

        // Verify wallet exists
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        List<Address> allAddresses = addressRepository.findByWalletId(walletId);

        long activeCount = allAddresses.stream()
                .filter(addr -> addr.getStatus() == AddressStatus.ACTIVE)
                .count();

        long archivedCount = allAddresses.stream()
                .filter(addr -> addr.getStatus() == AddressStatus.ARCHIVED)
                .count();

        return new AddressCountSummary(
                walletId,
                wallet.getName(),
                allAddresses.size(),
                activeCount,
                archivedCount);
    }

    /**
     * Summary of address counts for a wallet.
     */
    public record AddressCountSummary(
            UUID walletId,
            String walletName,
            int totalAddresses,
            long activeAddresses,
            long archivedAddresses) {
    }
}