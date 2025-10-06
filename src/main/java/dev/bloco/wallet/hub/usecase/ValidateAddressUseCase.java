package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.model.address.AccountAddress;
import dev.bloco.wallet.hub.domain.model.network.Network;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

/**
 * ValidateAddressUseCase is responsible for validating address formats and network compatibility.
 * It provides comprehensive address validation for different blockchain networks.
 * <p/>
 * Business Rules:
 * - Address format must be valid for the specific network
 * - Network must exist and be accessible
 * - Validation rules vary by network type
 * <p/>
 * No domain events are published by this validation operation.
 */
public record ValidateAddressUseCase(NetworkRepository networkRepository) {

    private static final String ERROR_ADDRESS_REQUIRED = "Address value must be provided";
    private static final String ERROR_CORRELATION_REQUIRED = "Correlation ID must be provided";
    private static final String ERROR_CORRELATION_INVALID = "Correlation ID must be a valid UUID";

    /**
     * Validates an address format for a specific network.
     *
     * @param addressValue the address value to validate
     * @param networkId the network to validate against (optional)
     * @return validation result with details
     * @throws IllegalArgumentException if address is null or empty
     */
    public AddressValidationResult validateAddress(String addressValue, UUID networkId, String correlationId) {
        if (!StringUtils.hasText(addressValue)) {
            throw new IllegalArgumentException(ERROR_ADDRESS_REQUIRED);
        }

        try {
            // Basic format validation
            AccountAddress accountAddress = new AccountAddress(addressValue);

            String format = determineAddressFormat(addressValue);
            boolean isFormatValid = !"Unknown".equals(format);
            String networkName = "Unknown";
            boolean isNetworkCompatible = false; // Default to incompatible

            // If network ID is provided, validate compatibility
            if (networkId != null) {
                String normalizedCorrelation = normalizeCorrelationId(correlationId);
                Network network = networkRepository.findById(networkId, normalizedCorrelation).orElse(null);
                if (network != null) {
                    networkName = network.getName();
                    // Only check compatibility if format is valid
                    isNetworkCompatible = isFormatValid && isAddressCompatibleWithNetwork(addressValue, network);
                } else {
                    isNetworkCompatible = false;
                }
            } else {
                // If no network specified, compatible only if format is valid
                isNetworkCompatible = isFormatValid;
            }

            return AddressValidationResult.builder()
                    .valid(isFormatValid)
                    .address(addressValue)
                    .format(format)
                    .network(networkName)
                    .networkCompatible(isNetworkCompatible)
                    .build();

        } catch (IllegalArgumentException e) {
            return AddressValidationResult.builder()
                    .valid(false)
                    .address(addressValue)
                    .format("Invalid")
                    .network("Unknown")
                    .networkCompatible(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Batch validates multiple addresses.
     *
     * @param addresses array of addresses to validate
     * @param networkId the network to validate against (optional)
     * @return array of validation results
     */
    public AddressValidationResult[] validateAddresses(String[] addresses, UUID networkId, String correlationId) {
        if (addresses == null) {
            return new AddressValidationResult[0];
        }

        return java.util.Arrays.stream(addresses)
                .map(address -> validateAddress(address, networkId, correlationId))
                .toArray(AddressValidationResult[]::new);
    }

    private String determineAddressFormat(String address) {
        // Ethereum-style addresses (0x followed by 40 hex characters)
        if (Pattern.matches("^0x[a-fA-F0-9]{40}$", address)) {
            return "Ethereum";
        }
        
        // Bitcoin-style addresses
        if (Pattern.matches("^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$", address)) {
            return "Bitcoin Legacy";
        }
        
        if (Pattern.matches("^bc1[a-z0-9]{39,59}$", address)) {
            return "Bitcoin Bech32";
        }
        
        // Generic hexadecimal format
        if (Pattern.matches("^[a-fA-F0-9]+$", address)) {
            return "Hexadecimal";
        }
        
        return "Unknown";
    }

    private boolean isAddressCompatibleWithNetwork(String address, Network network) {
        String format = determineAddressFormat(address);
        String networkName = network.getName().toLowerCase();
        
        // Basic compatibility rules - this would be expanded for real networks
        return switch (format) {
            case "Ethereum" -> networkName.contains("ethereum") || 
                              networkName.contains("bsc") || 
                              networkName.contains("polygon");
            case "Bitcoin Legacy", "Bitcoin Bech32" -> networkName.contains("bitcoin");
            default -> true; // Allow unknown formats for flexibility
        };
    }

    /**
     * Result of address validation operation.
     */
    public static class AddressValidationResult {
        private final boolean valid;
        private final String address;
        private final String format;
        private final String network;
        private final boolean networkCompatible;
        private final String error;

        AddressValidationResult(Builder builder) {
            this.valid = builder.valid;
            this.address = builder.address;
            this.format = builder.format;
            this.network = builder.network;
            this.networkCompatible = builder.networkCompatible;
            this.error = builder.error;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public boolean isValid() { return valid; }
        public String getAddress() { return address; }
        public String getFormat() { return format; }
        public String getNetwork() { return network; }
        public boolean isNetworkCompatible() { return networkCompatible; }
        public String getError() { return error; }

        public static class Builder {
            private boolean valid;
            private String address;
            private String format;
            private String network;
            private boolean networkCompatible;
            private String error;

            public Builder valid(boolean valid) { this.valid = valid; return this; }
            public Builder address(String address) { this.address = address; return this; }
            public Builder format(String format) { this.format = format; return this; }
            public Builder network(String network) { this.network = network; return this; }
            public Builder networkCompatible(boolean compatible) { this.networkCompatible = compatible; return this; }
            public Builder error(String error) { this.error = error; return this; }

            public AddressValidationResult build() {
                return new AddressValidationResult(this);
            }
        }
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
}