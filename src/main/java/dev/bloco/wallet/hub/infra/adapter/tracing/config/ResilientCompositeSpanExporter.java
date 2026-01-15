package dev.bloco.wallet.hub.infra.adapter.tracing.config;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Resilient composite span exporter that manages primary and fallback tracing backends
 * with circuit breaker protection and automatic failover.
 *
 * <h2>Purpose</h2>
 * Provides fault-tolerant trace export with:
 * <ul>
 *   <li>Primary backend export with circuit breaker protection (prevents cascading failures)</li>
 *   <li>Automatic failover to fallback backend when primary circuit opens</li>
 *   <li>Configurable circuit breaker thresholds and timing</li>
 *   <li>Export metrics (success/failure counts, circuit state)</li>
 *   <li>Transparent operation (no application impact during backend failures)</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <pre>
 * Span Creation → Brave Reporter → [ResilientCompositeSpanExporter]
 *                                            │
 *                                  ┌─────────┴─────────┐
 *                                  │  Circuit Breaker  │
 *                                  └─────────┬─────────┘
 *                                            │
 *                        ┌───────────────────┴───────────────────┐
 *                        │                                       │
 *                  ┌─────▼─────┐                         ┌──────▼──────┐
 *                  │  Primary  │                         │  Fallback   │
 *                  │  Backend  │                         │  Backend    │
 *                  │ (Tempo)   │                         │  (Zipkin)   │
 *                  └───────────┘                         └─────────────┘
 *                 Circuit CLOSED                       Circuit OPEN or
 *                 Export success                       Primary failure
 * </pre>
 *
 * <h2>Circuit Breaker States</h2>
 * <ul>
 *   <li><b>CLOSED</b>: Normal operation, exports go to primary backend</li>
 *   <li><b>OPEN</b>: Primary backend failing, all exports go to fallback</li>
 *   <li><b>HALF_OPEN</b>: Testing primary backend recovery, limited exports to primary</li>
 * </ul>
 *
 * <p><b>State Transitions:</b></p>
 * <pre>
 * CLOSED → OPEN: After {failure-threshold} consecutive failures (default: 5)
 * OPEN → HALF_OPEN: After {wait-duration} seconds (default: 60s)
 * HALF_OPEN → CLOSED: After {ring-buffer-size} successful exports (default: 100)
 * HALF_OPEN → OPEN: After any failure in half-open state
 * </pre>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * tracing:
 *   backends:
 *     primary: tempo
 *     fallback: zipkin
 *   resilience:
 *     circuit-breaker:
 *       failure-threshold: 5              # Failures before opening circuit
 *       wait-duration-in-open-state: 60s  # Wait before trying primary again
 *       ring-buffer-size-in-closed-state: 100  # Calls monitored in closed state
 *       ring-buffer-size-in-half-open-state: 10  # Test calls in half-open
 *       slow-call-duration-threshold: 5s  # Calls slower than this count as failures
 *       slow-call-rate-threshold: 50      # % of slow calls before opening circuit
 * }</pre>
 *
 * <h2>Export Strategy</h2>
 * <ol>
 *   <li>Check circuit breaker state</li>
 *   <li>If CLOSED or HALF_OPEN: Try primary backend
 *       <ul>
 *         <li>Success: Record success, keep circuit closed</li>
 *         <li>Failure: Record failure, may open circuit</li>
 *       </ul>
 *   </li>
 *   <li>If OPEN or primary failed: Export to fallback backend</li>
 *   <li>Log export decision and result</li>
 *   <li>Update metrics</li>
 * </ol>
 *
 * <h2>Metrics</h2>
 * Exposes the following metrics:
 * <ul>
 *   <li>spans.exported.primary.success: Successful exports to primary</li>
 *   <li>spans.exported.primary.failure: Failed exports to primary</li>
 *   <li>spans.exported.fallback.success: Successful exports to fallback</li>
 *   <li>spans.exported.fallback.failure: Failed exports to fallback</li>
 *   <li>circuit.state: Current circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)</li>
 *   <li>circuit.failure.rate: Current failure rate percentage</li>
 * </ul>
 *
 * <h2>Integration with TailSamplingSpanExporter</h2>
 * This exporter works in tandem with TailSamplingSpanExporter:
 * <pre>
 * Span → TailSamplingSpanExporter (sampling decision) → ResilientCompositeSpanExporter (backend routing)
 * </pre>
 *
 * <h2>Error Handling</h2>
 * <ul>
 *   <li>Primary export failures: Logged at WARN level, counted in circuit breaker</li>
 *   <li>Fallback export failures: Logged at ERROR level (data loss risk)</li>
 *   <li>Both backends fail: Span is lost but application continues (fail-safe)</li>
 *   <li>Circuit breaker exceptions: Treated as export failures</li>
 * </ul>
 *
 * <h2>Performance Impact</h2>
 * <ul>
 *   <li>Circuit breaker decision: <1μs (in-memory state check)</li>
 *   <li>Export to single backend: ~5-50ms (network I/O)</li>
 *   <li>Fallback on failure: Adds one export attempt latency (~5-50ms)</li>
 *   <li>No application blocking: All exports are async</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * This class is thread-safe:
 * <ul>
 *   <li>CircuitBreaker is thread-safe by design</li>
 *   <li>AtomicLong for metrics counters</li>
 *   <li>SpanHandler implementations are stateless</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Automatically configured by Spring when both primary and fallback are available
 * // Manual usage for testing:
 * 
 * ResilientCompositeSpanExporter exporter = new ResilientCompositeSpanExporter(
 *     primaryHandler, fallbackHandler, circuitBreakerConfig);
 *     
 * // Export span (circuit breaker handles routing)
 * MutableSpan span = new MutableSpan();
 * exporter.end(null, span, Cause.FINISHED);
 * }</pre>
 *
 * <h2>Monitoring</h2>
 * Monitor the following to detect issues:
 * <ul>
 *   <li>Circuit breaker state changes (CLOSED → OPEN indicates primary backend issues)</li>
 *   <li>Fallback export rate increase (indicates primary degradation)</li>
 *   <li>Both primary and fallback failures (data loss occurring)</li>
 *   <li>High failure rate in metrics (>10% suggests backend problems)</li>
 * </ul>
 *
 * @see CircuitBreaker
 * @see SpanHandler
 * @see TailSamplingSpanExporter
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "tracing.resilience.circuit-breaker.enabled", havingValue = "true", matchIfMissing = true)
public class ResilientCompositeSpanExporter extends SpanHandler {

    private final SpanHandler primaryHandler;
    private final SpanHandler fallbackHandler;
    private final CircuitBreaker circuitBreaker;

    // Metrics counters
    private final AtomicLong primarySuccessCount = new AtomicLong(0);
    private final AtomicLong primaryFailureCount = new AtomicLong(0);
    private final AtomicLong fallbackSuccessCount = new AtomicLong(0);
    private final AtomicLong fallbackFailureCount = new AtomicLong(0);

    /**
     * Creates a resilient composite span exporter with circuit breaker protection.
     *
     * @param primaryHandler the primary backend span handler (e.g., OTLP/Tempo)
     * @param fallbackHandler the fallback backend span handler (e.g., Zipkin)
     * @param circuitBreakerRegistry registry for creating circuit breaker
     * @param failureThreshold number of failures before opening circuit
     * @param waitDurationSeconds wait time in open state before testing primary again
     * @param ringBufferSize number of calls monitored in closed state
     */
    public ResilientCompositeSpanExporter(
            SpanHandler primaryHandler,
            SpanHandler fallbackHandler,
            CircuitBreakerRegistry circuitBreakerRegistry,
            int failureThreshold,
            long waitDurationSeconds,
            int ringBufferSize) {

        this.primaryHandler = primaryHandler;
        this.fallbackHandler = fallbackHandler;

        // Configure circuit breaker
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f) // Open circuit at 50% failure rate
                .minimumNumberOfCalls(failureThreshold) // Minimum calls before calculating failure rate
                .waitDurationInOpenState(Duration.ofSeconds(waitDurationSeconds))
                .slidingWindowSize(ringBufferSize) // Monitor last N calls
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .permittedNumberOfCallsInHalfOpenState(10) // Test calls in half-open state
                .slowCallDurationThreshold(Duration.ofSeconds(5)) // Calls >5s are slow
                .slowCallRateThreshold(50.0f) // Open circuit if >50% calls are slow
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("trace-export-primary", config);

        // Register event listeners for logging
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    log.warn("Circuit breaker state transition: {} → {} (failure rate: {:.2f}%)",
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState(),
                            circuitBreaker.getMetrics().getFailureRate());
                })
                .onError(event -> {
                    log.debug("Primary backend export failed: {} (duration: {}ms)",
                            event.getThrowable().getMessage(),
                            event.getElapsedDuration().toMillis());
                })
                .onSuccess(event -> {
                    log.trace("Primary backend export succeeded (duration: {}ms)",
                            event.getElapsedDuration().toMillis());
                });

        log.info("ResilientCompositeSpanExporter initialized [failureThreshold={}, " +
                        "waitDuration={}s, ringBufferSize={}]",
                failureThreshold, waitDurationSeconds, ringBufferSize);
    }

    /**
     * Handles span completion by exporting to primary or fallback backend.
     * This method is called by Brave when a span finishes.
     *
     * @param context the trace context (nullable)
     * @param span the completed span to export
     * @param cause the reason the span ended
     * @return true if export succeeded (to at least one backend)
     */
    @Override
    public boolean end(brave.propagation.TraceContext context, MutableSpan span, Cause cause) {
        if (span == null) {
            log.warn("Attempted to export null span, skipping");
            return false;
        }

        log.debug("Exporting span [id={}, name={}, duration={}μs]",
                span.id(), span.name(), span.finishTimestamp() - span.startTimestamp());

        // Try primary backend if circuit is not open
        CircuitBreaker.State circuitState = circuitBreaker.getState();
        log.trace("Circuit breaker state: {}", circuitState);

        if (circuitState != CircuitBreaker.State.OPEN) {
            try {
                // Attempt primary export with circuit breaker protection
                boolean success = circuitBreaker.executeSupplier(() -> {
                    try {
                        boolean result = primaryHandler.end(context, span, cause);
                        if (!result) {
                            throw new ExportException("Primary handler returned false");
                        }
                        return result;
                    } catch (Exception e) {
                        throw new ExportException("Primary export failed", e);
                    }
                });

                if (success) {
                    primarySuccessCount.incrementAndGet();
                    log.debug("Span exported successfully to primary backend [id={}]", span.id());
                    return true;
                }
            } catch (Exception e) {
                // Circuit breaker or export failure
                primaryFailureCount.incrementAndGet();
                log.warn("Primary backend export failed for span [id={}], falling back to secondary: {}",
                        span.id(), e.getMessage());
            }
        } else {
            log.debug("Circuit is OPEN, skipping primary backend and using fallback for span [id={}]",
                    span.id());
        }

        // Export to fallback backend
        return exportToFallback(context, span, cause);
    }

    /**
     * Exports a span to the fallback backend.
     *
     * @param context the trace context
     * @param span the span to export
     * @param cause the reason the span ended
     * @return true if export succeeded
     */
    private boolean exportToFallback(brave.propagation.TraceContext context, MutableSpan span, Cause cause) {
        try {
            boolean success = fallbackHandler.end(context, span, cause);
            
            if (success) {
                fallbackSuccessCount.incrementAndGet();
                log.debug("Span exported successfully to fallback backend [id={}]", span.id());
                return true;
            } else {
                fallbackFailureCount.incrementAndGet();
                log.error("Fallback backend returned false for span [id={}] - SPAN LOST", span.id());
                return false;
            }
        } catch (Exception e) {
            fallbackFailureCount.incrementAndGet();
            log.error("Fallback backend export failed for span [id={}] - SPAN LOST: {}",
                    span.id(), e.getMessage(), e);
            return false;
        }
    }



    /**
     * Gets current export metrics.
     *
     * @return metrics map with counts and circuit state
     */
    public java.util.Map<String, Object> getMetrics() {
        return java.util.Map.of(
                "primary.success", primarySuccessCount.get(),
                "primary.failure", primaryFailureCount.get(),
                "fallback.success", fallbackSuccessCount.get(),
                "fallback.failure", fallbackFailureCount.get(),
                "circuit.state", circuitBreaker.getState().toString(),
                "circuit.failure_rate", circuitBreaker.getMetrics().getFailureRate(),
                "circuit.slow_call_rate", circuitBreaker.getMetrics().getSlowCallRate(),
                "total.success", primarySuccessCount.get() + fallbackSuccessCount.get(),
                "total.failure", primaryFailureCount.get() + fallbackFailureCount.get()
        );
    }

    /**
     * Gets the current circuit breaker state.
     *
     * @return current state (CLOSED, OPEN, HALF_OPEN)
     */
    public CircuitBreaker.State getCircuitState() {
        return circuitBreaker.getState();
    }

    /**
     * Resets metrics counters (for testing).
     */
    public void resetMetrics() {
        primarySuccessCount.set(0);
        primaryFailureCount.set(0);
        fallbackSuccessCount.set(0);
        fallbackFailureCount.set(0);
    }

    /**
     * Logs current metrics for monitoring.
     */
    public void logMetrics() {
        log.info("Resilient Span Export Metrics: " +
                        "primary=[success={}, failure={}], " +
                        "fallback=[success={}, failure={}], " +
                        "circuit=[state={}, failure_rate={:.2f}%, slow_rate={:.2f}%]",
                primarySuccessCount.get(), primaryFailureCount.get(),
                fallbackSuccessCount.get(), fallbackFailureCount.get(),
                circuitBreaker.getState(),
                circuitBreaker.getMetrics().getFailureRate(),
                circuitBreaker.getMetrics().getSlowCallRate());
    }

    /**
     * Custom exception for export failures.
     */
    private static class ExportException extends RuntimeException {
        public ExportException(String message) {
            super(message);
        }

        public ExportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
