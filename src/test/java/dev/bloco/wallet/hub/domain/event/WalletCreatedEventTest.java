package dev.bloco.wallet.hub.domain.event;

import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Wallet Created Event Tests")
class WalletCreatedEventTest {

    @Test
    @DisplayName("WalletCreatedEvent sets fields via constructor")
    void builderSetsFields() {
        UUID walletId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        WalletCreatedEvent event = new WalletCreatedEvent(walletId, correlationId);

        assertThat(event.getWalletId()).isEqualTo(walletId);
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
    }
}
