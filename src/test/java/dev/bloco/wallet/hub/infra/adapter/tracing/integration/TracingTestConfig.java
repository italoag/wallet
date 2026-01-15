package dev.bloco.wallet.hub.infra.adapter.tracing.integration;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TracingTestConfig {

    @Bean
    @Primary
    public SimpleTracer simpleTracer() {
        return new SimpleTracer();
    }
}