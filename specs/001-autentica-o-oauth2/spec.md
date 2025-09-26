# Feature Specification: Autenticação OAuth2 para uso das APIs do Wallet

**Feature Branch**: `001-autentica-o-oauth2`  
**Created**: 2025-09-26  
**Status**: Draft  
**Input**: User description: "autenticação oauth2, implementar funcionalidade de autenticação para o usuario utilizar as APIs do wallet"

## Clarifications
### Session 2025-09-26
- Q: Qual conjunto principal de fluxos OAuth2 devemos suportar inicialmente? → A: Client Credentials (somente)
- Q: Qual deve ser a duração padrão do access token (Client Credentials) antes de expirar? → A: 30 minutos
- Q: Qual conjunto inicial de escopos (scopes) devemos definir para controlar acesso às operações do Wallet? → A: wallet.read / wallet.write
- Q: Qual mecanismo principal de revogação de tokens devemos adotar inicialmente? → A: Cache distribuído (TTL por token revogado)
- Q: Qual política inicial de limite para tentativas de autenticação falhas devemos aplicar? → A: 5 falhas em 5 min → bloqueio 30 min

## User Scenarios & Testing *(mandatory)*

### Primary User Story
Um usuário registrado deseja acessar recursos protegidos das APIs do Wallet Hub. Ele obtém um token de acesso (via fluxo OAuth2 suportado) e utiliza esse token em chamadas subsequentes para executar operações sobre suas carteiras e transações de forma segura.

### Acceptance Scenarios
1. **Given** um usuário previamente registrado e ativo, **When** solicita autenticação fornecendo credenciais válidas (fluxo suportado), **Then** recebe um token de acesso válido contendo sua identidade e escopo autorizado.
2. **Given** um token de acesso válido e não expirado, **When** o usuário chama uma API protegida, **Then** a resposta é bem-sucedida e limitada ao escopo permitido.
3. **Given** um token expirado, **When** o usuário tenta acessar uma API protegida, **Then** o sistema retorna erro de autenticação indicando expiração do token.
4. **Given** credenciais inválidas ou usuário desativado, **When** tenta autenticar, **Then** o sistema recusa a autenticação sem revelar detalhes sensíveis (mensagem genérica).
5. **Given** um token revogado ou inválido (assinatura ou manipulação), **When** usado em requisição, **Then** a requisição é negada e o evento de segurança é registrado.

### Edge Cases
- Bloqueio após 5 falhas em janela de 5 minutos: requisições de autenticação subsequentes retornam erro indicando bloqueio temporário até decurso de 30 minutos.
- Como o sistema deve reagir a relógio de cliente incorreto (token aparentemente "não vigente"). [NEEDS CLARIFICATION: tolerância de clock skew]
- Uso de token após desativação do usuário (invalidação imediata ou apenas futura). [NEEDS CLARIFICATION: estratégia de revogação]
- Requisições sem cabeçalho de autorização em endpoints protegidos → retorno padrão consistente.
- Token com escopo insuficiente tentando acessar recurso privilegiado → resposta de autorização negada.

## Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: O sistema MUST permitir que usuários autenticáveis obtenham um token de acesso para consumir APIs protegidas. (Fluxo inicial: Client Credentials somente)
- **FR-002**: O sistema MUST validar tokens enviados em requisições protegidas e rejeitar tokens inválidos ou expirados.
- **FR-003**: O sistema MUST suportar escopos de acesso: `wallet.read` (oper. de consulta) e `wallet.write` (operações mutacionais) aplicados ao fluxo Client Credentials.
- **FR-004**: O sistema MUST registrar eventos de autenticação bem-sucedida e falha para auditoria.
- **FR-005**: O sistema MUST impedir acesso a usuários marcados como desativados.
- **FR-006**: O sistema MUST expirar tokens em 30 minutos (TTL fixo) a partir da emissão no fluxo Client Credentials.
- **FR-007**: O sistema MUST prover meio de renovação de acesso sem reenvio completo de credenciais sensíveis. [NEEDS CLARIFICATION: uso de refresh token ou reautenticação] (Observação: escopo atual não inclui refresh pois fluxo é Client Credentials. Decisão futura se adicionar Authorization Code.)
- **FR-008**: O sistema MUST fornecer resposta de erro consistente (código + corpo padronizado) para falhas de autenticação/autorização.
- **FR-009**: O sistema MUST assegurar que chamadas a endpoints públicos não exijam token e não vazem dados protegidos.
- **FR-010**: O sistema MUST permitir revogação proativa de tokens antes do vencimento registrando identificador do token em cache distribuído com TTL restante (bloqueando seu uso até expiração natural).
- **FR-011**: O sistema MUST limitar tentativas consecutivas de autenticação malsucedidas: bloquear novas tentativas por 30 minutos após 5 falhas dentro de janela móvel de 5 minutos (contagem reinicia após desbloqueio).
- **FR-012**: O sistema MUST proteger contra reutilização de tokens revogados (cache ou lista de bloqueio). [NEEDS CLARIFICATION: janela e persistência]
- **FR-013**: O sistema MUST registrar métricas de autenticação (sucesso/falha, latência). (Valioso para observabilidade)
- **FR-014**: O sistema MUST assegurar segregação de ações entre diferentes usuários (não permitir acesso cruzado de recursos). 

*Ambiguidades sinalizadas devem ser resolvidas antes de saída de Draft.*

### Key Entities
- **Usuário Autenticável**: Representa identidade que pode gerar tokens; atributos principais: identificador, estado (ativo/desativado), credenciais/associações externas, escopos/perfis associados.
- **Token de Acesso**: Representação abstrata de credencial temporária; atributos: emissor, sujeito (usuário), escopos, emissão, expiração, identificador único (para revogação), estado (válido/revogado/expirado).
- **Sessão / Refresh Artefact** (se aplicável): Permite renovação sem reapresentar credenciais primárias. [NEEDS CLARIFICATION: será adotado?]
- **Evento de Auditoria**: Registro de segurança; atributos: tipo (login_success, login_failure, token_revoked), timestamp, sujeito, origem (IP/agent) [NEEDS CLARIFICATION: detalhamento de origem necessário?].

---

## Review & Acceptance Checklist
### Content Quality
- [ ] No implementation details (languages, frameworks, APIs) — OK
- [ ] Focused on user value and business needs — OK
- [ ] Written for non-technical stakeholders — Parcial (termos técnicos mínimos controlados)
- [ ] All mandatory sections completed — OK

### Requirement Completeness
- [ ] No [NEEDS CLARIFICATION] markers remain — Pendente
- [ ] Requirements are testable and unambiguous — Parcial (marcados onde faltam políticas)
- [ ] Success criteria are measurable — Parcial (duração/limites a definir)
- [ ] Scope is clearly bounded — OK inicial
- [ ] Dependencies and assumptions identified — Parcial (políticas a definir)

---

## Execution Status
- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [ ] Review checklist passed (aguarda resolução de ambiguidades)

---

### Resumo de Pontos que Exigem Decisão
1. Fluxo(s) OAuth2 suportados (Authorization Code, Client Credentials, Password não recomendado, Device Code?). → Decidido: Suportar inicialmente APENAS Client Credentials (sem interação end-user direta / sem refresh token por enquanto).
2. Política de duração de token de acesso (ex: 5m, 15m, 60m?). → Decidido: 30 minutos TTL (renovação via nova requisição Client Credentials).
3. Existência e duração de refresh token (se adotado) e rotação. (Agora dependente de eventual decisão futura de suportar Authorization Code)
4. Escopos / claims necessários (ex: wallet.read, wallet.write, transaction.initiate?). → Decidido: `wallet.read`, `wallet.write` (versão inicial mínima; outros poderão evoluir via nova versão de escopos).
5. Mecanismo de revogação (lista em cache, persistência, propagação em cluster). → Decidido: Cache distribuído com TTL por token revogado.
6. Limites de tentativas falhas / política de lock temporário (ex: 5 tentativas em 10 min → bloqueio 15 min?). → Decidido: 5 falhas em 5 minutos → bloqueio 30 minutos.
7. Tolerância de skew de relógio (ex: +/- 60s) para validade temporal.
8. Detalhe de auditoria: quais campos de origem (IP, user-agent, geo aproximado?).
9. Estratégia para invalidação de tokens ativos em desativação de usuário.

Após esclarecimento, atualizar: FRs 003, 006, 007, 010, 011, 012 e entidades relacionadas retirando marcadores.
