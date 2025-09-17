package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.Transaction;
import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.event.FundsTransferredEvent;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Transfer Funds Use Case Tests")
class TransferFundsUseCaseTest {

    @Test
    @DisplayName("transferFunds updates both wallets, saves transaction, and publishes event")
    void transferFunds_success() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        TransferFundsUseCase useCase = new TransferFundsUseCase(walletRepository, transactionRepository, eventPublisher);

        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        Wallet from = new Wallet(UUID.randomUUID());
        Wallet to = new Wallet(UUID.randomUUID());
        from.addFunds(new BigDecimal("50.00"));
        to.addFunds(new BigDecimal("5.00"));

        when(walletRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(walletRepository.findById(toId)).thenReturn(Optional.of(to));

        BigDecimal amount = new BigDecimal("20.00");
        String corr = "corr-xfer";

        useCase.transferFunds(fromId, toId, amount, corr);

        assertThat(from.getBalance()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(to.getBalance()).isEqualByComparingTo(new BigDecimal("25.00"));

        verify(walletRepository).findById(fromId);
        verify(walletRepository).findById(toId);
        verify(walletRepository).update(from);
        verify(walletRepository).update(to);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction tx = txCaptor.getValue();
        assertThat(tx.getType()).isEqualTo(Transaction.TransactionType.TRANSFER);
        assertThat(tx.getFromWalletId()).isEqualTo(from.getId());
        assertThat(tx.getToWalletId()).isEqualTo(to.getId());
        assertThat(tx.getAmount()).isEqualByComparingTo(amount);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(FundsTransferredEvent.class);
        FundsTransferredEvent evt = (FundsTransferredEvent) eventCaptor.getValue();
        assertThat(evt.fromWalletId()).isEqualTo(from.getId());
        assertThat(evt.toWalletId()).isEqualTo(to.getId());
        assertThat(evt.amount()).isEqualByComparingTo(amount);
        assertThat(evt.correlationId()).isEqualTo(corr);

        verifyNoMoreInteractions(walletRepository, transactionRepository, eventPublisher);
    }

    @Test
    @DisplayName("transferFunds throws when from wallet not found")
    void transferFunds_fromWalletNotFound() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        TransferFundsUseCase useCase = new TransferFundsUseCase(walletRepository, transactionRepository, eventPublisher);

        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        when(walletRepository.findById(fromId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.transferFunds(fromId, toId, new BigDecimal("1.00"), "c"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("From Wallet not found");

        verify(walletRepository, never()).update(any());
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("transferFunds throws when to wallet not found")
    void transferFunds_toWalletNotFound() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        TransferFundsUseCase useCase = new TransferFundsUseCase(walletRepository, transactionRepository, eventPublisher);

        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        when(walletRepository.findById(fromId)).thenReturn(Optional.of(new Wallet(UUID.randomUUID())));
        when(walletRepository.findById(toId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.transferFunds(fromId, toId, new BigDecimal("1.00"), "c"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("To Wallet not found");

        verify(walletRepository, never()).update(any());
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("transferFunds with insufficient funds propagates exception and avoids side effects")
    void transferFunds_insufficientFunds() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        TransferFundsUseCase useCase = new TransferFundsUseCase(walletRepository, transactionRepository, eventPublisher);

        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        Wallet from = new Wallet(UUID.randomUUID());
        Wallet to = new Wallet(UUID.randomUUID());
        from.addFunds(new BigDecimal("5.00"));
        when(walletRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(walletRepository.findById(toId)).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> useCase.transferFunds(fromId, toId, new BigDecimal("10.00"), "c"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");

        verify(walletRepository, never()).update(any());
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());

        // ensure destination wallet balance not modified
        assertThat(to.getBalance()).isEqualByComparingTo(new BigDecimal("0.00"));
    }
}
