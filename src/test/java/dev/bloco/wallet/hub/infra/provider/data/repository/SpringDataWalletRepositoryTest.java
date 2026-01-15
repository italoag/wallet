package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.WalletEntity;
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

@DisplayName("Spring Data Wallet Repository Tests")
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false"
})
class SpringDataWalletRepositoryTest {

    @Autowired
    private SpringDataWalletRepository repository;

    @Test
    @DisplayName("Save and find by id")
    void saveAndFindById_roundTrip() {
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(UUID.randomUUID());
        wallet.setBalance(new BigDecimal("99.99"));

        WalletEntity persisted = repository.save(wallet);
        assertThat(persisted.getId()).isNotNull();

        Optional<WalletEntity> reloaded = repository.findById(persisted.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getUserId()).isEqualTo(wallet.getUserId());
        assertThat(reloaded.get().getBalance()).isEqualByComparingTo("99.99");
    }
}
