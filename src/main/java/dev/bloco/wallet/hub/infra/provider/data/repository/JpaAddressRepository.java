package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AddressStatus;
import dev.bloco.wallet.hub.infra.provider.data.entity.AddressEntity;
import dev.bloco.wallet.hub.infra.provider.mapper.AddressMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JpaAddressRepository implements AddressRepository {

    private final SpringDataAddressRepository springDataAddressRepository;
    private final AddressMapper addressMapper;

    public JpaAddressRepository(SpringDataAddressRepository springDataAddressRepository, AddressMapper addressMapper) {
        this.springDataAddressRepository = springDataAddressRepository;
        this.addressMapper = addressMapper;
    }

    @Override
    public Address save(Address address) {
        AddressEntity entity = addressMapper.toEntity(address);
        AddressEntity savedEntity = springDataAddressRepository.save(entity);
        return addressMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Address> findById(UUID id) {
        return springDataAddressRepository.findById(id)
                .map(addressMapper::toDomain);
    }

    @Override
    public List<Address> findAll() {
        return springDataAddressRepository.findAll().stream()
                .map(addressMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        springDataAddressRepository.deleteById(id);
    }

    @Override
    public List<Address> findByWalletId(UUID walletId) {
        return springDataAddressRepository.findByWalletId(walletId).stream()
                .map(addressMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByNetworkId(UUID networkId) {
        return springDataAddressRepository.findByNetworkId(networkId).stream()
                .map(addressMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Address> findByAccountAddress(String accountAddress) {
        return springDataAddressRepository.findByAccountAddress(accountAddress)
                .map(addressMapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataAddressRepository.existsById(id);
    }

    @Override
    public void update(Address address) {
        AddressEntity entity = addressMapper.toEntity(address);
        springDataAddressRepository.save(entity);
    }

    @Override
    public List<Address> findByWalletIdAndStatus(UUID walletId, AddressStatus status) {
        return springDataAddressRepository.findByWalletIdAndStatus(walletId, status.name()).stream()
                .map(addressMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Address> findByNetworkIdAndAccountAddress(UUID networkId, String accountAddress) {
        return springDataAddressRepository.findByNetworkIdAndAccountAddress(networkId, accountAddress)
                .map(addressMapper::toDomain);
    }
}
