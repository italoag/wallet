package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenBalanceRepository;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AddressBalanceResult;
import dev.bloco.wallet.hub.domain.model.token.TokenBalance;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GetAddressBalanceUseCase is responsible for retrieving balance information
 * for addresses.
 * It provides comprehensive balance data including all tokens held by an
 * address.
 * <p/>
 * Business Rules:
 * - Address must exist
 * - Returns all token balances for the address
 * - Zero balances are included for completeness
 * <p/>
 * No domain events are published by this read-only operation.
 */
@RequiredArgsConstructor
public class GetAddressBalanceUseCase {

    private final AddressRepository addressRepository;
    private final TokenBalanceRepository tokenBalanceRepository;

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
                        TokenBalance::getBalance));

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
     * @param tokenId   the unique identifier of the token
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
     * @return map of address ID to balance a result
     */
    public Map<UUID, AddressBalanceResult> getMultipleAddressBalances(List<UUID> addressIds) {
        if (addressIds == null || addressIds.isEmpty()) {
            return Map.of();
        }

        return addressIds.stream()
                .collect(Collectors.toMap(
                        addressId -> addressId,
                        this::getAddressBalance));
    }
}