package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
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

import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Wallet Created Event Consumer Tests")
class WalletCreatedEventConsumerTest {

    private StateMachine<SagaStates, SagaEvents> stateMachine;
    private WalletCreatedEventConsumer consumerConfig;

    @BeforeEach
    void setUp() {
        stateMachine = mock(StateMachine.class);
        doReturn(Flux.empty()).when(stateMachine)
                .sendEvent(org.mockito.ArgumentMatchers.<reactor.core.publisher.Mono<org.springframework.messaging.Message<SagaEvents>>>any());
        consumerConfig = new WalletCreatedEventConsumer(stateMachine);
    }

    @Test
    @DisplayName("Should send WalletCreated event to state machine")
    void walletCreatedEventConsumerFunction_withCorrelationId_sendsWalletCreated() {
        Consumer<Message<WalletCreatedEvent>> fn = consumerConfig.walletCreatedEventConsumerFunction();
        UUID corrId = UUID.randomUUID();
        var event = new WalletCreatedEvent(UUID.randomUUID(), corrId);
        Message<WalletCreatedEvent> message = MessageBuilder.withPayload(event).build();

        fn.accept(message);

        ArgumentCaptor<Mono<Message<SagaEvents>>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(stateMachine).sendEvent(captor.capture());
        Message<SagaEvents> sent = captor.getValue().block();
        assertThat(sent.getPayload()).isEqualTo(SagaEvents.WALLET_CREATED);
        assertThat(sent.getHeaders().get("correlationId")).isEqualTo(corrId);
    }

    @Test
    @DisplayName("Should send SagaFailed event to state machine when correlationId is null")
    void walletCreatedEventConsumerFunction_withoutCorrelationId_sendsSagaFailed() {
        Consumer<Message<WalletCreatedEvent>> fn = consumerConfig.walletCreatedEventConsumerFunction();
        var event = new WalletCreatedEvent(UUID.randomUUID(), null);
        Message<WalletCreatedEvent> message = MessageBuilder.withPayload(event).build();

        fn.accept(message);

        ArgumentCaptor<Mono<Message<SagaEvents>>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(stateMachine, atLeastOnce()).sendEvent(captor.capture());
        Message<SagaEvents> last = captor.getAllValues().get(captor.getAllValues().size() - 1).block();
        assertThat(last.getPayload()).isEqualTo(SagaEvents.SAGA_FAILED);
    }
}
