package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenBalanceRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.domain.model.token.TokenBalance;
import dev.bloco.wallet.hub.domain.model.token.TokenBalanceDetails;

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

        return new TokenBalanceDetails(
                walletId,
                tokenId,
                token.getSymbol(),
                token.getName(),
                totalBalance,
                formattedBalance,
                token.getDecimals(),
                addressCount,
                wallet.getAddressIds().size()
        );
    }
}