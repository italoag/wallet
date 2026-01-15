package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@DisplayName("Spring Data Transaction Repository Tests")
@AutoConfigureTestDatabase(replace = Replace.ANY)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false"
})
class SpringDataTransactionRepositoryTest {

    @Autowired
    private SpringDataTransactionRepository repository;

    @Test
    @DisplayName("Save and find by id should round trip with blockchain schema fields")
    void saveAndFindById_roundTrip() {
        TransactionEntity tx = new TransactionEntity();
        tx.setNetworkId(UUID.randomUUID());
        tx.setHash("0xabc");
        tx.setFromAddress("0xfrom");
        tx.setToAddress("0xto");
        tx.setValue(new BigDecimal("42.00"));
        tx.setTimestamp(java.time.Instant.now());
        tx.setStatus(dev.bloco.wallet.hub.domain.model.transaction.TransactionStatus.PENDING);

        TransactionEntity persisted = repository.save(tx);
        assertThat(persisted.getId()).isNotNull();

        Optional<TransactionEntity> reloaded = repository.findById(persisted.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getNetworkId()).isEqualTo(tx.getNetworkId());
        assertThat(reloaded.get().getHash()).isEqualTo("0xabc");
        assertThat(reloaded.get().getFromAddress()).isEqualTo("0xfrom");
        assertThat(reloaded.get().getToAddress()).isEqualTo("0xto");
        assertThat(reloaded.get().getValue()).isEqualByComparingTo("42.00");
        assertThat(reloaded.get().getStatus()).isEqualTo(dev.bloco.wallet.hub.domain.model.transaction.TransactionStatus.PENDING);
    }
}
