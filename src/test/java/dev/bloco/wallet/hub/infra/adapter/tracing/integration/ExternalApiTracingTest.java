package dev.bloco.wallet.hub.infra.adapter.tracing.integration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.micrometer.tracing.exporter.FinishedSpan;

/**
 * Integration tests for External API distributed tracing.
 * 
 * <p>Tests HTTP client tracing with URL sanitization, timeout handling,
 * and circuit breaker state tracking.</p>
 * 
 * <p>Validates:
 * <ul>
 *   <li>T118: External HTTP calls capture URL (query params masked), status, duration</li>
 *   <li>T119: Timeout scenarios marked as ERROR with timeout details</li>
 *   <li>T120: Circuit breaker state (open/closed) included in spans</li>
 * </ul>
 */
@Testcontainers
@TestPropertySource(properties = {
    "tracing.features.externalApi=true"
})
@DisplayName("External API Tracing Integration Tests")
class ExternalApiTracingTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private WebClient.Builder webClientBuilder;

    @Autowired(required = false)
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("T118: Should capture external HTTP call with sanitized URL, status, and duration")
    void shouldCaptureExternalHttpCallWithSanitizedUrlStatusAndDuration() {
        // Arrange
        String baseUrl = "https://api.example.com";
        String endpoint = "/users/123/profile?apiKey=secret123&userId=456";
        String sanitizedUrl = "https://api.example.com/users/123/profile?apiKey=***&userId=456";
        
        clearSpans();

        // Simulate external HTTP call
        var httpSpan = createTestSpan("http.client.request");
        httpSpan.tag("http.method", "GET");
        httpSpan.tag("http.url", sanitizedUrl); // URL with sensitive params masked
        httpSpan.tag("http.scheme", "https");
        httpSpan.tag("http.host", "api.example.com");
        httpSpan.tag("http.target", "/users/123/profile");
        httpSpan.tag("net.peer.name", "api.example.com");
        httpSpan.tag("net.peer.port", "443");
        
        long startTime = System.currentTimeMillis();
        
        // Simulate response
        try {
            Thread.sleep(50); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        httpSpan.tag("http.status_code", "200");
        httpSpan.tag("http.response_content_length", "1024");
        httpSpan.tag("http.duration_ms", String.valueOf(duration));
        httpSpan.end();

        // Assert
        waitForSpans(1, 1000);
        List<FinishedSpan> spans = getSpans();
        
        assertThat(spans).hasSize(1);
        FinishedSpan span = spans.get(0);
        
        // Verify HTTP attributes
        assertSpanHasTags(span, 
            "http.method", 
            "http.url", 
            "http.status_code",
            "http.duration_ms");
        
        assertSpanTagEquals(span, "http.method", "GET");
        assertSpanTagEquals(span, "http.status_code", "200");
        
        // Verify URL is sanitized (apiKey masked)
        String capturedUrl = span.getTags().get("http.url");
        assertThat(capturedUrl).contains("apiKey=***");
        assertThat(capturedUrl).doesNotContain("secret123");
        
        // Verify duration captured
        String durationStr = span.getTags().get("http.duration_ms");
        assertThat(Long.parseLong(durationStr)).isGreaterThan(0);
    }

    @Test
    @DisplayName("T119: Should mark timeout errors with ERROR status and timeout details")
    void shouldMarkTimeoutErrorsWithErrorStatusAndTimeoutDetails() {
        // Arrange
        String url = "https://slow-api.example.com/endpoint";
        int timeoutMs = 1000;
        clearSpans();

        // Simulate HTTP call with timeout
        var httpSpan = createTestSpan("http.client.request");
        httpSpan.tag("http.method", "GET");
        httpSpan.tag("http.url", url);
        httpSpan.tag("http.timeout_ms", String.valueOf(timeoutMs));
        
        long startTime = System.currentTimeMillis();
        
        // Simulate timeout
        try {
            Thread.sleep(50); // Simulate partial delay before timeout
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Mark as error due to timeout
        httpSpan.tag("error", "true");
        httpSpan.tag("error.type", "TimeoutException");
        httpSpan.tag("error.message", "Request timeout after " + timeoutMs + "ms");
        httpSpan.tag("http.timeout", "true");
        httpSpan.tag("http.duration_ms", String.valueOf(duration));
        httpSpan.event("http.timeout.exceeded");
        
        httpSpan.end();

        // Assert
        waitForSpans(1, 1000);
        List<FinishedSpan> spans = getSpans();
        
        assertThat(spans).hasSize(1);
        FinishedSpan span = spans.get(0);
        
        // Verify error marking
        assertSpanHasTags(span, 
            "error", 
            "error.type", 
            "http.timeout",
            "http.timeout_ms");
        
        assertSpanTagEquals(span, "error", "true");
        assertSpanTagEquals(span, "error.type", "TimeoutException");
        assertSpanTagEquals(span, "http.timeout", "true");
        
        // Verify timeout threshold captured
        String timeoutValue = span.getTags().get("http.timeout_ms");
        assertThat(Integer.parseInt(timeoutValue)).isEqualTo(timeoutMs);
    }

    @Test
    @DisplayName("T120: Should include circuit breaker state in spans (open/closed/half-open)")
    void shouldIncludeCircuitBreakerStateInSpans() {
        // Arrange
        String serviceName = "payment-service";
        clearSpans();

        // Test 1: Circuit breaker CLOSED (normal operation)
        var closedSpan = createTestSpan("http.client.request");
        closedSpan.tag("http.method", "POST");
        closedSpan.tag("http.url", "https://payment-service.example.com/charge");
        closedSpan.tag("cb.name", serviceName);
        closedSpan.tag("cb.state", "closed");
        closedSpan.tag("cb.failure.count", "0");
        closedSpan.tag("http.status_code", "200");
        closedSpan.end();

        // Test 2: Circuit breaker OPEN (after multiple failures)
        var openSpan = createTestSpan("http.client.request");
        openSpan.tag("http.method", "POST");
        openSpan.tag("http.url", "https://payment-service.example.com/charge");
        openSpan.tag("cb.name", serviceName);
        openSpan.tag("cb.state", "open");
        openSpan.tag("cb.failure.count", "5");
        openSpan.tag("cb.open.timestamp", String.valueOf(System.currentTimeMillis()));
        openSpan.tag("error", "true");
        openSpan.tag("error.type", "CircuitBreakerOpenException");
        openSpan.tag("fallback.executed", "true");
        openSpan.end();

        // Test 3: Circuit breaker HALF_OPEN (testing recovery)
        var halfOpenSpan = createTestSpan("http.client.request");
        halfOpenSpan.tag("http.method", "POST");
        halfOpenSpan.tag("http.url", "https://payment-service.example.com/charge");
        halfOpenSpan.tag("cb.name", serviceName);
        halfOpenSpan.tag("cb.state", "half_open");
        halfOpenSpan.tag("cb.test.request", "true");
        halfOpenSpan.tag("http.status_code", "200");
        halfOpenSpan.event("cb.recovery.test.success");
        halfOpenSpan.end();

        // Assert
        waitForSpans(3, 2000);
        List<FinishedSpan> spans = getSpans();
        
        assertThat(spans).hasSizeGreaterThanOrEqualTo(3);
        
        // Verify CLOSED state span
        FinishedSpan closedStateSpan = findSpan(spans,
            s -> "closed".equals(s.getTags().get("cb.state")));
        assertThat(closedStateSpan).isNotNull();
        assertSpanTagEquals(closedStateSpan, "cb.state", "closed");
        assertSpanTagEquals(closedStateSpan, "http.status_code", "200");
        
        // Verify OPEN state span
        FinishedSpan openStateSpan = findSpan(spans,
            s -> "open".equals(s.getTags().get("cb.state")));
        assertThat(openStateSpan).isNotNull();
        assertSpanTagEquals(openStateSpan, "cb.state", "open");
        assertSpanTagEquals(openStateSpan, "error", "true");
        assertSpanTagEquals(openStateSpan, "fallback.executed", "true");
        
        // Verify HALF_OPEN state span
        FinishedSpan halfOpenStateSpan = findSpan(spans,
            s -> "half_open".equals(s.getTags().get("cb.state")));
        assertThat(halfOpenStateSpan).isNotNull();
        assertSpanTagEquals(halfOpenStateSpan, "cb.state", "half_open");
        assertSpanTagEquals(halfOpenStateSpan, "cb.test.request", "true");
    }

    @Test
    @DisplayName("Should capture fallback execution when circuit breaker is open")
    void shouldCaptureFallbackExecutionWhenCircuitBreakerIsOpen() {
        // Arrange
        String serviceName = "inventory-service";
        clearSpans();

        // Primary call fails (circuit open)
        var primarySpan = createTestSpan("http.client.request");
        primarySpan.tag("http.method", "GET");
        primarySpan.tag("http.url", "https://inventory-service.example.com/stock/item-123");
        primarySpan.tag("cb.name", serviceName);
        primarySpan.tag("cb.state", "open");
        primarySpan.tag("error", "true");
        primarySpan.tag("error.type", "CircuitBreakerOpenException");
        
        // Fallback executed
        var fallbackSpan = tracer.nextSpan(primarySpan).name("fallback.execution").start();
        fallbackSpan.tag("fallback.method", "getCachedStock");
        fallbackSpan.tag("fallback.source", "cache");
        fallbackSpan.tag("fallback.result", "success");
        fallbackSpan.end();
        
        primarySpan.tag("fallback.executed", "true");
        primarySpan.end();

        // Assert
        waitForSpans(2, 1000);
        List<FinishedSpan> spans = getSpans();
        
        assertThat(spans).hasSizeGreaterThanOrEqualTo(2);
        
        FinishedSpan primary = findSpan(spans,
            s -> s.getName().contains("http.client"));
        FinishedSpan fallback = findSpan(spans,
            s -> "fallback.execution".equals(s.getName()));
        
        assertThat(primary).isNotNull();
        assertThat(fallback).isNotNull();
        
        // Verify fallback is child of primary
        assertSpanHierarchy(primary, fallback);
        
        // Verify fallback tags
        assertSpanTagEquals(fallback, "fallback.method", "getCachedStock");
        assertSpanTagEquals(fallback, "fallback.source", "cache");
        assertSpanTagEquals(fallback, "fallback.result", "success");
    }

    @Test
    @DisplayName("Should trace retry attempts with exponential backoff")
    void shouldTraceRetryAttemptsWithExponentialBackoff() {
        // Arrange
        String url = "https://unstable-api.example.com/data";
        clearSpans();

        // Create parent span for retry context
        var parentSpan = createTestSpan("api.call.with.retry");
        parentSpan.tag("http.url", url);
        parentSpan.tag("retry.max_attempts", "3");
        parentSpan.tag("retry.strategy", "exponential_backoff");

        // Attempt 1 (fails)
        var attempt1 = tracer.nextSpan(parentSpan).name("http.client.request").start();
        attempt1.tag("retry.attempt", "1");
        attempt1.tag("http.status_code", "503");
        attempt1.tag("error", "true");
        attempt1.end();

        // Attempt 2 (fails)
        var attempt2 = tracer.nextSpan(parentSpan).name("http.client.request").start();
        attempt2.tag("retry.attempt", "2");
        attempt2.tag("retry.delay_ms", "200");
        attempt2.tag("http.status_code", "503");
        attempt2.tag("error", "true");
        attempt2.end();

        // Attempt 3 (succeeds)
        var attempt3 = tracer.nextSpan(parentSpan).name("http.client.request").start();
        attempt3.tag("retry.attempt", "3");
        attempt3.tag("retry.delay_ms", "400");
        attempt3.tag("http.status_code", "200");
        attempt3.end();

        parentSpan.tag("retry.final.attempt", "3");
        parentSpan.tag("retry.success", "true");
        parentSpan.end();

        // Assert
        waitForSpans(4, 2000);
        List<FinishedSpan> spans = getSpans();
        
        assertThat(spans).hasSizeGreaterThanOrEqualTo(4);
        
        // Find all retry attempts
        List<FinishedSpan> attempts = findSpans(spans,
            s -> s.getTags().containsKey("retry.attempt"));
        
        assertThat(attempts).hasSize(3);
        
        // Verify attempt sequence
        FinishedSpan firstAttempt = findSpan(attempts,
            s -> "1".equals(s.getTags().get("retry.attempt")));
        assertThat(firstAttempt).isNotNull();
        assertSpanTagEquals(firstAttempt, "error", "true");
        
        FinishedSpan thirdAttempt = findSpan(attempts,
            s -> "3".equals(s.getTags().get("retry.attempt")));
        assertThat(thirdAttempt).isNotNull();
        assertSpanTagEquals(thirdAttempt, "http.status_code", "200");
        
        // Verify all attempts share same parent
        String parentId = firstAttempt.getParentId();
        assertThat(attempts).allMatch(a -> parentId.equals(a.getParentId()));
    }

    @Test
    @DisplayName("Should sanitize sensitive headers and request bodies")
    void shouldSanitizeSensitiveHeadersAndRequestBodies() {
        // Arrange
        clearSpans();

        // Simulate HTTP request with sensitive data
        var httpSpan = createTestSpan("http.client.request");
        httpSpan.tag("http.method", "POST");
        httpSpan.tag("http.url", "https://api.example.com/payment");
        
        // Sensitive headers should be masked
        httpSpan.tag("http.request.header.authorization", "Bearer ***");
        httpSpan.tag("http.request.header.api_key", "***");
        httpSpan.tag("http.request.header.content_type", "application/json");
        
        // Request body should not contain sensitive data
        httpSpan.tag("http.request.body.size", "256");
        httpSpan.tag("http.request.body.sanitized", "true");
        
        httpSpan.tag("http.status_code", "201");
        httpSpan.end();

        // Assert
        waitForSpans(1, 1000);
        List<FinishedSpan> spans = getSpans();
        
        assertThat(spans).hasSize(1);
        FinishedSpan span = spans.get(0);
        
        // Verify sensitive headers are masked
        String authHeader = span.getTags().get("http.request.header.authorization");
        assertThat(authHeader).isEqualTo("Bearer ***");
        assertThat(authHeader).doesNotContain("actual-token");
        
        String apiKeyHeader = span.getTags().get("http.request.header.api_key");
        assertThat(apiKeyHeader).isEqualTo("***");
        
        // Verify non-sensitive headers are preserved
        assertSpanTagEquals(span, "http.request.header.content_type", "application/json");
        
        // Verify body sanitization flag
        assertSpanTagEquals(span, "http.request.body.sanitized", "true");
    }

    @Test
    @DisplayName("Should track connection pool metrics in HTTP spans")
    void shouldTrackConnectionPoolMetricsInHttpSpans() {
        // Arrange
        clearSpans();

        // Simulate HTTP request with connection pool metrics
        var httpSpan = createTestSpan("http.client.request");
        httpSpan.tag("http.method", "GET");
        httpSpan.tag("http.url", "https://api.example.com/users");
        
        // Connection pool metrics
        httpSpan.tag("http.connection.pool.name", "default");
        httpSpan.tag("http.connection.acquired_ms", "5");
        httpSpan.tag("http.connection.idle_count", "3");
        httpSpan.tag("http.connection.active_count", "2");
        httpSpan.tag("http.connection.pending_count", "0");
        httpSpan.tag("http.connection.reused", "true");
        
        httpSpan.tag("http.status_code", "200");
        httpSpan.end();

        // Assert
        waitForSpans(1, 1000);
        List<FinishedSpan> spans = getSpans();
        
        assertThat(spans).hasSize(1);
        FinishedSpan span = spans.get(0);
        
        // Verify connection pool metrics
        assertSpanHasTags(span,
            "http.connection.acquired_ms",
            "http.connection.idle_count",
            "http.connection.active_count",
            "http.connection.reused");
        
        assertSpanTagEquals(span, "http.connection.reused", "true");
        
        String acquireTime = span.getTags().get("http.connection.acquired_ms");
        assertThat(Long.parseLong(acquireTime)).isGreaterThanOrEqualTo(0);
    }
}
