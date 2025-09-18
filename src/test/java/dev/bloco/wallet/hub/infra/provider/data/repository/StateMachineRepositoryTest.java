package dev.bloco.wallet.hub.infra.provider.data.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StateMachine Repository Tests")
class StateMachineRepositoryTest {

    @Test
    @DisplayName("StateMachineRepository should extend JpaStateMachineRepository")
    void stateMachineRepository_isAssignableFromJpaStateMachineRepository() {
        assertTrue(JpaStateMachineRepository.class.isAssignableFrom(StateMachineRepository.class),
                "StateMachineRepository should extend JpaStateMachineRepository");
    }
}
