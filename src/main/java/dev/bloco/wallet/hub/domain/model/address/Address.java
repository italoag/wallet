package dev.bloco.wallet.hub.domain.model.address;

import dev.bloco.wallet.hub.domain.event.address.AddressCreatedEvent;
import dev.bloco.wallet.hub.domain.event.address.AddressStatusChangedEvent;
import dev.bloco.wallet.hub.domain.model.common.AggregateRoot;

import java.util.UUID;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    private Address(
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

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public AccountAddress getAccountAddress() {
        return accountAddress;
    }

    public AddressType getType() {
        return type;
    }

    public String getDerivationPath() {
        return derivationPath;
    }

    public AddressStatus getStatus() {
        return status;
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