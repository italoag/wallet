package dev.bloco.wallet.hub.domain.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents an event indicating that funds have been withdrawn from a wallet.
 *<p/>
 * This event encapsulates details about the wallet from which funds were withdrawn,
 * the amount that was withdrawn, and a correlation ID to track the associated operation.
 *<p/>
 * The correlation ID is used to relate this event to a specific transaction or process,
 * ensuring better traceability and coordination within the system's operations.
 *
 *  @param walletId id of the wallet from which funds were withdrawn
 *  @param amount amount of funds withdrawn
 *  @param correlationId correlation ID for the operation
 */
@Builder
public record FundsWithdrawnEvent(UUID walletId, BigDecimal amount, String correlationId) {

}
