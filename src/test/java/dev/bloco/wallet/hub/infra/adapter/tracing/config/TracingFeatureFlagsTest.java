package dev.bloco.wallet.hub.infra.adapter.tracing.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TracingFeatureFlags}.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>Default values (all flags true by default)</li>
 *   <li>Getter and setter functionality (Lombok-generated)</li>
 *   <li>Helper methods (isAnyTracingEnabled, isAllTracingDisabled, etc.)</li>
 *   <li>Component summary methods (getEnabledComponents, getDisabledComponents)</li>
 *   <li>Log configuration method</li>
 * </ul>
 * 
 * <p><b>Note on Property Binding and @RefreshScope Testing:</b></p>
 * Testing @ConfigurationProperties binding and @RefreshScope behavior requires a full
 * Spring Boot integration test. These unit tests focus on the class behavior without
 * Spring context to validate core functionality.
 * 
 * @see TracingFeatureFlags
 */
@DisplayName("TracingFeatureFlags Tests")
class TracingFeatureFlagsTest {

    @Nested
    @DisplayName("Default Configuration Tests")
    class DefaultConfigurationTests {

        @Test
        @DisplayName("Should have all flags enabled by default")
        void shouldHaveAllFlagsEnabledByDefault() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            assertTrue(flags.isDatabase(), "Database tracing should be enabled by default");
            assertTrue(flags.isKafka(), "Kafka tracing should be enabled by default");
            assertTrue(flags.isStateMachine(), "StateMachine tracing should be enabled by default");
            assertTrue(flags.isExternalApi(), "ExternalApi tracing should be enabled by default");
            assertTrue(flags.isReactive(), "Reactive tracing should be enabled by default");
            assertTrue(flags.isUseCase(), "UseCase tracing should be enabled by default");
        }

        @Test
        @DisplayName("Should report any tracing enabled when all defaults active")
        void shouldReportAnyTracingEnabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            assertTrue(flags.isAnyTracingEnabled(), 
                "Should report tracing enabled when all defaults are true");
        }

        @Test
        @DisplayName("Should not report all tracing disabled when defaults active")
        void shouldNotReportAllTracingDisabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            assertFalse(flags.isAllTracingDisabled(), 
                "Should not report all tracing disabled with default configuration");
        }

        @Test
        @DisplayName("Should list all components as enabled")
        void shouldListAllComponentsAsEnabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            String enabled = flags.getEnabledComponents();
            
            assertNotNull(enabled);
            assertTrue(enabled.contains("database"));
            assertTrue(enabled.contains("kafka"));
            assertTrue(enabled.contains("stateMachine"));
            assertTrue(enabled.contains("externalApi"));
            assertTrue(enabled.contains("reactive"));
            assertTrue(enabled.contains("useCase"));
        }

        @Test
        @DisplayName("Should list no components as disabled")
        void shouldListNoComponentsAsDisabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            String disabled = flags.getDisabledComponents();
            
            assertEquals("none", disabled, "No components should be disabled by default");
        }
    }

    @Nested
    @DisplayName("Property Modification Tests")
    class PropertyModificationTests {

        @Test
        @DisplayName("Should allow disabling individual flags")
        void shouldAllowDisablingIndividualFlags() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            flags.setDatabase(false);
            flags.setKafka(false);
            flags.setStateMachine(false);
            
            assertFalse(flags.isDatabase());
            assertFalse(flags.isKafka());
            assertFalse(flags.isStateMachine());
            // Others remain at default
            assertTrue(flags.isExternalApi());
            assertTrue(flags.isReactive());
            assertTrue(flags.isUseCase());
        }

        @Test
        @DisplayName("Should report any tracing enabled when some flags are true")
        void shouldReportAnyTracingEnabledWithMixedFlags() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(false);
            flags.setStateMachine(false);
            // externalApi, reactive, useCase remain true
            
            assertTrue(flags.isAnyTracingEnabled(), 
                "Should report tracing enabled when at least one flag is true");
        }

        @Test
        @DisplayName("Should list correct enabled and disabled components")
        void shouldListCorrectComponents() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(false);
            flags.setStateMachine(false);
            
            String enabled = flags.getEnabledComponents();
            assertFalse(enabled.contains("database"));
            assertFalse(enabled.contains("kafka"));
            assertFalse(enabled.contains("stateMachine"));
            assertTrue(enabled.contains("externalApi"));
            assertTrue(enabled.contains("reactive"));
            assertTrue(enabled.contains("useCase"));
            
            String disabled = flags.getDisabledComponents();
            assertTrue(disabled.contains("database"));
            assertTrue(disabled.contains("kafka"));
            assertTrue(disabled.contains("stateMachine"));
        }
    }

    @Nested
    @DisplayName("All Disabled Configuration Tests")
    class AllDisabledConfigurationTests {

        @Test
        @DisplayName("Should have all flags disabled when set")
        void shouldHaveAllFlagsDisabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(false);
            flags.setStateMachine(false);
            flags.setExternalApi(false);
            flags.setReactive(false);
            flags.setUseCase(false);
            
            assertFalse(flags.isDatabase());
            assertFalse(flags.isKafka());
            assertFalse(flags.isStateMachine());
            assertFalse(flags.isExternalApi());
            assertFalse(flags.isReactive());
            assertFalse(flags.isUseCase());
        }

        @Test
        @DisplayName("Should report no tracing enabled")
        void shouldReportNoTracingEnabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(false);
            flags.setStateMachine(false);
            flags.setExternalApi(false);
            flags.setReactive(false);
            flags.setUseCase(false);
            
            assertFalse(flags.isAnyTracingEnabled(), 
                "Should report no tracing enabled when all flags are false");
        }

        @Test
        @DisplayName("Should report all tracing disabled")
        void shouldReportAllTracingDisabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(false);
            flags.setStateMachine(false);
            flags.setExternalApi(false);
            flags.setReactive(false);
            flags.setUseCase(false);
            
            assertTrue(flags.isAllTracingDisabled(), 
                "Should report all tracing disabled when all flags are false");
        }

        @Test
        @DisplayName("Should list no components as enabled")
        void shouldListNoComponentsAsEnabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(false);
            flags.setStateMachine(false);
            flags.setExternalApi(false);
            flags.setReactive(false);
            flags.setUseCase(false);
            
            String enabled = flags.getEnabledComponents();
            assertEquals("none", enabled, "No components should be enabled");
        }

        @Test
        @DisplayName("Should list all components as disabled")
        void shouldListAllComponentsAsDisabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(false);
            flags.setStateMachine(false);
            flags.setExternalApi(false);
            flags.setReactive(false);
            flags.setUseCase(false);
            
            String disabled = flags.getDisabledComponents();
            
            assertTrue(disabled.contains("database"));
            assertTrue(disabled.contains("kafka"));
            assertTrue(disabled.contains("stateMachine"));
            assertTrue(disabled.contains("externalApi"));
            assertTrue(disabled.contains("reactive"));
            assertTrue(disabled.contains("useCase"));
        }
    }

    @Nested
    @DisplayName("Selective Enablement Tests")
    class SelectiveEnablementTests {

        @Test
        @DisplayName("Should enable only specific components")
        void shouldEnableOnlySpecifiedComponents() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(true);
            flags.setStateMachine(false);
            flags.setExternalApi(true);
            flags.setReactive(false);
            flags.setUseCase(true);
            
            assertFalse(flags.isDatabase());
            assertTrue(flags.isKafka());
            assertFalse(flags.isStateMachine());
            assertTrue(flags.isExternalApi());
            assertFalse(flags.isReactive());
            assertTrue(flags.isUseCase());
        }

        @Test
        @DisplayName("Should report any tracing enabled")
        void shouldReportAnyTracingEnabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(true);
            flags.setStateMachine(false);
            flags.setExternalApi(true);
            flags.setReactive(false);
            flags.setUseCase(true);
            
            assertTrue(flags.isAnyTracingEnabled());
        }

        @Test
        @DisplayName("Should not report all tracing disabled")
        void shouldNotReportAllTracingDisabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(true);
            flags.setStateMachine(false);
            flags.setExternalApi(true);
            flags.setReactive(false);
            flags.setUseCase(true);
            
            assertFalse(flags.isAllTracingDisabled());
        }

        @Test
        @DisplayName("Should list only enabled components")
        void shouldListOnlyEnabledComponents() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(true);
            flags.setStateMachine(false);
            flags.setExternalApi(true);
            flags.setReactive(false);
            flags.setUseCase(true);
            
            String enabled = flags.getEnabledComponents();
            
            assertFalse(enabled.contains("database"));
            assertTrue(enabled.contains("kafka"));
            assertFalse(enabled.contains("stateMachine"));
            assertTrue(enabled.contains("externalApi"));
            assertFalse(enabled.contains("reactive"));
            assertTrue(enabled.contains("useCase"));
        }

        @Test
        @DisplayName("Should list only disabled components")
        void shouldListOnlyDisabledComponents() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(true);
            flags.setStateMachine(false);
            flags.setExternalApi(true);
            flags.setReactive(false);
            flags.setUseCase(true);
            
            String disabled = flags.getDisabledComponents();
            
            assertTrue(disabled.contains("database"));
            assertFalse(disabled.contains("kafka"));
            assertTrue(disabled.contains("stateMachine"));
            assertFalse(disabled.contains("externalApi"));
            assertTrue(disabled.contains("reactive"));
            assertFalse(disabled.contains("useCase"));
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests (Lombok)")
    class GetterSetterTests {

        @Test
        @DisplayName("Should allow programmatic setting of database flag")
        void shouldAllowProgrammaticSettingOfDatabaseFlag() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            flags.setDatabase(false);
            assertFalse(flags.isDatabase());
            
            flags.setDatabase(true);
            assertTrue(flags.isDatabase());
        }

        @Test
        @DisplayName("Should allow programmatic setting of kafka flag")
        void shouldAllowProgrammaticSettingOfKafkaFlag() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            flags.setKafka(false);
            assertFalse(flags.isKafka());
            
            flags.setKafka(true);
            assertTrue(flags.isKafka());
        }

        @Test
        @DisplayName("Should allow programmatic setting of stateMachine flag")
        void shouldAllowProgrammaticSettingOfStateMachineFlag() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            flags.setStateMachine(false);
            assertFalse(flags.isStateMachine());
            
            flags.setStateMachine(true);
            assertTrue(flags.isStateMachine());
        }

        @Test
        @DisplayName("Should allow programmatic setting of externalApi flag")
        void shouldAllowProgrammaticSettingOfExternalApiFlag() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            flags.setExternalApi(false);
            assertFalse(flags.isExternalApi());
            
            flags.setExternalApi(true);
            assertTrue(flags.isExternalApi());
        }

        @Test
        @DisplayName("Should allow programmatic setting of reactive flag")
        void shouldAllowProgrammaticSettingOfReactiveFlag() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            flags.setReactive(false);
            assertFalse(flags.isReactive());
            
            flags.setReactive(true);
            assertTrue(flags.isReactive());
        }

        @Test
        @DisplayName("Should allow programmatic setting of useCase flag")
        void shouldAllowProgrammaticSettingOfUseCaseFlag() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            flags.setUseCase(false);
            assertFalse(flags.isUseCase());
            
            flags.setUseCase(true);
            assertTrue(flags.isUseCase());
        }

        @Test
        @DisplayName("Should allow setting multiple flags programmatically")
        void shouldAllowSettingMultipleFlagsProgrammatically() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            // Set all to false
            flags.setDatabase(false);
            flags.setKafka(false);
            flags.setStateMachine(false);
            flags.setExternalApi(false);
            flags.setReactive(false);
            flags.setUseCase(false);
            
            assertTrue(flags.isAllTracingDisabled());
            
            // Set some back to true
            flags.setKafka(true);
            flags.setUseCase(true);
            
            assertTrue(flags.isAnyTracingEnabled());
            assertFalse(flags.isAllTracingDisabled());
        }
    }

    @Nested
    @DisplayName("Helper Method Edge Cases")
    class HelperMethodEdgeCasesTests {

        @Test
        @DisplayName("Should handle only database enabled scenario")
        void shouldHandleOnlyDatabaseEnabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(true);
            flags.setKafka(false);
            flags.setStateMachine(false);
            flags.setExternalApi(false);
            flags.setReactive(false);
            flags.setUseCase(false);
            
            assertTrue(flags.isAnyTracingEnabled());
            assertFalse(flags.isAllTracingDisabled());
            assertEquals("database", flags.getEnabledComponents());
            assertTrue(flags.getDisabledComponents().contains("kafka"));
            assertTrue(flags.getDisabledComponents().contains("useCase"));
        }

        @Test
        @DisplayName("Should handle only one component disabled")
        void shouldHandleOnlyOneComponentDisabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(true);
            flags.setKafka(true);
            flags.setStateMachine(true);
            flags.setExternalApi(true);
            flags.setReactive(true);
            flags.setUseCase(false);  // Only this one disabled
            
            assertTrue(flags.isAnyTracingEnabled());
            assertFalse(flags.isAllTracingDisabled());
            assertFalse(flags.getEnabledComponents().contains("useCase"));
            assertEquals("useCase", flags.getDisabledComponents());
        }

        @Test
        @DisplayName("Should format enabled components list correctly")
        void shouldFormatEnabledComponentsListCorrectly() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(true);
            flags.setKafka(true);
            flags.setStateMachine(false);
            flags.setExternalApi(false);
            flags.setReactive(false);
            flags.setUseCase(false);
            
            String enabled = flags.getEnabledComponents();
            
            assertTrue(enabled.contains("database"));
            assertTrue(enabled.contains("kafka"));
            assertTrue(enabled.contains(","), "Should contain comma separator");
        }

        @Test
        @DisplayName("Should format disabled components list correctly")
        void shouldFormatDisabledComponentsListCorrectly() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(false);
            flags.setStateMachine(true);
            flags.setExternalApi(true);
            flags.setReactive(true);
            flags.setUseCase(true);
            
            String disabled = flags.getDisabledComponents();
            
            assertTrue(disabled.contains("database"));
            assertTrue(disabled.contains("kafka"));
            assertTrue(disabled.contains(","), "Should contain comma separator");
        }
    }

    @Nested
    @DisplayName("Integration Scenario Tests")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("Should support read-heavy workload optimization")
        void shouldSupportReadHeavyWorkloadOptimization() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);  // Disable for optimization
            
            assertFalse(flags.isDatabase());
            assertTrue(flags.isAnyTracingEnabled());
            assertEquals("database", flags.getDisabledComponents());
        }

        @Test
        @DisplayName("Should support minimal tracing configuration")
        void shouldSupportMinimalTracing() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(false);
            flags.setStateMachine(false);
            flags.setExternalApi(true);
            flags.setReactive(false);
            flags.setUseCase(true);
            
            assertTrue(flags.isAnyTracingEnabled());
            String enabled = flags.getEnabledComponents();
            assertTrue(enabled.contains("externalApi"));
            assertTrue(enabled.contains("useCase"));
        }

        @Test
        @DisplayName("Should support full observability")
        void shouldSupportFullObservability() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            assertTrue(flags.isAnyTracingEnabled());
            assertFalse(flags.isAllTracingDisabled());
            assertEquals("none", flags.getDisabledComponents());
        }
    }

    @Nested
    @DisplayName("Log Configuration Method Tests")
    class LogConfigurationTests {

        @Test
        @DisplayName("Should execute logConfiguration without exceptions")
        void shouldExecuteLogConfigurationWithoutExceptions() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            
            assertDoesNotThrow(() -> flags.logConfiguration());
        }

        @Test
        @DisplayName("Should execute logConfiguration with all flags disabled")
        void shouldExecuteLogConfigurationWithAllDisabled() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(false);
            flags.setKafka(false);
            flags.setStateMachine(false);
            flags.setExternalApi(false);
            flags.setReactive(false);
            flags.setUseCase(false);
            
            assertDoesNotThrow(() -> flags.logConfiguration());
        }

        @Test
        @DisplayName("Should execute logConfiguration with mixed flags")
        void shouldExecuteLogConfigurationWithMixedFlags() {
            TracingFeatureFlags flags = new TracingFeatureFlags();
            flags.setDatabase(true);
            flags.setKafka(false);
            flags.setStateMachine(true);
            flags.setExternalApi(false);
            flags.setReactive(true);
            flags.setUseCase(false);
            
            assertDoesNotThrow(() -> flags.logConfiguration());
        }
    }
}
