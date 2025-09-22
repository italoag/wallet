package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenBalanceRepository;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.token.TokenBalance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GetAddressBalanceUseCase is responsible for retrieving balance information for addresses.
 * It provides comprehensive balance data including all tokens held by an address.
 * <p/>
 * Business Rules:
 * - Address must exist
 * - Returns all token balances for the address
 * - Zero balances are included for completeness
 * <p/>
 * No domain events are published by this read-only operation.
 */
public record GetAddressBalanceUseCase(
    AddressRepository addressRepository,
    TokenBalanceRepository tokenBalanceRepository) {

    /**
     * Retrieves all token balances for a specific address.
     *
     * @param addressId the unique identifier of the address
     * @return comprehensive balance information
     * @throws IllegalArgumentException if address not found
     */
    public AddressBalanceResult getAddressBalance(UUID addressId) {
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID must be provided");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found with id: " + addressId));

        List<TokenBalance> tokenBalances = tokenBalanceRepository.findByAddressId(addressId);

        BigDecimal totalValue = tokenBalances.stream()
                .map(TokenBalance::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, BigDecimal> balancesByToken = tokenBalances.stream()
                .collect(Collectors.toMap(
                    TokenBalance::getTokenId,
                    TokenBalance::getBalance
                ));

        return AddressBalanceResult.builder()
                .addressId(addressId)
                .address(address.getAccountAddress().getValue())
                .walletId(address.getWalletId())
                .networkId(address.getNetworkId())
                .totalValue(totalValue)
                .tokenBalances(balancesByToken)
                .balanceCount(tokenBalances.size())
                .build();
    }

    /**
     * Retrieves balance for a specific token on an address.
     *
     * @param addressId the unique identifier of the address
     * @param tokenId the unique identifier of the token
     * @return token balance or zero if not found
     * @throws IllegalArgumentException if address not found
     */
    public BigDecimal getTokenBalance(UUID addressId, UUID tokenId) {
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID must be provided");
        }
        if (tokenId == null) {
            throw new IllegalArgumentException("Token ID must be provided");
        }

        // Verify address exists
        addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found with id: " + addressId));

        return tokenBalanceRepository.findByAddressIdAndTokenId(addressId, tokenId)
                .map(TokenBalance::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Retrieves balances for multiple addresses.
     *
     * @param addressIds list of address identifiers
     * @return map of address ID to balance result
     */
    public Map<UUID, AddressBalanceResult> getMultipleAddressBalances(List<UUID> addressIds) {
        if (addressIds == null || addressIds.isEmpty()) {
            return Map.of();
        }

        return addressIds.stream()
                .collect(Collectors.toMap(
                    addressId -> addressId,
                    this::getAddressBalance
                ));
    }

    /**
     * Result containing comprehensive address balance information.
     */
    public static class AddressBalanceResult {
        private final UUID addressId;
        private final String address;
        private final UUID walletId;
        private final UUID networkId;
        private final BigDecimal totalValue;
        private final Map<UUID, BigDecimal> tokenBalances;
        private final int balanceCount;

        private AddressBalanceResult(Builder builder) {
            this.addressId = builder.addressId;
            this.address = builder.address;
            this.walletId = builder.walletId;
            this.networkId = builder.networkId;
            this.totalValue = builder.totalValue;
            this.tokenBalances = builder.tokenBalances;
            this.balanceCount = builder.balanceCount;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public UUID getAddressId() { return addressId; }
        public String getAddress() { return address; }
        public UUID getWalletId() { return walletId; }
        public UUID getNetworkId() { return networkId; }
        public BigDecimal getTotalValue() { return totalValue; }
        public Map<UUID, BigDecimal> getTokenBalances() { return tokenBalances; }
        public int getBalanceCount() { return balanceCount; }

        public static class Builder {
            private UUID addressId;
            private String address;
            private UUID walletId;
            private UUID networkId;
            private BigDecimal totalValue;
            private Map<UUID, BigDecimal> tokenBalances;
            private int balanceCount;

            public Builder addressId(UUID addressId) { this.addressId = addressId; return this; }
            public Builder address(String address) { this.address = address; return this; }
            public Builder walletId(UUID walletId) { this.walletId = walletId; return this; }
            public Builder networkId(UUID networkId) { this.networkId = networkId; return this; }
            public Builder totalValue(BigDecimal totalValue) { this.totalValue = totalValue; return this; }
            public Builder tokenBalances(Map<UUID, BigDecimal> tokenBalances) { this.tokenBalances = tokenBalances; return this; }
            public Builder balanceCount(int balanceCount) { this.balanceCount = balanceCount; return this; }

            public AddressBalanceResult build() {
                return new AddressBalanceResult(this);
            }
        }
    }
}