package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.wallet.FundsAddedEvent;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Add Funds Use Case Tests")
class AddFundsUseCaseTest {

    @Test
    @DisplayName("addFunds updates wallet and publishes event")
    void addFunds_success() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        AddFundsUseCase useCase = new AddFundsUseCase(walletRepository, transactionRepository, eventPublisher);

        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");
        // Preload some balance to ensure arithmetic
        wallet.addFunds(new BigDecimal("10.00"));

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        String corr = "corr-add";
        BigDecimal amount = new BigDecimal("5.50");

        useCase.addFunds(walletId, amount, corr);

        // Wallet balance should be updated in-memory
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("15.50"));

        // Verify lookup and persistence
        verify(walletRepository).findById(walletId);
        verify(walletRepository).update(wallet);

        // Verify event published
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(FundsAddedEvent.class);
        FundsAddedEvent evt = (FundsAddedEvent) eventCaptor.getValue();
        assertThat(evt.walletId()).isEqualTo(wallet.getId());
        assertThat(evt.amount()).isEqualByComparingTo(amount);
        assertThat(evt.correlationId()).isEqualTo(corr);

        verifyNoMoreInteractions(walletRepository, transactionRepository, eventPublisher);
    }

    @Test
    @DisplayName("addFunds throws when wallet not found and does nothing else")
    void addFunds_walletNotFound() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        AddFundsUseCase useCase = new AddFundsUseCase(walletRepository, transactionRepository, eventPublisher);

        UUID walletId = UUID.randomUUID();
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.addFunds(walletId, new BigDecimal("1.00"), "c"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wallet not found");

        verify(walletRepository, never()).update(any());
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("addFunds with invalid amount propagates exception and avoids side effects")
    void addFunds_invalidAmount() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        AddFundsUseCase useCase = new AddFundsUseCase(walletRepository, transactionRepository, eventPublisher);

        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> useCase.addFunds(walletId, new BigDecimal("-1"), "c"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        verify(walletRepository, never()).update(any());
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}
