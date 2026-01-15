package dev.bloco.wallet.hub.infra.adapter.tracing.health;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.micrometer.tracing.Tracer;

/**
 * Health indicator for distributed tracing infrastructure.
 * 
 * <h2>Health Checks</h2>
 * <ul>
 *   <li>Tracer availability and functionality</li>
 *   <li>Feature flag states</li>
 *   <li>Span creation capability</li>
 * </ul>
 * 
 * <h2>Health Status</h2>
 * <ul>
 *   <li><b>UP</b>: Tracing fully functional</li>
 *   <li><b>DOWN</b>: Tracer unavailable or non-functional</li>
 *   <li><b>UNKNOWN</b>: Unable to determine tracing state</li>
 * </ul>
 * 
 * <h2>Exposed Details</h2>
 * <pre>
 * {
 *   "status": "UP",
 *   "details": {
 *     "tracer.available": true,
 *     "tracer.type": "BraveTracer",
 *     "features.api": true,
 *     "features.database": true,
 *     "features.kafka": true,
 *     "features.stateMachine": true,
 *     "features.externalApi": true,
 *     "features.reactive": true,
 *     "span.creation.test": "success"
 *   }
 * }
 * </pre>
 */
@Component
public class TracingHealthIndicator implements HealthIndicator {

    private final Tracer tracer;
    private final TracingFeatureFlags featureFlags;

    public TracingHealthIndicator(Tracer tracer, TracingFeatureFlags featureFlags) {
        this.tracer = tracer;
        this.featureFlags = featureFlags;
    }

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new LinkedHashMap<>();
            
            // Check tracer availability
            boolean tracerAvailable = tracer != null;
            details.put("tracer.available", tracerAvailable);
            
            if (!tracerAvailable) {
                return Health.down()
                        .withDetails(details)
                        .build();
            }
            
            // Get tracer type
            details.put("tracer.type", tracer.getClass().getSimpleName());
            
            // Check feature flags
            details.put("features.database", featureFlags.isDatabase());
            details.put("features.kafka", featureFlags.isKafka());
            details.put("features.stateMachine", featureFlags.isStateMachine());
            details.put("features.externalApi", featureFlags.isExternalApi());
            details.put("features.reactive", featureFlags.isReactive());
            details.put("features.useCase", featureFlags.isUseCase());
            
            // Test span creation
            String spanTestResult = testSpanCreation();
            details.put("span.creation.test", spanTestResult);
            
            if ("success".equals(spanTestResult)) {
                return Health.up()
                        .withDetails(details)
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", spanTestResult)
                        .withDetails(details)
                        .build();
            }
            
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }

    /**
     * Test span creation capability.
     * 
     * @return "success" if span can be created, error message otherwise
     */
    private String testSpanCreation() {
        try {
            var testSpan = tracer.nextSpan().name("health-check");
            if (testSpan == null) {
                return "span creation returned null";
            }
            testSpan.start();
            testSpan.end();
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
}
