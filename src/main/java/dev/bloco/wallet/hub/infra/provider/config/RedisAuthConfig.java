package dev.bloco.wallet.hub.infra.provider.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

/**
 * Redis configuration fragment for authentication concerns (revocation, rate limiting).
 * Reuses global Redis connection factory auto-configured by Spring Boot if present.
 */
@Configuration
public class RedisAuthConfig {

    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory factory) {
        return new ReactiveStringRedisTemplate(factory);
    }
}
