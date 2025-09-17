package dev.bloco.wallet.hub.infra.adapter.event.producer;

import dev.bloco.wallet.hub.domain.event.FundsAddedEvent;
import dev.bloco.wallet.hub.domain.event.FundsTransferredEvent;
import dev.bloco.wallet.hub.domain.event.FundsWithdrawnEvent;
import dev.bloco.wallet.hub.domain.event.WalletCreatedEvent;
import dev.bloco.wallet.hub.infra.provider.data.OutboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaEventProducer implements EventProducer {

    private final OutboxService outboxService;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaEventProducer(OutboxService outboxService, StreamBridge streamBridge, ObjectMapper objectMapper) {
        this.outboxService = outboxService;
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
    }

    @Override
    public void produceWalletCreatedEvent(WalletCreatedEvent event) {
        saveEventToOutbox("walletCreatedEventProducer", event);
    }

    @Override
    public void produceFundsAddedEvent(FundsAddedEvent event) {
        saveEventToOutbox("fundsAddedEventProducer", event);
    }

    @Override
    public void produceFundsWithdrawnEvent(FundsWithdrawnEvent event) {
        saveEventToOutbox("fundsWithdrawnEventProducer", event);
    }

    @Override
    public void produceFundsTransferredEvent(FundsTransferredEvent event) {
        saveEventToOutbox("fundsTransferredEventProducer", event);
    }

    private void saveEventToOutbox(String eventType, Object event) {
        try {
            var payload = objectMapper.writeValueAsString(event);
            outboxService.saveOutboxEvent(eventType, payload, null); // Adicionar correlationId se necessário
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event", e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    @Scheduled(fixedRate = 5000)
    public void processOutbox() {
        var unsentEvents = outboxService.getUnsentEvents();
        unsentEvents.stream()
            .filter(event -> streamBridge.send(event.getEventType() + "-out-0", event.getPayload()))
            .forEach(outboxService::markEventAsSent);
    }
}