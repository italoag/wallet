package dev.bloco.wallet.hub.infra.adapter.tracing.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable tracing for a specific class or method.
 * Primarily used for Use Cases to ensure they are proxied by the
 * UseCaseTracingAspect.
 * 
 * Note: When applied to a class, it must not be final (e.g., not a record)
 * to allow CGLIB proxying in Spring AOP.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Traced {
    /**
     * Optional operation name to use for the span.
     * If empty, the method name or class name will be used.
     */
    String value() default "";
}
