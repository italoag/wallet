package dev.bloco.wallet.hub.domain;

import dev.bloco.wallet.hub.domain.Transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transaction Domain Tests")
class TransactionTest {

    @Test
    @DisplayName("Constructor should set fields and generate non-null id and timestamp in range")
    void constructor_setsFields_andGeneratesIdAndTimestamp() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("123.45");
        LocalDateTime before = LocalDateTime.now();

        Transaction tx = new Transaction(from, to, amount, TransactionType.TRANSFER);

        LocalDateTime after = LocalDateTime.now();

        assertThat(tx.getId()).isNotNull();
        assertThat(tx.getFromWalletId()).isEqualTo(from);
        assertThat(tx.getToWalletId()).isEqualTo(to);
        assertThat(tx.getAmount()).isEqualByComparingTo("123.45");
        assertThat(tx.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(tx.getTimestamp()).isNotNull();
        assertThat(!tx.getTimestamp().isBefore(before) && !tx.getTimestamp().isAfter(after))
                .as("timestamp should be between before and after")
                .isTrue();
    }

    @Test
    @DisplayName("Enum values should include DEPOSIT, WITHDRAWAL and TRANSFER and support valueOf")
    void enum_values_and_valueOf() {
        assertThat(TransactionType.values()).containsExactlyInAnyOrder(
                TransactionType.DEPOSIT,
                TransactionType.WITHDRAWAL,
                TransactionType.TRANSFER
        );

        assertThat(TransactionType.valueOf("DEPOSIT")).isEqualTo(TransactionType.DEPOSIT);
        assertThat(TransactionType.valueOf("WITHDRAWAL")).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(TransactionType.valueOf("TRANSFER")).isEqualTo(TransactionType.TRANSFER);
    }
}
