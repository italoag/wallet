package dev.bloco.wallet.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration;

/**
 * Entry point for the WalletHub application.
 * This class serves as the main configuration and launching mechanism
 * for the application, annotated with {@code @SpringBootApplication}.
 * It defines the package scanning for JPA entities and other components.
 *<p/>
 * Annotations:
 * {@code @SpringBootApplication} - Denotes this class as the primary Spring Boot application.
 * {@code @EntityScan} - Configures JPA entity scanning for specified base packages:
 * - dev.bloco.wallet.hub.infra.provider.data.entity
 * - dev.bloco.wallet.hub.infra.provider.data
 * - org.springframework.statemachine.data.jpa
 *<p/>
 * Methods:
 * {@code main} - The main method which invokes the {@code SpringApplication.run()} method
 * to bootstrap the WalletHub application.
 */
@SpringBootApplication(exclude = {
        // Excluded because spring-statemachine 4.0.1 references old Spring Boot 3.x autoconfigure class
        StateMachineJpaRepositoriesAutoConfiguration.class
})
@EnableJpaRepositories(basePackages = {
        "dev.bloco.wallet.hub.infra.provider.data.repository",
        "org.springframework.statemachine.data.jpa"
})
@EntityScan(basePackages = {
        "dev.bloco.wallet.hub.infra.provider.data.entity",
        "dev.bloco.wallet.hub.infra.provider.data",
        "org.springframework.statemachine.data.jpa"
})
public class WalletHubApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(WalletHubApplication.class);
        application.setWebApplicationType(WebApplicationType.REACTIVE);
        application.setAllowBeanDefinitionOverriding(true);
        application.run(args);
    }

}
