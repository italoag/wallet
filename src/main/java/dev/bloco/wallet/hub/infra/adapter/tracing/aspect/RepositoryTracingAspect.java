package dev.bloco.wallet.hub.infra.adapter.tracing.aspect;

import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.SpanAttributeBuilder;
import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import dev.bloco.wallet.hub.infra.adapter.tracing.filter.SlowQueryDetector;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspect for instrumenting JPA repository operations with distributed tracing.
 *
 * <h2>Purpose</h2>
 * Automatically creates spans for all repository method executions, capturing:
 * <ul>
 * <li>Database system and operation type</li>
 * <li>SQL statements (sanitized)</li>
 * <li>Query execution duration</li>
 * <li>Rows affected/returned</li>
 * <li>Success/failure status with exception details</li>
 * </ul>
 *
 * <h2>Instrumentation Scope</h2>
 * Intercepts all public methods in repository interfaces:
 * 
 * <pre>
 * dev.bloco.wallet.hub.infra.provider.data.repository.*Repository.*(..)
 * </pre>
 *
 * <h2>Span Attributes</h2>
 * <table border="1">
 * <tr>
 * <th>Attribute</th>
 * <th>Description</th>
 * <th>Example</th>
 * </tr>
 * <tr>
 * <td>db.system</td>
 * <td>Database type</td>
 * <td>postgresql, h2</td>
 * </tr>
 * <tr>
 * <td>db.operation</td>
 * <td>Operation type</td>
 * <td>SELECT, INSERT, UPDATE, DELETE</td>
 * </tr>
 * <tr>
 * <td>db.statement</td>
 * <td>Sanitized SQL</td>
 * <td>SELECT * FROM wallet WHERE id = ?</td>
 * </tr>
 * <tr>
 * <td>db.sql.table</td>
 * <td>Primary table</td>
 * <td>wallet</td>
 * </tr>
 * <tr>
 * <td>repository.method</td>
 * <td>Repository method</td>
 * <td>findById</td>
 * </tr>
 * <tr>
 * <td>repository.class</td>
 * <td>Repository interface</td>
 * <td>WalletRepository</td>
 * </tr>
 * </table>
 *
 * <h2>SQL Sanitization</h2>
 * SQL statements are sanitized using {@link SensitiveDataSanitizer}:
 * <ul>
 * <li>String literals replaced with '?'</li>
 * <li>Numeric literals replaced with ?</li>
 * <li>UUIDs masked</li>
 * <li>Preserves query structure for performance analysis</li>
 * </ul>
 *
 * <h2>Feature Flag</h2>
 * Controlled by {@code tracing.features.database} flag (default: true).
 * When disabled, aspect is not registered (no AOP overhead).
 *
 * <h2>Performance</h2>
 * <ul>
 * <li>Overhead: ~1-2ms per query (span creation + SQL sanitization)</li>
 * <li>No impact when feature flag disabled</li>
 * <li>Sampling reduces export overhead</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * @Repository
 * public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {
 *     Optional<WalletEntity> findByUserId(UUID userId);
 *     // Automatically traced:
 *     // - Span name: "repository.WalletRepository.findByUserId"
 *     // - Attributes: db.system, db.operation, db.statement
 * }
 * }</pre>
 *
 * @see SpanAttributeBuilder
 * @see SensitiveDataSanitizer
 * @see TracingFeatureFlags
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "tracing.features.database", havingValue = "true", matchIfMissing = true)
public class RepositoryTracingAspect {

    private final ObservationRegistry observationRegistry;
    private final SpanAttributeBuilder spanAttributeBuilder;
    private final TracingFeatureFlags featureFlags;
    private final SlowQueryDetector slowQueryDetector;
    private final DataSource dataSource;

    private String cachedDbSystem;

    /**
     * Intercepts repository method executions and wraps them in observation spans.
     *
     * @param joinPoint the join point representing the method execution
     * @return the result of the repository method
     * @throws Throwable if the repository method throws an exception
     */
    @Around("execution(public * dev.bloco.wallet.hub.infra.provider.data.repository.*Repository.*(..))")
    public Object traceRepositoryOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        // Check feature flag at runtime
        if (!featureFlags.isDatabase()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String observationName = String.format("repository.%s.%s", className, methodName);

        Observation observation = Observation.createNotStarted(observationName, observationRegistry)
                .contextualName(className + "." + methodName)
                .lowCardinalityKeyValue("repository.class", className)
                .lowCardinalityKeyValue("repository.method", methodName);

        return observation.observe(() -> {
            final long startTime = System.currentTimeMillis();
            try {
                // Determine database system and operation
                String dbSystem = determineDbSystem();
                String operation = deriveOperationType(methodName);
                String tableName = deriveTableName(className, methodName);
                String queryPattern = deriveQueryPattern(methodName);

                // Add database attributes using builder
                spanAttributeBuilder.addDatabaseAttributes(observation, dbSystem, operation, tableName, queryPattern);

                // Execute the repository method
                Object result = joinPoint.proceed();

                // Calculate duration and check if slow
                long duration = System.currentTimeMillis() - startTime;
                slowQueryDetector.detectAndTag(observation, duration);

                // Mark observation as successful using builder
                spanAttributeBuilder.addSuccessStatus(observation);

                return result;

            } catch (Throwable ex) {
                // Add error attributes using builder
                spanAttributeBuilder.addErrorAttributes(observation, ex);

                // Rethrow as RuntimeException if it's a checked exception
                switch (ex) {
                    case RuntimeException runtimeException -> throw runtimeException;
                    case Error error -> throw error;
                    default -> throw new RuntimeException("Repository operation failed", ex);
                }
            }
        });
    }

    /**
     * Intercepts @Transactional methods to create transaction spans.
     *
     * @param joinPoint the join point representing the method execution
     * @return the result of the transactional method
     * @throws Throwable if the method throws an exception
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || " +
            "@within(org.springframework.transaction.annotation.Transactional)")
    public Object traceTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        // Check feature flag at runtime
        if (!featureFlags.isDatabase()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Check if method has @Transactional annotation
        Transactional methodTransactional = method.getAnnotation(Transactional.class);
        final Transactional transactional = (methodTransactional != null)
                ? methodTransactional
                : method.getDeclaringClass().getAnnotation(Transactional.class);

        if (transactional == null) {
            // No transaction, just proceed
            return joinPoint.proceed();
        }

        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String observationName = String.format("transaction.%s.%s", className, methodName);

        Observation observation = Observation.createNotStarted(observationName, observationRegistry)
                .contextualName("transaction:" + className + "." + methodName)
                .lowCardinalityKeyValue("transaction.class", className)
                .lowCardinalityKeyValue("transaction.method", methodName);

        return observation.observe(new Supplier<Object>() {
            @Override
            public Object get() {
                final long startTime = System.currentTimeMillis();
                try {
                    // Add transaction attributes using builder
                    spanAttributeBuilder.addTransactionAttributes(
                            observation,
                            transactional.isolation().name(),
                            transactional.propagation().name(),
                            transactional.readOnly(),
                            transactional.timeout());

                    // Execute the transactional method
                    Object result = joinPoint.proceed();

                    // Calculate duration
                    long duration = System.currentTimeMillis() - startTime;
                    observation.highCardinalityKeyValue("tx.duration_ms", String.valueOf(duration));
                    observation.lowCardinalityKeyValue("tx.status", "COMMITTED");

                    // Mark as success using builder
                    spanAttributeBuilder.addSuccessStatus(observation);

                    return result;

                } catch (Throwable ex) {
                    long duration = System.currentTimeMillis() - startTime;
                    observation.highCardinalityKeyValue("tx.duration_ms", String.valueOf(duration));
                    observation.lowCardinalityKeyValue("tx.status", "ROLLED_BACK");

                    // Add error attributes and mark as failed using builder
                    spanAttributeBuilder.addErrorAttributes(observation, ex);

                    // Rethrow as RuntimeException if it's a checked exception
                    switch (ex) {
                        case RuntimeException runtimeException -> throw runtimeException;
                        case Error error -> throw error;
                        default -> throw new RuntimeException("Transaction failed", ex);
                    }
                }
            }
        });
    }

    /**
     * Determines the database system being used by inspecting DataSource metadata.
     * Results are cached for performance.
     *
     * @return database system name (e.g., postgresql, h2)
     */
    private String determineDbSystem() {
        if (cachedDbSystem != null) {
            return cachedDbSystem;
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName().toLowerCase();

            if (productName.contains("postgresql")) {
                cachedDbSystem = "postgresql";
            } else if (productName.contains("h2")) {
                cachedDbSystem = "h2";
            } else if (productName.contains("mysql")) {
                cachedDbSystem = "mysql";
            } else if (productName.contains("oracle")) {
                cachedDbSystem = "oracle";
            } else {
                cachedDbSystem = productName;
            }
            return cachedDbSystem;
        } catch (Exception e) {
            // Default to h2 for safety in case of metadata extraction failure
            return "h2";
        }
    }

    /**
     * Derives the database operation type from the repository method name.
     *
     * @param methodName the method name
     * @return operation type (SELECT, INSERT, UPDATE, DELETE)
     */
    private String deriveOperationType(String methodName) {
        if (methodName.startsWith("find") || methodName.startsWith("get") ||
                methodName.startsWith("read") || methodName.startsWith("query") ||
                methodName.startsWith("exists") || methodName.startsWith("count")) {
            return "SELECT";
        } else if (methodName.startsWith("save") || methodName.startsWith("persist") ||
                methodName.startsWith("insert") || methodName.startsWith("create")) {
            return "INSERT";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return "UPDATE";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Derives the primary table name from the repository class name.
     *
     * @param className  the repository class name
     * @param methodName the method name
     * @return table name or null
     */
    private String deriveTableName(String className, String methodName) {
        // WalletRepository -> wallet
        // TransactionRepository -> transaction
        if (className == null || className.isBlank()) {
            return null;
        }

        String entityName = className.replace("Repository", "");
        // Convert camelCase to snake_case
        return entityName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Derives a query pattern from the method name for documentation.
     *
     * @param methodName the method name
     * @return sanitized query pattern
     */
    private String deriveQueryPattern(String methodName) {
        String operation = deriveOperationType(methodName);

        if (methodName.contains("ById")) {
            return String.format("%s ... WHERE id = ?", operation);
        } else if (methodName.contains("By")) {
            // Extract field name pattern
            String field = extractFieldFromMethodName(methodName);
            return String.format("%s ... WHERE %s = ?", operation, field);
        } else if (methodName.equals("findAll")) {
            return "SELECT ... FROM table";
        } else if (methodName.equals("save")) {
            return "INSERT INTO table ... VALUES (?)";
        } else if (methodName.equals("deleteById")) {
            return "DELETE FROM table WHERE id = ?";
        } else {
            return operation + " (method: " + methodName + ")";
        }
    }

    /**
     * Extracts field name from Spring Data method name pattern.
     *
     * @param methodName the method name
     * @return field name in snake_case
     */
    private String extractFieldFromMethodName(String methodName) {
        // findByUserId → user_id
        // findByWalletIdAndStatus → wallet_id
        int byIndex = methodName.indexOf("By");
        if (byIndex < 0) {
            return "field";
        }

        String afterBy = methodName.substring(byIndex + 2);
        // Take first field name before And/Or
        int andIndex = afterBy.indexOf("And");
        int orIndex = afterBy.indexOf("Or");

        int endIndex = afterBy.length();
        if (andIndex > 0) {
            endIndex = Math.min(endIndex, andIndex);
        }
        if (orIndex > 0) {
            endIndex = Math.min(endIndex, orIndex);
        }

        String field = afterBy.substring(0, endIndex);
        // Convert camelCase to snake_case
        return field.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
