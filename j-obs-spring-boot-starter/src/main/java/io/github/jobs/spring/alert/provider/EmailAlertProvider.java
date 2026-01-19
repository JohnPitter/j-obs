package io.github.jobs.spring.alert.provider;

import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertNotificationResult;
import io.github.jobs.domain.alert.AlertSeverity;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Email;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Email (SMTP) alert provider using direct socket connection.
 *
 * <p>Sends alert notifications via email using SMTP protocol.
 * This implementation uses direct socket connections and doesn't
 * require external mail dependencies like javax.mail.</p>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * j-obs:
 *   alerts:
 *     providers:
 *       email:
 *         enabled: true
 *         host: smtp.gmail.com
 *         port: 587
 *         username: ${SMTP_USER}
 *         password: ${SMTP_PASSWORD}
 *         from: alerts@myapp.com
 *         to:
 *           - team@myapp.com
 *           - oncall@myapp.com
 *         ssl: false
 *         start-tls: true
 * }</pre>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li>TLS 1.2 support for secure connections</li>
 *   <li>STARTTLS upgrade for port 587</li>
 *   <li>Implicit SSL for port 465</li>
 *   <li>AUTH LOGIN authentication</li>
 * </ul>
 *
 * <h2>Message Format</h2>
 * <p>Sends HTML-formatted emails with:</p>
 * <ul>
 *   <li>Color-coded header based on severity</li>
 *   <li>Structured table with alert details</li>
 *   <li>Labels displayed in the details section</li>
 * </ul>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see AbstractAlertProvider
 */
public class EmailAlertProvider extends AbstractAlertProvider {

    private final Email config;

    /**
     * Creates a new email provider with the specified configuration.
     *
     * @param config the email configuration containing SMTP settings and recipients
     */
    public EmailAlertProvider(Email config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return "email";
    }

    @Override
    public String getDisplayName() {
        return "Email";
    }

    @Override
    public boolean isConfigured() {
        return config.isConfigured();
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    protected CompletableFuture<AlertNotificationResult> doSend(AlertEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                sendEmail(event);
                return AlertNotificationResult.success(getName(),
                        "Email sent to " + config.getTo().size() + " recipient(s)");
            } catch (Exception e) {
                log.error("Failed to send email alert: {}", e.getMessage(), e);
                return AlertNotificationResult.failure(getName(), e.getMessage());
            }
        });
    }

    private void sendEmail(AlertEvent event) throws IOException {
        String host = config.getHost();
        int port = config.getPort();
        boolean useSsl = config.isSsl();

        Socket socket = null;
        SSLSocket sslSocket = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            // Create initial socket - SSL wrapped if using implicit SSL (port 465)
            if (useSsl) {
                SSLSocketFactory sslFactory = getSSLSocketFactory();
                sslSocket = (SSLSocket) sslFactory.createSocket(host, port);
                sslSocket.startHandshake();
                socket = sslSocket;
            } else {
                socket = new Socket(host, port);
            }

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            // Read server greeting
            String response = readResponse(reader);
            log.debug("SMTP greeting: {}", response);

            // Send EHLO
            sendCommand(writer, reader, "EHLO localhost");

            // STARTTLS for explicit TLS (typically port 587)
            if (config.isStartTls() && !useSsl) {
                sendCommand(writer, reader, "STARTTLS");

                // Upgrade to TLS
                SSLSocketFactory sslFactory = getSSLSocketFactory();
                sslSocket = (SSLSocket) sslFactory.createSocket(socket, host, port, true);
                sslSocket.startHandshake();

                // Create new reader/writer over the TLS socket
                reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8));

                // Re-send EHLO after TLS upgrade
                sendCommand(writer, reader, "EHLO localhost");
            }

            // Authenticate if credentials provided
            if (config.getUsername() != null && !config.getUsername().isEmpty()
                    && config.getPassword() != null && !config.getPassword().isEmpty()) {
                sendCommand(writer, reader, "AUTH LOGIN");
                sendCommand(writer, reader, Base64.getEncoder().encodeToString(
                        config.getUsername().getBytes(StandardCharsets.UTF_8)));
                sendCommand(writer, reader, Base64.getEncoder().encodeToString(
                        config.getPassword().getBytes(StandardCharsets.UTF_8)));
            }

            // Send email
            sendCommand(writer, reader, "MAIL FROM:<" + config.getFrom() + ">");

            for (String to : config.getTo()) {
                sendCommand(writer, reader, "RCPT TO:<" + to + ">");
            }

            sendCommand(writer, reader, "DATA");

            // Send headers and body
            writer.write("From: " + config.getFrom() + "\r\n");
            writer.write("To: " + String.join(", ", config.getTo()) + "\r\n");
            writer.write("Subject: " + buildSubject(event) + "\r\n");
            writer.write("Content-Type: text/html; charset=utf-8\r\n");
            writer.write("MIME-Version: 1.0\r\n");
            writer.write("\r\n");
            writer.write(buildHtmlBody(event));
            writer.write("\r\n.\r\n");
            writer.flush();

            // Read response
            response = readResponse(reader);
            log.debug("SMTP DATA response: {}", response);
            checkSmtpResponse(response, "DATA content");

            // Quit
            sendCommand(writer, reader, "QUIT");

        } finally {
            closeQuietly(writer);
            closeQuietly(reader);
            closeQuietly(sslSocket);
            closeQuietly(socket);
        }
    }

    private SSLSocketFactory getSSLSocketFactory() throws IOException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException("Failed to create SSL context", e);
        }
    }

    private String readResponse(BufferedReader reader) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
            // Multi-line responses have '-' after status code, last line has space
            if (line.length() >= 4 && line.charAt(3) == ' ') {
                break;
            }
            if (line.length() >= 4 && line.charAt(3) == '-') {
                response.append("\n");
                continue;
            }
            break;
        }
        return response.toString();
    }

    private void sendCommand(BufferedWriter writer, BufferedReader reader, String command) throws IOException {
        writer.write(command + "\r\n");
        writer.flush();
        String response = readResponse(reader);
        String cmdName = command.contains(" ") ? command.substring(0, command.indexOf(' ')) : command;
        log.debug("SMTP command [{}] response: {}", cmdName, response);
        checkSmtpResponse(response, cmdName);
    }

    private void checkSmtpResponse(String response, String command) throws IOException {
        if (response == null || response.isEmpty()) {
            throw new IOException("Empty SMTP response for command: " + command);
        }
        char firstChar = response.charAt(0);
        // 4xx = temporary failure, 5xx = permanent failure
        if (firstChar == '4' || firstChar == '5') {
            throw new IOException("SMTP error for " + command + ": " + response);
        }
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.debug("Error closing resource", e);
            }
        }
    }

    private String buildSubject(AlertEvent event) {
        return String.format("[%s] %s - %s",
                event.severity().displayName().toUpperCase(),
                event.alertName(),
                event.status().displayName()
        );
    }

    private String buildHtmlBody(AlertEvent event) {
        String severityColor = getSeverityColor(event.severity());

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'></head><body>");
        html.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");

        // Header
        html.append("<div style='background-color: ").append(severityColor)
                .append("; color: white; padding: 20px; border-radius: 5px 5px 0 0;'>");
        html.append("<h2 style='margin: 0;'>").append(getStatusEmoji(event)).append(" ")
                .append(escapeHtml(event.alertName())).append("</h2>");
        html.append("</div>");

        // Body
        html.append("<div style='background-color: #f5f5f5; padding: 20px; border: 1px solid #ddd;'>");
        html.append("<p style='font-size: 16px;'>").append(escapeHtml(event.message())).append("</p>");

        // Details table
        html.append("<table style='width: 100%; border-collapse: collapse; margin-top: 15px;'>");
        html.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd; font-weight: bold;'>Severity</td>");
        html.append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(event.severity().displayName()).append("</td></tr>");
        html.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd; font-weight: bold;'>Status</td>");
        html.append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(event.status().displayName()).append("</td></tr>");
        html.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd; font-weight: bold;'>Time</td>");
        html.append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(formatTimestamp(event)).append("</td></tr>");

        // Labels
        if (!event.labels().isEmpty()) {
            event.labels().forEach((key, value) -> {
                html.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd; font-weight: bold;'>")
                        .append(escapeHtml(key)).append("</td>");
                html.append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
                        .append(escapeHtml(value)).append("</td></tr>");
            });
        }

        html.append("</table>");
        html.append("</div>");

        // Footer
        html.append("<div style='background-color: #333; color: #999; padding: 15px; text-align: center; ");
        html.append("border-radius: 0 0 5px 5px; font-size: 12px;'>");
        html.append("Sent by J-Obs Alert System");
        html.append("</div>");

        html.append("</div></body></html>");
        return html.toString();
    }

    private String getSeverityColor(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "#dc3545";
            case WARNING -> "#ffc107";
            case INFO -> "#17a2b8";
        };
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
