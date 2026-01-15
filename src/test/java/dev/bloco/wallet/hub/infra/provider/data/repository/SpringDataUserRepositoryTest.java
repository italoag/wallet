package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DisplayName("Spring Data User Repository Tests")
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false"
})
class SpringDataUserRepositoryTest {

    @Autowired
    private SpringDataUserRepository repository;

    @Test
    @DisplayName("Save and find by id")
    void saveAndFindById_roundTrip() {
        UserEntity user = new UserEntity();
        user.setName("Carol");
        user.setEmail("carol@example.com");

        UserEntity persisted = repository.save(user);
        assertThat(persisted.getId()).isNotNull();

        Optional<UserEntity> reloaded = repository.findById(persisted.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getName()).isEqualTo("Carol");
        assertThat(reloaded.get().getEmail()).isEqualTo("carol@example.com");
    }
}
