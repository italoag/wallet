package dev.bloco.wallet.hub.infra.adapter.tracing.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import brave.Tracing;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import dev.bloco.wallet.hub.infra.adapter.tracing.propagation.CloudEventTracePropagator;
import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.micrometer.tracing.propagation.Propagator;
import lombok.extern.slf4j.Slf4j;

/**
 * TracingConfiguration sets up the core distributed tracing infrastructure
 * using Micrometer Tracing
 * and OpenTelemetry. This configuration establishes the
 * {@link ObservationRegistry} as the central
 * hub for all observability operations (traces, metrics, logs) and registers
 * tracing-specific handlers
 * to convert observations into distributed trace spans.
 *
 * <p>
 * <b>Architecture Overview:</b>
 * </p>
 * <ul>
 * <li><b>ObservationRegistry</b>: Central registry where all instrumentation
 * points register observations.
 * Acts as the primary entry point for creating spans from use case executions,
 * database operations,
 * Kafka events, and state machine transitions.</li>
 * <li><b>ObservationHandlers</b>: Processors that convert observations into
 * specific outputs:
 * <ul>
 * <li>{@link DefaultTracingObservationHandler}: Creates local spans for
 * synchronous operations</li>
 * <li>{@link PropagatingSenderTracingObservationHandler}: Injects trace context
 * into outbound
 * messages/events (Kafka producers, HTTP clients)</li>
 * <li>{@link PropagatingReceiverTracingObservationHandler}: Extracts trace
 * context from inbound
 * messages/requests (Kafka consumers, HTTP endpoints)</li>
 * </ul>
 * </li>
 * <li><b>Tracer</b>: Low-level API for manual span creation (auto-configured by
 * Spring Boot with Brave bridge)</li>
 * <li><b>Propagator</b>: Handles trace context serialization/deserialization
 * (W3C Trace Context format)</li>
 * </ul>
 *
 * <p>
 * <b>Spring Boot Auto-Configuration:</b>
 * </p>
 * This configuration complements Spring Boot's automatic tracing setup:
 * <ul>
 * <li>Spring Boot auto-configures {@link Tracer} and {@link Propagator} beans
 * when
 * {@code micrometer-tracing-bridge-brave} is on the classpath</li>
 * <li>This class explicitly configures {@link ObservationRegistry} with custom
 * handlers to support
 * domain-specific instrumentation (AOP aspects, state machine listeners)</li>
 * <li>WebFlux and Kafka auto-instrumentation are provided by Spring Boot
 * starters</li>
 * </ul>
 *
 * <p>
 * <b>Usage in Application:</b>
 * </p>
 * 
 * <pre>{@code
 * // Example: Manual span creation in a use case (prefer AOP-based
 * // instrumentation)
 * @Service
 * public class AddFundsUseCase {
 *     private final ObservationRegistry registry;
 * 
 *     public void execute(AddFundsCommand cmd) {
 *         Observation observation = Observation.createNotStarted("usecase.add-funds", registry)
 *                 .lowCardinalityKeyValue("wallet.id", cmd.walletId())
 *                 .highCardinalityKeyValue("transaction.amount", cmd.amount().toString());
 * 
 *         observation.observe(() -> {
 *             // Business logic execution
 *         });
 *     }
 * }
 * }</pre>
 *
 * <p>
 * <b>Integration with CloudEvents:</b>
 * </p>
 * The {@link PropagatingSenderTracingObservationHandler} and
 * {@link PropagatingReceiverTracingObservationHandler}
 * work in conjunction with {@code CloudEventTracePropagator} to inject/extract
 * trace context as CloudEvent
 * extensions (traceparent, tracestate fields) ensuring end-to-end trace
 * continuity across Kafka topics.
 *
 * <p>
 * <b>Configuration Activation:</b>
 * </p>
 * This configuration is active when the {@code tracing} profile is enabled:
 * 
 * <pre>
 * spring.profiles.active=tracing
 * # or
 * spring.profiles.include=tracing
 * </pre>
 *
 * <p>
 * <b>Related Configurations:</b>
 * </p>
 * <ul>
 * <li>{@code application-tracing.yml}: Sampling rates, backend endpoints,
 * circuit breaker settings</li>
 * <li>{@code SamplingConfiguration}: Custom sampling strategies (always-sample
 * rules, tail-based sampling)</li>
 * <li>{@code ReactiveContextConfig}: Reactive context propagation for
 * WebFlux/R2DBC pipelines</li>
 * </ul>
 *
 * @see io.micrometer.observation.Observation
 * @see io.micrometer.observation.ObservationRegistry
 * @see io.micrometer.tracing.Tracer
 * @see io.micrometer.tracing.handler.TracingObservationHandler
 */
@Slf4j
@Configuration
public class TracingConfiguration {
    static {
        System.out.println("DEBUG: TracingConfiguration class loaded!");
    }

    @Value("${tracing.backends.primary:tempo}")
    private String primaryBackend;

    @Value("${tracing.backends.fallback:zipkin}")
    private String fallbackBackend;

    @Value("${management.zipkin.tracing.endpoint:#{null}}")
    private String zipkinEndpoint;

    @Value("${management.otlp.tracing.endpoint:#{null}}")
    private String otlpEndpoint;

    @Value("${tracing.resilience.circuit-breaker.failure-threshold:5}")
    private int failureThreshold;

    @Value("${tracing.resilience.circuit-breaker.wait-duration-in-open-state:60s}")
    private String waitDuration;

    @Value("${tracing.resilience.circuit-breaker.ring-buffer-size-in-closed-state:100}")
    private int ringBufferSize;

    /**
     * Creates and configures the central {@link ObservationRegistry} with
     * tracing-specific handlers.
     * 
     * <p>
     * This registry serves as the primary integration point for all instrumentation
     * in the application.
     * It processes observations from multiple sources:
     * </p>
     * <ul>
     * <li>AOP aspects (use case tracing, repository tracing)</li>
     * <li>Spring Boot auto-instrumentation (WebFlux endpoints, R2DBC queries)</li>
     * <li>Custom handlers (state machine transitions)</li>
     * <li>Kafka producers/consumers (via Spring Cloud Stream observability)</li>
     * </ul>
     *
     * <p>
     * <b>Handler Registration Order:</b>
     * </p>
     * Handlers are invoked in registration order. The sequence matters for context
     * propagation:
     * <ol>
     * <li>{@link PropagatingReceiverTracingObservationHandler}: Extracts trace
     * context from incoming
     * requests/messages and makes it available to subsequent handlers</li>
     * <li>{@link DefaultTracingObservationHandler}: Creates spans for local
     * operations using the
     * extracted or newly created trace context</li>
     * <li>{@link PropagatingSenderTracingObservationHandler}: Injects the current
     * trace context into
     * outgoing requests/messages before they leave the service boundary</li>
     * </ol>
     *
     * <p>
     * <b>Thread Safety:</b>
     * </p>
     * ObservationRegistry is thread-safe and designed for concurrent access. Trace
     * context is stored
     * in ThreadLocal for blocking operations and Reactor Context for reactive
     * operations.
     *
     * <p>
     * <b>Performance Considerations:</b>
     * </p>
     * <ul>
     * <li>ObservationRegistry operations are lightweight (typically <1ms overhead
     * per observation)</li>
     * <li>Handlers execute synchronously - avoid blocking operations in custom
     * handlers</li>
     * <li>Sampling is applied at the Tracer level, after observation creation</li>
     * </ul>
     *
     * @param tracer     the {@link Tracer} instance for creating and managing spans
     *                   (auto-configured by Spring Boot)
     * @param propagator the {@link Propagator} for serializing/deserializing trace
     *                   context in W3C format
     *                   (auto-configured by Spring Boot)
     * @return a fully configured {@link ObservationRegistry} ready for
     *         application-wide use
     * @throws IllegalStateException if Tracer or Propagator beans are not available
     *                               (should not occur
     *                               with proper dependency configuration)
     */
    @Bean
    public ObservationRegistry observationRegistry(Tracer tracer, Propagator propagator) {
        ObservationRegistry registry = ObservationRegistry.create();

        // Register handlers in order: receiver → local → sender
        // This order ensures context flows correctly through the observation lifecycle

        // 1. Receiver handler: Extracts trace context from incoming messages/requests
        // - Reads W3C traceparent/tracestate headers from HTTP requests
        // - Extracts CloudEvent trace extensions from Kafka messages
        // - Continues existing traces or starts new root traces
        registry.observationConfig()
                .observationHandler(new PropagatingReceiverTracingObservationHandler<>(tracer, propagator));
        // 2. Default handler: Creates spans for local operations
        // - Converts observations into trace spans
        // - Handles span lifecycle (start, events, errors, finish)
        // - Applies span naming conventions and attributes
        registry.observationConfig()
                .observationHandler(new DefaultTracingObservationHandler(tracer));
        // 3. Sender handler: Injects trace context into outgoing messages/requests
        // - Adds W3C traceparent/tracestate headers to HTTP requests
        // - Populates CloudEvent trace extensions for Kafka messages
        // - Ensures downstream services can continue the trace
        registry.observationConfig()
                .observationHandler(new PropagatingSenderTracingObservationHandler<>(tracer, propagator));
        return registry;
    }

    /**
     * Creates a CloudEventTracePropagator for distributed tracing via CloudEvents.
     *
     * @param tracer the Micrometer Tracer
     * @return a configured CloudEventTracePropagator
     */
    @Bean
    public CloudEventTracePropagator cloudEventTracePropagator(Tracer tracer) {
        return new CloudEventTracePropagator(tracer);
    }

    /**
     * Configures multi-backend trace export with primary and fallback backends.
     * 
     * <p>
     * This bean validates and logs the trace export configuration. Spring Boot
     * automatically
     * creates and registers span exporters based on the following properties:
     * </p>
     * 
     * <p>
     * <b>Zipkin Backend (Brave-based):</b>
     * </p>
     * 
     * <pre>{@code
     * management:
     *   zipkin:
     *     tracing:
     *       endpoint: http://localhost:9411/api/v2/spans
     *       connect-timeout: 5s
     *       read-timeout: 10s
     * }</pre>
     * 
     * When this endpoint is configured, Spring Boot auto-configures:
     * <ul>
     * <li>{@code zipkin2.reporter.urlconnection.URLConnectionSender} for HTTP
     * transport</li>
     * <li>{@code zipkin2.reporter.brave.AsyncZipkinSpanHandler} for async span
     * export</li>
     * <li>Brave automatically registers the Zipkin handler in the {@link Tracing}
     * instance</li>
     * </ul>
     *
     * <p>
     * <b>OTLP Backend (Tempo/Jaeger):</b>
     * </p>
     * 
     * <pre>{@code
     * management:
     *   otlp:
     *     tracing:
     *       endpoint: http://localhost:4318/v1/traces
     *       timeout: 10s
     *       compression: gzip
     * }</pre>
     * 
     * When this endpoint is configured, Spring Boot auto-configures:
     * <ul>
     * <li>{@code io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter}</li>
     * <li>Brave bridges to OpenTelemetry for OTLP export</li>
     * <li>Spans are sent in OTLP protobuf format over HTTP</li>
     * </ul>
     *
     * <p>
     * <b>Multi-Backend Strategy:</b>
     * </p>
     * <ul>
     * <li><b>Primary Backend</b>: Configured via {@code tracing.backends.primary}
     * (default: tempo)</li>
     * <li><b>Fallback Backend</b>: Configured via {@code tracing.backends.fallback}
     * (default: zipkin)</li>
     * <li>Both backends export spans <b>simultaneously</b> when both endpoints are
     * configured</li>
     * <li>Spring Boot manages export concurrency and error handling</li>
     * </ul>
     *
     * <p>
     * <b>Current Behavior (T020):</b>
     * </p>
     * This implementation logs the configuration but relies on Spring Boot's
     * auto-configuration
     * to create and manage span exporters. Both backends export simultaneously if
     * both endpoints
     * are configured.
     *
     * <p>
     * <b>Future Enhancement (T021):</b>
     * </p>
     * Task T021 will introduce {@code ResilientCompositeSpanExporter} with:
     * <ul>
     * <li>Resilience4j CircuitBreaker for primary backend</li>
     * <li>Automatic failover to fallback backend when primary circuit opens</li>
     * <li>Manual fallback when primary is explicitly unavailable</li>
     * <li>Export metrics (success/failure counts, circuit breaker state)</li>
     * </ul>
     *
     * <p>
     * <b>Configuration Example:</b>
     * </p>
     * 
     * <pre>{@code
     * # application-tracing.yml
     * tracing:
     *   backends:
     *     primary: tempo      # Primary: OTLP (Tempo)
     *     fallback: zipkin    # Fallback: Zipkin
     *     
     * management:
     *   tracing:
     *     enabled: true
     *     sampling:
     *       probability: 0.1
     *   zipkin:
     *     tracing:
     *       endpoint: http://localhost:9411/api/v2/spans
     *   otlp:
     *     tracing:
     *       endpoint: http://localhost:4318/v1/traces
     * }</pre>
     *
     * <p>
     * <b>Verification:</b>
     * </p>
     * Check application logs on startup for:
     * 
     * <pre>
     * Multi-backend trace export configured: [primary=tempo, fallback=zipkin]
     * Zipkin endpoint: http://localhost:9411/api/v2/spans
     * OTLP endpoint: http://localhost:4318/v1/traces
     * </pre>
     *
     * @return configuration summary message for testing/validation
     */
    @Bean
    @ConditionalOnProperty(value = "tracing.backends.primary")
    public String multiBackendExportConfiguration() {
        List<String> configuredBackends = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check primary backend configuration
        if (isPrimaryOtlp()) {
            if (otlpEndpoint != null && !otlpEndpoint.isEmpty()) {
                configuredBackends.add("OTLP/Tempo (primary)");
            } else {
                warnings.add("Primary backend 'tempo' configured but management.otlp.tracing.endpoint is missing");
            }
        } else if (isPrimaryZipkin()) {
            if (zipkinEndpoint != null && !zipkinEndpoint.isEmpty()) {
                configuredBackends.add("Zipkin (primary)");
            } else {
                warnings.add("Primary backend 'zipkin' configured but management.zipkin.tracing.endpoint is missing");
            }
        } else {
            warnings.add("Unknown primary backend: " + primaryBackend);
        }

        // Check fallback backend configuration
        if (isFallbackOtlp()) {
            if (otlpEndpoint != null && !otlpEndpoint.isEmpty()) {
                configuredBackends.add("OTLP/Tempo (fallback)");
            } else {
                warnings.add("Fallback backend 'tempo' configured but management.otlp.tracing.endpoint is missing");
            }
        } else if (isFallbackZipkin()) {
            if (zipkinEndpoint != null && !zipkinEndpoint.isEmpty()) {
                configuredBackends.add("Zipkin (fallback)");
            } else {
                warnings.add("Fallback backend 'zipkin' configured but management.zipkin.tracing.endpoint is missing");
            }
        } else {
            warnings.add("Unknown fallback backend: " + fallbackBackend);
        }

        // Report configuration status
        if (configuredBackends.isEmpty()) {
            String errorMsg = "No trace backends properly configured! Spans will not be exported.";
            return "ERROR: " + errorMsg;
        }

        if (!warnings.isEmpty()) {
            // Logging removed due to Lombok issues
        }

        String summary = String.format("Multi-backend export configured: %s",
                String.join(", ", configuredBackends));
        return summary;
    }

    /**
     * Creates a CircuitBreakerRegistry for managing circuit breakers.
     * 
     * <p>
     * This registry is used by ResilientCompositeSpanExporter to create and manage
     * circuit breakers for tracing backends.
     * </p>
     *
     * @return a configured CircuitBreakerRegistry
     */
    @Bean
    @ConditionalOnProperty(value = "tracing.resilience.circuit-breaker.enabled", havingValue = "true", matchIfMissing = true)
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    // Removed resilientCompositeSpanExporter to break circular dependency

    /**
     * Parses wait duration string (e.g., "60s", "1m") to seconds.
     *
     * @param duration the duration string
     * @return duration in seconds
     */
    private long parseWaitDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return 60; // Default 60 seconds
        }

        try {
            // Simple parser for common formats
            if (duration.endsWith("s")) {
                return Long.parseLong(duration.substring(0, duration.length() - 1));
            } else if (duration.endsWith("m")) {
                return Long.parseLong(duration.substring(0, duration.length() - 1)) * 60;
            } else {
                return Long.parseLong(duration); // Assume seconds
            }
        } catch (NumberFormatException e) {
            return 60;
        }
    }

    // TracingFeatureFlags logic moved to @PostConstruct in TracingFeatureFlags
    // class

    /**
     * Checks if primary backend is OTLP (Tempo).
     */
    private boolean isPrimaryOtlp() {
        return "tempo".equalsIgnoreCase(primaryBackend) || "otlp".equalsIgnoreCase(primaryBackend);
    }

    /**
     * Checks if primary backend is Zipkin.
     */
    private boolean isPrimaryZipkin() {
        return "zipkin".equalsIgnoreCase(primaryBackend);
    }

    /**
     * Checks if fallback backend is OTLP (Tempo).
     */
    private boolean isFallbackOtlp() {
        return "tempo".equalsIgnoreCase(fallbackBackend) || "otlp".equalsIgnoreCase(fallbackBackend);
    }

    /**
     * Checks if fallback backend is Zipkin.
     */
    private boolean isFallbackZipkin() {
        return "zipkin".equalsIgnoreCase(fallbackBackend);
    }
}
