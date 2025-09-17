package dev.bloco.wallet.hub.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Wallet Created Event Tests")
class WalletCreatedEventTest {

    @Test
    @DisplayName("WalletCreatedEvent builder sets all fields")
    void builderSetsFields() {
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String correlationId = "corr-123";

        WalletCreatedEvent event = WalletCreatedEvent.builder()
                .walletId(walletId)
                .userId(userId)
                .correlationId(correlationId)
                .build();

        assertThat(event.walletId()).isEqualTo(walletId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.correlationId()).isEqualTo(correlationId);
    }
}
