package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AddressType;
import dev.bloco.wallet.hub.domain.model.address.PublicKey;
import dev.bloco.wallet.hub.domain.model.address.AccountAddress;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.network.Network;

import java.util.UUID;

import org.springframework.util.StringUtils;

/**
 * CreateAddressUseCase is responsible for generating new addresses for wallets.
 * It handles the creation of addresses on specific networks with proper validation.
 * <p/>
 * Business Rules:
 * - Wallet must exist and be active
 * - Network must exist and be active
 * - Address must be unique within the network
 * - Public key must be provided and valid
 * <p/>
 * Publishes:
 * - AddressCreatedEvent when address is successfully created
 */
public record CreateAddressUseCase(
    AddressRepository addressRepository,
    WalletRepository walletRepository,
    NetworkRepository networkRepository,
    DomainEventPublisher eventPublisher) {

    private static final String ERROR_WALLET_ID_REQUIRED = "Wallet ID must be provided";
    private static final String ERROR_NETWORK_ID_REQUIRED = "Network ID must be provided";
    private static final String ERROR_PUBLIC_KEY_REQUIRED = "Public key must be provided";
    private static final String ERROR_ACCOUNT_ADDRESS_REQUIRED = "Account address must be provided";
    private static final String ERROR_ADDRESS_TYPE_REQUIRED = "Address type must be provided";
    private static final String ERROR_WALLET_NOT_FOUND_TEMPLATE = "Wallet not found with id: %s";
    private static final String ERROR_NETWORK_NOT_FOUND_TEMPLATE = "Network not found with id: %s";
    private static final String ERROR_NETWORK_UNAVAILABLE_TEMPLATE = "Network is not available: %s";
    private static final String ERROR_ADDRESS_ALREADY_EXISTS_TEMPLATE = "Address already exists on network: %s";
    private static final String ERROR_CORRELATION_ID_REQUIRED = "Correlation ID must be provided";
    private static final String ERROR_CORRELATION_ID_INVALID = "Correlation ID must be a valid UUID";

    /**
     * Creates a new address for a wallet on a specific network.
     *
     * @param walletId the unique identifier of the wallet
     * @param networkId the unique identifier of the network
     * @param publicKeyValue the public key for the address
     * @param accountAddressValue the account address value
     * @param addressType the type of address (EXTERNAL, INTERNAL, CONTRACT)
     * @param derivationPath the BIP44 derivation path
     * @param correlationId a unique identifier used to trace this operation
     * @return the created address instance
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if wallet or network is not active
     */
    public Address createAddress(
            UUID walletId,
            UUID networkId,
            String publicKeyValue,
            String accountAddressValue,
            AddressType addressType,
            String derivationPath,
            String correlationId) {

        String normalizedCorrelation = normalizeCorrelationId(correlationId);
        validateInputs(walletId, networkId, publicKeyValue, accountAddressValue, addressType);

        // Validate wallet exists and is active
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_WALLET_NOT_FOUND_TEMPLATE.formatted(walletId)));
        wallet.validateOperationAllowed();

        // Validate network exists and is active
        Network network = networkRepository.findById(networkId, normalizedCorrelation)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_NETWORK_NOT_FOUND_TEMPLATE.formatted(networkId)));
        if (!network.isAvailable()) {
            throw new IllegalStateException(ERROR_NETWORK_UNAVAILABLE_TEMPLATE.formatted(network.getName()));
        }

        // Check if an address already exists on this network
        if (addressRepository.findByNetworkIdAndAccountAddress(networkId, accountAddressValue).isPresent()) {
            throw new IllegalArgumentException(ERROR_ADDRESS_ALREADY_EXISTS_TEMPLATE.formatted(accountAddressValue));
        }

        // Create value objects
        PublicKey publicKey = new PublicKey(publicKeyValue);
        AccountAddress accountAddress = new AccountAddress(accountAddressValue);

        // Create address
        Address address = Address.create(
                UUID.randomUUID(),
                walletId,
                networkId,
                publicKey,
                accountAddress,
                addressType,
                derivationPath
        );

        // Save address and update wallet
        addressRepository.save(address);
        wallet.addAddress(address.getId());
        walletRepository.update(wallet);

        // Publish events
        address.getDomainEvents().forEach(eventPublisher::publish);
        address.clearEvents();

        return address;
    }

    private void validateInputs(UUID walletId, UUID networkId, String publicKeyValue,
                              String accountAddressValue, AddressType addressType) {
        if (walletId == null) {
            throw new IllegalArgumentException(ERROR_WALLET_ID_REQUIRED);
        }
        if (networkId == null) {
            throw new IllegalArgumentException(ERROR_NETWORK_ID_REQUIRED);
        }
        if (!StringUtils.hasText(publicKeyValue)) {
            throw new IllegalArgumentException(ERROR_PUBLIC_KEY_REQUIRED);
        }
        if (!StringUtils.hasText(accountAddressValue)) {
            throw new IllegalArgumentException(ERROR_ACCOUNT_ADDRESS_REQUIRED);
        }
        if (addressType == null) {
            throw new IllegalArgumentException(ERROR_ADDRESS_TYPE_REQUIRED);
        }
    }

    private String normalizeCorrelationId(String correlationId) {
        if (!StringUtils.hasText(correlationId)) {
            throw new IllegalArgumentException(ERROR_CORRELATION_ID_REQUIRED);
        }

        try {
            UUID parsed = UUID.fromString(correlationId.trim());
            return parsed.toString();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ERROR_CORRELATION_ID_INVALID, ex);
        }
    }
}