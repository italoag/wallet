package dev.bloco.wallet.hub.infra.provider.data.config;

import dev.bloco.wallet.hub.infra.provider.data.repository.StateMachineRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(classes = {SagaStateMachineConfig.class, SagaStateMachineConfigTest.MockBeans.class})
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Saga State Machine Config (with JPA persistence) Tests")
class SagaStateMachineConfigTest {

    @Configuration
    static class MockBeans {
        @Bean
        StateMachineRepository stateMachineRepository() {
            return Mockito.mock(StateMachineRepository.class);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private StateMachine<SagaStates, SagaEvents> sm;

    @org.springframework.beans.factory.annotation.Autowired
    private StateMachineRepository repository;

    @Test
    @DisplayName("Machine id should be 'sagaStateMachine'")
    void machineId_isConfigured() {
        assertThat(sm.getId()).isEqualTo("sagaStateMachine");
    }

    @Test
    @DisplayName("Happy path should reach COMPLETED and persist via repository")
    void happyPath_reachesCompleted_andPersists() {
        // start from initial (reactively) and block until started
        sm.startReactively().block();
        assertThat(sm.getState().getId()).isEqualTo(SagaStates.INITIAL);

        // Send events using the reactive API and block until processed
        sendEvent(sm, SagaEvents.WALLET_CREATED);
        sendEvent(sm, SagaEvents.WALLET_CREATED);
        sendEvent(sm, SagaEvents.FUNDS_ADDED);
        sendEvent(sm, SagaEvents.FUNDS_WITHDRAWN);
        sendEvent(sm, SagaEvents.FUNDS_TRANSFERRED);
        sendEvent(sm, SagaEvents.SAGA_COMPLETED);

        assertThat(sm.getState().getId()).isEqualTo(SagaStates.COMPLETED);
        verify(repository, atLeastOnce()).save(any());
    }

    private static void sendEvent(StateMachine<SagaStates, SagaEvents> sm, SagaEvents event) {
        sm.sendEventCollect(Mono.just(MessageBuilder.withPayload(event).build())).block();
    }
}
