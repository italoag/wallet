package dev.bloco.wallet.hub.domain.model.store;

import dev.bloco.wallet.hub.domain.event.store.AddressAddedToStoreEvent;
import dev.bloco.wallet.hub.domain.event.store.StoreCreatedEvent;
import dev.bloco.wallet.hub.domain.event.store.StoreStatusChangedEvent;
import dev.bloco.wallet.hub.domain.model.common.AggregateRoot;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Store extends AggregateRoot {
    private String name;
    private final UUID vaultId;
    private String description;
    private StoreStatus status;
    private final Set<UUID> addressIds = new HashSet<>();

    public static Store create(
            UUID id,
            String name,
            UUID vaultId,
            String description) {
        
        Store store = new Store(id, name, vaultId, description);
        store.registerEvent(new StoreCreatedEvent(id, vaultId, name, null));
        return store;
    }

    private Store(
            UUID id,
            String name,
            UUID vaultId,
            String description) {
        super(id);
        this.name = name;
        this.vaultId = vaultId;
        this.description = description;
        this.status = StoreStatus.ACTIVE;
    }

    public String getName() {
        return name;
    }

    public UUID getVaultId() {
        return vaultId;
    }

    public String getDescription() {
        return description;
    }

    public StoreStatus getStatus() {
        return status;
    }

    public Set<UUID> getAddressIds() {
        return Collections.unmodifiableSet(addressIds);
    }

    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void activate() {
        if (this.status != StoreStatus.ACTIVE) {
            StoreStatus oldStatus = this.status;
            this.status = StoreStatus.ACTIVE;
            registerEvent(new StoreStatusChangedEvent(getId(), oldStatus, this.status, null));
        }
    }

    public void deactivate() {
        if (this.status != StoreStatus.INACTIVE) {
            StoreStatus oldStatus = this.status;
            this.status = StoreStatus.INACTIVE;
            registerEvent(new StoreStatusChangedEvent(getId(), oldStatus, this.status, null));
        }
    }

    public void addAddress(UUID addressId) {
        if (addressIds.add(addressId)) {
            registerEvent(new AddressAddedToStoreEvent(getId(), addressId, null));
        }
    }

    public void removeAddress(UUID addressId) {
        addressIds.remove(addressId);
    }

    public boolean containsAddress(UUID addressId) {
        return addressIds.contains(addressId);
    }

    public boolean isActive() {
        return this.status == StoreStatus.ACTIVE;
    }
}