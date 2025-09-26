# Wallet Hub

Event-driven wallet service built with Spring Boot and Spring Cloud Stream. The service models wallet operations (create wallet, add/withdraw/transfer funds) and publishes/consumes domain events via Kafka. Persistence defaults to H2 (file) for local development, with optional Postgres, Redis, and MongoDB via Docker Compose. The codebase follows a ports-and-adapters (Clean Architecture) layout and includes outbox and saga/state-machine patterns.

> Note: This README reflects the current repository snapshot as of 2025-09-17. Where details are unclear in the codebase, TODOs are noted rather than invented.

> Engineering Constitution: see `docs/CONSTITUTION.md` for authoritative guidelines (design principles, patterns, testing, observability, security, dependency policy).

## Stack at a glance
- Language: Java (toolchain release 25)
- Build/Package: Maven (Maven Wrapper included)
- Frameworks/Libraries:
  - Spring Boot 3.5.5 (WebFlux, Security, OAuth2 Client, WebSocket, Actuator, DevTools)
  - Spring Cloud 2025.0.x (Config Client/Server, Spring Cloud Stream)
  - Kafka (Spring for Apache Kafka, Kafka Streams)
  - Spring Cloud Stream binders: kafka, kafka-streams; test binder on test classpath
  - Persistence: JPA (H2/Postgres), R2DBC (H2/Postgres), Reactive Redis, Reactive MongoDB
  - Saga/State Machine: spring-statemachine (+ JPA persistence)
  - Integration: Apache Camel (camel-spring-boot)
  - Mapping: MapStruct
  - Observability: Micrometer (Prometheus/OTLP), Tracing (Brave)
  - CloudEvents integration (cloudevents-spring)
  - AI: Spring AI starters present in dependencies (usage not documented) — TODO
- Build tooling/plugins: Hibernate Enhance (LAZY/dirty-tracking/association), CycloneDX SBOM, Spring Boot Maven Plugin, GraalVM Native Build Tools
- Optional: GraalVM native image (native-maven-plugin configured)

## Requirements
- JDK: A JDK that supports release 25 (e.g., GraalVM CE 25 or a JDK 25 build)
  - For ad-hoc focused unit tests on older JDKs, you can temporarily override the compiler release (see Testing section).
- Maven Wrapper: Use the provided ./mvnw
- Optional toolchain: mise (mise.toml sets java = graalvm-community-24)
- Optional local infrastructure: Docker (for MongoDB, Postgres, Redis via compose.yaml)
- Kafka: Default binder points to localhost:9092. For tests, prefer the Spring Cloud Stream test binder (no Kafka needed).

## Quick start
- Build (skip tests):
  - ./mvnw -DskipTests package
- Run the application (H2 file DB by default):
  - ./mvnw spring-boot:run
- Bring up optional infra (MongoDB, Postgres, Redis):
  - docker compose up -d

## Entry points
- Application main: dev.bloco.wallet.hub.WalletHubApplication
- Messaging: functional producers/consumers configured via Spring Cloud Stream (see application.yml bindings below)
- Outbox publisher and saga state machine are present in infra/provider/data/config and related packages

## Scripts and useful Maven goals
- Clean: ./mvnw clean
- Build (tests): ./mvnw test
- Build JAR (skip tests): ./mvnw -DskipTests package
- Run app: ./mvnw spring-boot:run
- Build container image (JVM): ./mvnw spring-boot:build-image
- Build container image (Native, if configured): ./mvnw spring-boot:build-image -Pnative  (TODO: verify native profile availability)
- Build native executable (requires GraalVM native-image): ./mvnw native:compile
- Generate SBOM (CycloneDX): ./mvnw cyclonedx:makeAggregateBom
- Run tests in native image (if enabled): ./mvnw test -PnativeTest

See HELP.md for official Spring references and more details on native builds.

## Configuration and environment
Spring Boot properties can be overridden via environment variables following standard mapping.

Defaults (src/main/resources/application.yml):
- Application name: spring.application.name=wallet-hub
- H2 file DB (JPA):
  - spring.datasource.url=jdbc:h2:file:./db/wallet;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
  - spring.jpa.hibernate.ddl-auto=update
  - spring.jpa.show-sql=true
  - spring.h2.console.enabled=true
  - H2 console URL: http://localhost:8080/h2-console
- Spring Cloud Stream bindings → Kafka topics:
  - walletCreatedEventProducer-out-0 → wallet-created-topic
  - fundsAddedEventProducer-out-0 → funds-added-topic
  - fundsWithdrawnEventProducer-out-0 → funds-withdrawn-topic
  - fundsTransferredEventProducer-out-0 → funds-transferred-topic
- Kafka broker:
  - spring.cloud.stream.kafka.binder.brokers=localhost:9092

Common environment overrides (examples):
- SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydatabase
- SPRING_DATASOURCE_USERNAME=myuser
- SPRING_DATASOURCE_PASSWORD=secret
- SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS=localhost:9092
- SPRING_CLOUD_STREAM_DEFAULTBINDER=test (use the test binder in tests)

## Docker Compose (optional)
compose.yaml defines:
- mongodb (port 27017 inside container)
- postgres (port 5432 inside container)
- redis (port 6379 inside container)

Note: No host ports are mapped in the current file. If you need host access, add explicit port mappings, e.g., "5432:5432".

Start services:
- docker compose up -d

## Testing
- Full test suite (requires a JDK compatible with release 24):
  - ./mvnw test
- Run one test class only:
  - ./mvnw -Dtest=FullyQualifiedTestName test
- If your local JDK cannot compile with release 24, for targeted unit tests you may override the compiler release:
  - ./mvnw -Dmaven.compiler.release=8 -Dtest=dev.bloco.wallet.hub.DemoSanityTest test

Messaging tests:
- Prefer the Spring Cloud Stream test binder to avoid a real Kafka broker
- In test properties set: spring.cloud.stream.defaultBinder=test
- Use InputDestination/OutputDestination to send/receive messages in @SpringBootTest or by importing only needed config

Persistence tests:
- For JPA slices, use @DataJpaTest (H2 in-memory)

State machine tests:
- Import only the state machine configuration (Saga) and related enums to narrow the application context

Known pitfall in tests and full context:
- FundsAddedEventConsumer currently has both a @Component and a @Bean with the same name, which can cause BeanDefinitionOverrideException during full ApplicationContext bootstrap. Workarounds:
  1) Remove @Component and keep only the functional @Bean inside a @Configuration class; or
  2) Rename the @Bean/binding to avoid the name clash; or
  3) Enable bean definition overriding only in test profile (not recommended for production).

## Project structure (high level)
```
src/
  main/
    java/dev/bloco/wallet/hub/
      WalletHubApplication.java
      config/
      domain/
        event/
        gateway/
      usecase/
      infra/
        adapter/
          event/
            producer/
            consumer/
        provider/
          data/
            config/ (Saga state machine)
            entity/
            repository/
          mapper/
        util/
    resources/
      application.yml
  test/
    java/dev/bloco/wallet/hub/
      infra/... (producers/consumers tests)
```

## Changelog
- See CHANGELOG.md for release notes.

## License
- TODO: Add a LICENSE file and update this section (no explicit license declared in the current POM).

## Roadmap / TODOs
- API surface documentation (endpoints) — TODO: no public controllers are documented in this snapshot
- Kafka/Topic provisioning scripts — TODO: describe expected topics and provisioning strategy
- CI configuration — TODO: document or add workflow(s) if applicable
- Resolve the FundsAddedEventConsumer bean duplication noted above

## References
- Spring Boot Reference: https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/
- Spring Cloud Stream: https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/
- Spring for Apache Kafka: https://docs.spring.io/spring-kafka/reference/
- CloudEvents: https://cloudevents.io/
- Spring Statemachine: https://docs.spring.io/spring-statemachine/docs/current/reference/
- Resilience4j: https://resilience4j.readme.io/
- MapStruct: https://mapstruct.org/
- Micrometer: https://micrometer.io/
- Spring Data: https://spring.io/projects/spring-data
