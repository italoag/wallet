package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.infra.provider.data.OutboxEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;

@DisplayName("Outbox Repository Tests")
@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class OutboxRepositoryTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @DisplayName("findBySentFalse should return only unsent events")
    @Test
    void findBySentFalse_returnsOnlyUnsentEvents() {
        OutboxEvent e1 = new OutboxEvent();
        e1.setEventType("WalletCreated");
        e1.setPayload("{\"id\":1}");
        // sent defaults to false

        OutboxEvent e2 = new OutboxEvent();
        e2.setEventType("FundsAdded");
        e2.setPayload("{\"amount\":100}");
        e2.setSent(true);

        OutboxEvent e3 = new OutboxEvent();
        e3.setEventType("FundsWithdrawn");
        e3.setPayload("{\"amount\":50}");
        // sent defaults to false

        outboxRepository.saveAll(List.of(e1, e2, e3));

        List<OutboxEvent> unsent = outboxRepository.findBySentFalse();

        assertThat(unsent).hasSize(2);
        assertThat(unsent).extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("WalletCreated", "FundsWithdrawn");
    }
}
