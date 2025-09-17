package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.Transaction;
import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.event.FundsTransferredEvent;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferFundsUseCase(WalletRepository walletRepository, TransactionRepository transactionRepository,
                                   DomainEventPublisher eventPublisher) {

  public void transferFunds(UUID fromWalletId, UUID toWalletId, BigDecimal amount, String correlationId) {
    Wallet fromWallet = walletRepository.findById(fromWalletId)
        .orElseThrow(() -> new IllegalArgumentException("From Wallet not found"));
    Wallet toWallet = walletRepository.findById(toWalletId)
        .orElseThrow(() -> new IllegalArgumentException("To Wallet not found"));
    fromWallet.withdrawFunds(amount);
    toWallet.addFunds(amount);
    walletRepository.update(fromWallet);
    walletRepository.update(toWallet);
    transactionRepository.save(new Transaction(fromWallet.getId(), toWallet.getId(), amount, Transaction.TransactionType.TRANSFER));
    FundsTransferredEvent event = new FundsTransferredEvent(fromWallet.getId(), toWallet.getId(), amount, correlationId);
    eventPublisher.publish(event);
  }
}
