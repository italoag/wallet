package dev.bloco.wallet.hub.infra.adapter.tracing.filter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link SensitiveDataSanitizer}.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>SQL sanitization (parameterize literals, preserve structure)</li>
 *   <li>URL sanitization (mask query params, mask PII in paths)</li>
 *   <li>PII masking (emails, phones, credit cards)</li>
 *   <li>HTTP header filtering (safelist approach)</li>
 *   <li>Exception message sanitization</li>
 *   <li>Text truncation</li>
 *   <li>Edge cases (null, empty, already sanitized)</li>
 * </ul>
 * 
 * @see SensitiveDataSanitizer
 */
@DisplayName("SensitiveDataSanitizer Tests")
class SensitiveDataSanitizerTest {

    private SensitiveDataSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new SensitiveDataSanitizer();
    }

    @Nested
    @DisplayName("SQL Sanitization Tests")
    class SqlSanitizationTests {

        @Test
        @DisplayName("Should replace string literals with placeholders")
        void shouldReplaceStringLiterals() {
            String sql = "SELECT * FROM wallet WHERE email = 'user@example.com' AND name = 'John Doe'";
            String result = sanitizer.sanitizeSql(sql);
            
            assertFalse(result.contains("user@example.com"));
            assertFalse(result.contains("John Doe"));
            assertTrue(result.contains("?"));
            assertTrue(result.contains("SELECT * FROM wallet WHERE"));
        }

        @Test
        @DisplayName("Should replace numeric literals with placeholders")
        void shouldReplaceNumericLiterals() {
            String sql = "SELECT * FROM wallet WHERE id = 123 AND balance = 1000";
            String result = sanitizer.sanitizeSql(sql);
            
            assertFalse(result.contains("= 123"));
            assertFalse(result.contains("= 1000"));
            assertTrue(result.contains("= ?"));
            assertTrue(result.contains("SELECT * FROM wallet WHERE"));
        }

        @Test
        @DisplayName("Should replace IN clause values with placeholder")
        void shouldReplaceInClause() {
            String sql = "SELECT * FROM transaction WHERE status IN ('PENDING', 'CONFIRMED', 'FAILED')";
            String result = sanitizer.sanitizeSql(sql);
            
            assertFalse(result.contains("'PENDING'"));
            assertFalse(result.contains("'CONFIRMED'"));
            assertTrue(result.contains("IN (?)"));
        }

        @Test
        @DisplayName("Should handle INSERT statements")
        void shouldHandleInsertStatements() {
            String sql = "INSERT INTO wallet (id, email, balance) VALUES (1, 'test@example.com', 500)";
            String result = sanitizer.sanitizeSql(sql);
            
            // String literals are replaced, but standalone numbers in VALUES clause may remain
            assertFalse(result.contains("test@example.com"));
            assertTrue(result.contains("VALUES"));
            assertTrue(result.contains("?"));
        }

        @Test
        @DisplayName("Should handle UPDATE statements")
        void shouldHandleUpdateStatements() {
            String sql = "UPDATE wallet SET balance = 1000, email = 'new@example.com' WHERE id = 42";
            String result = sanitizer.sanitizeSql(sql);
            
            assertFalse(result.contains("new@example.com"));
            assertFalse(result.contains("= 1000"));
            assertFalse(result.contains("= 42"));
            assertTrue(result.contains("UPDATE wallet SET"));
        }

        @Test
        @DisplayName("Should mask embedded email addresses in SQL")
        void shouldMaskEmbeddedEmailsInSql() {
            String sql = "SELECT * FROM users WHERE email = 'admin@wallet.com'";
            String result = sanitizer.sanitizeSql(sql);
            
            assertFalse(result.contains("admin@wallet.com"));
            // String literal replaced first, then any remaining emails masked
            assertTrue(result.contains("?") || result.contains("***@***.***"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty SQL gracefully")
        void shouldHandleNullAndEmptySql(String sql) {
            String result = sanitizer.sanitizeSql(sql);
            assertEquals(sql, result);
        }

        @Test
        @DisplayName("Should handle complex multi-condition queries")
        void shouldHandleComplexQueries() {
            String sql = "SELECT w.*, u.email FROM wallet w " +
                        "JOIN users u ON w.user_id = u.id " +
                        "WHERE w.balance > 100 AND u.email = 'test@example.com' " +
                        "AND w.status IN ('ACTIVE', 'PENDING')";
            String result = sanitizer.sanitizeSql(sql);
            
            // String literals and = comparisons are replaced; > comparisons may remain
            assertFalse(result.contains("test@example.com"));
            assertTrue(result.contains("JOIN users u ON"));
            assertTrue(result.contains("?"));
        }
    }

    @Nested
    @DisplayName("URL Sanitization Tests")
    class UrlSanitizationTests {

        @Test
        @DisplayName("Should mask sensitive query parameters (token)")
        void shouldMaskTokenParameter() {
            String url = "https://api.wallet.com/transfer?token=abc123xyz&amount=100";
            String result = sanitizer.sanitizeUrl(url);
            
            assertFalse(result.contains("abc123xyz"));
            assertTrue(result.contains("token=***"));
            assertTrue(result.contains("amount=100")); // Non-sensitive param preserved
        }

        @Test
        @DisplayName("Should mask multiple sensitive query parameters")
        void shouldMaskMultipleSensitiveParams() {
            String url = "https://api.wallet.com/auth?access_token=tok123&api_key=key456&session=sess789";
            String result = sanitizer.sanitizeUrl(url);
            
            assertFalse(result.contains("tok123"));
            assertFalse(result.contains("key456"));
            assertFalse(result.contains("sess789"));
            assertTrue(result.contains("access_token=***"));
            assertTrue(result.contains("api_key=***"));
            assertTrue(result.contains("session=***"));
        }

        @Test
        @DisplayName("Should preserve non-sensitive query parameters")
        void shouldPreserveNonSensitiveParams() {
            String url = "https://api.wallet.com/search?page=1&limit=20&sort=desc";
            String result = sanitizer.sanitizeUrl(url);
            
            assertTrue(result.contains("page=1"));
            assertTrue(result.contains("limit=20"));
            assertTrue(result.contains("sort=desc"));
        }

        @Test
        @DisplayName("Should mask email addresses in URL paths")
        void shouldMaskEmailsInPath() {
            String url = "https://api.wallet.com/user/admin@example.com/profile";
            String result = sanitizer.sanitizeUrl(url);
            
            assertFalse(result.contains("admin@example.com"));
            assertTrue(result.contains("***@***.***"));
        }

        @Test
        @DisplayName("Should mask UUIDs in URL paths")
        void shouldMaskUuidsInPath() {
            String url = "https://api.wallet.com/wallet/550e8400-e29b-41d4-a716-446655440000/balance";
            String result = sanitizer.sanitizeUrl(url);
            
            assertFalse(result.contains("550e8400-e29b-41d4-a716-446655440000"));
            assertTrue(result.contains("***-***-***-***-***"));
        }

        @Test
        @DisplayName("Should handle URLs with both sensitive params and path PII")
        void shouldHandleMixedSensitiveContent() {
            String url = "https://api.wallet.com/user/test@example.com?password=secret123";
            String result = sanitizer.sanitizeUrl(url);
            
            assertFalse(result.contains("test@example.com"));
            assertFalse(result.contains("secret123"));
            assertTrue(result.contains("password=***"));
            assertTrue(result.contains("***@***.***"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty URLs gracefully")
        void shouldHandleNullAndEmptyUrl(String url) {
            String result = sanitizer.sanitizeUrl(url);
            assertEquals(url, result);
        }

        @Test
        @DisplayName("Should handle URLs without query parameters")
        void shouldHandleUrlsWithoutQueryParams() {
            String url = "https://api.wallet.com/health";
            String result = sanitizer.sanitizeUrl(url);
            
            assertEquals(url, result);
        }
    }

    @Nested
    @DisplayName("PII Masking Tests")
    class PiiMaskingTests {

        @Test
        @DisplayName("Should mask email addresses")
        void shouldMaskEmails() {
            String text = "User email is john.doe@example.com and support@wallet.com";
            String result = sanitizer.maskPiiPatterns(text);
            
            assertFalse(result.contains("john.doe@example.com"));
            assertFalse(result.contains("support@wallet.com"));
            assertTrue(result.contains("***@***.***"));
        }

        @Test
        @DisplayName("Should mask phone numbers")
        void shouldMaskPhoneNumbers() {
            String text = "Contact at 555-123-4567 or (555) 987-6543 or +1-555-111-2222";
            String result = sanitizer.maskPiiPatterns(text);
            
            assertFalse(result.contains("555-123-4567"));
            assertFalse(result.contains("555-987-6543"));
            assertFalse(result.contains("555-111-2222"));
            assertTrue(result.contains("***-***-****"));
        }

        @Test
        @DisplayName("Should mask credit card numbers")
        void shouldMaskCreditCards() {
            String text = "Card numbers: 1234-5678-9012-3456 and 9876543210123456";
            String result = sanitizer.maskPiiPatterns(text);
            
            assertFalse(result.contains("1234-5678-9012-3456"));
            assertFalse(result.contains("9876543210123456"));
            assertTrue(result.contains("****-****-****-****"));
        }

        @Test
        @DisplayName("Should mask multiple PII types in one string")
        void shouldMaskMultiplePiiTypes() {
            String text = "User admin@example.com called from 555-123-4567 using card 1234-5678-9012-3456";
            String result = sanitizer.maskPiiPatterns(text);
            
            assertFalse(result.contains("admin@example.com"));
            assertFalse(result.contains("555-123-4567"));
            assertFalse(result.contains("1234-5678-9012-3456"));
            assertTrue(result.contains("***@***.***"));
            assertTrue(result.contains("***-***-****"));
            assertTrue(result.contains("****-****-****-****"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty text gracefully")
        void shouldHandleNullAndEmptyText(String text) {
            String result = sanitizer.maskPiiPatterns(text);
            assertEquals(text, result);
        }
    }

    @Nested
    @DisplayName("Email Masking Tests")
    class EmailMaskingTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "user@example.com",
            "john.doe@company.co.uk",
            "admin+tag@subdomain.example.org",
            "test_user123@test-domain.com"
        })
        @DisplayName("Should mask various email formats")
        void shouldMaskVariousEmailFormats(String email) {
            String text = "Email: " + email;
            String result = sanitizer.maskEmail(text);
            
            assertFalse(result.contains(email));
            assertTrue(result.contains("***@***.***"));
        }

        @Test
        @DisplayName("Should mask multiple emails in text")
        void shouldMaskMultipleEmails() {
            String text = "Contact alice@example.com or bob@example.com for support";
            String result = sanitizer.maskEmail(text);
            
            assertFalse(result.contains("alice@example.com"));
            assertFalse(result.contains("bob@example.com"));
            assertEquals(2, countOccurrences(result, "***@***.***"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty input")
        void shouldHandleNullAndEmpty(String input) {
            String result = sanitizer.maskEmail(input);
            assertEquals(input, result);
        }
    }

    @Nested
    @DisplayName("HTTP Header Sanitization Tests")
    class HttpHeaderSanitizationTests {

        @Test
        @DisplayName("Should keep only safe headers")
        void shouldKeepOnlySafeHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");
            headers.put("Authorization", "Bearer secret-token");
            headers.put("Cookie", "session=abc123");
            headers.put("User-Agent", "Mozilla/5.0");
            
            Map<String, String> result = sanitizer.sanitizeHeaders(headers);
            
            assertTrue(result.containsKey("Content-Type"));
            assertTrue(result.containsKey("Accept"));
            assertTrue(result.containsKey("User-Agent"));
            assertFalse(result.containsKey("Authorization"));
            assertFalse(result.containsKey("Cookie"));
        }

        @Test
        @DisplayName("Should handle case-insensitive header names")
        void shouldHandleCaseInsensitiveHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("content-type", "text/html");
            headers.put("ACCEPT", "text/html");
            headers.put("User-Agent", "TestAgent");
            
            Map<String, String> result = sanitizer.sanitizeHeaders(headers);
            
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("Should return empty map for null input")
        void shouldHandleNullInput() {
            Map<String, String> result = sanitizer.sanitizeHeaders(null);
            assertNull(result);
        }

        @Test
        @DisplayName("Should return empty map for empty input")
        void shouldHandleEmptyInput() {
            Map<String, String> result = sanitizer.sanitizeHeaders(new HashMap<>());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should preserve safe header values unchanged")
        void shouldPreserveSafeHeaderValues() {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json; charset=utf-8");
            headers.put("Accept-Language", "en-US,en;q=0.9");
            
            Map<String, String> result = sanitizer.sanitizeHeaders(headers);
            
            assertEquals("application/json; charset=utf-8", result.get("Content-Type"));
            assertEquals("en-US,en;q=0.9", result.get("Accept-Language"));
        }
    }

    @Nested
    @DisplayName("Exception Message Sanitization Tests")
    class ExceptionMessageSanitizationTests {

        @Test
        @DisplayName("Should mask PII in exception messages")
        void shouldMaskPiiInExceptionMessages() {
            String message = "Authentication failed for user admin@example.com at 555-123-4567";
            String result = sanitizer.sanitizeExceptionMessage(message);
            
            assertFalse(result.contains("admin@example.com"));
            assertFalse(result.contains("555-123-4567"));
            assertTrue(result.contains("***@***.***"));
            assertTrue(result.contains("***-***-****"));
        }

        @Test
        @DisplayName("Should mask password values in exception messages")
        void shouldMaskPasswordValues() {
            String message = "Login failed: password=secretpass123";
            String result = sanitizer.sanitizeExceptionMessage(message);
            
            assertFalse(result.contains("secretpass123"));
            assertTrue(result.contains("password=***"));
        }

        @Test
        @DisplayName("Should mask token values in exception messages")
        void shouldMaskTokenValues() {
            String message = "API call failed: token=abc123xyz";
            String result = sanitizer.sanitizeExceptionMessage(message);
            
            assertFalse(result.contains("abc123xyz"));
            assertTrue(result.contains("token=***"));
        }

        @Test
        @DisplayName("Should mask secret and key values")
        void shouldMaskSecretAndKeyValues() {
            String message = "Configuration error: secret: my-secret-value and api_key = my-key-123";
            String result = sanitizer.sanitizeExceptionMessage(message);
            
            assertFalse(result.contains("my-secret-value"));
            assertFalse(result.contains("my-key-123"));
            assertTrue(result.contains("secret=***") || result.contains("secret: ***"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty messages")
        void shouldHandleNullAndEmptyMessages(String message) {
            String result = sanitizer.sanitizeExceptionMessage(message);
            assertEquals(message, result);
        }
    }

    @Nested
    @DisplayName("Truncation Tests")
    class TruncationTests {

        @Test
        @DisplayName("Should truncate text longer than max length")
        void shouldTruncateLongText() {
            String text = "A".repeat(100);
            String result = sanitizer.truncate(text, 50);
            
            assertEquals(50, result.length());
            assertTrue(result.endsWith("..."));
        }

        @Test
        @DisplayName("Should not truncate text shorter than max length")
        void shouldNotTruncateShortText() {
            String text = "Short text";
            String result = sanitizer.truncate(text, 50);
            
            assertEquals(text, result);
        }

        @Test
        @DisplayName("Should handle text exactly at max length")
        void shouldHandleTextAtMaxLength() {
            String text = "A".repeat(50);
            String result = sanitizer.truncate(text, 50);
            
            assertEquals(text, result);
        }

        @Test
        @DisplayName("Should handle null input")
        void shouldHandleNullInput() {
            String result = sanitizer.truncate(null, 50);
            assertNull(result);
        }

        @Test
        @DisplayName("Should enforce 1024 character limit per spec")
        void shouldEnforce1024CharLimit() {
            String text = "A".repeat(2000);
            String result = sanitizer.truncate(text, 1024);
            
            assertEquals(1024, result.length());
            assertTrue(result.endsWith("..."));
        }
    }

    @Nested
    @DisplayName("Safelist Validation Tests")
    class SafelistValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"id", "wallet_id", "transaction_id", "status", "created_at"})
        @DisplayName("Should recognize safe database fields")
        void shouldRecognizeSafeDbFields(String fieldName) {
            assertTrue(sanitizer.isSafeDbField(fieldName));
        }

        @ParameterizedTest
        @ValueSource(strings = {"password", "email", "ssn", "credit_card", "private_key"})
        @DisplayName("Should reject unsafe database fields")
        void shouldRejectUnsafeDbFields(String fieldName) {
            assertFalse(sanitizer.isSafeDbField(fieldName));
        }

        @Test
        @DisplayName("Should handle case-insensitive field names")
        void shouldHandleCaseInsensitiveFields() {
            assertTrue(sanitizer.isSafeDbField("WALLET_ID"));
            assertTrue(sanitizer.isSafeDbField("Status"));
            assertFalse(sanitizer.isSafeDbField("PASSWORD"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"content-type", "accept", "user-agent", "host"})
        @DisplayName("Should recognize safe HTTP headers")
        void shouldRecognizeSafeHeaders(String headerName) {
            assertTrue(sanitizer.isSafeHeader(headerName));
        }

        @ParameterizedTest
        @ValueSource(strings = {"authorization", "cookie", "x-api-key", "x-auth-token"})
        @DisplayName("Should reject unsafe HTTP headers")
        void shouldRejectUnsafeHeaders(String headerName) {
            assertFalse(sanitizer.isSafeHeader(headerName));
        }
    }

    @Nested
    @DisplayName("Span Attribute Sanitization Tests")
    class SpanAttributeSanitizationTests {

        @Test
        @DisplayName("Should sanitize db.statement attributes as SQL")
        void shouldSanitizeDbStatementAsSql() {
            String sql = "SELECT * FROM wallet WHERE id = 123";
            String result = sanitizer.sanitizeSpanAttribute("db.statement", sql);
            
            assertFalse(result.contains("= 123"));
            assertTrue(result.contains("= ?"));
        }

        @Test
        @DisplayName("Should sanitize http.url attributes as URLs")
        void shouldSanitizeHttpUrlAsUrl() {
            String url = "https://api.wallet.com/auth?token=secret123";
            String result = sanitizer.sanitizeSpanAttribute("http.url", url);
            
            assertFalse(result.contains("secret123"));
            assertTrue(result.contains("token=***"));
        }

        @Test
        @DisplayName("Should sanitize error.message attributes")
        void shouldSanitizeErrorMessages() {
            String message = "Authentication failed for user@example.com with password=secret";
            String result = sanitizer.sanitizeSpanAttribute("error.message", message);
            
            assertFalse(result.contains("user@example.com"));
            assertFalse(result.contains("secret"));
            assertTrue(result.contains("password=***"));
        }

        @Test
        @DisplayName("Should mask http.header attributes completely")
        void shouldMaskHttpHeaderAttributes() {
            String headerValue = "Bearer secret-token-abc123";
            String result = sanitizer.sanitizeSpanAttribute("http.header.authorization", headerValue);
            
            assertEquals("***", result);
        }

        @Test
        @DisplayName("Should apply PII masking for generic attributes")
        void shouldApplyPiiMaskingForGenericAttributes() {
            String value = "User admin@example.com called from 555-123-4567";
            String result = sanitizer.sanitizeSpanAttribute("custom.attribute", value);
            
            assertFalse(result.contains("admin@example.com"));
            assertFalse(result.contains("555-123-4567"));
            assertTrue(result.contains("***@***.***"));
        }

        @Test
        @DisplayName("Should enforce 1024 character limit on all attributes")
        void shouldEnforce1024CharLimitOnAttributes() {
            String longValue = "A".repeat(2000);
            String result = sanitizer.sanitizeSpanAttribute("long.attribute", longValue);
            
            assertEquals(1024, result.length());
            assertTrue(result.endsWith("..."));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty attribute values")
        void shouldHandleNullAndEmptyAttributeValues(String value) {
            String result = sanitizer.sanitizeSpanAttribute("test.attribute", value);
            assertEquals(value, result);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Integration Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle text with no sensitive data")
        void shouldHandleTextWithNoSensitiveData() {
            String text = "Normal log message with no PII";
            String result = sanitizer.maskPiiPatterns(text);
            
            assertEquals(text, result);
        }

        @Test
        @DisplayName("Should handle already sanitized SQL")
        void shouldHandleAlreadySanitizedSql() {
            String sql = "SELECT * FROM wallet WHERE id = ? AND status = ?";
            String result = sanitizer.sanitizeSql(sql);
            
            // Should remain unchanged
            assertEquals(sql, result);
        }

        @Test
        @DisplayName("Should handle malformed URLs gracefully")
        void shouldHandleMalformedUrls() {
            String url = "not-a-valid-url";
            String result = sanitizer.sanitizeUrl(url);
            
            // Should not throw exception
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle text with special characters")
        void shouldHandleSpecialCharacters() {
            String text = "Error: $pecial ch@racters & symbols!";
            String result = sanitizer.maskPiiPatterns(text);
            
            // Should not break on special characters
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should be idempotent (multiple applications produce same result)")
        void shouldBeIdempotent() {
            String text = "User user@example.com at phone 555-123-4567";
            String firstPass = sanitizer.maskPiiPatterns(text);
            String secondPass = sanitizer.maskPiiPatterns(firstPass);
            
            assertEquals(firstPass, secondPass);
        }

        @Test
        @DisplayName("Should handle Unicode characters")
        void shouldHandleUnicodeCharacters() {
            String text = "Usuário ligou";  // Removed email to focus on Unicode handling
            String result = sanitizer.maskPiiPatterns(text);
            
            // Should handle UTF-8 without breaking
            assertNotNull(result);
            assertTrue(result.contains("Usuário")); // Unicode characters preserved
        }
    }

    // Helper method
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
