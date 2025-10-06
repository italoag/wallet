package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.domain.gateway.UserSessionRepository;
import dev.bloco.wallet.hub.domain.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the DeactivateUserUseCase class.
 *
 *<p>These tests ensure that the DeactivateUserUseCase behaves as expected, both in successful cases
 * and when encountering various error conditions.
 */
@DisplayName("Deactivate User Use Case")
class DeactivateUserUseCaseTest {

  /**
   * Tests the DeactivateUserUseCase, responsible for:
   * - Validating input arguments.
   * - Fetching the user to be deactivated.
   * - Deactivating the user, updating repositories, invalidating sessions, and publishing events.
   */
  @Test
  @DisplayName("Deactivate user successfully")
  void deactivateUser_success() {
    // Arrange
    var userId = UUID.randomUUID();
    var reason = "Violation of terms";
    var correlationId = "test-correlation-id";

    User user = mock(User.class);

    UserRepository userRepository = mock(UserRepository.class);
    UserSessionRepository sessionRepository = mock(UserSessionRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(user.getDomainEvents()).thenReturn(Collections.emptyList());

    DeactivateUserUseCase useCase = new DeactivateUserUseCase(userRepository, sessionRepository, eventPublisher);

    // Act
    assertDoesNotThrow(() -> useCase.deactivateUser(userId, reason, correlationId));

    // Assert
    verify(userRepository).findById(userId);
    verify(user).deactivate();
    verify(userRepository).update(user);
    verify(sessionRepository).invalidateAllUserSessions(userId);
    verify(eventPublisher, never()).publish(any());
    verify(user).clearEvents();
  }

  /**
   * Tests the behavior of the `deactivateUser` method in `DeactivateUserUseCase` when the provided
   * user ID is null. This method validates that an `IllegalArgumentException` is thrown in such
   * a scenario.
   *<p/>
   * The test also verifies that no interactions occur with the `UserRepository`, `UserSessionRepository`,
   * or `DomainEventPublisher` components, as the method execution should terminate early due to the
   * invalid input.
   *<p/>
   * Test Steps:
   * - Arrange: Prepare test inputs including a null user ID, a valid reason, and a correlation ID.
   * - Act & Assert: Verify that invoking the `deactivateUser` method with a null user ID throws an
   *   `IllegalArgumentException`, and ensure that no interactions are made with the mocked components.
   */
  @Test
  @DisplayName("Throw exception when user ID is null")
  void deactivateUser_throwsExceptionWhenUserIdIsNull() {
    // Arrange
    var reason = "Violation of terms";
    var correlationId = "test-correlation-id";

    UserRepository userRepository = mock(UserRepository.class);
    UserSessionRepository sessionRepository = mock(UserSessionRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    DeactivateUserUseCase useCase = new DeactivateUserUseCase(userRepository, sessionRepository, eventPublisher);

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.deactivateUser(null, reason, correlationId));
    verifyNoInteractions(userRepository, sessionRepository, eventPublisher);
  }

  @Test
  @DisplayName("Throw exception when reason is null")
  void deactivateUser_throwsExceptionWhenReasonIsEmpty() {
    // Arrange
    var userId = UUID.randomUUID();
    var reason = " ";
    var correlationId = "test-correlation-id";

    UserRepository userRepository = mock(UserRepository.class);
    UserSessionRepository sessionRepository = mock(UserSessionRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    DeactivateUserUseCase useCase = new DeactivateUserUseCase(userRepository, sessionRepository, eventPublisher);

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.deactivateUser(userId, reason, correlationId));
    verifyNoInteractions(userRepository, sessionRepository, eventPublisher);
  }

  @Test
  @DisplayName("Throw exception when user not found")
  void deactivateUser_throwsExceptionWhenUserNotFound() {
    // Arrange
    var userId = UUID.randomUUID();
    var reason = "Violation of terms";
    var correlationId = "test-correlation-id";

    UserRepository userRepository = mock(UserRepository.class);
    UserSessionRepository sessionRepository = mock(UserSessionRepository.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    DeactivateUserUseCase useCase = new DeactivateUserUseCase(userRepository, sessionRepository, eventPublisher);

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.deactivateUser(userId, reason, correlationId));
    verify(userRepository).findById(userId);
    verifyNoInteractions(sessionRepository, eventPublisher);
  }

  @Test
  @DisplayName("Publish domain events")
  void deactivateUser_publishesDomainEvents() {
    // Arrange
    var userId = UUID.randomUUID();
    var reason = "Violation of terms";
    var correlationId = "test-correlation-id";

    User user = mock(User.class);
    DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    DomainEvent event = mock(DomainEvent.class);

    UserRepository userRepository = mock(UserRepository.class);
    UserSessionRepository sessionRepository = mock(UserSessionRepository.class);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(user.getDomainEvents()).thenReturn(Collections.singletonList(event));

    DeactivateUserUseCase useCase = new DeactivateUserUseCase(userRepository, sessionRepository, eventPublisher);

    // Act
    assertDoesNotThrow(() -> useCase.deactivateUser(userId, reason, correlationId));

    // Assert
    verify(userRepository).findById(userId);
    verify(user).deactivate();
    verify(userRepository).update(user);
    verify(sessionRepository).invalidateAllUserSessions(userId);
    verify(eventPublisher).publish(event);
    verify(user).clearEvents();
  }
}