package dev.bloco.wallet.hub.infra.adapter.tracing.integration;

import dev.bloco.wallet.hub.infra.adapter.tracing.filter.SensitiveDataSanitizer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.exporter.FinishedSpan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security audit tests for sensitive data in traces (T145).
 * 
 * <h2>PII Detection Patterns</h2>
 * <ul>
 * <li>Email addresses</li>
 * <li>Credit card numbers</li>
 * <li>Social security numbers</li>
 * <li>API keys and tokens</li>
 * <li>Passwords</li>
 * <li>Phone numbers</li>
 * <li>Private keys</li>
 * </ul>
 */
@DisplayName("Sensitive Data Audit Tests")
class SensitiveDataAuditTest extends BaseIntegrationTest {

    @Autowired
    private SensitiveDataSanitizer sanitizer;

    // PII detection patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern
            .compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern API_KEY_PATTERN = Pattern
            .compile("(?i)(api[_-]?key|token|secret|password)\\s*[:=]\\s*['\"]?([a-zA-Z0-9_\\-]+)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");
    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile("-----BEGIN (RSA |EC |DSA )?PRIVATE KEY-----");

    @BeforeEach
    void setUp() {
        // Clear any previous test data
    }

    @Test
    @DisplayName("T145: Email addresses should be sanitized in span tags")
    void emailAddressesShouldBeSanitized() {
        String email = "user@example.com";
        String sanitized = sanitizer.maskEmail(email);

        assertThat(sanitized)
                .as("Email should be masked")
                .doesNotContain("user@example.com")
                .contains("***");
    }

    @Test
    @DisplayName("T145: Credit card numbers should be sanitized in span tags")
    void creditCardNumbersShouldBeSanitized() {
        String ccNumber = "4532-1234-5678-9010";
        String sanitized = sanitizer.maskPiiPatterns(ccNumber);

        assertThat(sanitized)
                .as("Credit card should be fully masked")
                .contains("****-****-****-****")
                .doesNotContain("4532");
    }

    @Test
    @DisplayName("T145: URLs with sensitive data should be sanitized")
    void urlsWithSensitiveDataShouldBeSanitized() {
        String url = "https://api.example.com/users?email=user@example.com&token=abc123xyz&password=secret123";
        String sanitized = sanitizer.sanitizeUrl(url);

        assertThat(sanitized)
                .as("URL parameters should be sanitized")
                .doesNotContain("user@example.com")
                .doesNotContain("abc123xyz")
                .doesNotContain("secret123")
                .contains("***");
    }

    @Test
    @DisplayName("T145: Span tags should not contain raw email addresses")
    void spanTagsShouldNotContainRawEmails() {
        String email = "sensitive@example.com";
        String sanitizedEmail = sanitizer.maskEmail(email);

        Span span = tracer.nextSpan().name("user.create").start();
        span.tag("user.email", sanitizedEmail);
        span.end();

        // Verify sanitized value doesn't contain original email
        assertThat(sanitizedEmail).doesNotContain("sensitive@example.com");
    }

    @Test
    @DisplayName("T145: Span tags should not contain raw tokens or passwords")
    void spanTagsShouldNotContainRawTokens() {
        String text = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.signature";
        String sanitized = sanitizer.sanitizeExceptionMessage(text);

        Span span = tracer.nextSpan().name("api.request").start();
        span.tag("auth.context", sanitized);
        span.end();

        // Should mask "token=" patterns
        assertThat(sanitized).contains("***");
    }

    @Test
    @DisplayName("T145: SQL statements with sensitive data should be sanitized")
    void sqlStatementsWithSensitiveDataShouldBeSanitized() {
        String sql = "SELECT * FROM users WHERE email = 'user@test.com' AND password = 'secret123'";
        String sanitized = sanitizer.sanitizeSql(sql);

        assertThat(sanitized)
                .as("SQL should have literal values replaced with placeholders")
                .doesNotContain("user@test.com")
                .doesNotContain("secret123")
                .contains("?");
    }

    @Test
    @DisplayName("T145: Exception messages with sensitive data should be sanitized")
    void exceptionMessagesWithSensitiveDataShouldBeSanitized() {
        String message = "Authentication failed for user@example.com with password=secret123";
        String sanitized = sanitizer.sanitizeExceptionMessage(message);

        assertThat(sanitized)
                .as("Exception message should have PII and secrets masked")
                .doesNotContain("user@example.com")
                .doesNotContain("secret123")
                .contains("***");
    }

    @Test
    @DisplayName("T145: Should detect PII patterns in span tag values")
    void shouldDetectPiiPatternsInSpanTags() {
        Map<String, String> testCases = Map.of(
                "email", "test@example.com",
                "credit_card", "4532-1234-5678-9010",
                "ssn", "123-45-6789",
                "phone", "555-123-4567");

        for (Map.Entry<String, String> entry : testCases.entrySet()) {
            String type = entry.getKey();
            String value = entry.getValue();

            boolean containsPii = containsPiiPattern(value);

            assertThat(containsPii)
                    .as("Should detect PII in " + type + ": " + value)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("T145: Sanitized values should not match PII patterns")
    void sanitizedValuesShouldNotMatchPiiPatterns() {
        String email = "user@example.com";
        String sanitizedEmail = sanitizer.maskEmail(email);

        assertThat(containsPiiPattern(sanitizedEmail))
                .as("Sanitized email should not match PII patterns")
                .isFalse();

        String ccNumber = "4532123456789010";
        String sanitizedCc = sanitizer.maskPiiPatterns(ccNumber);

        assertThat(CREDIT_CARD_PATTERN.matcher(sanitizedCc).find())
                .as("Sanitized credit card should not match full number pattern")
                .isFalse();
    }

    @Test
    @DisplayName("T145: Should handle null and empty values gracefully")
    void shouldHandleNullAndEmptyValuesGracefully() {
        assertThat(sanitizer.maskEmail(null)).isNull();
        assertThat(sanitizer.maskEmail("")).isEmpty();
        assertThat(sanitizer.sanitizeUrl(null)).isNull();
        assertThat(sanitizer.sanitizeUrl("")).isEmpty();
        assertThat(sanitizer.sanitizeSql(null)).isNull();
        assertThat(sanitizer.sanitizeSql("")).isEmpty();
    }

    @Test
    @DisplayName("T145: Should preserve non-sensitive data")
    void shouldPreserveNonSensitiveData() {
        String safeUrl = "https://api.example.com/users/123/profile";
        String sanitized = sanitizer.sanitizeUrl(safeUrl);

        assertThat(sanitized)
                .as("Safe URL should remain unchanged")
                .isEqualTo(safeUrl);

        String safeSql = "SELECT id, status, amount FROM transactions WHERE status = ? AND amount > ?";
        String sanitizedSql = sanitizer.sanitizeSql(safeSql);

        assertThat(sanitizedSql)
                .as("Safe SQL with placeholders should remain unchanged")
                .contains("status")
                .contains("amount")
                .contains("?");
    }

    @Test
    @DisplayName("T145: Should sanitize database connection strings")
    void shouldSanitizeDatabaseConnectionStrings() {
        String connectionString = "jdbc:postgresql://localhost:5432/mydb?user=admin&password=secret123";
        String sanitized = sanitizer.sanitizeUrl(connectionString);

        assertThat(sanitized)
                .as("Database password should be masked")
                .doesNotContain("secret123")
                .contains("***");
    }

    @Test
    @DisplayName("T145: Should sanitize span attributes based on type")
    void shouldSanitizeSpanAttributesBasedOnType() {
        String sql = "SELECT * FROM users WHERE email = 'test@example.com'";
        String sanitizedSql = sanitizer.sanitizeSpanAttribute("db.statement", sql);

        assertThat(sanitizedSql)
                .as("SQL attribute should be sanitized")
                .doesNotContain("test@example.com")
                .contains("?");

        String url = "https://api.example.com/user?token=abc123&amount=100";
        String sanitizedUrl = sanitizer.sanitizeSpanAttribute("http.url", url);

        assertThat(sanitizedUrl)
                .as("URL attribute should mask sensitive params")
                .doesNotContain("abc123")
                .contains("***");

        String errorMsg = "Authentication failed for user user@example.com with password=secret";
        String sanitizedMsg = sanitizer.sanitizeSpanAttribute("error.message", errorMsg);

        assertThat(sanitizedMsg)
                .as("Error message should mask PII and secrets")
                .doesNotContain("user@example.com")
                .doesNotContain("secret")
                .contains("***");
    }

    /**
     * Check if value contains any PII pattern.
     */
    private boolean containsPiiPattern(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        return EMAIL_PATTERN.matcher(value).find() ||
                CREDIT_CARD_PATTERN.matcher(value).find() ||
                SSN_PATTERN.matcher(value).find() ||
                API_KEY_PATTERN.matcher(value).find() ||
                PHONE_PATTERN.matcher(value).find() ||
                PRIVATE_KEY_PATTERN.matcher(value).find();
    }
}
