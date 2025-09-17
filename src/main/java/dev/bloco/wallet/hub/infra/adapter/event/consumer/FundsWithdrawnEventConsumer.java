package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import dev.bloco.wallet.hub.domain.event.FundsWithdrawnEvent;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaEvents;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaStates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Component
@Slf4j
public class FundsWithdrawnEventConsumer {

    private final StateMachine<SagaStates, SagaEvents> stateMachine;

    public FundsWithdrawnEventConsumer(StateMachine<SagaStates, SagaEvents> stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Bean
    public Consumer<Message<FundsWithdrawnEvent>> fundsWithdrawnEventConsumerFunction() {
        return message -> {
            var event = message.getPayload();
            if (event.correlationId() != null) {
                var stateMachineMessage = MessageBuilder.withPayload(SagaEvents.FUNDS_WITHDRAWN)
                        .setHeader("correlationId", event.correlationId())
                        .build();
                var result = stateMachine.sendEvent(Mono.just(stateMachineMessage));
                result.subscribe(); // Process the result if needed
                log.info("Funds withdrawn: {} from wallet {}", event.amount(), event.walletId());
            } else {
                stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(SagaEvents.SAGA_FAILED).build())).subscribe();
                log.info("Failed to withdraw funds: Missing correlationId");
            }
        };
    }
}