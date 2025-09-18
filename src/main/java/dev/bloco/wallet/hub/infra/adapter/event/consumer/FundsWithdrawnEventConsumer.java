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

/**
 * The FundsWithdrawnEventConsumer class is responsible for handling events of type FundsWithdrawnEvent.
 * It processes the withdrawal of funds from a wallet and interacts with a state machine
 * to manage the saga state transitions related to this event.
 *<p/>
 * The class listens for FundsWithdrawnEvent messages, validates the correlation ID in the event,
 * and updates the state machine accordingly by sending appropriate events. If the correlation ID
 * is missing or null, it transitions the state machine to a SAGA_FAILED state and logs the failure.
 *<p/>
 * This class is configured as a Spring component and provides a bean of type Consumer<Message<FundsWithdrawnEvent>>,
 * which receives and processes the FundsWithdrawnEvent messages.
 */
@Component
@Slf4j
public class FundsWithdrawnEventConsumer {

    private final StateMachine<SagaStates, SagaEvents> stateMachine;

  /**
   * Constructs a FundsWithdrawnEventConsumer instance.
   *<p/>
   * This constructor initializes the {@code FundsWithdrawnEventConsumer} with a state machine
   * used for managing the state transitions of the saga process when funds are withdrawn from a wallet.
   *
   * @param stateMachine the state machine instance responsible for tracking and transitioning
   *                     between various states of the saga process
   */
  public FundsWithdrawnEventConsumer(StateMachine<SagaStates, SagaEvents> stateMachine) {
        this.stateMachine = stateMachine;
    }

  /**
   * Creates a consumer function for handling FundsWithdrawnEvent messages.
   *<p/>
   * This method is responsible for processing incoming messages of type FundsWithdrawnEvent,
   * ensuring the presence of a valid correlation ID, and transitioning the state machine
   * through the appropriate saga events. If the correlation ID is missing or null, the function
   * transitions the state machine to a SAGA_FAILED state and logs the failure.
   *<p/>
   * The function supports state management by interacting with a state machine and helps to
   * maintain consistency in distributed operations such as fund withdrawal processes.
   *
   * @return a Consumer function that processes messages containing FundsWithdrawnEvent data
   */
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