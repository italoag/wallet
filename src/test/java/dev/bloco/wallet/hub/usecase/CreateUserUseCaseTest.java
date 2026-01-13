// src/test/java/dev/bloco/wallet/hub/usecase/CreateUserUseCaseTest.java
package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.domain.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Create User Use Case Tests")
class CreateUserUseCaseTest {

  @Test
  @DisplayName("createUser creates user and publishes events")
  void createUser_shouldCreateUserSuccessfully() {
    // Arrange
    UserRepository userRepository = mock(UserRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateUserUseCase useCase = new CreateUserUseCase(userRepository, eventPublisher);

    String name = "John Doe";
    String email = "john.doe@example.com";
    String password = "Strong@123";
    String correlationId = "correlation-123";

    when(userRepository.existsByEmail(email)).thenReturn(false);

    // Act
    User createdUser = useCase.createUser(name, email, password, correlationId);

    // Assert
    assertNotNull(createdUser);
    assertEquals(name, createdUser.getName());
    assertEquals(email, createdUser.getEmail());
    assertFalse(createdUser.isEmailVerified());
    assertNotNull(createdUser.getEmailVerificationToken());

    verify(userRepository).save(createdUser);
    verify(eventPublisher, atLeastOnce()).publish(any());
  }

  @Test
  @DisplayName("createUser throws exception if email already exists")
  void createUser_shouldThrowExceptionIfEmailAlreadyExists() {
    // Arrange
    UserRepository userRepository = mock(UserRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateUserUseCase useCase = new CreateUserUseCase(userRepository, eventPublisher);

    String name = "Jane Doe";
    String email = "jane.doe@example.com";
    String password = "Secure#456";
    String correlationId = "correlation-456";

    when(userRepository.existsByEmail(email)).thenReturn(true);

    // Act & Assert
    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> useCase.createUser(name, email, password, correlationId));

    assertEquals("Email already exists: " + email, exception.getMessage());
    verify(userRepository, never()).save(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  @DisplayName("createUser throws exception if invalid email format")
  void createUser_shouldThrowExceptionIfInvalidEmailFormat() {
    // Arrange
    UserRepository userRepository = mock(UserRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateUserUseCase useCase = new CreateUserUseCase(userRepository, eventPublisher);

    String name = "Alice";
    String email = "invalid-email";
    String password = "Valid#789";
    String correlationId = "correlation-789";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.createUser(name, email, password, correlationId));

    assertEquals("Invalid email format", exception.getMessage());
    verify(userRepository, never()).save(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  @DisplayName("createUser throws exception if password is too weak")
  void createUser_shouldThrowExceptionIfPasswordIsTooWeak() {
    // Arrange
    UserRepository userRepository = mock(UserRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateUserUseCase useCase = new CreateUserUseCase(userRepository, eventPublisher);

    String name = "Bob";
    String email = "bob@example.com";
    String password = "weakpass";
    String correlationId = "correlation-890";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> useCase.createUser(name, email, password, correlationId));

    assertEquals("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character", exception.getMessage());
    verify(userRepository, never()).save(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  @DisplayName("createUser publishes events for domain events")
  void createUser_shouldPublishEventsForDomainEvents() {
    // Arrange
    UserRepository userRepository = mock(UserRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    CreateUserUseCase useCase = new CreateUserUseCase(userRepository, eventPublisher);

    String name = "Eve";
    String email = "eve@example.com";
    String password = "Secure@123";
    String correlationId = "correlation-321";

    when(userRepository.existsByEmail(email)).thenReturn(false);

    // Act
    User createdUser = useCase.createUser(name, email, password, correlationId);

    // Assert
    verify(eventPublisher, atLeastOnce()).publish(any());
    verify(userRepository).save(createdUser);
  }
}