package dev.bloco.wallet.hub.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

/**
 * Test configuration that explicitly creates JPA beans.
 * 
 * <p>
 * In Spring Boot 4.0.1, when using WebApplicationType.REACTIVE,
 * the JPA auto-configurations may not be loaded automatically.
 * This configuration ensures that JPA is properly configured for tests.
 * </p>
 */
@TestConfiguration
// EnableJpaRepositories is already configured in WalletHubApplication
// This class only provides DataSource and EntityManagerFactory beans for tests
public class TestJpaConfiguration {

    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                .driverClassName("org.h2.Driver")
                .username("sa")
                .password("")
                .build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(
                "dev.bloco.wallet.hub.infra.provider.data.entity",
                "dev.bloco.wallet.hub.infra.provider.data",
                "org.springframework.statemachine.data.jpa");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setShowSql(false);
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        properties.put("jakarta.persistence.jdbc.user", "sa");
        properties.put("jakarta.persistence.jdbc.password", "");
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
}
