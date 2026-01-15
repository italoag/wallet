package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import dev.bloco.wallet.hub.domain.event.wallet.FundsTransferredEvent;
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

@DisplayName("Funds Transferred Event Consumer Tests")
class FundsTransferredEventConsumerTest {

    private StateMachine<SagaStates, SagaEvents> stateMachine;
    private FundsTransferredEventConsumer consumerConfig;

    @BeforeEach
    void setUp() {
        stateMachine = mock(StateMachine.class);
        doReturn(Flux.empty()).when(stateMachine)
                .sendEvent(org.mockito.ArgumentMatchers.<reactor.core.publisher.Mono<org.springframework.messaging.Message<SagaEvents>>>any());
        consumerConfig = new FundsTransferredEventConsumer();
    }

    @Test
    @DisplayName("Should send Funds Transferred event to state machine")
    void fundsTransferredEventConsumerFunction_sendsFundsTransferredWithCorrelationId() {
        Consumer<Message<FundsTransferredEvent>> fn = consumerConfig.fundsTransferredEventConsumerFunction(stateMachine);
        var event = FundsTransferredEvent.builder()
                .fromWalletId(UUID.randomUUID())
                .toWalletId(UUID.randomUUID())
                .amount(new BigDecimal("3.14"))
                .correlationId("c123")
                .build();
        var message = MessageBuilder.withPayload(event).build();

        fn.accept(message);

        ArgumentCaptor<Mono<Message<SagaEvents>>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(stateMachine).sendEvent(captor.capture());
        Message<SagaEvents> sent = captor.getValue().block();
        assertThat(sent.getPayload()).isEqualTo(SagaEvents.FUNDS_TRANSFERRED);
        assertThat(sent.getHeaders().get("correlationId")).isEqualTo("c123");
    }
}
