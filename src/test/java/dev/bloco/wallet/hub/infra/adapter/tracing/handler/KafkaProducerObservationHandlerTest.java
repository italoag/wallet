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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.SpanAttributeBuilder;
import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;

/**
 * Unit tests for KafkaProducerObservationHandler.
 * Verifies PRODUCER span creation, messaging attributes, and lifecycle events.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class KafkaProducerObservationHandlerTest {

    @Mock
    private SpanAttributeBuilder spanAttributeBuilder;

    @Mock
    private TracingFeatureFlags featureFlags;

    @Mock
    private Observation.Context context;

    private KafkaProducerObservationHandler handler;

    @BeforeEach
    void setUp() {
        when(featureFlags.isKafka()).thenReturn(true);
        handler = new KafkaProducerObservationHandler(spanAttributeBuilder, featureFlags);
    }

    @Test
    void shouldSupportKafkaProducerContext() {
        // Given
        when(context.getName()).thenReturn("kafka.producer");

        // When
        boolean result = handler.supportsContext(context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldSupportSpringCloudStreamSenderContext() {
        // Given
        when(context.getName()).thenReturn("spring.cloud.stream.sender");

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
        KafkaProducerObservationHandler disabledHandler = new KafkaProducerObservationHandler(spanAttributeBuilder,
                featureFlags);
        when(context.getName()).thenReturn("kafka.producer");

        // When
        boolean result = disabledHandler.supportsContext(context);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void onStartShouldAddMessagingAttributes() {
        // Given
        String topic = "wallet-created-topic";
        String messageId = "msg-001";
        Integer partition = 2;
        when(context.getName()).thenReturn("kafka.producer");
        when(context.get("kafka.topic")).thenReturn(topic);
        when(context.get("message.id")).thenReturn(messageId);
        when(context.get("kafka.partition")).thenReturn(partition);

        // When
        handler.onStart(context);

        // Then - verify spanAttributeBuilder is called
        verify(spanAttributeBuilder).addMessagingProducerAttributes(context, topic, messageId, partition);

        // Verify serialization start timestamp is recorded
        verify(context).put(eq("serialization.start"), anyLong());
    }

    @Test
    void onStartShouldHandleMissingTopic() {
        // Given
        when(context.getName()).thenReturn("kafka.producer");
        when(context.get("kafka.topic")).thenReturn(null);
        when(context.get("message.id")).thenReturn(null);
        when(context.get("kafka.partition")).thenReturn(null);

        // When
        handler.onStart(context);

        // Then - verify spanAttributeBuilder is called even with null values
        verify(spanAttributeBuilder).addMessagingProducerAttributes(context, null, null, null);
    }

    @Test
    void onStopShouldAddPartitionOffsetAndSuccessStatus() {
        // Given
        when(context.getName()).thenReturn("kafka.producer");
        when(context.get("kafka.partition")).thenReturn(2);
        when(context.get("kafka.offset")).thenReturn(12345L);

        // Set serialization start timestamp (simulate onStart)
        long startTime = System.nanoTime() - 5_000_000; // 5ms ago
        when(context.get("serialization.start")).thenReturn(startTime);

        // When
        handler.onStop(context);

        // Then - verify spanAttributeBuilder.addSuccessStatus is called
        verify(spanAttributeBuilder).addSuccessStatus(context);

        // Verify partition and offset are added
        ArgumentCaptor<KeyValue> keyValueCaptor = ArgumentCaptor.forClass(KeyValue.class);
        verify(context, atLeast(2)).addLowCardinalityKeyValue(keyValueCaptor.capture());

        var capturedKeyValues = keyValueCaptor.getAllValues();
        assertThat(capturedKeyValues).extracting(KeyValue::getKey)
                .contains(SpanAttributeBuilder.MESSAGING_KAFKA_PARTITION, SpanAttributeBuilder.MESSAGING_KAFKA_OFFSET);
    }

    @Test
    void onStopShouldHandleMissingPartitionAndOffset() {
        // Given
        when(context.getName()).thenReturn("kafka.producer");
        when(context.get("kafka.partition")).thenReturn(null);
        when(context.get("kafka.offset")).thenReturn(null);

        long startTime = System.nanoTime() - 3_000_000;
        when(context.get("serialization.start")).thenReturn(startTime);

        // When
        handler.onStop(context);

        // Then - verify spanAttributeBuilder.addSuccessStatus is called
        verify(spanAttributeBuilder).addSuccessStatus(context);
    }

    @Test
    void onStopShouldCalculateSerializationDuration() {
        // Given
        when(context.getName()).thenReturn("kafka.producer");
        long startTime = System.nanoTime() - 10_000_000; // 10ms ago
        when(context.get("serialization.start")).thenReturn(startTime);

        // When
        handler.onStop(context);

        // Then
        ArgumentCaptor<KeyValue> keyValueCaptor = ArgumentCaptor.forClass(KeyValue.class);
        verify(context, atLeast(1)).addLowCardinalityKeyValue(keyValueCaptor.capture());

        var capturedKeyValues = keyValueCaptor.getAllValues();
        assertThat(capturedKeyValues).extracting(KeyValue::getKey)
                .contains("messaging.kafka.serialization_time_ms");
    }

    @Test
    void onStopShouldHandleMissingSerializationStartTime() {
        // Given
        when(context.getName()).thenReturn("kafka.producer");
        when(context.get("serialization.start")).thenReturn(null);
        when(context.get("kafka.partition")).thenReturn(null);
        when(context.get("kafka.offset")).thenReturn(null);

        // When
        handler.onStop(context);

        // Then - verify spanAttributeBuilder.addSuccessStatus is called
        verify(spanAttributeBuilder).addSuccessStatus(context);
    }

    @Test
    void onErrorShouldAddErrorAttributes() {
        // Given
        RuntimeException error = new RuntimeException("Kafka send failed");
        when(context.getName()).thenReturn("kafka.producer");
        when(context.getError()).thenReturn(error);

        // When
        handler.onError(context);

        // Then - verify spanAttributeBuilder.addErrorAttributes is called
        verify(spanAttributeBuilder).addErrorAttributes(context, error);
    }

    @Test
    void onErrorShouldHandleMissingError() {
        // Given
        when(context.getName()).thenReturn("kafka.producer");
        when(context.getError()).thenReturn(null);

        // When
        handler.onError(context);

        // Then - verify spanAttributeBuilder.addErrorAttributes is called with null
        verify(spanAttributeBuilder).addErrorAttributes(context, null);
    }

    @Test
    void shouldHandleExceptionsGracefully() {
        // Given
        when(context.getName()).thenReturn("kafka.producer");
        doThrow(new RuntimeException("Mock exception")).when(context).addLowCardinalityKeyValue(any());

        // When/Then - should not crash
        handler.onStart(context);
        handler.onStop(context);
        handler.onError(context);
    }
}
