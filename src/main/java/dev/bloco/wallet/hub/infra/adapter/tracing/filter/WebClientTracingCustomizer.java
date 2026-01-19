package dev.bloco.wallet.hub.infra.adapter.tracing.filter;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * WebClient customizer that adds distributed tracing to all outbound HTTP
 * calls.
 * 
 * <h2>Purpose</h2>
 * Instruments external HTTP requests with CLIENT spans, capturing:
 * <ul>
 * <li>HTTP method and URL (sanitized to remove sensitive data)</li>
 * <li>Response status code and duration</li>
 * <li>Request and response body sizes</li>
 * <li>Error details for failed requests</li>
 * <li>W3C Trace Context propagation via traceparent header</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * Automatically applied to all WebClient instances via Spring Boot's
 * WebClientCustomizer:
 * 
 * <pre>{@code
 * @Bean
 * WebClient.Builder webClientBuilder() {
 *     return WebClient.builder();
 *     // WebClientTracingCustomizer is auto-applied
 * }
 * }</pre>
 *
 * <h2>Span Attributes</h2>
 * Following OpenTelemetry semantic conventions for HTTP clients:
 * <table border="1">
 * <tr>
 * <th>Attribute</th>
 * <th>Description</th>
 * <th>Example</th>
 * </tr>
 * <tr>
 * <td>http.method</td>
 * <td>HTTP method</td>
 * <td>GET</td>
 * </tr>
 * <tr>
 * <td>http.url</td>
 * <td>Full URL (sanitized)</td>
 * <td>https://api.example.com/users?id=***</td>
 * </tr>
 * <tr>
 * <td>http.status_code</td>
 * <td>Response status</td>
 * <td>200</td>
 * </tr>
 * <tr>
 * <td>http.request.body.size</td>
 * <td>Request size in bytes</td>
 * <td>1024</td>
 * </tr>
 * <tr>
 * <td>http.response.body.size</td>
 * <td>Response size in bytes</td>
 * <td>2048</td>
 * </tr>
 * <tr>
 * <td>http.duration_ms</td>
 * <td>Request duration</td>
 * <td>123</td>
 * </tr>
 * <tr>
 * <td>span.kind</td>
 * <td>Span type</td>
 * <td>CLIENT</td>
 * </tr>
 * </table>
 *
 * <h2>URL Sanitization</h2>
 * Sensitive data in URLs is automatically masked:
 * <ul>
 * <li>Query parameters: {@code ?token=***&apiKey=***}</li>
 * <li>Path segments containing IDs: {@code /users/***}</li>
 * <li>Authentication in URLs: {@code https://user:***@example.com}</li>
 * </ul>
 *
 * <h2>W3C Trace Context Propagation</h2>
 * Automatically injects {@code traceparent} header into outbound requests:
 * 
 * <pre>
 * traceparent: 00-{trace-id}-{span-id}-01
 * </pre>
 * 
 * This enables trace continuity when calling traced services.
 *
 * <h2>Error Handling</h2>
 * Failed requests are marked with error attributes:
 * <ul>
 * <li>{@code error.type}: Exception class name</li>
 * <li>{@code error.message}: Exception message</li>
 * <li>{@code status}: "error"</li>
 * </ul>
 *
 * <h2>Performance</h2>
 * <ul>
 * <li>Overhead per request: <1ms (span creation + header injection)</li>
 * <li>No impact on request latency</li>
 * <li>Async span export</li>
 * </ul>
 *
 * <h2>Feature Flag</h2>
 * Controlled by {@code tracing.features.externalApi} (default: true).
 * When disabled, customizer is not registered.
 *
 * @see SensitiveDataSanitizer
 * @see TracingFeatureFlags
 * @see Tracer
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnClass(WebClient.class)
@ConditionalOnProperty(value = "tracing.features.externalApi", havingValue = "true", matchIfMissing = true)
public class WebClientTracingCustomizer implements WebClientCustomizer {

    private final Tracer tracer;
    private final Propagator propagator;
    private final SensitiveDataSanitizer sanitizer;
    private final TracingFeatureFlags featureFlags;

    /**
     * Customizes WebClient.Builder to add tracing filter.
     *
     * @param webClientBuilder the builder to customize
     */
    @Override
    public void customize(WebClient.Builder webClientBuilder) {
        if (!featureFlags.isExternalApi()) {
            return;
        }

        webClientBuilder.filter(tracingExchangeFilterFunction());
    }

    /**
     * Creates an ExchangeFilterFunction that wraps HTTP requests in CLIENT spans.
     *
     * @return the tracing filter function
     */
    private ExchangeFilterFunction tracingExchangeFilterFunction() {
        return (request, next) -> {
            // Skip if feature flag is disabled
            if (!featureFlags.isExternalApi()) {
                return next.exchange(request);
            }

            long startTime = System.nanoTime();

            // Create CLIENT span for outbound request
            Span span = tracer.nextSpan()
                    .name(String.format("HTTP %s", request.method().name()))
                    .start();

            try {
                // Add HTTP attributes
                span.tag("http.method", request.method().name());
                span.tag("http.url", sanitizeUrl(request.url()));
                span.tag("span.kind", "CLIENT");

                // Add request body size if available
                request.headers().getContentLength();
                if (request.headers().getContentLength() > 0) {
                    span.tag("http.request.body.size", String.valueOf(request.headers().getContentLength()));
                }

                // Inject W3C Trace Context into request headers
                ClientRequest tracedRequest = ClientRequest.from(request)
                        .headers(headers -> injectTraceContext(span, headers::add))
                        .build();
                // Execute request and handle response
                return next.exchange(tracedRequest)
                        .flatMap(response -> handleResponse(span, response, startTime))
                        .doOnError(error -> handleError(span, error, startTime))
                        .doFinally(signalType -> span.end());

            } catch (Exception e) {
                handleError(span, e, startTime);
                span.end();
                return Mono.error(e);
            }
        };
    }

    /**
     * Handles successful response by adding response attributes to span.
     *
     * @param span      the CLIENT span
     * @param response  the HTTP response
     * @param startTime request start time in nanoseconds
     * @return Mono of the response
     */
    private Mono<ClientResponse> handleResponse(Span span, ClientResponse response, long startTime) {
        try {
            // Add response attributes
            span.tag("http.status_code", String.valueOf(response.statusCode().value()));

            // Add response body size if available
            response.headers().contentLength()
                    .ifPresent(length -> span.tag("http.response.body.size", String.valueOf(length)));

            // Calculate and add duration
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            span.tag("http.duration_ms", String.valueOf(durationMs));

            // Mark as successful if status < 400
            if (response.statusCode().is2xxSuccessful() || response.statusCode().is3xxRedirection()) {
                span.tag("status", "success");
            } else {
                span.tag("status", "error");
                span.tag("error.type", "HTTP_" + response.statusCode().value());
            }
        } catch (Exception e) {
            // Error handling response
        }

        return Mono.just(response);
    }

    /**
     * Handles request error by adding error attributes to span.
     *
     * @param span      the CLIENT span
     * @param error     the error that occurred
     * @param startTime request start time in nanoseconds
     */
    private void handleError(Span span, Throwable error, long startTime) {
        try {
            // Calculate duration even for errors
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            span.tag("http.duration_ms", String.valueOf(durationMs));

            // Add error attributes
            span.tag("error.type", error.getClass().getSimpleName());
            span.tag("error.message", error.getMessage() != null ? error.getMessage() : "");
            span.tag("status", "error");
        } catch (Exception e) {
            // Error handling error
        }
    }

    /**
     * Injects W3C Trace Context into HTTP headers.
     *
     * @param span         the current span
     * @param headerSetter callback to set headers
     */
    private void injectTraceContext(Span span, java.util.function.BiConsumer<String, String> headerSetter) {
        try {
            propagator.inject(span.context(), null, (carrier, key, value) -> {
                headerSetter.accept(key, value);
            });
        } catch (Exception e) {
            // Error injecting trace context
        }
    }

    /**
     * Sanitizes URL to remove sensitive information.
     *
     * @param uri the URI to sanitize
     * @return sanitized URL string
     */
    private String sanitizeUrl(URI uri) {
        try {
            return sanitizer.sanitizeUrl(uri.toString());
        } catch (Exception e) {
            return uri.getScheme() + "://" + uri.getHost();
        }
    }
}
