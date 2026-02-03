package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Get Wallet Details Use Case Tests")
class GetWalletDetailsUseCaseTest {

  @Test
  @DisplayName("getWalletDetails retrieves a wallet successfully")
  void getWalletDetails_retrievesWalletSuccessfully() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, "Test Wallet", "Description");

    WalletRepository walletRepository = mock(WalletRepository.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

    GetWalletDetailsUseCase useCase = new GetWalletDetailsUseCase(walletRepository);

    // Act
    Wallet result = useCase.getWalletDetails(walletId);

    // Assert
    assertNotNull(result);
    assertEquals(walletId, result.getId());
    assertEquals("Test Wallet", result.getName());
    verify(walletRepository, times(1)).findById(walletId);
  }

  @Test
  @DisplayName("getWalletDetails throws exception when wallet ID is null")
  void getWalletDetails_throwsExceptionWhenWalletIdIsNull() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    GetWalletDetailsUseCase useCase = new GetWalletDetailsUseCase(walletRepository);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.getWalletDetails(null));

    assertEquals("Wallet ID must be provided", exception.getMessage());
    verify(walletRepository, never()).findById(any());
  }

  @Test
  @DisplayName("getWalletDetails throws exception when wallet not found")
  void getWalletDetails_throwsExceptionWhenWalletNotFound() {
    // Arrange
    UUID walletId = UUID.randomUUID();

    WalletRepository walletRepository = mock(WalletRepository.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    GetWalletDetailsUseCase useCase = new GetWalletDetailsUseCase(walletRepository);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.getWalletDetails(walletId));

    assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
    verify(walletRepository, times(1)).findById(walletId);
  }

  @Test
  @DisplayName("getWallet retrieves wallet successfully when includeDeleted is true")
  void getWallet_retrievesWalletSuccessfully_includeDeletedTrue() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    Wallet wallet = mock(Wallet.class);
    when(wallet.isDeleted()).thenReturn(true);

    WalletRepository walletRepository = mock(WalletRepository.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

    GetWalletDetailsUseCase useCase = new GetWalletDetailsUseCase(walletRepository);

    // Act
    Wallet result = useCase.getWallet(walletId, true);

    // Assert
    assertNotNull(result);
    verify(walletRepository, times(1)).findById(walletId);
  }

  @Test
  @DisplayName("getWallet throws exception when wallet is deleted and includeDeleted is false")
  void getWallet_throwsExceptionWhenWalletIsDeleted_includeDeletedFalse() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    Wallet wallet = mock(Wallet.class);
    when(wallet.isDeleted()).thenReturn(true);

    WalletRepository walletRepository = mock(WalletRepository.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

    GetWalletDetailsUseCase useCase = new GetWalletDetailsUseCase(walletRepository);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.getWallet(walletId, false));

    assertEquals("Wallet is deleted and not accessible", exception.getMessage());
    verify(walletRepository, times(1)).findById(walletId);
  }

  @Test
  @DisplayName("isWalletAccessible returns true when wallet exists and is not deleted")
  void isWalletAccessible_returnsTrueWhenWalletExistsAndNotDeleted() {
    // Arrange
    UUID walletId = UUID.randomUUID();
    Wallet wallet = mock(Wallet.class);
    when(wallet.isDeleted()).thenReturn(false);

    WalletRepository walletRepository = mock(WalletRepository.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

    GetWalletDetailsUseCase useCase = new GetWalletDetailsUseCase(walletRepository);

    // Act
    boolean result = useCase.isWalletAccessible(walletId);

    // Assert
    assertTrue(result);
    verify(walletRepository, times(1)).findById(walletId);
  }

  @Test
  @DisplayName("isWalletAccessible returns false when wallet does not exist")
  void isWalletAccessible_returnsFalseWhenWalletDoesNotExist() {
    // Arrange
    UUID walletId = UUID.randomUUID();

    WalletRepository walletRepository = mock(WalletRepository.class);
    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    GetWalletDetailsUseCase useCase = new GetWalletDetailsUseCase(walletRepository);

    // Act
    boolean result = useCase.isWalletAccessible(walletId);

    // Assert
    assertFalse(result);
    verify(walletRepository, times(1)).findById(walletId);
  }

  @Test
  @DisplayName("isWalletAccessible returns false when wallet ID is null")
  void isWalletAccessible_returnsFalseWhenWalletIdIsNull() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    GetWalletDetailsUseCase useCase = new GetWalletDetailsUseCase(walletRepository);

    // Act
    boolean result = useCase.isWalletAccessible(null);

    // Assert
    assertFalse(result);
    verify(walletRepository, never()).findById(any());
  }
}