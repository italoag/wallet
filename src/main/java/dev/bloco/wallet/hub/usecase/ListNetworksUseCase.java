package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.model.network.Network;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.util.StringUtils;

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
@RequiredArgsConstructor
public class ListNetworksUseCase {

    private final NetworkRepository networkRepository;

    private static final String ERROR_NETWORK_ID_REQUIRED = "Network ID must be provided";
    private static final String ERROR_NETWORK_NOT_FOUND_TEMPLATE = "Network not found with id: %s";
    private static final String ERROR_CORRELATION_ID_REQUIRED = "Correlation ID must be provided";
    private static final String ERROR_CORRELATION_ID_INVALID = "Correlation ID must be a valid UUID";
    private static final String HEALTH_STATUS_HEALTHY = "Healthy";
    private static final String HEALTH_STATUS_UNAVAILABLE = "Unavailable";

    /**
     * Retrieves all active networks.
     *
     * @return list of active networks
     */
    public List<Network> listActiveNetworks(String correlationId) {
        String normalizedCorrelation = normalizeCorrelationId(correlationId);
        return networkRepository.findAll(normalizedCorrelation).stream()
                .filter(Network::isAvailable)
                .toList();
    }

    /**
     * Retrieves all networks regardless of status.
     *
     * @return list of all networks
     */
    public List<Network> listAllNetworks(String correlationId) {
        return networkRepository.findAll(normalizeCorrelationId(correlationId));
    }

    /**
     * Gets a specific network by ID.
     *
     * @param networkId the unique identifier of the network
     * @return the network information
     * @throws IllegalArgumentException if network not found
     */
    public Network getNetworkDetails(UUID networkId, String correlationId) {
        validateNetworkId(networkId);
        String normalizedCorrelation = normalizeCorrelationId(correlationId);

        return networkRepository.findById(networkId, normalizedCorrelation)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_NETWORK_NOT_FOUND_TEMPLATE.formatted(networkId)));
    }

    /**
     * Gets networks with their health status.
     *
     * @return list of networks with health information
     */
    public List<NetworkHealthInfo> getNetworkHealthStatus(String correlationId) {
        List<Network> networks = networkRepository.findAll(normalizeCorrelationId(correlationId));

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
    public List<Network> searchNetworksByName(String namePattern, String correlationId) {
        if (!StringUtils.hasText(namePattern)) {
            return List.of();
        }

        String pattern = namePattern.toLowerCase();
        return networkRepository.findAll(normalizeCorrelationId(correlationId)).stream()
                .filter(network -> network.getName().toLowerCase().contains(pattern))
                .toList();
    }

    private NetworkHealthInfo createHealthInfo(Network network) {
        // In a real implementation, this would check network connectivity
        boolean isHealthy = network.isAvailable();
        String healthStatus = isHealthy ? HEALTH_STATUS_HEALTHY : HEALTH_STATUS_UNAVAILABLE;

        return new NetworkHealthInfo(
                network.getId(),
                network.getName(),
                network.getChainId(),
                network.getStatus(),
                isHealthy,
                healthStatus,
                network.getRpcUrl());
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
            String rpcUrl) {
    }

    private void validateNetworkId(UUID networkId) {
        if (networkId == null) {
            throw new IllegalArgumentException(ERROR_NETWORK_ID_REQUIRED);
        }
    }

    private String normalizeCorrelationId(String correlationId) {
        if (!StringUtils.hasText(correlationId)) {
            throw new IllegalArgumentException(ERROR_CORRELATION_ID_REQUIRED);
        }

        try {
            UUID parsed = UUID.fromString(correlationId.trim());
            return parsed.toString();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ERROR_CORRELATION_ID_INVALID, ex);
        }
    }
}