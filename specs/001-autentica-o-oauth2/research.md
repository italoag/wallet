# Research: OAuth2 Client Credentials Implementation

## Decisions & Rationale

### 1. Fluxo Inicial: Client Credentials Only
- Decision: Implementar apenas client_credentials
- Rationale: Evita complexidade de Authorization Code / UI; atende uso service-to-service inicial.
- Alternatives: Authorization Code (adiado), Device Code (não necessário), Password (deprecated).

### 2. TTL de Token = 30 minutos
- Decision: 30m expiração
- Rationale: Equilíbrio entre segurança e overhead de emissão; suficientemente curto para mitigação de vazamento.
- Alternatives: 15m (mais renovações), 60m (janela longa), 5m (overhead alto).

### 3. Escopos Iniciais: wallet.read / wallet.write
- Decision: Dois escopos granulares mínimos.
- Rationale: Simples, alinhado a padrões CRUD; permite expansão futura (ex: transaction.approve).
- Alternatives: Escopo único (menos controle), Escopos mais detalhados (complexidade prematura).

### 4. Revogação via Cache Distribuído (Redis)
- Decision: Armazenar jti com TTL restante.
- Rationale: Lookup O(1), expiração automática, escalável distribuído.
- Alternatives: Banco relacional (custo de IO), In-memory local (não cluster safe), Sem revogação (menos controle operacional).

### 5. Política de Tentativas Falhas (5/5 → 30m)
- Decision: 5 falhas em 5 minutos gera bloqueio de 30m.
- Rationale: Mitiga brute force sem bloquear em excesso.
- Alternatives: 3/5 (muito agressivo), 10/10 (menos seguro), Captcha (não aplicável server-to-server).

### 6. Clock Skew (Proposta)
- Proposed: Aceitar +/- 60s para iat/exp (a definir formalmente em atualização de spec).
- Rationale: Divergências de relógio menores comuns; reduz falsos negativos.

### 7. Auditoria (Proposta)
- Proposed fields: timestamp, clientId, scopes solicitados, scopes emitidos, outcome (SUCCESS|FAILURE|LOCKED|REVOKED), ip(remote), userAgent(optional).
- Rationale: Permite rastrear comportamento anômalo e investigações de segurança.

### 8. Invalidação em Desativação de Usuário
- Proposed: Ao desativar, iterar tokens ativos é caro; solução: revogação wildcard via flag de usuário + consulta rápida (user status) em cada requisição. Adicionalmente registrar revogação explícita se jti estiver disponível quando emitido (cache de jtis recentes por user).

## Implementation Notes
- JTI geração segura (UUID v7 ou random 128-bit).  
- Hash de client_secret usando BCrypt (custo calibrado) ou Argon2 (avaliar libs).  
- Token formato: JWT assinado (HS256 com chave rotativa guardada segura) ou assinatura assimétrica (preferível para validação stateless). Decisão futura se integração externa.

## Risk Log
| Risk | Impact | Mitigation |
|------|--------|------------|
| Cache indisponível | Revogação ignorada temporariamente | Fallback negar se sinal crítico; monitoramento health cache |
| Brute force distribuído | Aumento tentativas em IPs diferentes | Monitorar métricas por clientId, adicionar correlação IP mais tarde |
| Token leakage | Acesso indevido até expiração | TTL moderado + revogação manual |
| Clock drift extremo | Rejeição indevida | Documentar requisito de sincronização NTP |

## Open Items
- Formalizar clock skew e auditoria no spec (próxima iteração)
- Decidir algoritmo de assinatura (HS256 vs RS256) dependendo de necessidade de validação por outros serviços

