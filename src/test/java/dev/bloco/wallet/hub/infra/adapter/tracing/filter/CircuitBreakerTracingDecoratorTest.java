package dev.bloco.wallet.hub.infra.adapter.tracing.filter;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CircuitBreakerTracingDecorator}.
 * 
 * <h2>Test Strategy</h2>
 * <p>Since Resilience4j's event publisher uses lambdas that are difficult to test with mocks,
 * these tests focus on:</p>
 * <ul>
 *   <li>Verifying event subscription happens during init()</li>
 *   <li>Testing feature flag behavior</li>
 *   <li>Validating that event publisher methods are called correctly</li>
 * </ul>
 * 
 * <p>Note: Full integration tests with actual circuit breaker state transitions are deferred
 * to the integration test phase (T118-T120) where real circuit breakers will be tested.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CircuitBreakerTracingDecorator Unit Tests")
class CircuitBreakerTracingDecoratorTest {

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private Tracer tracer;

    @Mock
    private TracingFeatureFlags featureFlags;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private CircuitBreaker.EventPublisher eventPublisher;

    @Mock
    private CircuitBreaker.Metrics metrics;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    private CircuitBreakerTracingDecorator decorator;

    @BeforeEach
    void setUp() {
        decorator = new CircuitBreakerTracingDecorator(circuitBreakerRegistry, tracer, featureFlags);

        // Default mock behavior
        lenient().when(featureFlags.isExternalApi()).thenReturn(true);
        lenient().when(circuitBreaker.getName()).thenReturn("test-circuit-breaker");
        lenient().when(circuitBreaker.getEventPublisher()).thenReturn(eventPublisher);
        lenient().when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);
        lenient().when(circuitBreaker.getMetrics()).thenReturn(metrics);
        lenient().when(metrics.getFailureRate()).thenReturn(25.5f);
        lenient().when(metrics.getSlowCallRate()).thenReturn(10.0f);
        lenient().when(tracer.currentSpan()).thenReturn(span);
        lenient().when(span.context()).thenReturn(traceContext);
        lenient().when(traceContext.traceId()).thenReturn("test-trace-id");
        lenient().when(eventPublisher.onStateTransition(any())).thenReturn(eventPublisher);
        lenient().when(eventPublisher.onSuccess(any())).thenReturn(eventPublisher);
        lenient().when(eventPublisher.onError(any())).thenReturn(eventPublisher);
    }

    @Test
    @DisplayName("Should subscribe to circuit breaker events on init")
    void shouldSubscribeToCircuitBreakerEventsOnInit() {
        // Arrange
        when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(java.util.Collections.singleton(circuitBreaker));

        // Act
        decorator.init();

        // Assert - getEventPublisher called once per event type (3 times total: state, success, error)
        verify(circuitBreaker, atLeastOnce()).getEventPublisher();
        verify(eventPublisher, times(1)).onStateTransition(any());
        verify(eventPublisher, times(1)).onSuccess(any());
        verify(eventPublisher, times(1)).onError(any());
    }

    @Test
    @DisplayName("Should not subscribe to events when feature flag is disabled")
    void shouldNotSubscribeWhenFeatureFlagDisabled() {
        // Arrange
        lenient().when(featureFlags.isExternalApi()).thenReturn(false);
        lenient().when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(java.util.Collections.singleton(circuitBreaker));

        // Act
        decorator.init();

        // Assert - should not access event publisher
        verify(circuitBreaker, never()).getEventPublisher();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Should subscribe to multiple circuit breakers")
    void shouldSubscribeToMultipleCircuitBreakers() {
        // Arrange
        CircuitBreaker cb1 = mock(CircuitBreaker.class);
        CircuitBreaker cb2 = mock(CircuitBreaker.class);
        CircuitBreaker.EventPublisher ep1 = mock(CircuitBreaker.EventPublisher.class);
        CircuitBreaker.EventPublisher ep2 = mock(CircuitBreaker.EventPublisher.class);

        when(cb1.getName()).thenReturn("cb-1");
        when(cb2.getName()).thenReturn("cb-2");
        when(cb1.getEventPublisher()).thenReturn(ep1);
        when(cb2.getEventPublisher()).thenReturn(ep2);
        lenient().when(ep1.onStateTransition(any())).thenReturn(ep1);
        lenient().when(ep1.onSuccess(any())).thenReturn(ep1);
        lenient().when(ep1.onError(any())).thenReturn(ep1);
        lenient().when(ep2.onStateTransition(any())).thenReturn(ep2);
        lenient().when(ep2.onSuccess(any())).thenReturn(ep2);
        lenient().when(ep2.onError(any())).thenReturn(ep2);

        when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(java.util.Set.of(cb1, cb2));

        // Act
        decorator.init();

        // Assert - getEventPublisher called multiple times per circuit breaker
        verify(cb1, atLeastOnce()).getEventPublisher();
        verify(cb2, atLeastOnce()).getEventPublisher();
        verify(ep1, times(1)).onStateTransition(any());
        verify(ep2, times(1)).onStateTransition(any());
    }

    @Test
    @DisplayName("Should handle empty circuit breaker registry")
    void shouldHandleEmptyCircuitBreakerRegistry() {
        // Arrange
        when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(java.util.Collections.emptySet());

        // Act & Assert - should not throw exception
        decorator.init();

        // No circuit breakers to verify
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Should successfully initialize with real circuit breaker name")
    void shouldInitializeWithRealCircuitBreakerName() {
        // Arrange
        when(circuitBreaker.getName()).thenReturn("external-api-chainlist");
        when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(java.util.Collections.singleton(circuitBreaker));

        // Act
        decorator.init();

        // Assert
        verify(circuitBreaker, atLeastOnce()).getName();
        verify(eventPublisher, times(1)).onStateTransition(any());
    }

    @Test
    @DisplayName("Should return event publisher for chaining")
    void shouldReturnEventPublisherForChaining() {
        // Arrange
        when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(java.util.Collections.singleton(circuitBreaker));
        when(eventPublisher.onStateTransition(any())).thenReturn(eventPublisher);
        when(eventPublisher.onSuccess(any())).thenReturn(eventPublisher);
        when(eventPublisher.onError(any())).thenReturn(eventPublisher);

        // Act
        decorator.init();

        // Assert - verify fluent API chaining
        verify(eventPublisher).onStateTransition(any());
        verify(eventPublisher).onSuccess(any());
        verify(eventPublisher).onError(any());
    }
}
