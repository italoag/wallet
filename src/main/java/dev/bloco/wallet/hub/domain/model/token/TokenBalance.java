package dev.bloco.wallet.hub.domain.model.token;


import dev.bloco.wallet.hub.domain.event.common.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.event.token.TokenBalanceChangedEvent;
import dev.bloco.wallet.hub.domain.model.common.Entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents the balance of a specific token for a specific address.
 * This class is used to track the balance of a token associated with a specific address
 * and to manage updates to that balance while publishing events related to those updates.
 */
public class TokenBalance extends Entity {
    private final UUID addressId;
    private final UUID tokenId;
    private BigDecimal balance;
    private Instant lastUpdated;

  /**
   * Creates a new instance of the TokenBalance class with the specified parameters,
   * initializes its balance, and publishes a TokenBalanceChangedEvent.
   *
   * @param id              the unique identifier of the token balance
   * @param addressId       the unique identifier of the address associated with the token balance
   * @param tokenId         the unique identifier of the token
   * @param initialBalance  the initial balance of the token
   * @return a newly created TokenBalance instance with the specified parameters and initial balance
   */
    public static TokenBalance create(
            UUID id,
            UUID addressId,
            UUID tokenId,
            BigDecimal initialBalance) {
        
        TokenBalance tokenBalance = new TokenBalance(id, addressId, tokenId, initialBalance);
        DomainEventPublisher.publish(new TokenBalanceChangedEvent(id, addressId, tokenId, initialBalance, null));
        return tokenBalance;
    }

  /**
   * Constructs a TokenBalance instance with the specified id, addressId, tokenId, and balance.
   *
   * @param id        the unique identifier of the token balance
   * @param addressId the unique identifier of the address associated with the token balance
   * @param tokenId   the unique identifier of the token
   * @param balance   the current balance of the token
   */
    public TokenBalance(
        UUID id,
        UUID addressId,
        UUID tokenId,
        BigDecimal balance) {
        super(id);
        this.addressId = addressId;
        this.tokenId = tokenId;
        this.balance = balance;
        this.lastUpdated = Instant.now();
    }

    public UUID getAddressId() {
        return addressId;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

  /**
   * Updates the balance of the token to the specified value and records the time of the update.
   * A TokenBalanceChangedEvent is published to notify subscribers of the change in balance.
   *
   * @param newBalance The new balance to be set for the token.
   */
  public void updateBalance(BigDecimal newBalance) {
        BigDecimal oldBalance = this.balance;
        this.balance = newBalance;
        this.lastUpdated = Instant.now();
        
        DomainEventPublisher.publish(
            new TokenBalanceChangedEvent(getId(), addressId, tokenId, newBalance, null)
        );
    }

  /**
   * Adds the specified amount to the current balance of the token and updates the balance accordingly.
   *
   * @param amount The amount to be added to the current balance. Must be a non-null BigDecimal value.
   */
  public void addToBalance(BigDecimal amount) {
        updateBalance(this.balance.add(amount));
    }

  /**
   * Subtracts the specified amount from the current balance.
   * If the balance after subtraction is negative, an IllegalArgumentException is thrown.
   *
   * @param amount The amount to subtract from the current balance. Must be a non-null BigDecimal and less than or equal to the current balance.
   */
  public void subtractFromBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        updateBalance(this.balance.subtract(amount));
    }
}