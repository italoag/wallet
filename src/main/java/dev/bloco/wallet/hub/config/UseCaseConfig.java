package dev.bloco.wallet.hub.config;

import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public CreateWalletUseCase createWalletUseCase(WalletRepository walletRepository, DomainEventPublisher eventPublisher) {
        return new CreateWalletUseCase(walletRepository, eventPublisher);
    }

    @Bean
    public AddFundsUseCase addFundsUseCase(WalletRepository walletRepository, TransactionRepository transactionRepository, DomainEventPublisher eventPublisher) {
        return new AddFundsUseCase(walletRepository, transactionRepository, eventPublisher);
    }

    @Bean
    public WithdrawFundsUseCase withdrawFundsUseCase(WalletRepository walletRepository, TransactionRepository transactionRepository, DomainEventPublisher eventPublisher) {
        return new WithdrawFundsUseCase(walletRepository, transactionRepository, eventPublisher);
    }

    @Bean
    public TransferFundsUseCase transferFundsUseCase(WalletRepository walletRepository, TransactionRepository transactionRepository, DomainEventPublisher eventPublisher) {
        return new TransferFundsUseCase(walletRepository, transactionRepository, eventPublisher);
    }

    @Bean
    public CheckBalanceUseCase checkBalanceUseCase(WalletRepository walletRepository) {
        return new CheckBalanceUseCase(walletRepository);
    }
}
