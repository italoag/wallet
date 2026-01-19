package dev.bloco.wallet.hub.infra.provider.data.entity;

import dev.bloco.wallet.hub.domain.model.address.AddressStatus;
import dev.bloco.wallet.hub.domain.model.address.AddressType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity representing an Address in the database.
 * Maps to the "addresses" table.
 */
@Setter
@Getter
@Entity
@Table(name = "addresses", indexes = {
        @Index(name = "idx_addresses_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_addresses_network_id", columnList = "network_id"),
        @Index(name = "idx_account_address", columnList = "account_address"),
        @Index(name = "idx_wallet_status", columnList = "wallet_id,status")
})
public class AddressEntity {

    @Id
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "network_id", nullable = false)
    private UUID networkId;

    @Column(name = "public_key", nullable = false, length = 512)
    private String publicKey;

    @Column(name = "account_address", nullable = false, unique = true, length = 256)
    private String accountAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AddressType type;

    @Column(name = "derivation_path", length = 256)
    private String derivationPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AddressStatus status;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "address_transactions", joinColumns = @JoinColumn(name = "address_id"))
    @Column(name = "transaction_id")
    private Set<UUID> transactionIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "address_token_balances", joinColumns = @JoinColumn(name = "address_id"))
    @Column(name = "token_balance_id")
    private Set<UUID> tokenBalanceIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "address_owned_contracts", joinColumns = @JoinColumn(name = "address_id"))
    @Column(name = "contract_id")
    private Set<UUID> ownedContractIds = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAccountAddress() {
        return accountAddress;
    }

    public void setAccountAddress(String accountAddress) {
        this.accountAddress = accountAddress;
    }

    public AddressType getType() {
        return type;
    }

    public void setType(AddressType type) {
        this.type = type;
    }

    public String getDerivationPath() {
        return derivationPath;
    }

    public void setDerivationPath(String derivationPath) {
        this.derivationPath = derivationPath;
    }

    public AddressStatus getStatus() {
        return status;
    }

    public void setStatus(AddressStatus status) {
        this.status = status;
    }

    public Set<UUID> getTransactionIds() {
        return transactionIds;
    }

    public void setTransactionIds(Set<UUID> transactionIds) {
        this.transactionIds = transactionIds;
    }

    public Set<UUID> getTokenBalanceIds() {
        return tokenBalanceIds;
    }

    public void setTokenBalanceIds(Set<UUID> tokenBalanceIds) {
        this.tokenBalanceIds = tokenBalanceIds;
    }

    public Set<UUID> getOwnedContractIds() {
        return ownedContractIds;
    }

    public void setOwnedContractIds(Set<UUID> ownedContractIds) {
        this.ownedContractIds = ownedContractIds;
    }

}
