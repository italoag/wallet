package dev.bloco.wallet.hub.infra.provider.data.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User Entity Tests")
class UserEntityTest {

    @Test
    @DisplayName("Gets and sets all fields")
    void gettersAndSetters_shouldWorkForAllFields() {
        // given
        UserEntity user = new UserEntity();
        UUID id = UUID.randomUUID();
        String name = "Maria Luiza";
        String email = "malu@example.com";

        // when
        user.setId(id);
        user.setName(name);
        user.setEmail(email);

        // then
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Default values should be null before setting")
      void defaultValues_shouldBeNullBeforeSetting() {
        UserEntity user = new UserEntity();
        assertThat(user.getId()).isNull();
        assertThat(user.getName()).isNull();
        assertThat(user.getEmail()).isNull();
    }
}
