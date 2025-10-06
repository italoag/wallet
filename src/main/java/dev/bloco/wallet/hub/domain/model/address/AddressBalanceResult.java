package dev.bloco.wallet.hub.domain.model.address;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Result containing comprehensive address balance information.
 * This domain model represents the aggregated balance data for a specific address,
 * including all token balances and total value metrics.
 */
public class AddressBalanceResult {
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
