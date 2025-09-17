package dev.bloco.wallet.hub.infra.provider.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxWorker {

    private final OutboxService outboxService;
    private final StreamBridge streamBridge;

    @Autowired
    public OutboxWorker(OutboxService outboxService, StreamBridge streamBridge) {
        this.outboxService = outboxService;
        this.streamBridge = streamBridge;
    }

    @Scheduled(fixedRate = 5000)
    public void processOutbox() {
        for (OutboxEvent event : outboxService.getUnsentEvents()) {
            boolean success = streamBridge.send(event.getEventType() + "-out-0", event.getPayload());
            if (success) {
                outboxService.markEventAsSent(event);
            }
        }
    }
}
