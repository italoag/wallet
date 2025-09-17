package dev.bloco.wallet.hub.infra.adapter.event.producer;

import dev.bloco.wallet.hub.domain.event.FundsAddedEvent;
import dev.bloco.wallet.hub.domain.event.FundsTransferredEvent;
import dev.bloco.wallet.hub.domain.event.FundsWithdrawnEvent;
import dev.bloco.wallet.hub.domain.event.WalletCreatedEvent;

public interface EventProducer {
    void produceWalletCreatedEvent(WalletCreatedEvent event);
    void produceFundsAddedEvent(FundsAddedEvent event);
    void produceFundsWithdrawnEvent(FundsWithdrawnEvent event);
    void produceFundsTransferredEvent(FundsTransferredEvent event);
}
