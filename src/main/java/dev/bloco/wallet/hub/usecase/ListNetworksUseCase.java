package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.model.network.Network;

import java.util.List;
import java.util.UUID;

/**
 * ListNetworksUseCase is responsible for retrieving blockchain networks.
 * It provides filtering capabilities and network status information.
 * <p/>
 * Business Rules:
 * - Networks can be filtered by status
 * - Only active networks are returned by default
 * - Results include network health information
 * <p/>
 * No domain events are published by this read-only operation.
 */
public record ListNetworksUseCase(NetworkRepository networkRepository) {

    /**
     * Retrieves all active networks.
     *
     * @return list of active networks
     */
    public List<Network> listActiveNetworks() {
        return networkRepository.findAll().stream()
                .filter(Network::isAvailable)
                .toList();
    }

    /**
     * Retrieves all networks regardless of status.
     *
     * @return list of all networks
     */
    public List<Network> listAllNetworks() {
        return networkRepository.findAll();
    }

    /**
     * Gets a specific network by ID.
     *
     * @param networkId the unique identifier of the network
     * @return the network information
     * @throws IllegalArgumentException if network not found
     */
    public Network getNetworkDetails(UUID networkId) {
        if (networkId == null) {
            throw new IllegalArgumentException("Network ID must be provided");
        }

        return networkRepository.findById(networkId)
                .orElseThrow(() -> new IllegalArgumentException("Network not found with id: " + networkId));
    }

    /**
     * Gets networks with their health status.
     *
     * @return list of networks with health information
     */
    public List<NetworkHealthInfo> getNetworkHealthStatus() {
        List<Network> networks = networkRepository.findAll();
        
        return networks.stream()
                .map(this::createHealthInfo)
                .toList();
    }

    /**
     * Searches networks by name.
     *
     * @param namePattern the pattern to search for in network names
     * @return list of matching networks
     */
    public List<Network> searchNetworksByName(String namePattern) {
        if (namePattern == null || namePattern.trim().isEmpty()) {
            return List.of();
        }
        
        String pattern = namePattern.toLowerCase();
        return networkRepository.findAll().stream()
                .filter(network -> network.getName().toLowerCase().contains(pattern))
                .toList();
    }

    private NetworkHealthInfo createHealthInfo(Network network) {
        // In a real implementation, this would check network connectivity
        boolean isHealthy = network.isAvailable();
        String healthStatus = isHealthy ? "Healthy" : "Unavailable";
        
        return new NetworkHealthInfo(
            network.getId(),
            network.getName(),
            network.getChainId(),
            network.getStatus(),
            isHealthy,
            healthStatus,
            network.getRpcUrl()
        );
    }

    /**
     * Network health information for monitoring and display.
     */
    public record NetworkHealthInfo(
        UUID networkId,
        String name,
        String chainId,
        dev.bloco.wallet.hub.domain.model.network.NetworkStatus status,
        boolean isHealthy,
        String healthStatus,
        String rpcUrl
    ) {}
}