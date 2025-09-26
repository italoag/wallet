# Data Model: OAuth2 Client Credentials

## Entities

### User
| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | Aggregate root |
| status | ENUM(ACTIVE, DISABLED) | NOT NULL | Checked every request |
| createdAt | Instant | NOT NULL | Auditing |
| updatedAt | Instant | NOT NULL | Auditing |

### Credentials
| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK |  |
| userId | UUID | FK -> User.id | NOT NULL | 1..1 mapping (initial) |
| clientId | String | UNIQUE, NOT NULL | External identifier |
| clientSecretHash | String | NOT NULL | BCrypt/Argon2 hash |
| active | Boolean | NOT NULL |  |
| lastRotatedAt | Instant | NULLABLE | For rotation policy |

### TokenClaims (Transient)
| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| jti | String | Unique (per token) | Stored in revocation cache when revoked |
| sub | UUID | User id |  |
| scopes | Set<String> | Non-empty | Subset of allowed scopes |
| issuedAt | Instant | NOT NULL |  |
| expiresAt | Instant | NOT NULL | TTL = issuedAt + 30m |

### RevocationEntry (Cache)
| Field | Type | Constraints | Notes |
| jti | String | Key | TTL = remaining token life |
| expiresAt | Instant | Derived |  |

### FailedAuthWindow (Cache)
| Field | Type | Constraints | Notes |
| clientId | String | Key |  |
| failureCount | Int | >=0 | Sliding window (5m) |
| lockedUntil | Instant | Optional | Set when locked |

## Relationships
- User 1..1 Credentials (initial) — extensível a multi-credentials futuramente.
- RevocationEntry / FailedAuthWindow são elementos efêmeros (Redis).

## State Transitions
User: ACTIVE -> DISABLED (Triggers: admin action)  
Token: ISSUED -> (REVOKED | EXPIRED)  
Lock: UNLOCKED -> LOCKED (failure threshold) -> UNLOCKED (TTL expiry)

## Validation Rules
- clientId: length 3..64, alfanumérico + `-` / `_`  
- clientSecret: length >= 32 chars random (policy)  
- scopes solicitados ⊆ {wallet.read, wallet.write}  
- disabled user → emissão negada  
- failureCount >=5 within 5m → lock 30m  

## Derived / Computed
- expiresAt = issuedAt + 30m  
- lock state = lockedUntil > now  

## Open Points
- Multi-credential por usuário (futuro)  
- Rotação de segredo automática (não no escopo imediato)  
