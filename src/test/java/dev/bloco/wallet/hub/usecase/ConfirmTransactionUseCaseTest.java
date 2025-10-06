package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.transaction.TransactionConfirmedEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.model.transaction.Transaction;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionHash;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Confirm Transaction Use Case Tests")
class ConfirmTransactionUseCaseTest {

  @Test
  @DisplayName("confirmTransaction publishes TransactionConfirmedEvent")
  void shouldConfirmTransactionAndPublishEvent() {
    // Arrange
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    ConfirmTransactionUseCase confirmTransactionUseCase = new ConfirmTransactionUseCase(transactionRepository, eventPublisher);

    UUID transactionId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    Transaction transaction = Transaction.rehydrate(
        transactionId,
        networkId,
        new TransactionHash("txHash"),
        "fromAddress",
        "toAddress",
        BigDecimal.TEN,
        "data",
        Instant.now(),
        null,
        null,
        TransactionStatus.PENDING,
        null,
        null,
        null
    );

    when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

    long blockNumber = 123456L;
    String blockHash = "blockHash";
    BigDecimal gasUsed = BigDecimal.valueOf(30000);
    String correlationId = UUID.randomUUID().toString();

    // Act
    confirmTransactionUseCase.confirm(transactionId, blockNumber, blockHash, gasUsed, correlationId);

    // Assert
    assertEquals(TransactionStatus.CONFIRMED, transaction.getStatus());
    assertEquals(blockNumber, transaction.getBlockNumber());
    assertEquals(blockHash, transaction.getBlockHash());
    assertEquals(gasUsed, transaction.getGasUsed());
    verify(transactionRepository).save(transaction);

    ArgumentCaptor<TransactionConfirmedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionConfirmedEvent.class);
    verify(eventPublisher).publish(eventCaptor.capture());
    TransactionConfirmedEvent capturedEvent = eventCaptor.getValue();
    assertEquals(transactionId, capturedEvent.getTransactionId());
    assertEquals(blockNumber, capturedEvent.getBlockNumber());
    assertEquals(blockHash, capturedEvent.getBlockHash());
    assertEquals(gasUsed, capturedEvent.getGasUsed());
    assertEquals(UUID.fromString(correlationId), capturedEvent.getCorrelationId());
  }

  @Test
  @DisplayName("confirmTransaction throws IllegalArgumentException when transaction not found")
  void shouldThrowExceptionWhenTransactionNotFound() {
    // Arrange
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    ConfirmTransactionUseCase confirmTransactionUseCase = new ConfirmTransactionUseCase(transactionRepository, eventPublisher);

    UUID transactionId = UUID.randomUUID();
    when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

    long blockNumber = 123456L;
    String blockHash = "blockHash";
    BigDecimal gasUsed = BigDecimal.valueOf(30000);
    String correlationId = null;

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        confirmTransactionUseCase.confirm(transactionId, blockNumber, blockHash, gasUsed, correlationId)
    );
    assertEquals("Transaction not found", exception.getMessage());
    verify(transactionRepository, never()).save(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  @DisplayName("confirmTransaction handles null correlationId")
  void shouldHandleNullCorrelationIdWhenPublishingEvent() {
    // Arrange
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    ConfirmTransactionUseCase confirmTransactionUseCase = new ConfirmTransactionUseCase(transactionRepository, eventPublisher);

    UUID transactionId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    Transaction transaction = Transaction.rehydrate(
        transactionId,
        networkId,
        new TransactionHash("txHash"),
        "fromAddress",
        "toAddress",
        BigDecimal.TEN,
        "data",
        Instant.now(),
        null,
        null,
        TransactionStatus.PENDING,
        null,
        null,
        null
    );

    when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

    long blockNumber = 654321L;
    String blockHash = "blockHash";
    BigDecimal gasUsed = BigDecimal.valueOf(45000);
    String correlationId = null;

    // Act
    confirmTransactionUseCase.confirm(transactionId, blockNumber, blockHash, gasUsed, correlationId);

    // Assert
    assertEquals(TransactionStatus.CONFIRMED, transaction.getStatus());
    assertEquals(blockNumber, transaction.getBlockNumber());
    assertEquals(blockHash, transaction.getBlockHash());
    assertEquals(gasUsed, transaction.getGasUsed());
    verify(transactionRepository).save(transaction);

    ArgumentCaptor<TransactionConfirmedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionConfirmedEvent.class);
    verify(eventPublisher).publish(eventCaptor.capture());
    TransactionConfirmedEvent capturedEvent = eventCaptor.getValue();
    assertEquals(transactionId, capturedEvent.getTransactionId());
    assertEquals(blockNumber, capturedEvent.getBlockNumber());
    assertEquals(blockHash, capturedEvent.getBlockHash());
    assertEquals(gasUsed, capturedEvent.getGasUsed());
    assertNull(capturedEvent.getCorrelationId());
  }
}