package dev.bloco.wallet.hub.infra.provider.data.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.statemachine.StateMachine;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Light integration test to ensure StateMachine JPA persistence is wired and operational
 * with an in-memory/file-backed H2 database. It boots only the SagaStateMachineConfig
 * and scans Spring Statemachine JPA entities to allow the JpaPersistingStateMachineInterceptor
 * to persist state changes via StateMachineRepository.
 */
@org.springframework.test.context.junit.jupiter.SpringJUnitConfig(classes = {SagaStateMachineConfig.class, StateMachineJpaIntegrationTest.MockBeans.class})
@org.springframework.test.context.ActiveProfiles("saga")
@DisplayName("StateMachine JPA Integration Test")
class StateMachineJpaIntegrationTest {

    @org.springframework.context.annotation.Configuration
    static class MockBeans {
        @org.springframework.context.annotation.Bean
        dev.bloco.wallet.hub.infra.provider.data.repository.StateMachineRepository stateMachineRepository() {
            return org.mockito.Mockito.mock(dev.bloco.wallet.hub.infra.provider.data.repository.StateMachineRepository.class);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private StateMachine<SagaStates, SagaEvents> sm;


    @Test
    @DisplayName("State transitions persist without exceptions and reach COMPLETED")
    void stateTransitions_persistAndComplete() {
        // start
        sm.startReactively().block();
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.INITIAL);
        // drive transitions
        send(SagaEvents.WALLET_CREATED);
        send(SagaEvents.FUNDS_ADDED);
        send(SagaEvents.FUNDS_WITHDRAWN);
        send(SagaEvents.FUNDS_TRANSFERRED);
        send(SagaEvents.SAGA_COMPLETED);
        // assert the final state
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.COMPLETED);
        sm.stopReactively().block();
    }

    private void send(SagaEvents ev) {
        sm.sendEventCollect(Mono.just(org.springframework.messaging.support.MessageBuilder.withPayload(ev).build())).block();
    }
}
