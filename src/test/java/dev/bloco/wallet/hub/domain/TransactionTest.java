package dev.bloco.wallet.hub.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Transaction Tests")
class TransactionTest {

    @Test
    @DisplayName("Transaction constructor sets fields and generates id/timestamp")
    void transactionConstructor() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("42.42");

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Transaction tx = new Transaction(from, to, amount, Transaction.TransactionType.TRANSFER);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(tx.getId()).isNotNull();
        assertThat(tx.getFromWalletId()).isEqualTo(from);
        assertThat(tx.getToWalletId()).isEqualTo(to);
        assertThat(tx.getAmount()).isEqualByComparingTo(amount);
        assertThat(tx.getType()).isEqualTo(Transaction.TransactionType.TRANSFER);
        assertThat(tx.getTimestamp()).isBetween(before, after);
    }
}
