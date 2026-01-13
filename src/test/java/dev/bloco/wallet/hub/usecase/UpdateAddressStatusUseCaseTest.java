// src/test/java/dev/bloco/wallet/hub/usecase/UpdateAddressStatusUseCaseTest.java
package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.AddressStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("Update Address Status Use Case Tests")
class UpdateAddressStatusUseCaseTest {

  @Test
  @DisplayName("updateStatus updates address status and publishes event")
  void updateStatusShouldActivateAddressWhenNewStatusIsActive() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    UpdateAddressStatusUseCase useCase = new UpdateAddressStatusUseCase(addressRepository, eventPublisher);

    UUID addressId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();
    Address mockAddress = mock(Address.class);
    when(addressRepository.findById(addressId)).thenReturn(Optional.of(mockAddress));

    // Act
    Address result = useCase.updateStatus(addressId, AddressStatus.ACTIVE, correlationId);

    // Assert
    verify(mockAddress, times(1)).activate();
    verify(addressRepository, times(1)).update(mockAddress);
    verify(eventPublisher, times(mockAddress.getDomainEvents().size())).publish(any());
    assertEquals(mockAddress, result);
    verify(mockAddress, times(1)).clearEvents();
  }

  @Test
  @DisplayName("updateStatus updates address status to archived and publishes event")
  void updateStatusShouldArchiveAddressWhenNewStatusIsArchived() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    UpdateAddressStatusUseCase useCase = new UpdateAddressStatusUseCase(addressRepository, eventPublisher);

    UUID addressId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();
    Address mockAddress = mock(Address.class);
    when(addressRepository.findById(addressId)).thenReturn(Optional.of(mockAddress));

    // Act
    Address result = useCase.updateStatus(addressId, AddressStatus.ARCHIVED, correlationId);

    // Assert
    verify(mockAddress, times(1)).archive();
    verify(addressRepository, times(1)).update(mockAddress);
    verify(eventPublisher, times(mockAddress.getDomainEvents().size())).publish(any());
    assertEquals(mockAddress, result);
    verify(mockAddress, times(1)).clearEvents();
  }

  @Test
  @DisplayName("updateStatus throws exception when address is already archived")
  void updateStatusShouldThrowExceptionWhenAddressNotFound() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    UpdateAddressStatusUseCase useCase = new UpdateAddressStatusUseCase(addressRepository, eventPublisher);

    UUID addressId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();
    when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.updateStatus(addressId, AddressStatus.ACTIVE, correlationId)
    );
    assertEquals("Address not found with id: " + addressId, exception.getMessage());
  }

  @Test
  @DisplayName("updateStatus throws exception when address is already archived")
  void updateStatusShouldThrowExceptionWhenAddressIdIsNull() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    UpdateAddressStatusUseCase useCase = new UpdateAddressStatusUseCase(addressRepository, eventPublisher);

    String correlationId = UUID.randomUUID().toString();

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.updateStatus(null, AddressStatus.ACTIVE, correlationId)
    );
    assertEquals("Address ID must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("updateStatus throws exception when new status is null")
  void updateStatusShouldThrowExceptionWhenNewStatusIsNull() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    UpdateAddressStatusUseCase useCase = new UpdateAddressStatusUseCase(addressRepository, eventPublisher);

    UUID addressId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.updateStatus(addressId, null, correlationId)
    );
    assertEquals("New status must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("updateStatus throws exception when new status is not supported")
  void updateStatusShouldThrowExceptionForUnsupportedStatus() {
    // Arrange
    AddressRepository addressRepository = mock(AddressRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    UpdateAddressStatusUseCase useCase = new UpdateAddressStatusUseCase(addressRepository, eventPublisher);

    UUID addressId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();
    Address mockAddress = mock(Address.class);
    when(addressRepository.findById(addressId)).thenReturn(Optional.of(mockAddress));

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        useCase.updateStatus(addressId, null, correlationId)
    );
    assertEquals("New status must be provided", exception.getMessage());
  }
}