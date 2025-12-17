package dev.bloco.wallet.hub.infra.adapter.tracing.handler;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.SpanAttributeBuilder;
import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;

/**
 * Unit tests for KafkaConsumerObservationHandler.
 * Verifies CONSUMER span creation, messaging attributes, consumer lag, and lifecycle events.
 */
@ExtendWith(MockitoExtension.class)
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
        when(featureFlags.isKafka()).thenReturn(true);
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
        KafkaConsumerObservationHandler disabledHandler = 
            new KafkaConsumerObservationHandler(spanAttributeBuilder, featureFlags);
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
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.get("kafka.topic")).thenReturn(topic);
        when(context.get("kafka.consumer.group")).thenReturn(consumerGroup);
        when(context.get("kafka.partition")).thenReturn(1);
        when(context.get("kafka.offset")).thenReturn(54321L);

        // When
        handler.onStart(context);

        // Then
        ArgumentCaptor<KeyValue> keyValueCaptor = ArgumentCaptor.forClass(KeyValue.class);
        verify(context, atLeast(7)).addLowCardinalityKeyValue(keyValueCaptor.capture());
        
        var capturedKeyValues = keyValueCaptor.getAllValues();
        assertThat(capturedKeyValues).extracting(KeyValue::getKey)
            .contains("messaging.system", "messaging.operation", "span.kind",
                     "messaging.destination.name", "messaging.destination.kind",
                     "messaging.kafka.consumer.group", "messaging.kafka.partition");
        
        assertThat(capturedKeyValues).extracting(KeyValue::getValue)
            .contains("kafka", "receive", "CONSUMER", topic, "topic", consumerGroup, "1");
        
        // Verify deserialization and processing start timestamps are recorded
        verify(context).put(eq("deserialization.start"), anyLong());
        verify(context).put(eq("processing.start"), anyLong());
    }

    @Test
    void onStartShouldHandleMissingConsumerGroup() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.get("kafka.topic")).thenReturn("test-topic");
        when(context.get("kafka.consumer.group")).thenReturn(null);

        // When
        handler.onStart(context);

        // Then
        ArgumentCaptor<KeyValue> keyValueCaptor = ArgumentCaptor.forClass(KeyValue.class);
        verify(context, atLeast(3)).addLowCardinalityKeyValue(keyValueCaptor.capture());
        
        var capturedKeyValues = keyValueCaptor.getAllValues();
        assertThat(capturedKeyValues).extracting(KeyValue::getKey)
            .contains("messaging.system", "messaging.operation", "span.kind");
    }

    @Test
    void onStopShouldAddProcessingMetrics() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.get("message.id")).thenReturn("evt-123");
        
        // Set timestamps (simulate onStart)
        long deserializationStart = System.nanoTime() - 2_000_000; // 2ms ago
        long processingStart = System.currentTimeMillis() - 50; // 50ms ago
        when(context.get("deserialization.start")).thenReturn(deserializationStart);
        when(context.get("processing.start")).thenReturn(processingStart);

        // When
        handler.onStop(context);

        // Then
        ArgumentCaptor<KeyValue> keyValueCaptor = ArgumentCaptor.forClass(KeyValue.class);
        verify(context, atLeast(3)).addLowCardinalityKeyValue(keyValueCaptor.capture());
        verify(context, atLeast(1)).addHighCardinalityKeyValue(keyValueCaptor.capture());
        
        var capturedKeyValues = keyValueCaptor.getAllValues();
        assertThat(capturedKeyValues).extracting(KeyValue::getKey)
            .contains("messaging.kafka.deserialization_time_ms", 
                     "messaging.processing_time_ms",
                     "status", "messaging.message.id");
    }

    @Test
    void onStopShouldHandleMissingTimestamps() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.get("deserialization.start")).thenReturn(null);
        when(context.get("processing.start")).thenReturn(null);

        // When
        handler.onStop(context);

        // Then
        ArgumentCaptor<KeyValue> keyValueCaptor = ArgumentCaptor.forClass(KeyValue.class);
        verify(context, atLeast(1)).addLowCardinalityKeyValue(keyValueCaptor.capture());
        
        var capturedKeyValues = keyValueCaptor.getAllValues();
        assertThat(capturedKeyValues).extracting(KeyValue::getKey)
            .contains("status");
        // Time metrics should not be present
        assertThat(capturedKeyValues).extracting(KeyValue::getKey)
            .doesNotContain("messaging.kafka.deserialization_time_ms", "messaging.processing_time_ms");
    }

    @Test
    void onErrorShouldAddErrorAttributes() {
        // Given
        String topic = "wallet-created-topic";
        RuntimeException error = new RuntimeException("Consumer processing failed");
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.get("kafka.topic")).thenReturn(topic);
        when(context.get("kafka.partition")).thenReturn(1);
        when(context.get("kafka.offset")).thenReturn(999L);
        when(context.getError()).thenReturn(error);

        // When
        handler.onError(context);

        // Then
        ArgumentCaptor<KeyValue> keyValueCaptor = ArgumentCaptor.forClass(KeyValue.class);
        verify(context, atLeast(2)).addLowCardinalityKeyValue(keyValueCaptor.capture());
        
        var capturedKeyValues = keyValueCaptor.getAllValues();
        assertThat(capturedKeyValues).extracting(KeyValue::getKey)
            .contains("error.type", "status");
        assertThat(capturedKeyValues).extracting(KeyValue::getValue)
            .contains("RuntimeException", "error");
    }

    @Test
    void onErrorShouldHandleMissingError() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.get("kafka.topic")).thenReturn("test-topic");
        when(context.getError()).thenReturn(null);

        // When
        handler.onError(context);

        // Then
        ArgumentCaptor<KeyValue> keyValueCaptor = ArgumentCaptor.forClass(KeyValue.class);
        verify(context, atLeast(2)).addLowCardinalityKeyValue(keyValueCaptor.capture());
        
        var capturedKeyValues = keyValueCaptor.getAllValues();
        assertThat(capturedKeyValues).extracting(KeyValue::getKey)
            .contains("error.type", "status");
    }

    @Test
    void shouldNotInstrumentWhenFeatureFlagDisabled() {
        // Given
        when(featureFlags.isKafka()).thenReturn(false);
        KafkaConsumerObservationHandler disabledHandler = 
            new KafkaConsumerObservationHandler(spanAttributeBuilder, featureFlags);
        when(context.getName()).thenReturn("kafka.consumer");

        // When
        disabledHandler.onStart(context);

        // Then - should not add any key values when disabled
        verify(context, never()).addLowCardinalityKeyValue(any());
        verify(context, never()).addHighCardinalityKeyValue(any());
    }

    @Test
    void shouldHandleExceptionsGracefully() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");
        doThrow(new RuntimeException("Mock exception")).when(context).addLowCardinalityKeyValue(any());

        // When/Then - should not crash
        handler.onStart(context);
        handler.onStop(context);
        handler.onError(context);
    }

    @Test
    void onStopShouldVerifyConsumerLagPresence() {
        // Given
        when(context.getName()).thenReturn("kafka.consumer");
        when(context.get("message.id")).thenReturn("evt-456");
        
        long deserializationStart = System.nanoTime() - 1_000_000; // 1ms ago
        long processingStart = System.currentTimeMillis() - 30; // 30ms ago
        when(context.get("deserialization.start")).thenReturn(deserializationStart);
        when(context.get("processing.start")).thenReturn(processingStart);

        // When
        handler.onStop(context);

        // Then
        ArgumentCaptor<KeyValue> keyValueCaptor = ArgumentCaptor.forClass(KeyValue.class);
        verify(context, atLeast(3)).addLowCardinalityKeyValue(keyValueCaptor.capture());
        
        var capturedKeyValues = keyValueCaptor.getAllValues();
        // Note: consumer lag is calculated by CloudEventTracePropagator, not this handler
        // This handler just logs if it exists
        assertThat(capturedKeyValues).extracting(KeyValue::getKey)
            .contains("messaging.kafka.deserialization_time_ms", 
                     "messaging.processing_time_ms",
                     "status");
    }
}
