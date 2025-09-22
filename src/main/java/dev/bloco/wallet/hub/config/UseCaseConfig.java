package dev.bloco.wallet.hub.config;

import dev.bloco.wallet.hub.domain.gateway.TransactionRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.gateway.AddressRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenRepository;
import dev.bloco.wallet.hub.domain.gateway.TokenBalanceRepository;
import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.gateway.WalletTokenRepository;
import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.domain.gateway.UserSessionRepository;
import dev.bloco.wallet.hub.domain.gateway.TransactionFeeRepository;
import dev.bloco.wallet.hub.domain.gateway.DomainEventPublisher;
import dev.bloco.wallet.hub.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The UseCaseConfig class is a Spring configuration class that defines beans for various use cases
 * within the application. Each use case represents a specific business operation related to wallet
 * and transaction management. The class encapsulates the creation and configuration of these use cases,
 * injecting their required dependencies such as repositories and event publishers.
 *<p/>
 * Beans defined in this configuration:
 *<p/>
 * 1. CreateWalletUseCase
 *    - Use case for creating a new wallet. It manages the creation of wallet entities and publishes
 *      the wallet creation event.
 *<p/>
 * 2. AddFundsUseCase
 *    - Use case for adding funds to a wallet. It handles the wallet's balance update, records the transaction,
 *      and publishes a fund addition event.
 *<p/>
 * 3. WithdrawFundsUseCase
 *    - Use case for withdrawing funds from a wallet. It checks for sufficient balance, updates the wallet's balance,
 *      records the transaction, and publishes a fund withdrawal event.
 *<p/>
 * 4. TransferFundsUseCase
 *    - Use case for transferring funds between wallets. This use case deducts the specified amount from the source wallet,
 *      adds it to the destination wallet, records the transfer transaction, and publishes a fund transfer event.
 *<p/>
 * 5. CheckBalanceUseCase
 *    - Use case for checking the balance of a specific wallet. It retrieves the current wallet balance
 *      based on the wallet ID.
 */
@Configuration
public class UseCaseConfig {

  /**
   * Defines a Spring Bean for the CreateWalletUseCase. This use case is responsible for creating
   * a new wallet for a user. It manages the persistence of the wallet entity and publishes
   * a wallet creation event.
   *
   * @param walletRepository the repository responsible for managing wallet persistence
   * @param eventPublisher the publisher responsible for publishing domain events, such as wallet creation events
   * @return a configured instance of CreateWalletUseCase
   */
    @Bean
    public CreateWalletUseCase createWalletUseCase(WalletRepository walletRepository, DomainEventPublisher eventPublisher) {
        return new CreateWalletUseCase(walletRepository, eventPublisher);
    }

  /**
   * Defines a Spring Bean for the AddFundsUseCase. This use case is responsible for adding funds
   * to a specific wallet within the system. It manages the validation and updating of the wallet's
   * balance, the persistence of the associated deposit transaction, and the publication of
   * a domain event representing the operation.
   *
   * @param walletRepository the repository responsible for accessing and managing wallet data
   * @param transactionRepository the repository responsible for persisting transaction records
   * @param eventPublisher the publisher responsible for publishing domain events, such as FundsAddedEvent
   * @return a configured instance of AddFundsUseCase
   */
    @Bean
    public AddFundsUseCase addFundsUseCase(WalletRepository walletRepository, TransactionRepository transactionRepository, DomainEventPublisher eventPublisher) {
        return new AddFundsUseCase(walletRepository, transactionRepository, eventPublisher);
    }

  /**
   * Defines a Spring Bean for the WithdrawFundsUseCase. This use case handles the
   * process of withdrawing funds from a specific wallet within the system. It manages
   * the validation and update of the wallet's balance, the persistence of the associated
   * withdrawal transaction, and the publication of a domain event representing the operation.
   *
   * @param walletRepository        the repository responsible for accessing and managing wallet data
   * @param transactionRepository   the repository responsible for persisting transaction records
   * @param eventPublisher          the publisher responsible for publishing domain events, such as FundsWithdrawnEvent
   * @return a configured instance of WithdrawFundsUseCase
   */
    @Bean
    public WithdrawFundsUseCase withdrawFundsUseCase(WalletRepository walletRepository, TransactionRepository transactionRepository, DomainEventPublisher eventPublisher) {
        return new WithdrawFundsUseCase(walletRepository, transactionRepository, eventPublisher);
    }

  /**
   * Defines a Spring Bean for the TransferFundsUseCase. This use case is responsible for transferring
   * funds between two wallets within the system. It performs validation and updates the balances of
   * the source and destination wallets, persists the associated transfer transaction, and publishes
   * a domain event to signal that the transfer has occurred.
   *
   * @param walletRepository        the repository responsible for accessing and managing wallet data
   * @param transactionRepository   the repository responsible for persisting transaction records
   * @param eventPublisher          the publisher responsible for publishing domain events, such as FundsTransferredEvent
   * @return a configured instance of TransferFundsUseCase
   */
    @Bean
    public TransferFundsUseCase transferFundsUseCase(WalletRepository walletRepository, TransactionRepository transactionRepository, DomainEventPublisher eventPublisher) {
        return new TransferFundsUseCase(walletRepository, transactionRepository, eventPublisher);
    }

  /**
   * Defines a Spring Bean for the CheckBalanceUseCase. This use case is responsible for
   * retrieving the balance of a specific wallet within the system. It validates the existence
   * of the wallet and fetches its balance using the provided repository.
   *
   * @param walletRepository the repository responsible for accessing and managing wallet data
   * @return a configured instance of CheckBalanceUseCase
   */
  @Bean
    public CheckBalanceUseCase checkBalanceUseCase(WalletRepository walletRepository) {
        return new CheckBalanceUseCase(walletRepository);
    }

    /**
     * Defines a Spring Bean for the CreateTransactionUseCase.
     */
    @Bean
    public CreateTransactionUseCase createTransactionUseCase(TransactionRepository transactionRepository,
                                                             DomainEventPublisher eventPublisher) {
        return new CreateTransactionUseCase(transactionRepository, eventPublisher);
    }

    /**
     * Defines a Spring Bean for the ConfirmTransactionUseCase.
     */
    @Bean
    public ConfirmTransactionUseCase confirmTransactionUseCase(TransactionRepository transactionRepository,
                                                               DomainEventPublisher eventPublisher) {
        return new ConfirmTransactionUseCase(transactionRepository, eventPublisher);
    }

    /**
     * Defines a Spring Bean for the FailTransactionUseCase.
     */
    @Bean
    public FailTransactionUseCase failTransactionUseCase(TransactionRepository transactionRepository,
                                                         DomainEventPublisher eventPublisher) {
        return new FailTransactionUseCase(transactionRepository, eventPublisher);
    }

    // ========== NEW WALLET MANAGEMENT USE CASES ==========
    
    @Bean
    public UpdateWalletUseCase updateWalletUseCase(WalletRepository walletRepository, DomainEventPublisher eventPublisher) {
        return new UpdateWalletUseCase(walletRepository, eventPublisher);
    }
    
    @Bean
    public ActivateWalletUseCase activateWalletUseCase(WalletRepository walletRepository, DomainEventPublisher eventPublisher) {
        return new ActivateWalletUseCase(walletRepository, eventPublisher);
    }
    
    @Bean
    public DeactivateWalletUseCase deactivateWalletUseCase(WalletRepository walletRepository, DomainEventPublisher eventPublisher) {
        return new DeactivateWalletUseCase(walletRepository, eventPublisher);
    }
    
    @Bean
    public DeleteWalletUseCase deleteWalletUseCase(WalletRepository walletRepository, DomainEventPublisher eventPublisher) {
        return new DeleteWalletUseCase(walletRepository, eventPublisher);
    }
    
    @Bean
    public ListWalletsUseCase listWalletsUseCase(WalletRepository walletRepository) {
        return new ListWalletsUseCase(walletRepository);
    }
    
    @Bean
    public GetWalletDetailsUseCase getWalletDetailsUseCase(WalletRepository walletRepository, AddressRepository addressRepository) {
        return new GetWalletDetailsUseCase(walletRepository, addressRepository);
    }
    
    @Bean
    public RecoverWalletUseCase recoverWalletUseCase(WalletRepository walletRepository, DomainEventPublisher eventPublisher) {
        return new RecoverWalletUseCase(walletRepository, eventPublisher);
    }
    
    // ========== ADDRESS MANAGEMENT USE CASES ==========
    
    @Bean
    public CreateAddressUseCase createAddressUseCase(AddressRepository addressRepository, WalletRepository walletRepository, 
                                                     NetworkRepository networkRepository, DomainEventPublisher eventPublisher) {
        return new CreateAddressUseCase(addressRepository, walletRepository, networkRepository, eventPublisher);
    }
    
    @Bean
    public ValidateAddressUseCase validateAddressUseCase(NetworkRepository networkRepository) {
        return new ValidateAddressUseCase(networkRepository);
    }
    
    @Bean
    public GetAddressBalanceUseCase getAddressBalanceUseCase(AddressRepository addressRepository, TokenBalanceRepository tokenBalanceRepository) {
        return new GetAddressBalanceUseCase(addressRepository, tokenBalanceRepository);
    }
    
    @Bean
    public ListAddressesByWalletUseCase listAddressesByWalletUseCase(AddressRepository addressRepository, WalletRepository walletRepository) {
        return new ListAddressesByWalletUseCase(addressRepository, walletRepository);
    }
    
    @Bean
    public UpdateAddressStatusUseCase updateAddressStatusUseCase(AddressRepository addressRepository, DomainEventPublisher eventPublisher) {
        return new UpdateAddressStatusUseCase(addressRepository, eventPublisher);
    }
    
    @Bean
    public ImportAddressUseCase importAddressUseCase(AddressRepository addressRepository, WalletRepository walletRepository, 
                                                    NetworkRepository networkRepository, DomainEventPublisher eventPublisher, 
                                                    ValidateAddressUseCase validateAddressUseCase) {
        return new ImportAddressUseCase(addressRepository, walletRepository, networkRepository, eventPublisher, validateAddressUseCase);
    }
    
    // ========== TOKEN MANAGEMENT USE CASES ==========
    
    @Bean
    public AddTokenToWalletUseCase addTokenToWalletUseCase(WalletRepository walletRepository, TokenRepository tokenRepository, 
                                                          WalletTokenRepository walletTokenRepository, DomainEventPublisher eventPublisher) {
        return new AddTokenToWalletUseCase(walletRepository, tokenRepository, walletTokenRepository, eventPublisher);
    }
    
    @Bean
    public RemoveTokenFromWalletUseCase removeTokenFromWalletUseCase(WalletRepository walletRepository, WalletTokenRepository walletTokenRepository, 
                                                                    DomainEventPublisher eventPublisher) {
        return new RemoveTokenFromWalletUseCase(walletRepository, walletTokenRepository, eventPublisher);
    }
    
    @Bean
    public GetTokenBalanceUseCase getTokenBalanceUseCase(WalletRepository walletRepository, TokenRepository tokenRepository, 
                                                        TokenBalanceRepository tokenBalanceRepository) {
        return new GetTokenBalanceUseCase(walletRepository, tokenRepository, tokenBalanceRepository);
    }
    
    @Bean
    public ListSupportedTokensUseCase listSupportedTokensUseCase(TokenRepository tokenRepository, NetworkRepository networkRepository) {
        return new ListSupportedTokensUseCase(tokenRepository, networkRepository);
    }
    
    // ========== NETWORK MANAGEMENT USE CASES ==========
    
    @Bean
    public AddNetworkUseCase addNetworkUseCase(NetworkRepository networkRepository, DomainEventPublisher eventPublisher) {
        return new AddNetworkUseCase(networkRepository, eventPublisher);
    }
    
    // ========== USER MANAGEMENT USE CASES ==========
    
    @Bean
    public CreateUserUseCase createUserUseCase(UserRepository userRepository, DomainEventPublisher eventPublisher) {
        return new CreateUserUseCase(userRepository, eventPublisher);
    }
    
    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(UserRepository userRepository, UserSessionRepository sessionRepository, 
                                                          DomainEventPublisher eventPublisher) {
        return new AuthenticateUserUseCase(userRepository, sessionRepository, eventPublisher);
    }
    
    @Bean
    public UpdateUserProfileUseCase updateUserProfileUseCase(UserRepository userRepository, DomainEventPublisher eventPublisher) {
        return new UpdateUserProfileUseCase(userRepository, eventPublisher);
    }
    
    @Bean
    public ChangePasswordUseCase changePasswordUseCase(UserRepository userRepository, UserSessionRepository sessionRepository, 
                                                      DomainEventPublisher eventPublisher) {
        return new ChangePasswordUseCase(userRepository, sessionRepository, eventPublisher);
    }
    
    @Bean
    public DeactivateUserUseCase deactivateUserUseCase(UserRepository userRepository, UserSessionRepository sessionRepository, 
                                                      DomainEventPublisher eventPublisher) {
        return new DeactivateUserUseCase(userRepository, sessionRepository, eventPublisher);
    }
    
    // ========== ANALYTICS AND PORTFOLIO USE CASES ==========
    
    @Bean
    public EstimateTransactionFeeUseCase estimateTransactionFeeUseCase(NetworkRepository networkRepository, 
                                                                      TransactionFeeRepository feeRepository) {
        return new EstimateTransactionFeeUseCase(networkRepository, feeRepository);
    }
    
    @Bean
    public GetPortfolioSummaryUseCase getPortfolioSummaryUseCase(WalletRepository walletRepository, AddressRepository addressRepository, 
                                                                TokenBalanceRepository tokenBalanceRepository, TokenRepository tokenRepository) {
        return new GetPortfolioSummaryUseCase(walletRepository, addressRepository, tokenBalanceRepository, tokenRepository);
    }
    
    @Bean
    public ListNetworksUseCase listNetworksUseCase(NetworkRepository networkRepository) {
        return new ListNetworksUseCase(networkRepository);
    }
}
