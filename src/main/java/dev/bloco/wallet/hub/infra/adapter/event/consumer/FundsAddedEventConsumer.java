package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import dev.bloco.wallet.hub.infra.provider.data.config.SagaEvents;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaStates;
import dev.bloco.wallet.hub.domain.event.wallet.FundsAddedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * The FundsAddedEventConsumer class is responsible for handling events of the
 * type FundsAddedEvent.
 * It processes the addition of funds to a wallet in the system and interacts
 * with a state machine
 * to manage the saga state transitions related to this event.
 * <p/>
 * The class listens for FundsAddedEvent messages, validates the correlation ID
 * in the event,
 * and updates the state machine accordingly by sending appropriate events. It
 * logs the status
 * of the operation and handles exceptions if the correlation ID is missing or
 * null.
 * <p/>
 * This class is configured as a Spring component and provides a bean of type
 * Consumer<Message<FundsAddedEvent>>,
 * which receives and processes the FundsAddedEvent messages.
 *
 */
@Configuration
@Slf4j
public class FundsAddedEventConsumer {

    /**
     * Creates a consumer function for handling FundsAddedEvent messages.
     * <p/>
     * This method is responsible for processing incoming messages of type
     * FundsAddedEvent,
     * ensuring the correlation ID is present, and transitioning the state machine
     * through
     * the appropriate saga events. If the correlation ID is missing or null, the
     * function
     * transitions the state machine to a SAGA_FAILED state and logs the failure.
     * <p/>
     * The function supports state management by interacting with a state machine
     * and helps in maintaining consistency in distributed operations.
     *
     * @return a Consumer function that processes messages containing
     *         FundsAddedEvent data
     */
    @Bean
    public Consumer<Message<FundsAddedEvent>> fundsAddedEventConsumerFunction(
            StateMachine<SagaStates, SagaEvents> stateMachine) {
        return message -> {
            var event = message.getPayload();
            String corr = event.correlationId();
            if (corr == null || corr.isBlank()) {
                stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(SagaEvents.SAGA_FAILED).build()))
                        .subscribe();
                log.warn("Failed to add funds due to missing correlationId for wallet {}", event.walletId());
                return;
            }
            var stateMachineMessage = MessageBuilder.withPayload(SagaEvents.FUNDS_ADDED)
                    .setHeader("correlationId", corr)
                    .build();
            stateMachine.sendEvent(Mono.just(stateMachineMessage)).subscribe();
            log.info("Funds added: {} to wallet {}", event.amount(), event.walletId());
        };
    }
}