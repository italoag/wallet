package dev.bloco.wallet.hub.domain.model.token;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Detailed token balance information for a wallet.
 * This record encapsulates comprehensive balance data including token metadata,
 * formatted values, and address distribution statistics.
 */
@Builder
public record TokenBalanceDetails(
    UUID walletId,
    UUID tokenId,
    String tokenSymbol,
    String tokenName,
    BigDecimal rawBalance,
    String formattedBalance,
    int decimals,
    int addressesWithBalance,
    int totalAddresses
) {
    /**
     * Validates the token balance details upon construction.
     */
    public TokenBalanceDetails {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must not be null");
        }
        if (tokenId == null) {
            throw new IllegalArgumentException("Token ID must not be null");
        }
        if (rawBalance == null) {
            throw new IllegalArgumentException("Raw balance must not be null");
        }
        if (addressesWithBalance < 0) {
            throw new IllegalArgumentException("Addresses with balance cannot be negative");
        }
        if (totalAddresses < 0) {
            throw new IllegalArgumentException("Total addresses cannot be negative");
        }
        if (addressesWithBalance > totalAddresses) {
            throw new IllegalArgumentException("Addresses with balance cannot exceed total addresses");
        }
    }
}
