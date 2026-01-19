package io.github.jobs.domain.log;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LogEntryTest {

    @Test
    void shouldCreateLogEntryWithBuilder() {
        Instant now = Instant.now();

        LogEntry entry = LogEntry.builder()
                .timestamp(now)
                .level(LogLevel.INFO)
                .loggerName("com.example.MyClass")
                .message("Test message")
                .threadName("main")
                .build();

        assertThat(entry.timestamp()).isEqualTo(now);
        assertThat(entry.level()).isEqualTo(LogLevel.INFO);
        assertThat(entry.loggerName()).isEqualTo("com.example.MyClass");
        assertThat(entry.message()).isEqualTo("Test message");
        assertThat(entry.threadName()).isEqualTo("main");
    }

    @Test
    void shouldGenerateIdIfNotProvided() {
        LogEntry entry = LogEntry.builder()
                .message("Test")
                .build();

        assertThat(entry.id()).isNotNull();
        assertThat(entry.id()).isNotEmpty();
    }

    @Test
    void shouldExtractShortLoggerName() {
        LogEntry entry = LogEntry.builder()
                .loggerName("com.example.service.UserService")
                .message("Test")
                .build();

        assertThat(entry.shortLoggerName()).isEqualTo("UserService");
    }

    @Test
    void shouldHandleNullLoggerName() {
        LogEntry entry = LogEntry.builder()
                .message("Test")
                .build();

        assertThat(entry.shortLoggerName()).isNull();
    }

    @Test
    void shouldDetectTraceId() {
        LogEntry withTrace = LogEntry.builder()
                .message("Test")
                .traceId("abc123")
                .build();

        LogEntry withoutTrace = LogEntry.builder()
                .message("Test")
                .build();

        assertThat(withTrace.hasTraceId()).isTrue();
        assertThat(withoutTrace.hasTraceId()).isFalse();
    }

    @Test
    void shouldDetectThrowable() {
        LogEntry withThrowable = LogEntry.builder()
                .message("Error occurred")
                .throwable("java.lang.NullPointerException: value is null\n\tat com.example.Test.main(Test.java:10)")
                .build();

        LogEntry withoutThrowable = LogEntry.builder()
                .message("Normal log")
                .build();

        assertThat(withThrowable.hasThrowable()).isTrue();
        assertThat(withoutThrowable.hasThrowable()).isFalse();
    }

    @Test
    void shouldDetectErrorStatus() {
        LogEntry error = LogEntry.builder()
                .level(LogLevel.ERROR)
                .message("Error")
                .build();

        LogEntry info = LogEntry.builder()
                .level(LogLevel.INFO)
                .message("Info")
                .build();

        LogEntry withThrowable = LogEntry.builder()
                .level(LogLevel.INFO)
                .message("Info with exception")
                .throwable("Exception")
                .build();

        assertThat(error.hasError()).isTrue();
        assertThat(info.hasError()).isFalse();
        assertThat(withThrowable.hasError()).isTrue();
    }

    @Test
    void shouldMatchQueryByLevel() {
        LogEntry error = LogEntry.builder()
                .level(LogLevel.ERROR)
                .message("Error")
                .build();

        LogEntry info = LogEntry.builder()
                .level(LogLevel.INFO)
                .message("Info")
                .build();

        LogQuery errorQuery = LogQuery.builder().minLevel(LogLevel.ERROR).build();
        LogQuery warnQuery = LogQuery.builder().minLevel(LogLevel.WARN).build();

        assertThat(error.matches(errorQuery)).isTrue();
        assertThat(error.matches(warnQuery)).isTrue();
        assertThat(info.matches(errorQuery)).isFalse();
    }

    @Test
    void shouldMatchQueryByLogger() {
        LogEntry entry = LogEntry.builder()
                .loggerName("com.example.UserService")
                .message("Test")
                .build();

        LogQuery matching = LogQuery.builder().loggerName("UserService").build();
        LogQuery nonMatching = LogQuery.builder().loggerName("OrderService").build();

        assertThat(entry.matches(matching)).isTrue();
        assertThat(entry.matches(nonMatching)).isFalse();
    }

    @Test
    void shouldMatchQueryByMessage() {
        LogEntry entry = LogEntry.builder()
                .message("User logged in successfully")
                .build();

        LogQuery matching = LogQuery.builder().messagePattern("logged in").build();
        LogQuery nonMatching = LogQuery.builder().messagePattern("logged out").build();

        assertThat(entry.matches(matching)).isTrue();
        assertThat(entry.matches(nonMatching)).isFalse();
    }

    @Test
    void shouldMatchQueryByTraceId() {
        LogEntry entry = LogEntry.builder()
                .message("Test")
                .traceId("trace-123")
                .build();

        LogQuery matching = LogQuery.builder().traceId("trace-123").build();
        LogQuery nonMatching = LogQuery.builder().traceId("trace-456").build();

        assertThat(entry.matches(matching)).isTrue();
        assertThat(entry.matches(nonMatching)).isFalse();
    }

    @Test
    void shouldStoreMdc() {
        Map<String, String> mdc = Map.of("userId", "123", "requestId", "req-456");

        LogEntry entry = LogEntry.builder()
                .message("Test")
                .mdc(mdc)
                .build();

        assertThat(entry.mdc()).containsEntry("userId", "123");
        assertThat(entry.mdc()).containsEntry("requestId", "req-456");
    }
}
