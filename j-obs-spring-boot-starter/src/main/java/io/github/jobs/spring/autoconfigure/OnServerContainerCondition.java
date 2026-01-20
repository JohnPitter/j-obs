package io.github.jobs.spring.autoconfigure;

import jakarta.servlet.ServletContext;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.context.WebApplicationContext;

/**
 * Condition that matches when a WebSocket {@code ServerContainer} is available
 * in the {@link ServletContext}.
 * <p>
 * This condition is used to prevent WebSocket auto-configuration from failing
 * in test environments that use {@code MockServletContext}, which does not
 * have a real WebSocket container.
 * <p>
 * The condition checks for the presence of the {@code javax.websocket.server.ServerContainer}
 * attribute in the {@code ServletContext}. This attribute is set by the servlet
 * container (Tomcat, Jetty, etc.) when WebSocket support is available.
 *
 * @see jakarta.websocket.server.ServerContainer
 */
public class OnServerContainerCondition extends SpringBootCondition {

    /**
     * The ServletContext attribute name for the WebSocket ServerContainer.
     */
    private static final String SERVER_CONTAINER_ATTRIBUTE = "jakarta.websocket.server.ServerContainer";

    /**
     * Legacy attribute name for javax.websocket (pre-Jakarta EE 9).
     */
    private static final String LEGACY_SERVER_CONTAINER_ATTRIBUTE = "javax.websocket.server.ServerContainer";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConditionMessage.Builder message = ConditionMessage.forCondition("ServerContainer Condition");

        // Check if we're in a web application context
        if (!(context.getResourceLoader() instanceof WebApplicationContext)) {
            return ConditionOutcome.noMatch(message.because("not a WebApplicationContext"));
        }

        WebApplicationContext webContext = (WebApplicationContext) context.getResourceLoader();
        ServletContext servletContext = webContext.getServletContext();

        if (servletContext == null) {
            return ConditionOutcome.noMatch(message.because("ServletContext is null"));
        }

        // Check for Jakarta EE 9+ attribute (jakarta.websocket)
        Object serverContainer = servletContext.getAttribute(SERVER_CONTAINER_ATTRIBUTE);

        // Fallback to legacy javax.websocket attribute
        if (serverContainer == null) {
            serverContainer = servletContext.getAttribute(LEGACY_SERVER_CONTAINER_ATTRIBUTE);
        }

        if (serverContainer != null) {
            return ConditionOutcome.match(message.foundExactly("ServerContainer in ServletContext"));
        }

        return ConditionOutcome.noMatch(
                message.because("ServerContainer not found in ServletContext (test environment with MockServletContext?)"));
    }
}
