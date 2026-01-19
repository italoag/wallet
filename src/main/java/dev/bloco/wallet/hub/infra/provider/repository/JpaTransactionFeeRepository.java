package dev.bloco.wallet.hub.infra.provider.repository;

import dev.bloco.wallet.hub.domain.gateway.TransactionFeeRepository;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionFee;
import dev.bloco.wallet.hub.infra.provider.data.repository.SpringDataTransactionFeeRepository;
import dev.bloco.wallet.hub.infra.provider.mapper.TransactionFeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional
public class JpaTransactionFeeRepository implements TransactionFeeRepository {
    private final SpringDataTransactionFeeRepository springDataRepository;
    private final TransactionFeeMapper mapper;

    @Override
    public TransactionFee save(TransactionFee fee) {
        var entity = mapper.toEntity(fee);
        var savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransactionFee> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionFee> findAll() {
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
    public List<TransactionFee> findByNetworkId(UUID networkId) {
        return springDataRepository.findByNetworkId(networkId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return springDataRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionFee> findByNetworkIdAndLevel(UUID networkId,
            dev.bloco.wallet.hub.domain.model.transaction.FeeLevel level) {
        return springDataRepository.findByNetworkIdAndFeeLevel(networkId, level).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransactionFee> findLatestByNetworkIdAndLevel(UUID networkId,
            dev.bloco.wallet.hub.domain.model.transaction.FeeLevel level) {
        return springDataRepository.findFirstByNetworkIdAndFeeLevelOrderByTimestampDesc(networkId, level)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteOldEstimates(int maxAgeSeconds) {
        java.time.Instant cutoff = java.time.Instant.now().minusSeconds(maxAgeSeconds);
        springDataRepository.deleteByTimestampBefore(cutoff);
    }
}
