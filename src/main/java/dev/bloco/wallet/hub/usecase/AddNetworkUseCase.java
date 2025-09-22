package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.network.Network;
import dev.bloco.wallet.hub.domain.event.network.NetworkCreatedEvent;

import java.util.UUID;

/**
 * AddNetworkUseCase is responsible for adding new blockchain networks to the system.
 * This enables support for additional blockchain networks and their tokens.
 * <p/>
 * Business Rules:
 * - Network name must be unique
 * - Chain ID must be unique
 * - RPC URL must be valid and accessible
 * - Explorer URL should be provided for transaction tracking
 * <p/>
 * Publishes:
 * - NetworkCreatedEvent when network is successfully added
 */
public record AddNetworkUseCase(
    NetworkRepository networkRepository,
    DomainEventPublisher eventPublisher) {

    /**
     * Adds a new blockchain network to the system.
     *
     * @param name the display name of the network
     * @param chainId the unique chain identifier
     * @param rpcUrl the RPC endpoint URL for the network
     * @param explorerUrl the block explorer URL for the network
     * @param correlationId a unique identifier used to trace this operation
     * @return the created network instance
     * @throws IllegalArgumentException if validation fails
     */
    public Network addNetwork(String name, String chainId, String rpcUrl, String explorerUrl, String correlationId) {
        // Validate inputs
        validateInputs(name, chainId, rpcUrl, explorerUrl);

        // TODO: In a real implementation, we would check for uniqueness
        // For now, we'll create the network directly

        // Create network
        Network network = Network.create(
                UUID.randomUUID(),
                name,
                chainId,
                rpcUrl,
                explorerUrl
        );

        // Save network
        networkRepository.save(network);

        // Publish event
        NetworkCreatedEvent event = NetworkCreatedEvent.builder()
                .networkId(network.getId())
                .name(name)
                .chainId(chainId)
                .correlationId(UUID.fromString(correlationId))
                .build();
        
        eventPublisher.publish(event);

        return network;
    }

    private void validateInputs(String name, String chainId, String rpcUrl, String explorerUrl) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Network name must be provided");
        }
        if (chainId == null || chainId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chain ID must be provided");
        }
        if (rpcUrl == null || rpcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("RPC URL must be provided");
        }
        if (explorerUrl == null || explorerUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Explorer URL must be provided");
        }

        // Basic URL validation
        if (!rpcUrl.startsWith("http://") && !rpcUrl.startsWith("https://")) {
            throw new IllegalArgumentException("RPC URL must be a valid HTTP/HTTPS URL");
        }
        if (!explorerUrl.startsWith("http://") && !explorerUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Explorer URL must be a valid HTTP/HTTPS URL");
        }
    }
}