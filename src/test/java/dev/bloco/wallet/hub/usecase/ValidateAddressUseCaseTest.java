package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.model.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ValidateAddressUseCase.
 * Tests address validation functionality for different formats and networks.
 */
@ExtendWith(MockitoExtension.class)
class ValidateAddressUseCaseTest {

    @Mock
    private NetworkRepository networkRepository;

    private ValidateAddressUseCase validateAddressUseCase;
    private UUID networkId;
    private Network testNetwork;
    private String correlationId;

    @BeforeEach
    void setUp() {
        validateAddressUseCase = new ValidateAddressUseCase(networkRepository);
        networkId = UUID.randomUUID();
        testNetwork = Network.create(networkId, "Ethereum", "1", "https://eth.llamarpc.com", "https://etherscan.io");
        correlationId = UUID.randomUUID().toString();
    }

    @Test
    void validateAddress_shouldReturnValid_forEthereumAddress() {
        // Arrange
        String ethereumAddress = "0x742dB5C8A5d8c837e95C5fc73D3DCFFF84C8b742";
        when(networkRepository.findById(eq(networkId), anyString())).thenReturn(Optional.of(testNetwork));

        // Act
        ValidateAddressUseCase.AddressValidationResult result =
            validateAddressUseCase.validateAddress(ethereumAddress, networkId, correlationId);

        // Assert
        assertTrue(result.isValid());
        assertEquals("Ethereum", result.getFormat());
        assertEquals("Ethereum", result.getNetwork());
        assertTrue(result.isNetworkCompatible());
        assertNull(result.getError());
    }

    @Test
    void validateAddress_shouldReturnValid_forBitcoinLegacyAddress() {
        // Arrange
        String bitcoinAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa";
        Network bitcoinNetwork = Network.create(UUID.randomUUID(), "Bitcoin", "bitcoin", "https://bitcoin.llamarpc.com", "https://blockstream.info");
        when(networkRepository.findById(eq(networkId), anyString())).thenReturn(Optional.of(bitcoinNetwork));

        // Act
        ValidateAddressUseCase.AddressValidationResult result =
            validateAddressUseCase.validateAddress(bitcoinAddress, networkId, correlationId);

        // Assert
        assertTrue(result.isValid());
        assertEquals("Bitcoin Legacy", result.getFormat());
        assertEquals("Bitcoin", result.getNetwork());
        assertTrue(result.isNetworkCompatible());
    }

    @Test
    void validateAddress_shouldReturnInvalid_forEmptyAddress() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validateAddressUseCase.validateAddress("", networkId, correlationId)
        );

        assertEquals("Address value must be provided", exception.getMessage());
    }

    @Test
    void validateAddress_shouldReturnInvalid_forNullAddress() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validateAddressUseCase.validateAddress(null, networkId, correlationId)
        );

        assertEquals("Address value must be provided", exception.getMessage());
    }

    @Test
    void validateAddress_shouldWorkWithoutNetwork_whenNetworkIdIsNull() {
        // Arrange
        String ethereumAddress = "0x742dB5C8A5d8c837e95C5fc73D3DCFFF84C8b742";

        // Act
        ValidateAddressUseCase.AddressValidationResult result =
            validateAddressUseCase.validateAddress(ethereumAddress, null, null);

        // Assert
        assertTrue(result.isValid());
        assertEquals("Ethereum", result.getFormat());
        assertEquals("Unknown", result.getNetwork());
        assertTrue(result.isNetworkCompatible()); // True when no network specified
        verify(networkRepository, never()).findById(any(), any());
    }

    @Test
    void validateAddress_shouldReturnIncompatible_whenAddressNotCompatibleWithNetwork() {
        // Arrange
        String bitcoinAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"; // Bitcoin address
        when(networkRepository.findById(eq(networkId), anyString())).thenReturn(Optional.of(testNetwork)); // Ethereum network

        // Act
        ValidateAddressUseCase.AddressValidationResult result =
            validateAddressUseCase.validateAddress(bitcoinAddress, networkId, correlationId);

        // Assert
        assertTrue(result.isValid()); // Address format is valid
        assertEquals("Bitcoin Legacy", result.getFormat());
        assertEquals("Ethereum", result.getNetwork());
        assertFalse(result.isNetworkCompatible()); // Not compatible with Ethereum
    }

    @Test
    void validateAddresses_shouldValidateMultipleAddresses() {
        // Arrange
        String[] addresses = {
            "0x742dB5C8A5d8c837e95C5fc73D3DCFFF84C8b742",
            "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa",
            "invalid-address"
        };
        when(networkRepository.findById(eq(networkId), anyString())).thenReturn(Optional.of(testNetwork));

        // Act
        ValidateAddressUseCase.AddressValidationResult[] results =
            validateAddressUseCase.validateAddresses(addresses, networkId, correlationId);

        // Assert
        assertEquals(3, results.length);
        
        // First address - valid Ethereum
        assertTrue(results[0].isValid());
        assertEquals("Ethereum", results[0].getFormat());
        assertTrue(results[0].isNetworkCompatible());
        
        // Second address - valid Bitcoin but incompatible with Ethereum network
        assertTrue(results[1].isValid());
        assertEquals("Bitcoin Legacy", results[1].getFormat());
        assertFalse(results[1].isNetworkCompatible());
        
        // Third address - invalid format
        assertFalse(results[2].isValid());
        assertEquals("Unknown", results[2].getFormat());
        assertFalse(results[2].isNetworkCompatible());
    }

    @Test
    void validateAddresses_shouldReturnEmpty_forNullInput() {
        // Act
        ValidateAddressUseCase.AddressValidationResult[] results =
            validateAddressUseCase.validateAddresses(null, networkId, correlationId);

        // Assert
        assertEquals(0, results.length);
    }
}