# Arquitetura Detalhada - Blockchain Connector

## Visão Geral da Arquitetura

### Arquitetura Hexagonal (Ports & Adapters)

```
                    ┌─────────────────────────────────────┐
                    │                                     │
     KAFKA     ────▶│           Inbound Adapters          │
     REST      ────▶│       (Infrastructure Layer)        │
                    │                                     │
                    └───────────────────┬─────────────────┘
                                        │
                                        ▼
                    ┌─────────────────────────────────────┐
                    │                                     │
                    │          Application Layer          │
                    │     (Use Cases, Services, DTOs)     │
                    │                                     │
                    └───────────────────┬─────────────────┘
                                        │
                                        ▼
                    ┌─────────────────────────────────────┐
                    │          ┌──────────────┐           │
                    │          │    Domain    │           │
                    │          │    Model     │           │
                    │          │  (Entities)  │           │
                    │          └──────────────┘           │
                    │                                     │
                    │  Ports (Interfaces)                 │
                    │  - Inbound: Use Cases               │
                    │  - Outbound: Repositories, Gateways │
                    │                                     │
                    └──────────────────┬──────────────────┘
                                       │
                                       ▼
                    ┌─────────────────────────────────────┐
                    │                                     │
                    │          Outbound Adapters          │
                    │        (Infrastructure Layer)       │
                    │                                     │
                    └──────────────────┬──────────────────┘
                                       │
                    ┌────────┬─────────┴─────────┬────────┐
                    │        │                   │        │
                    ▼        ▼                   ▼        ▼
                 MongoDB   Web3j               Kafka    Redis
```

## Diagrama de Sequência - Fluxo Completo

```
┌──────┐    ┌──────────┐    ┌─────────────┐    ┌──────────┐   ┌──────────┐    ┌───────────┐
│Client│    │  Kafka   │    │ Transaction │    │  Signer  │   │Blockchain│    │  MongoDB  │
│      │    │ Consumer │    │ Use Case/SM │    │ Service  │   │  (Web3j) │    │    opens  │
└──┬───┘    └────┬─────┘    └──────┬──────┘    └────┬─────┘   └────┬─────┘    └─────┬─────┘
   │             │                 │                │              │                │
   │ 1. Publish Transaction        │                │              │                │
   ├────────────▶│                 │                │              │                │
   │             │                 │                │              │                │
   │             │ 2. Consume      │                │              │                │
   │             ├────────────────▶│                │              │                │
   │             │                 │                │              │                │
   │             │            3. Create Transaction │              │                │
   │             │                 ├───────────────────────────────┬───────────────▶│
   │             │                 │                │              │                │
   │             │            4. Validate           │              │                │
   │             │                 ├─┐              │              │                │
   │             │                 │ │              │              │                │
   │             │                 │◀┘              │              │                │
   │             │                 │                │              │                │
   │             │            5. Calculate Gas      │              │                │
   │             │                 ├───────────────────────────────┬───────────────▶│
   │             │                 │                │         RPC  │                │
   │             │                 │◀──────────────────────────────┤                │
   │             │                 │                │              │                │
   │             │            6. Request Signature (Kafka)         │                │
   │             ├────────────────▶│                │              │                │
   │             │                 ├───────────────▶│              │                │
   │             │                 │                │              │                │
   │             │                 │  7. Sign       │              │                │
   │             │                 │                ├─┐            │                │
   │             │                 │                │ │            │                │
   │             │                 │                │◀┘            │                │
   │             │                 │                │              │                │
   │             │            8. Signature Response (Kafka)        │                │
   │             │◀────────────────┤                │              │                │
   │             ├────────────────▶│◀───────────────┤              │                │
   │             │                 │                │              │                │
   │             │  [IF TIMEOUT: Fallback to REST]  │              │                │
   │             │                 ├───────────────▶│              │                │
   │             │                 │  REST API      │              │                │
   │             │                 │◀───────────────┤              │                │
   │             │                 │                │              │                │
   │             │            9. Submit to Blockchain              │                │
   │             │                 ├───────────────────────────────────────────────▶│
   │             │                 │                │        sendRawTransaction     │
   │             │                 │◀───────────────────────────────────────────────┤
   │             │                 │    (TxHash)    │              │                │
   │             │                 │                │              │                │
   │             │           10. Save State         │              │                │
   │             │                 ├───────────────────────────────────────────────▶│
   │             │                 │                │              │                │
   │             │           11. Monitor            │              │                │
   │             │                 ├─┐              │              │                │
   │             │                 │ │ Poll         │              │                │
   │             │                 │ └─────────────────────────────┬───────────────▶│
   │             │                 │                │    getTransactionReceipt      │
   │             │                 │◀──────────────────────────────┤                │
   │             │                 │                │              │                │
   │             │           12. Confirmed          │              │                │
   │             │                 ├───────────────────────────────────────────────▶│
   │             │                 │                │              │                │
   │             │           13. Publish Event      │              │                │
   │             │◀────────────────┤                │              │                │
   │             │  Transaction    │                │              │                │
   │             │  Confirmed      │                │              │                │
   │             │                 │                │              │                │
```

## Domain-Driven Design (DDD)

### Bounded Context

```
┌───────────────────────────────────────────────────────────┐
│           Transaction Management Context                  │
│                                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │              Transaction Aggregate                  │  │
│  │                                                     │  │
│  │  ┌─────────────┐     ┌──────────────┐               │  │
│  │  │ Transaction │────▶│     Nonce    │               │  │
│  │  │  (Root)     │     └──────────────┘               │  │
│  │  │             │                                    │  │
│  │  │  - id       │     ┌──────────────┐               │  │
│  │  │  - status   │────▶│  GasConfig   │               │  │
│  │  │  - from     │     └──────────────┘               │  │
│  │  │  - to       │                                    │  │
│  │  │  - value    │     ┌──────────────┐               │  │
│  │  │             │────▶│   Receipt    │               │  │
│  │  └─────────────┘     └──────────────┘               │  │
│  │                                                     │  │
│  │  Domain Services:                                   │  │
│  │  - TransactionValidator                             │  │
│  │  - GasCalculator                                    │  │
│  │  - NonceManager                                     │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                           │
│  Domain Events:                                           │
│  - TransactionReceivedEvent                               │
│  - TransactionValidatedEvent                              │
│  - GasCalculatedEvent                                     │
│  - SignatureRequestedEvent                                │
│  - TransactionSignedEvent                                 │
│  - TransactionSubmittedEvent                              │
│  - TransactionConfirmedEvent                              │
│  - TransactionFailedEvent                                 │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

### Aggregate Rules (Invariantes)

```java
class Transaction {
    // Invariantes que SEMPRE devem ser verdadeiras:
    
    // 1. Value não pode ser negativo
    if (value.compareTo(BigDecimal.ZERO) < 0) {
        throw new InvalidTransactionException("Negative value");
    }
    
    // 2. From e To devem ser diferentes
    if (from.equals(to)) {
        throw new InvalidTransactionException("Same sender and recipient");
    }
    
    // 3. Não pode ser confirmada sem hash
    if (status == CONFIRMED && hash == null) {
        throw new InvalidTransactionException("No hash for confirmed tx");
    }
    
    // 4. Retry count não pode exceder max
    if (currentRetryCount > maxRetries) {
        throw new MaxRetryExceededException("Max retries exceeded");
    }
    
    // 5. State transitions válidas
    if (!isValidTransition(currentState, newState)) {
        throw new InvalidTransactionException("Invalid state transition");
    }
}
```

## State Machine Detalhada

### Estados e Transições

```
┌─────────────────────────────────────────────────────────────┐
│                     State Machine                           │
└─────────────────────────────────────────────────────────────┘

            ┌──────────┐
            │ PENDING  │ (Initial State)
            └────┬─────┘
                 │ VALIDATE
                 ▼
            ┌──────────┐
            │VALIDATED │
            └────┬─────┘
                 │ CALCULATE_GAS
                 ▼
         ┌──────────────────┐
         │ CALCULATING_GAS  │
         └────┬─────────────┘
              │ GAS_CALCULATED
              ▼
      ┌─────────────────────────┐
      │ REQUESTING_SIGNATURE    │
      └──┬────────────────┬─────┘
         │                │
         │ SUCCESS        │ TIMEOUT
         │                ▼
         │         ┌──────────────────┐
         │         │SIGNATURE_TIMEOUT │
         │         └────┬─────────────┘
         │              │ RETRY_VIA_REST
         │              └──────┐
         │ SIGNATURE_RECEIVED  │
         ▼                     ▼
    ┌────────┐         ┌──────────────────┐
    │ SIGNED │◀────────│ (Retry Loop)     │
    └───┬────┘         └──────────────────┘
        │ SUBMIT
        ▼
    ┌─────────────┐
    │ SUBMITTING  │
    └───┬─────────┘
        │ SUBMITTED
        ▼
    ┌─────────────┐
    │ MONITORING  │◀─────┐
    └───┬─────────┘      │
        │ CONFIRMED      │ ERROR + CAN_RETRY
        ▼                │
    ┌─────────────┐      │
    │ CONFIRMED   │      │
    └─────────────┘      │
                         │
         ┌───────────────┘
         │
         ▼
    ┌───────────────┐
    │ PENDING_RETRY │
    └───┬───────────┘
        │ RETRY (if possible)
        │
        ▼
    ┌────────┐        ┌──────────┐
    │ FAILED │   or   │CANCELLED │
    └────────┘        └──────────┘
    (Terminal)        (Terminal)
```

### Actions (Ações executadas em transições)

| Action | Descrição | Estado Origem | Estado Destino |
|--------|-----------|---------------|----------------|
| ValidateTransactionAction | Valida regras de negócio | PENDING | VALIDATED |
| CalculateGasAction | Calcula gas e nonce | VALIDATED | CALCULATING_GAS |
| RequestSignatureAction | Solicita assinatura | CALCULATING_GAS | REQUESTING_SIGNATURE |
| SubmitToBlockchainAction | Envia para blockchain | SIGNED | SUBMITTING |
| MonitorConfirmationAction | Monitora confirmação | SUBMITTING | MONITORING |
| HandleErrorAction | Trata erros | Qualquer | FAILED/RETRY |
| PublishCompletionAction | Publica evento final | Terminal | - |

### Guards (Condições para transições)

| Guard | Descrição | Condição |
|-------|-----------|----------|
| CanRetryGuard | Pode tentar novamente? | retryCount < maxRetries |
| MaxRetryGuard | Atingiu max retries? | retryCount >= maxRetries |
| SignatureTimeoutGuard | Timeout na assinatura? | tempo > thresholdTimeout |

## Padrões de Design Utilizados

### 1. Hexagonal Architecture (Ports & Adapters)

**Objetivo**: Isolar lógica de negócio de detalhes técnicos

```java
// Port (Interface no Domain)
public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(TransactionId id);
}

// Adapter (Implementação na Infrastructure)
@Repository
public class TransactionRepositoryImpl implements TransactionRepository {
    private final TransactionMongoRepository mongoRepo;
    
    @Override
    public Transaction save(Transaction transaction) {
        // Adapta para MongoDB
    }
}
```

### 2. Domain-Driven Design

**Objetivo**: Modelar software baseado no domínio de negócio

```java
// Aggregate Root
public class Transaction {
    // Encapsula lógica de negócio
    // Garante invariantes
    // Publica domain events
}

// Value Object
public class WalletAddress {
    private final String address;
    // Imutável, sem identidade
}
```

### 3. State Machine Pattern

**Objetivo**: Gerenciar estados complexos de forma explícita

```java
@Configuration
public class TransactionStateMachineConfig {
    // Define estados, eventos, transições, actions e guards
}
```

### 4. Strategy Pattern (Signer)

**Objetivo**: Alternar entre estratégias de assinatura

```java
public interface SignerStrategy {
    SignatureResponse sign(SignatureRequest request);
}

@Component
public class KafkaSignerStrategy implements SignerStrategy {
    // Primary: via Kafka
}

@Component
public class RestSignerStrategy implements SignerStrategy {
    // Fallback: via REST
}
```

### 5. Outbox Pattern

**Objetivo**: Garantir consistência entre DB e mensagens

```java
@Transactional
public void processTransaction(Transaction tx) {
    // 1. Salva transação
    transactionRepository.save(tx);
    
    // 2. Salva evento na outbox
    outboxRepository.save(
        OutboxEvent.of(tx.getDomainEvent())
    );
    
    // 3. Scheduler processa outbox e publica no Kafka
}
```

### 6. Circuit Breaker

**Objetivo**: Prevenir falhas em cascata

```java
@CircuitBreaker(name = "signerService")
@Retry(name = "signerService")
public SignatureResponse requestSignature(SignatureRequest request) {
    return signerClient.sign(request);
}
```

## Fluxo de Dados

### Write Path (Escrita)

```
[Kafka Topic: transaction.received]
           │
           ▼
[TransactionConsumer]
           │
           ▼
[SubmitTransactionUseCase]
           │
           ├──▶ [Domain: Transaction.create()]
           │
           ├──▶ [Domain: Transaction.validate()]
           │
           ├──▶ [Repository: save()]
           │              │
           │              ▼
           │         [MongoDB]
           │
           ├──▶ [EventPublisher: publish()]
           │              │
           │              ▼
           │    [Kafka Topic: transaction.validated]
           │
           └──▶ [StateMachine: start()]
```

### Read Path (Leitura)

```
[REST API: GET /transactions/{id}]
           │
           ▼
[TransactionController]
           │
           ▼
[CheckTransactionStatusUseCase]
           │
           ├──▶ [Cache: check]
           │         │
           │         ▼ (miss)
           │
           ├──▶ [Repository: findById()]
           │              │
           │              ▼
           │         [MongoDB]
           │              │
           │              ▼
           └──▶ [Cache: put]
                      │
                      ▼
              [Return Response]
```

## Resiliência

### Circuit Breaker Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      signerService:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 10
      
      blockchainService:
        failure-rate-threshold: 40
        wait-duration-in-open-state: 30s
```

### Retry Configuration

```yaml
resilience4j:
  retry:
    instances:
      signerService:
        max-attempts: 2
        wait-duration: 1s
      
      blockchainService:
        max-attempts: 5
        wait-duration: 2s
        exponential-backoff-multiplier: 2
```

### Rate Limiting

```yaml
resilience4j:
  ratelimiter:
    instances:
      blockchainRpc:
        limit-for-period: 50
        limit-refresh-period: 1s
```

## Observability

### Métricas Customizadas

```java
@Component
public class TransactionMetrics {
    
    private final Counter submittedCounter;
    private final Counter confirmedCounter;
    private final Timer processingTimer;
    private final Gauge pendingGauge;
    
    public void recordSubmitted(String network) {
        submittedCounter.increment();
    }
    
    public void recordConfirmed(String network, Duration duration) {
        confirmedCounter.increment();
        processingTimer.record(duration);
    }
}
```

### Tracing (OpenTelemetry)

```java
@WithSpan
public Transaction submitTransaction(SubmitTransactionRequest request) {
    Span span = Span.current();
    span.setAttribute("transaction.network", request.getNetwork());
    span.setAttribute("transaction.value", request.getValue().toString());
    
    // ... logic
}
```

### Structured Logging

```java
log.info("Transaction state changed: " +
    "transactionId={}, " +
    "oldState={}, " +
    "newState={}, " +
    "network={}, " +
    "retryCount={}",
    transaction.getId(),
    oldState,
    newState,
    transaction.getNetwork(),
    transaction.getCurrentRetryCount());
```

## Segurança

### API Security

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
        
        return http.build();
    }
}
```

### Secrets Management

- **Desenvolvimento**: Variáveis de ambiente
- **Produção**: AWS Secrets Manager / Kubernetes Secrets

## Performance

### Otimizações

1. **Caching**: Redis para nonces e gas prices
2. **Connection Pooling**: MongoDB e HTTP connections
3. **Async Processing**: Virtual threads Java 21
4. **Batch Processing**: Processamento em lote de eventos
5. **Índices MongoDB**: Otimizados para queries frequentes

### Índices MongoDB

```javascript
// transactions collection
db.transactions.createIndex({ "id": 1 }, { unique: true });
db.transactions.createIndex({ "status": 1, "updatedAt": -1 });
db.transactions.createIndex({ "hash": 1 }, { unique: true, sparse: true });
db.transactions.createIndex({ "idempotencyKey": 1 }, { unique: true });
db.transactions.createIndex({ "correlationId": 1 });
db.transactions.createIndex({ "createdAt": -1 });
```

## Decisões Arquiteturais (ADRs)

Veja a pasta `docs/architecture/ADR/` para Architecture Decision Records detalhados.

## Referências

- **Hexagonal Architecture**: Alistair Cockburn
- **DDD**: Eric Evans - "Domain-Driven Design"
- **State Machine**: Martin Fowler - "State Machine Pattern"
- **Microservices Patterns**: Chris Richardson

---

**Documento mantido por**: Equipe de Arquitetura
**Última atualização**: 2024-12