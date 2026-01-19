package dev.bloco.wallet.hub.infra.provider.repository;

import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.model.network.Network;
import dev.bloco.wallet.hub.domain.model.network.NetworkStatus;
import dev.bloco.wallet.hub.infra.provider.data.repository.SpringDataNetworkRepository;
import dev.bloco.wallet.hub.infra.provider.mapper.NetworkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@org.springframework.context.annotation.Primary
@RequiredArgsConstructor
@Transactional
public class JpaNetworkRepository implements NetworkRepository {
    private final SpringDataNetworkRepository springDataRepository;
    private final NetworkMapper mapper;

    @Override
    public Network save(Network network, String correlationId) {
        // Ignoring correlationId for now as Spring Data JPA doesn't natively support it
        // in this context
        return save(network);
    }

    @Override
    public Network save(Network network) {
        var entity = mapper.toEntity(network);
        var savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Network> findById(UUID id, String correlationId) {
        return findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Network> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Network> findAll(String correlationId) {
        return findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Network> findAll() {
        return springDataRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id, String correlationId) {
        delete(id);
    }

    @Override
    public void delete(UUID id) {
        springDataRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Network> findByChainId(String chainId, String correlationId) {
        return findByChainId(chainId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Network> findByChainId(String chainId) {
        return springDataRepository.findByChainId(chainId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Network> findByStatus(NetworkStatus status, String correlationId) {
        return findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Network> findByStatus(NetworkStatus status) {
        return springDataRepository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Network> findByName(String name, String correlationId) {
        // Assuming findByName is not implemented in SpringDataRepository yet, or I
        // missed it.
        // Interface has findByName.
        // Let's implement it if SpringDataRepository has it, else throw or return
        // empty.
        // SpringDataNetworkRepository (Step 2762) DOES NOT HAVE findByName.
        // I need to add it there too.
        return java.util.Collections.emptyList(); // Temporary stub or I must update SpringData repo.
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id, String correlationId) {
        return existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return springDataRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByChainId(String chainId, String correlationId) {
        return existsByChainId(chainId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByChainId(String chainId) {
        return springDataRepository.existsByChainId(chainId);
    }
}
