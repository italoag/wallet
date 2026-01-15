package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import java.net.URI;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;

import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import dev.bloco.wallet.hub.infra.adapter.tracing.propagation.CloudEventTracePropagator;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaEvents;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaStates;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.micrometer.tracing.Span;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@DisplayName("Wallet Created Event Consumer Tests")
class WalletCreatedEventConsumerTest {

    private StateMachine<SagaStates, SagaEvents> stateMachine;
    private CloudEventTracePropagator tracePropagator;
    private Span mockSpan;
    private WalletCreatedEventConsumer consumerConfig;

    @BeforeEach
    void setUp() {
        stateMachine = mock(StateMachine.class);
        tracePropagator = mock(CloudEventTracePropagator.class);
        mockSpan = mock(Span.class);
        
        // Mock the trace propagator to return a mock span
        doReturn(mockSpan).when(tracePropagator).extractTraceContext(org.mockito.ArgumentMatchers.any(CloudEvent.class));
        doReturn(mockSpan).when(mockSpan).name(org.mockito.ArgumentMatchers.anyString());
        doReturn(mockSpan).when(mockSpan).tag(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        doReturn(mockSpan).when(mockSpan).event(org.mockito.ArgumentMatchers.anyString());
        
        doReturn(Flux.empty()).when(stateMachine)
                .sendEvent(org.mockito.ArgumentMatchers.<reactor.core.publisher.Mono<org.springframework.messaging.Message<SagaEvents>>>any());
        consumerConfig = new WalletCreatedEventConsumer();
    }

    @Test
    @DisplayName("Should send WalletCreated event to state machine")
    void walletCreatedEventConsumerFunction_withCorrelationId_sendsWalletCreated() throws Exception {
        Consumer<Message<CloudEvent>> fn = consumerConfig.walletCreatedEventConsumerFunction(stateMachine, tracePropagator);
        UUID corrId = UUID.randomUUID();
        var event = new WalletCreatedEvent(UUID.randomUUID(), corrId);
        
        CloudEvent cloudEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType("WalletCreatedEvent")
                .withSource(URI.create("/test"))
                .withDataContentType("application/json")
                .withData(("{ \"walletId\": \"" + event.getWalletId() + "\", \"correlationId\": \"" + corrId + "\" }").getBytes())
                .build();
        
        Message<CloudEvent> message = MessageBuilder.withPayload(cloudEvent).build();

        fn.accept(message);

        ArgumentCaptor<Mono<Message<SagaEvents>>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(stateMachine).sendEvent(captor.capture());
        Message<SagaEvents> sent = captor.getValue().block();
        assertThat(sent.getPayload()).isEqualTo(SagaEvents.WALLET_CREATED);
        assertThat(sent.getHeaders().get("correlationId")).isEqualTo(corrId);
    }

    @Test
    @DisplayName("Should send SagaFailed event to state machine when correlationId is null")
    void walletCreatedEventConsumerFunction_withoutCorrelationId_sendsSagaFailed() throws Exception {
        Consumer<Message<CloudEvent>> fn = consumerConfig.walletCreatedEventConsumerFunction(stateMachine, tracePropagator);
        var event = new WalletCreatedEvent(UUID.randomUUID(), null);
        
        CloudEvent cloudEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType("WalletCreatedEvent")
                .withSource(URI.create("/test"))
                .withDataContentType("application/json")
                .withData(("{ \"walletId\": \"" + event.getWalletId() + "\" }").getBytes())
                .build();
        
        Message<CloudEvent> message = MessageBuilder.withPayload(cloudEvent).build();

        fn.accept(message);

        ArgumentCaptor<Mono<Message<SagaEvents>>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(stateMachine, atLeastOnce()).sendEvent(captor.capture());
        Message<SagaEvents> last = captor.getAllValues().get(captor.getAllValues().size() - 1).block();
        assertThat(last.getPayload()).isEqualTo(SagaEvents.SAGA_FAILED);
    }
}
