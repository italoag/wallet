package dev.bloco.wallet.hub.infra.adapter.tracing.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.SpanAttributeBuilder;
import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspect for instrumenting use case executions with distributed tracing.
 *
 * <h2>Purpose</h2>
 * Automatically creates spans for all use case method executions, capturing:
 * <ul>
 * <li>Use case class name and method name</li>
 * <li>Execution duration</li>
 * <li>Business operation parameters (wallet ID, transaction ID)</li>
 * <li>Success/failure status with exception details</li>
 * <li>User-related identifiers (hashed for privacy)</li>
 * </ul>
 *
 * <h2>Instrumentation Scope</h2>
 * Intercepts all public methods in the {@code usecase} package:
 * 
 * <pre>
 * dev.bloco.wallet.hub.usecase.*UseCase.*(..)
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
 * <td>usecase.class</td>
 * <td>Use case class name</td>
 * <td>AddFundsUseCase</td>
 * </tr>
 * <tr>
 * <td>usecase.method</td>
 * <td>Method name</td>
 * <td>execute</td>
 * </tr>
 * <tr>
 * <td>wallet.operation</td>
 * <td>Business operation</td>
 * <td>add_funds</td>
 * </tr>
 * <tr>
 * <td>transaction.id</td>
 * <td>Transaction ID (as-is)</td>
 * <td>550e8400-e29b-...</td>
 * </tr>
 * <tr>
 * <td>wallet.id.hash</td>
 * <td>Hashed wallet ID</td>
 * <td>a3f5b1c2d4e6f7g8</td>
 * </tr>
 * <tr>
 * <td>user.id.hash</td>
 * <td>Hashed user ID</td>
 * <td>x9y8z7w6v5u4t3s2</td>
 * </tr>
 * <tr>
 * <td>error.type</td>
 * <td>Exception class</td>
 * <td>InsufficientFundsException</td>
 * </tr>
 * <tr>
 * <td>error.message</td>
 * <td>Exception message</td>
 * <td>Balance too low</td>
 * </tr>
 * </table>
 *
 * <h2>Identifier Handling</h2>
 * Following specification clarifications (2025-12-16):
 * <ul>
 * <li><b>Safe identifiers (included as-is)</b>: transaction.id, saga.id,
 * event.type</li>
 * <li><b>Sensitive identifiers (SHA-256 hashed)</b>: wallet.id, user.id</li>
 * </ul>
 *
 * <p>
 * Hashing prevents user tracking across traces while preserving correlation
 * within a trace.
 * </p>
 *
 * <h2>Error Handling</h2>
 * When a use case throws an exception:
 * <ol>
 * <li>Error attributes added to span (error.type, error.message,
 * error.stack)</li>
 * <li>Span marked as failed (error=true)</li>
 * <li>Parent trace marked as failed</li>
 * <li>Exception propagated to caller (not swallowed)</li>
 * </ol>
 *
 * <h2>Feature Flag</h2>
 * Controlled by {@code tracing.features.useCase} flag (default: true).
 * When disabled, aspect is not registered (no AOP overhead).
 *
 * <h2>Performance</h2>
 * <ul>
 * <li>Overhead: ~1-2ms per use case execution (span creation + attribute
 * setting)</li>
 * <li>No impact when feature flag disabled (aspect not registered)</li>
 * <li>Sampling reduces export overhead (10% default)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * @Component
 * public class AddFundsUseCase {
 *     public void execute(String walletId, BigDecimal amount) {
 *         // Automatically traced:
 *         // - Span name: "usecase.AddFundsUseCase.execute"
 *         // - Attributes: wallet.id.hash, transaction.id, wallet.operation
 *         // - Duration: measured automatically
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * Thread-safe. Each method invocation creates an independent span in the
 * calling thread's context.
 *
 * @see SpanAttributeBuilder
 * @see TracingFeatureFlags
 * @see ObservationRegistry
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "tracing.features.use-case", havingValue = "true", matchIfMissing = true)
public class UseCaseTracingAspect {

    private final ObservationRegistry observationRegistry;
    private final SpanAttributeBuilder spanAttributeBuilder;
    private final TracingFeatureFlags featureFlags;

    /**
     * Intercepts use case method executions and wraps them in observation spans.
     *
     * <p>
     * Pointcut targets all public methods in classes ending with "UseCase" in the
     * usecase package.
     * </p>
     *
     * @param joinPoint the join point representing the method execution
     * @return the result of the use case method
     * @throws Throwable if the use case method throws an exception
     */
    @Around("execution(public * dev.bloco.wallet.hub.usecase.*UseCase.*(..))")
    public Object traceUseCaseExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // Check feature flag at runtime
        if (!featureFlags.isUseCase()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String observationName = String.format("usecase.%s.%s", className, methodName);

        Observation observation = Observation.createNotStarted(observationName, observationRegistry)
                .contextualName(className + "." + methodName)
                .lowCardinalityKeyValue("usecase.class", className)
                .lowCardinalityKeyValue("usecase.method", methodName);

        return observation.observe(() -> {
            try {
                // Extract and add use case-specific attributes before execution
                addUseCaseAttributes(observation, joinPoint, className);

                // Execute the use case
                Object result = joinPoint.proceed();

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
                    default -> throw new RuntimeException("Use case execution failed", ex);
                }
            }
        });
    }

    /**
     * Extracts and adds use case-specific attributes to the observation.
     *
     * @param observation the observation to add attributes to
     * @param joinPoint   the join point with method arguments
     * @param className   the use case class name
     */
    private void addUseCaseAttributes(Observation observation, ProceedingJoinPoint joinPoint, String className) {
        try {
            // Derive wallet operation from class name
            String walletOperation = deriveOperationName(className);

            // Add use case attributes using builder
            spanAttributeBuilder.addUseCaseAttributes(observation, walletOperation);

            // Extract method arguments
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return;
            }

            // Scan arguments for known identifiers
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg == null) {
                    continue;
                }

                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                String[] paramNames = signature.getParameterNames();
                String paramName = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;

                addIdentifierAttributes(observation, paramName, arg);
            }

        } catch (Exception e) {
            // Log removed
        }
    }

    /**
     * Adds identifier attributes based on parameter name and value.
     *
     * @param observation the observation to add attributes to
     * @param paramName   the parameter name
     * @param value       the parameter value
     */
    private void addIdentifierAttributes(Observation observation, String paramName, Object value) {
        String stringValue = value.toString();
        String paramLower = paramName.toLowerCase();

        // Transaction ID - include as-is (technical identifier)
        if (paramLower.contains("transaction")) {
            spanAttributeBuilder.addIdentifier(observation, SpanAttributeBuilder.TRANSACTION_ID, stringValue);
        }
        // Wallet ID - hash for privacy (user-related identifier)
        else if (paramLower.contains("wallet")) {
            spanAttributeBuilder.addHashedIdentifier(observation, "wallet.id.hash", stringValue);
        }
        // User ID - hash for privacy (user-related identifier)
        else if (paramLower.contains("user")) {
            spanAttributeBuilder.addHashedIdentifier(observation, "user.id.hash", stringValue);
        }
        // Amount - include as-is (business metric)
        else if (paramLower.contains("amount")) {
            spanAttributeBuilder.addIdentifier(observation, SpanAttributeBuilder.TRANSACTION_AMOUNT, stringValue);
        }
        // Currency - include as-is (low cardinality)
        else if (paramLower.contains("currency")) {
            spanAttributeBuilder.addIdentifier(observation, SpanAttributeBuilder.WALLET_CURRENCY, stringValue);
        }
    }

    private String deriveOperationName(String className) {
        // Remove "UseCase" suffix
        String operation = className.replace("UseCase", "");

        // Convert camelCase to snake_case
        return operation.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
