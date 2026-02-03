# Data Sanitization Module

## Overview

The Data Sanitization module provides comprehensive protection of sensitive information in distributed traces. It ensures that Personally Identifiable Information (PII), credentials, and other sensitive data are masked or removed before being exported to tracing backends, maintaining security while preserving debugging utility.

## Purpose and Core Functionality

### Primary Objectives
1. **PII Protection**: Mask email addresses, phone numbers, and personal identifiers
2. **Credential Security**: Remove authentication tokens, API keys, and passwords
3. **Compliance**: Support GDPR, CCPA, and other privacy regulations
4. **Debugging Utility**: Preserve enough information for troubleshooting while protecting sensitive data

### Key Features
- **Pattern-based Detection**: Regex patterns for common PII formats
- **Context-aware Sanitization**: Different rules for URLs, SQL, headers, and messages
- **Safe Lists**: Whitelist approach for non-sensitive fields
- **Truncation Control**: Prevent span attribute size violations
- **Comprehensive Coverage**: SQL statements, HTTP URLs, exception messages, headers

## Architecture

### Sanitization Pipeline

```mermaid
graph TB
    subgraph "Input Sources"
        SQL[SQL Statements]
        URL[HTTP URLs]
        Headers[HTTP Headers]
        Messages[Exception Messages]
        Text[Arbitrary Text]
    end
    
    subgraph "SensitiveDataSanitizer"
        PII[PII Detection]
        Patterns[Pattern Matching]
        Masking[Data Masking]
        Truncation[Size Truncation]
    end
    
    subgraph "Output Destinations"
        Span[Span Attributes]
        Logs[Application Logs]
        Traces[Trace Backends]
    end
    
    SQL -->|sanitizeSql| PII
    URL -->|sanitizeUrl| Patterns
    Headers -->|sanitizeHeaders| Masking
    Messages -->|sanitizeExceptionMessage| Truncation
    Text -->|maskPiiPatterns| PII
    
    PII --> Span
    Patterns --> Logs
    Masking --> Traces
    Truncation --> Span
```

## Core Components

### SensitiveDataSanitizer

The main component that provides all sanitization functionality.

#### Key Responsibilities
1. **Pattern Detection**: Identify PII using regex patterns
2. **Context-aware Processing**: Apply different rules based on data type
3. **Safe List Management**: Determine which fields can be included
4. **Size Enforcement**: Truncate data to stay within OpenTelemetry limits

#### Detection Patterns
| Pattern Type | Example | Masked Output |
|-------------|---------|---------------|
| Email | `user@example.com` | `***@***.***` |
| Phone | `555-123-4567` | `***-***-****` |
| Credit Card | `1234-5678-9012-3456` | `****-****-****-****` |
| UUID | `123e4567-e89b-12d3-a456-426614174000` | `***-***-***-***-***` |
| JWT Token | `eyJhbGciOiJIUzI1NiIs...` | `***` |

### Safe Lists

#### Database Fields (Allowed in traces)
```java
private static final Set<String> SAFE_DB_FIELDS = Set.of(
    "id", "wallet_id", "transaction_id", "saga_id", "event_id",
    "status", "state", "type", "operation", "amount",
    "created_at", "updated_at", "version"
);
```

#### HTTP Headers (Allowed in traces)
```java
private static final Set<String> SAFE_HTTP_HEADERS = Set.of(
    "content-type", "accept", "user-agent", "accept-language",
    "accept-encoding", "connection", "host", "referer"
);
```

#### Sensitive Query Parameters (Always masked)
```java
private static final Set<String> SENSITIVE_QUERY_PARAMS = Set.of(
    "token", "access_token", "refresh_token", "api_key", "apikey", "key",
    "secret", "password", "pwd", "auth", "authorization", "session", "ssn"
);
```

## Sanitization Methods

### 1. SQL Statement Sanitization

**Purpose**: Protect database query parameters while preserving query structure.

**Input**: `SELECT * FROM users WHERE email = 'user@example.com' AND id = 123`

**Process**:
1. Replace string literals: `'user@example.com'` → `?`
2. Replace number literals: `id = 123` → `id = ?`
3. Replace IN clauses: `IN (1, 2, 3)` → `IN (?)`
4. Mask any remaining PII

**Output**: `SELECT * FROM users WHERE email = ? AND id = ?`

### 2. URL Sanitization

**Purpose**: Mask sensitive query parameters in HTTP URLs.

**Input**: `https://api.wallet.com/transfer?token=abc123&amount=100&user=john@example.com`

**Process**:
1. Mask sensitive params: `token=abc123` → `token=***`
2. Mask email in path: `john@example.com` → `***@***.***`
3. Preserve safe params: `amount=100` unchanged

**Output**: `https://api.wallet.com/transfer?token=***&amount=100&user=***@***.***`

### 3. HTTP Header Sanitization

**Purpose**: Filter HTTP headers to safe list only.

**Input Headers**:
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIs...",
  "Content-Type": "application/json",
  "Cookie": "session=abc123",
  "User-Agent": "Mozilla/5.0"
}
```

**Process**: Keep only headers in `SAFE_HTTP_HEADERS`

**Output Headers**:
```json
{
  "Content-Type": "application/json",
  "User-Agent": "Mozilla/5.0"
}
```

### 4. Exception Message Sanitization

**Purpose**: Remove PII from error messages and stack traces.

**Input**: `"Failed to process payment for user john@example.com with card 1234-5678-9012-3456"`

**Process**:
1. Mask email: `john@example.com` → `***@***.***`
2. Mask credit card: `1234-5678-9012-3456` → `****-****-****-****`
3. Mask secrets: `password=secret123` → `password=***`

**Output**: `"Failed to process payment for user ***@***.*** with card ****-****-****-****"`

## Integration with Tracing System

### Span Attribute Sanitization

```mermaid
sequenceDiagram
    participant App as Application
    participant Tracer as Tracer
    participant Span as Span
    participant Sanitizer as SensitiveDataSanitizer
    participant Backend as Trace Backend
    
    App->>Tracer: Create span with attributes
    Tracer->>Span: Store raw attributes
    
    loop For each attribute
        Span->>Sanitizer: sanitizeSpanAttribute(key, value)
        Sanitizer->>Sanitizer: Apply context rules
        Sanitizer->>Sanitizer: Mask PII patterns
        Sanitizer->>Sanitizer: Truncate to 1024 chars
        Sanitizer-->>Span: Sanitized value
    end
    
    Span->>Backend: Export sanitized trace
```

### Attribute Type Handling
```java
public String sanitizeSpanAttribute(String attributeKey, String value) {
    if (attributeKey.startsWith("db.statement")) {
        value = sanitizeSql(value);
    } else if (attributeKey.startsWith("http.url") || attributeKey.contains("endpoint")) {
        value = sanitizeUrl(value);
    } else if (attributeKey.contains("message") || attributeKey.contains("error")) {
        value = sanitizeExceptionMessage(value);
    } else if (attributeKey.startsWith("http.header")) {
        value = "***";  // Never include header values
    } else {
        value = maskPiiPatterns(value);  // General PII masking
    }
    
    return truncate(value, 1024);  // OpenTelemetry limit
}
```

## Configuration

### No Configuration Required
The SensitiveDataSanitizer is automatically configured and used by:
- [WebClientTracingCustomizer](webclient_tracing.md) for URL sanitization
- [RepositoryTracingAspect](repository_tracing.md) for SQL sanitization
- [UseCaseTracingAspect](use_case_tracing.md) for exception message sanitization
- All tracing components for general PII protection

## Performance Characteristics

### Overhead Analysis
| Operation | Typical Overhead | Notes |
|-----------|-----------------|-------|
| Pattern Matching | < 0.1ms per pattern | Compile patterns once, reuse |
| URL Sanitization | < 0.05ms | Simple regex replacement |
| SQL Sanitization | < 0.2ms | Multiple pattern replacements |
| Header Filtering | < 0.01ms | Set membership check |
| Truncation | < 0.001ms | String length check |

### Memory Footprint
- **Pattern Objects**: ~5KB (compiled regex patterns)
- **Safe Lists**: ~1KB (HashSet storage)
- **Per-call Memory**: Minimal (reuses pattern matchers)

## Use Cases and Examples

### 1. Database Query Tracing
**Scenario**: Trace SQL queries without exposing sensitive data.

**Before Sanitization**:
```sql
SELECT * FROM users 
WHERE email = 'john@example.com' 
  AND ssn = '123-45-6789'
  AND status = 'ACTIVE'
```

**After Sanitization**:
```sql
SELECT * FROM users 
WHERE email = ? 
  AND ssn = ?
  AND status = ?
```

**Trace Attribute**: `db.statement` contains sanitized SQL

### 2. External API Call Tracing
**Scenario**: Trace external API calls without leaking credentials.

**Original URL**: 
```
https://api.payment.com/charge?api_key=sk_live_abc123&amount=100&customer=user@example.com
```

**Sanitized URL**:
```
https://api.payment.com/charge?api_key=***&amount=100&customer=***@***.***
```

**Trace Attribute**: `http.url` contains sanitized URL

### 3. Error Reporting
**Scenario**: Include error messages in traces without PII.

**Original Error**:
```
Payment failed for user john@example.com (phone: 555-123-4567): 
Card 4111-1111-1111-1111 declined
```

**Sanitized Error**:
```
Payment failed for user ***@***.*** (phone: ***-***-****): 
Card ****-****-****-**** declined
```

**Trace Attribute**: `error.message` contains sanitized error

## Best Practices

### 1. Safe List Management
```java
// Regularly review and update safe lists
private static final Set<String> SAFE_DB_FIELDS = Set.of(
    // Business identifiers
    "id", "reference_id", "transaction_id", "order_id",
    
    // Status fields
    "status", "state", "type", "category",
    
    // Timestamps
    "created_at", "updated_at", "processed_at",
    
    // Business metrics (non-sensitive)
    "amount", "quantity", "price", "total",
    
    // Never include:
    // - Personal data: email, phone, address
    // - Financial data: credit_card, bank_account
    // - Credentials: password, token, secret
);
```

### 2. Integration Testing
```java
@SpringBootTest
class TracingIntegrationTest {
    
    @Autowired
    private SensitiveDataSanitizer sanitizer;
    
    @Test
    void testFullPipeline() {
        // Simulate real tracing scenario
        String sql = "SELECT * FROM users WHERE email = 'test@example.com'";
        String url = "https://api.example.com/data?token=abc123&id=456";
        
        String cleanSql = sanitizer.sanitizeSql(sql);
        String cleanUrl = sanitizer.sanitizeUrl(url);
        
        assertFalse(cleanSql.contains("@"));
        assertFalse(cleanUrl.contains("abc123"));
        assertTrue(cleanUrl.contains("id=456"));  // Safe param preserved
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Over-sanitization
**Symptoms**: Important debugging information is missing.

**Diagnosis**: Check if safe fields are incorrectly excluded.

**Solutions**:
- Review `SAFE_DB_FIELDS` and `SAFE_HTTP_HEADERS`
- Add necessary fields to safe lists
- Test with sample data to verify preservation

#### 2. Under-sanitization
**Symptoms**: Sensitive data appears in traces.

**Diagnosis**: New PII patterns not covered.

**Solutions**:
- Add new patterns to detection system
- Update sensitive parameter lists
- Review trace exports for leaks

## Security Considerations

### 1. Defense in Depth
- Primary: Application-level data protection
- Secondary: Tracing system access controls
- Tertiary: Trace backend security

### 2. Regular Audits
- Monthly: Review trace exports for PII leaks
- Quarterly: Update detection patterns
- Annually: Security penetration testing

## Related Modules

- [WebClient Tracing](webclient_tracing.md): URL sanitization for external API calls
- [Circuit Breaker Tracing](circuit_breaker_tracing.md): Secure tracing of circuit breaker states
- [Tracing Configuration](tracing_configuration.md): Core tracing setup and feature flags
- [Repository Tracing](repository_tracing.md): SQL query sanitization

## References

- [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)
- [GDPR Data Protection](https://gdpr-info.eu/)
- [OWASP Data Validation](https://owasp.org/www-project-data-validation/)
- [PCI DSS Security Standards](https://www.pcisecuritystandards.org/)

---

*Last Updated: January 2024*  
*Module Version: 1.0.0*  
*Compatibility: Spring Boot 3.x, OpenTelemetry 1.x*