// <llm-snippet-file>ListWalletsUseCaseTest.java</llm-snippet-file>
package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("List Wallets Use Case Tests")
class ListWalletsUseCaseTest {

  /**
   * Tests for the listWallets() method in ListWalletsUseCase.
   * <p>
   * listWallets() retrieves a list of wallets associated with a given userId
   * while filtering out deleted wallets.
   */

  @Test
  @DisplayName("listWallets returns a list of non-deleted wallets")
  void shouldReturnListOfNonDeletedWallets() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    ListWalletsUseCase useCase = new ListWalletsUseCase(walletRepository);

    UUID userId = UUID.randomUUID();
    Wallet wallet1 = mock(Wallet.class);
    Wallet wallet2 = mock(Wallet.class);
    Wallet wallet3 = mock(Wallet.class);

    when(wallet1.isDeleted()).thenReturn(false);
    when(wallet2.isDeleted()).thenReturn(false);
    when(wallet3.isDeleted()).thenReturn(true);

    when(walletRepository.findByUserId(userId)).thenReturn(List.of(wallet1, wallet2, wallet3));

    // Act
    List<Wallet> result = useCase.listWallets(userId);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.contains(wallet1));
    assertTrue(result.contains(wallet2));
    assertFalse(result.contains(wallet3));

    verify(walletRepository, times(1)).findByUserId(userId);
    verify(wallet1, times(1)).isDeleted();
    verify(wallet2, times(1)).isDeleted();
    verify(wallet3, times(1)).isDeleted();
  }

  @Test
  @DisplayName("listWallets throws exception when userId is null")
  void shouldThrowExceptionWhenUserIdIsNull() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    ListWalletsUseCase useCase = new ListWalletsUseCase(walletRepository);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.listWallets(null));

    assertEquals("User ID must be provided", exception.getMessage());
    verifyNoInteractions(walletRepository);
  }

  @Test
  @DisplayName("listWallets returns an empty list when no wallets exist")
  void shouldReturnEmptyListWhenNoWalletsExistForUserId() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    ListWalletsUseCase useCase = new ListWalletsUseCase(walletRepository);

    UUID userId = UUID.randomUUID();
    when(walletRepository.findByUserId(userId)).thenReturn(List.of());

    // Act
    List<Wallet> result = useCase.listWallets(userId);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(walletRepository, times(1)).findByUserId(userId);
  }

  @Test
  @DisplayName("listWallets ignores deleted wallets")
  void shouldIgnoreDeletedWalletsFromRepository() {
    // Arrange
    WalletRepository walletRepository = mock(WalletRepository.class);
    ListWalletsUseCase useCase = new ListWalletsUseCase(walletRepository);

    UUID userId = UUID.randomUUID();
    Wallet wallet1 = mock(Wallet.class);
    Wallet wallet2 = mock(Wallet.class);

    when(wallet1.isDeleted()).thenReturn(true); // Marked as deleted
    when(wallet2.isDeleted()).thenReturn(false);

    when(walletRepository.findByUserId(userId)).thenReturn(List.of(wallet1, wallet2));

    // Act
    List<Wallet> result = useCase.listWallets(userId);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.contains(wallet2));
    assertFalse(result.contains(wallet1));

    verify(walletRepository, times(1)).findByUserId(userId);
    verify(wallet1, times(1)).isDeleted();
    verify(wallet2, times(1)).isDeleted();
  }
}