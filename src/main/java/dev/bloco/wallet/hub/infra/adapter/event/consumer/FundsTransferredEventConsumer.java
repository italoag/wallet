package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import dev.bloco.wallet.hub.domain.event.FundsTransferredEvent;
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
public class FundsTransferredEventConsumer {

    private final StateMachine<SagaStates, SagaEvents> stateMachine;

    public FundsTransferredEventConsumer(StateMachine<SagaStates, SagaEvents> stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Bean
    public Consumer<Message<FundsTransferredEvent>> fundsTransferredEventConsumerFunction() {
        return message -> {
            var event = message.getPayload();
            var stateMachineMessage = MessageBuilder.withPayload(SagaEvents.FUNDS_TRANSFERRED)
                    .setHeader("correlationId", event.correlationId())
                    .build();
            var result = stateMachine.sendEvent(Mono.just(stateMachineMessage));
            result.subscribe(); // Process the result if needed
            log.info("Funds transferred: {} from wallet {} to wallet {}", event.amount(), event.fromWalletId(), event.toWalletId());
        };
    }
}