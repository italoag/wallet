package dev.bloco.wallet.hub.infra.provider.data.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = StandardSagaStateMachineConfig.class)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Standard Saga State Machine Config Tests")
class StandardSagaStateMachineConfigTest {

    @org.springframework.beans.factory.annotation.Autowired
    private StateMachine<SagaStates, SagaEvents> sm;


    @Test
    @DisplayName("Happy path should reach COMPLETED through the defined transitions")
    void happyPath_reachesCompleted() {
        sm.startReactively().block();
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.INITIAL);

        sendEvent(sm, SagaEvents.WALLET_CREATED);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.WALLET_CREATED);

        sendEvent(sm, SagaEvents.FUNDS_ADDED);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.FUNDS_ADDED);

        sendEvent(sm, SagaEvents.FUNDS_WITHDRAWN);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.FUNDS_WITHDRAWN);

        sendEvent(sm, SagaEvents.FUNDS_TRANSFERRED);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.FUNDS_TRANSFERRED);

        sendEvent(sm, SagaEvents.SAGA_COMPLETED);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.COMPLETED);

        sm.stopReactively().block();
    }

    @Test
    @DisplayName("SAGA_FAILED should transition to FAILED from INITIAL")
    void failureFromInitial_reachesFailed() {
        sm.startReactively().block();
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.INITIAL);
        sendEvent(sm, SagaEvents.SAGA_FAILED);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.FAILED);
        sm.stopReactively().block();
    }

    @Test
    @DisplayName("SAGA_FAILED should transition to FAILED from WALLET_CREATED")
    void failureFromWalletCreated_reachesFailed() {
        sm.startReactively().block();
        sendEvent(sm, SagaEvents.WALLET_CREATED);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.WALLET_CREATED);
        sendEvent(sm, SagaEvents.SAGA_FAILED);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.FAILED);
        sm.stopReactively().block();
    }

    @Test
    @DisplayName("SAGA_FAILED should transition to FAILED from FUNDS_ADDED")
    void failureFromFundsAdded_reachesFailed() {
        sm.startReactively().block();
        sendEvent(sm, SagaEvents.WALLET_CREATED);
        sendEvent(sm, SagaEvents.FUNDS_ADDED);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.FUNDS_ADDED);
        sendEvent(sm, SagaEvents.SAGA_FAILED);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.FAILED);
        sm.stopReactively().block();
    }

    @Test
    @DisplayName("SAGA_FAILED should transition to FAILED from FUNDS_WITHDRAWN")
    void failureFromFundsWithdrawn_reachesFailed() {
        sm.startReactively().block();
        sendEvent(sm, SagaEvents.WALLET_CREATED);
        sendEvent(sm, SagaEvents.FUNDS_ADDED);
        sendEvent(sm, SagaEvents.FUNDS_WITHDRAWN);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.FUNDS_WITHDRAWN);
        sendEvent(sm, SagaEvents.SAGA_FAILED);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.FAILED);
        sm.stopReactively().block();
    }

    @Test
    @DisplayName("SAGA_FAILED should transition to FAILED from FUNDS_TRANSFERRED")
    void failureFromFundsTransferred_reachesFailed() {
        sm.startReactively().block();
        sendEvent(sm, SagaEvents.WALLET_CREATED);
        sendEvent(sm, SagaEvents.FUNDS_ADDED);
        sendEvent(sm, SagaEvents.FUNDS_WITHDRAWN);
        sendEvent(sm, SagaEvents.FUNDS_TRANSFERRED);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.FUNDS_TRANSFERRED);
        sendEvent(sm, SagaEvents.SAGA_FAILED);
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.FAILED);
        sm.stopReactively().block();
    }

    private static void sendEvent(StateMachine<SagaStates, SagaEvents> sm, SagaEvents event) {
        sm.sendEventCollect(Mono.just(MessageBuilder.withPayload(event).build())).block();
    }
}
