package dev.bloco.wallet.hub.domain.model.address;

import dev.bloco.wallet.hub.domain.model.network.Network;
import java.util.Objects;
import java.util.regex.Pattern;

public final class AccountAddress {

    public enum AddressFormat {
        ETHEREUM("Ethereum"),
        BITCOIN_LEGACY("Bitcoin Legacy"),
        BITCOIN_BECH32("Bitcoin Bech32"),
        HEXADECIMAL("Hexadecimal"),
        UNKNOWN("Unknown");

        private final String description;

        AddressFormat(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final String value;
    private final AddressFormat format;

    public AccountAddress(String value) {
        validateAccountAddress(value);
        this.value = value;
        this.format = determineAddressFormat(value);
    }

    private void validateAccountAddress(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Account address cannot be null or blank");
        }
    }

    private AddressFormat determineAddressFormat(String address) {
        // Ethereum-style addresses (0x followed by 40 hex characters)
        if (Pattern.matches("^0x[a-fA-F0-9]{40}$", address)) {
            return AddressFormat.ETHEREUM;
        }

        // Bitcoin-style addresses
        if (Pattern.matches("^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$", address)) {
            return AddressFormat.BITCOIN_LEGACY;
        }

        if (Pattern.matches("^bc1[a-z0-9]{39,59}$", address)) {
            return AddressFormat.BITCOIN_BECH32;
        }

        // Generic hexadecimal format
        if (Pattern.matches("^[a-fA-F0-9]+$", address)) {
            return AddressFormat.HEXADECIMAL;
        }

        return AddressFormat.UNKNOWN;
    }

    public boolean isCompatibleWith(Network network) {
        if (network == null) {
            return false;
        }

        String networkName = network.getName().toLowerCase();

        return switch (this.format) {
            case ETHEREUM -> networkName.contains("ethereum") ||
                    networkName.contains("bsc") ||
                    networkName.contains("polygon");
            case BITCOIN_LEGACY, BITCOIN_BECH32 -> networkName.contains("bitcoin");
            // Allow unknown formats for flexibility or default to true for generic hex if
            // we want lenient
            default -> true;
        };
    }

    public String getValue() {
        return value;
    }

    public AddressFormat getFormat() {
        return format;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AccountAddress that = (AccountAddress) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}