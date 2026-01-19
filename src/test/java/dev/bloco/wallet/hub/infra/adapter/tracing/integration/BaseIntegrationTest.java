package dev.bloco.wallet.hub.infra.adapter.tracing.integration;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.redis.testcontainers.RedisContainer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Base class for integration tests with testcontainers infrastructure.
 * 
 * <h2>Infrastructure</h2>
 * Containers are declared as static fields and managed by JUnit Testcontainers
 * extension:
 * <ul>
 * <li>PostgreSQL 16 - for JPA and R2DBC tests</li>
 * <li>Apache Kafka - for event streaming tests</li>
 * <li>Redis - for reactive caching tests</li>
 * <li>Grafana LGTM - for OTLP tracing</li>
 * </ul>
 * 
 * <h2>Span Collection</h2>
 * <p>
 * Collects all exported spans in-memory for assertion. Use {@link #getSpans()}
 * to access collected spans and {@link #clearSpans()} to reset between tests.
 * </p>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.web-application-type=none",
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "management.tracing.sampling.probability=1.0",
        "tracing.features.use-case=false",
        "tracing.features.api=true",
        "tracing.features.database=true",
        "tracing.features.kafka=true",
        "tracing.features.stateMachine=true",
        "tracing.features.externalApi=true",
        "tracing.features.reactive=true"
})
@Import(TestSpanExporterConfig.class)
public abstract class BaseIntegrationTest {

    // ============= Testcontainers =============

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("wallet_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    @SuppressWarnings("resource")
    static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @Container
    @SuppressWarnings("resource")
    static final RedisContainer REDIS = new RedisContainer(
            RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

    @Container
    @SuppressWarnings("resource")
    static final LgtmStackContainer LGTM = new LgtmStackContainer(
            DockerImageName.parse("grafana/otel-lgtm:latest"))
            .withStartupTimeout(Duration.ofMinutes(2));

    // ============= Span Collection =============

    @Autowired(required = false)
    protected SimpleTracer simpleTracer;

    @Autowired(required = false)
    protected io.micrometer.tracing.Tracer tracer;

    // ============= Dynamic Properties =============

    /**
     * Configure dynamic properties for testcontainers.
     * All connection properties are set after containers are started.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // JDBC Datasource configuration - required for JPA/EntityManagerFactory
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // R2DBC configuration
        registry.add("spring.r2dbc.url", () -> String.format(
                "r2dbc:postgresql://%s:%d/%s",
                POSTGRES.getHost(),
                POSTGRES.getMappedPort(5432),
                POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);

        // JPA configuration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        // Redis configuration
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        // OTLP tracing and metrics configuration
        registry.add("management.otlp.tracing.endpoint", LGTM::getOtlpHttpUrl);
        registry.add("management.otlp.metrics.export.url", () -> LGTM.getOtlpHttpUrl() + "/v1/metrics");
        registry.add("management.otlp.tracing.enabled", () -> "true");
        registry.add("management.otlp.metrics.export.enabled", () -> "true");

        // Enable all tracing features for tests
        registry.add("tracing.features.api", () -> "true");
        registry.add("tracing.features.database", () -> "true");
        registry.add("tracing.features.kafka", () -> "true");
        registry.add("tracing.features.stateMachine", () -> "true");
        registry.add("tracing.features.externalApi", () -> "true");
        registry.add("tracing.features.reactive", () -> "true");

        // Use in-memory span exporter for tests
        registry.add("management.tracing.sampling.probability", () -> "1.0");
    }

    @BeforeEach
    void setUp() {
        clearSpans();
    }

    // ============= Span Collection Methods =============

    protected void clearSpans() {
        if (simpleTracer != null) {
            simpleTracer.getSpans().clear();
        }
    }

    protected List<FinishedSpan> getSpans() {
        if (simpleTracer != null) {
            return new java.util.ArrayList<>(simpleTracer.getSpans());
        }
        return Collections.emptyList();
    }

    protected List<FinishedSpan> getSpansWithName(String name) {
        return getSpans().stream()
                .filter(span -> name.equals(span.getName()))
                .toList();
    }

    protected List<FinishedSpan> getSpansMatching(Predicate<FinishedSpan> predicate) {
        return getSpans().stream()
                .filter(predicate)
                .toList();
    }

    /**
     * Wait for spans to be collected (with timeout).
     * InterruptedException is caught and the thread's interrupt status is restored.
     */
    protected void waitForSpans(int expectedCount, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        try {
            while (getSpans().size() < expectedCount && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for spans", e);
        }
    }

    // ============= Assertion Helpers =============

    protected void assertSpanExists(String name) {
        var spans = getSpansWithName(name);
        if (spans.isEmpty()) {
            throw new AssertionError("Expected span with name '" + name + "' but none found. Available spans: "
                    + getSpans().stream().map(FinishedSpan::getName).toList());
        }
    }

    protected void assertSpanExists(String name, Span.Kind kind) {
        var spans = getSpansWithName(name).stream()
                .filter(span -> kind.equals(span.getKind()))
                .toList();
        if (spans.isEmpty()) {
            throw new AssertionError("Expected span with name '" + name + "' and kind '" + kind + "' but none found.");
        }
    }

    protected void assertSpanHasTag(FinishedSpan span, String tagKey, String expectedValue) {
        String actualValue = span.getTags().get(tagKey);
        if (!expectedValue.equals(actualValue)) {
            throw new AssertionError(
                    "Expected tag '" + tagKey + "' to be '" + expectedValue + "' but was '" + actualValue + "'");
        }
    }

    protected void assertSpanTagEquals(FinishedSpan span, String tagKey, String expectedValue) {
        assertSpanHasTag(span, tagKey, expectedValue);
    }

    /**
     * Asserts that the span has all the specified tag keys (does not verify
     * values).
     */
    protected void assertSpanHasTags(FinishedSpan span, String... tagKeys) {
        for (String key : tagKeys) {
            if (!span.getTags().containsKey(key)) {
                throw new AssertionError("Expected span to have tag '" + key + "' but it was not found. " +
                        "Available tags: " + span.getTags().keySet());
            }
        }
    }

    /**
     * Asserts that the span has the specified tag key-value pairs.
     */
    protected void assertSpanHasTagValues(FinishedSpan span, String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("keyValuePairs must be even (key, value, key, value, ...)");
        }
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            assertSpanHasTag(span, keyValuePairs[i], keyValuePairs[i + 1]);
        }
    }

    // ============= Span Creation Helpers =============

    /**
     * Creates a test span with the given name using the tracer.
     * This span can be used for testing tracing functionality.
     */
    protected Span createTestSpan(String name) {
        if (tracer == null) {
            throw new IllegalStateException("Tracer is not available. Ensure tracing is properly configured.");
        }
        return tracer.nextSpan().name(name).start();
    }

    /**
     * Creates a child span from an existing parent span.
     */
    protected Span createChildSpan(Span parent, String name) {
        if (tracer == null) {
            throw new IllegalStateException("Tracer is not available. Ensure tracing is properly configured.");
        }
        return tracer.nextSpan(parent).name(name).start();
    }

    // ============= Span Search Helpers =============

    /**
     * Finds the first span matching the given predicate.
     */
    protected FinishedSpan findSpan(List<FinishedSpan> spans, Predicate<FinishedSpan> predicate) {
        return spans.stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds all spans matching the given predicate.
     */
    protected List<FinishedSpan> findSpans(List<FinishedSpan> spans, Predicate<FinishedSpan> predicate) {
        return spans.stream()
                .filter(predicate)
                .toList();
    }

    /**
     * Asserts that child span is a child of parent span (same trace ID, different
     * span ID).
     */
    protected void assertSpanHierarchy(FinishedSpan parent, FinishedSpan child) {
        if (parent == null || child == null) {
            throw new AssertionError("Parent or child span is null");
        }
        if (!parent.getTraceId().equals(child.getTraceId())) {
            throw new AssertionError("Spans are not in the same trace. Parent trace: "
                    + parent.getTraceId() + ", Child trace: " + child.getTraceId());
        }
        if (parent.getSpanId().equals(child.getSpanId())) {
            throw new AssertionError("Parent and child have the same span ID: " + parent.getSpanId());
        }
    }
}