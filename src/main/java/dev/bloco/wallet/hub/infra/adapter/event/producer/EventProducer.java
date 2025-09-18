package dev.bloco.wallet.hub.infra.adapter.event.producer;

import dev.bloco.wallet.hub.domain.event.wallet.FundsAddedEvent;
import dev.bloco.wallet.hub.domain.event.wallet.FundsTransferredEvent;
import dev.bloco.wallet.hub.domain.event.wallet.FundsWithdrawnEvent;
import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;

/**
 * An interface for producing various types of domain events.
 *<p/>
 * EventProducer defines methods for creating and persisting distinct types of events
 * related to wallet and financial operations, such as wallet creation, funds addition,
 * funds withdrawal, and fund transfer. These events are intended to be consumed by
 * external systems or services, enabling event-driven architecture and decoupled communication.
 *<p/>
 * Implementers of this interface are expected to provide specific mechanisms for event persistence
 * and delivery, such as writing events to an outbox and sending them to a messaging system like Kafka.
 */
public interface EventProducer {
    void produceWalletCreatedEvent(WalletCreatedEvent event);
    void produceFundsAddedEvent(FundsAddedEvent event);
    void produceFundsWithdrawnEvent(FundsWithdrawnEvent event);
    void produceFundsTransferredEvent(FundsTransferredEvent event);
}
