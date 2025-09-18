package dev.bloco.wallet.hub.infra.provider.data.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transaction Entity Tests")
class TransactionEntityTest {

    @Test
    @DisplayName("Gets and sets all fields")
    void gettersAndSetters_shouldWorkForAllFields() {
        // given
        TransactionEntity tx = new TransactionEntity();
        UUID id = UUID.randomUUID();
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("123.45");
        LocalDateTime timestamp = LocalDateTime.now();
        TransactionEntity.TransactionType type = TransactionEntity.TransactionType.TRANSFER;

        // when
        tx.setId(id);
        tx.setFromWalletId(fromId);
        tx.setToWalletId(toId);
        tx.setAmount(amount);
        tx.setTimestamp(timestamp);
        tx.setType(type);

        // then
        assertThat(tx.getId()).isEqualTo(id);
        assertThat(tx.getFromWalletId()).isEqualTo(fromId);
        assertThat(tx.getToWalletId()).isEqualTo(toId);
        assertThat(tx.getAmount()).isEqualByComparingTo(amount);
        assertThat(tx.getTimestamp()).isEqualTo(timestamp);
        assertThat(tx.getType()).isEqualTo(type);
    }

    @Test
    @DisplayName("Default values should be null before setting")
    void defaultValues_shouldBeNullBeforeSetting() {
        TransactionEntity tx = new TransactionEntity();
        assertThat(tx.getId()).isNull();
        assertThat(tx.getFromWalletId()).isNull();
        assertThat(tx.getToWalletId()).isNull();
        assertThat(tx.getAmount()).isNull();
        assertThat(tx.getTimestamp()).isNull();
        assertThat(tx.getType()).isNull();
    }

    @Test
    @DisplayName("Transaction type enum should contain expected values")
    void transactionTypeEnum_shouldContainExpectedValues() {
        assertThat(TransactionEntity.TransactionType.valueOf("DEPOSIT")).isNotNull();
        assertThat(TransactionEntity.TransactionType.valueOf("WITHDRAWAL")).isNotNull();
        assertThat(TransactionEntity.TransactionType.valueOf("TRANSFER")).isNotNull();
    }
}
