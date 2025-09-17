package dev.bloco.wallet.hub.infra.provider.data.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

@Configuration
@EnableStateMachine
public class StandardSagaStateMachineConfig extends StateMachineConfigurerAdapter<SagaStates, SagaEvents> {

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
                .source(SagaStates.ANY).target(SagaStates.FAILED).event(SagaEvents.SAGA_FAILED);
    }
}
