# Tasks: OAuth2 Client Credentials Authentication

**Input**: Design documents from `/specs/001-autentica-o-oauth2/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/, quickstart.md

## Phase 3.1: Setup
- [ ] T001 Create package structure for security feature: `src/main/java/dev/bloco/wallet/hub/infra/adapter/security/{config,token,web}` and `infra/adapter/security/cache`
- [ ] T002 Add (if missing) Spring Security dependency sanity check and ensure no duplicate Boot versions (align `<spring-boot.version>` if needed) in `pom.xml`
- [ ] T003 [P] Define `SecurityConfig` skeleton (HTTP security, placeholder for JWT filter) at `.../infra/adapter/security/config/SecurityConfig.java`
- [ ] T004 [P] Add `application.yml` placeholders for auth (token ttl=1800, lock thresholds) with comments
- [ ] T005 Introduce Redis configuration bean (if not present) for revocation + rate limiting under `infra/provider/config/RedisAuthConfig.java`

## Phase 3.2: Tests First (TDD)
(Write tests; they MUST fail initially.)
- [ ] T006 [P] Unit test TokenService: generate token (scopes subset, TTL=30m, jti uniqueness) in `src/test/java/.../security/token/TokenServiceTest.java`
- [ ] T007 [P] Unit test RevocationCacheAdapter: revoke + idempotent + expiry in `.../security/token/RevocationCacheAdapterTest.java`
- [ ] T008 [P] Unit test RateLimiter (failed attempts → lock) in `.../security/token/RateLimiterTest.java`
- [ ] T009 [P] Unit test AuthenticateClientUseCase (valid client → token; disabled client → error) in `.../usecase/AuthenticateClientUseCaseTest.java`
- [ ] T010 Integration test POST /oauth2/token success + scope filtering in `src/test/java/.../security/web/OAuth2TokenEndpointIT.java`
- [ ] T011 [P] Integration test revocation POST /oauth2/revoke (token unusable after) in `.../security/web/RevocationEndpointIT.java`
- [ ] T012 [P] Integration test lock after 5 failures returns 429 then unlock after 30m (time simulated) in `.../security/web/RateLimitIT.java`
- [ ] T013 [P] Integration test introspection endpoint (active vs revoked) in `.../security/web/IntrospectionEndpointIT.java`
- [ ] T014 Contract test alignment: validate OpenAPI schemas for /oauth2/token in `.../contract/OAuth2TokenContractTest.java`
- [ ] T015 [P] Contract test alignment: /oauth2/revoke in `.../contract/OAuth2RevokeContractTest.java`
- [ ] T016 [P] Contract test alignment: /auth/introspect in `.../contract/OAuth2IntrospectContractTest.java`

## Phase 3.3: Core Implementation
(Implement only after tests above exist & fail.)
- [ ] T017 Create domain models: `User`, `Credentials`, transient `TokenClaims` at `domain/model/*` (immutable records where applicable)
- [ ] T018 [P] Create gateway interfaces: `UserCredentialsRepository`, `TokenRevocationStore`, `FailedAuthTracker` in `domain/gateway/*`
- [ ] T019 Implement JPA entities `UserEntity`, `CredentialEntity` in `infra/provider/entity/*`
- [ ] T020 Implement Spring Data repository `JpaUserCredentialsRepository` + adapter implementing `UserCredentialsRepository`
- [ ] T021 [P] Implement Redis revocation adapter `RevocationCacheAdapter` (TokenRevocationStore) + key schema `revoked:{jti}`
- [ ] T022 [P] Implement Redis failed attempts adapter `FailedAuthTrackerRedis` with keys `auth:fail:{clientId}` and `auth:lock:{clientId}`
- [ ] T023 Implement `TokenService` (issue + encode JWT + sign) in `infra/adapter/security/token/TokenService.java`
- [ ] T024 Implement `RateLimiter` small service (uses FailedAuthTracker) in `infra/adapter/security/token/RateLimiter.java`
- [ ] T025 Implement `AuthenticateClientUseCase` orchestrating repository + rate limiter + token service
- [ ] T026 [P] Implement `SecurityConfig` (permit endpoints, require auth others, scope mapping) replacing earlier skeleton
- [ ] T027 Implement token endpoint controller/handler `OAuth2TokenController` at `infra/adapter/security/web`
- [ ] T028 Implement revoke endpoint `OAuth2RevokeController`
- [ ] T029 [P] Implement introspection endpoint `OAuth2IntrospectController`
- [ ] T030 Implement authentication filter (extract bearer, validate jti, scopes) `BearerAuthenticationFilter`
- [ ] T031 Wire metrics counters (success/failure/revoked) and timer in TokenService / controllers
- [ ] T032 Add audit logging events (structured) on auth success/fail/revoke in a dedicated logger `AUTH_AUDIT`

## Phase 3.4: Integration
- [ ] T033 Ensure non-blocking usage (wrap JPA calls with boundedElastic where needed in reactive path)
- [ ] T034 Add configuration properties binding class `AuthProperties` (ttl, lockThreshold, lockWindow, lockDuration) + validation
- [ ] T035 Add clock skew (+/- 60s) validation utility in token validation path
- [ ] T036 Add health indicator for Redis revocation store `RevocationStoreHealthIndicator`
- [ ] T037 Implement user disable flow hook (listener or usecase) to enforce token invalidation via user status check
- [ ] T038 Add OpenAPI doc publication or link into README security section

## Phase 3.5: Polish
- [ ] T039 [P] Add unit tests for audit logging formatting `AuditLoggingTest`
- [ ] T040 [P] Add performance test (baseline token issuance throughput) script notes `docs/perf/oauth2-auth.md`
- [ ] T041 Documentation update: README section "Authentication" referencing flows and quickstart
- [ ] T042 [P] Refactor duplication (consolidate Redis key building) `RedisKeyFactory`
- [ ] T043 Security hardening checklist (secret rotation placeholder, algorithm choice doc) `docs/security/oauth2-hardening.md`
- [ ] T044 Remove any leftover TODOs in new classes

## Dependencies & Ordering Notes
- Setup (T001–T005) precede all
- Tests (T006–T016) precede implementation (enforce TDD)
- Models/gateways (T017–T018) before repos/services (T019–T025)
- TokenService (T023) depends on entities + revocation adapter (T021)
- Controllers (T027–T029) depend on TokenService + UseCase
- Filter (T030) depends on TokenService + revocation + properties
- Metrics/audit (T031–T032) after core path established
- Integration tasks (T033–T038) after functional coverage
- Polish tasks (T039–T044) last; [P] where file-isolated

## Parallel Execution Examples
```
# Example batch 1 (after setup):
T006 T007 T008 T009 (unit tests parallel)

# Example batch 2 (contract tests):
T014 T015 T016

# Example batch 3 (adapters):
T021 T022 T026 (security config final) once prereqs done

# Example batch 4 (polish):
T039 T040 T042 T043
```

## Validation Checklist
- [ ] All contracts mapped to contract tests (T014–T016)
- [ ] All entities in data-model have creation tasks (User, Credentials, Claims transient, RevocationEntry ephemeral)
- [ ] Tests precede implementation (Phase 3.2 before 3.3)
- [ ] Parallel [P] tasks touch distinct files
- [ ] Revocation + rate limit implemented and tested
- [ ] Metrics & audit logging tasks present
- [ ] Docs & security hardening tasks included

