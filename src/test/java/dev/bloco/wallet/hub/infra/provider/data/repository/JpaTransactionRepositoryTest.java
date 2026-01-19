package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.model.transaction.Transaction;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionHash;
import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;
import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;
import dev.bloco.wallet.hub.infra.provider.mapper.TransactionMapper;
import dev.bloco.wallet.hub.infra.provider.repository.JpaTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
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
    @DisplayName("save should map domain to entity and back to domain")
    void save_shouldMapDomainToEntity_andBackToDomain() {
        UUID id = UUID.randomUUID();
        UUID networkId = UUID.randomUUID();
        Transaction domain = Transaction.create(id, networkId, new TransactionHash("0xhash"), "0xfrom", "0xto",
                new BigDecimal("12.34"), null);

        TransactionEntity entity = new TransactionEntity();
        entity.setId(id);
        entity.setNetworkId(networkId);
        entity.setHash("0xhash");
        entity.setFromAddress("0xfrom");
        entity.setToAddress("0xto");
        entity.setValue(new BigDecimal("12.34"));
        entity.setTimestamp(Instant.now());

        TransactionEntity savedEntity = new TransactionEntity();
        savedEntity.setId(id);
        savedEntity.setNetworkId(networkId);
        savedEntity.setHash("0xhash");
        savedEntity.setFromAddress("0xfrom");
        savedEntity.setToAddress("0xto");
        savedEntity.setValue(new BigDecimal("12.34"));
        savedEntity.setTimestamp(Instant.now());

        Transaction mappedBack = Transaction.create(id, networkId, new TransactionHash("0xhash"), "0xfrom", "0xto",
                new BigDecimal("12.34"), null);

        when(transactionMapper.toEntity(domain)).thenReturn(entity);
        when(springDataTransactionRepository.save(entity)).thenReturn(savedEntity);
        when(transactionMapper.toDomain(savedEntity)).thenReturn(mappedBack);

        Transaction result = repository.save(domain);

        assertThat(result).isSameAs(mappedBack);

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(springDataTransactionRepository).save(captor.capture());
        TransactionEntity captured = captor.getValue();
        assertThat(captured.getNetworkId()).isEqualTo(networkId);
        assertThat(captured.getHash()).isEqualTo("0xhash");
        assertThat(captured.getFromAddress()).isEqualTo("0xfrom");
        assertThat(captured.getToAddress()).isEqualTo("0xto");
        assertThat(captured.getValue()).isEqualByComparingTo("12.34");

        verify(transactionMapper).toEntity(domain);
        verify(transactionMapper).toDomain(savedEntity);
        verifyNoMoreInteractions(transactionMapper, springDataTransactionRepository);
    }

    @Test
    @DisplayName("findByNetworkId should delegate to SpringData repo and map all results")
    void findByNetworkId_shouldDelegateToSpringData_andMapAll() {
        UUID networkId = UUID.randomUUID();

        TransactionEntity e1 = new TransactionEntity();
        e1.setId(UUID.randomUUID());
        e1.setNetworkId(networkId);
        e1.setHash("0x1");
        e1.setFromAddress("0xA");
        e1.setToAddress("0xB");
        e1.setValue(new BigDecimal("1.00"));
        e1.setTimestamp(Instant.now());

        TransactionEntity e2 = new TransactionEntity();
        e2.setId(UUID.randomUUID());
        e2.setNetworkId(networkId);
        e2.setHash("0x2");
        e2.setFromAddress("0xC");
        e2.setToAddress("0xD");
        e2.setValue(new BigDecimal("2.00"));
        e2.setTimestamp(Instant.now());

        Transaction d1 = Transaction.create(e1.getId(), networkId, new TransactionHash("0x1"), "0xA", "0xB",
                new BigDecimal("1.00"), null);
        Transaction d2 = Transaction.create(e2.getId(), networkId, new TransactionHash("0x2"), "0xC", "0xD",
                new BigDecimal("2.00"), null);

        when(springDataTransactionRepository.findByNetworkId(networkId)).thenReturn(List.of(e1, e2));
        when(transactionMapper.toDomain(e1)).thenReturn(d1);
        when(transactionMapper.toDomain(e2)).thenReturn(d2);

        List<Transaction> result = repository.findByNetworkId(networkId);

        assertThat(result).containsExactly(d1, d2);
        verify(springDataTransactionRepository).findByNetworkId(networkId);
        verify(transactionMapper).toDomain(e1);
        verify(transactionMapper).toDomain(e2);
        verifyNoMoreInteractions(transactionMapper, springDataTransactionRepository);
    }
}
