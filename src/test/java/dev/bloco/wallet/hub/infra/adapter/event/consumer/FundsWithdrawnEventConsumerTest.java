package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import dev.bloco.wallet.hub.domain.event.FundsWithdrawnEvent;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaEvents;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaStates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Funds Withdrawn Event Consumer Tests")
class FundsWithdrawnEventConsumerTest {

    private StateMachine<SagaStates, SagaEvents> stateMachine;
    private FundsWithdrawnEventConsumer consumerConfig;

    @BeforeEach
    void setUp() {
        stateMachine = mock(StateMachine.class);
        doReturn(Flux.empty()).when(stateMachine)
                .sendEvent(org.mockito.ArgumentMatchers.<reactor.core.publisher.Mono<org.springframework.messaging.Message<SagaEvents>>>any());
        consumerConfig = new FundsWithdrawnEventConsumer(stateMachine);
    }

    @Test
    @DisplayName("Should send FundsWithdrawn event to state machine")
    void fundsWithdrawnEventConsumerFunction_withCorrelationId_sendsFundsWithdrawn() {
        Consumer<Message<FundsWithdrawnEvent>> fn = consumerConfig.fundsWithdrawnEventConsumerFunction();
        var event = FundsWithdrawnEvent.builder()
                .walletId(UUID.randomUUID())
                .amount(new BigDecimal("7.77"))
                .correlationId("cid")
                .build();
        var message = MessageBuilder.withPayload(event).build();

        fn.accept(message);

        ArgumentCaptor<Mono<Message<SagaEvents>>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(stateMachine).sendEvent(captor.capture());
        Message<SagaEvents> sent = captor.getValue().block();
        assertThat(sent.getPayload()).isEqualTo(SagaEvents.FUNDS_WITHDRAWN);
        assertThat(sent.getHeaders().get("correlationId")).isEqualTo("cid");
    }

    @Test
    @DisplayName("Should send SagaFailed event to state machine when correlationId is null")
    void fundsWithdrawnEventConsumerFunction_withoutCorrelationId_sendsSagaFailed() {
        Consumer<Message<FundsWithdrawnEvent>> fn = consumerConfig.fundsWithdrawnEventConsumerFunction();
        var event = FundsWithdrawnEvent.builder()
                .walletId(UUID.randomUUID())
                .amount(new BigDecimal("7.77"))
                .correlationId(null)
                .build();
        var message = MessageBuilder.withPayload(event).build();

        fn.accept(message);

        ArgumentCaptor<Mono<Message<SagaEvents>>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(stateMachine, atLeastOnce()).sendEvent(captor.capture());
        Message<SagaEvents> last = captor.getAllValues().get(captor.getAllValues().size() - 1).block();
        assertThat(last.getPayload()).isEqualTo(SagaEvents.SAGA_FAILED);
    }
}
