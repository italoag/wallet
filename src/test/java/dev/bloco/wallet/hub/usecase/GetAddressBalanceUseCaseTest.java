package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenBalanceRepository;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AccountAddress;
import dev.bloco.wallet.hub.domain.model.address.AddressType;
import dev.bloco.wallet.hub.domain.model.address.PublicKey;
import dev.bloco.wallet.hub.domain.model.token.TokenBalance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
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
    GetAddressBalanceUseCase.AddressBalanceResult result = useCase.getAddressBalance(addressId);

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
    GetAddressBalanceUseCase.AddressBalanceResult result = useCase.getAddressBalance(addressId);

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
}