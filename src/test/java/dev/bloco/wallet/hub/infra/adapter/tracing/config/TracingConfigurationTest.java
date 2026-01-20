package dev.bloco.wallet.hub.infra.adapter.tracing.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TracingConfigurationTest {

    @Test
    @DisplayName("Should report valid configuration for Primary OTLP and Fallback Zipkin")
    void shouldReportValidConfigForPrimaryOtlpFallbackZipkin() {
        TracingConfiguration config = new TracingConfiguration();
        setField(config, "primaryBackend", "tempo");
        setField(config, "otlpEndpoint", "http://localhost:4318");
        setField(config, "fallbackBackend", "zipkin");
        setField(config, "zipkinEndpoint", "http://localhost:9411");

        String result = config.multiBackendExportConfiguration();

        assertThat(result)
                .contains("Multi-backend export configured")
                .contains("OTLP/Tempo (primary)")
                .contains("Zipkin (fallback)");
    }

    @Test
    @DisplayName("Should resolve 'otlp' as Tempo backend")
    void shouldResolveOtlpAsTempo() {
        TracingConfiguration config = new TracingConfiguration();
        setField(config, "primaryBackend", "otlp");
        setField(config, "otlpEndpoint", "http://localhost:4318");
        setField(config, "fallbackBackend", "none");

        String result = config.multiBackendExportConfiguration();

        assertThat(result).contains("OTLP/Tempo (primary)");
    }

    @Test
    @DisplayName("Should report error when no backends are configured")
    void shouldReportErrorWhenNoBackendsConfigured() {
        TracingConfiguration config = new TracingConfiguration();
        setField(config, "primaryBackend", "unknown");
        setField(config, "fallbackBackend", "unknown");

        String result = config.multiBackendExportConfiguration();

        assertThat(result)
                .startsWith("ERROR:")
                .contains("No trace backends properly configured");
    }

    @Test
    @DisplayName("Should warn when endpoint is missing")
    void shouldWarnWhenEndpointIsMissing() {
        // Since the method returns a string summary which might not include warnings
        // (logic says "Logging removed"),
        // we check what IS configured. If nothing is compliant, it should return ERROR.
        // But if one is valid and one is missing endpoint, it should list only the
        // valid one.

        TracingConfiguration config = new TracingConfiguration();
        setField(config, "primaryBackend", "tempo");
        setField(config, "otlpEndpoint", ""); // Missing
        setField(config, "fallbackBackend", "zipkin");
        setField(config, "zipkinEndpoint", "http://localhost:9411");

        String result = config.multiBackendExportConfiguration();

        assertThat(result)
                .doesNotContain("OTLP/Tempo (primary)")
                .contains("Zipkin (fallback)");
    }

    // Helper to set private fields using reflection
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}
