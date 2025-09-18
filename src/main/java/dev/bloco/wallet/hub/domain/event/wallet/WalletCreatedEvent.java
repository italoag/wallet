package dev.bloco.wallet.hub.domain.event.wallet;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Represents an event indicating the creation of a wallet.
 *
 * This event is triggered when a new wallet is created in the domain model.
 * It contains information about the wallet's unique identifier and allows
 * for tracking the event's correlation within the system.
 *
 * The correlation ID is used to associate this event with related operations
 * or processes, ensuring better traceability and consistency in handling
 * domain events.
 */
@Getter
public class WalletCreatedEvent extends DomainEvent {
    private final UUID walletId;


  /**
   * Constructs a WalletCreatedEvent with the specified wallet ID and correlation ID.
   *
   * This event is triggered when a new wallet is created, allowing the system to
   * log and handle the creation within the domain. The correlation ID ensures that
   * this event is associated with a specific workflow or operation.
   *
   * @param walletId the unique identifier of the wallet being created
   * @param correlationId the unique identifier for correlating this event with related operations
   */
  public WalletCreatedEvent(UUID walletId, UUID correlationId) {
        super(correlationId);
      this.walletId = walletId;
    }

}