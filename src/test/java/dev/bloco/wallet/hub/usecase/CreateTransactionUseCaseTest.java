package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.transaction.TransactionCreatedEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.model.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Create Transaction Use Case Tests")
class CreateTransactionUseCaseTest {

  /**
   * Tests the createTransaction method of CreateTransactionUseCase.
   * Verifies interactions with TransactionRepository and DomainEventPublisher
   * as well as the creation and correct behavior of the Transaction object.
   */
  @Test
  @DisplayName("createTransaction creates and publishes TransactionCreatedEvent")
  void shouldCreateTransactionSuccessfully() {
    // Arrange
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateTransactionUseCase createTransactionUseCase = new CreateTransactionUseCase(transactionRepository, eventPublisher);

    UUID networkId = UUID.randomUUID();
    String hash = "fakeHash12345";
    String fromAddress = "fromAddress";
    String toAddress = "toAddress";
    BigDecimal value = BigDecimal.valueOf(1000);
    String data = "some-data";
    String correlationId = null;

    Transaction transactionMock = mock(Transaction.class);
    when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionMock);

    // Act
    Transaction result = createTransactionUseCase.createTransaction(
        networkId, hash, fromAddress, toAddress, value, data, correlationId
    );

    // Assert
    assertNotNull(result);
    verify(transactionRepository, times(1)).save(any(Transaction.class));
    verify(eventPublisher, times(1)).publish(any(TransactionCreatedEvent.class));
  }

  @Test
  @DisplayName("createTransaction publishes TransactionCreatedEvent with correlationId")
  void shouldPublishEventWithCorrelationId() {
    // Arrange
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateTransactionUseCase createTransactionUseCase = new CreateTransactionUseCase(transactionRepository, eventPublisher);

    UUID networkId = UUID.randomUUID();
    String hash = "anotherHash123";
    String fromAddress = "senderAddress";
    String toAddress = "receiverAddress";
    BigDecimal value = BigDecimal.valueOf(2000);
    String data = "sample-data";
    String correlationId = UUID.randomUUID().toString();

    Transaction transactionMock = mock(Transaction.class);
    when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionMock);

    // Act
    createTransactionUseCase.createTransaction(networkId, hash, fromAddress, toAddress, value, data, correlationId);

    // Assert
    ArgumentCaptor<TransactionCreatedEvent> captor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
    verify(eventPublisher, times(1)).publish(captor.capture());
    TransactionCreatedEvent event = captor.getValue();

    assertNotNull(event);
    assertEquals(hash, event.getTransactionHash());
    assertEquals(fromAddress, event.getFromAddress());
    assertEquals(toAddress, event.getToAddress());
    assertEquals(UUID.fromString(correlationId), event.getCorrelationId());
  }

  @Test
  @DisplayName("createTransaction publishes TransactionCreatedEvent with null correlationId")
  void shouldHandleNullCorrelationIdGracefully() {
    // Arrange
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateTransactionUseCase createTransactionUseCase = new CreateTransactionUseCase(transactionRepository, eventPublisher);

    UUID networkId = UUID.randomUUID();
    String hash = "testHash";
    String fromAddress = "from";
    String toAddress = "to";
    BigDecimal value = BigDecimal.valueOf(500);
    String data = "test-data";
    String correlationId = null;

    Transaction transactionMock = mock(Transaction.class);
    when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionMock);

    // Act
    createTransactionUseCase.createTransaction(networkId, hash, fromAddress, toAddress, value, data, correlationId);

    // Assert
    ArgumentCaptor<TransactionCreatedEvent> captor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
    verify(eventPublisher, times(1)).publish(captor.capture());
    TransactionCreatedEvent event = captor.getValue();

    assertNotNull(event);
    assertEquals(hash, event.getTransactionHash());
    assertNull(event.getCorrelationId());
  }

  @Test
  @DisplayName("createTransaction persists Transaction object correctly")
  void shouldPersistTransactionCorrectly() {
    // Arrange
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateTransactionUseCase createTransactionUseCase = new CreateTransactionUseCase(transactionRepository, eventPublisher);

    UUID networkId = UUID.randomUUID();
    String hash = "persistHash";
    String fromAddress = "fromAddr";
    String toAddress = "toAddr";
    BigDecimal value = BigDecimal.valueOf(750);
    String data = "test-persist-data";
    String correlationId = UUID.randomUUID().toString();

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

    // Act
    createTransactionUseCase.createTransaction(networkId, hash, fromAddress, toAddress, value, data, correlationId);

    // Assert
    verify(transactionRepository, times(1)).save(captor.capture());
    Transaction savedTransaction = captor.getValue();

    assertNotNull(savedTransaction);
    assertEquals(networkId, savedTransaction.getNetworkId());
    assertEquals(hash, savedTransaction.getHash());
    assertEquals(fromAddress, savedTransaction.getFromAddress());
    assertEquals(toAddress, savedTransaction.getToAddress());
    assertEquals(value, savedTransaction.getValue());
  }
}