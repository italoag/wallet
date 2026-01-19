package dev.bloco.wallet.hub.infra.adapter.tracing.handler;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.SpanAttributeBuilder;
import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import dev.bloco.wallet.hub.infra.adapter.tracing.filter.SensitiveDataSanitizer;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.PoolMetrics;
import io.r2dbc.spi.ConnectionFactoryMetadata;

/**
 * Unit tests for {@link R2dbcObservationHandler}.
 * 
 * <p>
 * Tests verify:
 * </p>
 * <ul>
 * <li>Handler supports R2DBC observations</li>
 * <li>Connection acquisition timing is captured</li>
 * <li>Connection pool metrics are added</li>
 * <li>Database attributes are set correctly</li>
 * <li>Error handling adds appropriate tags</li>
 * <li>Feature flag behavior</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class R2dbcObservationHandlerTest {

    @Mock
    private TracingFeatureFlags featureFlags;

    @Mock
    private ConnectionPool connectionPool;

    @Mock
    private PoolMetrics poolMetrics;

    @Mock
    private ConnectionFactoryMetadata connectionFactoryMetadata;

    @Mock
    private SensitiveDataSanitizer sanitizer;

    private SpanAttributeBuilder spanAttributeBuilder;
    private R2dbcObservationHandler handler;

    @BeforeEach
    void setUp() {
        spanAttributeBuilder = new SpanAttributeBuilder(sanitizer);
        handler = new R2dbcObservationHandler(spanAttributeBuilder, featureFlags,
                java.util.Optional.of(connectionPool));

        // Default: feature flag enabled
        when(featureFlags.isDatabase()).thenReturn(true);
    }

    @Test
    void shouldSupportR2dbcObservations() {
        // Given
        Observation.Context context = new Observation.Context();
        context.setName("r2dbc.query");

        // When
        boolean supports = handler.supportsContext(context);

        // Then
        assertThat(supports).isTrue();
    }

    @Test
    void shouldNotSupportNonR2dbcObservations() {
        // Given
        Observation.Context context = new Observation.Context();
        context.setName("http.request");

        // When
        boolean supports = handler.supportsContext(context);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    void shouldNotSupportWhenFeatureFlagDisabled() {
        // Given
        when(featureFlags.isDatabase()).thenReturn(false);
        Observation.Context context = new Observation.Context();
        context.setName("r2dbc.connection");

        // When
        boolean supports = handler.supportsContext(context);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    void shouldRecordConnectionAcquisitionStartTime() {
        // Given
        Observation.Context context = new Observation.Context();
        context.setName("r2dbc.connection");

        // When
        handler.onStart(context);

        // Then
        Long startTime = context.get("connection.acquisition.start");
        assertThat(startTime).isNotNull();
        assertThat(startTime).isGreaterThan(0);
    }

    @Test
    void shouldAddConnectionPoolMetricsOnStop() {
        // Given
        Observation.Context context = new Observation.Context();
        context.setName("r2dbc.query");
        context.put("connection.acquisition.start", System.nanoTime() - 5_000_000); // 5ms ago

        when(connectionPool.getMetrics()).thenReturn(Optional.of(poolMetrics));
        when(poolMetrics.acquiredSize()).thenReturn(3);
        when(poolMetrics.idleSize()).thenReturn(7);
        when(poolMetrics.getMaxAllocatedSize()).thenReturn(10);
        when(poolMetrics.pendingAcquireSize()).thenReturn(1);

        // When
        handler.onStop(context);

        // Then
        KeyValue activeKv = context.getLowCardinalityKeyValue("db.connection_pool.active");
        assertThat(activeKv).isNotNull();
        assertThat(activeKv.getValue()).isEqualTo("3");

        KeyValue idleKv = context.getLowCardinalityKeyValue("db.connection_pool.idle");
        assertThat(idleKv).isNotNull();
        assertThat(idleKv.getValue()).isEqualTo("7");

        KeyValue maxKv = context.getLowCardinalityKeyValue("db.connection_pool.max");
        assertThat(maxKv).isNotNull();
        assertThat(maxKv.getValue()).isEqualTo("10");

        KeyValue pendingKv = context.getLowCardinalityKeyValue("db.connection_pool.pending");
        assertThat(pendingKv).isNotNull();
        assertThat(pendingKv.getValue()).isEqualTo("1");

        KeyValue statusKv = context.getLowCardinalityKeyValue("status");
        assertThat(statusKv).isNotNull();
        assertThat(statusKv.getValue()).isEqualTo("success");
    }

    @Test
    void shouldCalculateConnectionAcquisitionTime() {
        // Given
        Observation.Context context = new Observation.Context();
        context.setName("r2dbc.connection");
        context.put("connection.acquisition.start", System.nanoTime() - 5_000_000); // 5ms ago

        when(connectionPool.getMetrics()).thenReturn(Optional.empty());

        // When
        handler.onStop(context);

        // Then
        KeyValue acquisitionTimeKv = context.getLowCardinalityKeyValue("db.connection.acquisition_time_ms");
        assertThat(acquisitionTimeKv).isNotNull();
        // Should be approximately 5ms (allowing some tolerance)
        double timeMs = Double.parseDouble(acquisitionTimeKv.getValue());
        assertThat(timeMs).isGreaterThanOrEqualTo(4.0).isLessThanOrEqualTo(10.0);
    }

    @Test
    void shouldAddDatabaseSystemAttribute() {
        // Given
        Observation.Context context = new Observation.Context();
        context.setName("r2dbc.query.select");

        when(connectionPool.getMetrics()).thenReturn(Optional.empty());
        when(connectionPool.getMetadata()).thenReturn(connectionFactoryMetadata);
        when(connectionFactoryMetadata.getName()).thenReturn("PostgreSQL");

        // When
        handler.onStop(context);

        // Then
        KeyValue dbSystem = context.getLowCardinalityKeyValue(SpanAttributeBuilder.DB_SYSTEM);
        assertThat(dbSystem).isNotNull();
        assertThat(dbSystem.getValue()).isEqualTo("postgresql");
    }

    @Test
    void shouldDeriveOperationFromName() {
        // Given
        Observation.Context context = new Observation.Context();
        context.setName("r2dbc.query.select");

        when(connectionPool.getMetrics()).thenReturn(Optional.empty());

        // When
        handler.onStop(context);

        // Then
        KeyValue operation = context.getLowCardinalityKeyValue(SpanAttributeBuilder.DB_OPERATION);
        assertThat(operation).isNotNull();
        assertThat(operation.getValue()).isEqualTo("SELECT");
    }

    @Test
    void shouldAddErrorAttributesOnError() {
        // Given
        // Configure sanitizer mock to return input unchanged
        when(sanitizer.sanitizeExceptionMessage(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Observation.Context context = new Observation.Context();
        context.setName("r2dbc.query");
        RuntimeException error = new RuntimeException("Connection timeout");
        context.setError(error);

        // When
        handler.onError(context);

        // Then
        KeyValue errorKv = context.getLowCardinalityKeyValue(SpanAttributeBuilder.ERROR);
        assertThat(errorKv).isNotNull();
        assertThat(errorKv.getValue()).isEqualTo("true");

        KeyValue errorTypeKv = context.getLowCardinalityKeyValue(SpanAttributeBuilder.ERROR_TYPE);
        assertThat(errorTypeKv).isNotNull();
        assertThat(errorTypeKv.getValue()).isEqualTo("RuntimeException");

        KeyValue errorMsgKv = context.getHighCardinalityKeyValue(SpanAttributeBuilder.ERROR_MESSAGE);
        assertThat(errorMsgKv).isNotNull();
        assertThat(errorMsgKv.getValue()).contains("Connection timeout");

        KeyValue statusKv = context.getLowCardinalityKeyValue("status");
        assertThat(statusKv).isNotNull();
        assertThat(statusKv.getValue()).isEqualTo("error");
    }

    @Test
    void shouldCalculatePoolUtilization() {
        // Given
        Observation.Context context = new Observation.Context();
        context.setName("r2dbc.query");

        when(connectionPool.getMetrics()).thenReturn(Optional.of(poolMetrics));
        when(poolMetrics.acquiredSize()).thenReturn(8); // 8 active
        when(poolMetrics.idleSize()).thenReturn(2); // 2 idle
        when(poolMetrics.getMaxAllocatedSize()).thenReturn(10); // 10 max
        when(poolMetrics.pendingAcquireSize()).thenReturn(0);

        // When
        handler.onStop(context);

        // Then
        KeyValue utilization = context.getLowCardinalityKeyValue("db.connection_pool.utilization_percent");
        assertThat(utilization).isNotNull();
        assertThat(utilization.getValue()).isEqualTo("80.0"); // 8/10 = 80%
    }

    @Test
    void shouldHandleNullPoolMetrics() {
        // Given
        Observation.Context context = new Observation.Context();
        context.setName("r2dbc.query");

        when(connectionPool.getMetrics()).thenReturn(Optional.empty());

        // When/Then - should not throw exception
        handler.onStop(context);

        // Pool metrics should not be present
        assertThat(context.getLowCardinalityKeyValue("db.connection_pool.active")).isNull();
    }

    @Test
    void shouldSkipWhenFeatureFlagDisabled() {
        // Given
        when(featureFlags.isDatabase()).thenReturn(false);
        Observation.Context context = new Observation.Context();
        context.setName("r2dbc.query");

        // When
        handler.onStart(context);
        handler.onStop(context);

        // Then - no attributes should be added
        Long startTime = context.get("connection.acquisition.start");
        assertThat(startTime).isNull();
        assertThat(context.getLowCardinalityKeyValue("status")).isNull();
    }
}
