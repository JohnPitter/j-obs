package io.github.jobs.spring.autoconfigure;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OnServerContainerCondition}.
 * <p>
 * This condition is used to prevent WebSocket auto-configuration from loading
 * in test environments using {@code MockServletContext}, which does not have
 * a real WebSocket ServerContainer.
 */
class OnServerContainerConditionTest {

    private OnServerContainerCondition condition;

    @Mock
    private ConditionContext context;

    @Mock
    private AnnotatedTypeMetadata metadata;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        condition = new OnServerContainerCondition();
    }

    @Nested
    @DisplayName("When not in a WebApplicationContext")
    class NotWebApplicationContext {

        @Test
        @DisplayName("should not match when ResourceLoader is not a WebApplicationContext")
        void shouldNotMatchWhenNotWebApplicationContext() {
            // Given a non-web resource loader
            ResourceLoader resourceLoader = mock(ResourceLoader.class);
            when(context.getResourceLoader()).thenReturn(resourceLoader);

            // When evaluating the condition
            ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

            // Then it should not match
            assertThat(outcome.isMatch()).isFalse();
            assertThat(outcome.getMessage()).contains("not a WebApplicationContext");
        }
    }

    @Nested
    @DisplayName("When ServletContext is null")
    class NullServletContext {

        @Test
        @DisplayName("should not match when ServletContext is null")
        void shouldNotMatchWhenServletContextIsNull() {
            // Given a WebApplicationContext with null ServletContext
            WebApplicationContext webContext = mock(WebApplicationContext.class);
            when(webContext.getServletContext()).thenReturn(null);
            when(context.getResourceLoader()).thenReturn(webContext);

            // When evaluating the condition
            ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

            // Then it should not match
            assertThat(outcome.isMatch()).isFalse();
            assertThat(outcome.getMessage()).contains("ServletContext is null");
        }
    }

    @Nested
    @DisplayName("When using MockServletContext (test environment)")
    class MockServletContextScenario {

        @Test
        @DisplayName("should not match when ServerContainer attribute is not set")
        void shouldNotMatchWithMockServletContext() {
            // Given a MockServletContext without ServerContainer (typical test scenario)
            MockServletContext mockServletContext = new MockServletContext();
            WebApplicationContext webContext = mock(WebApplicationContext.class);
            when(webContext.getServletContext()).thenReturn(mockServletContext);
            when(context.getResourceLoader()).thenReturn(webContext);

            // When evaluating the condition
            ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

            // Then it should not match
            assertThat(outcome.isMatch()).isFalse();
            assertThat(outcome.getMessage()).contains("ServerContainer not found");
        }

        @Test
        @DisplayName("should not match when ServerContainer attribute is explicitly null")
        void shouldNotMatchWhenServerContainerIsNull() {
            // Given a ServletContext with null ServerContainer attribute
            MockServletContext mockServletContext = new MockServletContext();
            mockServletContext.setAttribute("jakarta.websocket.server.ServerContainer", null);
            WebApplicationContext webContext = mock(WebApplicationContext.class);
            when(webContext.getServletContext()).thenReturn(mockServletContext);
            when(context.getResourceLoader()).thenReturn(webContext);

            // When evaluating the condition
            ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

            // Then it should not match
            assertThat(outcome.isMatch()).isFalse();
        }
    }

    @Nested
    @DisplayName("When ServerContainer is available (real servlet container)")
    class RealServerContainerScenario {

        @Test
        @DisplayName("should match when Jakarta ServerContainer is present")
        void shouldMatchWithJakartaServerContainer() {
            // Given a ServletContext with Jakarta ServerContainer attribute
            MockServletContext mockServletContext = new MockServletContext();
            Object mockServerContainer = new Object(); // Simulating a ServerContainer
            mockServletContext.setAttribute("jakarta.websocket.server.ServerContainer", mockServerContainer);

            WebApplicationContext webContext = mock(WebApplicationContext.class);
            when(webContext.getServletContext()).thenReturn(mockServletContext);
            when(context.getResourceLoader()).thenReturn(webContext);

            // When evaluating the condition
            ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

            // Then it should match
            assertThat(outcome.isMatch()).isTrue();
            assertThat(outcome.getMessage()).contains("ServerContainer in ServletContext");
        }

        @Test
        @DisplayName("should match when legacy javax ServerContainer is present")
        void shouldMatchWithLegacyJavaxServerContainer() {
            // Given a ServletContext with legacy javax ServerContainer attribute
            MockServletContext mockServletContext = new MockServletContext();
            Object mockServerContainer = new Object(); // Simulating a ServerContainer
            mockServletContext.setAttribute("javax.websocket.server.ServerContainer", mockServerContainer);

            WebApplicationContext webContext = mock(WebApplicationContext.class);
            when(webContext.getServletContext()).thenReturn(mockServletContext);
            when(context.getResourceLoader()).thenReturn(webContext);

            // When evaluating the condition
            ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

            // Then it should match (fallback to legacy attribute)
            assertThat(outcome.isMatch()).isTrue();
            assertThat(outcome.getMessage()).contains("ServerContainer in ServletContext");
        }
    }

    @Nested
    @DisplayName("Condition message formatting")
    class ConditionMessageTest {

        @Test
        @DisplayName("should provide helpful message for test environment detection")
        void shouldProvideHelpfulMessageForTestEnvironment() {
            // Given a typical test setup with MockServletContext
            MockServletContext mockServletContext = new MockServletContext();
            WebApplicationContext webContext = mock(WebApplicationContext.class);
            when(webContext.getServletContext()).thenReturn(mockServletContext);
            when(context.getResourceLoader()).thenReturn(webContext);

            // When evaluating the condition
            ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

            // Then the message should hint at test environment
            assertThat(outcome.getMessage())
                    .contains("MockServletContext")
                    .describedAs("Message should mention MockServletContext to help developers identify the issue");
        }
    }
}
