package dev.bloco.wallet.hub.infra.provider.repository;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AddressStatus;
import dev.bloco.wallet.hub.infra.provider.data.repository.SpringDataAddressRepository;
import dev.bloco.wallet.hub.infra.provider.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA implementation of AddressRepository.
 * Adapts the domain repository interface to Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
@Transactional
public class JpaAddressRepository implements AddressRepository {

    private final SpringDataAddressRepository springDataRepository;
    private final AddressMapper mapper;

    @Override
    public Address save(Address address) {
        var entity = mapper.toEntity(address);
        var savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Address> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> findAll() {
        return springDataRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        springDataRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> findByWalletId(UUID walletId) {
        return springDataRepository.findByWalletId(walletId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> findByNetworkId(UUID networkId) {
        return springDataRepository.findByNetworkId(networkId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Address> findByAccountAddress(String accountAddress) {
        return springDataRepository.findByAccountAddress(accountAddress)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public void update(Address address) {
        // In JPA, save() handles both insert and update
        save(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> findByWalletIdAndStatus(UUID walletId, AddressStatus status) {
        return springDataRepository.findByWalletIdAndStatus(walletId, status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Address> findByNetworkIdAndAccountAddress(UUID networkId, String accountAddress) {
        return springDataRepository.findByNetworkIdAndAccountAddress(networkId, accountAddress)
                .map(mapper::toDomain);
    }
}
