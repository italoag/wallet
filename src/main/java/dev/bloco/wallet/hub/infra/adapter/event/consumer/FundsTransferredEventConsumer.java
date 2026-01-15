package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import dev.bloco.wallet.hub.domain.event.wallet.FundsTransferredEvent;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaEvents;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaStates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * The FundsTransferredEventConsumer class is responsible for handling events of type FundsTransferredEvent.
 * It processes the transfer of funds between wallets in the system and interacts with a state machine
 * to manage the saga state transitions related to this event.
 *<p/>
 * This class listens for FundsTransferredEvent messages, retrieves the event details, and updates the
 * state machine by sending the appropriate saga event for funds being successfully transferred. The class
 * also logs the details of the transaction, such as the amount transferred and the source and destination wallets.
 *<p/>
 * FundsTransferredEventConsumer provides a bean of type
 * Consumer<Message<FundsTransferredEvent>>, which manages the processing of incoming event messages.
 */
@Component
@Slf4j
public class FundsTransferredEventConsumer {

    /**
     * Creates a consumer function for handling FundsTransferredEvent messages.
     *<p/>
     * This method processes incoming messages of type FundsTransferredEvent, retrieves the event details,
     * and transitions the state machine through the respective saga states. The state transition is achieved
     * by sending the SagaEvents.FUNDS_TRANSFERRED event to the state machine. After processing the event,
     * the function logs the transfer details, including the amount, source wallet ID, and destination wallet ID.
     *<p/>
     * The consumer supports distributed state management and ensures that the funds transfer operation
     * is successfully integrated into the saga process.
     *
     * @return a Consumer function that processes messages containing FundsTransferredEvent data
     */
    @Bean
    public Consumer<Message<FundsTransferredEvent>> fundsTransferredEventConsumerFunction(StateMachine<SagaStates, SagaEvents> stateMachine) {
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