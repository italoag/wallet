package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.event.WalletCreatedEvent;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Create Wallet Use Case Tests")
class CreateWalletUseCaseTest {

    @Test
    @DisplayName("createWallet saves wallet and publishes WalletCreatedEvent")
    void createWallet_success() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        CreateWalletUseCase useCase = new CreateWalletUseCase(walletRepository, eventPublisher);

        UUID userId = UUID.randomUUID();
        String correlationId = "corr-create";

        // Stub save to return the same wallet back (not strictly needed for current logic)
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = useCase.createWallet(userId, correlationId);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getId()).isNotNull();

        // Verify repository save called with created wallet
        verify(walletRepository, times(1)).save(result);

        // Capture and assert published event
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publish(eventCaptor.capture());
        Object published = eventCaptor.getValue();
        assertThat(published).isInstanceOf(WalletCreatedEvent.class);
        WalletCreatedEvent evt = (WalletCreatedEvent) published;
        assertThat(evt.walletId()).isEqualTo(result.getId());
        assertThat(evt.userId()).isEqualTo(userId);
        assertThat(evt.correlationId()).isEqualTo(correlationId);

        verifyNoMoreInteractions(walletRepository, eventPublisher);
    }
}
