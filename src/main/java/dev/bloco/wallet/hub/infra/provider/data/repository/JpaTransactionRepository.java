package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.Transaction;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;
import dev.bloco.wallet.hub.infra.provider.mapper.TransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JpaTransactionRepository implements TransactionRepository {
    private final SpringDataTransactionRepository springDataTransactionRepository;
    private final TransactionMapper transactionMapper;

    @Autowired
    public JpaTransactionRepository(SpringDataTransactionRepository springDataTransactionRepository, TransactionMapper transactionMapper) {
        this.springDataTransactionRepository = springDataTransactionRepository;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = transactionMapper.toEntity(transaction);
        return transactionMapper.toDomain(springDataTransactionRepository.save(entity));
    }

    @Override
    public List<Transaction> findByWalletId(UUID walletId) {
        return springDataTransactionRepository.findByFromWalletIdOrToWalletId(walletId).stream()
                .map(transactionMapper::toDomain)
                .collect(Collectors.toList());
    }
}
