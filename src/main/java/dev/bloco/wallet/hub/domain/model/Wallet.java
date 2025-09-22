package dev.bloco.wallet.hub.domain.model;

import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import dev.bloco.wallet.hub.domain.event.wallet.WalletUpdatedEvent;
import dev.bloco.wallet.hub.domain.event.wallet.WalletStatusChangedEvent;
import dev.bloco.wallet.hub.domain.event.wallet.WalletDeletedEvent;
import dev.bloco.wallet.hub.domain.event.wallet.WalletRecoveryInitiatedEvent;
import dev.bloco.wallet.hub.domain.model.common.AggregateRoot;
import dev.bloco.wallet.hub.domain.model.wallet.WalletStatus;
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
    private WalletStatus status;
    private UUID userId;

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
        this.status = WalletStatus.ACTIVE;
    }

    public Wallet(UUID id, String name, String description, UUID userId) {
        super(id);
        this.name = name;
        this.description = description;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.balance = BigDecimal.ZERO;
        this.status = WalletStatus.ACTIVE;
        this.userId = userId;
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

  /**
   * Activates the wallet, allowing all operations to be performed.
   * If the wallet is already active, no change occurs.
   */
  public void activate() {
    if (this.status != WalletStatus.ACTIVE) {
      WalletStatus oldStatus = this.status;
      this.status = WalletStatus.ACTIVE;
      this.updatedAt = Instant.now();
      registerEvent(new WalletStatusChangedEvent(getId(), oldStatus, this.status, "Wallet activated", this.correlationId));
    }
  }

  /**
   * Deactivates the wallet, restricting operations.
   * The wallet can be reactivated later.
   */
  public void deactivate() {
    if (this.status != WalletStatus.INACTIVE) {
      WalletStatus oldStatus = this.status;
      this.status = WalletStatus.INACTIVE;
      this.updatedAt = Instant.now();
      registerEvent(new WalletStatusChangedEvent(getId(), oldStatus, this.status, "Wallet deactivated", this.correlationId));
    }
  }

  /**
   * Soft deletes the wallet. The wallet data is retained for audit purposes
   * but is hidden from normal operations.
   * 
   * @param reason the reason for deletion
   */
  public void delete(String reason) {
    if (this.status != WalletStatus.DELETED) {
      WalletStatus oldStatus = this.status;
      this.status = WalletStatus.DELETED;
      this.updatedAt = Instant.now();
      registerEvent(new WalletStatusChangedEvent(getId(), oldStatus, this.status, reason, this.correlationId));
      registerEvent(new WalletDeletedEvent(getId(), reason, this.correlationId));
    }
  }

  /**
   * Locks the wallet due to security concerns.
   * No operations are allowed until the wallet is unlocked.
   * 
   * @param reason the reason for locking
   */
  public void lock(String reason) {
    if (this.status != WalletStatus.LOCKED) {
      WalletStatus oldStatus = this.status;
      this.status = WalletStatus.LOCKED;
      this.updatedAt = Instant.now();
      registerEvent(new WalletStatusChangedEvent(getId(), oldStatus, this.status, reason, this.correlationId));
    }
  }

  /**
   * Initiates wallet recovery process.
   * The wallet enters recovery state while being restored.
   * 
   * @param recoveryMethod the method used for recovery (e.g., "seed_phrase", "backup")
   */
  public void initiateRecovery(String recoveryMethod) {
    if (this.status != WalletStatus.RECOVERING) {
      WalletStatus oldStatus = this.status;
      this.status = WalletStatus.RECOVERING;
      this.updatedAt = Instant.now();
      registerEvent(new WalletStatusChangedEvent(getId(), oldStatus, this.status, "Recovery initiated", this.correlationId));
      registerEvent(new WalletRecoveryInitiatedEvent(getId(), this.userId, recoveryMethod, this.correlationId));
    }
  }

  /**
   * Checks if the wallet is active and can perform operations.
   * 
   * @return true if the wallet is active, false otherwise
   */
  public boolean isActive() {
    return this.status == WalletStatus.ACTIVE;
  }

  /**
   * Checks if the wallet is deleted.
   * 
   * @return true if the wallet is deleted, false otherwise
   */
  public boolean isDeleted() {
    return this.status == WalletStatus.DELETED;
  }

  /**
   * Validates if operations can be performed on this wallet.
   * Operations are allowed only for active wallets.
   * 
   * @throws IllegalStateException if the wallet is not in a state that allows operations
   */
  public void validateOperationAllowed() {
    if (!isActive()) {
      throw new IllegalStateException("Operation not allowed. Wallet status: " + this.status);
    }
  }
}