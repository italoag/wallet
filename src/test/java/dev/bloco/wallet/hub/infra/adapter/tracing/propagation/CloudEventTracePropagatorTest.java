package dev.bloco.wallet.hub.infra.adapter.tracing.propagation;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CloudEventTracePropagator}.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>W3C traceparent injection (producer side)</li>
 *   <li>W3C traceparent extraction (consumer side)</li>
 *   <li>Trace context format validation (00-{traceId}-{spanId}-{flags})</li>
 *   <li>Sampling flag propagation (01=sampled, 00=not-sampled)</li>
 *   <li>Error handling (no active span, missing traceparent, invalid format)</li>
 *   <li>Parent-child span relationship establishment</li>
 *   <li>CloudEvent immutability (original event not modified)</li>
 * </ul>
 * 
 * @see CloudEventTracePropagator
 */
@DisplayName("CloudEventTracePropagator Tests")
class CloudEventTracePropagatorTest {

    private Tracer tracer;
    private CloudEventTracePropagator propagator;

    @BeforeEach
    void setUp() {
        tracer = mock(Tracer.class);
        propagator = new CloudEventTracePropagator(tracer);
    }

    @Nested
    @DisplayName("Trace Context Injection Tests (Producer Side)")
    class TraceContextInjectionTests {

        @Test
        @DisplayName("Should inject traceparent extension when active span exists")
        void shouldInjectTraceparentWhenActiveSpanExists() {
            // Arrange
            String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
            String spanId = "00f067aa0ba902b7";
            
            Span mockSpan = mock(Span.class);
            TraceContext mockContext = mock(TraceContext.class);
            
            when(tracer.currentSpan()).thenReturn(mockSpan);
            when(mockSpan.context()).thenReturn(mockContext);
            when(mockContext.traceId()).thenReturn(traceId);
            when(mockContext.spanId()).thenReturn(spanId);
            when(mockContext.sampled()).thenReturn(true);
            
            CloudEvent originalEvent = createTestCloudEvent("FundsAddedEvent");
            
            // Act
            CloudEvent enrichedEvent = propagator.injectTraceContext(originalEvent);
            
            // Assert
            assertNotNull(enrichedEvent);
            assertNotSame(originalEvent, enrichedEvent, "Should return a new CloudEvent instance");
            
            Object traceparent = enrichedEvent.getExtension("traceparent");
            assertNotNull(traceparent, "traceparent extension should be present");
            
            String expectedTraceparent = "00-" + traceId + "-" + spanId + "-01";
            assertEquals(expectedTraceparent, traceparent.toString());
            
            // Verify original event unchanged
            assertNull(originalEvent.getExtension("traceparent"), "Original event should not be modified");
        }

        @Test
        @DisplayName("Should use sampled flag '01' when trace is sampled")
        void shouldUseSampledFlagWhenTraceSampled() {
            // Arrange
            Span mockSpan = mock(Span.class);
            TraceContext mockContext = mock(TraceContext.class);
            
            when(tracer.currentSpan()).thenReturn(mockSpan);
            when(mockSpan.context()).thenReturn(mockContext);
            when(mockContext.traceId()).thenReturn("0af7651916cd43dd8448eb211c80319c");
            when(mockContext.spanId()).thenReturn("b9c7c989f97918e1");
            when(mockContext.sampled()).thenReturn(true);
            
            CloudEvent event = createTestCloudEvent("TestEvent");
            
            // Act
            CloudEvent result = propagator.injectTraceContext(event);
            
            // Assert
            String traceparent = result.getExtension("traceparent").toString();
            assertTrue(traceparent.endsWith("-01"), "Should end with sampled flag '01'");
        }

        @Test
        @DisplayName("Should use not-sampled flag '00' when trace is not sampled")
        void shouldUseNotSampledFlagWhenTraceNotSampled() {
            // Arrange
            Span mockSpan = mock(Span.class);
            TraceContext mockContext = mock(TraceContext.class);
            
            when(tracer.currentSpan()).thenReturn(mockSpan);
            when(mockSpan.context()).thenReturn(mockContext);
            when(mockContext.traceId()).thenReturn("0af7651916cd43dd8448eb211c80319c");
            when(mockContext.spanId()).thenReturn("b9c7c989f97918e1");
            when(mockContext.sampled()).thenReturn(false);
            
            CloudEvent event = createTestCloudEvent("TestEvent");
            
            // Act
            CloudEvent result = propagator.injectTraceContext(event);
            
            // Assert
            String traceparent = result.getExtension("traceparent").toString();
            assertTrue(traceparent.endsWith("-00"), "Should end with not-sampled flag '00'");
        }

        @Test
        @DisplayName("Should use not-sampled flag '00' when sampled is null")
        void shouldUseNotSampledFlagWhenSampledIsNull() {
            // Arrange
            Span mockSpan = mock(Span.class);
            TraceContext mockContext = mock(TraceContext.class);
            
            when(tracer.currentSpan()).thenReturn(mockSpan);
            when(mockSpan.context()).thenReturn(mockContext);
            when(mockContext.traceId()).thenReturn("0af7651916cd43dd8448eb211c80319c");
            when(mockContext.spanId()).thenReturn("b9c7c989f97918e1");
            when(mockContext.sampled()).thenReturn(null);
            
            CloudEvent event = createTestCloudEvent("TestEvent");
            
            // Act
            CloudEvent result = propagator.injectTraceContext(event);
            
            // Assert
            String traceparent = result.getExtension("traceparent").toString();
            assertTrue(traceparent.endsWith("-00"), "Should default to not-sampled flag '00' when sampled is null");
        }

        @Test
        @DisplayName("Should return original event unchanged when no active span")
        void shouldReturnOriginalEventWhenNoActiveSpan() {
            // Arrange
            when(tracer.currentSpan()).thenReturn(null);
            CloudEvent originalEvent = createTestCloudEvent("TestEvent");
            
            // Act
            CloudEvent result = propagator.injectTraceContext(originalEvent);
            
            // Assert
            assertSame(originalEvent, result, "Should return same instance when no active span");
            assertNull(result.getExtension("traceparent"), "Should not have traceparent extension");
        }

        @Test
        @DisplayName("Should return original event unchanged when span has no context")
        void shouldReturnOriginalEventWhenSpanHasNoContext() {
            // Arrange
            Span mockSpan = mock(Span.class);
            when(tracer.currentSpan()).thenReturn(mockSpan);
            when(mockSpan.context()).thenReturn(null);
            
            CloudEvent originalEvent = createTestCloudEvent("TestEvent");
            
            // Act
            CloudEvent result = propagator.injectTraceContext(originalEvent);
            
            // Assert
            assertSame(originalEvent, result, "Should return same instance when span has no context");
            assertNull(result.getExtension("traceparent"), "Should not have traceparent extension");
        }

        @Test
        @DisplayName("Should throw NullPointerException when event is null")
        void shouldThrowNullPointerExceptionWhenEventIsNull() {
            // Act & Assert
            assertThrows(NullPointerException.class, () -> {
                propagator.injectTraceContext(null);
            }, "Should throw NPE when CloudEvent is null");
        }

        @Test
        @DisplayName("Should preserve existing CloudEvent properties")
        void shouldPreserveExistingCloudEventProperties() {
            // Arrange
            Span mockSpan = mock(Span.class);
            TraceContext mockContext = mock(TraceContext.class);
            
            when(tracer.currentSpan()).thenReturn(mockSpan);
            when(mockSpan.context()).thenReturn(mockContext);
            when(mockContext.traceId()).thenReturn("trace123");
            when(mockContext.spanId()).thenReturn("span456");
            when(mockContext.sampled()).thenReturn(true);
            
            CloudEvent originalEvent = CloudEventBuilder.v1()
                    .withId("evt-12345")
                    .withType("WalletCreatedEvent")
                    .withSource(URI.create("/wallet-hub"))
                    .withDataContentType("application/json")
                    .withData("{\"walletId\":\"w-123\"}".getBytes())
                    .withExtension("custom", "value")
                    .build();
            
            // Act
            CloudEvent result = propagator.injectTraceContext(originalEvent);
            
            // Assert
            assertEquals(originalEvent.getId(), result.getId());
            assertEquals(originalEvent.getType(), result.getType());
            assertEquals(originalEvent.getSource(), result.getSource());
            assertEquals(originalEvent.getDataContentType(), result.getDataContentType());
            assertArrayEquals(originalEvent.getData().toBytes(), result.getData().toBytes());
            assertEquals("value", result.getExtension("custom"));
            assertNotNull(result.getExtension("traceparent"));
        }

        @Test
        @DisplayName("Should format traceparent according to W3C specification")
        void shouldFormatTraceparentAccordingToW3CSpec() {
            // Arrange
            String traceId = "0af7651916cd43dd8448eb211c80319c";  // 32 hex chars
            String spanId = "b9c7c989f97918e1";                   // 16 hex chars
            
            Span mockSpan = mock(Span.class);
            TraceContext mockContext = mock(TraceContext.class);
            
            when(tracer.currentSpan()).thenReturn(mockSpan);
            when(mockSpan.context()).thenReturn(mockContext);
            when(mockContext.traceId()).thenReturn(traceId);
            when(mockContext.spanId()).thenReturn(spanId);
            when(mockContext.sampled()).thenReturn(true);
            
            CloudEvent event = createTestCloudEvent("TestEvent");
            
            // Act
            CloudEvent result = propagator.injectTraceContext(event);
            
            // Assert
            String traceparent = result.getExtension("traceparent").toString();
            
            // W3C format: version-traceid-spanid-flags
            String[] parts = traceparent.split("-");
            assertEquals(4, parts.length, "traceparent should have 4 parts");
            assertEquals("00", parts[0], "Version should be 00");
            assertEquals(traceId, parts[1], "Trace ID should match");
            assertEquals(spanId, parts[2], "Span ID should match");
            assertEquals("01", parts[3], "Flags should be 01 (sampled)");
        }
    }

    @Nested
    @DisplayName("Trace Context Extraction Tests (Consumer Side)")
    class TraceContextExtractionTests {

        @Test
        @DisplayName("Should extract trace context and create child span")
        void shouldExtractTraceContextAndCreateChildSpan() {
            // Arrange
            String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
            String parentSpanId = "00f067aa0ba902b7";
            String traceparent = "00-" + traceId + "-" + parentSpanId + "-01";
            
            CloudEvent event = CloudEventBuilder.v1()
                    .withId("evt-123")
                    .withType("FundsAddedEvent")
                    .withSource(URI.create("/wallet-hub"))
                    .withExtension("traceparent", traceparent)
                    .build();
            
            // Mock the tracer's builder chain
            Span.Builder mockSpanBuilder = mock(Span.Builder.class);
            TraceContext.Builder mockContextBuilder = mock(TraceContext.Builder.class);
            TraceContext mockParentContext = mock(TraceContext.class);
            Span mockChildSpan = mock(Span.class);
            
            when(tracer.traceContextBuilder()).thenReturn(mockContextBuilder);
            when(mockContextBuilder.traceId(traceId)).thenReturn(mockContextBuilder);
            when(mockContextBuilder.spanId(parentSpanId)).thenReturn(mockContextBuilder);
            when(mockContextBuilder.sampled(true)).thenReturn(mockContextBuilder);
            when(mockContextBuilder.build()).thenReturn(mockParentContext);
            
            when(tracer.spanBuilder()).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.setParent(mockParentContext)).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.name("consume:FundsAddedEvent")).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.start()).thenReturn(mockChildSpan);
            
            // Act
            Span result = propagator.extractTraceContext(event);
            
            // Assert
            assertNotNull(result);
            assertEquals(mockChildSpan, result);
            
            // Verify span builder was called with correct parameters
            verify(tracer).traceContextBuilder();
            verify(mockContextBuilder).traceId(traceId);
            verify(mockContextBuilder).spanId(parentSpanId);
            verify(mockContextBuilder).sampled(true);
            verify(mockSpanBuilder).setParent(mockParentContext);
            verify(mockSpanBuilder).name("consume:FundsAddedEvent");
            verify(mockChildSpan).tag("span.kind", "CONSUMER");
        }

        @Test
        @DisplayName("Should parse sampled flag correctly (01 = true)")
        void shouldParseSampledFlagTrue() {
            // Arrange
            String traceparent = "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01";
            CloudEvent event = createTestCloudEventWithTraceparent(traceparent);
            
            TraceContext.Builder mockContextBuilder = mock(TraceContext.Builder.class);
            TraceContext mockParentContext = mock(TraceContext.class);
            Span.Builder mockSpanBuilder = mock(Span.Builder.class);
            Span mockSpan = mock(Span.class);
            
            when(tracer.traceContextBuilder()).thenReturn(mockContextBuilder);
            when(mockContextBuilder.traceId(any())).thenReturn(mockContextBuilder);
            when(mockContextBuilder.spanId(any())).thenReturn(mockContextBuilder);
            when(mockContextBuilder.sampled(anyBoolean())).thenReturn(mockContextBuilder);
            when(mockContextBuilder.build()).thenReturn(mockParentContext);
            when(tracer.spanBuilder()).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.setParent(any())).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.name(any())).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.start()).thenReturn(mockSpan);
            
            // Act
            propagator.extractTraceContext(event);
            
            // Assert
            verify(mockContextBuilder).sampled(true);
        }

        @Test
        @DisplayName("Should parse sampled flag correctly (00 = false)")
        void shouldParseSampledFlagFalse() {
            // Arrange
            String traceparent = "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-00";
            CloudEvent event = createTestCloudEventWithTraceparent(traceparent);
            
            TraceContext.Builder mockContextBuilder = mock(TraceContext.Builder.class);
            TraceContext mockParentContext = mock(TraceContext.class);
            Span.Builder mockSpanBuilder = mock(Span.Builder.class);
            Span mockSpan = mock(Span.class);
            
            when(tracer.traceContextBuilder()).thenReturn(mockContextBuilder);
            when(mockContextBuilder.traceId(any())).thenReturn(mockContextBuilder);
            when(mockContextBuilder.spanId(any())).thenReturn(mockContextBuilder);
            when(mockContextBuilder.sampled(anyBoolean())).thenReturn(mockContextBuilder);
            when(mockContextBuilder.build()).thenReturn(mockParentContext);
            when(tracer.spanBuilder()).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.setParent(any())).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.name(any())).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.start()).thenReturn(mockSpan);
            
            // Act
            propagator.extractTraceContext(event);
            
            // Assert
            verify(mockContextBuilder).sampled(false);
        }

        @Test
        @DisplayName("Should create new root span when traceparent is missing")
        void shouldCreateNewRootSpanWhenTraceparentMissing() {
            // Arrange
            CloudEvent event = createTestCloudEvent("TestEvent");  // No traceparent extension
            
            Span mockRootSpan = mock(Span.class);
            Span mockNamedSpan = mock(Span.class);
            Span mockStartedSpan = mock(Span.class);
            
            when(tracer.nextSpan()).thenReturn(mockRootSpan);
            when(mockRootSpan.name("consume:TestEvent")).thenReturn(mockNamedSpan);
            when(mockNamedSpan.start()).thenReturn(mockStartedSpan);
            
            // Act
            Span result = propagator.extractTraceContext(event);
            
            // Assert
            assertNotNull(result);
            assertEquals(mockStartedSpan, result);
            verify(tracer).nextSpan();
            verify(mockRootSpan).name("consume:TestEvent");
            verify(mockNamedSpan).start();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "invalid-format",
            "00-traceId-spanId",  // Missing flags
            "00-shortid-spanId-01",  // Invalid trace ID length
            "01-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01",  // Wrong version
            "",
            "   "
        })
        @DisplayName("Should create new root span for invalid traceparent formats")
        void shouldCreateNewRootSpanForInvalidTraceparentFormats(String invalidTraceparent) {
            // Arrange
            CloudEvent event = createTestCloudEventWithTraceparent(invalidTraceparent);
            
            Span mockRootSpan = mock(Span.class);
            Span mockNamedSpan = mock(Span.class);
            Span mockStartedSpan = mock(Span.class);
            
            when(tracer.nextSpan()).thenReturn(mockRootSpan);
            when(mockRootSpan.name(any())).thenReturn(mockNamedSpan);
            when(mockNamedSpan.start()).thenReturn(mockStartedSpan);
            
            // Act
            Span result = propagator.extractTraceContext(event);
            
            // Assert
            assertNotNull(result, "Should return a valid span even with invalid traceparent");
            verify(tracer).nextSpan();
            verify(mockStartedSpan).tag("span.kind", "CONSUMER");
        }

        @Test
        @DisplayName("Should create new root span when version is not '00'")
        void shouldCreateNewRootSpanWhenVersionIsNot00() {
            // Arrange
            String traceparent = "01-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01";
            CloudEvent event = createTestCloudEventWithTraceparent(traceparent);
            
            Span mockRootSpan = mock(Span.class);
            Span mockNamedSpan = mock(Span.class);
            Span mockStartedSpan = mock(Span.class);
            
            when(tracer.nextSpan()).thenReturn(mockRootSpan);
            when(mockRootSpan.name(any())).thenReturn(mockNamedSpan);
            when(mockNamedSpan.start()).thenReturn(mockStartedSpan);
            
            // Act
            Span result = propagator.extractTraceContext(event);
            
            // Assert
            assertNotNull(result);
            verify(tracer).nextSpan();
            verify(mockStartedSpan).tag("span.kind", "CONSUMER");
        }

        @Test
        @DisplayName("Should throw NullPointerException when event is null")
        void shouldThrowNullPointerExceptionWhenEventIsNull() {
            // Act & Assert
            assertThrows(NullPointerException.class, () -> {
                propagator.extractTraceContext(null);
            }, "Should throw NPE when CloudEvent is null");
        }

        @Test
        @DisplayName("Should handle extraction exception gracefully")
        void shouldHandleExtractionExceptionGracefully() {
            // Arrange
            String traceparent = "00-valid-trace-id-00";  // Will cause exception in parsing
            CloudEvent event = createTestCloudEventWithTraceparent(traceparent);
            
            Span mockRootSpan = mock(Span.class);
            Span mockNamedSpan = mock(Span.class);
            Span mockStartedSpan = mock(Span.class);
            
            when(tracer.nextSpan()).thenReturn(mockRootSpan);
            when(mockRootSpan.name(any())).thenReturn(mockNamedSpan);
            when(mockNamedSpan.start()).thenReturn(mockStartedSpan);
            
            // Act
            Span result = propagator.extractTraceContext(event);
            
            // Assert - should not throw exception, should create new root span
            assertNotNull(result);
            verify(tracer).nextSpan();
        }

        @Test
        @DisplayName("Should set span name to 'consume:{eventType}'")
        void shouldSetSpanNameToConsumeEventType() {
            // Arrange
            CloudEvent event = CloudEventBuilder.v1()
                    .withId("evt-123")
                    .withType("WalletCreatedEvent")
                    .withSource(URI.create("/wallet-hub"))
                    .withExtension("traceparent", "00-trace123-span456-01")
                    .build();
            
            TraceContext.Builder mockContextBuilder = mock(TraceContext.Builder.class);
            TraceContext mockParentContext = mock(TraceContext.class);
            Span.Builder mockSpanBuilder = mock(Span.Builder.class);
            Span mockSpan = mock(Span.class);
            
            when(tracer.traceContextBuilder()).thenReturn(mockContextBuilder);
            when(mockContextBuilder.traceId(any())).thenReturn(mockContextBuilder);
            when(mockContextBuilder.spanId(any())).thenReturn(mockContextBuilder);
            when(mockContextBuilder.sampled(anyBoolean())).thenReturn(mockContextBuilder);
            when(mockContextBuilder.build()).thenReturn(mockParentContext);
            when(tracer.spanBuilder()).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.setParent(any())).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.name(any())).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.start()).thenReturn(mockSpan);
            
            // Act
            propagator.extractTraceContext(event);
            
            // Assert
            verify(mockSpanBuilder).name("consume:WalletCreatedEvent");
        }

        @Test
        @DisplayName("Should tag span with kind=CONSUMER")
        void shouldTagSpanWithKindConsumer() {
            // Arrange
            CloudEvent event = createTestCloudEventWithTraceparent(
                "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01"
            );
            
            TraceContext.Builder mockContextBuilder = mock(TraceContext.Builder.class);
            TraceContext mockParentContext = mock(TraceContext.class);
            Span.Builder mockSpanBuilder = mock(Span.Builder.class);
            Span mockSpan = mock(Span.class);
            
            when(tracer.traceContextBuilder()).thenReturn(mockContextBuilder);
            when(mockContextBuilder.traceId(any())).thenReturn(mockContextBuilder);
            when(mockContextBuilder.spanId(any())).thenReturn(mockContextBuilder);
            when(mockContextBuilder.sampled(anyBoolean())).thenReturn(mockContextBuilder);
            when(mockContextBuilder.build()).thenReturn(mockParentContext);
            when(tracer.spanBuilder()).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.setParent(any())).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.name(any())).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.start()).thenReturn(mockSpan);
            
            // Act
            propagator.extractTraceContext(event);
            
            // Assert
            verify(mockSpan).tag("span.kind", "CONSUMER");
        }
    }

    @Nested
    @DisplayName("Round-Trip Tests (Inject + Extract)")
    class RoundTripTests {

        @Test
        @DisplayName("Should maintain trace continuity in round-trip scenario")
        void shouldMaintainTraceContinuityInRoundTrip() {
            // Arrange - Producer side
            String originalTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
            String producerSpanId = "00f067aa0ba902b7";
            
            Span mockProducerSpan = mock(Span.class);
            TraceContext mockProducerContext = mock(TraceContext.class);
            
            when(tracer.currentSpan()).thenReturn(mockProducerSpan);
            when(mockProducerSpan.context()).thenReturn(mockProducerContext);
            when(mockProducerContext.traceId()).thenReturn(originalTraceId);
            when(mockProducerContext.spanId()).thenReturn(producerSpanId);
            when(mockProducerContext.sampled()).thenReturn(true);
            
            CloudEvent originalEvent = createTestCloudEvent("FundsAddedEvent");
            
            // Act - Producer injects trace context
            CloudEvent enrichedEvent = propagator.injectTraceContext(originalEvent);
            
            // Verify injection
            assertNotNull(enrichedEvent.getExtension("traceparent"));
            String traceparent = enrichedEvent.getExtension("traceparent").toString();
            assertTrue(traceparent.contains(originalTraceId), "Should preserve trace ID");
            assertTrue(traceparent.contains(producerSpanId), "Should include producer span ID");
            
            // Arrange - Consumer side (different tracer state)
            TraceContext.Builder mockContextBuilder = mock(TraceContext.Builder.class);
            TraceContext mockExtractedContext = mock(TraceContext.class);
            Span.Builder mockSpanBuilder = mock(Span.Builder.class);
            Span mockConsumerSpan = mock(Span.class);
            
            when(tracer.traceContextBuilder()).thenReturn(mockContextBuilder);
            when(mockContextBuilder.traceId(originalTraceId)).thenReturn(mockContextBuilder);
            when(mockContextBuilder.spanId(producerSpanId)).thenReturn(mockContextBuilder);
            when(mockContextBuilder.sampled(true)).thenReturn(mockContextBuilder);
            when(mockContextBuilder.build()).thenReturn(mockExtractedContext);
            when(tracer.spanBuilder()).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.setParent(mockExtractedContext)).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.name(any())).thenReturn(mockSpanBuilder);
            when(mockSpanBuilder.start()).thenReturn(mockConsumerSpan);
            
            // Act - Consumer extracts trace context
            Span consumerSpan = propagator.extractTraceContext(enrichedEvent);
            
            // Assert - Trace continuity maintained
            assertNotNull(consumerSpan);
            verify(mockContextBuilder).traceId(originalTraceId);
            verify(mockContextBuilder).spanId(producerSpanId);
            verify(mockSpanBuilder).setParent(mockExtractedContext);
            verify(mockConsumerSpan).tag("span.kind", "CONSUMER");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle CloudEvent with existing extensions")
        void shouldHandleCloudEventWithExistingExtensions() {
            // Arrange
            Span mockSpan = mock(Span.class);
            TraceContext mockContext = mock(TraceContext.class);
            
            when(tracer.currentSpan()).thenReturn(mockSpan);
            when(mockSpan.context()).thenReturn(mockContext);
            when(mockContext.traceId()).thenReturn("trace123");
            when(mockContext.spanId()).thenReturn("span456");
            when(mockContext.sampled()).thenReturn(true);
            
            CloudEvent event = CloudEventBuilder.v1()
                    .withId("evt-123")
                    .withType("TestEvent")
                    .withSource(URI.create("/test"))
                    .withExtension("custom1", "value1")
                    .withExtension("custom2", "value2")
                    .build();
            
            // Act
            CloudEvent result = propagator.injectTraceContext(event);
            
            // Assert
            assertEquals("value1", result.getExtension("custom1"));
            assertEquals("value2", result.getExtension("custom2"));
            assertNotNull(result.getExtension("traceparent"));
        }

        @Test
        @DisplayName("Should handle very long trace IDs")
        void shouldHandleVeryLongTraceIds() {
            // Arrange
            String longTraceId = "a".repeat(32);  // 32 hex chars (standard length)
            String spanId = "b".repeat(16);       // 16 hex chars (standard length)
            
            Span mockSpan = mock(Span.class);
            TraceContext mockContext = mock(TraceContext.class);
            
            when(tracer.currentSpan()).thenReturn(mockSpan);
            when(mockSpan.context()).thenReturn(mockContext);
            when(mockContext.traceId()).thenReturn(longTraceId);
            when(mockContext.spanId()).thenReturn(spanId);
            when(mockContext.sampled()).thenReturn(true);
            
            CloudEvent event = createTestCloudEvent("TestEvent");
            
            // Act
            CloudEvent result = propagator.injectTraceContext(event);
            
            // Assert
            String traceparent = result.getExtension("traceparent").toString();
            assertTrue(traceparent.contains(longTraceId));
        }

        @Test
        @DisplayName("Should handle CloudEvent with null data")
        void shouldHandleCloudEventWithNullData() {
            // Arrange
            Span mockSpan = mock(Span.class);
            TraceContext mockContext = mock(TraceContext.class);
            
            when(tracer.currentSpan()).thenReturn(mockSpan);
            when(mockSpan.context()).thenReturn(mockContext);
            when(mockContext.traceId()).thenReturn("trace123");
            when(mockContext.spanId()).thenReturn("span456");
            when(mockContext.sampled()).thenReturn(true);
            
            CloudEvent event = CloudEventBuilder.v1()
                    .withId("evt-123")
                    .withType("TestEvent")
                    .withSource(URI.create("/test"))
                    .build();  // No data
            
            // Act
            CloudEvent result = propagator.injectTraceContext(event);
            
            // Assert
            assertNotNull(result);
            assertNotNull(result.getExtension("traceparent"));
        }
    }

    // Helper methods

    private CloudEvent createTestCloudEvent(String type) {
        return CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType(type)
                .withSource(URI.create("/wallet-hub"))
                .withDataContentType("application/json")
                .withData("{\"test\":\"data\"}".getBytes())
                .build();
    }

    private CloudEvent createTestCloudEventWithTraceparent(String traceparent) {
        return CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType("TestEvent")
                .withSource(URI.create("/wallet-hub"))
                .withExtension("traceparent", traceparent)
                .build();
    }
}
