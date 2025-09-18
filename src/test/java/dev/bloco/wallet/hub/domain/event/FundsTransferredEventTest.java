package dev.bloco.wallet.hub.domain.event;

import dev.bloco.wallet.hub.domain.event.wallet.FundsTransferredEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Funds Transferred Event Tests")
class FundsTransferredEventTest {

    @Test
    @DisplayName("FundsTransferredEvent builder sets all fields")
    void builderSetsFields() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("77.77");
        String correlationId = "corr-xfer";

        FundsTransferredEvent event = FundsTransferredEvent.builder()
                .fromWalletId(from)
                .toWalletId(to)
                .amount(amount)
                .correlationId(correlationId)
                .build();

        assertThat(event.fromWalletId()).isEqualTo(from);
        assertThat(event.toWalletId()).isEqualTo(to);
        assertThat(event.amount()).isEqualByComparingTo(amount);
        assertThat(event.correlationId()).isEqualTo(correlationId);
    }
}
