package dev.bloco.wallet.hub.domain.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record FundsTransferredEvent(UUID fromWalletId, UUID toWalletId, BigDecimal amount, String correlationId) {

}
