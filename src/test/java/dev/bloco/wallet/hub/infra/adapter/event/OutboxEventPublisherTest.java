package dev.bloco.wallet.hub.infra.adapter.event;

import tools.jackson.databind.ObjectMapper;
import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import dev.bloco.wallet.hub.infra.provider.data.OutboxEvent;
import dev.bloco.wallet.hub.infra.provider.data.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.core.JacksonException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Outbox Event Publisher Tests")
class OutboxEventPublisherTest {

  private OutboxRepository outboxRepository;
  private OutboxEventPublisher publisher;

  @BeforeEach
  void setUp() {
    outboxRepository = mock(OutboxRepository.class);
    ObjectMapper objectMapper = new ObjectMapper();
    publisher = new OutboxEventPublisher(outboxRepository, objectMapper);
  }

  @Test
  @DisplayName("Should save event into outbox")
  void publish_serializesEventAndSavesOutbox() {
    // given
    var event = new WalletCreatedEvent(UUID.randomUUID(), UUID.randomUUID());

    // when
    publisher.publish(event);

    // then
    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxRepository).save(captor.capture());
    OutboxEvent saved = captor.getValue();
    assertThat(saved.getEventType()).isEqualTo("WalletCreatedEvent");
    assertThat(saved.getPayload()).contains("correlationId");
  }

  @Test
  @DisplayName("Should throw when serialization fails")
  void publish_onSerializationError_throwsRuntimeException() throws JacksonException {
    // given
    var badMapper = mock(ObjectMapper.class);
    when(badMapper.writeValueAsString(any())).thenThrow(new JacksonException("boom") {
    });
    var failingPublisher = new OutboxEventPublisher(outboxRepository, badMapper);

    // when/then
    assertThatThrownBy(() -> failingPublisher.publish(new Object()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to serialize event");
    verify(outboxRepository, never()).save(any());
  }
}
