package dev.bloco.wallet.hub.infra.provider.data.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Wallet Entity Tests")
class WalletEntityTest {

    @Test
    @DisplayName("Gets and sets all fields")
    void gettersAndSetters_shouldWorkForAllFields() {
        // given
        WalletEntity wallet = new WalletEntity();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal balance = new BigDecimal("999.99");

        // when
        wallet.setId(id);
        wallet.setUserId(userId);
        wallet.setBalance(balance);

        // then
        assertThat(wallet.getId()).isEqualTo(id);
        assertThat(wallet.getUserId()).isEqualTo(userId);
        assertThat(wallet.getBalance()).isEqualByComparingTo(balance);
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void equals_shouldReturnTrueForSameInstance() {
        WalletEntity wallet = new WalletEntity();
        assertThat(wallet.equals(wallet)).isTrue();
    }

    @Test
    @DisplayName("equals should be based on ID when non-null")
    void equals_shouldBeBasedOnIdWhenNonNull() {
        UUID id = UUID.randomUUID();
        WalletEntity a = new WalletEntity();
        a.setId(id);
        WalletEntity b = new WalletEntity();
        b.setId(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("equals should return false when IDs differ or are null")
    void equals_shouldReturnFalseWhenIdsDifferOrNull() {
        WalletEntity a = new WalletEntity();
        a.setId(UUID.randomUUID());
        WalletEntity b = new WalletEntity();
        b.setId(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);

        WalletEntity c = new WalletEntity();
        WalletEntity d = new WalletEntity();
        d.setId(UUID.randomUUID());
        assertThat(c).isNotEqualTo(d);
        assertThat(c).isNotEqualTo(null);
        assertThat(c).isNotEqualTo("some other type");
    }

    @Test
    @DisplayName("toString should contain field names")
    void toString_shouldContainFieldNames() {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(UUID.randomUUID());
        wallet.setUserId(UUID.randomUUID());
        wallet.setBalance(new BigDecimal("1.23"));

        String s = wallet.toString();
        assertThat(s).contains("WalletEntity");
        assertThat(s).contains("id=");
        assertThat(s).contains("userId=");
        assertThat(s).contains("balance=");
    }
}
