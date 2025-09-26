# Implementation Plan: OAuth2 Client Credentials Authentication

**Branch**: `001-autentica-o-oauth2` | **Date**: 2025-09-26 | **Spec**: `specs/001-autentica-o-oauth2/spec.md`
**Input**: Feature specification from `/specs/001-autentica-o-oauth2/spec.md`

## Execution Flow (/plan command scope)
```
(Refer to template – executed logically; gates satisfied: Clarifications present, 5 decisions locked.)
```

## Summary
Implementar autenticação baseada em fluxo OAuth2 Client Credentials para proteger APIs do Wallet Hub, emitindo tokens de acesso com TTL fixo de 30 minutos, escopos iniciais `wallet.read` e `wallet.write`, suporte a revogação via cache distribuído e política de bloqueio após 5 falhas em 5 minutos (bloqueio de 30 minutos).

Valor: Controlar acesso programático seguro (service-to-service / integrador) antes de flows interativos, fornecendo base para auditoria, métricas e evolução futura para Authorization Code.

## Technical Context
**Language/Version**: Java 24 (mínimo 21 compat)  
**Primary Dependencies**: Spring Boot 3.5.x, Spring Security, (potencial uso Spring Authorization Server ou implementação leve custom se escopo reduzido)  
**Storage**: JPA (usuários/credenciais), Cache distribuído (Redis) para revogação e lock attempts  
**Testing**: JUnit + Spring Boot Test + Security Test + Test slices (unit domain / integration auth filter)  
**Target Platform**: JVM backend (WebFlux + endpoints REST)  
**Project Type**: Backend service (clean architecture, ports & adapters)  
**Performance Goals**: Emissão de token p95 < 50ms (cache aquecido), validação de request p95 < 5ms adicional sobre baseline  
**Constraints**: Sem dependência externa de IdP ainda; arquitetura extensível para Authorization Code futura; evitar bloquear event-loop com operações JPA (usar fallback boundedElastic)  
**Scale/Scope**: Até alguns milhares de requisições/min em fase inicial (suposição); revogação eventual baixa frequência; lock attempts somente em falhas

## Constitution Check
Gates (derivados de `docs/CONSTITUTION.md` + memory constitution template):
- Simplicidade progressiva: iniciar somente Client Credentials (ok)  
- Clean Architecture: separar domínio (User, Credentials), gateway (UserAuthRepository), adaptador (SecurityConfig, TokenIssuer)  
- Idempotência: emissão não precisa ser idempotente além da garantia de credenciais consistentes; revogação deve ser idempotente  
- Observabilidade first: métricas de sucesso/falha + logs estruturados de eventos de auth  
- Segurança: não logar segredos; hash/sal das credenciais

Status: PASS (nenhuma violação estrutural). Nenhuma necessidade de Complexity Tracking agora.

## Project Structure
(Atual apenas - não criar subprojetos; inserir classes em pacotes adequados.)
```
src/main/java/dev/bloco/wallet/hub/
  domain/
    model/ (User, Credentials, TokenRevocationEntry?)
    gateway/ (UserCredentialsRepository, TokenRevocationStore)
  usecase/
    AuthenticateClientUseCase (gera token) ← usa gateways
  infra/
    provider/
      entity/ (UserEntity, CredentialEntity)
      repository/ (JpaUserCredentialsRepository)
    adapter/
      security/
        config/ (SecurityConfig, JwtEncoderConfig?)
        token/ (TokenService, RevocationCacheAdapter)
        web/ (AuthenticationFilter / AuthenticationManager)
      cache/ (RedisRevocationStore)
```
**Structure Decision**: Manter monólito modular (camadas clean). Introduzir subpacote `adapter.security` para isolar concerns. Sem criação de módulo separado neste estágio.

## Phase 0: Outline & Research
Unknowns ainda abertos no spec: clock skew, auditoria campos, invalidação de tokens em desativação de usuário, refresh futuro (fora escopo imediato). Pesquisas:
1. Clock skew padrão recomendado (JWT / OAuth2) → tipicamente +/- 60s  
2. Campos mínimos de auditoria: IP (se disponível), user-agent, client-id, outcome  
3. Invalidação de tokens em desativação: registrar revogação ao desativar usuário; tokens existentes consultam revocation store  
4. Revocation via Redis: chave `revoked:{tokenId}` TTL = restante do token  
5. Rate limit failed attempts: usar contador Redis `auth:fail:{clientId}` com janela sliding (5 min) + chave lock `auth:lock:{clientId}` TTL 30m

research.md será gerado com decisões e alternativas (ex: porque não usar Authorization Server completo agora). (Gerado nesta fase.)

## Phase 1: Design & Contracts
### Data Model (high level)
- User (id, status, createdAt, updatedAt)  
- Credentials (userId FK, clientId unique, clientSecretHash, active, lastRotatedAt)  
- TokenClaims (valor transitório: subject=userId, scopes, issuedAt, expiresAt, jti)  
- RevocationEntry (cache only: jti, expiresAt)  
- FailedAuthCounter (cache ephemeral)  

### API Contracts (propostos)
1. POST /oauth2/token (grant_type=client_credentials, client_id, client_secret, scope="...")  
   - 200: { access_token, token_type=Bearer, expires_in, scope }  
   - 400: invalid_request / unsupported_scope  
   - 401: invalid_client / unauthorized  
   - 429: temporarily_locked (quando em lock)  
2. POST /oauth2/revoke (token)  
   - 200 sempre (idempotente)  
3. GET /auth/introspect?token=... (opcional para debug/observabilidade interna)  
   - 200: { active: bool, scope, exp, sub } ou 401 se não permitido

### Security Flow
- Auth filter extrai Bearer token → valida assinatura / exp / revogação / escopo.  
- Escopos mapeados para autorizações de endpoints (matcher `hasAuthority("SCOPE_wallet.read")` etc.).  
- Revogação: consulta Redis; se presente negar.  
- Lock de client: antes de validar segredo, checar lock key.  

### Observability
Métricas: 
- counter: wallet.auth.success, wallet.auth.failure{reason}  
- counter: wallet.auth.revoked  
- gauge (ou derived): active_revocations (opcional)  
- timer: wallet.auth.token.issue  

quickstart.md mostrará fluxo: criar credencial → solicitar token → chamar endpoint protegido.

### Agent Context Update
Executar script update-agent após criação de artefatos (fora deste passo manualmente; documentado).

## Phase 2: Task Planning Approach
Ver template (não gerar tasks.md agora). Estratégia:
- Iniciar por testes de unidade de TokenService (geração + TTL + escopos)  
- Testes de revogação (idempotência)  
- Testes de rate limit (falhas → lock)  
- Testes de segurança de endpoints (authorization by scope)  
- Só então implementação concreta repos/cache.

## Complexity Tracking
(Nenhuma violação.)

## Progress Tracking
**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [ ] All NEEDS CLARIFICATION resolved (restam 3 não-críticas)
- [ ] Complexity deviations documented (none needed)

---
*Based on Constitution (repo) & memory template*
