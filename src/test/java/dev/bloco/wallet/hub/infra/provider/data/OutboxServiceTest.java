package dev.bloco.wallet.hub.infra.provider.data;

import dev.bloco.wallet.hub.infra.provider.data.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Outbox Service Tests")
class OutboxServiceTest {

    private OutboxRepository outboxRepository;
    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(OutboxRepository.class);
        outboxService = new OutboxService(outboxRepository);
    }

    @Test
    @DisplayName("saveOutboxEvent persists event with provided fields")
    void saveOutboxEvent_persistsWithProvidedFields() {
        // when
        outboxService.saveOutboxEvent("type-A", "{payload}", "corr-xyz");

        // then
        verify(outboxRepository).save(argThat(e ->
                e.getEventType().equals("type-A") &&
                e.getPayload().equals("{payload}") &&
                e.getCorrelationId().equals("corr-xyz") &&
                !e.isSent()
        ));
    }

    @Test
    @DisplayName("markEventAsSent sets flag and saves")
    void markEventAsSent_setsFlagAndSaves() {
        OutboxEvent event = new OutboxEvent();
        event.setEventType("t");
        event.setPayload("p");
        event.setSent(false);

        outboxService.markEventAsSent(event);

        assertThat(event.isSent()).isTrue();
        verify(outboxRepository).save(event);
    }

    @Test
    @DisplayName("getUnsentEvents delegates to repository")
    void getUnsentEvents_delegatesToRepository() {
        when(outboxRepository.findBySentFalse()).thenReturn(List.of(new OutboxEvent()));

        List<OutboxEvent> result = outboxService.getUnsentEvents();

        assertThat(result).hasSize(1);
        verify(outboxRepository).findBySentFalse();
    }
}
