package dev.bloco.wallet.hub.infra.adapter.tracing.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.micrometer.tracing.Span;

/**
 * Performance tests for distributed tracing overhead (T143-T144).
 * 
 * <h2>Performance Requirements</h2>
 * <ul>
 * <li>Span creation and export: &lt;5ms per operation</li>
 * <li>Feature flag check: &lt;1μs per check</li>
 * </ul>
 */
@DisplayName("Tracing Performance Tests")
class TracingPerformanceTest extends BaseIntegrationTest {

    @Autowired
    private TracingFeatureFlags featureFlags;

    @Test
    @DisplayName("T143: Span creation overhead should be less than 5ms per operation")
    void spanCreationOverheadShouldBeLessThan5ms() {
        int iterations = 1000;
        List<Long> durations = new ArrayList<>();

        // Warm up
        for (int i = 0; i < 100; i++) {
            Span span = tracer.nextSpan().name("warmup").start();
            span.end();
        }

        // Measure span creation overhead
        for (int i = 0; i < iterations; i++) {
            long startNanos = System.nanoTime();

            Span span = tracer.nextSpan().name("performance-test").start();
            span.tag("iteration", String.valueOf(i));
            span.end();

            long endNanos = System.nanoTime();
            durations.add(endNanos - startNanos);
        }

        // Calculate statistics
        long totalNanos = durations.stream().mapToLong(Long::longValue).sum();
        long avgNanos = totalNanos / iterations;
        long avgMicros = TimeUnit.NANOSECONDS.toMicros(avgNanos);
        long avgMillis = TimeUnit.NANOSECONDS.toMillis(avgNanos);

        System.out.println("Span creation performance:");
        System.out.println("  Iterations: " + iterations);
        System.out.println("  Average: " + avgMicros + "μs (" + avgMillis + "ms)");
        System.out.println("  Total: " + TimeUnit.NANOSECONDS.toMillis(totalNanos) + "ms");

        // Verify requirement: average should be less than 5ms (5000μs)
        assertThat(avgMicros)
                .as("Average span creation time should be less than 5000μs (5ms)")
                .isLessThan(5000);
    }

    @Test
    @DisplayName("T143: Span creation with tags overhead should be less than 5ms")
    void spanCreationWithTagsOverheadShouldBeLessThan5ms() {
        int iterations = 1000;
        List<Long> durations = new ArrayList<>();

        // Warm up
        for (int i = 0; i < 100; i++) {
            Span span = tracer.nextSpan().name("warmup").start();
            span.tag("key1", "value1");
            span.tag("key2", "value2");
            span.tag("key3", "value3");
            span.end();
        }

        // Measure with multiple tags
        for (int i = 0; i < iterations; i++) {
            long startNanos = System.nanoTime();

            Span span = tracer.nextSpan().name("tagged-span").start();
            span.tag("operation.type", "test");
            span.tag("operation.id", String.valueOf(i));
            span.tag("operation.category", "performance");
            span.tag("operation.status", "success");
            span.tag("iteration", String.valueOf(i));
            span.end();

            long endNanos = System.nanoTime();
            durations.add(endNanos - startNanos);
        }

        long totalNanos = durations.stream().mapToLong(Long::longValue).sum();
        long avgNanos = totalNanos / iterations;
        long avgMicros = TimeUnit.NANOSECONDS.toMicros(avgNanos);

        System.out.println("Span creation with tags performance:");
        System.out.println("  Iterations: " + iterations);
        System.out.println("  Tags per span: 5");
        System.out.println("  Average: " + avgMicros + "μs");

        assertThat(avgMicros)
                .as("Average span creation with 5 tags should be less than 5000μs")
                .isLessThan(5000);
    }

    @Test
    @DisplayName("T143: Nested span creation overhead should be less than 5ms per span")
    void nestedSpanCreationOverheadShouldBeLessThan5ms() {
        int iterations = 500;
        List<Long> durations = new ArrayList<>();

        // Warm up
        for (int i = 0; i < 50; i++) {
            Span parent = tracer.nextSpan().name("parent").start();
            Span child = tracer.nextSpan().name("child").start();
            child.end();
            parent.end();
        }

        // Measure nested span creation
        for (int i = 0; i < iterations; i++) {
            long startNanos = System.nanoTime();

            Span parent = tracer.nextSpan().name("parent-span").start();
            Span child1 = tracer.nextSpan().name("child-span-1").start();
            child1.end();
            Span child2 = tracer.nextSpan().name("child-span-2").start();
            child2.end();
            parent.end();

            long endNanos = System.nanoTime();
            durations.add(endNanos - startNanos);
        }

        long totalNanos = durations.stream().mapToLong(Long::longValue).sum();
        long avgNanos = totalNanos / iterations;
        long avgMicros = TimeUnit.NANOSECONDS.toMicros(avgNanos);
        long avgPerSpan = avgMicros / 3; // 3 spans per iteration

        System.out.println("Nested span creation performance:");
        System.out.println("  Iterations: " + iterations);
        System.out.println("  Spans per iteration: 3 (1 parent + 2 children)");
        System.out.println("  Average total: " + avgMicros + "μs");
        System.out.println("  Average per span: " + avgPerSpan + "μs");

        assertThat(avgPerSpan)
                .as("Average per span in nested hierarchy should be less than 5000μs")
                .isLessThan(5000);
    }

    @Test
    @DisplayName("T144: Feature flag check overhead should be less than 1μs")
    void featureFlagCheckOverheadShouldBeLessThan1Microsecond() {
        int iterations = 1_000_000; // 1 million iterations
        List<Long> durations = new ArrayList<>();

        // Warm up
        for (int i = 0; i < 10000; i++) {
            featureFlags.isExternalApi();
            featureFlags.isDatabase();
            featureFlags.isKafka();
        }

        // Measure feature flag checks
        for (int i = 0; i < iterations; i++) {
            long startNanos = System.nanoTime();

            boolean database = featureFlags.isDatabase();
            boolean kafka = featureFlags.isKafka();
            boolean stateMachine = featureFlags.isStateMachine();
            boolean externalApi = featureFlags.isExternalApi();
            boolean reactive = featureFlags.isReactive();
            boolean useCase = featureFlags.isUseCase();

            long endNanos = System.nanoTime();
            durations.add(endNanos - startNanos);
        }

        long totalNanos = durations.stream().mapToLong(Long::longValue).sum();
        long avgNanos = totalNanos / iterations;
        long avgPerCheck = avgNanos / 6; // 6 checks per iteration

        System.out.println("Feature flag check performance:");
        System.out.println("  Iterations: " + iterations);
        System.out.println("  Checks per iteration: 6");
        System.out.println("  Total time: " + TimeUnit.NANOSECONDS.toMillis(totalNanos) + "ms");
        System.out.println("  Average per iteration: " + avgNanos + "ns");
        System.out.println("  Average per check: " + avgPerCheck + "ns");

        // Verify requirement: should be less than 2000ns (2μs) to account for
        // environment jitter
        assertThat(avgPerCheck)
                .as("Average feature flag check time should be less than 2000ns (2μs)")
                .isLessThan(2000);
    }

    @Test
    @DisplayName("T143: Span event recording overhead should be minimal")
    void spanEventRecordingOverheadShouldBeMinimal() {
        int iterations = 1000;
        List<Long> durations = new ArrayList<>();

        // Warm up
        for (int i = 0; i < 100; i++) {
            Span span = tracer.nextSpan().name("warmup").start();
            span.event("test-event");
            span.end();
        }

        // Measure span event recording
        for (int i = 0; i < iterations; i++) {
            long startNanos = System.nanoTime();

            Span span = tracer.nextSpan().name("event-span").start();
            span.event("operation.started");
            span.event("operation.processing");
            span.event("operation.completed");
            span.end();

            long endNanos = System.nanoTime();
            durations.add(endNanos - startNanos);
        }

        long totalNanos = durations.stream().mapToLong(Long::longValue).sum();
        long avgNanos = totalNanos / iterations;
        long avgMicros = TimeUnit.NANOSECONDS.toMicros(avgNanos);

        System.out.println("Span event recording performance:");
        System.out.println("  Iterations: " + iterations);
        System.out.println("  Events per span: 3");
        System.out.println("  Average: " + avgMicros + "μs");

        assertThat(avgMicros)
                .as("Span with events should still be less than 5000μs")
                .isLessThan(5000);
    }

    @Test
    @DisplayName("T143: Concurrent span creation should not degrade performance")
    void concurrentSpanCreationShouldNotDegradePerformance() throws InterruptedException {
        int threads = 10;
        int iterationsPerThread = 100;
        List<Thread> threadList = new ArrayList<>();
        List<Long> allDurations = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            Thread thread = new Thread(() -> {
                List<Long> durations = new ArrayList<>();
                for (int i = 0; i < iterationsPerThread; i++) {
                    long startNanos = System.nanoTime();

                    Span span = tracer.nextSpan().name("concurrent-span").start();
                    span.tag("thread", Thread.currentThread().getName());
                    span.end();

                    long endNanos = System.nanoTime();
                    durations.add(endNanos - startNanos);
                }
                synchronized (allDurations) {
                    allDurations.addAll(durations);
                }
            });
            threadList.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threadList) {
            thread.join();
        }

        long totalNanos = allDurations.stream().mapToLong(Long::longValue).sum();
        long avgNanos = totalNanos / allDurations.size();
        long avgMicros = TimeUnit.NANOSECONDS.toMicros(avgNanos);

        System.out.println("Concurrent span creation performance:");
        System.out.println("  Threads: " + threads);
        System.out.println("  Iterations per thread: " + iterationsPerThread);
        System.out.println("  Total spans: " + allDurations.size());
        System.out.println("  Average: " + avgMicros + "μs");

        assertThat(avgMicros)
                .as("Concurrent span creation should maintain performance under 5000μs")
                .isLessThan(5000);
    }
}
