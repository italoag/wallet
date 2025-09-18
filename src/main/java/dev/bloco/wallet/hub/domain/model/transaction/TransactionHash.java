package dev.bloco.wallet.hub.domain.model.transaction;

import java.util.Objects;

public final class TransactionHash {
    private final String value;

    public TransactionHash(String value) {
        validateHash(value);
        this.value = value;
    }

    private void validateHash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Transaction hash cannot be null or blank");
        }
        
        // Additional blockchain-specific hash validation can be added here
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionHash that = (TransactionHash) o;
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