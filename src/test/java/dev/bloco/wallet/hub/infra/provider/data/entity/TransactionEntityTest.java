package dev.bloco.wallet.hub.infra.provider.data.entity;

import dev.bloco.wallet.hub.domain.model.transaction.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transaction Entity Tests")
class TransactionEntityTest {

    @Test
    @DisplayName("Gets and sets all fields (blockchain schema)")
    void gettersAndSetters_shouldWorkForAllFields() {
        // given
        TransactionEntity tx = new TransactionEntity();
        UUID id = UUID.randomUUID();
        UUID networkId = UUID.randomUUID();
        String hash = "0xabc";
        String from = "0xfrom";
        String to = "0xto";
        BigDecimal value = new BigDecimal("123.45");
        BigDecimal gasPrice = new BigDecimal("1");
        BigDecimal gasLimit = new BigDecimal("21000");
        BigDecimal gasUsed = new BigDecimal("21000");
        String data = "0xdata";
        Instant timestamp = Instant.now();
        Long blockNumber = 100L;
        String blockHash = "0xblock";
        TransactionStatus status = TransactionStatus.PENDING;

        // when
        tx.setId(id);
        tx.setNetworkId(networkId);
        tx.setHash(hash);
        tx.setFromAddress(from);
        tx.setToAddress(to);
        tx.setValue(value);
        tx.setGasPrice(gasPrice);
        tx.setGasLimit(gasLimit);
        tx.setGasUsed(gasUsed);
        tx.setData(data);
        tx.setTimestamp(timestamp);
        tx.setBlockNumber(blockNumber);
        tx.setBlockHash(blockHash);
        tx.setStatus(status);

        // then
        assertThat(tx.getId()).isEqualTo(id);
        assertThat(tx.getNetworkId()).isEqualTo(networkId);
        assertThat(tx.getHash()).isEqualTo(hash);
        assertThat(tx.getFromAddress()).isEqualTo(from);
        assertThat(tx.getToAddress()).isEqualTo(to);
        assertThat(tx.getValue()).isEqualByComparingTo(value);
        assertThat(tx.getGasPrice()).isEqualByComparingTo(gasPrice);
        assertThat(tx.getGasLimit()).isEqualByComparingTo(gasLimit);
        assertThat(tx.getGasUsed()).isEqualByComparingTo(gasUsed);
        assertThat(tx.getData()).isEqualTo(data);
        assertThat(tx.getTimestamp()).isEqualTo(timestamp);
        assertThat(tx.getBlockNumber()).isEqualTo(blockNumber);
        assertThat(tx.getBlockHash()).isEqualTo(blockHash);
        assertThat(tx.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("Default values should be null before setting")
    void defaultValues_shouldBeNullBeforeSetting() {
        TransactionEntity tx = new TransactionEntity();
        assertThat(tx.getId()).isNull();
        assertThat(tx.getNetworkId()).isNull();
        assertThat(tx.getHash()).isNull();
        assertThat(tx.getFromAddress()).isNull();
        assertThat(tx.getToAddress()).isNull();
        assertThat(tx.getValue()).isNull();
        assertThat(tx.getGasPrice()).isNull();
        assertThat(tx.getGasLimit()).isNull();
        assertThat(tx.getGasUsed()).isNull();
        assertThat(tx.getData()).isNull();
        assertThat(tx.getTimestamp()).isNull();
        assertThat(tx.getBlockNumber()).isNull();
        assertThat(tx.getBlockHash()).isNull();
        assertThat(tx.getStatus()).isNull();
    }
}
