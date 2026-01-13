package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenBalanceRepository;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AccountAddress;
import dev.bloco.wallet.hub.domain.model.address.AddressBalanceResult;
import dev.bloco.wallet.hub.domain.model.address.AddressType;
import dev.bloco.wallet.hub.domain.model.address.PublicKey;
import dev.bloco.wallet.hub.domain.model.token.TokenBalance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Get Address Balance Use Case Tests")
class GetAddressBalanceUseCaseTest {

  @Test
  @DisplayName("getAddressBalance returns address balance")
  void shouldReturnAddressBalanceSuccessfully() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);

    UUID addressId = UUID.randomUUID();
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    Address mockAddress = new Address(
        addressId,
        walletId,
        networkId,
        new PublicKey("0xpublickey123"),
        new AccountAddress("0x123456789"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/0"
    );
    UUID tokenId1 = UUID.randomUUID();
    UUID tokenId2 = UUID.randomUUID();
    List<TokenBalance> mockTokenBalances = List.of(
        new TokenBalance(UUID.randomUUID(), addressId, tokenId1, new BigDecimal("100.00")),
        new TokenBalance(UUID.randomUUID(), addressId, tokenId2, new BigDecimal("200.00"))
    );

    when(addressRepository.findById(addressId)).thenReturn(Optional.of(mockAddress));
    when(tokenBalanceRepository.findByAddressId(addressId)).thenReturn(mockTokenBalances);

    // Act
    AddressBalanceResult result = useCase.getAddressBalance(addressId);

    // Assert
    assertNotNull(result);
    assertEquals(addressId, result.getAddressId());
    assertEquals("0x123456789", result.getAddress());
    assertEquals(mockAddress.getWalletId(), result.getWalletId());
    assertEquals(mockAddress.getNetworkId(), result.getNetworkId());
    assertEquals(new BigDecimal("300.00"), result.getTotalValue());
    assertEquals(2, result.getBalanceCount());
    assertEquals(2, result.getTokenBalances().size());
    verify(addressRepository, times(1)).findById(addressId);
    verify(tokenBalanceRepository, times(1)).findByAddressId(addressId);
  }

  @Test
  @DisplayName("getAddressBalance throws exception when address not found")
  void shouldThrowExceptionWhenAddressNotFound() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);

    UUID addressId = UUID.randomUUID();
    when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.getAddressBalance(addressId));
    assertEquals("Address not found with id: " + addressId, exception.getMessage());
    verify(addressRepository, times(1)).findById(addressId);
    verify(tokenBalanceRepository, never()).findByAddressId(any());
  }

  @Test
  @DisplayName("getAddressBalance throws exception when address id is null")
  void shouldThrowExceptionWhenAddressIdIsNull() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.getAddressBalance(null));
    assertEquals("Address ID must be provided", exception.getMessage());
    verifyNoInteractions(addressRepository);
    verifyNoInteractions(tokenBalanceRepository);
  }

  @Test
  @DisplayName("getAddressBalance handles empty token balances")
  void shouldHandleEmptyTokenBalances() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);

    UUID addressId = UUID.randomUUID();
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    Address mockAddress = new Address(
        addressId,
        walletId,
        networkId,
        new PublicKey("0xpublickey123"),
        new AccountAddress("0x123456789"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/0"
    );

    when(addressRepository.findById(addressId)).thenReturn(Optional.of(mockAddress));
    when(tokenBalanceRepository.findByAddressId(addressId)).thenReturn(List.of());

    // Act
    AddressBalanceResult result = useCase.getAddressBalance(addressId);

    // Assert
    assertNotNull(result);
    assertEquals(addressId, result.getAddressId());
    assertEquals("0x123456789", result.getAddress());
    assertEquals(mockAddress.getWalletId(), result.getWalletId());
    assertEquals(mockAddress.getNetworkId(), result.getNetworkId());
    assertEquals(BigDecimal.ZERO, result.getTotalValue());
    assertEquals(0, result.getBalanceCount());
    assertTrue(result.getTokenBalances().isEmpty());
    verify(addressRepository, times(1)).findById(addressId);
    verify(tokenBalanceRepository, times(1)).findByAddressId(addressId);
  }

  @Test
  @DisplayName("getTokenBalance throws exception when address id is null")
  void shouldThrowExceptionWhenGetTokenBalanceWithNullAddressId() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);

    UUID tokenId = UUID.randomUUID();

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
        () -> useCase.getTokenBalance(null, tokenId));
    assertEquals("Address ID must be provided", exception.getMessage());
    verifyNoInteractions(addressRepository);
    verifyNoInteractions(tokenBalanceRepository);
  }

  @Test
  @DisplayName("getTokenBalance throws exception when token id is null")
  void shouldThrowExceptionWhenGetTokenBalanceWithNullTokenId() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);

    UUID addressId = UUID.randomUUID();

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
        () -> useCase.getTokenBalance(addressId, null));
    assertEquals("Token ID must be provided", exception.getMessage());
    verifyNoInteractions(addressRepository);
    verifyNoInteractions(tokenBalanceRepository);
  }

  @Test
  @DisplayName("getTokenBalance throws exception when address not found")
  void shouldThrowExceptionWhenGetTokenBalanceWithNonExistentAddress() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);

    UUID addressId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();

    when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
        () -> useCase.getTokenBalance(addressId, tokenId));
    assertEquals("Address not found with id: " + addressId, exception.getMessage());
    verify(addressRepository, times(1)).findById(addressId);
    verifyNoInteractions(tokenBalanceRepository);
  }

  @Test
  @DisplayName("getTokenBalance returns balance when token exists")
  void shouldReturnTokenBalanceWhenTokenExists() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);

    UUID addressId = UUID.randomUUID();
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();

    Address mockAddress = new Address(
        addressId,
        walletId,
        networkId,
        new PublicKey("0xpublickey123"),
        new AccountAddress("0x123456789"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/0"
    );
    TokenBalance mockTokenBalance = new TokenBalance(UUID.randomUUID(), addressId, tokenId, new BigDecimal("150.50"));

    when(addressRepository.findById(addressId)).thenReturn(Optional.of(mockAddress));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId, tokenId)).thenReturn(Optional.of(mockTokenBalance));

    // Act
    BigDecimal balance = useCase.getTokenBalance(addressId, tokenId);

    // Assert
    assertNotNull(balance);
    assertEquals(new BigDecimal("150.50"), balance);
    verify(addressRepository, times(1)).findById(addressId);
    verify(tokenBalanceRepository, times(1)).findByAddressIdAndTokenId(addressId, tokenId);
  }

  @Test
  @DisplayName("getTokenBalance returns zero when token not found")
  void shouldReturnZeroWhenTokenNotFound() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);

    UUID addressId = UUID.randomUUID();
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();

    Address mockAddress = new Address(
        addressId,
        walletId,
        networkId,
        new PublicKey("0xpublickey123"),
        new AccountAddress("0x123456789"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/0"
    );

    when(addressRepository.findById(addressId)).thenReturn(Optional.of(mockAddress));
    when(tokenBalanceRepository.findByAddressIdAndTokenId(addressId, tokenId)).thenReturn(Optional.empty());

    // Act
    BigDecimal balance = useCase.getTokenBalance(addressId, tokenId);

    // Assert
    assertNotNull(balance);
    assertEquals(BigDecimal.ZERO, balance);
    verify(addressRepository, times(1)).findById(addressId);
    verify(tokenBalanceRepository, times(1)).findByAddressIdAndTokenId(addressId, tokenId);
  }

  @Test
  @DisplayName("getMultipleAddressBalances returns empty map when list is null")
  void shouldReturnEmptyMapWhenAddressListIsNull() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);

    // Act
    Map<UUID, AddressBalanceResult> result = useCase.getMultipleAddressBalances(null);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verifyNoInteractions(addressRepository);
    verifyNoInteractions(tokenBalanceRepository);
  }

  @Test
  @DisplayName("getMultipleAddressBalances returns empty map when list is empty")
  void shouldReturnEmptyMapWhenAddressListIsEmpty() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);

    // Act
    Map<UUID, AddressBalanceResult> result = useCase.getMultipleAddressBalances(List.of());

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verifyNoInteractions(addressRepository);
    verifyNoInteractions(tokenBalanceRepository);
  }

  @Test
  @DisplayName("getMultipleAddressBalances returns balances for multiple addresses")
  void shouldReturnBalancesForMultipleAddresses() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    TokenBalanceRepository tokenBalanceRepository = mock(TokenBalanceRepository.class);
    GetAddressBalanceUseCase useCase = new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);

    UUID addressId1 = UUID.randomUUID();
    UUID addressId2 = UUID.randomUUID();
    UUID walletId = UUID.randomUUID();
    UUID networkId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();

    Address mockAddress1 = new Address(
        addressId1,
        walletId,
        networkId,
        new PublicKey("0xpublickey1"),
        new AccountAddress("0xAddress1"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/0"
    );
    Address mockAddress2 = new Address(
        addressId2,
        walletId,
        networkId,
        new PublicKey("0xpublickey2"),
        new AccountAddress("0xAddress2"),
        AddressType.EXTERNAL,
        "m/44'/60'/0'/0/1"
    );

    TokenBalance tokenBalance1 = new TokenBalance(UUID.randomUUID(), addressId1, tokenId, new BigDecimal("100.00"));
    TokenBalance tokenBalance2 = new TokenBalance(UUID.randomUUID(), addressId2, tokenId, new BigDecimal("200.00"));

    when(addressRepository.findById(addressId1)).thenReturn(Optional.of(mockAddress1));
    when(addressRepository.findById(addressId2)).thenReturn(Optional.of(mockAddress2));
    when(tokenBalanceRepository.findByAddressId(addressId1)).thenReturn(List.of(tokenBalance1));
    when(tokenBalanceRepository.findByAddressId(addressId2)).thenReturn(List.of(tokenBalance2));

    // Act
    Map<UUID, AddressBalanceResult> result = useCase.getMultipleAddressBalances(List.of(addressId1, addressId2));

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    
    AddressBalanceResult balance1 = result.get(addressId1);
    assertNotNull(balance1);
    assertEquals(addressId1, balance1.getAddressId());
    assertEquals("0xAddress1", balance1.getAddress());
    assertEquals(new BigDecimal("100.00"), balance1.getTotalValue());
    assertEquals(1, balance1.getBalanceCount());

    AddressBalanceResult balance2 = result.get(addressId2);
    assertNotNull(balance2);
    assertEquals(addressId2, balance2.getAddressId());
    assertEquals("0xAddress2", balance2.getAddress());
    assertEquals(new BigDecimal("200.00"), balance2.getTotalValue());
    assertEquals(1, balance2.getBalanceCount());

    verify(addressRepository, times(1)).findById(addressId1);
    verify(addressRepository, times(1)).findById(addressId2);
    verify(tokenBalanceRepository, times(1)).findByAddressId(addressId1);
    verify(tokenBalanceRepository, times(1)).findByAddressId(addressId2);
  }
}