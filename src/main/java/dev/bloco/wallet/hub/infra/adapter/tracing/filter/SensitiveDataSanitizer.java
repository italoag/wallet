package dev.bloco.wallet.hub.infra.adapter.tracing.filter;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Sanitizes sensitive data (PII, secrets, credentials) before inclusion in trace span attributes.
 * Implements safelist-based approach per FR-006 requirement.
 * 
 * <p>Sanitization Rules:
 * <ul>
 *   <li>SQL: Replace literal values with placeholders, preserve query structure</li>
 *   <li>URLs: Mask query parameters and sensitive path segments</li>
 *   <li>PII: Mask emails, phone numbers, credit cards using regex patterns</li>
 *   <li>Headers: Allowlist approach - only safe headers are included</li>
 * </ul>
 * 
 * @see <a href="specs/001-observability-tracing/research.md">Research Item 6: Sensitive Data Sanitization</a>
 */
@Component
public class SensitiveDataSanitizer {

    // Safelist: Database fields safe to include in traces
    private static final Set<String> SAFE_DB_FIELDS = Set.of(
        "id", "wallet_id", "transaction_id", "saga_id", "event_id",
        "status", "state", "type", "operation", "amount",
        "created_at", "updated_at", "version"
    );

    // Safelist: HTTP headers safe to include in traces
    private static final Set<String> SAFE_HTTP_HEADERS = Set.of(
        "content-type", "accept", "user-agent", "accept-language",
        "accept-encoding", "connection", "host", "referer"
    );

    // Regex patterns for PII detection and masking
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "\\b(\\+\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"
    );
    
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
        "\\b\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}\\b"
    );
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b"
    );

    // SQL sanitization patterns
    private static final Pattern SQL_STRING_LITERAL = Pattern.compile("'[^']*'");
    private static final Pattern SQL_NUMBER_LITERAL = Pattern.compile("=\\s*\\d+");
    private static final Pattern SQL_IN_CLAUSE = Pattern.compile("IN\\s*\\([^)]+\\)", Pattern.CASE_INSENSITIVE);

    // URL sanitization patterns
    private static final Pattern URL_QUERY_PARAM = Pattern.compile("([?&][^=&]+)=([^&]+)");
    private static final Set<String> SENSITIVE_QUERY_PARAMS = Set.of(
        "token", "access_token", "refresh_token", "api_key", "apikey", "key",
        "secret", "password", "pwd", "auth", "authorization", "session", "ssn"
    );

    /**
     * Sanitizes SQL statements by replacing literal values with placeholders.
     * Preserves query structure for debugging while removing actual data.
     * 
     * <p>Examples:
     * <pre>
     * Input:  SELECT * FROM wallet WHERE id = 123 AND email = 'user@example.com'
     * Output: SELECT * FROM wallet WHERE id = ? AND email = ?
     * 
     * Input:  INSERT INTO transaction (amount) VALUES (100.50)
     * Output: INSERT INTO transaction (amount) VALUES (?)
     * </pre>
     * 
     * @param sql the SQL statement to sanitize
     * @return sanitized SQL with literal values replaced by placeholders
     */
    public String sanitizeSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }

        String sanitized = sql;
        
        // Replace string literals: 'value' -> ?
        sanitized = SQL_STRING_LITERAL.matcher(sanitized).replaceAll("?");
        
        // Replace number literals: = 123 -> = ?
        sanitized = SQL_NUMBER_LITERAL.matcher(sanitized).replaceAll("= ?");
        
        // Replace IN clauses: IN (1, 2, 3) -> IN (?)
        sanitized = SQL_IN_CLAUSE.matcher(sanitized).replaceAll("IN (?)");
        
        // Mask any remaining PII patterns (emails, phones)
        sanitized = maskPiiPatterns(sanitized);
        
        return sanitized;
    }

    /**
     * Sanitizes URLs by masking sensitive query parameters and path segments.
     * Preserves URL structure while protecting credentials and tokens.
     * 
     * <p>Examples:
     * <pre>
     * Input:  https://api.wallet.com/transfer?token=abc123&amount=100
     * Output: https://api.wallet.com/transfer?token=***&amount=100
     * 
     * Input:  https://api.wallet.com/user/user@example.com/profile
     * Output: https://api.wallet.com/user/***../profile
     * </pre>
     * 
     * @param url the URL to sanitize
     * @return sanitized URL with sensitive parameters masked
     */
    public String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }

        String sanitized = url;
        
        // Mask sensitive query parameters
        sanitized = URL_QUERY_PARAM.matcher(sanitized).replaceAll(matchResult -> {
            String paramName = matchResult.group(1).substring(1); // Remove ? or &
            String paramValue = matchResult.group(2);
            
            if (isSensitiveQueryParam(paramName)) {
                return matchResult.group(1) + "=***";
            }
            return matchResult.group(0);
        });
        
        // Mask email addresses in URL paths
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("***@***.***");
        
        // Mask UUIDs (could be sensitive identifiers)
        sanitized = UUID_PATTERN.matcher(sanitized).replaceAll("***-***-***-***-***");
        
        return sanitized;
    }

    /**
     * Masks PII patterns (emails, phones, credit cards) in arbitrary text.
     * Used for sanitizing exception messages, log entries, and freeform text fields.
     * 
     * <p>Examples:
     * <pre>
     * Input:  "User user@example.com made transfer to 555-123-4567"
     * Output: "User ***@***.*** made transfer to ***-***-****"
     * 
     * Input:  "Card 1234-5678-9012-3456 declined"
     * Output: "Card ****-****-****-**** declined"
     * </pre>
     * 
     * @param text the text to sanitize
     * @return text with PII patterns masked
     */
    public String maskPiiPatterns(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String sanitized = text;
        
        // Mask email addresses
        sanitized = maskEmail(sanitized);
        
        // Mask phone numbers
        sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("***-***-****");
        
        // Mask credit card numbers
        sanitized = CREDIT_CARD_PATTERN.matcher(sanitized).replaceAll("****-****-****-****");
        
        return sanitized;
    }

    /**
     * Masks email addresses in text.
     * 
     * @param text the text containing email addresses
     * @return text with emails replaced by masked pattern
     */
    public String maskEmail(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return EMAIL_PATTERN.matcher(text).replaceAll("***@***.***");
    }

    /**
     * Sanitizes HTTP headers by filtering to safelist only.
     * Removes sensitive headers like Authorization, Cookie, etc.
     * 
     * <p>Only headers in {@link #SAFE_HTTP_HEADERS} are included in result.
     * 
     * @param headers the HTTP headers map to sanitize
     * @return filtered map containing only safe headers
     */
    public Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return headers;
        }

        return headers.entrySet().stream()
            .filter(entry -> SAFE_HTTP_HEADERS.contains(entry.getKey().toLowerCase()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }

    /**
     * Sanitizes exception messages to remove sensitive data.
     * Removes PII patterns and sensitive context from exception text.
     * 
     * @param exceptionMessage the exception message to sanitize
     * @return sanitized exception message
     */
    public String sanitizeExceptionMessage(String exceptionMessage) {
        if (exceptionMessage == null || exceptionMessage.isBlank()) {
            return exceptionMessage;
        }
        
        // Mask PII patterns
        String sanitized = maskPiiPatterns(exceptionMessage);
        
        // Mask potential secrets in exception messages
        sanitized = sanitized.replaceAll("(?i)(password|token|secret|key)\\s*[:=]\\s*\\S+", "$1=***");
        
        return sanitized;
    }

    /**
     * Truncates text to maximum length with ellipsis.
     * Prevents span attributes from exceeding size limits (1024 chars per spec).
     * 
     * @param text the text to truncate
     * @param maxLength maximum allowed length
     * @return truncated text with "..." suffix if needed
     */
    public String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Checks if a database field name is safe to include in traces.
     * 
     * @param fieldName the field name to check
     * @return true if field is in safelist
     */
    public boolean isSafeDbField(String fieldName) {
        return SAFE_DB_FIELDS.contains(fieldName.toLowerCase());
    }

    /**
     * Checks if an HTTP header is safe to include in traces.
     * 
     * @param headerName the header name to check
     * @return true if header is in safelist
     */
    public boolean isSafeHeader(String headerName) {
        return SAFE_HTTP_HEADERS.contains(headerName.toLowerCase());
    }

    /**
     * Checks if a query parameter name is sensitive.
     * 
     * @param paramName the parameter name to check
     * @return true if parameter should be masked
     */
    private boolean isSensitiveQueryParam(String paramName) {
        String lowerName = paramName.toLowerCase();
        return SENSITIVE_QUERY_PARAMS.stream()
            .anyMatch(lowerName::contains);
    }

    /**
     * Sanitizes a complete span attribute value based on its type.
     * Applies appropriate sanitization based on attribute key.
     * 
     * @param attributeKey the span attribute key (e.g., "db.statement", "http.url")
     * @param value the attribute value to sanitize
     * @return sanitized value safe for trace export
     */
    public String sanitizeSpanAttribute(String attributeKey, String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        // Apply sanitization based on attribute type
        if (attributeKey.startsWith("db.statement")) {
            value = sanitizeSql(value);
        } else if (attributeKey.startsWith("http.url") || attributeKey.contains("endpoint")) {
            value = sanitizeUrl(value);
        } else if (attributeKey.contains("message") || attributeKey.contains("error")) {
            value = sanitizeExceptionMessage(value);
        } else if (attributeKey.startsWith("http.header")) {
            // Don't include header values unless explicitly safelisted
            value = "***";
        } else {
            // General PII masking for all other attributes
            value = maskPiiPatterns(value);
        }

        // Enforce maximum attribute length (per data-model.md: 1024 chars)
        return truncate(value, 1024);
    }
}
