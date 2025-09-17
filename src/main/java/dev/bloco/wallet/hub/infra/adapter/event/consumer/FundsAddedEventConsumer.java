package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import dev.bloco.wallet.hub.infra.provider.data.config.SagaEvents;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaStates;
import dev.bloco.wallet.hub.domain.event.FundsAddedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class FundsAddedEventConsumer {

    private final StateMachine<SagaStates, SagaEvents> stateMachine;

    public FundsAddedEventConsumer(StateMachine<SagaStates, SagaEvents> stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Bean
    public Consumer<Message<FundsAddedEvent>> fundsAddedEventConsumerFunction() {
        return message -> {
            var event = message.getPayload();
            try {
                Objects.requireNonNull(event.correlationId(), "correlationId is required");
                var stateMachineMessage = MessageBuilder.withPayload(SagaEvents.FUNDS_ADDED)
                        .setHeader("correlationId", event.correlationId())
                        .build();
                var result = stateMachine.sendEvent(Mono.just(stateMachineMessage));
                result.subscribe(); // Process the result if needed
                log.info("Funds added: {} to wallet {}", event.amount(), event.walletId());
            } catch (NullPointerException e) {
                stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(SagaEvents.SAGA_FAILED).build())).subscribe();
                log.info("Failed to add funds: {}", e.getMessage());
            }
        };
    }
}