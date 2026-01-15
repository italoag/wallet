package dev.bloco.wallet.hub.config;

import dev.bloco.wallet.hub.domain.gateway.*;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestGatewayConfig {

    @Bean
    @Primary
    public NetworkRepository networkRepository() {
        return Mockito.mock(NetworkRepository.class);
    }

    @Bean
    @Primary
    public TokenRepository tokenRepository() {
        return Mockito.mock(TokenRepository.class);
    }

    @Bean
    @Primary
    public TokenBalanceRepository tokenBalanceRepository() {
        return Mockito.mock(TokenBalanceRepository.class);
    }

    @Bean
    @Primary
    public WalletTokenRepository walletTokenRepository() {
        return Mockito.mock(WalletTokenRepository.class);
    }

    @Bean
    @Primary
    public UserSessionRepository userSessionRepository() {
        return Mockito.mock(UserSessionRepository.class);
    }

    @Bean
    @Primary
    public TransactionFeeRepository transactionFeeRepository() {
        return Mockito.mock(TransactionFeeRepository.class);
    }
}
