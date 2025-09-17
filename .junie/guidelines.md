# Diretrizes específicas do projeto — Wallet Hub

## Público-alvo: Desenvolvedores(as) experientes em Spring Boot/Cloud trabalhando neste repositório.

1) Stack, Build e Configuração
- Java toolchain
  - O pom.xml define java.version=24. Use um JDK compatível com release 24 para builds normais e execução da suíte completa de testes.
  - Se sua JDK local/CI não suporta 24 (ex.: imagem antiga), você ainda pode rodar testes unitários focados sobrescrevendo o release:
    - ./mvnw -Dmaven.compiler.release=8 -Dtest=SeuTeste test
    - Uso apenas para rodadas ad-hoc. Builds de produção/dev devem usar JDK moderna alinhada ao pom.
- Entradas de build (Maven Wrapper)
  - Compilar rápido (sem testes): ./mvnw -DskipTests package
  - Executar app (perfil default com H2): ./mvnw spring-boot:run
  - Nativo (opcional, GraalVM): native-maven-plugin já configurado; exige GraalVM compatível e metadata de reachability. Não validado neste snapshot.
- Processamento de anotações
  - MapStruct habilitado (mapstruct-processor).
  - configuration-processor do Spring Boot habilitado para metadata de configuração.
  - Plugin Hibernate Enhance ativo (LAZY/dirty-tracking/association-management); mantenha entidades seguras à instrumentação.
- Infra de runtime
  - compose.yaml sobe MongoDB, Postgres e Redis (sem portas mapeadas externas por padrão; use mapeamento local se precisar de acesso pelo host).
  - application.yml usa H2 file para JPA por padrão (jdbc:h2:file:./db/wallet).
  - Spring Cloud Stream (Kafka) configurado para brokers em localhost:9092. Em testes, prefira o test binder.

2) Execução local e perfis
- Banco: H2 (arquivo) no perfil padrão; console H2 habilitado.
- Mensageria: para desenvolvimento sem Kafka, use o test binder em testes. Para rodar a aplicação com Kafka real, levante um broker local e ajuste brokers conforme necessário.
- Docker Compose (opcional):
  - MongoDB (27017), Postgres (5432), Redis (6379). Ajuste variáveis de ambiente conforme necessidade no compose.yaml.

3) Estrutura de diretórios (real)
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

4) Arquitetura e Padrões
- Clean Architecture (portas e adaptadores)
  - domain: entidades, eventos, contratos (gateways)
  - usecase: orquestra as regras de negócio (interactors)
  - infra/adapter: integrações externas (Kafka producers/consumers, publishers)
  - infra/provider: persistência (JPA/repos), mappers e configuração de saga
- Padrões de microsserviços
  - Event-driven com Kafka + CloudEvents
  - Outbox pattern para publicação assíncrona e confiável
  - Saga/State Machine para coordenação de transações distribuídas
  - Circuit Breaker (Resilience4j) quando integrar com serviços remotos
  - Configuração centralizada (Spring Cloud Config)
- Reativo vs bloqueante
  - WebFlux, Redis/Mongo são reativos; JPA e Kafka Template são bloqueantes.
  - Não execute chamadas bloqueantes em threads event-loop. Faça hand-off (boundedElastic) ou separe as camadas.

5) Mensageria e CloudEvents
- Use Spring Cloud Stream com bindings funcionais (Supplier/Function/Consumer).
- Bindings atuais (application.yml):
  - walletCreatedEventProducer-out-0 -> wallet-created-topic
  - fundsAddedEventProducer-out-0   -> funds-added-topic
  - fundsWithdrawnEventProducer-out-0 -> funds-withdrawn-topic
  - fundsTransferredEventProducer-out-0 -> funds-transferred-topic
- Utilize CloudEventUtils para preencher cabeçalhos CloudEvent.
- Consumidores devem ser idempotentes; produtores devem usar Outbox quando houver transação de domínio.

6) Outbox & Saga
- Outbox: persista eventos no outbox na mesma transação da mudança de estado do domínio; publique de forma assíncrona (OutboxWorker) via KafkaEventProducer.
- Garanta idempotência (chaves de agregação e deduplicação) no publicador e nos consumidores.
- State Machine (Spring Statemachine): estados/eventos em enums; persistência JPA habilitada; utilize StandardSagaStateMachineConfig.

7) Boas práticas de código (Clean Code + SOLID)
- Injeção por construtor; evite @Autowired em campo.
- Coesão (SRP) em entidades/casos de uso. Dependa de abstrações (DIP via gateways).
- Eventos e modelos de domínio preferencialmente imutáveis; use records quando fizer sentido.
- Não exponha entidades JPA diretamente nas APIs; use DTOs e MapStruct para mapeamentos.
- Valide entradas (Bean Validation) e trate exceções com @ControllerAdvice nas APIs.
- Logging com SLF4J e mensagens parametrizadas. Sem logs verbosos em loops quentes.
- Externalize configurações (application.yml, @ConfigurationProperties, @Value). Evite valores hardcoded.
- Formatação/estilo consistente (google-java-format/IDE). Nomes claros; evite abreviações obscuras.

8) Testes: configuração e execução
- Contexto importante
  - Já houve falha de ApplicationContext por duplicidade de bean na camada de consumidores de eventos (ver armadilha abaixo). Limpe antes de rodar uma seleção nova de testes para evitar influência de classes compiladas antigas em target.
    - Limpar: ./mvnw clean
  - O test binder do Spring Cloud Stream está no classpath de testes; prefira-o para evitar Kafka real.
- Executando testes
  - Suíte completa (requer JDK compatível com release 24): ./mvnw test
  - Um único teste: ./mvnw -Dtest=NomeDoTeste test
  - Sobrescrever release para ad-hoc em JDKs antigas:
    - Exemplo: ./mvnw -Dmaven.compiler.release=8 -Dtest=DemoSanityTest test
- Adicionando novos testes
  - Prefira testes unitários puros (sem contexto Spring) para domínio e utilitários; use JUnit 5, AssertJ/Mockito.
  - Mensageria:
    - Inclua spring-cloud-stream-test-binder no teste (@SpringBootTest ou @Import do mínimo necessário).
    - Defina o binder de teste e evite subir Kafka real:
      - Propriedade: spring.cloud.stream.defaultBinder=test
    - Para bindings funcionais (Supplier/Function/Consumer), teste como funções puras quando possível, ou use InputDestination/OutputDestination.
  - Persistência:
    - Utilize @DataJpaTest para slices JPA (H2 in-memory).
  - State machine:
    - Importe apenas StandardSagaStateMachineConfig e enums necessários para manter o contexto leve e evitar conflitos de beans.
- Demo de sanidade (validado):
  - Criado teste trivial dev.bloco.wallet.hub.DemoSanityTest e executado:
    - ./mvnw -Dmaven.compiler.release=8 -Dtest=DemoSanityTest test
  - Resultado: 1 teste, 0 falhas/erros.

9) Observabilidade
- Actuator habilitado; exponha apenas o necessário em produção.
- Micrometer: registries Prometheus/OTLP disponíveis via dependências. Configure exporters conforme seu ambiente.
- Tracing (Brave) presente via micrometer-tracing-bridge-brave.

10) Segurança e segredos
- Não versione segredos (.env, chaves). Use variáveis de ambiente e/ou Vault.
- Para habilitar Vault, adicione spring.config.import=vault:// e as propriedades necessárias ao perfil apropriado.

11) Comandos rápidos
- Build jar (sem testes): ./mvnw -DskipTests package
- Executar app (H2 por padrão): ./mvnw spring-boot:run
- Subir infra opcional: docker compose up -d
- Limpar saídas e testes antigos: ./mvnw clean
- Rodar teste focado (em JDK antiga): ./mvnw -Dmaven.compiler.release=8 -Dtest=FullyQualifiedTestName test

12) Armadilhas conhecidas
- Duplicidade de bean em FundsAddedEventConsumer: a classe está anotada com @Component e há um método @Bean fundsAddedEventConsumer() com o mesmo nome (consumer funcional), causando BeanDefinitionOverrideException durante o bootstrap do ApplicationContext.
  - Opções de resolução:
    - Remover @Component e manter apenas o @Bean em uma @Configuration, OU
    - Renomear o @Bean/binding para evitar colisão, OU
    - Habilitar overriding de definição de bean apenas no perfil de teste (não recomendado para produção).
  - Enquanto não resolvido, evite @SpringBootTest de aplicação inteira; escopar o contexto e/ou usar @Import apenas do necessário.

Notas
- Se adicionar novos bindings do Spring Cloud Stream, atualize application.yml (destinations) e, em testes, adicione configuração correspondente do test binder.
- Este documento está alinhado com o pom.xml, application.yml e a estrutura atual do repositório. Atualize-o sempre que a stack/arquitetura evoluir.
