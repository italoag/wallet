package dev.bloco.wallet.hub.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a wallet in the system. Each wallet is associated with a specific user and has a balance
 * that tracks the monetary amount stored in it. Wallets are uniquely identified using a UUID.
 * Provides functionality to add and withdraw funds while validating the operations.
 */
@Getter
public class Wallet {
    private final UUID id;
    private final UUID userId;
    private BigDecimal balance;

  /**
   * Constructs a new Wallet instance for the specified user. Each wallet is
   * assigned a unique identifier (UUID) at the time of creation and initialized
   * with a balance of zero.
   *
   * @param userId the unique identifier of the user to which this wallet belongs
   */
  public Wallet(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.balance = BigDecimal.ZERO;
    }

  /**
   * Adds a specified amount to the wallet's balance. The amount must be greater than zero.
   * If the amount is invalid (less than or equal to zero), an {@code IllegalArgumentException}
   * is thrown.
   *
   * @param amount the monetary value to be added to the wallet's balance.
   *               This must be a positive number greater than zero.
   * @throws IllegalArgumentException if the specified amount is not greater than zero.
   */
  public void addFunds(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
        } else {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

  /**
   * Withdraws a specified amount from the wallet's balance. The amount must be greater than zero
   * and less than or equal to the current balance. If the amount is invalid or the wallet has
   * insufficient balance, an {@code IllegalArgumentException} is thrown.
   *
   * @param amount the monetary value to be withdrawn from the wallet's balance.
   *               This must be a positive number greater than zero and less than or equal
   *               to the current wallet balance.
   * @throws IllegalArgumentException if the specified amount is not greater than zero
   *                                  or the wallet has insufficient balance.
   */
  public void withdrawFunds(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0 && this.balance.compareTo(amount) >= 0) {
            this.balance = this.balance.subtract(amount);
        } else {
            throw new IllegalArgumentException("Insufficient balance or invalid amount");
        }
    }
}
