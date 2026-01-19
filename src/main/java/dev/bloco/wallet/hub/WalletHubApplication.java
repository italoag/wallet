package dev.bloco.wallet.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Entry point for the WalletHub application.
 * This class serves as the main configuration and launching mechanism
 * for the application, annotated with {@code @SpringBootApplication}.
 * It defines the package scanning for JPA entities and other components.
 * <p/>
 * Annotations:
 * {@code @SpringBootApplication} - Denotes this class as the primary Spring
 * Boot application.
 * {@code @EntityScan} - Configures JPA entity scanning for specified base
 * packages:
 * - dev.bloco.wallet.hub.infra.provider.data.entity
 * - dev.bloco.wallet.hub.infra.provider.data
 * - org.springframework.statemachine.data.jpa
 * <p/>
 * Methods:
 * {@code main} - The main method which invokes the
 * {@code SpringApplication.run()} method
 * to bootstrap the WalletHub application.
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "dev.bloco.wallet.hub.infra.provider.data.repository")
@EntityScan(basePackages = {
        "dev.bloco.wallet.hub.infra.provider.data.entity",
        "dev.bloco.wallet.hub.infra.provider.data",
        "org.springframework.statemachine.data.jpa"
})
public class WalletHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletHubApplication.class, args);
    }

}
