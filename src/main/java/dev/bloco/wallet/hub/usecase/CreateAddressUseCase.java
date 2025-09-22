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

        // Validate inputs
        validateInputs(walletId, networkId, publicKeyValue, accountAddressValue, addressType);

        // Validate wallet exists and is active
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));
        wallet.validateOperationAllowed();

        // Validate network exists and is active
        Network network = networkRepository.findById(networkId)
                .orElseThrow(() -> new IllegalArgumentException("Network not found with id: " + networkId));
        if (!network.isAvailable()) {
            throw new IllegalStateException("Network is not available: " + network.getName());
        }

        // Check if address already exists on this network
        if (addressRepository.findByNetworkIdAndAccountAddress(networkId, accountAddressValue).isPresent()) {
            throw new IllegalArgumentException("Address already exists on network: " + accountAddressValue);
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
            throw new IllegalArgumentException("Wallet ID must be provided");
        }
        if (networkId == null) {
            throw new IllegalArgumentException("Network ID must be provided");
        }
        if (publicKeyValue == null || publicKeyValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Public key must be provided");
        }
        if (accountAddressValue == null || accountAddressValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Account address must be provided");
        }
        if (addressType == null) {
            throw new IllegalArgumentException("Address type must be provided");
        }
    }
}