package dev.bloco.wallet.hub.infra.adapter.tracing.integration;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Container configuration interface for integration tests.
 * 
 * These containers are shared between tests using @ImportTestcontainers.
 * The containers are started automatically before the Spring context and
 * 
 * @ServiceConnection configures the datasource, kafka, redis, and OTLP
 *                    properties automatically.
 */
public interface TestContainers {

        /**
         * PostgreSQL container - explicitly specifies JDBC for
         * JPA/EntityManagerFactory.
         * By default both JDBC and R2DBC should be configured, but we need JDBC
         * explicitly
         * for JPA repositories.
         */
        @Container
        @ServiceConnection(type = { JdbcConnectionDetails.class, R2dbcConnectionDetails.class })
        PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
                        DockerImageName.parse("postgres:16-alpine"))
                        .withDatabaseName("wallet_test")
                        .withUsername("test")
                        .withPassword("test")
                        .withReuse(true);

        @Container
        @ServiceConnection
        ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(
                        DockerImageName.parse("confluentinc/cp-kafka:latest"))
                        .withReuse(true);

        @Container
        @ServiceConnection(name = "redis")
        RedisContainer REDIS = new RedisContainer(
                        RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG))
                        .withReuse(true);

        @Container
        @ServiceConnection
        LgtmStackContainer LGTM = new LgtmStackContainer(
                        DockerImageName.parse("grafana/otel-lgtm:latest"))
                        .withStartupTimeout(Duration.ofMinutes(2))
                        .withReuse(true);
}
