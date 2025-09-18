package dev.bloco.wallet.hub.domain.model.address;

import java.util.Objects;

public final class PublicKey {
    private final String value;

    public PublicKey(String value) {
        validatePublicKey(value);
        this.value = value;
    }

    private void validatePublicKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Public key cannot be null or blank");
        }
        
        // Additional validation specific to blockchain public keys can be added here
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicKey publicKey = (PublicKey) o;
        return Objects.equals(value, publicKey.value);
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