package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenBalanceRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.domain.model.token.TokenBalance;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * GetTokenBalanceUseCase is responsible for retrieving token balance information.
 * It provides balance data for specific tokens within wallets.
 * <p/>
 * Business Rules:
 * - Wallet must exist
 * - Token must exist
 * - Returns zero balance if no balance record exists
 * <p/>
 * No domain events are published by this read-only operation.
 */
public record GetTokenBalanceUseCase(
    WalletRepository walletRepository,
    TokenRepository tokenRepository,
    TokenBalanceRepository tokenBalanceRepository) {

    /**
     * Retrieves the balance of a specific token for a wallet.
     * This aggregates balances across all addresses in the wallet.
     *
     * @param walletId the unique identifier of the wallet
     * @param tokenId the unique identifier of the token
     * @return the total token balance for the wallet
     * @throws IllegalArgumentException if wallet or token not found
     */
    public BigDecimal getWalletTokenBalance(UUID walletId, UUID tokenId) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must be provided");
        }
        if (tokenId == null) {
            throw new IllegalArgumentException("Token ID must be provided");
        }

        // Validate wallet exists
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        // Validate token exists
        tokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found with id: " + tokenId));

        // Aggregate balances across all addresses in the wallet
        BigDecimal totalBalance = BigDecimal.ZERO;
        
        for (UUID addressId : wallet.getAddressIds()) {
            BigDecimal addressBalance = tokenBalanceRepository.findByAddressIdAndTokenId(addressId, tokenId)
                    .map(TokenBalance::getBalance)
                    .orElse(BigDecimal.ZERO);
            totalBalance = totalBalance.add(addressBalance);
        }

        return totalBalance;
    }

    /**
     * Retrieves detailed token balance information including metadata.
     *
     * @param walletId the unique identifier of the wallet
     * @param tokenId the unique identifier of the token
     * @return detailed balance information
     * @throws IllegalArgumentException if wallet or token not found
     */
    public TokenBalanceDetails getTokenBalanceDetails(UUID walletId, UUID tokenId) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID must be provided");
        }
        if (tokenId == null) {
            throw new IllegalArgumentException("Token ID must be provided");
        }

        // Validate wallet exists
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        // Validate token exists
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found with id: " + tokenId));

        // Calculate total balance and collect address details
        BigDecimal totalBalance = BigDecimal.ZERO;
        int addressCount = 0;
        
        for (UUID addressId : wallet.getAddressIds()) {
            java.util.Optional<TokenBalance> balanceOpt = 
                tokenBalanceRepository.findByAddressIdAndTokenId(addressId, tokenId);
            
            if (balanceOpt.isPresent()) {
                totalBalance = totalBalance.add(balanceOpt.get().getBalance());
                if (balanceOpt.get().getBalance().compareTo(BigDecimal.ZERO) > 0) {
                    addressCount++;
                }
            }
        }

        // Format the balance according to token decimals
        String formattedBalance = token.formatAmount(totalBalance);

        return TokenBalanceDetails.builder()
                .walletId(walletId)
                .tokenId(tokenId)
                .tokenSymbol(token.getSymbol())
                .tokenName(token.getName())
                .rawBalance(totalBalance)
                .formattedBalance(formattedBalance)
                .decimals(token.getDecimals())
                .addressesWithBalance(addressCount)
                .totalAddresses(wallet.getAddressIds().size())
                .build();
    }

    /**
     * Detailed token balance information.
     */
    public static class TokenBalanceDetails {
        private final UUID walletId;
        private final UUID tokenId;
        private final String tokenSymbol;
        private final String tokenName;
        private final BigDecimal rawBalance;
        private final String formattedBalance;
        private final int decimals;
        private final int addressesWithBalance;
        private final int totalAddresses;

        private TokenBalanceDetails(Builder builder) {
            this.walletId = builder.walletId;
            this.tokenId = builder.tokenId;
            this.tokenSymbol = builder.tokenSymbol;
            this.tokenName = builder.tokenName;
            this.rawBalance = builder.rawBalance;
            this.formattedBalance = builder.formattedBalance;
            this.decimals = builder.decimals;
            this.addressesWithBalance = builder.addressesWithBalance;
            this.totalAddresses = builder.totalAddresses;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public UUID getWalletId() { return walletId; }
        public UUID getTokenId() { return tokenId; }
        public String getTokenSymbol() { return tokenSymbol; }
        public String getTokenName() { return tokenName; }
        public BigDecimal getRawBalance() { return rawBalance; }
        public String getFormattedBalance() { return formattedBalance; }
        public int getDecimals() { return decimals; }
        public int getAddressesWithBalance() { return addressesWithBalance; }
        public int getTotalAddresses() { return totalAddresses; }

        public static class Builder {
            private UUID walletId;
            private UUID tokenId;
            private String tokenSymbol;
            private String tokenName;
            private BigDecimal rawBalance;
            private String formattedBalance;
            private int decimals;
            private int addressesWithBalance;
            private int totalAddresses;

            public Builder walletId(UUID walletId) { this.walletId = walletId; return this; }
            public Builder tokenId(UUID tokenId) { this.tokenId = tokenId; return this; }
            public Builder tokenSymbol(String tokenSymbol) { this.tokenSymbol = tokenSymbol; return this; }
            public Builder tokenName(String tokenName) { this.tokenName = tokenName; return this; }
            public Builder rawBalance(BigDecimal rawBalance) { this.rawBalance = rawBalance; return this; }
            public Builder formattedBalance(String formattedBalance) { this.formattedBalance = formattedBalance; return this; }
            public Builder decimals(int decimals) { this.decimals = decimals; return this; }
            public Builder addressesWithBalance(int addressesWithBalance) { this.addressesWithBalance = addressesWithBalance; return this; }
            public Builder totalAddresses(int totalAddresses) { this.totalAddresses = totalAddresses; return this; }

            public TokenBalanceDetails build() {
                return new TokenBalanceDetails(this);
            }
        }
    }
}