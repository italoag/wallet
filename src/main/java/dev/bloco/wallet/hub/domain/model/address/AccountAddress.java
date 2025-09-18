package dev.bloco.wallet.hub.domain.model;

import java.util.Objects;

public final class AccountAddress {
    private final String value;

    public AccountAddress(String value) {
        validateAccountAddress(value);
        this.value = value;
    }

    private void validateAccountAddress(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Account address cannot be null or blank");
        }
        
        // Blockchain-specific address validation can be added here
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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