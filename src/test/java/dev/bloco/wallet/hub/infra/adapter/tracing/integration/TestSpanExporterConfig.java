package dev.bloco.wallet.hub.infra.adapter.tracing.integration;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.tracing.Span.Kind;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Test configuration providing tracing beans and infrastructure for integration
 * tests.
 * 
 * DataSource is manually created because DataSourceAutoConfiguration does not
 * activate properly in the test context (likely due to R2DBC presence or
 * timing issues with @DynamicPropertySource).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestSpanExporterConfig {

    // ============= DataSource Bean (Manual) =============

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(env.getProperty("spring.datasource.url"));
        dataSource.setUsername(env.getProperty("spring.datasource.username"));
        dataSource.setPassword(env.getProperty("spring.datasource.password"));
        dataSource.setDriverClassName(env.getProperty("spring.datasource.driver-class-name", "org.postgresql.Driver"));
        dataSource.setMaximumPoolSize(5);
        return dataSource;
    }

    // ============= Tracing Test Beans =============

    @Bean
    public SimpleTracer simpleTracer() {
        return new SimpleTracer();
    }

    @Bean
    @Primary
    public Tracer tracer(SimpleTracer simpleTracer) {
        return simpleTracer;
    }

    @Bean
    public SpanHandler testSpanHandler() {
        return new SpanHandler() {
            @Override
            public boolean end(TraceContext context, MutableSpan span, Cause cause) {
                BaseIntegrationTest.SPAN_QUEUE.add(new BraveFinishedSpanAdapter(context, span));
                return true;
            }
        };
    }

    private static class BraveFinishedSpanAdapter implements FinishedSpan {
        private final TraceContext context;
        private final MutableSpan span;

        // Caching tags to match interface expectations if needed, but direct access is
        // better

        public BraveFinishedSpanAdapter(TraceContext context, MutableSpan span) {
            this.context = context;
            // We must copy the span as MutableSpan is mutable and reused
            this.span = new MutableSpan(span);
        }

        @Override
        public String getName() {
            return span.name();
        }

        @Override
        public Instant getStartTimestamp() {
            return Instant.ofEpochMilli(span.startTimestamp() / 1000);
        }

        @Override
        public Instant getEndTimestamp() {
            return Instant.ofEpochMilli(span.finishTimestamp() / 1000);
        }

        @Override
        public Map<String, String> getTags() {
            return span.tags();
        }

        @Override
        public Collection<Map.Entry<Long, String>> getEvents() {
            return Collections.emptyList();
        }

        @Override
        public String getSpanId() {
            return context.spanIdString();
        }

        @Override
        public String getTraceId() {
            return context.traceIdString();
        }

        @Override
        public String getParentId() {
            return context.parentIdString();
        }

        @Override
        public String getRemoteIp() {
            return span.remoteIp();
        }

        @Override
        public int getRemotePort() {
            return span.remotePort();
        }

        @Override
        public Throwable getError() {
            return span.error();
        }

        @Override
        public Kind getKind() {
            switch (span.kind()) {
                case CLIENT:
                    return Kind.CLIENT;
                case SERVER:
                    return Kind.SERVER;
                case PRODUCER:
                    return Kind.PRODUCER;
                case CONSUMER:
                    return Kind.CONSUMER;
                default:
                    return null;
            }
        }

        public java.util.List<io.micrometer.tracing.Link> getLinks() {
            return Collections.emptyList();
        }

        @Override
        public String getRemoteServiceName() {
            return span.remoteServiceName();
        }

        @Override
        public FinishedSpan setRemoteServiceName(String remoteServiceName) {
            span.remoteServiceName(remoteServiceName);
            return this;
        }

        @Override
        public FinishedSpan setError(Throwable error) {
            span.error(error);
            return this;
        }

        public FinishedSpan setRemoteIp(String remoteIp) {
            span.remoteIp(remoteIp);
            return this;
        }

        @Override
        public FinishedSpan setRemotePort(int remotePort) {
            span.remotePort(remotePort);
            return this;
        }

        @Override
        public FinishedSpan setLocalIp(String localIp) {
            span.localIp(localIp);
            return this;
        }

        @Override
        public FinishedSpan setName(String name) {
            span.name(name);
            return this;
        }

        @Override
        public FinishedSpan setEvents(Collection<Map.Entry<Long, String>> events) {
            for (Map.Entry<Long, String> entry : events) {
                span.annotate(entry.getKey(), entry.getValue());
            }
            return this;
        }

        @Override
        public FinishedSpan setTags(Map<String, String> tags) {
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                span.tag(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public String getLocalIp() {
            return span.localIp();
        }
    }
}
