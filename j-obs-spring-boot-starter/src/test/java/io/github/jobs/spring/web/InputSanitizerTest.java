package io.github.jobs.spring.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for InputSanitizer.
 */
class InputSanitizerTest {

    // ==================== sanitizeLogger ====================

    @Test
    void sanitizeLogger_shouldAcceptValidLoggerName() {
        assertThat(InputSanitizer.sanitizeLogger("com.example.MyClass")).isEqualTo("com.example.MyClass");
        assertThat(InputSanitizer.sanitizeLogger("MyClass")).isEqualTo("MyClass");
        assertThat(InputSanitizer.sanitizeLogger("_PrivateClass")).isEqualTo("_PrivateClass");
        assertThat(InputSanitizer.sanitizeLogger("com.example$Inner")).isEqualTo("com.example$Inner");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void sanitizeLogger_shouldReturnNullForBlankInput(String input) {
        assertThat(InputSanitizer.sanitizeLogger(input)).isNull();
    }

    @Test
    void sanitizeLogger_shouldRejectInvalidCharacters() {
        assertThat(InputSanitizer.sanitizeLogger("com.example.My-Class")).isNull(); // dash not allowed
        assertThat(InputSanitizer.sanitizeLogger("com/example")).isNull(); // slash not allowed
        assertThat(InputSanitizer.sanitizeLogger("1InvalidStart")).isNull(); // can't start with number
    }

    @Test
    void sanitizeLogger_shouldTruncateLongInput() {
        String longLogger = "a".repeat(300);
        String result = InputSanitizer.sanitizeLogger(longLogger);
        assertThat(result).hasSize(256);
    }

    // ==================== sanitizeMessage ====================

    @Test
    void sanitizeMessage_shouldAcceptValidMessage() {
        assertThat(InputSanitizer.sanitizeMessage("Error occurred")).isEqualTo("Error occurred");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void sanitizeMessage_shouldReturnNullForBlankInput(String input) {
        assertThat(InputSanitizer.sanitizeMessage(input)).isNull();
    }

    @Test
    void sanitizeMessage_shouldEscapeRegexCharacters() {
        // Regex special characters should be escaped
        assertThat(InputSanitizer.sanitizeMessage("test.log")).isEqualTo("test\\.log");
        assertThat(InputSanitizer.sanitizeMessage("a[b]")).isEqualTo("a\\[b\\]");
        assertThat(InputSanitizer.sanitizeMessage("(test)")).isEqualTo("\\(test\\)");
    }

    @Test
    void sanitizeMessage_shouldConvertWildcardToRegex() {
        // Asterisk should become .* for wildcard matching
        assertThat(InputSanitizer.sanitizeMessage("Error*")).isEqualTo("Error.*");
        assertThat(InputSanitizer.sanitizeMessage("*Exception")).isEqualTo(".*Exception");
    }

    @Test
    void sanitizeMessage_shouldTruncateLongInput() {
        String longMessage = "x".repeat(1500);
        String result = InputSanitizer.sanitizeMessage(longMessage);
        assertThat(result).hasSize(1000);
    }

    // ==================== sanitizeTraceId ====================

    @Test
    void sanitizeTraceId_shouldAcceptValidTraceIds() {
        // Hex format (OpenTelemetry)
        assertThat(InputSanitizer.sanitizeTraceId("abc123def456")).isEqualTo("abc123def456");

        // With dashes (W3C format)
        assertThat(InputSanitizer.sanitizeTraceId("abc-123-def")).isEqualTo("abc-123-def");

        // Alphanumeric with underscore
        assertThat(InputSanitizer.sanitizeTraceId("trace_123")).isEqualTo("trace_123");

        // Mixed case
        assertThat(InputSanitizer.sanitizeTraceId("TraceID-ABC")).isEqualTo("TraceID-ABC");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void sanitizeTraceId_shouldReturnNullForBlankInput(String input) {
        assertThat(InputSanitizer.sanitizeTraceId(input)).isNull();
    }

    @Test
    void sanitizeTraceId_shouldRejectInvalidCharacters() {
        assertThat(InputSanitizer.sanitizeTraceId("trace.id")).isNull(); // dot not allowed
        assertThat(InputSanitizer.sanitizeTraceId("trace/id")).isNull(); // slash not allowed
        assertThat(InputSanitizer.sanitizeTraceId("trace id")).isNull(); // space not allowed
        assertThat(InputSanitizer.sanitizeTraceId("trace@id")).isNull(); // @ not allowed
    }

    @Test
    void sanitizeTraceId_shouldTrimWhitespace() {
        assertThat(InputSanitizer.sanitizeTraceId("  trace123  ")).isEqualTo("trace123");
    }

    @Test
    void sanitizeTraceId_shouldTruncateLongInput() {
        String longTraceId = "a".repeat(100);
        String result = InputSanitizer.sanitizeTraceId(longTraceId);
        assertThat(result).hasSize(64);
    }

    // ==================== sanitizeThreadName ====================

    @Test
    void sanitizeThreadName_shouldAcceptValidThreadName() {
        assertThat(InputSanitizer.sanitizeThreadName("main")).isEqualTo("main");
        assertThat(InputSanitizer.sanitizeThreadName("http-nio-8080-exec-1")).isEqualTo("http-nio-8080-exec-1");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void sanitizeThreadName_shouldReturnNullForBlankInput(String input) {
        assertThat(InputSanitizer.sanitizeThreadName(input)).isNull();
    }

    @Test
    void sanitizeThreadName_shouldRemoveControlCharacters() {
        assertThat(InputSanitizer.sanitizeThreadName("thread\u0000name")).isEqualTo("threadname");
    }

    // ==================== sanitizeGeneric ====================

    @Test
    void sanitizeGeneric_shouldAcceptValidInput() {
        assertThat(InputSanitizer.sanitizeGeneric("test-value")).isEqualTo("test-value");
        assertThat(InputSanitizer.sanitizeGeneric("value_123")).isEqualTo("value_123");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void sanitizeGeneric_shouldReturnNullForBlankInput(String input) {
        assertThat(InputSanitizer.sanitizeGeneric(input)).isNull();
    }

    // ==================== sanitizeLimit ====================

    @Test
    void sanitizeLimit_shouldReturnDefaultForInvalidValues() {
        assertThat(InputSanitizer.sanitizeLimit(0)).isEqualTo(100);
        assertThat(InputSanitizer.sanitizeLimit(-1)).isEqualTo(100);
        assertThat(InputSanitizer.sanitizeLimit(-100)).isEqualTo(100);
    }

    @Test
    void sanitizeLimit_shouldCapAtMaximum() {
        assertThat(InputSanitizer.sanitizeLimit(5000)).isEqualTo(1000);
        assertThat(InputSanitizer.sanitizeLimit(Integer.MAX_VALUE)).isEqualTo(1000);
    }

    @Test
    void sanitizeLimit_shouldAcceptValidValues() {
        assertThat(InputSanitizer.sanitizeLimit(50)).isEqualTo(50);
        assertThat(InputSanitizer.sanitizeLimit(1000)).isEqualTo(1000);
    }

    // ==================== sanitizeOffset ====================

    @Test
    void sanitizeOffset_shouldReturnZeroForNegativeValues() {
        assertThat(InputSanitizer.sanitizeOffset(-1)).isEqualTo(0);
        assertThat(InputSanitizer.sanitizeOffset(-100)).isEqualTo(0);
    }

    @Test
    void sanitizeOffset_shouldAcceptValidValues() {
        assertThat(InputSanitizer.sanitizeOffset(0)).isEqualTo(0);
        assertThat(InputSanitizer.sanitizeOffset(100)).isEqualTo(100);
        assertThat(InputSanitizer.sanitizeOffset(Integer.MAX_VALUE)).isEqualTo(Integer.MAX_VALUE);
    }
}
