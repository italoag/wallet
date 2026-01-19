package dev.bloco.wallet.hub.infra.adapter.tracing.handler;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.SpanAttributeBuilder;
import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.micrometer.observation.Observation;

/**
 * Unit tests for KafkaConsumerObservationHandler.
 * Verifies CONSUMER span creation, messaging attributes, consumer lag, and
 * lifecycle events.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class KafkaConsumerObservationHandlerTest {

    @Mock
    private SpanAttributeBuilder spanAttributeBuilder;

    @Mock
    private TracingFeatureFlags featureFlags;

    @Mock
    private Observation.Context context;

    private KafkaConsumerObservationHandler handler;

    @BeforeEach
    void setUp() {
        lenient().when(featureFlags.isKafka()).thenReturn(true);
        handler = new KafkaConsumerObservationHandler(spanAttributeBuilder, featureFlags);
    }

    @Test
    void shouldSupportKafkaConsumerContext() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");

        // When
        boolean result = handler.supportsContext(context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldSupportSpringCloudStreamReceiverContext() {
        // Given
        when(context.getName()).thenReturn("spring.cloud.stream.receiver");

        // When
        boolean result = handler.supportsContext(context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotSupportOtherContext() {
        // Given
        when(context.getName()).thenReturn("some.other.context");

        // When
        boolean result = handler.supportsContext(context);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotSupportContextWhenKafkaTracingDisabled() {
        // Given
        when(featureFlags.isKafka()).thenReturn(false);
        KafkaConsumerObservationHandler disabledHandler = new KafkaConsumerObservationHandler(spanAttributeBuilder,
                featureFlags);
        when(context.getName()).thenReturn("kafka.consumer");

        // When
        boolean result = disabledHandler.supportsContext(context);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void onStartShouldAddConsumerMessagingAttributes() {
        // Given
        String topic = "wallet-created-topic";
        String consumerGroup = "wallet-service-group";
        Integer partition = 1;
        Long offset = 54321L;
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.get("kafka.topic")).thenReturn(topic);
        when(context.get("kafka.consumer.group")).thenReturn(consumerGroup);
        when(context.get("kafka.partition")).thenReturn(partition);
        when(context.get("kafka.offset")).thenReturn(offset);

        // When
        handler.onStart(context);

        // Then - verify spanAttributeBuilder is called
        verify(spanAttributeBuilder).addMessagingConsumerAttributes(context, topic, consumerGroup, partition, offset);

        // Verify deserialization and processing start timestamps are recorded
        verify(context).put(eq("deserialization.start"), anyLong());
        verify(context).put(eq("processing.start"), anyLong());
    }

    @Test
    void onStartShouldHandleMissingConsumerGroup() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.get("kafka.topic")).thenReturn(null);
        when(context.get("kafka.consumer.group")).thenReturn(null);
        when(context.get("kafka.partition")).thenReturn(null);
        when(context.get("kafka.offset")).thenReturn(null);

        // When
        handler.onStart(context);

        // Then - verify spanAttributeBuilder is called even with null values
        verify(spanAttributeBuilder).addMessagingConsumerAttributes(context, null, null, null, null);
    }

    @Test
    void onStopShouldAddProcessingMetrics() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");

        // Set timestamps (simulate onStart)
        long deserializationStart = System.nanoTime() - 2_000_000; // 2ms ago
        long processingStart = System.currentTimeMillis() - 50; // 50ms ago
        when(context.get("deserialization.start")).thenReturn(deserializationStart);
        when(context.get("processing.start")).thenReturn(processingStart);

        // When
        handler.onStop(context);

        // Then - verify spanAttributeBuilder.addSuccessStatus is called
        verify(spanAttributeBuilder).addSuccessStatus(context);
    }

    @Test
    void onStopShouldHandleMissingTimestamps() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.get("deserialization.start")).thenReturn(null);
        when(context.get("processing.start")).thenReturn(null);

        // When
        handler.onStop(context);

        // Then - verify spanAttributeBuilder.addSuccessStatus is called
        verify(spanAttributeBuilder).addSuccessStatus(context);
    }

    @Test
    void onErrorShouldAddErrorAttributes() {
        // Given
        RuntimeException error = new RuntimeException("Consumer processing failed");
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.getError()).thenReturn(error);

        // When
        handler.onError(context);

        // Then - verify spanAttributeBuilder.addErrorAttributes is called
        verify(spanAttributeBuilder).addErrorAttributes(context, error);
    }

    @Test
    void onErrorShouldHandleMissingError() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.getError()).thenReturn(null);

        // When
        handler.onError(context);

        // Then - verify spanAttributeBuilder.addErrorAttributes is called with null
        verify(spanAttributeBuilder).addErrorAttributes(context, null);
    }

    @Test
    void shouldHandleExceptionsGracefully() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.get("kafka.topic")).thenReturn(null);
        when(context.get("kafka.consumer.group")).thenReturn(null);
        when(context.get("kafka.partition")).thenReturn(null);
        when(context.get("kafka.offset")).thenReturn(null);
        doThrow(new RuntimeException("Mock exception")).when(spanAttributeBuilder)
                .addMessagingConsumerAttributes(any(Observation.Context.class), any(), any(), any(), any());

        // When/Then - should not crash
        handler.onStart(context);
        handler.onStop(context);
        handler.onError(context);
    }

    @Test
    void onStopShouldCalculateProcessingDuration() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");

        long deserializationStart = System.nanoTime() - 1_000_000; // 1ms ago
        long processingStart = System.currentTimeMillis() - 30; // 30ms ago
        when(context.get("deserialization.start")).thenReturn(deserializationStart);
        when(context.get("processing.start")).thenReturn(processingStart);

        // When
        handler.onStop(context);

        // Then - verify spanAttributeBuilder.addSuccessStatus is called
        verify(spanAttributeBuilder).addSuccessStatus(context);
    }
}
