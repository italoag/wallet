package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AddressStatus;

import java.util.UUID;

/**
 * UpdateAddressStatusUseCase is responsible for changing the status of addresses.
 * It manages the lifecycle transitions of addresses within wallets.
 * <p/>
 * Business Rules:
 * - Address must exist
 * - Status changes must be valid transitions
 * - Events are published for audit and integration purposes
 * <p/>
 * Publishes:
 * - AddressStatusChangedEvent when address status is successfully changed
 */
public record UpdateAddressStatusUseCase(
    AddressRepository addressRepository,
    DomainEventPublisher eventPublisher) {

    /**
     * Activates an address, making it available for transactions.
     *
     * @param addressId the unique identifier of the address
     * @param correlationId a unique identifier used to trace this operation
     * @return the updated address instance
     * @throws IllegalArgumentException if address not found
     */
    public Address activateAddress(UUID addressId, String correlationId) {
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID must be provided");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found with id: " + addressId));

        address.activate();
        addressRepository.update(address);

        // Publish events
        address.getDomainEvents().forEach(eventPublisher::publish);
        address.clearEvents();

        return address;
    }

    /**
     * Archives an address, removing it from active use but keeping it for history.
     *
     * @param addressId the unique identifier of the address
     * @param correlationId a unique identifier used to trace this operation
     * @return the updated address instance
     * @throws IllegalArgumentException if address not found
     */
    public Address archiveAddress(UUID addressId, String correlationId) {
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID must be provided");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found with id: " + addressId));

        address.archive();
        addressRepository.update(address);

        // Publish events
        address.getDomainEvents().forEach(eventPublisher::publish);
        address.clearEvents();

        return address;
    }

    /**
     * Updates the status of an address to a specific value.
     *
     * @param addressId the unique identifier of the address
     * @param newStatus the new status to set
     * @param correlationId a unique identifier used to trace this operation
     * @return the updated address instance
     * @throws IllegalArgumentException if address not found or status is null
     */
    public Address updateStatus(UUID addressId, AddressStatus newStatus, String correlationId) {
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID must be provided");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status must be provided");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found with id: " + addressId));

        // Apply the appropriate status change
        switch (newStatus) {
            case ACTIVE -> address.activate();
            case ARCHIVED -> address.archive();
            default -> throw new IllegalArgumentException("Unsupported status transition: " + newStatus);
        }

        addressRepository.update(address);

        // Publish events
        address.getDomainEvents().forEach(eventPublisher::publish);
        address.clearEvents();

        return address;
    }

    /**
     * Batch updates the status of multiple addresses.
     *
     * @param addressIds list of address identifiers to update
     * @param newStatus the new status to set for all addresses
     * @param correlationId a unique identifier used to trace this operation
     * @return count of successfully updated addresses
     */
    public int batchUpdateStatus(UUID[] addressIds, AddressStatus newStatus, String correlationId) {
        if (addressIds == null || addressIds.length == 0) {
            return 0;
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status must be provided");
        }

        int updatedCount = 0;
        for (UUID addressId : addressIds) {
            try {
                updateStatus(addressId, newStatus, correlationId);
                updatedCount++;
            } catch (IllegalArgumentException e) {
                // Log error but continue with other addresses
                // In production, this would use proper logging
                System.err.println("Failed to update address " + addressId + ": " + e.getMessage());
            }
        }

        return updatedCount;
    }
}