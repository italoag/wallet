package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.wallet.WalletStatus;
import dev.bloco.wallet.hub.domain.event.wallet.WalletUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UpdateWalletUseCase.
 * Tests the wallet update functionality including validation and event publishing.
 */
@ExtendWith(MockitoExtension.class)
class UpdateWalletUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private UpdateWalletUseCase updateWalletUseCase;

    private UUID walletId;
    private String correlationId;
    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        updateWalletUseCase = new UpdateWalletUseCase(walletRepository, eventPublisher);
        walletId = UUID.randomUUID();
        correlationId = UUID.randomUUID().toString();
        
        // Create test wallet with active status
        testWallet = new Wallet(walletId, "Original Name", "Original Description");
        testWallet.setStatus(WalletStatus.ACTIVE);
    }

    @Test
    void updateWallet_shouldUpdateNameAndDescription_whenValidInput() {
        // Arrange
        String newName = "Updated Wallet Name";
        String newDescription = "Updated wallet description";
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act
        Wallet result = updateWalletUseCase.updateWallet(walletId, newName, newDescription, correlationId);

        // Assert
        assertNotNull(result);
        assertEquals(newName, result.getName());
        assertEquals(newDescription, result.getDescription());
        
        // Verify repository interactions
        verify(walletRepository).findById(walletId);
        verify(walletRepository).update(testWallet);
        
        // Verify event publishing
        verify(eventPublisher).publish(any(WalletUpdatedEvent.class));
    }

    @Test
    void updateWallet_shouldUpdateOnlyName_whenDescriptionIsNull() {
        // Arrange
        String newName = "Updated Name Only";
        String originalDescription = testWallet.getDescription();
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act
        Wallet result = updateWalletUseCase.updateWallet(walletId, newName, null, correlationId);

        // Assert
        assertEquals(newName, result.getName());
        assertEquals(originalDescription, result.getDescription());
        
        verify(walletRepository).update(testWallet);
        verify(eventPublisher).publish(any(WalletUpdatedEvent.class));
    }

    @Test
    void updateWallet_shouldUpdateOnlyDescription_whenNameIsNull() {
        // Arrange
        String newDescription = "Updated Description Only";
        String originalName = testWallet.getName();
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act
        Wallet result = updateWalletUseCase.updateWallet(walletId, null, newDescription, correlationId);

        // Assert
        assertEquals(originalName, result.getName());
        assertEquals(newDescription, result.getDescription());
        
        verify(walletRepository).update(testWallet);
        verify(eventPublisher).publish(any(WalletUpdatedEvent.class));
    }

    @Test
    void updateWallet_shouldThrowException_whenBothNameAndDescriptionAreNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> updateWalletUseCase.updateWallet(walletId, null, null, correlationId)
        );

        assertEquals("At least one field (name or description) must be provided", exception.getMessage());
        
        // Verify no repository interactions
        verify(walletRepository, never()).findById(any());
        verify(walletRepository, never()).update(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updateWallet_shouldThrowException_whenWalletNotFound() {
        // Arrange
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> updateWalletUseCase.updateWallet(walletId, "New Name", "New Description", correlationId)
        );

        assertEquals("Wallet not found with id: " + walletId, exception.getMessage());
        
        // Verify repository interactions
        verify(walletRepository).findById(walletId);
        verify(walletRepository, never()).update(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updateWallet_shouldThrowException_whenWalletIsNotActive() {
        // Arrange
        testWallet.setStatus(WalletStatus.DELETED);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> updateWalletUseCase.updateWallet(walletId, "New Name", "New Description", correlationId)
        );

        assertTrue(exception.getMessage().contains("Operation not allowed"));
        
        // Verify repository interactions
        verify(walletRepository).findById(walletId);
        verify(walletRepository, never()).update(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updateWallet_shouldSetCorrelationId_whenUpdating() {
        // Arrange
        String newName = "Updated Name";
        String newDescription = "Updated Description";
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

        // Act
        updateWalletUseCase.updateWallet(walletId, newName, newDescription, correlationId);

        // Assert
        assertEquals(UUID.fromString(correlationId), testWallet.getCorrelationId());
        
        verify(walletRepository).update(testWallet);
    }
}