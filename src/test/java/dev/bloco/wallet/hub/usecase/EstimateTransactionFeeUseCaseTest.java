package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.gateway.TransactionFeeRepository;
import dev.bloco.wallet.hub.domain.model.network.Network;
import dev.bloco.wallet.hub.domain.model.transaction.BlockchainTransactionType;
import dev.bloco.wallet.hub.domain.model.transaction.FeeEstimateResult;
import dev.bloco.wallet.hub.domain.model.transaction.FeeLevel;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionFee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@DisplayName("Estimate Transaction Fee Use Case Tests")
class EstimateTransactionFeeUseCaseTest {

  @Test
  @DisplayName("estimateTransactionFee estimates transaction fee")
  void shouldEstimateTransactionFeeSuccessfully() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    BigDecimal gasLimit = new BigDecimal("21000");
    String correlationId = UUID.randomUUID().toString();

    Network mockNetwork = Mockito.mock(Network.class);
    Mockito.when(mockNetwork.isAvailable()).thenReturn(true);
    Mockito.when(mockNetwork.getName()).thenReturn("Ethereum");
    Mockito.when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(mockNetwork));

    FeeLevel[] feeLevels = FeeLevel.values();
    for (FeeLevel level : feeLevels) {
      TransactionFee mockTransactionFee = Mockito.mock(TransactionFee.class);
      Mockito.when(mockTransactionFee.calculateTotalCost(gasLimit))
          .thenReturn(gasLimit.multiply(new BigDecimal("0.00002")));
      Mockito.when(transactionFeeRepository.findLatestByNetworkIdAndLevel(networkId, level))
          .thenReturn(Optional.of(mockTransactionFee));
    }

    FeeEstimateResult result = useCase.estimateTransactionFee(networkId, gasLimit, correlationId);

    assertEquals(networkId, result.networkId());
    assertEquals(mockNetwork.getName(), result.networkName());
    assertEquals(gasLimit, result.gasLimit());
    assertNotNull(result.estimates());
    assertEquals(feeLevels.length, result.estimates().size());
  }

  @Test
  @DisplayName("estimateTransactionFee throws exception when networkId is null")
  void shouldThrowExceptionWhenNetworkIdIsNull() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    BigDecimal gasLimit = new BigDecimal("21000");
    String correlationId = UUID.randomUUID().toString();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.estimateTransactionFee(null, gasLimit, correlationId));

    assertEquals("Network ID must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("estimateTransactionFee throws exception when gasLimit is null")
  void shouldThrowExceptionWhenGasLimitIsZeroOrNegative() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    IllegalArgumentException exceptionZero = assertThrows(IllegalArgumentException.class,
        () -> useCase.estimateTransactionFee(networkId, BigDecimal.ZERO, correlationId));
    assertEquals("Gas limit must be positive", exceptionZero.getMessage());

    IllegalArgumentException exceptionNegative = assertThrows(IllegalArgumentException.class,
        () -> useCase.estimateTransactionFee(networkId, BigDecimal.valueOf(-1), correlationId));
    assertEquals("Gas limit must be positive", exceptionNegative.getMessage());
  }

  @Test
  @DisplayName("estimateTransactionFee throws exception when correlationId is null or invalid")
  void shouldThrowExceptionWhenCorrelationIdIsInvalid() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    BigDecimal gasLimit = new BigDecimal("21000");

    IllegalArgumentException exceptionEmpty = assertThrows(IllegalArgumentException.class,
        () -> useCase.estimateTransactionFee(networkId, gasLimit, ""));
    assertEquals("Correlation ID must be provided", exceptionEmpty.getMessage());

    IllegalArgumentException exceptionInvalid = assertThrows(IllegalArgumentException.class,
        () -> useCase.estimateTransactionFee(networkId, gasLimit, "invalid-uuid"));
    assertEquals("Correlation ID must be a valid UUID", exceptionInvalid.getMessage());
  }

  @Test
  @DisplayName("estimateTransactionFee throws exception when network is not found")
  void shouldThrowExceptionWhenNetworkNotFound() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    BigDecimal gasLimit = new BigDecimal("21000");
    String correlationId = UUID.randomUUID().toString();

    Mockito.when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.estimateTransactionFee(networkId, gasLimit, correlationId));

    assertEquals("Network not found with id: " + networkId, exception.getMessage());
  }

  @Test
  @DisplayName("estimateTransactionFee throws exception when network is not available")
  void shouldThrowExceptionWhenNetworkIsUnavailable() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    BigDecimal gasLimit = new BigDecimal("21000");
    String correlationId = UUID.randomUUID().toString();

    Network mockNetwork = Mockito.mock(Network.class);
    Mockito.when(mockNetwork.isAvailable()).thenReturn(false);
    Mockito.when(mockNetwork.getName()).thenReturn("Ethereum");
    Mockito.when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(mockNetwork));

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> useCase.estimateTransactionFee(networkId, gasLimit, correlationId));

    assertEquals("Network is not available: Ethereum", exception.getMessage());
  }

  @Test
  @DisplayName("estimateTransactionFee applies default fees when no transaction fees are found")
  void shouldReturnDefaultFeeEstimatesWhenTransactionFeesAreAbsent() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    BigDecimal gasLimit = new BigDecimal("21000");
    String correlationId = UUID.randomUUID().toString();

    Network mockNetwork = Mockito.mock(Network.class);
    Mockito.when(mockNetwork.isAvailable()).thenReturn(true);
    Mockito.when(mockNetwork.getName()).thenReturn("Ethereum");
    Mockito.when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(mockNetwork));

    // Simulate no transaction fees available for any fee level
    Mockito.when(transactionFeeRepository.findLatestByNetworkIdAndLevel(eq(networkId), any()))
        .thenReturn(Optional.empty());

    FeeEstimateResult result = useCase.estimateTransactionFee(networkId, gasLimit, correlationId);

    assertEquals(networkId, result.networkId());
    assertEquals(mockNetwork.getName(), result.networkName());
    assertEquals(gasLimit, result.gasLimit());
    assertNotNull(result.estimates());
    assertEquals(FeeLevel.values().length, result.estimates().size());

    result.estimates().forEach(feeEstimate -> {
      assertNotNull(feeEstimate.gasPrice());
      assertTrue(feeEstimate.gasPrice().compareTo(BigDecimal.ZERO) > 0);
      assertNotNull(feeEstimate.totalCost());
      assertTrue(feeEstimate.totalCost().compareTo(BigDecimal.ZERO) > 0);
    });
  }

  @Test
  @DisplayName("estimateTransactionFee uses normalized correlation ID")
  void shouldUseNormalizedCorrelationIdForFeeCalculation() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    BigDecimal gasLimit = new BigDecimal("21000");
    String correlationIdWithExtraSpaces = "  " + UUID.randomUUID().toString() + "  ";

    Network mockNetwork = Mockito.mock(Network.class);
    Mockito.when(mockNetwork.isAvailable()).thenReturn(true);
    Mockito.when(mockNetwork.getName()).thenReturn("Ethereum");
    Mockito.when(networkRepository.findById(eq(networkId), eq(correlationIdWithExtraSpaces.trim())))
        .thenReturn(Optional.of(mockNetwork));

    FeeLevel feeLevel = FeeLevel.STANDARD;
    TransactionFee mockTransactionFee = Mockito.mock(TransactionFee.class);
    Mockito.when(mockTransactionFee.calculateTotalCost(gasLimit)).thenReturn(new BigDecimal("0.0005"));
    Mockito.when(transactionFeeRepository.findLatestByNetworkIdAndLevel(networkId, feeLevel))
        .thenReturn(Optional.of(mockTransactionFee));

    FeeEstimateResult result = useCase.estimateTransactionFee(networkId, gasLimit, correlationIdWithExtraSpaces);

    assertEquals(networkId, result.networkId());
    assertEquals(mockNetwork.getName(), result.networkName());
    assertEquals(gasLimit, result.gasLimit());
    assertNotNull(result.estimates());
  }

  @Test
  @DisplayName("estimateGasLimit returns correct gas limit for SIMPLE_TRANSFER")
  void shouldEstimateGasLimitForSimpleTransfer() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    Network mockNetwork = Mockito.mock(Network.class);
    Mockito.when(mockNetwork.isAvailable()).thenReturn(true);
    Mockito.when(mockNetwork.getName()).thenReturn("Ethereum");
    Mockito.when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(mockNetwork));

    BigDecimal gasLimit = useCase.estimateGasLimit(networkId, BlockchainTransactionType.SIMPLE_TRANSFER, correlationId);

    assertEquals(new BigDecimal("21000"), gasLimit);
  }

  @Test
  @DisplayName("estimateGasLimit returns correct gas limit for ERC20_TRANSFER")
  void shouldEstimateGasLimitForERC20Transfer() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    Network mockNetwork = Mockito.mock(Network.class);
    Mockito.when(mockNetwork.isAvailable()).thenReturn(true);
    Mockito.when(mockNetwork.getName()).thenReturn("Ethereum");
    Mockito.when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(mockNetwork));

    BigDecimal gasLimit = useCase.estimateGasLimit(networkId, BlockchainTransactionType.ERC20_TRANSFER, correlationId);

    assertEquals(new BigDecimal("65000"), gasLimit);
  }

  @Test
  @DisplayName("estimateGasLimit returns correct gas limit for ERC721_TRANSFER")
  void shouldEstimateGasLimitForERC721Transfer() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    Network mockNetwork = Mockito.mock(Network.class);
    Mockito.when(mockNetwork.isAvailable()).thenReturn(true);
    Mockito.when(mockNetwork.getName()).thenReturn("Ethereum");
    Mockito.when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(mockNetwork));

    BigDecimal gasLimit = useCase.estimateGasLimit(networkId, BlockchainTransactionType.ERC721_TRANSFER, correlationId);

    assertEquals(new BigDecimal("85000"), gasLimit);
  }

  @Test
  @DisplayName("estimateGasLimit returns correct gas limit for CONTRACT_INTERACTION")
  void shouldEstimateGasLimitForContractInteraction() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    Network mockNetwork = Mockito.mock(Network.class);
    Mockito.when(mockNetwork.isAvailable()).thenReturn(true);
    Mockito.when(mockNetwork.getName()).thenReturn("Ethereum");
    Mockito.when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(mockNetwork));

    BigDecimal gasLimit = useCase.estimateGasLimit(networkId, BlockchainTransactionType.CONTRACT_INTERACTION,
        correlationId);

    assertEquals(new BigDecimal("150000"), gasLimit);
  }

  @Test
  @DisplayName("estimateGasLimit returns correct gas limit for CONTRACT_DEPLOYMENT")
  void shouldEstimateGasLimitForContractDeployment() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    Network mockNetwork = Mockito.mock(Network.class);
    Mockito.when(mockNetwork.isAvailable()).thenReturn(true);
    Mockito.when(mockNetwork.getName()).thenReturn("Ethereum");
    Mockito.when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(mockNetwork));

    BigDecimal gasLimit = useCase.estimateGasLimit(networkId, BlockchainTransactionType.CONTRACT_DEPLOYMENT,
        correlationId);

    assertEquals(new BigDecimal("500000"), gasLimit);
  }

  @Test
  @DisplayName("estimateGasLimit returns correct gas limit for COMPLEX_DEFI")
  void shouldEstimateGasLimitForComplexDeFi() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    Network mockNetwork = Mockito.mock(Network.class);
    Mockito.when(mockNetwork.isAvailable()).thenReturn(true);
    Mockito.when(mockNetwork.getName()).thenReturn("Ethereum");
    Mockito.when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.of(mockNetwork));

    BigDecimal gasLimit = useCase.estimateGasLimit(networkId, BlockchainTransactionType.COMPLEX_DEFI, correlationId);

    assertEquals(new BigDecimal("300000"), gasLimit);
  }

  @Test
  @DisplayName("estimateGasLimit throws exception when networkId is null")
  void shouldThrowExceptionWhenEstimateGasLimitWithNullNetworkId() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    String correlationId = UUID.randomUUID().toString();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.estimateGasLimit(null, BlockchainTransactionType.SIMPLE_TRANSFER, correlationId));

    assertEquals("Network ID must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("estimateGasLimit throws exception when transactionType is null")
  void shouldThrowExceptionWhenEstimateGasLimitWithNullTransactionType() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.estimateGasLimit(networkId, null, correlationId));

    assertEquals("Transaction type must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("estimateGasLimit throws exception when network is not found")
  void shouldThrowExceptionWhenEstimateGasLimitWithNonExistentNetwork() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    Mockito.when(networkRepository.findById(eq(networkId), any())).thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.estimateGasLimit(networkId, BlockchainTransactionType.SIMPLE_TRANSFER, correlationId));

    assertEquals("Network not found with id: " + networkId, exception.getMessage());
  }

  @Test
  @DisplayName("getLatestFee returns fee from repository when available")
  void shouldReturnFeeFromRepositoryWhenAvailable() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    FeeLevel feeLevel = FeeLevel.STANDARD;

    TransactionFee mockFee = Mockito.mock(TransactionFee.class);
    Mockito.when(transactionFeeRepository.findLatestByNetworkIdAndLevel(networkId, feeLevel))
        .thenReturn(Optional.of(mockFee));

    TransactionFee result = useCase.getLatestFee(networkId, feeLevel);

    assertEquals(mockFee, result);
    Mockito.verify(transactionFeeRepository, Mockito.times(1))
        .findLatestByNetworkIdAndLevel(networkId, feeLevel);
  }

  @Test
  @DisplayName("getLatestFee returns default fee when not found in repository")
  void shouldReturnDefaultFeeWhenNotFoundInRepository() {
    NetworkRepository networkRepository = Mockito.mock(NetworkRepository.class);
    TransactionFeeRepository transactionFeeRepository = Mockito.mock(TransactionFeeRepository.class);
    EstimateTransactionFeeUseCase useCase = new EstimateTransactionFeeUseCase(networkRepository,
        transactionFeeRepository);

    UUID networkId = UUID.randomUUID();
    FeeLevel feeLevel = FeeLevel.FAST;

    Mockito.when(transactionFeeRepository.findLatestByNetworkIdAndLevel(networkId, feeLevel))
        .thenReturn(Optional.empty());

    TransactionFee result = useCase.getLatestFee(networkId, feeLevel);

    assertNotNull(result);
    assertNotNull(result.getGasPrice());
    assertTrue(result.getGasPrice().compareTo(BigDecimal.ZERO) > 0);
    assertEquals(feeLevel, result.getLevel());
    Mockito.verify(transactionFeeRepository, Mockito.times(1))
        .findLatestByNetworkIdAndLevel(networkId, feeLevel);
  }
}