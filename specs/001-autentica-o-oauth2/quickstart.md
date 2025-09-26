# Quickstart: OAuth2 Client Credentials

## 1. Criar credencial (admin / script interno)
```
POST /internal/clients
{
  "clientId": "demo-client",
  "scopes": ["wallet.read","wallet.write"],
  "secretPolicy": {"length": 48}
}
=> retorna client_secret uma vez (armazenar em local seguro)
```

## 2. Obter token
```
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&client_id=demo-client&client_secret=***&scope=wallet.read%20wallet.write
```
Resposta 200:
```
{
  "access_token": "<JWT>",
  "token_type": "Bearer",
  "expires_in": 1800,
  "scope": "wallet.read wallet.write"
}
```

## 3. Chamar endpoint protegido
```
GET /api/wallets
Authorization: Bearer <JWT>
```

## 4. Revogar token (exemplo)
```
POST /oauth2/revoke
Content-Type: application/x-www-form-urlencoded

token=<JWT>
```
Sempre 200.

## 5. Testar bloqueio após falhas
Repetir chamada ao /oauth2/token com client_secret incorreto >5 vezes em 5 min → respostas 401 até limite, depois 429 durante 30 min.

## 6. Introspecção (opcional interna)
```
GET /auth/introspect?token=<JWT>
```

## Observabilidade Esperada
- Métricas: wallet.auth.success / wallet.auth.failure{reason} / wallet.auth.revoked
- Logs: evento estruturado por autenticação (correlation id, clientId, outcome)

