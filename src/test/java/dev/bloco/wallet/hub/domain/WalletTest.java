package dev.bloco.wallet.hub.domain;

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
        UUID userId = UUID.randomUUID();
        Wallet wallet = new Wallet(userId);

        assertThat(wallet.getId()).isNotNull();
        assertThat(wallet.getUserId()).isEqualTo(userId);
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("addFunds increases the balance for positive amounts")
    void addFundsSuccess() {
        Wallet wallet = new Wallet(UUID.randomUUID());

        wallet.addFunds(new BigDecimal("10.50"));
        wallet.addFunds(new BigDecimal("0.50"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("11.00"));
    }

    @Test
    @DisplayName("addFunds rejects zero or negative values")
    void addFundsInvalid() {
        Wallet wallet = new Wallet(UUID.randomUUID());

        assertThatThrownBy(() -> wallet.addFunds(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
        assertThatThrownBy(() -> wallet.addFunds(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("withdrawFunds decreases balance when sufficient and positive")
    void withdrawFundsSuccess() {
        Wallet wallet = new Wallet(UUID.randomUUID());
        wallet.addFunds(new BigDecimal("100.00"));

        wallet.withdrawFunds(new BigDecimal("30.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    @DisplayName("withdrawFunds rejects invalid or insufficient balance")
    void withdrawFundsInvalid() {
        Wallet wallet = new Wallet(UUID.randomUUID());
        wallet.addFunds(new BigDecimal("10.00"));

        assertThatThrownBy(() -> wallet.withdrawFunds(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance or invalid amount");
        assertThatThrownBy(() -> wallet.withdrawFunds(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance or invalid amount");
        assertThatThrownBy(() -> wallet.withdrawFunds(new BigDecimal("20")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance or invalid amount");
    }
}
