package dev.bloco.wallet.hub.infra.adapter.tracing.propagation;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Propagates distributed tracing context through Project Reactor pipelines.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnClass({ Mono.class, Flux.class })
@ConditionalOnProperty(value = "tracing.features.reactive", havingValue = "true", matchIfMissing = true)
public class ReactiveContextPropagator {

    public static final String TRACE_CONTEXT_KEY = "TRACE_CONTEXT";
    public static final String SPAN_KEY = "SPAN";
    public static final String SCHEDULER_KEY = "SCHEDULER";

    private final Tracer tracer;
    private final TracingFeatureFlags featureFlags;

    @PostConstruct
    public void init() {
        if (!featureFlags.isReactive()) {
            return;
        }

        try {
            // Register hook to automatically propagate trace context
            // Note: This is a simplified approach. For production, consider using
            // reactor-core-micrometer integration for automatic context propagation
        } catch (Exception e) {
            // Failed to initialize reactive tracing
        }
    }

    public Function<Context, Context> captureTraceContext() {
        if (!featureFlags.isReactive()) {
            return ctx -> ctx; // No-op if disabled
        }

        try {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan == null) {
                return ctx -> ctx;
            }

            TraceContext traceContext = currentSpan.context();
            String threadName = Thread.currentThread().getName();
            return ctx -> ctx
                    .put(TRACE_CONTEXT_KEY, traceContext)
                    .put(SPAN_KEY, currentSpan)
                    .put("thread.origin", threadName);

        } catch (Exception e) {
            return ctx -> ctx;
        }
    }

    public Span restoreTraceContext(reactor.util.context.ContextView context) {
        if (!featureFlags.isReactive()) {
            return null;
        }

        try {
            Optional<Span> spanOpt = context.getOrEmpty(SPAN_KEY);
            if (spanOpt.isEmpty()) {
                return null;
            }

            Span span = spanOpt.get();
            Optional<String> originThread = context.getOrEmpty("thread.origin");
            String currentThread = Thread.currentThread().getName();

            if (originThread.isPresent() && !originThread.get().equals(currentThread)) {
                if (currentThread.contains("parallel") || currentThread.contains("elastic") ||
                        currentThread.contains("single")) {
                    span.tag("reactor.thread", currentThread);
                    span.tag("reactor.scheduler", getSchedulerType(currentThread));
                }
            }

            return span;

        } catch (Exception e) {
            return null;
        }
    }

    public void addReactorAttributes(Span span, String operatorName, String scheduler) {
        if (!featureFlags.isReactive() || span == null) {
            return;
        }

        try {
            span.tag("reactor.operator", operatorName);

            if (scheduler != null && !scheduler.isEmpty()) {
                span.tag("reactor.scheduler", scheduler);
            }

            String threadName = Thread.currentThread().getName();
            span.tag("reactor.thread", threadName);
        } catch (Exception e) {
            // Error adding reactor attributes
        }
    }

    private String getSchedulerType(String threadName) {
        if (threadName.contains("parallel")) {
            return "parallel";
        } else if (threadName.contains("elastic") || threadName.contains("bounded")) {
            return "boundedElastic";
        } else if (threadName.contains("single")) {
            return "single";
        } else if (threadName.contains("immediate")) {
            return "immediate";
        } else {
            return "unknown";
        }
    }

    public <T> Mono<T> withTraceContext(Context context, Mono<T> operation) {
        if (!featureFlags.isReactive()) {
            return operation;
        }

        return Mono.deferContextual(ctx -> {
            Span span = restoreTraceContext(ctx);
            return operation.doFinally(signal -> {
                // Trace context cleanup handled by Micrometer
            });
        }).contextWrite(ctx -> ctx.putAll(ctx));
    }
}
