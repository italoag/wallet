package dev.bloco.wallet.hub.infra.adapter.tracing.filter;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Decorator that adds distributed tracing attributes and events to circuit breaker operations.
 * 
 * <h2>Purpose</h2>
 * Enhances circuit breaker observability by:
 * <ul>
 *   <li>Adding circuit breaker state as span attributes (open/closed/half-open)</li>
 *   <li>Capturing circuit breaker name for identifying which breaker triggered</li>
 *   <li>Creating span events for state transitions (opened, closed, half-opened)</li>
 *   <li>Enabling correlation between circuit breaker behavior and trace failures</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * Automatically subscribes to all registered circuit breakers via {@link CircuitBreakerRegistry}:
 * <pre>{@code
 * circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
 *     cb.getEventPublisher()
 *       .onStateTransition(event -> addSpanEvent(event));
 * });
 * }</pre>
 *
 * <h2>Span Attributes</h2>
 * Added to active spans when circuit breaker events occur:
 * <table border="1">
 *   <tr><th>Attribute</th><th>Description</th><th>Values</th></tr>
 *   <tr><td>cb.name</td><td>Circuit breaker identifier</td><td>e.g., "trace-export-primary", "external-api-chainlist"</td></tr>
 *   <tr><td>cb.state</td><td>Current circuit breaker state</td><td>CLOSED, OPEN, HALF_OPEN, DISABLED, FORCED_OPEN</td></tr>
 *   <tr><td>cb.failure_rate</td><td>Failure rate percentage</td><td>0.0 to 100.0</td></tr>
 *   <tr><td>cb.slow_call_rate</td><td>Slow call rate percentage</td><td>0.0 to 100.0</td></tr>
 * </table>
 *
 * <h2>Span Events</h2>
 * Timeline events added to spans for circuit breaker state transitions:
 * <ul>
 *   <li><strong>cb.opened</strong>: Circuit breaker transitioned to OPEN (too many failures)</li>
 *   <li><strong>cb.closed</strong>: Circuit breaker transitioned to CLOSED (healthy again)</li>
 *   <li><strong>cb.half_open</strong>: Circuit breaker transitioned to HALF_OPEN (testing recovery)</li>
 *   <li><strong>cb.state_transition</strong>: Generic transition event with from/to states</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Identifying which external service caused request failure (open circuit)</li>
 *   <li>Correlating circuit breaker flapping with application errors</li>
 *   <li>Analyzing circuit breaker recovery patterns in distributed traces</li>
 *   <li>Debugging cascading failures across microservices</li>
 * </ul>
 *
 * <h2>Example Trace</h2>
 * When circuit breaker opens during external API call:
 * <pre>
 * Span: HTTP GET (CLIENT)
 *   Attributes:
 *     - http.method: GET
 *     - http.url: https://api.chainlist.org/chains
 *     - cb.name: external-api-chainlist
 *     - cb.state: OPEN
 *     - status: error
 *   Events:
 *     - cb.opened: Circuit breaker opened due to high failure rate (75%)
 * </pre>
 *
 * <h2>Performance</h2>
 * <ul>
 *   <li>Overhead per state transition: <0.1ms (span attribute update + event)</li>
 *   <li>Event subscription: One-time registration at startup</li>
 *   <li>No impact on circuit breaker decision-making</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * Thread-safe. Circuit breaker event publishers are thread-safe, and Micrometer Tracer
 * handles concurrent span access.
 *
 * <h2>Feature Flag</h2>
 * Controlled by {@code tracing.features.externalApi} (default: true).
 * When disabled, decorator does not subscribe to events.
 *
 * @see CircuitBreakerRegistry
 * @see Tracer
 * @see TracingFeatureFlags
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnClass(CircuitBreaker.class)
@ConditionalOnBean(CircuitBreakerRegistry.class)
@ConditionalOnProperty(value = "tracing.features.externalApi", havingValue = "true", matchIfMissing = true)
public class CircuitBreakerTracingDecorator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Tracer tracer;
    private final TracingFeatureFlags featureFlags;

    /**
     * Initializes circuit breaker event subscriptions after bean construction.
     * Subscribes to all registered circuit breakers for state transition events.
     */
    @PostConstruct
    public void init() {
        if (!featureFlags.isExternalApi()) {
            return;
        }

        // Subscribe to all circuit breaker state transitions
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            String cbName = circuitBreaker.getName();

            // Listen for state transitions
            circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    Span currentSpan = tracer.currentSpan();
                    if (currentSpan == null) {
                        return;
                    }

                    try {
                        // Add circuit breaker attributes to span
                        currentSpan.tag("cb.name", cbName);
                        currentSpan.tag("cb.state", event.getStateTransition().getToState().name());

                        // Add state transition event
                        String eventName = getEventName(event.getStateTransition().getToState());
                        String eventMessage = String.format(
                            "Circuit breaker state transition: %s → %s",
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()
                        );
                        currentSpan.event(eventName + ": " + eventMessage);
                    } catch (Exception e) {
                    }
                });

            // Listen for success events to capture metrics
            circuitBreaker.getEventPublisher()
                .onSuccess(event -> addMetricsToSpan(circuitBreaker, cbName));

            // Listen for error events to capture metrics
            circuitBreaker.getEventPublisher()
                .onError(event -> {
                    addMetricsToSpan(circuitBreaker, cbName);

                    Span currentSpan = tracer.currentSpan();
                    if (currentSpan != null) {
                        try {
                            currentSpan.tag("cb.name", cbName);
                            currentSpan.tag("cb.state", circuitBreaker.getState().name());
                            currentSpan.event("cb.error: " + event.getThrowable().getClass().getSimpleName());
                        } catch (Exception e) {
                        }
                    }
                });
        });
    }

    /**
     * Adds circuit breaker metrics to the current span.
     *
     * @param circuitBreaker the circuit breaker
     * @param cbName the circuit breaker name
     */
    private void addMetricsToSpan(CircuitBreaker circuitBreaker, String cbName) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return;
        }

        try {
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

            // Add current state and metrics as span attributes
            currentSpan.tag("cb.name", cbName);
            currentSpan.tag("cb.state", circuitBreaker.getState().name());
            currentSpan.tag("cb.failure_rate", String.format("%.2f", metrics.getFailureRate()));
            currentSpan.tag("cb.slow_call_rate", String.format("%.2f", metrics.getSlowCallRate()));

        } catch (Exception e) {
        }
    }

    /**
     * Maps circuit breaker state to span event name.
     *
     * @param state the circuit breaker state
     * @return event name for the state transition
     */
    private String getEventName(CircuitBreaker.State state) {
        return switch (state) {
            case OPEN -> "cb.opened";
            case CLOSED -> "cb.closed";
            case HALF_OPEN -> "cb.half_open";
            case DISABLED -> "cb.disabled";
            case FORCED_OPEN -> "cb.forced_open";
            default -> "cb.state_transition";
        };
    }
}
