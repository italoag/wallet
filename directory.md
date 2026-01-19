# Directory Structure

```bash
blockchain-connector/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── company/
│   │   │           └── blockchain/
│   │   │               └── connector/
│   │   │                   │
│   │   │                   ├── domain/                          # CAMADA DE DOMÍNIO (núcleo da aplicação)
│   │   │                   │   │
│   │   │                   │   ├── model/                       # Entidades e Agregados
│   │   │                   │   │   ├── transaction/
│   │   │                   │   │   │   ├── Transaction.java            # Agregado raiz
│   │   │                   │   │   │   ├── TransactionId.java          # Value Object
│   │   │                   │   │   │   ├── TransactionStatus.java      # Enum
│   │   │                   │   │   │   ├── TransactionHash.java        # Value Object
│   │   │                   │   │   │   ├── GasConfiguration.java       # Value Object
│   │   │                   │   │   │   ├── Nonce.java                  # Value Object
│   │   │                   │   │   │   └── TransactionReceipt.java     # Entity
│   │   │                   │   │   │
│   │   │                   │   │   ├── signature/
│   │   │                   │   │   │   ├── SignatureRequest.java       # Entity
│   │   │                   │   │   │   ├── SignatureResponse.java      # Value Object
│   │   │                   │   │   │   ├── SignatureStatus.java        # Enum
│   │   │                   │   │   │   └── Signer.java                 # Value Object
│   │   │                   │   │   │
│   │   │                   │   │   ├── blockchain/
│   │   │                   │   │   │   ├── BlockchainNetwork.java      # Value Object
│   │   │                   │   │   │   ├── ChainId.java                # Value Object
│   │   │                   │   │   │   ├── BlockNumber.java            # Value Object
│   │   │                   │   │   │   ├── WalletAddress.java          # Value Object
│   │   │                   │   │   │   └── SmartContract.java          # Value Object
│   │   │                   │   │   │
│   │   │                   │   │   └── retry/
│   │   │                   │   │       ├── RetryPolicy.java            # Value Object
│   │   │                   │   │       ├── RetryAttempt.java           # Value Object
│   │   │                   │   │       └── RetryStrategy.java          # Enum
│   │   │                   │   │
│   │   │                   │   ├── event/                       # Domain Events
│   │   │                   │   │   ├── TransactionReceivedEvent.java
│   │   │                   │   │   ├── TransactionValidatedEvent.java
│   │   │                   │   │   ├── GasCalculatedEvent.java
│   │   │                   │   │   ├── SignatureRequestedEvent.java
│   │   │                   │   │   ├── TransactionSignedEvent.java
│   │   │                   │   │   ├── TransactionSubmittedEvent.java
│   │   │                   │   │   ├── TransactionConfirmedEvent.java
│   │   │                   │   │   ├── TransactionFailedEvent.java
│   │   │                   │   │   ├── RetryScheduledEvent.java
│   │   │                   │   │   └── DomainEvent.java                # Interface base
│   │   │                   │   │
│   │   │                   │   ├── exception/                   # Domain Exceptions
│   │   │                   │   │   ├── DomainException.java
│   │   │                   │   │   ├── InvalidTransactionException.java
│   │   │                   │   │   ├── InsufficientGasException.java
│   │   │                   │   │   ├── SignatureTimeoutException.java
│   │   │                   │   │   ├── BlockchainException.java
│   │   │                   │   │   └── MaxRetryExceededException.java
│   │   │                   │   │
│   │   │                   │   ├── port/                        # Portas (interfaces)
│   │   │                   │   │   ├── inbound/                 # Casos de uso (entradas)
│   │   │                   │   │   │   ├── SubmitTransactionUseCase.java
│   │   │                   │   │   │   ├── ProcessSignedTransactionUseCase.java
│   │   │                   │   │   │   ├── CheckTransactionStatusUseCase.java
│   │   │                   │   │   │   ├── RetryTransactionUseCase.java
│   │   │                   │   │   │   └── CancelTransactionUseCase.java
│   │   │                   │   │   │
│   │   │                   │   │   └── outbound/                # Portas de saída
│   │   │                   │   │       ├── TransactionRepository.java
│   │   │                   │   │       ├── BlockchainGateway.java
│   │   │                   │   │       ├── SignerGateway.java
│   │   │                   │   │       ├── EventPublisher.java
│   │   │                   │   │       ├── GasEstimator.java
│   │   │                   │   │       ├── NonceProvider.java
│   │   │                   │   │       └── TransactionMonitor.java
│   │   │                   │   │
│   │   │                   │   └── service/                     # Domain Services
│   │   │                   │       ├── TransactionValidator.java
│   │   │                   │       ├── GasCalculator.java
│   │   │                   │       ├── NonceManager.java
│   │   │                   │       ├── RetryPolicyService.java
│   │   │                   │       └── TransactionStateManager.java
│   │   │                   │
│   │   │                   ├── application/                     # CAMADA DE APLICAÇÃO
│   │   │                   │   │
│   │   │                   │   ├── usecase/                     # Implementação dos casos de uso
│   │   │                   │   │   ├── SubmitTransactionUseCaseImpl.java
│   │   │                   │   │   ├── ProcessSignedTransactionUseCaseImpl.java
│   │   │                   │   │   ├── CheckTransactionStatusUseCaseImpl.java
│   │   │                   │   │   ├── RetryTransactionUseCaseImpl.java
│   │   │                   │   │   └── CancelTransactionUseCaseImpl.java
│   │   │                   │   │
│   │   │                   │   ├── service/                     # Application Services (orquestração)
│   │   │                   │   │   ├── TransactionOrchestrator.java
│   │   │                   │   │   ├── SignatureCoordinator.java
│   │   │                   │   │   ├── BlockchainSubmissionService.java
│   │   │                   │   │   └── TransactionMonitoringService.java
│   │   │                   │   │
│   │   │                   │   ├── statemachine/                # State Machine
│   │   │                   │   │   ├── config/
│   │   │                   │   │   │   ├── TransactionStateMachineConfig.java
│   │   │                   │   │   │   └── StateMachineListenerConfig.java
│   │   │                   │   │   ├── state/
│   │   │                   │   │   │   └── TransactionState.java         # Enum de estados
│   │   │                   │   │   ├── event/
│   │   │                   │   │   │   └── TransactionStateEvent.java    # Enum de eventos
│   │   │                   │   │   ├── action/
│   │   │                   │   │   │   ├── ValidateTransactionAction.java
│   │   │                   │   │   │   ├── CalculateGasAction.java
│   │   │                   │   │   │   ├── RequestSignatureAction.java
│   │   │                   │   │   │   ├── SubmitToBlockchainAction.java
│   │   │                   │   │   │   ├── MonitorConfirmationAction.java
│   │   │                   │   │   │   ├── HandleErrorAction.java
│   │   │                   │   │   │   └── PublishCompletionAction.java
│   │   │                   │   │   └── guard/
│   │   │                   │   │       ├── CanRetryGuard.java
│   │   │                   │   │       ├── SignatureTimeoutGuard.java
│   │   │                   │   │       └── MaxRetryGuard.java
│   │   │                   │   │
│   │   │                   │   ├── mapper/                      # Application DTOs e Mappers
│   │   │                   │   │   ├── TransactionMapper.java
│   │   │                   │   │   ├── SignatureMapper.java
│   │   │                   │   │   └── BlockchainMapper.java
│   │   │                   │   │
│   │   │                   │   └── dto/                         # DTOs da camada de aplicação
│   │   │                   │       ├── TransactionRequest.java
│   │   │                   │       ├── TransactionResponse.java
│   │   │                   │       ├── SignatureRequest.java
│   │   │                   │       ├── SignatureResponse.java
│   │   │                   │       └── TransactionStatusResponse.java
│   │   │                   │
│   │   │                   └── infrastructure/                  # CAMADA DE INFRAESTRUTURA
│   │   │                       │
│   │   │                       ├── adapter/                     # Adaptadores
│   │   │                       │   │
│   │   │                       │   ├── inbound/                 # Adaptadores de entrada
│   │   │                       │   │   ├── messaging/
│   │   │                       │   │   │   ├── kafka/
│   │   │                       │   │   │   │   ├── consumer/
│   │   │                       │   │   │   │   │   ├── TransactionConsumer.java
│   │   │                       │   │   │   │   │   ├── SignedTransactionConsumer.java
│   │   │                       │   │   │   │   │   └── BlockchainEventConsumer.java
│   │   │                       │   │   │   │   ├── config/
│   │   │                       │   │   │   │   │   ├── KafkaConsumerConfig.java
│   │   │                       │   │   │   │   │   ├── KafkaProducerConfig.java
│   │   │                       │   │   │   │   │   └── KafkaTopicConfig.java
│   │   │                       │   │   │   │   └── dto/
│   │   │                       │   │   │   │       ├── TransactionKafkaMessage.java
│   │   │                       │   │   │   │       └── SignatureKafkaMessage.java
│   │   │                       │   │   │   │
│   │   │                       │   │   │   └── mapper/
│   │   │                       │   │   │       └── KafkaMessageMapper.java
│   │   │                       │   │   │
│   │   │                       │   │   └── rest/
│   │   │                       │   │       ├── controller/
│   │   │                       │   │       │   ├── TransactionController.java
│   │   │                       │   │       │   ├── HealthController.java
│   │   │                       │   │       │   └── MetricsController.java
│   │   │                       │   │       ├── dto/
│   │   │                       │   │       │   ├── TransactionRequestDTO.java
│   │   │                       │   │       │   ├── TransactionResponseDTO.java
│   │   │                       │   │       │   └── ErrorResponseDTO.java
│   │   │                       │   │       └── mapper/
│   │   │                       │   │           └── RestDTOMapper.java
│   │   │                       │   │
│   │   │                       │   └── outbound/                # Adaptadores de saída
│   │   │                       │       │
│   │   │                       │       ├── persistence/
│   │   │                       │       │   ├── mongodb/
│   │   │                       │       │   │   ├── repository/
│   │   │                       │       │   │   │   ├── TransactionMongoRepository.java
│   │   │                       │       │   │   │   ├── SignatureRequestMongoRepository.java
│   │   │                       │       │   │   │   └── OutboxEventRepository.java
│   │   │                       │       │   │   ├── entity/
│   │   │                       │       │   │   │   ├── TransactionDocument.java
│   │   │                       │       │   │   │   ├── SignatureRequestDocument.java
│   │   │                       │       │   │   │   └── OutboxEventDocument.java
│   │   │                       │       │   │   ├── mapper/
│   │   │                       │       │   │   │   ├── TransactionDocumentMapper.java
│   │   │                       │       │   │   │   └── SignatureDocumentMapper.java
│   │   │                       │       │   │   └── config/
│   │   │                       │       │   │       └── MongoConfig.java
│   │   │                       │       │   │
│   │   │                       │       │   └── TransactionRepositoryImpl.java
│   │   │                       │       │
│   │   │                       │       ├── blockchain/
│   │   │                       │       │   ├── web3j/
│   │   │                       │       │   │   ├── Web3jBlockchainGatewayImpl.java
│   │   │                       │   │   │   ├── Web3jGasEstimatorImpl.java
│   │   │                       │       │   │   ├── Web3jNonceProviderImpl.java
│   │   │                       │       │   │   ├── Web3jTransactionMonitorImpl.java
│   │   │                       │       │   │   ├── config/
│   │   │                       │       │   │   │   ├── Web3jConfig.java
│   │   │                       │       │   │   │   └── BlockchainNetworkConfig.java
│   │   │                       │       │   │   ├── mapper/
│   │   │                       │       │   │   │   └── Web3jMapper.java
│   │   │                       │       │   │   └── util/
│   │   │                       │       │   │       ├── Web3jUtils.java
│   │   │                       │       │   │       └── EthereumAddressValidator.java
│   │   │                       │       │   │
│   │   │                       │       │   └── dto/
│   │   │                       │       │       └── BlockchainTransactionDTO.java
│   │   │                       │       │
│   │   │                       │       ├── signer/
│   │   │                       │       │   ├── SignerGatewayImpl.java
│   │   │                       │       │   ├── kafka/
│   │   │                       │       │   │   ├── KafkaSignerProducer.java
│   │   │                       │       │   │   └── KafkaSignerConsumer.java
│   │   │                       │       │   ├── rest/
│   │   │                       │       │   │   ├── RestSignerClient.java
│   │   │                       │       │   │   └── SignerWebClientConfig.java
│   │   │                       │       │   ├── strategy/
│   │   │                       │       │   │   ├── SignerStrategy.java
│   │   │                       │       │   │   ├── KafkaSignerStrategy.java
│   │   │                       │       │   │   └── RestSignerStrategy.java
│   │   │                       │       │   └── dto/
│   │   │                       │       │       ├── SignerRequestDTO.java
│   │   │                       │       │       └── SignerResponseDTO.java
│   │   │                       │       │
│   │   │                       │       ├── messaging/
│   │   │                       │       │   ├── kafka/
│   │   │                       │       │   │   ├── producer/
│   │   │                       │       │   │   │   ├── TransactionEventProducer.java
│   │   │                       │       │   │   │   ├── SignatureRequestProducer.java
│   │   │                       │       │   │   │   └── DeadLetterProducer.java
│   │   │                       │       │   │   └── mapper/
│   │   │                       │       │   │       └── EventMessageMapper.java
│   │   │                       │       │   │
│   │   │                       │       │   └── EventPublisherImpl.java
│   │   │                       │       │
│   │   │                       │       └── cache/
│   │   │                       │           ├── RedisCacheAdapter.java
│   │   │                       │           └── config/
│   │   │                       │               └── CacheConfig.java
│   │   │                       │
│   │   │                       ├── config/                      # Configurações gerais
│   │   │                       │   ├── ApplicationConfig.java
│   │   │                       │   ├── AsyncConfig.java
│   │   │                       │   ├── SchedulingConfig.java
│   │   │                       │   ├── SecurityConfig.java
│   │   │                       │   ├── SwaggerConfig.java
│   │   │                       │   └── properties/
│   │   │                       │       ├── BlockchainProperties.java
│   │   │                       │       ├── KafkaProperties.java
│   │   │                       │       ├── SignerProperties.java
│   │   │                       │       └── RetryProperties.java
│   │   │                       │
│   │   │                       ├── scheduler/                   # Tarefas agendadas
│   │   │                       │   ├── TransactionMonitorScheduler.java
│   │   │                       │   ├── RetryScheduler.java
│   │   │                       │   ├── OutboxProcessorScheduler.java
│   │   │                       │   └── StaleTransactionCleanupScheduler.java
│   │   │                       │
│   │   │                       ├── resilience/                  # Resiliência
│   │   │                       │   ├── CircuitBreakerConfig.java
│   │   │                       │   ├── RetryConfig.java
│   │   │                       │   ├── RateLimiterConfig.java
│   │   │                       │   └── BulkheadConfig.java
│   │   │                       │
│   │   │                       ├── observability/               # Observabilidade
│   │   │                       │   ├── metrics/
│   │   │                       │   │   ├── MetricsRegistry.java
│   │   │                       │   │   ├── TransactionMetrics.java
│   │   │                       │   │   └── BlockchainMetrics.java
│   │   │                       │   ├── tracing/
│   │   │                       │   │   └── TracingConfig.java
│   │   │                       │   └── logging/
│   │   │                       │       ├── LoggingAspect.java
│   │   │                       │       └── StructuredLogger.java
│   │   │                       │
│   │   │                       └── exception/                   # Exception Handlers
│   │   │                           ├── GlobalExceptionHandler.java
│   │   │                           ├── KafkaExceptionHandler.java
│   │   │                           └── RestExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-staging.yml
│   │       ├── application-prod.yml
│   │       ├── logback-spring.xml
│   │       ├── db/
│   │       │   └── migration/                                   # Scripts MongoDB (se usar Mongock)
│   │       │       ├── V1__initial_schema.js
│   │       │       └── V2__add_indexes.js
│   │       └── statemachine/
│   │           └── transaction-state-machine.yml                # Config State Machine (opcional)
│   │
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── company/
│       │           └── blockchain/
│       │               └── connector/
│       │                   ├── domain/
│       │                   │   ├── model/
│       │                   │   │   └── TransactionTest.java
│       │                   │   └── service/
│       │                   │       └── TransactionValidatorTest.java
│       │                   │
│       │                   ├── application/
│       │                   │   ├── usecase/
│       │                   │   │   └── SubmitTransactionUseCaseTest.java
│       │                   │   └── statemachine/
│       │                   │       └── TransactionStateMachineTest.java
│       │                   │
│       │                   ├── infrastructure/
│       │                   │   ├── adapter/
│       │                   │   │   ├── inbound/
│       │                   │   │   │   ├── messaging/
│       │                   │   │   │   │   └── TransactionConsumerTest.java
│       │                   │   │   │   └── rest/
│       │                   │   │   │       └── TransactionControllerTest.java
│       │                   │   │   └── outbound/
│       │                   │   │       ├── blockchain/
│       │                   │   │       │   └── Web3jBlockchainGatewayTest.java
│       │                   │   │       └── persistence/
│       │                   │   │           └── TransactionRepositoryImplTest.java
│       │                   │   │
│       │                   │   └── integration/
│       │                   │       ├── BlockchainIntegrationTest.java
│       │                   │       ├── KafkaIntegrationTest.java
│       │                   │       └── EndToEndTest.java
│       │                   │
│       │                   └── architecture/
│       │                       └── ArchitectureTest.java         # ArchUnit tests
│       │
│       └── resources/
│           ├── application-test.yml
│           ├── testcontainers/
│           │   ├── docker-compose-test.yml
│           │   └── kafka-test-config.yml
│           └── fixtures/
│               ├── transaction-sample.json
│               └── signature-sample.json
│
├── docker/
│   ├── Dockerfile
│   ├── Dockerfile.dev
│   └── docker-compose.yml
│
├── k8s/                                                          # Kubernetes manifests
│   ├── deployment.yml
│   ├── service.yml
│   ├── configmap.yml
│   ├── secret.yml
│   └── hpa.yml
│
├── scripts/
│   ├── build.sh
│   ├── deploy.sh
│   └── run-local.sh
│
├── docs/
│   ├── architecture/
│   │   ├── ADR/                                                  # Architecture Decision Records
│   │   │   ├── 001-hexagonal-architecture.md
│   │   │   ├── 002-state-machine-pattern.md
│   │   │   └── 003-outbox-pattern.md
│   │   ├── diagrams/
│   │   │   ├── component-diagram.puml
│   │   │   ├── sequence-diagram.puml
│   │   │   └── state-machine-diagram.puml
│   │   └── README.md
│   ├── api/
│   │   └── openapi.yml
│   └── runbook/
│       └── operational-guide.md
│
├── .github/
│   └── workflows/
│       ├── ci.yml
│       ├── cd.yml
│       └── security-scan.yml
│
├── pom.xml
├── .gitignore
├── README.md
├── CHANGELOG.md
└── LICENSE
```

## New Directory

```shell
blockchain-connector/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/blockchainconnector/
    │   │       ├── boot/
    │   │       │   ├── BlockchainConnectorApplication.java
    │   │       │   └── StartupRunner.java
    │   │       │
    │   │       ├── domain/
    │   │       │   ├── model/
    │   │       │   │   ├── transaction/
    │   │       │   │   │   ├── BlockchainTransaction.java
    │   │       │   │   │   ├── TransactionStatus.java
    │   │       │   │   │   ├── GasEstimation.java
    │   │       │   │   │   └── Nonce.java
    │   │       │   │   ├── wallet/
    │   │       │   │   │   └── Wallet.java
    │   │       │   │   └── event/
    │   │       │   │       └── ChainEvent.java
    │   │       │   │
    │   │       │   ├── exception/
    │   │       │   │   ├── TransactionNotFoundException.java
    │   │       │   │   └── SignatureException.java
    │   │       │   │
    │   │       │   ├── port/
    │   │       │   │   ├── inbound/
    │   │       │   │   │   ├── KafkaEventListenerPort.java
    │   │       │   │   │   └── ApiInboundPort.java
    │   │       │   │   ├── outbound/
    │   │       │   │   │   ├── SignerRestClientPort.java
    │   │       │   │   │   ├── SignerEventProducerPort.java
    │   │       │   │   │   ├── BlockchainClientPort.java
    │   │       │   │   │   ├── TransactionRepositoryPort.java
    │   │       │   │   │   ├── GasEstimatorPort.java
    │   │       │   │   │   ├── NonceManagerPort.java
    │   │       │   │   │   └── ConfirmationListenerPort.java
    │   │       │   │   └── state/
    │   │       │   │       ├── StateMachinePort.java
    │   │       │   │       └── StateTransitionCallbackPort.java
    │   │       │   │
    │   │       │   └── service/
    │   │       │       ├── TransactionService.java
    │   │       │       ├── GasService.java
    │   │       │       ├── NonceService.java
    │   │       │       └── ConfirmationService.java
    │   │       │
    │   │       ├── application/
    │   │       │   ├── dto/
    │   │       │   │   ├── request/
    │   │       │   │   │   ├── CreateTransactionRequest.java
    │   │       │   │   │   └── SignerCallbackRequest.java
    │   │       │   │   ├── response/
    │   │       │   │   │   └── TransactionStatusResponse.java
    │   │       │   │   └── event/
    │   │       │   │       ├── OutboundTransactionEvent.java
    │   │       │   │       └── ConfirmationEventPayload.java
    │   │       │   │
    │   │       │   ├── mapper/ 
    │   │       │   │   ├── TransactionMapper.java
    │   │       │   │   ├── SignerMapper.java
    │   │       │   │   └── EventMapper.java
    │   │       │   │
    │   │       │   ├── usecase/
    │   │       │   │   ├── CreateTransactionUseCase.java
    │   │       │   │   ├── HandleSignatureCallbackUseCase.java
    │   │       │   │   ├── BroadcastTransactionUseCase.java
    │   │       │   │   ├── WaitForConfirmationUseCase.java
    │   │       │   │   └── CompleteFlowUseCase.java
    │   │       │   │
    │   │       │   ├── statemachine/
    │   │       │   │   ├── config/
    │   │       │   │   │   └── BlockchainStateMachineConfig.java
    │   │       │   │   ├── state/
    │   │       │   │   │   ├── BlockchainStates.java
    │   │       │   │   │   └── BlockchainEvents.java
    │   │       │   │   └── handler/
    │   │       │   │       ├── StartupStateHandler.java
    │   │       │   │       └── StateTransitionListener.java
    │   │       │   │
    │   │       │   └── orchestrator/
    │   │       │           └── TransactionOrchestrator.java
    │   │       │
    │   │       ├── infrastructure/
    │   │       │   ├── config/
    │   │       │   │   ├── KafkaConfig.java
    │   │       │   │   ├── MongoConfig.java
    │   │       │   │   ├── Web3jConfig.java
    │   │       │   │   ├── WebClientConfig.java
    │   │       │   │   └── StateMachineConfig.java
    │   │       │   │
    │   │       │   ├── adapter/
    │   │       │   │   ├── inbound/
    │   │       │   │   │   ├── api/
    │   │       │   │   │   │   └── TransactionController.java
    │   │       │   │   │   └── kafka/
    │   │       │   │   │       └── TransactionEventConsumer.java
    │   │       │   │   └── outbound/
    │   │       │   │       ├── kafka/
    │   │       │   │       │   └── ConfirmationEventProducer.java
    │   │       │   │       ├── rest/
    │   │       │   │       │   ├── SignerRestClient.java
    │   │       │   │       │   └── ExternalApiClient.java
    │   │       │   │       ├── blockchain/
    │   │       │   │       │   ├── Web3jBlockchainClient.java
    │   │       │   │       │   ├── GasEstimatorWeb3j.java
    │   │       │   │       │   └── NonceManagerWeb3j.java
    │   │       │   │       └── repository/
    │   │       │   │           └── MongoTransactionRepository.java
    │   │       │   │
    │   │       │   └── security/
    │   │       │       └── HttpSecurityConfig.java
    │   │       │
    │   │       └── shared/
    │   │           ├── annotation/
    │   │           │   └── DomainService.java
    │   │           ├── util/
    │   │           │   ├── JsonUtils.java
    │   │           │   └── RetryUtils.java
    │   │           └── events/
    │   │               └── KafkaHeadersConstants.java
    │   │
    │   └── resources/
    │       ├── application.yml
    │       ├── state-machine/
    │       │   └── blockchain-state-machine.sm
    │       └── mappings/
    │           └── transaction-mapper.xml
    │
    └── test/
        └── java/
            └── com/example/blockchainconnector/
                ├── domain/
                ├── application/
                ├── infrastructure/
                └── integration/

```

Esta é uma arquitetura extremamente robusta e alinhada com os padrões modernos de sistemas distribuídos de alta complexidade. A combinação de DDD, Arquitetura Hexagonal (Ports and Adapters) e Máquina de Estados é ideal para lidar com a natureza assíncrona e imprevisível de transações em Blockchain.

Abaixo, apresento a estrutura de diretórios sugerida e, na sequência, o Documento de Arquitetura Técnica detalhado.

-----

### Parte 1: Estrutura de Diretórios (Java 21 / Spring Boot)

A estrutura reflete a separação clara entre **Domain** (Regra de negócio), **Application** (Casos de uso/Orquestração) e **Infrastructure** (Tecnologias/Frameworks).

```shell
src/
└── main/
    ├── java/
    │   └── com/
    │       └── tech/
    │           └── blockchainconnector/
    │               ├── BlockchainConnectorApplication.java
    │               │
    │               ├── domain/                         # O NÚCLEO (Sem dependências de Frameworks pesados)
    │               │   ├── model/                      # Entidades e Value Objects
    │               │   │   ├── Transaction.java        # Aggregate Root (State, hash, payload)
    │               │   │   ├── TransactionId.java
    │               │   │   ├── NetworkConfig.java      # Configuração da Chain (RPC, ChainId)
    │               │   │   └── GasInfo.java
    │               │   ├── event/                      # Eventos de Domínio (Internos)
    │               │   │   ├── TransactionSignedEvent.java
    │               │   │   └── TransactionConfirmedEvent.java
    │               │   ├── exception/                  # Exceções de Domínio
    │               │   │   └── InsufficientFundsException.java
    │               │   └── port/                       # Portas (Interfaces)
    │               │       ├── inbound/                # Portas de Entrada (Use Cases interfaces)
    │               │       │   └── ProcessTransactionUseCase.java
    │               │       └── outbound/               # Portas de Saída (Interfaces para Infra)
    │               │           ├── TransactionRepository.java
    │               │           ├── BlockchainClientPort.java  # Web3j Abstraction
    │               │           ├── SignerClientPort.java      # Hybrid (Kafka/Rest)
    │               │           └── EventPublisherPort.java    # Publicação no Kafka (Notification)
    │               │
    │               ├── application/                    # CAMADA DE APLICAÇÃO
    │               │   ├── dto/                        # DTOs de Entrada/Saída
    │               │   │   ├── TransactionRequestDTO.java
    │               │   │   └── TransactionResponseDTO.java
    │               │   ├── mapper/                     # MapStruct Interfaces
    │               │   │   └── TransactionMapper.java
    │               │   └── service/                    # Implementação dos Use Cases
    │               │       ├── TransactionOrchestrator.java
    │               │       └── RecoveryService.java    # Lógica de reprocessamento em restart
    │               │
    │               └── infrastructure/                 # ADAPTADORES (Tecnologia real)
    │                   ├── config/                     # Configurações do Spring
    │                   │   ├── KafkaConfig.java
    │                   │   ├── Web3jConfig.java
    │                   │   ├── OpenTelemetryConfig.java
    │                   │   ├── MongoConfig.java
    │                   │   └── StateMachineConfig.java # Definição dos Estados e Transições
    │                   │
    │                   ├── adapter/
    │                   │   ├── inbound/                # Quem chama a aplicação
    │                   │   │   ├── kafka/              # Listeners do Kafka
    │                   │   │   │   └── TransactionRequestConsumer.java
    │                   │   │   └── scheduler/          # Jobs recorrentes
    │                   │   │       └── StuckTransactionRecoveryJob.java
    │                   │   │
    │                   │   └── outbound/               # Quem a aplicação chama
    │                   │       ├── persistence/        # MongoDB
    │                   │       │   ├── MongoTransactionRepository.java # Impl do Port
    │                   │       │   └── entity/         # Entidades JPA/Mongo (separadas do Domain)
    │                   │       │       └── TransactionDocument.java
    │                   │       ├── blockchain/         # Web3j
    │                   │       │   ├── Web3jClientAdapter.java
    │                   │       │   └── NonceManager.java
    │                   │       ├── signer/             # Comunicação com Signer
    │                   │       │   ├── HybridSignerAdapter.java # Lógica Kafka c/ fallback REST
    │                   │       │   └── rest/           # WebClient impl
    │                   │       └── messaging/          # Kafka Producer
    │                   │           └── KafkaEventPublisher.java
    │                   │
    │                   └── statemachine/               # Lógica específica do Spring State Machine
    │                       ├── TransactionStateMachineInterceptor.java
    │                       ├── actions/                # Ações disparadas nas trocas de estado
    │                       │   ├── EstimateGasAction.java
    │                       │   ├── RequestSignatureAction.java
    │                       │   └── BroadcastToChainAction.java
    │                       └── guards/                 # Validações antes das transições
    │                           └── BalanceGuard.java
    │
    └── resources/
        ├── application.yml
        ├── application-prod.yml
        └── statemachine-diagram.puml # Documentação visual da SM
```

-----

### Parte 2: Documento de Arquitetura Técnica

#### 1\. Visão Geral

O **Blockchain Connector** atua como um gateway resiliente entre aplicações internas e múltiplas redes Blockchain (EVM-compatible). Ele abstrai a complexidade de *nonce management*, estimativa de gas, assinatura segura e monitoramento de confirmações de bloco.

#### 2\. Tecnologias Core

  * **Runtime:** Java 21 (Virtual Threads habilitadas para I/O intensivo).
  * **Framework:** Spring Boot 3.x.
  * **Blockchain Lib:** Web3j.
  * **Storage:** MongoDB (Armazenamento de estado das transações e payload).
  * **Messaging:** AWS MSK (Kafka) para ingestão assíncrona e comunicação entre microsserviços.
  * **StateMachine:** Spring State Machine para orquestrar o ciclo de vida complexo.
  * **Resilience:** Resilience4j (Retry, Circuit Breaker).
  * **Observability:** OpenTelemetry (Traces) + Micrometer (Metrics).

#### 3\. Ciclo de Vida da Transação (Máquina de Estados)

A máquina de estados é o coração do sistema, garantindo consistência.

**Estados Propostos:**

1.  `RECEIVED`: Evento consumido do Kafka, persistido no Mongo.
2.  `PREPARING`: Calculando Gas Limit, Gas Price e obtendo o próximo Nonce.
3.  `SIGNING_REQUESTED`: Payload enviado para o componente Signer (via Kafka).
4.  `SIGNING_FALLBACK`: (Estado Intermediário) Timeout no Kafka, tentando via REST.
5.  `SIGNED`: Transação assinada (raw hex) recebida e salva.
6.  `SUBMITTED`: Enviada para a blockchain (txHash gerado).
7.  `CONFIRMING`: Aguardando N confirmações de blocos.
8.  `COMPLETED`: Sucesso confirmado. Evento final disparado.
9.  `FAILED`: Falha irreversível (Ex: Revert na EVM ou Gas insuficiente após retries).

#### 4\. Detalhamento dos Componentes Chave

##### 4.1. Gerenciamento de Identidade e Idempotência

  * **Idempotency Key:** O `correlationId` recebido na mensagem original será a chave de idempotência.
  * **Implementação:** Antes de processar `RECEIVED`, consulta-se o MongoDB. Se o ID já existir, ignora o processamento ou retorna o status atual (caso seja uma re-solicitação síncrona).

##### 4.2. Estratégia de Nonce (Crítico)

Para suportar múltiplas redes e concorrência:

  * O `NonceManager` deve usar *Optimistic Locking* no MongoDB ou `findAndModify` atômico na coleção de controle de contas (`AccountNonces`).
  * **Nota:** O conector deve gerenciar uma fila interna (virtual) por *Sender Address* para garantir que transações da mesma carteira saiam em ordem sequencial de Nonce.

##### 4.3. Comunicação Híbrida com Signer (Sua Requisito Específico)

Este é um padrão complexo. A implementação sugerida no `HybridSignerAdapter`:

1.  **Caminho Feliz:** Envia mensagem para o tópico `signer-request-topic`.
2.  **Wait State:** A Máquina de Estado entra em `SIGNING_REQUESTED`.
3.  **Timeout/Fallback:**
      * Utiliza-se o recurso de *State Timer* do Spring State Machine ou um Scheduler externo.
      * Se após X segundos o evento `TransactionSigned` não for consumido, dispara uma *Action* de transição para tentar via REST.
      * **Resilience4j:** O WebClient do Signer deve estar envolto em um `CircuitBreaker` e `TimeLimiter`.

##### 4.4. Recuperação e Restart (Reconciliation)

No startup da aplicação (`CommandLineRunner`):

1.  Busca no MongoDB todas as transações em estados não terminais (`!= COMPLETED` e `!= FAILED`).
2.  **Lógica de Re-hidratação:**
      * Se `SUBMITTED`: Consulta a Blockchain (Web3j `ethGetTransactionReceipt`). Se confirmada, avança estado. Se sumiu do mempool, re-submete.
      * Se `RECEIVED/PREPARING`: Reinjeta na máquina de estados para processar.

-----

### Parte 3: Respostas e Recomendações Técnicas

#### 1\. Múltiplas Redes Blockchain

**Abordagem:** Utilize um padrão *Factory* para o `Web3j`.

  * Crie uma configuração `Map<String, Web3j> clients`, onde a chave é o `chainId` ou alias da rede (ex: "eth-mainnet", "polygon-mumbai").
  * No adapter, selecione o cliente correto baseado no campo `network` da mensagem de entrada.

#### 2\. Estratégia de Retry (Resilience4j)

**Recomendação:** **Exponential Backoff com Jitter.**

  * **Por que não Fixed?** Blockchains sofrem congestionamentos repentinos. Tentar a cada 2s fixos vai falhar repetidamente e custar recursos. O exponencial permite que a rede se recupere.
  * **Configuração Sugerida:**
      * RPC Calls (Network Errors): `waitDuration: 500ms`, `maxAttempts: 5`, `multiplier: 2`.
      * Confirmação de Transação (Mining): Polling com backoff. Começa checando a cada 2s, depois 5s, 10s...

#### 3\. Observabilidade e Fallback

  * **OTEL:** Injete o `traceId` e `spanId` nos headers do Kafka e nas chamadas Web3j (se suportado pelo RPC provider) e WebClient.
  * **Métricas como Insumo:**
      * Crie um *Gauge* métrico para "Gas Price Médio" e "Tempo de Mineração".
      * **Fallback Inteligente:** Se o tempo médio de mineração subir drasticamente (congestionamento), o sistema pode automaticamente aumentar o *Gas Price* sugerido para novas transações (Gas Escalation Strategy) ou pausar ingestão (Bulkhead).

#### 4\. Transações Pendentes e Restart

A estratégia de "Reconciliation" descrita na seção 4.4 é mandatória.

  * **Ponto de Atenção:** Cuidado com o "Zombie Transaction". Se o sistema cair logo após enviar via Web3j mas antes de salvar o estado `SUBMITTED`, ao reiniciar, ele pode tentar enviar de novo.
  * **Solução:** O Web3j lança erro se tentar enviar transação com Nonce já utilizado (conhecido). O sistema deve capturar esse erro específico ("Nonce too low" ou "Transaction already known") e, em vez de falhar, consultar a blockchain pelo hash calculado para verificar se já existe.

#### 5\. Idempotência e Correlação

Está correto. O `MapStruct` deve mapear o `correlationId` da mensagem Kafka para o `id` ou um campo indexado único no MongoDB (`@Indexed(unique = true)`).

### [Diagrama Visual Sugerido - State Machine]

*Nota para visualização mental:*
`(Start)` -\> `RECEIVED` -\> `PREPARING` -\> `SIGNING_REQUESTED` --(timeout)--\> `SIGNING_FALLBACK` -\> `SIGNED` -\> `SUBMITTED` --(loop check)--\> `CONFIRMING` -\> `COMPLETED`.
(Qualquer erro recuperável leva a `RETRYING` que volta para o estado anterior).

### Próximo passo que posso fazer por você

Posso gerar o código Java específico para a configuração da **Spring State Machine** com a lógica de **Timeout Action** para alternar entre Kafka e REST no componente Signer, que é a parte mais complexa da orquestração?