package dev.bloco.wallet.hub.infra.adapter.tracing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import dev.bloco.wallet.hub.usecase.AddFundsUseCase;
import dev.bloco.wallet.hub.infra.adapter.tracing.integration.BaseIntegrationTest;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.exporter.FinishedSpan;
import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for end-to-end distributed tracing.
 * 
 * <p>
 * Tests verify complete trace flows: use case -> database -> kafka.
 * </p>
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("tracing")
@TestPropertySource(properties = {
        "management.tracing.enabled=true",
        "management.tracing.sampling.probability=1.0",
        "tracing.features.use-case=true",
        "tracing.features.database=true",
        "tracing.features.kafka=true",
        "spring.main.web-application-type=none"
})
class EndToEndTracingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AddFundsUseCase addFundsUseCase;

    @Autowired
    private dev.bloco.wallet.hub.domain.gateway.WalletRepository walletRepository;

    @Test
    void shouldCreateEndToEndTraceForAddFunds() {
        // Given: Tracing is available and a wallet exists
        if (tracer == null) {
            log.warn("Tracer not available, skipping integration test");
            return;
        }

        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        String correlationId = UUID.randomUUID().toString();

        dev.bloco.wallet.hub.domain.model.Wallet wallet = new dev.bloco.wallet.hub.domain.model.Wallet(walletId,
                "Test Wallet", "For tracing test", userId);
        walletRepository.save(wallet);
        clearSpans(); // Clear spans from setup

        // When: Execute use case within a parent span
        Span rootSpan = tracer.nextSpan().name("api-request").start();
        try (@SuppressWarnings("unused")
        Tracer.SpanInScope scope = tracer.withSpan(rootSpan)) {
            addFundsUseCase.addFunds(walletId, amount, correlationId);
        } finally {
            rootSpan.end();
        }

        // Then: Verify E2E trace hierarchy
        // Root Span -> UseCase Span -> DB Spans & Kafka Spans
        waitForSpans(4, 5000);
        List<FinishedSpan> spans = getSpans();

        // 1. Verify root span
        FinishedSpan finishedRoot = findSpan(spans, s -> "api-request".equals(s.getName()));
        assertThat(finishedRoot).as("Root span 'api-request' should exist").isNotNull();

        // 2. Verify UseCase span
        FinishedSpan useCaseSpan = findSpan(spans,
                s -> s.getName() != null && s.getName().contains("add-funds-use-case.add-funds"));
        assertThat(useCaseSpan).as("UseCase span should exist").isNotNull();
        assertSpanHierarchy(finishedRoot, useCaseSpan);

        // 3. Verify Database spans (as children of UseCase)
        List<FinishedSpan> dbSpans = findSpans(spans,
                s -> s.getName() != null && (s.getName().contains("query") || s.getName().contains("select")
                        || s.getName().contains("update")
                        || s.getName().contains("find-by-id") || s.getName().contains("save")));
        assertThat(dbSpans).as("Database spans should exist").isNotEmpty();
        for (FinishedSpan dbSpan : dbSpans) {
            assertSpanHierarchy(useCaseSpan, dbSpan);
        }

        // 4. Verify Kafka span
        FinishedSpan kafkaSpan = findSpan(spans,
                s -> s.getName() != null && (s.getName().contains("publish") || s.getName().contains("send")
                        || s.getName().contains("FundsAddedEvent")));
        if (kafkaSpan != null) {
            assertSpanHierarchy(useCaseSpan, kafkaSpan);
        }

        // 5. Verify Tags
        assertSpanHasTag(useCaseSpan, "usecase.class", "AddFundsUseCase");
        assertSpanHasTag(useCaseSpan, "wallet.operation", "add_funds");
        assertSpanHasTag(useCaseSpan, "transaction.amount", amount.toString());
    }

    @Test
    void shouldPropagateTraceContextAcrossLayers() {
        // Given: Tracing is available
        if (tracer == null) {
            log.warn("Tracer not available, skipping integration test");
            return;
        }

        // When: Create parent span
        Span parentSpan = tracer.nextSpan().name("parent").start();
        String parentTraceId = parentSpan.context().traceId();

        try (@SuppressWarnings("unused")
        Tracer.SpanInScope scope = tracer.withSpan(parentSpan)) {
            // Create child span
            Span childSpan = tracer.nextSpan().name("child").start();
            childSpan.end();

            // Then: Verify hierarchy
            waitForSpans(2, 2000);
            List<FinishedSpan> currentSpans = getSpans();
            FinishedSpan p = findSpan(currentSpans, s -> "parent".equals(s.getName()));
            FinishedSpan c = findSpan(currentSpans, s -> "child".equals(s.getName()));

            assertThat(p).isNotNull();
            assertThat(c).isNotNull();
            assertSpanHierarchy(p, c);
            assertThat(c.getTraceId()).isEqualTo(parentTraceId);
        } finally {
            parentSpan.end();
        }
    }

    @Test
    void shouldCaptureSpanAttributes() {
        // Given: Tracing is available
        if (tracer == null) {
            log.warn("Tracer not available, skipping integration test");
            return;
        }

        // When: Create span with attributes
        Span span = tracer.nextSpan().name("test-span").start();
        span.tag("wallet.id.hash", "abc123");
        span.tag("operation.name", "add_funds");
        span.tag("status", "success");
        span.end();

        // Then: Verify attributes in finished span
        waitForSpans(1, 2000);
        FinishedSpan finishedSpan = findSpan(getSpans(), s -> "test-span".equals(s.getName()));
        assertThat(finishedSpan).isNotNull();
        assertSpanHasTagValues(finishedSpan,
                "wallet.id.hash", "abc123",
                "operation.name", "add_funds",
                "status", "success");
    }

    @Test
    void shouldCaptureErrorSpans() {
        // Given: Tracing is available
        if (tracer == null) {
            log.warn("Tracer not available, skipping integration test");
            return;
        }

        // When: Create span with error
        Span span = tracer.nextSpan().name("error-span").start();
        Exception error = new RuntimeException("Test error");
        span.error(error);
        span.tag("error", "true");
        span.tag("error.type", "RuntimeException");
        span.end();

        // Then: Verify error in finished span
        waitForSpans(1, 2000);
        FinishedSpan finishedSpan = findSpan(getSpans(), s -> "error-span".equals(s.getName()));
        assertThat(finishedSpan).isNotNull();
        assertSpanHasTagValues(finishedSpan,
                "error", "true",
                "error.type", "RuntimeException");
    }

    @Test
    void shouldMeasureSpanDuration() {
        // Given: Tracing is available
        if (tracer == null) {
            log.warn("Tracer not available, skipping integration test");
            return;
        }

        // When: Create span with delay
        Span span = tracer.nextSpan().name("timed-span").start();
        try {
            Thread.sleep(10); // Simulate work to ensure duration > 0
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        span.end();

        // Then: Verify duration is captured
        waitForSpans(1, 2000);
        FinishedSpan finishedSpan = findSpan(getSpans(), s -> "timed-span".equals(s.getName()));
        assertThat(finishedSpan).isNotNull();
        assertThat(finishedSpan.getStartTimestamp()).isBefore(finishedSpan.getEndTimestamp());
    }

    @Test
    void shouldCreateSpanHierarchy() {
        // Given: Tracing is available
        if (tracer == null) {
            log.warn("Tracer not available, skipping integration test");
            return;
        }

        // When: Create parent-child-grandchild hierarchy
        Span parent = tracer.nextSpan().name("parent").start();
        try (@SuppressWarnings("unused")
        Tracer.SpanInScope parentScope = tracer.withSpan(parent)) {
            Span child = tracer.nextSpan().name("child").start();
            try (@SuppressWarnings("unused")
            Tracer.SpanInScope childScope = tracer.withSpan(child)) {
                Span grandchild = tracer.nextSpan().name("grandchild").start();
                grandchild.end();
            } finally {
                child.end();
            }
        } finally {
            parent.end();
        }

        // Then: Verify hierarchy
        waitForSpans(3, 2000);
        List<FinishedSpan> spans = getSpans();
        FinishedSpan p = findSpan(spans, s -> "parent".equals(s.getName()));
        FinishedSpan c = findSpan(spans, s -> "child".equals(s.getName()));
        FinishedSpan g = findSpan(spans, s -> "grandchild".equals(s.getName()));

        assertThat(p).isNotNull();
        assertThat(c).isNotNull();
        assertThat(g).isNotNull();
        assertSpanHierarchy(p, c);
        assertSpanHierarchy(c, g);
    }

    @Test
    void shouldHandleSamplingConfiguration() {
        // Given: Tracing is available with 100% sampling
        if (tracer == null) {
            log.warn("Tracer not available, skipping integration test");
            return;
        }

        // When: Create multiple spans
        for (int i = 0; i < 5; i++) {
            Span span = tracer.nextSpan().name("span-" + i).start();
            span.end();
        }

        // Then: All spans should be sampled
        waitForSpans(5, 2000);
        assertThat(getSpans()).hasSize(5);
    }
}
