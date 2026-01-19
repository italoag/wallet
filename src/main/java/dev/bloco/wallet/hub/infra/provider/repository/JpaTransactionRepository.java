package dev.bloco.wallet.hub.infra.provider.repository;

import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.model.transaction.Transaction;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionStatus;
import dev.bloco.wallet.hub.infra.provider.data.repository.SpringDataTransactionRepository;
import dev.bloco.wallet.hub.infra.provider.mapper.TransactionMapper;
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
public class JpaTransactionRepository implements TransactionRepository {
    private final SpringDataTransactionRepository springDataRepository;
    private final TransactionMapper mapper;

    @Override
    public Transaction save(Transaction transaction) {
        var entity = mapper.toEntity(transaction);
        var savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transaction> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findAll() {
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
    public Optional<Transaction> findByHash(String hash) {
        return springDataRepository.findByHash(hash).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findByNetworkId(UUID networkId) {
        return springDataRepository.findByNetworkId(networkId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findByFromAddress(String fromAddress) {
        return springDataRepository.findByFromAddress(fromAddress).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findByToAddress(String toAddress) {
        return springDataRepository.findByToAddress(toAddress).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findByStatus(TransactionStatus status) {
        return springDataRepository.findByStatus(status).stream()
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
    public boolean existsByHash(String hash) {
        return springDataRepository.existsByHash(hash);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findByWalletId(UUID walletId) {
        return springDataRepository.findByWalletId(walletId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findByTimestampBetween(java.time.Instant start, java.time.Instant end) {
        return springDataRepository.findByTimestampBetween(start, end).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
