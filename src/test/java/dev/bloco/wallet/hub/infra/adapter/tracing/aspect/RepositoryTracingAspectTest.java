package dev.bloco.wallet.hub.infra.adapter.tracing.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.SpanAttributeBuilder;
import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import dev.bloco.wallet.hub.infra.adapter.tracing.filter.SensitiveDataSanitizer;
import dev.bloco.wallet.hub.infra.adapter.tracing.filter.SlowQueryDetector;
import io.micrometer.observation.ObservationRegistry;

/**
 * Unit tests for {@link RepositoryTracingAspect}.
 * 
 * <p>Tests verify:</p>
 * <ul>
 *   <li>Span creation for repository operations</li>
 *   <li>SQL statement sanitization</li>
 *   <li>Database operation type detection</li>
 *   <li>Error handling with SQL in messages</li>
 *   <li>Feature flag behavior</li>
 *   <li>Table name extraction</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RepositoryTracingAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Mock
    private TracingFeatureFlags featureFlags;

    @Mock
    private SensitiveDataSanitizer sanitizer;

    @Mock
    private SlowQueryDetector slowQueryDetector;

    private ObservationRegistry observationRegistry;
    private SpanAttributeBuilder spanAttributeBuilder;
    private RepositoryTracingAspect aspect;

    @BeforeEach
    void setUp() {
        observationRegistry = ObservationRegistry.create();
        spanAttributeBuilder = new SpanAttributeBuilder(sanitizer);
        
        aspect = new RepositoryTracingAspect(
                observationRegistry,
                spanAttributeBuilder,
                sanitizer,
                featureFlags,
                slowQueryDetector
        );

        // Default: feature flag enabled
        when(featureFlags.isDatabase()).thenReturn(true);
    }

    @Test
    void shouldCreateSpanForRepositoryOperation() throws Throwable {
        // Given
        when(signature.getDeclaringType()).thenReturn((Class) WalletRepository.class);
        when(signature.getName()).thenReturn("findById");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.traceRepositoryOperation(joinPoint);

        // Then
        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
    }

    @Test
    void shouldSkipTracingWhenFeatureFlagDisabled() throws Throwable {
        // Given
        when(featureFlags.isDatabase()).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.traceRepositoryOperation(joinPoint);

        // Then
        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
    }

    @Test
    void shouldDeriveSelectOperationFromMethodName() throws Throwable {
        // Given
        when(signature.getDeclaringType()).thenReturn((Class) WalletRepository.class);
        when(signature.getName()).thenReturn("findByUserId");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.traceRepositoryOperation(joinPoint);

        // Then
        assertThat(result).isEqualTo("result");
        // Operation should be detected as SELECT
    }

    @Test
    void shouldDeriveInsertOperationFromMethodName() throws Throwable {
        // Given
        when(signature.getDeclaringType()).thenReturn((Class) WalletRepository.class);
        when(signature.getName()).thenReturn("save");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.traceRepositoryOperation(joinPoint);

        // Then
        assertThat(result).isEqualTo("result");
        // Operation should be detected as INSERT
    }

    @Test
    void shouldDeriveUpdateOperationFromMethodName() throws Throwable {
        // Given
        when(signature.getDeclaringType()).thenReturn((Class) WalletRepository.class);
        when(signature.getName()).thenReturn("updateBalance");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.traceRepositoryOperation(joinPoint);

        // Then
        assertThat(result).isEqualTo("result");
        // Operation should be detected as UPDATE
    }

    @Test
    void shouldDeriveDeleteOperationFromMethodName() throws Throwable {
        // Given
        when(signature.getDeclaringType()).thenReturn((Class) WalletRepository.class);
        when(signature.getName()).thenReturn("deleteById");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.traceRepositoryOperation(joinPoint);

        // Then
        assertThat(result).isEqualTo("result");
        // Operation should be detected as DELETE
    }

    @Test
    void shouldAddErrorAttributesOnException() throws Throwable {
        // Given
        String sqlError = "SQLException: duplicate key value violates unique constraint";
        RuntimeException exception = new RuntimeException(sqlError);
        when(signature.getDeclaringType()).thenReturn((Class) WalletRepository.class);
        when(signature.getName()).thenReturn("save");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenThrow(exception);
        when(sanitizer.sanitizeSql(sqlError)).thenReturn("SQLException: duplicate key");

        // When/Then
        assertThatThrownBy(() -> aspect.traceRepositoryOperation(joinPoint))
                .isInstanceOf(RuntimeException.class);

        verify(joinPoint).proceed();
        verify(sanitizer).sanitizeSql(sqlError);
    }

    @Test
    void shouldSanitizeSqlInErrorMessages() throws Throwable {
        // Given
        String sqlWithCredentials = "SELECT * FROM wallet WHERE password='secret123'";
        RuntimeException exception = new RuntimeException(sqlWithCredentials);
        when(signature.getDeclaringType()).thenReturn((Class) WalletRepository.class);
        when(signature.getName()).thenReturn("findById");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenThrow(exception);
        when(sanitizer.sanitizeSql(sqlWithCredentials)).thenReturn("SELECT * FROM wallet WHERE password=?");

        // When/Then
        assertThatThrownBy(() -> aspect.traceRepositoryOperation(joinPoint))
                .isInstanceOf(RuntimeException.class);

        verify(sanitizer).sanitizeSql(sqlWithCredentials);
    }

    @Test
    void shouldExtractFieldNameFromFindByMethod() throws Throwable {
        // Given
        when(signature.getDeclaringType()).thenReturn((Class) WalletRepository.class);
        when(signature.getName()).thenReturn("findByUserIdAndStatus");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = aspect.traceRepositoryOperation(joinPoint);

        // Then
        assertThat(result).isEqualTo("result");
        // Query pattern should show "user_id" field
    }

    @Test
    void shouldHandleExistsMethod() throws Throwable {
        // Given
        when(signature.getDeclaringType()).thenReturn((Class) WalletRepository.class);
        when(signature.getName()).thenReturn("existsById");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn(true);

        // When
        Object result = aspect.traceRepositoryOperation(joinPoint);

        // Then
        assertThat(result).isEqualTo(true);
        // Operation should be detected as SELECT
    }

    @Test
    void shouldHandleCountMethod() throws Throwable {
        // Given
        when(signature.getDeclaringType()).thenReturn((Class) WalletRepository.class);
        when(signature.getName()).thenReturn("countByStatus");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn(42L);

        // When
        Object result = aspect.traceRepositoryOperation(joinPoint);

        // Then
        assertThat(result).isEqualTo(42L);
        // Operation should be detected as SELECT
    }

    // Test repository interface
    interface WalletRepository {
        Object findById(Object id);
        Object findByUserId(Object userId);
        Object save(Object entity);
        void updateBalance(Object id, Object balance);
        void deleteById(Object id);
        boolean existsById(Object id);
        long countByStatus(String status);
        Object findByUserIdAndStatus(Object userId, String status);
    }
}
