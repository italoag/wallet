package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.wallet.FundsTransferredEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Transfer Funds Use Case Tests")
class TransferFundsUseCaseTest {

  @Test
  @DisplayName("transferFunds updates both wallets and publishes event")
  void transferFunds_success() {
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    TransferFundsUseCase useCase = new TransferFundsUseCase(walletRepository, eventPublisher);

    UUID fromId = UUID.randomUUID();
    UUID toId = UUID.randomUUID();
    Wallet from = new Wallet(UUID.randomUUID(), "From", "");
    Wallet to = new Wallet(UUID.randomUUID(), "To", "");
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

    ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher).publish(eventCaptor.capture());
    assertThat(eventCaptor.getValue()).isInstanceOf(FundsTransferredEvent.class);
    FundsTransferredEvent evt = (FundsTransferredEvent) eventCaptor.getValue();
    assertThat(evt.fromWalletId()).isEqualTo(from.getId());
    assertThat(evt.toWalletId()).isEqualTo(to.getId());
    assertThat(evt.amount()).isEqualByComparingTo(amount);
    assertThat(evt.correlationId()).isEqualTo(corr);

    verifyNoMoreInteractions(walletRepository, eventPublisher);
  }

  @Test
  @DisplayName("transferFunds throws when from wallet not found")
  void transferFunds_fromWalletNotFound() {
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    TransferFundsUseCase useCase = new TransferFundsUseCase(walletRepository, eventPublisher);

    UUID fromId = UUID.randomUUID();
    UUID toId = UUID.randomUUID();
    when(walletRepository.findById(fromId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.transferFunds(fromId, toId, new BigDecimal("1.00"), "c"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("From Wallet not found");

    verify(walletRepository, never()).update(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  @DisplayName("transferFunds throws when to wallet not found")
  void transferFunds_toWalletNotFound() {
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    TransferFundsUseCase useCase = new TransferFundsUseCase(walletRepository, eventPublisher);

    UUID fromId = UUID.randomUUID();
    UUID toId = UUID.randomUUID();
    when(walletRepository.findById(fromId)).thenReturn(Optional.of(new Wallet(UUID.randomUUID(), "From", "")));
    when(walletRepository.findById(toId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.transferFunds(fromId, toId, new BigDecimal("1.00"), "c"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("To Wallet not found");

    verify(walletRepository, never()).update(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  @DisplayName("transferFunds with insufficient funds propagates exception and avoids side effects")
  void transferFunds_insufficientFunds() {
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    TransferFundsUseCase useCase = new TransferFundsUseCase(walletRepository, eventPublisher);

    UUID fromId = UUID.randomUUID();
    UUID toId = UUID.randomUUID();
    Wallet from = new Wallet(UUID.randomUUID(), "From", "");
    Wallet to = new Wallet(UUID.randomUUID(), "To", "");
    from.addFunds(new BigDecimal("5.00"));
    when(walletRepository.findById(fromId)).thenReturn(Optional.of(from));
    when(walletRepository.findById(toId)).thenReturn(Optional.of(to));

    assertThatThrownBy(() -> useCase.transferFunds(fromId, toId, new BigDecimal("10.00"), "c"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Insufficient");

    verify(walletRepository, never()).update(any());
    verify(eventPublisher, never()).publish(any());

    // ensure destination wallet balance not modified
    assertThat(to.getBalance()).isEqualByComparingTo(new BigDecimal("0.00"));
  }

  @Test
  @DisplayName("transferFunds passes correct correlation ID to FundsTransferredEvent")
  void transferFunds_correctCorrelationId() {
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    TransferFundsUseCase useCase = new TransferFundsUseCase(walletRepository, eventPublisher);

    UUID fromId = UUID.randomUUID();
    UUID toId = UUID.randomUUID();
    Wallet from = new Wallet(fromId, "From", "");
    Wallet to = new Wallet(toId, "To", "");
    from.addFunds(new BigDecimal("100.00"));
    to.addFunds(new BigDecimal("50.00"));
    String correlationId = "test-correlation-id";

    when(walletRepository.findById(fromId)).thenReturn(Optional.of(from));
    when(walletRepository.findById(toId)).thenReturn(Optional.of(to));

    useCase.transferFunds(fromId, toId, new BigDecimal("25.00"), correlationId);

    ArgumentCaptor<FundsTransferredEvent> eventCaptor = ArgumentCaptor.forClass(FundsTransferredEvent.class);
    verify(eventPublisher).publish(eventCaptor.capture());
    FundsTransferredEvent publishedEvent = eventCaptor.getValue();
    assertThat(publishedEvent.correlationId()).isEqualTo(correlationId);
  }

  @Test
  @DisplayName("transferFunds throws on zero or negative amount")
  void transferFunds_zeroOrNegativeAmount() {
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    TransferFundsUseCase useCase = new TransferFundsUseCase(walletRepository, eventPublisher);

    UUID fromId = UUID.randomUUID();
    UUID toId = UUID.randomUUID();
    Wallet from = new Wallet(fromId, "From", "");
    Wallet to = new Wallet(toId, "To", "");
    from.addFunds(new BigDecimal("50.00"));

    when(walletRepository.findById(fromId)).thenReturn(Optional.of(from));
    when(walletRepository.findById(toId)).thenReturn(Optional.of(to));

    assertThatThrownBy(() -> useCase.transferFunds(fromId, toId, BigDecimal.ZERO, "test-correlation"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Transfer amount must be greater than zero");

    assertThatThrownBy(() -> useCase.transferFunds(fromId, toId, new BigDecimal("-10.00"), "test-correlation"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Transfer amount must be greater than zero");
  }

  @Test
  @DisplayName("transferFunds ensures no changes to wallets on failed operation")
  void transferFunds_noSideEffectsOnFailure() {
    WalletRepository walletRepository = mock(WalletRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    TransferFundsUseCase useCase = new TransferFundsUseCase(walletRepository, eventPublisher);

    UUID fromId = UUID.randomUUID();
    UUID toId = UUID.randomUUID();
    Wallet from = new Wallet(fromId, "From", "");
    Wallet to = new Wallet(toId, "To", "");
    from.addFunds(new BigDecimal("20.00"));
    to.addFunds(new BigDecimal("30.00"));

    when(walletRepository.findById(fromId)).thenReturn(Optional.of(from));
    when(walletRepository.findById(toId)).thenReturn(Optional.of(to));

    assertThatThrownBy(() -> useCase.transferFunds(fromId, toId, new BigDecimal("30.00"), "failure-corr"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Insufficient");

    // Assert wallet balances are unchanged
    assertThat(from.getBalance()).isEqualByComparingTo(new BigDecimal("20.00"));
    assertThat(to.getBalance()).isEqualByComparingTo(new BigDecimal("30.00"));

    verify(walletRepository, never()).update(any());
    verify(eventPublisher, never()).publish(any());
  }
}
