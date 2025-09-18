package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.Transaction;
import dev.bloco.wallet.hub.domain.Transaction.TransactionType;
import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;
import dev.bloco.wallet.hub.infra.provider.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("JPA Transaction Repository Tests")
@ExtendWith(MockitoExtension.class)
class JpaTransactionRepositoryTest {

    @Mock
    private SpringDataTransactionRepository springDataTransactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    private JpaTransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaTransactionRepository(springDataTransactionRepository, transactionMapper);
    }

    @Test
    @DisplayName("Save should delegate to SpringDataUserRepository")
    void save_shouldMapDomainToEntity_andBackToDomain() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        Transaction domain = new Transaction(from, to, new BigDecimal("12.34"), TransactionType.TRANSFER);

        TransactionEntity entity = new TransactionEntity();
        entity.setFromWalletId(from);
        entity.setToWalletId(to);
        entity.setAmount(new BigDecimal("12.34"));
        entity.setTimestamp(LocalDateTime.now());
        entity.setType(TransactionEntity.TransactionType.TRANSFER);

        TransactionEntity savedEntity = new TransactionEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setFromWalletId(from);
        savedEntity.setToWalletId(to);
        savedEntity.setAmount(new BigDecimal("12.34"));
        savedEntity.setTimestamp(LocalDateTime.now());
        savedEntity.setType(TransactionEntity.TransactionType.TRANSFER);

        Transaction mappedBack = new Transaction(from, to, new BigDecimal("12.34"), TransactionType.TRANSFER);

        when(transactionMapper.toEntity(domain)).thenReturn(entity);
        when(springDataTransactionRepository.save(entity)).thenReturn(savedEntity);
        when(transactionMapper.toDomain(savedEntity)).thenReturn(mappedBack);

        Transaction result = repository.save(domain);

        assertThat(result).isSameAs(mappedBack);

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(springDataTransactionRepository).save(captor.capture());
        TransactionEntity captured = captor.getValue();
        assertThat(captured.getFromWalletId()).isEqualTo(from);
        assertThat(captured.getToWalletId()).isEqualTo(to);
        assertThat(captured.getAmount()).isEqualByComparingTo("12.34");
        assertThat(captured.getType()).isEqualTo(TransactionEntity.TransactionType.TRANSFER);

        verify(transactionMapper).toEntity(domain);
        verify(transactionMapper).toDomain(savedEntity);
        verifyNoMoreInteractions(transactionMapper, springDataTransactionRepository);
    }

    @Test
    @DisplayName("findByWalletId should delegate to SpringDataUserRepository")
    void findByWalletId_shouldDelegateToSpringData_andMapAll() {
        UUID walletId = UUID.randomUUID();

        TransactionEntity e1 = new TransactionEntity();
        e1.setId(UUID.randomUUID());
        e1.setFromWalletId(walletId);
        e1.setToWalletId(UUID.randomUUID());
        e1.setAmount(new BigDecimal("1.00"));
        e1.setTimestamp(LocalDateTime.now());
        e1.setType(TransactionEntity.TransactionType.DEPOSIT);

        TransactionEntity e2 = new TransactionEntity();
        e2.setId(UUID.randomUUID());
        e2.setFromWalletId(UUID.randomUUID());
        e2.setToWalletId(walletId);
        e2.setAmount(new BigDecimal("2.00"));
        e2.setTimestamp(LocalDateTime.now());
        e2.setType(TransactionEntity.TransactionType.WITHDRAWAL);

        Transaction d1 = new Transaction(e1.getFromWalletId(), e1.getToWalletId(), e1.getAmount(), TransactionType.DEPOSIT);
        Transaction d2 = new Transaction(e2.getFromWalletId(), e2.getToWalletId(), e2.getAmount(), TransactionType.WITHDRAWAL);

        when(springDataTransactionRepository.findByFromWalletIdOrToWalletId(walletId, walletId)).thenReturn(List.of(e1, e2));
        when(transactionMapper.toDomain(e1)).thenReturn(d1);
        when(transactionMapper.toDomain(e2)).thenReturn(d2);

        List<Transaction> result = repository.findByWalletId(walletId);

        assertThat(result).containsExactly(d1, d2);
        verify(springDataTransactionRepository).findByFromWalletIdOrToWalletId(walletId, walletId);
        verify(transactionMapper).toDomain(e1);
        verify(transactionMapper).toDomain(e2);
        verifyNoMoreInteractions(transactionMapper, springDataTransactionRepository);
    }
}
