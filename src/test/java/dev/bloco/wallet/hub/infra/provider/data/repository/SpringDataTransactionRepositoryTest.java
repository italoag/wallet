package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@DisplayName("Spring Data Transaction Repository Tests")
@AutoConfigureTestDatabase(replace = ANY)
class SpringDataTransactionRepositoryTest {

    @Autowired
    private SpringDataTransactionRepository repository;

    @Test
    @DisplayName("Save and find by id should round trip")
    void saveAndFindById_roundTrip() {
        TransactionEntity tx = new TransactionEntity();
        tx.setFromWalletId(UUID.randomUUID());
        tx.setToWalletId(UUID.randomUUID());
        tx.setAmount(new BigDecimal("42.00"));
        tx.setTimestamp(LocalDateTime.now());
        tx.setType(TransactionEntity.TransactionType.TRANSFER);

        TransactionEntity persisted = repository.save(tx);
        assertThat(persisted.getId()).isNotNull();

        Optional<TransactionEntity> reloaded = repository.findById(persisted.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getFromWalletId()).isEqualTo(tx.getFromWalletId());
        assertThat(reloaded.get().getToWalletId()).isEqualTo(tx.getToWalletId());
        assertThat(reloaded.get().getAmount()).isEqualByComparingTo("42.00");
        assertThat(reloaded.get().getType()).isEqualTo(TransactionEntity.TransactionType.TRANSFER);
    }
}
