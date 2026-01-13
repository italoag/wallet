package dev.bloco.wallet.hub.infra.adapter.event.producer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.cloud.stream.function.StreamBridge;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.bloco.wallet.hub.domain.event.wallet.FundsAddedEvent;
import dev.bloco.wallet.hub.domain.event.wallet.FundsTransferredEvent;
import dev.bloco.wallet.hub.domain.event.wallet.FundsWithdrawnEvent;
import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import dev.bloco.wallet.hub.infra.adapter.tracing.propagation.CloudEventTracePropagator;
import dev.bloco.wallet.hub.infra.provider.data.OutboxEvent;
import dev.bloco.wallet.hub.infra.provider.data.OutboxService;
import dev.bloco.wallet.hub.infra.provider.data.OutboxWorker;

@DisplayName("Kafka Event Producer Tests")
class KafkaEventProducerTest {

    private OutboxService outboxService;
    private StreamBridge streamBridge;
    private ObjectMapper objectMapper;
    private CloudEventTracePropagator tracePropagator;
    private KafkaEventProducer producer;

    @BeforeEach
    void setUp() {
        outboxService = mock(OutboxService.class);
        streamBridge = mock(StreamBridge.class);
        objectMapper = new ObjectMapper();
        tracePropagator = mock(CloudEventTracePropagator.class);
        producer = new KafkaEventProducer(outboxService, streamBridge, objectMapper, tracePropagator);
    }

    @Test
    @DisplayName("Should produce wallet created event saved into outbox with correct type")
    void produceWalletCreatedEvent_savesIntoOutboxWithCorrectType() {
        var correlationId = UUID.randomUUID();
        var event = new WalletCreatedEvent(UUID.randomUUID(), correlationId);

        producer.produceWalletCreatedEvent(event);

        verifySaved("walletCreatedEventProducer", event, correlationId.toString());
    }

    @Test
    @DisplayName("Should produce funds added event saved into outbox with correct type")
    void produceFundsAddedEvent_savesIntoOutboxWithCorrectType() {
        var event = FundsAddedEvent.builder()
                .walletId(UUID.randomUUID())
                .amount(new BigDecimal("10.50"))
                .correlationId("c-2")
                .build();

        producer.produceFundsAddedEvent(event);

        verifySaved("fundsAddedEventProducer", event, "c-2");
    }

    @Test
    @DisplayName("Should produce funds withdrawn event saved into outbox with correct type")
    void produceFundsWithdrawnEvent_savesIntoOutboxWithCorrectType() {
        var event = FundsWithdrawnEvent.builder()
                .walletId(UUID.randomUUID())
                .amount(new BigDecimal("5.00"))
                .correlationId("c-3")
                .build();

        producer.produceFundsWithdrawnEvent(event);

        verifySaved("fundsWithdrawnEventProducer", event, "c-3");
    }

    @Test
    @DisplayName("Should produce funds transferred event saved into outbox with correct type")
    void produceFundsTransferredEvent_savesIntoOutboxWithCorrectType() {
        var event = FundsTransferredEvent.builder()
                .fromWalletId(UUID.randomUUID())
                .toWalletId(UUID.randomUUID())
                .amount(new BigDecimal("1.00"))
                .correlationId("c-4")
                .build();

        producer.produceFundsTransferredEvent(event);

        verifySaved("fundsTransferredEventProducer", event, "c-4");
    }

  private void verifySaved(String expectedType, Object expectedEvent, String expectedCorrelationId) {
    ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> corrCaptor = ArgumentCaptor.forClass(String.class);
    verify(outboxService).saveOutboxEvent(typeCaptor.capture(), payloadCaptor.capture(), corrCaptor.capture());
    assertThat(typeCaptor.getValue()).isEqualTo(expectedType);
    assertThat(payloadCaptor.getValue()).contains("correlationId");
    assertThat(corrCaptor.getValue()).isEqualTo(expectedCorrelationId);
  }

    @Test
    @DisplayName("Should process outbox and send events to kafka")
    void processOutbox_sendsOnlySuccessfulAndMarksAsSent() {
        // given
        var e1 = new OutboxEvent();
        e1.setId(1L);
        e1.setEventType("fundsAddedEventProducer");
        e1.setPayload("p1");
        var e2 = new OutboxEvent();
        e2.setId(2L);
        e2.setEventType("fundsWithdrawnEventProducer");
        e2.setPayload("p2");
        when(outboxService.getUnsentEvents()).thenReturn(List.of(e1, e2));
        // Now expects CloudEvent objects instead of raw strings
        when(streamBridge.send(eq("fundsAddedEventProducer-out-0"), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(streamBridge.send(eq("fundsWithdrawnEventProducer-out-0"), org.mockito.ArgumentMatchers.any())).thenReturn(false);

    // when
    OutboxWorker worker = new OutboxWorker(outboxService, streamBridge);
    worker.processOutbox();

    // then
    verify(outboxService).markEventAsSent(e1);
    verify(outboxService, never()).markEventAsSent(e2);
  }
}
