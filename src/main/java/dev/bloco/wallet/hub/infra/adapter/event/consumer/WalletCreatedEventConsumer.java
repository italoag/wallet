package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import dev.bloco.wallet.hub.infra.provider.data.config.SagaEvents;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaStates;
import dev.bloco.wallet.hub.domain.event.WalletCreatedEvent;
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
public class WalletCreatedEventConsumer {

    private final StateMachine<SagaStates, SagaEvents> stateMachine;

    public WalletCreatedEventConsumer(StateMachine<SagaStates, SagaEvents> stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Bean
    public Consumer<Message<WalletCreatedEvent>> walletCreatedEventConsumerFunction() {
        return message -> {
            var event = message.getPayload();
            if (event.correlationId() != null) {
                var stateMachineMessage = MessageBuilder.withPayload(SagaEvents.WALLET_CREATED)
                        .setHeader("correlationId", event.correlationId())
                        .build();
                var result = stateMachine.sendEvent(Mono.just(stateMachineMessage));
                result.subscribe(); // Process the result if needed
                log.info("Wallet created: {}", event.walletId());
            } else {
                stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(SagaEvents.SAGA_FAILED).build())).subscribe();
                log.info("Failed to create wallet: Missing correlationId");
            }
        };
    }
}