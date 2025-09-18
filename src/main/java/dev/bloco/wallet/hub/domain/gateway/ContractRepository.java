package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.contract.Contract;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository {
    Contract save(Contract contract);
    
    Optional<Contract> findById(UUID id);
    
    List<Contract> findAll();
    
    void delete(UUID id);
    
    List<Contract> findByNetworkId(UUID networkId);
    
    Optional<Contract> findByNetworkIdAndAddress(UUID networkId, String address);
    
    List<Contract> findByOwnerAddressId(UUID addressId);
    
    boolean existsById(UUID id);
}