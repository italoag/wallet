package dev.bloco.wallet.hub.domain;

import dev.bloco.wallet.hub.domain.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Wallet Tests")
class WalletTest {

    @Test
    @DisplayName("New wallet starts with zero balance and has generated id")
    void newWalletDefaults() {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");

        assertThat(wallet.getId()).isNotNull();
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("addFunds increases the balance for positive amounts")
    void addFundsSuccess() {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");

        wallet.addFunds(new BigDecimal("10.50"));
        wallet.addFunds(new BigDecimal("0.50"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("11.00"));
    }

    @Test
    @DisplayName("addFunds rejects zero or negative values")
    void addFundsInvalid() {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");

        BigDecimal zero = BigDecimal.ZERO;
        assertThatThrownBy(() -> wallet.addFunds(zero))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
        BigDecimal negative = new BigDecimal("-1");
        assertThatThrownBy(() -> wallet.addFunds(negative))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("withdrawFunds decreases balance when sufficient and positive")
    void withdrawFundsSuccess() {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");
        wallet.addFunds(new BigDecimal("100.00"));

        wallet.withdrawFunds(new BigDecimal("30.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    @DisplayName("withdrawFunds rejects invalid or insufficient balance")
    void withdrawFundsInvalid() {
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");
        wallet.addFunds(new BigDecimal("10.00"));

        BigDecimal zero = BigDecimal.ZERO;
        assertThatThrownBy(() -> wallet.withdrawFunds(zero))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance or invalid amount");
        BigDecimal negative = new BigDecimal("-1");
        assertThatThrownBy(() -> wallet.withdrawFunds(negative))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance or invalid amount");
        BigDecimal twenty = new BigDecimal("20");
        assertThatThrownBy(() -> wallet.withdrawFunds(twenty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance or invalid amount");
    }
}
