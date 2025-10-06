package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.network.NetworkCreatedEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.model.network.Network;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("Add Network Use Case Tests")
class AddNetworkUseCaseTest {

  @Test
  @DisplayName("addNetwork adds network and publishes event")
  void testAddNetwork_success() {
    NetworkRepository mockRepository = mock(NetworkRepository.class);
    DomainEventPublisher mockPublisher = mock(DomainEventPublisher.class);
    AddNetworkUseCase useCase = new AddNetworkUseCase(mockRepository, mockPublisher);

    String name = "Test Network";
    String chainId = "123";
    String rpcUrl = "http://test-rpc.com";
    String explorerUrl = "http://test-explorer.com";
    String correlationId = UUID.randomUUID().toString();

    when(mockRepository.existsByChainId(chainId, correlationId)).thenReturn(false);

    Network result = useCase.addNetwork(name, chainId, rpcUrl, explorerUrl, correlationId);

    assertEquals(name, result.getName());
    assertEquals(chainId, result.getChainId());
    assertEquals(rpcUrl, result.getRpcUrl());
    assertEquals(explorerUrl, result.getExplorerUrl());
    verify(mockRepository, times(1)).save(any(Network.class), eq(correlationId));
    verify(mockPublisher, times(1)).publish(any(NetworkCreatedEvent.class));
  }

  @Test
  @DisplayName("addNetwork throws exception when network already exists")
  void testAddNetwork_missingName() {
    NetworkRepository mockRepository = mock(NetworkRepository.class);
    DomainEventPublisher mockPublisher = mock(DomainEventPublisher.class);
    AddNetworkUseCase useCase = new AddNetworkUseCase(mockRepository, mockPublisher);

    String chainId = "123";
    String rpcUrl = "http://test-rpc.com";
    String explorerUrl = "http://test-explorer.com";
    String correlationId = UUID.randomUUID().toString();

    Exception exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.addNetwork(null, chainId, rpcUrl, explorerUrl, correlationId)
    );

    assertEquals("Network name must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("addNetwork throws exception when network already exists")
  void testAddNetwork_duplicateChainId() {
    NetworkRepository mockRepository = mock(NetworkRepository.class);
    DomainEventPublisher mockPublisher = mock(DomainEventPublisher.class);
    AddNetworkUseCase useCase = new AddNetworkUseCase(mockRepository, mockPublisher);

    String name = "Test Network";
    String chainId = "123";
    String rpcUrl = "http://test-rpc.com";
    String explorerUrl = "http://test-explorer.com";
    String correlationId = UUID.randomUUID().toString();

    when(mockRepository.existsByChainId(chainId, correlationId)).thenReturn(true);

    Exception exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.addNetwork(name, chainId, rpcUrl, explorerUrl, correlationId)
    );

    assertEquals("A network with the provided chain ID already exists", exception.getMessage());
  }

  @Test
  @DisplayName("addNetwork throws exception when network already exists")
  void testAddNetwork_invalidRpcUrl() {
    NetworkRepository mockRepository = mock(NetworkRepository.class);
    DomainEventPublisher mockPublisher = mock(DomainEventPublisher.class);
    AddNetworkUseCase useCase = new AddNetworkUseCase(mockRepository, mockPublisher);

    String name = "Test Network";
    String chainId = "123";
    String rpcUrl = "ftp://invalid-rpc.com";
    String explorerUrl = "http://test-explorer.com";
    String correlationId = UUID.randomUUID().toString();

    Exception exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.addNetwork(name, chainId, rpcUrl, explorerUrl, correlationId)
    );

    assertEquals("RPC URL must be a valid HTTP/HTTPS URL", exception.getMessage());
  }

  @Test
  @DisplayName("addNetwork throws exception when network already exists")
  void testAddNetwork_invalidExplorerUrl() {
    NetworkRepository mockRepository = mock(NetworkRepository.class);
    DomainEventPublisher mockPublisher = mock(DomainEventPublisher.class);
    AddNetworkUseCase useCase = new AddNetworkUseCase(mockRepository, mockPublisher);

    String name = "Test Network";
    String chainId = "123";
    String rpcUrl = "http://test-rpc.com";
    String explorerUrl = "invalid-url";
    String correlationId = UUID.randomUUID().toString();

    Exception exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.addNetwork(name, chainId, rpcUrl, explorerUrl, correlationId)
    );

    assertEquals("Explorer URL must be a valid HTTP/HTTPS URL", exception.getMessage());
  }

  @Test
  @DisplayName("addNetwork throws exception when network already exists")
  void testAddNetwork_invalidCorrelationId() {
    NetworkRepository mockRepository = mock(NetworkRepository.class);
    DomainEventPublisher mockPublisher = mock(DomainEventPublisher.class);
    AddNetworkUseCase useCase = new AddNetworkUseCase(mockRepository, mockPublisher);

    String name = "Test Network";
    String chainId = "123";
    String rpcUrl = "http://test-rpc.com";
    String explorerUrl = "http://test-explorer.com";
    String correlationId = "invalid-uuid";

    Exception exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.addNetwork(name, chainId, rpcUrl, explorerUrl, correlationId)
    );

    assertEquals("Correlation ID must be a valid UUID", exception.getMessage());
  }
}