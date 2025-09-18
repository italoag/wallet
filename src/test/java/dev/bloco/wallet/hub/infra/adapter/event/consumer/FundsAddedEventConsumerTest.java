package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import dev.bloco.wallet.hub.domain.event.wallet.FundsAddedEvent;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Funds Added Event Consumer Tests")
class FundsAddedEventConsumerTest {

    private StateMachine<SagaStates, SagaEvents> stateMachine;
    private FundsAddedEventConsumer consumerConfig;

    @BeforeEach
    void setUp() {
        stateMachine = mock(StateMachine.class);
        doReturn(Flux.empty()).when(stateMachine)
                .sendEvent(org.mockito.ArgumentMatchers.<reactor.core.publisher.Mono<org.springframework.messaging.Message<SagaEvents>>>any());
        consumerConfig = new FundsAddedEventConsumer(stateMachine);
    }

    @Test
    @DisplayName("Should send FundsAdded event to state machine")
    void fundsAddedEventConsumerFunction_sendsFundsAddedEventToStateMachine() {
        // given
        Consumer<Message<FundsAddedEvent>> fn = consumerConfig.fundsAddedEventConsumerFunction();
        var event = FundsAddedEvent.builder()
                .walletId(UUID.randomUUID())
                .amount(new BigDecimal("12.34"))
                .correlationId("corr-xyz")
                .build();
        var message = MessageBuilder.withPayload(event).build();

        // when
        fn.accept(message);

        // then capture Mono<Message<SagaEvents>> and assert the payload type and header
        ArgumentCaptor<Mono<Message<SagaEvents>>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(stateMachine).sendEvent(captor.capture());
        Message<SagaEvents> sent = captor.getValue().block();
        assertThat(sent).isNotNull();
        assertThat(sent.getPayload()).isEqualTo(SagaEvents.FUNDS_ADDED);
        assertThat(sent.getHeaders().get("correlationId")).isEqualTo("corr-xyz");
    }

    @Test
    @DisplayName("Should send SagaFailed event to state machine when correlationId is null")
    void fundsAddedEventConsumerFunction_whenCorrelationMissing_sendsSagaFailed() {
        // given
        Consumer<Message<FundsAddedEvent>> fn = consumerConfig.fundsAddedEventConsumerFunction();
        var event = FundsAddedEvent.builder()
                .walletId(UUID.randomUUID())
                .amount(new BigDecimal("12.34"))
                .correlationId(null)
                .build();
        var message = MessageBuilder.withPayload(event).build();

        // when
        fn.accept(message);

        // then
        ArgumentCaptor<Mono<Message<SagaEvents>>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(stateMachine, atLeastOnce()).sendEvent(captor.capture());
        // the last event should be SAGA_FAILED
        Message<SagaEvents> last = captor.getAllValues().get(captor.getAllValues().size() - 1).block();
        assertThat(last.getPayload()).isEqualTo(SagaEvents.SAGA_FAILED);
    }
}
