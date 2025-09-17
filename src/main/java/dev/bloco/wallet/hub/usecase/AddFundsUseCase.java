package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.Transaction;
import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.event.FundsAddedEvent;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

public record AddFundsUseCase(WalletRepository walletRepository, TransactionRepository transactionRepository,
                              DomainEventPublisher eventPublisher) {

  public void addFunds(UUID walletId, BigDecimal amount, String correlationId) {
    Wallet wallet = walletRepository.findById(walletId)
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    wallet.addFunds(amount);
    walletRepository.update(wallet);
    transactionRepository.save(new Transaction(null, wallet.getId(), amount, Transaction.TransactionType.DEPOSIT));
    FundsAddedEvent event = new FundsAddedEvent(wallet.getId(), amount, correlationId);
    eventPublisher.publish(event);
  }
}
