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
 * ImportAddressUseCase is responsible for importing existing addresses into wallets.
 * This allows users to add pre-existing addresses to their wallet management system.
 * <p/>
 * Business Rules:
 * - Wallet must exist and be active
 * - Network must exist and be active
 * - Address must not already exist in the system
 * - Address format must be valid for the target network
 * - Public key is optional for watch-only addresses
 * <p/>
 * Publishes:
 * - AddressCreatedEvent when address is successfully imported
 */
public record ImportAddressUseCase(
    AddressRepository addressRepository,
    WalletRepository walletRepository,
    NetworkRepository networkRepository,
    DomainEventPublisher eventPublisher,
    ValidateAddressUseCase validateAddressUseCase) {

    private static final String ERROR_WALLET_ID_REQUIRED = "Wallet ID must be provided";
    private static final String ERROR_NETWORK_ID_REQUIRED = "Network ID must be provided";
    private static final String ERROR_ACCOUNT_ADDRESS_REQUIRED = "Account address must be provided";
    private static final String ERROR_WALLET_NOT_FOUND_TEMPLATE = "Wallet not found with id: %s";
    private static final String ERROR_NETWORK_NOT_FOUND_TEMPLATE = "Network not found with id: %s";
    private static final String ERROR_NETWORK_UNAVAILABLE_TEMPLATE = "Network is not available: %s";
    private static final String ERROR_INVALID_ADDRESS_TEMPLATE = "Invalid address format for network: %s";
    private static final String ERROR_ADDRESS_ALREADY_EXISTS_TEMPLATE = "Address already exists in the system: %s";
    private static final String ERROR_PUBLIC_KEY_REQUIRED = "Public key must be provided for non-watch-only addresses";
    private static final String ERROR_CORRELATION_REQUIRED = "Correlation ID must be provided";
    private static final String ERROR_CORRELATION_INVALID = "Correlation ID must be a valid UUID";

    /**
     * Imports an existing address into a wallet.
     *
     * @param walletId the unique identifier of the wallet
     * @param networkId the unique identifier of the network
     * @param accountAddressValue the address value to import
     * @param publicKeyValue the public key (optional for watch-only)
     * @param label a user-friendly label for the address
     * @param isWatchOnly whether this is a watch-only address (no private key)
     * @param correlationId a unique identifier used to trace this operation
     * @return the imported address instance
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if wallet or network is not active
     */
    public Address importAddress(
            UUID walletId,
            UUID networkId,
            String accountAddressValue,
            String publicKeyValue,
            String label,
            boolean isWatchOnly,
            String correlationId) {

        String normalizedCorrelation = normalizeCorrelationId(correlationId);
        validateInputs(walletId, networkId, accountAddressValue);

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

        // Validate address format for the network
        ValidateAddressUseCase.AddressValidationResult validationResult =
            validateAddressUseCase.validateAddress(accountAddressValue, networkId, normalizedCorrelation);
        if (!validationResult.isValid() || !validationResult.isNetworkCompatible()) {
            throw new IllegalArgumentException(ERROR_INVALID_ADDRESS_TEMPLATE.formatted(validationResult.getError()));
        }

        // Check if address already exists
        if (addressRepository.findByNetworkIdAndAccountAddress(networkId, accountAddressValue).isPresent()) {
            throw new IllegalArgumentException(ERROR_ADDRESS_ALREADY_EXISTS_TEMPLATE.formatted(accountAddressValue));
        }

        // For watch-only addresses, we don't require a public key
        PublicKey publicKey = null;
        if (!isWatchOnly) {
            if (!StringUtils.hasText(publicKeyValue)) {
                throw new IllegalArgumentException(ERROR_PUBLIC_KEY_REQUIRED);
            }
            publicKey = new PublicKey(publicKeyValue);
        } else {
            // For watch-only, create a placeholder public key
            publicKey = new PublicKey("watch-only-placeholder");
        }

        // Create value objects
        AccountAddress accountAddress = new AccountAddress(accountAddressValue);
        
        // Determine address type based on import context
        AddressType addressType = isWatchOnly ? AddressType.EXTERNAL : AddressType.EXTERNAL;
        
        // Create derivation path for imported address
        String derivationPath = "imported/" + (label != null ? label : "unlabeled");

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

    /**
     * Imports multiple addresses in batch.
     *
     * @param walletId the unique identifier of the wallet
     * @param networkId the unique identifier of the network
     * @param addressImports array of addresses to import
     * @param correlationId a unique identifier used to trace this operation
     * @return result summary of the batch import
     */
    public BatchImportResult importAddresses(
            UUID walletId,
            UUID networkId,
            AddressImport[] addressImports,
            String correlationId) {

        if (addressImports == null || addressImports.length == 0) {
            return new BatchImportResult(0, 0, new String[0]);
        }

        int successCount = 0;
        int failureCount = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();

        for (AddressImport importRequest : addressImports) {
            try {
                importAddress(
                    walletId,
                    networkId,
                    importRequest.accountAddress(),
                    importRequest.publicKey(),
                    importRequest.label(),
                    importRequest.isWatchOnly(),
                    correlationId
                );
                successCount++;
            } catch (Exception e) {
                failureCount++;
                errors.add("Failed to import " + importRequest.accountAddress() + ": " + e.getMessage());
            }
        }

        return new BatchImportResult(successCount, failureCount, errors.toArray(new String[0]));
    }

    private void validateInputs(UUID walletId, UUID networkId, String accountAddressValue) {
        if (walletId == null) {
            throw new IllegalArgumentException(ERROR_WALLET_ID_REQUIRED);
        }
        if (networkId == null) {
            throw new IllegalArgumentException(ERROR_NETWORK_ID_REQUIRED);
        }
        if (!StringUtils.hasText(accountAddressValue)) {
            throw new IllegalArgumentException(ERROR_ACCOUNT_ADDRESS_REQUIRED);
        }
    }

    private String normalizeCorrelationId(String correlationId) {
        if (!StringUtils.hasText(correlationId)) {
            throw new IllegalArgumentException(ERROR_CORRELATION_REQUIRED);
        }

        try {
            UUID parsed = UUID.fromString(correlationId.trim());
            return parsed.toString();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ERROR_CORRELATION_INVALID, ex);
        }
    }

    /**
     * Represents an address import request.
     */
    public record AddressImport(
        String accountAddress,
        String publicKey,
        String label,
        boolean isWatchOnly
    ) {}

    /**
     * Result of a batch import operation.
     */
    public record BatchImportResult(
        int successCount,
        int failureCount,
        String[] errors
    ) {}
}