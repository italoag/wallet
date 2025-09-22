package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AddressStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository {
    Address save(Address address);
    
    Optional<Address> findById(UUID id);
    
    List<Address> findAll();
    
    void delete(UUID id);
    
    List<Address> findByWalletId(UUID walletId);
    
    List<Address> findByNetworkId(UUID networkId);
    
    Optional<Address> findByAccountAddress(String accountAddress);
    
    boolean existsById(UUID id);
    
    void update(Address address);
    
    List<Address> findByWalletIdAndStatus(UUID walletId, AddressStatus status);
    
    Optional<Address> findByNetworkIdAndAccountAddress(UUID networkId, String accountAddress);
}