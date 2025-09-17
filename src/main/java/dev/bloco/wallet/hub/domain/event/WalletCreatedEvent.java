package dev.bloco.wallet.hub.domain.event;

import lombok.Builder;

import java.util.UUID;

@Builder
public record WalletCreatedEvent(UUID walletId, UUID userId, String correlationId) {

}
