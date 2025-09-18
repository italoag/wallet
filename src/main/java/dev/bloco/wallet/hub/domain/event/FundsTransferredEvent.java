package dev.bloco.wallet.hub.domain.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents an event indicating that funds have been transferred between wallets.
 *<p/>
 * This event encapsulates details about the source and destination wallets,
 * the amount of funds transferred, and a correlation ID to track the related operation.
 *<p/>
 * The correlation ID can be used to associate this event with a specific
 * transaction or process, facilitating easier tracking of operations across
 * different components of the system.
 *
 * @param fromWalletId id of the source wallet
 * @param toWalletId id of the destination wallet
 * @param amount amount of funds transferred
 * @param correlationId correlation ID for the operation
 *
 */
@Builder
public record FundsTransferredEvent(UUID fromWalletId, UUID toWalletId, BigDecimal amount, String correlationId) {

}
