package dev.bloco.wallet.hub.infra.provider.data.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

/**
 * Configuration class for defining the state machine states and transitions 
 * for a standard saga process.
 *<p/>
 * This class extends {@link StateMachineConfigurerAdapter} to provide custom 
 * state configuration and transition handling for a saga workflow. It employs
 * the {@link SagaStates} as states and {@link SagaEvents} as events. The configuration 
 * enables the creation of a state machine for handling a saga involving wallet 
 * creation and funds operations.
 *<p/>
 * Key features:
 * - Defines a set of states starting with an initial state and ending with 
 *   completion or failure states.
 * - Configures transitions between states based on specific events.
 * - Uses the Spring StateMachine framework for state management and transition logic.
 *<p/>
 * State Transition Details:
 * - Initial state transitions to `WALLET_CREATED` upon the `WALLET_CREATED` event.
 * - `WALLET_CREATED` state transitions to `FUNDS_ADDED` upon the `FUNDS_ADDED` event.
 * - `FUNDS_ADDED` state transitions to `FUNDS_WITHDRAWN` upon the `FUNDS_WITHDRAWN` event.
 * - `FUNDS_WITHDRAWN` state transitions to `FUNDS_TRANSFERRED` upon the `FUNDS_TRANSFERRED` event.
 * - `FUNDS_TRANSFERRED` state transitions to the `COMPLETED` end state upon the `SAGA_COMPLETED` event.
 * - Any state can transition to the `FAILED` end state upon the `SAGA_FAILED` event.
 */
@Configuration
@EnableStateMachine
public class StandardSagaStateMachineConfig extends StateMachineConfigurerAdapter<SagaStates, SagaEvents> {

  /**
   * Configures the states of the state machine for the saga process.
   *<p/>
   * This method defines the states that the state machine can transition through
   * during the lifecycle of a saga. It sets up the initial state, intermediary states, 
   * and the end states for successful completion or failure.
   *
   * @param states the {@link StateMachineStateConfigurer} used to configure the state machine states.
   *               It provides methods to define the initial state, multiple states, and end states.
   * @throws Exception if an error occurs during the configuration of the state machine.
   */
  @Override
    public void configure(StateMachineStateConfigurer<SagaStates, SagaEvents> states) throws Exception {
        states
                .withStates()
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
   *<p/>
   * This method defines the transition rules between different states in the lifecycle
   * of a saga, specifying the source state, target state, and the event that triggers
   * the transition. It handles both normal flow and failure scenarios.
   *
   * @param transitions the {@link StateMachineTransitionConfigurer} used to define state
   *                    transitions within the state machine. It provides methods to specify
   *                    the source state, target state, and the event for transitions.
   * @throws Exception if an error occurs during the configuration of state transitions.
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
}
