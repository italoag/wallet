# Especificação Funcional — Wallet Hub (Compra e Venda de Criptoativos)

> Documento criado com base na análise do repositório atual. O objetivo é detalhar funcionalidades necessárias para um sistema completo de gestão de wallets com compra e venda de criptoativos, cobrindo domínios, fluxos, integrações e requisitos não funcionais.

## 1. Objetivo
Definir as funcionalidades, fluxos e capacidades necessárias para que o Wallet Hub evolua de operações básicas de wallet (criar, depositar, sacar, transferir) para um **sistema completo de gestão e negociação (compra/venda) de criptoativos**, incluindo precificação, ordens, liquidação, compliance e contabilidade.

## 2. Escopo
### 2.1 Dentro do escopo
- Compra e venda de criptoativos (spot).
- Cotação e precificação em tempo real.
- Gestão de ordens e execução.
- Integração on-chain (transferência, assinatura, gas).
- On-ramp e off-ramp fiat (pix/transferência, contas bancárias).
- Compliance (KYC/AML) e limites de risco.
- Ledger contábil e reconciliação.
- Exposição via API (REST/GraphQL/WebSocket) e mensageria.

### 2.2 Fora do escopo (neste documento)
- Derivativos (futuros, opções, margin).
- Staking e rendimento (yield).
- Lending/borrowing.
- Custódia institucional avançada (MPC/HSM) — definido apenas como integração futura.

## 3. Estado Atual (Resumo da análise)
- **Operações de wallet** existentes: criação, atualização, ativação/desativação, deleção, listagem, recuperação, saldo, transferência. 
- **Domínio de transações** limitado a `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`.
- **Portfolio** usa **preços mockados** (placeholder) e não integra market data real.
- **Blockchain connector** existe apenas como especificação draft.
- **APIs públicas para compra/venda** não estão presentes (entrada principal via eventos).

## 4. Funcionalidades necessárias (Gaps)
1. **Domínio de Trading**
   - Entidades: `Order`, `Trade`, `Quote`, `Execution`, `Market`, `PriceSnapshot`.
   - Estados de ordem: `NEW`, `PARTIALLY_FILLED`, `FILLED`, `CANCELED`, `FAILED`.
   - Tipos de transação: adicionar `BUY`, `SELL`, `SWAP`, `FEE`, `REFUND`.

2. **Market Data e Pricing**
   - Integração com provedores externos (ex.: Binance, Coinbase, Chainlink, etc.).
   - Cache e snapshots de preço.
   - Suporte a spreads, slippage e market impact.

3. **Engine de Ordens e Execução**
   - Place order (market/limit).
   - Cancel order.
   - Status de ordem em tempo real.

4. **On/Off-ramp Fiat**
   - Contas fiat do usuário.
   - Integração com PSP/banco (PIX/ACH).
   - Conversão fiat/crypto e liquidação.

5. **Blockchain Connector real**
   - Implementar micro-kernel, plugins de conta, gas, nonce e envio.
   - Suporte EOA, Smart Accounts (ERC-4337), multisig.

6. **Compliance e Risk**
   - KYC/KYB.
   - AML e screening.
   - Limites por usuário (volume/dia, risco).

7. **Ledger Contábil e Reconciliação**
   - Double-entry ledger.
   - Reconciliação com blockchain e PSP.
   - Relatórios fiscais/auditoria.

8. **API Externa**
   - REST/GraphQL endpoints para quotes, orders, trades, balances.
   - WebSocket para preços e status de ordens.

## 5. Requisitos Funcionais (Detalhados)

### 5.1 Mercado e Precificação
- O sistema deve fornecer cotação de compra/venda em tempo real.
- Deve suportar múltiplos pares (BTC/USDT, ETH/BRL etc.).
- Deve aplicar spread configurável por mercado.
- Deve fornecer estimativa de slippage.

### 5.2 Criação de Ordem
- Usuário pode criar ordem do tipo **market** ou **limit**.
- Ordem deve validar:
  - saldo disponível (fiat/crypto)
  - limites de risco
  - status de KYC
- Ordem deve gerar evento de domínio `OrderCreated`.

### 5.3 Execução de Ordem
- Ordem market deve ser executada imediatamente via provedor de liquidez.
- Ordem limit deve aguardar preço alvo.
- Ordem executada gera:
  - `Trade` com detalhes
  - evento `OrderExecuted`

### 5.4 Liquidação
- Ao executar ordem, o ledger deve registrar débitos/créditos.
- A liquidação deve atualizar saldos da wallet.
- Deve haver reconciliação periódica com blockchain.

### 5.5 On/Off-ramp
- Usuário pode depositar fiat (PIX, TED, ACH).
- Usuário pode sacar fiat após venda.
- Integração deve registrar eventos `FiatDepositReceived`, `FiatWithdrawalProcessed`.

### 5.6 Blockchain Operations
- Transferências on-chain devem ser assinadas via plugin apropriado.
- O sistema deve suportar estimativa de gas real.
- Transações devem ser monitoradas até confirmação.

### 5.7 Compliance e Segurança
- Toda operação de compra/venda deve validar status de KYC.
- Deve haver bloqueio de contas com falhas ou sanções.
- Log/auditoria completa de operações.

## 6. Casos de Uso Prioritários
1. **Comprar cripto com fiat**
2. **Vender cripto para fiat**
3. **Consultar portfólio com preço real**
4. **Monitorar status de ordem**
5. **Depositar e sacar fiat**

## 7. Plano de Implementação (Roadmap)

### Fase 0 — Fundamentos de domínio
- Criar entidades Order/Trade/Quote.
- Expandir tipos de transação.
- Criar eventos de domínio de trading.

### Fase 1 — Market Data
- PriceService + integração externa.
- Atualizar portfólio para preço real.

### Fase 2 — Order Engine
- Use cases para place/cancel/status.
- Validação de saldo e limites.

### Fase 3 — On/Off-ramp
- Domínio fiat.
- Integração PSP.

### Fase 4 — Blockchain Connector
- Implementar plugins reais.
- Integração com Web3j/Alchemy.

### Fase 5 — Compliance & Risk
- KYC/AML.
- Limites e score de risco.

### Fase 6 — Ledger & Reconciliação
- Ledger contábil.
- Reconciliação periódica.

### Fase 7 — API Externa
- REST/GraphQL e WebSocket.
- Observabilidade e métricas.

## 8. Dependências e Integrações
- **Market data provider** (Chainlink, Binance, etc.)
- **Liquidity provider** (exchange ou OTC)
- **Blockchain RPC providers**
- **PSP/Banco** (pix, ACH)
- **Ferramentas de compliance**

## 9. Critérios de Aceite
- Compra e venda funcionando ponta-a-ponta.
- Portfólio atualizado com preços reais.
- Ledger auditável.
- Reconciliação automática.
- Compliance aplicado antes de execução.

## 10. Considerações de Observabilidade
- Métricas de ordens (latência, falhas).
- Tracing distribuído por operação.
- Logs auditáveis por usuário.

---

## Apêndice A — Referências internas
- README e estrutura geral do projeto.
- Especificação draft do BlockchainConnector.

