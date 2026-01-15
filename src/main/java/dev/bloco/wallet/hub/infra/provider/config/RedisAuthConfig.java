package dev.bloco.wallet.hub.infra.provider.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import dev.bloco.wallet.hub.infra.adapter.tracing.decorator.TracedReactiveStringRedisTemplate;
import dev.bloco.wallet.hub.infra.adapter.tracing.propagation.ReactiveContextPropagator;
import io.micrometer.tracing.Tracer;

/**
 * Redis configuration fragment for authentication concerns (revocation, rate limiting).
 * Reuses global Redis connection factory auto-configured by Spring Boot if present.
 * 
 * <p>Automatically configures distributed tracing for Redis operations when tracing
 * components are available.</p>
 */
@Configuration
public class RedisAuthConfig {

    /**
     * Creates a traced ReactiveStringRedisTemplate when tracing is available.
     * 
     * @param factory the Redis connection factory
     * @param tracer the distributed tracer
     * @param featureFlags the tracing feature flags
     * @param contextPropagator the reactive context propagator
     * @return traced ReactiveStringRedisTemplate
     */
    @Bean
    @ConditionalOnClass(name = "io.micrometer.tracing.Tracer")
    public TracedReactiveStringRedisTemplate tracedReactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory factory,
            Tracer tracer,
            TracingFeatureFlags featureFlags,
            ReactiveContextPropagator contextPropagator) {
        return new TracedReactiveStringRedisTemplate(factory, tracer, featureFlags, contextPropagator);
    }

    /**
     * Fallback bean when tracing is not available.
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.data.redis.core.ReactiveStringRedisTemplate")
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        return new ReactiveStringRedisTemplate(factory);
    }
}
