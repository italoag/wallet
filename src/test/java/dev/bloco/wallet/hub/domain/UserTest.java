package dev.bloco.wallet.hub.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User Tests")
class UserTest {

    @Test
    @DisplayName("User is created with random id and provided attributes")
    void userConstructor() {
        String name = "Alice";
        String email = "alice@example.com";

        User user = new User(name, email);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getEmail()).isEqualTo(email);
    }
}
