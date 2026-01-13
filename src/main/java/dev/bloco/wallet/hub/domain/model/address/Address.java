package dev.bloco.wallet.hub.domain.model.address;

import dev.bloco.wallet.hub.domain.event.address.AddressCreatedEvent;
import dev.bloco.wallet.hub.domain.event.address.AddressStatusChangedEvent;
import dev.bloco.wallet.hub.domain.model.common.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a blockchain address associated with a wallet and network.
 * This address contains a unique public key, account information,
 * and associated transactions, balances, and owned contracts.
 * It also maintains its type, status, and derivation path metadata.
 *<p/>
 *This class extends from {@code AggregateRoot} and thus supports domain event tracking.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class Address extends AggregateRoot {
    private final UUID walletId;
    private final UUID networkId;
    private final PublicKey publicKey;
    private final AccountAddress accountAddress;
    private final AddressType type;
    private final String derivationPath;
    private AddressStatus status;
    private final Set<UUID> transactionIds = new HashSet<>();
    private final Set<UUID> tokenBalanceIds = new HashSet<>();
    private final Set<UUID> ownedContractIds = new HashSet<>();

  /**
   * Creates a new Address instance with the provided parameters and registers an
   * AddressCreatedEvent for tracking the creation of the address.
   *
   * @param id the unique identifier of the address
   * @param walletId the identifier of the wallet associated with this address
   * @param networkId the identifier of the network to which this address belongs
   * @param publicKey the public key associated with this address
   * @param accountAddress the account address representation of this address
   * @param type the type of the address (e.g., EXTERNAL, INTERNAL, CONTRACT)
   * @param derivationPath the derivation path used to generate this address
   * @return a new Address instance with the specified parameters
   */
    public static Address create(
            UUID id,
            UUID walletId,
            UUID networkId,
            PublicKey publicKey,
            AccountAddress accountAddress,
            AddressType type,
            String derivationPath) {
        
        Address address = new Address(
            id, walletId, networkId, publicKey, accountAddress, type, derivationPath
        );
        address.registerEvent(new AddressCreatedEvent(id, walletId, networkId, accountAddress.getValue(), null));
        return address;
    }

  /**
   * Constructs a new Address instance with the specified properties.
   *
   * @param id the unique identifier of the address
   * @param walletId the identifier of the wallet associated with this address
   * @param networkId the identifier of the network to which this address belongs
   * @param publicKey the public key associated with this address
   * @param accountAddress the account address representation of the address
   * @param type the type of the address (e.g., EXTERNAL, INTERNAL, CONTRACT)
   * @param derivationPath the derivation path used to generate this address
   */
    public Address(
        UUID id,
        UUID walletId,
        UUID networkId,
        PublicKey publicKey,
        AccountAddress accountAddress,
        AddressType type,
        String derivationPath) {
        super(id);
        this.walletId = walletId;
        this.networkId = networkId;
        this.publicKey = publicKey;
        this.accountAddress = accountAddress;
        this.type = type;
        this.derivationPath = derivationPath;
        this.status = AddressStatus.ACTIVE;
    }

  public Set<UUID> getTransactionIds() {
        return Collections.unmodifiableSet(transactionIds);
    }

    public Set<UUID> getTokenBalanceIds() {
        return Collections.unmodifiableSet(tokenBalanceIds);
    }

    public Set<UUID> getOwnedContractIds() {
        return Collections.unmodifiableSet(ownedContractIds);
    }

    public void archive() {
        if (this.status != AddressStatus.ARCHIVED) {
            AddressStatus oldStatus = this.status;
            this.status = AddressStatus.ARCHIVED;
            registerEvent(new AddressStatusChangedEvent(getId(), oldStatus, this.status, null));
        }
    }

    public void activate() {
        if (this.status != AddressStatus.ACTIVE) {
            AddressStatus oldStatus = this.status;
            this.status = AddressStatus.ACTIVE;
            registerEvent(new AddressStatusChangedEvent(getId(), oldStatus, this.status, null));
        }
    }

    public void addTransaction(UUID transactionId) {
        transactionIds.add(transactionId);
    }

    public void addTokenBalance(UUID tokenBalanceId) {
        tokenBalanceIds.add(tokenBalanceId);
    }

    public void addOwnedContract(UUID contractId) {
        ownedContractIds.add(contractId);
    }

    public void removeOwnedContract(UUID contractId) {
        ownedContractIds.remove(contractId);
    }

    public boolean canSign() {
        return this.type != AddressType.CONTRACT && this.status == AddressStatus.ACTIVE;
    }
}