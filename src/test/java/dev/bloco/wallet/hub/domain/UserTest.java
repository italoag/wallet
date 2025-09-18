package dev.bloco.wallet.hub.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User Domain Tests")
class UserTest {

    @Test
    @DisplayName("Constructor should set name/email and generate non-null id")
    void constructor_setsFields_andGeneratesId() {
        User user = new User("Alice", "alice@example.com");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getName()).isEqualTo("Alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("Id should remain constant across multiple getter calls")
    void id_isStable() {
        User user = new User("Bob", "bob@example.com");
        UUID id1 = user.getId();
        UUID id2 = user.getId();
        assertThat(id1).isEqualTo(id2);
    }
}
