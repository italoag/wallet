package dev.bloco.wallet.hub.domain;

import dev.bloco.wallet.hub.domain.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User Domain Tests")
class UserTest {

    @Test
    @DisplayName("Create method should set name/email and generate non-null id")
    void create_setsFields_andGeneratesId() {
        UUID id = UUID.randomUUID();
        User user = User.create(id, "Alice", "alice@example.com", "password");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getName()).isEqualTo("Alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("Id should remain constant across multiple getter calls")
    void id_isStable() {
        UUID id = UUID.randomUUID();
        User user = User.create(id, "Bob", "bob@example.com", "password");
        UUID id1 = user.getId();
        UUID id2 = user.getId();
        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isEqualTo(id);
    }
}
