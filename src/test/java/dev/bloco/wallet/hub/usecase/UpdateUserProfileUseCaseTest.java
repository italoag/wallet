package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.domain.model.user.User;
import dev.bloco.wallet.hub.domain.model.user.UserStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@DisplayName("Update User Profile Use Case Tests")
class UpdateUserProfileUseCaseTest {

  /**
   * Tests for the updateProfile method in UpdateUserProfileUseCase.
   * This method is responsible for updating a user's profile details, including their name and email,
   * while validating domain constraints (e.g., active user, valid email format, unique email).
   */

  @Test
  @DisplayName("Update user profile successfully")
  void testUpdateProfile_successfullyUpdatesUserProfile() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String updatedName = "Updated Name";
    String updatedEmail = "updatedemail@example.com";
    String correlationId = UUID.randomUUID().toString();

    User mockUser = mock(User.class);
    UserRepository userRepository = mock(UserRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
    when(mockUser.getEmail()).thenReturn("currentemail@example.com");
    when(mockUser.getStatus()).thenReturn(UserStatus.ACTIVE);
    when(userRepository.existsByEmail(updatedEmail)).thenReturn(false);

    UpdateUserProfileUseCase useCase = new UpdateUserProfileUseCase(userRepository, eventPublisher);

    // Act
    User result = useCase.updateProfile(userId, updatedName, updatedEmail, correlationId);

    // Assert
    verify(mockUser).validateOperationAllowed();
    verify(mockUser).updateProfile(updatedName, updatedEmail);
    verify(userRepository).update(mockUser);
    verify(eventPublisher, times(mockUser.getDomainEvents().size())).publish(any());
    verify(mockUser).clearEvents();
    Assertions.assertNotNull(result);
  }

  @Test
  @DisplayName("Update user profile throws exception when user is not active")
  void testUpdateProfile_throwsWhenUserIdIsNull() {
    // Arrange
    UpdateUserProfileUseCase useCase = new UpdateUserProfileUseCase(mock(UserRepository.class), mock(DomainEventPublisher.class));
    String correlationId = UUID.randomUUID().toString();

    // Act & Assert
    Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.updateProfile(null, "Name", "email@example.com", correlationId));
  }

  @Test
  @DisplayName("Update user profile throws exception when user is not active")
  void testUpdateProfile_throwsWhenUserDoesNotExist() {
    // Arrange
    UUID userId = UUID.randomUUID();
    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findById(userId)).thenReturn(Optional.empty());
    UpdateUserProfileUseCase useCase = new UpdateUserProfileUseCase(userRepository, mock(DomainEventPublisher.class));
    String correlationId = UUID.randomUUID().toString();

    // Act & Assert
    Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.updateProfile(userId, "Name", null, correlationId));
  }

  @Test
  @DisplayName("Update user profile throws exception when user is not active")
  void testUpdateProfile_throwsWhenNameIsEmpty() {
    // Arrange
    UUID userId = UUID.randomUUID();
    User mockUser = mock(User.class);
    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
    UpdateUserProfileUseCase useCase = new UpdateUserProfileUseCase(userRepository, mock(DomainEventPublisher.class));
    String correlationId = UUID.randomUUID().toString();

    // Act & Assert
    Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.updateProfile(userId, " ", "email@example.com", correlationId));
  }

  @Test
  @DisplayName("Update user profile throws exception when user is not active")
  void testUpdateProfile_throwsWhenEmailIsInvalid() {
    // Arrange
    UUID userId = UUID.randomUUID();
    User mockUser = mock(User.class);
    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
    UpdateUserProfileUseCase useCase = new UpdateUserProfileUseCase(userRepository, mock(DomainEventPublisher.class));
    String correlationId = UUID.randomUUID().toString();

    // Act & Assert
    Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.updateProfile(userId, "Name", "invalidemail", correlationId));
  }

  @Test
  @DisplayName("Update user profile throws exception when user is not active")
  void testUpdateProfile_throwsWhenEmailAlreadyExists() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String newEmail = "newemail@example.com";
    User mockUser = mock(User.class);
    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
    when(mockUser.getEmail()).thenReturn("oldemail@example.com");
    when(userRepository.existsByEmail(newEmail)).thenReturn(true);

    UpdateUserProfileUseCase useCase = new UpdateUserProfileUseCase(userRepository, mock(DomainEventPublisher.class));
    String correlationId = UUID.randomUUID().toString();

    // Act & Assert
    Assertions.assertThrows(IllegalStateException.class, () -> useCase.updateProfile(userId, "Name", newEmail, correlationId));
  }

  @Test
  @DisplayName("Update user profile does not update name if unchanged")
  void testUpdateProfile_doesNotUpdateEmailIfUnchanged() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String existingEmail = "email@example.com";
    User mockUser = mock(User.class);
    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
    when(mockUser.getEmail()).thenReturn(existingEmail);

    UpdateUserProfileUseCase useCase = new UpdateUserProfileUseCase(userRepository, mock(DomainEventPublisher.class));
    String correlationId = UUID.randomUUID().toString();

    // Act
    User result = useCase.updateProfile(userId, "New Name", existingEmail, correlationId);

    // Assert
    verify(userRepository, never()).existsByEmail(existingEmail);
    Assertions.assertNotNull(result);
  }
}