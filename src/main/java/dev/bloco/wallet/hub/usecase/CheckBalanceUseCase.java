package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CheckBalanceUseCase is responsible for retrieving the current balance of a specific wallet.
 * It encapsulates the logic for identifying a wallet by its unique identifier and obtaining
 * the current monetary balance stored within it.
 *<p/>
 * Responsibilities:
 * - Validate and locate a wallet using its unique identifier.
 * - Retrieve the balance of the located wallet.
 *<p/>
 * Uses:
 * - WalletRepository for fetching the Wallet entity from the system's persistent storage.
 *<p/>
 * Exceptions:
 * - IllegalArgumentException is thrown if the wallet cannot be found in the repository.
 *
 * @param walletRepository Repository for wallet data access.
 */
public record CheckBalanceUseCase(WalletRepository walletRepository) {

  /**
   * Retrieves the current balance of the specified wallet.
   * This method locates the wallet using its unique identifier (UUID),
   * and if found, returns the monetary balance associated with the wallet.
   * If the wallet is not found, an IllegalArgumentException is thrown.
   *
   * @param walletId the unique identifier of the wallet whose balance is to be retrieved
   * @return the current balance of the wallet as a BigDecimal
   * @throws IllegalArgumentException if the wallet with the specified ID does not exist
   */
  public BigDecimal checkBalance(UUID walletId) {
    Wallet wallet = walletRepository.findById(walletId)
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    return wallet.getBalance();
  }
}
