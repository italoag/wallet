package dev.bloco.wallet.hub.domain.event;

import lombok.Builder;

import java.util.UUID;

/**
 * Represents an event indicating that a wallet was created.
 *<p/>
 * This event contains essential information about the newly created wallet,
 * including the unique identifier of the wallet, the associated user, and a
 * correlation ID for tracking related operations.
 *<p/>
 * The correlation ID can be used to link this event to other
 * operations or transactions, enabling better traceability and
 * coordination within the system.
 *
 * @param walletId the unique identifier of the created wallet
 * @param userId the unique identifier of the user associated with the wallet
 * @param correlationId a correlation ID for tracking the operation
 */
@Builder
public record WalletCreatedEvent(UUID walletId, UUID userId, String correlationId) {

}
