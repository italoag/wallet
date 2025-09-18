package dev.bloco.wallet.hub.domain.event.wallet;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents an event indicating that funds have been added to a wallet.
 *<p/>
 * This event contains details about the wallet where funds were added,
 * the amount added, and a correlation ID to track related operations.
 *<p/>
 * The correlation ID helps in associating this event with a specific
 * transaction or process, enabling streamlined tracking and management
 * of operations across different components.
 *
 * @param walletId id of the wallet where funds were added
 * @param amount amount of funds added
 * @param correlationId correlation ID for the operation
 *
 */
@Builder
public record FundsAddedEvent(UUID walletId, BigDecimal amount, String correlationId) {

}
