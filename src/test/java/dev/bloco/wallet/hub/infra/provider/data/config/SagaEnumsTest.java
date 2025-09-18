package dev.bloco.wallet.hub.infra.provider.data.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Saga enums coverage")
class SagaEnumsTest {

    @Test
    @DisplayName("SagaStates should contain all expected values including ANY")
    void sagaStates_values() {
        SagaStates[] values = SagaStates.values();
        assertThat(values).isNotEmpty();
        assertThat(values).contains(
                SagaStates.INITIAL,
                SagaStates.WALLET_CREATED,
                SagaStates.FUNDS_ADDED,
                SagaStates.FUNDS_WITHDRAWN,
                SagaStates.FUNDS_TRANSFERRED,
                SagaStates.COMPLETED,
                SagaStates.FAILED,
                SagaStates.ANY
        );
        // ensure count stays consistent
        assertThat(values.length).isEqualTo(8);
    }

    @Test
    @DisplayName("SagaEvents should contain all expected values")
    void sagaEvents_values() {
        SagaEvents[] values = SagaEvents.values();
        assertThat(values).isNotEmpty();
        assertThat(values).contains(
                SagaEvents.WALLET_CREATED,
                SagaEvents.FUNDS_ADDED,
                SagaEvents.FUNDS_WITHDRAWN,
                SagaEvents.FUNDS_TRANSFERRED,
                SagaEvents.SAGA_COMPLETED,
                SagaEvents.SAGA_FAILED
        );
        assertThat(values.length).isEqualTo(6);
    }
}
