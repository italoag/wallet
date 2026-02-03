package dev.bloco.wallet.hub.infra.adapter.tracing.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TailSamplingSpanExporterTest {

    @Mock
    private SamplingConfiguration.SamplingDecisionEvaluator samplingEvaluator;

    @Mock
    private SamplingConfiguration.SamplingProperties samplingProperties;

    private TailSamplingSpanExporter exporter;
    private SamplingConfiguration.TailSamplingProperties tailSamplingProperties;

    @BeforeEach
    void setUp() {
        tailSamplingProperties = new SamplingConfiguration.TailSamplingProperties();
        tailSamplingProperties.setEnabled(true);
        tailSamplingProperties.setBufferDurationMs(100); // Fast buffer for testing
        tailSamplingProperties.setMaxBufferSize(100);
        tailSamplingProperties.setPropagateToChildren(true);

        when(samplingEvaluator.getProperties()).thenReturn(samplingProperties);
        when(samplingProperties.getTailSampling()).thenReturn(tailSamplingProperties);

        exporter = new TailSamplingSpanExporter(samplingEvaluator);
    }

    @Test
    void shouldExportAllSpansInTraceWhenOneMatchesAlwaysSampleRule() {
        // Given
        String traceId = "trace-1";
        String spanA = "span-a";
        String spanB = "span-b"; // This one will have error

        // Configure evaluator to sample spanB due to error (retroactive)
        when(samplingEvaluator.shouldSample(anyString())).thenReturn(false);
        when(samplingEvaluator.shouldRetroactivelySample(anyString(), anyLong(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(2));

        // When
        // Buffer normal span A
        exporter.bufferSpan(spanA, "normal-op", System.currentTimeMillis(), traceId, null);

        // Buffer error span B
        exporter.bufferSpan(spanB, "error-op", System.currentTimeMillis() + 10, traceId, spanA);

        // Complete span B with error
        exporter.completeSpan(spanB, 50, true);

        // Then
        // Wait for evaluation (buffer duration is 100ms)
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            var metrics = exporter.getMetrics();
            // Both spans should be sampled: B (forced by error) + A (forced by trace-level)
            assertThat(metrics).as("Sampled spans").containsEntry("sampled", 2L);
            assertThat(metrics).as("Sampled traces").containsEntry("traces_sampled", 1L);
        });
    }
}
