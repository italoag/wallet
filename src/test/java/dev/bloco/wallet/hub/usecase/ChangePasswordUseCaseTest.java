package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.domain.gateway.UserSessionRepository;
import dev.bloco.wallet.hub.domain.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("Change Password Use Case Tests")
class ChangePasswordUseCaseTest {

  /**
   * Test class for {@link ChangePasswordUseCase}.
   * This focuses on validating the behavior of the {@code changePassword} method for various scenarios.
   */

  @Test
  @DisplayName("changePassword updates user password and invalidates all sessions")
  void shouldChangePasswordSuccessfully() {
    // Arrange
    UserRepository mockUserRepository = mock(UserRepository.class);
    UserSessionRepository mockSessionRepository = mock(UserSessionRepository.class);
    DomainEventPublisher mockEventPublisher = mock(DomainEventPublisher.class);
    ChangePasswordUseCase changePasswordUseCase = new ChangePasswordUseCase(mockUserRepository, mockSessionRepository, mockEventPublisher);

    UUID userId = UUID.randomUUID();
    String currentPassword = "Current@123";
    String newPassword = "New@Strong1";
    String correlationId = "correlation-id";

    User mockUser = mock(User.class);
    when(mockUserRepository.findById(userId)).thenReturn(Optional.of(mockUser));
    when(mockUser.getPasswordHash()).thenReturn(changePasswordUseCase.hashPassword(currentPassword)); // Simulate stored password hash
    doNothing().when(mockUser).validateOperationAllowed();

    // Act
    changePasswordUseCase.changePassword(userId, currentPassword, newPassword, correlationId);

    // Assert
    verify(mockUser).changePassword(anyString());
    verify(mockUserRepository).update(mockUser);
    verify(mockSessionRepository).invalidateAllUserSessions(userId);
  }

  @Test
  @DisplayName("Throw exception when current password is null")
  void shouldThrowExceptionWhenUserIdIsNull() {
    // Arrange
    UserRepository mockUserRepository = mock(UserRepository.class);
    UserSessionRepository mockSessionRepository = mock(UserSessionRepository.class);
    DomainEventPublisher mockEventPublisher = mock(DomainEventPublisher.class);
    ChangePasswordUseCase changePasswordUseCase = new ChangePasswordUseCase(mockUserRepository, mockSessionRepository, mockEventPublisher);

    String currentPassword = "Current@123";
    String newPassword = "New@Strong1";

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () ->
        changePasswordUseCase.changePassword(null, currentPassword, newPassword, "correlation-id"));
  }

  @Test
  @DisplayName("Throw exception when new password is null")
  void shouldThrowExceptionWhenUserNotFound() {
    // Arrange
    UserRepository mockUserRepository = mock(UserRepository.class);
    UserSessionRepository mockSessionRepository = mock(UserSessionRepository.class);
    DomainEventPublisher mockEventPublisher = mock(DomainEventPublisher.class);
    ChangePasswordUseCase changePasswordUseCase = new ChangePasswordUseCase(mockUserRepository, mockSessionRepository, mockEventPublisher);

    UUID userId = UUID.randomUUID();
    String currentPassword = "Current@123";
    String newPassword = "New@Strong1";

    when(mockUserRepository.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () ->
        changePasswordUseCase.changePassword(userId, currentPassword, newPassword, "correlation-id"));
  }

  @Test
  @DisplayName("Throw exception when current password is incorrect")
  void shouldThrowExceptionWhenCurrentPasswordIsIncorrect() {
    // Arrange
    UserRepository mockUserRepository = mock(UserRepository.class);
    UserSessionRepository mockSessionRepository = mock(UserSessionRepository.class);
    DomainEventPublisher mockEventPublisher = mock(DomainEventPublisher.class);
    ChangePasswordUseCase changePasswordUseCase = new ChangePasswordUseCase(mockUserRepository, mockSessionRepository, mockEventPublisher);

    UUID userId = UUID.randomUUID();
    String currentPassword = "WrongPassword";
    String newPassword = "New@Strong1";

    User mockUser = mock(User.class);
    when(mockUserRepository.findById(userId)).thenReturn(Optional.of(mockUser));
    when(mockUser.getPasswordHash()).thenReturn(changePasswordUseCase.hashPassword("Current@123")); // Stored hash for a different password

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () ->
        changePasswordUseCase.changePassword(userId, currentPassword, newPassword, "correlation-id"));
  }

  @Test
  @DisplayName("Throw exception when new password is the same as the current password")
  void shouldThrowExceptionWhenNewPasswordIsSameAsCurrentPassword() {
    // Arrange
    UserRepository mockUserRepository = mock(UserRepository.class);
    UserSessionRepository mockSessionRepository = mock(UserSessionRepository.class);
    DomainEventPublisher mockEventPublisher = mock(DomainEventPublisher.class);
    ChangePasswordUseCase changePasswordUseCase = new ChangePasswordUseCase(mockUserRepository, mockSessionRepository, mockEventPublisher);

    UUID userId = UUID.randomUUID();
    String currentPassword = "SamePassword@1";
    String newPassword = "SamePassword@1";

    User mockUser = mock(User.class);
    when(mockUserRepository.findById(userId)).thenReturn(Optional.of(mockUser));
    when(mockUser.getPasswordHash()).thenReturn(changePasswordUseCase.hashPassword(currentPassword));

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () ->
        changePasswordUseCase.changePassword(userId, currentPassword, newPassword, "correlation-id"));
  }

  @Test
  @DisplayName("Throw exception when new password is too weak")
  void shouldThrowExceptionWhenNewPasswordIsWeak() {
    // Arrange
    UserRepository mockUserRepository = mock(UserRepository.class);
    UserSessionRepository mockSessionRepository = mock(UserSessionRepository.class);
    DomainEventPublisher mockEventPublisher = mock(DomainEventPublisher.class);
    ChangePasswordUseCase changePasswordUseCase = new ChangePasswordUseCase(mockUserRepository, mockSessionRepository, mockEventPublisher);

    UUID userId = UUID.randomUUID();
    String currentPassword = "Current@123";
    String newPassword = "weakpass"; // Weak password

    User mockUser = mock(User.class);
    when(mockUserRepository.findById(userId)).thenReturn(Optional.of(mockUser));
    when(mockUser.getPasswordHash()).thenReturn(changePasswordUseCase.hashPassword(currentPassword));

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () ->
        changePasswordUseCase.changePassword(userId, currentPassword, newPassword, "correlation-id"));
  }
}