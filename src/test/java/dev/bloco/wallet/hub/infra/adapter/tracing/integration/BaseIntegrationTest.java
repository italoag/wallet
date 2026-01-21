package dev.bloco.wallet.hub.infra.adapter.tracing.integration;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import dev.bloco.wallet.hub.config.TestGatewayConfig;
import dev.bloco.wallet.hub.config.TestJpaConfiguration;
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

import com.redis.testcontainers.RedisContainer;

/**
 * Base class for integration tests with testcontainers infrastructure.
 * 
 * <h2>Infrastructure</h2>
 * <ul>
 * <li>PostgreSQL 16 - for JPA and R2DBC tests</li>
 * <li>Apache Kafka 7.6.0 - for event streaming tests</li>
 * <li>Redis 7-alpine - for reactive caching tests</li>
 * </ul>
 * 
 * <h2>Span Collection</h2>
 * <p>
 * Collects all exported spans in-memory for assertion. Use {@link #getSpans()}
 * to access collected spans and {@link #clearSpans()} to reset between tests.
 * </p>
 * 
 * <h2>Usage</h2>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;Testcontainers
 *     class MyIntegrationTest extends BaseIntegrationTest {
 * 
 *         @Test
 *         void shouldTraceOperation() {
 *             // Execute operation
 *             service.performOperation();
 * 
 *             // Assert spans
 *             List<FinishedSpan> spans = getSpans();
 *             assertThat(spans).hasSize(3);
 * 
 *             FinishedSpan rootSpan = findSpan(spans, s -> s.getName().equals("operation"));
 *             assertThat(rootSpan.getTags()).containsEntry("operation.type", "create");
 *         }
 *     }
 * }
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@Import({ TestJpaConfiguration.class, TracingTestConfig.class, TestGatewayConfig.class })
@TestPropertySource(properties = {
        "spring.main.web-application-type=none",
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "management.tracing.sampling.probability=1.0",
        "tracing.features.use-case=true",
        "tracing.features.api=true",
        "tracing.features.database=true",
        "tracing.features.kafka=true",
        "tracing.features.stateMachine=true",
        "tracing.features.externalApi=true",
        "tracing.features.reactive=true"
})
public abstract class BaseIntegrationTest {

    static {
        // Disable Ryuk to avoid connection issues on macOS/Rancher Desktop
        System.setProperty("TESTCONTAINERS_RYUK_DISABLED", "true");
        // Force 127.0.0.1 to avoid IPv6 issues (localhost resolving to ::1)
        System.setProperty("TESTCONTAINERS_HOST_OVERRIDE", "127.0.0.1");
    }

    /**
     * PostgreSQL container for JPA and R2DBC integration tests.
     */
    @Container
    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("wallet_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    /**
     * Kafka container for event streaming integration tests.
     * Testcontainers 2.x provides ConfluentKafkaContainer for Confluent images.
     */
    @Container
    @SuppressWarnings("resource")
    protected static final ConfluentKafkaContainer KAFKA_CONTAINER = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest"))
            .withReuse(true);

    /**
     * Redis container for reactive caching integration tests.
     */
    // @Container
    // @SuppressWarnings("resource")
    // protected static final GenericContainer<?> REDIS_CONTAINER = new
    // GenericContainer<>(
    // DockerImageName.parse("redis:7-alpine"))
    // .withExposedPorts(6379)
    // .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
    // .withReuse(true);

    @Container
    @SuppressWarnings("resource")
    protected static final RedisContainer REDIS_CONTAINER = new RedisContainer(
            RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG))
            .withExposedPorts(6379)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
            .withReuse(true);
    /**
     * Grafana LGTM stack container for tracing backend.
     */
    @Container
    @SuppressWarnings("resource")
    protected static final LgtmStackContainer LGTM_CONTAINER = new LgtmStackContainer(
            DockerImageName.parse("grafana/otel-lgtm:latest"))
            .withStartupTimeout(java.time.Duration.ofMinutes(2))
            .withReuse(true);

    /**
     * In-memory span collector for assertions.
     */
    protected static final Queue<FinishedSpan> SPAN_QUEUE = new ConcurrentLinkedQueue<>();

    public static void addSpan(FinishedSpan span) {
        SPAN_QUEUE.offer(span);
    }

    @Autowired
    protected io.micrometer.tracing.Tracer tracer;

    @Autowired
    protected SimpleTracer simpleTracer;

    /**
     * Configure dynamic properties for testcontainers.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        // R2DBC configuration
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + POSTGRES_CONTAINER.getHost() + ":" +
                POSTGRES_CONTAINER.getFirstMappedPort() + "/" + POSTGRES_CONTAINER.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES_CONTAINER::getPassword);

        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.cloud.stream.kafka.binder.brokers", KAFKA_CONTAINER::getBootstrapServers);

        // Redis configuration
        // Redis configuration
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", REDIS_CONTAINER::getFirstMappedPort);

        // OTLP configuration for tracing and metrics (disabled for tests to avoid
        // external dependencies)
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

        // LGTM OTLP and HTTP endpoints
        registry.add("management.opentelemetry.tracing.export.otlp.endpoint",
                () -> String.format("http://%s:%d/v1/traces",
                        LGTM_CONTAINER.getHost(), LGTM_CONTAINER.getMappedPort(4317)));
    }

    @BeforeEach
    void setUp() {
        clearSpans();
    }

    /**
     * Get all collected spans.
     * 
     * @return list of finished spans
     */
    protected List<FinishedSpan> getSpans() {
        if (simpleTracer != null) {
            return List.copyOf(simpleTracer.getSpans());
        }
        return List.copyOf(SPAN_QUEUE);
    }

    /**
     * Clear all collected spans.
     */
    protected void clearSpans() {
        if (simpleTracer != null) {
            simpleTracer.getSpans().clear();
        }
        SPAN_QUEUE.clear();
    }

    /**
     * Find the first span matching predicate.
     * 
     * @param spans     list of spans to search
     * @param predicate matching condition
     * @return first matching span or null
     */
    protected FinishedSpan findSpan(List<FinishedSpan> spans, Predicate<FinishedSpan> predicate) {
        return spans.stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    /**
     * Find all spans matching predicate.
     * 
     * @param spans     list of spans to search
     * @param predicate matching condition
     * @return list of matching spans
     */
    protected List<FinishedSpan> findSpans(List<FinishedSpan> spans, Predicate<FinishedSpan> predicate) {
        return spans.stream()
                .filter(predicate)
                .toList();
    }

    /**
     * Wait for spans to be exported.
     * 
     * @param expectedCount expected number of spans
     * @param timeoutMs     maximum wait time in milliseconds
     * @return true if expected spans were collected, false if timeout
     */
    protected boolean waitForSpans(int expectedCount, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            int currentCount = simpleTracer != null ? simpleTracer.getSpans().size() : SPAN_QUEUE.size();
            if (currentCount >= expectedCount) {
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Assert span hierarchy - verify parent-child relationships.
     * 
     * @param parent parent span
     * @param child  child span
     */
    protected void assertSpanHierarchy(FinishedSpan parent, FinishedSpan child) {
        String parentSpanId = parent.getSpanId();
        String childParentId = child.getParentId();

        org.assertj.core.api.Assertions.assertThat(childParentId)
                .as("Child span should have parent span ID")
                .isEqualTo(parentSpanId);

        String parentTraceId = parent.getTraceId();
        String childTraceId = child.getTraceId();

        org.assertj.core.api.Assertions.assertThat(childTraceId)
                .as("Child and parent should have same trace ID")
                .isEqualTo(parentTraceId);
    }

    /**
     * Assert span contains required tags.
     * 
     * @param span         span to check
     * @param requiredTags tags that must be present
     */
    protected void assertSpanHasTags(FinishedSpan span, String... requiredTags) {
        for (String tag : requiredTags) {
            org.assertj.core.api.Assertions.assertThat(span.getTags())
                    .as("Span should have tag: " + tag)
                    .containsKey(tag);
        }
    }

    /**
     * Assert span contains a specific tag value.
     * 
     * @param span          span to check
     * @param tag           tag name
     * @param expectedValue expected tag value
     */
    protected void assertSpanTagEquals(FinishedSpan span, String tag, String expectedValue) {
        org.assertj.core.api.Assertions.assertThat(span.getTags())
                .as("Span should have tag '" + tag + "' with value '" + expectedValue + "'")
                .containsEntry(tag, expectedValue);
    }

    /**
     * Create a test span for testing.
     * 
     * @param name span name
     * @return started span
     */
    protected Span createTestSpan(String name) {
        return tracer.nextSpan().name(name).start();
    }
}
