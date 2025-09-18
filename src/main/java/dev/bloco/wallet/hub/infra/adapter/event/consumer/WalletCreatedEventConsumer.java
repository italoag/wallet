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

/**
 * The WalletCreatedEventConsumer class is responsible for handling events of type WalletCreatedEvent.
 * It processes wallet creation events in a distributed system and interacts with a state machine
 * to manage the corresponding saga state transitions.
 *<p/>
 * This class listens for WalletCreatedEvent messages, validates the correlation ID in the event,
 * and updates the state machine accordingly by sending appropriate saga events. If the correlation ID
 * is missing or null, the function transitions the state machine to a SAGA_FAILED state and logs the failure.
 *<p/>
 * The class is annotated as a Spring component and provides a bean of type Consumer<Message<WalletCreatedEvent>>,
 * which is used to receive and process the WalletCreatedEvent messages.
 */
@Component
@Slf4j
public class WalletCreatedEventConsumer {

    private final StateMachine<SagaStates, SagaEvents> stateMachine;

    public WalletCreatedEventConsumer(StateMachine<SagaStates, SagaEvents> stateMachine) {
        this.stateMachine = stateMachine;
    }

  /**
   * Creates a consumer function for handling WalletCreatedEvent messages.
   *<p/>
   * This method processes incoming messages of type WalletCreatedEvent, validates the presence
   * of a correlation ID, and triggers appropriate state transitions in the state machine
   * using saga events. If the correlation ID is present, it transitions the state machine
   * to the WALLET_CREATED state and logs the success. If the correlation ID is missing or
   * null, it transitions the state machine to the SAGA_FAILED state and logs the failure.
   *<p/>
   * The function facilitates the coordination of distributed operations by managing
   * saga state transitions through the state machine.
   *
   * @return a Consumer function that processes messages containing WalletCreatedEvent data
   */
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