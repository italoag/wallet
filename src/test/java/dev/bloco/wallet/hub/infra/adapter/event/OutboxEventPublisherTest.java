package dev.bloco.wallet.hub.infra.adapter.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import dev.bloco.wallet.hub.infra.provider.data.OutboxEvent;
import dev.bloco.wallet.hub.infra.provider.data.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Outbox Event Publisher Tests")
class OutboxEventPublisherTest {

    private OutboxRepository outboxRepository;
    private ObjectMapper objectMapper;
    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(OutboxRepository.class);
        objectMapper = new ObjectMapper();
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
    void publish_onSerializationError_throwsRuntimeException(CapturedOutput output) throws JsonProcessingException {
        // given
        var badMapper = mock(ObjectMapper.class);
        when(badMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {
        });
        var failingPublisher = new OutboxEventPublisher(outboxRepository, badMapper);

        // when/then
        var event = new Object();
        assertThatThrownBy(() -> failingPublisher.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to serialize event");
        verify(outboxRepository, never()).save(any());
        assertThat(output.getOut()).contains("Failed to serialize event");
    }
}
