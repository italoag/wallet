package dev.bloco.wallet.hub.infra.adapter.tracing.filter;

import io.micrometer.observation.Observation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Component for detecting slow database queries and adding diagnostic tags.
 * 
 * <h2>Purpose</h2>
 * Identifies queries that exceed performance thresholds and marks them with
 * slow_query tags for easier filtering and alerting in tracing backends.
 *
 * <h2>Detection Logic</h2>
 * <ul>
 *   <li>Threshold: Configurable (default 50ms)</li>
 *   <li>Applies to: JPA queries, R2DBC queries, transactions</li>
 *   <li>Tag added: {@code slow_query=true}</li>
 *   <li>Additional tag: {@code query.duration_ms} (actual duration)</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * Set threshold via application properties:
 * <pre>
 * tracing:
 *   sampling:
 *     slow-query-threshold-ms: 50  # Queries slower than 50ms are tagged
 * </pre>
 *
 * <h2>Integration</h2>
 * Called by:
 * <ul>
 *   <li>{@link dev.bloco.wallet.hub.infra.adapter.tracing.aspect.RepositoryTracingAspect}</li>
 *   <li>{@link dev.bloco.wallet.hub.infra.adapter.tracing.handler.R2dbcObservationHandler}</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Around("execution(* repository.*.*(..))")
 * public Object trace(ProceedingJoinPoint joinPoint) {
 *     long start = System.currentTimeMillis();
 *     Object result = joinPoint.proceed();
 *     long duration = System.currentTimeMillis() - start;
 *     
 *     Observation observation = createObservation();
 *     slowQueryDetector.detectAndTag(observation, duration);
 *     
 *     return result;
 * }
 * }</pre>
 *
 * <h2>Alerting</h2>
 * Slow queries can trigger alerts in observability platforms:
 * <pre>
 * Grafana Alert: count(traces{slow_query="true"}) > 100 in 5m
 * Tempo Query: {slow_query="true"} | latency > 50ms
 * </pre>
 *
 * <h2>Performance</h2>
 * <ul>
 *   <li>Detection overhead: <0.1ms (simple threshold comparison)</li>
 *   <li>No impact when queries are fast</li>
 *   <li>Tagging is zero-copy operation</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class SlowQueryDetector {

    private final long thresholdMs;

    /**
     * Constructs a SlowQueryDetector with configurable threshold.
     *
     * @param thresholdMs threshold in milliseconds (default: 50ms)
     */
    public SlowQueryDetector(
            @Value("${tracing.sampling.slow-query-threshold-ms:50}") long thresholdMs) {
        this.thresholdMs = thresholdMs;
        log.info("SlowQueryDetector initialized with threshold: {}ms", thresholdMs);
    }

    /**
     * Detects if a query is slow and adds diagnostic tags to the observation.
     *
     * @param observation the observation to tag
     * @param durationMs query duration in milliseconds
     * @return true if query is slow, false otherwise
     */
    public boolean detectAndTag(Observation observation, long durationMs) {
        if (observation == null) {
            return false;
        }

        boolean isSlow = durationMs > thresholdMs;
        
        if (isSlow) {
            observation.lowCardinalityKeyValue("slow_query", "true");
            observation.highCardinalityKeyValue("query.duration_ms", String.valueOf(durationMs));
            
            log.debug("Slow query detected: {}ms (threshold: {}ms, observation: {})",
                     durationMs, thresholdMs, observation.getContext().getName());
        }

        return isSlow;
    }

    /**
     * Detects if a query is slow using Duration.
     *
     * @param observation the observation to tag
     * @param duration query duration
     * @return true if query is slow, false otherwise
     */
    public boolean detectAndTag(Observation observation, Duration duration) {
        if (duration == null) {
            return false;
        }
        return detectAndTag(observation, duration.toMillis());
    }

    /**
     * Detects if a query is slow using nanosecond timing.
     *
     * @param observation the observation to tag
     * @param startNanos start time in nanoseconds
     * @param endNanos end time in nanoseconds
     * @return true if query is slow, false otherwise
     */
    public boolean detectAndTagNanos(Observation observation, long startNanos, long endNanos) {
        long durationMs = (endNanos - startNanos) / 1_000_000;
        return detectAndTag(observation, durationMs);
    }

    /**
     * Gets the configured slow query threshold.
     *
     * @return threshold in milliseconds
     */
    public long getThresholdMs() {
        return thresholdMs;
    }

    /**
     * Checks if a duration exceeds the threshold without tagging.
     *
     * @param durationMs duration in milliseconds
     * @return true if duration exceeds threshold
     */
    public boolean isSlow(long durationMs) {
        return durationMs > thresholdMs;
    }

    /**
     * Checks if a Duration exceeds the threshold without tagging.
     *
     * @param duration the duration to check
     * @return true if duration exceeds threshold
     */
    public boolean isSlow(Duration duration) {
        return duration != null && duration.toMillis() > thresholdMs;
    }
}
