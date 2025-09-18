package dev.bloco.wallet.hub.domain.model;

import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import dev.bloco.wallet.hub.domain.event.wallet.WalletUpdatedEvent;
import dev.bloco.wallet.hub.domain.model.common.AggregateRoot;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a Wallet entity that aggregates and manages related data and behaviors
 * associated with a wallet in the domain. Wallets are identified by a unique UUID and
 * support operations such as creating, updating, and managing associated addresses.
 *
 * This class extends the AggregateRoot entity to support the recognition and publishing
 * of domain events, such as wallet creation and updates.
 */
@Getter
@Setter
public class Wallet extends AggregateRoot {

    private String name;
    private String description;
    private final Set<UUID> addressIds = new HashSet<>();
    private final Instant createdAt;
    private Instant updatedAt;
    private BigDecimal balance;
    private UUID correlationId;

  /**
   * Creates a new Wallet instance with the specified attributes and registers a WalletCreatedEvent
   * to indicate its creation within the domain model.
   *
   * @param id the unique identifier for the wallet
   * @param name the name of the wallet
   * @param description a description of the wallet
   * @return a new Wallet instance initialized with the provided attributes
   */
    public static Wallet create(UUID id, String name, String description) {
        Wallet wallet = new Wallet(id, name, description);
        wallet.registerEvent(new WalletCreatedEvent(id, wallet.getCorrelationId()));
        return wallet;
    }

    public Wallet(UUID id, String name, String description) {
        super(id);
        this.name = name;
        this.description = description;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.balance = BigDecimal.ZERO;
    }

  public Set<UUID> getAddressIds() {
        return Collections.unmodifiableSet(addressIds);
    }

  public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = Instant.now();
        registerEvent(new WalletUpdatedEvent(getId(), name, description, this.getCorrelationId()));
    }

    public void addAddress(UUID addressId) {
        if (addressIds.add(addressId)) {
            this.updatedAt = Instant.now();
        }
    }

    public void removeAddress(UUID addressId) {
        if (addressIds.remove(addressId)) {
            this.updatedAt = Instant.now();
        }
    }

    public boolean containsAddress(UUID addressId) {
        return addressIds.contains(addressId);
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