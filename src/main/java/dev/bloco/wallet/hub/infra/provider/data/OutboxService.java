package dev.bloco.wallet.hub.infra.provider.data;

import dev.bloco.wallet.hub.infra.provider.data.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;

    @Autowired
    public OutboxService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public void saveOutboxEvent(String eventType, String payload, String correlationId) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setCorrelationId(correlationId);
        outboxRepository.save(event);
    }

    @Transactional
    public void markEventAsSent(OutboxEvent event) {
        event.setSent(true);
        outboxRepository.save(event);
    }

    public List<OutboxEvent> getUnsentEvents() {
        return outboxRepository.findBySentFalse();
    }
}
