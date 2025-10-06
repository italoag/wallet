package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.transaction.TransactionStatusChangedEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.model.transaction.Transaction;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Fail Transaction Use Case Tests")
class FailTransactionUseCaseTest {

  /**
   * Tests successful failure of a transaction.
   * The transaction status is updated, saved, and an event is published.
   */
  @Test
  @DisplayName("failTransaction updates transaction status and publishes event")
  void shouldFailTransactionSuccessfully() {
    // Arrange
    TransactionRepository mockTransactionRepository = Mockito.mock(TransactionRepository.class);
    DomainEventPublisher mockEventPublisher = Mockito.mock(DomainEventPublisher.class);
    FailTransactionUseCase useCase = new FailTransactionUseCase(mockTransactionRepository, mockEventPublisher);

    UUID transactionId = UUID.randomUUID();
    String reason = "Failed due to insufficient funds";
    String correlationId = UUID.randomUUID().toString();
    Transaction mockTransaction = mock(Transaction.class);

    when(mockTransactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTransaction));
    when(mockTransaction.getStatus()).thenReturn(TransactionStatus.PENDING, TransactionStatus.FAILED);

    // Act
    useCase.fail(transactionId, reason, correlationId);

    // Assert
    verify(mockTransaction).fail(reason);
    verify(mockTransactionRepository).save(mockTransaction);
    
    ArgumentCaptor<TransactionStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionStatusChangedEvent.class);
    verify(mockEventPublisher).publish(eventCaptor.capture());
    
    TransactionStatusChangedEvent capturedEvent = eventCaptor.getValue();
    assertEquals(transactionId, capturedEvent.getTransactionId());
    assertEquals(TransactionStatus.PENDING, capturedEvent.getOldStatus());
    assertEquals(TransactionStatus.FAILED, capturedEvent.getNewStatus());
    assertEquals(reason, capturedEvent.getReason());
    assertEquals(UUID.fromString(correlationId), capturedEvent.getCorrelationId());
  }

  /**
   * Ensures that an exception is thrown when failing a non-existent transaction.
   */
  @Test
  @DisplayName("failTransaction throws exception when transaction is not found")
  void shouldThrowExceptionWhenTransactionNotFound() {
    // Arrange
    TransactionRepository mockTransactionRepository = Mockito.mock(TransactionRepository.class);
    DomainEventPublisher mockEventPublisher = Mockito.mock(DomainEventPublisher.class);
    FailTransactionUseCase useCase = new FailTransactionUseCase(mockTransactionRepository, mockEventPublisher);

    UUID transactionId = UUID.randomUUID();
    String reason = "Transaction not found";

    when(mockTransactionRepository.findById(transactionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.fail(transactionId, reason, null));
    verify(mockTransactionRepository).findById(transactionId);
    verifyNoInteractions(mockEventPublisher);
  }

  /**
   * Verifies that the fail method handles null correlation ID correctly and does not include it in the event.
   */
  @Test
  @DisplayName("failTransaction handles null correlation ID correctly")
  void shouldHandleNullCorrelationId() {
    // Arrange
    TransactionRepository mockTransactionRepository = Mockito.mock(TransactionRepository.class);
    DomainEventPublisher mockEventPublisher = Mockito.mock(DomainEventPublisher.class);
    FailTransactionUseCase useCase = new FailTransactionUseCase(mockTransactionRepository, mockEventPublisher);

    UUID transactionId = UUID.randomUUID();
    String reason = "Timeout occurred";
    Transaction mockTransaction = mock(Transaction.class);

    when(mockTransactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTransaction));
    when(mockTransaction.getStatus()).thenReturn(TransactionStatus.PENDING, TransactionStatus.FAILED);

    // Act
    useCase.fail(transactionId, reason, null);

    // Assert
    verify(mockTransaction).fail(reason);
    verify(mockTransactionRepository).save(mockTransaction);
    
    ArgumentCaptor<TransactionStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionStatusChangedEvent.class);
    verify(mockEventPublisher).publish(eventCaptor.capture());
    
    TransactionStatusChangedEvent capturedEvent = eventCaptor.getValue();
    assertEquals(transactionId, capturedEvent.getTransactionId());
    assertEquals(TransactionStatus.PENDING, capturedEvent.getOldStatus());
    assertEquals(TransactionStatus.FAILED, capturedEvent.getNewStatus());
    assertEquals(reason, capturedEvent.getReason());
    assertNull(capturedEvent.getCorrelationId());
  }
}