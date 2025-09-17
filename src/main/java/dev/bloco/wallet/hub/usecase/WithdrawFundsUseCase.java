package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.Transaction;
import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.event.FundsWithdrawnEvent;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawFundsUseCase(WalletRepository walletRepository, TransactionRepository transactionRepository,
                                   DomainEventPublisher eventPublisher) {

  public void withdrawFunds(UUID walletId, BigDecimal amount, String correlationId) {
    Wallet wallet = walletRepository.findById(walletId)
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    wallet.withdrawFunds(amount);
    walletRepository.update(wallet);
    transactionRepository.save(new Transaction(wallet.getId(), null, amount, Transaction.TransactionType.WITHDRAWAL));
    FundsWithdrawnEvent event = new FundsWithdrawnEvent(wallet.getId(), amount, correlationId);
    eventPublisher.publish(event);
  }
}

