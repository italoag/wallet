package dev.bloco.wallet.hub.infra.adapter.tracing.integration;

import dev.bloco.wallet.hub.infra.provider.data.repository.OutboxRepository;
import dev.bloco.wallet.hub.infra.provider.data.repository.SpringDataUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class JpaDiagnosticTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void checkRepositories() {
        assertThat(context.containsBean("outboxRepository")).isTrue();
        assertThat(context.containsBean("springDataUserRepository")).isTrue();
    }
}
