# Instruções do GitHub Copilot — Wallet Hub

Estas instruções orientam o GitHub Copilot para gerar código alinhado às práticas, arquitetura e stack reais deste repositório.

## Contexto do Projeto

- Tipo: Backend Web API e microsserviço orientado a eventos (event-driven)
- Linguagem/Toolchain: Java 24 (maven.compiler.release 24) com Maven Wrapper
- Frameworks principais: Spring Boot 3.5.6, Spring Cloud 2025.x
- Paradigmas: Clean Architecture (portas e adaptadores), reativo (WebFlux) e bloqueante (Kafka) combinados
- Mensageria/Eventos: Spring Cloud Stream com Kafka (binders Kafka/Kafka Streams) e CloudEvents
- Persistência: JPA (H2/PostgreSQL), R2DBC (H2/Postgres), Redis (reativo), MongoDB (reativo)
- Orquestração de Saga: Spring Statemachine (com persistência JPA)
- Observabilidade: Actuator, Micrometer (Prometheus/OTLP), Tracing (OpenTelemetry), Micrometer Tracing
- Maquina de Estados: Spring Statemachine para coordenação de transações distribuídas
- Resiliência: Resilience4j (Circuit Breaker)
- Configuração externa/Segurança: Spring Cloud Config, Spring Security + OAuth2 Client, Vault (opcional)
- AI/Extras: Spring AI starters (presentes no POM, uso opcional)

## Estrutura de Diretórios (real)

```text
src/
  main/
    java/
      dev/bloco/wallet/hub/
        WalletHubApplication.java
        config/
          DinamoLibraryLoader.java
          UseCaseConfig.java
        domain/
          event/ (ex.: FundsAddedEvent, FundsWithdrawnEvent, ...)
          gateway/ (ex.: WalletRepository, TransactionRepository, DomainEventPublisher)
          (entidades de domínio: Wallet, User, Transaction)
        usecase/ (ex.: CreateWalletUseCase, AddFundsUseCase, ...)
        infra/
          adapter/
            event/
              producer/ (KafkaEventProducer, EventProducer)
              consumer/ (WalletCreatedEventConsumer, FundsAddedEventConsumer, ...)
            (OutboxEventPublisher)
          provider/
            data/
              config/ (StandardSagaStateMachineConfig, SagaStates, SagaEvents)
              entity/ (WalletEntity, UserEntity, TransactionEntity)
              repository/ (Spring Data + JPA impl + OutboxRepository)
            mapper/ (MapStruct mappers)
          util/ (CloudEventUtils)
    resources/
      application.yml (bindings de stream e DB H2 por padrão)
  test/
    java/
      dev/bloco/wallet/hub/
        infra/... (tests de produtores/consumidores e util)
```

## Dependências-chave (pom.xml)

- Spring Boot: actuator, webflux, websocket, security, oauth2-client, data-jpa, data-r2dbc, cache, hateoas, quartz, docker-compose
- Spring Cloud: config-server, starter-config, starter, stream, stream-binder-kafka/kafka-streams
- Kafka: spring-kafka, kafka-streams
- CloudEvents: io.cloudevents:cloudevents-spring
- State Machine: spring-statemachine-starter, spring-statemachine-data-jpa
- Persistência: H2, postgresql (JDBC), r2dbc-h2, r2dbc-postgresql
- Reactive data: spring-boot-starter-data-redis-reactive, spring-boot-starter-data-mongodb-reactive
- Resiliência: spring-cloud-starter-circuitbreaker-reactor-resilience4j
- Observabilidade: micrometer-tracing-bridge-brave, micrometer-registry-prometheus, micrometer-registry-otlp
- Config/Segurança: spring-cloud-starter-vault-config, spring-vault-core
- Build/Mapping: mapstruct + mapstruct-processor, hibernate-enhance-maven-plugin
- Testes: spring-boot-starter-test, reactor-test, spring-kafka-test, spring-cloud-stream-test-binder, testcontainers

## Arquitetura e Padrões
- Spring Boot 3.5.6
  - WebFlux
  - Reactive data (Redis, MongoDB)
  - Reactive streams (Kafka)
  - Reactive programming (Reactor)
  - Reactive security (OAuth2)
  - Reactive config (Vault)
  - Reactive tracing (Brave)
  - Reactive observability (Micrometer)
  - Reactive AI (Spring AI)
- Clean Architecture (portas e adaptadores)
  - domain: entidades, eventos, contratos (gateways)
  - usecase: orquestra regras de negócio (interactors)
  - infra/adapter: integrações externas (Kafka producers/consumers, publishers)
  - infra/provider: persistência (JPA, repositórios), mappers, configuração de saga
- Microservices patterns aplicados
  - Event-driven com Kafka + CloudEvents
  - Outbox pattern (OutboxEvent, OutboxService, OutboxWorker) para publicação assíncrona e confiável
  - Saga/State Machine para coordenação de transações distribuídas
  - Circuit Breaker (Resilience4j) quando integrar com serviços remotos
  - Configuração centralizada (Spring Cloud Config)
- Reativo vs Bloqueante
  - WebFlux, Redis/Mongo são reativos; JPA e Kafka (template) são bloqueantes
  - Não execute chamadas bloqueantes em threads event-loop. Use boundedElastic quando necessário, ou separe camadas/hand-offs

## Convenções de Código (Clean Code + SOLID)
- DI por construtor; evite injeção em campo
- Entidades e casos de uso coesos (Single Responsibility). Dependências por abstrações (Dependency Inversion via gateways)
- Domínio e eventos preferencialmente imutáveis; use records onde fizer sentido
- Não exponha entidades JPA; use DTOs quando necessário e MapStruct para mapeamento
- Valide entradas (Bean Validation) e trate exceções com @ControllerAdvice quando expor APIs
- Logging com SLF4J e mensagens parametrizadas
- Não hardcode; use application.yml, @ConfigurationProperties ou @Value
- Siga formatação consistente (google-java-format/IDE)

## Mensageria e CloudEvents
- Use Spring Cloud Stream com bindings funcionais (Supplier/Function/Consumer)
- Padrão de bindings atual (application.yml):
  - walletCreatedEventProducer-out-0 -> wallet-created-topic
  - fundsAddedEventProducer-out-0   -> funds-added-topic
  - fundsWithdrawnEventProducer-out-0 -> funds-withdrawn-topic
  - fundsTransferredEventProducer-out-0 -> funds-transferred-topic
- Utilize CloudEventUtils para preencher cabeçalhos CloudEvent
- Consumidores devem ser idempotentes; produtores devem publicar via Outbox quando a mudança for transacional

## Outbox & Saga
- Outbox pattern: persista eventos no outbox dentro da mesma transação do domínio; publique assíncrono (OutboxWorker) via KafkaEventProducer
- Garanta idempotência no consumidor e no publicador (chaves de agregação, deduplicação)
- State Machine (Spring Statemachine): estados/eventos em enums; persistência JPA habilitada; utilize StandardSagaStateMachineConfig

## Testes
- Preferir testes unitários puros para domínio e utilitários
- Mensageria: usar spring-cloud-stream-test-binder (sem Kafka real)
  - Defina spring.cloud.stream.defaultBinder=test em propriedades de teste
  - Use InputDestination/OutputDestination para enviar/receber mensagens
- Persistência: @DataJpaTest para slices JPA (H2 in-memory)
- Aplicação completa: evite @SpringBootTest amplo até resolver duplicidades de beans; escopar apenas o necessário com @Import
- Toolchain/execução:
  - Limpar entre execuções: ./mvnw clean
  - Rodar suíte completa (JDK compatível com 24): ./mvnw test
  - Rodar um teste específico e sobrescrever release (ad-hoc):
    - ./mvnw -Dmaven.compiler.release=8 -Dtest=FullyQualifiedTestName test

## Build e Execução
- Build rápido (sem testes): ./mvnw -DskipTests package
- Executar app (perfil default com H2): ./mvnw spring-boot:run
- Infra local opcional via Docker Compose (Mongo, Postgres, Redis): docker compose up -d
- Kafka binder: brokers em localhost:9092 (ajuste ao usar broker local)
- Nunca tente utilizar o comando timeout no MacOS, pois ele não existe por padrão.


## Prompts de Exemplo (alinhados ao projeto)
- "Crie um produtor funcional Spring Cloud Stream para publicar CloudEvent<WalletCreatedEvent> no tópico wallet-created-topic usando CloudEventUtils."
- "Implemente um consumidor funcional para FundsWithdrawnEvent com idempotência e teste usando spring-cloud-stream-test-binder."
- "Crie um UseCase AddFundsUseCase que persiste transação, atualiza saldo e grava evento no outbox dentro da mesma transação."
- "Defina mappers MapStruct para converter WalletEntity <-> Wallet e escreva testes unitários."
- "Configure Resilience4j CircuitBreaker em uma chamada remota no adaptador e cubra com testes."

## Configuração Padrão (application.yml)
- Banco: H2 file em ./db/wallet; JPA ddl-auto=update; H2 console habilitado
- Stream Kafka: bindings listados acima; broker localhost:9092

## Referências
- Spring Boot: https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/
- Spring Cloud Stream: https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/
- Spring for Apache Kafka: https://docs.spring.io/spring-kafka/reference/
- CloudEvents: https://cloudevents.io/
- Spring Statemachine: https://docs.spring.io/spring-statemachine/docs/current/reference/
- Resilience4j: https://resilience4j.readme.io/
- MapStruct: https://mapstruct.org/
- Micrometer: https://micrometer.io/
- Spring Data: https://spring.io/projects/spring-data