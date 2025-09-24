package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.gateway.TransactionFeeRepository;
import dev.bloco.wallet.hub.domain.model.network.Network;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionFee;
import dev.bloco.wallet.hub.domain.model.transaction.FeeLevel;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.util.StringUtils;

/**
 * EstimateTransactionFeeUseCase is responsible for calculating transaction fees.
 * It provides fee estimates for different speed levels and networks.
 * <p/>
 * Business Rules:
 * - Network must be active
 * - Fee estimates are cached for performance
 * - Multiple fee levels are provided for user choice
 * - Gas limit estimation for different transaction types
 * <p/>
 * No domain events are published by this read-only operation.
 */
public record EstimateTransactionFeeUseCase(
    NetworkRepository networkRepository,
    TransactionFeeRepository feeRepository) {

    private static final String ERROR_NETWORK_ID_REQUIRED = "Network ID must be provided";
    private static final String ERROR_GAS_LIMIT_REQUIRED = "Gas limit must be positive";
    private static final String ERROR_NETWORK_NOT_FOUND_TEMPLATE = "Network not found with id: %s";
    private static final String ERROR_NETWORK_UNAVAILABLE_TEMPLATE = "Network is not available: %s";
    private static final String ERROR_TRANSACTION_TYPE_REQUIRED = "Transaction type must be provided";
    private static final String ERROR_CORRELATION_REQUIRED = "Correlation ID must be provided";
    private static final String ERROR_CORRELATION_INVALID = "Correlation ID must be a valid UUID";

    /**
     * Estimates transaction fees for all fee levels on a network.
     *
     * @param networkId the unique identifier of the network
     * @param gasLimit the estimated gas limit for the transaction
     * @param correlationId the correlation identifier for tracking downstream requests
     * @return fee estimates for all levels
     * @throws IllegalArgumentException if network not found or invalid gas limit
     */
    public FeeEstimateResult estimateTransactionFee(UUID networkId, BigDecimal gasLimit, String correlationId) {
        if (networkId == null) {
            throw new IllegalArgumentException(ERROR_NETWORK_ID_REQUIRED);
        }
        if (gasLimit == null || gasLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(ERROR_GAS_LIMIT_REQUIRED);
        }

        String normalizedCorrelation = normalizeCorrelationId(correlationId);

        // Validate network exists and is active
        Network network = networkRepository.findById(networkId, normalizedCorrelation)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_NETWORK_NOT_FOUND_TEMPLATE.formatted(networkId)));

        if (!network.isAvailable()) {
            throw new IllegalStateException(ERROR_NETWORK_UNAVAILABLE_TEMPLATE.formatted(network.getName()));
        }

        // Get fee estimates for all levels
        List<FeeEstimate> estimates = List.of(FeeLevel.values()).stream()
                .map(level -> estimateForLevel(networkId, level, gasLimit))
                .toList();

        return new FeeEstimateResult(
            networkId,
            network.getName(),
            gasLimit,
            estimates
        );
    }

    /**
     * Estimates gas limit for common transaction types.
     *
     * @param networkId the unique identifier of the network
     * @param transactionType the type of transaction
     * @param correlationId correlation identifier for repository lookups
     * @return estimated gas limit
     */
    public BigDecimal estimateGasLimit(UUID networkId, TransactionType transactionType, String correlationId) {
        if (networkId == null) {
            throw new IllegalArgumentException(ERROR_NETWORK_ID_REQUIRED);
        }
        if (transactionType == null) {
            throw new IllegalArgumentException(ERROR_TRANSACTION_TYPE_REQUIRED);
        }

        // Validate network
        Network network = networkRepository.findById(networkId, normalizeCorrelationId(correlationId))
                .orElseThrow(() -> new IllegalArgumentException(ERROR_NETWORK_NOT_FOUND_TEMPLATE.formatted(networkId)));

        // Return estimated gas limits based on transaction type
        return switch (transactionType) {
            case SIMPLE_TRANSFER -> new BigDecimal("21000");
            case ERC20_TRANSFER -> new BigDecimal("65000");
            case ERC721_TRANSFER -> new BigDecimal("85000");
            case CONTRACT_INTERACTION -> new BigDecimal("150000");
            case CONTRACT_DEPLOYMENT -> new BigDecimal("500000");
            case COMPLEX_DEFI -> new BigDecimal("300000");
        };
    }

    /**
     * Gets the latest fee information for a specific level.
     *
     * @param networkId the unique identifier of the network
     * @param level the fee level
     * @return the latest fee information
     */
    public TransactionFee getLatestFee(UUID networkId, FeeLevel level) {
        return feeRepository.findLatestByNetworkIdAndLevel(networkId, level)
                .orElseGet(() -> createDefaultFee(networkId, level));
    }

    private FeeEstimate estimateForLevel(UUID networkId, FeeLevel level, BigDecimal gasLimit) {
        TransactionFee fee = getLatestFee(networkId, level);
        BigDecimal totalCost = fee.calculateTotalCost(gasLimit);

        return new FeeEstimate(
            level,
            fee.getGasPrice(),
            fee.getBaseFee(),
            fee.getPriorityFee(),
            totalCost,
            getEstimatedConfirmationTime(level),
            fee.getLevelDescription()
        );
    }

    private TransactionFee createDefaultFee(UUID networkId, FeeLevel level) {
        // Create default fees if none exist
        BigDecimal gasPrice = switch (level) {
            case SLOW -> new BigDecimal("20000000000"); // 20 Gwei
            case STANDARD -> new BigDecimal("25000000000"); // 25 Gwei
            case FAST -> new BigDecimal("35000000000"); // 35 Gwei
            case URGENT -> new BigDecimal("50000000000"); // 50 Gwei
        };

        return TransactionFee.create(
            UUID.randomUUID(),
            networkId,
            level,
            gasPrice,
            null,
            null,
            true
        );
    }

    private String getEstimatedConfirmationTime(FeeLevel level) {
        return switch (level) {
            case SLOW -> "5-10 minutes";
            case STANDARD -> "2-5 minutes";
            case FAST -> "1-2 minutes";
            case URGENT -> "< 1 minute";
        };
    }

    private String normalizeCorrelationId(String correlationId) {
        if (!StringUtils.hasText(correlationId)) {
            throw new IllegalArgumentException(ERROR_CORRELATION_REQUIRED);
        }

        try {
            UUID parsed = UUID.fromString(correlationId.trim());
            return parsed.toString();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ERROR_CORRELATION_INVALID, ex);
        }
    }

    /**
     * Transaction types for gas estimation.
     */
    public enum TransactionType {
        SIMPLE_TRANSFER,
        ERC20_TRANSFER,
        ERC721_TRANSFER,
        CONTRACT_INTERACTION,
        CONTRACT_DEPLOYMENT,
        COMPLEX_DEFI
    }

    /**
     * Fee estimate for a specific level.
     */
    public record FeeEstimate(
        FeeLevel level,
        BigDecimal gasPrice,
        BigDecimal baseFee,
        BigDecimal priorityFee,
        BigDecimal totalCost,
        String estimatedTime,
        String description
    ) {}

    /**
     * Complete fee estimation result.
     */
    public record FeeEstimateResult(
        UUID networkId,
        String networkName,
        BigDecimal gasLimit,
        List<FeeEstimate> estimates
    ) {}
}
