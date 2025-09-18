package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.WalletEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;

@DisplayName("Spring Data Wallet Repository Tests")
@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
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
