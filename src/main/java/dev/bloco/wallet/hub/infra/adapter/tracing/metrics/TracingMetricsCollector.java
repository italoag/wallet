package dev.bloco.wallet.hub.infra.adapter.tracing.metrics;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;

/**
 * Collects and exposes distributed tracing metrics via Micrometer.
 * 
 * <h2>Metrics Exposed</h2>
 * <ul>
 *   <li><b>tracing.spans.created</b>: Total spans created</li>
 *   <li><b>tracing.spans.exported</b>: Total spans exported</li>
 *   <li><b>tracing.spans.dropped</b>: Total spans dropped</li>
 *   <li><b>tracing.feature.flags.changes</b>: Feature flag change events</li>
 *   <li><b>tracing.feature.flags.state</b>: Current state of each feature flag (gauge)</li>
 * </ul>
 * 
 * <h2>Tags</h2>
 * <ul>
 *   <li><b>feature</b>: Feature name (api, database, kafka, stateMachine, externalApi, reactive)</li>
 *   <li><b>state</b>: Feature flag state (enabled, disabled)</li>
 * </ul>
 * 
 * <h2>Usage in Prometheus</h2>
 * <pre>
 * # Total spans created
 * tracing_spans_created_total
 * 
 * # Spans exported by type
 * tracing_spans_exported_total
 * 
 * # Feature flag state
 * tracing_feature_flags_state{feature="api"} 1.0
 * tracing_feature_flags_state{feature="database"} 1.0
 * </pre>
 */
@Component
public class TracingMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final Tracer tracer;
    private final TracingFeatureFlags featureFlags;
    
    private Counter spansCreatedCounter;
    private Counter spansExportedCounter;
    private Counter spansDroppedCounter;
    private Counter featureFlagChangesCounter;
    
    private final AtomicLong databaseFeatureState = new AtomicLong(0);
    private final AtomicLong kafkaFeatureState = new AtomicLong(0);
    private final AtomicLong stateMachineFeatureState = new AtomicLong(0);
    private final AtomicLong externalApiFeatureState = new AtomicLong(0);
    private final AtomicLong reactiveFeatureState = new AtomicLong(0);
    private final AtomicLong useCaseFeatureState = new AtomicLong(0);

    public TracingMetricsCollector(
            MeterRegistry meterRegistry,
            Tracer tracer,
            TracingFeatureFlags featureFlags) {
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
        this.featureFlags = featureFlags;
    }

    @PostConstruct
    public void init() {
        // Initialize counters
        spansCreatedCounter = Counter.builder("tracing.spans.created")
                .description("Total number of spans created")
                .register(meterRegistry);
        
        spansExportedCounter = Counter.builder("tracing.spans.exported")
                .description("Total number of spans exported")
                .register(meterRegistry);
        
        spansDroppedCounter = Counter.builder("tracing.spans.dropped")
                .description("Total number of spans dropped")
                .register(meterRegistry);
        
        featureFlagChangesCounter = Counter.builder("tracing.feature.flags.changes")
                .description("Total number of feature flag changes")
                .register(meterRegistry);
        
        // Initialize gauges for feature flag states
        registerFeatureFlagGauge("database", databaseFeatureState);
        registerFeatureFlagGauge("kafka", kafkaFeatureState);
        registerFeatureFlagGauge("stateMachine", stateMachineFeatureState);
        registerFeatureFlagGauge("externalApi", externalApiFeatureState);
        registerFeatureFlagGauge("reactive", reactiveFeatureState);
        registerFeatureFlagGauge("useCase", useCaseFeatureState);
        
        // Update initial states
        updateFeatureFlagStates();
    }

    /**
     * Register a gauge for a feature flag state.
     * 
     * @param featureName feature name
     * @param stateHolder atomic long holding state (1=enabled, 0=disabled)
     */
    private void registerFeatureFlagGauge(String featureName, AtomicLong stateHolder) {
        meterRegistry.gauge(
                "tracing.feature.flags.state",
                List.of(Tag.of("feature", featureName)),
                stateHolder
        );
    }

    /**
     * Update feature flag states from current configuration.
     */
    public void updateFeatureFlagStates() {
        databaseFeatureState.set(featureFlags.isDatabase() ? 1 : 0);
        kafkaFeatureState.set(featureFlags.isKafka() ? 1 : 0);
        stateMachineFeatureState.set(featureFlags.isStateMachine() ? 1 : 0);
        externalApiFeatureState.set(featureFlags.isExternalApi() ? 1 : 0);
        reactiveFeatureState.set(featureFlags.isReactive() ? 1 : 0);
        useCaseFeatureState.set(featureFlags.isUseCase() ? 1 : 0);
    }

    /**
     * Record span creation.
     */
    public void recordSpanCreated() {
        spansCreatedCounter.increment();
    }

    /**
     * Record span export.
     */
    public void recordSpanExported() {
        spansExportedCounter.increment();
    }

    /**
     * Record span drop.
     */
    public void recordSpanDropped() {
        spansDroppedCounter.increment();
    }

    /**
     * Record feature flag change.
     */
    public void recordFeatureFlagChange() {
        featureFlagChangesCounter.increment();
        updateFeatureFlagStates();
    }
}
