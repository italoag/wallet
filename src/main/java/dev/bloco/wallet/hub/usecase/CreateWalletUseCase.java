package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.event.WalletCreatedEvent;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;


import java.util.UUID;

public record CreateWalletUseCase(WalletRepository walletRepository, DomainEventPublisher eventPublisher) {

  public Wallet createWallet(UUID userId, String correlationId) {
    Wallet wallet = new Wallet(userId);
    walletRepository.save(wallet);
    WalletCreatedEvent event = new WalletCreatedEvent(wallet.getId(), userId, correlationId);
    eventPublisher.publish(event);
    return wallet;
  }
}
