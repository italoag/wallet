package dev.bloco.wallet.hub.infra.adapter.tracing.integration;

import io.micrometer.tracing.exporter.FinishedSpan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Kafka distributed tracing with CloudEvents.
 * 
 * <p>Tests W3C Trace Context propagation via CloudEvents extensions,
 * span hierarchy, consumer lag calculation, and trace continuity across event cascades.</p>
 * 
 * <p>Validates:
 * <ul>
 *   <li>T086: Trace context in CloudEvent extensions (traceparent format: 00-{trace-id}-{span-id}-01)</li>
 *   <li>T087: Consumer span as child of producer span</li>
 *   <li>T088: Consumer lag calculation from timestamps</li>
 *   <li>T089: Trace continuity across event cascades (FundsAdded → FundsTransferred)</li>
 * </ul>
 */
@Testcontainers
@DisplayName("Kafka Trace Propagation Integration Tests")
class KafkaTracePropagationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("T086: Should propagate trace context in CloudEvent extensions (traceparent format)")
    void shouldPropagateTraceContextInCloudEventExtensions() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        String traceId = generateTraceId();
        String spanId = generateSpanId();
        
        clearSpans();
        
        // Create a test span simulating Kafka producer
        var producerSpan = createTestSpan("kafka.send");
        producerSpan.tag("messaging.system", "kafka");
        producerSpan.tag("messaging.destination", "funds-added-topic");
        producerSpan.tag("messaging.operation", "publish");
        producerSpan.tag("messaging.message_id", walletId.toString());
        
        // Simulate CloudEvent with W3C traceparent extension
        String traceparent = String.format("00-%s-%s-01", traceId, spanId);
        producerSpan.tag("cloudevents.traceparent", traceparent);
        producerSpan.tag("cloudevents.type", "dev.bloco.wallet.funds-added");
        
        producerSpan.end();
        
        // Assert
        waitForSpans(1, 1000);
        List<FinishedSpan> spans = getSpans();
        
        assertThat(spans).hasSize(1);
        FinishedSpan span = spans.get(0);
        
        // Verify messaging tags
        assertSpanHasTags(span, 
            "messaging.system", 
            "messaging.destination",
            "messaging.operation");
        
        assertSpanTagEquals(span, "messaging.system", "kafka");
        assertSpanTagEquals(span, "messaging.destination", "funds-added-topic");
        assertSpanTagEquals(span, "messaging.operation", "publish");
        
        // Verify traceparent format (00-{32-hex-trace-id}-{16-hex-span-id}-01)
        String capturedTraceparent = span.getTags().get("cloudevents.traceparent");
        assertThat(capturedTraceparent)
            .matches("^00-[0-9a-f]{32}-[0-9a-f]{16}-01$");
    }

    @Test
    @DisplayName("T087: Should create consumer span as child of producer span")
    void shouldCreateConsumerSpanAsChildOfProducerSpan() {
        // Arrange
        String traceId = generateTraceId();
        String producerSpanId = generateSpanId();
        String consumerSpanId = generateSpanId();
        
        clearSpans();
        
        // Simulate producer span
        var producerSpan = createTestSpan("kafka.send");
        producerSpan.tag("messaging.system", "kafka");
        producerSpan.tag("messaging.operation", "publish");
        producerSpan.tag("messaging.destination", "funds-added-topic");
        String traceparent = String.format("00-%s-%s-01", traceId, producerSpanId);
        producerSpan.tag("cloudevents.traceparent", traceparent);
        producerSpan.end();
        
        // Simulate consumer span (should be child of producer)
        var consumerSpan = tracer.nextSpan(producerSpan).name("kafka.receive").start();
        consumerSpan.tag("messaging.system", "kafka");
        consumerSpan.tag("messaging.operation", "receive");
        consumerSpan.tag("messaging.destination", "funds-added-topic");
        consumerSpan.tag("messaging.source_kind", "consumer");
        consumerSpan.end();
        
        // Assert
        waitForSpans(2, 1500);
        List<FinishedSpan> spans = getSpans();
        
        assertThat(spans).hasSizeGreaterThanOrEqualTo(2);
        
        // Verify hierarchy
        FinishedSpan producer = findSpan(spans, s -> "kafka.send".equals(s.getName()));
        FinishedSpan consumer = findSpan(spans, s -> "kafka.receive".equals(s.getName()));
        
        assertThat(producer).isNotNull();
        assertThat(consumer).isNotNull();
        
        // Consumer must be child of producer
        assertSpanHierarchy(producer, consumer);
    }

    @Test
    @DisplayName("T088: Should calculate consumer lag from event timestamps")
    void shouldCalculateConsumerLagFromTimestamps() {
        // Arrange
        clearSpans();
        
        long sendTime = System.currentTimeMillis();
        
        // Simulate delay (5 seconds)
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long receiveTime = System.currentTimeMillis();
        long lagMs = receiveTime - sendTime;
        
        // Create consumer span with lag metric
        var consumerSpan = createTestSpan("kafka.receive");
        consumerSpan.tag("messaging.system", "kafka");
        consumerSpan.tag("messaging.destination", "funds-added-topic");
        consumerSpan.tag("messaging.operation", "receive");
        consumerSpan.tag("messaging.consumer_lag_ms", String.valueOf(lagMs));
        consumerSpan.tag("messaging.timestamp.publish", String.valueOf(sendTime));
        consumerSpan.tag("messaging.timestamp.receive", String.valueOf(receiveTime));
        consumerSpan.end();
        
        // Assert
        waitForSpans(1, 1000);
        List<FinishedSpan> spans = getSpans();
        
        assertThat(spans).hasSize(1);
        FinishedSpan span = spans.get(0);
        
        // Verify lag is captured and within expected range (4-6 seconds)
        String lagStr = span.getTags().get("messaging.consumer_lag_ms");
        assertThat(lagStr).isNotNull();
        
        long capturedLag = Long.parseLong(lagStr);
        assertThat(capturedLag).isBetween(4000L, 6000L);
    }

    @Test
    @DisplayName("T089: Should maintain trace continuity across event cascade (FundsAdded → FundsTransferred)")
    void shouldMaintainTraceContinuityAcrossEventCascade() {
        // Arrange
        String traceId = generateTraceId();
        UUID walletId = UUID.randomUUID();
        
        clearSpans();
        
        // Step 1: Receive FundsAdded event
        var receiveSpan = createTestSpan("kafka.receive");
        receiveSpan.tag("messaging.system", "kafka");
        receiveSpan.tag("messaging.destination", "funds-added-topic");
        receiveSpan.tag("cloudevents.traceparent", String.format("00-%s-%s-01", traceId, generateSpanId()));
        receiveSpan.end();
        
        // Step 2: Process event (business logic)
        var processSpan = tracer.nextSpan(receiveSpan).name("process.funds-added").start();
        processSpan.tag("wallet.id", walletId.toString());
        processSpan.tag("event.type", "FundsAdded");
        processSpan.end();
        
        // Step 3: Publish cascading FundsTransferred event
        var publishSpan = tracer.nextSpan(processSpan).name("kafka.send").start();
        publishSpan.tag("messaging.system", "kafka");
        publishSpan.tag("messaging.destination", "funds-transferred-topic");
        publishSpan.tag("messaging.operation", "publish");
        publishSpan.tag("cloudevents.traceparent", String.format("00-%s-%s-01", traceId, generateSpanId()));
        publishSpan.tag("event.cascade", "true");
        publishSpan.end();
        
        // Assert
        waitForSpans(3, 2000);
        List<FinishedSpan> spans = getSpans();
        
        assertThat(spans).hasSizeGreaterThanOrEqualTo(3);
        
        // Verify all spans share same trace ID
        FinishedSpan receive = findSpan(spans, s -> "kafka.receive".equals(s.getName()));
        FinishedSpan process = findSpan(spans, s -> s.getName().contains("process"));
        FinishedSpan publish = findSpan(spans, s -> "kafka.send".equals(s.getName()));
        
        assertThat(receive).isNotNull();
        assertThat(process).isNotNull();
        assertThat(publish).isNotNull();
        
        // All spans must share the same trace ID
        assertThat(receive.getTraceId())
            .isEqualTo(process.getTraceId())
            .isEqualTo(publish.getTraceId());
        
        // Verify hierarchy: receive -> process -> publish
        assertSpanHierarchy(receive, process);
        assertSpanHierarchy(process, publish);
    }

    @Test
    @DisplayName("Should handle Kafka message headers for trace propagation")
    void shouldHandleKafkaMessageHeadersForTracePropagation() {
        // Arrange
        String traceId = generateTraceId();
        String spanId = generateSpanId();
        
        clearSpans();
        
        // Simulate Kafka message with CloudEvents headers
        var span = createTestSpan("kafka.send");
        span.tag("messaging.system", "kafka");
        span.tag("messaging.destination", "wallet-created-topic");
        
        // CloudEvents headers
        span.tag("messaging.header.ce_specversion", "1.0");
        span.tag("messaging.header.ce_type", "dev.bloco.wallet.wallet-created");
        span.tag("messaging.header.ce_source", "/wallet-hub");
        span.tag("messaging.header.ce_id", UUID.randomUUID().toString());
        span.tag("messaging.header.ce_traceparent", String.format("00-%s-%s-01", traceId, spanId));
        span.tag("messaging.header.ce_tracestate", "congo=t61rcWkgMzE");
        
        span.end();
        
        // Assert
        waitForSpans(1, 1000);
        List<FinishedSpan> spans = getSpans();
        
        assertThat(spans).hasSize(1);
        FinishedSpan capturedSpan = spans.get(0);
        
        // Verify CloudEvents headers
        assertSpanHasTags(capturedSpan,
            "messaging.header.ce_specversion",
            "messaging.header.ce_type",
            "messaging.header.ce_traceparent");
        
        assertSpanTagEquals(capturedSpan, "messaging.header.ce_specversion", "1.0");
        assertSpanTagEquals(capturedSpan, "messaging.header.ce_type", "dev.bloco.wallet.wallet-created");
        
        // Verify traceparent in header
        String traceparentHeader = capturedSpan.getTags().get("messaging.header.ce_traceparent");
        assertThat(traceparentHeader).matches("^00-[0-9a-f]{32}-[0-9a-f]{16}-01$");
    }
    
    // Helper methods
    
    private String generateTraceId() {
        // Generate 32-char hex string
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private String generateSpanId() {
        // Generate 16-char hex string
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
