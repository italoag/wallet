package dev.bloco.wallet.hub.domain.event;

import dev.bloco.wallet.hub.domain.event.wallet.FundsAddedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Funds Added Event Tests")
class FundsAddedEventTest {

    @Test
    @DisplayName("FundsAddedEvent builder sets all fields")
    void builderSetsFields() {
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("12.34");
        String correlationId = "corr-abc";

        FundsAddedEvent event = FundsAddedEvent.builder()
                .walletId(walletId)
                .amount(amount)
                .correlationId(correlationId)
                .build();

        assertThat(event.walletId()).isEqualTo(walletId);
        assertThat(event.amount()).isEqualByComparingTo(amount);
        assertThat(event.correlationId()).isEqualTo(correlationId);
    }
}
