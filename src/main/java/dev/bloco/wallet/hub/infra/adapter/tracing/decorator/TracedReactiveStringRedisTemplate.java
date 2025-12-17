package dev.bloco.wallet.hub.infra.adapter.tracing.decorator;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import dev.bloco.wallet.hub.infra.adapter.tracing.propagation.ReactiveContextPropagator;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 * Traced wrapper for ReactiveStringRedisTemplate that adds distributed tracing
 * to Redis operations.
 * 
 * <h2>Span Attributes</h2>
 * <ul>
 *   <li><b>cache.system</b>: "redis"</li>
 *   <li><b>cache.operation</b>: get, set, delete, exists, etc.</li>
 *   <li><b>cache.key</b>: Redis key (sanitized if sensitive)</li>
 *   <li><b>cache.hit</b>: true/false for get operations</li>
 *   <li><b>cache.ttl</b>: TTL in seconds for set operations</li>
 * </ul>
 * 
 * <h2>Span Events</h2>
 * <ul>
 *   <li><b>cache.hit</b>: Key found in cache</li>
 *   <li><b>cache.miss</b>: Key not found in cache</li>
 *   <li><b>cache.error</b>: Operation failed</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * @Bean
 * public TracedReactiveStringRedisTemplate tracedRedisTemplate(
 *     ReactiveRedisConnectionFactory factory,
 *     Tracer tracer,
 *     TracingFeatureFlags flags,
 *     ReactiveContextPropagator propagator) {
 *     return new TracedReactiveStringRedisTemplate(factory, tracer, flags, propagator);
 * }
 * 
 * // Use exactly like ReactiveStringRedisTemplate
 * return tracedRedisTemplate.get("user:123")
 *     .map(value -> deserialize(value));
 * }</pre>
 */
public class TracedReactiveStringRedisTemplate {

    private static final Logger log = LoggerFactory.getLogger(TracedReactiveStringRedisTemplate.class);
    
    private final ReactiveStringRedisTemplate delegate;
    private final Tracer tracer;
    private final TracingFeatureFlags featureFlags;
    private final ReactiveContextPropagator contextPropagator;

    public TracedReactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory,
            Tracer tracer,
            TracingFeatureFlags featureFlags,
            ReactiveContextPropagator contextPropagator) {
        this.delegate = new ReactiveStringRedisTemplate(connectionFactory);
        this.tracer = tracer;
        this.featureFlags = featureFlags;
        this.contextPropagator = contextPropagator;
    }

    /**
     * Get value with tracing.
     */
    public Mono<String> get(String key) {
        if (!featureFlags.isReactive()) {
            return delegate.opsForValue().get(key);
        }

        return Mono.deferContextual(ctx -> {
            Span span = createSpan("cache.get", key);
            
            return delegate.opsForValue().get(key)
                .doOnNext(value -> {
                    span.tag("cache.hit", "true");
                    span.event("cache.hit");
                })
                .doOnSuccess(value -> {
                    if (value == null) {
                        span.tag("cache.hit", "false");
                        span.event("cache.miss");
                    }
                    span.end();
                })
                .doOnError(error -> {
                    span.tag("cache.hit", "false");
                    span.tag("error.type", error.getClass().getSimpleName());
                    span.tag("error.message", error.getMessage());
                    span.event("cache.error");
                    span.end();
                })
                .contextWrite(contextPropagator.captureTraceContext());
        });
    }

    /**
     * Set value with tracing.
     */
    public Mono<Boolean> set(String key, String value) {
        if (!featureFlags.isReactive()) {
            return delegate.opsForValue().set(key, value);
        }

        return Mono.deferContextual(ctx -> {
            Span span = createSpan("cache.set", key);
            span.tag("cache.value.size", String.valueOf(value != null ? value.length() : 0));
            
            return delegate.opsForValue().set(key, value)
                .doOnSuccess(result -> {
                    span.tag("cache.result", String.valueOf(result));
                    span.end();
                })
                .doOnError(error -> {
                    span.tag("error.type", error.getClass().getSimpleName());
                    span.tag("error.message", error.getMessage());
                    span.event("cache.error");
                    span.end();
                })
                .contextWrite(contextPropagator.captureTraceContext());
        });
    }

    /**
     * Set value with TTL.
     */
    public Mono<Boolean> set(String key, String value, Duration timeout) {
        if (!featureFlags.isReactive()) {
            return delegate.opsForValue().set(key, value, timeout);
        }

        return Mono.deferContextual(ctx -> {
            Span span = createSpan("cache.set", key);
            span.tag("cache.value.size", String.valueOf(value != null ? value.length() : 0));
            span.tag("cache.ttl", String.valueOf(timeout.getSeconds()));
            
            return delegate.opsForValue().set(key, value, timeout)
                .doOnSuccess(result -> {
                    span.tag("cache.result", String.valueOf(result));
                    span.end();
                })
                .doOnError(error -> {
                    span.tag("error.type", error.getClass().getSimpleName());
                    span.tag("error.message", error.getMessage());
                    span.event("cache.error");
                    span.end();
                })
                .contextWrite(contextPropagator.captureTraceContext());
        });
    }

    /**
     * Delete key with tracing.
     */
    public Mono<Boolean> delete(String key) {
        if (!featureFlags.isReactive()) {
            return delegate.opsForValue().delete(key);
        }

        return Mono.deferContextual(ctx -> {
            Span span = createSpan("cache.delete", key);
            
            return delegate.opsForValue().delete(key)
                .doOnSuccess(result -> {
                    span.tag("cache.result", String.valueOf(result));
                    span.end();
                })
                .doOnError(error -> {
                    span.tag("error.type", error.getClass().getSimpleName());
                    span.tag("error.message", error.getMessage());
                    span.event("cache.error");
                    span.end();
                })
                .contextWrite(contextPropagator.captureTraceContext());
        });
    }

    /**
     * Check if key exists with tracing.
     */
    public Mono<Boolean> exists(String key) {
        if (!featureFlags.isReactive()) {
            return delegate.hasKey(key);
        }

        return Mono.deferContextual(ctx -> {
            Span span = createSpan("cache.exists", key);
            
            return delegate.hasKey(key)
                .doOnSuccess(result -> {
                    span.tag("cache.result", String.valueOf(result));
                    span.end();
                })
                .doOnError(error -> {
                    span.tag("error.type", error.getClass().getSimpleName());
                    span.tag("error.message", error.getMessage());
                    span.event("cache.error");
                    span.end();
                })
                .contextWrite(contextPropagator.captureTraceContext());
        });
    }

    /**
     * Get multiple values with tracing.
     */
    public Mono<java.util.List<String>> multiGet(Collection<String> keys) {
        if (!featureFlags.isReactive()) {
            return delegate.opsForValue().multiGet(keys);
        }

        return Mono.deferContextual(ctx -> {
            Span span = createSpan("cache.mget", null);
            span.tag("cache.keys.count", String.valueOf(keys.size()));
            
            return delegate.opsForValue().multiGet(keys)
                .doOnSuccess(values -> {
                    long hits = values.stream().filter(v -> v != null).count();
                    long misses = values.size() - hits;
                    
                    span.tag("cache.hits", String.valueOf(hits));
                    span.tag("cache.misses", String.valueOf(misses));
                    span.end();
                })
                .doOnError(error -> {
                    span.tag("error.type", error.getClass().getSimpleName());
                    span.tag("error.message", error.getMessage());
                    span.event("cache.error");
                    span.end();
                })
                .contextWrite(contextPropagator.captureTraceContext());
        });
    }

    /**
     * Set multiple values with tracing.
     */
    public Mono<Boolean> multiSet(Map<String, String> map) {
        if (!featureFlags.isReactive()) {
            return delegate.opsForValue().multiSet(map);
        }

        return Mono.deferContextual(ctx -> {
            Span span = createSpan("cache.mset", null);
            span.tag("cache.keys.count", String.valueOf(map.size()));
            
            return delegate.opsForValue().multiSet(map)
                .doOnSuccess(result -> {
                    span.tag("cache.result", String.valueOf(result));
                    span.end();
                })
                .doOnError(error -> {
                    span.tag("error.type", error.getClass().getSimpleName());
                    span.tag("error.message", error.getMessage());
                    span.event("cache.error");
                    span.end();
                })
                .contextWrite(contextPropagator.captureTraceContext());
        });
    }

    /**
     * Get the underlying delegate template for operations not wrapped by this class.
     */
    public ReactiveStringRedisTemplate getDelegate() {
        return delegate;
    }

    /**
     * Creates a CLIENT span for Redis operations.
     */
    private Span createSpan(String operation, String key) {
        Span span = tracer.nextSpan().name(operation);
        
        span.tag("cache.system", "redis");
        span.tag("cache.operation", operation);
        
        if (key != null) {
            span.tag("cache.key", key);
        }
        
        span.start();
        return span;
    }
}
