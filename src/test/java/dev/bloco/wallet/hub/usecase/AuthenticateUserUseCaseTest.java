// <llm-snippet-file>AuthenticateUserUseCaseTest.java</llm-snippet-file>
package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.user.UserAuthenticatedEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.domain.gateway.UserSessionRepository;
import dev.bloco.wallet.hub.domain.model.user.User;
import dev.bloco.wallet.hub.domain.model.user.UserSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Authenticate User Use Case Tests")
class AuthenticateUserUseCaseTest {

  @Test
  @DisplayName("authenticate returns authentication result")
  void givenValidCredentials_whenAuthenticate_thenReturnsAuthenticationResult() {
    // Arrange
    String email = "test@example.com";
    String password = "password123";
    String hashedPassword = "hashedPassword";
    String ipAddress = "127.0.0.1";
    String userAgent = "Test User Agent";
    String correlationId = UUID.randomUUID().toString();

    UUID userId = UUID.randomUUID();
    User user = mock(User.class);
    UserSession session = mock(UserSession.class);
    UserRepository userRepository = mock(UserRepository.class);
    UserSessionRepository sessionRepository = mock(UserSessionRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(user.getPasswordHash()).thenReturn(hashedPassword);
    when(user.getId()).thenReturn(userId);
    when(user.getName()).thenReturn("Test User");
    when(user.getEmail()).thenReturn(email);
    when(session.getSessionToken()).thenReturn("sessionToken");
    when(session.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
    when(user.isTwoFactorEnabled()).thenReturn(false);

    AuthenticateUserUseCase authenticateUserUseCase =
        Mockito.spy(new AuthenticateUserUseCase(userRepository, sessionRepository, eventPublisher));
    doReturn(true).when(authenticateUserUseCase).verifyPassword(password, hashedPassword);
    doReturn(session).when(authenticateUserUseCase).createSession(userId, ipAddress, userAgent);

    // Act
    AuthenticateUserUseCase.AuthenticationResult result = authenticateUserUseCase
        .authenticate(email, password, ipAddress, userAgent, correlationId);

    // Assert
    assertNotNull(result);
    assertEquals(userId, result.userId());
    assertEquals("Test User", result.name());
    assertEquals(email, result.email());
    assertEquals(session.getSessionToken(), result.sessionToken());
    assertNotNull(result.expiresAt());
    assertFalse(result.twoFactorEnabled());

    verify(userRepository).findByEmail(email);
    verify(userRepository).update(user);
    verify(sessionRepository).save(session);
    verify(eventPublisher, times(1)).publish(any(UserAuthenticatedEvent.class));
  }

  @Test
  @DisplayName("authenticate throws exception when user is not found")
  void givenNullEmail_whenAuthenticate_thenThrowsIllegalArgumentException() {
    // Arrange
    AuthenticateUserUseCase authenticateUserUseCase = new AuthenticateUserUseCase(
        mock(UserRepository.class),
        mock(UserSessionRepository.class),
        mock(DomainEventPublisher.class)
    );

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> authenticateUserUseCase.authenticate(null, "password", "127.0.0.1", "User Agent", UUID.randomUUID().toString()));

    assertEquals("Email must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("authenticate throws exception when password is not provided")
  void givenNullPassword_whenAuthenticate_thenThrowsIllegalArgumentException() {
    // Arrange
    AuthenticateUserUseCase authenticateUserUseCase = new AuthenticateUserUseCase(
        mock(UserRepository.class),
        mock(UserSessionRepository.class),
        mock(DomainEventPublisher.class)
    );

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> authenticateUserUseCase.authenticate("test@example.com", null, "127.0.0.1", "User Agent", UUID.randomUUID().toString()));

    assertEquals("Password must be provided", exception.getMessage());
  }

  @Test
  @DisplayName("authenticate throws exception when user is not found")
  void givenInvalidEmailOrPassword_whenAuthenticate_thenThrowsIllegalArgumentException() {
    // Arrange
    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

    AuthenticateUserUseCase authenticateUserUseCase = new AuthenticateUserUseCase(
        userRepository,
        mock(UserSessionRepository.class),
        mock(DomainEventPublisher.class)
    );

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> authenticateUserUseCase.authenticate("test@example.com", "wrongPassword", "127.0.0.1", "User Agent", UUID.randomUUID().toString()));

    assertEquals("Invalid email or password", exception.getMessage());
  }

  @Test
  @DisplayName("authenticate throws exception when user is locked")
  void givenLockedAccount_whenAuthenticate_thenThrowsIllegalStateException() {
    // Arrange
    User user = mock(User.class);
    when(user.isLocked()).thenReturn(true);

    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

    AuthenticateUserUseCase authenticateUserUseCase = new AuthenticateUserUseCase(
        userRepository,
        mock(UserSessionRepository.class),
        mock(DomainEventPublisher.class)
    );

    // Act & Assert
    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> authenticateUserUseCase.authenticate("test@example.com", "password123", "127.0.0.1", "User Agent", UUID.randomUUID().toString()));

    assertEquals("Account is temporarily locked due to failed login attempts", exception.getMessage());
  }
}