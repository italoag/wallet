package dev.bloco.wallet.hub.domain.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record FundsWithdrawnEvent(UUID walletId, BigDecimal amount, String correlationId) {

}
