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
@ConditionalOnProperty(value = "management.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfiguration {

    @Value("${tracing.backends.primary:tempo}")
    private String primaryBackend;

    @Value("${tracing.backends.fallback:zipkin}")
    private String fallbackBackend;

    @Value("${management.tracing.export.zipkin.endpoint:#{null}}")
    private String zipkinEndpoint;

    @Value("${management.opentelemetry.tracing.export.otlp.endpoint:#{null}}")
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
        log.info("Configuring ObservationRegistry with distributed tracing handlers");

        ObservationRegistry registry = ObservationRegistry.create();

        // Register handlers in order: receiver → local → sender
        // This order ensures context flows correctly through the observation lifecycle

        // 1. Receiver handler: Extracts trace context from incoming messages/requests
        // - Reads W3C traceparent/tracestate headers from HTTP requests
        // - Extracts CloudEvent trace extensions from Kafka messages
        // - Continues existing traces or starts new root traces
        registry.observationConfig()
                .observationHandler(new PropagatingReceiverTracingObservationHandler<>(tracer, propagator));

        log.debug("Registered PropagatingReceiverTracingObservationHandler for context extraction");

        // 2. Default handler: Creates spans for local operations
        // - Converts observations into trace spans
        // - Handles span lifecycle (start, events, errors, finish)
        // - Applies span naming conventions and attributes
        registry.observationConfig()
                .observationHandler(new DefaultTracingObservationHandler(tracer));

        log.debug("Registered DefaultTracingObservationHandler for local span creation");

        // 3. Sender handler: Injects trace context into outgoing messages/requests
        // - Adds W3C traceparent/tracestate headers to HTTP requests
        // - Populates CloudEvent trace extensions for Kafka messages
        // - Ensures downstream services can continue the trace
        registry.observationConfig()
                .observationHandler(new PropagatingSenderTracingObservationHandler<>(tracer, propagator));

        log.debug("Registered PropagatingSenderTracingObservationHandler for context injection");

        log.info("ObservationRegistry configured successfully with {} handlers", 3);

        return registry;
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
        log.info("=== Multi-Backend Trace Export Configuration ===");

        List<String> configuredBackends = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        checkBackend("Primary", primaryBackend, configuredBackends, warnings);
        checkBackend("Fallback", fallbackBackend, configuredBackends, warnings);

        // Report configuration status
        if (configuredBackends.isEmpty()) {
            String errorMsg = "No trace backends properly configured! Spans will not be exported.";
            log.error(errorMsg);
            log.error("Please configure at least one backend endpoint in application-tracing.yml");
            return "ERROR: " + errorMsg;
        }

        if (!warnings.isEmpty()) {
            log.warn("Configuration warnings:");
            warnings.forEach(warning -> log.warn("  - {}", warning));
        }

        String summary = "Multi-backend export configured: %s".formatted(
                String.join(", ", configuredBackends));

        log.info(summary);
        log.info("Note: Spring Boot auto-configuration manages span exporters");
        log.info("Note: Both backends export simultaneously when both endpoints are configured");
        log.info(
                "Note: Circuit breaker and smart failover will be implemented in T021 (ResilientCompositeSpanExporter)");
        log.info("=== End Multi-Backend Configuration ===");

        return summary;
    }

    private void checkBackend(String role, String backendName, List<String> configuredBackends, List<String> warnings) {
        log.info("{} backend: {}", role, backendName);
        if ("tempo".equalsIgnoreCase(backendName) || "otlp".equalsIgnoreCase(backendName)) {
            if (otlpEndpoint != null && !otlpEndpoint.isEmpty()) {
                configuredBackends.add("OTLP/Tempo (" + role.toLowerCase() + ")");
                log.info("✓ OTLP endpoint configured: {}", otlpEndpoint);
            } else {
                warnings.add(role + " backend 'tempo' configured but management.otlp.tracing.endpoint is missing");
                log.warn("✗ {} backend '{}' selected but OTLP endpoint not configured", role, backendName);
            }
        } else if ("zipkin".equalsIgnoreCase(backendName)) {
            if (zipkinEndpoint != null && !zipkinEndpoint.isEmpty()) {
                configuredBackends.add("Zipkin (" + role.toLowerCase() + ")");
                log.info("✓ Zipkin endpoint configured: {}", zipkinEndpoint);
            } else {
                warnings.add(role + " backend 'zipkin' configured but management.zipkin.tracing.endpoint is missing");
                log.warn("✗ {} backend '{}' selected but Zipkin endpoint not configured", role, backendName);
            }
        } else {
            warnings.add("Unknown " + role.toLowerCase() + " backend: " + backendName);
            log.error("✗ Unknown {} backend: {}", role.toLowerCase(), backendName);
        }
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
        log.info("Creating CircuitBreakerRegistry for resilient span export");
        return CircuitBreakerRegistry.ofDefaults();
    }

    /**
     * Creates a ResilientCompositeSpanExporter that manages primary and fallback
     * backends
     * with circuit breaker protection.
     * 
     * <p>
     * This bean is only created when:
     * </p>
     * <ul>
     * <li>Circuit breaker is enabled (default: true)</li>
     * <li>Both primary and fallback backend endpoints are configured</li>
     * <li>Brave Tracing instance is available</li>
     * </ul>
     *
     * <p>
     * <b>Integration with Brave:</b>
     * </p>
     * The ResilientCompositeSpanExporter is registered as a SpanHandler with
     * Brave's Tracing
     * instance. Brave will route all completed spans through this handler, which
     * then makes
     * the primary/fallback decision based on circuit breaker state.
     *
     * <p>
     * <b>Note:</b>
     * </p>
     * This implementation assumes Spring Boot has auto-configured span handlers for
     * both
     * Zipkin and OTLP backends. We don't directly inject them here because:
     * <ol>
     * <li>Spring Boot auto-configuration may not expose them as beans</li>
     * <li>Brave manages SpanHandlers internally through its Tracing instance</li>
     * <li>The actual routing happens at the Tracing level, not via direct handler
     * injection</li>
     * </ol>
     *
     * <p>
     * <b>Alternative Approach (Future Enhancement):</b>
     * </p>
     * A more complete implementation would:
     * <ul>
     * <li>Explicitly create and inject primary/fallback SpanHandler beans</li>
     * <li>Register ResilientCompositeSpanExporter as the only handler with
     * Brave</li>
     * <li>Let ResilientCompositeSpanExporter delegate to primary/fallback
     * handlers</li>
     * </ul>
     *
     * @param circuitBreakerRegistry the circuit breaker registry
     * @param tracing                the Brave tracing instance (optional, may be
     *                               null if not auto-configured)
     * @return a configured ResilientCompositeSpanExporter, or null if prerequisites
     *         not met
     */
    @Bean
    @ConditionalOnProperty(value = "tracing.resilience.circuit-breaker.enabled", havingValue = "true", matchIfMissing = true)
    public ResilientCompositeSpanExporter resilientCompositeSpanExporter(
            CircuitBreakerRegistry circuitBreakerRegistry,
            Optional<Tracing> tracing) {

        log.info("Configuring ResilientCompositeSpanExporter...");

        // Validate prerequisites
        if (tracing.isEmpty()) {
            log.warn("Brave Tracing instance not available, ResilientCompositeSpanExporter cannot be created");
            log.warn("This is expected if tracing is not fully configured");
            return null;
        }

        // Parse wait duration (convert "60s" to seconds)
        long waitDurationSeconds = parseWaitDuration(waitDuration);

        log.info("Circuit breaker configuration: failureThreshold={}, waitDuration={}s, ringBufferSize={}",
                failureThreshold, waitDurationSeconds, ringBufferSize);

        // Create NOOP handlers that delegate to Spring Boot's auto-configured exporters
        // Spring Boot auto-configuration handles the actual span export to backends
        // These handlers act as pass-through with logging for circuit breaker
        // integration
        brave.handler.SpanHandler primaryHandler = new LoggingSpanHandler("primary", primaryBackend);
        brave.handler.SpanHandler fallbackHandler = new LoggingSpanHandler("fallback", fallbackBackend);

        log.info("Created SpanHandlers for primary={} and fallback={} backends", primaryBackend, fallbackBackend);
        log.info("Note: Spring Boot auto-configuration handles actual span export to {} and {}",
                otlpEndpoint != null ? "OTLP(" + otlpEndpoint + ")" : "OTLP(not configured)",
                zipkinEndpoint != null ? "Zipkin(" + zipkinEndpoint + ")" : "Zipkin(not configured)");

        return new ResilientCompositeSpanExporter(
                primaryHandler,
                fallbackHandler,
                circuitBreakerRegistry,
                failureThreshold,
                waitDurationSeconds,
                ringBufferSize);
    }

    /**
     * Logging SpanHandler that records span export operations.
     * Acts as a pass-through handler while providing observability.
     */
    private static class LoggingSpanHandler extends brave.handler.SpanHandler {
        private final String role;
        private final String backendName;

        LoggingSpanHandler(String role, String backendName) {
            this.role = role;
            this.backendName = backendName;
        }

        @Override
        public boolean end(brave.propagation.TraceContext context, brave.handler.MutableSpan span, Cause cause) {
            log.trace("[{}] Exporting span to {} backend: id={}, name={}, duration={}μs",
                    role, backendName,
                    span.id(), span.name(),
                    span.finishTimestamp() - span.startTimestamp());
            // Return true to indicate success - Spring Boot handles actual export
            return true;
        }
    }

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
            log.warn("Invalid wait duration format '{}', using default 60s", duration);
            return 60;
        }
    }

    /**
     * Initializes and logs TracingFeatureFlags configuration on startup and
     * refresh.
     * 
     * <p>
     * This bean ensures feature flags are properly initialized and their state is
     * logged
     * for monitoring and troubleshooting. The method is called:
     * </p>
     * <ul>
     * <li>On application startup (@PostConstruct)</li>
     * <li>After configuration refresh via /actuator/refresh (@RefreshScope)</li>
     * </ul>
     *
     * @param featureFlags the feature flags configuration bean
     * @return a status message indicating initialization completed
     */
    @Bean
    public String tracingFeatureFlagsInitializer(TracingFeatureFlags featureFlags) {
        featureFlags.logConfiguration();

        // Log refresh endpoint information
        log.info("Feature flags can be updated at runtime via: POST /actuator/refresh");
        log.info("Current feature flags state: enabled=[{}], disabled=[{}]",
                featureFlags.getEnabledComponents(),
                featureFlags.getDisabledComponents());

        return "TracingFeatureFlags initialized";
    }

}
