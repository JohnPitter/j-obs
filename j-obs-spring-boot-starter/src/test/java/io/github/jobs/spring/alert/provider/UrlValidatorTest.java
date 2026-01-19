package io.github.jobs.spring.alert.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UrlValidator SSRF prevention.
 */
class UrlValidatorTest {

    // ==================== Valid URLs ====================

    @Test
    void shouldAcceptKnownWebhookDomains() {
        assertThat(UrlValidator.isSafe("https://api.telegram.org/bot123/sendMessage")).isTrue();
        assertThat(UrlValidator.isSafe("https://hooks.slack.com/services/xxx")).isTrue();
        assertThat(UrlValidator.isSafe("https://outlook.office.com/webhook/xxx")).isTrue();
        assertThat(UrlValidator.isSafe("https://outlook.webhook.office.com/xxx")).isTrue();
    }

    @Test
    void shouldAcceptPublicUrls() {
        assertThat(UrlValidator.isSafe("https://example.com/webhook")).isTrue();
        assertThat(UrlValidator.isSafe("https://my-service.company.com/alerts")).isTrue();
        assertThat(UrlValidator.isSafe("http://public-server.net:8080/notify")).isTrue();
    }

    @Test
    void shouldAcceptHttpAndHttps() {
        assertThat(UrlValidator.isSafe("http://example.com")).isTrue();
        assertThat(UrlValidator.isSafe("https://example.com")).isTrue();
        assertThat(UrlValidator.isSafe("HTTP://example.com")).isTrue();
        assertThat(UrlValidator.isSafe("HTTPS://example.com")).isTrue();
    }

    // ==================== Invalid/Empty URLs ====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void shouldRejectEmptyUrls(String url) {
        assertThat(UrlValidator.isSafe(url)).isFalse();

        UrlValidator.ValidationResult result = UrlValidator.validate(url);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).contains("empty");
    }

    @Test
    void shouldRejectMalformedUrls() {
        assertThat(UrlValidator.isSafe("not-a-url")).isFalse();
        assertThat(UrlValidator.isSafe("://missing-scheme.com")).isFalse();
        assertThat(UrlValidator.isSafe("http://")).isFalse();
    }

    // ==================== Blocked Schemes ====================

    @Test
    void shouldRejectNonHttpSchemes() {
        assertThat(UrlValidator.isSafe("ftp://example.com")).isFalse();
        assertThat(UrlValidator.isSafe("file:///etc/passwd")).isFalse();
        assertThat(UrlValidator.isSafe("ssh://server.com")).isFalse();
        assertThat(UrlValidator.isSafe("gopher://server.com")).isFalse();

        UrlValidator.ValidationResult result = UrlValidator.validate("ftp://example.com");
        assertThat(result.errorMessage()).contains("scheme");
    }

    // ==================== Blocked Hosts (SSRF Prevention) ====================

    @Test
    void shouldBlockLocalhost() {
        assertThat(UrlValidator.isSafe("http://localhost/admin")).isFalse();
        assertThat(UrlValidator.isSafe("https://localhost:8080")).isFalse();
        assertThat(UrlValidator.isSafe("http://LOCALHOST")).isFalse();

        UrlValidator.ValidationResult result = UrlValidator.validate("http://localhost");
        assertThat(result.errorMessage()).contains("blocked");
    }

    @Test
    void shouldBlockLoopbackIPs() {
        assertThat(UrlValidator.isSafe("http://127.0.0.1")).isFalse();
        assertThat(UrlValidator.isSafe("http://127.0.0.1:8080/api")).isFalse();
        assertThat(UrlValidator.isSafe("http://0.0.0.0")).isFalse();
    }

    @Test
    void shouldBlockPrivateIPv4Ranges() {
        // 10.0.0.0/8
        assertThat(UrlValidator.isSafe("http://10.0.0.1")).isFalse();
        assertThat(UrlValidator.isSafe("http://10.255.255.255")).isFalse();

        // 172.16.0.0/12
        assertThat(UrlValidator.isSafe("http://172.16.0.1")).isFalse();
        assertThat(UrlValidator.isSafe("http://172.31.255.255")).isFalse();

        // 192.168.0.0/16
        assertThat(UrlValidator.isSafe("http://192.168.0.1")).isFalse();
        assertThat(UrlValidator.isSafe("http://192.168.255.255")).isFalse();
    }

    @Test
    void shouldBlockLinkLocalAddresses() {
        // 169.254.0.0/16 (APIPA)
        assertThat(UrlValidator.isSafe("http://169.254.0.1")).isFalse();
        assertThat(UrlValidator.isSafe("http://169.254.169.254")).isFalse(); // AWS metadata endpoint
    }

    @Test
    void shouldBlockIPv6LoopbackAndPrivate() {
        assertThat(UrlValidator.isSafe("http://[::1]")).isFalse();
        assertThat(UrlValidator.isSafe("http://[0:0:0:0:0:0:0:1]")).isFalse();
    }

    // ==================== Edge Cases ====================

    @Test
    void shouldAllowNonPrivateIPv4() {
        // Note: These IPs should be allowed as they are not in private ranges
        // 8.8.8.8 is Google DNS
        assertThat(UrlValidator.isSafe("http://8.8.8.8")).isTrue();
        assertThat(UrlValidator.isSafe("http://1.1.1.1")).isTrue();
    }

    @Test
    void shouldValidatePortRange() {
        assertThat(UrlValidator.isSafe("http://example.com:80")).isTrue();
        assertThat(UrlValidator.isSafe("http://example.com:443")).isTrue();
        assertThat(UrlValidator.isSafe("http://example.com:8080")).isTrue();
        assertThat(UrlValidator.isSafe("http://example.com:65535")).isTrue();
    }

    // ==================== ValidationResult ====================

    @Test
    void validationResult_shouldProvideErrorMessage() {
        UrlValidator.ValidationResult result = UrlValidator.validate("http://localhost");
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).isNotNull();
        assertThat(result.errorMessage()).isNotBlank();
    }

    @Test
    void validationResult_shouldBeValidForGoodUrls() {
        UrlValidator.ValidationResult result = UrlValidator.validate("https://api.telegram.org/bot");
        assertThat(result.isValid()).isTrue();
        assertThat(result.errorMessage()).isNull();
    }
}
