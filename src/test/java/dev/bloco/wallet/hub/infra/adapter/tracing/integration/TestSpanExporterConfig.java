package dev.bloco.wallet.hub.infra.adapter.tracing.integration;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

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
    public SpanHandler testSpanHandler(SimpleTracer simpleTracer) {
        return new SpanHandler() {
            @Override
            public boolean end(TraceContext context, MutableSpan span, Cause cause) {
                // simpleTracer handles collecting spans automatically if it's the bridge
                // but we can also manually add here if needed for custom FinishedSpan adapters
                return true;
            }
        };
    }

}
