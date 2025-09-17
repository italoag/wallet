package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckBalanceUseCase(WalletRepository walletRepository) {

  public BigDecimal checkBalance(UUID walletId) {
    Wallet wallet = walletRepository.findById(walletId)
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    return wallet.getBalance();
  }
}
