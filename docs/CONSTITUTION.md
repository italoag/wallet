# Constituição de Engenharia & Padrões do Projeto Wallet Hub

> Versão 1.0 (vivente) — Este documento orienta decisões técnicas. Mantê-lo enxuto e pragmático. Atualizações exigem revisão de PR.

## 1. Princípios Fundamentais
1. Clareza acima de esperteza (no clever code).  
2. Simplicidade progressiva: só introduza abstração após 2–3 usos reais.  
3. Coesão alta, acoplamento baixo (SRP, DIP).  
4. Domínio primeiro: tecnologia é detalhe.  
5. Eventos como fonte de verdade de mudanças significativas (event-driven).  
6. Idempotência em tudo que pode ser reprocessado (consumidores, publicadores).  
7. Evitar “soluções mirabolantes”: usar padrões de projeto consagrados antes de inventar frameworks internos.  
8. Segurança by default (inputs, segredos, acesso).  
9. Observabilidade é recurso de primeira classe (logs, métricas, tracing, eventos).  
10. Evolução incremental guiada por testes e dados (profiling/metrics antes de micro-otimizar).

## 2. Stack e Versões de Referência
| Área | Tecnologia | Observação |
|------|------------|------------|
| Linguagem | Java 24 (mínimo 21) | `pom.xml` define `<java.version>24</java.version>` |
| Framework | Spring Boot 3.5.x | Parent 3.5.5; propriedade `spring-boot.version`=3.5.6 (avaliar alinhar) |
| Cloud | Spring Cloud 2025.0.x | Gerenciado via BOM |
| Mensageria | Spring Cloud Stream + Kafka | Functional bindings + CloudEvents |
| Persistência | JPA (H2/Postgres), R2DBC, Redis, MongoDB | JPA = bloqueante / R2DBC & Redis/Mongo = reativo |
| Saga | Spring Statemachine | Persistência JPA habilitada |
| Observabilidade | Micrometer + Brave | Prometheus + OTLP |
| Mapeamento | MapStruct | Gerar mapeamentos determinísticos |
| AI (Opcional) | Spring AI | Não usar em caminhos críticos sem ADR |

Política: Não atualizar versões fora do BOM sem justificativa + review. Evitar dependências não utilizadas; remover “zumbis”. Novas libs: justificar em PR (tamanho, licença, manutenção, alternativas nativas).

## 3. Arquitetura (Clean Architecture / Ports & Adapters)
Camadas:
- domain: entidades, eventos (imutáveis preferencialmente `record`), gateways (interfaces). Sem dependência de Spring.
- usecase: orquestra regras; depende apenas de gateways e modelos de domínio.
- infra/adapter: integrações externas (Kafka, REST externos, outbox publisher, consumidores). Traduz tecnologias → portas.
- infra/provider: persistência (JPA/R2DBC/Redis/Mongo), mappers, configurações de saga/state machine.

Regra de dependência: fluxo sempre de fora para dentro; núcleo não conhece frameworks.

## 4. Padrões de Projeto Utilizados (Preferenciais)
- Ports & Adapters (arquitetural).  
- Outbox Pattern (consistência eventual/publicação confiável).  
- Saga / State Machine (coordenação de transações distribuídas).  
- Domain Events (propagação de mudanças).  
- Factory / Builder simples para agregados complexos (evitar construtores telégrafo).  
- Strategy para seleção de provedores / validações específicas.  
- Template Method apenas se evitar duplicação significativa.  
- Null Object só quando evita cascata de `if` e não mascara erro.  

Anti-padrões a evitar: Anemic Domain Model, God Service, Leakage de entidades JPA em fronteiras públicas, DTOs bi-direcionais sem necessidade, over-layering.

## 5. Reatividade vs Bloqueio
- WebFlux e repositórios reativos: nunca chamar diretamente operações bloqueantes (JPA, Kafka `Template`) no event-loop.  
- Para ponte: usar `publishOn(Schedulers.boundedElastic())` ou adaptar camada separada.  
- Evitar misturar transações JPA dentro de pipelines reativos longos; isolar boundary.  
- Backpressure: preferir fluxos finitos; validar operadores (buffer, onBackpressureXXX) conscientemente.

## 6. Eventos & Mensageria
- Publicar eventos de domínio via outbox na mesma transação da mudança de estado.  
- Envelope CloudEvents: usar utilitário `CloudEventUtils` (id, source, type, subject, time, data).  
- Chave de partição: id do agregado principal ou correlação de saga.  
- Idempotência do consumidor: checar chave de negócio ou tabela de deduplicação (quando aplicável).  
- Retry: preferir mecanismos do binder; fallback manual apenas para casos explícitos (exponencial + jitter).  
- Contratos de evento versionados (`type` ou `dataschema`).  
- Evolução compatível: adicionar campos; remover ou mudar semântica via nova versão de evento.

## 7. Persistência
- Transações: delimitar no caso de uso (service boundary) ou no adaptador se multi-repositório.  
- Lazy loading: evitar N+1 (usar fetch join/mapeamentos otimizados).  
- Repositórios: expor *interfaces* em domínio (gateway) + implementação infra.  
- Mapeamento: MapStruct para DTO ↔ entidade; sem lógica de negócio em mappers.  
- Outbox: tabela dedicada com status (NEW, PUBLISHED, FAILED, RETRYING). Worker com backoff previsível.

## 8. Código & Estilo
- Imutabilidade: usar `record` para value objects e eventos.  
- Lombok: uso moderado (evitar `@Data` em entidades JPA; preferir getters/setters explícitos ou construtor).  
- Logging: SLF4J com placeholders (`log.info("Wallet {} created", id)`). Nunca logar segredos / PII sensível sem mascarar.  
- Nomeação: métodos de use case → verbo+Objeto (`CreateWalletUseCase`).  
- Pacotes: não criar micro-pacotes para uma única classe sem justificativa.  
- Evitar lógica em construtores; usar métodos de fábrica estáticos se preparar invariantes.

## 9. Validação & Erros
- Bean Validation em DTOs de entrada (controladores / adaptadores).  
- Domínio reforça invariantes (fail fast).  
- Exceções checked: raras (somente se fluxo alternativo for esperado).  
- Mapeamento consistente de erros em adaptadores (ex: HTTP → Problem Details / JSON).  
- Idempotência: operações financeiras devem ser seguras contra retry.

## 10. Segurança
- Sem segredos em `application.yml` versionado; usar Vault / variáveis de ambiente.  
- Principais práticas: TLS externo, cabeçalhos de segurança, sanitização de entrada.  
- Autorização: regra no domínio sempre que possível (ex: verificar dono da wallet).  
- Auditoria: eventos + logs estruturados (correlation / trace id).  
- Dependabot / scanners (quando configurados) → tratar vulnerabilidades altas rapidamente.

## 11. Observabilidade
- Logs estruturados (JSON em produção idealmente).  
- Tracing: cada mensagem/evento carrega `traceparent` (propagação).  
- Métricas custom: prefixo `wallet.` (timer, counter) – evitar cardinalidade explosiva em labels.  
- Health: readiness separada de liveness quando dependências críticas (Kafka, DB) precisarem sinalizar indisponibilidade.  
- Dashboards padrão: throughput de eventos, latência de casos de uso, taxas de erro por tipo.

## 12. Performance & Resiliência
- Circuit Breaker (Resilience4j) para integrações externas com timeouts explícitos.  
- Bulkhead: evitar saturar boundedElastic.  
- Cache: apenas onde perfil provar ganho; usar métricas de hit ratio.  
- Não otimizar prematuramente; priorizar clareza + testes.  
- Load tests antes de tunar pool sizes ou parâmetros de GC.

## 13. Testes (Pirâmide)
1. Domínio (unit puro) — rápido, sem Spring.  
2. Use Cases (unit + mock gateways).  
3. Adaptadores (mensageria: test binder; persistência: slices).  
4. Contratos (Spring Cloud Contract) para tópicos/eventos quando aplicável.  
5. E2E selecionados (mínimos) — focar jornadas críticas.  

Diretrizes: deterministic, sem test flakiness; nomear cenários dado_quando_entao (ou GivenWhenThen em inglês).  
Coverage qualitativa: invariantes, ramificações de regra, falhas.

## 14. Evolução & Decisões (ADR)
- Cada mudança arquitetural relevante → ADR (`docs/adr/NNN-descriptive-title.md`).  
- Template mínimo: Contexto, Decisão, Alternativas, Consequências, Data.  
- Numeração incremental.  
- PR deve citar ADR.

## 15. Processo para Introduzir Nova Dependência
Checklist em PR:
1. Problema real descrito.  
2. Avaliada solução nativa / já existente?  
3. Licença compatível?  
4. Maturidade / manutenção do projeto (commits recentes).  
5. Peso (JAR size) justificado.  
6. Plano de remoção (se experimento).  
7. Testes cobrindo integração.  

## 16. Qualidade Contínua
- Build deve falhar em: testes quebrados, erro estático, falta de formatação se plugin adicionado futuramente.  
- SBOM (CycloneDX) atualizado; monitorar vulnerabilidades.  
- Lint estático / análise (SpotBugs / ErrorProne) — adicionar via ADR se introduzido.

## 17. Deploy & Operação (Futuro)
- Imagem container via buildpacks (Spring Boot plugin) ou Native (quando validado).  
- Variáveis externas definem endpoints críticos (Kafka brokers, DB).  
- Estratégia de migração DB: versionamento (ex: Flyway) — a introduzir antes de produção.  
- Observabilidade: exporter Prometheus + tracing OTLP integrados desde início.

## 18. Segurança de Dados & Privacidade
- Minimizar dados sensíveis persistidos.  
- Pseudonimização onde aplicável.  
- Logs: mascarar campos (ex: e-mail parcial) — criar util quando necessário.

## 19. Checklist de Revisão de PR
- [ ] Nome descritivo e escopo claro.  
- [ ] Tests verdes e cobrindo regra principal.  
- [ ] Não introduz dependências desnecessárias.  
- [ ] Respeita camadas (no leak de frameworks no domínio).  
- [ ] Sem código morto / TODO injustificado.  
- [ ] Logs adequados, sem PII.  
- [ ] Performance aceitável (sem bloqueio no event-loop).  
- [ ] Eventos versionados se alterados.  
- [ ] Atualizou docs (README / ADR / Constituição se preciso).  

## 20. Glossário (Essencial)
- Agregado: entidade raiz que coordena consistência de um cluster de objetos.  
- Outbox: tabela intermediária para publicação de eventos de forma confiável.  
- Saga: coordenação de transações distribuídas por meio de passos compensáveis.  
- CloudEvent: especificação neutra de envelope de evento.

---
Manter este documento sucinto. Propostas extensas → ADR. Dúvidas: abrir issue ou PR de melhoria.
