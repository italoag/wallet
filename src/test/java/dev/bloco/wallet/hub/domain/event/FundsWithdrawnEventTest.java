package dev.bloco.wallet.hub.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Funds Withdrawn Event Tests")
class FundsWithdrawnEventTest {

    @Test
    @DisplayName("FundsWithdrawnEvent builder sets all fields")
    void builderSetsFields() {
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("5.00");
        String correlationId = "corr-wd";

        FundsWithdrawnEvent event = FundsWithdrawnEvent.builder()
                .walletId(walletId)
                .amount(amount)
                .correlationId(correlationId)
                .build();

        assertThat(event.walletId()).isEqualTo(walletId);
        assertThat(event.amount()).isEqualByComparingTo(amount);
        assertThat(event.correlationId()).isEqualTo(correlationId);
    }
}
