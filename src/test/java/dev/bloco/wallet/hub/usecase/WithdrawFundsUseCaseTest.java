package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.wallet.FundsWithdrawnEvent;
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

@DisplayName("Withdraw Funds Use Case Tests")
class WithdrawFundsUseCaseTest {

    @Test
    @DisplayName("withdrawFunds updates wallet and publishes event")
    void withdrawFunds_success() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        WithdrawFundsUseCase useCase = new WithdrawFundsUseCase(walletRepository, eventPublisher);

        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");
        wallet.addFunds(new BigDecimal("100.00"));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        String corr = "corr-wd";
        BigDecimal amount = new BigDecimal("40.00");

        useCase.withdrawFunds(walletId, amount, corr);

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("60.00"));
        verify(walletRepository).findById(walletId);
        verify(walletRepository).update(wallet);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(FundsWithdrawnEvent.class);
        FundsWithdrawnEvent evt = (FundsWithdrawnEvent) eventCaptor.getValue();
        assertThat(evt.walletId()).isEqualTo(wallet.getId());
        assertThat(evt.amount()).isEqualByComparingTo(amount);
        assertThat(evt.correlationId()).isEqualTo(corr);

        verifyNoMoreInteractions(walletRepository, eventPublisher);
    }

    @Test
    @DisplayName("withdrawFunds throws when wallet not found and avoids side effects")
    void withdrawFunds_walletNotFound() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        WithdrawFundsUseCase useCase = new WithdrawFundsUseCase(walletRepository, eventPublisher);

        UUID walletId = UUID.randomUUID();
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        BigDecimal amount = new BigDecimal("1.00");
        assertThatThrownBy(() -> useCase.withdrawFunds(walletId, amount, "c"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wallet not found");

        verify(walletRepository, never()).update(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("withdrawFunds with insufficient balance propagates exception and avoids side effects")
    void withdrawFunds_insufficientBalance() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        WithdrawFundsUseCase useCase = new WithdrawFundsUseCase(walletRepository, eventPublisher);

        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(UUID.randomUUID(), "Test", "");
        wallet.addFunds(new BigDecimal("10.00"));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        BigDecimal amount = new BigDecimal("20.00");
        assertThatThrownBy(() -> useCase.withdrawFunds(walletId, amount, "c"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient");

        verify(walletRepository, never()).update(any());
        verify(eventPublisher, never()).publish(any());
    }
}
