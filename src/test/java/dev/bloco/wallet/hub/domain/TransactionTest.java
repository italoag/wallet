package dev.bloco.wallet.hub.domain;

import dev.bloco.wallet.hub.domain.model.transaction.Transaction;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionHash;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transaction Domain Tests")
class TransactionTest {

    @Test
    @DisplayName("create should set fields and generate non-null id and timestamp in range with PENDING status")
    void create_setsFields_andGeneratesIdAndTimestamp() {
        UUID id = UUID.randomUUID();
        UUID networkId = UUID.randomUUID();
        TransactionHash hash = new TransactionHash("0xabc");
        String from = "0xfrom";
        String to = "0xto";
        BigDecimal value = new BigDecimal("123.45");
        String data = "0xdata";

        Instant before = Instant.now();
        Transaction tx = Transaction.create(id, networkId, hash, from, to, value, data);
        Instant after = Instant.now();

        assertThat(tx.getId()).isEqualTo(id);
        assertThat(tx.getNetworkId()).isEqualTo(networkId);
        assertThat(tx.getHash()).isEqualTo("0xabc");
        assertThat(tx.getFromAddress()).isEqualTo(from);
        assertThat(tx.getToAddress()).isEqualTo(to);
        assertThat(tx.getValue()).isEqualByComparingTo("123.45");
        assertThat(tx.getData()).isEqualTo(data);
        assertThat(tx.getTimestamp()).isNotNull();
        assertThat(!tx.getTimestamp().isBefore(before) && !tx.getTimestamp().isAfter(after))
                .as("timestamp should be between before and after")
                .isTrue();
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    @DisplayName("confirm and fail should update status accordingly")
    void confirm_and_fail_updateStatus() {
        Transaction tx = Transaction.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new TransactionHash("0xabc"),
                "0xfrom",
                "0xto",
                new BigDecimal("1.00"),
                null
        );

        tx.confirm(100L, "0xblock", new BigDecimal("0.21"));
        assertThat(tx.isConfirmed()).isTrue();
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.CONFIRMED);

        tx.fail("oops");
        assertThat(tx.isFailed()).isTrue();
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);
    }
}
