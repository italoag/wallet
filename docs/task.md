# Wallet Hub — Codebase Audit Task List (2025-09-19)

This document captures findings from an audit of the repository and a comprehensive, actionable task list to remediate bugs, inconsistencies, and risks. It targets experienced Spring Boot/Cloud developers working on this codebase.

Notes
- Scope: build (pom), runtime configuration (application.yml), messaging (Cloud Stream/Kafka), outbox pattern, saga/statemachine, AOP loader, domain/events, and tests.
- Priorities: P0 (critical), P1 (high), P2 (normal), P3 (nice-to-have).
- Each task includes acceptance criteria and file pointers where applicable.

--------------------------------------------------------------------------------

## P0 — Critical correctness and runtime stability

1) Duplicate State Machine configurations (bean conflicts, undefined behavior)
   - Status: DONE (2025-10-22)
   - Finding: Two @EnableStateMachine configs exist:
     - infra/provider/data/config/SagaStateMachineConfig.java
     - infra/provider/data/config/StandardSagaStateMachineConfig.java
     Running with both can create multiple state machine beans and conflicting transition sets (one uses ANY state), breaking injection and transitions.
   - Action:
     - Decide on a single state machine configuration. Prefer keeping SagaStateMachineConfig (has JPA persistence) and removing StandardSagaStateMachineConfig; or merge the richer failure transitions from Standard into SagaStateMachineConfig and keep only one class.
     - Ensure only one @EnableStateMachine is active.
   - Acceptance:
     - ApplicationContext boots with exactly one StateMachine<SagaStates,SagaEvents>.
     - Transitions verified by a unit/integration test for all events.
   - Files: src/main/java/dev/bloco/wallet/hub/infra/provider/data/config/*Saga*.java; tests under src/test/java/.../config.

2) Outbox processing scheduled twice (duplicate sends)
   - Status: DONE (2025-10-22)
   - Finding: Two schedulers process outbox events:
     - KafkaEventProducer.processOutbox() @Scheduled(fixedRate=5000)
     - OutboxWorker.processOutbox() @Scheduled(fixedRate=5000)
     This risks double sending and race conditions.
   - Action:
     - Keep a single scheduled worker; recommend using OutboxWorker and remove the scheduler from KafkaEventProducer.
     - Ensure idempotency at the outbox/publication layer (see P1.1).
   - Acceptance:
     - Only one scheduled method exists and is covered by tests; no duplicate sends when both classes are active.
   - Files: infra/adapter/event/producer/KafkaEventProducer.java; infra/provider/data/OutboxWorker.java; tests.

3) FundsAddedEventConsumer is not bound to any input destination
   - Status: DONE (2025-10-22)
   - Finding: Functional consumer bean name fundsAddedEventConsumerFunction has no corresponding spring.cloud.stream.bindings.*-in-0 binding in application.yml and no spring.cloud.function.definition set. It will not receive messages in a running app.
   - Action:
     - Add consumer binding and function definition, e.g.:
       - spring.cloud.function.definition=fundsAddedEventConsumerFunction
       - spring.cloud.stream.bindings.fundsAddedEventConsumerFunction-in-0.destination=funds-added-topic
     - Align naming with producer destinations.
   - Acceptance:
     - Application receives messages on funds-added-topic and drives the state machine.
     - Messaging test using test binder passes.
   - Files: src/main/resources/application.yml; FundsAddedEventConsumer; messaging tests.

4) DinamoLibraryLoader aspect likely never runs and contains Linux path typo
   - Status: DONE (2025-10-22)
   - Finding:
     - Uses Spring AOP (@Aspect) to advise execution of static main method; Spring AOP does not weave static methods by default. Without AspectJ load-time weaving configured, @Before on main probably never executes.
     - Linux lib path typo: "liibtacndlib.so" (extra i).
     - OS detection map uses key "nix" which won’t match typical os.name values like "linux"; Windows key "win" is fine.
   - Action:
     - Decide if native libs are actually required. If yes, replace with a regular @PostConstruct in a @Configuration class or an ApplicationRunner; or enable AspectJ LTW and document it.
     - Fix Linux path typo; broaden OS detection (linux, unix, mac if needed) or guard with profiles.
     - Wrap System.load with try/catch; fail fast only if strictly required.
   - Acceptance:
     - Deterministic library loading behavior across OSes; tests or manual verification path.
   - Files: config/DinamoLibraryLoader.java; build (AspectJ if chosen).

5) Saga transition using pseudo ANY state (invalid in one config)
   - Status: DONE (2025-10-22)
   - Finding: SagaStateMachineConfig previously used a transition .source(SagaStates.ANY) which is not a valid pseudo state in Spring Statemachine and would fail at runtime. The configuration now enumerates all failure transitions explicitly.
   - Action:
     - Remove the ANY-based transition; explicitly declare SAGA_FAILED transitions for each relevant state (as in Standard).
     - Consolidate to a single config per P0.1.
   - Acceptance:
     - State machine starts and processes all transitions in tests; no configuration exceptions.
   - Files: infra/provider/data/config/SagaStateMachineConfig.java.

--------------------------------------------------------------------------------

## P1 — High-priority reliability and correctness

1) Outbox idempotency, retention, and correlation usage
   - Status: PARTIAL (2025-10-22) — correlationId now propagated from domain events into outbox; idempotency/retention pending
   - Finding:
     - OutboxService.saveOutboxEvent stores correlationId as null in KafkaEventProducer; no uniqueness or deduplication keys exist.
     - No retention/cleanup policy for sent events.
   - Action:
     - Pass correlationId from domain events when available; or compute a deterministic event key (e.g., aggregateId + type + logical id).
     - Add unique constraint + upsert semantics to avoid duplicates; or handle dedup in publisher/consumer using keys.
     - Add retention policy (scheduled cleanup or DB TTL) for sent rows.
   - Acceptance:
     - Duplicate attempts do not produce duplicate sends; test covers double insert scenario.
     - Table does not grow unbounded.
   - Files: OutboxService, OutboxEvent (entity + schema migration), KafkaEventProducer, OutboxWorker.

2) CloudEvent construction is lossy and lacks content-type
   - Status: DONE (2025-10-22)
   - Finding: CloudEventUtils serializes payload using data.toString().getBytes() and does not set datacontenttype; extension name uses lowercase "correlationid" (check naming policy).
   - Action:
     - Use ObjectMapper to serialize data to JSON; set datacontenttype to application/json.
     - Standardize extension attribute for correlation ID (e.g., correlationId) and ensure producers/consumers propagate it via headers when using Spring Cloud Stream.
   - Acceptance:
     - CloudEvents carry structured JSON payload; tests assert CE attributes and data.
   - Files: infra/util/CloudEventUtils.java; possibly inject ObjectMapper.

3) Mixed reactive/blocking persistence starters without profile separation
   - Finding: Both spring-boot-starter-data-jpa and spring-boot-starter-data-r2dbc (+ r2dbc-h2, r2dbc-postgresql) are included. WebFlux is also present. Running both stacks together can cause confusion and heavier startup.
   - Action:
     - Choose one per profile (default: JPA + H2 file; reactive/r2dbc behind an alternate profile) and conditionally include or configure beans.
     - Optionally, split modules or use Spring Boot profiles to toggle.
   - Acceptance:
     - The default profile cleanly uses JPA/H2; reactive stack disabled. Alternate profile exercises R2DBC.
   - Files: pom.xml (optional), application.yml (profiles), configuration.

4) Dependency hygiene and versioning
   - Finding:
     - spring-cloud-starter-stream-kafka (4.1.0) is redundant alongside spring-cloud-stream + binder-kafka; pinning may diverge from BOM.
     - Numerous Spring AI starters likely unused, inflating footprint and risk.
     - POM metadata is empty (license, scm, developers); README references missing LICENSE.
   - Action:
     - Remove redundant spring-cloud-starter-stream-kafka or justify with usage; rely on BOM-managed versions.
     - Remove or move AI starters to optional/dev profile; keep only what’s used.
     - Fill POM metadata and add LICENSE file; align README.
   - Acceptance:
     - Build still passes; dependency tree simplified; SBOM generated; metadata present.
   - Files: pom.xml, README.md, LICENSE.

5) FundsAddedEventConsumer validation & logging
   - Status: DONE (2025-10-22)
   - Finding: Uses Objects.requireNonNull and catches NullPointerException to trigger SAGA_FAILED; logs with info level on failure.
   - Action:
     - Validate correlationId explicitly; send SAGA_FAILED without relying on NPE; log at warn/error with context-safe details.
     - Consider dead-letter or metrics on failures.
   - Acceptance:
     - Unit tests updated; no reliance on NPE.
   - Files: infra/adapter/event/consumer/FundsAddedEventConsumer.java; tests.

--------------------------------------------------------------------------------

## P2 — Functional completeness and testability

1) Bindings and function definition hygiene
   - Status: DONE (2025-10-22)
   - Finding: application.yml defines only producer out-0 bindings; consumers not defined; no spring.cloud.function.definition.
   - Action:
     - Add function definition for all functional beans (producers/consumers), and align destinations. Keep test binder overrides in tests.
   - Acceptance:
     - Messaging flows verified with test binder; no Kafka needed for tests.
   - Files: src/main/resources/application.yml; test properties (if any).

2) State machine persistence and repository
   - Status: PARTIAL (2025-10-22) — repository wiring confirmed; minimal JPA integration test attempted but context bootstrap requires broader auto-config; defer to follow-up
   - Finding: SagaStateMachineConfig wires JpaPersistingStateMachineInterceptor; confirm StateMachineRepository is picked up; ensure @EntityScan includes statemachine JPA entities (present in main app).
   - Action:
     - Add a light integration test that boots only the state machine config with an in-memory DB to ensure tables and transitions work.
   - Acceptance:
     - Test passes; state persists between restarts (if configured).
   - Files: infra/provider/data/config/*, infra/provider/data/repository/StateMachineRepository.java; tests.

3) Time types and auditing in OutboxEvent
   - Status: DONE (2025-10-22)
   - Finding: OutboxEvent uses LocalDateTime.now(); timezone-sensitive and not DB-defaulted.
   - Action:
     - Consider Instant with @CreationTimestamp, or set DB default; add index on created_at; ensure consistent timezone.
   - Acceptance:
     - New events have consistent UTC timestamps; query by time efficient.
   - Files: OutboxEvent; migration.

4) StreamBridge channel naming coupling
   - Status: DONE (2025-10-22)
   - Finding: KafkaEventProducer and OutboxWorker derive channel from eventType + "-out-0"; eventType values are hard-coded strings (e.g., "fundsAddedEventProducer"). Typos will break.
   - Action:
     - Centralize event type constants and validate against available bindings at startup; or use StreamBridge with binding name constants.
   - Acceptance:
     - Renaming one place updates all usages; tests ensure mapping is correct.
   - Files: KafkaEventProducer, OutboxWorker, constants class.

5) Error handling and observability on outbox send
   - Status: DONE (2025-10-22)
   - Finding: OutboxWorker only marks sent on success; failures are silently skipped; no retries/backoff/metrics.
   - Action:
     - Add logging at warn/error with event id; add meter counters; optionally implement retry/backoff or dead-letter.
   - Acceptance:
     - Metrics visible via Actuator/Micrometer; tests assert error branch invoked.
   - Files: OutboxWorker.

--------------------------------------------------------------------------------

## P3 — Documentation, cleanup, and polish

1) README vs code drift (FundsAddedEventConsumer duplication)
   - Finding: README warns of a @Component + @Bean duplication for FundsAddedEventConsumer, but the code shows @Configuration-only class. Docs are out of date.
   - Action:
     - Update README to reflect current state and the actual pitfalls (bindings missing, schedulers duplication, state machine duplication).
   - Acceptance:
     - README matches codebase; contributors can run tests without confusion.
   - Files: README.md.

2) application.yml cleanup
   - Finding: Manual driver-class-name and H2 dialect set; may not be required with Boot defaults. Property AUTO_RECONNECT in URL is suspicious for H2.
   - Action:
     - Verify URL parameters; remove unnecessary properties; ensure H2 console security guidance in non-dev profiles.
   - Acceptance:
     - App still boots; config minimal and correct.
   - Files: src/main/resources/application.yml.

3) Tooling & build improvements
   - Action:
     - Add Maven Toolchains or toolchain config for JDK 24; document fallback test command.
     - Ensure Hibernate Enhance plugin actually runs in this setup under JDK 24; add note if incompatible.
   - Acceptance:
     - Reproducible builds with the documented toolchain.
   - Files: pom.xml; docs.

4) Licensing and SCM metadata
   - Finding: POM <licenses>, <developers>, <scm> are empty; LICENSE missing.
   - Action:
     - Pick a license (e.g., Apache-2.0); add LICENSE file; fill POM metadata.
   - Acceptance:
     - OSS hygiene is complete.
   - Files: pom.xml; LICENSE; README.md.

--------------------------------------------------------------------------------

## Suggested implementation order (roadmap)

1) Consolidate Saga config to a single, valid state machine with explicit failure transitions (P0.1, P0.5).
2) Remove duplicate outbox scheduler; keep OutboxWorker (P0.2).
3) Wire consumer bindings and function definition; add messaging tests with test binder (P0.3).
4) Fix DinamoLibraryLoader behavior and path; guard by profile or replace mechanism (P0.4).
5) Improve outbox idempotency and retention (P1.1) and CloudEventUtils JSON/content-type (P1.2).
6) Clean dependencies and profiles for persistence tech stacks (P1.3, P1.4).
7) Polish validation/logging and observability (P1.5, P2.5).
8) Documentation, configs, and metadata cleanup (P3 items).

--------------------------------------------------------------------------------

## Test plan overview

- Messaging:
  - Use Spring Cloud Stream test binder; set spring.cloud.stream.defaultBinder=test in tests.
  - Add tests for consumer binding and processing path (InputDestination -> StateMachine transitions).
- Outbox:
  - Unit tests for OutboxService upsert/dedup; integration tests for OutboxWorker sending and marking sent; simulate failures.
- State machine:
  - Unit tests for transitions; integration test booting only the config with in-memory JPA persistence.
- CloudEvents:
  - Tests asserting dataContentType, id/type/source/extension attributes, and JSON data.
- Dinamo loader:
  - If retained, add unit tests around path building and guarded loading; ensure disabled in tests.

--------------------------------------------------------------------------------

## Quick references

- State machine configs: src/main/java/dev/bloco/wallet/hub/infra/provider/data/config/
- Outbox: infra/provider/data/{OutboxEvent, OutboxService, OutboxWorker}, infra/provider/data/repository/OutboxRepository
- Messaging: infra/adapter/event/{producer,consumer}; application.yml bindings
- CloudEvents: infra/util/CloudEventUtils
- Build: pom.xml; README.md; LICENSE

End of document.
