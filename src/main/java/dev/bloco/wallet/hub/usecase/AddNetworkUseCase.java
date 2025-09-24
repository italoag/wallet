package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.network.Network;
import dev.bloco.wallet.hub.domain.event.network.NetworkCreatedEvent;

import java.util.UUID;

import org.springframework.util.StringUtils;

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

    private static final String ERROR_NAME_REQUIRED = "Network name must be provided";
    private static final String ERROR_CHAIN_ID_REQUIRED = "Chain ID must be provided";
    private static final String ERROR_RPC_URL_REQUIRED = "RPC URL must be provided";
    private static final String ERROR_EXPLORER_URL_REQUIRED = "Explorer URL must be provided";
    private static final String ERROR_INVALID_RPC_URL = "RPC URL must be a valid HTTP/HTTPS URL";
    private static final String ERROR_INVALID_EXPLORER_URL = "Explorer URL must be a valid HTTP/HTTPS URL";
    private static final String ERROR_CORRELATION_ID_REQUIRED = "Correlation ID must be provided";
    private static final String ERROR_CORRELATION_ID_INVALID = "Correlation ID must be a valid UUID";
    private static final String ERROR_CHAIN_ID_IN_USE = "A network with the provided chain ID already exists";

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
        String normalizedCorrelation = normalizeCorrelationId(correlationId);
        validateInputs(name, chainId, rpcUrl, explorerUrl);

        if (networkRepository.existsByChainId(chainId, normalizedCorrelation)) {
            throw new IllegalArgumentException(ERROR_CHAIN_ID_IN_USE);
        }

        Network network = Network.create(
                UUID.randomUUID(),
                name,
                chainId,
                rpcUrl,
                explorerUrl
        );

        networkRepository.save(network, normalizedCorrelation);

        NetworkCreatedEvent event = NetworkCreatedEvent.builder()
                .networkId(network.getId())
                .name(name)
                .chainId(chainId)
                .correlationId(UUID.fromString(normalizedCorrelation))
                .build();

        eventPublisher.publish(event);

        return network;
    }

    private void validateInputs(String name, String chainId, String rpcUrl, String explorerUrl) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException(ERROR_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(chainId)) {
            throw new IllegalArgumentException(ERROR_CHAIN_ID_REQUIRED);
        }
        if (!StringUtils.hasText(rpcUrl)) {
            throw new IllegalArgumentException(ERROR_RPC_URL_REQUIRED);
        }
        if (!StringUtils.hasText(explorerUrl)) {
            throw new IllegalArgumentException(ERROR_EXPLORER_URL_REQUIRED);
        }

        if (!isHttpUrl(rpcUrl)) {
            throw new IllegalArgumentException(ERROR_INVALID_RPC_URL);
        }
        if (!isHttpUrl(explorerUrl)) {
            throw new IllegalArgumentException(ERROR_INVALID_EXPLORER_URL);
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

    private boolean isHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}