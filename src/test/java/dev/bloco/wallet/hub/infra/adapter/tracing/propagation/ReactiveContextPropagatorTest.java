package dev.bloco.wallet.hub.infra.adapter.tracing.propagation;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

/**
 * Unit tests for {@link ReactiveContextPropagator}.
 * 
 * <h2>Test Strategy</h2>
 * <ul>
 * <li>Mock Tracer and TracingFeatureFlags</li>
 * <li>Verify context capture creates Reactor Context entries</li>
 * <li>Verify context restore extracts span from Reactor Context</li>
 * <li>Test feature flag behavior</li>
 * <li>Validate reactor.* span attributes</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReactiveContextPropagator Unit Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ReactiveContextPropagatorTest {

    @Mock
    private Tracer tracer;

    @Mock
    private TracingFeatureFlags featureFlags;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    private ReactiveContextPropagator propagator;

    @BeforeEach
    void setUp() {
        propagator = new ReactiveContextPropagator(tracer, featureFlags);

        // Default mock behavior
        lenient().when(featureFlags.isReactive()).thenReturn(true);
        lenient().when(tracer.currentSpan()).thenReturn(span);
        lenient().when(span.context()).thenReturn(traceContext);
        lenient().when(traceContext.traceId()).thenReturn("test-trace-id-12345");
    }

    @Test
    @DisplayName("Should initialize without errors when feature flag enabled")
    void shouldInitializeWithoutErrors() {
        // Arrange
        when(featureFlags.isReactive()).thenReturn(true);

        // Act & Assert - should not throw
        propagator.init();
    }

    @Test
    @DisplayName("Should not initialize when feature flag disabled")
    void shouldNotInitializeWhenFeatureFlagDisabled() {
        // Arrange
        when(featureFlags.isReactive()).thenReturn(false);

        // Act
        propagator.init();

        // Assert - verify no exceptions and proper logging
        verify(featureFlags, atLeastOnce()).isReactive();
    }

    @Test
    @DisplayName("Should capture trace context into Reactor Context")
    void shouldCaptureTraceContextIntoReactorContext() {
        // Arrange
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);

        // Act
        Function<Context, Context> captureFunction = propagator.captureTraceContext();
        Context emptyContext = Context.empty();
        Context enrichedContext = captureFunction.apply(emptyContext);

        // Assert
        assertThat(enrichedContext).isNotNull();

        boolean hasTraceContext = enrichedContext.hasKey(ReactiveContextPropagator.TRACE_CONTEXT_KEY);
        boolean hasSpan = enrichedContext.hasKey(ReactiveContextPropagator.SPAN_KEY);
        boolean hasThreadOrigin = enrichedContext.hasKey("thread.origin");

        assertThat(hasTraceContext).isTrue();
        assertThat(hasSpan).isTrue();
        assertThat(hasThreadOrigin).isTrue();

        TraceContext actualTraceContext = enrichedContext.get(ReactiveContextPropagator.TRACE_CONTEXT_KEY);
        Span actualSpan = enrichedContext.get(ReactiveContextPropagator.SPAN_KEY);
        assertThat(actualTraceContext).isEqualTo(traceContext);
        assertThat(actualSpan).isEqualTo(span);
    }

    @Test
    @DisplayName("Should return no-op function when no active span")
    void shouldReturnNoOpFunctionWhenNoActiveSpan() {
        // Arrange
        when(tracer.currentSpan()).thenReturn(null);

        // Act
        Function<Context, Context> captureFunction = propagator.captureTraceContext();
        Context emptyContext = Context.empty();
        Context result = captureFunction.apply(emptyContext);

        // Assert - context should be unchanged
        assertThat(result).isEqualTo(emptyContext);
        boolean hasKey = result.hasKey(ReactiveContextPropagator.TRACE_CONTEXT_KEY);
        assertThat(hasKey).isFalse();
    }

    @Test
    @DisplayName("Should return no-op function when feature flag disabled")
    void shouldReturnNoOpFunctionWhenFeatureFlagDisabled() {
        // Arrange
        when(featureFlags.isReactive()).thenReturn(false);

        // Act
        Function<Context, Context> captureFunction = propagator.captureTraceContext();
        Context emptyContext = Context.empty();
        Context result = captureFunction.apply(emptyContext);

        // Assert
        assertThat(result).isEqualTo(emptyContext);
        verify(tracer, never()).currentSpan();
    }

    @Test
    @DisplayName("Should restore span from Reactor Context")
    void shouldRestoreSpanFromReactorContext() {
        // Arrange
        Context context = Context.of(
                ReactiveContextPropagator.SPAN_KEY, span,
                ReactiveContextPropagator.TRACE_CONTEXT_KEY, traceContext,
                "thread.origin", "main");

        // Act
        Span restoredSpan = propagator.restoreTraceContext(context);

        // Assert
        assertThat(restoredSpan).isNotNull();
        assertThat(restoredSpan).isEqualTo(span);
    }

    @Test
    @DisplayName("Should return null when no span in Reactor Context")
    void shouldReturnNullWhenNoSpanInReactorContext() {
        // Arrange
        Context emptyContext = Context.empty();

        // Act
        Span restoredSpan = propagator.restoreTraceContext(emptyContext);

        // Assert
        assertThat(restoredSpan).isNull();
    }

    @Test
    @DisplayName("Should return null when feature flag disabled during restore")
    void shouldReturnNullWhenFeatureFlagDisabledDuringRestore() {
        // Arrange
        when(featureFlags.isReactive()).thenReturn(false);
        Context context = Context.of(ReactiveContextPropagator.SPAN_KEY, span);

        // Act
        Span restoredSpan = propagator.restoreTraceContext(context);

        // Assert
        assertThat(restoredSpan).isNull();
    }

    @Test
    @DisplayName("Should add reactor attributes to span")
    void shouldAddReactorAttributesToSpan() {
        // Arrange
        String operatorName = "flatMap";
        String scheduler = "parallel";

        // Act
        propagator.addReactorAttributes(span, operatorName, scheduler);

        // Assert
        verify(span).tag("reactor.operator", operatorName);
        verify(span).tag("reactor.scheduler", scheduler);
        verify(span).tag(eq("reactor.thread"), anyString());
    }

    @Test
    @DisplayName("Should not add attributes when span is null")
    void shouldNotAddAttributesWhenSpanIsNull() {
        // Act
        propagator.addReactorAttributes(null, "flatMap", "parallel");

        // Assert - no exceptions, no interactions
        verifyNoInteractions(span);
    }

    @Test
    @DisplayName("Should not add attributes when feature flag disabled")
    void shouldNotAddAttributesWhenFeatureFlagDisabled() {
        // Arrange
        when(featureFlags.isReactive()).thenReturn(false);

        // Act
        propagator.addReactorAttributes(span, "flatMap", "parallel");

        // Assert
        verify(span, never()).tag(anyString(), anyString());
    }

    @Test
    @DisplayName("Should detect scheduler transition and add thread attributes")
    void shouldDetectSchedulerTransitionAndAddThreadAttributes() {
        // Arrange
        Context context = Context.of(
                ReactiveContextPropagator.SPAN_KEY, span,
                "thread.origin", "main");

        // Simulate thread switch to parallel scheduler
        Thread.currentThread().setName("parallel-1");

        // Act
        Span restoredSpan = propagator.restoreTraceContext(context);

        // Assert
        assertThat(restoredSpan).isNotNull();
        verify(span).tag(eq("reactor.thread"), contains("parallel"));
        verify(span).tag("reactor.scheduler", "parallel");
    }

    @Test
    @DisplayName("Should work in reactive pipeline with context propagation")
    void shouldWorkInReactivePipelineWithContextPropagation() {
        // Arrange
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("test-trace-123");
        Function<Context, Context> captureFunction = propagator.captureTraceContext();

        // Act & Assert
        StepVerifier.create(
                Mono.deferContextual(ctx -> {
                    boolean hasSpan = ctx.hasKey(ReactiveContextPropagator.SPAN_KEY);
                    assertThat(hasSpan).isTrue();
                    return Mono.just("test-value");
                })
                        .map(String::toUpperCase)
                        .contextWrite(captureFunction))
                .expectNext("TEST-VALUE")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle exceptions gracefully during context capture")
    void shouldHandleExceptionsGracefullyDuringContextCapture() {
        // Arrange
        when(tracer.currentSpan()).thenThrow(new RuntimeException("Tracer error"));

        // Act
        Function<Context, Context> captureFunction = propagator.captureTraceContext();
        Context result = captureFunction.apply(Context.empty());

        // Assert - should return empty context without throwing
        assertThat(result).isEqualTo(Context.empty());
    }

    @Test
    @DisplayName("Should handle exceptions gracefully during context restore")
    void shouldHandleExceptionsGracefullyDuringContextRestore() {
        // Arrange - simulate span.context().traceId() throwing during thread boundary
        // detection
        Context context = Context.of(
                ReactiveContextPropagator.SPAN_KEY, span,
                "thread.origin", "main" // This triggers thread boundary detection
        );

        // Change current thread name to trigger scheduler detection logic
        Thread.currentThread().setName("parallel-1");

        when(span.tag(anyString(), anyString())).thenThrow(new RuntimeException("Span error"));

        // Act
        Span restoredSpan = propagator.restoreTraceContext(context);

        // Assert - should return null without throwing
        assertThat(restoredSpan).isNull();

        // Restore thread name
        Thread.currentThread().setName("main");
    }

    @Test
    @DisplayName("Should preserve trace context across multiple operators")
    void shouldPreserveTraceContextAcrossMultipleOperators() {
        // Arrange
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("test-trace-456");
        Function<Context, Context> captureFunction = propagator.captureTraceContext();

        // Act & Assert
        StepVerifier.create(
                Mono.deferContextual(ctx -> {
                    boolean hasSpan = ctx.hasKey(ReactiveContextPropagator.SPAN_KEY);
                    assertThat(hasSpan).isTrue();
                    Span contextSpan = ctx.get(ReactiveContextPropagator.SPAN_KEY);
                    assertThat(contextSpan).isEqualTo(span);
                    return Mono.just(1);
                })
                        .flatMap(value -> Mono.just(value + 1))
                        .map(value -> value * 2)
                        .contextWrite(captureFunction))
                .expectNext(4) // (1+1)*2 = 4
                .verifyComplete();
    }

    @Test
    @DisplayName("Should work with withTraceContext helper method")
    void shouldWorkWithWithTraceContextHelperMethod() {
        // Arrange
        Context context = Context.of(ReactiveContextPropagator.SPAN_KEY, span);
        Mono<String> operation = Mono.just("result");

        // Act
        Mono<String> result = propagator.withTraceContext(context, operation);

        // Assert
        StepVerifier.create(result)
                .expectNext("result")
                .verifyComplete();
    }
}
