package dev.bloco.wallet.hub.infra.provider.data.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;

import dev.bloco.wallet.hub.infra.provider.data.repository.StateMachineRepository;

/**
 * Configuration class for the Saga State Machine.
 * </p>
 * This class configures the states, transitions, and persistence settings
 * for a state machine that is responsible for managing the saga workflow.
 * </p>
 * A saga workflow involves various steps, represented as states, and transitions
 * between them triggered by specific events.
 * </p>
 * The configuration includes:
 * - Defining the states (e.g., INITIAL, COMPLETED, FAILED).
 * - Defining the events that trigger state transitions.
 * - Setting up state persistence to ensure the state machine's state is maintained.
 * </p>
 * It uses Spring State Machine's {@link StateMachineConfigurerAdapter} and
 * integrates a {@link StateMachineRepository} for persisting the state machine state.
 */
@Configuration
@EnableStateMachine
@Profile("saga")
public class SagaStateMachineConfig extends StateMachineConfigurerAdapter<SagaStates, SagaEvents> {

    private final StateMachineRepository stateMachineRepository;

    public SagaStateMachineConfig(StateMachineRepository stateMachineRepository) {
        this.stateMachineRepository = stateMachineRepository;
    }


  /**
   * Configures the states of the state machine for the saga process.
   *</p>
   * This method is responsible for defining all the states that the state machine can enter
   * during the lifecycle of a saga. It establishes the initial state, various intermediary states,
   * and the end states when the saga is either completed successfully or fails.
   *
   * @param states the {@link StateMachineStateConfigurer} used for configuring the available
   *               states in the state machine. It allows chaining methods to define
   *               the initial state, multiple intermediate states, and the end states.
   * @throws Exception if there is an error while configuring the state machine states.
   */
  @Override
    public void configure(StateMachineStateConfigurer<SagaStates, SagaEvents> states) throws Exception {
        states.withStates()
                .initial(SagaStates.INITIAL)
                .state(SagaStates.WALLET_CREATED)
                .state(SagaStates.FUNDS_ADDED)
                .state(SagaStates.FUNDS_WITHDRAWN)
                .state(SagaStates.FUNDS_TRANSFERRED)
                .end(SagaStates.COMPLETED)
                .end(SagaStates.FAILED);
    }

  /**
   * Configures the transitions of the state machine for the saga process.
   *</p>
   * This method establishes the transition rules between states within the state machine.
   * It specifies the source state, target state, and the event that triggers each transition,
   * covering the entire saga lifecycle from initiation to completion or failure.
   *</p>
   * @param transitions the {@link StateMachineTransitionConfigurer} used to define state
   *                    transitions in the state machine. It allows chaining methods to specify
   *                    transitions between states, their source, target, and the triggering event.
   * @throws Exception if an error occurs while configuring the state transitions.
   */
  @Override
    public void configure(StateMachineTransitionConfigurer<SagaStates, SagaEvents> transitions) throws Exception {
        transitions
                .withExternal()
                .source(SagaStates.INITIAL).target(SagaStates.WALLET_CREATED).event(SagaEvents.WALLET_CREATED)
                .and()
                .withExternal()
                .source(SagaStates.WALLET_CREATED).target(SagaStates.FUNDS_ADDED).event(SagaEvents.FUNDS_ADDED)
                .and()
                .withExternal()
                .source(SagaStates.FUNDS_ADDED).target(SagaStates.FUNDS_WITHDRAWN).event(SagaEvents.FUNDS_WITHDRAWN)
                .and()
                .withExternal()
                .source(SagaStates.FUNDS_WITHDRAWN).target(SagaStates.FUNDS_TRANSFERRED).event(SagaEvents.FUNDS_TRANSFERRED)
                .and()
                .withExternal()
                .source(SagaStates.FUNDS_TRANSFERRED).target(SagaStates.COMPLETED).event(SagaEvents.SAGA_COMPLETED)
                .and()
                // Explicit failure transitions from each state
                .withExternal()
                .source(SagaStates.INITIAL).target(SagaStates.FAILED).event(SagaEvents.SAGA_FAILED)
                .and()
                .withExternal()
                .source(SagaStates.WALLET_CREATED).target(SagaStates.FAILED).event(SagaEvents.SAGA_FAILED)
                .and()
                .withExternal()
                .source(SagaStates.FUNDS_ADDED).target(SagaStates.FAILED).event(SagaEvents.SAGA_FAILED)
                .and()
                .withExternal()
                .source(SagaStates.FUNDS_WITHDRAWN).target(SagaStates.FAILED).event(SagaEvents.SAGA_FAILED)
                .and()
                .withExternal()
                .source(SagaStates.FUNDS_TRANSFERRED).target(SagaStates.FAILED).event(SagaEvents.SAGA_FAILED);
    }

  /**
   * Configures the global settings of the state machine for the saga process.
   * </p>
   * This method sets up configuration options for the state machine, including its machine ID
   * and persistence settings. It links the state machine to a specified runtime persister to
   * maintain state data across sessions or restarts.
   *
   * @param config the {@link StateMachineConfigurationConfigurer} used to configure general
   *               settings of the state machine. Provides methods to configure the machine ID
   *               and integrative features such as persistence.
   * @throws Exception if an error occurs during the configuration process.
   */
  @Override
    public void configure(StateMachineConfigurationConfigurer<SagaStates, SagaEvents> config) throws Exception {
        config
                .withConfiguration()
                .machineId("sagaStateMachine")
                .and()
                .withPersistence()
                .runtimePersister(new JpaPersistingStateMachineInterceptor<>(stateMachineRepository));
    }
}
