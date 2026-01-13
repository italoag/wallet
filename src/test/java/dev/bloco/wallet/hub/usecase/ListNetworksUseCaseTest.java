package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.model.network.Network;
import dev.bloco.wallet.hub.domain.model.network.NetworkStatus;
import dev.bloco.wallet.hub.usecase.ListNetworksUseCase.NetworkHealthInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("List Networks Use Case Tests")
class ListNetworksUseCaseTest {

  /**
   * Tests for ListNetworksUseCase#listActiveNetworks method.
   * The method returns a list of active (available) networks by using a network repository.
   * It accepts a correlation ID which is validated and used to fetch networks.
   */

  @Test
  @DisplayName("listActiveNetworks returns only active networks")
  void shouldReturnOnlyActiveNetworks() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    Network activeNetwork1 = mockNetwork(UUID.randomUUID(), "Network 1", NetworkStatus.ACTIVE);
    Network activeNetwork2 = mockNetwork(UUID.randomUUID(), "Network 2", NetworkStatus.ACTIVE);
    Network inactiveNetwork = mockNetwork(UUID.randomUUID(), "Network 3", NetworkStatus.INACTIVE);

    when(networkRepository.findAll(correlationId)).thenReturn(List.of(activeNetwork1, activeNetwork2, inactiveNetwork));

    // Act
    List<Network> result = listNetworksUseCase.listActiveNetworks(correlationId);

    // Assert
    assertEquals(2, result.size());
    assertTrue(result.contains(activeNetwork1));
    assertTrue(result.contains(activeNetwork2));
    assertFalse(result.contains(inactiveNetwork));

    verify(networkRepository, times(1)).findAll(correlationId);
  }

  @Test
  @DisplayName("listActiveNetworks throws exception when correlationId is invalid")
  void shouldThrowExceptionWhenCorrelationIdIsInvalid() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String invalidCorrelationId = "invalid-uuid";

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> listNetworksUseCase.listActiveNetworks(invalidCorrelationId));

    assertEquals("Correlation ID must be a valid UUID", exception.getMessage());
    verify(networkRepository, never()).findAll(any());
  }

  @Test
  @DisplayName("listActiveNetworks throws exception when correlationId is null")
  void shouldThrowExceptionWhenCorrelationIdIsNull() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> listNetworksUseCase.listActiveNetworks(null));

    assertEquals("Correlation ID must be provided", exception.getMessage());
    verify(networkRepository, never()).findAll(any());
  }

  @Test
  @DisplayName("listActiveNetworks returns an empty list when no networks exist")
  void shouldReturnEmptyListWhenNoNetworksAreActive() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    Network inactiveNetwork1 = mockNetwork(UUID.randomUUID(), "Network 1", NetworkStatus.INACTIVE);
    Network inactiveNetwork2 = mockNetwork(UUID.randomUUID(), "Network 2", NetworkStatus.MAINTENANCE);

    when(networkRepository.findAll(correlationId)).thenReturn(List.of(inactiveNetwork1, inactiveNetwork2));

    // Act
    List<Network> result = listNetworksUseCase.listActiveNetworks(correlationId);

    // Assert
    assertTrue(result.isEmpty());
    verify(networkRepository, times(1)).findAll(correlationId);
  }

  @Test
  @DisplayName("listActiveNetworks normalizes and uses correlationId properly")
  void shouldNormalizeAndUseCorrelationIdProperly() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String rawCorrelationId = " " + UUID.randomUUID() + " ";

    Network activeNetwork = mockNetwork(UUID.randomUUID(), "Network 1", NetworkStatus.ACTIVE);

    when(networkRepository.findAll(rawCorrelationId.trim())).thenReturn(List.of(activeNetwork));

    // Act
    List<Network> result = listNetworksUseCase.listActiveNetworks(rawCorrelationId);

    // Assert
    assertEquals(1, result.size());
    assertTrue(result.contains(activeNetwork));
    verify(networkRepository, times(1)).findAll(rawCorrelationId.trim());
  }

  private Network mockNetwork(UUID id, String name, NetworkStatus status) {
    Network network = mock(Network.class);
    when(network.getId()).thenReturn(id);
    when(network.getName()).thenReturn(name);
    when(network.getStatus()).thenReturn(status);
    when(network.isAvailable()).thenReturn(status == NetworkStatus.ACTIVE);
    return network;
  }

  @Test
  @DisplayName("listActiveNetworks does not modify network objects returned")
  void shouldNotModifyNetworkObjectsReturned() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    Network activeNetwork = mockNetwork(UUID.randomUUID(), "Network Active", NetworkStatus.ACTIVE);
    when(networkRepository.findAll(correlationId)).thenReturn(List.of(activeNetwork));
    String originalName = activeNetwork.getName();

    // Act
    listNetworksUseCase.listActiveNetworks(correlationId);

    // Assert
    assertEquals(originalName, activeNetwork.getName());
    verify(networkRepository, times(1)).findAll(correlationId);
  }

  @Test
  @DisplayName("listActiveNetworks calls isAvailable() for each network")
  void shouldCallIsAvailableForEachNetwork() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    Network network1 = mock(Network.class);
    Network network2 = mock(Network.class);
    when(networkRepository.findAll(correlationId)).thenReturn(List.of(network1, network2));
    when(network1.isAvailable()).thenReturn(true);
    when(network2.isAvailable()).thenReturn(false);

    // Act
    listNetworksUseCase.listActiveNetworks(correlationId);

    // Assert
    verify(network1, times(1)).isAvailable();
    verify(network2, times(1)).isAvailable();
    verify(networkRepository, times(1)).findAll(correlationId);
  }

  /**
   * Tests for ListNetworksUseCase#listAllNetworks method.
   * The method returns all networks regardless of their status.
   */

  @Test
  @DisplayName("listAllNetworks returns all networks regardless of status")
  void shouldReturnAllNetworksRegardlessOfStatus() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    Network activeNetwork = mockNetwork(UUID.randomUUID(), "Network 1", NetworkStatus.ACTIVE);
    Network inactiveNetwork = mockNetwork(UUID.randomUUID(), "Network 2", NetworkStatus.INACTIVE);
    Network maintenanceNetwork = mockNetwork(UUID.randomUUID(), "Network 3", NetworkStatus.MAINTENANCE);

    when(networkRepository.findAll(correlationId)).thenReturn(List.of(activeNetwork, inactiveNetwork, maintenanceNetwork));

    // Act
    List<Network> result = listNetworksUseCase.listAllNetworks(correlationId);

    // Assert
    assertEquals(3, result.size());
    assertTrue(result.contains(activeNetwork));
    assertTrue(result.contains(inactiveNetwork));
    assertTrue(result.contains(maintenanceNetwork));
    verify(networkRepository, times(1)).findAll(correlationId);
  }

  @Test
  @DisplayName("listAllNetworks throws exception when correlationId is null")
  void listAllNetworks_shouldThrowExceptionWhenCorrelationIdIsNull() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> listNetworksUseCase.listAllNetworks(null));

    assertEquals("Correlation ID must be provided", exception.getMessage());
    verify(networkRepository, never()).findAll(any());
  }

  @Test
  @DisplayName("listAllNetworks throws exception when correlationId is invalid")
  void listAllNetworks_shouldThrowExceptionWhenCorrelationIdIsInvalid() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String invalidCorrelationId = "invalid-uuid";

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> listNetworksUseCase.listAllNetworks(invalidCorrelationId));

    assertEquals("Correlation ID must be a valid UUID", exception.getMessage());
    verify(networkRepository, never()).findAll(any());
  }

  @Test
  @DisplayName("listAllNetworks returns empty list when no networks exist")
  void listAllNetworks_shouldReturnEmptyListWhenNoNetworksExist() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    when(networkRepository.findAll(correlationId)).thenReturn(List.of());

    // Act
    List<Network> result = listNetworksUseCase.listAllNetworks(correlationId);

    // Assert
    assertTrue(result.isEmpty());
    verify(networkRepository, times(1)).findAll(correlationId);
  }

  /**
   * Tests for ListNetworksUseCase#getNetworkDetails method.
   * The method retrieves a specific network by its ID.
   */

  @Test
  @DisplayName("getNetworkDetails returns network when found")
  void shouldReturnNetworkDetailsWhenNetworkExists() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    Network network = mockNetwork(networkId, "Ethereum", NetworkStatus.ACTIVE);
    when(networkRepository.findById(networkId, correlationId)).thenReturn(Optional.of(network));

    // Act
    Network result = listNetworksUseCase.getNetworkDetails(networkId, correlationId);

    // Assert
    assertNotNull(result);
    assertEquals(networkId, result.getId());
    assertEquals("Ethereum", result.getName());
    verify(networkRepository, times(1)).findById(networkId, correlationId);
  }

  @Test
  @DisplayName("getNetworkDetails throws exception when network not found")
  void shouldThrowExceptionWhenNetworkNotFound() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    when(networkRepository.findById(networkId, correlationId)).thenReturn(Optional.empty());

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> listNetworksUseCase.getNetworkDetails(networkId, correlationId));

    assertTrue(exception.getMessage().contains("Network not found with id: " + networkId));
    verify(networkRepository, times(1)).findById(networkId, correlationId);
  }

  @Test
  @DisplayName("getNetworkDetails throws exception when networkId is null")
  void shouldThrowExceptionWhenNetworkIdIsNull() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> listNetworksUseCase.getNetworkDetails(null, correlationId));

    assertEquals("Network ID must be provided", exception.getMessage());
    verify(networkRepository, never()).findById(any(), any());
  }

  @Test
  @DisplayName("getNetworkDetails throws exception when correlationId is null")
  void getNetworkDetails_shouldThrowExceptionWhenCorrelationIdIsNull() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    UUID networkId = UUID.randomUUID();

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> listNetworksUseCase.getNetworkDetails(networkId, null));

    assertEquals("Correlation ID must be provided", exception.getMessage());
    verify(networkRepository, never()).findById(any(), any());
  }

  @Test
  @DisplayName("getNetworkDetails throws exception when correlationId is invalid")
  void getNetworkDetails_shouldThrowExceptionWhenCorrelationIdIsInvalid() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    UUID networkId = UUID.randomUUID();
    String invalidCorrelationId = "invalid-uuid";

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> listNetworksUseCase.getNetworkDetails(networkId, invalidCorrelationId));

    assertEquals("Correlation ID must be a valid UUID", exception.getMessage());
    verify(networkRepository, never()).findById(any(), any());
  }

  /**
   * Tests for ListNetworksUseCase#searchNetworksByName method.
   * The method searches networks by name pattern (case-insensitive).
   */

  @Test
  @DisplayName("searchNetworksByName returns matching networks")
  void shouldReturnMatchingNetworksByName() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    Network ethereum = mockNetwork(UUID.randomUUID(), "Ethereum", NetworkStatus.ACTIVE);
    Network polygon = mockNetwork(UUID.randomUUID(), "Polygon", NetworkStatus.ACTIVE);
    Network bitcoin = mockNetwork(UUID.randomUUID(), "Bitcoin", NetworkStatus.ACTIVE);

    when(networkRepository.findAll(correlationId)).thenReturn(List.of(ethereum, polygon, bitcoin));

    // Act
    List<Network> result = listNetworksUseCase.searchNetworksByName("eth", correlationId);

    // Assert
    assertEquals(1, result.size());
    assertTrue(result.contains(ethereum));
    assertFalse(result.contains(polygon));
    assertFalse(result.contains(bitcoin));
    verify(networkRepository, times(1)).findAll(correlationId);
  }

  @Test
  @DisplayName("searchNetworksByName is case-insensitive")
  void shouldSearchNetworksCaseInsensitively() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    Network ethereum = mockNetwork(UUID.randomUUID(), "Ethereum", NetworkStatus.ACTIVE);
    Network polygon = mockNetwork(UUID.randomUUID(), "Polygon", NetworkStatus.ACTIVE);

    when(networkRepository.findAll(correlationId)).thenReturn(List.of(ethereum, polygon));

    // Act
    List<Network> result = listNetworksUseCase.searchNetworksByName("ETHEREUM", correlationId);

    // Assert
    assertEquals(1, result.size());
    assertTrue(result.contains(ethereum));
    verify(networkRepository, times(1)).findAll(correlationId);
  }

  @Test
  @DisplayName("searchNetworksByName returns empty list when pattern is null")
  void shouldReturnEmptyListWhenPatternIsNull() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    // Act
    List<Network> result = listNetworksUseCase.searchNetworksByName(null, correlationId);

    // Assert
    assertTrue(result.isEmpty());
    verify(networkRepository, never()).findAll(any());
  }

  @Test
  @DisplayName("searchNetworksByName returns empty list when pattern is empty")
  void shouldReturnEmptyListWhenPatternIsEmpty() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    // Act
    List<Network> result = listNetworksUseCase.searchNetworksByName("", correlationId);

    // Assert
    assertTrue(result.isEmpty());
    verify(networkRepository, never()).findAll(any());
  }

  @Test
  @DisplayName("searchNetworksByName returns empty list when no matches found")
  void shouldReturnEmptyListWhenNoMatchesFound() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    Network ethereum = mockNetwork(UUID.randomUUID(), "Ethereum", NetworkStatus.ACTIVE);
    Network polygon = mockNetwork(UUID.randomUUID(), "Polygon", NetworkStatus.ACTIVE);

    when(networkRepository.findAll(correlationId)).thenReturn(List.of(ethereum, polygon));

    // Act
    List<Network> result = listNetworksUseCase.searchNetworksByName("Bitcoin", correlationId);

    // Assert
    assertTrue(result.isEmpty());
    verify(networkRepository, times(1)).findAll(correlationId);
  }

  @Test
  @DisplayName("searchNetworksByName throws exception when correlationId is invalid")
  void searchNetworksByName_shouldThrowExceptionWhenCorrelationIdIsInvalid() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String invalidCorrelationId = "invalid-uuid";

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> listNetworksUseCase.searchNetworksByName("eth", invalidCorrelationId));

    assertEquals("Correlation ID must be a valid UUID", exception.getMessage());
    verify(networkRepository, never()).findAll(any());
  }

  /**
   * Tests for ListNetworksUseCase#getNetworkHealthStatus method.
   * The method returns health information for all networks.
   */

  @Test
  @DisplayName("getNetworkHealthStatus returns health info for all networks")
  void shouldReturnHealthInfoForAllNetworks() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    Network activeNetwork = mockNetworkWithDetails(UUID.randomUUID(), "Ethereum", "1", NetworkStatus.ACTIVE, "https://eth.rpc");
    Network inactiveNetwork = mockNetworkWithDetails(UUID.randomUUID(), "Polygon", "137", NetworkStatus.INACTIVE, "https://polygon.rpc");

    when(networkRepository.findAll(correlationId)).thenReturn(List.of(activeNetwork, inactiveNetwork));

    // Act
    List<NetworkHealthInfo> result = listNetworksUseCase.getNetworkHealthStatus(correlationId);

    // Assert
    assertEquals(2, result.size());
    verify(networkRepository, times(1)).findAll(correlationId);
  }

  @Test
  @DisplayName("getNetworkHealthStatus maps healthy status correctly")
  void shouldMapHealthyStatusCorrectly() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();
    UUID networkId = UUID.randomUUID();

    Network activeNetwork = mockNetworkWithDetails(networkId, "Ethereum", "1", NetworkStatus.ACTIVE, "https://eth.rpc");

    when(networkRepository.findAll(correlationId)).thenReturn(List.of(activeNetwork));

    // Act
    List<NetworkHealthInfo> result = listNetworksUseCase.getNetworkHealthStatus(correlationId);

    // Assert
    assertEquals(1, result.size());
    NetworkHealthInfo healthInfo = result.get(0);
    assertEquals(networkId, healthInfo.networkId());
    assertEquals("Ethereum", healthInfo.name());
    assertEquals("1", healthInfo.chainId());
    assertEquals(NetworkStatus.ACTIVE, healthInfo.status());
    assertTrue(healthInfo.isHealthy());
    assertEquals("Healthy", healthInfo.healthStatus());
    assertEquals("https://eth.rpc", healthInfo.rpcUrl());
  }

  @Test
  @DisplayName("getNetworkHealthStatus maps unavailable status correctly")
  void shouldMapUnavailableStatusCorrectly() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();
    UUID networkId = UUID.randomUUID();

    Network inactiveNetwork = mockNetworkWithDetails(networkId, "Polygon", "137", NetworkStatus.INACTIVE, "https://polygon.rpc");

    when(networkRepository.findAll(correlationId)).thenReturn(List.of(inactiveNetwork));

    // Act
    List<NetworkHealthInfo> result = listNetworksUseCase.getNetworkHealthStatus(correlationId);

    // Assert
    assertEquals(1, result.size());
    NetworkHealthInfo healthInfo = result.get(0);
    assertEquals(networkId, healthInfo.networkId());
    assertEquals("Polygon", healthInfo.name());
    assertEquals("137", healthInfo.chainId());
    assertEquals(NetworkStatus.INACTIVE, healthInfo.status());
    assertFalse(healthInfo.isHealthy());
    assertEquals("Unavailable", healthInfo.healthStatus());
    assertEquals("https://polygon.rpc", healthInfo.rpcUrl());
  }

  @Test
  @DisplayName("getNetworkHealthStatus returns empty list when no networks exist")
  void getNetworkHealthStatus_shouldReturnEmptyListWhenNoNetworksExist() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String correlationId = UUID.randomUUID().toString();

    when(networkRepository.findAll(correlationId)).thenReturn(List.of());

    // Act
    List<NetworkHealthInfo> result = listNetworksUseCase.getNetworkHealthStatus(correlationId);

    // Assert
    assertTrue(result.isEmpty());
    verify(networkRepository, times(1)).findAll(correlationId);
  }

  @Test
  @DisplayName("getNetworkHealthStatus throws exception when correlationId is null")
  void getNetworkHealthStatus_shouldThrowExceptionWhenCorrelationIdIsNull() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> listNetworksUseCase.getNetworkHealthStatus(null));

    assertEquals("Correlation ID must be provided", exception.getMessage());
    verify(networkRepository, never()).findAll(any());
  }

  @Test
  @DisplayName("getNetworkHealthStatus throws exception when correlationId is invalid")
  void getNetworkHealthStatus_shouldThrowExceptionWhenCorrelationIdIsInvalid() {
    // Arrange
    NetworkRepository networkRepository = mock(NetworkRepository.class);
    ListNetworksUseCase listNetworksUseCase = new ListNetworksUseCase(networkRepository);
    String invalidCorrelationId = "invalid-uuid";

    // Act and Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> listNetworksUseCase.getNetworkHealthStatus(invalidCorrelationId));

    assertEquals("Correlation ID must be a valid UUID", exception.getMessage());
    verify(networkRepository, never()).findAll(any());
  }

  /**
   * Tests for NetworkHealthInfo record.
   * Verifies the record creation and field access.
   */

  @Test
  @DisplayName("NetworkHealthInfo record creates instance with all fields")
  void shouldCreateNetworkHealthInfoWithAllFields() {
    // Arrange
    UUID networkId = UUID.randomUUID();
    String name = "Ethereum";
    String chainId = "1";
    NetworkStatus status = NetworkStatus.ACTIVE;
    boolean isHealthy = true;
    String healthStatus = "Healthy";
    String rpcUrl = "https://eth.rpc";

    // Act
    NetworkHealthInfo healthInfo = new NetworkHealthInfo(networkId, name, chainId, status, isHealthy, healthStatus, rpcUrl);

    // Assert
    assertNotNull(healthInfo);
    assertEquals(networkId, healthInfo.networkId());
    assertEquals(name, healthInfo.name());
    assertEquals(chainId, healthInfo.chainId());
    assertEquals(status, healthInfo.status());
    assertTrue(healthInfo.isHealthy());
    assertEquals(healthStatus, healthInfo.healthStatus());
    assertEquals(rpcUrl, healthInfo.rpcUrl());
  }

  @Test
  @DisplayName("NetworkHealthInfo record handles different network statuses")
  void shouldHandleDifferentNetworkStatuses() {
    // Arrange
    UUID networkId1 = UUID.randomUUID();
    UUID networkId2 = UUID.randomUUID();
    UUID networkId3 = UUID.randomUUID();

    // Act
    NetworkHealthInfo activeInfo = new NetworkHealthInfo(networkId1, "Network 1", "1", NetworkStatus.ACTIVE, true, "Healthy", "https://rpc1");
    NetworkHealthInfo inactiveInfo = new NetworkHealthInfo(networkId2, "Network 2", "2", NetworkStatus.INACTIVE, false, "Unavailable", "https://rpc2");
    NetworkHealthInfo maintenanceInfo = new NetworkHealthInfo(networkId3, "Network 3", "3", NetworkStatus.MAINTENANCE, false, "Unavailable", "https://rpc3");

    // Assert
    assertEquals(NetworkStatus.ACTIVE, activeInfo.status());
    assertTrue(activeInfo.isHealthy());
    assertEquals("Healthy", activeInfo.healthStatus());

    assertEquals(NetworkStatus.INACTIVE, inactiveInfo.status());
    assertFalse(inactiveInfo.isHealthy());
    assertEquals("Unavailable", inactiveInfo.healthStatus());

    assertEquals(NetworkStatus.MAINTENANCE, maintenanceInfo.status());
    assertFalse(maintenanceInfo.isHealthy());
    assertEquals("Unavailable", maintenanceInfo.healthStatus());
  }

  private Network mockNetworkWithDetails(UUID id, String name, String chainId, NetworkStatus status, String rpcUrl) {
    Network network = mock(Network.class);
    when(network.getId()).thenReturn(id);
    when(network.getName()).thenReturn(name);
    when(network.getChainId()).thenReturn(chainId);
    when(network.getStatus()).thenReturn(status);
    when(network.isAvailable()).thenReturn(status == NetworkStatus.ACTIVE);
    when(network.getRpcUrl()).thenReturn(rpcUrl);
    return network;
  }
}